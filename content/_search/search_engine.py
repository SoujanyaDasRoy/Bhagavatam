#!/usr/bin/env python3
"""Reference reader of search.db: the behaviour the Kotlin SearchIndex must reproduce (plan section 3.3).

    from search_engine import Index
    ix = Index()                       # opens content/_search/search.db and reads the stories from Episodes.kt
    ix.verses("Krishna")               # set of verse ids (rowids of content.db), every word must be present
    ix.chapters(hit, "verse", query)   # set of (skandha, chapter), plus the chapters of matching stories

What one query word expands to (union of posting lists):
  1. its loose key                                          (spelling and script variants meet)
  2. the loose keys of every alias group the key belongs to  (Narasimha / Nrisimha / नृसिंह)
  3. the exact forms of every related-word group the exact form belongs to (alligator / मगर / ग्राह)
  4. if all of that is empty: the nearest loose keys, one edit away (typos: Krshna -> Krishna)
Words are ANDed. scope="chapter" ANDs at chapter level instead (words may sit in different verses of one chapter).
"""
import bisect
import re
import sqlite3
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
sys.path.insert(0, str(HERE))
import build_index as bi  # noqa: E402  (posting codec and the key functions, one copy)

SEARCH_DB = HERE / "search.db"
EPISODES = ROOT / "app" / "src" / "main" / "java" / "com" / "bhagavatam" / "app" / "data" / "Episodes.kt"
TOKEN = re.compile(r"[^\s\"“”'‘’.,;:!?()]+")


def parse_reference(q: str):
    m = re.match(r"^(\d+)[.:/\-\s]+(\d+)(?:[.:/\-\s]+(\d+))?$", q.strip())
    if not m or not 0 <= int(m.group(1)) <= 12:
        return None
    return [int(m.group(1)), int(m.group(2)), int(m.group(3)) if m.group(3) else None]


def read_stories(path: Path = EPISODES):
    """(skandha, chapter, [title and keyword strings]) for every Episode in Episodes.kt: the single source of the stories."""
    src = path.read_text(encoding="utf-8")
    out = []
    for m in re.finditer(r'Episode\(\s*s = (\d+), a = (\d+), en = "([^"]*)", hi = "([^"]*)", bn = "([^"]*)"(.*?)\n        \)', src, re.S):
        s, a, en, hi, bn, rest = m.groups()
        kw = re.search(r"keywords = listOf\(([^)]*)\)", rest)
        words = re.findall(r'"([^"]*)"', kw.group(1)) if kw else []
        out.append((int(s), int(a), [en, hi, bn] + words))
    return out


def edits_le1(a: str, b: str) -> bool:
    """True if a and b differ by at most one insertion, deletion or substitution."""
    if a == b:
        return True
    la, lb = len(a), len(b)
    if abs(la - lb) > 1:
        return False
    i = 0
    while i < min(la, lb) and a[i] == b[i]:
        i += 1
    if la == lb:
        return a[i + 1:] == b[i + 1:]
    if la > lb:
        return a[i + 1:] == b[i:]
    return a[i:] == b[i + 1:]


