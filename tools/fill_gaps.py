"""Last pass on the verses that still have no Bengali or Odia translation: one direct question per verse, with strict checks.

    py tools/fill_gaps.py --dry                  list the pages that would be read
    py tools/fill_gaps.py --cache-only           judge the readings already saved, no API call
    py tools/fill_gaps.py                        Bengali and Odia
    py tools/fill_gaps.py --bn   |   --or        one language

Needs GEMINI_API_KEY (environment and/or the keys in content/_bengali/bn_extract/.env; several keys are rotated).
For each empty verse it looks at the pages around the gap (the pages of the verse before and the verse after) and asks the model
for that verse's translation only. A reading is rejected, and the verse stays empty, when it
  - contains a verse marker (॥) or Devanagari text, i.e. it is the Sanskrit shloka and not the translation
  - is the same text as a neighbouring verse or as another reading
  - is far shorter or longer than the Hindi translation of the same verse (ratio outside 0.3 .. 4)
Accepted readings are written only into verses that are empty in content.db, in both copies. Everything is cached per verse in
content/_odia/work_v/gaps2/, so a re-run (after a quota stop) continues where it ended. Lists: gaps2_filled.json / gaps2_rejected.json.
"""
import json
import os
import re
import shutil
import sqlite3
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DBS = [ROOT / "content" / "content.db", ROOT / "app" / "src" / "main" / "assets" / "content.db"]
WORK = ROOT / "content" / "_odia" / "work_v"
CACHE = WORK / "gaps2"
DRY = "--dry" in sys.argv
CACHE_ONLY = "--cache-only" in sys.argv   # judge the readings already saved, no API call
BN_IMAGES = ROOT / "content" / "_bengali" / "bn_extract" / "work" / "images"
MODELS = [m for m in [os.environ.get("GAPS_MODEL")] if m] + [
    "gemini-3.1-flash-lite", "gemini-3.5-flash-lite", "gemini-3.8-flash", "gemini-3-flash-preview",
    "gemini-3.6-flash", "gemini-3.7-flash", "gemini-3.5-flash", "gemini-2.5-flash"]
LANGS = {
    "bn": dict(name="Bengali", col="bn", frm="bn_from", folder="bn", marker="॥", digits="০১২৩৪৫৬৭৮৯"),
    "or": dict(name="Odia", col='"or"', frm="or_from", folder="or", marker="॥", digits="୦୧୨୩୪୫୬୭୮୯"),
}
sys.path.insert(0, str(ROOT / "content" / "_odia"))
sys.path.insert(0, str(ROOT / "content" / "_build"))
sys.stdout.reconfigure(encoding="utf-8")


def keys():
    out = [k for k in re.split(r"[,;\s]+", os.environ.get("GEMINI_API_KEY", "")) if k]
    env = ROOT / "content" / "_bengali" / "bn_extract" / ".env"
    if env.exists():
        for line in env.read_text(encoding="utf-8").splitlines():
            if line.startswith("GEMINI_API_KEY="):
                for k in re.split(r"[,;\s]+", line.split("=", 1)[1].strip().strip('"')):
                    if k and k not in out:
                        out.append(k)
    return out


def chapter_file(lang, s, c):
    return ROOT / "content" / LANGS[lang]["folder"] / (f"skandha-{s:02d}" if s else ("mahatmya" if lang == "or" else "skandha-00")) / f"chapter-{c:02d}.md"


def pages_of(path, digits):
    t = path.read_text(encoding="utf-8")
    vol = int(re.search(r'source_pages:\s*\["v(\d)', t).group(1))
    tr = str.maketrans(digits, "0123456789")
    pages, cur = {}, None
    for line in t.splitlines():
        m = re.match(r"###\s*\S+\s+([" + digits + r"0-9]+)", line)
        if m:
            cur = int(m.group(1).translate(tr))
        m = re.search(r"<!--\s*page:\s*(\d+)", line)
        if m and cur is not None and cur not in pages:
            pages[cur] = int(m.group(1))
    srcs = [int(x) for x in re.findall(r"v\d:(\d+)", re.search(r"source_pages:\s*\[(.*?)\]", t).group(1))]
    return vol, pages, srcs


