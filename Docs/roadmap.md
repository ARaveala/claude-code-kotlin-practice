# Roadmap

Rule for Claude Code: only build what's in the **current phase**. Do not
implement later phase features early, even if they seem simple, unless
explicitly asked. See domain_model.md for entity/rule definitions this
roadmap refers to.

Each phase should be tested and working before moving to the next,
same "test proves correctness" standard as MariaDB work, not just eyeballed.

- [ ] **Phase 0 — Entry point (skip for now)**
  No login/username. App opens straight to the Area list. Can be added
  later without touching the domain model below it.

- [x] **Phase 1 — Area list + blank canvas**
  List screen: add Area, tap to open. Area opens to an empty, pannable,
  zoomable canvas (draw.io-style blank space).

- [ ] **Phase 2 — GrowZone creation + nesting validation**
  Create zone types (greenhouse, plot, box, wild). Enforce nesting rules
  table. Test each valid/invalid containment case explicitly.

- [ ] **Phase 3 — Resizing**
  Finger-drag resize and manual measurement entry, both producing the
  same scale-to-cm result. Determine real sizing caps here via testing
  (see domain_model.md — Sizing Caps).

- [ ] **Phase 4 — PlacedItem (pot/hole) creation + detail card**
  Color-swatch placeholder (left), fields (right): label, plant name,
  diameter, height, notes. Support items placed directly in an Area
  (unnested) as well as inside a GrowZone.

- [ ] **Phase 4.5 — Images (stretch, not core)**
  Image picker for GrowZone types and PlacedItems, replacing color
  swatches. Only start once Phase 4 works, treat as optional bonus
  scope, not something to fold into Phase 4.

- [ ] **Phase 5 — Nesting interaction + rendering**
  Free placement of items inside zone bounds. Wall collision enforcement
  (toggleable). Tap-to-zoom into GrowZones. Level-of-detail / dot-mode
  rendering at extreme zoom-out.

- [ ] **Phase 6 — Watering icon + timestamp**
  Watering icon appears in zoomed-in pot/hole view. Press, sets
  `watered_last` from device clock.

- [ ] **Phase 7 — Calendar sync (later, separately scoped)**
  Start single-user only: writing "last watered" to a personal calendar
  event/reminder. Multi-person sync + notification batching (avoiding
  per-plant spam) is a v2+ problem.

## Explicitly out of scope for phases
- Multi-user login/auth
- Multi-person calendar notification logic
- Non-rect GrowZone shapes
- Plant-specific seasonal planting suggestions (the "tomato needs
  planting within these months" idea), future concept, not yet designed
