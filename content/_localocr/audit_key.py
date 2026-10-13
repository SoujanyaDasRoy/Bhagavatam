#!/usr/bin/env python3
"""P0 gate: how clean is the Skandha 1 to 4 transcript that the recogniser would be trained on?

    py content/_localocr/audit_key.py --n 300 --seed 11     -> content/_localocr/work/key_audit/audit/audit.html
Open that file in a browser, mark each shown word Correct or Wrong against the page image beside it (keys: Enter = correct),
press "Download answers", save the file as content/_localocr/work/key_audit/audit/results.json, then:
    py content/_odia/odia_verify.py audit-score content/_localocr/work/key_audit/audit/results.json
Gate: the 95% upper bound of the error rate is at most 1%. Reuses the Odia audit tools; writes nothing outside work/ (ignored by git).
"""
import argparse
import os
import sys
import types
from pathlib import Path

HERE = Path(__file__).resolve().parent
ODIA = HERE.parent / "_odia"
WORK = HERE / "work" / "key_audit"
os.environ["ODIA_VWORK"] = str(WORK)                                   # the Odia tools read this at import time
os.environ.setdefault("ODIA_PDF_1", str(Path.home() / "Downloads" / "Srimad-Bhagavata-Mahapurana-Gita-Press-Odia-Part-1.pdf"))
sys.path.insert(0, str(ODIA))
sys.stdout.reconfigure(encoding="utf-8")
import odia_review as R  # noqa: E402
import odia_verify as V  # noqa: E402


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--n", type=int, default=300)
    ap.add_argument("--seed", type=int, default=11)
    a = ap.parse_args()
    trusted = ODIA / "vision_transcripts"
    V.cmd_audit_sample(types.SimpleNamespace(dir=str(trusted), n=a.n, seed=a.seed, stratum="agreed"))
    import json
    import odia_vision as ov
    items = json.loads((WORK / "audit" / "items.json").read_text(encoding="utf-8"))
    pages = sorted({(it["vol"], it["page"]) for it in items})
    print(f"rendering {len(pages)} page image(s) ...")
    for v, p in pages:
        ov.page_image(v, p)
    R.cmd_audit(types.SimpleNamespace(dir=str(trusted)))
    print(f"\nOpen: {WORK / 'audit' / 'audit.html'}")


if __name__ == "__main__":
    main()
