package com.ridho.learningpath.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.ridho.learningpath.R
import com.ridho.learningpath.data.repository.AuthRepository
import com.ridho.learningpath.data.repository.GamificationRepository
import com.ridho.learningpath.databinding.ActivityEditProfilBinding
import kotlinx.coroutines.launch

class EditProfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfilBinding
    private val auth = FirebaseAuth.getInstance()
    private val authRepository = AuthRepository()
    private val gamificationRepository = GamificationRepository()
    private var currentAvatar: String = ""
    private var pickedAvatarUri: Uri? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            pickedAvatarUri = uri
            binding.ivAvatar.visibility = View.VISIBLE
            binding.tvAvatar.visibility = View.GONE
            binding.ivAvatar.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.layoutAvatar.setOnClickListener { openImagePicker() }
        binding.tvUbahFoto.setOnClickListener { openImagePicker() }

        loadProfile()
        binding.btnSimpan.setOnClickListener { saveProfile() }
    }

    private fun openImagePicker() {
        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val user = gamificationRepository.getUser(uid) ?: return@launch
            binding.etNama.setText(user.nama)
            binding.etEmail.setText(user.email)
            currentAvatar = user.avatar
            if (user.avatar.isNotBlank()) {
                binding.ivAvatar.visibility = View.VISIBLE
                binding.tvAvatar.visibility = View.GONE
                Glide.with(this@EditProfilActivity)
                    .load(user.avatar)
                    .circleCrop()
                    .into(binding.ivAvatar)
            } else {
                showInitial(user.nama)
            }
        }
    }

    private fun showInitial(nama: String) {
        binding.ivAvatar.visibility = View.GONE
        binding.tvAvatar.visibility = View.VISIBLE
        binding.tvAvatar.text = if (nama.isNotEmpty()) nama.take(1).uppercase() else "R"
    }

    private fun saveProfile() {
        val nama = binding.etNama.text.toString().trim()
        if (nama.isEmpty()) {
            Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSimpan.isEnabled = false

        lifecycleScope.launch {
            val uid = auth.currentUser?.uid
            var finalAvatar = currentAvatar

            if (uid != null && pickedAvatarUri != null) {
                val url = authRepository.uploadAvatar(this@EditProfilActivity, pickedAvatarUri!!, uid)
                if (url == null) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSimpan.isEnabled = true
                    Toast.makeText(this@EditProfilActivity, R.string.gagal_unggah_foto, Toast.LENGTH_LONG).show()
                    return@launch
                }
                finalAvatar = url
            }

            val error = authRepository.updateProfil(nama, finalAvatar)
            binding.progressBar.visibility = View.GONE
            binding.btnSimpan.isEnabled = true

            if (error == null) {
                Toast.makeText(this@EditProfilActivity, R.string.profil_diperbarui, Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@EditProfilActivity, error, Toast.LENGTH_SHORT).show()
            }
        }
    }
}