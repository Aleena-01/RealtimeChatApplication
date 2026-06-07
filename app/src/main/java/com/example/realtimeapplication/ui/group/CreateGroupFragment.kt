package com.example.realtimeapplication.ui.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.realtimeapplication.data.repository.GroupRepository
import com.example.realtimeapplication.databinding.FragmentCreateGroupBinding
import com.example.realtimeapplication.ui.home.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class CreateGroupFragment : Fragment() {
    private var _binding: FragmentCreateGroupBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by viewModels()
    private val groupRepository = GroupRepository()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: SelectableUserAdapter
    private var selectedMembers = listOf<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        homeViewModel.homeItemsWithContacts.observe(viewLifecycleOwner) { pair ->
            // Extract all users from homeItems (which might include groups, so filter for User)
            val users = pair.first.filterIsInstance<com.example.realtimeapplication.data.model.User>()
            adapter.submitList(users, pair.second)
        }

        binding.btnCreateGroup.setOnClickListener {
            val groupName = binding.etGroupName.text.toString().trim()
            if (groupName.isEmpty()) {
                Toast.makeText(requireContext(), "Enter group name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedMembers.size < 2) {
                Toast.makeText(requireContext(), "Please select at least 2 members", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    groupRepository.createGroup(groupName, selectedMembers)
                    Toast.makeText(requireContext(), "Group created successfully", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = SelectableUserAdapter { selectedIds ->
            selectedMembers = selectedIds
        }
        binding.rvSelectMembers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSelectMembers.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
