package com.example.realtimeapplication.ui.chat

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.realtimeapplication.databinding.FragmentChatBinding
import com.example.realtimeapplication.data.repository.StorageRepository
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.realtimeapplication.R
import com.example.realtimeapplication.data.model.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private val args: ChatFragmentArgs by navArgs()
    private lateinit var adapter: MessageAdapter
    private val storageRepository = StorageRepository()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    
    private var currentUserSettings: User? = null
    private var otherUserSettings: User? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadImage(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        // Fetch current user settings
        currentUserId?.let { uid ->
            viewModel.getOtherUser(uid).observe(viewLifecycleOwner) { user ->
                currentUserSettings = user
                updateAdapterSettings()
            }
        }

        viewModel.getOtherUser(args.userId).observe(viewLifecycleOwner) { otherUser ->
            otherUserSettings = otherUser
            updateAdapterSettings()
            
            otherUser?.let {
                binding.tvChatUsername.text = it.username
                
                // Last seen logic
                if (it.showLastSeen) {
                    binding.tvChatStatus.text = if (it.status == "Online") "Online" 
                        else "Last seen: ${formatLastSeen(it.lastSeen)}"
                } else {
                    binding.tvChatStatus.text = if (it.status == "Online") "Online" else ""
                }
                
                binding.tvTyping.visibility = if (it.isTyping) View.VISIBLE else View.GONE
                
                Glide.with(this).load(it.profileImageUrl)
                    .placeholder(R.drawable.ic_person).into(binding.ivChatUser)
            }
        }

        viewModel.getMessages(args.userId).observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages)
            
            // Mark last received message as read if receipts are on
            messages.lastOrNull { it.senderId == args.userId && !it.read }?.let {
                if (currentUserSettings?.showReadReceipts == true) {
                    viewModel.markAsRead(args.userId, it.messageId)
                }
            }
            
            if (messages.isNotEmpty()) {
                binding.rvMessages.scrollToPosition(messages.size - 1)
            }
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString()
            if (text.isNotEmpty()) {
                viewModel.sendMessage(args.userId, text)
                binding.etMessage.setText("")
            }
        }

        binding.btnAttach.setOnClickListener {
            getContent.launch("image/*")
        }

        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setTyping(s.toString().isNotEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateAdapterSettings() {
        // Only show read receipts if BOTH users have them enabled
        adapter.showReadReceipts = (currentUserSettings?.showReadReceipts == true) && 
                                   (otherUserSettings?.showReadReceipts == true)
        adapter.notifyDataSetChanged()
    }

    private fun formatLastSeen(timestamp: Long): String {
        if (timestamp == 0L) return "Unknown"
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun uploadImage(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val imageUrl = storageRepository.uploadImage(uri, "chat_images")
                viewModel.sendMessage(args.userId, "", type = "image", imageUrl = imageUrl)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter()
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.setTyping(false)
        _binding = null
    }
}
