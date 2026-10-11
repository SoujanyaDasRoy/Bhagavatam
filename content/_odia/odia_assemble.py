#!/usr/bin/env python3
"""Assemble Odia page JSON (work/vision) into per-chapter Markdown in the same layout as content/hi and the
Bengali tool output (out/skandha-NN/chapter-NN.md).  Adapted from bn_extract.py `assemble`.
"""
import argparse, csv, json, os, re, sys
from collections import defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent
WORK = Path(os.environ.get("ODIA_WORK", HERE / "work"))
OUT = Path(os.environ.get("ODIA_OUT", HERE / "out"))
REVIEW = Path(os.environ.get("ODIA_REVIEW", HERE / "review"))
CFG = json.loads((HERE / "config.json").read_text(encoding="utf-8"))
EXPECTED = json.loads((HERE / "data" / "expected_verses.json").read_text(encoding="utf-8"))
sys.stdout.reconfigure(encoding="utf-8")

OD = "୦୧୨୩୪୫୬୭୮୯"
SK_NAMES = ["", "ପ୍ରଥମ", "ଦ୍ଵିତୀୟ", "ତୃତୀୟ", "ଚତୁର୍ଥ", "ପଞ୍ଚମ", "ଷଷ୍ଠ", "ସପ୍ତମ", "ଅଷ୍ଟମ", "ନବମ", "ଦଶମ", "ଏକାଦଶ", "ଦ୍ଵାଦଶ"]


def od_digits(n):
    return "".join(OD[int(c)] if c.isdigit() else c for c in str(n))


def od2int(s):
    t = "".join(str(OD.index(c)) for c in s if c in OD)
    return int(t) if t else None


D = r"(?:[୦-୯]\s?){1,3}"
RANGE = D + r"(?:\s*[,\-–—~]\s*" + D + r")*"
MARK = re.compile(r"[॥।|!]{1,3}[’'”\"]?\s*(" + RANGE + r")\s*[॥।|!]{1,3}")
MARK_END = re.compile(r"[॥।|!]{1,3}[’'”\"]?\s*(" + RANGE + r")\s*[॥।|!]{0,3}\s*$")


def parse_marker(raw):
    nums = [od2int(x) for x in re.split(r"[,\-–—~]", raw) if od2int(x) is not None]
    if not nums:
        return None, None
    return nums[0], nums[-1]


# ---------------------------------------------------------------- structure: ordered list of chapters
def build_order():
    order = []
    for k in sorted(EXPECTED["verses"], key=lambda k: tuple(int(x) for x in k.split("."))):
        s, c = (int(x) for x in k.split("."))
        order.append((s, c))
    return order


ORDER = build_order()
IDX = {k: i for i, k in enumerate(ORDER)}


def section_name(s):
    return "mahatmya" if s == 0 else f"skandha-{s:02d}"


def exp_verses(s, c):
    return EXPECTED["verses"].get(f"{s}.{c}")


# ---------------------------------------------------------------- pages
def load_pages():
    pages = []
    src = WORK / "fixed" if (WORK / "fixed").exists() and not os.environ.get("ODIA_RAW") else WORK / "vision"
    for f in sorted(src.glob("v*_p*.json")):
        d = json.loads(f.read_text(encoding="utf-8"))
        m = re.match(r"v(\d+)_p(\d+)", f.stem)
        d["_vol"], d["_pg"] = m.group(1), int(m.group(2))
        pages.append(d)
    pages.sort(key=lambda d: (d["_vol"], d["_pg"]))
    return pages


class Chapter:
    def __init__(self, s, c):
        self.s, self.c = s, c
        self.title = ""
        self.pages = []
        self.units = []
        self.pending = []
        self.pending_src = []
        self.pending_flags = []
        self.colophon = ""
        self.notes = []
        self.leftover = ""
        self.sa_numbers = set()
        self.heads = []
        self.footnotes = []
        self.closed = False
        self.mismatch = 0
        self.expect_new = False

    @property
    def section(self):
        return section_name(self.s)

    @property
    def key(self):
        return f"{self.s}.{self.c}"

    def prev_end(self):
        return max((u["b"] for u in self.units), default=0)


