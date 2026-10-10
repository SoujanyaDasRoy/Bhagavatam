#!/usr/bin/env python3
"""Unit tests for content/_dict/parse_wiktionary.py.

Runs with:
    py -m unittest content/_dict/tests/test_parse.py
"""
import unittest
from content._dict.parse_wiktionary import (
    clean_wikitext,
    extract_senses_from_wikitext,
    map_pos,
    parse_wiktextract_dump,
)


class TestParseWiktionary(unittest.TestCase):

    def test_clean_wikitext_links_and_entities(self):
        sample = "A [[king|ruler]] &amp; [[monarch]] of the &quot;realm&quot;."
        self.assertEqual(clean_wikitext(sample), 'A ruler & monarch of the "realm".')

    def test_clean_wikitext_templates_and_refs(self):
        sample = "{{gloss|sovereign}} <ref>Historical citation</ref> Supreme leader {{context|formal}}."
        cleaned = clean_wikitext(sample)
        self.assertNotIn("<ref>", cleaned)
        self.assertNotIn("Historical citation", cleaned)
        self.assertNotIn("{{", cleaned)
        self.assertIn("Supreme leader", cleaned)

    def test_clean_wikitext_dashes_and_length(self):
        # Em dash (—) and en dash (–) must be replaced with spaced hyphen " - "
        sample = "God—the creator – and protector"
        cleaned = clean_wikitext(sample)
        self.assertNotIn("—", cleaned)
        self.assertNotIn("–", cleaned)
        self.assertEqual(cleaned, "God - the creator - and protector")

    def test_clean_wikitext_leading_bullets(self):
        sample = "* # : Sovereign ruler of a kingdom."
        self.assertEqual(clean_wikitext(sample), "Sovereign ruler of a kingdom.")

    def test_parse_odia_entry(self):
        # Real snippet from orwiktionary (ଶିଶୁ)
        text = """==ବିଶେଷଣ (ସଂସ୍କୃତ)==
* ଅତି ଅଳ୍ପବୟସ୍କ<!--of tender age-->
==ବିଶେଷ୍ୟ (ସଂସ୍କୃତ)==
* ଜୀବଶାବକ; ଜନ୍ତୁଙ୍କର ଛୁଆ
* ଆଠ ବର୍ଷରୁ ନିମ୍ନବୟସ୍କ ବାଳକବାଳିକା
* ଦୁଧଖିଆ ପିଲା, ଯାହାର ଅନ୍ନପ୍ରାଶନ ହୋଇ ନାଥାଏ
"""
        pos_markers = {
            "ବିଶେଷ୍ୟ": "noun",
            "ବିଶେଷଣ": "adj",
            "କ୍ରିୟା": "verb",
            "ସର୍ବନାମ": "pronoun",
            "ଅବ୍ୟୟ": "ind",
        }
        entries = extract_senses_from_wikitext("ଶିଶୁ", text, "or", pos_markers, "orwikt")
        self.assertGreaterEqual(len(entries), 2)
        # Entry 1: adjective
        self.assertEqual(entries[0]["pos"], "adj")
        self.assertIn("ଅତି ଅଳ୍ପବୟସ୍କ", [g["text"] for g in entries[0]["glosses"]])
        # Entry 2: noun
        self.assertEqual(entries[1]["pos"], "noun")
        texts = [g["text"] for g in entries[1]["glosses"]]
        self.assertTrue(any("ଜୀବଶାବକ" in t for t in texts))
        # Ensure no comments left
        for e in entries:
            for g in e["glosses"]:
                self.assertNotIn("<!--", g["text"])

    def test_parse_bengali_entry(self):
        # Real Bengali wikitext structure
        text = """==বাংলা==
===উচ্চারণ===
* {{আইপিএ|/bʱɔɡoban/}}

===বিশেষ্য===
# পরমেশ্বর; ঈশ্বর; সর্বশক্তিমান।
# পুণ্যবান ব্যক্তি।

===ব্যুৎপত্তি===
[[ভগ]] + [[বৎ]]
"""
        pos_markers = {
            "বিশেষ্য": "noun",
            "বিশেষণ": "adj",
            "ক্রিয়া": "verb",
            "ক্রিয়াবিশেষণ": "adv",
            "সর্বনাম": "pronoun",
            "অব্যয়": "ind",
        }
        entries = extract_senses_from_wikitext("ভগবান", text, "bn", pos_markers, "bnwikt")
        self.assertGreaterEqual(len(entries), 1)
        self.assertEqual(entries[0]["pos"], "noun")
        self.assertEqual(entries[0]["headword"], "ভগবান")
        texts = [g["text"] for g in entries[0]["glosses"]]
        self.assertTrue(any("পরমেশ্বর" in t for t in texts))

    def test_parse_hindi_entry_with_shabdsagar_excluded(self):
        # Real Hindi entry with both standard Wiktionary and Shabdsagar
        text = """==हिन्दी==
===संज्ञा===
# ईश्वर; परमात्मा; प्रभु।
# भाग्यवान व्यक्ति।

==शब्दसागर==
राजा का अर्थ नृप होता है।
"""
        pos_markers = {
            "संज्ञा": "noun",
            "विशेषण": "adj",
            "क्रिया": "verb",
            "सर्वनाम": "pronoun",
            "अव्यय": "ind",
        }
        # In parse_native_xml_dump, Shabdsagar is stripped beforehand
        import re
        stripped_text = re.sub(r"==+\s*शब्दसागर\s*==+.*?(?===|\Z)", "", text, flags=re.DOTALL)
        entries = extract_senses_from_wikitext("भगवान", stripped_text, "hi", pos_markers, "hiwikt")
        self.assertEqual(len(entries), 1)
        self.assertEqual(entries[0]["pos"], "noun")
        self.assertEqual(entries[0]["headword"], "भगवान")
        texts = [g["text"] for g in entries[0]["glosses"]]
        self.assertTrue(any("ईश्वर" in t for t in texts))
        # Shabdsagar text must NOT appear
        self.assertFalse(any("शब्दसागर" in t or "नृप" in t for t in texts))

    def test_map_pos(self):
        self.assertEqual(map_pos("संज्ञा"), "noun")
        self.assertEqual(map_pos("বিশেষণ"), "adj")
        self.assertEqual(map_pos("କ୍ରିୟା"), "verb")
        self.assertEqual(map_pos("adverb"), "adv")
        self.assertEqual(map_pos("unknown"), "noun")


if __name__ == "__main__":
    unittest.main()
