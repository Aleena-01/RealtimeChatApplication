package com.example.realtimeapplication.ui.auth

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.realtimeapplication.R
import com.example.realtimeapplication.data.model.User
import com.example.realtimeapplication.data.repository.AuthRepository
import com.example.realtimeapplication.data.repository.StorageRepository
import com.example.realtimeapplication.databinding.FragmentRegisterBinding
import com.example.realtimeapplication.util.Constants
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    private val authRepository = AuthRepository()
    private val storageRepository = StorageRepository()
    
    private var selectedImageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.ivProfile.setImageURI(it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
            return
        }

        // Pre-fill data if available
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val userData = authRepository.getUserData(currentUser.uid)
                if (userData != null) {
                    if (userData.username.isNotEmpty()) {
                        binding.etUsername.setText(userData.username)
                        binding.btnSave.text = "Continue"
                    }
                    
                    binding.etAbout.setText(userData.about)
                    
                    if (userData.profileImageUrl.isNotEmpty()) {
                        Glide.with(this@RegisterFragment)
                            .load(userData.profileImageUrl)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .into(binding.ivProfile)
                    }
                }
            } catch (e: Exception) {
                // Silently ignore or log fetch error
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }

        binding.fabAddPhoto.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etUsername.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveProfile(name)
        }

        binding.btnSkip.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun saveProfile(name: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val phone = FirebaseAuth.getInstance().currentUser?.phoneNumber ?: ""
        val about = binding.etAbout.text.toString().trim()
        
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val existingUser = authRepository.getUserData(uid)
                var imageUrl = existingUser?.profileImageUrl ?: ""
                
                selectedImageUri?.let { uri ->
                    try {
                        binding.progressBar.visibility = View.VISIBLE
                        val uploadedUrl = storageRepository.uploadImage(uri, "profile_images")
                        if (uploadedUrl.isNotEmpty()) {
                            imageUrl = uploadedUrl
                        }
                    } catch (uploadError: Exception) {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Image upload failed: ${uploadError.message}. Profile will be saved without image.", Toast.LENGTH_LONG).show()
                    }
                }

                val user = User(
                    uid = uid,
                    username = name,
                    phoneNumber = if (phone.isNotEmpty()) Constants.normalizePhone(phone) else (existingUser?.phoneNumber ?: ""),
                    profileImageUrl = imageUrl,
                    status = "Online",
                    about = if (about.isNotEmpty()) about else (existingUser?.about ?: "Hey there! I am using ChatApp."),
                    lastSeen = System.currentTimeMillis(),
                    showLastSeen = existingUser?.showLastSeen ?: true,
                    showReadReceipts = existingUser?.showReadReceipts ?: true
                )
                authRepository.saveUser(user)
                Toast.makeText(requireContext(), "Profile saved successfully", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to save profile: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