STRICT = []


def assemble_chapters(pages, starts, ends=None):
    """starts: {(vol,page): (s,c)} marks where the stream (re)starts; ends: {vol: (s,c)} last chapter of a volume."""
    ends = ends or {}
    chapters = []
    cur = None
    warnings = []
    finished = set()
    for pg in pages:
        v, p = pg["_vol"], pg["_pg"]
        key = (v, p)
        skip = any(sv == v and a <= p <= b for sv, a, b in CFG.get("skip_pages", []))
        if skip:
            continue
        forced = None
        if key in starts:
            s, c = starts[key]
            if cur is not None and str(pg.get("chapter_marker_text") or "").startswith("boxed heading") and int(pg.get("paragraphs_before_chapter_heading") or 0) > 0:
                forced = (s, c)      # heading page with pre-heading text: text belongs to the previous chapter
            else:
                if cur is not None and cur.pending:
                    cur.leftover = " ".join(cur.pending)
                    cur.pending, cur.pending_src, cur.pending_flags = [], [], []
                cur = Chapter(s, c)
                chapters.append(cur)
        if cur is None or v in finished:
            continue
        if v in ends and (cur.s, cur.c) == tuple(ends[v]):
            exp_last = exp_verses(cur.s, cur.c) or []
            if exp_last and cur.prev_end() >= max(exp_last):
                finished.add(v)
                continue
        colo = pg.get("colophon")
        if colo and not re.match(r"^ଇତି\s+[ଖଶ]\S{0,3}[ମନ]", colo.strip()):
            pg["colophon"] = None      # a Sanskrit verse line that starts with "ଇତି", not a chapter colophon
        H = pg.get("running_head_chapter")
        if isinstance(H, int) and cur.units:
            expc = exp_verses(cur.s, cur.c) or []
            if H != cur.c and (not expc or cur.prev_end() >= max(expc) * 0.5):
                cur.expect_new = True
                cur.mismatch += 1
            elif H == cur.c:
                cur.expect_new = False
                cur.mismatch = 0
        paras = list(pg.get("paragraphs", []))
        before = int(pg.get("paragraphs_before_chapter_heading") or 0)
        heading = str(pg.get("chapter_marker_text") or "").startswith("boxed heading")
        if (v, p) not in cur.pages:
            cur.pages.append((v, p))
        cur.sa_numbers.update(n for n in pg.get("sanskrit_verse_numbers", []) if isinstance(n, int))
        cur.heads.append((p, pg.get("running_head_skandha"), pg.get("running_head_chapter")))
        if pg.get("colophon"):
            cur.colophon = pg["colophon"]
        for fn in pg.get("footnotes", []):
            cur.footnotes.append((p, fn.strip()))

        def advance(reason):
            nonlocal cur
            if any(sv == v and a <= p <= b for sv, a, b in STRICT):
                return False      # dense, verified chapter starts exist for this range: chapters change only at starts
            i = IDX.get((cur.s, cur.c))
            if i is None or i + 1 >= len(ORDER):
                return False
            if cur.pending:
                cur.leftover = " ".join(cur.pending)
                cur.pending, cur.pending_src, cur.pending_flags = [], [], []
            ns, nc = ORDER[i + 1]
            cur = Chapter(ns, nc)
            chapters.append(cur)
            cur.pages.append((v, p))
            cur.notes.append(f"started on v{v} p{p} ({reason})")
            return True

        def feed(ch, plist, continues):
            first = True
            for para in plist:
                para = re.sub(r"([॥।|!]+)\s*\[\?\]", r"\1", para.strip())
                para = re.sub(r"\[\?\]\s*([॥।|!]+)", r"\1", para)
                if not para:
                    continue
                glue = " " if (first and continues and ch.pending) else None
                if first and continues and ch.pending:
                    ch.pending_flags.append("continued across a page")
                first = False
                pos = 0
                while True:
                    m = MARK.search(para, pos)
                    if not m:
                        m2 = MARK_END.search(para, pos)
                        if m2:
                            m = m2
                    if not m:
                        break
                    seg = para[pos:m.end()].strip()
                    pos = m.end()
                    a, b = parse_marker(m.group(1))
                    if glue is not None and ch.pending:
                        ch.pending[-1] = ch.pending[-1] + glue + seg
                    else:
                        ch.pending.append(seg)
                    glue = None
                    ch.pending_src.append((v, p))
                    text = "\n\n".join(ch.pending)
                    src = sorted(set(ch.pending_src))
                    fl = list(ch.pending_flags)
                    ch.pending, ch.pending_src, ch.pending_flags = [], [], []
                    # chapter reset by numbering: marker restarts at 1 after a (nearly) complete chapter
                    nonlocal_cur = ch
                    if a is not None and a <= 4 and nonlocal_cur.prev_end() >= 6:
                        exp = exp_verses(nonlocal_cur.s, nonlocal_cur.c) or []
                        if not exp or nonlocal_cur.closed or nonlocal_cur.expect_new or (a == 1 and nonlocal_cur.prev_end() >= max(exp) * 0.6):
                            if advance("verse numbering restarted"):
                                nonlocal_cur = cur
                    nonlocal_cur.units.append({"a": a, "b": b, "text": text, "src": src, "flags": fl})
                    ch = nonlocal_cur
                    first_in_chain[0] = ch
                rest = para[pos:].strip()
                if rest:
                    if glue is not None and ch.pending:
                        ch.pending[-1] = ch.pending[-1] + glue + rest
                    else:
                        ch.pending.append(rest)
                    ch.pending_src.append((v, p))
            return ch

        first_in_chain = [cur]
        n_chapters_before = len(chapters)
        cont = bool(pg.get("first_paragraph_continues_previous_page"))
        if heading:
            if pg.get('colophon'):
                cur.closed = True
            cur = feed(cur, paras[:before], cont)
            exp = exp_verses(cur.s, cur.c) or []
            head_ch = pg.get("running_head_chapter")
            if forced:
                if cur.pending:
                    cur.leftover = " ".join(cur.pending)
                    cur.pending, cur.pending_src, cur.pending_flags = [], [], []
                cur = Chapter(*forced)
                chapters.append(cur)
                cur.pages.append((v, p))
                cur.notes.append(f"started on v{v} p{p} (forced start)")
            elif cur.units and (not exp or cur.closed or cur.prev_end() >= max(exp) * 0.6 or (isinstance(head_ch, int) and head_ch > cur.c)):
                advance("heading box")
            elif cur.units:
                cur.notes.append(f"v{v} p{p}: heading box ignored (chapter only at verse {cur.prev_end()} of {max(exp) if exp else '?'})")
            if pg.get("chapter_title"):
                cur.title = re.sub(r"^[\(\[]?\s*ଅ[ଥଧ][^\)\]]{0,40}[\)\]]\s*", "", pg["chapter_title"]).strip()
            cur = feed(cur, paras[before:], False)
        else:
            cur = feed(cur, paras, cont)
            expc = exp_verses(cur.s, cur.c) or []
            moved = len(chapters) > n_chapters_before
            if not moved and cur.mismatch >= 2 and (not expc or cur.prev_end() >= max(expc) * 0.8):
                advance("resynced by running-head chapter number")
                moved = True
            if pg.get("colophon") and not moved:
                if cur.units:
                    advance("colophon at end of page")
                else:
                    cur.closed = True
    if cur is not None and cur.pending:
        cur.leftover = " ".join(cur.pending)
    return chapters


