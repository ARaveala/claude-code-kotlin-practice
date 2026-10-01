# Decisions

Dated record of deliberate choices. Undecided questions live in known_issues.md
(Potential Concerns) instead, and move here once settled.

### 2026-10-01 — Navigation 3, migrated early
**Decision:** Replaced the if/else screen router with Nav3 `NavDisplay` while only two
screens existed.
**Why:** cheap now, expensive later; runtime is multiplatform (keeps cross-platform open).

### 2026-10-01 — Back stack saved via explicit `navConfig`
**Decision:** `rememberNavBackStack(navConfig, ...)` with a hand-registered serializer
table, not the reflection-based default.
**Why:** no runtime reflection; the multiplatform-compatible path.
**Cost:** every new NavKey must be registered in Destinations.kt, or saving state
crashes at runtime.
**Revisit if:** Nav3 offers compile-time registration.

### 2026-10-01 — System back on the canvas leaves the Area (verify)
**Decision:** System back = leave the Area, same as the toolbar arrow. Zooming out one
level is the separate on-canvas button only.
**Why:** prefer an explicit zoom-out control; zooming isn't treated as navigation.
**Revisit if:** users expect back to zoom out (then: `BackHandler(enabled = focusedZoneId != null)`).

### 2026-10-01 — Layered packages, root package assembles the app
**Decision:** ui → viewmodel → data → model, downward only; `model/` is plain Kotlin.
MainActivity and PlantUserApp live in the root package and may depend on everything.
**Why:** removed a ui ⇄ viewmodel cycle; rules reusable across screens; model/ is
cross-platform-ready. Enforced by ArchitectureTest.

### 2026-10-01 — Measurements in Double end to end
**Decision:** cm values stay Double through model/data/viewmodel; Float only when drawing.
**Why:** a Double → Float → Double round trip (via Compose `Offset`) silently changed
353.3 to 353.29998…; Phase 3 needs typed and dragged measurements to compare equal.
**Guarded by:** precision unit test on `nextNestedPositionCm`.

### 2026-10-01 — "Newer version available" lint checks are informational
**Decision:** `AndroidGradlePluginVersion`, `GradleDependency`, `NewerVersionAvailable`
reported but don't fail the build.
**Why:** they depend on the date, not the code; version bumps are deliberate commits.

### 2026-10-01 — Instrumented tests run on a Gradle Managed Device
**Decision:** `pixel8DebugAndroidTest` is the check that must pass before a PR.
**Why:** headless, no manual emulator start, and `connectedAndroidTest` uninstalls the
app (wiping a dev emulator's data). DSL is @Incubating — may need edits on AGP upgrades.