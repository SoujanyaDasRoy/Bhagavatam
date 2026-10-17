#!/usr/bin/env python3
"""Language audit: walks the main screens of the debug app in a chosen app and reading language and lists every visible
string that is written in the wrong script (Latin letters in a Hindi or Bengali app, and so on) plus clickable items with no label.

    py tools/qa/lang_audit.py --ui HI --read HI --out work_audit [--serial emulator-5554]

Method: set the preferences with `run-as`, start the app, press each tab, press each row of Settings in turn, and read the screen
with `uiautomator dump`. It cannot tell a brand or a book name that is meant to stay Latin (Bhagavatam, Wiktionary, CC BY-SA, Gita
Press), so those are on an allow-list; anything else is printed for a person to judge.
"""
import argparse
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ADB = r"C:\Users\sdroy\AppData\Local\Android\Sdk\platform-tools\adb.exe"
PKG = "com.bhagavatam.app"
ALLOW = re.compile(r"Bhagavatam|Wiktionary|CC BY|Gita Press|Gorakhpur|Telegram|GitHub|Kangra|Shloka|Skandha|Adhyaya|Mahapuran|Mahatmya|Shrimad|IAST|Roman|Sutra|\bOK\b|TTS|Google", re.I)
LATIN = re.compile(r"[A-Za-z]{3,}")


def adb(serial, *a, binary=False):
    cmd = [ADB] + (["-s", serial] if serial else []) + list(a)
    r = subprocess.run(cmd, capture_output=True, env={**__import__("os").environ, "MSYS_NO_PATHCONV": "1"})
    return r.stdout if binary else r.stdout.decode("utf-8", "replace")


def prefs(ui, read, theme="Prabhat"):
    show = {"HI": "showHi", "BN": "showBn", "EN": "showEn", "OR": "showOr"}.get(read, "")
    flags = "".join(f'<boolean name="{k}" value="{"true" if k == show else "false"}" />' for k in ("showHi", "showBn", "showEn", "showOr"))
    return ("<?xml version='1.0' encoding='utf-8' standalone='yes' ?><map>"
            '<boolean name="onboarded" value="true" /><boolean name="tipsEnabled" value="false" /><boolean name="hasRead" value="true" />'
            f'<boolean name="showSanskrit" value="{"true" if read == "SA" else "false"}" />{flags}'
            f'<string name="uiLang">{ui}</string><string name="readLang">{read}</string><string name="theme">{theme}</string>'
            '<int name="lastS" value="10" /><int name="lastA" value="3" /></map>')


def dump(serial):
    adb(serial, "shell", "uiautomator", "dump", "/sdcard/u.xml")
    x = adb(serial, "exec-out", "cat", "/sdcard/u.xml")
    try:
        return ET.fromstring(x)
    except ET.ParseError:
        return None


def texts(root):
    out = []
    for e in root.iter("node"):
        t = (e.get("text") or "").strip()
        d = (e.get("content-desc") or "").strip()
        if t:
            out.append(("text", t))
        if d:
            out.append(("desc", d))
    return out


def rows(root):
    """Clickable nodes in screen order with their centre."""
    out = []
    for e in root.iter("node"):
        if e.get("clickable") == "true":
            x1, y1, x2, y2 = map(int, re.findall(r"\d+", e.get("bounds")))
            if x2 - x1 > 200 and y2 - y1 > 60 and y1 > 250 and y2 < 2250:
                out.append(((x1 + x2) // 2, (y1 + y2) // 2, y1))
    return sorted(out, key=lambda r: r[2])


def tap(serial, x, y):
    adb(serial, "shell", "input", "tap", str(x), str(y))
    time.sleep(1.6)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--ui", required=True)
    ap.add_argument("--read", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--serial", default="")
    a = ap.parse_args()
    out = Path(a.out)
    out.mkdir(parents=True, exist_ok=True)
    tmp = out / "_p.xml"
    tmp.write_text(prefs(a.ui, a.read), encoding="utf-8")
    adb(a.serial, "shell", "am", "force-stop", PKG)
    adb(a.serial, "push", str(tmp), "/data/local/tmp/bh.xml")
    adb(a.serial, "shell", "run-as", PKG, "cp", "/data/local/tmp/bh.xml", "shared_prefs/bhagavatam.xml")
    adb(a.serial, "shell", "am", "start", "-n", f"{PKG}/.MainActivity")
    time.sleep(30)
    seen = {}
    labels = {}

    def grab(name):
        r = dump(a.serial)
        if r is None:
            return
        for kind, t in texts(r):
            seen.setdefault(name, []).append((kind, t))
        for e in r.iter("node"):
            if e.get("clickable") == "true" and not (e.get("text") or e.get("content-desc")) and not any(c.get("text") or c.get("content-desc") for c in e.iter("node")):
                labels.setdefault(name, []).append(e.get("bounds"))

    tabs = [("home", 176), ("content", 420), ("search", 660), ("settings", 904)]
    for name, x in tabs:
        tap(a.serial, x, 2220)
        grab(name)
    # Settings rows, one at a time
    tap(a.serial, 904, 2220)
    r = dump(a.serial)
    rws = rows(r) if r is not None else []
    for i, (x, y, _) in enumerate(rws[:9]):
        tap(a.serial, 904, 2220)
        tap(a.serial, x, y)
        grab(f"settings_{i}")
        if i in (4, 5):
            adb(a.serial, "shell", "input", "swipe", "540", "1800", "540", "700", "300")
            time.sleep(1)
            grab(f"settings_{i}_down")
        adb(a.serial, "shell", "input", "keyevent", "4")
        time.sleep(1)
    # a chapter: open the resume card
    tap(a.serial, 176, 2220)
    tap(a.serial, 540, 600)
    time.sleep(3)
    grab("reader")
    adb(a.serial, "shell", "input", "tap", "540", "180")  # language pill
    time.sleep(2)
    grab("reader_sheet")

    bad = []
    for scr, items in seen.items():
        for kind, t in dict.fromkeys(items):
            if a.ui in ("HI", "BN") and LATIN.search(t) and not ALLOW.search(t):
                bad.append((scr, kind, t))
    print(f"app={a.ui} reading={a.read}: {sum(len(v) for v in seen.values())} strings on {len(seen)} screens")
    print("Latin text in a non-English app (judge each):")
    for scr, kind, t in bad:
        print(f"  [{scr}] {t[:90]}")
    print("Tap targets with no label:", {k: len(v) for k, v in labels.items() if v})
    tmp.unlink(missing_ok=True)


if __name__ == "__main__":
    sys.exit(main())
