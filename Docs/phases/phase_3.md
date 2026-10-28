- [x] **Phase 3a — Manual resize + sizing caps**
- **Name box** name box on top left next to back button: breadcrumb of the full path, parent to
  child, to the selected zone (or to the focused zone when nothing is selected), e.g.
  Area › House1 › Nested. Each name is clickable and selects that zone; the selected one is
  highlighted. Selecting only, focusing from the breadcrumb is later (known_issues).
- **Action bar rules.** Non-modal: no scrim, consumes no touches outside its
  own bounds. Selection survives pan and zoom. Panning must never select a
  zone. Hit-testing ignores the currently focused zone (only its children and
  empty space respond) and picks the smallest zone under the finger.
  The gesture handler only calls `onZoneTapped(id)`; `enterZone(id)` and
  `openEditPanel(id)` are separate functions so the gesture mapping can change
  later without touching the actions.
  FIX
- **Edit size panel.** Inline panel anchored at the bottom (side in landscape),
   part of the canvas composition. Canvas state, not a Nav3 destination and not a dialog
   (reasons: decisions.md, "GrowZone interaction is canvas state").
   Contents are measurements only: width, depth, height (height nullable, every type: the user decides how to use it).
   The soft warning confirmation (Accept / Cancel) is shown inside the panel, not as a separate window.
  Every input with width/depth (create dialog and edit panel) shows the same soft warning.
- **Live ghost preview.** While typing, the new width x depth is drawn as a
  ghost outline over the zone, only when the parsed value is valid. Nothing is
  applied until confirm; cancel discards the draft. The top-left corner stays
  fixed on resize. Hoist the ghost's `Stroke` out of the draw lambda (no new
  per-frame allocations, see known_issues).
- **Sizing caps**, all in one file (`model/SizingLimits.kt`), plain `Double` cm,
  so every value changes in one place:
  Height is not visualised yet; it is stored to later tell the user a pot or
  plant doesn't fit. Hard caps are a numeric safety ceiling (the phone cost of
  a large zone is negligible, see Decided). The same validation function serves the
  create dialog and the edit panel, since they share fields.
- **Soft warning (non-blocking fat-finger check).** When width or depth is
  above `SOFT_WARN_WIDTH_DEPTH_CM` (and within the hard cap), show a
  confirmation: "That's very large for a single zone, is that correct?" with
  the entered number, **Accept** and **Cancel**. Save stays enabled. Accept
  applies the size; Cancel resizes nothing. The warning is awareness only,
  never a validation failure.
- **Grid and zoom range:** Phase 3e. Until then the existing grid and `MIN_SCALE` stay, and
  a zone too large to fit at `MIN_SCALE` is framed clipped.

### Exit criteria
- Boundary tests, same standard as Phase 2's nesting depth cap, on the
  validation function in `model/` (Double cm):
  - width/depth: 0.99 rejected, 1.0 accepted; 99,999 accepted, 100,000
    accepted, 100,001 rejected.
  - height: 1,000 accepted, 1,001 rejected.
  - soft warning: 5,000 no warning, 5,001 warns.
  - Cancel on the warning leaves the stored size unchanged.
- Hit-test unit tests: tap inside, outside, on a nested child, on the focused
  zone (ignored), smallest-zone-wins.
