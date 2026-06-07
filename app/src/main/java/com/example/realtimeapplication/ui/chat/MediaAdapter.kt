package com.example.realtimeapplication.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.realtimeapplication.databinding.ItemMediaBinding

class MediaAdapter(private val mediaUrls: List<String>) : RecyclerView.Adapter<MediaAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMediaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.itemView.context)
            .load(mediaUrls[position])
            .into(holder.binding.ivMedia)
    }

    override fun getItemCount(): Int = mediaUrls.size
}
