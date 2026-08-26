package com.practice.plant_user.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practice.plant_user.data.AreaDao
import com.practice.plant_user.data.AreaEntity
import com.practice.plant_user.ui.Area
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AreaViewModel(private val areaDao: AreaDao) : ViewModel() {
    val areas: StateFlow<List<Area>> = areaDao.getAll()
        .map { entities -> entities.map { Area(id = it.id, name = it.name) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addArea(name: String) {
        viewModelScope.launch {
            areaDao.insert(AreaEntity(name = name))
        }
    }
}
