# basics. — Play Store Release Checklist

## 0. ⚠️ BEFORE ANYTHING — Back up your signing key (do this FIRST)

Your release builds are signed with:
- **Keystore file:** `C:\Users\Jerson\ai-workspace\basics\release-keystore.jks`
- **Passwords:** in `C:\Users\Jerson\ai-workspace\basics\keystore.properties`
  (storeFile, storePassword, keyAlias, keyPassword)

**Copy the keystore file AND the 4 values to:**
1. a password manager (e.g. Bitwarden/1Password/Google), AND
2. an offline copy (USB drive / printed paper)

> Losing this keystore = you can never update the app on Play. Google cannot
> recover it. There is no way around this.

## 1. Create the developer account
- Go to `play.google.com/console` → **Create account**
- One-time $25 fee, personal or organization account
- Complete identity verification when prompted

## 2. Create the app
- **Create app** → App name: `basics.` → Default language: English (United States)
- App or game: App → Free → agree to policy

## 3. Enroll in Play App Signing
- **Setup → App signing** → enroll
- Your `release-keystore.jks` becomes the **upload key**. Google generates the
  app-signing key and re-signs the AAB for distribution.
- (Optional but recommended: export the generated app-signing key and store it
  offline too — you can export it from the same page.)

## 4. Upload the release bundle
- **Testing → Internal testing → Create release**
- Upload: `app/build/outputs/bundle/release/app-release.aab`
- Add yourself as a tester, install on your real phone, sanity-check the app
  (timer, scramble, stats, history, charts)

## 5. Store listing
- **Grow → Store presence → Main store listing**
- Upload screenshots/icon/graphics from `play-store-assets/`
- Paste the short + full description and "What's new" from `store_listing.md`
- **Privacy policy URL:** must be a live HTTPS link. Host `privacy-policy.html`:
  - GitHub Pages (easiest): push `privacy-policy.html` to a repo → Settings →
    Pages → enable → get `https://<user>.github.io/<repo>/privacy-policy.html`
  - Or Google Sites, or any static host you already use
- Contact email: must be a real, verified address you own
  (⚠️ the file currently says `basics.timer.app@gmail.com` — use a real inbox
  you control, or create that one)

## 6. Policy forms (Setup → Policy)
- **Data safety:** this app collects NO data and shares NO data — answer the
  questionnaire accordingly (all "No")
- **Content rating:** complete the IARC questionnaire (a cubing timer = likely
  Everyone / PEGI 3)
- **Target audience & content:** everyone; no ads; not a news or government app
- **App access:** all features available without login
- **Ads:** No
- **Permissions:** none declared, nothing to explain

## 7. Closed testing (required for new personal accounts)
- **Testing → Closed testing** → upload the same AAB
- Add ≥ 12 testers (share the opt-in link)
- Keep the test running with those testers for **14 continuous days**
- Then **Apply for production access** (account verification)
- ⚠️ You cannot publish to production until this is approved

## 8. Production release
- **Testing → Production → Create release** → upload AAB
- Roll out to 10% first, monitor, then 100%

---

## Rebuild commands (when you need a fresh bundle)

```powershell
cd C:\Users\Jerson\ai-workspace\basics
.\gradlew.bat :app:bundleRelease          # signed AAB
.\gradlew.bat :app:assembleDebug          # installable debug APK
```
