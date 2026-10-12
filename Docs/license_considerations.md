# License considerations

Verify each item against the project's own LICENSE/README before relying
on it; terms can change between versions. Last reviewed: (07.10.2026).

## Tooling (dev/CI, not shipped in the app)
- **gitleaks** (scanner): MIT.
- **gitleaks-action** (GitHub Action wrapper): free for personal-account
  repos. Organization-owned repos need a license key (`GITLEAKS_LICENSE`
  secret). Fallback if that becomes an issue: run the gitleaks binary
  directly in a `run:` step instead of the action.
- **ktlint** and the **ktlint Gradle plugin**: MIT.
- **GitHub-maintained actions** (checkout, setup-java, setup-gradle):
  open source. Free for public repos; private repos draw on the account's
  Actions minutes quota.

## Dependencies shipped in the app
- Kotlin, Jetpack Compose, Room, AndroidX: Apache 2.0 (keep license/notice
  text if redistributing).
- Check each new dependency's license before adding it. Be especially
  careful with GPL/AGPL, which can impose conditions on the whole app.

## Project itself
- License: MIT (LICENSE file at repo root; keep the copyright line current).
- Contributions are MIT-licensed inbound under GitHub's terms; no CLA/DCO
  for now. Revisit if the project gains outside contributors.
- Avoid GPL/AGPL dependencies (incompatible with keeping the app MIT).
- If shipping to an app store, include a third-party licenses screen
  (Apache 2.0 requires preserving notices).

## Developer environment
- Android Studio and the Android SDK have their own Google terms of use.
  These apply to the developer, not to the app's license.