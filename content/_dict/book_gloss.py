#!/usr/bin/env python3
"""Gives every word of the book a meaning: the words the dictionary does not know get one from the book itself.

    py content/_dict/book_gloss.py [dictionary.db] [content.db ...]   [--fresh: align again instead of using work/align_*.pickle]
        defaults: reads content/_dict/dictionary.db; writes content/content.db and app/src/main/assets/content.db

The Odia and Bengali translations follow the Hindi verse by verse, the Hindi follows the English, and the shloka is
translated by the Hindi. A word-alignment model (IBM Model 1 with a spelling-likeness prior) is trained on the 14,580
verses for each pair, and every word gets the words it is translated by:
  or, bn, sa -> hi      hi -> en      en -> hi
A word is also split into parts the dictionary knows (Sanskrit sandhi at a vowel sign, compounds, case endings).
Result: table book_gloss(lang, word, hi, en, parts), only for words the dictionary itself cannot resolve (same lookup
order as the app), written into content.db: it is derived from the Gita Press translations, so it lives with them and
not in dictionary.db (that file holds openly licensed text only, see docs/agent/DECISIONS.md, item 7). The app shows it
as the last step of Dictionary.lookup (BookGloss.kt), marked as matched by machine. Run again after content.db is rebuilt.
"""
import collections
import re
import sqlite3
import sys
import unicodedata
from pathlib import Path

import functools
import pickle

import numpy as np

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
sys.path.insert(0, str(HERE))
sys.stdout.reconfigure(encoding="utf-8")
import coverage_real as cr  # noqa: E402
sys.path.insert(0, str(ROOT / "content" / "_search"))
import prototype as proto  # noqa: E402

DIGITS = re.compile(r"[०-९০-৯୦-୯৷]")
EN_WORD = re.compile(r"[A-Za-zĀ-ſḀ-ỿ]+")
EN_STOP = set("""a an the of to in and is are was were be been being it its that this these those as at by for from with on or but
not no he him his she her they them their we us our you your i me my who whom whose which what when where how why than then so
such there here all any each every some also may can will shall would should could must have has had do does did unto into upon
about over under after before between through both either neither only other another very more most much many own same too just if
because while during without within thus therefore hence yet nay even indeed thy thee thou ye o oh""".split())
HI_STOP = set("का की के को से में पर ने है हैं था थे थी और तो भी ही यह वह जो कि एक इस उस न नहीं हो गया गये गयी कर करके किया".split())
COLS = {"sa": "sa", "hi": "hi", "en": "en", "bn": "bn", "or": '"or"'}
PAIRS = (("or", "hi"), ("bn", "hi"), ("sa", "hi"), ("hi", "en"), ("en", "hi"))
ITER = 6
VOWEL_OF = {"ा": "अआ", "ि": "इ", "ी": "ईइ", "ु": "उ", "ू": "ऊउ", "े": "एइ", "ो": "ओउ", "ै": "ऐए", "ौ": "औओ"}
# Sanskrit sandhi at the end of a piece, put back to the dictionary form
SANDHI_END = [("र्", "ः"), ("ो", "ः"), ("स्", "ः"), ("श्", "ः"), ("ष्", "ः"), ("न्", "म्"), ("ं", "म्"), ("द्", "त्"), ("ग्", "क्"),
              ("ज्", "त्"), ("च्", "त्"), ("ब्", "प्"), ("ञ्", "म्"), ("ङ्", "म्"), ("ण्", "म्")]
# short last parts of compounds in the translations: भक्तजन, देवगण, तेजोमय, करुणावश
SHORT_TAIL = set("जन गण मय वश वत् गत कर দল জন গণ ময় বশ ଜନ ଗଣ ମୟ ବଶ".split())
ENCLITIC = set("च तु हि वा अपि एव इति इव वै ह खलु ननु".split())
SHORT_SA = set("च तु हि न स वा अपि एव इति यत् तत् मे ते नः वः वै ह मा यः सः तम् तं यम् अहम् त्वम् इदम् अयम् सा या का कः किम् यदि यथा तथा अथ अद्य इह अत्र तत्र यत्र सदा पुनः ननु खलु इव च".split())


def nfc(s):
    return unicodedata.normalize("NFC", s)


