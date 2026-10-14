#!/usr/bin/env python3
"""Third look at the words the two readings disagreed on: a stronger model sees the page and the candidates.

    py odia_arbitrate.py run --skandha 5-12 --model gemini-2.5-pro [--workers 2] [--limit N]
    py odia_verify.py apply work_v/arbitration/decisions_model.json --source model     (writes the answers into the consensus pages)
    py odia_arbitrate.py status                                                        (what is settled, what still needs a person)

Only words flagged "dispute", "single" or "lex" in work_v/consensus are sent, a page at a time, with the page image and a few
words of context. The model must pick A, B or T (Tesseract) or write the printed word. High-confidence answers become
decisions; low-confidence or "other" answers stay in the review queue for a person. Every model-settled word is a separate
stratum in the audit (odia_verify.py audit-sample --stratum arbitrated), because "a model decided" is not "checked".
Needs GEMINI_API_KEY (or ANTHROPIC_API_KEY with --backend anthropic), set in the shell.
"""
import argparse
import json
import os
import re
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import odia_text as T  # noqa: E402
import odia_verify as V  # noqa: E402

sys.stdout.reconfigure(encoding="utf-8")
WORK = V.WORK
SEND = {"dispute", "single", "lex"}          # hygiene-only flags are not choices between readings; they go to a person
CONTEXT_BEFORE, CONTEXT_AFTER = 7, 4

PROMPT = """This is one scanned page of the Gita Press Odia edition of the Shrimad Bhagavata Mahapurana: Sanskrit shlokas in Odia script on the left, the Odia translation on the right.
Earlier readings of the TRANSLATION column (right side) disagree on the words listed below. For each item you get the words around it as read so far, with the word in question between >> and <<, and the candidate spellings.
Look at the printed page and decide what is actually printed at that place.
- Answer "A", "B" or "T" if that candidate is exactly the printed word (same letters, same conjuncts, same matras).
- Answer "other" and write the word exactly as printed in "text" if no candidate is exact.
- Never correct, modernise or normalise the spelling: the print is the authority, even where it looks unusual.
- confidence is "high" only if you can clearly see the word; otherwise "low".
Return ONE JSON array and nothing else: [{"id": 1, "choice": "A", "text": "<the printed word>", "confidence": "high"}, ...]

Items:
"""


def build_items(page):
    """Disputed positions of one consensus page: [{id, para, idx, a, b, t, context}]."""
    items = []
    paras = [p.split(" ") for p in page.get("paragraphs", [])]
    for fl in page.get("_verify", {}).get("flags", []):
        kind = str(fl["why"]).split(":")[0]
        if fl.get("para") is None or kind not in SEND or fl.get("resolved"):
            continue
        words = paras[fl["para"]]
        lo, hi = max(0, fl["idx"] - CONTEXT_BEFORE), fl["idx"] + CONTEXT_AFTER + 1
        ctx = [re.sub(r"\[\?+\]$", "", w) for w in words[lo:hi]]
        ctx[fl["idx"] - lo] = ">> " + ctx[fl["idx"] - lo] + " <<"
        items.append({"id": len(items) + 1, "para": fl["para"], "idx": fl["idx"], "a": fl.get("a"), "b": fl.get("b"), "t": fl.get("t"),
                      "context": " ".join(ctx)})
    return items


def prompt_for(items):
    lines = []
    for it in items:
        cands = "; ".join(f"{k.upper()}: {it[k]}" for k in ("a", "b", "t") if it[k] not in (None, ""))
        lines.append(f"{it['id']}. {it['context']}\n   candidates - {cands}")
    return PROMPT + "\n".join(lines)


def parse_answer(text, items):
    """Model text -> {id: (word or None, confident)}. Only answers that name a real candidate or give a word are kept."""
    t = re.sub(r"^```(?:json)?\s*|\s*```$", "", (text or "").strip())
    arr = json.loads(t)
    by_id = {it["id"]: it for it in items}
    out = {}
    for r in arr if isinstance(arr, list) else []:
        it = by_id.get(r.get("id"))
        if not it:
            continue
        ch = str(r.get("choice", "")).strip().upper()
        confident = str(r.get("confidence", "")).lower() == "high"
        if ch in ("A", "B", "T"):
            w = it[ch.lower()]
        elif ch == "OTHER" and r.get("text"):
            w = T.norm(str(r["text"]))
            if not w or " " in w or T.severe(w):
                continue
        else:
            continue
        if w:
            out[it["id"]] = (T.core(w) if ch == "OTHER" else w, confident)
    return out


