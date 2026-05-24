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
import kotlinx.coroutines.launch

class CreateGroupFragment : Fragment() {
    private var _binding: FragmentCreateGroupBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by viewModels()
    private val groupRepository = GroupRepository()
    private lateinit var adapter: SelectableUserAdapter
    private var selectedMembers = listOf<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        homeViewModel.users.observe(viewLifecycleOwner) { users ->
            adapter.submitList(users)
        }

        binding.btnCreateGroup.setOnClickListener {
            val groupName = binding.etGroupName.text.toString()
            if (groupName.isEmpty()) {
                Toast.makeText(requireContext(), "Enter group name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedMembers.isEmpty()) {
                Toast.makeText(requireContext(), "Select at least one member", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    groupRepository.createGroup(groupName, selectedMembers)
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
