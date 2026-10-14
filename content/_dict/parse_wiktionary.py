#!/usr/bin/env python3
"""Task 3 of the dictionary plan: parse native Wiktionary XML dumps and Wiktextract JSONL to normalised JSONL.

Output format per line in JSONL:
{
  "lang": "hi",                      # "hi" | "bn" | "or" | "en"
  "headword": "भगवान",
  "pos": "noun",                     # "noun" | "verb" | "adj" | "adv" | etc.
  "glosses": [
    {"lang": "hi", "text": "ईश्वर, परमात्मा"},
    {"lang": "en", "text": "God, deity, lord"}
  ],
  "etymology": null,
  "ipa": "/bʱəɡ.ʋɑːn/",
  "forms": ["भगवानों", "भगवान्"],
  "source": "hiwikt"                 # source code matching sources.json
}

Rules:
- Strip wiki markup: [[a|b]] -> b, [[a]] -> a, remove templates {{...}}, <ref>...</ref>, HTML entities.
- No em dashes (—) or en dashes (–): replaced by " - " (project rule 2).
- Glosses cut at 240 characters (plan 3.3).
- Shabdsagar text inside Hindi Wiktionary is omitted (DECISIONS.md rule 6).
"""
import argparse
import bz2
import gzip
import html
import json
import re
import sys
import unicodedata
import xml.etree.ElementTree as ET
from pathlib import Path

HERE = Path(__file__).resolve().parent
WORK = HERE / "work"
DUMPS = WORK / "dumps"

RE_REF = re.compile(r"<ref[^>]*>.*?</ref>|<ref[^>]*/>", re.IGNORECASE | re.DOTALL)
RE_COMMENT = re.compile(r"<!--.*?-->", re.DOTALL)
RE_HTML_TAG = re.compile(r"</?[a-zA-Z][^>]*>")
RE_CATEGORY = re.compile(r"\[\[(?:Category|শ্রেণী|श्रेणी|ଶ୍ରେଣୀ|File|চিত্র|चित्र|ଫାଇଲ):[^\]]+\]\]", re.IGNORECASE)
RE_FILE = re.compile(r"\[\[(?:Image|File|চিত্র|चित्र|ଫାଇଲ):[^\]]+\]\]", re.IGNORECASE)
RE_LINK = re.compile(r"\[\[([^\|\]]+)(?:\|([^\]]+))?\]\]")
RE_DASHES = re.compile(r"[\u2013\u2014]")  # en dash and em dash


def norm_unicode(s: str) -> str:
    """Normalize string using NFC, remove zero-width joiners/non-joiners."""
    if not s:
        return ""
    return unicodedata.normalize("NFC", s).replace("\u200c", "").replace("\u200d", "").strip()


