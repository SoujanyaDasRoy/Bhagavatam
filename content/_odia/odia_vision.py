#!/usr/bin/env python3
"""Vision-model transcription of the Gita Press Odia Bhagavatam (the same method as _bengali/bn_extract).

Writes one JSON per page to work_v/vision/vN_pNNNN.json in the schema odia_assemble.py reads, so afterwards:
    $env:ODIA_WORK = "work_v"; python odia_assemble.py        (writes out/ and review/)

Setup (PowerShell, in this folder):
    pip install pymupdf pillow anthropic
    $env:ANTHROPIC_API_KEY = "sk-ant-..."
    $env:ODIA_PDF_1 = "C:\\path\\Srimad-Bhagavata-Mahapurana-Gita-Press-Odia-Part-1.pdf"
    $env:ODIA_PDF_2 = "C:\\path\\Srimad-Bhagavata-Mahapurana-Gita-Press-Odia-Part-2.pdf"
Run:
    python odia_vision.py run --vol 1 --pages 87-93            # pilot, 7 pages
    python odia_vision.py estimate
    python odia_vision.py run --vol 1 --workers 4
    python odia_vision.py run --vol 2 --workers 4
Restartable: a page is done when its JSON exists. Failures go to work_v/errors.log; re-run to retry.
--model claude-opus-5-5 --force re-reads hard pages with a stronger model.
"""
import argparse, base64, io, json, os, re, sys, threading, time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

HERE = Path(__file__).resolve().parent
WORK = Path(os.environ.get("ODIA_VWORK", HERE / "work_v"))
CFG = json.loads((HERE / "config.json").read_text(encoding="utf-8"))
DEFAULT_MODEL = os.environ.get("ODIA_MODEL", "claude-sonnet-5-5")
LONG_EDGE = 2000
OD = "୦୧୨୩୪୫୬୭୮୯"
sys.stdout.reconfigure(encoding="utf-8")

TOOL = {
    "name": "record_page",
    "description": "Record the transcription of one page of the Gita Press Odia Shrimad Bhagavata Mahapurana.",
    "input_schema": {
        "type": "object",
        "properties": {
            "running_head_chapter": {"type": ["integer", "null"], "description": "Chapter number in the running header at the top ('ଅଧ୍ୟାୟ ୩'), as an integer; null if not readable."},
            "running_head_skandha": {"type": ["integer", "null"], "description": "Skandha number named in the running header (e.g. 'ପ୍ରଥମ ସ୍କନ୍ଧ' = 1), as an integer; null if the header has no skandha name."},
            "skandha_heading": {"type": ["string", "null"], "description": "A Skandha heading printed as a body heading on this page (not the running header), else null."},
            "chapter_number": {"type": ["integer", "null"], "description": "If a chapter STARTS on this page (boxed heading such as 'ଅଥ ତୃତୀୟୋଽଧ୍ୟାୟଃ'), its number as an integer; else null."},
            "chapter_title": {"type": ["string", "null"], "description": "The Odia subtitle printed under the boxed chapter heading, exactly as printed; else null."},
            "colophon": {"type": ["string", "null"], "description": "The closing Sanskrit line 'ଇତି ଶ୍ରୀମଦ୍ଭାଗବତେ ... ॥ N ॥' of a chapter that ENDS on this page, exactly as printed (both lines); else null."},
            "sanskrit_verse_numbers": {"type": "array", "items": {"type": "integer"}, "description": "Verse numbers printed at the end of the Sanskrit shlokas in the left column, as integers, in order. Empty if none."},
            "paragraphs": {"type": "array", "items": {"type": "string"}, "description": "The ODIA TRANSLATION text of the page in reading order, one entry per printed paragraph. Exclude the Sanskrit shlokas, Sanskrit speaker headings, running header, page number and footnotes."},
            "paragraphs_before_chapter_heading": {"type": "integer", "description": "Only when a new chapter starts on this page: how many of the first entries of 'paragraphs' come BEFORE the heading (they belong to the chapter that ended). 0 if the heading is at the top."},
            "first_paragraph_continues_previous_page": {"type": "boolean", "description": "True if the first paragraph begins mid-sentence, continuing from the previous page."},
            "footnotes": {"type": "array", "items": {"type": "string"}, "description": "Footnotes at the bottom, each exactly as printed including its marker like (୧)."},
            "page_label": {"type": ["string", "null"], "description": "Printed page number, else null."},
            "layout_note": {"type": ["string", "null"], "description": "Anything unusual: illustration, table, no translation on the page, damaged print."},
        },
        "required": ["paragraphs", "sanskrit_verse_numbers", "footnotes", "first_paragraph_continues_previous_page"],
    },
}

