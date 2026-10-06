# CLAUDE.md — Plant App (Kotlin/Android Practice Project)

Purpose: practicing Claude Code + learning Kotlin, building toward a real
UI concept for a larger plant tracking project. See domain_model.md for
entity/nesting rules and roadmap.md for current build phase, **only
build what's in the current phase unless explicitly asked otherwise.**

## Current Status
Phase 2 (GrowZone creation + nesting validation) is complete — see
`Docs/progression_log.md`. **Phase 3 (GrowZone completeness — resize,
move, containment) is in progress, currently on Phase 3a.** Phase 3 is
split into sub-phases for testability; see `Docs/phases/phase3/` for
the full breakdown and exit criteria, `roadmap.md` for the summary.

Phase 3a scope: manual measurement entry (no gestures yet), determining
real sizing caps via boundary tested limits, with safety and UX in mind.
Drag resize (3b), drag move (3c), and collision enforcement + the
`bounds_enforced` toggle (3d) are scoped but not started.


## Safety Rules
- Never auto-commit. Always show the diff and let me review before committing.
- Always propose schema changes (Room entities/migrations) before applying 
  same caution as sql/log_event.cc on the MariaDB side.
- Explain reasoning before writing non-trivial logic (placement math,      nesting validation, resize scaling), don't just hand me a finished function. Boilerplate (Compose scaffolding, navigation setup) doesn't need this.
- When a task spans multiple files, explain each file's change briefly
  right after writing it — a sentence or two is enough — rather than
  saving it all for one summary once everything is done. This is about
  pacing so I can follow along as we go, not a stop-and-wait-for-approval
  gate on every file.

## JVM/Kotlin Runtime Notes (I have a C/C++ background, not Kotlin/JVM)
When explaining code or suggesting patterns, always note:
- Whether a value is a primitive (stack-eligible, like C `int`) or an object
  (heap, GC-managed)
- Whether a collection/lambda causes autoboxing or closure capture (hidden
  heap allocation)
