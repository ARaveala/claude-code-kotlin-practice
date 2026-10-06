package com.practice.plant_user.ui.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/** The Area list screen. No arguments, so a single shared instance is enough. */
@Serializable
data object AreaListKey : NavKey

/** The canvas for one Area. Carries only the id, not the Area itself. */
@Serializable
data class AreaCanvasKey(val areaId: Long) : NavKey

/**
 * Tells the back-stack saver which concrete key types exist, so it can
 * save and restore them without reflection. Every new screen key must be
 * registered here, or saving state will crash at runtime.
 */
val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(AreaListKey::class, AreaListKey.serializer())
            subclass(AreaCanvasKey::class, AreaCanvasKey.serializer())
        }
    }
}
