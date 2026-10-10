# GEMINI.md

Read `AGENTS.md` in this folder first and follow it exactly. It is the single source of rules for every agent working on this project. Then read `CLAUDE.md` (architecture) and `docs/agent/RUNBOOKS.md` (procedures).

Summary of the rules that are easiest to get wrong:
- Run `git status --short` before editing. If you see changes you did not make, another agent is working: leave those files alone.
- Verify every change on the emulator with a screenshot and a crash-log check (`tools/agent/verify.sh`), in English and Hindi UI, a light and a dark theme.
- Commit or push only when the user asks. No co-author or tool credits in commits. Run `tools/agent/commit_check.sh` before committing.
- Never stage book text, databases, APKs, PDFs or `.env` files.
- Do not transcribe scripture pages by hand. Do not scrape YouTube. No em dashes in anything a person reads.
- Ask before deleting, force-pushing or overwriting a database. Do not stop to ask about anything else.