def clean_wikitext(text: str, max_chars: int = 240) -> str:
    """Strip all wikitext markup, templates, refs, HTML tags, unescape HTML entities, replace dashes."""
    if not text:
        return ""

    # Replace HTML entities first or decode
    text = html.unescape(text)

    # Remove comments and refs
    text = RE_COMMENT.sub("", text)
    text = RE_REF.sub("", text)

    # Remove categories and file links
    text = RE_CATEGORY.sub("", text)
    text = RE_FILE.sub("", text)

    # Resolve wiki links: [[target|text]] -> text, [[target]] -> target
    def repl_link(m):
        target = m.group(1).strip()
        disp = m.group(2)
        if disp is not None:
            return disp.strip()
        return target

    text = RE_LINK.sub(repl_link, text)

    # Resolve templates: iterate to handle nested templates {{...}}
    # Extract useful parameters from common templates
    for _ in range(5):
        if "{{" not in text:
            break

        def repl_template(m):
            inner = m.group(1).strip()
            parts = inner.split("|")
            tname = parts[0].strip().lower()

            # Mentions / links: {{m|lang|word}} or {{l|lang|word}}
            if tname in ("m", "l", "link") and len(parts) >= 3:
                return parts[2].strip()
            # Gloss: {{gloss|text}}
            if tname == "gloss" and len(parts) >= 2:
                return f"({parts[1].strip()})"
            # Label/qualifier: {{lb|...}}, {{label|...}}, {{context|...}}
            if tname in ("lb", "label", "context", "qualifier", "q") and len(parts) >= 2:
                label_words = [p.strip() for p in parts[2:] if p.strip() and "=" not in p]
                if label_words:
                    return f"({', '.join(label_words)})"
                return ""
            # Synonyms / antonyms / translations markers
            if tname in ("audio", "rfdef", "t+", "t-", "t"):
                return ""
            # Default: if there's a simple text parameter, use it, or empty
            for p in reversed(parts[1:]):
                p_clean = p.strip()
                if p_clean and "=" not in p_clean and not p_clean.startswith("lang="):
                    if len(p_clean) < 40 and not p_clean.isdigit():
                        return p_clean
            return ""

        text = re.sub(r"\{\{([^{}]+)\}\}", repl_template, text)

    # Clean leftover curly braces or brackets if any
    text = re.sub(r"\{+|\}+", "", text)
    text = re.sub(r"\[+|\]+", "", text)

    # Strip HTML tags
    text = RE_HTML_TAG.sub("", text)

    # Replace em and en dashes with " - "
    text = RE_DASHES.sub(" - ", text)

    # Collapse repeated whitespace
    text = re.sub(r"\s+", " ", text).strip()

    # Strip leading bullets or punctuation from lists
    text = re.sub(r"^[\*\#\:\;\-\–\—\s\.\,\;]+", "", text).strip()

    # Cut to max length cleanly at word boundary if possible
    if len(text) > max_chars:
        cut = text[:max_chars]
        last_space = cut.rfind(" ")
        if last_space > max_chars * 0.7:
            text = cut[:last_space].rstrip(",; -")
        else:
            text = cut.rstrip(",; -")

    return norm_unicode(text)


def map_pos(pos_str: str) -> str:
    """Map language-specific or detailed POS names to canonical POS."""
    p = pos_str.strip().lower()
    if p in ("संज्ञा", "বিশেষ্য", "ବିଶେଷ୍ୟ", "noun", "proper noun", "নামবিশেষ্য"):
        return "noun"
    if p in ("विशेषण", "বিশেষণ", "ବିଶେଷଣ", "adj", "adjective"):
        return "adj"
    if p in ("क्रिया", "ক্রিয়া", "କ୍ରିୟା", "verb"):
        return "verb"
    if p in ("क्रियाविशेषण", "क्रिया विशेषण", "ক্রিয়াবিশেষণ", "କ୍ରିୟାବିଶେଷଣ", "adv", "adverb"):
        return "adv"
    if p in ("सर्वनाम", "সর্বনাম", "ସର୍ବନାମ", "pron", "pronoun"):
        return "pronoun"
    if p in ("अव्यय", "অব্যয়", "ଅବ୍ୟୟ", "indecl", "indeclinable"):
        return "ind"
    if p in ("অনন্বয়ী অব্যয়", "विस्मयादिबोधक", "interj", "interjection"):
        return "interj"
    return "noun"


