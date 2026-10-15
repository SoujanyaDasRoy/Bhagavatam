#!/usr/bin/env python3
"""
vision_extract.py - High-accuracy Bengali PDF transcription using Multimodal Vision AI.
"""

from __future__ import annotations

import argparse
import base64
import json
import os
import re
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
import requests

import bn_extract as b

HERE = Path(__file__).resolve().parent
WORK = HERE / "work"
VISION_DIR = WORK / "vision"


class KeyPool:
    def __init__(self, raw_keys: str):
        if not raw_keys:
            self.keys = []
        else:
            self.keys = [k.strip() for k in re.split(r'[,;\s]+', raw_keys) if k.strip()]
        self.lock = threading.Lock()
        self.state_file = WORK / ".active_key_idx"
        self.idx = 0
        if self.state_file.exists():
            try:
                saved_idx = int(self.state_file.read_text(encoding="utf-8").strip())
                if 0 <= saved_idx < len(self.keys):
                    self.idx = saved_idx
            except Exception:
                pass

    def get(self) -> str:
        with self.lock:
            return self.keys[self.idx] if self.keys else ""

    def advance(self, failed_key: str, reason: str = "") -> bool:
        with self.lock:
            if not self.keys:
                return False
            is_daily = "limit: 1500" in reason.lower() or "perday" in reason.lower() or "daily" in reason.lower()
            if is_daily and failed_key in self.keys:
                print(f"\n[KeyPool] Key {failed_key[:12]}... reached daily quota. Deactivating.", flush=True)
                self.keys.remove(failed_key)
                if not self.keys:
                    return False
                self.idx = self.idx % len(self.keys)
                return True

            self.idx = (self.idx + 1) % len(self.keys)
            print(f"\n[KeyPool] Rotating to key #{self.idx + 1} of {len(self.keys)} ({reason})", flush=True)
            return True

    def has_more(self) -> bool:
        with self.lock:
            return self.idx < len(self.keys) - 1


def _get_api_key():
    keys = []
    k = os.environ.get("GEMINI_API_KEY", "").strip()
    if k:
        keys.extend([x.strip() for x in re.split(r'[,;\s]+', k) if x.strip()])
    env_file = HERE / ".env"
    if env_file.exists():
        for line in env_file.read_text(encoding="utf-8").splitlines():
            if line.startswith("GEMINI_API_KEY="):
                val = line.split("=", 1)[1].strip().strip('"').strip("'")
                for x in re.split(r'[,;\s]+', val):
                    x = x.strip()
                    if x and x not in keys:
                        keys.append(x)
    return ",".join(keys)

DEFAULT_GEMINI_KEY = _get_api_key()

SYSTEM_PROMPT = """You are an expert transcriber of Bengali classical texts.
You are given a scanned page of the Gita Press Bengali edition of the Shrimad Bhagavata Mahapurana.

Page Layout:
- Two columns: The Sanskrit shlokas in Bengali/Devanagari script are on the LEFT. The Bengali TRANSLATION is on the RIGHT.
- Headings: Chapter-opening pages have a wide heading block across the top.
- Footer: Footnotes at the bottom below a thin divider line.
- Running header: At the very top (ignore).
- Page number: In square brackets or top/bottom (extract into page_label).

YOUR TASK:
Transcribe the BENGALI TRANSLATION column faithfully and output ONLY a JSON object matching this schema:

{
  "skandha_heading": "string or null (e.g. 'একাদশ স্কন্ধ' if printed on page)",
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
2. VERSE MARKERS: Keep the Bengali numeral verse marker at the end of each unit (e.g. '॥ ১ ॥' or '॥ ৪-৫ ॥' or '।। ৫১ ।।').
3. EXCLUDE SANSKRIT COLUMN: Do NOT include the Sanskrit shlokas or speaker markers (like 'শ্রীশুক উবাচ') inside paragraphs.
4. UNREADABLE WORDS: If a word is unclear, write your best reading followed immediately by [?]. Never guess silently.
"""

def get_api_key(provider: str, explicit_key: str | None = None) -> str:
    if explicit_key:
        return explicit_key
    if provider == "gemini":
        return os.environ.get("GEMINI_API_KEY") or os.environ.get("GOOGLE_API_KEY") or DEFAULT_GEMINI_KEY
    elif provider == "anthropic":
        key = os.environ.get("ANTHROPIC_API_KEY")
        if not key:
            sys.exit("Error: ANTHROPIC_API_KEY is not set.")
        return key
    elif provider == "openai":
        key = os.environ.get("OPENAI_API_KEY")
        if not key:
            sys.exit("Error: OPENAI_API_KEY is not set.")
        return key
    else:
        sys.exit(f"Unknown provider: {provider}")


