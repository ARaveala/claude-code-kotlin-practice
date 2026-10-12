- [ ] **Phase 3a — Manual resize + sizing caps**
  Manual measurement entry only (no gestures yet). No collision
  enforcement yet (overlap is silently allowed for now).
  Button shown while zone focused, where minimal adjustment settings live, eg edit measurements.
  Cm anchor applied here
  Exit criterion: sizing caps determined and unit tested at each
  boundary (just under cap accepted, at cap accepted, over cap
  rejected), same boundary testing standard as Phase 2's nesting
  depth cap. Max cap is a safety measure. warnings should be added to absurd sizes as to not restrict user . Testing on other emulators here would be wise to prepare for physical phone testing.  

- [ ] **Phase 3b — Drag resize**
  Finger drag resize. Still no collision enforcement.
  Exit criterion: a test asserting drag resize and 3a's manual entry
  produce the identical scale to cm result for the same effective
  size, not just visually similar, numerically equal. Here testing on real device should begin to simplify and understand debugging aswel as test true user feel.

- [ ] **Phase 3c — Drag move**
  Still no collision enforcement.
  Drag to move a GrowZone (pulled forward from old Phase 5 scope).
  Position_locked (bool, default false).
  Position_locked bool behaviour , how to toggle for quick resizing and move,
  without bothering panning through area (maybe click zone, options button appears for zone?)
  Exit criterion: unit tests on the move math itself (position delta
  → new coordinates, position_locked items reject move) — same
  extract and test treatment CanvasTransform.kt got in Phase 1,
  since move math is exactly the "complex/error prone" category
  CLAUDE.md calls out for extraction.

- [ ] **Phase 3d — Collision enforcement, visual feedback, and the
  `bounds_enforced` toggle**
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
    violate the bounds (details in the open questions below).
  Exit criterion: a GrowZone can be created, resized, moved, and
  nested with real containment enforced when the toggle is on, and
  freeform overlap when it's off — both paths covered by unit tests
  on the collision detection function itself (toggle on + violating
  → rejected/snapped, toggle on + valid → allowed, toggle off →
  always allowed), plus manual on device confirmation of the visual
  feedback, which isn't something a unit test can verify.
