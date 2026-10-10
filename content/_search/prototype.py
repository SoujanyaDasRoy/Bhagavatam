#!/usr/bin/env python3
"""Prototype of the planned search index (research for docs/superpowers/plans/2026-10-09-search.md).

It builds, in memory, from content/content.db:
  - per-language word indexes (word -> verses) with light normalisation
  - a cross-script "skeleton" key, so that Roman 'krishna', Devanagari 'कृष्ण', Bengali 'কৃষ্ণ' and IAST 'kṛṣṇa' all meet
and then runs the queries that fail in the app today, plus an estimate of the index size.

    python content/_search/prototype.py

This is research code: the real indexer is Task 2 of the plan. Nothing here is imported by the app.
"""
import collections
import re
import sqlite3
import sys
import time
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
sys.stdout.reconfigure(encoding="utf-8")

# ---------------------------------------------------------------- normalisation
JOINERS = dict.fromkeys(map(ord, "‌‍"), None)


def nfc(w: str) -> str:
    return unicodedata.normalize("NFC", w).translate(JOINERS)


SCRIPTS = {"deva": (0x0900, 0x097F), "bn": (0x0980, 0x09FF), "or": (0x0B00, 0x0B7F)}
WORD = {
    "hi": re.compile(r"[ऀ-ॿ]+"),
    "sa": re.compile(r"[ऀ-ॿ]+"),
    "bn": re.compile(r"[ঀ-৿]+"),
    "or": re.compile(r"[଀-୿]+"),
    "en": re.compile(r"[A-Za-zÀ-ɏḀ-ỿ'’]+"),
    "iast": re.compile(r"[A-Za-zÀ-ɏḀ-ỿ]+"),
}


def to_deva(w: str) -> str:
    """Bengali and Odia letters laid out like Devanagari: shift them into the Devanagari block (the two scripts are parallel)."""
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


# Devanagari consonants -> a folded Roman consonant (aspirates, retroflexes, sibilants and nasals merged on purpose)
DEVA_CONS = {}
for roman, letters in {
    "k": "कखक़ख़", "g": "गघग़", "n": "ङञणनऩमंँ", "c": "चछ", "j": "जझज़यय़झ", "t": "टठतथ", "d": "डढदधड़ढ़", "p": "पफफ़",
    "b": "बभवव", "r": "रऱऋॠृॄ", "l": "लळऌ", "s": "शषसश", "h": "हः", "f": "",
}.items():
    for ch in letters:
        DEVA_CONS[ch] = roman


def skeleton_from_indic(word: str) -> str:
    """Consonant skeleton of an Indic word: vowels, viramas and length marks are dropped; similar consonants are merged."""
    w = to_deva(nfc(word))
    return "".join(DEVA_CONS.get(ch, "") for ch in w)


def skeleton_from_roman(word: str) -> str:
    """Same skeleton from Roman text (plain English spelling or IAST): fold diacritics, merge digraphs, drop vowels."""
    w = unicodedata.normalize("NFD", word.lower())
    # keep dot-below r and l as consonants (ṛ, ṝ, ḷ) before stripping marks
    w = w.replace("ṛ", "r").replace("ṝ", "r").replace("ḷ", "l")
    w = "".join(ch for ch in w if not unicodedata.combining(ch))
    w = re.sub(r"[^a-z]", "", w)
    w = re.sub(r"(?<=[kgcjtdpb])h", "", w)         # aspirates: kh gh ch jh th dh ph bh
    w = w.replace("sh", "s").replace("ng", "n").replace("ny", "n").replace("ri", "r")
    w = w.replace("w", "b").replace("v", "b").replace("y", "j").replace("z", "j").replace("q", "k").replace("x", "ks")
    w = re.sub(r"[mn]", "n", w)
    w = w.replace("th", "t").replace("sh", "s")
    w = re.sub(r"[aeiouh]", "", w)                  # vowels and any remaining h
    return w


# ---------------------------------------------------------------- build
def build():
    db = sqlite3.connect(ROOT / "content" / "content.db")
    idx = {k: collections.defaultdict(set) for k in ("hi", "bn", "en", "sa")}
    skel = collections.defaultdict(set)   # skeleton -> verse ids (any language)
    rows = db.execute('select rowid, skandha, chapter, num, sa, iast, hi, en, "bn" from verse').fetchall()
    for vid, s, c, n, sa, iast, hi, en, bn in rows:
        for lang, text in (("hi", hi), ("bn", bn), ("en", en), ("sa", sa)):
            if not text:
                continue
            for w in WORD[lang].findall(text.replace("।", " ").replace("॥", " ")):
                w = nfc(w).lower().strip("'’")
                if len(w) < 2:
                    continue
                idx[lang][w].add(vid)
                k = skeleton_from_indic(w) if lang != "en" else skeleton_from_roman(w)
                if len(k) >= 2:
                    skel[k].add(vid)
        if iast:
            for w in WORD["iast"].findall(iast):
                k = skeleton_from_roman(w)
                if len(k) >= 2:
                    skel[k].add(vid)
    return rows, idx, skel


