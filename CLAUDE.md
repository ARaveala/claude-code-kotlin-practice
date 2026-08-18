# CLAUDE.md — Plant App (Kotlin/Android Practice Project)

Purpose: practicing Claude Code + learning Kotlin, building toward a real
UI concept for a larger plant tracking project. See domain_model.md for
entity/nesting rules and roadmap.md for current build phase, **only
build what's in the current phase unless explicitly asked otherwise.**

## Safety Rules
- Never auto-commit. Always show the diff and let me review before committing.
- Always propose schema changes (Room entities/migrations) before applying 
  same caution as sql/log_event.cc on the MariaDB side.
- Explain reasoning before writing non-trivial logic (placement math,      nesting validation, resize scaling), don't just hand me a finished function. Boilerplate (Compose scaffolding, navigation setup) doesn't need this.

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
- Error messages (dev builds): must state what failed, where (file/function),
  and enough surrounding context to debug fast, not a generic exception message.
- Logging: use Android's `Log` class (`Log.d`/`Log.e` etc.), not `println`,
  filterable by tag, strippable from release builds. Use one tagged logger
  convention per file (tag = class/file name), consistently.

## Project Structure
```
app/src/main/java/com/practice/plant_user/
  ├── ui/          Compose screens and layout components
  ├── data/        (phase 2)Room database, entities (GrowZone.kt, PlacedItem.kt, Area.kt, etc.)
  ├── viewmodel/   (phase 2)State-holding classes between UI and data
  └── MainActivity.kt
app/src/main/res/  Icons, strings, colors
Docs/              General docs for project
```

## Build/Run Commands
- Build: `./gradlew build`
- Install debug build to connected device/emulator: `./gradlew installDebug`
- Run unit tests: `./gradlew test`
- Run instrumented tests (needs emulator/device running): `./gradlew connectedAndroidTest`
- Launch from Android Studio: Run ▶ button, or Shift+F10

## Git Workflow
See git_workflow.md, commit-msg hook enforces `feat:`/`fix:`/`refactor:`/
`test:`/`docs:`/`chore:` prefixes. Branch per feature, PR + self-review
before merging into main, even solo.

## Reference Files
- `Docs/domain_model.md` — entities, nesting rules, sizing/rendering rules
- `Docs/roadmap.md` — phased build plan, current scope boundary
- `Docs/git_workflow.md` — branching and commit conventions
- `Docs/known_issues.md` — build/tooling gotchas (created as they come up)
- `Docs/progression_log.md` — session log: what was built, what broke, what was learned