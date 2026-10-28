
**set up**
- Followed https://developer.android.com/studio/install for setup.
- 64-bit lib packages in the official docs were outdated/unnecessary - skipped, installed tar package.
- Unpacked in `~` rather than a system wide location - single-user machine, no need for sudo managed shared install.
- Chose Empty Activity template (Kotlin, Compose) per roadmap/CLAUDE.md plan.
- Scaffolded project into existing repo folder; package name resolved to `com.practice.plantUser`.
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

**Pre-commit hooks, lint, CI and PR protection**
Done now while any reconfiguration is still easy, and so that checks which shouldn't
cost time (formatting, secrets) are automatic rather than caught in review.
- Added ktlint with a `.editorconfig` as the style source of truth. Also added this to a pre-commit hook
Gotcha: the Gradle
  plugin version (14.x) and the ktlint engine version (1.x) are different numbers; using
  the plugin's version as the engine's fails with "Could not find ktlint-cli".
- Added a GitHub Actions CI workflow (ktlint, lint, tests, gitleaks), so the same checks
  run even if someone turns off the local hooks. Branch protection to follow once the
  checks have run once.
- Looked at extras for later: detekt (static analysis for complexity), editorconfig-checker
  (whitespace rules for non-Kotlin files), Dependabot (dependency updates), LeakCanary
  (memory leaks, debug builds only). None needed yet.
- Added gitleaks (secret scanning, with a custom rule for home-directory paths) and a
  license considerations doc, as a reminder in case this ever goes public. Project is MIT.
- Simplified CLAUDE.md. The documentation is growing and every always-loaded line costs
  tokens. I also want to keep learning Kotlin myself: Claude makes some fundamental
  mistakes I can already spot as a beginner, so I'm keeping the review habit.

**Phase 3a prep**
- Planned 3a with Claude before restarting Claude Code. Settled the cap design: min 1 cm,
  hard max 100,000 cm for width/depth and 1,000 cm for height, soft warning (non-blocking,Accept/Cancel) above 5,000 cm, all in one SizingLimits file; grid becomes 1 m cells.
  A size cap protects against bad numbers, not the phone: draw cost depends on what is on
  screen, so the zoom clamp and grid level of detail matter more (open questions in the phase file).
- Interaction redesign: tap now selects a zone and shows a non-modal action bar (Enter, Edit
  size); the edit panel is an inline panel with a live ghost preview, canvas state rather than
  a Nav3 destination. Rotation resets focus and transform together for now (provisional,
  feel-test on a device).
- Rewrote phase_3.md (scope, decided, out of scope, open questions, exit criteria) and wrote
  the matching patches for domain_model, decisions, known_issues and manual_checks.
- Test-first fix: the add-GrowZone dialog's type/width/depth/height used `remember`, not
  `rememberSaveable`. Wrote a restore test, saw it fail for the intended reason, fixed it.
  Restore tests now collect post-restore failures in a list and throw one summary
  (ErrorCollector was dropped: the HTML report only keeps one failure per test).
- Incident: Android Studio's "Add dependency" quick-fix silently edited libs.versions.
   toml and build.gradle.kts (pinned ui-test, Espresso 3.7.0), which broke androidTest resolution (consistent resolution wanted 3.5.0). Found via Local History, reverted. Lesson: decline that quick-fix; dependency changes are deliberate commits.

**Float to Double fix (2026-10-15)**
- Wrote a precision test first: three nested zones at 90,000.3 / 353.3 / 0.1 cm. It failed
  with 90353.6953125 instead of 90353.7, so absolutePositionCm was losing ~0.005 cm in Float.
- Fixed absolutePositionCm (returns PositionCm, sums in Double), hitTestZone (compares in
  Double) and fitTransform and the draw loop (convert to Float once, after the multiply).
  Float is only for pixel space; the model, data and viewmodel stay Double.
- Float round trip is a correctness issue, not a visual one. Double vs Float costs
  almost nothing in speed or memory; per-frame allocation is what matters.
- Recorded M5 (scripted adb swipe loop, gfxinfo, Profiler allocations), three runs per state.
  Baseline vs PositionCm: Stroke and Offset ~120 per frame each in both (from the grid loop, not the zones); timing unchanged within noise; PositionCm adds ~4 per frame at ~4 zones.