class Index:
    def __init__(self, db_path: Path = SEARCH_DB):
        self.db = sqlite3.connect(str(db_path))
        self.starts = [r[0] for r in self.db.execute("select first_id from chapter_start order by idx")]
        self.chap = [(r[0], r[1]) for r in self.db.execute("select skandha, chapter from chapter_start order by idx")]
        self.stories = [(s, a, {k for t in texts for w in re.findall(r"[^\W\d_]+", t) if len(k := bi.key_of(w)) >= bi.MIN_KEY})
                        for s, a, texts in read_stories()]
        self.keys = None  # loaded on the first typo lookup: (key, df) sorted by key

    # ------------------------------------------------------------ lookups
    def chapter_of(self, vid: int):
        return self.chap[bisect.bisect_right(self.starts, vid) - 1]

    def _loose(self, key: str):
        r = self.db.execute("select post from loose where key=?", (key,)).fetchone()
        return bi.decode_posting(r[0]) if r else []

    def _exact(self, form: str):
        r = self.db.execute("select post from exact where form=?", (form,)).fetchone()
        return bi.decode_posting(r[0]) if r else []

    def _group_members(self, term: str, kind: str):
        gs = [g for (g,) in self.db.execute("select g from grp_by_term where term=? and kind=?", (term, kind))]
        for g in gs:
            yield from self.db.execute("select kind, term from grp where g=?", (g,))

    def expand(self, word: str) -> set:
        """All verse ids a single query word stands for (rules 1 to 4 of the module docstring)."""
        return self.lookup(word)[0]

    def lookup(self, word: str):
        """(verse ids, corrected loose key or None). The key is set only when rule 4 (typo) supplied the result: the UI says so."""
        key, ex = bi.key_of(word), bi.exact_of(word)
        keys, forms = {key}, {ex}
        for kind, term in list(self._group_members(key, "k")) + list(self._group_members(ex, "f")):
            (keys if kind == "k" else forms).add(term)
        ids = set()
        for k in keys:
            if len(k) >= bi.MIN_KEY:
                ids.update(self._loose(k))
        for f in forms:
            ids.update(self._exact(f))
        if not ids and len(key) >= 4:
            for cand in self.suggest(key):
                return set(self._loose(cand[0])), cand[0]
        return ids, None

    def suggest(self, key: str, limit: int = 3):
        """Loose keys one edit away from `key`, most frequent first: [(key, df, display forms)]."""
        if self.keys is None:
            self.keys = list(self.db.execute("select key, df from loose order by key"))
        out = [(k, df) for k, df in self.keys if abs(len(k) - len(key)) <= 1 and k[:1] == key[:1] and k != key and edits_le1(k, key)]
        # a dropped letter is the commonest slip, so longer candidates come first; then the commoner word
        out.sort(key=lambda kd: (0 if len(kd[0]) > len(key) else 1 if len(kd[0]) == len(key) else 2, -kd[1], kd[0]))
        res = []
        for k, df in out[:limit]:
            f = self.db.execute("select forms from loose where key=?", (k,)).fetchone()
            res.append((k, df, f[0] if f else None))
        return res

    # ------------------------------------------------------------ queries
    def words(self, query: str):
        return TOKEN.findall(query.strip()) if len(query.strip()) >= 2 else []

    def verses(self, query: str, scope: str = "verse") -> set:
        ws = self.words(query)
        if not ws:
            return set()
        sets = []
        for w in ws:
            ids = self.expand(w)
            if scope == "chapter":
                ids = {self.chapter_of(v) for v in ids}
            sets.append(ids)
        return set.intersection(*sets)

    def chapters(self, hit, scope: str = "verse", query: str = "") -> set:
        out = set(hit) if scope == "chapter" else {self.chapter_of(v) for v in hit}
        out |= self.story_chapters(query)
        return out

    def story_chapters(self, query: str) -> set:
        keys = {bi.key_of(w) for w in self.words(query)}
        keys.discard("")
        if not keys:
            return set()
        return {(s, a) for s, a, sk in self.stories if all(k in sk for k in keys)}

    def title_chapters(self, query: str) -> set:
        sets = []
        for w in self.words(query):
            r = self.db.execute("select post from title_key where key=?", (bi.key_of(w),)).fetchone()
            sets.append({self.chap[i] for i in bi.decode_posting(r[0])} if r else set())
        return set.intersection(*sets) if sets else set()


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    ix = Index()
    for q in sys.argv[1:] or ["Krishna", "Krshna", "Rasa Lila", "crocodile elephant"]:
        v = ix.verses(q)
        c = ix.chapters(v, "verse", q)
        print(f"{q!r}: {len(v)} verses, {len(c)} chapters, stories {sorted(ix.story_chapters(q))}, suggest {ix.suggest(bi.key_of(q.split()[0]))[:2]}")
