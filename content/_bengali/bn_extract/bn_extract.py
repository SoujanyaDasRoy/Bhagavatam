#!/usr/bin/env python3
"""
bn_extract.py - Bengali Bhagavatam (Gita Press) transcription pipeline.

Reads the two scanned Gita Press Bengali PDFs, has a vision model transcribe the Bengali translation of every page,
cross-checks each verse against an independent OCR (Tesseract text, bundled), checks verse-number sequence against the
Hindi edition, and writes one Markdown file per chapter in the same layout as content/hi and content/en.

Nothing is silently corrected: anything uncertain stays in the text as printed-or-best-reading with a "[?]" after the
word, and every verse with a problem is listed in review/flagged.csv and review/index.html.

Commands (run from this folder):
    python bn_extract.py check                      # dependencies, PDFs, API key
    python bn_extract.py estimate [--vol 1] [--pages 200-300]
    python bn_extract.py run      [--vol 1] [--pages 200-300] [--workers 4] [--model MODEL] [--limit N] [--force]
    python bn_extract.py assemble                   # build Markdown + review files from the pages done so far
    python bn_extract.py status

Resumable: a page is done when work/vision/vN_pNNNN.json exists. Stop and restart any time.
"""
from __future__ import annotations

import argparse
import base64
import csv
import html
import io
import json
import os
import re
import shutil
import subprocess
import sys
import threading
import time
from collections import defaultdict
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stderr.reconfigure(encoding="utf-8")
except Exception:
    pass

HERE = Path(__file__).resolve().parent
CONFIG_PATH = HERE / "config.json"
WORK = HERE / "work"
OUT = HERE / "out"
REVIEW = HERE / "review"
DATA = HERE / "data"

DEFAULT_MODEL = os.environ.get("BN_MODEL", "claude-sonnet-5-5")

BN_DIGITS = "০১২৩৪৫৬৭৮৯"
SKANDHA_WORDS = {
    "প্রথম": 1, "দ্বিতীয়": 2, "তৃতীয়": 3, "চতুর্থ": 4, "পঞ্চম": 5, "ষষ্ঠ": 6,
    "সপ্তম": 7, "অষ্টম": 8, "নবম": 9, "দশম": 10, "একাদশ": 11, "দ্বাদশ": 12,
}
SKANDHA_NAMES = ["", "প্রথম", "দ্বিতীয়", "তৃতীয়", "চতুর্থ", "পঞ্চম", "ষষ্ঠ", "সপ্তম", "অষ্টম", "নবম", "দশম", "একাদশ", "দ্বাদশ"]

BENGALI_ORDINALS = {
    "প্রথম": 1, "দ্বিতীয়": 2, "দ্বিতীয়": 2, "তৃতীয়": 3, "তৃতীয়": 3, "চতুর্থ": 4, "পঞ্চম": 5, "ষষ্ঠ": 6,
    "সপ্তম": 7, "অষ্টম": 8, "নবম": 9, "দশম": 10, "একাদশ": 11, "দ্বাদশ": 12, "ত্রয়োদশ": 13, "ত্রয়োদশ": 13,
    "চতুর্দশ": 14, "পঞ্চদশ": 15, "ষোড়শ": 16, "ষোড়শ": 16, "সপ্তদশ": 17, "অষ্টাদশ": 18, "ঊনবিংশ": 19, "উনবিংশ": 19,
    "বিংশ": 20, "একবিংশ": 21, "দ্বাবিংশ": 22, "ত্রয়োবিংশ": 23, "ত্রয়োবিংশ": 23, "চতুর্বিংশ": 24,
    "পঞ্চবিংশ": 25, "ষড়বিংশ": 26, "ষড়বিংশ": 26, "ষড্বিংশ": 26, "সপ্তবিংশ": 27, "অষ্টাবিংশ": 28, "অষ্টবিংশ": 28, "ঊনত্রিংশ": 29, "উনত্রিংশ": 29,
    "ত্রিংশ": 30, "ত্রিমংশ": 30, "ত্রিশ": 30, "একত্রিংশ": 31, "দ্বাত্রিংশ": 32, "ত্রয়স্ত্রিংশ": 33, "ত্রয়স্ত্রিংশ": 33,
    "চতুস্ত্রিংশ": 34, "পঞ্চত্রিংশ": 35, "ষট্ত্রিংশ": 36, "ষটত্রিংশ": 36, "সপ্তত্রিংশ": 37, "অষ্টাত্রিংশ": 38, "অষ্টাত্ৰিংশ": 38, "ঊনচত্বারিংশ": 39, "উনচত্বারিংশ": 39,
    "চত্বারিংশ": 40, "একচত্বারিংশ": 41, "একত্বারিংশ": 41, "দ্বিচত্বারিংশ": 42, "দ্বাচত্বারিংশ": 42, "ত্রয়শ্চত্বারিংশ": 43, "ত্রয়শ্চত্বারিংশ": 43, "ত্রিচত্বারিংশ": 43, "ত্রিনত্বারিংশ": 43, "চতুশ্চত্বারিংশ": 44, "চাতুশ্চত্বারিংশ": 44,
    "পঞ্চচত্বারিংশ": 45, "পঞ্চত্বারিংশ": 45, "ষট্চত্বারিংশ": 46, "ষটচত্বারিংশ": 46, "সপ্তচত্বারিংশ": 47, "অষ্টাচত্বারিংশ": 48, "ঊনপঞ্চাশ": 49, "উনপঞ্চাশ": 49,
    "পঞ্চাশ": 50, "একপঞ্চাশ": 51, "দ্বিপঞ্চাশ": 52, "দ্বাপঞ্চাশ": 52, "ত্রিপঞ্চাশ": 53, "ত্রয়ঃপঞ্চাশ": 53, "চতুঃপঞ্চাশ": 54, "চতুপঞ্চাশ": 54,
    "পঞ্চপঞ্চাশ": 55, "ষট্পঞ্চাশ": 56, "ষটপঞ্চাশ": 56, "সপ্তপঞ্চাশ": 57, "অষ্টপঞ্চাশ": 58, "অষ্টাপঞ্চাশ": 58, "ঊনষষ্টি": 59, "উনষষ্টি": 59, "উনাষষ্টি": 59,
    "ষষ্টি": 60, "ষষ্ঠি": 60, "ষষ্টিতম": 60, "একষষ্টি": 61, "দ্বিষষ্টি": 62, "ত্রিষষ্টি": 63, "চতুঃষষ্টি": 64, "পঞ্চষষ্টি": 65, "ষট্ষষ্টি": 66, "ষটষষ্টি": 66, "ষট্‌ষষ্টি": 66,
    "সপ্তষষ্টি": 67, "অষ্টষষ্টি": 68, "ঊনসপ্ততি": 69, "উনসপ্ততি": 69, "সপ্ততি": 70, "একসপ্ততি": 71, "দ্বিসপ্ততি": 72, "ত্রিসপ্ততি": 73, "চতুঃসপ্ততি": 74,
    "পঞ্চসপ্ততি": 75, "ষট্সপ্ততি": 76, "ষটসপ্ততি": 76, "সপ্তসপ্ততি": 77, "অষ্টসপ্ততি": 78, "ঊনআশীতি": 79, "উনআশিত": 79, "ঊনআশিত": 79, "ঊনাশীতি": 79,
    "অশীতি": 80, "আশিত": 80, "একাসীতি": 81, "একাশীতি": 81, "দ্ব্যশীতি": 82, "দ্বাশীতি": 82, "ত্রাশিত": 83, "ত্র্যশীতি": 83, "চতুরশীতি": 84,
    "পঞ্চাশীতি": 85, "পঁচাশিত": 85, "ষড়শীতি": 86, "ষড়শীতি": 86, "সপ্তাশীতি": 87, "সপ্তাশীভি": 87, "অষ্টাসীতি": 88, "অষ্টাশীতি": 88, "ঊননবতি": 89, "উননবতি": 89,
    "নবতি": 90,
}

