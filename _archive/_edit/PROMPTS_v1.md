# Bhagavatam — background art prompts

Backgrounds for the Skandha cards, chapter headers and the Home hero. The app works without any of them
(it falls back to the gradient and mandala), so you can add images a few at a time.

## 1. One style for everything

Paste this **style block at the end of every prompt** so all 350+ images look like one set.

```
STYLE: contemplative devotional illustration in the manner of Pahari and Kangra miniature painting,
rendered as soft opaque gouache on aged paper, fine brush outlines, flat layered colour with gentle
gradients, stylised faces with lotus-shaped eyes, ornamental foliage and flowing water, luminous sky.
Reverent, calm and original. Wide 16:10 composition, the main subject right of centre, the left third
calm and uncluttered (sky, water or mist). The lower third a little darker and quieter than the rest.
No text, no letters, no calligraphy, no watermark, no border, no frame, no modern objects, no photographic
realism, no 3D render look, no glossy AI sheen.
```

Why these choices
- The left third and the dark lower third are where the app places its title and numbers. White text stays readable on them.
- "No text" matters. Image models spell Sanskrit badly; the app draws all lettering itself.
- Each Skandha has one colour. Mention it in the prompt and the card, gradient and image will agree.

Sizes: generate at 1600 × 1000 (or the nearest 16:10). `tools/import_art.py` resizes and converts to WebP (about 60–120 KB each).
Naming: `sk_01` … `sk_12`, `sk_00` (Mahatmya), `home_hero`, and `ch_<skandha>_<chapter>` (for example `ch_10_29`).
Save the files in `content/art/raw/` with those names (.png or .jpg), then run `python tools/import_art.py`.

Respectful depiction: show the Lord and sages in the traditional, devotional manner of the miniature school. If a tool refuses or
produces something that feels wrong, prefer symbols (lotus, conch, flute, peacock feather, pillar, ocean, flame) over faces.

## 2. The Skandha prompts (do these first: 14 images)

Each line is the **scene**; add the style block after it.

| File | Skandha and colour | Scene |
|---|---|---|
| `sk_00` | Mahatmya, antique gold | Narada, a sage with a vina, meets a weary young woman Bhakti and her two ageing sons on a river bank at dawn; golden light, lotus pond, a quiet temple spire in the distance. |
| `sk_01` | 1, saffron orange | Sages seated in a circle around a sacrificial fire in the forest of Naimisharanya at dusk, a lone narrator on a raised seat, smoke rising into an orange sky. |
| `sk_02` | 2, golden yellow | A vast cosmic form suggested as layered heavens and oceans in a gold sky, tiny figures in meditation at the bottom, stars drawn as lotus petals. |
| `sk_03` | 3, ochre | The divine boar lifting the Earth, a green disc with rivers and mountains, from a dark primordial ocean; spray and lotus clouds in an ochre dawn. |
| `sk_04` | 4, leaf green | A small boy meditating alone under a great tree in a forest at night, one bright pole star above him, soft green fireflies and sleeping deer. |
| `sk_05` | 5, jade green | Mount Meru rising through concentric continents and seas, seen from above like a mandala, with a sage and a gentle deer in the foreground. |
| `sk_06` | 6, deep teal | Two groups of messengers, bright and shadowed, meeting over a kneeling old man in a quiet courtyard; teal twilight, a conch and a rope. |
| `sk_07` | 7, vermilion | A great stone pillar splitting open at twilight, a fierce lion-faced form emerging in flame, a child with folded hands calm in the foreground. |
| `sk_08` | 8, deep blue | The churning of the ocean: a mountain as the churning rod, a great serpent as the rope, gods and demons pulling on both sides, a tortoise beneath, waves and a rising moon. |
| `sk_09` | 9, amber | Sunrise over a royal city with a river, banners and a bow resting on a throne; two dynasties suggested by a sun and a moon in the same sky. |
| `sk_10` | 10, indigo blue | A moonlit Vrindavan grove by the Yamuna, kadamba trees, cows resting, a flute and a peacock feather on a rock, a circle of dancing figures in the distance. |
| `sk_11` | 11, rose pink | Dwarka by the sea at sunset, two friends seated in quiet conversation on a terrace, rose and gold sky, gulls over the water. |
| `sk_12` | 12, violet | A sage speaking to a king on the Ganga bank at dusk, a violet sky with a long line of cranes, a flame fading on the horizon. |
| `home_hero` | Home screen, indigo to plum | A lamp and an open palm-leaf manuscript on a low wooden stand, a lotus beside it, a deep indigo and plum night sky with small stars, space on the left. |

## 3. Chapter prompts (341 images, optional)

Generate the full list from the book itself:

```
python tools/make_chapter_prompts.py
```

It writes `content/art/chapter_prompts.csv` with columns `file, skandha, chapter, title, prompt`. Each prompt uses the chapter's own English
title and the colour of its Skandha, so the set stays consistent. Work through it in batches; the most-read chapters first
(the app's "Well-loved stories" use these):

`ch_10_29`, `ch_8_2`, `ch_7_8`, `ch_4_8`, `ch_10_3`, `ch_8_6`, `ch_10_25`, `ch_1_8`, `ch_6_1`, `ch_8_18`, `ch_10_80`, `ch_11_7`

Hand-written scenes for those twelve (add the style block):

| File | Scene |
|---|---|
| `ch_10_29` | Autumn moonlit night in a forest, cowherd women walking toward the sound of a flute, a circle of dancers around a single dark blue figure, the river shining. |
| `ch_8_2` | An elephant in a lotus lake seized by a crocodile, lifting a lotus up in prayer, a bright figure on a bird descending from a gold sky. |
| `ch_7_8` | A split stone pillar in a palace hall at twilight, a lion-man form of light and flame, a king's mace falling, a calm child seated at the side. |
| `ch_4_8` | A five-year-old boy walking into a forest with a sage showing the way, a great tree, evening light, a single star. |
| `ch_10_3` | A prison cell at midnight lit by a soft blue glow, a mother and father with folded hands, a new-born on a lotus, rain outside. |
| `ch_8_6` | Gods and demons holding a truce beside the milk ocean, a tall mountain being lifted, a coiled serpent, a rising moon. |
| `ch_10_25` | A boy lifting a mountain on one finger like an umbrella, cowherds and cows sheltering beneath, a storm of dark clouds above. |
| `ch_1_8` | A queen in white standing with folded hands in a chariot-lit courtyard, a fiery arrow in the sky, a protective blue light around her. |
| `ch_6_1` | An old man on his deathbed calling out to a child, two shining messengers arriving at the door, a dim lamp. |
| `ch_8_18` | A small boy in sacred thread with an umbrella and staff before a great king at a sacrificial ground, a single stride stretching across the sky. |
| `ch_10_80` | A poor brahmin carrying a small bundle of flattened rice to a golden palace gate, a smiling king rising to greet him. |
| `ch_11_7` | A quiet seaside terrace at evening, a teacher and a student seated close, an old city behind them, the sea calm. |

## 4. Tips for the image tool

- Generate 4 variants, keep the one whose left third is calmest.
- If text appears anywhere, regenerate. Never paint over it.
- If faces look generic, add "stylised Pahari miniature faces" and remove "realistic".
- Keep the same tool and settings for the whole set; consistency matters more than any single image.
- Rights: use a tool whose licence allows commercial or app use even if the app is free, and keep the tool name and date in `content/art/CREDITS.md`.
