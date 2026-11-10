# HR Deckbuilder – Android wrapper

A small WebView app that opens your Apps Script web app full-screen. No dependencies; the APK is built by GitHub Actions.

## Create the repo and build
```bash
cd herorush-android
./setup.sh                      # asks for your web app URL, creates a private repo, pushes, builds
gh run watch                    # follow the build
gh run download -n herorush-deckbuilder-apk   # get herorush-deckbuilder.apk
```
Manual route: create a repo, push this folder, add the repository variable **APP_URL**
(Settings → Secrets and variables → Actions → Variables), then run **Actions → Build Android APK**.
Changing the URL later: update `APP_URL` and re-run the workflow (or pass `app_url` when running it by hand).
Tag a version (`git tag v1.0 && git push --tags`) to also publish the APK as a GitHub Release.

## Install
Copy the APK to the phone and open it (allow "install unknown apps" for your file manager or browser). Android 8.0+.

## Optional: sign with your own key (needed for updates over an older install, or Play Store)
```bash
keytool -genkeypair -v -keystore release.jks -alias herorush -keyalg RSA -keysize 2048 -validity 10000
base64 < release.jks | tr -d '\n' | gh secret set KEYSTORE_BASE64
gh secret set KEYSTORE_PASSWORD
gh secret set KEY_ALIAS --body herorush
gh secret set KEY_PASSWORD
```
Keep `release.jks` somewhere safe; if you lose it you can't ship updates that install over the old app.
Without these secrets the APK is signed with the debug key, which is fine for personal sideloading.

## Important: which web app URL to use
Google blocks its sign-in page inside embedded WebViews, so the app must open a deployment that needs no sign-in.

- **`APP_URL` = your Public deployment** ("Who has access: Anyone"). Never the Admin one.
- Keep a second, **Admin deployment** ("Only myself") and open it in a normal browser. Owner tools
  (update cards, sync images, rules, settings) only work there, because Google has to recognise you.
  The Android app can't use it. If you use a short link, point it at the Public URL.

## Saved decks and the edit key
Each browser or app install creates a private edit key (stored in the app's local storage) that proves
which saved decks are yours. Consequences:
- Uninstalling the app or clearing its storage loses the key; those decks can then only be changed by the owner.
- The Android app and a desktop browser have different keys, so decks saved in one can't be edited from the other.
- Anyone can still load and view any shared deck.
