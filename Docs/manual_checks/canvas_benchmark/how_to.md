# M5 — canvas pan benchmark (scripted)

Compares frame timing between code states while panning a stress Area.
Use for relative before/after only: a debug build on an emulator is not a phone.

## Setup (once)
1. Dev emulator running, app installed (Run from Android Studio). `adb devices` lists it.
2. In the app, create an Area named `stress`. Note its id (`SELECT id, name FROM areas;`
   in the Database Inspector).
3. Seed it in the Database Inspector, replacing <AREA_ID> everywhere:

```sql
-- 200 top-level boxes in a 20 x 10 grid
WITH RECURSIVE n(i) AS (SELECT 0 UNION ALL SELECT i + 1 FROM n WHERE i < 199)
INSERT INTO grow_zones (areaId, parentGrowZoneId, type, name, widthCm, depthCm, heightCm, boundsEnforced, xCm, yCm)
SELECT <AREA_ID>, NULL, 'BOX', 'stress-' || i, 40.0, 40.0, NULL, 1,
       (i % 20) * 60.0, (i / 20) * 60.0
FROM n;
```

```sql
-- depth 2: stress-1..3, 11..13, ... go inside stress-0, 10, 20, ...
UPDATE grow_zones
SET parentGrowZoneId = (
        SELECT p.id FROM grow_zones p
        WHERE p.areaId = grow_zones.areaId
          AND p.name = 'stress-' || ((CAST(substr(grow_zones.name, 8) AS INTEGER) / 10) * 10)),
    widthCm = 8.0, depthCm = 8.0,
    xCm = 4.0 + (CAST(substr(name, 8) AS INTEGER) % 10) * 9.0, yCm = 16.0
WHERE areaId = <AREA_ID> AND name LIKE 'stress-%'
  AND CAST(substr(name, 8) AS INTEGER) % 10 BETWEEN 1 AND 3;
```

```sql
-- depth 3: stress-5, 15, 25, ... go inside stress-1, 11, 21, ...
-- relies on consecutive ids from the insert above (stress-N has id = first id + N)
UPDATE grow_zones
SET parentGrowZoneId = id - 4,
    widthCm = 3.0, depthCm = 3.0, xCm = 2.0, yCm = 2.0
WHERE areaId = <AREA_ID>
  AND CAST(substr(name, 8) AS INTEGER) % 10 = 5;
```

Check: `SELECT COUNT(*) FROM grow_zones WHERE areaId = <AREA_ID> AND parentGrowZoneId IS NOT NULL;`
must return 80 (60 at depth 2, 20 at depth 3).

Cleanup: `DELETE FROM grow_zones WHERE areaId = <AREA_ID>;`
Dev emulator only. Never run `connectedAndroidTest` there (it wipes the data).

## Running
1. Build and install the code state to measure (Run from Android Studio, or
   `adb install -r <apk>` to swap prebuilt APKs; the data survives).
2. Open the stress Area so the canvas is on screen.
3. `./drawTest.sh <label> 200` (label like `positioncm.1`). It does the warm-up pan,
   resets the frame counters, runs the measured pan and appends a row to
   `~/m5_results/m5.csv`.
4. Repeat three times per state and take the median.

## Reading the results
- Compare janky %, p50, p90 and the frame count. A difference only counts if it is
  larger than the spread between the three runs (about +/-10% on the emulator).
- Allocation counts (Stroke, Offset, PositionCm) come from the Android Studio
  Profiler and are manual: record Java/Kotlin allocations during the pan only.
  **Divide each count by Total frames rendered** to get per-frame values. Slower runs
  render fewer frames, so their raw totals are smaller even if each frame allocates the same.
- With the Profiler recording, allocation tracking slows the app a lot, so those
  runs are not comparable with bare-script runs. Keep the two sets in separate tables.
- `rendernode_kb` only changes when the number of draw calls changes.
- GC columns: one pan triggers only 0-2 GCs, so they cannot separate similar states.

## Last recorded (Profiler on, 200 zones, 2026-10-16)
Without precompute vs with precompute (median of 3): Stroke/frame 311 vs 136,
PositionCm/frame ~180 vs 0, janky 76% vs 53%, p50 65 vs 48 ms, p90 101 vs 65 ms.
With 4 zones the same change was within noise.