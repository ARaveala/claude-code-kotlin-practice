package com.practice.plant_user.Architecture

import com.lemonappdev.konsist.api.architecture.Layer
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import org.junit.Test

class ArchitectureTest {
    @Test
    fun layersOnlyDependDownwards() {
        Konsist.scopeFromProduction().assertArchitecture {
            val model = Layer("Model", "com.practice.plant_user.model..")
            val data = Layer("Data", "com.practice.plant_user.data..")
            val viewModel = Layer("ViewModel", "com.practice.plant_user.viewmodel..")
            val ui = Layer("UI", "com.practice.plant_user.ui..")

            model.dependsOnNothing()
            data.dependsOn(model)
            viewModel.dependsOn(model, data)
            ui.dependsOn(model, viewModel)
        }
    }


}