- Point out the JVM/C++ equivalent when relevant (e.g. "this is like a
  `std::vector` push_back, amortized allocation" or "this closure captures
  `x` by reference, similar to a C++ lambda with `[&]` capture, but the
  object outlives the stack frame here because GC manages lifetime")
- Flag any allocation happening inside a loop or a UI recomposition, call
  it out explicitly, don't just write it silently
- Never silently introduce reflection, unnecessary object wrapping, or
  inefficient collection operations (e.g. `.map{}.filter{}.map{}` chains
  that allocate three intermediate lists where a single loop would do),
  surface the tradeoff and let me decide

## Code Principles
- Favour efficiency. If the efficient approach means notably larger/more
  complex code, explain the tradeoff and let me choose, don't pick silently.
- Validate untrusted input before arithmetic (API responses, calendar sync
  data, any user-entered measurement).
- Prefer stack/primitive over heap where possible; flag allocations per the
  runtime notes above.
- Before reaching for "extract a function + write a unit test," consider
  whether a simpler check proves correctness just as well (a one-off
  script, a manual verification) — don't restructure code purely to make
  it unit-testable. Only pull logic into its own function when either:
  it's already a naturally separable, well-named piece of code (reads
  cleaner on its own merit, not just isolated for a test), or the logic
  is complex/error-prone enough to warrant independent verification
  (placement math, nesting validation, resize scaling — see Safety
  Rules). If extraction would only serve the test and make the
  surrounding code harder to follow, that's a readability tradeoff to
  flag and ask about, not decide silently — see `ui/CanvasTransform.kt`
  for a case judged worth it (Phase 1's pan/zoom math).
  - Layers: ui → viewmodel → data → model; dependencies only point downward. `model/` is
  plain Kotlin. Root package (MainActivity, PlantUserApp) may use everything. Enforced
  by `ArchitectureTest` in `./gradlew test`.
- Measurements stay `Double` (cm) in model/data/viewmodel. Convert to `Float` only at
  the drawing step in ui (Compose draws in Float px). Never round-trip through Float
  mid-calculation (e.g. Compose `Offset`) — see decisions.md.
- Bugs are fixed test-first: write a test that fails for the intended reason, then fix, user(me) must check
failed test output first for clarity.
- Every new Nav3 screen gets a state-restore test (open screen → rotate → still there)
  in the instrumented navigation tests. This also catches a NavKey missing from
  navConfig, which otherwise crashes on rotation.
- UI state the user deliberately created (open dialogs, typed text) uses
  `rememberSaveable`; measured/derived values (e.g. canvas size) stay `remember`.

## Error/Validation Messages & Debug Logging
- Error/validation messages (dev builds): named, centralized constants
  (e.g. `ValidationMessages.kt`) — one per validation rejection case
  (`SIZING_CAP_EXCEEDED`, `NESTING_DEPTH_EXCEEDED`, `COLLISION_DETECTED`,
  etc.), referenced everywhere rather than ad hoc strings at each call
  site. Each message must state what failed, where (file/function), and
  enough surrounding context to debug fast — not a generic exception
  message. New validation cases get a named constant here as a standing
  rule, not a one-phase task.
- Logging: Android's `Log` class (`Log.d`/`Log.e`), not `println` —
  filterable by tag, strippable from release builds. One tagged logger
  convention per file (tag = class/file name), consistently. Added incrementally as each new
  function is built, not deferred to real-device testing — Phase 1's
  `CanvasTransform.kt` logging is what made the emulator-touch anomaly
  diagnosable in the first place, and instrumentation needs to already
  be trustworthy before device-specific bugs show up on top of it.

## Project Structure
```
app/src/main/java/com/practice/plant_user/
  ├── MainActivity.kt   — Android entry point only: edge-to-edge, theme, builds the
  │                       DAO(s) from applicationContext and passes them to PlantUserApp.
  │                       Keep it thin; platform setup lives here, app logic doesn't.
  ├── ui/
  │    PlantUserApp.kt      — root composable: AreaViewModel, Nav3 back stack + NavDisplay
  │    AreaListScreen.kt    — Area list, add dialog, name/count caps (Phase 1)
  │    AreaCanvasScreen.kt  — pannable/zoomable canvas + grid, GrowZone rendering,
  │                           tap-to-zoom, add-GrowZone dialog (Phase 1–2)
  │    CanvasTransform.kt   — pure pan/zoom + grid math, unit-tested (Phase 1)
  │    navigation/
  │      Destinations.kt    — Nav3 screen keys (@Serializable NavKey) + navConfig.
  │                           Every key MUST be registered in navConfig or state
  │                           saving crashes at runtime.
  │    theme/               — Material theme (template; dynamic colour on Android 12+)
  ├── viewmodel/     AreaViewModel, GrowZoneViewModel. Depends on model + data.
  ├── data/          Room 3: GardenDatabase, entities, DAOs. Depends on model.
  └── model/         Plain Kotlin, no Android/Compose/Room. Area, GrowZone, GrowZoneType,
                     PositionCm, GrowZoneRules.kt (nesting + layout rules). Depends on nothing.
app/src/test/java/com/practice/plant_user/         — JVM unit tests (no emulator):
                                                     ui/ pure functions, viewmodel/
app/src/androidTest/java/com/practice/plant_user/  — instrumented tests (emulator):
                                                     state restore across rotation
app/src/main/res/  Icons, strings, colors
Docs/              General docs for project (see Reference Files below)
```

## Build/Run Commands
- Lint is strict (`app/build.gradle.kts`): `warningsAsErrors = true`,
  `abortOnError = true` — a lint warning fails the build, not just errors.
- Dependency versions are pinned in `gradle/libs.versions.toml` and get
  bumped independently of chat sessions — check that file for current
  versions before adding a new dependency (e.g. Room in Phase 2), don't
  assume a version from training data.
- Build: `./gradlew build` runs lint + all unit tests (including ArchitectureTest). The Run ▶
  button only installs; it does not run tests.
- Run instrumented tests on the managed device (headless, boots its own emulator;
  the check that must pass before commit/PR): `./gradlew pixel8DebugAndroidTest`
- Run instrumented tests on a running emulator (faster while writing a test):
  `./gradlew connectedAndroidTest`. WARNING: uninstalls the app afterwards, wiping
  that emulator's data. Use a dedicated test AVD, not the dev one.
- Run a single test class:
  `./gradlew pixel8DebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.practice.plant_user.PlantUserAppNavigationTest`
  (append `#methodName` for one test)
- Test reports: `find app/build/reports -name index.html`; per-test logcat:
  `app/build/outputs/androidTest-results/`
- Launch from Android Studio: Run ▶ button, or Shift+F10

## Git Workflow
See git_workflow.md, commit-msg hook enforces `feat:`/`fix:`/`refactor:`/
`test:`/`docs:`/`chore:` prefixes. Branch per feature, PR + self-review
before merging into main, even solo.

## Reference Files
- `Docs/domain_model.md` — entities, nesting rules, sizing/rendering rules
- `Docs/roadmap.md` — phased build plan, current scope boundary
- `Docs/git_workflow.md` — branching and commit conventions
- `Docs/known_issues.md` — two sections: "Known Issues" (build/tooling gotchas)
  and "Potential Concerns" (forward-looking design watch-items, not bugs — check
  this before starting a new phase, some entries may become relevant)
- `Docs/progression_log.md` — session log: what was built, what broke, what was learned
- `Docs/kotlin_notes.md`, `Docs/android_compose_notes.md` — user's own private,
  gitignored Kotlin/Compose study notes (not in the repo). May reference these
  in conversation; don't expect them to exist on a fresh checkout.
- `Docs/decisions.md` — why things are the way they are (dated, with "revisit if").
  Read before changing a behaviour that looks like a bug — it may be intentional.