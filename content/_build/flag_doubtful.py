#!/usr/bin/env python3
"""Find words in the Hindi, Bengali and English texts that may be misread, the same idea as the red-underlined Odia words.

    py content/_build/flag_doubtful.py content/content.db [--dry-run] [--show N]

A word is flagged when it is rare in the book (at most MAX_COUNT times) AND one letter-edit away from a word that is common
(at least MIN_NEIGHBOUR times) and is not itself a known dictionary form. That is the shape of an OCR slip: a rare word that is
almost a frequent word. Names and Sanskrit words in a translation that are simply rare are NOT flagged (no frequent neighbour).
Writes table doubt(verse_id, lang, word) in the database (replaced on every run). Run it on both copies of content.db.
"""
import collections
import re
import sqlite3
import sys
import unicodedata

MAX_COUNT = 2
MIN_NEIGHBOUR = 25
MIN_LEN = {"hi": 4, "bn": 4, "en": 5}
# Particles and case endings that attach to a word: "भागवतके", "ভাগবতই", "ঋষিগণও" are spellings, not slips.
ATTACH = {
    "hi": ["के", "की", "का", "को", "में", "मे", "से", "ने", "पर", "ही", "भी", "तो", "तक", "सा", "सी", "से", "ोंके", "ोंकी", "ोंका", "ोंको", "ोंमें", "ोंसे", "ोंने", "ों", "ें", "ो", "े", "ी", "ा", "ें", "जी", "गण", "रूप", "मय", "वत्", "वान्", "युक्त", "हीन", "रहित"],
    "bn": ["ই", "ও", "ের", "েরই", "েও", "কে", "কেই", "তে", "তেই", "র", "রই", "রও", "রা", "দের", "ে", "েই", "টি", "টা", "গণ", "গণের", "গুলি", "গুলো", "ময়", "রূপ", "হীন", "যুক্ত", "মাত্র", "ন", "েন", "নি", "ছে", "ল", "লে", "ও"],
    "en": [],
}
WORD = {"hi": re.compile(r"[ऀ-ॿ]+"), "bn": re.compile(r"[ঀ-৿]+"), "en": re.compile(r"[A-Za-zÀ-ɏ]+")}


def nfc(s):
    return unicodedata.normalize("NFC", s)


def neighbours(w, alphabet):
    """All strings one edit away (delete, substitute, insert, transpose)."""
    out = set()
    for i in range(len(w) + 1):
        if i < len(w):
            out.add(w[:i] + w[i + 1:])
            for a in alphabet:
                out.add(w[:i] + a + w[i + 1:])
        for a in alphabet:
            out.add(w[:i] + a + w[i:])
        if i + 1 < len(w):
            out.add(w[:i] + w[i + 1] + w[i] + w[i + 2:])
    out.discard(w)
    return out


def main():
    path = sys.argv[1]
    dry = "--dry-run" in sys.argv
    show = int(sys.argv[sys.argv.index("--show") + 1]) if "--show" in sys.argv else 0
    db = sqlite3.connect(path)
    dic = set()
    try:
        d = sqlite3.connect(path.replace("content/content.db", "app/src/main/assets/dictionary.db")) if "content/content.db" in path else None
        if d:
            dic = {nfc(r[0]).lower() for r in d.execute("select form_norm from form")} | {nfc(r[0]).lower() for r in d.execute("select norm from entry")}
    except Exception:
        pass
    flagged = []
    for lang in ("hi", "bn", "en"):
        cnt = collections.Counter()
        where = collections.defaultdict(set)
        for vid, text in db.execute(f"select rowid, {lang} from verse"):
            for w in WORD[lang].findall(text or ""):
                w = nfc(w).lower() if lang == "en" else nfc(w)
                cnt[w] += 1
                where[w].add(vid)
        alphabet = sorted({ch for w in cnt for ch in w if cnt[w] >= MIN_NEIGHBOUR})
        n = 0
        for w, c in cnt.items():
            if c > MAX_COUNT or len(w) < MIN_LEN[lang] or w in dic:
                continue
            if any(w.endswith(a) and len(w) > len(a) + 1 and (cnt.get(w[:-len(a)], 0) >= 3 or w[:-len(a)] in dic) for a in ATTACH[lang]):
                continue
            near = [x for x in neighbours(w, alphabet) if cnt.get(x, 0) >= MIN_NEIGHBOUR]
            # an affix of one or two letters on a common word (अकुशल, দণ্ডক, বিহ্বলা) is a word, not a slip; a TRUNCATED word (विशुद्) is kept
            near = [x for x in near if not ((w.endswith(x) or w.startswith(x)) and 0 < len(w) - len(x) <= 2)]
            if not near:
                continue
            n += 1
            best = max(near, key=lambda x: cnt[x])
            for vid in where[w]:
                flagged.append((vid, lang, w, best))
            if show and n <= show:
                print(f"{lang}  {w} ({c})  ~  {best} ({cnt[best]})")
        print(f"{lang}: {n} words flagged in {sum(1 for f in flagged if f[1] == lang)} verses")
    if dry:
        return
    db.execute("DROP TABLE IF EXISTS doubt")
    db.execute("CREATE TABLE doubt(verse_id INTEGER, lang TEXT, word TEXT)")
    db.executemany("INSERT INTO doubt VALUES (?,?,?)", [(v, l, w) for v, l, w, _ in flagged])
    db.execute("CREATE INDEX doubt_verse ON doubt(verse_id)")
    db.commit()
    db.close()


if __name__ == "__main__":
    main()