def compact(nums):
    out, i = [], 0
    while i < len(nums):
        j = i
        while j + 1 < len(nums) and nums[j + 1] == nums[j] + 1:
            j += 1
        out.append(str(nums[i]) if i == j else f"{nums[i]}-{nums[j]}")
        i = j + 1
    return ", ".join(out)


def check_chapter(ch):
    problems = []
    exp = exp_verses(ch.s, ch.c)
    expset = set(exp) if exp else None
    seen, prev_end = [], 0
    for u in ch.units:
        a, b = u["a"], u["b"]
        bad = a is None or b is None or b < a
        if not bad and expset is not None and (a not in expset or b not in expset or a in seen):
            nxt = prev_end + 1
            if nxt in expset and nxt not in seen:
                u["flags"].append(f"marker read as {a if a == b else f'{a}-{b}'}, set to {nxt} from the sequence")
                u["a"] = u["b"] = a = b = nxt
            else:
                u["flags"].append(f"verse number {a} not usable (not in the Hindi edition or repeated)")
                continue
        if bad:
            u["flags"].append("bad verse marker"); continue
        if a <= prev_end:
            u["flags"].append(f"verse number not increasing (after {prev_end})")
        elif a != prev_end + 1 and prev_end:
            u["flags"].append(f"gap: {prev_end + 1}-{a - 1} missing before this")
        prev_end = max(prev_end, b)
        seen.extend(range(a, b + 1))
    if exp:
        missing = sorted(set(exp) - set(seen)); extra = sorted(set(seen) - set(exp))
        if missing: problems.append(f"verses with no translation unit: {compact(missing)}")
        if extra: problems.append(f"translation verse numbers not in the Hindi edition: {compact(extra)}")
    else:
        problems.append("no Hindi reference for this chapter")
    if ch.sa_numbers and seen:
        only_sa = sorted(ch.sa_numbers - set(seen))
        if only_sa and not (exp and set(only_sa) - set(exp)): problems.append(f"Sanskrit column has verse numbers with no translation unit: {compact(only_sa)}")
    if ch.leftover:
        done = bool(exp) and prev_end >= max(exp)
        if done:
            ch.notes.append("trailing text after the last verse (colophon/ornament line) not kept: " + ch.leftover[:60])
        else:
            problems.append("text after the last verse marker was not closed by a marker: " + ch.leftover[:80])
    return problems


