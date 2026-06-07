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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.realtimeapplication.databinding.FragmentChatBinding
import com.example.realtimeapplication.data.repository.StorageRepository
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.realtimeapplication.R
import com.example.realtimeapplication.data.model.User
import com.example.realtimeapplication.util.TimeUtils
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
    private var contactData: com.example.realtimeapplication.data.model.Contact? = null

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

        if (args.isGroup) {
            setupGroupChat()
        } else {
            setupPersonalChat()
            binding.btnAddContact.visibility = View.VISIBLE
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        val onProfileClick = View.OnClickListener {
            val action = ChatFragmentDirections.actionChatFragmentToContactInfoFragment(args.userId, args.isGroup)
            findNavController().navigate(action)
        }
        binding.ivChatUser.setOnClickListener(onProfileClick)
        binding.tvChatUsername.setOnClickListener(onProfileClick)
        binding.tvChatStatus.setOnClickListener(onProfileClick)

        binding.btnAddContact.setOnClickListener {
            addToContacts()
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                if (args.isGroup) {
                    viewModel.sendGroupMessage(args.userId, text)
                } else {
                    viewModel.sendMessage(args.userId, text)
                }
                binding.etMessage.setText("")
            }
        }

        binding.btnAttach.setOnClickListener {
            getContent.launch("image/*")
        }

        binding.btnSearchChat.setOnClickListener {
            if (binding.etSearchChat.visibility == View.VISIBLE) {
                binding.etSearchChat.visibility = View.GONE
                binding.layoutChatInfo.visibility = View.VISIBLE
                binding.etSearchChat.setText("")
                val allMessages = if (args.isGroup) viewModel.getGroupMessages(args.userId).value 
                                 else viewModel.getMessages(args.userId).value
                adapter.submitList(allMessages ?: emptyList())
            } else {
                binding.etSearchChat.visibility = View.VISIBLE
                binding.layoutChatInfo.visibility = View.GONE
                binding.etSearchChat.requestFocus()
            }
        }

        binding.etSearchChat.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                val currentMessages = if (args.isGroup) viewModel.getGroupMessages(args.userId).value 
                                     else viewModel.getMessages(args.userId).value
                if (query.isNotEmpty()) {
                    val filtered = currentMessages?.filter { 
                        it.messageText.contains(query, ignoreCase = true) 
                    }
                    adapter.submitList(filtered ?: emptyList())
                } else {
                    adapter.submitList(currentMessages ?: emptyList())
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!args.isGroup) viewModel.setTyping(s.toString().isNotEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupPersonalChat() {
        currentUserId?.let { uid ->
            viewModel.getOtherUser(uid).observe(viewLifecycleOwner) { user ->
                currentUserSettings = user
                updateAdapterSettings()
            }
        }

        viewModel.getContact(args.userId).observe(viewLifecycleOwner) { contact ->
            contactData = contact
            binding.btnAddContact.visibility = if (contact == null) View.VISIBLE else View.GONE
            updateChatHeader()
        }

        viewModel.getOtherUser(args.userId).observe(viewLifecycleOwner) { otherUser ->
            otherUserSettings = otherUser
            updateAdapterSettings()
            updateChatHeader()
        }

        viewModel.getMessages(args.userId).observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages)
            
            // Mark messages as read if they're from the other person
            if (messages.any { it.senderId == args.userId && !it.read }) {
                if (currentUserSettings?.showReadReceipts == true) {
                    viewModel.markAllAsRead(args.userId)
                }
            }
            
            // Auto scroll to bottom
            if (messages.isNotEmpty()) {
                binding.rvMessages.postDelayed({
                    binding.rvMessages.scrollToPosition(messages.size - 1)
                }, 100)
            }
        }
    }

    private fun updateChatHeader() {
        otherUserSettings?.let { user ->
            binding.tvChatUsername.text = contactData?.customName ?: user.phoneNumber
            
            if (user.showLastSeen) {
                binding.tvChatStatus.text = if (user.status == "Online") "Online" 
                    else "Last seen: ${TimeUtils.formatLastSeen(user.lastSeen)}"
            } else {
                binding.tvChatStatus.text = if (user.status == "Online") "Online" else ""
            }
            binding.tvTyping.visibility = if (user.isTyping) View.VISIBLE else View.GONE
            Glide.with(this).load(user.profileImageUrl)
                .placeholder(R.drawable.ic_person).into(binding.ivChatUser)
        }
    }

    private fun setupGroupChat() {
        viewModel.getGroup(args.userId).observe(viewLifecycleOwner) { group ->
            group?.let {
                binding.tvChatUsername.text = it.groupName
                binding.tvChatStatus.text = "${it.members.size} members"
                Glide.with(this).load(it.groupImageUrl)
                    .placeholder(R.drawable.ic_person).into(binding.ivChatUser)
            }
        }

        viewModel.getGroupMessages(args.userId).observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages)
            if (messages.isNotEmpty()) {
                binding.rvMessages.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun updateAdapterSettings() {
        adapter.showReadReceipts = (currentUserSettings?.showReadReceipts == true)
        adapter.notifyDataSetChanged()
    }

    private fun addToContacts() {
        otherUserSettings?.let { user ->
            val dialogBinding = com.example.realtimeapplication.databinding.DialogAddContactBinding.inflate(layoutInflater)
            dialogBinding.etContactPhone.setText(user.phoneNumber)
            dialogBinding.etContactPhone.isEnabled = false
            dialogBinding.etContactName.setText(user.username)

            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Add to Contacts")
                .setView(dialogBinding.root)
                .setPositiveButton("Add") { _, _ ->
                    val customName = dialogBinding.etContactName.text.toString().trim()
                    if (customName.isNotEmpty()) {
                        lifecycleScope.launch {
                            val contactRepository = com.example.realtimeapplication.data.repository.ContactRepository()
                            contactRepository.addContactBidirectional(user, customName)
                            Toast.makeText(requireContext(), "Contact added: $customName", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun uploadImage(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val imageUrl = storageRepository.uploadImage(uri, "chat_images")
                if (args.isGroup) {
                    // viewModel.sendGroupImageMessage(...) -> Add this to ViewModel if needed
                } else {
                    viewModel.sendMessage(args.userId, "", type = "image", imageUrl = imageUrl)
                }
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
        binding.rvMessages.setHasFixedSize(true)
        binding.rvMessages.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!args.isGroup) viewModel.setTyping(false)
        _binding = null
    }
}
