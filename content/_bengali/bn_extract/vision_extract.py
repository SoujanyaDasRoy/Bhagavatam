#!/usr/bin/env python3
"""
vision_extract.py - High-accuracy Bengali PDF transcription using Multimodal Vision AI.

Supports:
  - Google Gemini (Gemini 1.5 Flash / Pro, Gemini 2.0/2.5) -> Recommended (fast & low cost / free tier)
  - Anthropic Claude (Claude 3.5 Sonnet)
  - OpenAI (GPT-4o, GPT-4o-mini)

Outputs the exact `work/vision/vN_pNNNN.json` format expected by `bn_extract.py assemble`.

Usage:
  # Check status & API keys
  python vision_extract.py check

  # Run a test page (e.g. Vol 1, Page 360)
  python vision_extract.py run --vol 1 --pages 360 --provider gemini

  # Run Skandha 3 remaining pages (Pages 362-402)
  python vision_extract.py run --vol 1 --pages 362-402 --provider gemini --workers 4

  # Run Skandha 11 (Vol 2, Pages 803-1008)
  python vision_extract.py run --vol 2 --pages 803-1008 --provider gemini --workers 4

  # Assemble the results into content chapters
  python bn_extract.py assemble
"""

from __future__ import annotations

import argparse
import base64
import json
import os
import re
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from PIL import Image

import bn_extract as b

HERE = Path(__file__).resolve().parent
WORK = HERE / "work"
VISION_DIR = WORK / "vision"

SYSTEM_PROMPT = """You are an expert transcriber of Bengali classical texts.
You are given a scanned page of the Gita Press Bengali edition of the Shrimad Bhagavata Mahapurana.

Page Layout:
- Two columns: The Sanskrit shlokas in Bengali/Devanagari script are on the LEFT. The Bengali TRANSLATION is on the RIGHT.
- Headings: Chapter-opening pages have a wide heading block across the top.
- Footer: Footnotes at the bottom below a thin divider line.
- Running header: At the very top (ignore).
- Page number: In square brackets or top/bottom (extract into page_label).

YOUR TASK:
Transcribe the BENGALI TRANSLATION column faithfully and output ONLY valid JSON matching this schema:

{
  "skandha_heading": "string or null (e.g. 'তৃতীয় স্কন্ধ' if printed on page)",
  "chapter_number": "integer or null (if a chapter starts on this page, e.g. 1, 2, 25)",
  "chapter_title": "string or null (the Bengali chapter title line under the heading, exactly as printed)",
  "colophon": "string or null (closing line such as 'ইতি শ্রীমদ্ভাগবতে ... সমাপ্ত' if present)",
  "sanskrit_verse_numbers": [1, 2, 3],  // Integers: verse numbers at the end of each Sanskrit shloka on the left
  "paragraphs": [
    "First paragraph of Bengali translation ending with ॥ ১ ॥",
    "Second paragraph of Bengali translation ending with ॥ ২ ॥"
  ],
  "paragraphs_before_chapter_heading": 0, // Number of paragraphs on this page belonging to previous chapter
  "first_paragraph_continues_previous_page": false, // true if paragraph begins mid-sentence
  "footnotes": ["(১) ...", "(২) ..."], // Footnotes at bottom
  "page_label": "string or null (printed page number)",
  "layout_note": "string or null (any damaged text or notes)"
}

RULES:
1. FAITHFUL TRANSCRIPTION: Copy exactly what is printed in the right-column translation. Do not modernize or summarize.
2. VERSE MARKERS: Keep the Bengali numeral verse marker at the end of each unit (e.g. '॥ ১ ॥' or '॥ ৪-৫ ॥').
3. EXCLUDE SANSKRIT COLUMN: Do NOT include the Sanskrit shlokas or speaker markers (like 'শ্রীশুক উবাচ') inside paragraphs.
4. UNREADABLE WORDS: If a word is unclear, write your best reading followed immediately by [?]. Never guess silently.
"""

def get_api_key(provider: str, explicit_key: str | None = None) -> str:
    if explicit_key:
        return explicit_key
    if provider == "gemini":
        key = os.environ.get("GEMINI_API_KEY") or os.environ.get("GOOGLE_API_KEY")
        if not key:
            sys.exit("Error: GEMINI_API_KEY or GOOGLE_API_KEY environment variable is not set.\n"
                     "Set it with: $env:GEMINI_API_KEY = 'your_key_here'")
        return key
    elif provider == "anthropic":
        key = os.environ.get("ANTHROPIC_API_KEY")
        if not key:
            sys.exit("Error: ANTHROPIC_API_KEY is not set.\n"
                     "Set it with: $env:ANTHROPIC_API_KEY = 'sk-ant-...'")
        return key
    elif provider == "openai":
        key = os.environ.get("OPENAI_API_KEY")
        if not key:
            sys.exit("Error: OPENAI_API_KEY is not set.\n"
                     "Set it with: $env:OPENAI_API_KEY = 'sk-...'")
        return key
    else:
        sys.exit(f"Unknown provider: {provider}")


