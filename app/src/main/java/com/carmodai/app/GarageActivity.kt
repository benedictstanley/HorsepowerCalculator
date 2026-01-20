package com.carmodai.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.carmodai.app.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class GarageActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var adapter: CarBuildAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_garage)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "car-mods-db")
            .fallbackToDestructiveMigration()
            .build()
        adapter = CarBuildAdapter { build ->
            val intent = android.content.Intent()
            intent.putExtra("selected_build", build)
            setResult(RESULT_OK, intent)
            finish()
        }

        val recyclerView = findViewById<RecyclerView>(R.id.rvGarage)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        CoroutineScope(Dispatchers.Main).launch {
            db.carBuildDao().getAllBuilds().collect { builds ->
                adapter.submitList(builds)
            }
        }
    }
}