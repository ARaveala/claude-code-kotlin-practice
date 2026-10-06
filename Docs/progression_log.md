
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

**phase 2 - GrowZone creation + nesting validation**
- Planned a schema plan for Room (@Entity definitions for GrowZone and its nesting fields), created persistent state (emulator only for now), logged in known_issues: real device must pass persistence beyond dev testing. 
- Chose destructive migration (fallbackToDestructiveMigration) for now (this needs to be revisited when providing for users)
- Noted that having claude write code burns a lot of tokens, with a suprising amount of calls library/api rather than logic itself. Next to asses how intuitive the generated code is to hand tweak, since that's the real cost/benefit question, not raw token spend.
- Simplified the nesting validation logic after realizing the original version was solving for cases with no evidence they'd occur yet. Some remaining aspects of the nesting rules will need real usage data before they can be pinned down further, not guessing ahead of that.
- Researched how comparable apps handle canvas/placement interaction and pulled out failure patterns to design around rather than discover later: pan/zoom gestures getting hijacked by movable objects (accidental drags mid zoom), rigid box only shapes with no freeform option, and generally overcomplicated placement UIs. These are now design watch items for Phase 5 rendering/interaction work, not Phase 2 scope creep. 

**BUG HUNT**
All Areas showed the same GrowZones. Looked inconsistent (sometimes all zones, sometimes none) seems because whichever Area was entered first after process start "won."
Used Android Studio's Database Inspector: Area IDs were distinct, and zone rows had areaIds but mostly the wrong one (1). GrowZoneDao's query was correct, so the problem wasn't in the SQL.
Logging in GrowZoneViewModel's init showed it was constructed only once per process. The cause was viewModel() with no key: the Activity wide store hands back the cached instance, and the factory never runs for later Areas.
Fixed with a per Area key. Learned: onCreate runs once, and navigation is recomposition; stack traces via Throwable(); viewModel overloads; lambdas with receiver.
Did some research and some issues would be resolved by using Nav3 not to mention it may provide a faster passage to certain future features not yet documented. Worth the bloat?

**phase2.5 switch to Navigation 3**
- Migrated to Navigation 3 now while there are only two screens, before navigation
  grows. Nav3 is stable (1.2.0), its runtime is multiplatform, and the back stack is a
  plain observable list I own, which fits a possible cross platform future without
  committing to it.
- Merging dependencies: the Android docs' setup snippet is written as if Nav3 is the
  only thing in the project, so I merged it into my catalog instead of copying it. It
  would have added an alpha lifecycle version (Gradle resolves to the highest, dragging
  all lifecycle artifacts onto alpha) and a serialization plugin version that didn't
  match my Kotlin. Lesson: release notes version tables over guide snippets, and reuse
  existing version refs.
- Did the migration in small, separately buildable commits: keys file → extract
  `PlantUserApp()` from MainActivity with no behaviour change → swap the if/else router
  for `NavDisplay`. MainActivity now only does platform setup and passes the DAO in.
- Chose the explicit serializer back stack (`navConfig`) over the reflection based
  default: no reflection, and it's the multiplatform path. Cost: every new screen key
  must be registered or state saving crashes at runtime(navigation/Destinations.kt).
- Ran a quick audit of Android only imports as a cross platform baseline (logged in
  known_issues).
- Found that keyed ViewModels (`GrowZoneViewModel` per area) are never cleared from the
  Activity's store, (Fix planned, logged in known_issues).
  **Layering + model package** Due to already going through code to migrate and trying to understand it...
- Found a ui ⇄ viewmodel dependency cycle: domain classes (Area, GrowZone) lived in ui/,
  and GrowZoneType + displayName in data/. Kotlin doesn't flag package cycles, and
  same-package code needs no imports, so it was invisible in import lists.
- Created model/ (plain Kotlin) for Area, GrowZone, GrowZoneType, PositionCm and the
  nesting/layout rules; moved displayName to ui. Rule for moves: only when a concrete
  second user already exists (here: dialog + ViewModel both use the rules).