def plan(lang):
    L = LANGS[lang]
    db = sqlite3.connect(DBS[0])
    rows = db.execute(f'''SELECT skandha, chapter, num FROM verse WHERE coalesce({L["col"]},'')='' AND {L["frm"]} IS NULL ORDER BY 1,2,3''').fetchall()
    by = defaultdict(list)
    for s, c, n in rows:
        by[(s, c)].append(n)
    todo = defaultdict(set)
    for (s, c), ns in by.items():
        p = chapter_file(lang, s, c)
        if not p.exists():
            continue
        vol, pages, srcs = pages_of(p, L["digits"])
        for n in ns:
            before = max((pg for v, pg in pages.items() if v < n), default=srcs[0])
            after = min((pg for v, pg in pages.items() if v > n), default=srcs[-1] + 1)
            for pg in range(before, after + 1):
                todo[(vol, pg)].add((s, c, n))
    return todo, len(rows)


def image(lang, vol, p):
    if lang == "bn":
        f = BN_IMAGES / f"v{vol}_p{p:04d}.png"
        if f.exists():
            return f
        sys.exit(f"Bengali page image missing: {f} (run vision_extract for that page first)")
    for v in (1, 2):
        os.environ.setdefault(f"ODIA_PDF_{v}", str(Path.home() / "Downloads" / f"Srimad-Bhagavata-Mahapurana-Gita-Press-Odia-Part-{v}.pdf"))
    import odia_vision as ov
    return ov.page_image(vol, p)


def ask(cl, model, img, name, s, c, n):
    from google.genai import types
    q = (f"This is a page of the Gita Press {name} Shrimad Bhagavata Mahapurana. Find the {name.upper()} TRANSLATION paragraph of "
         f"Bhagavatam verse number {n} of this chapter (the paragraph that follows the Sanskrit shloka marked ॥ {n} ॥). "
         f"Do NOT copy the Sanskrit shloka itself; the translation is prose in {name}. "
         "Answer ONE JSON object: {\"found\": true or false, \"text\": \"the translation exactly as printed\"}. "
         "If the verse is not on this page, found is false and text is empty. Never guess, complete or invent text.")
    r = cl.models.generate_content(model=model, contents=[types.Part.from_bytes(data=img.read_bytes(), mime_type="image/png"), q],
                                   config=types.GenerateContentConfig(temperature=0, response_mime_type="application/json"))
    d = json.loads(re.sub(r"^```(?:json)?\s*|\s*```$", "", (r.text or "").strip()))
    return (d.get("text") or "").strip() if d.get("found") else ""


def clean(text, n=None, digits="0123456789"):
    """One line of text. When the model returned several verses, keep the one that ends with the printed marker for verse n."""
    t = re.sub(r"\s+", " ", text).strip()
    mark = re.compile(r"[।॥]{1,2}\s*([" + digits + r"0-9]+)\s*[।॥]{1,2}")
    marks = list(mark.finditer(t))
    if n is not None and marks:
        tr = str.maketrans(digits, "0123456789")
        start = 0
        for m in marks:
            if int(m.group(1).translate(tr)) == n:
                t = t[start:m.start()].strip() + "।"
                break
            start = m.end()
        else:
            t = mark.sub(" ", t)
    else:
        t = mark.sub(" ", t)
    return re.sub(r"\s+", " ", t).strip()


def verdict(text, lang, hi, neighbours, others):
    L = LANGS[lang]
    if not text or len(text) < 20:
        return "empty or too short"
    if "॥" in text or re.search(r"[ऀ-ॣ०-ॿ]", text):   # the danda (U+0964) is part of Bengali and Odia too
        return "Sanskrit or marker text"
    if len(re.findall(r"[ঃଃ](?=[\s।,;]|$)", text)) >= 2:   # word-final visarga is Sanskrit, not Bengali or Odia prose
        return "Sanskrit text (visarga)"
    script = {"bn": r"[ঀ-৿]", "or": r"[଀-୿]"}[lang]
    if len(re.findall(script, text)) < 0.7 * len(re.sub(r"[\s\d.,;:!?'\"‘’“”()\-—–।]", "", text)):
        return "not mostly " + L["name"]
    if text in neighbours:
        return "same as a neighbouring verse"
    if text in others:
        return "same as another reading"
    if hi:
        r = len(text) / max(1, len(hi))
        if not 0.3 <= r <= 4:
            return f"length {r:.1f}x the Hindi"
    return ""


