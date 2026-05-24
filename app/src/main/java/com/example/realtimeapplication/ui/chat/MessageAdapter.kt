package com.example.realtimeapplication.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.realtimeapplication.R
import com.example.realtimeapplication.data.model.Message
import com.example.realtimeapplication.databinding.ItemMessageReceivedBinding
import com.example.realtimeapplication.databinding.ItemMessageSentBinding
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var messages = listOf<Message>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var showReadReceipts: Boolean = true

    companion object {
        private const val TYPE_SENT = 1
        private const val TYPE_RECEIVED = 2
    }

    fun submitList(newMessages: List<Message>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) TYPE_SENT else TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_SENT) {
            val binding = ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            SentViewHolder(binding)
        } else {
            val binding = ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ReceivedViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is SentViewHolder) holder.bind(message)
        else if (holder is ReceivedViewHolder) holder.bind(message)
    }

    override fun getItemCount() = messages.size

    inner class SentViewHolder(private val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            if (message.type == "image") {
                binding.ivMessageImage.visibility = View.VISIBLE
                binding.tvMessage.visibility = if (message.messageText.isEmpty()) View.GONE else View.VISIBLE
                Glide.with(binding.ivMessageImage.context).load(message.imageUrl).into(binding.ivMessageImage)
            } else {
                binding.ivMessageImage.visibility = View.GONE
                binding.tvMessage.visibility = View.VISIBLE
                binding.tvMessage.text = message.messageText
            }
            binding.tvTime.text = formatTime(message.timestamp)
            
            // Read receipt logic
            if (showReadReceipts && message.read) {
                binding.ivReadStatus.setImageResource(R.drawable.ic_check_circle)
                binding.ivReadStatus.imageTintList = android.content.res.ColorStateList.valueOf(
                    binding.root.context.getColor(R.color.primary)
                )
            } else {
                binding.ivReadStatus.setImageResource(R.drawable.ic_check)
                binding.ivReadStatus.imageTintList = android.content.res.ColorStateList.valueOf(
                    binding.root.context.getColor(R.color.text_secondary)
                )
            }
        }
    }

    inner class ReceivedViewHolder(private val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            if (message.type == "image") {
                binding.ivMessageImage.visibility = View.VISIBLE
                binding.tvMessage.visibility = if (message.messageText.isEmpty()) View.GONE else View.VISIBLE
                Glide.with(binding.ivMessageImage.context).load(message.imageUrl).into(binding.ivMessageImage)
            } else {
                binding.ivMessageImage.visibility = View.GONE
                binding.tvMessage.visibility = View.VISIBLE
                binding.tvMessage.text = message.messageText
            }
            binding.tvTime.text = formatTime(message.timestamp)
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
