package com.example.realtimeapplication.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.realtimeapplication.R
import com.example.realtimeapplication.data.model.Message
import com.example.realtimeapplication.databinding.FragmentContactInfoBinding

class ContactInfoFragment : Fragment() {
    private var _binding: FragmentContactInfoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private val args: ContactInfoFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContactInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        if (args.isGroup) {
            setupGroupInfo()
        } else {
            setupUserInfo()
        }

        binding.btnSearchChat.setOnClickListener {
            findNavController().navigateUp()
            // In a real app, you might pass a flag to open search automatically
        }

        binding.btnVoice.setOnClickListener {
            Toast.makeText(requireContext(), "Voice call not implemented", Toast.LENGTH_SHORT).show()
        }

        binding.btnVideo.setOnClickListener {
            Toast.makeText(requireContext(), "Video call not implemented", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupUserInfo() {
        // Observe contact data
        viewModel.getContact(args.userId).observe(viewLifecycleOwner) { contact ->
            if (contact != null) {
                binding.tvContactName.text = contact.customName
                binding.tvContactPhone.text = contact.phoneNumber
            } else {
                // Not a contact, will be handled by user data observer
            }
        }

        // Observe user data
        viewModel.getOtherUser(args.userId).observe(viewLifecycleOwner) { user ->
            user?.let {
                // If it's not a contact, use registered data
                if (binding.tvContactName.text == "Name" || binding.tvContactName.text == it.phoneNumber) {
                     binding.tvContactName.text = it.username.ifEmpty { it.phoneNumber }
                     binding.tvContactPhone.text = it.phoneNumber
                }
                
                binding.tvAboutValue.text = it.about
                Glide.with(this)
                    .load(it.profileImageUrl)
                    .placeholder(R.drawable.ic_person)
                    .into(binding.ivContactProfile)
            }
        }

        viewModel.getMessages(args.userId).observe(viewLifecycleOwner) { messages ->
            val mediaMessages = messages.filter { it.type == "image" }
            setupMediaRecyclerView(mediaMessages)
        }
    }

    private fun setupGroupInfo() {
        binding.tvTitle.text = "Group info"
        viewModel.getGroup(args.userId).observe(viewLifecycleOwner) { group ->
            group?.let {
                binding.tvContactName.text = it.groupName
                binding.tvContactPhone.text = "${it.members.size} members"
                binding.tvAboutValue.text = "Group created by ${it.adminId}" // Placeholder
                Glide.with(this).load(it.groupImageUrl)
                    .placeholder(R.drawable.ic_person).into(binding.ivContactProfile)
            }
        }

        viewModel.getGroupMessages(args.userId).observe(viewLifecycleOwner) { messages ->
            val mediaMessages = messages.filter { it.type == "image" }
            setupMediaRecyclerView(mediaMessages)
        }
    }

    private fun setupMediaRecyclerView(mediaMessages: List<Message>) {
        binding.tvMediaCount.text = mediaMessages.size.toString()
        val mediaAdapter = MediaAdapter(mediaMessages.map { it.imageUrl })
        binding.rvMedia.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvMedia.adapter = mediaAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