def run_lang(lang, clients, st):
    L = LANGS[lang]
    todo, total = plan(lang)
    print(f"{L['name']}: {total} empty verses, {len(todo)} pages", flush=True)
    if DRY:
        for (vol, p), vs in sorted(todo.items())[:15]:
            print(f"  v{vol} page {p}: {sorted(vs)[:4]}")
        return {}, {}
    CACHE.mkdir(parents=True, exist_ok=True)
    db = sqlite3.connect(DBS[0])
    got, rej = {}, {}
    done = set()
    for (vol, p), vs in sorted(todo.items()):
        for s, c, n in sorted(vs):
            key = f"{s}.{c}.{n}"
            if key in done:
                continue
            cf = CACHE / f"{lang}_{key}_p{p}.json"
            if not cf.exists() and CACHE_ONLY:
                continue
            if cf.exists():
                r = json.loads(cf.read_text(encoding="utf-8"))
            else:
                r = None
                for _ in range(len(clients) * 2):
                    for m in MODELS:
                        try:
                            txt = ask(clients[st["i"]], m, image(lang, vol, p), L["name"], s, c, n)
                            r = {"text": txt}
                            break
                        except SystemExit:
                            raise
                        except Exception as e:
                            if "NOT_FOUND" in str(e) or "404" in str(e):
                                continue
                            if "429" in str(e) or "RESOURCE_EXHAUSTED" in str(e):
                                st["i"] = (st["i"] + 1) % len(clients)
                                print(f"  key rotated to #{st['i'] + 1}", flush=True)
                                break
                            print(f"  {key} p{p}: {str(e)[:90]}", flush=True)
                            r = {"text": "", "error": True}
                            break
                    if r is not None:
                        break
                if r is None:
                    print(f"  {key} p{p}: no reading (quota)", flush=True)
                    continue
                cf.write_text(json.dumps(r, ensure_ascii=False), encoding="utf-8")
            text = clean(r.get("text", ""), n, L["digits"])
            if not text:
                continue
            hi = (db.execute("SELECT hi FROM verse WHERE skandha=? AND chapter=? AND num=?", (s, c, n)).fetchone() or [""])[0] or ""
            nb = {(db.execute(f"SELECT {L['col']} FROM verse WHERE skandha=? AND chapter=? AND num=?", (s, c, m)).fetchone() or [""])[0] for m in (n - 1, n + 1)}
            why = verdict(text, lang, hi, {x.strip() for x in nb if x}, set(got.values()))
            if why:
                rej[key] = why
                print(f"  {key} p{p}: rejected ({why})", flush=True)
            else:
                got[key] = text
                done.add(key)
                print(f"  {key} p{p}: read", flush=True)
    return got, rej


def main():
    langs = [l for l, f in (("bn", "--or"), ("or", "--bn")) if f not in sys.argv]
    ks = keys()
    if not DRY and not CACHE_ONLY and not ks:
        sys.exit("GEMINI_API_KEY is not set")
    clients = []
    if not DRY and not CACHE_ONLY:
        from google import genai
        clients = [genai.Client(api_key=k) for k in ks]
    st = {"i": 0}
    allgot = {}
    allrej = {}
    for lang in langs:
        got, rej = run_lang(lang, clients, st)
        allgot[lang], allrej[lang] = got, rej
    if DRY:
        return
    (WORK / "gaps2_filled.json").write_text(json.dumps(allgot, ensure_ascii=False, indent=1), encoding="utf-8")
    (WORK / "gaps2_rejected.json").write_text(json.dumps(allrej, ensure_ascii=False, indent=1), encoding="utf-8")
    n = sum(len(v) for v in allgot.values())
    if not n:
        print("nothing could be filled")
        return
    for path in DBS:
        db = sqlite3.connect(path)
        for lang, got in allgot.items():
            L = LANGS[lang]
            for k, t in got.items():
                s, c, v = map(int, k.split("."))
                db.execute(f'''UPDATE verse SET {L["col"]}=?, {L["frm"]}=NULL WHERE skandha=? AND chapter=? AND num=?
                               AND coalesce({L["col"]},'')='' AND {L["frm"]} IS NULL''', (t, s, c, v))
        ver = int(db.execute("SELECT value FROM meta WHERE key='content_version'").fetchone()[0]) + 1
        db.execute("UPDATE meta SET value=? WHERE key='content_version'", (str(ver),))
        db.commit()
        db.close()
    subprocess.check_call([sys.executable, str(ROOT / "content" / "_search" / "build_index.py")], stdout=subprocess.DEVNULL)
    shutil.copy(ROOT / "content" / "_search" / "search.db", ROOT / "app" / "src" / "main" / "assets" / "search.db")
    subprocess.call([sys.executable, str(ROOT / "tools" / "missing_report.py"), "--readme"])
    print(f"\nfilled {n} verses; rejected {sum(len(v) for v in allrej.values())}. Lists: content/_odia/work_v/gaps2_filled.json, gaps2_rejected.json")


if __name__ == "__main__":
    main()