def parse_chapter_num(val) -> int | None:
    if val is None:
        return None
    if isinstance(val, int):
        return val
    s = str(val).strip()
    n = bn2int(s)
    if n is not None:
        return n
    # Check longer words first so "দ্বিপঞ্চাশ" matches before "পঞ্চাশ"
    for w in sorted(BENGALI_ORDINALS.keys(), key=len, reverse=True):
        if w in s:
            return BENGALI_ORDINALS[w]
    return None

# ----------------------------------------------------------------------------------------------------------------
# config
# ----------------------------------------------------------------------------------------------------------------


def load_config() -> dict:
    with open(CONFIG_PATH, encoding="utf-8") as f:
        cfg = json.load(f)
    if os.environ.get("BN_PDF_DIR"):
        cfg["pdf_dir"] = os.environ["BN_PDF_DIR"]
    return cfg


def pdf_path(cfg: dict, vol: str) -> Path:
    return Path(cfg["pdf_dir"]) / cfg["volumes"][vol]["file"]


def vol_pages(cfg: dict, vol: str) -> int:
    return int(cfg["volumes"][vol]["pages"])


def parse_range(s: str | None, hi: int) -> tuple[int, int]:
    if not s:
        return 1, hi
    a, _, b = s.partition("-")
    return int(a), int(b or a)


def bn_digits(n: int | str) -> str:
    return "".join(BN_DIGITS[int(c)] if c.isdigit() else c for c in str(n))


def bn2int(s: str) -> int | None:
    t = "".join(str(BN_DIGITS.index(ch)) if ch in BN_DIGITS else ch for ch in s)
    t = re.sub(r"\D", "", t)
    return int(t) if t else None


def page_key(vol: str, page: int) -> str:
    return f"v{vol}_p{page:04d}"


# ----------------------------------------------------------------------------------------------------------------
# page images
# ----------------------------------------------------------------------------------------------------------------


def _fitz():
    try:
        import pymupdf as fitz  # type: ignore
    except ImportError:
        import fitz  # type: ignore
    return fitz


