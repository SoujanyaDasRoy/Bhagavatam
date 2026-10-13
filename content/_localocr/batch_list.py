import json, sys, collections
sys.stdout.reconfigure(encoding="utf-8")
W = "work/key_audit"
items = json.load(open(W + "/audit/items.json", encoding="utf-8"))
by = collections.OrderedDict()
for n, it in enumerate(items):
    by.setdefault((it["vol"], it["page"]), []).append((n, it))
pages = list(by)
a, b = int(sys.argv[1]), int(sys.argv[2])
for key in pages[a:b]:
    v, p = key
    j = json.load(open(f"../_odia/vision_transcripts/v{v}_p{p:04d}.json", encoding="utf-8"))
    print(f"## v{v}_p{p:04d}  image: {W}/images/v{v}_p{p:04d}.png")
    for n, it in by[key]:
        words = j["paragraphs"][it["para"]].split(" ")
        i = it["idx"]
        ctx = " ".join(words[max(0, i-5):i] + [">>" + words[i] + "<<"] + words[i+1:i+4])
        print(f"  #{n}: {ctx}")
print(f"(pages {a}-{b} of {len(pages)})")
