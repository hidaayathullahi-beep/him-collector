# HIM Collector — Android app (v1: printing test)

This turns your existing collector web app into an installed Android app that prints
Malayalam slips **directly** to the SEZNIK Veer over Bluetooth — no RawBT, no popup.

**This v1 build is only to prove printing works.** It opens a "Print setup" test page.
Once a test slip prints cleanly, the developer wires the real Print button into the
collector app (v2).

You do NOT need Android Studio. GitHub builds the app for you in the cloud. Follow along.

---

## STEP 1 — Make a free GitHub account
Go to https://github.com and sign up (free). Verify your email.

## STEP 2 — Create a repository
1. Top-right **+** → **New repository**.
2. Name it `him-collector` (anything is fine). Choose **Private**. Click **Create repository**.

## STEP 3 — Upload these files
1. On the new repo page, click **uploading an existing file** (the link in the middle).
2. **Drag the *contents* of this folder** (the `app` folder, `.github` folder,
   `build.gradle`, `settings.gradle`, `gradle.properties`, this README) into the page.
   - IMPORTANT: keep the folder structure. The easiest way: drag the whole `him-app`
     folder's inside — GitHub keeps the sub-folders.
3. Click **Commit changes**.

## STEP 4 — Let it build (automatic)
1. Click the **Actions** tab at the top of the repo.
2. You'll see a run called **Build APK** working (yellow dot). Wait ~3–6 minutes.
3. Green tick = success. (Red = something to fix — see "If the build fails" below.)

## STEP 5 — Download the app
1. Click into the finished run.
2. Scroll to **Artifacts** → click **HIM-Collector-APK** to download a `.zip`.
3. Unzip it → you get **app-debug.apk**.

## STEP 6 — Install on the phone
1. Send `app-debug.apk` to the Android phone (WhatsApp to yourself, Google Drive, USB — anything).
2. Tap it to install. Android will warn "install from unknown sources" — allow it for this once.
3. Open the **HIM Collector** app.

## STEP 7 — Test printing
1. Pair the Veer in the phone's Bluetooth settings first (if not already).
2. In the app, tap **Select printer** → choose the Veer.
3. Tap **Print test slip**.
4. It should print the Malayalam slip instantly, with **no popup**.
5. Tap **Open the Collector app** to check your normal collector app also loads inside.

Then tell the developer how it went: did it print with no popup? Was it instant?

---

## If the build fails (STEP 4 is red)
Open the red run, click the failed step, and copy the last ~20 lines of the log to the
developer. First-time cloud builds sometimes need a tiny version tweak — this is normal
and quick to fix. Nothing on your phone or your live apps is affected.

## Change the collector app address
If your collector app is NOT at
`https://masjid.hidaayathullahi.workers.dev/collector.html`, edit that one line in:
`app/src/main/res/values/strings.xml`
(GitHub lets you edit it right in the browser — pencil icon — then commit; it rebuilds.)

## Known for v2 (not in this test build)
- Real Print button inside the collector app (auto receipt number, dues line).
- Taking census **photos** through the app (camera/file upload in the shell).
- Keeping the printer connection warm for extra speed.