def parse_json_safely(raw_text: str) -> dict:
    raw_text = raw_text.strip()
    if raw_text.startswith("```json"):
        raw_text = raw_text[7:]
    if raw_text.startswith("```"):
        raw_text = raw_text[3:]
    if raw_text.endswith("```"):
        raw_text = raw_text[:-3]
    raw_text = raw_text.strip()
    start = raw_text.find("{")
    if start != -1:
        raw_text = raw_text[start:]
    
    try:
        data = json.loads(raw_text, strict=False)
        if isinstance(data, dict):
            return data
    except Exception:
        pass
    
    try:
        decoder = json.JSONDecoder(strict=False)
        obj, _ = decoder.raw_decode(raw_text)
        if isinstance(obj, dict):
            return obj
    except Exception:
        pass

    end = raw_text.rfind("}")
    if end != -1:
        try:
            data = json.loads(raw_text[:end+1], strict=False)
            if isinstance(data, dict):
                return data
        except Exception:
            pass

    raise ValueError(f"Could not parse JSON output: {raw_text[:200]}")


def transcribe_gemini(image_path: Path, key_pool: KeyPool | str, model_name: str = "gemini-2.5-flash-lite") -> dict:
    if isinstance(key_pool, str):
        pool = KeyPool(key_pool)
    else:
        pool = key_pool

    b64_img = base64.b64encode(image_path.read_bytes()).decode("utf-8")
    
    # Priority rotation of active supported models
    models_pool = [
        "gemini-3.1-flash-lite",
        "gemini-3.5-flash-lite",
        "gemini-3.8-flash",
        "gemini-3-flash-preview",
        "gemini-3.6-flash",
        "gemini-3.7-flash",
        "gemini-3.5-flash",
    ]
    if model_name not in models_pool:
        models_pool.insert(0, model_name)
    
    last_err = None
    while True:
        current_key = pool.get()
        if not current_key:
            raise RuntimeError(f"No API key available for {image_path.name}")

        key_switched = False
        for m in models_pool:
            if key_switched:
                break
            url = f"https://generativelanguage.googleapis.com/v1beta/models/{m}:generateContent?key={current_key}"
            payload = {
                "contents": [{
                    "parts": [
                        {"text": SYSTEM_PROMPT + "\nTranscribe this page into a single JSON object. Do NOT truncate."},
                        {"inlineData": {"mimeType": "image/png", "data": b64_img}}
                    ]
                }],
                "generationConfig": {
                    "temperature": 0.0,
                    "responseMimeType": "application/json",
                    "maxOutputTokens": 8192
                }
            }
            for attempt in range(3):
                try:
                    resp = requests.post(url, json=payload, timeout=90)
                    if resp.status_code == 200:
                        raw_text = resp.json()["candidates"][0]["content"]["parts"][0]["text"]
                        data = parse_json_safely(raw_text)
                        rec = normalize_record(data)
                        rec["_model_used"] = m
                        return rec
                    elif resp.status_code == 429:
                        last_err = f"429 Rate limit on {m}: {resp.text[:160]}"
                        err_text = resp.text.lower()
                        if "quota" in err_text or "resource_exhausted" in err_text or attempt >= 2:
                            if pool.advance(current_key, reason=f"429 quota on {m}"):
                                key_switched = True
                                break
                        wait_time = (attempt + 1) * 4
                        time.sleep(wait_time)
                    elif resp.status_code in (400, 403):
                        last_err = f"HTTP {resp.status_code}: {resp.text[:160]}"
                        if pool.advance(current_key, reason=f"HTTP {resp.status_code}"):
                            key_switched = True
                            break
                        break
                    elif resp.status_code in (500, 503, 504):
                        time.sleep((attempt + 1) * 3)
                    else:
                        last_err = f"HTTP {resp.status_code}: {resp.text[:160]}"
                        break
                except Exception as e:
                    last_err = str(e)
                    time.sleep(2)

        if key_switched:
            continue
        elif pool.advance(current_key, reason=f"all models failed on key: {last_err}"):
            continue
        else:
            break

    raise RuntimeError(f"Gemini failed on {image_path.name}: {last_err}")


def normalize_record(data: dict) -> dict:
    """Ensure all required fields exist with correct types."""
    # Handle possible nested format if model wrapped verses
    paras = data.get("paragraphs") or []
    if not paras and "verses" in data and isinstance(data["verses"], list):
        paras = [v.get("bengali_translation") or v.get("translation") for v in data["verses"] if isinstance(v, dict)]
        paras = [p for p in paras if p]

    sv = data.get("sanskrit_verse_numbers") or []
    if not sv and "verses" in data and isinstance(data["verses"], list):
        sv = [v.get("verse_number") for v in data["verses"] if isinstance(v, dict) and v.get("verse_number")]

    return {
        "skandha_heading": data.get("skandha_heading") or None,
        "chapter_number": int(data["chapter_number"]) if data.get("chapter_number") is not None and str(data.get("chapter_number")).isdigit() else None,
        "chapter_title": data.get("chapter_title") or None,
        "colophon": data.get("colophon") or None,
        "sanskrit_verse_numbers": [int(x) for x in sv if str(x).isdigit()],
        "paragraphs": [str(p).strip() for p in paras if str(p).strip()],
        "paragraphs_before_chapter_heading": int(data.get("paragraphs_before_chapter_heading") or 0),
        "first_paragraph_continues_previous_page": bool(data.get("first_paragraph_continues_previous_page", False)),
        "footnotes": [str(fn).strip() for fn in data.get("footnotes", []) if str(fn).strip()],
        "page_label": str(data.get("page_label")) if data.get("page_label") else None,
        "layout_note": data.get("layout_note") or None,
    }


