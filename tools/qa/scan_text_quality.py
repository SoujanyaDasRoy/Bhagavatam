#!/usr/bin/env python3
"""Scans content/content.db for text that looks garbled or like a placeholder, and prints a list for a person to judge.

    py tools/qa/scan_text_quality.py [--db content/content.db] [--out work_scan]

Checks
  Odia verses     letters from other scripts or Latin letters, symbols that never occur in Odia, stray combining marks at the start of a
                  word, a vowel sign after a space, very few Odia letters, a text far shorter or longer than the
                  Hindi of the same verse.
  Chapter titles  empty, only a number or an ordinal word ("पञ्चम अध्याय", "পঞ্চম অধ্যায়"), text in brackets, the wrong script, a copy
                  of another language's title.
Nothing is changed. Verses and titles listed here are for the Odia or Bengali reader to look at against the printed book.
"""
import argparse
import re
import sqlite3
import sys
import unicodedata
from pathlib import Path

ODIA = range(0x0B00, 0x0B80)
DEVA = range(0x0900, 0x0980)
BENG = range(0x0980, 0x0A00)


def block(ch):
    o = ord(ch)
    if o in (0x0964, 0x0965): return None  # the danda is shared by all Indic scripts
    if o in ODIA: return "or"
    if o in DEVA: return "hi"
    if o in BENG: return "bn"
    if ch.isascii() and ch.isalpha(): return "latin"
    return None


def odia_problems(text, hi_text):
    out = []
    text = text.replace("[?]", "")  # words the readers disagreed on are marked in the text; they are known, not garbling
    letters = [ch for ch in text if block(ch)]
    if not letters:
        return ["no letters"]
    counts = {}
    for ch in letters:
        counts[block(ch)] = counts.get(block(ch), 0) + 1
    total = sum(counts.values())
    foreign = {k: v for k, v in counts.items() if k != "or"}
    if foreign:
        # a few Latin letters can be a reference or a name in brackets; many mean the page was read wrongly
        n = sum(foreign.values())
        if n >= 3 or n / total > 0.03:
            out.append("other script " + ",".join(f"{k}:{v}" for k, v in sorted(foreign.items())))
    if counts.get("or", 0) / total < 0.8:
        out.append(f"only {counts.get('or', 0) * 100 // total}% Odia letters")
    # symbols that never occur in Odia running text
    odd = set(re.findall(r"[୤୥୰୲-୷୸-୿]", text))  # danda forms, fractions, isshar, etc.
    odd |= set(re.findall(r"[୤୥]", text)) - {"୤", "୥"}  # legacy duplicates (danda is U+0964)
    if odd:
        out.append("odd signs " + "".join(sorted(odd)))
    bad = re.findall(r"(?:^|\s)[ା-୍ଁ-ଃ]", text)
    if bad:
        out.append(f"{len(bad)} words start with a vowel sign or virama")
    if re.search(r"[;:]\s*,|,\s*,|;\s*;", text):
        out.append("stray punctuation runs")
    if hi_text:
        hl = len(re.sub(r"\s+", "", hi_text)); ol = len(re.sub(r"\s+", "", text))
        if hl > 40 and (ol < hl * 0.5 or ol > hl * 2.2):
            out.append(f"length {ol} against Hindi {hl}")
    return out


ORDINALS = {
    "hi": ["प्रथम", "द्वितीय", "तृतीय", "चतुर्थ", "पञ्चम", "पंचम", "षष्ठ", "सप्तम", "अष्टम", "नवम", "दशम", "एकादश", "द्वादश", "त्रयोदश", "चतुर्दश"],
    "bn": ["প্রথম", "দ্বিতীয়", "তৃতীয়", "চতুর্থ", "পঞ্চম", "ষষ্ঠ", "সপ্তম", "অষ্টম", "নবম", "দশম", "একাদশ", "দ্বাদশ", "ত্রয়োদশ", "চতুর্দশ"],
    "or": ["ପ୍ରଥମ", "ଦ୍ୱିତୀୟ", "ତୃତୀୟ", "ଚତୁର୍ଥ", "ପଞ୍ଚମ", "ଷଷ୍ଠ", "ସପ୍ତମ", "ଅଷ୍ଟମ", "ନବମ", "ଦଶମ", "ଏକାଦଶ", "ଦ୍ୱାଦଶ"],
}
WORD_ADHYAYA = {"hi": "अध्याय", "bn": "অধ্যায়", "or": "ଅଧ୍ଯାୟ|ଅଧ୍ୟାୟ"}
SCRIPT = {"hi": "hi", "bn": "bn", "or": "or"}


def title_problems(lang, title, others):
    if title is None or not title.strip():
        return ["empty"]
    t = title.strip()
    out = []
    core = re.sub(r"[\s()\[\].,:;\-–—0-9०-९০-৯୦-୯]+", " ", t).strip()
    if t[0] in "([" and t[-1] in ")]":
        out.append("in brackets")
    words = ORDINALS[lang]
    adhy = WORD_ADHYAYA[lang]
    if re.fullmatch(rf"(?:{'|'.join(words)})?\s*(?:{adhy})?\s*", core) or not core:
        out.append("only a number or an ordinal word")
    elif re.match(rf"^(?:{'|'.join(words)})\s+(?:{adhy})\b", core) and len(core.split()) <= 4:
        out.append("ordinal word with a short tail")
    letters = [ch for ch in t if block(ch)]
    if letters:
        share = sum(block(ch) == SCRIPT[lang] for ch in letters) / len(letters)
        if share < 0.6:
            out.append(f"{int(share * 100)}% of letters are in the right script")
    for name, other in others.items():
        if other and other.strip() == t:
            out.append(f"same as the {name} title")
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--db", default="content/content.db")
    ap.add_argument("--out", default=None)
    a = ap.parse_args()
    c = sqlite3.connect(a.db)
    lines = []

    lines.append("== Odia verses ==")
    n = 0
    for sk, ch, num, hi, orv in c.execute("SELECT skandha, chapter, num, hi, \"or\" FROM verse WHERE \"or\" IS NOT NULL AND TRIM(\"or\")<>'' ORDER BY skandha, chapter, num"):
        p = odia_problems(orv, hi or "")
        if p:
            n += 1
            lines.append(f"{sk}.{ch}.{num}  " + "; ".join(p) + "  | " + orv.replace("\n", " ")[:80])
    lines.append(f"-- {n} Odia verses to look at")

    for lang, col in (("hi", "title_hi"), ("bn", "title_bn"), ("or", "title_or")):
        lines.append(f"== {lang} chapter titles ==")
        n = 0
        for sk, ch, en, hi, bn, orr in c.execute("SELECT skandha, chapter, title_en, title_hi, title_bn, title_or FROM chapter ORDER BY skandha, chapter"):
            t = {"hi": hi, "bn": bn, "or": orr}[lang]
            if lang == "or" and not (t or "").strip():
                continue  # Odia titles are optional; a missing one shows another language
            others = {"English": en}
            if lang != "hi": others["Hindi"] = hi
            p = title_problems(lang, t, others)
            if p:
                n += 1
                lines.append(f"{sk}.{ch}  " + "; ".join(p) + f"  | {t!r}")
        lines.append(f"-- {n} {lang} titles to look at")

    text = "\n".join(lines)
    print(text)
    if a.out:
        Path(a.out).mkdir(parents=True, exist_ok=True)
        (Path(a.out) / "text_quality.txt").write_text(text, encoding="utf-8")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    main()
