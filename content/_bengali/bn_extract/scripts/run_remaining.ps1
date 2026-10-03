# Finishes the Bengali extraction: Skandha 3 (rest), 10, 11, 12. Run from this folder in PowerShell:
#   $env:ANTHROPIC_API_KEY = "sk-ant-..."
#   .\run_remaining.ps1
# Safe to stop (Ctrl+C) and run again: a page is done when work\vision\vN_pNNNN.json exists.
$ErrorActionPreference = "Stop"
python bn_extract.py check
python bn_extract.py run --vol 1 --pages 359-402 --workers 4     # Skandha 3 chapters 25-33
python bn_extract.py run --vol 2 --pages 803-1084 --workers 4    # Skandha 11 and 12
python bn_extract.py run --vol 2 --pages 168-802 --workers 4     # Skandha 10
python bn_extract.py assemble
python ..\..\_build\promote_bengali.py                           # only complete chapters go to content\bn
Write-Host "Next: python ..\..\_build\add_bengali.py <path-to-content.db> <version>, copy the db to app\src\main\assets, bump the app version, rebuild."
