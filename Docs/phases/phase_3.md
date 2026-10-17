- [ ] **Phase 3a — Manual resize + sizing caps**
- **Name box** name box on top left next to back button, so we can see what growzone is selected.
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
   Contents are measurements only: width, depth, height (height nullable, plot/box only).
   The soft warning confirmation (Accept / Cancel) is shown inside the panel, not as a separate window.
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

- [ ] **Phase 3b — Drag resize**
  Finger drag resize. Still no collision enforcement.
  Exit criterion: a test asserting drag resize and 3a's manual entry
  produce the identical scale to cm result for the same effective
  size, not just visually similar, numerically equal. Here testing on real device should begin to simplify and understand debugging aswel as test true user feel.

- [ ] **Phase 3c — Drag move**
  Still no collision enforcement.
  Drag to move a GrowZone (pulled forward from old Phase 5 scope).
  Position_locked (bool, default false).
  Exit criterion: unit tests on the move math itself (position delta
  → new coordinates, position_locked items reject drag move and drag resize, edit panel resizing still works) — same
  extract and test treatment CanvasTransform.kt got in Phase 1,
  since move math is exactly the "complex/error prone" category
  CLAUDE.md calls out for extraction.

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
  Exit criterion: a GrowZone can be created, resized, moved, and
  nested with real containment enforced when the toggle is on, and
  freeform overlap when it's off — both paths covered by unit tests
  on the collision detection function itself (toggle on + violating
  → rejected/snapped, toggle on + valid → allowed, toggle off →
  always allowed), plus manual on device confirmation of the visual
  feedback, which isn't something a unit test can verify.

## Phase 3e — Canvas scale (grid and zoom range)
The grid is a scale, not a fixed 1 m ruler, and the zoom range is derived from the size cap.
Independent of 3a-3d; build it after 3a, or earlier if device testing shows grid cost or
clipped framing of very large zones.
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
- A `MAX_WIDTH_DEPTH_CM` zone fits at `MIN_SCALE` without clamping (test with the viewport
  sizes you support).
- Cells drawn per frame never exceed the bound at `MIN_SCALE`.
- Manual: scale bar matches the grid on the emulator at several zoom levels; M5 on the
  stress Area shows no regression.