def parse_native_xml_dump(bz2_path: Path, lang: str, source_code: str, vocab_filter: set[str] = None):
    """Yield parsed entries from a native Wiktionary mediawiki XML dump."""
    # State tracking
    pos_markers = {
        "hi": {"संज्ञा": "noun", "विशेषण": "adj", "क्रिया": "verb", "क्रियाविशेषण": "adv", "सर्वनाम": "pronoun", "अव्यय": "ind"},
        "bn": {"বিশেষ্য": "noun", "বিশেষণ": "adj", "ক্রিয়া": "verb", "ক্রিয়াবিশেষণ": "adv", "সর্বনাম": "pronoun", "অব্যয়": "ind"},
        "or": {"ବିଶେଷ୍ୟ": "noun", "ବିଶେଷଣ": "adj", "କ୍ରିୟା": "verb", "କ୍ରିୟାବିଶେଷଣ": "adv", "ସର୍ବନାମ": "pronoun", "ଅବ୍ୟୟ": "ind"},
    }[lang]

    with bz2.open(bz2_path, "rt", encoding="utf-8", errors="replace") as f:
        title = ""
        in_text = False
        text_lines = []

        for line in f:
            if "<title>" in line:
                m = re.search(r"<title>(.*?)</title>", line)
                if m:
                    title = m.group(1).strip()
            elif "<text" in line:
                in_text = True
                text_lines = [line]
            elif in_text:
                text_lines.append(line)
                if "</text>" in line:
                    in_text = False
                    # Filter out namespaces (colons), talk pages, templates
                    if not title or ":" in title or "/" in title:
                        continue
                    if title.startswith("Wiktionary:") or title.startswith("Help:"):
                        continue

                    headword = norm_unicode(title)
                    norm_word = headword.lower()
                    if vocab_filter is not None and norm_word not in vocab_filter:
                        continue

                    full_text = "".join(text_lines)
                    # Skip redirects
                    if "#REDIRECT" in full_text.upper() or "#পুনর্নির্দেশন" in full_text or "#पुनर्निर्देशन" in full_text:
                        continue

                    # Shabdsagar exclusion for Hindi (DECISIONS.md #6)
                    if lang == "hi":
                        # If the entire page is only Shabdsagar or section has Shabdsagar, strip it
                        full_text = re.sub(r"==+\s*शब्दसागर\s*==+.*?(?===|\Z)", "", full_text, flags=re.DOTALL)
                        if "शब्दसागर" in full_text and len(full_text.strip()) < 100:
                            continue

                    entries = extract_senses_from_wikitext(headword, full_text, lang, pos_markers, source_code)
                    for entry in entries:
                        yield entry


def extract_senses_from_wikitext(headword: str, text: str, lang: str, pos_markers: dict, source_code: str) -> list[dict]:
    """Parse sections, parts of speech, numbered senses and pronunciation."""
    entries = []
    lines = text.splitlines()

    current_pos = None
    current_section = "other"  # "pos" | "pronunciation" | "etymology" | "other"
    current_senses = []
    ipa = None
    etymology = None

    non_pos_keywords = {
        "উচ্চারণ": "pronunciation", "उच्चारण": "pronunciation", "ଉଚ୍ଚାରଣ": "pronunciation", "pronunciation": "pronunciation",
        "ব্যুৎপত্তি": "etymology", "व्युत्पत्ति": "etymology", "etymology": "etymology",
        "সমার্থক": "other", "पर्यायवाची": "other", "ପ୍ରତିଶବ୍ଦ": "other", "synonym": "other",
        "অনুবাদ": "other", "अनुवाद": "other", "translation": "other",
        "টীকা": "other", "सन्दर्भ": "other", "রেফারেন্স": "other", "references": "other",
    }

    for line in lines:
        stripped = line.strip()
        if not stripped:
            continue

        # Check section headings or template headers
        if stripped.startswith("==") or (stripped.startswith("{{-") and stripped.endswith("-}}")):
            # If we had senses for a POS, save that entry
            if current_pos and current_senses:
                entries.append({
                    "lang": lang,
                    "headword": headword,
                    "pos": current_pos,
                    "glosses": [{"lang": lang, "text": s} for s in current_senses[:6]],
                    "etymology": etymology,
                    "ipa": ipa,
                    "forms": [],
                    "source": source_code,
                })
                current_senses = []

            # Template POS header: {{-noun-}}, {{-संज्ञा-}}, etc.
            if stripped.startswith("{{-") and stripped.endswith("-}}"):
                t_pos = stripped[3:-3].strip()
                current_pos = map_pos(t_pos)
                current_section = "pos"
                continue

            # Determine new section type
            heading_lower = stripped.lower()
            matched_non_pos = False
            for kw, sec_type in non_pos_keywords.items():
                if kw in heading_lower:
                    current_section = sec_type
                    matched_non_pos = True
                    break

            if not matched_non_pos:
                # Check if it's a POS section
                matched_pos = False
                for pos_name, canonical_pos in pos_markers.items():
                    if pos_name in stripped:
                        current_pos = canonical_pos
                        current_section = "pos"
                        matched_pos = True
                        break
                if not matched_pos and current_pos is None:
                    current_section = "other"

            continue

        # Extract IPA
        if "{{ipa|" in stripped.lower() or "{{আইপিএ|" in stripped or "{{आईपीए|" in stripped:
            m_ipa = re.search(r"\{\{(?:ipa|আইপিএ|आईपीए)\|[^\|]*\|?([^\}]+)\}\}", stripped, re.IGNORECASE)
            if m_ipa and not ipa:
                ipa_raw = m_ipa.group(1).split("|")[-1].strip()
                if "/" in ipa_raw or "[" in ipa_raw:
                    ipa = ipa_raw

        # In etymology section, capture text if not already captured
        if current_section == "etymology" and not etymology:
            cleaned_etym = clean_wikitext(stripped)
            if cleaned_etym and len(cleaned_etym) >= 3:
                etymology = cleaned_etym

        # Senses belong to POS sections
        if current_section == "pos" and (stripped.startswith("#") or stripped.startswith("*")):
            if not stripped.startswith("##") and not stripped.startswith("**"):
                cleaned = clean_wikitext(stripped)
                if cleaned and len(cleaned) >= 2:
                    current_senses.append(cleaned)

        # Fallback: if in translation section, capture English translations as senses
        if current_section == "other" and ("{{en}}" in stripped or "{{en:" in stripped):
            cleaned = clean_wikitext(stripped)
            if cleaned and len(cleaned) >= 2 and len(current_senses) < 3:
                current_senses.append(cleaned)

    # Flush last entry
    if current_pos and current_senses:
        entries.append({
            "lang": lang,
            "headword": headword,
            "pos": current_pos,
            "glosses": [{"lang": lang, "text": s} for s in current_senses[:6]],
            "etymology": etymology,
            "ipa": ipa,
            "forms": [],
            "source": source_code,
        })

    return entries


