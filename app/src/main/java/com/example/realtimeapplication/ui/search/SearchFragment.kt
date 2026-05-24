package com.example.realtimeapplication.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.realtimeapplication.databinding.FragmentSearchBinding
import com.example.realtimeapplication.ui.home.UserAdapter

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var adapter: UserAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                viewModel.searchUsers(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        viewModel.searchResults.observe(viewLifecycleOwner) { users ->
            adapter.submitList(users)
            
            val query = binding.etSearch.text.toString().trim()
            if (users.isEmpty() && query.isNotEmpty()) {
                binding.tvNoResults.visibility = View.VISIBLE
                binding.tvNoResults.text = "The person with '$query' is not on the app."
            } else {
                binding.tvNoResults.visibility = View.GONE
            }
        }

        viewModel.isSearching.observe(viewLifecycleOwner) { isSearching ->
            // You could add a small ProgressBar in the endIcon or somewhere
            // For now, we'll just handle the results state
        }
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter { user ->
            val action = SearchFragmentDirections.actionSearchFragmentToChatFragment(user.uid)
            findNavController().navigate(action)
        }
        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearchResults.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