def process_page(cfg: dict, vol: str, page: int, provider: str, model_name: str, key_pool: KeyPool | str, force: bool = False) -> tuple[str, int, str]:
    key = b.page_key(vol, page)
    out_json = VISION_DIR / f"{key}.json"
    if out_json.exists() and not force:
        return vol, page, "skipped (exists)"

    # Render/retrieve preprocessed image
    img_path = b.prepared_image(cfg, vol, page)

    if provider == "gemini":
        record = transcribe_gemini(img_path, key_pool, model_name)
    else:
        raise ValueError(f"Unknown provider: {provider}")

    record["_page"] = {"vol": vol, "page": page, "model": f"{provider}:{model_name}"}

    with open(out_json, "w", encoding="utf-8") as f:
        json.dump(record, f, ensure_ascii=False, indent=2)

    p_count = len(record["paragraphs"])
    sv_count = len(record["sanskrit_verse_numbers"])
    return vol, page, f"done ({p_count} paras, {sv_count} verses)"


def cmd_run(args) -> None:
    cfg = b.load_config()
    VISION_DIR.mkdir(parents=True, exist_ok=True)
    raw_keys = get_api_key(args.provider, args.api_key)
    pool = KeyPool(raw_keys)

    def parse_page_spec(spec: str | None, max_p: int) -> list[int]:
        if not spec:
            return list(range(1, max_p + 1))
        res = []
        for part in spec.split(','):
            part = part.strip()
            if not part:
                continue
            if '-' in part:
                a, _, c = part.partition('-')
                res.extend(range(int(a), int(c or a) + 1))
            else:
                res.append(int(part))
        return res

    vols = [args.vol] if args.vol else list(cfg["volumes"])
    todo = []
    for v in vols:
        pages_to_do = parse_page_spec(args.pages, b.vol_pages(cfg, v))
        for p in pages_to_do:
            if p > b.vol_pages(cfg, v):
                continue
            if not args.force and (VISION_DIR / f"{b.page_key(v, p)}.json").exists():
                continue
            if any(v == str(v0) and a0 <= p <= b0 for v0, a0, b0 in cfg.get("skip_pages", [])):
                continue
            todo.append((v, p))

    if args.limit:
        todo = todo[: args.limit]

    print(f"Starting extraction for {len(todo)} pages using {args.provider} ({args.model})...", flush=True)
    if not todo:
        print("All pages in this range are already completed!", flush=True)
        return

    success = 0
    failed = 0
    with ThreadPoolExecutor(max_workers=args.workers) as executor:
        futures = {
            executor.submit(process_page, cfg, v, p, args.provider, args.model, pool, args.force): (v, p)
            for v, p in todo
        }
        for future in as_completed(futures):
            v, p = futures[future]
            try:
                vol, page, msg = future.result()
                print(f"[v{vol} p{page:04d}] {msg}", flush=True)
                success += 1
            except Exception as e:
                print(f"[v{v} p{p:04d}] ERROR: {e}", flush=True)
                failed += 1

    print(f"\nCompleted: {success} succeeded, {failed} failed.", flush=True)
    if failed > 0:
        sys.exit(1)
    print("Next step: Run `python bn_extract.py assemble` to build chapter files and verify review/index.html", flush=True)


def main():
    parser = argparse.ArgumentParser(description="Bengali Bhagavatam Vision Transcription")
    subparsers = parser.add_subparsers(dest="cmd", required=True)

    # run
    run_parser = subparsers.add_parser("run", help="Transcribe pages")
    run_parser.add_argument("--vol", default="2", choices=["1", "2"], help="Volume number (1 or 2)")
    run_parser.add_argument("--pages", default="803-1008", help="Page range")
    run_parser.add_argument("--provider", default="gemini", choices=["gemini", "anthropic", "openai"], help="AI Provider")
    run_parser.add_argument("--model", default="gemini-3.1-flash-lite", help="Model name")
    run_parser.add_argument("--workers", type=int, default=3, help="Parallel worker threads")
    run_parser.add_argument("--limit", type=int, help="Max pages to process")
    run_parser.add_argument("--force", action="store_true", help="Overwrite existing vision JSONs")
    run_parser.add_argument("--api-key", help="Explicit API key (optional)")

    args = parser.parse_args()
    if args.cmd == "run":
        cmd_run(args)


if __name__ == "__main__":
    main()
