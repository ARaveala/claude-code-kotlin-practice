# Known Issues

Build/tooling gotchas, as they come up.

- **Android Studio emulator synthetic multi touch / stylus popup tool seems to  be able to inject spurious touch events.** Observed while testing
  `AreaCanvasScreen`'s pinch to zoom (Phase 1): a swipe occasionally
  produced a jittery/phantom pointer zoom jump, and the emulator's "try
  your stylus" popup interfered with dialog taps. Confirmed via logcat
  that the app's own gesture math (`updateTransform` in
  `CanvasTransform.kt`) reports `zoom=1.0` correctly for genuine
  single finger pans, the anomaly is emulator input simulation, not
  app code. Not yet tested on a real device

- **`connectedAndroidTest` uninstalls the app afterwards**, wiping the target emulator's
  data. Use the managed device, or a dedicated test AVD.
- **`sun.misc.Unsafe` warnings during Gradle runs** come from protobuf inside the build
  tooling (Gradle on JDK 25). Not app code, not fixable locally; goes away when AGP
  updates protobuf.

- **Canvas allocates per frame while panning: grid cell Offsets.** ~260 `Offset`/frame at the
  benchmark zoom (boxed in the List returned by `visibleGridCellOrigins`, one per cell),
  regardless of zone count. Fix: iterate cells without a boxed list (Phase 3e grid as lines).
  Also not done: every zone is drawn even when off screen (viewport culling / dot mode, Phase 5).
  Verify with `Docs/manual_checks/canvas_benchmark/`. Fixed parts (zone loop, grid `Stroke`):
  progression_log.md. — Phase 3e / Phase 5

- Add dependency in android studio quick fix, silently edited libs.versions.toml, causing build issues.

# Potential Concerns

Design/architecture points worth remembering for future phases, not
bugs, nothing needs fixing now.

- **App assembly lives in the root package (MainActivity + PlantUserApp).** The if/else
  router is gone (Nav3). As more DAOs/ViewModels appear, consider a small AppContainer
  created once in an Application subclass instead of building dependencies inline.
  — when a third DAO arrives

- **Canvas pan/zoom (`CanvasTransform`) resets on navigating away and back.**
  Currently unavoidable, it's local `remember` state. Worth
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
  — Phase 3d/phase3c

- **Quick focus from the name box breadcrumb.** 3a's breadcrumb only selects a zone.
  Tapping a breadcrumb entry could also focus (enter) that zone directly, a quick way back up
  the chain. Decide the gesture (tap selected entry again, or long-press) once 3a is in use.
  — after Phase 3a

- **Size draft state is duplicated between the edit panel and the create dialog.** Each
  holds three texts + a warning value in `rememberSaveable`, each field's change lambda
  clears the warning, and the "show soft warning or confirm" branch is written twice
  (`saveEdit` and the dialog's Create button). Decided for the edit panel: the 3b edit session
  is one saveable class (decisions.md 2026-10-20). Still open: whether the create dialog reuses
  its draft part, or keeps its own.
  — Phase 3b

- **The grid recomposes on every pan frame.** `transform` is read during composition (in
  `AreaCanvasScreen`'s `BoxWithConstraints` content), so each pan frame recomposes
  `PannableZoomableGrid` instead of only redrawing. Fix: defer the read to the draw phase
  (pass `() -> CanvasTransform` or the `State`). Check recomposition counts in Layout
  Inspector first; compare with the Profiler-off baseline in canvas_benchmark/how_to.md.
  — Phase 3e

- **Snapping (deferred).** Drag only, never typed values; a toggle next to the drag controls;
  the drag result passes through one pure function so it's a one-place change. Needs 3e's grid
  and real-device feel first (decisions.md 2026-10-20).
  — after 3e

- **X/Y fields in the Move panel (deferred).** No obvious anchor yet: which point of the zone,
  relative to what. Related idea: long-press to copy coordinates.
  — after 3c

- **Resize anchor arrow / direction indicator.** 3b marks the anchor with a dot only. An arrow
  showing which way the zone grows may help; decide after real-device use.
  — after 3b

- **Undo for resize/move.** Leave room for an undo button in the panels. Likely more useful once
  custom shapes exist (many vertex edits).
  — Phase 3.5+ / custom shapes

- **Custom shapes need a different size readout.** 3b's readout is width x depth; an octagon or
  half circle has no meaningful width/depth. Also later: an "add a handle" action that creates
  a new vertex, for custom shape creation. Each new vertex is a possible anchor; dragging works
  as planned for corner handles. 3b's "corner handles only" tier for small zones is meant to hide
  these extra handles.
  — custom shapes phase

- **Negative positions are allowed (3b).** Review the policy once real layouts exist: whether
  zones far left of/above the origin cause framing, panning or grid issues. Related idea: Area
  borders so users can easily stay inside, probably with panning still allowed outside them.
  Either way, negatives must never crash.
  — Phase 5

- **New-child placement can overlap existing children.** The placeholder placement doesn't check
  siblings. Open: never overlap an existing child; what happens when there's no room; whether the
  free-spot search applies when `bounds_enforced` is off. Test idea: a pure function over the
  existing child rects.
  — Phase 3d

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

- **Cross-platform (KMP) is a possible future direction, not planned.**
  Baseline audit of Android-only code: MainActivity (platform shim, expected),
  GardenDatabase (`Context`; database construction would split per platform),
  Theme.kt (template dynamic colour: a design decision, not just a port),
  GrowZoneViewModel (`android.util.Log`: needs a logger wrapper),
  Habit: keep `android.*` and `Context` out of shared logic, and pass platform
  objects in from MainActivity. Model/ is already plain Kotlin.

- **`StateRestorationTester` is an emulation of rotation.** It rebuilds the composition
  in the same Activity. Dialog windows can linger briefly and give a false pass. The
  exact equivalent is `ActivityScenario.recreate()`, which needs an injectable database
  to stay isolated.


- **Process-death gap on the canvas.** Urgent once Area deletion comes to play. 
  After process death the back stack restores immediately but `areas` starts empty until Room emits, so the canvas entry briefly renders nothing. Decide: placeholder vs pop back if the Area no longer exists (loading, loaded).
  Reproduce it with: open a canvas, press Home, run adb shell am kill com.practice.plantUser, then reopen the app from recents.


# From practice to release

- **Release optimization off during early phases**
  app/build.gradle.kts build types, R8/ProGuard disabled, consider enable later.


# Linter + android studio remarks

- **Duplicate dependencies**
  - app/build.gradle.kts
    Dependency 'platform(libs.androidx.compose.bom)' is declared multiple times.
    False positive, seperate class paths.
  - unused resources, colours, acceptable for experimenting
  - "Newer version available" checks set to informational (see decisions.md).

