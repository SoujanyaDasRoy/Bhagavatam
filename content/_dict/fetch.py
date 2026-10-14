#!/usr/bin/env python3
"""Task 2 of the dictionary plan: fetch source dumps from dumps.wikimedia.org and Kaikki.org.

Usage:
    py content/_dict/fetch.py --all
    py content/_dict/fetch.py --sources orwikt,bnwikt,hiwikt,enwikt
    py content/_dict/fetch.py --check

Downloads to content/_dict/work/dumps/ and records dump date, size, and sha256 in content/_dict/work/fetched.json.
Uses polite User-Agent with contact info, HTTP resume support, and exponential backoff on 429/5xx.
Does NOT crawl Wikimedia web APIs.
"""
import argparse
import hashlib
import json
import os
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

HERE = Path(__file__).resolve().parent
WORK = HERE / "work"
DUMPS = WORK / "dumps"
FETCHED_JSON = WORK / "fetched.json"

USER_AGENT = "BhagavatamApp/1.0 (https://github.com/soujanyadasroy/bhagavatam; contact: soujanyadasroy@gmail.com)"

SOURCES = {
    "orwikt": {
        "name": "Odia Wiktionary",
        "url": "https://dumps.wikimedia.org/orwiktionary/latest/orwiktionary-latest-pages-articles.xml.bz2",
        "filename": "orwiktionary-latest-pages-articles.xml.bz2",
        "licence": "CC BY-SA 4.0",
        "lang": "or",
        "kind": "native_xml",
    },
    "bnwikt": {
        "name": "Bengali Wiktionary",
        "url": "https://dumps.wikimedia.org/bnwiktionary/latest/bnwiktionary-latest-pages-articles.xml.bz2",
        "filename": "bnwiktionary-latest-pages-articles.xml.bz2",
        "licence": "CC BY-SA 4.0",
        "lang": "bn",
        "kind": "native_xml",
    },
    "hiwikt": {
        "name": "Hindi Wiktionary",
        "url": "https://dumps.wikimedia.org/hiwiktionary/latest/hiwiktionary-latest-pages-articles.xml.bz2",
        "filename": "hiwiktionary-latest-pages-articles.xml.bz2",
        "licence": "CC BY-SA 4.0",
        "lang": "hi",
        "kind": "native_xml",
    },
    "enwikt": {
        "name": "English Wiktionary (English words via Wiktextract)",
        "url": "https://kaikki.org/dictionary/English/kaikki.org-dictionary-English.jsonl.gz",
        "filename": "kaikki-dictionary-English.jsonl.gz",
        "licence": "CC BY-SA 4.0",
        "lang": "en",
        "kind": "wiktextract_jsonl",
    },
    "enwikt_hi": {
        "name": "English Wiktionary (Hindi words with English glosses)",
        "url": "https://kaikki.org/dictionary/Hindi/kaikki.org-dictionary-Hindi.jsonl.gz",
        "filename": "kaikki-dictionary-Hindi.jsonl.gz",
        "licence": "CC BY-SA 4.0",
        "lang": "hi",
        "kind": "wiktextract_jsonl",
    },
    "enwikt_bn": {
        "name": "English Wiktionary (Bengali words with English glosses)",
        "url": "https://kaikki.org/dictionary/Bengali/kaikki.org-dictionary-Bengali.jsonl.gz",
        "filename": "kaikki-dictionary-Bengali.jsonl.gz",
        "licence": "CC BY-SA 4.0",
        "lang": "bn",
        "kind": "wiktextract_jsonl",
    },
    "enwikt_or": {
        "name": "English Wiktionary (Odia words with English glosses)",
        "url": "https://kaikki.org/dictionary/Odia/kaikki.org-dictionary-Odia.jsonl.gz",
        "filename": "kaikki-dictionary-Odia.jsonl.gz",
        "licence": "CC BY-SA 4.0",
        "lang": "or",
        "kind": "wiktextract_jsonl",
    },
}


def compute_sha256(filepath: Path) -> str:
    h = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(1024 * 1024):
            h.update(chunk)
    return h.hexdigest()


