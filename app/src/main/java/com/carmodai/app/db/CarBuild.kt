package com.carmodai.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "car_builds")
data class CarBuild(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val year: String,
    val make: String,
    val model: String,
    val baseHp: Int,
    val mods: String,
    val estimatedHp: Int,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable