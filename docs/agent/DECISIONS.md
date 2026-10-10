# Owner decisions (final)

These are the owner's decisions. They are final: where code, a data file, a plan or an agent's opinion disagrees with this file, this file wins and the code or file gets fixed. For matching rules, the real words of the book are the judge (see "How a rule is decided").

Recorded 2026-10-11. Source: the owner's reply "yes" to the recommended answers in `QUESTIONS.md`, and "treat the word as the final, not the code and not whatever the file says".

## Search
1. **Index size.** A `search.db` of about 4 to 5 MB is accepted (hard limit 8 MB, as in the plan).
2. **"Did you mean".** Included in version 1 (the `vocab` table, about 0.3 MB). The gold queries `typo-1` and `typo-2` are to be made to pass.
3. **Name aliases.** Start from the stories list in `Episodes.kt` and the most frequent names. Every alias or related word is checked against the book text before it is added.
4. **Minimum Android version.** Stays API 26 (Android 8).

## Dictionary
5. **Sources for now:** only the four verified Wiktionaries (Hindi, Bengali, Odia, English), all CC BY-SA 4.0. Others are added only when their licence is confirmed.
6. **Shabdsagar text inside Hindi Wiktionary:** left out until its copyright status is checked.
7. **Format:** a separate `dictionary.db` plus a Credits screen (source, licence, link). Share-alike applies to that file, not to the app code. Not legal advice; revisit before any public release.

## How a rule is decided (matching keys, aliases, spelling folding)
The real words of the book decide, not the code and not a test file:
1. Apply the candidate rule to every word of the book (`content/_search/compare_rules.py`).
2. List the words it joins that the current rule keeps apart. If they are mostly different words, the rule is rejected. If they are mostly the same word, it is accepted.
3. A rule must also not raise the number of large collision groups, and no `required` gold query may fail.
4. The test file `loose_pairs.json` is generated from the accepted code, never the other way round.

## Decided by that method
- **Key rule A (the code in `prototype.py`) stays.** Rule B (the one implied by the earlier pairs file: drop every "h", merge "ks" to "s", drop a "v" between two "a") was tested on all 246,972 distinct words of the book and rejected: it joins different words (and / hand, is / his, that / तथा, was / বাস, man / নাম), makes the largest collision groups 31% bigger (1,414 to 1,846 keys with six or more words), maps "Hari" to "ari", and contradicts itself (it keeps the "v" in Dhruva and Vishnu but drops it in Bhagavan).
- **Spelling variants such as Prahlad / Pralhad** are handled by the verified alias list (`aliases.json`, search plan Task 3, each entry checked against the book), not by a global rule. The key code was regenerated into `content/_search/tests/loose_pairs.json` on 2026-10-10: 20 of its 81 hand-written keys were wrong against the code and were replaced. All must-match and must-not-match pairs, and all 22 required gold queries, pass.
