#!/usr/bin/env python3
"""Adds the book's own vocabulary to dictionary.db: every word of the Sanskrit, Hindi, English, Bengali and Odia text.

    py content/_dict/add_book_words.py [path/to/dictionary.db]      (default: content/_dict/dictionary.db)

The text is read from content/content.db (compiled from content/{hi,en,bn,or}). No meaning is written: only what the book shows.
  book_word(id, lang, word, freq, entry_id) every word that occurs twice or more (or has an entry), how often it occurs, and the entry it resolves to
                                           (same lookup order as the app: exact, inflected form, stem, chandrabindu/nukta, other script);
                                           entry_id is NULL for a word the dictionary has no sense for yet
  book_ref(word_id, ref)                   the first verses where the word occurs; ref = skandha*1000000 + chapter*1000 + verse
Run again after content.db or the dictionary changes: the two tables are rebuilt. Words marked [?] in the Odia text are left out.
"""
import collections
import re
import sqlite3
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
sys.path.insert(0, str(HERE))
sys.stdout.reconfigure(encoding="utf-8")
import coverage_real as cr  # noqa: E402

REFS = 3
REFS_NO_ENTRY = 1
COLS = (("sa", "sa", "sa"), ("hi", "hi", "hi"), ("en", "en", "en"), ("bn", "bn", "bn"), ("or", '"or"', "or"))


def main():
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else HERE / "dictionary.db"
    d = sqlite3.connect(out)
    by_norm, by_form, by_skey = {}, {}, {}
    for eid, lang, norm, head, skey in d.execute("select id, lang, norm, headword, skey from entry order by freq desc, id"):
        by_norm.setdefault((lang, norm), eid); by_norm.setdefault((lang, head), eid); by_skey.setdefault(skey, eid)
    for lang, fnorm, eid in d.execute("select e.lang, f.form_norm, f.entry_id from form f join entry e on e.id = f.entry_id"):
        by_form.setdefault((lang, fnorm), eid)

    def resolve(w, lang):
        for k in ((lang, w),):
            if k in by_norm: return by_norm[k]
            if k in by_form: return by_form[k]
        for st in cr.stems(w, lang):
            for m in (by_norm, by_form):
                if (lang, st) in m: return m[(lang, st)]
        if cr.INDIC.search(w):
            for v in (cr.fold_cb(w), cr.strip_nukta(w)):
                for m in (by_norm, by_form):
                    if v != w and (lang, v) in m: return m[(lang, v)]
            return by_skey.get(cr.to_deva(w).lower())
        return None

    c = sqlite3.connect(str(ROOT / "content/content.db"))
    d.executescript("DROP TABLE IF EXISTS book_word; DROP TABLE IF EXISTS book_ref;"
                    "CREATE TABLE book_word(id INTEGER PRIMARY KEY, lang TEXT, word TEXT, freq INTEGER, entry_id INTEGER);"
                    "CREATE TABLE book_ref(word_id INTEGER, ref INTEGER);")
    print(f"{'language':<10}{'words':>9}{'with entry':>12}{'without':>9}")
    for lang, col, look in COLS:
        pat = cr.WORD["hi" if lang == "sa" else lang]
        freq, refs = collections.Counter(), collections.defaultdict(list)
        for s, ch, n, t in c.execute(f"select skandha, chapter, num, {col} from verse"):
            if not t:
                continue
            if lang == "or":
                t = re.sub(r"\S*\[\?\]", " ", t)
            seen = set()
            for w in pat.findall(t.replace("।", " ").replace("॥", " ")):
                w = cr.clean_word(w, look)
                if len(w) < 2:
                    continue
                freq[w] += 1
                if w not in seen and len(refs[w]) < REFS:
                    refs[w].append((s, ch, n)); seen.add(w)
        rows, got = 0, 0
        for w, n in freq.items():
            eid = resolve(w, look) if lang != "sa" else (resolve(w, "sa") or resolve(w, "hi"))
            if n < 2 and eid is None:
                continue
            cur = d.execute("INSERT INTO book_word(lang, word, freq, entry_id) VALUES(?,?,?,?)", (lang, w, n, eid))
            keep = refs[w][:REFS if eid is not None else REFS_NO_ENTRY]
            d.executemany("INSERT INTO book_ref VALUES(?,?)", [(cur.lastrowid, s * 1000000 + ch * 1000 + num) for s, ch, num in keep])
            rows += 1
            got += eid is not None
        print(f"{lang:<10}{rows:>9,}{got:>12,}{rows - got:>9,}")
    d.executescript("CREATE INDEX IF NOT EXISTS book_word_k ON book_word(lang, word);"
                    "CREATE INDEX IF NOT EXISTS book_ref_k ON book_ref(word_id);")
    d.commit()
    d.execute("VACUUM")
    d.close()
    print("size MB", round(out.stat().st_size / 1e6, 1))


if __name__ == "__main__":
    main()
