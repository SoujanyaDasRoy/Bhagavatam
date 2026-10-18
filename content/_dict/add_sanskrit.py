#!/usr/bin/env python3
"""Adds Sanskrit entries to dictionary.db from English Wiktionary's Sanskrit words (Wiktextract JSON, kaikki.org).

    py -I content/_dict/add_sanskrit.py [path/to/dictionary.db] [path/to/kaikki-sanskrit.jsonl]

Only the words that occur in the book are written: for every Sanskrit word of content/content.db the matching dictionary word is found (the
word itself, one of its declined or conjugated forms, or the word an inflected entry points to), and its English meanings, part of speech and
origin are stored as an entry with lang 'sa'. The word as it stands in the book is stored as a form of that entry, so the app finds it by
the tapped word. Licence: English Wiktionary, CC BY-SA 4.0 (source 4 of the database). Run again after the book text changes; earlier
Sanskrit entries are removed first. Then run add_book_words.py to refresh the vocabulary lists.
"""
import collections
import json
import re
import sqlite3
import sys
import unicodedata
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
sys.stdout.reconfigure(encoding="utf-8")
sys.path.insert(0, str(HERE))
import coverage_real as cr  # noqa: E402

SOURCE_ID = 4  # English Wiktionary (via Kaikki.org / Wiktextract)
MAX_SENSES = 6
ACCENTS = re.compile("[॑-॔᳐-᳿꣠-ꣿ]")
TRAILING_DIGITS = re.compile(r"[0-9०-९]+$")


def norm(w):
    w = unicodedata.normalize("NFC", w)
    w = ACCENTS.sub("", w)
    w = TRAILING_DIGITS.sub("", w)
    return w.replace("‌", "").replace("‍", "").strip()


def book_words():
    c = sqlite3.connect(str(ROOT / "content/content.db"))
    freq = collections.Counter()
    for (t,) in c.execute("select sa from verse where sa is not null"):
        for w in cr.WORD["hi"].findall(t.replace("।", " ").replace("॥", " ")):
            w = norm(cr.clean_word(w, "sa"))
            if len(w) >= 2:
                freq[w] += 1
    return freq


def gloss_of(sense):
    g = sense.get("glosses") or sense.get("raw_glosses") or []
    # project rule: no em or en dashes
    return re.sub(r"\s*[–—]\s*", " - ", (g[-1] if g else "").strip())


def main():
    db_path = Path(sys.argv[1]) if len(sys.argv) > 1 else HERE / "dictionary.db"
    src = Path(sys.argv[2]) if len(sys.argv) > 2 else HERE / "work/dumps/sanskrit_kaikki/kaikki-sanskrit.jsonl"
    vocab = book_words()
    print("book words:", len(vocab))

    lemmas = collections.defaultdict(list)   # headword -> [entry json]
    pointers = collections.defaultdict(set)  # inflected headword -> base words it points to
    by_form = collections.defaultdict(set)   # form -> lemma headwords
    n = 0
    with open(src, encoding="utf-8") as f:
        for line in f:
            n += 1
            e = json.loads(line)
            head = norm(e.get("word", ""))
            if not head or e.get("lang_code") not in (None, "sa"):
                continue
            senses = e.get("senses") or []
            bases = {norm(fo["word"]) for s in senses for fo in (s.get("form_of") or []) if fo.get("word")}
            real = [s for s in senses if gloss_of(s) and not s.get("form_of")]
            if bases and not real:
                pointers[head] |= bases
                continue
            if not real:
                continue
            lemmas[head].append(e)
            for fm in e.get("forms") or []:
                form = norm(fm.get("form", ""))
                if form and form != head and re.fullmatch(r"[ऀ-ॿ]+", form):
                    by_form[form].add(head)
    print("wiktionary lines:", n, "lemmas:", len(lemmas), "inflected entries:", len(pointers))

    def variants(w):
        """The word as printed, then the forms a dictionary lists: final anusvara for m, a voiced stop at the end of a word for the unvoiced one."""
        yield w
        if w.endswith("ं"):
            yield w[:-1] + "म्"
        for voiced, plain in (("द्", "त्"), ("ग्", "क्"), ("ब्", "प्"), ("ड्", "ट्")):
            if w.endswith(voiced):
                yield w[: -len(voiced)] + plain

    chosen = {}  # book word -> headword
    for w in vocab:
        hit = None
        for v in variants(w):
            if v in lemmas:
                hit = v
            elif v in by_form:
                hit = sorted(by_form[v])[0]
            elif v in pointers:
                for b in sorted(pointers[v]):
                    if b in lemmas:
                        hit = b
                        break
            if hit:
                break
        if hit:
            chosen[w] = hit
    heads = collections.defaultdict(list)
    for w, h in chosen.items():
        heads[h].append(w)
    print("book words found:", len(chosen), "of", len(vocab), f"({100 * len(chosen) / len(vocab):.1f}%)", "entries to add:", len(heads))

    d = sqlite3.connect(str(db_path))
    old = [r[0] for r in d.execute("select id from entry where lang='sa'")]
    if old:
        q = ",".join("?" * len(old))
        d.execute(f"delete from sense where entry_id in ({q})", old)
        d.execute(f"delete from form where entry_id in ({q})", old)
        d.execute("delete from entry where lang='sa'")
        print("removed earlier Sanskrit entries:", len(old))
    for head, words in heads.items():
        es = lemmas[head]
        pos = es[0].get("pos", "")
        eth = next((e.get("etymology_text") for e in es if e.get("etymology_text")), None)
        eth = re.sub(r"\s*[–—]\s*", " - ", re.sub(r"\s+", " ", eth)).strip()[:400] if eth else None
        freq = sum(vocab[w] for w in words)
        cur = d.execute(
            "INSERT INTO entry(lang, headword, norm, skey, pos, ipa, etymology, freq, source_id) VALUES('sa',?,?,?,?,?,?,?,?)",
            (head, head, cr.to_deva(head).lower(), pos, None, eth, freq, SOURCE_ID),
        )
        eid = cur.lastrowid
        seen, idx = set(), 0
        for e in es:
            for s in e.get("senses") or []:
                g = gloss_of(s)
                if not g or s.get("form_of") or g in seen:
                    continue
                seen.add(g)
                d.execute("INSERT INTO sense(entry_id, idx, gloss_lang, gloss, example) VALUES(?,?,?,?,NULL)", (eid, idx, "en", g[:300]))
                idx += 1
                if idx >= MAX_SENSES:
                    break
            if idx >= MAX_SENSES:
                break
        for w in words:
            if w != head:
                d.execute("INSERT INTO form(form_norm, entry_id, kind) VALUES(?,?,?)", (w, eid, "inflected"))
    d.commit()
    d.execute("VACUUM")
    d.close()
    print("size MB", round(db_path.stat().st_size / 1e6, 1))


if __name__ == "__main__":
    main()
