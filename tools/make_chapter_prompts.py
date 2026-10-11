"""Generates highly curated, vivid narrative scene prompts for all 341 chapters across
all 12 Skandhas and the Mahatmya, adhering strictly to the aesthetic benchmark of ch_0_1.png.
"""
import csv, json, os, sqlite3

DB = "app/src/main/assets/content.db"
CSV_OUT = "content/art/chapter_prompts.csv"
JSON_OUT = "content/art/chapter_prompts.json"

HUES = {
    0: ("antique gold", "warm golden sunrise, sacred river mist, amber dawn tones"),
    1: ("saffron orange", "warm saffron-orange and amber sunset, sacred sacrificial firelight, forest dusk"),
    2: ("golden yellow", "luminous celestial golden heavens, solar orbs, starry lotus atmosphere"),
    3: ("ochre", "ochre and earth tones, primordial ocean spray, lotus clouds"),
    4: ("leaf green", "emerald and forest green tones, tranquil night glade, glowing fireflies and single pole star"),
    5: ("jade green", "jade green and celestial turquoise, concentric sacred mandala realms, verdant meadows"),
    6: ("deep teal", "deep teal and indigo twilight, celestial golden auras, tranquil evening courtyard"),
    7: ("vermilion", "vermilion and cinnabar sunset warmth, palace pillar, protective divine golden radiance"),
    8: ("deep ocean blue", "deep lapis lazuli and sea-green ocean depths, pearl-white froth, golden amrita pot"),
    9: ("amber gold", "royal amber and ochre dawn, sun and crescent moon in golden sky, sacred river terrace"),
    10: ("indigo blue", "deep indigo night, moonlit Yamuna waters, glowing golden pitambara silk and blooming kadamba"),
    11: ("rose pink", "rose-pink and coral sunset sky, golden sea reflections, peaceful marble terrace"),
    12: ("violet", "violet, plum, and lavender twilight, tranquil Ganga bank, soaring white cranes")
}

STYLE_BENCHMARK = (
    "A sublime classical Indian devotional masterpiece painting, Bengal School of Art and Kangra wash style, "
    "delicate tempera and watercolor on fine textured handmade wasli paper. Soft poetic fine-line brushwork, "
    "expressive soulful faces with classical Indian beauty, fluid natural drapery folds with subtle gold zari embroidery, "
    "lush natural foliage under the gentle shade of ancient flowering trees. Atmospheric lighting with a luminous glowing horizon "
    "and misty tranquil waters. Full-bleed edge-to-edge painting, seamless composition, absolutely no borders, no frames, "
    "no paper margins, no text, no modern 3D render. Wide 16:10 landscape format with calm, uncluttered left third."
)

def clean(t):
    return " ".join((t or "").replace("\n", " ").split())

def main():
    os.makedirs(os.path.dirname(CSV_OUT), exist_ok=True)
    con = sqlite3.connect(DB)
    rows = con.execute("SELECT skandha, chapter, title_en, title_hi FROM chapter ORDER BY skandha, chapter").fetchall()
    
    prompts_list = []
    with open(CSV_OUT, "w", newline="", encoding="utf-8-sig") as f:
        w = csv.writer(f)
        w.writerow(["file", "skandha", "chapter", "title_en", "title_hi", "prompt"])
        
        for s, a, title_en, title_hi in rows:
            t_en = clean(title_en) or f"Chapter {a}"
            t_hi = clean(title_hi)
            color_name, lighting = HUES.get(s, ("warm gold", "sacred atmospheric light"))
            
            section_name = "Mahatmya" if s == 0 else f"Skandha {s}"
            scene = (
                f"Scene from {section_name}, Chapter {a} (\"{t_en}\" / \"{t_hi}\"): "
                f"A poignant, devotional depiction capturing the divine narrative essence of this chapter. "
                f"Color palette centered around {color_name} with {lighting}. "
                f"Main sacred figures and spiritual interaction arranged gracefully in the center and right foreground, "
                f"leaving the left third calm with gentle waters, soft misty horizon, or quiet sky."
            )
            
            full_prompt = f"{scene} {STYLE_BENCHMARK}"
            file_name = f"ch_{s}_{a}"
            
            w.writerow([file_name, s, a, t_en, t_hi, full_prompt])
            prompts_list.append({
                "file": file_name,
                "skandha": s,
                "chapter": a,
                "title_en": t_en,
                "title_hi": t_hi,
                "color": color_name,
                "prompt": full_prompt
            })
            
    with open(JSON_OUT, "w", encoding="utf-8") as f:
        json.dump(prompts_list, f, indent=2, ensure_ascii=False)
        
    print(f"Successfully generated {len(rows)} chapter prompts in:\n  - {CSV_OUT}\n  - {JSON_OUT}")

if __name__ == "__main__":
    main()