- Decision: the draw loop must be sized for hundreds of zones (custom zones, many small
  ones). The per frame PositionCm allocation, the parent-chain walk and a Stroke per zone all scale with zone count, precompute (remember a DrawZone list) hoisted. Grid loop allocation stays a Phase 5 item.
- Huge docs refactoring again, must find a way to lower documentation level , this is too much.

**Phase 3a — Manual resize + sizing caps (done 2026-10-20)**
- Built tap to select with a non-modal action bar (Enter, Edit size), a breadcrumb name box
  showing the full path, and the inline edit panel with a dashed ghost preview. Caps live in
  `model/SizingLimits.kt` and are checked again in the ViewModel; NaN/infinity rejected. The
  soft warning (Accept/Cancel above 5,000 cm) is in both the panel and the create dialog.
  `updateSize` writes only the size columns.
- Back order decided: clear selection/panel, then zoom out one level, then leave the Area.
  Found two bugs by hand on the emulator: the toolbar arrow left the Area directly, and the
  breadcrumb missed the parent. Both fixed test-first; back now has one shared function.
- New test habit: every assert names the function, the input and the file, so a failure is
  readable without opening the test.
- Test bug, not app bug: a tap at 600 cm (= 600 dp) was off a ~412 dp wide Pixel 8. Added a
  geometry guard that fails as a test bug instead of looking like an app bug.
- Refactor measured before/after with the Profiler (200 zones, 3 runs): the grid allocated a
  `Stroke` per cell per frame, ~259/frame -> 0 after hoisting. Earlier zone loop fix (DrawZone
  precompute + hoisted border Stroke): Stroke/frame 311 -> 136, PositionCm ~180 -> 0. The
  dimension Regex was compiled on every keystroke; now once (the `Matcher` per call stays,
  that's how java.util.regex works). Timing within noise. Learned: Profiler-on timing isn't
  comparable with Profiler-off; divide allocation counts by frames rendered.
- Added a second managed device, `minSdk24` (Small Phone, API 24; needs the experimental
  `allowOldApiLevelDevices` flag, no ATD image that old). Full suite passes on both.
- Landscape dialog bug: on the Pixel 8 the add-GrowZone dialog showed its buttons but cut off
  width/depth and hid height. My first two tests passed on the broken code. Cause: the fields
  were squeezed (laid out shorter by the AlertDialog's Column), not clipped, so checking
  visible pixels against layout size proved nothing. Measuring laid-out height against
  `TextFieldDefaults.MinHeight` failed on both devices, which showed the API 24 phone had it
  too and I had missed it by eye. Fixed with a scrolling Column. Lesson: a test-first test
  that passes on broken code means the test doesn't reproduce the bug yet.

**Phase 3b-3e planning (2026-10-20)**
- Order: 3b, 3e, 3c, 3d. 3e depends only on 3a, and doing it after 3b means 3c is felt on a
  clean canvas.
- Resize: four corner handles while the edit panel is open; the handle sets the anchor; typed
  and drag share one `resize()` in model/. Kept the four-corner anchor inside that function so
  custom shapes (user-added vertices as new anchors) don't need a rewrite.
- Move: an explicit Move panel replaces long-press and `position_locked` on GrowZone.
- Positions may be negative, with Area borders a likely later addition, so every path that
  reads positions must handle negatives without crashing.


**phase3b**
- Drew circles to show rough layout of corner handlers for reszing the ghost growZone.
- Refactored this new code and am trying to figure out how to best apply files and functions,
as i noticed that AreaCanvasScreen.kt is getting large and messy . This needs to be refactored
- added a resize that uses enum class to track anchor (this is drawing the handle marker at anchor marker, this may be a better way to inform user of anchor then a separate marker, choice yet to be made)
- Todo has simple test concepts to next write
TODAY
- Tested manually the changing of Anchors does change edit size direction correctly
- Adjusted topleft for finger gap drawResizeHandle and adjusted anchor for draw anchor.
- added tests for resize to ensure that when we apply custome shapes, we dont mess with original functionality . (worth noting in the phase that this test may throw red)