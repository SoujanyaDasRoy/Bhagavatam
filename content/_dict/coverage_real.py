#!/usr/bin/env python3
"""Dictionary coverage of the book measured with the app's own lookup order (Dictionary.kt lookup, steps 1 to 5).

    py content/_dict/coverage_real.py [path/to/dictionary.db]

For every word of every verse (hi, bn, or, en, and the Sanskrit shloka looked up as Hindi, as the app does) the same steps run:
  1 exact entry   2 inflected form   3 stem (Lemmatizer endings)   4 chandrabindu / nukta variants   5 same word in another script (skey)
Words marked "[?]" in the Odia text are left out (they are doubtful, and the app does not look them up as certain).
Prints tokens covered and distinct words covered per language, and writes the most frequent words with no entry to
content/_dict/work/uncovered_<lang>.tsv (git-ignored: derived from the Gita Press text).
"""
import collections
import re
import sqlite3
import sys
import unicodedata
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
WORK = HERE / "work"
sys.stdout.reconfigure(encoding="utf-8")

SUFFIX = {
    "bn": ["গুলোর", "গুলো", "দের", "েরা", "ের", "কে", "তে", "েই", "টি", "রা", "র", "ে", "ই", "ও"],
    "hi": ["ाओं", "ओं", "ें", "ों", "ने", "को", "का", "की", "के", "से", "में", "पर", "ा", "ी", "े"],
    "or": ["ମାନଙ୍କର", "ମାନଙ୍କୁ", "ମାନେ", "ଙ୍କର", "ଙ୍କୁ", "ଙ୍କ", "ରେ", "କୁ", "ର", "ଟି"],
    "en": ["ing", "ed", "es", "ly", "s"],
}
WORD = {
    "hi": re.compile(r"[ऀ-ॿ]+"),
    "bn": re.compile(r"[ঀ-৿]+"),
    "or": re.compile(r"[଀-୿]+"),
    "en": re.compile(r"[A-Za-z]+(?:'[A-Za-z]+)?"),
}
INDIC = re.compile(r"[ऀ-ॿঀ-৿଀-୿]")
TRIM = ".,;:!?\"'‘’“”()[]{}।॥-—–"


def nfc(s):
    return unicodedata.normalize("NFC", s)


def to_deva(w):
    out = []
    for ch in w:
        c = ord(ch)
        out.append(chr(c - 0x80) if 0x0980 <= c <= 0x09FF else chr(c - 0x200) if 0x0B00 <= c <= 0x0B7F else ch)
    return "".join(out)


def clean_word(w, lang):
    w = nfc(w.strip().strip(TRIM))
    return w.lower() if lang == "en" or not INDIC.search(w) else w


def stems(w, lang):
    out = []
    for suf in sorted(SUFFIX.get(lang, []), key=len, reverse=True):
        if len(w) > len(suf) + 1 and w.endswith(suf):
            st = w[:-len(suf)]
            out.append(st)
            if lang == "bn" and st.endswith("ত"):
                out.append(st[:-1] + "ৎ")
    return list(dict.fromkeys(out))


def fold_cb(w):
    return nfc(w).replace("ँ", "ं").replace("ঁ", "ং").replace("ଁ", "ଂ")


def strip_nukta(w):
    return nfc(unicodedata.normalize("NFD", w).replace("़", "").replace("়", "").replace("଼", ""))


def main():
    db_path = sys.argv[1] if len(sys.argv) > 1 else str(ROOT / "app/src/main/assets/dictionary.db")
    d = sqlite3.connect(db_path)
    by_norm, by_form, by_skey = set(), set(), collections.defaultdict(set)
    for lang, norm, head, skey in d.execute("select lang, norm, headword, skey from entry"):
        by_norm.add((lang, norm)); by_norm.add((lang, head)); by_skey[skey].add(lang)
    for lang, fnorm in d.execute("select e.lang, f.form_norm from form f join entry e on e.id = f.entry_id"):
        by_form.add((lang, fnorm))
    c = sqlite3.connect(str(ROOT / "content/content.db"))
    WORK.mkdir(exist_ok=True)

    def covered(w, lang):
        if (lang, w) in by_norm or (lang, w) in by_form:
            return True
        for st in stems(w, lang):
            if (lang, st) in by_norm or (lang, st) in by_form:
                return True
        if INDIC.search(w):
            for v in (fold_cb(w), strip_nukta(w)):
                if v != w and ((lang, v) in by_norm or (lang, v) in by_form):
                    return True
            if any(l != lang for l in by_skey.get(to_deva(w).lower(), ())):
                return True
        return False

    print(f"{'language':<10}{'tokens':>10}{'covered':>10}{'distinct':>10}{'covered':>10}   (dictionary: {db_path})")
    for name, col, lcode in (("English", "en", "en"), ("Hindi", "hi", "hi"), ("Bengali", "bn", "bn"), ("Odia", '"or"', "or"), ("Sanskrit", "sa", "hi")):
        pat = WORD["en" if name == "English" else lcode]
        cnt = collections.Counter()
        for (t,) in c.execute(f"select {col} from verse"):
            if not t:
                continue
            t = re.sub(r"\S*\[\?\]", " ", t) if name == "Odia" else t
            for w in pat.findall(t.replace("।", " ").replace("॥", " ")):
                w = clean_word(w, lcode)
                if len(w) >= 2:
                    cnt[w] += 1
        hit = {w: covered(w, lcode) for w in cnt}
        tot = sum(cnt.values())
        got = sum(n for w, n in cnt.items() if hit[w])
        dd = sum(1 for w in hit if hit[w])
        print(f"{name:<10}{tot:>10,}{100 * got / max(1, tot):>9.1f}%{len(cnt):>10,}{100 * dd / max(1, len(cnt)):>9.1f}%")
        miss = sorted(((n, w) for w, n in cnt.items() if not hit[w]), reverse=True)
        (WORK / f"uncovered_{name.lower()}.tsv").write_text("\n".join(f"{w}\t{n}" for n, w in miss[:5000]), encoding="utf-8")
    c.close()


if __name__ == "__main__":
    main()
