#!/usr/bin/env python3
"""Does every word of every chapter markdown file have a meaning in the app's dictionary?

    py tools/agent/word_coverage.py [--list N]          (needs content/{hi,en,bn,or}/**/*.md and the built databases)

Words are taken from the body of each markdown file (front matter and HTML comments left out) exactly as the app finds them:
Devanagari words (the Hindi files hold the Sanskrit shloka too), Bengali, Odia, and English words with their diacritics.
A word HAS A MEANING when the app can show one: a dictionary entry by the app's lookup order (exact, inflected form, stem,
chandrabindu or nukta variant, other script) or a row in content.db's book_gloss with a matched word, parts, a stem that has a meaning, or the correction of a misprint
(a word that has only its verse as fallback does not count).
Prints distinct words and tokens per language, and writes the words with no meaning to content/_dict/work/md_missing_<lang>.tsv.
"""
import collections
import re
import sqlite3
import sys
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
sys.path.insert(0, str(ROOT / "content/_dict"))
sys.stdout.reconfigure(encoding="utf-8")
import coverage_real as cr  # noqa: E402

WORD = {
    "hi": re.compile(r"[ऀ-ॿ]+"),
    "bn": re.compile(r"[ঀ-৿]+"),
    "or": re.compile(r"[଀-୿]+"),
    "en": re.compile(r"[A-Za-zĀ-ſḀ-ỿ]+"),
}
DIGITS = re.compile(r"[०-९০-৯୦-୯৷]")
FRONT = re.compile(r"\A---\n.*?\n---\n", re.S)
COMMENT = re.compile(r"<!--.*?-->", re.S)
JOINERS = dict.fromkeys(map(ord, "‌‍"), None)


def words_of(path, lang):
    t = path.read_text(encoding="utf-8")
    t = FRONT.sub("", t)
    t = COMMENT.sub(" ", t)
    t = re.sub(r"\S*\[\?\]", " ", t)  # doubtful words are not looked up as certain
    t = unicodedata.normalize("NFC", t.translate(JOINERS))
    for w in WORD[lang].findall(t.replace("।", " ").replace("॥", " ")):
        w = w.lower() if lang == "en" else w
        if len(w) >= 2 and not DIGITS.search(w):
            yield w


def main():
    n_list = int(sys.argv[sys.argv.index("--list") + 1]) if "--list" in sys.argv else 15
    d = sqlite3.connect(ROOT / "app/src/main/assets/dictionary.db")
    by_norm, by_form, by_skey = set(), set(), collections.defaultdict(set)
    for lang, norm, head, skey in d.execute("select lang, norm, headword, skey from entry"):
        by_norm.add((lang, norm)); by_norm.add((lang, head)); by_skey[skey].add(lang)
    for lang, fnorm in d.execute("select e.lang, f.form_norm from form f join entry e on e.id = f.entry_id"):
        by_form.add((lang, fnorm))
    g = sqlite3.connect(ROOT / "app/src/main/assets/content.db")
    gloss = {(lg, w) for lg, w, hi, en, parts, stem, fix in g.execute("select lang, word, hi, en, parts, stem, fix from book_gloss") if hi or en or parts or stem or fix}

    def covered(w, lang):
        langs = ("sa", "hi") if lang == "hi" else (lang,)
        for L in langs:
            if (L, w) in by_norm or (L, w) in by_form:
                return True
            for st in cr.stems(w, L):
                if (L, st) in by_norm or (L, st) in by_form:
                    return True
            if cr.INDIC.search(w):
                for v in (cr.fold_cb(w), cr.strip_nukta(w)):
                    if v != w and ((L, v) in by_norm or (L, v) in by_form):
                        return True
                if any(x != L for x in by_skey.get(cr.to_deva(w).lower(), ())):
                    return True
        return any((L, w) in gloss for L in ("hi", "sa", lang))

    print(f"{'language':<9}{'files':>6}{'words':>9}{'tokens':>10}{'missing words':>15}{'missing tokens':>16}")
    for lang in ("hi", "en", "bn", "or"):
        files = sorted((ROOT / "content" / lang).rglob("*.md"))
        cnt = collections.Counter()
        for f in files:
            cnt.update(words_of(f, lang))
        miss = {w: n for w, n in cnt.items() if not covered(w, lang)}
        print(f"{lang:<9}{len(files):>6}{len(cnt):>9,}{sum(cnt.values()):>10,}{len(miss):>15,}{sum(miss.values()):>16,}")
        out = ROOT / "content/_dict/work" / f"md_missing_{lang}.tsv"
        out.write_text("\n".join(f"{w}\t{n}" for w, n in sorted(miss.items(), key=lambda x: -x[1])), encoding="utf-8")
        print("   e.g.", " ".join(w for w, _ in sorted(miss.items(), key=lambda x: -x[1])[:n_list]))


if __name__ == "__main__":
    main()
