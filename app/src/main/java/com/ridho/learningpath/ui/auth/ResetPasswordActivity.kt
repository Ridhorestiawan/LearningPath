package com.ridho.learningpath.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ridho.learningpath.data.repository.AuthRepository
import com.ridho.learningpath.databinding.ActivityResetPasswordBinding
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResetPasswordBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSend.setOnClickListener { sendReset() }
    }

    private fun sendReset() {
        val email = binding.etEmail.text.toString().trim()
        if (email.isEmpty()) {
            Toast.makeText(this, "Email wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnSend.isEnabled = false

        lifecycleScope.launch {
            val error = authRepository.resetPassword(email)
            binding.progressBar.visibility = android.view.View.GONE
            binding.btnSend.isEnabled = true
            Toast.makeText(
                this@ResetPasswordActivity,
                error ?: "Link reset telah dikirim ke email Anda",
                Toast.LENGTH_LONG
            ).show()
            if (error == null) finish()
        }
    }
}
