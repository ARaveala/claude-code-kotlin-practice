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
- width_cm, depth_cm, height_cm (nullable, any type: the user decides how to use it)
- x_cm, y_cm: position relative to the parent zone, or to the Area when top level. May be
  negative; finite values only (decisions.md 2026-10-20)
- shape (rect to start; extend later). Collision (3d) takes the zone, not an assumed rectangle
- bounds_enforced (bool, default true) — see Wall Collision rules below
- No position_locked: GrowZones are moved only through the explicit Move panel (Phase 3c,
  decisions.md 2026-10-20)

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
- position_locked (bool) — meaning decided in Phase 4
- watered_last (nullable timestamp, set from device clock on watering-icon press)

Note: exactly one of `grow_zone_id` / `area_id` should be set per PlacedItem, never both.

## Nesting Rules

**Simplified during Phase 2** — this section originally had a strict container type table (e.g.
only a Box could nest inside a Plot; Wild could contain nothing but a Hole). This fought real
use cases, surfaced while building GrowZone nesting: a small Greenhouse representing an indoor
microclimate sitting on a Plot, a Box overlapping a Plot boundary onto open ground, and 
longer term user defined zone types, a fixed table can't account for. Simplified to:

- Any GrowZone type may nest inside any other GrowZone type. `type` is a label (drives color and
  rendering now, growing condition semantics later), not a containment taxonomy to enforce.
- A flat nesting depth cap remains, currently 3 levels total, a top level zone counting as depth
  1 as a sanity limit on tree depth, not a type specific rule (the old "Greenhouse max depth 2"
  wording is gone; the cap now applies uniformly regardless of type).
- Whether a nested zone actually fits inside its parent isn't validated at creation time anymore,
  that's real containment geometry, which is Phase 3's job via `bounds_enforced` (see Wall
  Collision below), not something a nesting type rule should be doing.

This is a deliberate simplification, not a final answer, worth revisiting once growing condition
inheritance or user defined zone types are actually designed, if a real need for stricter
containment shows up then.

## Wall Collision

- GrowZone within GrowZone placement enforces containment by default (`bounds_enforced = true`): child bounds must stay fully within parent bounds, no partial overlap.
- Can be toggled off per-placement if the user wants freeform/overlapping layout.
- Pots and Holes may be exempt from wall collision — free placement, no containment enforcement.
- Top-level GrowZones (direct children of an Area, no parent GrowZone) use the same
  `bounds_enforced` flag to govern overlap with sibling GrowZones in that Area — `true`
  (default) keeps siblings apart, `false` allows freeform/overlapping placement. Same flag,
  same meaning, just no parent to be contained by at that level.
- GrowZones have no `position_locked` (dropped 2026-10-20): a zone only moves through the Move
  panel, so accidental moves are already prevented. PlacedItem's `position_locked` stays, a
  separate axis from `bounds_enforced`; decided in Phase 4.
- This is also how "visually overlapping, but not sharing conditions" is meant to work: a Box
  that visually sits on top of a Plot without being managed by it is just a top-level Box
  (`parent_grow_zone_id = null`) with `bounds_enforced = false`, positioned so it happens to
  overlap the Plot's rectangle — not nested at all. Nesting is reserved for when the
  relationship is intentional (shared conditions, hierarchy, drill-down).

## Sizing Caps

- Dimension caps exist to prevent overflow/rendering issues at extreme zoom and to catch fat-finger entry (e.g. a 5000cm box).
- Exact cap values for GrowZones are in app/src/main/java/com/practice/plantUser/model/SizingLimits.kt
- Caps are a sanity ceiling, not a physical/weight simulation. Weight-based limits (e.g. "70kg full") are informational only, not a validation gate — soil density varies too much to enforce as a hard rule.
- The grid spacing will use adaptive scale, min 1m max for now 1000m, to lessen draw amount on zoom out.

## Rendering Rules (Level of Detail)

- Below a minimum on-screen pixel size, an item (GrowZone or PlacedItem, nested or not) renders as a fixed-size marker (dot, later: type-colored icon) instead of continuing to shrink with zoom.
- Marker size stays constant regardless of further zoom-out — this is what tells the user "something is here, but not to scale."
- Crossing back above the size threshold on zoom-in restores true-to-scale rendering (or the image, once Phase 4.5 lands).
- This rule applies uniformly — no special-casing for unnested vs. nested items.

## Interaction Notes
- Tap GrowZone: selects it (outline highlight, inline action bar with Enter,
  Edit size and, from 3c, Move), aswell as a name box for GrowZone name top left near back button.
  The camera does not move.Tapping empty canvas clears selection.
- Enter (bar button): fast zoom in, framed so the zone's edges are fully
  visible. Selection survives pan/zoom. The focused zone ignores taps; only its
  children and empty space respond; smallest zone under the finger wins.
- Move (3c): the action bar's Move button opens an inline panel (Done / Cancel) with a ghost
  and a move handle; dragging the handle moves the zone, panning always pans, nothing applies
  until Done. Children travel with the parent (stored relative to it).
- From inside a GrowZone, further zoom is possible (nested content, or approaching the dot-mode threshold).
- Tap PlacedItem (pot/hole) → does NOT zoom the canvas. Opens a detail card/sheet instead (left: color swatch or image, right: fields).
- Resize GrowZone either by finger-drag or by entering exact measurements — both go through one
  `resize()` function and resolve to the same Double cm result.
- Edit size: inline non-modal panel, measurements only (width, depth, height), live ghost
  preview with a size readout. From 3b, corner handles show while the panel is open; the
  grabbed or tapped handle's opposite corner is the anchor (default top-left). Children stay
  put on screen when the parent's origin moves.
