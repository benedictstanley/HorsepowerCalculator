package com.carmodai.app.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CarBuild::class, User::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carBuildDao(): CarBuildDao
    abstract fun userDao(): UserDao
}
