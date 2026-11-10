#!/usr/bin/env bash
# Creates a private GitHub repo from this folder, sets APP_URL, and starts the first build.
set -euo pipefail
NAME="${1:-herorush-deckbuilder-android}"
command -v gh >/dev/null || { echo "Install the GitHub CLI first: https://cli.github.com"; exit 1; }
gh auth status >/dev/null 2>&1 || gh auth login
read -rp "Your Apps Script web app URL (or short link): " APP_URL
[ -d .git ] || git init -b main
git add -A
git commit -m "Android wrapper for HR Deckbuilder" || true
gh repo create "$NAME" --private --source=. --remote=origin
gh variable set APP_URL --body "$APP_URL"
git push -u origin main
echo "Pushed. Watch the build with:  gh run watch"
echo "Download the APK with:        gh run download -n herorush-deckbuilder-apk"
