package com.example.realtimeapplication.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.realtimeapplication.databinding.FragmentSearchBinding
import com.example.realtimeapplication.ui.home.UserAdapter

import android.widget.Toast
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
    private val contactRepository = com.example.realtimeapplication.data.repository.ContactRepository()
    private lateinit var adapter: UserAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.ccp.registerCarrierNumberEditText(binding.etSearch)

        binding.btnSearchAdd.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val phone = if (binding.ccp.isValidFullNumber) binding.ccp.fullNumberWithPlus else binding.etSearch.text.toString().trim()
            
            if (phone.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            viewModel.searchUsers(phone)
        }

        viewModel.searchResults.observe(viewLifecycleOwner) { users ->
            contactRepository.getContacts().asLiveData().observe(viewLifecycleOwner) { contacts ->
                adapter.submitList(users, contacts)
            }
            
            val query = binding.etSearch.text.toString().trim()
            val customName = binding.etName.text.toString().trim()

            if (users.isEmpty() && query.isNotEmpty()) {
                binding.tvNoResults.visibility = View.VISIBLE
                binding.tvNoResults.text = "This user is currently not on ChatApp."
            } else {
                binding.tvNoResults.visibility = View.GONE
                // If user found and name is provided, show add dialog automatically or on click
                if (users.isNotEmpty() && customName.isNotEmpty()) {
                    // We could auto-add here or let them click the result
                }
            }
        }
    }

    private fun showUserNotFoundDialog(query: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("User Not Found")
            .setMessage("The person with details '$query' is not registered on ChatApp yet. You can only start a conversation with existing members.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter { item ->
            if (item is com.example.realtimeapplication.data.model.User) {
                showUserOptionsDialog(item)
            }
        }
        adapter.showPhoneNumber = true
        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearchResults.adapter = adapter
    }

    private fun showUserOptionsDialog(user: com.example.realtimeapplication.data.model.User) {
        val options = arrayOf("Message", "Add to Contacts")
        AlertDialog.Builder(requireContext())
            .setTitle(user.phoneNumber)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // Message
                        val action = SearchFragmentDirections.actionSearchFragmentToChatFragment(user.uid)
                        findNavController().navigate(action)
                    }
                    1 -> { // Add to Contacts
                        showAddContactDialog(user)
                    }
                }
            }
            .show()
    }

    private fun showAddContactDialog(user: com.example.realtimeapplication.data.model.User) {
        val dialogBinding = com.example.realtimeapplication.databinding.DialogAddContactBinding.inflate(layoutInflater)
        dialogBinding.etContactPhone.setText(user.phoneNumber)
        dialogBinding.etContactPhone.isEnabled = false
        
        // Use the name from the search field if provided, else their username
        val searchName = binding.etName.text.toString().trim()
        dialogBinding.etContactName.setText(if (searchName.isNotEmpty()) searchName else user.username)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Contact")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                val customName = dialogBinding.etContactName.text.toString().trim()
                if (customName.isNotEmpty()) {
                    lifecycleScope.launch {
                        contactRepository.addContactBidirectional(user, customName)
                        Toast.makeText(requireContext(), "Contact added: $customName", Toast.LENGTH_SHORT).show()
                        val action = SearchFragmentDirections.actionSearchFragmentToChatFragment(user.uid)
                        findNavController().navigate(action)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