def transcribe_gemini(image_path: Path, api_key: str, model_name: str = "gemini-1.5-flash") -> dict:
    try:
        import google.generativeai as genai
    except ImportError:
        sys.exit("google-generativeai package missing. Run: pip install google-generativeai")

    genai.configure(api_key=api_key)
    model = genai.GenerativeModel(
        model_name=model_name,
        generation_config={
            "temperature": 0.0,
            "response_mime_type": "application/json",
        }
    )

    img = Image.open(image_path)
    prompt = SYSTEM_PROMPT + "\nTranscribe this page into the exact JSON structure specified above."
    
    for attempt in range(4):
        try:
            response = model.generate_content([img, prompt])
            raw_text = response.text.strip()
            # Clean up possible markdown fence
            if raw_text.startswith("```json"):
                raw_text = raw_text[7:]
            if raw_text.startswith("```"):
                raw_text = raw_text[3:]
            if raw_text.endswith("```"):
                raw_text = raw_text[:-3]
            data = json.loads(raw_text.strip())
            return normalize_record(data)
        except Exception as e:
            if attempt == 3:
                raise RuntimeError(f"Gemini failed on {image_path.name}: {e}")
            time.sleep(2 * (attempt + 1))


def transcribe_anthropic(image_path: Path, api_key: str, model_name: str = "claude-3-5-sonnet-20241022") -> dict:
    try:
        import anthropic
    except ImportError:
        sys.exit("anthropic package missing. Run: pip install anthropic")

    client = anthropic.Anthropic(api_key=api_key, max_retries=5)
    b64_img = base64.standard_b64encode(image_path.read_bytes()).decode()

    for attempt in range(4):
        try:
            resp = client.messages.create(
                model=model_name,
                max_tokens=4096,
                temperature=0,
                tools=[b.TOOL],
                tool_choice={"type": "tool", "name": "record_page"},
                messages=[{
                    "role": "user",
                    "content": [
                        {"type": "image", "source": {"type": "base64", "media_type": "image/png", "data": b64_img}},
                        {"type": "text", "text": b.PROMPT},
                    ]
                }]
            )
            for block in resp.content:
                if getattr(block, "type", "") == "tool_use":
                    return normalize_record(dict(block.input))
            raise RuntimeError("no tool call returned by Anthropic")
        except Exception as e:
            if attempt == 3:
                raise RuntimeError(f"Anthropic failed on {image_path.name}: {e}")
            time.sleep(2 * (attempt + 1))


def transcribe_openai(image_path: Path, api_key: str, model_name: str = "gpt-4o-mini") -> dict:
    try:
        import openai
    except ImportError:
        sys.exit("openai package missing. Run: pip install openai")

    client = openai.OpenAI(api_key=api_key)
    b64_img = base64.standard_b64encode(image_path.read_bytes()).decode()

    for attempt in range(4):
        try:
            resp = client.chat.completions.create(
                model=model_name,
                response_format={"type": "json_object"},
                temperature=0.0,
                messages=[
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {
                        "role": "user",
                        "content": [
                            {"type": "text", "text": "Transcribe this page into JSON."},
                            {"type": "image_url", "image_url": {"url": f"data:image/png;base64,{b64_img}"}},
                        ],
                    },
                ],
            )
            content = resp.choices[0].message.content
            return normalize_record(json.loads(content))
        except Exception as e:
            if attempt == 3:
                raise RuntimeError(f"OpenAI failed on {image_path.name}: {e}")
            time.sleep(2 * (attempt + 1))


def normalize_record(data: dict) -> dict:
    """Ensure all required fields exist with correct types."""
    return {
        "skandha_heading": data.get("skandha_heading") or None,
        "chapter_number": int(data["chapter_number"]) if data.get("chapter_number") is not None else None,
        "chapter_title": data.get("chapter_title") or None,
        "colophon": data.get("colophon") or None,
        "sanskrit_verse_numbers": [int(x) for x in data.get("sanskrit_verse_numbers", [])],
        "paragraphs": [str(p).strip() for p in data.get("paragraphs", []) if str(p).strip()],
        "paragraphs_before_chapter_heading": int(data.get("paragraphs_before_chapter_heading") or 0),
        "first_paragraph_continues_previous_page": bool(data.get("first_paragraph_continues_previous_page", False)),
        "footnotes": [str(fn).strip() for fn in data.get("footnotes", []) if str(fn).strip()],
        "page_label": str(data.get("page_label")) if data.get("page_label") else None,
        "layout_note": data.get("layout_note") or None,
    }


