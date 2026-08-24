
**set up**
- Followed https://developer.android.com/studio/install for setup.
- 64-bit lib packages in the official docs were outdated/unnecessary - skipped, installed tar package.
- Unpacked in `~` rather than a system wide location - single-user machine, no need for sudo managed shared install.
- Chose Empty Activity template (Kotlin, Compose) per roadmap/CLAUDE.md plan.
- Scaffolded project into existing repo folder; package name resolved to `com.practice.plant_user`.
- `./gradlew build` initially failed: JAVA_HOME not set, Set JAVA_HOME to Android Studio's bundled `jbr` path in `~/.bashrc`
  (reused existing JDK, no separate install needed).
- `./gradlew build` confirmed BUILD SUCCESSFUL after the fix.
**Setup 2**
- tested emulator works, chose pixel 8 with google play


**Phase 1 — Area list + blank canvas**
- Built the Area list screen (add via FAB dialog, tap to open) and a pannable/zoomable blank canvas with a light grey grid, per roadmap scope. The grid doubles as the zoom-in/out visual cue and a future Phase 3 scale reference, grid concept and the area/name caps (50 char / 100 areas) were directed by me; Claude implemented them.
- I manually reviewed all generated code and the new learning notes docs line by line, surprised at how clean the generated code was.
- Kotlin has real structural differences from C/C++ worth studying deliberately, not just skimming past. Directed Claude to set up a private, gitignored study system (`Docs/kotlin_notes.md`, `Docs/android_compose_notes.md`): checklist based, requires writing my own C/C++-equivalent snippet before "graduating" a section. Confirmed each example is small enough to compile/test directly with local `gcc`/`g++`/`clang`.
- Directed a `Docs/known_issues.md` split into "Known Issues" vs. a "Potential Concerns" section for forward looking design watch items (placeholder ID counter, MainActivity state ownership, grid's missing real-world cm anchor, unbounded panning, canvas accessibility gap) so these aren't lost before Phase 2, without blocking Phase 1.
- Claude wrote 16 unit tests (pan/zoom transform math, grid-cell visibility, name-length cap, area-count cap). I reviewed and expanded the proposed test list (added the name-length and area count boundary tests) before implementation. Extracting the pan/zoom/grid math into plain functions was required to make any of this testable at all — worth watching whether extracting for testability starts happening too often relative to the actual complexity of the logic; no guideline written yet, still deciding.
- Manually tested the built app on a Pixel 8 emulator (list + canvas), confirming the flow end to end myself. Open item: all sizing/grid values were only verified against this one device's density (420dpi/xxhdpi) — needs testing on other emulator profiles (tablet, lower-density/older phone) before trusting the dp-based scaling generalizes.
- Caught the emulator's "try your stylus" popup interfering with dialog taps during my own manual testing; Claude confirmed via logcat it was a simulator only input artifact (not an app bug) and logged it in `known_issues.md`.

TO DO: 
- add stricter compile flags 
- fix linter issues, this will also help understanding kotlin
- update project files for private notes
- create a make test first mentality for claude code, to ensure clean error reports
- Manually go through checklist again more strict this time
