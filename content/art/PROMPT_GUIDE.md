# How to write the chapter prompts (for ChatGPT Go or the API)

Two parts: text you set up **once** in a ChatGPT Project, and a **short prompt per chapter**.

## Part 1. Set up once (Project "Bhagavatam paintings")
Put `ch_0_1.png` in the Project's files and paste this into the Project's instructions:

```
You paint chapter illustrations for a devotional reading app. For every message I send, make ONE new image and nothing else: no questions, no suggestions.

STYLE (always): use the attached picture ch_0_1 only as the style reference (palette, brushwork, light, paper texture, level of detail), never copy its scene.
Classical Indian devotional painting in the Bengal School / Kangra wash manner: soft tempera and gouache on aged cream paper with visible grain, fine brush outlines,
slender figures with long lotus-shaped eyes and calm faces, flowing drapery in saffron, indigo, cream and madder red with small gold patterns, fine jewellery.
One low warm light source with a glowing horizon and its reflection, hazy blue-green atmospheric distance. Reverent, serene, luminous.

FORMAT (always): wide landscape 3:2. Main figures in the right half. The LEFT THIRD is calm sky, water or mist with no figures and nothing important.
The bottom edge a little darker and quieter. Keep everything important inside the centre of the picture.

NEVER: text, letters, numerals, signatures, borders, frames, modern objects, photographic realism, 3D look, glossy AI sheen, blood or gore, nudity, extra fingers or limbs.

CHARACTERS (use exactly when named): 
Krishna: dark blue skin, yellow silk, peacock feather in the crown, flute. Balarama: fair skin, blue silk. 
Vishnu: four arms with conch, discus, mace and lotus, dark blue skin, yellow silk. Narada: slim sage with a vina, sacred thread, saffron cloth, tilak. 
Suta: elderly sage with a white beard on a raised seat. Shukadeva: radiant young ascetic, long hair, simple ochre cloth. Parikshit: young king, simple crown, folded hands. 
Brahma: four faces, white beard, seated on a lotus. Shiva: matted hair, crescent moon, ash on the body, a serpent at the neck. Lakshmi: golden-yellow sari, lotus in hand.
```

## Part 2. One message per chapter
Keep each message to **40 to 90 words**, in this shape (the code is for your own list; ChatGPT ignores it):

```
ch_10_29. SCENE: <one picture: who, where, the single action>. TIME AND LIGHT: <time of day, colour of the light>. 
FIGURES: <who is where, at most four>. MOOD: <one word>.
```

### Rules for the scene line
1. **One picture, one action.** "Gopis walk through a moonlit forest toward a flute", not "the story of the Rasa dance".
2. **Concrete nouns, not ideas.** Say lotus lake, conch, pillar, chariot, banyan, river steps. Never "devotion", "dharma", "surrender".
3. **At most four figures**, and say where they stand ("the sage seated on the right, the king kneeling in front of him").
4. **Say the light.** Dawn gold, noon white-gold, sunset saffron, moonlit indigo, firelight. Use the Skandha's colour:
   1 saffron, 2 golden yellow, 3 ochre, 4 leaf green, 5 jade, 6 teal, 7 vermilion, 8 deep blue, 9 amber, 10 indigo, 11 rose pink, 12 violet; Mahatmya antique gold.
5. **Violence and the moment:** show the moment before or after, with glow instead of harm ("the lion-faced form of light and flame stands at the split pillar; the child stands calm").
6. **Names help, but describe too.** The Characters lines in the Project fix the looks; the scene says what they do.
7. **No words to paint.** Never ask for a sign, a name or a verse in the picture.

### Chapters that are teaching, hymns or family lines (about 4 in 10 of the 341)
These have no single action. Use one of five **frames**, change the place, time and light each time so they do not look alike:
- *The telling:* Shukadeva seated on a raised mat on the Ganga bank, Parikshit and listening sages in front, a banyan above.
- *The sages' circle:* Suta on a high seat before sages around a fire at Naimisharanya.
- *The lesson:* a sage under a tree speaking to a king or a boy, one telling object beside them (a pot, a bird, a lamp, a lotus).
- *The hymn:* one devotee with folded hands before a glowing form in the sky or in a temple niche, offerings at the front.
- *The line of kings:* a procession of crowned figures walking away along a river bank toward the horizon, a sun and a moon in the same sky.

## Examples
```
ch_10_29. SCENE: A circle of cowherd women in bright saris dancing around Krishna in a moonlit forest clearing beside the Yamuna, flute at his lips.
TIME AND LIGHT: autumn night, deep indigo sky, silver moon, soft golden lamplight on the dancers. FIGURES: Krishna in the centre-right, eight to ten women around him in a ring. MOOD: tender.
```
```
ch_8_2. SCENE: A great elephant stands in a lotus lake with a crocodile gripping his foreleg, lifting one pink lotus high in his trunk; Vishnu descends on a golden eagle through the sky.
TIME AND LIGHT: dawn, deep sea-blue and gold. FIGURES: elephant and crocodile in the lake centre, Vishnu on the eagle upper right. MOOD: urgent prayer.
```
```
ch_7_8. SCENE: In a palace hall at twilight a great stone pillar has split; a lion-faced form of light and flame stands at it, a king's mace falling from his hand; a small boy stands calm at the side with folded hands.
TIME AND LIGHT: dusk, vermilion fire and gold. FIGURES: the lion-faced form centre-right, the king left of it, the boy at the right edge. MOOD: awe.
```
```
ch_10_3. SCENE: In a stone prison cell at midnight, a mother and father with folded hands before a four-armed blue form with conch, discus, mace and lotus, standing in a soft blue glow; rain at the barred window.
TIME AND LIGHT: midnight, deep indigo with a gentle blue and gold light. FIGURES: Vasudeva and Devaki kneeling lower right, the blue form above them. MOOD: wonder.
```
```
ch_1_1. SCENE: Sages seated in a circle around a sacrificial fire in a forest clearing, listening to an elderly narrator on a raised seat; smoke rising into the trees.
TIME AND LIGHT: late afternoon turning to dusk, saffron and amber. FIGURES: the narrator on the right, five or six sages in a half circle facing him. MOOD: quiet attention.
```
```
ch_11_7. SCENE: An old ascetic in plain ochre cloth sits under a banyan, speaking to a young king seated on a deerskin; beside them a pigeon, a bee on a lotus and a coiled python on a rock.
TIME AND LIGHT: late morning, jade and soft gold. FIGURES: the ascetic centre-right, the king in front of him, the animals around the roots. MOOD: calm teaching.
```

## Fixing a picture without starting again
Send one short line in the same chat and do not re-explain:
- "Same scene. Move the figures into the right half and leave the left third empty sky."
- "Same scene. Remove all writing and symbols that look like letters."
- "Same scene. Make it less photographic: softer brush outlines and visible paper grain."
- "Same scene. Fix the hands." / "Same scene. Make the light warmer."

## If a picture is refused
Say "a traditional devotional temple painting" once; if it still refuses, replace the faces by symbols (lotus, conch, flute, peacock feather, lamp, pillar, ocean) as in `PROMPTS.md`.
