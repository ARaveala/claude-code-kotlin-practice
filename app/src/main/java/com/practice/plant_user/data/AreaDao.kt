package com.practice.plant_user.data

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AreaDao {
    @Insert
    suspend fun insert(area: AreaEntity): Long

    @Query("SELECT * FROM areas")
    fun getAll(): Flow<List<AreaEntity>>
}
