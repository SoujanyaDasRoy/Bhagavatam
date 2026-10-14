import json, sys, os
f = "work/key_audit/judged.json"
d = json.load(open(f)) if os.path.exists(f) else {}
# usage: rec.py "0-11" "10:ଲ" ...   (range correct by default, listed ids wrong with optional correction)
rng = sys.argv[1]; a, b = [int(x) for x in rng.split("-")]
wrong = {}
for t in sys.argv[2:]:
    i, _, c = t.partition(":"); wrong[int(i)] = c
for n in range(a, b + 1):
    d[str(n)] = {"ok": n not in wrong, "correction": wrong.get(n, "")}
json.dump(d, open(f, "w"), ensure_ascii=False)
w = sum(1 for v in d.values() if not v["ok"])
print(f"judged {len(d)}, wrong {w}")
