- [ ] **Phase 3a — Manual resize + sizing caps**
  Manual measurement entry only (no gestures yet). No collision
  enforcement yet (overlap is silently allowed for now).
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
  Drag to move a GrowZone (pulled forward from old Phase 5 scope).
  Still no collision enforcement.
  Exit criterion: unit tests on the move math itself (position delta
  → new coordinates, locked items reject move) — same
  extract and test treatment CanvasTransform.kt got in Phase 1,
  since move math is exactly the "complex/error prone" category
  CLAUDE.md calls out for extraction.

- [ ] **Phase 3d — Collision enforcement, visual feedback, and the
  `bounds_enforced` toggle**
  [...same content as before...]
  Exit criterion: a GrowZone can be created, resized, moved, and
  nested with real containment enforced when the toggle is on, and
  freeform overlap when it's off — both paths covered by unit tests
  on the collision detection function itself (toggle on + violating
  → rejected/snapped, toggle on + valid → allowed, toggle off →
  always allowed), plus manual on device confirmation of the visual
  feedback, which isn't something a unit test can verify.

