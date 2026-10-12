package com.practice.plantUser.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import org.junit.Test

class ArchitectureTest {
    @Test
    fun layersOnlyDependDownwards() {
        Konsist.scopeFromProduction().assertArchitecture {
            val model = Layer("Model", "com.practice.plantUser.model..")
            val data = Layer("Data", "com.practice.plantUser.data..")
            val viewModel = Layer("ViewModel", "com.practice.plantUser.viewmodel..")
            val ui = Layer("UI", "com.practice.plantUser.ui..")

            model.dependsOnNothing()
            data.dependsOn(model)
            viewModel.dependsOn(model, data)
            ui.dependsOn(model, viewModel)
        }
    }
}
