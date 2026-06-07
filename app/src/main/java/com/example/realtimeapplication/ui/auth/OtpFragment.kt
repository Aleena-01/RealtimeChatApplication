package com.example.realtimeapplication.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.realtimeapplication.R
import com.example.realtimeapplication.data.repository.AuthRepository
import com.example.realtimeapplication.databinding.FragmentOtpBinding
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OtpFragment : Fragment() {
    private var _binding: FragmentOtpBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    private val args: OtpFragmentArgs by navArgs()
    private val repository = AuthRepository()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnVerifyOtp.setOnClickListener {
            val code = binding.etOtp.text.toString()
            if (code.length == 6) {
                val credential = PhoneAuthProvider.getCredential(args.verificationId, code)
                verifyOtp(credential)
            } else {
                Toast.makeText(requireContext(), "Enter valid 6-digit OTP", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun verifyOtp(credential: com.google.firebase.auth.PhoneAuthCredential) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnVerifyOtp.isEnabled = false
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.signInWithCredential(credential)
                }
                val uid = result.user?.uid
                if (uid != null) {
                    val userData = withContext(Dispatchers.IO) {
                        repository.getUserData(uid)
                    }
                    if (userData == null || userData.username.isEmpty()) {
                        // First time or no profile
                        findNavController().navigate(OtpFragmentDirections.actionOtpFragmentToRegisterFragment())
                    } else {
                        // Already has profile
                        findNavController().navigate(OtpFragmentDirections.actionOtpFragmentToHomeFragment())
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnVerifyOtp.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
