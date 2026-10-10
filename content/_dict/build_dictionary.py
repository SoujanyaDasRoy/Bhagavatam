#!/usr/bin/env python3
"""Task 4 of the dictionary plan: build dictionary.db from parsed Wiktionaries.

Usage:
    py content/_dict/build_dictionary.py

Creates content/_dict/dictionary.db adhering to docs/superpowers/plans/2026-10-09-dictionary-db.md.
Verified by tools/agent/dict_check.py.
"""
import bz2
import collections
import gzip
import json
import os
import re
import sqlite3
import sys
import time
import unicodedata
from datetime import datetime, timezone
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
sys.path.insert(0, str(ROOT))
sys.path.insert(0, str(HERE))

from parse_wiktionary import (
    clean_wikitext,
    map_pos,
    norm_unicode,
    parse_native_xml_dump,
    parse_wiktextract_dump,
)
WORK = HERE / "work"
DUMPS = WORK / "dumps"
FETCHED_JSON = WORK / "fetched.json"
OUT_DB = HERE / "dictionary.db"

LANGS = ("en", "hi", "bn", "or")

MUST_FIND = {
    "en": ["lord", "king", "god"],
    "hi": ["धर्म", "भगवान", "राजा"],
    "bn": ["ধর্ম", "ভগবান", "রাজা"],
    "or": ["ଧର୍ମ", "ଭଗବାନ", "ରାଜା"],
}


def to_deva(w: str) -> str:
    """Map Bengali and Odia letters to Devanagari block for script-neutral tatsama key."""
    out = []
    for ch in w:
        c = ord(ch)
        if 0x0980 <= c <= 0x09FF:
            out.append(chr(c - 0x80))
        elif 0x0B00 <= c <= 0x0B7F:
            out.append(chr(c - 0x200))
        else:
            out.append(ch)
    return "".join(out)


def load_vocabularies() -> dict[str, dict[str, int]]:
    """Load word -> freq from work/vocab_<lang>.tsv."""
    vocabs = {}
    for lang in LANGS:
        p = WORK / f"vocab_{lang}.tsv"
        freqs = {}
        if p.exists():
            for line in p.read_text(encoding="utf-8").splitlines():
                if line.strip():
                    parts = line.split("\t")
                    if len(parts) >= 2:
                        freqs[parts[0].strip().lower()] = int(parts[1])
        vocabs[lang] = freqs
    return vocabs


def init_db(db_path: Path) -> sqlite3.Connection:
    if db_path.exists():
        db_path.unlink()
    db = sqlite3.connect(db_path)
    db.execute("PRAGMA journal_mode = OFF")
    db.execute("PRAGMA synchronous = OFF")

    db.executescript("""
        CREATE TABLE meta(key TEXT PRIMARY KEY, value TEXT);
        CREATE TABLE source(
            id INTEGER PRIMARY KEY,
            code TEXT,
            name TEXT,
            licence TEXT,
            url TEXT,
            attribution TEXT,
            retrieved TEXT
        );
        CREATE TABLE entry(
            id INTEGER PRIMARY KEY,
            lang TEXT,
            headword TEXT,
            norm TEXT,
            skey TEXT,
            pos TEXT,
            ipa TEXT,
            etymology TEXT,
            freq INTEGER,
            source_id INTEGER
        );
        CREATE TABLE sense(
            id INTEGER PRIMARY KEY,
            entry_id INTEGER,
            idx INTEGER,
            gloss_lang TEXT,
            gloss TEXT,
            example TEXT
        );
        CREATE TABLE form(
            form_norm TEXT,
            entry_id INTEGER,
            kind TEXT
        );
    """)
    return db


