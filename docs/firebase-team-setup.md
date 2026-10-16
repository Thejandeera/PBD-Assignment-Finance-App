# Firebase Team Setup – Guideline (Member A)

How every member gets access to the Clarity Finance Firebase project and registers their machine so
Email/Password **and Google Sign-In** work for them.

- **Member A (Firebase owner)** follows this file.
- **Members B and C** follow [firebase-setup-members-b-c.md](firebase-setup-members-b-c.md).

> Why a SHA-1? Google Sign-In only accepts APKs signed by a known key. Each laptop signs debug builds
> with its own `debug.keystore`, so every member's SHA-1 must be added to the Firebase app. If Google
> Sign-In works for one member but fails for another (often "Developer error" / code 10), a missing
> SHA-1 is almost always the cause.

---

## Step 1 – Invite B and C to the Firebase project

1. Open <https://console.firebase.google.com> and select the **clarity-finance** project.
2. Click the **⚙ gear** next to *Project Overview* → **Users and permissions**.
3. Click **Add member**.
4. Enter Member B's Google e-mail, choose role **Editor**, click **Add member**.
5. Repeat for Member C.
6. Send B and C the link to [firebase-setup-members-b-c.md](firebase-setup-members-b-c.md) and ask
   them to send back their SHA-1 and SHA-256.

Only Member A stays **Owner**. Editors can view data, rules and Auth users, which is enough for the
project.

---

## Step 2 – Register every fingerprint

1. Firebase Console → **⚙ Project settings** → **General** tab.
2. Scroll to **Your apps** → select the Android app **com.clarity.finance**.
3. Click **Add fingerprint**, paste a SHA-1, click **Save**. Repeat for each SHA-256.
4. Repeat for every member until all three members' fingerprints are listed.

| Member | SHA-1 added | SHA-256 added |
|--------|-------------|---------------|
| A      | ☐           | ☐             |
| B      | ☐           | ☐             |
| C      | ☐           | ☐             |

---

## Step 3 – Share the updated config

1. On the same **Your apps** card, click **google-services.json** to download the updated file.
2. Replace `app/google-services.json` in the repository (in `app/`, not the project root).
3. Commit it on a branch and open a PR into `develop`:

   ```
   git checkout develop
   git pull
   git checkout -b chore/update-google-services
   git add app/google-services.json
   git commit -m "chore: update google-services.json with team fingerprints"
   git push -u origin chore/update-google-services
   ```

4. After the PR is merged, tell the team: *"Firebase is ready – pull `develop`."*

---

## Rules for everyone

- Never commit **service-account keys**, **release keystores** (`*.jks`, `*.keystore`) or passwords.
  The `.gitignore` already blocks `*.jks`, `*.keystore` and `*service-account*.json`.
- `google-services.json` **is** committed – it identifies the app, not a secret admin key, and the
  repository is private. Data is protected by Firebase Auth and `firestore.rules`.
- Do not change the Firestore **security rules** or **indexes** in the console directly – edit
  `firestore.rules` / `firestore.indexes.json` in the repo and ask Member A to deploy them, so the
  repository always matches what is live.
