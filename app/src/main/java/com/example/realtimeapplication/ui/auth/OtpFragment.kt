package com.example.realtimeapplication.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.realtimeapplication.R
import com.example.realtimeapplication.data.model.User
import com.example.realtimeapplication.data.repository.AuthRepository
import com.example.realtimeapplication.databinding.FragmentOtpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OtpFragment : Fragment() {
    private var _binding: FragmentOtpBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    private val args: OtpFragmentArgs by navArgs()
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
                verifyAndRegister(credential)
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

    private fun verifyAndRegister(credential: com.google.firebase.auth.PhoneAuthCredential) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val result = auth.signInWithCredential(credential).await()
                val uid = result.user?.uid
                if (uid != null && args.username != null) {
                    // It's a new registration, save user data
                    val user = User(
                        uid = uid,
                        username = args.username!!,
                        phoneNumber = args.phoneNumber,
                        status = "Online",
                        lastSeen = System.currentTimeMillis()
                    )
                    db.collection("users").document(uid).set(user).await()
                }
                findNavController().navigate(R.id.action_otpFragment_to_homeFragment)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
