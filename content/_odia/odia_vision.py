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

Gemini backend (the one to use with a Google AI Studio key; the free tier is enough for a pilot):
    pip install google-genai pymupdf pillow
    $env:GEMINI_API_KEY = "..."               # set it in the shell; never write a key into a file in the repository
    python odia_vision.py run --backend gemini --skandha 5 --pages 579-585            # pilot: first chapter of Skandha 5
    python odia_vision.py run --backend gemini --skandha 5-12 --workers 3             # pass A  -> work_v/vision
    python odia_vision.py run --backend gemini --skandha 5-12 --pass B --model gemini-2.5-pro --workers 2   # pass B -> work_v/vision_b
Pass A and pass B are two independent readings of every page; odia_verify.py compares them word by word.
The backend is --backend, else gemini when GEMINI_API_KEY is set, else anthropic.
"""
import argparse, base64, io, json, os, re, sys, threading, time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

HERE = Path(__file__).resolve().parent
WORK = Path(os.environ.get("ODIA_VWORK", HERE / "work_v"))
CFG = json.loads((HERE / "config.json").read_text(encoding="utf-8"))
DEFAULT_MODEL = os.environ.get("ODIA_MODEL")   # when unset, main() picks the backend default
GEMINI_MODEL = os.environ.get("ODIA_GEMINI_MODEL", "gemini-2.5-flash")
ANTHROPIC_MODEL = "claude-sonnet-5-5"
PASS_DIR = {"A": "vision", "B": "vision_b", "C": "vision_c"}   # where each independent reading is stored
LONG_EDGE = int(os.environ.get("ODIA_LONG_EDGE", "2000"))
# Pass B is rendered at another size from another cache folder: a second reading of the SAME pixels would repeat the same slips.
LONG_EDGE_B = int(os.environ.get("ODIA_LONG_EDGE_B", "2600"))
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


def page_image(vol, page, edge=None, sub="images"):
    edge = edge or LONG_EDGE
    out = WORK / sub / f"v{vol}_p{page:04d}.png"
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
    s = edge / max(w, h)
    img = img.resize((max(1, round(w * s)), max(1, round(h * s))), Image.LANCZOS)
    img.save(out, optimize=True)
    return out


def client(backend="anthropic"):
    if backend == "gemini":
        try:
            from google import genai
        except ImportError:
            sys.exit("pip install google-genai pymupdf pillow")
        if not os.environ.get("GEMINI_API_KEY"):
            sys.exit("GEMINI_API_KEY is not set (create a key in Google AI Studio and set it in the shell, not in a file).")
        return genai.Client(api_key=os.environ["GEMINI_API_KEY"])
    try:
        import anthropic
    except ImportError:
        sys.exit("pip install pymupdf pillow anthropic")
    if not os.environ.get("ANTHROPIC_API_KEY"):
        sys.exit("ANTHROPIC_API_KEY is not set.")
    return anthropic.Anthropic(max_retries=6, timeout=180)


def gemini_prompt():
    """The tool schema, spelled out for a model that answers in plain JSON instead of calling a tool."""
    props = TOOL["input_schema"]["properties"]
    fields = "\n".join(f'- "{k}": {v.get("description", "")}' for k, v in props.items())
    return (PROMPT + "\nAnswer with ONE JSON object and nothing else (no markdown fence). Keys:\n" + fields +
            "\nUse null for an absent value, [] for an empty list, and keep every key.")


def parse_gemini_json(text):
    """Model text -> dict. Tolerates a ```json fence; raises on anything that is not an object with 'paragraphs'."""
    t = (text or "").strip()
    t = re.sub(r"^```(?:json)?\s*|\s*```$", "", t)
    d = json.loads(t)
    if isinstance(d, list) and d and isinstance(d[0], dict):
        d = d[0]
    if not isinstance(d, dict) or "paragraphs" not in d:
        raise ValueError("no paragraphs in the answer")
    return d


def transcribe_gemini(cl, model, img_path):
    from google.genai import types
    last = None
    prompt = gemini_prompt()
    for attempt in range(5):
        try:
            r = cl.models.generate_content(
                model=model,
                contents=[types.Part.from_bytes(data=img_path.read_bytes(), mime_type="image/png"), prompt],
                config=types.GenerateContentConfig(temperature=0, response_mime_type="application/json", max_output_tokens=16384))
            out = parse_gemini_json(r.text)
            u = getattr(r, "usage_metadata", None)
            out["_usage"] = {"in": getattr(u, "prompt_token_count", None), "out": getattr(u, "candidates_token_count", None)}
            return out
        except Exception as e:  # noqa
            last = e
            time.sleep(3 + 5 * attempt)
    raise RuntimeError(f"giving up: {last}")


def transcribe(cl, model, img_path, backend="anthropic"):
    if backend == "gemini":
        return transcribe_gemini(cl, model, img_path)
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


def skandha_spans(spec):
    """'5' or '5-12' -> [(vol, first_page, last_page)] from data/skandha_pages.json (made from the assembled chapters)."""
    sp = json.loads((HERE / "data" / "skandha_pages.json").read_text(encoding="utf-8"))
    lo, _, hi = spec.partition("-")
    lo, hi = int(lo), int(hi or lo)
    return [(int(v), x, y) for k in range(lo, hi + 1) for v, x, y in sp[str(k)]]


def pages_to_do(a, out_dir):
    """The (vol, page) list a run covers. Pure function of the arguments and the files already written."""
    if a.skandha:
        want = skandha_spans(a.skandha)
        if a.pages:
            plo, phi = parse_range(a.pages, 10 ** 6)
            want = [(v, max(x, plo), min(y, phi)) for v, x, y in want if max(x, plo) <= min(y, phi)]
        cand = [(v, p) for v, x, y in want for p in range(x, y + 1)]
        if a.vol:
            cand = [(v, p) for v, p in cand if v == a.vol]
    else:
        cand = []
        for v in ([a.vol] if a.vol else [1, 2]):
            lo, hi = parse_range(a.pages, vol_pages(v))
            cand += [(v, p) for p in range(lo, min(hi, vol_pages(v)) + 1)]
    todo = []
    for v, p in dict.fromkeys(cand):
        if any(str(sv) == str(v) and x <= p <= y for sv, x, y in CFG.get("skip_pages", [])):
            continue
        if a.force or not (out_dir / f"v{v}_p{p:04d}.json").exists():
            todo.append((v, p))
    return todo


def cmd_run(a):
    out_dir = WORK / PASS_DIR[a.passname]
    todo = pages_to_do(a, out_dir)
    if a.limit:
        todo = todo[:a.limit]
    print(f"{len(todo)} page(s) with {a.backend}:{a.model} -> {out_dir}", flush=True)
    if not todo:
        return
    out_dir.mkdir(parents=True, exist_ok=True)
    cl = client(a.backend); lock = threading.Lock(); done = [0]

    def job(v, p):
        img = page_image(v, p, LONG_EDGE_B, "images_b") if a.passname == "B" else page_image(v, p)
        res = normalise(transcribe(cl, a.model, img, a.backend), v, p, f"{a.backend}:{a.model}")
        res["_page"]["pass"] = a.passname
        (out_dir / f"v{v}_p{p:04d}.json").write_text(json.dumps(res, ensure_ascii=False, indent=1), encoding="utf-8")
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
    r.add_argument("--model"); r.add_argument("--limit", type=int); r.add_argument("--force", action="store_true")
    r.add_argument("--backend", choices=["anthropic", "gemini"]); r.add_argument("--skandha", help="5 or 5-12 (uses data/skandha_pages.json)")
    r.add_argument("--pass", dest="passname", choices=list(PASS_DIR), default="A", help="A=work_v/vision, B=work_v/vision_b, C=work_v/vision_c")
    sp.add_parser("estimate")
    a = ap.parse_args()
    if a.cmd == "run":
        a.backend = a.backend or ("gemini" if os.environ.get("GEMINI_API_KEY") else "anthropic")
        a.model = a.model or DEFAULT_MODEL or (GEMINI_MODEL if a.backend == "gemini" else ANTHROPIC_MODEL)
    {"run": cmd_run, "estimate": cmd_estimate}[a.cmd](a)


if __name__ == "__main__":
    main()
