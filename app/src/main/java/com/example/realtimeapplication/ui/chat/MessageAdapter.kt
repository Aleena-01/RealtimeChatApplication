package com.example.realtimeapplication.ui.chat

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.realtimeapplication.R
import com.example.realtimeapplication.data.model.Message
import com.example.realtimeapplication.databinding.ItemMessageReceivedBinding
import com.example.realtimeapplication.databinding.ItemMessageSentBinding
import com.example.realtimeapplication.util.TimeUtils
import com.google.firebase.auth.FirebaseAuth

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
            binding.tvTime.text = TimeUtils.formatTime(message.timestamp)
            
            // Read receipt logic: show blue ticks if read and showReadReceipts is true
            if (showReadReceipts) {
                binding.ivReadStatus.visibility = View.VISIBLE
                if (message.read) {
                    binding.ivReadStatus.setImageResource(R.drawable.ic_check) // Use a double check if you have ic_double_check
                    binding.ivReadStatus.imageTintList = ColorStateList.valueOf(Color.parseColor("#34B7F1")) // WhatsApp Blue
                } else {
                    binding.ivReadStatus.setImageResource(R.drawable.ic_check)
                    binding.ivReadStatus.imageTintList = ColorStateList.valueOf(Color.LTGRAY)
                }
            } else {
                binding.ivReadStatus.visibility = View.GONE
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
            binding.tvTime.text = TimeUtils.formatTime(message.timestamp)
        }
    }
}