def prepared_image(cfg: dict, vol: str, page: int) -> Path:
    """Greyscale, contrast-stretched, scaled so the long edge is 1568 px. Uses the scan's own image, not a re-render."""
    out = WORK / "images" / f"{page_key(vol, page)}.png"
    if out.exists():
        return out
    from PIL import Image, ImageOps, ImageFilter

    fitz = _fitz()
    out.parent.mkdir(parents=True, exist_ok=True)
    with fitz.open(pdf_path(cfg, vol)) as doc:
        pg = doc[page - 1]
        img = None
        imgs = pg.get_images(full=True)
        if len(imgs) == 1:
            try:
                info = doc.extract_image(imgs[0][0])
                img = Image.open(io.BytesIO(info["image"]))
            except Exception:
                img = None
        if img is None:  # vector page or several image strips: render
            pix = pg.get_pixmap(dpi=220, colorspace=fitz.csGRAY)
            img = Image.open(io.BytesIO(pix.tobytes("png")))
    img = ImageOps.autocontrast(img.convert("L"), cutoff=1)
    w, h = img.size
    scale = 1568 / max(w, h)
    img = img.resize((max(1, round(w * scale)), max(1, round(h * scale))), Image.LANCZOS)
    if scale > 1.2:
        img = img.filter(ImageFilter.UnsharpMask(radius=1.2, percent=60, threshold=3))
    img.save(out, optimize=True)
    return out


# ----------------------------------------------------------------------------------------------------------------
# vision transcription
# ----------------------------------------------------------------------------------------------------------------

TOOL = {
    "name": "record_page",
    "description": "Record the transcription of one page of the Gita Press Bengali Shrimad Bhagavata Mahapurana.",
    "input_schema": {
        "type": "object",
        "properties": {
            "skandha_heading": {"type": ["string", "null"], "description": "Skandha heading printed on this page, e.g. 'তৃতীয় স্কন্ধ', else null. Not the running header."},
            "chapter_number": {"type": ["integer", "null"], "description": "If a chapter STARTS on this page (a heading like 'প্রথম অধ্যায়' with a title), its number as an integer; else null."},
            "chapter_title": {"type": ["string", "null"], "description": "The Bengali chapter title line under the heading, exactly as printed; else null."},
            "colophon": {"type": ["string", "null"], "description": "A closing line such as 'ইতি শ্রীমদ্ভাগবতে ... সমাপ্ত' or 'শ্রীমদ্ভাগবতমাহাত্ম্যে ... নামক ... অধ্যায় সমাপ্ত', exactly as printed; else null."},
            "sanskrit_verse_numbers": {"type": "array", "items": {"type": "integer"}, "description": "Verse numbers printed at the end of the Sanskrit shlokas on this page (as integers), in order. Empty if none."},
            "paragraphs": {
                "type": "array",
                "description": "The Bengali TRANSLATION text of the page, in reading order, one entry per printed paragraph. Exclude the Sanskrit shlokas, speaker headings of the Sanskrit column, running headers, page numbers and footnotes.",
                "items": {"type": "string"},
            },
            "paragraphs_before_chapter_heading": {"type": "integer", "description": "Only when a chapter heading appears on this page: how many of the first entries in 'paragraphs' come BEFORE that heading (they belong to the chapter that ended on this page). 0 if the heading is at the top."},
            "first_paragraph_continues_previous_page": {"type": "boolean", "description": "True if the first paragraph begins mid-sentence, continuing from the previous page."},
            "footnotes": {"type": "array", "items": {"type": "string"}, "description": "Footnotes at the bottom of the page, each exactly as printed including its marker like (১)."},
            "page_label": {"type": ["string", "null"], "description": "Printed page number, e.g. '1577', else null."},
            "layout_note": {"type": ["string", "null"], "description": "Anything unusual: illustration, table, no translation on this page, damaged print, text you could not separate."},
        },
        "required": ["paragraphs", "sanskrit_verse_numbers", "footnotes", "first_paragraph_continues_previous_page"],
    },
}

PROMPT = """This is one scanned page of the Gita Press Bengali edition of the Shrimad Bhagavata Mahapurana. Pages usually have two columns: the Sanskrit shlokas in Bengali script on the left (with speaker headings such as 'শ্রীশুক উবাচ'), and the Bengali translation on the right. Chapter-opening pages have a heading block across the top. Some pages (the Mahatmya) may be a single column.

Transcribe the BENGALI TRANSLATION faithfully and call record_page.

Rules:
- Faithful transcription only. Copy exactly what is printed: same words, same spelling (including old spellings), same punctuation, same dashes and quotation marks. Do not correct, modernise, complete, summarise or translate anything.
- Each translation unit ends with its verse marker, for example '॥ ১ ॥' or '॥ ৪-৫ ॥'. Keep the marker at the end of its paragraph exactly as printed, in Bengali digits.
- Keep the Bengali translation only. Do not include the Sanskrit shlokas (they end in '।' or '॥ N' with a verse number and are set in the left column), Sanskrit speaker headings, the running header, the page number box such as '[ 1577 ]', or footnotes in paragraphs. Put footnotes in 'footnotes'.
- Reading order: top to bottom of the right column. If the page is single-column prose, top to bottom.
- If a word or character is not clearly legible, write your best reading and put [?] straight after that word. Never guess silently. If a whole line is unreadable, write [?????] there.
- Join words broken across lines with a hyphen only when the printed word is clearly one word; otherwise keep the text as printed. Do not add line breaks inside a paragraph.
- If the page has a chapter heading, give chapter_number and chapter_title. If it has a Skandha heading (e.g. 'তৃতীয় স্কন্ধ' under the book title), give skandha_heading.
- sanskrit_verse_numbers: the numbers at the end of each Sanskrit shloka on this page, as integers, in order (they are only used as a cross-check).
- If a new chapter heading appears in the middle of the page, set paragraphs_before_chapter_heading to the number of translation paragraphs above it (they belong to the previous chapter), and put the previous chapter's closing line in colophon.
- If the first paragraph starts mid-sentence because it continues from the previous page, set first_paragraph_continues_previous_page to true.
"""