def parse_wiktextract_dump(jsonl_gz_path: Path, target_lang: str, source_code: str, vocab_filter: set[str] = None):
    """Yield parsed entries from a Kaikki Wiktextract JSONL.gz file."""
    with gzip.open(jsonl_gz_path, "rt", encoding="utf-8", errors="replace") as f:
        for line in f:
            if not line.strip():
                continue
            try:
                data = json.loads(line)
            except json.JSONDecodeError:
                continue

            word = data.get("word")
            if not word or ":" in word:
                continue

            headword = norm_unicode(word)
            norm_word = headword.lower()

            # Forms
            forms = []
            for f_item in data.get("forms", []):
                f_val = f_item.get("form")
                if f_val and f_val != headword and len(f_val) < 60:
                    # Filter out metadata tags
                    if not any(tag in f_val for tag in ("table-tags", "decl", "conj", "stem")):
                        forms.append(norm_unicode(f_val))

            if vocab_filter is not None:
                # Keep if headword is in vocab_filter, or if any form is in vocab_filter
                if norm_word not in vocab_filter and not any(f.lower() in vocab_filter for f in forms):
                    continue

            pos = map_pos(data.get("pos", "noun"))

            # Senses & glosses
            glosses = []
            for s in data.get("senses", []):
                raw_gloss_list = s.get("glosses") or s.get("raw_glosses") or []
                for g in raw_gloss_list:
                    cg = clean_wikitext(g)
                    if cg and len(cg) >= 2:
                        glosses.append({"lang": "en", "text": cg})
                        break  # 1 gloss per sense
                if len(glosses) >= 6:
                    break

            if not glosses:
                continue

            # Sounds / IPA
            ipa = None
            for snd in data.get("sounds", []):
                if snd.get("ipa"):
                    ipa = snd["ipa"]
                    break

            yield {
                "lang": target_lang,
                "headword": headword,
                "pos": pos,
                "glosses": glosses,
                "etymology": clean_wikitext(data.get("etymology_text", "")) or None,
                "ipa": ipa,
                "forms": list(dict.fromkeys(forms)),
                "source": source_code,
            }