ODIA = re.compile(r"[଀-୿]")


def unit_flags(u):
    t = u["text"]
    q = t.count("[?]")
    if q >= 3:
        u["flags"].append(f"{q} uncertain readings marked [?]")
    clean = t.replace("[?]", "")
    letters = len(ODIA.findall(clean))
    junk = len(re.findall(r"[A-Za-z/\\<>=#@~`{}\[\]®©×±«»›‹¥§]", clean))
    if junk >= 2 or (letters and junk / max(1, letters) > 0.02):
        u["flags"].append(f"{junk} stray non-Odia symbol(s)")
    from odia_fix import GARBAGE, KNOWN_OK, SPLIT
    resid = [w for w in SPLIT.split(clean) if w and w not in KNOWN_OK and "ଙ୍କ" not in w and GARBAGE.match(w)]
    if resid:
        u["flags"].append(f"possible unfixed ଙ୍କ misread: {', '.join(resid[:3])}")
    if not MARK_END.search(t.strip().split("\n\n")[-1]) and not MARK.search(t.strip().split("\n\n")[-1]):
        u["flags"].append("does not end with a verse marker")
    if letters < 8:
        u["flags"].append("very short text")


def chapter_md(ch, status):
    srcs = sorted({f"v{v}:{p}" for v, p in ch.pages})
    pdfs = sorted({Path(CFG["volumes"][v]["path"]).name for v, _ in ch.pages})
    title = ch.title.replace('"', "'")
    head = ["---", f"skandha: {ch.s}", f"chapter: {ch.c}", f'title: "{title}"', "language: or", f"section: {ch.section}",
            f'source_pdf: "{"; ".join(pdfs)}"', "source_pages: [" + ", ".join(f'"{x}"' for x in srcs) + "]",
            'extraction_method: "Tesseract OCR (ori) + layout analysis"', f'verification_status: "{status}"', "---", ""]
    head.append(f"# {SK_NAMES[ch.s]} ସ୍କନ୍ଧ" if ch.s else "# ମାହାତ୍ମ୍ୟ")
    head += ["", f"## ଅଧ୍ୟାୟ {od_digits(ch.c)}" + (f" - {ch.title}" if ch.title else ""), ""]
    body = []
    for u in ch.units:
        label = od_digits(u["a"]) if u["a"] == u["b"] else f"{od_digits(u['a'])}–{od_digits(u['b'])}"
        body.append(f"### ଶ୍ଳୋକ {label}")
        meta = f"page: {u['src'][0][1]}"
        if u["flags"]:
            real = [f for f in u["flags"]]
            meta += " | FLAGS: " + "; ".join(real)
        body.append(f"<!-- {meta} -->")
        body.append("")
        body.append(u["text"])
        body.append("")
    if ch.colophon:
        body += ["---", "", ch.colophon, ""]
    if ch.footnotes:
        body += ["### ଟୀକା", ""]
        body += [f"- (ପୃଷ୍ଠା {od_digits(p)}) {t}" for p, t in ch.footnotes]
        body.append("")
    return "\n".join(head + body)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--starts", default=str(HERE / "data" / "starts.json"))
    a = ap.parse_args()
    starts_raw = json.loads(Path(a.starts).read_text(encoding="utf-8"))
    ends = {k: tuple(v) for k, v in starts_raw.pop("_ends", {}).items()}
    STRICT.extend((k, a, b) for k, (a, b) in starts_raw.pop("_strict", {}).items())
    starts = {(k.split(":")[0], int(k.split(":")[1])): tuple(v) for k, v in starts_raw.items()}
    pages = load_pages()
    chapters = assemble_chapters(pages, starts, ends)
    OUT.mkdir(parents=True, exist_ok=True); REVIEW.mkdir(parents=True, exist_ok=True)
    flagged, rows = [], []
    for ch in chapters:
        problems = check_chapter(ch)
        for u in ch.units:
            unit_flags(u)
        real = lambda u: [f for f in u["flags"] if not f.startswith("continued")]
        nflag = sum(1 for u in ch.units if real(u))
        status = "machine_transcribed_clean" if not problems and nflag == 0 else "needs_review"
        sub = OUT / ch.section; sub.mkdir(parents=True, exist_ok=True)
        target = sub / f"chapter-{ch.c:02d}.md"
        if target.exists() and 'verification_status: "reviewed"' in target.read_text(encoding="utf-8"):
            print("kept reviewed", target); continue
        target.write_text(chapter_md(ch, status), encoding="utf-8")
        for u in ch.units:
            if real(u):
                flagged.append([ch.key, f"{u['a']}-{u['b']}" if u["a"] != u["b"] else str(u["a"]), f"v{u['src'][0][0]} p{u['src'][0][1]}", "; ".join(real(u)), u["text"].replace("\n", " ")[:300], f"{ch.section}/chapter-{ch.c:02d}.md"])
        rows.append((ch, problems, nflag, status))
    with open(REVIEW / "flagged.csv", "w", newline="", encoding="utf-8-sig") as f:
        w = csv.writer(f); w.writerow(["chapter", "verses", "page", "flags", "text_start", "file", "fixed(y)"])
        for r in flagged: w.writerow(r + [""])
    lines = ["# Odia extraction report", "", f"- pages OCRed: {len(pages)}", f"- chapters assembled: {len(chapters)}",
             f"- translation units: {sum(len(c.units) for c in chapters)}", f"- units flagged: {len(flagged)}", "",
             "## Chapters", "", "| chapter | pages | verses | flagged | status | problems |", "|---|---|---|---|---|---|"]
    for ch, problems, nflag, status in rows:
        pg = sorted(p for _, p in ch.pages)
        lines.append(f"| {ch.key} | {pg[0]}-{pg[-1]} | {len(ch.units)} | {nflag} | {status} | {'; '.join(problems)} |")
    notes = [f"- {c.key}: {n}" for c in chapters for n in c.notes if "ignored" in n or "restarted" in n]
    if notes: lines += ["", "## Chapter-boundary notes", ""] + notes
    (REVIEW / "report.md").write_text("\n".join(lines), encoding="utf-8")
    print("\n".join(lines[:8]))


if __name__ == "__main__":
    main()
