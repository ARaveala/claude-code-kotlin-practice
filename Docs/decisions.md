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
**Superseded 2026-10-17:** see "System back clears, then zooms out one level".

### 2026-10-01 — Layered packages, root package assembles the app
**Decision:** ui → viewmodel → data → model, downward only; `model/` is plain Kotlin.
MainActivity and PlantUserApp live in the root package and may depend on everything.
**Why:** removed a ui ⇄ viewmodel cycle; rules reusable across screens; model/ is
cross-platform-ready. Enforced by ArchitectureTest.

### 2026-10-01(updated 2026-10-17) — Measurements in Double end to end
**Decision:** cm values stay Double through model/data/viewmodel; Float only when drawing.
`absolutePositionCm` returns `PositionCm` and sums in Double, `hitTestZone` compares in Double, `fitTransform` and the canvas convert once, after the multiply. Canvas draw data is precomputed with `remember` (`DrawZone`) and the border `Stroke` is hoisted.
**Why:** a Double → Float → Double round trip (via Compose `Offset`) silently changed
353.3 to 353.29998…The draw loop must be sized for hundreds of zones: PositionCm,
the parent-chain walk and a Stroke per zone per frame all scale with zone count (200 zones:
p50 65 to 48 ms, janky 76% to 53%).
**Guarded by:** precision unit test on `nextNestedPositionCm` and `absolutePositionCm`.

### 2026-10-01 — "Newer version available" lint checks are informational
**Decision:** `AndroidGradlePluginVersion`, `GradleDependency`, `NewerVersionAvailable`
reported but don't fail the build.
**Why:** they depend on the date, not the code; version bumps are deliberate commits.

### 2026-10-01 — Instrumented tests run on a Gradle Managed Device
**Decision:** `pixel8DebugAndroidTest` is the check that must pass before a PR.
**Why:** headless, no manual emulator start, and `connectedAndroidTest` uninstalls the
app (wiping a dev emulator's data). DSL is @Incubating — may need edits on AGP upgrades.

### 2026-10-02 — No separate navConfig registration test
**Decision:** rely on a state-restore test per screen instead of a dedicated
registration guard.
**Why:** rotation tests already exercise the same lookup and catch a missing
registration; a separate guard adds maintenance for no extra coverage.
**Revisit if:** a missing registration ever reaches a device, or screens are added
without their rotation test.

### 2026-10-14 — Tap selects, entering a zone is an explicit action
**Decision:** Tapping a zone sets `selectedZoneId` and shows an inline action bar
(Enter, Edit size; lock in 3c). Entering is the bar's Enter button. Supersedes
"tap zooms" in the 2026-10-06 entry (zone focus is still canvas state).
**Why:** edit/resize needs the zone selected while neighbours stay visible at
scale; double-tap-to-select adds ~300 ms latency to taps; long-press is
reserved for 3c drag-move.
**Rules:** bar is non-modal; selection survives pan/zoom; empty-canvas tap
clears it; focused zone ignores taps; panning never selects.
**Revisit if:** entering feels slow (then: tapping an already-selected zone
enters it). Gesture handler only calls `onZoneTapped`, so this is one handler.

### 2026-10-14 — Edit size is an inline panel, canvas state, not navigation
**Decision:** inline non-modal panel with live ghost preview, no Nav3
destination, no `AlertDialog`/`ModalBottomSheet`. Top-left corner stays fixed
when the size changes. Contents are measurements only for now.
**Why:** the user must see the zone next to other objects while typing;
separate windows cover the canvas and cause lingering-window false passes.

### 2026-10-14 — Rotation resets focus and transform together (provisional)
**Decision:** `focusedZoneId` and `transform` stay plain `remember` and reset
together on rotation. `selectedZoneId` and the edit panel's zone id and draft
use `rememberSaveable`.
**Why:** a consistent state with no rotation scale math; avoids saving focus
without the zoom (shows "inside a zone" at default zoom).
**Revisit if:** the device feel test shows re-finding the zone is annoying
(then: save focus, recompute with `fitTransform` once measured).

### “2026-10-15 — cm to Float conversion happens once, at the draw boundary.
**Decision:** AbsolutePositionCm returns PositionCm (Double) and sums in Double; hitTestZone compares in Double; fitTransform and the canvas convert after the multiply. The canvas precomputes draw-ready DrawZone values with remember.
**Why:** Float accumulation lost 0.005 cm over 3 nesting levels at 90,000 cm (precision test), and the old code allocated nothing extra but rounded every level.

### 2026-10-17 — Sizing limits are a numeric guard in one file
**Decision:** min 1 cm; hard max 100,000 cm width/depth, 1,000 cm height; soft
warning at 5,000 cm (initial). All in `model/SizingLimits.kt`. Grid on adaptive scale min 1 m cells max 1000m.
**Why:** The cap protects against
overflow/NaN/fat-finger. Zoom clamp and grid LOD are the real phone protection
(open in phase_3.md).
**Revisit if:** zoom range is set and the largest zone doesn't fit at min zoom. (phase 3e)

### 2026-10-17 — GrowZone interaction is canvas state, not navigation
**Decision:** Tap selects (`selectedZoneId`, inline non-modal action bar: Enter, Edit size;
lock in 3c). Enter zooms within AreaCanvasScreen (`focusedZoneId` + transform), no Nav3
destination. Edit size is an inline non-modal panel with live ghost preview, measurements
only, top-left corner fixed.
**Rules:** the bar never consumes canvas gestures; selection survives pan/zoom; empty-canvas
tap clears it; the focused zone ignores taps; panning never selects.
**Rotation (provisional):** `focusedZoneId` and `transform` are plain `remember` and reset
together. `selectedZoneId`, the panel's zone id and its draft use `rememberSaveable`.
**Why:** neighbours stay visible at scale while editing; double-tap adds ~300 ms latency;
long-press is reserved for 3c drag-move; separate windows cover the canvas.
**Revisit if:** entering feels slow (then: tap on a selected zone enters), or re-finding the
zone after rotation is annoying (then: save focus, recompute with `fitTransform`).

### 2026-10-17 — System back clears, then zooms out one level (provisional)
**Decision:** System back, in order: if a zone is selected or the edit panel is open, clear
all of it (selection, panel, draft) and stop. Otherwise, if focused inside a zone, zoom out
one nesting level. At the Area's top level, leave the Area. The toolbar's top-left arrow does
exactly the same (one shared `navigateBack`, fixed 2026-10-20: it used to leave the Area
directly). Matches the on-canvas zoom-out button. Supersedes "System back on the canvas leaves
the Area" (2026-10-01).
**Why:** the code already zoomed out per level; clearing first means back never discards a
zoom level while the user is mid-edit.
**Revisit if:** the device feel test shows per-level back is slow from deep nesting (then:
jump straight to the Area's top level).