def _client():
    try:
        import anthropic
    except ImportError:
        sys.exit("The 'anthropic' package is missing. Run: pip install -r requirements.txt")
    if not os.environ.get("ANTHROPIC_API_KEY"):
        sys.exit("ANTHROPIC_API_KEY is not set. In PowerShell:  $env:ANTHROPIC_API_KEY = 'sk-ant-...'")
    return anthropic.Anthropic(max_retries=6, timeout=180)


def transcribe(client, model: str, image_path: Path) -> dict:
    data = base64.standard_b64encode(image_path.read_bytes()).decode()
    last_err = None
    for max_tokens in (4096, 8192):
        for attempt in range(4):
            try:
                resp = client.messages.create(
                    model=model, max_tokens=max_tokens, temperature=0,
                    tools=[TOOL], tool_choice={"type": "tool", "name": "record_page"},
                    messages=[{"role": "user", "content": [
                        {"type": "image", "source": {"type": "base64", "media_type": "image/png", "data": data}},
                        {"type": "text", "text": PROMPT},
                    ]}],
                )
                for block in resp.content:
                    if getattr(block, "type", "") == "tool_use":
                        out = dict(block.input)
                        out["_usage"] = {"in": resp.usage.input_tokens, "out": resp.usage.output_tokens}
                        out["_stop"] = resp.stop_reason
                        if resp.stop_reason == "max_tokens":
                            raise RuntimeError("truncated")
                        return out
                raise RuntimeError("no tool call in reply")
            except Exception as e:  # noqa: BLE001
                last_err = e
                if str(e) == "truncated":
                    break  # go to the larger token limit
                time.sleep(2 + 4 * attempt)
    raise RuntimeError(f"giving up: {last_err}")


def cmd_run(args) -> None:
    cfg = load_config()
    vols = [args.vol] if args.vol else list(cfg["volumes"])
    todo = []
    for v in vols:
        a, b = parse_range(args.pages, vol_pages(cfg, v))
        for p in range(a, min(b, vol_pages(cfg, v)) + 1):
            if not args.force and (WORK / "vision" / f"{page_key(v, p)}.json").exists():
                continue
            if any(v == str(v0) and a0 <= p <= b0 for v0, a0, b0 in cfg.get("skip_pages", [])):
                continue
            todo.append((v, p))
    if args.limit:
        todo = todo[: args.limit]
    print(f"{len(todo)} page(s) to transcribe with {args.model}")
    if not todo:
        return
    client = _client()
    (WORK / "vision").mkdir(parents=True, exist_ok=True)
    lock = threading.Lock()
    done = 0
    tin = tout = 0
    errors = []

    def job(v: str, p: int):
        img = prepared_image(cfg, v, p)
        res = transcribe(client, args.model, img)
        res["_page"] = {"vol": v, "page": p, "model": args.model}
        tmp = WORK / "vision" / f"{page_key(v, p)}.json.tmp"
        tmp.write_text(json.dumps(res, ensure_ascii=False, indent=1), encoding="utf-8")
        tmp.replace(WORK / "vision" / f"{page_key(v, p)}.json")
        return res

    with ThreadPoolExecutor(max_workers=args.workers) as ex:
        futs = {ex.submit(job, v, p): (v, p) for v, p in todo}
        for fut in as_completed(futs):
            v, p = futs[fut]
            try:
                r = fut.result()
                with lock:
                    done += 1
                    tin += r["_usage"]["in"]
                    tout += r["_usage"]["out"]
                    print(f"[{done}/{len(todo)}] v{v} p{p}  paragraphs={len(r.get('paragraphs', []))}  tokens in/out={r['_usage']['in']}/{r['_usage']['out']}", flush=True)
            except Exception as e:  # noqa: BLE001
                errors.append((v, p, str(e)))
                print(f"[FAILED] v{v} p{p}: {e}", flush=True)
                with open(WORK / "errors.log", "a", encoding="utf-8") as f:
                    f.write(f"v{v} p{p}: {e}\n")
    print(f"Done {done}, failed {len(errors)}. Tokens in {tin:,} / out {tout:,}. Re-run the same command to retry failures.")


def cmd_estimate(args) -> None:
    cfg = load_config()
    vols = [args.vol] if args.vol else list(cfg["volumes"])
    n = 0
    for v in vols:
        a, b = parse_range(args.pages, vol_pages(cfg, v))
        for p in range(a, min(b, vol_pages(cfg, v)) + 1):
            if not (WORK / "vision" / f"{page_key(v, p)}.json").exists():
                n += 1
    print(f"{n} pages left. Per page roughly 2,300 image tokens + ~500 prompt tokens in, ~900-1,300 tokens out.")
    print(f"About {n * 2800 / 1e6:.1f}M input and {n * 1100 / 1e6:.1f}M output tokens. Multiply by your model's current per-million prices.")
    print("Tip: first run  python bn_extract.py run --vol 1 --pages 228-240  (13 pages) and look at the result before the full run.")


# ----------------------------------------------------------------------------------------------------------------
# agreement with OCR
# ----------------------------------------------------------------------------------------------------------------

_BN_KEEP = re.compile(r"[ঀ-৿]")
_STRIP = re.compile(r"[\s‌‍।॥০-৯।॥\[\]\?\(\)\-–—,;:.!'\"‘’“”]")


