# Painting every chapter with ChatGPT in the style of Mahatmya 1 (research, 2026-10-18)

Goal: 341 chapter paintings that look like one set with `ch_0_1` (Narada and Bhakti by the river at dawn), the picture you like.
What was read: the picture itself, `content/art/PROMPTS.md`, `docs/ART_AND_ASSETS.md`, `tools/art/make_chapter_prompts.py`,
and current notes on ChatGPT and the OpenAI image API (links at the end; they are third-party summaries, so prices and limits must be
checked on OpenAI's own pages before you spend money).

## 1. What makes `ch_0_1` look the way it does
Looking at the image, these are the traits to copy, in this order of importance:
1. **One low, warm light source.** A sun on the horizon at left, a gold-to-peach haze, and its reflection in the water. The whole picture is lit by it.
2. **Painterly, not flat.** Soft tempera or gouache on aged paper, visible paper grain, gentle atmospheric depth (blue-grey distant trees and a temple spire fading into mist). It is closer to a Bengal School or Raja Ravi Varma devotional painting than to a flat Kangra miniature, which is what the older prompt in `PROMPTS.md` asks for. The generator script already says "Bengal School and Kangra wash"; use that wording everywhere.
3. **Slender figures with long lotus-shaped eyes**, calm faces, jewellery drawn with fine lines, drapery in saffron, indigo and cream with small gold patterns.
4. **A framed composition**: a flowering tree arching in from the top right, a lotus pond with pink lotuses in the lower left, the main figures in the right half.
5. **Quiet left third** (sky and water) where the app puts the title, and a darker, calmer bottom edge.
6. **No text, no border, no modern objects.**

## 2. Which ChatGPT route to use
| | ChatGPT app (chat window) | OpenAI image API (script) |
|---|---|---|
| How | paste a prompt, attach the reference picture, save each result by hand | `tools/art` script sends the prompt and the reference picture, saves the file, remembers what is done |
| Limits | an hourly or daily cap that OpenAI does not publish as a fixed number (reports vary: for example 50 per 3 hours); 341 images take days | rate limits by account tier; a batch mode at half price exists |
| Cost | the monthly plan | pay per image: about $0.016 / $0.063 / $0.25 per landscape image (low / medium / high) for `gpt-image-1`; the newer `gpt-image-2` is reported near $0.006 / $0.04 to $0.05 / $0.21 (square, medium and high); so roughly **$5 to $22 for the whole set at low to medium quality, about $70 to $85 at high** |
| Consistency | drifts in long chats; the reference picture must be attached again | the same reference and the same style block go with every request |
| Best for | trying the style on 5 to 10 chapters | the real run |

Recommendation: try the style in the ChatGPT app on 6 chapters, then run all 341 through the API with the same prompt text.
Never paste an API key into a chat or commit it; put it in an environment variable (`OPENAI_API_KEY`).

## 2b. If you have only ChatGPT Go (the owner's plan)
What the notes say (third-party blogs, OpenAI publishes no number): Go has "higher limits" than Free for images but no fixed daily count; reports range from about 10 to 120 images a day, 20 to 30 being the figure repeated most. Uploads are limited to 80 files per 3 hours on Go, Plus and Pro, and 20 MB per image, so attaching the reference picture to each new chat is fine. Go includes Projects (25 files per project). Go does **not** include the API: the API is billed separately at platform.openai.com. Treat the numbers as rough and watch the message ChatGPT shows when the limit is reached.
What that means for 341 images: at 20 to 30 a day it is 12 to 17 days of pasting, with re-runs more like 3 weeks; at 10 a day, more than a month.
Ways to make it work on Go:
1. **Make a Project** ("Bhagavatam paintings"). Put `ch_0_1`, the style block and the character lines in the project's files and instructions, so each new chat in it starts with the style and nothing has to be pasted or attached again. Start a new chat inside the project every 8 to 10 images.
2. **Do the 40 most important chapters on Go** (the twelve well-loved stories, the first chapter of each Skandha, the Mahatmya) and let the rest keep the Skandha painting, which the app already falls back to. The set stays beautiful and finishes in about 2 weeks.
3. **Or put $10 to $25 of credit on the API for the rest.** About $14 to $22 pays for all 341 at medium quality in one evening with a script, and the pictures are the same model. This does not need Go at all.
4. **Pace yourself with a list.** A ready-to-paste file (`content/art/paste_list.md`: chapter code, scene, one block per chapter, tick boxes) and a small intake script that renames the downloaded files in the order you made them and shows a contact sheet to confirm before they are installed.
Not recommended: asking for several scenes in one image (a grid) and cropping; the pictures come out small and the style slips.

## 3. The prompt: three parts, always in this order
**A. Style block (fixed, identical in all 341 prompts).** Written from the picture, not from a school name:
```
STYLE REFERENCE: use the attached image only as a style reference (palette, brushwork, light, paper texture, level of detail), not as a scene to copy.
Classical Indian devotional painting in the Bengal School / Kangra wash manner: soft tempera and gouache on aged cream paper with visible grain,
fine brush outlines, slender figures with long lotus-shaped eyes and calm faces, flowing drapery in saffron, indigo, cream and madder red with small gold patterns,
fine jewellery. One low warm light source with a glowing horizon and its reflection, hazy blue-green atmospheric distance, a flowering tree arching in from the
top right, lotuses in the lower left foreground. Reverent, serene, luminous.
COMPOSITION: wide landscape. Main figures in the right half. The left third is calm sky, water or mist with no figures. The bottom edge is a little darker and quieter.
NEVER: text, letters, signatures, borders, frames, modern objects, photographic realism, 3D render look, glossy AI sheen, extra fingers or limbs.
```
**B. Scene (one per chapter, written by hand or generated and then read by you).** One concrete picture, not a summary:
who is there (at most three or four figures), where, time of day, the single action, the light colour of the Skandha.
Bad: "a poignant depiction of the divine narrative." Good: "Gajendra, a great elephant standing in a lotus lake with a crocodile gripping his leg, lifting one lotus in his trunk, while a blue-skinned four-armed figure descends on a large golden bird through a gold sky." The current `make_chapter_prompts.py` output is of the first kind, so the scenes must be written first.
**C. Characters (fixed lines, pasted in whenever the person appears):** keep one sentence for each recurring figure so they look alike across chapters, for example
Krishna (dark blue skin, yellow silk, peacock feather in the crown, flute), Narada (slim sage with a vina, sacred thread, saffron cloth, tilak), Suta (elderly sage on a raised seat, white beard), Shukadeva (radiant young ascetic, bare chest, long hair), Parikshit (young king, simple crown, folded hands), Vishnu (four arms, conch, discus, mace, lotus, golden-yellow cloth), Brahma (four faces, white beard, on a lotus), Shiva (matted hair, crescent moon, ash, serpent).
Write these once in a file (`content/art/characters.md`) and let the script add the lines for the names found in the scene.

## 4. Keeping 341 images alike
- Attach `ch_0_1` as the style reference **in every request** (API: the `images.edit` call with the image and `input_fidelity` left on its default; app: attach it again in each new chat).
- Use **one model and one quality** for the whole set. Do not switch halfway.
- Generate in **landscape 1536 x 1024** (3:2); the app crops to 16:10, so keep the subject inside the centre 90% of the height.
- Short chats: in the app start a new chat every 8 to 10 images and re-attach the picture and the style block.
- Ask for **2 or 3 variants** for important chapters and choose.
- Colour per Skandha (from `HUES` in the script) goes into the scene line: "warm saffron sunset", "deep indigo night".

## 5. Checking every picture before it goes in
Make a contact sheet of 20 per page and look at each. Reject and re-run when there is: any lettering, wrong number of hands or limbs, a modern object, a face that is too photographic, a figure in the left third, a flat white sky, or a picture that does not match the chapter. Expect to re-run about 1 in 5. Keep a CSV of `file, status, notes`.
Then `python tools/art/import_art.py <file> --name ch_S_C --install` (converts to WebP, about 50 to 70 KB each, 341 images about 20 MB more in the app).

## 6. Rights and honesty
- OpenAI's terms give you the output; check them once for the current wording.
- Ask for styles by school and period (Bengal School, Kangra, Pahari), never "in the style of" a living artist.
- Say in the app's credits (`docs/ART_CREDITS.md` and the Credits screen) that the chapter paintings are AI-generated, and keep the public-domain miniatures credited as they are.
- Depicting the Lord and sages: models usually allow it when asked reverently. If one refuses, use the fallback in `PROMPTS.md` (symbols such as the lotus, conch, flute and peacock feather instead of faces).

## 7. Steps, in order
1. Write the 341 scene lines (about 25 words each) from the chapter titles and the chapter text, grouped by Skandha; you read them before any image is made. (I can draft them.)
2. Write `characters.md`.
3. Pilot: 6 chapters in the ChatGPT app with the style block and `ch_0_1` attached. Judge the look; adjust the style block once.
4. Write `tools/art/openai_generate.py`: reads `chapter_prompts.csv`, builds style + scene + characters, calls the API with the reference, saves to `content/art/raw/ch_S_C.png`, skips what exists, logs cost, retries on rate limits. (I can write it.)
5. Run 20 chapters, check the contact sheet, then the rest in Skandha order, the twelve "Well-loved stories" first.
6. Import, build, look at the app in light and dark, update the credits.

## Sources
- ChatGPT Go limits and uploads (unofficial, they disagree): https://www.aifreeapi.com/en/posts/chatgpt-go-image-generation-limit, https://chatgptimage2.org/blog/chatgpt-go-image-generation-limit, https://onefileapp.com/faq, https://blog.laozhang.ai/en/posts/chatgpt-file-upload-limits-free-plus-pro, https://www.glbgpt.com/hub/how-many-images-can-you-generate-with-chatgpt-go/
- ChatGPT image limits (unofficial, they disagree): https://blog.laozhang.ai/en/posts/chatgpt-image-generation-upload-limits.md, https://www.cometapi.com/en/how-many-images-can-i-generate-with-chatgpt-plus-%E2%80%94-a-2026-status-report/, https://community.openai.com/t/why-is-chatgpt-limiting-the-number-of-images-i-try-to-generate-for-something-i-paid-for/1088555
- OpenAI image model pages and prices: https://developers.openai.com/api/docs/models/gpt-image-1, https://developers.openai.com/api/docs/models/gpt-image-1-mini, https://community.openai.com/t/gpt-image-1-5-rolling-out-in-the-api-and-chatgpt/1369443?page=3
- GPT Image 2 prices (resellers and summaries, check OpenAI): https://www.pixazo.ai/blog/gpt-image-2-api-cheapest-pricing, https://enter.converge.ai/page/en-US/news/gpt-image-2-pricing-cost-breakdown, https://www.atlascloud.ai/blog/gpt-image-2-api-guide
- Editing with reference images and input fidelity: https://developers.openai.com/cookbook/examples/generate_images_with_high_input_fidelity, https://community.openai.com/t/how-to-generate-a-new-image-using-an-existing-image-as-input-like-chatgpt-does-with-gpt-4o/1325999
- Keeping one style across many images: https://community.openai.com/t/consistent-image-style-across-multiple-images/666866, https://help.trypencil.com/en/articles/9472364-how-do-i-generate-images-and-videos-in-different-styles, https://rendair.ai/faq/how-do-i-keep-the-same-style-across-a-project