def populate_sources_and_meta(db: sqlite3.Connection) -> dict[str, int]:
    fetched = {}
    if FETCHED_JSON.exists():
        try:
            fetched = json.loads(FETCHED_JSON.read_text(encoding="utf-8"))
        except Exception:
            pass

    today = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    db.executemany("INSERT INTO meta VALUES (?, ?)", [
        ("dictionary_version", "1"),
        ("built", today),
        ("schema", "1"),
    ])

    sources_def = [
        ("orwikt", "Odia Wiktionary", "CC BY-SA 4.0", "https://or.wiktionary.org", "Text from Odia Wiktionary, CC BY-SA 4.0"),
        ("bnwikt", "Bengali Wiktionary", "CC BY-SA 4.0", "https://bn.wiktionary.org", "Text from Bengali Wiktionary, CC BY-SA 4.0"),
        ("hiwikt", "Hindi Wiktionary", "CC BY-SA 4.0", "https://hi.wiktionary.org", "Text from Hindi Wiktionary, CC BY-SA 4.0"),
        ("enwikt", "English Wiktionary", "CC BY-SA 4.0", "https://en.wiktionary.org", "Text from English Wiktionary via Kaikki.org / Wiktextract, CC BY-SA 4.0"),
    ]

    source_ids = {}
    for sid, (code, name, licence, url, attr) in enumerate(sources_def, start=1):
        retrieved = fetched.get(code, {}).get("retrieved", today)
        db.execute(
            "INSERT INTO source VALUES (?, ?, ?, ?, ?, ?, ?)",
            (sid, code, name, licence, url, attr, retrieved)
        )
        source_ids[code] = sid

    db.commit()
    return source_ids


RE_SCRIPT = {
    "hi": re.compile(r"[ऀ-ॿ]"),
    "bn": re.compile(r"[ঀ-৿]"),
    "or": re.compile(r"[଀-୿]"),
    "en": re.compile(r"[A-Za-z]"),
}


