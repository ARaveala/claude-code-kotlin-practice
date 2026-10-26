# Manual checks

Things verified by hand because automating them costs more than it protects.
Run the relevant checks when a PR touches the listed area, and all of them before a release.

### M1 — Screen ViewModels are cleared on back
**Run when:** touching NavDisplay, entryDecorators, or where a ViewModel is created.
**Steps:** dev emulator, Logcat filter `tag:GrowZoneViewModel`. Open area A → back →
open area B → back.
**Expected:** each back press logs `cleared for areaId=…`.
**Deeper check:** Profiler → Heap Dump (after the steps above) → filter
`GrowZoneViewModel` → count is 0.

### M2 — Dialogs and the edit panel survive a real rotation
**Run when:** touching dialog or panel state, or `rememberSaveable` usage.
**Why manual:** StateRestorationTester can give a false pass for dialogs (see known_issues).
**Steps:**
1. Add-area dialog: type a name, rotate the emulator.
2. Add-GrowZone dialog on the canvas: pick a non-default type, type a name, width, depth
   and height, rotate.
3. Edit-size panel (3a): select a zone, open Edit size, type a width and depth, rotate.
   Repeat with a value above the soft warning threshold so the confirmation is showing.
**Expected:** each dialog or panel is still open with its typed text and chosen type; the
edit panel still targets the same zone and the ghost preview is still drawn; the selection
is kept. Canvas zoom and zone focus reset on rotation (decisions.md, provisional).
This can be removed once false pass is figured out

### M3 — Restore after process death
**Steps:** open a canvas → press Home (app must be in background) →
`adb shell am kill com.practice.plantUser` → confirm dead with
`adb shell pidof com.practice.plantUser` (no output) → open the emulator's
recent-apps screen (square button, or swipe up and hold) → tap the app card.
**Expected:** returns to the same canvas (a brief blank is a known issue).
**Don't:** use Android Studio Stop/Run, which force-stops and discards saved state.

### M4 — Older device
**Run when:** before a release, or after changing sizes or layout.
**Steps:** run the app on a minSdk emulator profile (AVD `minsdk24_T1`: Small Phone, API 24).
**Expected:** list, canvas, and dialogs usable; grid and zones render correctly.
**Last run:** 2026-10-20, passed on the API 24 Small Phone emulator (add-GrowZone dialog
landscape clipping found and fixed the same day, progression_log.md). The emulator is hardware
accelerated, so this says nothing about real old-phone frame timing (that is M5).

### M5 — Allocation profile while panning the canvas
**Run when:** changing canvas drawing, gestures, or anything in the draw loop.
**Steps:** Profiler → record Java/Kotlin allocations → pan for ~5 s → sort by count.
**Expected:** no per-frame allocation of objects that could be created once
(compare counts to the previous recording noted here).
**Last recorded (2026-10-20, 200 zones, Profiler on, median of 3, per frame):** Stroke 0
(was ~259 before the grid Stroke hoist), Offset ~264 (grid cell list, open until 3e).
Details: `canvas_benchmark/how_to.md`.

### M6 — Panning and pinching don't select
**Run when:** touching canvas gestures, hit-testing, or the action bar.
**Why manual:** emulator touch injection is jittery (see known_issues); use a real device
once one is available.
**Status:** passes on the emulator (2026-10-20, Phase 3a); real device not tested yet.
**Steps:** start a one-finger pan with the finger on top of a zone; start a pinch with a
finger on a zone; tap a zone; tap empty canvas; with a zone selected, pan and zoom.
**Expected:** pan and pinch never select a zone or show the bar; a tap selects and shows
the bar; a tap on empty canvas clears it; selection and bar survive pan and zoom; the bar
doesn't block canvas gestures outside its own bounds.