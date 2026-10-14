# Questions for the owner (gates)

## Search Task 0

1. **Size budget.** The search index (`search.db`) is estimated at 4 to 5 MB as compressed posting lists. Is that acceptable as part of the APK?

2. **"Did you mean" in v1.** Should the first version include typo suggestions ("Did you mean Krishna?" when a person types `Krshna`)? It adds a `vocab` table (about 0.3 MB) and some code. If no, typo-1 and typo-2 gold queries stay as targets.

3. **Name aliases.** Which name aliases matter most? The stories list in `Episodes.kt` is the starting point. Candidates from the book:
   - Narasimha / Nrisimha / Nrisimdeva (all for the same avatar, but the Roman vs Sanskrit roots differ)
   - Gajendra / Gajaraja
   - Prahlada / Prahlad
   - Alligator / crocodile (English synonyms for the same animal in the Gajendra story)
   - Rasa Lila as a story pointing to chapter 10.29
   Should I start with the stories list and the most frequent capitalised English names?

4. **Minimum Android version.** The plan assumes API 26 (Android 8). This stays?

## Dictionary Task 0

1. **Confirmed sources to use (all CC BY-SA 4.0, verified):**
   - Hindi Wiktionary (hiwiktionary) - 185,100 articles
   - Bengali Wiktionary (bnwiktionary) - 164,500 articles
   - Odia Wiktionary (orwiktionary) - 108,600 articles
   - English Wiktionary (enwiktionary) - 9.2 million articles, English glosses for hi/bn/or

2. **Sources still unverified (need your decision):**
   - **Wiktextract / kaikki.org** (`wiktextract`): machine-readable extracts of English Wiktionary. It is derived from Wiktionary text and likely CC BY-SA 4.0, but the site does not state its own licence. The per-language files are marked "deprecated". Use it only if we confirm the licence, or skip it and parse enwiktionary dumps directly.
   - **Praharaj Odia dictionary (DSAL)** (`praharaj`): the standard Odia dictionary (1931-40). The DSAL page says "licensed under a Creative Commons License" without saying which one. It could be CC BY-NC-SA (non-commercial) which would matter. This is the best Odia source if the licence allows.
   - **IndoWordNet** (`indowordnet`): Hindi, Bengali, Odia wordnets with synonym sets. Licence not found on the home page. Useful later for synonyms.
   - **Princeton WordNet** (`pwn`): English senses. Permissive with notice, but the page did not load for verification. Not critical now.
   - **Wikidata lexemes** (`wikidata`): CC0, Odia 286 and Bengali 11,811 lexemes. Small but free. Not checked at runtime.
   - **Shabdsagar text inside Hindi Wiktionary**: the Hindi Wiktionary imported text from a published Hindi dictionary called Shabdsagar. Its own copyright status is unchecked. We can either use only the non-Shabdsagar Hindi Wiktionary entries, or check Shabdsagar's status.

3. **Rejected (copyrighted, do not use):**
   - Samsad Bangla Abhidhan (DSAL) - copyrighted
   - Oxford McGregor Hindi-English Dictionary (DSAL) - copyrighted

4. **Question:** Should I proceed with only the four verified Wiktionary sources for now, and add the others if and when their licences are confirmed? That gives good Hindi and English coverage, good Bengali coverage, and partial Odia coverage.

5. **CC BY-SA 4.0 compliance.** The dictionary.db is a separate file from the app code, so share-alike applies to dictionary.db, not the app. I will add a Credits screen listing each source with its name, licence (CC BY-SA 4.0) and URL. Please confirm this approach is acceptable.
