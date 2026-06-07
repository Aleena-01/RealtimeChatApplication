package com.example.realtimeapplication.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.realtimeapplication.R
import com.example.realtimeapplication.data.model.Group
import com.example.realtimeapplication.data.model.User
import com.example.realtimeapplication.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: UserAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeActions()

        viewModel.homeItemsWithContacts.observe(viewLifecycleOwner) { pair ->
            adapter.submitList(pair.first, pair.second)
        }

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_search -> {
                    findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
                    true
                }
                R.id.action_create_group -> {
                    findNavController().navigate(R.id.action_homeFragment_to_createGroupFragment)
                    true
                }
                else -> false
            }
        }

        binding.fabAddContact.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
        }
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter { item ->
            when (item) {
                is User -> {
                    val action = HomeFragmentDirections.actionHomeFragmentToChatFragment(item.uid, false)
                    findNavController().navigate(action)
                }
                is Group -> {
                    val action = HomeFragmentDirections.actionHomeFragmentToChatFragment(item.groupId, true)
                    findNavController().navigate(action)
                }
            }
        }
        binding.rvChats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChats.adapter = adapter
    }

    private fun setupSwipeActions() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.getItemAt(position)
                
                if (direction == ItemTouchHelper.LEFT) {
                    // Delete action
                    showDeleteConfirmation(item, position)
                } else if (direction == ItemTouchHelper.RIGHT) {
                    // Mute/Unmute action
                    toggleMute(item, position)
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.rvChats)
    }

    private fun showDeleteConfirmation(item: Any, position: Int) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Chat")
            .setMessage("Are you sure you want to delete this chat?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteChat(item)
            }
            .setNegativeButton("Cancel") { _, _ ->
                adapter.notifyItemChanged(position)
            }
            .show()
    }

    private fun toggleMute(item: Any, position: Int) {
        val name = if (item is User) item.username else if (item is Group) item.groupName else ""
        android.widget.Toast.makeText(requireContext(), "Chat with $name muted", android.widget.Toast.LENGTH_SHORT).show()
        adapter.notifyItemChanged(position)
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateStatus("Online")
    }

    override fun onPause() {
        super.onPause()
        viewModel.updateStatus("Offline")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