PROMPT = """This is one scanned page of the Gita Press Odia edition of the Shrimad Bhagavata Mahapurana (title in the running header: ଶ୍ରୀମଦ୍ଭାଗବତ). Body pages have two columns: the Sanskrit shlokas in Odia script on the left (with speaker headings such as 'ଶ୍ରୀଶୁକ ଉବାଚ'), and the Odia translation on the right, with a thin vertical rule between them. Chapter-opening pages have a boxed heading ('ଅଥ ପ୍ରଥମୋଽଧ୍ୟାୟଃ') with an Odia subtitle below it; the previous chapter's closing line ('ଇତି ଶ୍ରୀମଦ୍ଭାଗବତେ ମହାପୁରାଣେ ... ॥ N ॥') sits just above it. Some pages (the Mahatmya, or commentary below the verses) are single column. Footnotes sit at the page bottom.

Transcribe the ODIA TRANSLATION faithfully and call record_page.

Rules:
- Faithful transcription only. Copy exactly what is printed: same words, same spelling, same punctuation, dashes, quotation marks and conjuncts. Do not correct, modernise, complete, summarise or translate anything.
- Each translation unit ends with its verse marker, e.g. '॥ ୨୧ ॥' or '॥ ୪-୫ ॥'. Keep the marker at the end of its paragraph exactly as printed, in Odia digits (୦୧୨୩୪୫୬୭୮୯).
- Keep the Odia translation only. Do not include the Sanskrit shlokas (left column, ending '॥ N ॥'), Sanskrit speaker headings, the running header, the page number, or footnotes in the paragraphs; put footnotes in 'footnotes'.
- Reading order: top to bottom of the right column; if the page is single-column prose, top to bottom.
- If a word or character is not clearly legible, write your best reading and put [?] straight after that word. Never guess silently. If a whole line is unreadable write [?????].
- Do not add line breaks inside a paragraph. Join a word split across two lines.
- running_head_chapter / running_head_skandha: read them from the running header at the top of the page (odd pages name the skandha; even pages show only the chapter).
- If a chapter heading box is on the page give chapter_number and chapter_title. If a new chapter starts mid-page set paragraphs_before_chapter_heading to the number of translation paragraphs above the heading and put the closing line in colophon.
- sanskrit_verse_numbers: the numbers at the end of each Sanskrit shloka on this page, as integers (cross-check only).
- If the first paragraph starts mid-sentence because it continues from the previous page, set first_paragraph_continues_previous_page to true.
"""


def pdf_path(vol):
    p = os.environ.get(f"ODIA_PDF_{vol}") or CFG["volumes"][str(vol)]["path"]
    return p


def vol_pages(vol):
    return CFG["volumes"][str(vol)]["pages"]


def _fitz():
    try:
        import pymupdf as fitz
    except ImportError:
        import fitz
    return fitz


def page_image(vol, page):
    out = WORK / "images" / f"v{vol}_p{page:04d}.png"
    if out.exists():
        return out
    from PIL import Image, ImageOps
    out.parent.mkdir(parents=True, exist_ok=True)
    fitz = _fitz()
    with fitz.open(pdf_path(vol)) as doc:
        pg = doc[page - 1]
        img = None
        imgs = pg.get_images(full=True)
        if len(imgs) == 1:
            try:
                img = Image.open(io.BytesIO(doc.extract_image(imgs[0][0])["image"]))
            except Exception:
                img = None
        if img is None:
            pix = pg.get_pixmap(dpi=250, colorspace=fitz.csGRAY)
            img = Image.open(io.BytesIO(pix.tobytes("png")))
    img = ImageOps.autocontrast(img.convert("L"), cutoff=1)
    w, h = img.size
    s = LONG_EDGE / max(w, h)
    img = img.resize((max(1, round(w * s)), max(1, round(h * s))), Image.LANCZOS)
    img.save(out, optimize=True)
    return out


def client():
    try:
        import anthropic
    except ImportError:
        sys.exit("pip install pymupdf pillow anthropic")
    if not os.environ.get("ANTHROPIC_API_KEY"):
        sys.exit("ANTHROPIC_API_KEY is not set.")
    return anthropic.Anthropic(max_retries=6, timeout=180)


