package com.practice.plantUser.ui

import androidx.compose.runtime.saveable.Saver
import com.practice.plantUser.model.Anchor

/** The open edit panel's state (phase_3.md 3b, decisions.md 2026-10-20): which zone, the size draft
 * as typed or written by a handle drag, the soft warning, and the selected handle. A null session
 * means the panel is closed, so "panel open" and "draft exists" can't disagree.
 *
 * Kept with rememberSaveable via [EditSessionSaver]. Derived values (parsed sizes, ghost, the drag's
 * parent origin) are recomputed after rotation, not saved. Mode (resize / move) joins in 3c. */
data class EditSession(
    val zoneId: Long,
    val widthText: String,
    val depthText: String,
    val heightText: String,
    val warningCm: Double? = null,
    val selectedHandle: Anchor? = null
) {
    /** The selected handle's opposite corner; top-left (3a's behaviour) when none is selected. */
    val anchor: Anchor
        get() = selectedHandle?.opposite ?: Anchor.TOP_LEFT
}

private const val KEY_ZONE_ID = "zoneId"
private const val KEY_WIDTH = "widthText"
private const val KEY_DEPTH = "depthText"
private const val KEY_HEIGHT = "heightText"
private const val KEY_WARNING = "warningCm"
private const val KEY_HANDLE = "selectedHandle"

/** EditSession <-> a map of Bundle-storable values (Long, String, Double, the handle's enum name):
 * hand-written serialise/deserialise, no reflection or Parcelize plugin. Runs only when the state is
 * saved or restored (rotation, process death), never per frame. A null session saves nothing and
 * restores as null (panel closed). */
val EditSessionSaver: Saver<EditSession?, Any> = Saver(
    save = { session ->
        session?.let {
            hashMapOf(
                KEY_ZONE_ID to it.zoneId,
                KEY_WIDTH to it.widthText,
                KEY_DEPTH to it.depthText,
                KEY_HEIGHT to it.heightText,
                KEY_WARNING to it.warningCm,
                KEY_HANDLE to it.selectedHandle?.name
            )
        }
    },
    restore = { saved ->
        // Map<*, *> is a checked cast; each value is cast back to the type save() put in.
        val map = saved as Map<*, *>
        EditSession(
            zoneId = map[KEY_ZONE_ID] as Long,
            widthText = map[KEY_WIDTH] as String,
            depthText = map[KEY_DEPTH] as String,
            heightText = map[KEY_HEIGHT] as String,
            warningCm = map[KEY_WARNING] as Double?,
            selectedHandle = (map[KEY_HANDLE] as String?)?.let { Anchor.valueOf(it) }
        )
    }
)
