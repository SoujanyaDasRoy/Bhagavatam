#!/usr/bin/env python3
"""Make the Bengali text of every verse look like the Hindi text: the translation only.

The Gita Press Bengali pages print, in one column, the speaker line ("রাজোবাচ"), the Sanskrit verse in Bengali script, a
footnote line and then the translation. The first extraction kept all of it inside verse.bn. The app already shows the Sanskrit
in its own layer, so here it is removed from the translation:
  1. footnote lines that stand alone, like "(১) ক্রাঞ্চল।"     (listed in the report, not silently lost)
  2. speaker lines that stand alone ("...উবাচ", "রাজোবাচ")
  3. the Sanskrit verse of that same row, found from verse.sa written in Bengali script, up to its printed verse number
  4. a verse number left at the very end of the text ("১২ ।।"): Hindi has none
Nothing else is changed. Run:
    py content/_build/clean_bn_text.py content/content.db --dry-run        report only
    py content/_build/clean_bn_text.py content/content.db                  write (a backup is kept next to the database)
Also called by add_bengali.apply(), so a rebuild keeps the cleaning.
"""
import difflib
import re
import shutil
import sqlite3
import sys

BN_DIGITS = "০-৯"
FOOTNOTE = re.compile(r"(?m)^[ \t]*\([" + BN_DIGITS + r"0-9]+\)[ \t]*[^\n]{0,30}$\n?")
SPEAKER = re.compile(r"(?m)^[ \t]*[^\n]{0,25}(?:উবাচ|ঊবাচ|বাচ)[ \t]*$\n?")
MARK = re.compile(r"[॥।]+\s*[" + BN_DIGITS + r"]+(?:\s*[-–]\s*[" + BN_DIGITS + r"]+)*\s*[।॥]+")
TRAIL_MARK = re.compile(r"\s*[" + BN_DIGITS + r"]+\s*[।॥]+\s*$")
WORD = re.compile(r"[ঀ-৿]+")


_SPECIAL = {"व": "ব", "ळ": "ল", "ऽ": "ঽ"}


def to_bengali(deva: str) -> str:
    """Devanagari letters to the Bengali block (by offset, va written as ba): enough to find the verse in the text, not for display."""
    out = []
    for c in deva:
        if c in _SPECIAL:
            out.append(_SPECIAL[c])
        elif 0x0900 <= ord(c) <= 0x097F and c not in "।॥":
            out.append(chr(ord(c) + 0x80))
        else:
            out.append(c)
    return "".join(out)


def norm(s: str) -> str:
    """Spelling variants that are the same letter in print: ya with a dot, nukta, visarga-less final m."""
    return s.replace("য়", "য").replace("়", "").replace("ৎ", "ত্")


def clean(bn: str, sa: str):
    """Returns (cleaned text, what was removed) for one verse."""
    removed = []
    text = bn.replace("\r\n", "\n")

    for m in list(FOOTNOTE.finditer(text)):
        removed.append(("footnote", m.group(0).strip()))
    text = FOOTNOTE.sub("", text)
    for m in list(SPEAKER.finditer(text)):
        removed.append(("speaker", m.group(0).strip()))
    text = SPEAKER.sub("", text)

    # The row's Sanskrit may begin with the speaker line ("राजोवाच"); that is not part of the verse itself.
    sa_lines = [l for l in (sa or "").split("\n") if not re.search(r"वाच\s*$", l.strip())]
    words = WORD.findall(to_bengali("\n".join(sa_lines)))
    if len(words) >= 4:
        compact = norm(re.sub(r"\s+", " ", text))
        # The opening words of the verse are spelled the same in the text; the last word often differs (anusvara or m).
        pos = compact.find(words[0] + " " + words[1])
        if pos < 0 and len(words[0]) >= 6:
            pos = compact.find(words[0])
        sa_flat = "".join(words)
        # Try where the opening words are, then the very start of the text (the Sanskrit often comes first).
        for start in ([pos] if pos >= 0 else []) + ([0] if pos != 0 else []):
            end_m = MARK.search(compact, start)
            if not end_m or end_m.end() - start > 450:
                continue
            chunk = compact[start:end_m.end()]
            # The cut must look like this verse: similar letters (print and spelling differ a little) and a similar length.
            chunk_flat = "".join(WORD.findall(MARK.sub("", chunk)))
            ratio = difflib.SequenceMatcher(None, chunk_flat, sa_flat, autojunk=False).ratio()
            rest = (compact[:start] + " " + compact[end_m.end():]).strip()
            # Never leave a verse empty: if only the Sanskrit is stored, the translation is missing and that is reported, not hidden.
            if ratio >= 0.62 and 0.6 <= len(chunk_flat) / max(1, len(sa_flat)) <= 1.6 and len(rest) >= 20:
                removed.append(("sanskrit", chunk.strip()))
                text = (compact[:start] + " " + compact[end_m.end():]).strip()
                break

    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{3,}", "\n\n", text).strip()
    m = TRAIL_MARK.search(text)
    if m and removed is not None:
        removed.append(("marker", m.group(0).strip()))
        text = text[:m.start()].rstrip()
        if text and text[-1] not in "।?!\"”’)":
            text += "।"
    return text, removed


def run(db_path: str, dry: bool = False, backup: bool = True):
    db = sqlite3.connect(db_path)
    rows = db.execute("SELECT rowid, skandha, chapter, num, sa, bn FROM verse WHERE bn IS NOT NULL AND bn <> ''").fetchall()
    changed, kinds, report = 0, {}, []
    updates = []
    for rid, s, c, n, sa, bn in rows:
        new, removed = clean(bn, sa or "")
        if new != bn:
            changed += 1
            for k, _ in removed:
                kinds[k] = kinds.get(k, 0) + 1
            updates.append((new, rid))
            if any(k != "marker" for k, _ in removed):
                report.append((s, c, n, removed))
    if not dry and updates:
        if backup:
            shutil.copy(db_path, db_path + ".bak_bn")
        db.executemany("UPDATE verse SET bn=? WHERE rowid=?", updates)
        db.commit()
    db.close()
    return len(rows), changed, kinds, report


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    total, changed, kinds, report = run(args[0], dry="--dry-run" in sys.argv, backup="--no-backup" not in sys.argv)
    print(f"{total} Bengali verses, {changed} changed; removed: {kinds}")
    for s, c, n, removed in report:
        for k, t in removed:
            if k != "marker":
                print(f"  {s}.{c}.{n} {k}: {t[:110]}")
