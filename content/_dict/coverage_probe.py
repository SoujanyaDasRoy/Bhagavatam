#!/usr/bin/env python3
"""Task 1 of the dictionary plan: which words does the app's own text contain, and how often?

Reads content/content.db (hi, bn, en, sa columns) and content/or/**/*.md (the Odia Tesseract draft, noisy) and writes
    content/_dict/work/vocab_<lang>.tsv      word <TAB> count      (most frequent first)
and prints a summary: tokens, distinct forms, forms seen twice or more, and how much of the text the top N words cover.

Later tasks use these files to (a) decide which dictionary entries to keep, (b) measure what share of the book a
candidate dictionary covers:   python coverage_probe.py --against work/headwords_hi.txt   (one headword per line)

The output is derived from the Gita Press text, so work/ is in .gitignore. Do not commit it.
"""
import argparse
import collections
import glob
import re
import sqlite3
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
WORK = HERE / "work"
RX = {
    "hi": re.compile(r"[ऀ-ॿ]+"),
    "sa": re.compile(r"[ऀ-ॿ]+"),
    "bn": re.compile(r"[ঀ-৿]+"),
    "or": re.compile(r"[଀-୿]+"),
    "en": re.compile(r"[A-Za-zÀ-ɏḀ-ỿ'’]+"),
}
DANDAS = str.maketrans({"।": " ", "॥": " "})


def norm(w: str) -> str:
    """The same light normalisation the app will use: NFC, no joiners, lower case, no edge apostrophes."""
    import unicodedata
    w = unicodedata.normalize("NFC", w).replace("‍", "").replace("‌", "")
    return w.lower().strip("'’-")


def count_db(col: str, rx: re.Pattern) -> collections.Counter:
    c = collections.Counter()
    db = sqlite3.connect(ROOT / "content" / "content.db")
    for (t,) in db.execute(f'select "{col}" from verse where "{col}" is not null and "{col}" <> ""'):
        for w in rx.findall(t.translate(DANDAS)):
            c[norm(w)] += 1
    return c


def count_odia() -> collections.Counter:
    c = collections.Counter()
    for f in glob.glob(str(ROOT / "content" / "or" / "**" / "*.md"), recursive=True):
        t = Path(f).read_text(encoding="utf-8").replace("[?]", " ")
        for w in RX["or"].findall(t.translate(DANDAS)):
            c[norm(w)] += 1
    return c


def summary(lang: str, c: collections.Counter, top: int = 5000) -> str:
    tot = sum(c.values())
    if not tot:
        return f"{lang}: no text found"
    cover = sum(n for _, n in c.most_common(top))
    return (f"{lang}: tokens {tot:,} | distinct forms {len(c):,} | seen 2+ times {sum(1 for n in c.values() if n >= 2):,} "
            f"| top {top:,} words cover {100 * cover / tot:.1f}% of the text")


def against(lang: str, c: collections.Counter, path: Path) -> str:
    heads = {norm(l.strip()) for l in path.read_text(encoding="utf-8").splitlines() if l.strip()}
    tot = sum(c.values())
    hit_tokens = sum(n for w, n in c.items() if w in heads)
    hit_forms = sum(1 for w in c if w in heads)
    top = c.most_common(1000)
    top_hit = sum(1 for w, _ in top if w in heads)
    return (f"{lang} vs {path.name}: {hit_forms:,} of {len(c):,} forms found; they make up {100 * hit_tokens / tot:.1f}% of the text; "
            f"{top_hit} of the 1,000 most frequent words found")


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--langs", default="hi,bn,en,or,sa")
    ap.add_argument("--against", help="file with one headword per line; the language is taken from the file name headwords_<lang>.txt")
    a = ap.parse_args()
    sys.stdout.reconfigure(encoding="utf-8")
    WORK.mkdir(exist_ok=True)
    for lang in a.langs.split(","):
        c = count_odia() if lang == "or" else count_db(lang, RX[lang])
        (WORK / f"vocab_{lang}.tsv").write_text("\n".join(f"{w}\t{n}" for w, n in c.most_common()) + "\n", encoding="utf-8")
        print(summary(lang, c))
        if a.against:
            p = Path(a.against)
            m = re.search(r"headwords_(\w+)\.txt$", p.name)
            if m and m.group(1) == lang:
                print("   ", against(lang, c, p))
    if "or" in a.langs.split(","):
        print("note: Odia counts come from the Tesseract draft, which has many misread words; treat them as an upper bound on distinct forms.")


if __name__ == "__main__":
    main()
