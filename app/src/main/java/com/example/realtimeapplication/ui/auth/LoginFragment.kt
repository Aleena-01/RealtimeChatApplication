package com.example.realtimeapplication.ui.auth

import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.realtimeapplication.R
import com.example.realtimeapplication.databinding.FragmentLoginBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Gradient text for "ChatApp"
        val paint = binding.tvWelcome.paint
        val width = paint.measureText(binding.tvWelcome.text.toString())
        val textShader = LinearGradient(
            0f, 0f, width, binding.tvWelcome.textSize,
            intArrayOf(
                ContextCompat.getColor(requireContext(), R.color.primary),
                ContextCompat.getColor(requireContext(), R.color.accent)
            ), null, Shader.TileMode.CLAMP
        )
        binding.tvWelcome.paint.shader = textShader

        // Setup CCP with EditText
        binding.ccp.registerCarrierNumberEditText(binding.etPhone)

        // Add some "interactive" feel with a simple fade-in
        binding.cardLogin.alpha = 0f
        binding.cardLogin.animate().alpha(1f).setDuration(800).start()
        binding.ivLogo.scaleX = 0.8f
        binding.ivLogo.scaleY = 0.8f
        binding.ivLogo.animate().scaleX(1f).scaleY(1f).setDuration(1000).start()

        binding.btnSendOtp.setOnClickListener {
            if (binding.ccp.isValidFullNumber) {
                val fullNumber = binding.ccp.fullNumberWithPlus
                startPhoneVerification(fullNumber)
            } else {
                Toast.makeText(requireContext(), "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToHomeFragment())
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
                binding.progressBar.visibility = View.GONE
                binding.btnSendOtp.isEnabled = true
            }
        }
    }

    private fun startPhoneVerification(phoneNumber: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSendOtp.isEnabled = false
        
        val options = PhoneAuthOptions.newBuilder()
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                    viewModel.signInWithCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSendOtp.isEnabled = true
                    Toast.makeText(requireContext(), "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSendOtp.isEnabled = true
                    val action = LoginFragmentDirections.actionLoginFragmentToOtpFragment(phoneNumber, verificationId)
                    findNavController().navigate(action)
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
