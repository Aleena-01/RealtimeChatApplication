package com.example.realtimeapplication.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.realtimeapplication.data.repository.AuthRepository
import com.example.realtimeapplication.data.repository.ChatRepository
import com.example.realtimeapplication.databinding.FragmentAboutEditBinding
import com.example.realtimeapplication.databinding.ItemStatusSuggestionBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AboutEditFragment : Fragment() {
    private var _binding: FragmentAboutEditBinding? = null
    private val binding get() = _binding!!
    private val authRepository = AuthRepository()
    private val chatRepository = ChatRepository()
    
    private val suggestions = listOf(
        "Available", "Busy", "At school", "At the movies", "At work",
        "Battery about to die", "Can't talk, ChatApp only", "In a meeting",
        "At the gym", "Sleeping", "Urgent calls only"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAboutEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid != null) {
            chatRepository.getUser(currentUid).asLiveData().observe(viewLifecycleOwner) { user ->
                if (user != null && binding.etAbout.text?.isEmpty() == true) {
                    binding.etAbout.setText(user.about)
                }
            }
        }

        binding.btnSave.setOnClickListener {
            val newAbout = binding.etAbout.text.toString().trim()
            if (newAbout.isNotEmpty()) {
                updateAbout(newAbout)
            }
        }

        setupSuggestions()
    }

    private fun setupSuggestions() {
        binding.rvSuggestions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSuggestions.adapter = SuggestionAdapter(suggestions) { selected ->
            binding.etAbout.setText(selected)
            updateAbout(selected)
        }
    }

    private fun updateAbout(about: String) {
        lifecycleScope.launch {
            try {
                authRepository.updateAbout(about)
                Toast.makeText(requireContext(), "About updated", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to update about", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class SuggestionAdapter(
        private val list: List<String>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<SuggestionAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemStatusSuggestionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(list[position])
        }

        override fun getItemCount() = list.size

        inner class ViewHolder(private val binding: ItemStatusSuggestionBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(text: String) {
                binding.tvSuggestion.text = text
                binding.root.setOnClickListener { onClick(text) }
            }
        }
    }
}
