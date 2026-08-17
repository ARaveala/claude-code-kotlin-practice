# Domain Model

## Entities

**Area**
- id, name
- Contains one or more GrowZones
- Cannot be nested inside another Area

**GrowZone**
- id, area_id, parent_grow_zone_id (nullable — set when nested inside another GrowZone)
- type: greenhouse | plot | box | wild
- name
- width_cm, depth_cm, height_cm (nullable — plot/box only)
- shape (rect to start; extend later)
- bounds_enforced (bool, default true) — see Wall Collision rules below

**PlacedItem** (unifies pot and hole — same entity, `kind` field distinguishes)
- id, grow_zone_id (nullable — an item can exist directly in an Area, unnested)
- area_id (nullable — set when item is NOT inside any GrowZone)
- kind: pot | hole
- label, plant_latin_name (nullable)
- diameter_cm, height_cm
- x, y (free-placement position, scaled to real-world cm — not grid-locked)
- image (nullable, local file path) — Phase 4.5
- color (placeholder visual before images — Phase 4)
- notes (nullable)
- position_locked (bool)
- watered_last (nullable timestamp, set from device clock on watering-icon press)

Note: exactly one of `grow_zone_id` / `area_id` should be set per PlacedItem, never both.

## Nesting Rules

| Container   | Can contain                              |
|-------------|-------------------------------------------|
| Area        | GrowZones only (never another Area)       |
| Greenhouse  | Smaller Greenhouse (max depth 2, no further nesting), Box, Pot, Hole |
| Box         | Pot, Hole                                  |
| Plot        | Box, Pot, Hole                             |
| Wild        | Hole only                                  |

A nested Greenhouse must be strictly smaller (width_cm and depth_cm) than its parent. Validate this before accepting placement, not after.

## Wall Collision

- GrowZone-within-GrowZone placement enforces containment by default (`bounds_enforced = true`): child bounds must stay fully within parent bounds, no partial overlap.
- Can be toggled off per-placement if the user wants freeform/overlapping layout.
- Pots and Holes are always exempt from wall collision — free placement, no containment enforcement.

## Sizing Caps

- Dimension caps exist to prevent overflow/rendering issues at extreme zoom and to catch fat-finger entry (e.g. a 5000cm box).
- Exact cap values are TBD — determine via testing in Phase 3, not hardcoded assumptions yet.
- Caps are a sanity ceiling, not a physical/weight simulation. Weight-based limits (e.g. "70kg full") are informational only, not a validation gate — soil density varies too much to enforce as a hard rule.

## Rendering Rules (Level of Detail)

- Below a minimum on-screen pixel size, an item (GrowZone or PlacedItem, nested or not) renders as a fixed-size marker (dot, later: type-colored icon) instead of continuing to shrink with zoom.
- Marker size stays constant regardless of further zoom-out — this is what tells the user "something is here, but not to scale."
- Crossing back above the size threshold on zoom-in restores true-to-scale rendering (or the image, once Phase 4.5 lands).
- This rule applies uniformly — no special-casing for unnested vs. nested items.

## Interaction Notes

- Move: long-press + drag. Locked items (`position_locked = true`) reject move.
- Tap GrowZone → fast zoom in, framed so the zone's edges are fully visible on screen.
- From inside a GrowZone, further zoom is possible (nested content, or approaching the dot-mode threshold).
- Tap PlacedItem (pot/hole) → does NOT zoom the canvas. Opens a detail card/sheet instead (left: color swatch or image, right: fields).
- Resize GrowZone either by finger-drag or by entering exact measurements — both must resolve to the same scale-to-cm result.
