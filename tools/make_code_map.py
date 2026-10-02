#!/usr/bin/env python3
"""Rebuild the Bhagavatam code map with Graphify (code only: no API key, no LLM).

One-time:   pip install "graphifyy[sql]"
Each time:  python tools/make_code_map.py
Output:     graphify-out/graph.html (open in a browser), graph.json, GRAPH_REPORT.md
It copies only the code into a temporary folder (app source, build scripts, Bengali tool, DB schema) so the
12,000+ content files, fonts and APKs are not scanned.
"""
import shutil, sqlite3, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "graphify-out"
tmp = Path(tempfile.mkdtemp(prefix="bhagavatam_map_"))

shutil.copytree(ROOT / "app" / "src", tmp / "frontend_app" / "src",
                ignore=shutil.ignore_patterns("*.ttf", "*.db", "*.png", "*.webp", "font"))
for f in ("app/build.gradle.kts", "build.gradle.kts", "settings.gradle.kts"):
    if (ROOT / f).exists():
        shutil.copy(ROOT / f, tmp / "frontend_app" / Path(f).name)
(tmp / "backend_build").mkdir()
for f in (ROOT / "content" / "_build").glob("*.py"):
    shutil.copy(f, tmp / "backend_build" / f.name)
bn = ROOT / "content" / "_bengali" / "bn_extract" / "bn_extract.py"
if bn.exists():
    (tmp / "backend_bengali").mkdir()
    shutil.copy(bn, tmp / "backend_bengali" / bn.name)
db = ROOT / "app" / "src" / "main" / "assets" / "content.db"
if db.exists():
    (tmp / "database").mkdir()
    con = sqlite3.connect(f"file:{db.as_posix()}?mode=ro", uri=True)
    sql = "-- content.db schema (bundled in the APK as assets/content.db)\n" + "\n".join(
        r[0] + ";" for r in con.execute("select sql from sqlite_master where sql is not null order by type desc, name"))
    (tmp / "database" / "schema.sql").write_text(sql, encoding="utf-8")

subprocess.run([sys.executable, "-m", "graphify", "update", str(tmp), "--force"], check=False)
src = tmp / "graphify-out"
if src.exists():
    OUT.mkdir(exist_ok=True)
    for f in ("graph.html", "graph.json", "GRAPH_REPORT.md"):
        if (src / f).exists():
            shutil.copy(src / f, OUT / f)
    print("Map written to", OUT)
shutil.rmtree(tmp, ignore_errors=True)
