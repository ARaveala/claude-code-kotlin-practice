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

### M2 — Dialogs survive a real rotation
**Run when:** touching dialog state or rememberSaveable usage.
**Why manual:** StateRestorationTester can give a false pass for dialogs (see known_issues).
**Steps:** open the add-area dialog, type a name, rotate the emulator. Repeat for the
add-GrowZone dialog on the canvas.
**Expected:** dialog still open, typed text kept.

### M3 — Restore after process death (not tried yet, pre-emptive)
**Run when:** touching navigation keys, navConfig, or screen loading.
**Steps:** open a canvas → Home → `adb shell am kill com.practice.plant_user` →
reopen from recents.
**Expected:** returns to the same canvas (a brief blank is a known issue).

### M4 — Older device (not tried yet, pre-emptive)
**Run when:** before a release, or after changing sizes or layout.
**Steps:** run the app on a minSdk emulator profile.
**Expected:** list, canvas, and dialogs usable; grid and zones render correctly.

### M5 — Allocation profile while panning the canvas
**Run when:** changing canvas drawing, gestures, or anything in the draw loop.
**Steps:** Profiler → record Java/Kotlin allocations → pan for ~5 s → sort by count.
**Expected:** no per-frame allocation of objects that could be created once
(compare counts to the previous recording noted here).
**Last recorded:** Stroke ~25.8k, Offset ~26.5k per 5 s (before Phase 5 fixes).

### Tests to do list