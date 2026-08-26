# Known Issues

Build/tooling gotchas, as they come up.

- **Android Studio emulator synthetic multi touch / stylus popup tool seems to 
  be able to inject spurious touch events.** Observed while testing
  `AreaCanvasScreen`'s pinch to zoom (Phase 1): a swipe occasionally
  produced a jittery/phantom pointer zoom jump, and the emulator's "try
  your stylus" popup interfered with dialog taps. Confirmed via logcat
  that the app's own gesture math (`updateTransform` in
  `CanvasTransform.kt`) reports `zoom=1.0` correctly for genuine
  single finger pans, the anomaly is emulator input simulation, not
  app code. Not yet tested on a real device

# Potential Concerns

Design/architecture points worth remembering for future phases, not
bugs, nothing needs fixing now.

- **All app state currently lives in `MainActivity`'s `onCreate`
  composition**, with a raw `if/else` screen router
  (`MainActivity.kt`). Expect this to move into the planned
  `viewmodel/` layer as more screens are added.
  — Phase 2+

- **Grid spacing (`GRID_SPACING` in `AreaCanvasScreen.kt`) has no
  real world cm anchor yet.** Phase 3's scale to cm resize work will
  need to define one; the grid should likely switch to being cm based
  at that point so it functions as an actual ruler, not just a zoom
  indicator.
  — Phase 3

- **Canvas pan/zoom (`CanvasTransform`) resets on navigating away and
  back.** Currently unavoidable, it's local `remember` state. Worth
  deciding later on whether it should persist per Area or always fresh 
  start from the same location.
  — Phase 5

- **The raw `Canvas` has no accessibility semantics.** Matters once
  GrowZones/PlacedItems become real interactive content on it, a bare
  `Canvas` exposes nothing to TalkBack/assistive tech by default. Decide:
  whether we can build for accessability alongside each phase or treat it
  as new phase later down the line. Also worth checking if the same semantic
  layer could double as non accessability feature. 
  — Phase 4+

- **Panning is currently unbounded** (no clamp to content extents).
  Correct for now for empty canvas, nothing to bound around yet. Worth
  revisiting once there's placed content (clamp like Miro/draw.io do).
  — Phase 3

- **naming conventions**
  If Area names or type names are the same, this may affect user experince and
  any search features we may add. Decide on naming convention rules.

- **Android's Auto Backup for Apps may back up the Room database to the user's Google account by default on a real device**
  (emulator testing
  doesn't reliably exercise this path). Not a concern with placeholder
  data, but once real garden/plant data exists this is a privacy/design
  decision to make deliberately (`android:allowBackup` / backup rules),
  not something to silently inherit from the default.
  — Phase 4+

- **How a future "list what's inside this zone" feature should work is undecided.**
  Now that visual overlap doesn't require formal nesting
  (see domain_model.md's Wall Collision, a Box can visually sit on a
  plot via `bounds_enforced = false` without being its child), a listing
  built purely off `parent_grow_zone_id` would miss zones/items that
  look contained on screen but aren't formally nested. Whether a listing
  should show only formal children, compute visual/geometric containment
  instead, or show both, isn't decided, no listing feature is scoped
  yet, so nothing to build now, just don't assume `parent_grow_zone_id`
  alone answers "what's in this zone" once that's designed.
  — Phase 4+

- **`GardenDatabase` uses the OS-backed `AndroidSQLiteDriver`, not `BundledSQLiteDriver`**
  (chosen in Phase 2 for a smaller APK). This
  means the actual SQLite engine version is whatever ships with each
  device's Android build, not one we control — Room targets a
  conservative common feature set so this is low risk, but it's the one
  place "works on the emulator" doesn't strictly guarantee "works
  identically on every real device," worth remembering if a real device
  bug ever doesn't reproduce on the emulator.

# From practice to release

- **Release optimization off during early phases**
  app/build.gradle.kts build types, R8/ProGuard disabled, consider enable later.


# Linter + andoird studio remarks

- **Duplicate dependencies**
  - app/build.gradle.kts
    Dependency 'platform(libs.androidx.compose.bom)' is declared multiple times.
    False positive, seperate class paths.
  - unused resources, colours, acceptable for experimenting