- Added a Konsist ArchitectureTest so layer rules run in every `./gradlew build`.
- Found a precision bug: nested positions went Double → Float → Double through Compose's
  Offset (353.3 became 353.29998…). Test-first fix: positions stay Double, Float only at
  draw time. Learned Kotlin has no implicit numeric conversions (20f won't pass as Double).
- Made version-update lint checks informational after a Gradle release broke the build
  with no code change.

- ViewModel scoping (completes Nav3 migration)**
- Scoped GrowZoneViewModel to its Nav3 back-stack entry (ViewModel-store decorator),
  fixing ViewModels accumulating for the app's lifetime: one was left behind per Area
  visited, because they lived in the Activity's store and were never cleared.
- AreaCanvasScreen no longer builds its own ViewModel: it receives growZones and an
  onAddGrowZone callback. It no longer touches the database or Context, and its preview
  works for the first time.
- Verified by hand rather than an automated test (would have needed injection just for
  testability): onCleared() logging plus heap dumps. Started Docs/manual_checks.md for
  checks like this, so others can repeat them.
- Learned to read heap dumps: each class's count includes one `Class` row (the type's
  metadata); instances with empty Depth are unreachable and just waiting for GC.
- Found while profiling: the canvas allocates ~25.8k Stroke and ~26.5k Offset objects
  per 5 s of panning (new Stroke per drawRect; boxed Offsets in a List). Logged for
  Phase 5, not fixed here.
- Gotcha: `./gradlew build`/`test` never compile androidTest/; a broken instrumented
  test only shows up when running compileDebugAndroidTestKotlin or the device task.

**Testing**
- Adopted test first for bugs: find the issue, write a test that proves it, then fix.
  Applied it retroactively to rotation: wrote a state restore test against the Nav3
  version, then checked out the old router to confirm it fails there and passes on the
  fix.
- Wrote the tests myself to learn the code Claude Code generated, rather than only
  reviewing it.
- First instrumented tests: created the missing `androidTest/` folder, set up a Gradle
  Managed Device (headless, separate from my dev emulator). Found that
  `connectedAndroidTest` uninstalls the app afterwards and wiped my dev emulator's data.
- Moved to the v2 Compose test API (v1 is deprecated; v2 queues coroutines rather
  than running them immediately, so async work must be waited for explicitly).
- Debugging: wrote a `dumpLabels()` helper to print on-screen text and content
  descriptions. It showed my assertion was checking a content description as text,
  so the test failed for the wrong reason. Lesson: confirm a red test fails for
  the intended reason before trusting it.

**Creating pre commit hooks, and pr protection**


TO DO: 

- create a make test first mentality for claude code, to ensure clean error reports
- provide a plan for a list system
- provide plan for generic items such as pots and holes, so u can choose from a list, 
rather then recreate new every time. 
- find a few devices to start testing on
- add change area name
- add delete area 
- add delete GrowZone
- Test and fix rotation issues for canvas: `showAddDialog`, `focusedZoneId`, AddGrowZoneDialog fields
- Unit test: every nav key registered in navConfig
- Second managed device at minSdk for older phone testing
- Add decisions doc, clarifying simply decisions made , with why, cost and revisit.
- **Area name cap (50 chars) is enforced only in the UI text field.** Move the rule to
  model/ and check it in the ViewModel too, so future paths (import, sync) can't bypass it.

- Step 4: TAG constant + onCleared() logging in GrowZoneViewModel
- Step D: log before/after (created vs cleared), per-entry ViewModel decorator, hoist
  GrowZoneViewModel out of AreaCanvasScreen (screen takes growZones + onAddGrowZone),
  growZoneDao passed from MainActivity
- Zone-focus-on-rotation (after Step D)
- PR + squash-merge feature/Nav3 (full suite on pixel8 first)
- Later, own branch: Gradle 9.8 bump; second managed device at minSdk
- learn about leakCanary and how to use it, , debugImplementation.
- gain a better understanding how to write this type of code, that uses a garbage collector, things must have no referencing to be removed from heap 
