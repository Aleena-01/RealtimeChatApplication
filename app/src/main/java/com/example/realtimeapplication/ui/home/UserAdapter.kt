package com.example.realtimeapplication.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.realtimeapplication.R
import com.example.realtimeapplication.data.model.Group
import com.example.realtimeapplication.data.model.User
import com.example.realtimeapplication.databinding.ItemUserBinding

class UserAdapter(private val onClick: (Any) -> Unit) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private var items = mutableListOf<Any>()
    private var contactMap = mutableMapOf<String, String>()
    var showPhoneNumber: Boolean = false

    fun submitList(newItems: List<Any>, contacts: List<com.example.realtimeapplication.data.model.Contact> = emptyList()) {
        items = newItems.toMutableList()
        contactMap = contacts.associateBy({ it.contactUid }, { it.customName }).toMutableMap()
        notifyDataSetChanged()
    }

    fun getItemAt(position: Int): Any = items[position]

    fun removeItem(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Any) {
            if (item is User) {
                val customName = contactMap[item.uid]
                // Priority: Custom Contact Name > Registered Username > Phone Number
                binding.tvUsername.text = customName ?: item.username.ifEmpty { item.phoneNumber }
                
                binding.tvLastMessage.text = if (item.isTyping) "typing..." 
                    else if (item.lastMessage.isNotEmpty()) item.lastMessage
                    else if (item.status == "Online") "Online"
                    else item.about
                
                // Show unread count if > 0
                val unreadCount = item.unreadCount
                if (unreadCount > 0) {
                    binding.tvUnreadCount.visibility = View.VISIBLE
                    binding.tvUnreadCount.text = unreadCount.toString()
                } else {
                    binding.tvUnreadCount.visibility = View.GONE
                }

                Glide.with(binding.ivUserProfile.context)
                    .load(item.profileImageUrl)
                    .placeholder(R.drawable.ic_person)
                    .into(binding.ivUserProfile)

                binding.onlineIndicator.visibility = if (item.status == "Online") View.VISIBLE else View.GONE
                binding.root.setOnClickListener { onClick(item) }
            } else if (item is Group) {
                binding.tvUsername.text = item.groupName
                binding.tvLastMessage.text = "${item.members.size} members"
                
                Glide.with(binding.ivUserProfile.context)
                    .load(item.groupImageUrl)
                    .placeholder(R.drawable.ic_person) // Should ideally be a group icon
                    .into(binding.ivUserProfile)

                binding.onlineIndicator.visibility = View.GONE
                binding.root.setOnClickListener { onClick(item) }
            }
        }
    }
}
