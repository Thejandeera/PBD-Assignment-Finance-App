# Firebase Setup – Tasks for Members B and C

Follow these steps in order. They take about 10 minutes. When you finish Step 5, send your
fingerprints to Member A and wait for the "Firebase is ready" message before doing Step 6.

> Why do we need this? Google Sign-In only accepts apps signed by a key Firebase knows. Each laptop
> signs debug builds with its own `debug.keystore`, so every member's SHA-1 fingerprint must be added
> to the Firebase app. Without it, Google Sign-In fails on your machine with "Developer error".

---

## Step 1 – Accept the Firebase invite

1. Open the e-mail *"You've been invited to join the Firebase project clarity-finance"*.
2. Click **Join project** and sign in with the **same Google account** the invite was sent to.
3. Check: <https://console.firebase.google.com> now lists **clarity-finance**.

Didn't get the e-mail? Check spam, then ask Member A to confirm which address they invited.

---

## Step 2 – Make sure you have a debug keystore

The debug keystore is created the first time you run **any** app from Android Studio.
Check that this file exists:

| OS          | Path                                       |
|-------------|--------------------------------------------|
| Windows     | `C:\Users\<you>\.android\debug.keystore`   |
| macOS/Linux | `~/.android/debug.keystore`                |

If it is missing: open any project in Android Studio and click **Run ▶** once (emulator or phone).

---

## Step 3 – Get your SHA-1 and SHA-256

Use **one** of the two options. Option A is recommended because it works even if the project does not
build yet.

### Option A – Terminal (recommended)

Use the `keytool` that ships with Android Studio.

**Windows (PowerShell):**

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v `
  -keystore "$env:USERPROFILE\.android\debug.keystore" `
  -alias androiddebugkey -storepass android -keypass android
```

**macOS:**

```bash
"/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" -list -v \
  -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

**Linux:**

```bash
~/android-studio/jbr/bin/keytool -list -v \
  -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

In the output, copy the two lines that start with `SHA1:` and `SHA256:`.

### Option B – Android Studio / IntelliJ IDEA (Gradle panel)

1. Open the project in Android Studio (or IntelliJ IDEA with the Android plugin).
2. Open the **Gradle** tool window (right-hand side, elephant icon).
3. Expand **Clarity Finance → app → Tasks → android** and double-click **signingReport**.
   - Don't see *Tasks*? Go to **File → Settings → Experimental** (IntelliJ IDEA: **Build, Execution,
     Deployment → Gradle**), enable *"Configure all Gradle tasks during Gradle Sync"*, then
     **File → Sync Project with Gradle Files** and try again.
4. In the **Run** panel at the bottom, find the block starting with `Variant: debug` and copy the
   `SHA1:` and `SHA-256:` lines.

> `android` is the standard, public password of every **debug** keystore – it is not a secret.
> Never share or commit a **release** keystore (`*.jks`, `*.keystore`) or its password.

---

## Step 4 – Send your fingerprints to Member A

Send this message in the team's private chat:

```
Member <B/C> debug fingerprints
SHA1:   AA:BB:CC:...
SHA256: AA:BB:CC:...
```

---

## Step 5 – Wait for "Firebase is ready"

Member A adds all fingerprints to Firebase and commits the updated `app/google-services.json` to
`develop`. Wait for their message before continuing.

---

## Step 6 – Pull and check the config

```
git checkout develop
git pull
```

Open `app/google-services.json` and check that it contains:

- `"package_name": "com.clarity.finance"`
- an `oauth_client` entry with `"client_type": 3` (the web client ID Google Sign-In needs)

---

## Step 7 – Build and test on your machine

1. In Android Studio: **File → Sync Project with Gradle Files**, then **Build → Rebuild Project**.
2. Run the app and register a test account with e-mail and password.
3. Open Firebase Console → **Authentication → Users** – your test account should be listed.
4. Sign out and try **Continue with Google**.
5. Tell the team it works, or report the exact error message.

### If something fails

| Problem                                   | Fix                                                                 |
|-------------------------------------------|---------------------------------------------------------------------|
| Google Sign-In: "Developer error" / code 10 | Your SHA-1 is missing in Firebase, or you have an old `google-services.json` – ask Member A, then pull again |
| `File google-services.json is missing`    | Run `git pull` on `develop`; the file must be in `app/`, not the project root |
| Can't see the project in the Firebase console | Accept the invite with the exact Google account it was sent to |
| Register fails with a network error       | Check internet; Auth sign-up needs a connection                     |

---

## Rules

- Never commit **service-account keys**, **release keystores** (`*.jks`, `*.keystore`) or passwords.
- Do not edit Firestore **rules** or **indexes** in the console. Change `firestore.rules` /
  `firestore.indexes.json` in the repo and ask Member A to deploy them, so the repo always matches
  what is live.
