"""Fills the Odia verses that the local readers never found, by reading only the pages around each gap with Gemini vision.

    py tools/fill_odia_gaps.py --dry       list the pages that would be read, no API call
    py tools/fill_odia_gaps.py             read them, fill the empty verses in both content.db copies, rebuild search.db

Needs GEMINI_API_KEY (environment, or the first key in content/_bengali/bn_extract/.env).
Rules it follows:
  - only a verse that is empty in content.db is ever written; existing text is never touched
  - a verse is filled only when the page's printed verse numbers and translation paragraphs line up one to one
  - every filled verse is listed in content/_odia/work_v/gaps_filled.json for a person to check
"""
import json
import os
import re
import sqlite3
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ODIA = ROOT / "content" / "_odia"
DBS = [ROOT / "content" / "content.db", ROOT / "app" / "src" / "main" / "assets" / "content.db"]
OUT = ODIA / "work_v" / "gaps_filled.json"
DRY = "--dry" in sys.argv
# the model the Bengali extractor already uses with this key; override with ODIA_GEMINI_MODEL
MODEL = os.environ.get("ODIA_GEMINI_MODEL", "gemini-3.1-flash-lite")
sys.path.insert(0, str(ODIA))
sys.path.insert(0, str(ROOT / "content" / "_build"))
sys.stdout.reconfigure(encoding="utf-8")


def api_key():
    k = os.environ.get("GEMINI_API_KEY", "").strip()
    if not k:
        env = ROOT / "content" / "_bengali" / "bn_extract" / ".env"
        if env.exists():
            for line in env.read_text(encoding="utf-8").splitlines():
                if line.startswith("GEMINI_API_KEY="):
                    k = line.split("=", 1)[1].strip().strip('"')
    return re.split(r"[,;\s]+", k)[0] if k else ""


def chapter_file(s, c):
    return ROOT / "content" / "or" / (f"skandha-{s:02d}" if s else "mahatmya") / f"chapter-{c:02d}.md"


def verse_pages(path):
    """verse number -> PDF page, and the volume, from the '<!-- page: N -->' comment under each verse heading."""
    t = path.read_text(encoding="utf-8")
    vol = int(re.search(r'source_pages:\s*\["v(\d)', t).group(1))
    pages, cur = {}, None
    for line in t.splitlines():
        m = re.match(r"###\s*ଶ୍ଳୋକ\s+([୦-୯0-9]+)", line)
        if m:
            cur = int(m.group(1).translate(str.maketrans("୦୧୨୩୪୫୬୭୮୯", "0123456789")))
        m = re.search(r"<!--\s*page:\s*(\d+)", line)
        if m and cur is not None and cur not in pages:
            pages[cur] = int(m.group(1))
    srcs = [int(x) for x in re.findall(r"v\d:(\d+)", re.search(r"source_pages:\s*\[(.*?)\]", t).group(1))]
    return vol, pages, srcs


def plan():
    db = sqlite3.connect(DBS[0])
    rows = db.execute('''SELECT skandha, chapter, num FROM verse WHERE coalesce("or",'')='' AND or_from IS NULL ORDER BY 1,2,3''').fetchall()
    by = defaultdict(list)
    for s, c, n in rows:
        by[(s, c)].append(n)
    todo = {}   # (vol, page) -> set of (s, c, n)
    for (s, c), ns in by.items():
        vol, pages, srcs = verse_pages(chapter_file(s, c))
        for n in ns:
            before = max((p for v, p in pages.items() if v < n), default=srcs[0])
            after = min((p for v, p in pages.items() if v > n), default=srcs[-1] + 1)
            for p in range(before, after + 1):
                todo.setdefault((vol, p), set()).add((s, c, n))
    return todo


def main():
    todo = plan()
    print(f"{len(todo)} pages hold the {len({v for x in todo.values() for v in x})} missing verses")
    if DRY:
        for (vol, p), vs in sorted(todo.items())[:40]:
            print(f"  v{vol} page {p}: verses {sorted(vs)[:4]}")
        print("  ...")
        return
    key = api_key()
    if not key:
        sys.exit("GEMINI_API_KEY is not set")
    os.environ["GEMINI_API_KEY"] = key
    for v in (1, 2):   # config.json points at an upload folder that only exists elsewhere
        f = Path.home() / "Downloads" / f"Srimad-Bhagavata-Mahapurana-Gita-Press-Odia-Part-{v}.pdf"
        os.environ.setdefault(f"ODIA_PDF_{v}", str(f))
        if not Path(os.environ[f"ODIA_PDF_{v}"]).exists():
            sys.exit(f"PDF not found: {os.environ[f'ODIA_PDF_{v}']} (set ODIA_PDF_{v})")
    import odia_vision as ov
    import add_odia
    cl = ov.client("gemini")
    filled = {}
    for (vol, p), vs in sorted(todo.items()):
        try:
            res = ov.transcribe_gemini(cl, MODEL, ov.page_image(vol, p))
            d = res[0] if isinstance(res, tuple) else res
        except Exception as e:
            print(f"v{vol} p{p}: failed ({str(e)[:160]})")
            if "NOT_FOUND" in str(e) or "404" in str(e):
                sys.exit(f"model {MODEL} is not available for this key; set ODIA_GEMINI_MODEL to one that is")
            continue
        nums, paras = d.get("sanskrit_verse_numbers") or [], d.get("paragraphs") or []
        skip = d.get("paragraphs_before_chapter_heading") or 0
        paras = paras[skip:] if len(paras) - skip == len(nums) else paras
        if len(nums) != len(paras):
            print(f"v{vol} p{p}: {len(nums)} verse numbers but {len(paras)} paragraphs, not used")
            continue
        for s, c, n in vs:
            if n in nums:
                filled[f"{s}.{c}.{n}"] = add_odia.clean_translation(paras[nums.index(n)])
                print(f"v{vol} p{p}: verse {s}.{c}.{n} read")
    OUT.parent.mkdir(exist_ok=True)
    OUT.write_text(json.dumps(filled, ensure_ascii=False, indent=1), encoding="utf-8")
    if not filled:
        print("nothing could be filled")
        return
    for path in DBS:
        db = sqlite3.connect(path)
        for k, t in filled.items():
            s, c, n = map(int, k.split("."))
            db.execute('''UPDATE verse SET "or"=?, or_from=NULL WHERE skandha=? AND chapter=? AND num=?
                          AND coalesce("or",'')='' AND or_from IS NULL''', (t, s, c, n))
        v = int(db.execute("SELECT value FROM meta WHERE key='content_version'").fetchone()[0]) + 1
        db.execute("UPDATE meta SET value=? WHERE key='content_version'", (str(v),))
        db.commit()
        db.close()
    subprocess.check_call([sys.executable, str(ROOT / "content" / "_search" / "build_index.py")])
    import shutil
    shutil.copy(ROOT / "content" / "_search" / "search.db", ROOT / "app" / "src" / "main" / "assets" / "search.db")
    subprocess.call([sys.executable, str(ROOT / "tools" / "missing_report.py"), "--readme"])
    print(f"\nfilled {len(filled)} verses; list in {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