def process_page(cfg: dict, vol: str, page: int, provider: str, model_name: str, api_key: str, force: bool = False) -> tuple[str, int, str]:
    key = b.page_key(vol, page)
    out_json = VISION_DIR / f"{key}.json"
    if out_json.exists() and not force:
        return vol, page, "skipped (exists)"

    # Render/retrieve preprocessed image
    img_path = b.prepared_image(cfg, vol, page)

    if provider == "gemini":
        record = transcribe_gemini(img_path, api_key, model_name)
    elif provider == "anthropic":
        record = transcribe_anthropic(img_path, api_key, model_name)
    elif provider == "openai":
        record = transcribe_openai(img_path, api_key, model_name)
    else:
        raise ValueError(f"Unknown provider: {provider}")

    record["_page"] = {"vol": vol, "page": page, "model": f"{provider}:{model_name}"}

    with open(out_json, "w", encoding="utf-8") as f:
        json.dump(record, f, ensure_ascii=False, indent=2)

    p_count = len(record["paragraphs"])
    sv_count = len(record["sanskrit_verse_numbers"])
    return vol, page, f"done ({p_count} paras, {sv_count} verses)"


def cmd_check(args) -> None:
    cfg = b.load_config()
    print("=== Configuration & Setup Check ===")
    print("PDF Directory:", cfg.get("pdf_dir"))
    for v, info in cfg.get("volumes", {}).items():
        path = b.pdf_path(cfg, v)
        print(f"  Volume {v} ({info['file']}): {'[FOUND]' if path.exists() else '[NOT FOUND]'}")

    print("\nAPI Keys Detected in Environment:")
    gemini_key = os.environ.get("GEMINI_API_KEY") or os.environ.get("GOOGLE_API_KEY")
    anthropic_key = os.environ.get("ANTHROPIC_API_KEY")
    openai_key = os.environ.get("OPENAI_API_KEY")
    print(f"  GEMINI_API_KEY / GOOGLE_API_KEY: {'[SET]' if gemini_key else '[NOT SET]'}")
    print(f"  ANTHROPIC_API_KEY:               {'[SET]' if anthropic_key else '[NOT SET]'}")
    print(f"  OPENAI_API_KEY:                  {'[SET]' if openai_key else '[NOT SET]'}")

    vision_count = len(list(VISION_DIR.glob("*.json"))) if VISION_DIR.exists() else 0
    print(f"\nPages already transcribed in work/vision/: {vision_count}")


def cmd_run(args) -> None:
    cfg = b.load_config()
    VISION_DIR.mkdir(parents=True, exist_ok=True)
    api_key = get_api_key(args.provider, args.api_key)

    vols = [args.vol] if args.vol else list(cfg["volumes"])
    todo = []
    for v in vols:
        a, b_page = b.parse_range(args.pages, b.vol_pages(cfg, v))
        for p in range(a, min(b_page, b.vol_pages(cfg, v)) + 1):
            if not args.force and (VISION_DIR / f"{b.page_key(v, p)}.json").exists():
                continue
            if any(v == str(v0) and a0 <= p <= b0 for v0, a0, b0 in cfg.get("skip_pages", [])):
                continue
            todo.append((v, p))

    if args.limit:
        todo = todo[: args.limit]

    print(f"Starting extraction for {len(todo)} pages using {args.provider} ({args.model})...")
    if not todo:
        print("No pages to process.")
        return

    success = 0
    failed = 0
    with ThreadPoolExecutor(max_workers=args.workers) as executor:
        futures = {
            executor.submit(process_page, cfg, v, p, args.provider, args.model, api_key, args.force): (v, p)
            for v, p in todo
        }
        for future in as_completed(futures):
            v, p = futures[future]
            try:
                vol, page, msg = future.result()
                print(f"[v{vol} p{page:04d}] {msg}")
                success += 1
            except Exception as e:
                print(f"[v{v} p{p:04d}] ERROR: {e}")
                failed += 1

    print(f"\nCompleted: {success} succeeded, {failed} failed.")
    print("Next step: Run `python bn_extract.py assemble` to build chapter files and verify review/index.html")


def main():
    parser = argparse.ArgumentParser(description="Bengali Bhagavatam Vision Transcription")
    subparsers = parser.add_subparsers(dest="cmd", required=True)

    # check
    subparsers.add_parser("check", help="Check setup and API keys")

    # run
    run_parser = subparsers.add_parser("run", help="Transcribe pages")
    run_parser.add_argument("--vol", default="1", choices=["1", "2"], help="Volume number (1 or 2)")
    run_parser.add_argument("--pages", help="Page range (e.g. 362-402 or 803-1008)")
    run_parser.add_argument("--provider", default="gemini", choices=["gemini", "anthropic", "openai"], help="AI Provider")
    run_parser.add_argument("--model", default="gemini-1.5-flash", help="Model name")
    run_parser.add_argument("--workers", type=int, default=4, help="Parallel worker threads")
    run_parser.add_argument("--limit", type=int, help="Max pages to process")
    run_parser.add_argument("--force", action="store_true", help="Overwrite existing vision JSONs")
    run_parser.add_argument("--api-key", help="Explicit API key (optional)")

    args = parser.parse_args()
    if args.cmd == "check":
        cmd_check(args)
    elif args.cmd == "run":
        cmd_run(args)


if __name__ == "__main__":
    main()
