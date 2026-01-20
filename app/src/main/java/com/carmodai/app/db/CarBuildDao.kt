package com.carmodai.app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CarBuildDao {
    @Insert
    suspend fun insert(build: CarBuild)

    @Query("SELECT * FROM car_builds ORDER BY timestamp DESC")
    fun getAllBuilds(): Flow<List<CarBuild>>
}