def norm(s: str) -> str:
    s = s.replace("[?????]", "").replace("[?]", "")
    s = _STRIP.sub("", s)
    return "".join(ch for ch in s if _BN_KEEP.match(ch))


def ngrams(s: str, n: int = 8) -> set[str]:
    return {s[i: i + n] for i in range(max(0, len(s) - n + 1))}


_ocr_cache: dict[str, str] = {}


def ocr_text(vol: str, page: int) -> str | None:
    key = page_key(vol, page)
    if key in _ocr_cache:
        return _ocr_cache[key]
    for d in (WORK / "ocr", DATA / "ocr"):
        f = d / f"{key}.txt"
        if f.exists():
            t = f.read_text(encoding="utf-8", errors="replace")
            _ocr_cache[key] = t
            return t
    return None


def agreement(text: str, sources: list[tuple[str, int]]) -> float | None:
    """Share of the verse's 8-character n-grams that also occur in the OCR text of its page(s). Real ~0.6-0.8, paraphrase <0.35."""
    mine = norm(text)
    if len(mine) < 14:
        return None
    pool: set[str] = set()
    have = False
    for v, p in sources:
        t = ocr_text(v, p)
        if t is None:
            continue
        have = True
        pool |= ngrams(norm(t))
    if not have:
        return None
    g = ngrams(mine)
    return len(g & pool) / max(1, len(g))


# ----------------------------------------------------------------------------------------------------------------
# assemble
# ----------------------------------------------------------------------------------------------------------------

MARKER = re.compile(r"(?:[॥।।৷|।?!]+)['’\"”]?\s*([০-৯0-9]+)\s*(?:[-–—~]\s*([০-৯0-9]+))?\s*(?:[॥।।৷|।?!]+)?\s*$")
ANY_MARKER = re.compile(r"(?:[॥।।৷|।?!]+)['’\"”]?\s*([০-৯0-9]+)\s*(?:[-–—~]\s*([০-৯0-9]+))?\s*(?:[॥।।৷|।?!]+)?")


def load_pages(cfg: dict) -> list[dict]:
    pages = []
    for f in sorted((WORK / "vision").glob("v*_p*.json")):
        d = json.loads(f.read_text(encoding="utf-8"))
        info = d.get("_page") or {}
        if not info:
            m = re.match(r"v(\d+)_p(\d+)", f.stem)
            info = {"vol": m.group(1), "page": int(m.group(2))}
        d["_vol"], d["_pg"] = str(info["vol"]), int(info["page"])
        pages.append(d)
    pages.sort(key=lambda d: (d["_vol"], d["_pg"]))
    return pages


def section_for(cfg: dict, vol: str, page: int):
    for name, sec in cfg["mahatmya"].items():
        if sec["vol"] == vol and sec["from"] <= page <= sec["to"]:
            return name, sec
    return None, None


class Chapter:
    def __init__(self, section: str, skandha: int, number: int, title: str, vol: str, page: int):
        self.section, self.skandha, self.number, self.title = section, skandha, number, title
        self.pages: list[tuple[str, int]] = [(vol, page)]
        self.units: list[dict] = []
        self.pending: list[str] = []
        self.pending_src: list[tuple[str, int]] = []
        self.pending_flags: list[str] = []
        self.footnotes: list[tuple[int, str]] = []
        self.colophon = ""
        self.sa_numbers: set[int] = set()
        self.notes: list[str] = []
        self.leftover: str = ""

    @property
    def key(self) -> str:
        return f"{self.section}.{self.number}"


