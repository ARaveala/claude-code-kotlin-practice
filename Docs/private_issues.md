**Cross-platform (KMP) is a possible future direction**
not planned. Baseline audit (Phase 2): Android-only imports are in MainActivity (platform shim, expected), GardenDatabase (Context, database construction splits per platform), Theme.kt (template dynamic colour: a design choice, not just a port), and GrowZoneViewModel (Log: needs a logger wrapper). Habit going forward: keep android.* and Context out of shared logic, and pass platform objects in from MainActivity rather than reaching for them.

- complete phase1's private study c++ comp snippets. 