def ask(client, backend, model, image_path, text):
    last = None
    for attempt in range(4):
        try:
            if backend == "gemini":
                from google.genai import types
                r = client.models.generate_content(
                    model=model, contents=[types.Part.from_bytes(data=image_path.read_bytes(), mime_type="image/png"), text],
                    config=types.GenerateContentConfig(temperature=0, response_mime_type="application/json", max_output_tokens=8192))
                return r.text
            import base64
            r = client.messages.create(model=model, max_tokens=4096, temperature=0, messages=[{"role": "user", "content": [
                {"type": "image", "source": {"type": "base64", "media_type": "image/png", "data": base64.standard_b64encode(image_path.read_bytes()).decode()}},
                {"type": "text", "text": text}]}])
            return "".join(b.text for b in r.content if getattr(b, "type", "") == "text")
        except Exception as e:  # noqa
            last = e
            time.sleep(3 + 5 * attempt)
    raise RuntimeError(f"giving up: {last}")


def cmd_run(a):
    import odia_vision as ov
    cdir = Path(a.dir) if a.dir else WORK / "consensus"
    out_dir = WORK / "arbitration"
    out_dir.mkdir(parents=True, exist_ok=True)
    pages = V.page_ids(cdir)
    keys = sorted(pages)
    if a.skandha:
        spans = ov.skandha_spans(a.skandha)
        keys = [k for k in keys if any(v == k[0] and x <= k[1] <= y for v, x, y in spans)]
    todo = []
    for k in keys:
        j = json.loads(pages[k].read_text(encoding="utf-8"))
        items = build_items(j)
        done = out_dir / f"v{k[0]}_p{k[1]:04d}.json"
        if items and (a.force or not done.exists()):
            todo.append((k, items))
    if a.limit:
        todo = todo[: a.limit]
    print(f"{len(todo)} page(s), {sum(len(i) for _, i in todo)} word(s) to settle with {a.backend}:{a.model}", flush=True)
    if not todo:
        return
    cl = ov.client(a.backend)
    lock, n = threading.Lock(), [0]

    def job(k, items):
        v, p = k
        img = ov.page_image(v, p, ov.LONG_EDGE_B, "images_b")
        got = parse_answer(ask(cl, a.backend, a.model, img, prompt_for(items)), items)
        rec = {"vol": v, "page": p, "model": f"{a.backend}:{a.model}", "items": [
            {**{k2: it[k2] for k2 in ("id", "para", "idx", "a", "b", "t")}, "answer": got.get(it["id"], (None, False))[0],
             "confident": got.get(it["id"], (None, False))[1]} for it in items]}
        (out_dir / f"v{v}_p{p:04d}.json").write_text(json.dumps(rec, ensure_ascii=False, indent=1), encoding="utf-8")

    with ThreadPoolExecutor(a.workers) as ex:
        futs = {ex.submit(job, k, items): k for k, items in todo}
        for f in as_completed(futs):
            k = futs[f]
            try:
                f.result()
                st = "ok"
            except Exception as e:  # noqa
                st = f"FAILED {e}"
                with lock, open(WORK / "errors.log", "a", encoding="utf-8") as fh:
                    fh.write(f"arbitrate v{k[0]} p{k[1]}: {e}\n")
            n[0] += 1
            print(f"{n[0]}/{len(todo)} v{k[0]}_p{k[1]:04d} {st}", flush=True)
    write_decisions()


def write_decisions():
    out_dir = WORK / "arbitration"
    dec, left = [], 0
    for f in sorted(out_dir.glob("v*_p*.json")):
        r = json.loads(f.read_text(encoding="utf-8"))
        for it in r["items"]:
            if it["answer"] and it["confident"]:
                dec.append({"vol": r["vol"], "page": r["page"], "para": it["para"], "idx": it["idx"], "text": it["answer"]})
            else:
                left += 1
    (out_dir / "decisions_model.json").write_text(json.dumps(dec, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{len(dec)} confident answers -> {out_dir / 'decisions_model.json'}; {left} word(s) stay for a person")


def cmd_status(a):
    write_decisions()


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sp = ap.add_subparsers(dest="cmd", required=True)
    r = sp.add_parser("run")
    r.add_argument("--skandha"); r.add_argument("--dir"); r.add_argument("--workers", type=int, default=2); r.add_argument("--limit", type=int)
    r.add_argument("--force", action="store_true"); r.add_argument("--backend", choices=["gemini", "anthropic"])
    r.add_argument("--model")
    sp.add_parser("status")
    a = ap.parse_args()
    if a.cmd == "run":
        a.backend = a.backend or ("gemini" if os.environ.get("GEMINI_API_KEY") else "anthropic")
        a.model = a.model or ("gemini-2.5-pro" if a.backend == "gemini" else "claude-opus-5-5")
    {"run": cmd_run, "status": cmd_status}[a.cmd](a)


if __name__ == "__main__":
    main()