def build_chapters(cfg: dict, pages: list[dict]) -> tuple[list[Chapter], list[str]]:
    chapters: list[Chapter] = []
    unassigned: list[str] = []
    cur: Chapter | None = None
    skandha = 0

    def feed(ch: Chapter, v: str, p: int, paras: list[str], continues: bool) -> None:
        """Cut the page text into units at every verse marker (a marker can sit in the middle of a printed paragraph)."""
        first = True
        for para in paras:
            para = para.strip()
            if not para:
                continue
            if first and continues and ch.pending:
                ch.pending_flags.append("continued across a page")
                glue = " "
            else:
                glue = None
            first = False
            pos = 0
            for m in ANY_MARKER.finditer(para):
                seg = para[pos:m.end()].strip()
                pos = m.end()
                if glue is not None and ch.pending:
                    ch.pending[-1] = ch.pending[-1] + glue + seg
                else:
                    ch.pending.append(seg)
                glue = None
                ch.pending_src.append((v, p))
                a = bn2int(m.group(1))
                b = bn2int(m.group(2)) if m.group(2) else a
                ch.units.append({"a": a, "b": b, "text": "\n\n".join(ch.pending), "src": sorted(set(ch.pending_src)), "flags": list(ch.pending_flags)})
                ch.pending, ch.pending_src, ch.pending_flags = [], [], []
            rest = para[pos:].strip()
            if rest:
                if glue is not None and ch.pending:
                    ch.pending[-1] = ch.pending[-1] + glue + rest
                else:
                    ch.pending.append(rest)
                ch.pending_src.append((v, p))

    for pg in pages:
        v, p = pg["_vol"], pg["_pg"]
        sec_name, _sec = section_for(cfg, v, p)
        paras = [x for x in pg.get("paragraphs", [])]
        before = 0
        matched_sk = None
        for v0, a0, b0, s0 in cfg.get("skandha_pages", []):
            if str(v0) == v and a0 <= p <= b0 and not sec_name:
                matched_sk = int(s0)
        if matched_sk is not None:
            skandha = matched_sk
        elif pg.get("skandha_heading") and not sec_name:
            for w, n in SKANDHA_WORDS.items():
                if w in pg["skandha_heading"]:
                    skandha = n
                    break
        ch_num = parse_chapter_num(pg.get("chapter_number"))
        if ch_num is None and pg.get("skandha_heading"):
            ch_num = parse_chapter_num(pg.get("skandha_heading"))
        has_title = bool(pg.get("chapter_title") and pg.get("chapter_title").strip())
        is_new = False
        
        if ch_num is not None:
            if cur is None:
                is_new = True
            elif sec_name and cur.section != sec_name:
                is_new = True
            elif not sec_name and cur.section != f"skandha-{skandha:02d}":
                is_new = True
            elif ch_num == cur.number + 1:
                is_new = True
            elif ch_num > cur.number and (has_title or pg.get("paragraphs_before_chapter_heading")):
                is_new = True

        if is_new:
            before = max(0, int(pg.get("paragraphs_before_chapter_heading") or 0))
            if cur is not None:
                if before:
                    feed(cur, v, p, paras[:before], bool(pg.get("first_paragraph_continues_previous_page")))
                    paras = paras[before:]
                if pg.get("colophon"):
                    cur.colophon = pg["colophon"].strip()
                if cur.pending:
                    cur.leftover = " ".join(cur.pending)
                if (v, p) not in cur.pages:
                    cur.pages.append((v, p))
            section, sk = (sec_name, 0) if sec_name else (f"skandha-{skandha:02d}", skandha)
            cur = Chapter(section, sk, ch_num, (pg.get("chapter_title") or "").strip(), v, p)
            chapters.append(cur)
            continues = False
        else:
            continues = bool(pg.get("first_paragraph_continues_previous_page"))
            if cur is None:
                unassigned.append(f"v{v} p{p}")
                continue
            if (v, p) not in cur.pages:
                cur.pages.append((v, p))
            if pg.get("colophon"):
                cur.colophon = pg["colophon"].strip()
        if pg.get("layout_note"):
            cur.notes.append(f"v{v} p{p}: {pg['layout_note']}")
        cur.sa_numbers.update(int(n) for n in pg.get("sanskrit_verse_numbers", []) if isinstance(n, int))
        for fn in pg.get("footnotes", []):
            cur.footnotes.append((p, fn.strip()))
        feed(cur, v, p, paras, continues)
    if cur is not None and cur.pending:
        cur.leftover = " ".join(cur.pending)
    return chapters, unassigned


def check_chapter(ch: Chapter, expected: dict) -> list[str]:
    problems = []
    exp = None
    if ch.section.startswith("skandha-"):
        exp = expected["verses"].get(f"{ch.skandha}.{ch.number}")
    elif ch.section == "mahatmya":
        exp = expected["verses"].get(f"0.{ch.number}")
    seen: list[int] = []
    prev_end = 0
    for u in ch.units:
        if u["a"] is None or u["b"] is None or u["b"] < u["a"]:
            u["flags"].append("bad verse marker")
            continue
        if u["a"] <= prev_end:
            u["flags"].append(f"verse number not increasing (after {prev_end})")
        elif u["a"] != prev_end + 1 and prev_end:
            u["flags"].append(f"gap: {prev_end + 1}-{u['a'] - 1} missing before this")
        prev_end = max(prev_end, u["b"])
        seen.extend(range(u["a"], u["b"] + 1))
    if exp:
        missing = sorted(set(exp) - set(seen))
        extra = sorted(set(seen) - set(exp))
        if missing:
            problems.append(f"verses with no translation unit: {compact(missing)}")
        if extra:
            problems.append(f"translation verse numbers not in the Hindi edition: {compact(extra)}")
    elif ch.section.startswith("skandha-") or ch.section == "mahatmya":
        problems.append("no Hindi reference for this chapter")
    if ch.sa_numbers and seen:
        only_sa = sorted(ch.sa_numbers - set(seen))
        if only_sa:
            problems.append(f"Sanskrit column has verse numbers with no translation unit: {compact(only_sa)}")
    if ch.leftover:
        problems.append("text after the last verse marker was not closed by a marker: " + ch.leftover[:80])
    return problems


def compact(nums: list[int]) -> str:
    out, i = [], 0
    while i < len(nums):
        j = i
        while j + 1 < len(nums) and nums[j + 1] == nums[j] + 1:
            j += 1
        out.append(str(nums[i]) if i == j else f"{nums[i]}-{nums[j]}")
        i = j + 1
    return ", ".join(out)


