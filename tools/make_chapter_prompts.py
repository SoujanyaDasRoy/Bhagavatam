"""Writes content/art/chapter_prompts.csv: one image prompt per chapter, from the titles in content.db.
Run from the project root:  python tools/make_chapter_prompts.py
"""
import csv, os, sqlite3

DB = "app/src/main/assets/content.db"
OUT = "content/art/chapter_prompts.csv"

HUE = {0: "antique gold", 1: "saffron orange", 2: "golden yellow", 3: "ochre", 4: "leaf green", 5: "jade green",
       6: "deep teal", 7: "vermilion", 8: "deep blue", 9: "amber", 10: "indigo blue", 11: "rose pink", 12: "violet"}

STYLE = ("STYLE: contemplative devotional illustration in the manner of Pahari and Kangra miniature painting, soft opaque gouache on aged paper, "
         "fine brush outlines, flat layered colour with gentle gradients, stylised lotus-eyed faces, ornamental foliage and flowing water, luminous sky. "
         "Reverent, calm and original. Wide 16:10 composition, main subject right of centre, left third calm and uncluttered, lower third slightly darker. "
         "No text, no letters, no watermark, no border, no modern objects, no photographic realism, no 3D render look.")

def clean(t):
    return " ".join((t or "").replace("\n", " ").split())

def main():
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    con = sqlite3.connect(DB)
    rows = con.execute("select skandha, chapter, title_en from chapter order by skandha, chapter").fetchall()
    with open(OUT, "w", newline="", encoding="utf-8-sig") as f:
        w = csv.writer(f)
        w.writerow(["file", "skandha", "chapter", "title", "prompt"])
        for s, a, title in rows:
            title = clean(title) or f"Chapter {a}"
            scene = (f"A scene that evokes the chapter titled \"{title}\" from the Shrimad Bhagavata Purana. "
                     f"Show one clear moment or place from that story, not a crowd. Dominant colour: {HUE.get(s, 'gold')}.")
            w.writerow([f"ch_{s}_{a}", s, a, title, f"{scene} {STYLE}"])
    print(f"{len(rows)} prompts -> {OUT}")

if __name__ == "__main__":
    main()