def transcribe(cl, model, img_path):
    data = base64.standard_b64encode(img_path.read_bytes()).decode()
    last = None
    for max_tokens in (4096, 8192):
        for attempt in range(4):
            try:
                r = cl.messages.create(
                    model=model, max_tokens=max_tokens, temperature=0,
                    tools=[TOOL], tool_choice={"type": "tool", "name": "record_page"},
                    messages=[{"role": "user", "content": [
                        {"type": "image", "source": {"type": "base64", "media_type": "image/png", "data": data}},
                        {"type": "text", "text": PROMPT}]}])
                for b in r.content:
                    if getattr(b, "type", "") == "tool_use":
                        if r.stop_reason == "max_tokens":
                            raise RuntimeError("truncated")
                        out = dict(b.input)
                        out["_usage"] = {"in": r.usage.input_tokens, "out": r.usage.output_tokens}
                        return out
                raise RuntimeError("no tool call")
            except Exception as e:  # noqa
                last = e
                if str(e) == "truncated":
                    break
                time.sleep(2 + 4 * attempt)
    raise RuntimeError(f"giving up: {last}")


def normalise(res, vol, page, model):
    """Fill the fields odia_assemble.py expects."""
    res.setdefault("paragraphs", []); res.setdefault("footnotes", []); res.setdefault("sanskrit_verse_numbers", [])
    res["paragraphs"] = [re.sub(r"\s+", " ", p).strip() for p in res["paragraphs"] if p and p.strip()]
    res["sanskrit_verse_numbers"] = [n for n in res["sanskrit_verse_numbers"] if isinstance(n, int)]
    res["chapter_title"] = (res.get("chapter_title") or "").strip()
    if res.get("chapter_number") or res["chapter_title"]:
        res["chapter_marker_text"] = "boxed heading"
        res.setdefault("paragraphs_before_chapter_heading", 0)
    res["paragraphs_before_chapter_heading"] = int(res.get("paragraphs_before_chapter_heading") or 0)
    res["running_head_chapter"] = res.get("running_head_chapter") if isinstance(res.get("running_head_chapter"), int) else None
    res["running_head_skandha"] = res.get("running_head_skandha") if isinstance(res.get("running_head_skandha"), int) else None
    res["_page"] = {"vol": str(vol), "page": page, "model": model}
    return res


def parse_range(s, hi):
    if not s:
        return 1, hi
    a, _, b = s.partition("-")
    return int(a), int(b or a)


def cmd_run(a):
    vols = [a.vol] if a.vol else [1, 2]
    todo = []
    for v in vols:
        lo, hi = parse_range(a.pages, vol_pages(v))
        for p in range(lo, min(hi, vol_pages(v)) + 1):
            if any(str(sv) == str(v) and x <= p <= y for sv, x, y in CFG.get("skip_pages", [])):
                continue
            f = WORK / "vision" / f"v{v}_p{p:04d}.json"
            if a.force or not f.exists():
                todo.append((v, p))
    if a.limit:
        todo = todo[:a.limit]
    print(f"{len(todo)} page(s) with {a.model}", flush=True)
    if not todo:
        return
    (WORK / "vision").mkdir(parents=True, exist_ok=True)
    cl = client(); lock = threading.Lock(); done = [0]

    def job(v, p):
        img = page_image(v, p)
        res = normalise(transcribe(cl, a.model, img), v, p, a.model)
        (WORK / "vision" / f"v{v}_p{p:04d}.json").write_text(json.dumps(res, ensure_ascii=False, indent=1), encoding="utf-8")
        return v, p

    with ThreadPoolExecutor(a.workers) as ex:
        futs = {ex.submit(job, v, p): (v, p) for v, p in todo}
        for f in as_completed(futs):
            v, p = futs[f]
            try:
                f.result(); status = "ok"
            except Exception as e:  # noqa
                status = f"FAILED {e}"
                with lock, open(WORK / "errors.log", "a", encoding="utf-8") as fh:
                    fh.write(f"v{v} p{p}: {e}\n")
            done[0] += 1
            print(f"{done[0]}/{len(todo)} v{v}_p{p:04d} {status}", flush=True)


def cmd_estimate(a):
    n = sum(vol_pages(v) for v in (1, 2)) - sum(y - x + 1 for _, x, y in CFG.get("skip_pages", []))
    print(f"{n} pages: about {n * 3000 / 1e6:.1f}M input and {n * 1600 / 1e6:.1f}M output tokens. Multiply by your model's current per-million prices.")


def main():
    ap = argparse.ArgumentParser(); sp = ap.add_subparsers(dest="cmd", required=True)
    r = sp.add_parser("run"); r.add_argument("--vol", type=int); r.add_argument("--pages"); r.add_argument("--workers", type=int, default=4)
    r.add_argument("--model", default=DEFAULT_MODEL); r.add_argument("--limit", type=int); r.add_argument("--force", action="store_true")
    sp.add_parser("estimate")
    a = ap.parse_args()
    {"run": cmd_run, "estimate": cmd_estimate}[a.cmd](a)


if __name__ == "__main__":
    main()
