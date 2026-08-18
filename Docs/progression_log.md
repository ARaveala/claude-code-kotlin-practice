
**set up**
- Followed https://developer.android.com/studio/install for setup.
- 64-bit lib packages in the official docs were outdated/unnecessary - skipped, installed tar package.
- Unpacked in `~` rather than a system wide location - single-user machine, no need for sudo-managed shared install.
- Chose Empty Activity template (Kotlin, Compose) per roadmap/CLAUDE.md plan.
- Scaffolded project into existing repo folder; package name resolved to `com.practice.plant_user`.
- `./gradlew build` initially failed: JAVA_HOME not set, Set JAVA_HOME to Android Studio's bundled `jbr` path in `~/.bashrc`
  (reused existing JDK, no separate install needed).
- `./gradlew build` confirmed BUILD SUCCESSFUL after the fix.
