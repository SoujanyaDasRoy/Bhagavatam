#!/usr/bin/env python3
"""Conservative post-OCR corrections for the Odia pages.

1. ଙ୍କ misreads: Tesseract renders the very common suffix ଙ୍କ(ର|ୁ) as garbage like ର୍କ୍ରର / ବ୍କୀ / ର୍ଚ୍ଚ.
   A token is rewritten only when  stem + (ଙ୍କ | ଙ୍କର | ଙ୍କୁ)  is itself attested at least MIN_FREQ times
   in the clean part of the book, and the garbage matches the known shape.
2. ଥି misread as ଥୁ/ଥୂ before ଲ/ବ  (ଥୁଲା -> ଥିଲା, କରିଥୁବା -> କରିଥିବା).

Every change is logged to review/autofix_log.csv so nothing is silent.
Usage:  python3 odia_fix.py        (reads work/vision/*.json, writes work/fixed/*.json)
"""
import csv, json, os, re, sys
from collections import Counter
from pathlib import Path

HERE = Path(__file__).resolve().parent
WORK = Path(os.environ.get("ODIA_WORK", HERE / "work"))
REVIEW = Path(os.environ.get("ODIA_REVIEW", HERE / "review"))
sys.stdout.reconfigure(encoding="utf-8")
MIN_FREQ = 2

SPLIT = re.compile(r"([\s।॥,;:!?\"'“”‘’()\[\]\-–—]+)")
# the garbage that stands for ଙ୍କ + suffix: a ର/ବ/ଚ/ଜ + virama + କ/ଚ/ଦ/ତ/ର/ଵ ... then optional vowel/extra-conjunct bits
GARBAGE = re.compile(r"^(?P<stem>.{2,}?)(?P<g>[ରବଜ]୍[କଚଦତବ](?:୍?[କଚରତଦବ]?[ୀୁୂାେିୌ]?[ରକଚ]?)?(?:୍?[ରକଚ])?[ୀୁୂର]?)$")
SUBS = [("ଗ୍ବ", "ଚା"), ("ଥୂ", "ଥି"), ("ଥୁ", "ଥି"), ("ଧୂ", "ଧି"), ("ଧୁ", "ଧି"), ("ଖୂ", "ଖି"), ("ଖୁ", "ଖି"), ("ଘୁ", "ଘି"), ("ସୂଷ୍ଟ", "ସୃଷ୍ଟ"), ("ଟୂ", "ଟି"), ("ମ୍ୂ", "ମ୍ୟ"), ("ମ୍ିି", "ମ୍ମି")]
KNOWN_OK = set("ନର୍କ ତର୍କ ସମ୍ପର୍କ ସମ୍ପର୍କୀୟ ଚର୍ଚ୍ଚା ଚର୍ଚ୍ଚ ଉଚ୍ଚ ଉଚ୍ଚାରଣ ସଚ୍ଚିଦାନନ୍ଦ ଚତୁର୍ଦିଗ ଚତୁର୍ଦ୍ଦିଗ ଅର୍କ ସୂର୍ଯ୍ୟ".split())


def suffix_class(g):
    if g.endswith("ର") and len(g) > 3:
        return "ଙ୍କର"
    if g.endswith("ୁ") or g.endswith("ୂ"):
        return "ଙ୍କୁ"
    return "ଙ୍କ"


def tokens(text):
    return [t for t in SPLIT.split(text) if t and not SPLIT.fullmatch(t)]


def build_vocab(pages):
    c = Counter()
    for d in pages:
        for p in d.get("paragraphs", []):
            for t in tokens(p.replace("[?]", " ")):
                c[t] += 1
    return c


def make_fixer(vocab):
    log = []
    cache = {}

    def fix_token(t):
        if t in cache:
            return cache[t]
        out = t
        if t not in KNOWN_OK and "ଙ୍କ" not in t:
            m = GARBAGE.match(t)
            if m:
                stem, g = m.group("stem"), m.group("g")
                cls = suffix_class(g)
                cands = [stem + cls] + [stem + x for x in ("ଙ୍କ", "ଙ୍କର", "ଙ୍କୁ") if stem + x != stem + cls]
                good = [(vocab.get(c, 0), c) for c in cands if vocab.get(c, 0) >= MIN_FREQ]
                if good:
                    first = stem + cls
                    out = first if vocab.get(first, 0) >= MIN_FREQ else max(good)[1]
        # ଥି misread, ପୃଥ misread
        if out == t:
            t2 = re.sub(r"ଥ[ୁୂ]([ଲବ])", r"ଥି\1", t)
            t2 = re.sub(r"ପୂଥ", "ପୃଥ", t2)
            if t2 != t:
                out = t2
        # generic attested-word substitutions for rare tokens (ି/ୁ/ୂ and ଚା/ଗ୍ବ confusions)
        if out == t and vocab.get(t, 0) <= 3:
            for wrong, right in SUBS:
                if wrong in t:
                    cand = t.replace(wrong, right)
                    if vocab.get(cand, 0) >= 3 and vocab.get(cand, 0) > 3 * vocab.get(t, 0):
                        out = cand
                        break
        cache[t] = out
        return out

    def fix_text(s, where):
        parts = SPLIT.split(s)
        res = []
        for part in parts:
            if part and not SPLIT.fullmatch(part):
                new = fix_token(part)
                if new != part:
                    log.append((where, part, new))
                part = new
            res.append(part)
        return "".join(res)

    return fix_text, log


def main():
    files = sorted((WORK / "vision").glob("v*_p*.json"))
    pages = [json.loads(f.read_text(encoding="utf-8")) for f in files]
    vocab = build_vocab(pages)
    fix_text, log = make_fixer(vocab)
    out = WORK / "fixed"
    out.mkdir(parents=True, exist_ok=True)
    for f, d in zip(files, pages):
        where = f.stem
        d["paragraphs"] = [fix_text(p, where) for p in d.get("paragraphs", [])]
        if d.get("chapter_title"):
            d["chapter_title"] = fix_text(d["chapter_title"], where)
        (out / f.name).write_text(json.dumps(d, ensure_ascii=False, indent=1), encoding="utf-8")
    REVIEW.mkdir(parents=True, exist_ok=True)
    with open(REVIEW / "autofix_log.csv", "w", newline="", encoding="utf-8-sig") as fh:
        w = csv.writer(fh); w.writerow(["page", "ocr_text", "corrected"])
        w.writerows(log)
    print(f"pages {len(files)}, tokens fixed {len(log)}, distinct {len(set((a, b) for _, a, b in log))}")


if __name__ == "__main__":
    main()