def download_file(url: str, dest_path: Path, max_retries: int = 5) -> dict:
    dest_path.parent.mkdir(parents=True, exist_ok=True)
    temp_path = dest_path.with_suffix(dest_path.suffix + ".part")

    existing_size = temp_path.stat().st_size if temp_path.exists() else 0

    for attempt in range(max_retries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
            if existing_size > 0:
                req.add_header("Range", f"bytes={existing_size}-")

            try:
                response = urllib.request.urlopen(req, timeout=60)
            except urllib.error.HTTPError as e:
                if e.code == 416:  # Range Not Satisfiable (already fully downloaded or server doesn't like range)
                    if existing_size > 0:
                        temp_path.replace(dest_path)
                        return {
                            "size": dest_path.stat().st_size,
                            "last_modified": None,
                        }
                    raise
                elif e.code in (429, 500, 502, 503, 504):
                    wait = 2 ** (attempt + 1) * 3
                    print(f"HTTP {e.code} for {url}. Backing off for {wait}s (attempt {attempt + 1}/{max_retries})...")
                    time.sleep(wait)
                    continue
                else:
                    raise

            status = response.status
            content_length = response.headers.get("Content-Length")
            total_size = int(content_length) if content_length else None
            last_modified = response.headers.get("Last-Modified")

            if status == 206:  # Partial Content
                print(f"Resuming {dest_path.name} from {existing_size / (1024*1024):.1f} MB...")
                mode = "ab"
            else:
                existing_size = 0
                mode = "wb"

            downloaded = existing_size
            start_time = time.time()
            last_print = start_time

            with open(temp_path, mode) as f:
                while True:
                    chunk = response.read(1024 * 256)
                    if not chunk:
                        break
                    f.write(chunk)
                    downloaded += len(chunk)
                    now = time.time()
                    if now - last_print >= 2.0:
                        speed = (downloaded - existing_size) / (now - start_time) / 1024 / 1024
                        prog = f"{downloaded / (1024*1024):.1f} MB"
                        if total_size and status == 200:
                            prog += f" / {total_size / (1024*1024):.1f} MB ({100 * downloaded / total_size:.1f}%)"
                        print(f"  {dest_path.name}: {prog} @ {speed:.2f} MB/s", end="\r", flush=True)
                        last_print = now

            print(f"  {dest_path.name}: {downloaded / (1024*1024):.1f} MB complete.                ")
            temp_path.replace(dest_path)
            return {
                "size": downloaded,
                "last_modified": last_modified,
            }

        except (urllib.error.URLError, TimeoutError, ConnectionResetError) as e:
            wait = 2 ** (attempt + 1) * 3
            print(f"\nNetwork error downloading {url}: {e}. Retrying in {wait}s...")
            time.sleep(wait)
            if temp_path.exists():
                existing_size = temp_path.stat().st_size
            continue

    raise RuntimeError(f"Failed to download {url} after {max_retries} attempts")


def fetch(source_keys: list[str]) -> None:
    DUMPS.mkdir(parents=True, exist_ok=True)
    fetched = {}
    if FETCHED_JSON.exists():
        try:
            fetched = json.loads(FETCHED_JSON.read_text(encoding="utf-8"))
        except Exception:
            fetched = {}

    for key in source_keys:
        if key not in SOURCES:
            print(f"Unknown source: {key}. Known: {list(SOURCES.keys())}")
            continue

        info = SOURCES[key]
        dest = DUMPS / info["filename"]
        print(f"\n[{key}] {info['name']}")
        print(f"Source URL: {info['url']}")

        # If already exists and recorded, verify
        if dest.exists() and key in fetched and fetched[key].get("size") == dest.stat().st_size:
            print(f"  Already downloaded: {dest.name} ({dest.stat().st_size / (1024*1024):.1f} MB)")
            continue

        meta = download_file(info["url"], dest)
        sha256_hash = compute_sha256(dest)
        now_str = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%SZ")

        fetched[key] = {
            "source_code": key,
            "name": info["name"],
            "url": info["url"],
            "filename": info["filename"],
            "size": dest.stat().st_size,
            "sha256": sha256_hash,
            "retrieved": now_str,
            "last_modified": meta.get("last_modified"),
            "licence": info["licence"],
            "lang": info["lang"],
            "kind": info["kind"],
        }
        FETCHED_JSON.write_text(json.dumps(fetched, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    print("\nFetch complete. Summary in", FETCHED_JSON)


def check() -> int:
    if not FETCHED_JSON.exists():
        print(f"FAIL: {FETCHED_JSON} does not exist. Run fetch.py first.")
        return 1
    fetched = json.loads(FETCHED_JSON.read_text(encoding="utf-8"))
    all_ok = True
    print(f"Checking fetched dumps in {DUMPS}:")
    for key, info in fetched.items():
        p = DUMPS / info["filename"]
        if not p.exists():
            print(f"  FAIL: {key} ({info['filename']}) missing from disk")
            all_ok = False
            continue
        actual_size = p.stat().st_size
        recorded_size = info.get("size")
        if actual_size != recorded_size:
            print(f"  FAIL: {key} size mismatch: disk={actual_size}, recorded={recorded_size}")
            all_ok = False
            continue
        print(f"  OK {key}: {info['filename']} ({actual_size / (1024*1024):.1f} MB, {info.get('retrieved')})")
    return 0 if all_ok else 1


def main() -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--all", action="store_true", help="Download all sources")
    parser.add_argument("--sources", help="Comma-separated list of sources to download")
    parser.add_argument("--check", action="store_true", help="Check downloaded files against fetched.json")
    args = parser.parse_args()

    if args.check:
        sys.exit(check())

    if args.all:
        keys = list(SOURCES.keys())
    elif args.sources:
        keys = [s.strip() for s in args.sources.split(",")]
    else:
        parser.print_help()
        sys.exit(1)

    fetch(keys)


if __name__ == "__main__":
    main()
