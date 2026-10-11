#!/usr/bin/env python3
"""Checks for the Odia verification tools. Pure Python, no network, no book text beyond a few made-up words.
Run:  py content/_odia/tests/test_verify.py      (exit code 0 = pass)
"""
import sys
import types
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))
sys.stdout.reconfigure(encoding="utf-8")
import odia_text as T  # noqa: E402
import odia_verify as V  # noqa: E402

fails = []


def check(ok, msg):
    print(("ok   " if ok else "FAIL ") + msg)
    if not ok:
        fails.append(msg)


# ---- words and markers
toks = T.tokens("ରାଜା କହିଲେ, ହେ ମୁନି ! ॥ ୧୨ ॥")
check(toks[-1] == "॥୧୨॥", "a verse marker is one token")
check(T.core("“ରାଜା,") == "ରାଜା", "core strips quotes and commas")
check(T.norm("କ [?] ଖ") == "କ ଖ", "[?] is removed before two readings are compared")

# ---- hygiene
check(T.hygiene("ରାଜା") == [], "a normal word has no issue")
check("latin" in T.hygiene("ରାabc"), "Latin letters inside an Odia word are flagged")
check("foreign_script" in T.hygiene("ରान"), "a Devanagari letter in an Odia word is flagged")
check("starts_with_mark" in T.hygiene("ାକ"), "a vowel sign at the start of a word is flagged")
check("double_vowel_sign" in T.hygiene("କାି"), "two vowel signs in a row are flagged")
check("virama_then_sign" in T.hygiene("କ୍ା"), "virama followed by a vowel sign is flagged")
check(T.hygiene("ଁକ") == [], "a candrabindu at the start is not flagged here")

# ---- decide: 2 of 3, lexicon, single
check(V.decide("କ", "କ", "ଖ")[1:] == ("AB", False), "A=B wins")
check(V.decide("କ", "ଖ", "କ")[1:] == ("AT", False), "A=T wins")
check(V.decide("କ", "ଖ", "ଖ") == ("ଖ", "BT", False), "B=T wins")
chosen, conf, rev = V.decide("କ", "ଖ", "ଗ", lex={"ଖ"})
check((chosen, conf, rev) == ("ଖ", "lex", True), "a lone known word settles a dispute but is still reviewed")
check(V.decide("କ", "ଖ", "ଗ")[1:] == ("dispute", True), "no majority and no lexicon: dispute")
check(V.decide("କ", None, None)[1:] == ("single", True), "one reading only needs review")

# ---- combine_page
a = ["ରାଜା କହିଲେ ହେ ମୁନି ॥ ୧ ॥"]
b = ["ରାଜା କହିଲେ ହେ ମୁନି ॥ ୧ ॥"]
paras, flags, st = V.combine_page(a, b, None)
check(flags == [] and st["AB"] == 4, "two identical readings: nothing flagged, 4 words agreed")
check(paras == ["ରାଜା କହିଲେ ହେ ମୁନି ॥୧॥"] or paras[0].startswith("ରାଜା କହିଲେ ହେ ମୁନି"), "text is kept")

b2 = ["ରାଜା କହିଲେ ହେ ମୁନୀ ॥ ୧ ॥"]
paras, flags, st = V.combine_page(a, b2, ["ରାଜା କହିଲେ ହେ ମୁନି ॥ ୧ ॥"])
check(flags == [] and st["AT"] == 1, "a word settled by the third reading is not flagged")
paras, flags, st = V.combine_page(a, b2, None)
check(len(flags) == 1 and flags[0]["why"] == "dispute" and paras[0].count("[?]") == 1, "a two-way disagreement is flagged and marked [?]")

b3 = ["ରାଜା କହିଲେ ହେ ମହା ମୁନି ॥ ୧ ॥"]
paras, flags, st = V.combine_page(a, b3, None)
check(any(f["why"] == "omission?" for f in flags), "a word only the other reading has is reported as a possible omission")

paras, flags, st = V.combine_page(["ରାଜା abc"], ["ରାଜା abc"], None)
check(any(f["why"].startswith("hygiene") for f in flags), "agreed but impossible words are still flagged")

check(V.page_meta_mismatches({"running_head_chapter": 3}, {"running_head_chapter": 4}) != [], "different running-head chapters are reported")
check(V.page_meta_mismatches({"paragraphs": ["x"]}, {"paragraphs": ["x"]}) == [], "equal structure: no mismatch")

# ---- audit statistics
lo, hi = V.wilson(0, 200)
check(lo == 0.0 and 0.015 < hi < 0.02, f"0 errors in 200 words: upper bound {hi:.4f} is about 1.9%")
lo, hi = V.wilson(10, 200)
check(0.025 < lo < 0.05 < hi < 0.10, "10 errors in 200 words: interval brackets 5%")