def chapter_md(cfg: dict, ch: Chapter, status: str) -> str:
    srcs = sorted({f"v{v}:{p}" for v, p in ch.pages})
    pdfs = sorted({cfg["volumes"][v]["file"] for v, _ in ch.pages})
    head = ["---", f"skandha: {ch.skandha}", f"chapter: {ch.number}", f'title: "{ch.title.replace(chr(34), chr(39))}"', "language: bn",
            f"section: {ch.section}", f'source_pdf: "{"; ".join(pdfs)}"', "source_pages: [" + ", ".join(f'"{s}"' for s in srcs) + "]",
            'extraction_method: "vision transcription + OCR cross-check"', f'verification_status: "{status}"', "---", ""]
    if ch.section.startswith("skandha-"):
        head.append(f"# {SKANDHA_NAMES[ch.skandha]} স্কন্ধ")
    else:
        head.append("# মাহাত্ম্য")
    head += ["", f"## অধ্যায় {bn_digits(ch.number)}" + (f" - {ch.title}" if ch.title else ""), ""]
    body: list[str] = []
    for u in ch.units:
        label = bn_digits(u["a"]) if u["a"] == u["b"] else f"{bn_digits(u['a'])}–{bn_digits(u['b'])}"
        body.append(f"### শ্লোক {label}")
        meta = f"page: {u['src'][0][1]}"
        if u.get("score") is not None:
            meta += f" | ocr-agreement: {u['score']:.2f}"
        if u["flags"]:
            meta += " | FLAGS: " + "; ".join(u["flags"])
        body.append(f"<!-- {meta} -->")
        body.append("")
        body.append(u["text"])
        body.append("")
    if ch.colophon:
        body += ["---", "", ch.colophon, ""]
    if ch.footnotes:
        body += ["### টীকা", ""]
        body += [f"- (পৃষ্ঠা {bn_digits(p)}) {t}" for p, t in ch.footnotes]
        body.append("")
    return "\n".join(head + body)


def cmd_assemble(args) -> None:
    cfg = load_config()
    expected = json.loads((DATA / "expected_verses.json").read_text(encoding="utf-8"))
    pages = load_pages(cfg)
    if not pages:
        sys.exit("No pages transcribed yet (work/vision is empty). Run: python bn_extract.py run --vol 1 --pages 228-240")
    chapters, unassigned = build_chapters(cfg, pages)
    low, mid = cfg["thresholds"]["low"], cfg["thresholds"]["check"]
    REVIEW.mkdir(exist_ok=True)
    flagged_rows = []
    chapter_rows = []
    uncertain_total = 0
    for ch in chapters:
        problems = check_chapter(ch, expected)
        for u in ch.units:
            u["score"] = agreement(u["text"], u["src"])
            q = u["text"].count("[?")
            uncertain_total += q
            if q:
                u["flags"].append(f"{q} uncertain reading(s) marked [?]")
            if u["score"] is None:
                u["flags"].append("no OCR text to cross-check")
            elif u["score"] < low:
                u["flags"].append(f"LOW agreement with OCR ({u['score']:.2f})")
            elif u["score"] < mid:
                u["flags"].append(f"check: moderate agreement with OCR ({u['score']:.2f})")
            if re.search(r"[A-Za-z]", re.sub(r"\[\?+\]", "", u["text"])):
                u["flags"].append("Latin letters in text")
            if not MARKER.search(u["text"].strip().split("\n\n")[-1]):
                u["flags"].append("does not end with a verse marker")
        nflags = sum(1 for u in ch.units if u["flags"] and any(not f.startswith("continued") for f in u["flags"]))
        status = "machine_transcribed_clean" if not problems and nflags == 0 else "needs_review"
        sub = OUT / ch.section
        sub.mkdir(parents=True, exist_ok=True)
        target = sub / f"chapter-{ch.number:02d}.md"
        if target.exists() and 'verification_status: "reviewed"' in target.read_text(encoding="utf-8"):
            print(f"kept your reviewed file {target.name} in {ch.section}")
            continue
        target.write_text(chapter_md(cfg, ch, status), encoding="utf-8")
        for u in ch.units:
            real = [f for f in u["flags"] if not f.startswith("continued")]
            if real:
                flagged_rows.append({"chapter": ch.key, "verses": f"{u['a']}-{u['b']}" if u["a"] != u["b"] else str(u["a"]),
                                     "page": f"v{u['src'][0][0]} p{u['src'][0][1]}", "score": "" if u["score"] is None else f"{u['score']:.2f}",
                                     "flags": "; ".join(real), "text": u["text"].replace("\n", " ")[:300],
                                     "_img": page_key(*u["src"][0]), "_file": f"{ch.section}/chapter-{ch.number:02d}.md"})
        chapter_rows.append((ch, problems, nflags, status))

    # skandha-level continuity
    seen = defaultdict(set)
    for ch in chapters:
        seen[ch.section].add(ch.number)
    gaps = []
    for sec, nums in seen.items():
        top = max(nums)
        if sec.startswith("skandha-"):
            top = min(top, expected["chapters_per_skandha"][str(int(sec.split("-")[1]))])
        miss = sorted(set(range(1, top + 1)) - nums)
        if miss and len(nums) > 1:
            gaps.append(f"{sec}: chapter(s) not found below {top}: {compact(miss)}")

    with open(REVIEW / "flagged.csv", "w", newline="", encoding="utf-8-sig") as f:
        w = csv.writer(f)
        w.writerow(["chapter", "verses", "page", "ocr_agreement", "flags", "text_start", "file", "fixed(y)"])
        for r in flagged_rows:
            w.writerow([r["chapter"], r["verses"], r["page"], r["score"], r["flags"], r["text"], r["_file"], ""])
    write_review_html(flagged_rows, chapter_rows)
    lines = ["# Bengali extraction report", "",
             f"- pages transcribed: {len(pages)}", f"- chapters assembled: {len(chapters)}",
             f"- translation units: {sum(len(c.units) for c in chapters)}", f"- units flagged: {len(flagged_rows)}",
             f"- words marked uncertain [?]: {uncertain_total}", ""]
    if gaps:
        lines += ["## Chapters missing in the pages done so far", ""] + [f"- {g}" for g in gaps] + [""]
    if unassigned:
        lines += ["## Pages before the first chapter heading (skipped)", "", ", ".join(unassigned[:60]), ""]
    lines += ["## Chapters", "", "| chapter | verses | flagged | status | problems |", "|---|---|---|---|---|"]
    for ch, problems, nflags, status in chapter_rows:
        lines.append(f"| {ch.key} | {len(ch.units)} | {nflags} | {status} | {'; '.join(problems) or ''} |")
    (REVIEW / "report.md").write_text("\n".join(lines), encoding="utf-8")
    print("\n".join(lines[:12]))
    print(f"\nMarkdown: {OUT}\nReport:   {REVIEW / 'report.md'}\nReview:   {REVIEW / 'index.html'}  (open in a browser)\nCSV:      {REVIEW / 'flagged.csv'}")


