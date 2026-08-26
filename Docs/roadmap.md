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

- [x] **Phase 2 — GrowZone creation + nesting validation**
  Create zone types (greenhouse, plot, box, wild). Nesting validation
  started as a strict container type table, but that was simplified
  during this phase — see domain_model.md's Nesting Rules, down to a
  flat nesting depth cap, tested explicitly at each boundary. Tap to zoom
  into a GrowZone (originally scoped to Phase 5) landed early here
  instead, alongside the nesting mechanics.

- [ ] **Phase 3 — GrowZone completeness (resize, move, containment)**
  Finger drag resize and manual measurement entry, both producing the
  same scale to cm result. Determine real sizing caps here via testing
  (see domain_model.md — Sizing Caps). Also pulled forward from the old
  Phase 5 scope: drag to move a GrowZone, and actually enforcing
  `bounds_enforced` for GrowZone in GrowZone containment (currently a
  placeholder auto layout only, per known_issues.md). Exit criterion: a
  GrowZone can be created, resized, moved, and nested with real
  containment — no PlacedItem work starts until this is solid.

- [ ] **Phase 4 — PlacedItem (pot/hole) creation + detail card**
  Color-swatch placeholder (left), fields (right): label, plant name,
  diameter, height, notes. Support items placed directly in an Area
  (unnested) as well as inside a GrowZone. Free drag placement, pots
  and holes may be exempt from wall collision (domain_model.md), so
  this needs no containment math, unlike Phase 3's GrowZone placement.

- [ ] **Phase 4.5 — Images (stretch, not core)**
  Image picker for GrowZone types and PlacedItems, replacing color
  swatches. Only start once Phase 4 works, treat as optional bonus
  scope, not something to fold into Phase 4.

- [ ] **Phase 5 — Rendering polish**
  Level of detail / dot mode rendering at extreme zoom out, for both
  GrowZones and PlacedItems. Deliberately small after the Phase 3/4
  restructure — canvas pan/zoom state persisting per Area instead of
  resetting on navigation (known_issues.md, currently tagged Phase 5)
  is a natural candidate to fold in here too, decide when this phase
  starts rather than committing to it now.

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
