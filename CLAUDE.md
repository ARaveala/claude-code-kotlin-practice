# CLAUDE.md — Plant App (Kotlin/Android Practice Project)

Practicing Claude Code + learning Kotlin, building toward a real UI concept for
a larger plant-tracking project. **Only build what's in the current phase unless
explicitly asked otherwise.**

## Current phase
Phase 3a (manual resize + sizing caps). Scope and exit criteria: `Docs/phases/phase_3/`.

## Safety Rules
- Never auto-commit. Show the diff and let me review before committing.
- Propose schema changes (Room entities/migrations) before applying, same caution
  as sql/log_event.cc on the MariaDB side.
- Explain reasoning before writing non-trivial logic (placement math, nesting
  validation, resize scaling). Boilerplate (Compose scaffolding, navigation) doesn't need it.
- When a task spans multiple files, explain each file's change in a sentence or two
  right after writing it. This is pacing, not an approval gate.

## JVM/Kotlin Runtime Notes (I have a C/C++ background, not Kotlin/JVM)
When explaining code or suggesting patterns, note:
- Primitive (stack-eligible, like C `int`) vs object (heap, GC-managed).
- Autoboxing or closure capture (hidden heap allocation); give the C++ equivalent
  when useful (e.g. "like a lambda with `[&]`, but GC extends the lifetime").
- Flag any allocation inside a loop or a UI recomposition explicitly.
- Never silently introduce reflection, unnecessary wrapping, or wasteful collection
  chains (`.map{}.filter{}.map{}` where one loop would do). Surface the tradeoff
  and let me decide.

## Code Principles
- Favour efficiency. If the efficient approach is notably larger or more complex,
  explain the tradeoff and let me choose.
- Validate untrusted input (API data, calendar sync, user-entered measurements)
  before arithmetic.
- Don't restructure code purely to make it unit-testable. Extract a function only
  if it's naturally separable, or complex/error-prone enough to need independent
  verification (placement math, nesting validation, resize scaling; see
  `ui/CanvasTransform.kt`). Otherwise flag the readability tradeoff and ask.
- Layers: ui → viewmodel → data → model, dependencies only point downward.
  `model/` is plain Kotlin. Root package (MainActivity, PlantUserApp) may use
  everything. Enforced by `ArchitectureTest`.
- Measurements stay `Double` (cm) in model/data/viewmodel. Convert to `Float` only
  when drawing. Never round-trip through Float mid-calculation (see Docs/decisions.md).
- Bugs are fixed test-first: write a test that fails for the intended reason, I check
  the failing output, then fix.
- Every new Nav3 screen gets a state-restore test (open → rotate → still there).
  Every new NavKey must be registered in `navigation/Destinations.kt` or state saving
  crashes at runtime.
- User-created UI state (open dialogs, typed text) uses `rememberSaveable`;
  measured/derived values (e.g. canvas size) use `remember`.

## Style (ktlint)
- `.editorconfig` is the source of truth for style. Don't reformat existing code or
  touch unrelated lines.
- Don't run `ktlintFormat`/`ktlintCheck`; I run them myself.

## Error/Validation Messages & Logging
- Validation rejections use named constants in a central file (e.g. `ValidationMessages.kt`:
  `SIZING_CAP_EXCEEDED`, `NESTING_DEPTH_EXCEEDED`, `COLLISION_DETECTED`), never ad hoc
  strings. Each message states what failed, where (file/function), and enough context
  to debug. New validation case = new named constant.
- Log with Android `Log.d`/`Log.e`, not `println`. One tag per file (tag = class/file
  name). Add logging as each function is built, not later.

## Commands
- `./gradlew build`: lint (warnings are errors) + all unit tests incl. ArchitectureTest.
  The Run button only installs, it doesn't test.
- `./gradlew pixel8DebugAndroidTest`: instrumented tests on the managed device; must
  pass before a PR.
- `./gradlew connectedAndroidTest` WIPES the target emulator's data. Never on the dev emulator.
- Single test class: add `-Pandroid.testInstrumentationRunnerArguments.class=<fqcn>`.
- Check `gradle/libs.versions.toml` for current dependency versions; don't assume
  from training data.
- Git conventions: `Docs/git_workflow.md` (commit prefixes enforced by hook).

## Docs: read only when relevant
- `Docs/roadmap.md`, `Docs/domain_model.md`: at the start of a phase, or when a task touches entities/rules.
- `Docs/known_issues.md`: when starting a phase ("Potential Concerns" may have become relevant).
- `Docs/decisions.md`: before changing behaviour that looks like a bug (it may be intentional).
- `Docs/manual_checks.md`: when a change touches an area it lists.
- Don't read unless asked: `Docs/progression_log.md`, `Docs/kotlin_notes.md`,
  `Docs/android_compose_notes.md`, `Docs/todo` (private, gitignored study notes).