"""Translation blocks that the assembly put on the wrong verse, put back on the right one.

verse_moves.json (reviewed by a person; tools/qa/verse_fit.py --fix writes proposals to verse_moves.proposed.json):
  {"bn": {"10.69": {"33": [34], "34": [35], ...}, "10.80": {"12": {"from": 11}}}, "or": {...}}
  each entry: verse -> the verses whose current text it gets, joined in that order (the verse is then no longer marked as
  joined to another, *_from = NULL), or {"from": n}: its text is printed with verse n (one block covers both).
  Every verse named on either side is emptied first.
build_db.py (hi), add_odia.py (or) and add_bengali.py (bn) call apply_moves() after they fill the column, so a rebuild keeps the fix.
"""
import hashlib
import json
from pathlib import Path

FILE = Path(__file__).resolve().parent / "verse_moves.json"


def apply_moves(db, lang, moves=None):
    """Apply the moves for `lang` ("or", "bn" or "hi") to an open content.db; returns how many verses got text."""
    if moves is None:
        if not FILE.exists():
            return 0
        moves = json.loads(FILE.read_text(encoding="utf-8")).get(lang, {})
    col, frm = {"or": ('"or"', "or_from"), "bn": ("bn", "bn_from"), "hi": ("hi", "hi_from")}[lang]
    # moving is not idempotent: the meta table remembers which list was applied, and the same list is not applied twice
    stamp = hashlib.sha1(json.dumps(moves, sort_keys=True).encode()).hexdigest()[:12]
    done = db.execute("SELECT value FROM meta WHERE key = ?", (f"verse_moves_{lang}",)).fetchone()
    if done and done[0] == stamp:
        return 0
    db.execute("INSERT OR REPLACE INTO meta VALUES (?, ?)", (f"verse_moves_{lang}", stamp))
    n = 0
    for key, plan in moves.items():
        s, c = map(int, key.split("."))
        names = {int(k) for k in plan} | {a for v in plan.values() if isinstance(v, list) for a in v}
        text = {a: (db.execute(f"SELECT {col} FROM verse WHERE skandha=? AND chapter=? AND num=?", (s, c, a)).fetchone() or [""])[0] or ""
                for a in names}
        for a in names:
            db.execute(f"UPDATE verse SET {col}='' WHERE skandha=? AND chapter=? AND num=?", (s, c, a))
        for k, src in plan.items():
            if isinstance(src, dict):
                db.execute(f"UPDATE verse SET {col}='', {frm}=? WHERE skandha=? AND chapter=? AND num=?", (src["from"], s, c, int(k)))
                continue
            joined = "\n\n".join(text[a].strip() for a in src if text[a].strip())
            db.execute(f"UPDATE verse SET {col}=?, {frm}=NULL WHERE skandha=? AND chapter=? AND num=?", (joined, s, c, int(k)))
            n += 1
    return n