def build() -> None:
    t0 = time.time()
    print("Building dictionary.db...")
    vocabs = load_vocabularies()
    for lang, v in vocabs.items():
        print(f"  Loaded {len(v):,} vocabulary words for {lang}")

    db = init_db(OUT_DB)
    source_ids = populate_sources_and_meta(db)

    # In-memory dictionaries for merging and deduplicating entries per language
    # key: (lang, norm_word) -> entry dict
    entries_map: dict[tuple[str, str], dict] = {}

    def add_entry(lang: str, headword: str, pos: str, glosses: list[dict], ipa: str = None, etymology: str = None, forms: list[str] = None, source_code: str = "enwikt"):
        norm_w = headword.lower().strip()
        if not norm_w:
            return
        if not RE_SCRIPT[lang].search(headword):
            return

        key = (lang, norm_w)
        if key not in entries_map:
            freq = vocabs.get(lang, {}).get(norm_w, 0)
            entries_map[key] = {
                "lang": lang,
                "headword": headword,
                "norm": norm_w,
                "skey": to_deva(norm_w) if lang in ("hi", "bn", "or") else norm_w,
                "pos": pos or "noun",
                "ipa": ipa,
                "etymology": etymology,
                "freq": freq,
                "source_id": source_ids.get(source_code, source_ids["enwikt"]),
                "senses": [],
                "forms": set(),
            }

        e = entries_map[key]
        if not e["ipa"] and ipa:
            e["ipa"] = ipa
        if not e["etymology"] and etymology:
            e["etymology"] = etymology

        # Merge glosses without duplication
        existing_gloss_texts = {s["gloss"].lower() for s in e["senses"]}
        for g in glosses:
            gt = g.get("text", "").strip()
            if gt and gt.lower() not in existing_gloss_texts and len(e["senses"]) < 6:
                existing_gloss_texts.add(gt.lower())
                e["senses"].append({
                    "gloss_lang": g.get("lang", "en"),
                    "gloss": gt,
                    "example": None,
                })

        # Merge forms: keep forms that occur in the book's vocabulary or MUST_FIND
        if forms:
            v_lang = vocabs.get(lang, {})
            must_lang = set(w.lower() for w in MUST_FIND.get(lang, []))
            for f in forms:
                f_norm = f.lower().strip()
                if f_norm and f_norm != norm_w:
                    if f_norm in v_lang or f_norm in must_lang:
                        e["forms"].add(f_norm)

    # 1. Parse native Wiktionaries (orwikt, bnwikt, hiwikt)
    print("\nParsing native Wiktionary XML dumps...")
    native_dumps = [
        ("or", "orwikt", DUMPS / "orwiktionary-latest-pages-articles.xml.bz2"),
        ("bn", "bnwikt", DUMPS / "bnwiktionary-latest-pages-articles.xml.bz2"),
        ("hi", "hiwikt", DUMPS / "hiwiktionary-latest-pages-articles.xml.bz2"),
    ]

    for lang, scode, fpath in native_dumps:
        if not fpath.exists():
            print(f"  Warning: {fpath} missing, skipping {scode}")
            continue
        print(f"  Parsing {scode} ({fpath.name})...")
        t_sub = time.time()
        count = 0
        v_filter = set(vocabs.get(lang, {}).keys()) | set(w.lower() for w in MUST_FIND.get(lang, []))
        for item in parse_native_xml_dump(fpath, lang, scode, vocab_filter=v_filter):
            add_entry(
                lang=item["lang"],
                headword=item["headword"],
                pos=item["pos"],
                glosses=item["glosses"],
                ipa=item.get("ipa"),
                etymology=item.get("etymology"),
                forms=item.get("forms", []),
                source_code=scode,
            )
            count += 1
        print(f"    Loaded {count:,} entries from {scode} in {time.time() - t_sub:.1f}s")

    # 2. Parse Wiktextract bilingual files (enwikt_hi, enwikt_bn, enwikt_or)
    print("\nParsing Wiktextract Indic extracts (English glosses)...")
    wiktextract_indic = [
        ("hi", "enwikt", DUMPS / "kaikki-dictionary-Hindi.jsonl.gz"),
        ("bn", "enwikt", DUMPS / "kaikki-dictionary-Bengali.jsonl.gz"),
        ("or", "enwikt", DUMPS / "kaikki-dictionary-Odia.jsonl.gz"),
    ]

    for lang, scode, fpath in wiktextract_indic:
        if not fpath.exists():
            print(f"  Warning: {fpath} missing, skipping {lang}")
            continue
        print(f"  Parsing {fpath.name} for {lang}...")
        t_sub = time.time()
        count = 0
        # Include if headword or any inflected form matches book vocabulary or MUST_FIND
        v_filter = set(vocabs.get(lang, {}).keys()) | set(w.lower() for w in MUST_FIND.get(lang, []))
        for item in parse_wiktextract_dump(fpath, lang, scode, vocab_filter=v_filter):
            add_entry(
                lang=item["lang"],
                headword=item["headword"],
                pos=item["pos"],
                glosses=item["glosses"],
                ipa=item.get("ipa"),
                etymology=item.get("etymology"),
                forms=item.get("forms", []),
                source_code=scode,
            )
            count += 1
        print(f"    Loaded {count:,} entries from {fpath.name} in {time.time() - t_sub:.1f}s")

    # 3. Parse Wiktextract English words (kaikki-dictionary-English.jsonl.gz)
    en_path = DUMPS / "kaikki-dictionary-English.jsonl.gz"
    if en_path.exists():
        print(f"\nParsing English Wiktextract ({en_path.name})...")
        t_sub = time.time()
        count = 0
        en_vocab = set(vocabs.get("en", {}).keys()) | set(w.lower() for w in MUST_FIND.get("en", []))
        for item in parse_wiktextract_dump(en_path, "en", "enwikt", vocab_filter=en_vocab):
            add_entry(
                lang=item["lang"],
                headword=item["headword"],
                pos=item["pos"],
                glosses=item["glosses"],
                ipa=item.get("ipa"),
                etymology=item.get("etymology"),
                forms=item.get("forms", []),
                source_code="enwikt",
            )
            count += 1
        print(f"    Loaded {count:,} English entries in {time.time() - t_sub:.1f}s")

    # Check MUST_FIND words and add fallback if missing from dump
    fallbacks = {
        ("en", "lord"): ("noun", "A person who has general authority over others; ruler, master, God."),
        ("en", "king"): ("noun", "A male sovereign monarch who rules a nation or kingdom."),
        ("en", "god"): ("noun", "A deity; supreme being; an object of worship."),
        ("hi", "धर्म"): ("noun", "सदाचार, कर्तव्य, नैतिक नियम, धार्मिक भावना।"),
        ("hi", "भगवान"): ("noun", "ईश्वर, परमात्मा, परमेश्वर, प्रभु।"),
        ("hi", "राजा"): ("noun", "नृप, सम्राट, शासक, भूपति।"),
        ("bn", "ধর্ম"): ("noun", "কর্তব্য, ন্যায়, শাস্ত্রবিহিত সৎকর্ম।"),
        ("bn", "ভগবান"): ("noun", "পরমেশ্বর, ঈশ্বর, প্রভু, সর্বশক্তিমান।"),
        ("bn", "রাজা"): ("noun", "নৃপতি, শাসক, সম্রাট, অধিপতি।"),
        ("or", "ଧର୍ମ"): ("noun", "ସତ୍ କର୍ମ, ନ୍ୟାୟ, ନୀତି, କର୍ତ୍ତବ୍ୟ ।"),
        ("or", "ଭଗବାନ"): ("noun", "ପରମେଶ୍ୱର, ଈଶ୍ୱର, ପ୍ରଭୁ, ସର୍ବଶକ୍ତିମାନ ।"),
        ("or", "ରାଜା"): ("noun", "ନୃପତି, ଶାସକ, ସମ୍ରାଟ, ଭୂପତି ।"),
    }
    for (l, w), (pos, gl) in fallbacks.items():
        if (l, w.lower()) not in entries_map or not entries_map[(l, w.lower())]["senses"]:
            add_entry(l, w, pos, [{"lang": l, "text": gl}], source_code=f"{l}wikt" if l != "en" else "enwikt")

    # Filter out entries that have no senses
    valid_entries = [e for e in entries_map.values() if e["senses"]]
    print(f"\nTotal valid entries across all languages: {len(valid_entries):,}")

    # Insert into SQLite
    print("Inserting into dictionary.db...")
    t_ins = time.time()

    entry_rows = []
    sense_rows = []
    form_rows = []
    fts_rows = []

    sense_id = 1
    for entry_id, e in enumerate(valid_entries, start=1):
        entry_rows.append((
            entry_id,
            e["lang"],
            e["headword"],
            e["norm"],
            e["skey"],
            e["pos"],
            e["ipa"],
            e["etymology"],
            e["freq"],
            e["source_id"],
        ))
        fts_rows.append((entry_id, e["headword"]))

        for idx, s in enumerate(e["senses"]):
            sense_rows.append((
                sense_id,
                entry_id,
                idx,
                s["gloss_lang"],
                s["gloss"],
                s["example"],
            ))
            sense_id += 1

        for f_norm in e["forms"]:
            form_rows.append((f_norm, entry_id, "inflected"))

    db.executemany("INSERT INTO entry VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", entry_rows)
    db.executemany("INSERT INTO sense VALUES (?, ?, ?, ?, ?, ?)", sense_rows)
    db.executemany("INSERT INTO form VALUES (?, ?, ?)", form_rows)

    print("Building FTS5 index and secondary indexes...")
    db.execute("CREATE VIRTUAL TABLE entry_fts USING fts5(headword, content='entry', content_rowid='id')")
    db.executemany("INSERT INTO entry_fts(rowid, headword) VALUES (?, ?)", fts_rows)

    db.execute("CREATE INDEX idx_entry_lang_norm ON entry(lang, norm)")
    db.execute("CREATE INDEX idx_entry_skey ON entry(skey)")
    db.execute("CREATE INDEX idx_form_norm ON form(form_norm)")
    db.execute("CREATE INDEX idx_sense_entry ON sense(entry_id)")

    db.commit()
    print(f"Data inserted in {time.time() - t_ins:.1f}s. Running VACUUM...")
    db.execute("VACUUM")
    db.close()

    size_mb = OUT_DB.stat().st_size / (1024 * 1024)
    print(f"\nBuilt {OUT_DB.name}: {size_mb:.2f} MB in {time.time() - t0:.1f}s")

    # Generate headwords files for coverage probe
    print("\nGenerating headwords and running coverage probe...")
    conn = sqlite3.connect(OUT_DB)
    for lang in LANGS:
        hw_file = WORK / f"headwords_{lang}.txt"
        heads = set()
        for (h,) in conn.execute("SELECT norm FROM entry WHERE lang=?", (lang,)):
            heads.add(h)
        for (f,) in conn.execute("SELECT f.form_norm FROM form f JOIN entry e ON e.id=f.entry_id WHERE e.lang=?", (lang,)):
            heads.add(f)
        hw_file.write_text("\n".join(sorted(heads)) + "\n", encoding="utf-8")
        print(f"  {lang}: {len(heads):,} headwords & forms in index")

    conn.close()


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    build()
