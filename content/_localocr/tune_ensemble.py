import sys, re, json, glob, collections, difflib
sys.path.insert(0, "."); sys.path.insert(0, "../_odia"); sys.stdout.reconfigure(encoding="utf-8")
import cv2, crnn, odia_text as T
from ensemble import Reader, Lexicon
models = sys.argv[1].split(",")
pens = [float(x) for x in sys.argv[2].split(",")]
cnt = collections.Counter()
for f in glob.glob("../_odia/vision_transcripts/v1_p*.json"):
    if int(re.search(r"p(\d+)", f).group(1)) > 410: continue
    for para in json.load(open(f, encoding="utf-8"))["paragraphs"]:
        for w in T.tokens(para):
            c = T.core(w)
            if c and not T.is_marker(w): cnt[c] += 1
lex = Lexicon(cnt)
ev = [(p, t) for p, t in crnn.load_lines("eval") if int(re.search(r"p(\d+)", p.name).group(1)) >= 496][:800]
arr = [cv2.imread(str(p), cv2.IMREAD_GRAYSCALE) for p, _ in ev]
def norm(s):
    s = re.sub(r"\s+([।॥,;:!?)])", r"\1", s); return " ".join(s.split())
def wer(preds):
    e = n = 0
    for (_, t), q in zip(ev, preds):
        a, b = norm(t).split(), norm(q).split()
        sm = difflib.SequenceMatcher(a=a, b=b, autojunk=False)
        e += len(a) - sum(bl.size for bl in sm.get_matching_blocks()); n += len(a)
    return 100 * e / n
for pen in pens:
    r = Reader(models, lex if pen > 0 else None, penalty=pen)
    out = r.read_lines(arr)
    print(f"models {models}  penalty {pen}: word mismatch {wer(out):.2f}% over {len(ev)} lines", flush=True)