def tokens(text, lang):
    """The words of a verse as the app looks them up (Normalise.word): Indic as is, English lower case."""
    if not text:
        return []
    if lang == "or":
        text = re.sub(r"\S*\[\?\]", " ", text)
    text = text.replace("।", " ").replace("॥", " ").replace(chr(0x200C), "").replace(chr(0x200D), "")  # the app drops joiners too
    if lang == "en":
        return [nfc(w).lower() for w in EN_WORD.findall(text) if len(w) >= 2]
    pat = cr.WORD["hi" if lang == "sa" else lang]
    return [w for w in (cr.clean_word(x, lang) for x in pat.findall(text)) if len(w) >= 2 and not DIGITS.search(w)]


def deva(w):
    return cr.to_deva(w)


def similar(a, b):
    """Spelling likeness of two words in any script (0..1): the shared start of their loose Roman keys (the search's
    cross-script key: कृष्ण, କୃଷ୍ଣ, কৃষ্ণ and kṛṣṇa meet) over the shorter key (case endings differ between the languages)."""
    a, b = loose(a), loose(b)
    n = min(len(a), len(b))
    if n < 4:
        return 1.0 if a and a == b else 0.0
    k = 0
    while k < n and a[k] == b[k]:
        k += 1
    return k / n if k >= 4 else 0.0


@functools.lru_cache(maxsize=None)
def loose(w):
    return proto.loose_from_indic(w) if cr.INDIC.search(w) else proto.loose_from_roman(w)


def is_ending(p, lang):
    """True for a piece that is only case endings (में, योंमें, ମାନଙ୍କୁ, দের)."""
    ends = cr.SUFFIX.get(lang, [])
    if p in ends:
        return True
    return any(p.endswith(e) and p[:-len(e)] in ends for e in ends)


def pick(w, table, sure, k):
    """The aligned words worth showing: spelled like the word itself, or the steady translation of a word seen 3+ times."""
    counter = table.get(w)
    if not counter:
        return []
    total = sum(counter.values())
    out = [g for g, n in counter.most_common(k + 2) if similar(w, g) >= 0.6 or (total >= 3 and n >= 2 and n / total >= 0.4)]
    if not out and total >= 5:
        # a common word translated by several forms of one word (करते, करें, करनेपर): judge the forms together
        groups = collections.Counter()
        for g, n in counter.items():
            groups[loose(g)[:3]] += n
        key, n = groups.most_common(1)[0]
        if n / total >= 0.35:
            out = [g for g, _ in counter.most_common() if loose(g)[:3] == key]
    return out[:k]


