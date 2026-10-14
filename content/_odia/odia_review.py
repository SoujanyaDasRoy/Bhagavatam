#!/usr/bin/env python3
"""A local web page for the person who reads Odia: the page image next to the words that need a decision.

    py odia_review.py queue [--limit 300] [--why dispute,hygiene,single,omission]   words where the readings disagree
    py odia_review.py audit                                                         the random sample of AGREED words
Open the file it prints (work_v/review/queue.html or work_v/audit/audit.html) in a browser. No server, no network.
Your answers are kept in the browser while you work; "Download" saves them as a JSON file:
    queue  -> work_v/review/decisions.json   then   py odia_verify.py apply work_v/review/decisions.json
    audit  -> work_v/audit/results.json      then   py odia_verify.py audit-score work_v/audit/results.json

Keys: 1 = take reading A, 2 = take B, 3 = take Tesseract, Enter = keep the chosen word, N / P = next / previous item.
The page images come from work_v/images (written by odia_vision.py); if one is missing the item still shows its text.
The generated page contains book text: it stays in work_v/ and is never committed.
"""
import argparse
import html
import json
import os
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import odia_text as T  # noqa: E402
import odia_verify as V  # noqa: E402

sys.stdout.reconfigure(encoding="utf-8")
WORK = V.WORK

PAGE = """<!doctype html><html lang="or"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>__TITLE__</title><style>
body{font:16px/1.5 system-ui,"Noto Sans Oriya","Nirmala UI",sans-serif;margin:0;background:#f4f1ea;color:#1c1b19}
header{position:sticky;top:0;background:#fff;border-bottom:1px solid #d8d2c4;padding:8px 14px;display:flex;gap:12px;align-items:center;z-index:5}
header b{flex:1}button,input{font:inherit}button{padding:6px 12px;border:1px solid #9a8f78;background:#fff;border-radius:6px;cursor:pointer}
button.on{background:#8a4b12;color:#fff;border-color:#8a4b12}main{display:grid;grid-template-columns:minmax(300px,1fr) 1fr;gap:14px;padding:14px}
@media(max-width:900px){main{grid-template-columns:1fr}}
.img{background:#fff;border:1px solid #d8d2c4;padding:6px;max-height:88vh;overflow:auto}.img img{width:100%;display:block}
.card{background:#fff;border:1px solid #d8d2c4;border-radius:8px;padding:14px}.ctx{font-size:20px;line-height:1.9;margin:8px 0 14px}
mark{background:#ffd54a;padding:0 3px;border-radius:3px}.opts{display:flex;flex-wrap:wrap;gap:8px;margin:8px 0}.opt{font-size:20px}
.why{display:inline-block;background:#eee;border-radius:10px;padding:1px 9px;font-size:13px;margin-right:6px}.done{color:#2f6b2f}
input[type=text]{font-size:20px;padding:5px 8px;border:1px solid #9a8f78;border-radius:6px;min-width:12em}
</style></head><body>
<header><b>__TITLE__</b><span id="pos"></span><button onclick="go(-1)">Previous</button><button onclick="go(1)">Next</button>
<button onclick="save()">Download answers</button></header>
<main><div class="img"><img id="img" alt="page image"></div><div class="card" id="card"></div></main>
<script>
const MODE="__MODE__", ITEMS=__ITEMS__, KEY="odia_review_"+MODE+"_"+ITEMS.length;
let ans=JSON.parse(localStorage.getItem(KEY)||"{}"), cur=0;
const esc=s=>String(s??"").replace(/[&<>]/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;"}[c]));
function ctx(it){const w=it.words.map((x,i)=>i===it.idx?"<mark>"+esc(x)+"</mark>":esc(x));return w.join(" ");}
function draw(){const it=ITEMS[cur];document.getElementById("pos").textContent=(cur+1)+" / "+ITEMS.length+"  ("+Object.keys(ans).length+" answered)";
 document.getElementById("img").src=it.image||"";let h="<div><span class=why>"+esc(it.why)+"</span>v"+it.vol+" page "+it.page+"</div><div class=ctx>"+ctx(it)+"</div>";
 const a=ans[cur];
 if(MODE==="queue"){h+="<div class=opts>";[["1","A",it.a],["2","B",it.b],["3","T",it.t]].forEach(([k,n,v])=>{if(v!=null)h+="<button class='opt "+(a&&a.text===v?"on":"")+"' onclick='pick("+JSON.stringify(v)+")'>"+k+" &nbsp;"+n+": "+esc(v)+"</button>";});
  h+="</div><div>or type the word as printed: <input type=text id=free value='"+esc(a&&a.free||"")+"'> <button onclick='pickFree()'>Use it</button></div>";}
 else{h+="<div class=opts><button class='opt "+(a&&a.ok===true?"on":"")+"' onclick='mark(true)'>Correct (Enter)</button><button class='opt "+(a&&a.ok===false?"on":"")+"' onclick='mark(false)'>Wrong</button></div>"+
  "<div>if wrong, the word as printed: <input type=text id=free value='"+esc(a&&a.free||"")+"'></div>";}
 if(a)h+="<p class=done>answered</p>";document.getElementById("card").innerHTML=h;}
function pick(t){ans[cur]={text:t};store();go(1);}
function pickFree(){const v=document.getElementById("free").value.trim();if(v){ans[cur]={text:v,free:v};store();go(1);}}
function mark(ok){const v=(document.getElementById("free")||{}).value||"";ans[cur]={ok:ok,free:v||undefined};store();go(1);}
function store(){localStorage.setItem(KEY,JSON.stringify(ans));}
function go(d){cur=Math.max(0,Math.min(ITEMS.length-1,cur+d));draw();}
function save(){const out=ITEMS.map((it,i)=>{const a=ans[i];if(!a)return null;
  return MODE==="queue"?{vol:it.vol,page:it.page,para:it.para,idx:it.idx,text:a.text}:{vol:it.vol,page:it.page,para:it.para,idx:it.idx,word:it.word,ok:a.ok,correction:a.free};}).filter(Boolean);
 const b=new Blob([JSON.stringify(out,null,1)],{type:"application/json"});const l=document.createElement("a");l.href=URL.createObjectURL(b);
 l.download=MODE==="queue"?"decisions.json":"results.json";l.click();}
document.addEventListener("keydown",e=>{if(e.target.tagName==="INPUT")return;const it=ITEMS[cur];
 if(MODE==="queue"){if(e.key==="1"&&it.a!=null)pick(it.a);if(e.key==="2"&&it.b!=null)pick(it.b);if(e.key==="3"&&it.t!=null)pick(it.t);}
 else if(e.key==="Enter")mark(true);
 if(e.key==="n")go(1);if(e.key==="p")go(-1);});
draw();
</script></body></html>"""


