package com.example.realtimeapplication.ui.group

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.realtimeapplication.R
import com.example.realtimeapplication.data.model.User
import com.example.realtimeapplication.databinding.ItemUserSelectableBinding

class SelectableUserAdapter(private val onSelectionChanged: (List<String>) -> Unit) : 
    RecyclerView.Adapter<SelectableUserAdapter.ViewHolder>() {

    private var users = listOf<User>()
    private var contactMap = mapOf<String, String>()
    private val selectedUserIds = mutableSetOf<String>()

    fun submitList(newUsers: List<User>, contacts: List<com.example.realtimeapplication.data.model.Contact> = emptyList()) {
        users = newUsers
        contactMap = contacts.associateBy({ it.contactUid }, { it.customName })
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserSelectableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount() = users.size

    inner class ViewHolder(private val binding: ItemUserSelectableBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.tvUsername.text = contactMap[user.uid] ?: user.username
            Glide.with(binding.ivUserProfile.context)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.ic_person)
                .into(binding.ivUserProfile)

            binding.cbSelect.setOnCheckedChangeListener(null)
            binding.cbSelect.isChecked = selectedUserIds.contains(user.uid)
            
            binding.cbSelect.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedUserIds.add(user.uid)
                else selectedUserIds.remove(user.uid)
                onSelectionChanged(selectedUserIds.toList())
            }

            binding.root.setOnClickListener {
                binding.cbSelect.isChecked = !binding.cbSelect.isChecked
            }
        }
    }
}