# ---- page selection of odia_vision
import odia_vision as ov  # noqa: E402

args = types.SimpleNamespace(skandha="5", pages="579-581", vol=None, force=True)
check(ov.pages_to_do(args, Path("no-such-dir")) == [(1, 579), (1, 580), (1, 581)], "skandha 5 pages 579-581")
args = types.SimpleNamespace(skandha="5-12", pages=None, vol=None, force=True)
todo = ov.pages_to_do(args, Path("no-such-dir"))
check(1400 < len(todo) < 1500 and todo[0] == (1, 579), f"skandha 5-12 is {len(todo)} pages, starting at v1:579")
check(ov.parse_gemini_json('```json\n{"paragraphs": ["x"]}\n```')["paragraphs"] == ["x"], "a fenced JSON answer is accepted")
try:
    ov.parse_gemini_json('{"nothing": 1}')
    check(False, "an answer without paragraphs is rejected")
except ValueError:
    check(True, "an answer without paragraphs is rejected")

# ---- arbitration: items, prompt, answer parsing
import json  # noqa: E402
import tempfile  # noqa: E402

import odia_arbitrate as AR  # noqa: E402

page = {"paragraphs": ["ରାଜା କହିଲେ ହେ ମୁନି[?] ଆଉ ଭଗବାନ୍ ॥୧॥"],
        "_verify": {"flags": [{"para": 0, "idx": 3, "a": "ମୁନି", "b": "ମୁନୀ", "t": None, "chosen": "ମୁନି", "why": "dispute"},
                              {"para": 0, "idx": 1, "a": "x", "b": "x", "t": None, "chosen": "x", "why": "hygiene:latin"},
                              {"para": None, "idx": None, "a": None, "b": "ଯୋଗ", "t": None, "chosen": None, "why": "omission?"}]}}
items = AR.build_items(page)
check(len(items) == 1 and items[0]["id"] == 1, "only a choice between readings is sent for arbitration")
check(">> ମୁନି <<" in items[0]["context"] and "[?]" not in items[0]["context"], "context marks the word and has no [?]")
check("candidates - A: ମୁନି; B: ମୁନୀ" in AR.prompt_for(items), "the prompt lists the candidates")
got = AR.parse_answer('[{"id": 1, "choice": "B", "text": "ମୁନୀ", "confidence": "high"}]', items)
check(got == {1: ("ମୁନୀ", True)}, "choice B returns candidate B, confident")
got = AR.parse_answer('[{"id": 1, "choice": "other", "text": "ମୁନିଙ୍କ", "confidence": "low"}]', items)
check(got == {1: ("ମୁନିଙ୍କ", False)}, "'other' returns the written word, not confident")
check(AR.parse_answer('[{"id": 1, "choice": "other", "text": "abc", "confidence": "high"}]', items) == {}, "an answer with Latin letters is dropped")
check(AR.parse_answer('[{"id": 9, "choice": "A", "confidence": "high"}]', items) == {}, "an unknown id is dropped")

# ---- apply: a decision replaces the word, drops [?], keeps punctuation and marks the flag resolved
with tempfile.TemporaryDirectory() as td:
    td = Path(td)
    (td / "v1_p0001.json").write_text(json.dumps({"paragraphs": ["ହେ ମୁନି,[?] ଆଉ"], "_verify": {"flags": [
        {"para": 0, "idx": 1, "a": "ମୁନି,", "b": "ମୁନୀ,", "t": None, "chosen": "ମୁନି,", "why": "dispute"}]}}, ensure_ascii=False), encoding="utf-8")
    dec = td / "dec.json"
    dec.write_text(json.dumps([{"vol": 1, "page": 1, "para": 0, "idx": 1, "text": "ମୁନୀ"}], ensure_ascii=False), encoding="utf-8")
    V.cmd_apply(types.SimpleNamespace(decisions=str(dec), dir=str(td), source="model"))
    res = json.loads((td / "v1_p0001.json").read_text(encoding="utf-8"))
    check(res["paragraphs"] == ["ହେ ମୁନୀ, ଆଉ"], "apply: new word, comma kept, [?] gone")
    check(res["_verify"]["flags"][0].get("resolved") == "model" and res["_verify"]["arbitrated"] == [[0, 1]] or res["_verify"]["arbitrated"] == [(0, 1)],
          "apply: flag resolved, word recorded as model-settled")

print("\nPASS" if not fails else f"\n{len(fails)} failures")
sys.exit(1 if fails else 0)