def search_native(idx, lang, query):
    """AND of the words of the query, within one language."""
    words = [nfc(w).lower() for w in WORD[lang].findall(query)]
    if not words:
        return set()
    sets = [idx[lang].get(w, set()) for w in words]
    return set.intersection(*sets) if sets else set()


def search_skeleton(skel, query):
    parts = query.split()
    sets = []
    for p in parts:
        k = skeleton_from_indic(p) if re.search(r"[ऀ-ॿঀ-৿଀-୿]", p) else skeleton_from_roman(p)
        sets.append(skel.get(k, set()))
    return set.intersection(*sets) if sets else set()


def main():
    t = time.time()
    rows, idx, skel = build()
    print(f"built in {time.time() - t:.1f}s: verses {len(rows):,}")
    for lang, m in idx.items():
        postings = sum(len(v) for v in m.values())
        print(f"  {lang}: {len(m):,} distinct words, {postings:,} (word, verse) pairs")
    total_pairs = sum(len(v) for m in idx.values() for v in m.values()) + sum(len(v) for v in skel.values())
    # rough size: each pair as a delta-coded varint is about 1.6 bytes; as a table row about 9 bytes
    print(f"  skeletons: {len(skel):,} keys, {sum(len(v) for v in skel.values()):,} pairs")
    print(f"  size estimate: about {total_pairs * 1.6 / 1e6:.1f} MB as compressed posting lists, about {total_pairs * 9 / 1e6:.0f} MB as plain table rows")

    print("\n== queries that fail in the app today")
    demos = [
        ("en", "Krishna", "skel"), ("en", "Gajendra", "skel"), ("en", "crocodile elephant", "native"), ("en", "elephant crocodile", "native"),
        ("hi", "हाथी मगर", "native"), ("bn", "হাতি কুমির", "native"), ("en", "krishna", "skel"), ("hi", "कृष्ण", "skel"), ("bn", "কৃষ্ণ", "skel"),
        ("en", "yudhishthira", "skel"), ("en", "bhagavan", "skel"), ("en", "dharma", "skel"),
    ]
    for lang, q, mode in demos:
        r = search_skeleton(skel, q) if mode == "skel" else search_native(idx, lang, q)
        print(f"  [{mode:6}] {q!r:26} -> {len(r):5} verses")
    print("\n== does a Roman query reach the Devanagari/Bengali text? (verses containing the name in each language)")
    for q in ("krishna", "gajendra", "yudhishthira", "narada"):
        k = skeleton_from_roman(q)
        per = {lang: sum(1 for w, vs in idx[lang].items() if skeleton_from_indic(w) == k for _ in [0] for _ in [0]) for lang in ("hi", "bn")}
        sample = [w for w in idx["hi"] if skeleton_from_indic(w) == k][:4], [w for w in idx["bn"] if skeleton_from_indic(w) == k][:4]
        print(f"  {q:14} skeleton {k!r:10} hindi forms {sample[0]} | bengali forms {sample[1]}")
    print("\n== how many different words share one skeleton? (collisions)")
    by = collections.defaultdict(set)
    for lang in ("hi", "bn"):
        for w in idx[lang]:
            by[skeleton_from_indic(w)].add(w)
    sizes = collections.Counter(min(len(v), 6) for v in by.values())
    print(f"  Hindi+Bengali words per skeleton: {dict(sorted(sizes.items()))} (6 means 6 or more)")


# ---------------------------------------------------------------- stricter key: "loose phonetic" (vowels kept, lengths merged)
CONS = {}
for roman, letters in {
    "k": "कखक़ख़", "g": "गघग़", "n": "ङञणनऩमं", "c": "चछ", "j": "जझज़यय़", "t": "टठतथ", "d": "डढदधड़ढ़", "p": "पफफ़",
    "b": "बभवव", "r": "रऱ", "l": "लळ", "s": "शषस", "h": "ह",
}.items():
    for ch in letters:
        CONS[ch] = roman
