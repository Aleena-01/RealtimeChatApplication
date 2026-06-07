package com.example.realtimeapplication.ui.profile

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.realtimeapplication.R
import com.example.realtimeapplication.data.repository.AuthRepository
import com.example.realtimeapplication.data.repository.ChatRepository
import com.example.realtimeapplication.data.repository.StorageRepository
import com.example.realtimeapplication.databinding.FragmentProfileBinding
import com.example.realtimeapplication.ui.auth.AuthViewModel
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()
    private val authRepository = AuthRepository()
    private val storageRepository = StorageRepository()
    private val chatRepository = ChatRepository()

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadProfileImage(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPrefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        binding.switchDarkMode.isChecked = sharedPrefs.getBoolean("dark_mode", false)

        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid != null) {
            chatRepository.getUser(currentUid).asLiveData().observe(viewLifecycleOwner) { user ->
                user?.let {
                    binding.tvProfileName.text = it.username
                    binding.tvProfileEmail.text = it.phoneNumber
                    binding.tvCurrentStatus.text = it.about
                    
                    Glide.with(this@ProfileFragment)
                        .load(it.profileImageUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(binding.ivLargeProfile)
                    
                    binding.switchReadReceipts.isChecked = it.showReadReceipts
                    binding.switchLastSeen.isChecked = it.showLastSeen
                }
            }
        }

        binding.ivEditStatus.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_aboutEditFragment)
        }

        binding.chipBusy.setOnClickListener { updateStatus("Busy 🚫") }
        binding.chipNotAvailable.setOnClickListener { updateStatus("Not available 📴") }
        binding.chipOnlyCalls.setOnClickListener { updateStatus("Only calls 📞") }
        binding.chipGoodVibes.setOnClickListener { updateStatus("Good vibes only ✨") }

        binding.fabChangeDp.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("dark_mode", isChecked).apply()
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        binding.switchReadReceipts.setOnCheckedChangeListener { _, _ ->
            updatePrivacy()
        }

        binding.switchLastSeen.setOnCheckedChangeListener { _, _ ->
            updatePrivacy()
        }

        binding.btnLogout.setOnClickListener {
            authViewModel.logout()
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }
    }

    private fun uploadProfileImage(uri: Uri) {
        lifecycleScope.launch {
            try {
                val url = storageRepository.uploadImage(uri, "profile_pics")
                authRepository.updateProfileImage(url)
                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePrivacy() {
        lifecycleScope.launch {
            authRepository.updatePrivacySettings(
                binding.switchLastSeen.isChecked,
                binding.switchReadReceipts.isChecked
            )
        }
    }

    private fun updateStatus(status: String) {
        lifecycleScope.launch {
            try {
                authRepository.updateAbout(status)
                Toast.makeText(requireContext(), "Status updated", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to update status", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
