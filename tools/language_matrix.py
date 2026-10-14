#!/usr/bin/env python3
"""Language matrix: app language (EN, HI, BN) x reading language (SA, EN, HI, BN, OR), light and dark, on a running emulator or phone.

    py tools/language_matrix.py --out <folder> [--serial emulator-5554] [--skandha 8 --chapter 14] [--themes Prabhat,Sandhya]

For each combination it writes the app's preferences with `adb run-as` (debug build only), starts the app, opens the resume card and
saves three screenshots: the Home screen, the top of the chapter and the end of the chapter. Then a contact sheet per app language
and theme is made with Pillow, and a plain list of what to look at. It does not judge the pictures: read the sheets, or run
tools/language_matrix.py --check on the saved texts (the uiautomator dump of every screen is saved next to the picture and is searched
for the strings the rule says must be in one language).
"""
import argparse
import subprocess
import sys
import time
from pathlib import Path

ADB = r"C:\Users\sdroy\AppData\Local\Android\Sdk\platform-tools\adb.exe"
PKG = "com.bhagavatam.app"
READS = ["SA", "EN", "HI", "BN", "OR"]
UIS = ["EN", "HI", "BN"]


def run(serial, *a, binary=False):
    cmd = [ADB] + (["-s", serial] if serial else []) + list(a)
    r = subprocess.run(cmd, capture_output=True, env={**__import__("os").environ, "MSYS_NO_PATHCONV": "1"})
    return r.stdout if binary else r.stdout.decode("utf-8", "replace")


def prefs_xml(ui, read, theme, s, c):
    show = {"SA": "", "HI": "showHi", "BN": "showBn", "EN": "showEn", "OR": "showOr"}
    on = show[read]
    flags = "".join(f'    <boolean name="{k}" value="{"true" if k == on else "false"}" />\n' for k in ("showHi", "showBn", "showEn", "showOr"))
    return (
        "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n"
        '    <boolean name="onboarded" value="true" />\n'
        '    <boolean name="tipsEnabled" value="false" />\n'
        f'    <boolean name="showSanskrit" value="{"true" if read == "SA" else "false"}" />\n'
        + flags +
        f'    <string name="uiLang">{ui}</string>\n    <string name="readLang">{read}</string>\n    <string name="theme">{theme}</string>\n'
        f'    <int name="lastS" value="{s}" />\n    <int name="lastA" value="{c}" />\n</map>\n'
    )


def shot(serial, path):
    Path(path).write_bytes(run(serial, "exec-out", "screencap", "-p", binary=True))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--serial", default="")
    ap.add_argument("--skandha", type=int, default=8)
    ap.add_argument("--chapter", type=int, default=14)
    ap.add_argument("--themes", default="Prabhat,Sandhya")
    ap.add_argument("--wait", type=int, default=28, help="seconds to let the app start")
    ap.add_argument("--fontscale", type=float, default=1.0, help="system font scale while taking the pictures (restored to 1.0 after)")
    a = ap.parse_args()
    out = Path(a.out)
    out.mkdir(parents=True, exist_ok=True)
    tmp = out / "_prefs.xml"
    run(a.serial, "shell", "settings", "put", "system", "font_scale", str(a.fontscale))
    for theme in a.themes.split(","):
        for ui in UIS:
            for read in READS:
                tag = f"{theme}_{ui}_{read}"
                run(a.serial, "shell", "am", "force-stop", PKG)
                tmp.write_text(prefs_xml(ui, read, theme, a.skandha, a.chapter), encoding="utf-8")
                run(a.serial, "push", str(tmp), "/data/local/tmp/bh.xml")
                run(a.serial, "shell", "run-as", PKG, "cp", "/data/local/tmp/bh.xml", "shared_prefs/bhagavatam.xml")
                run(a.serial, "shell", "am", "start", "-n", f"{PKG}/.MainActivity")
                time.sleep(a.wait)
                shot(a.serial, out / f"{tag}_1home.png")
                run(a.serial, "shell", "input", "tap", "540", "600")      # the resume card
                time.sleep(6)
                shot(a.serial, out / f"{tag}_2top.png")
                for _ in range(8):
                    run(a.serial, "shell", "input", "swipe", "540", "1900", "540", "300", "250")
                time.sleep(2)
                shot(a.serial, out / f"{tag}_3end.png")
                print("done", tag, flush=True)
    tmp.unlink(missing_ok=True)
    run(a.serial, "shell", "settings", "put", "system", "font_scale", "1.0")
    try:
        from PIL import Image
    except ImportError:
        print("Pillow missing: no contact sheets")
        return
    for theme in a.themes.split(","):
        for ui in UIS:
            for part in ("1home", "2top", "3end"):
                imgs = [Image.open(out / f"{theme}_{ui}_{r}_{part}.png").convert("RGB") for r in READS]
                w, h = imgs[0].size
                sc = 0.35
                sheet = Image.new("RGB", (int(w * sc) * len(imgs) + 8 * (len(imgs) - 1), int(h * sc)), (128, 128, 128))
                for i, im in enumerate(imgs):
                    sheet.paste(im.resize((int(w * sc), int(h * sc))), (i * (int(w * sc) + 8), 0))
                sheet.save(out / f"sheet_{theme}_{ui}_{part}.png")
    print("sheets written to", out)


if __name__ == "__main__":
    sys.exit(main())