def align(src_lang, tgt_lang, rows):
    """IBM Model 1, every source word aligned to one target word of the same verse (or to nothing).
    Returns {source word: Counter(target word -> times aligned)}."""
    sv, tv = {}, {}
    verses = []
    for s_text, t_text in rows:
        s = [sv.setdefault(w, len(sv)) for w in tokens(s_text, src_lang)]
        t = [tv.setdefault(w, len(tv)) for w in tokens(t_text, tgt_lang)
             if not (tgt_lang == "en" and w in EN_STOP) and not (tgt_lang == "hi" and w in HI_STOP)]
        if s and t:
            verses.append((np.array(s, np.int64), np.array(sorted(set(t)), np.int64)))
    NULL = len(tv)
    ps, pt = [], []
    for s, t in verses:
        t = np.append(t, NULL)
        ps.append(np.repeat(s, len(t))); pt.append(np.tile(t, len(s)))
    ps, pt = np.concatenate(ps), np.concatenate(pt)
    # one group per source token: the posterior over its verse's target words is normalised inside the group
    tok_group = np.repeat(np.arange(sum(len(s) for s, _ in verses)), np.repeat([len(t) + 1 for _, t in verses], [len(s) for s, _ in verses]))
    key = ps * (NULL + 1) + pt
    uk, inv = np.unique(key, return_inverse=True)
    us, ut = uk // (NULL + 1), uk % (NULL + 1)
    # spelling prior between Indic words (names and tatsama words keep their spelling across Hindi, Bengali, Odia, Sanskrit)
    prior = np.ones(len(uk))
    sl = {i: w for w, i in sv.items()}; tl = {i: w for w, i in tv.items()}
    if True:
        for j in range(len(uk)):
            if ut[j] == NULL:
                continue
            sim = similar(sl[us[j]], tl[ut[j]])
            if sim:
                prior[j] = 1 + 8 * sim
    prior[ut == NULL] = 0.3
    t = np.ones(len(uk))
    for _ in range(ITER):
        q = (t * prior)[inv]
        den = np.bincount(tok_group, weights=q)
        post = q / den[tok_group]
        c = np.bincount(inv, weights=post, minlength=len(uk))
        tot = np.bincount(ut, weights=c, minlength=NULL + 1)  # t(s|t): normalise over the source words each target word explains
        t = c / np.maximum(tot[ut], 1e-12)
    # best target per source token
    score = (t * prior)[inv]
    post = score / np.bincount(tok_group, weights=score)[tok_group]
    order = np.lexsort((-score, tok_group))
    first = np.ones(len(order), bool)
    first[1:] = tok_group[order][1:] != tok_group[order][:-1]
    best = order[first]
    sl = {i: w for w, i in sv.items()}; tl = {i: w for w, i in tv.items()}
    out = collections.defaultdict(collections.Counter)
    sure = collections.defaultdict(float)  # (source, target) -> the highest posterior of that link in any verse
    for j in best:
        if pt[j] != NULL:
            a, b = sl[ps[j]], tl[pt[j]]
            out[a][b] += 1
            sure[(a, b)] = max(sure[(a, b)], float(post[j]))
    return out, sure


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    dpath = Path(args[0]) if args else HERE / "dictionary.db"
    targets = [Path(a) for a in args[1:]] or [ROOT / "content/content.db", ROOT / "app/src/main/assets/content.db"]
    d = sqlite3.connect(dpath)
    by_norm, by_form, by_skey = set(), set(), collections.defaultdict(set)
    for lang, norm, head, skey in d.execute("select lang, norm, headword, skey from entry"):
        by_norm.add((lang, norm)); by_norm.add((lang, head)); by_skey[skey].add(lang)
    for lang, fnorm in d.execute("select e.lang, f.form_norm from form f join entry e on e.id = f.entry_id"):
        by_form.add((lang, fnorm))

    def known(w, lang):
        for L in (("sa", "hi") if lang == "sa" else (lang,)):
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
        return False

    c = sqlite3.connect(str(ROOT / "content/content.db"))
    text = {L: [] for L in COLS}
    for row in c.execute("select sa, hi, en, bn, \"or\" from verse"):
        for L, x in zip(("sa", "hi", "en", "bn", "or"), row):
            text[L].append(x or "")
    freq = {L: collections.Counter(w for x in text[L] for w in tokens(x, L)) for L in COLS}

    aligned = {}
    for s, t in PAIRS:
        rows = [(a, b) for a, b in zip(text[s], text[t]) if a and b]
        cache = HERE / "work" / f"align_{s}_{t}.pickle"
        if cache.exists() and "--fresh" not in sys.argv:
            aligned[(s, t)] = pickle.loads(cache.read_bytes())
        else:
            aligned[(s, t)] = align(s, t, rows)
            cache.write_bytes(pickle.dumps(aligned[(s, t)]))
        print(f"aligned {s} -> {t}: {len(aligned[(s, t)][0]):,} words")

    from functools import lru_cache

    def parts(w, lang):
        """Split into 2 to 4 pieces the dictionary knows (longest head first). Pieces have 3+ letters, except the Sanskrit
        particles in SHORT_SA. In Sanskrit and Hindi a vowel sign may start the next piece as a full vowel
        (विशुद्धानुभव = विशुद्ध + अनुभव, चापि = च + अपि, तथैव = तथा + एव); in Sanskrit a piece may end in a sandhi form
        that is put back (नमस् = नमः, कुररैर् = कुररैः, तच् + छ्रुत्वा = तत् + श्रुत्वा, सोऽपि = सः + अपि)."""
        sa = lang == "sa"

        def form(p):
            cands = [p]
            if sa:
                for end, rep in SANDHI_END:
                    if p.endswith(end):
                        cands.append(p[:-len(end)] + rep)
            for c in cands:
                if (len(c) >= 3 or (sa and c in SHORT_SA) or c in SHORT_TAIL) and (known(c, lang) or (sa and known(c, "hi"))):
                    return c
            return None

        form = lru_cache(maxsize=None)(form)

        @lru_cache(maxsize=None)
        def split(x, depth):
            """(cost, pieces) of the cheapest split: one point a piece, more for a short particle or a sandhi change."""
            best = None
            if depth:
                f = form(x)
                if f:
                    best = (1 + (1.5 if len(f) < 3 else 0) + (0.3 if f != x else 0), (f,))
            if depth >= 3:
                return best
            for j in range(len(x) - 1, 0, -1):
                head, rest = x[:j], x[j:]
                tries = [(head, rest, 0)]
                if sa and rest.startswith("ऽ"):
                    tries.append((head, "अ" + rest[1:], 0))
                if sa and head.endswith("च्") and rest.startswith("छ"):
                    tries.append((head[:-2] + "त्", "श" + rest[1:], 0.3))
                if lang in ("sa", "hi") and head[-1] in VOWEL_OF and len(head) > 1:
                    lefts = [head[:-1]] + ([head[:-1] + "ा"] if head[-1] in "ाैौे" else [])
                    tries += [(l, v + rest, 0.3) for l in lefts for v in VOWEL_OF[head[-1]]]
                for h, r, extra in tries:
                    f = form(h)
                    if not f:
                        continue
                    sub = split(r, depth + 1)
                    # च, अपि, एव follow a word: one of them can start the word only before more of them (चापि, चैव)
                    if sub and depth == 0 and f in ENCLITIC and not all(p in SHORT_SA for p in sub[1]):
                        continue
                    if sub:
                        c = 1 + (1.5 if len(f) < 3 else 0) + (0.3 if f != h else 0) + extra + sub[0]
                        if best is None or c < best[0]:
                            best = (c, (f,) + sub[1])
            return best

        r = split(w, 0)
        if not r:
            return None
        out = list(r[1])
        # a case ending is not a part of its own: रश्मि + योंमें is रश्मियोंमें
        while len(out) > 1 and is_ending(out[-1], lang):
            out[-2:] = [out[-2] + out[-1]]
        return out if len(out) > 1 else None

    all_rows = []
    print(f"{'language':<9}{'words':>9}{'in dict':>9}{'by book':>9}{'none':>7}   tokens covered")
    for L in ("en", "hi", "bn", "or", "sa"):
        rows_out, n_dict, n_book, none = [], 0, 0, []
        tok_tot = tok_cov = 0
        for w, n in freq[L].items():
            tok_tot += n
            if known(w, L):
                n_dict += 1; tok_cov += n
                continue
            hi = en = ""
            if L in ("or", "bn", "sa"):
                hi = "|".join(pick(w, *aligned[(L, "hi")], 2))
            if L == "hi":
                en = "|".join(pick(w, *aligned[("hi", "en")], 3))
            if L == "en":
                hi = "|".join(pick(w, *aligned[("en", "hi")], 2))
            pr = "+".join(parts(w, L) or [])
            if hi or en or pr:
                rows_out.append((L, w, hi, en, pr)); n_book += 1; tok_cov += n
            else:
                none.append((n, w))
        all_rows += rows_out
        print(f"{L:<9}{len(freq[L]):>9,}{n_dict:>9,}{n_book:>9,}{len(none):>7,}   {100 * tok_cov / max(1, tok_tot):.2f}%")
        (HERE / "work" / f"no_gloss_{L}.tsv").write_text("\n".join(f"{w}\t{n}" for n, w in sorted(none, reverse=True)), encoding="utf-8")
    for t in targets:
        o = sqlite3.connect(t)
        o.executescript("DROP TABLE IF EXISTS book_gloss;"
                        "CREATE TABLE book_gloss(lang TEXT, word TEXT, hi TEXT, en TEXT, parts TEXT, PRIMARY KEY(lang, word)) WITHOUT ROWID;")
        o.executemany("insert into book_gloss values (?,?,?,?,?)", all_rows)
        o.execute("insert or replace into meta values ('book_gloss', 'IBM Model 1 over the verses: or/bn/sa->hi, hi->en, en->hi; parts from dictionary.db')")
        o.commit()
        o.execute("vacuum")
        o.close()
        print(f"book_gloss: {len(all_rows):,} rows -> {t} ({t.stat().st_size / 1048576:.1f} MB)")


if __name__ == "__main__":
    main()
