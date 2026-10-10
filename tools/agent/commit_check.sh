#!/usr/bin/env bash
# Run after staging and before committing. Fails if anything that must stay out of git is staged.
#   git add -A && tools/agent/commit_check.sh
set -u
cd "$(git rev-parse --show-toplevel)"
bad=0
staged="$(git diff --cached --name-only --diff-filter=AM)"
[ -z "$staged" ] && { echo "Nothing is staged."; exit 0; }

echo "== files that must not be committed (book text, databases, APKs, PDFs, keys)"
hits="$(echo "$staged" | grep -E '(^|/)\.env$|\.env$|\.db$|\.apk$|\.pdf$|\.tgz$|^content/(bn|hi|en|or)/|^content/_odia/(work|out|review|vision_transcripts|batches)/|^content/_bengali/.*/(work|out|review)/|^_archive/' || true)"
if [ -n "$hits" ]; then echo "$hits" | head -20; echo "FAIL: remove these from the index (git rm --cached) and add them to .gitignore"; bad=1; else echo "none"; fi

echo "== key-like strings in the staged changes"
if git diff --cached -U0 --no-textconv --text | grep -aE '^\+' | grep -aEv '^\+\+\+' | grep -aE 'AIza[0-9A-Za-z_-]{20,}|sk-ant-[A-Za-z0-9_-]{10,}|ghp_[A-Za-z0-9]{20,}|-----BEGIN [A-Z ]*PRIVATE KEY' >/dev/null; then
  echo "FAIL: something that looks like an API key or token is staged"; bad=1
else echo "none"; fi

echo "== very large files (over 5 MB)"
big=0
while IFS= read -r f; do
  [ -f "$f" ] || continue
  s=$(stat -c %s "$f" 2>/dev/null || echo 0)
  if [ "$s" -gt 5242880 ]; then echo "$f ($((s/1048576)) MB)"; big=1; fi
done <<< "$staged"
[ $big -eq 0 ] && echo "none" || echo "WARNING: check these are meant to be committed"

echo "== em dashes in staged text files (project rule: none)"
EM="$(printf '\342\200\224')"; EN="$(printf '\342\200\223')"   # em dash and en dash as raw UTF-8 bytes
em="$(git diff --cached -U0 --no-textconv --text -- '*.md' '*.kt' | grep -aE '^\+' | grep -aEv '^\+\+\+' | grep -aF -e "$EM" -e "$EN" | head -5 || true)"
if [ -n "$em" ]; then echo "$em" | cut -c1-140; echo "WARNING: replace these with a spaced hyphen"; else echo "none"; fi

[ $bad -eq 0 ] && echo "OK to commit (no co-author lines, no credits in the message)." || exit 1
