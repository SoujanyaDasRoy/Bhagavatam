#!/usr/bin/env python3
"""Text helpers shared by the Odia verification tools (no network, no book text inside).

  norm(s)              NFC, one kind of quote and danda, no [?] marks, single spaces: the form two readings are compared in
  tokens(s)            word tokens of a paragraph (verse markers like "॥ ୧୨ ॥" are kept as tokens)
  core(tok)            a token without surrounding punctuation: what is compared
  hygiene(tok)         list of issue codes for a word that cannot be right in Odia (stray Latin letters, a vowel sign with no
                       consonant, two vowel signs in a row, a Devanagari or Bengali letter, ASCII digits ...)
  align(a, b)          word alignment of two token lists: [(i or None, j or None)] in order, equal words paired
  wer(a, b)            word error rate of reading b against reading a
"""
import difflib
import re
import unicodedata

OD_DIGITS = "୦୧୨୩୪୫୬୭୮୯"
PUNCT = "“”‘’\"'()[]{}!?;:,.-–—~|।॥*#"
_QUOTES = str.maketrans({"“": '"', "”": '"', "‘": "'", "’": "'", "|": "।", "–": "-", "—": "-"})
_JOINERS = dict.fromkeys(map(ord, "‌‍﻿"), None)

ODIA_BLOCK = re.compile(r"[଀-୿]")
LATIN = re.compile(r"[A-Za-z]")
DEVA_BENGALI = re.compile(r"[ऀ-ॿঀ-৿]")
ASCII_DIGIT = re.compile(r"[0-9]")
VOWEL_SIGN = re.compile(r"[ା-ୌୖୗୢୣ]")
VIRAMA = "୍"
MARKS = re.compile(r"[ଁ-ଃ଼ା-୍ୖୗୢୣ]")   # nothing here may start a word
UNSURE = re.compile(r"\[\?+\]")


def norm(s: str) -> str:
    s = unicodedata.normalize("NFC", s or "").translate(_JOINERS).translate(_QUOTES)
    s = UNSURE.sub("", s)
    return re.sub(r"\s+", " ", s).strip()


def core(tok: str) -> str:
    return tok.strip(PUNCT)


def is_marker(tok: str) -> bool:
    """A verse marker token such as ॥୧୨॥ (made by tokens()) or a bare danda."""
    return tok.lstrip("“”‘’\"'").startswith("॥") or core(tok) == ""


def tokens(s: str):
    """Word tokens of a paragraph. A verse marker (danda, digits, danda) is joined into one token."""
    s = norm(s)
    s = re.sub(r"([^\s॥])(?=॥)", r"\1 ", s)            # "କହିଲେ-॥୧॥": the marker is its own token
    s = re.sub(r"॥\s*([୦-୯0-9][୦-୯0-9\s,\-]*)\s*॥", lambda m: "॥" + re.sub(r"\s+", "", m.group(1)) + "॥", s)
    return [t for t in s.split(" ") if t]


def hygiene(tok: str):
    """Issue codes for one word. Empty list = nothing wrong that can be seen without the page."""
    w = core(tok).replace("।", "").replace("॥", "")
    if not w or is_marker(tok):
        return []
    out = []
    if LATIN.search(w):
        out.append("latin")
    if DEVA_BENGALI.search(w):
        out.append("foreign_script")
    if ASCII_DIGIT.search(w):
        out.append("ascii_digit")
    if ODIA_BLOCK.search(w):
        if MARKS.match(w[0]) and not w[0] == "ଁ":
            out.append("starts_with_mark")
        if re.search(VOWEL_SIGN.pattern + VOWEL_SIGN.pattern, w):
            out.append("double_vowel_sign")
        if re.search(VIRAMA + VOWEL_SIGN.pattern, w) or VIRAMA + VIRAMA in w:
            out.append("virama_then_sign")
        if re.search(r"[ଂଃ]" + VOWEL_SIGN.pattern, w):
            out.append("anusvara_then_sign")
        if w.endswith(VIRAMA):
            out.append("ends_with_virama")
    return out


# Issues that cannot occur in correct Odia text and send a word to review. "ends_with_virama" is only information:
# ଭଗବାନ୍, ସାକ୍ଷାତ୍, ରାଜନ୍ are ordinary words (1.2 in 1000 words of the checked Skandha 1 to 4 pages).
SEVERE = {"latin", "foreign_script", "ascii_digit", "starts_with_mark", "double_vowel_sign", "virama_then_sign", "anusvara_then_sign"}


def severe(tok: str):
    return [c for c in hygiene(tok) if c in SEVERE]


def align(a, b):
    """Pair up two token lists by their cores. Returns [(i, j)] with None where one side has no word."""
    ca, cb = [core(t) for t in a], [core(t) for t in b]
    sm = difflib.SequenceMatcher(a=ca, b=cb, autojunk=False)
    out = []
    for op, i1, i2, j1, j2 in sm.get_opcodes():
        if op == "equal":
            out += [(i1 + k, j1 + k) for k in range(i2 - i1)]
        elif op == "replace":
            n = min(i2 - i1, j2 - j1)
            out += [(i1 + k, j1 + k) for k in range(n)]
            out += [(i1 + k, None) for k in range(n, i2 - i1)]
            out += [(None, j1 + k) for k in range(n, j2 - j1)]
        elif op == "delete":
            out += [(i1 + k, None) for k in range(i2 - i1)]
        else:
            out += [(None, j1 + k) for k in range(j2 - j1)]
    return out


def wer(a, b) -> float:
    """Word error rate of reading b against reading a (token lists): 0.0 = identical."""
    ca, cb = [core(t) for t in a], [core(t) for t in b]
    if not ca:
        return 0.0 if not cb else 1.0
    same = sum(blk.size for blk in difflib.SequenceMatcher(a=ca, b=cb, autojunk=False).get_matching_blocks())
    return 1.0 - same / max(len(ca), len(cb))


def char_diffs(x: str, y: str):
    """(from, to) character-level replacements between two spellings of one word, for the confusion table."""
    sm = difflib.SequenceMatcher(a=x, b=y, autojunk=False)
    return [(x[i1:i2], y[j1:j2]) for op, i1, i2, j1, j2 in sm.get_opcodes() if op != "equal"]