def write_review_html(rows: list[dict], chapter_rows) -> None:
    img_rel = os.path.relpath(WORK / "images", REVIEW).replace("\\", "/")
    out = ["<!doctype html><meta charset=utf-8><title>Bengali review</title>",
           "<style>body{font:16px/1.6 system-ui,sans-serif;max-width:1100px;margin:24px auto;padding:0 16px;color:#1c1a17}"
           "details{border:1px solid #ddd;border-radius:10px;margin:10px 0;padding:8px 14px}summary{cursor:pointer;font-weight:600}"
           ".bn{font-size:19px;line-height:1.9;background:#faf7f0;padding:10px 14px;border-radius:8px}.flag{color:#8a3b0f}"
           "img{max-width:100%;border:1px solid #ccc;margin-top:10px}code{background:#eee;padding:1px 5px;border-radius:4px}</style>",
           f"<h1>Needs a look: {len(rows)} verse(s)</h1>",
           "<p>Fix the text in the <code>out/…/chapter-NN.md</code> file named in each row, comparing with the page image. "
           "Delete the <code>[?]</code> and the FLAGS note once checked.</p>"]
    for r in rows:
        out.append(f"<details><summary>{html.escape(r['chapter'])} verse {html.escape(r['verses'])} &middot; {html.escape(r['page'])} "
                   f"&middot; <span class=flag>{html.escape(r['flags'])}</span></summary>"
                   f"<p>File: <code>{html.escape(r['_file'])}</code></p><div class=bn>{html.escape(r['text'])}</div>"
                   f"<img loading=lazy src='{img_rel}/{r['_img']}.png'></details>")
    (REVIEW / "index.html").write_text("\n".join(out), encoding="utf-8")


# ----------------------------------------------------------------------------------------------------------------
# status / check
# ----------------------------------------------------------------------------------------------------------------


def cmd_status(_args) -> None:
    cfg = load_config()
    for v in cfg["volumes"]:
        n = len(list((WORK / "vision").glob(f"v{v}_p*.json"))) if (WORK / "vision").exists() else 0
        print(f"volume {v}: {n} / {vol_pages(cfg, v)} pages transcribed")
    if (WORK / "errors.log").exists():
        print("errors.log has entries; re-run 'run' to retry failed pages.")


def cmd_check(_args) -> None:
    cfg = load_config()
    ok = True
    for mod in ("fitz", "PIL", "anthropic"):
        try:
            if mod == "fitz":
                _fitz()
            else:
                __import__(mod)
            print(f"[ok] {mod}")
        except ImportError:
            print(f"[MISSING] {mod}  ->  pip install -r requirements.txt")
            ok = False
    for v in cfg["volumes"]:
        p = pdf_path(cfg, v)
        print(f"[{'ok' if p.exists() else 'MISSING'}] PDF volume {v}: {p}")
        ok &= p.exists()
    print(f"[{'ok' if os.environ.get('ANTHROPIC_API_KEY') else 'MISSING'}] ANTHROPIC_API_KEY (needed for 'run')")
    n = len(list((DATA / "ocr").glob("*.txt")))
    print(f"[{'ok' if n else 'MISSING'}] bundled OCR text for the cross-check: {n} pages")
    print(f"model: {DEFAULT_MODEL}")
    print("All good." if ok else "Fix the items above first.")


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    sub.add_parser("check").set_defaults(fn=cmd_check)
    sub.add_parser("status").set_defaults(fn=cmd_status)
    sub.add_parser("assemble").set_defaults(fn=cmd_assemble)
    for name, fn in (("estimate", cmd_estimate), ("run", cmd_run)):
        p = sub.add_parser(name)
        p.add_argument("--vol", choices=["1", "2"])
        p.add_argument("--pages", help="page range in the PDF, e.g. 228-240")
        if name == "run":
            p.add_argument("--workers", type=int, default=4)
            p.add_argument("--model", default=DEFAULT_MODEL)
            p.add_argument("--limit", type=int)
            p.add_argument("--force", action="store_true", help="redo pages that already have a result")
        p.set_defaults(fn=fn)
    args = ap.parse_args()
    args.fn(args)


if __name__ == "__main__":
    main()
