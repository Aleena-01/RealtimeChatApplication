package com.example.realtimeapplication.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.realtimeapplication.R
import com.example.realtimeapplication.data.model.User
import com.example.realtimeapplication.databinding.ItemUserBinding

class UserAdapter(private val onUserClick: (User) -> Unit) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private var users = listOf<User>()

    fun submitList(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount() = users.size

    inner class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.tvUsername.text = user.username
            binding.tvLastMessage.text = if (user.status == "Online") "Online" else "Last seen: ${user.lastSeen}"
            
            Glide.with(binding.ivUserProfile.context)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.ic_person)
                .into(binding.ivUserProfile)

            binding.onlineIndicator.visibility = if (user.status == "Online") View.VISIBLE else View.GONE

            binding.root.setOnClickListener { onUserClick(user) }
        }
    }
}
