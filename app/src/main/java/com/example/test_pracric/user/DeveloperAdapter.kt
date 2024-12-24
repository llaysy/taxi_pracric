package com.example.test_pracric.user

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.test_pracric.R

class DeveloperAdapter(private val developers: List<Developer>) : RecyclerView.Adapter<DeveloperAdapter.DeveloperViewHolder>() {

    class DeveloperViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val developerImage: ImageView = itemView.findViewById(R.id.developer_image)
        val developerName: TextView = itemView.findViewById(R.id.developer_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeveloperViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.developer_item, parent, false)
        return DeveloperViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeveloperViewHolder, position: Int) {
        val developer = developers[position]
        holder.developerName.text = developer.name
        holder.developerImage.setImageResource(developer.imageResId)
    }

    override fun getItemCount(): Int {
        return developers.size
    }
}