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

- **Phone rotate whilst in area causes kick back to list of areas**
Potentially, remember { mutableStateOf<Area?>(null) } uses remember, not rememberSaveable.
MainActivity.kt , onCreate. 

- **`connectedAndroidTest` uninstalls the app afterwards**, wiping the target emulator's
  data. Use the managed device, or a dedicated test AVD.
- **`sun.misc.Unsafe` warnings during Gradle runs** come from protobuf inside the build
  tooling (Gradle on JDK 25). Not app code, not fixable locally; goes away when AGP
  updates protobuf.

- **Canvas allocates per frame while panning.** ~5 s of panning: ~25.8k `Stroke`
  (new Stroke per drawRect in grid and zone loops) and ~26.5k `Offset` (likely boxed
  in the List returned by visibleGridCellOrigins). Short-lived, but GC churn → stutter
  risk on older phones. Fix: hoist Stroke out of the loops (remember), iterate grid
  cells without a boxed list. Verify with a before/after allocation recording.
  — Phase 5

# Potential Concerns

Design/architecture points worth remembering for future phases, not
bugs, nothing needs fixing now.

- **App assembly lives in the root package (MainActivity + PlantUserApp).** The if/else
  router is gone (Nav3). As more DAOs/ViewModels appear, consider a small AppContainer
  created once in an Application subclass instead of building dependencies inline.
  — when a third DAO arrives

- **Grid spacing (`GRID_SPACING` in `AreaCanvasScreen.kt`) has no real world cm anchor yet.** 
  Phase 3's scale to cm resize work will
  need to define one; the grid should likely switch to being cm based
  at that point so it functions as an actual ruler, not just a zoom
  indicator.
  — Phase 3

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

- **Cross-platform (KMP) is a possible future direction, not planned.**
  Baseline audit of Android-only code: MainActivity (platform shim, expected),
  GardenDatabase (`Context`; database construction would split per platform),
  Theme.kt (template dynamic colour: a design decision, not just a port),
  GrowZoneViewModel (`android.util.Log`: needs a logger wrapper),
  AreaCanvasScreen (`LocalContext` to build its ViewModel; removed in Step D).
  Habit: keep `android.*` and `Context` out of shared logic, and pass platform
  objects in from MainActivity. Model/ is already plain Kotlin.

- **`GrowZoneViewModel` instances accumulate for the Activity's lifetime.**
  `AreaCanvasScreen` creates them with `viewModel(key = "growZones-${area.id}")`,
  which stores them in the Activity's ViewModelStore. Leaving the canvas never
  clears them, so each visited Area keeps a ViewModel (and possibly its Room
  observation) alive until the app closes. Correct per area, but grows with Areas
  visited. Bounded by the 100 area cap, so not urgent. Fix: Nav3 per-entry
  ViewModel decorator (`lifecycle-viewmodel-navigation3`) + hoist construction
  out of the screen.
- NOW

- **Zone focus is lost on rotation.** `focusedZoneId` and `transform` are plain
  `remember`. They must be saved together or the UI shows "inside a zone" at default
  zoom. Likely approach: save `focusedZoneId`, recompute the zoom with `fitTransform`
  once the canvas is measured (doesn't decide the Phase 5 "persist pan/zoom" question).
  Need to scope viewmodels to each entry, so a test can pass in fake zones.


- **`StateRestorationTester` is an emulation of rotation.** It rebuilds the composition
  in the same Activity. Dialog windows can linger briefly and give a false pass. The
  exact equivalent is `ActivityScenario.recreate()`, which needs an injectable database
  to stay isolated.


- **Process-death gap on the canvas.** Urgent once Area deletion comes to play. 
  After process death the back stack restores immediately but `areas` starts empty until Room emits, so the canvas entry briefly renders nothing. Decide: placeholder vs pop back if the Area no longer exists (loading, loaded).
  Reproduce it with: open a canvas, press Home, run adb shell am kill com.practice.plant_user, then reopen the app from recents.


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

