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
**Partly superseded 2026-10-20:** no lock in 3c and long-press is no longer reserved; the bar
gains a Move button instead (see "Move is an explicit panel").

### 2026-10-14 — Edit size is an inline panel, canvas state, not navigation
**Decision:** inline non-modal panel with live ghost preview, no Nav3
destination, no `AlertDialog`/`ModalBottomSheet`. Top-left corner stays fixed
when the size changes. Contents are measurements only for now.
**Why:** the user must see the zone next to other objects while typing;
separate windows cover the canvas and cause lingering-window false passes.
**Partly superseded 2026-10-20:** top-left is now only the default anchor; from 3b the selected
handle sets it (see "A resize handle sets the anchor").

### 2026-10-14 — Rotation resets focus and transform together (provisional)
**Decision:** `focusedZoneId` and `transform` stay plain `remember` and reset
together on rotation. `selectedZoneId` and the edit panel's zone id and draft
use `rememberSaveable`.
**Why:** a consistent state with no rotation scale math; avoids saving focus
without the zoom (shows "inside a zone" at default zoom).
**Revisit if:** the device feel test shows re-finding the zone is annoying
(then: save focus, recompute with `fitTransform` once measured).
**Revisit triggered 2026-10-29:** on a real phone the reset is annoying inside a zone, in Edit
size and with a zone selected. Selection and edit draft survive; only the repositioning is the
problem. Planned replacement as above; tracked in the phase_3.md polish list.

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
**Partly superseded 2026-10-20:** the bar gains Move instead of a lock, long-press is no longer
reserved for drag-move, and top-left is only the default resize anchor (entries below).

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

### 2026-10-20 — Phase 3 order: 3b, 3e, 3c, 3d
**Decision:** build 3b (drag resize), then 3e (canvas scale), then 3c (move), then 3d (collision).
**Why:** 3e depends only on 3a. Real-device testing starts in 3b; doing 3e next means 3c's move is
felt and measured on the cleaned-up canvas (grid allocations, per-frame recomposition), and
`MIN_SCALE` changes before more gesture code builds on it.
**Revisit if:** 3b decides drag snaps to the grid step (then 3e goes first).

### 2026-10-20 — Move is an explicit panel; GrowZone has no position_locked
**Decision:** a Move button in the action bar opens an inline panel (Done / Cancel) with a ghost
and a move handle; nothing applies until Done. Panning always pans. GrowZone gets no
`position_locked` (never added to the schema); PlacedItem keeps its own, decided in Phase 4.
Replaces the long-press + drag plan.
**Why:** an explicit mode already prevents accidental moves, which is what the lock was for, so
the lock would be a second guard for the same thing. Long-press is slow to discover and fights
pan on a canvas.
**Revisit if:** users report accidental moves inside the panel.

### 2026-10-20 — A resize handle sets the anchor; typed and drag share one function
**Decision:** all four corners get a handle while the edit panel is open; the grabbed or tapped
handle's opposite corner is the anchor (default top-left, 3a's behaviour). Typed entry and
drag both call one pure `resize(rect, newWidth, newDepth, anchor)` in `model/` (Double cm).
Handle visual about 16-24 dp, hit area at least 48 dp, all sizes named constants in one `ui/`
file, in dp.
**Rule:** the four-corner anchor lives only inside `resize()` and the handle layout. The edit
session, `Saver`, DAO and hit-testing don't hard-code four corners, so custom shapes (user-added
vertices as new anchors) replace one function instead of a rewrite.
**Why:** one function makes "drag == typed" true by construction. dp, not mm: dp is what
Android and Material specify touch targets in (48 dp is about 9 mm on any phone), Compose has
no physical unit, and converting to mm needs `xdpi`/`ydpi`, which some devices misreport.
Density buckets make dp vary by about 10-20% in physical size between phones; tuning the
constants on a real phone absorbs that.
**Revisit if:** handle sizes feel wrong across several real phones (then: consider mm via
`xdpi`, checked per device).

### 2026-10-20 — Children stay put on corner resize, travel on move
**Decision:** resizing from a non-top-left corner moves the parent's `xCm`/`yCm`; the same Room
transaction subtracts that shift from each direct child's relative position, so children stay
put on screen. Moving a zone changes only its own row: children are relative to it and travel
with it. No schema change.
**Why:** resizing is "change the outline", not "move the contents"; moving is "move it all".
Everything is computed before the transaction, which holds only the DAO calls.

### 2026-10-20 — Snapping deferred
**Decision:** no snapping in 3b or 3c. If added later: drag only, never typed values; a toggle
next to the drag controls; the drag result passes through one pure function so snapping is a
one-place change.
**Why:** a fixed cm step or a grid step needs 3e's grid and real-device feel first.

### 2026-10-20 — X/Y position fields deferred
**Decision:** the Move panel has no X/Y fields for now.
**Why:** no obvious anchor (which point of the zone do X/Y name, relative to what?).
**Revisit if:** precise placement is requested (with long-press to copy coordinates as a
related idea).

### 2026-10-20 — Positions may be negative
**Decision:** `xCm`/`yCm` may be negative (finite only). Sizes keep the existing validation.
**Why:** resizing from a top or left handle, or moving, can put a zone left of or above its
parent's or the Area's origin; rejecting that would make the gestures fight the user. Area
borders may come later to help users stay inside, likely with panning still allowed outside
them, so every path that reads positions must handle negatives without crashing.
**Revisit in:** Phase 5 (known_issues).

### 2026-10-20 — Edit session is one saveable class, canvas state
**Decision:** the edit session (mode, selected handle, draft values) is one small data class
with a `Saver`, kept with `rememberSaveable`, saved in zone-relative cm; clamping is recomputed
after rotation. Not a Nav3 destination.
**Why:** one place for the draft that typed and drag input both write; the canvas must stay
visible under the ghost, and every Nav3 key must be registered.
