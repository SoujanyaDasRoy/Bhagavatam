import sys, json, glob, re, collections
sys.path.insert(0, "../_odia"); sys.stdout.reconfigure(encoding="utf-8")
import odia_text as T
# lexicon from Skandha 1-3 trusted pages only (pages 88-410): no leakage into the Skandha 4 test
cnt = collections.Counter()
for f in glob.glob("../_odia/vision_transcripts/v1_p*.json"):
    pg = int(re.search(r"p(\d+)", f).group(1))
    if pg > 410: continue
    for para in json.load(open(f, encoding="utf-8"))["paragraphs"]:
        for w in T.tokens(para):
            c = T.core(w)
            if c and not T.is_marker(w): cnt[c] += 1
print("lexicon words", len(cnt), "tokens", sum(cnt.values()))
preds = json.load(open("work/eval_preds.json", encoding="utf-8"))
def deletes(w):
    return {w[:i] + w[i+1:] for i in range(len(w))}
idx = collections.defaultdict(set)
for w in cnt:
    idx[w].add(w)
    for d in deletes(w): idx[d].add(w)
def candidates(w):
    out = set(idx.get(w, ()))
    for d in deletes(w):
        out |= idx.get(d, set())
    return {c for c in out if c != w}
def fix(word):
    c = T.core(word)
    if len(c) < 4 or c in cnt or T.is_marker(word): return None
    cands = [(cnt[k], k) for k in candidates(c) if abs(len(k) - len(c)) <= 1]
    if not cands: return None
    cands.sort(reverse=True)
    top = cands[0]
    if top[0] < 3: return None
    if len(cands) > 1 and top[0] < 3 * cands[1][0]: return None
    return word.replace(c, top[1], 1)
def norm(s):
    s = re.sub(r"\s+([।॥,;:!?)])", r"\1", s); return " ".join(s.split())
changed = right = 0; wer_before = wer_after = n = 0
half = [p for p in preds if int(re.search(r"p(\d+)", p["id"]).group(1)) >= 496]   # second half of Skandha 4
for p in half:
    g = norm(p["gt"]).split(); q = norm(p["pred"]).split()
    q2 = []
    for w in q:
        f = fix(w)
        if f and f != w:
            changed += 1
            # was it right? compare against aligned label words by membership in the gt line
            right += f in g
            q2.append(f)
        else: q2.append(w)
    import difflib
    def werr(a, b): 
        sm = difflib.SequenceMatcher(a=a, b=b, autojunk=False); return len(a) - sum(bl.size for bl in sm.get_matching_blocks())
    wer_before += werr(g, q); wer_after += werr(g, q2); n += len(g)
print(f"words changed {changed}, now equal to a label word in the line: {right} ({100*right/max(1,changed):.1f}%)")
print(f"word mismatches before {wer_before} ({100*wer_before/n:.2f}%), after {wer_after} ({100*wer_after/n:.2f}%) of {n} words")
