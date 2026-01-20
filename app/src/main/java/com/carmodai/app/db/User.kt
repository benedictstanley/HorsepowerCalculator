package com.carmodai.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val email: String,
    val phoneNumber: String = "",
    val password: String, // Storing plain text for demo purposes only
    val planName: String = "Free",
    val isUnlimited: Boolean = false,
    val calculationsLeft: Int = 3,
    val dynoRunsLeft: Int = 0,
    val subscriptionExpiry: Long = 0L
)