def load_page(d, vol, page):
    f = d / f"v{vol}_p{page:04d}.json"
    return json.loads(f.read_text(encoding="utf-8")) if f.exists() else None


def image_rel(out_dir, vol, page):
    img = WORK / "images" / f"v{vol}_p{page:04d}.png"
    return os.path.relpath(img, out_dir).replace("\\", "/") if img.exists() else ""


def build(items, mode, out_path, title):
    out_path.parent.mkdir(parents=True, exist_ok=True)
    doc = PAGE.replace("__TITLE__", html.escape(title)).replace("__MODE__", mode)
    doc = doc.replace("__ITEMS__", json.dumps(items, ensure_ascii=False).replace("</", "<\\/"))
    out_path.write_text(doc, encoding="utf-8")


def cmd_queue(a):
    cdir = Path(a.dir) if a.dir else WORK / "consensus"
    want = set(a.why.split(",")) if a.why else None
    out_dir = WORK / "review"
    items = []
    for (v, p), f in sorted(V.page_ids(cdir).items()):
        j = json.loads(f.read_text(encoding="utf-8"))
        paras = [x.split(" ") for x in j.get("paragraphs", [])]
        for fl in j.get("_verify", {}).get("flags", []):
            kind = str(fl["why"]).split(":")[0]
            if fl["para"] is None or fl.get("resolved") or (want and kind not in want):
                continue
            items.append({"vol": v, "page": p, "para": fl["para"], "idx": fl["idx"], "why": fl["why"], "a": fl["a"], "b": fl["b"], "t": fl["t"],
                          "words": paras[fl["para"]], "image": image_rel(out_dir, v, p),
                          "_prio": {"hygiene": 0, "dispute": 1, "single": 2, "lex": 3}.get(kind, 9), "_score": j["_verify"].get("score", 1)})
    items.sort(key=lambda x: (x["_prio"], x["_score"], x["vol"], x["page"], x["para"], x["idx"]))
    items = items[: a.limit]
    if not items:
        sys.exit("nothing to review (no flagged words in " + str(cdir) + ")")
    out = out_dir / "queue.html"
    build(items, "queue", out, f"Odia review: {len(items)} words")
    print(f"{len(items)} words -> {out}")


def cmd_audit(a):
    items_f = WORK / "audit" / "items.json"
    if not items_f.exists():
        sys.exit("run: py odia_verify.py audit-sample --n 200 first")
    cdir = Path(a.dir) if a.dir else WORK / "consensus"
    out_dir = WORK / "audit"
    items = []
    for it in json.loads(items_f.read_text(encoding="utf-8")):
        j = load_page(cdir, it["vol"], it["page"])
        if not j:
            continue
        words = j["paragraphs"][it["para"]].split(" ")
        items.append({**it, "words": words, "why": "agreed by both readings", "image": image_rel(out_dir, it["vol"], it["page"])})
    out = out_dir / "audit.html"
    build(items, "audit", out, f"Odia audit: {len(items)} agreed words")
    print(f"{len(items)} words -> {out}")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sp = ap.add_subparsers(dest="cmd", required=True)
    q = sp.add_parser("queue"); q.add_argument("--limit", type=int, default=300); q.add_argument("--why"); q.add_argument("--dir")
    u = sp.add_parser("audit"); u.add_argument("--dir")
    a = ap.parse_args()
    {"queue": cmd_queue, "audit": cmd_audit}[a.cmd](a)


if __name__ == "__main__":
    main()