VOWEL_SIGN = {"ा": "a", "ि": "i", "ी": "i", "ु": "u", "ू": "u", "ृ": "ri", "ॄ": "ri", "े": "e", "ै": "e", "ो": "o", "ौ": "o", "ॉ": "o", "ॅ": "e"}
VOWEL_IND = {"अ": "a", "आ": "a", "इ": "i", "ई": "i", "उ": "u", "ऊ": "u", "ऋ": "ri", "ॠ": "ri", "ए": "e", "ऐ": "e", "ओ": "o", "औ": "o", "ऑ": "o"}


def _tidy(s: str) -> str:
    s = re.sub(r"(.)\1+", r"\1", s)        # geminates and doubled vowels
    return re.sub(r"a$", "", s) or s       # final inherent a is optional in speech and in spelling


def loose_from_indic(word: str) -> str:
    w = to_deva(nfc(word)).replace("ँ", "ं")
    out, i, n = [], 0, len(w)
    while i < n:
        ch = w[i]
        if ch in CONS:
            out.append(CONS[ch])
            nxt = w[i + 1] if i + 1 < n else ""
            if nxt == "्":
                i += 1
            elif nxt in VOWEL_SIGN:
                out.append(VOWEL_SIGN[nxt]); i += 1
            else:
                out.append("a")
        elif ch in VOWEL_IND:
            out.append(VOWEL_IND[ch])
        elif ch == "ं":
            out.append("n")
        i += 1
    return _tidy("".join(out))


def loose_from_roman(word: str) -> str:
    w = unicodedata.normalize("NFD", word.lower())
    w = w.replace("r\u0323\u0304", "ri").replace("r\u0323", "ri").replace("l\u0323", "li")
    w = "".join(ch for ch in w if not unicodedata.combining(ch))
    w = re.sub(r"[^a-z]", "", w)
    w = re.sub(r"(?<=[kgcjtdpb])h", "", w)
    w = w.replace("sh", "s").replace("ng", "n").replace("ny", "n")
    w = w.replace("w", "b").replace("v", "b").replace("y", "j").replace("z", "j").replace("q", "k").replace("x", "ks").replace("f", "p")
    w = re.sub(r"[mn]", "n", w)
    w = w.replace("ai", "e").replace("au", "o").replace("ee", "i").replace("oo", "u")
    return _tidy(w)


def build_loose():
    db = sqlite3.connect(ROOT / "content" / "content.db")
    loose = collections.defaultdict(set)
    forms = collections.defaultdict(collections.Counter)
    for vid, sa, iast, hi, en, bn in db.execute('select rowid, sa, iast, hi, en, "bn" from verse'):
        for lang, text in (("hi", hi), ("bn", bn), ("sa", sa)):
            for w in WORD[lang].findall((text or "").replace("\u0964", " ").replace("\u0965", " ")):
                k = loose_from_indic(w)
                if len(k) >= 3:
                    loose[k].add(vid); forms[k][nfc(w)] += 1
        for text in (iast, en):
            for w in WORD["iast"].findall(text or ""):
                k = loose_from_roman(w)
                if len(k) >= 3:
                    loose[k].add(vid); forms[k][w] += 1
    return loose, forms


def compare_keys():
    loose, forms = build_loose()
    print("\n== stricter key (vowels kept): verses and the words that matched")
    for q in ("krishna", "kunti", "rasa", "parikshit", "narada", "gajendra", "yudhishthira", "prahlada", "arjuna", "hari"):
        k = loose_from_roman(q)
        hit = loose.get(k, set())
        top = [w for w, _ in forms[k].most_common(5)]
        print(f"  {q:14} key {k!r:12} {len(hit):5} verses | forms: {top}")
    print("  must match:", [(a, b, loose_from_roman(a) == loose_from_indic(b)) for a, b in (("krishna", "कृष्ण"), ("krishna", "কৃষ্ণ"), ("yudhishthira", "युधिष्ठिर"), ("narada", "नारद"), ("shri", "श्री"), ("rishi", "ऋषि"))])
    print("  must NOT match:", [(a, b, loose_from_roman(a) == loose_from_indic(b)) for a, b in (("krishna", "आकर्षण"), ("narada", "निरोध"), ("kunti", "कान्त"))])
    sizes = collections.Counter(min(len(v), 6) for v in forms.values())
    print(f"  distinct forms per key (collisions): {dict(sorted(sizes.items()))} (6 means 6 or more)")


if __name__ == "__main__":
    main()
    compare_keys()