- The preview size equals the size applied on confirm, numerically equal in
  Double cm (sets up 3b's criterion).
- State-restore test: rotate with the panel open, selected zone and draft text
  kept.
  FIX
- Manual (manual_checks.md): M2 (edit panel included), M6 (panning doesn't select),
  second managed device at minSdk before physical phone testing.
- NaN, +/-infinity, zero, negative: rejected with the named constants.

Build order after 3a: **3b, then 3e, then 3c, then 3d** (3e depends only on 3a; decisions.md
2026-10-20).

- [ ] **Phase 3b — Drag resize**
  Finger drag resize from any corner. Still no collision enforcement. No schema change.
  - **Resize math first.** First commit of 3b: a pure function in `model/` on Double cm,
    `resize(rect, newWidth, newDepth, anchor)`, returning the new rect with the anchor point
    unchanged. 3a's top-left behaviour is its first test case. Typed entry and drag resize both
    call it, which is how the "numerically equal" criterion is met. Don't code into a corner:
    custom shapes will add user-created vertices as new anchors, so the four-corner anchor stays
    inside `resize()` (and the handle layout); nothing else (edit session, Saver, DAO,
    hit-testing) should hard-code a closed set of four corners. Custom shapes then replace or
    generalise one function ("move vertex i, keep vertex j fixed").
  - **Handles.** One drag handle per corner, shown only while the edit panel is open. The handle
    you grab sets the anchor: the opposite corner stays fixed. Handles sit outside the corners
    with a gap so the corner itself stays visible.
  - **Handle sizes.** Visual handle small (about 16-24 dp); invisible hit area at least 48 dp
    (start at 48, try 56-64 on a real phone). Offset from the corner about half the hit size.
    Below a minimum on-screen zone size, corner handles only (hides user-added vertex handles
    once custom shapes exist; in 3b every handle is a corner, so this tier is a no-op); below
    about one hit size, no handles and a "zoom in to resize" hint. All values are named constants in one file in `ui/`,
    in dp, tuned on a real phone (dp reasoning: decisions.md 2026-10-20).
  - **No jump on touch-down.** Record the offset between the touch point and the handle centre at
    touch-down and keep it for the whole drag, so the first frame doesn't snap the corner to the
    finger.
  - **Live feedback.** The ghost shows the new size before anything is committed, with a live
    readout of its size (e.g. "153.3 x 80.0 cm"). The panel's numbers update live while dragging.
  - **Typed resize with an anchor.** Tapping a handle selects it (highlighted); its opposite
    corner becomes the anchor, marked with a small dot. Nothing selected = top-left anchor (3a's
    behaviour). Tapping the zone body in edit mode clears the handle selection. No other zone is
    hit-tested in edit mode: the user leaves with Accept/Save or Cancel.
  - **Soft warning, Accept and Cancel** behave as in 3a. Leave room in the panel for an undo
    button later (known_issues).
  - **Children stay put.** Resizing from any corner other than top-left moves the parent's
    `xCm`/`yCm`. In the same Room transaction, subtract that shift from each direct child's
    relative position, so children don't move on screen (grandchildren are relative to their own
    parent and don't change). Compute everything first; the transaction holds only the DAO
    calls; log failures.
  - **Positions may be negative** (finite values only). Sizes keep the existing validation.
    Negative positions must never crash or misbehave anywhere (grid, hit-test, `fitTransform`,
    `absolutePositionCm`): Area borders may come later, with panning still allowed outside them.
  - **Edit session state.** Mode, selected handle and draft values live in one small data class
    with a `Saver`, kept with `rememberSaveable`. Saved in zone-relative cm; any clamping is
    recomputed after rotation. Canvas state, not a Nav3 destination: the canvas must stay visible
    under the ghost, and Nav3 keys must be registered.
  - **Later, not 3b:** "add a handle" to create new vertices for custom shapes (each new vertex
    is also a possible anchor; dragging works the same way as corner handles); an anchor arrow/direction
    indicator; undo (known_issues).

  Exit criteria:
  - Unit tests on `resize()`: each of the four anchors, the anchor corner unchanged to a tiny
    epsilon (not `==`: the opposite corner's x + w - newW can round), 3a's top-left case.
  - A test asserting drag resize and 3a's typed entry produce the identical result for the same
    effective size, numerically equal in Double cm (both go through `resize()`).
  - Children stay put: after a non-top-left resize, each direct child's absolute position is
    unchanged (in-memory DB, one transaction).
  - State-restore test: rotate with the edit session open (selected handle, draft) and it's kept.
  - Testing on a real device starts here, to understand debugging and the real feel (handle
    sizes, touch slop, M6 on a real device).

  **3b real-device checklist** (code done; first time on a physical phone). Record findings here
  or in known_issues; constant changes go in `ui/ResizeHandles.kt`.
  - Setup: USB debugging on, app installed and launched from Android Studio, logcat filtered
    by tag (`AreaCanvasScreen`, `GrowZoneViewModel`) to follow taps, drags and saves.
  - Handle hit size: `HANDLE_HIT_RADIUS` 24 dp (48 dp target). Try 28-32 dp (56-64) if handles
    are hard to grab. Visual size (`HANDLE_RADIUS`, `SELECTED_HANDLE_RADIUS`) and `HANDLE_GAP`.
  - Drag feel: no jump on touch-down, whole-cm steps not jittery, clamp at 1 cm (no flip),
    a tap without movement still selects a handle (touch slop).
  - M6 on the phone: panning and pinching never select; panning the zone body in edit mode pans.
  - Tap off a handle clears the selection (known_issues): annoying on a real finger or fine?
  - Edit panel in portrait: does it cover bottom handles while dragging? If it blocks real use,
    pull "collapsible edit panel" forward from the polish list.
  - Size readout and "Zoom in to resize" hint readable; readout off screen near the top edge
    (polish list) annoying in practice?
  - Save after drag on a nested zone: zone lands where the ghost was, children stay put.
  - Emulator oddities from known_issues (phantom pinch, stylus popup) reproduce on the phone or not.

- [ ] **Phase 3c — Move**
  Still no collision enforcement. Pulled forward from old Phase 5 scope.
  - **Move button** in the action bar next to Enter and Edit size. It opens an inline panel with
    Done and Cancel and a ghost; nothing applies until Done. (Replaces the earlier long-press +
    drag plan.)
  - **No `position_locked` on GrowZone** (never added to the schema, so docs only). PlacedItem
    keeps its own `position_locked`; its meaning is decided in Phase 4.
  - **Move handle on the ghost.** Dragging it moves the zone by the finger displacement. Panning
    always pans. Default handle point = centre of the visible part of the zone, clamped to
    (visible screen area intersect zone) inset by a finger margin (named constant, start about
    48 dp converted to cm at the current scale). The handle follows the pan. If the zone is
    entirely off screen, hide the handle and show a "recentre on zone" button.
  - **"Move here"** button in the panel: pan freely, press it, and the ghost jumps so the handle's
    point lands at the screen centre. Open question for device testing: which reference point to
    use for large zones.
  - **Children travel with the parent.** Children are stored relative to the parent, so no child
    rows change on move.
  - **Move math is a pure function** (finger delta at the current scale -> new position; handle
    clamp), extracted and unit-tested like `CanvasTransform.kt`.
  - **Deferred, possible additions (known_issues):** X/Y fields in the move panel (no obvious
    anchor); long-press to copy coordinates; snapping (drag only, never typing; if added, a toggle
    next to the drag controls, with the drag result passing through one pure function so snapping
    is a one-place change); automatic zoom-out-then-in framing.

  Exit criterion: unit tests on the move math (finger delta at a given scale -> new
  coordinates, handle clamp to the visible part of the zone, off-screen zone -> no handle),
  moving a parent leaves its child rows unchanged, and edit panel resizing still works — same
  extract and test treatment `CanvasTransform.kt` got in Phase 1, since move math is exactly the
  "complex/error prone" category CLAUDE.md calls out for extraction.

- [ ] **Phase 3d — Collision enforcement, visual feedback, and the`bounds_enforced` toggle**
  Replaces the placeholder auto layout.
  - Toggle on, nested zone: child bounds must stay fully inside the
    parent. Partial overlap is not allowed.
  - Toggle on, top-level zone: siblings in the same Area must not overlap.
  - Toggle off: freeform placement, overlap always allowed.
  - Applies to create, resize (3a/3b) and move (3c).
  - PlacedItems (pots/holes) are not part of this phase.
  - Collision check is a pure function in model/ on Double cm, with a
    named COLLISION_DETECTED validation message.
  - Visual feedback while dragging: show when a move or resize would
    violate the bounds
  - The collision check takes the zone, not an assumed rectangle, and starts on bounding
    boxes. Keep the `shape` field so non-rect shapes can plug in later.
  - Open questions (decide at the start of 3d):
    - New children must never be placed overlapping an existing child (the placeholder
      placement doesn't check siblings).
    - What happens when there's no free room for a new child.
    - Whether the free-spot placement applies when `bounds_enforced` is off.
    Test idea: a pure function over the existing child rects that returns a free spot or none.
  Exit criterion: a GrowZone can be created, resized, moved, and
  nested with real containment enforced when the toggle is on, and
  freeform overlap when it's off — both paths covered by unit tests
  on the collision detection function itself (toggle on + violating
  → rejected/snapped, toggle on + valid → allowed, toggle off →
  always allowed), plus manual on device confirmation of the visual
  feedback, which isn't something a unit test can verify.

## Phase 3e — Canvas scale (grid and zoom range)
The grid is a scale, not a fixed 1 m ruler, and the zoom range is derived from the size cap.
Depends only on 3a. Built after 3b and before 3c (decisions.md 2026-10-20).
- **Adaptive grid.** Step chosen from 1, 2, 5, 10, 20, 50 ... metres: the smallest step whose
  on-screen cell is at least `MIN_CELL_PX` (about 40, tune on device). Never below 1 m.
  Lines anchored at multiples of the step from the origin. Cells per frame stay bounded
  at any zoom.
- **Scale bar.** A small bar in a corner whose length is one grid step, labelled ("1 m",
  "20 m", "1 km"), derived from the same number as the grid so they can't disagree.
- **Zoom range.** `MIN_SCALE` derived so a zone of `MAX_WIDTH_DEPTH_CM` fits the shorter
  viewport side without `fitTransform` clamping. Check on the smallest device profile.
- **Optional, same change:** draw the grid as lines (one per step per axis) instead of one
  `drawRect` per cell, removing the per-cell `Stroke` and boxed `Offset` allocations
  (known_issues).

Out of scope: dot mode and viewport culling for zones (Phase 5), cell sizes below 1 m.

Exit criteria:
- Unit tests on the step-choice function (pure, Double): a cell just under `MIN_CELL_PX`
  picks the next step, at it keeps the current one, never below 1 m.
- Negative-coordinate tests on the grid-anchoring function (and step choice, if it takes
  coordinates): `%` and `toInt()` truncate toward zero, which can misalign lines left of and
  above the origin. Zones and panning can be at negative coordinates (3b), so this must hold.
- A `MAX_WIDTH_DEPTH_CM` zone fits at `MIN_SCALE` without clamping (test with the viewport
  sizes you support).
- Cells drawn per frame never exceed the bound at `MIN_SCALE`.
- Manual: scale bar matches the grid on the emulator at several zoom levels; M5 on the
  stress Area shows no regression.
- Manual: with the new zoom range, zooming out on a zone in Edit size hides its handles and
  shows the "Zoom in to resize" hint below `MIN_ZONE_SIZE_FOR_HANDLES` (built preemptively in
  3b, only reachable there with small zones).

## Phase 3 polish (last, after 3d)
UX adjustments only, no new features or schema changes (roadmap.md, polish step). Collected
while building; decide what's worth it after real-device testing (3b onwards). An item may be
pulled forward into a sub-phase that touches the same code anyway; evaluate it at that time.
- **Handle visuals.** Replace the plain dots with the intended handle (a small grey or white
  square with an arrow pointing outward). Sizes stay the named dp constants in
  `ui/ResizeHandles.kt`.
- **Edge readouts.** A length label along each edge (width on top/bottom, depth on the sides),
  like CAD dimension lines, instead of one readout above the ghost. Also useful later for
  custom shapes, where each edge has its own length.
- **Collapsible edit panel.** Put the panel away and bring it back, so it doesn't cover the
  canvas (e.g. a bottom handle under the portrait panel). If it already blocks dragging on the
  real phone, pull it forward into 3b as a usability fix.
- **Readout stays on screen.** The size readout is drawn above the ghost and goes off screen
  when the ghost's top edge is near the top; clamp it into view (or flip it below the ghost).
