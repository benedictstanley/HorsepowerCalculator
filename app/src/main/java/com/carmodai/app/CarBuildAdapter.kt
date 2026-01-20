package com.carmodai.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.carmodai.app.db.CarBuild

class CarBuildAdapter(private val onItemClick: (CarBuild) -> Unit) : RecyclerView.Adapter<CarBuildAdapter.ViewHolder>() {
    private var builds: List<CarBuild> = emptyList()

    fun submitList(newBuilds: List<CarBuild>) {
        builds = newBuilds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_car_build, parent, false)
        return ViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val build = builds[position]
        holder.bind(build)
    }

    override fun getItemCount() = builds.size

    class ViewHolder(itemView: View, private val onItemClick: (CarBuild) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvCarName)
        private val tvMods: TextView = itemView.findViewById(R.id.tvMods)

        fun bind(build: CarBuild) {
            tvTitle.text = "${build.year} ${build.make} ${build.model}"
            tvMods.text = "Est. HP: ${build.estimatedHp}\nMods: ${build.mods}"
            
            itemView.setOnClickListener {
                onItemClick(build)
            }
        }
    }
}