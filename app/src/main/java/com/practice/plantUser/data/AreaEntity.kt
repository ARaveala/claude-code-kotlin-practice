package com.practice.plantUser.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "areas")
data class AreaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)
