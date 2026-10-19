package com.ridho.learningpath.ui.course

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.ridho.learningpath.R
import com.ridho.learningpath.data.repository.GamificationRepository
import com.ridho.learningpath.databinding.ActivityModuleDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class ModuleDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModuleDetailBinding
    private val gamificationRepository = GamificationRepository()

    companion object {
        const val EXTRA_MODULE_ID = "module_id"
        const val EXTRA_JUDUL = "module_judul"
        const val EXTRA_KONTEN = "module_konten"
        const val EXTRA_XP = "module_xp"
        const val EXTRA_IS_COMPLETED = "module_is_completed"
        const val EXTRA_FILE_NAME = "module_file_name"
        const val EXTRA_FILE_URL = "module_file_url"
        const val EXTRA_VIDEO_URL = "module_video_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModuleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val moduleId = intent.getStringExtra(EXTRA_MODULE_ID) ?: run { finish(); return }
        val judul = intent.getStringExtra(EXTRA_JUDUL) ?: ""
        val konten = intent.getStringExtra(EXTRA_KONTEN).orEmpty()
        val xp = intent.getIntExtra(EXTRA_XP, 50)
        val isCompleted = intent.getBooleanExtra(EXTRA_IS_COMPLETED, false)
        val fileName = intent.getStringExtra(EXTRA_FILE_NAME).orEmpty()
        val fileUrl = intent.getStringExtra(EXTRA_FILE_URL).orEmpty()
        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL).orEmpty()

        binding.toolbar.setTitle(judul)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.tvJudul.text = judul
        binding.tvXp.text = getString(R.string.xp_reward, xp)
        binding.tvKonten.text = konten

        if (fileUrl.isNotBlank()) {
            binding.btnBukaFile.visibility = android.view.View.VISIBLE
            binding.btnBukaFile.setOnClickListener { openModuleFile(moduleId, fileName, fileUrl) }
        }
        if (videoUrl.isNotBlank()) {
            binding.btnTontonVideo.visibility = android.view.View.VISIBLE
            binding.btnTontonVideo.setOnClickListener { openVideoLink(videoUrl) }
        }

        if (isCompleted) {
            setCompletedState()
        }

        binding.btnSelesai.setOnClickListener {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Snackbar.make(binding.root, R.string.login_dulu, Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            binding.btnSelesai.isEnabled = false
            lifecycleScope.launch {
                val awarded = try {
                    gamificationRepository.completeModule(uid, moduleId, xp)
                } catch (e: Exception) {
                    false
                }
                if (awarded) {
                    Snackbar.make(binding.root, getString(R.string.xp_awarded, xp), Snackbar.LENGTH_LONG).show()
                    setCompletedState()
                } else {
                    binding.btnSelesai.isEnabled = true
                    Snackbar.make(binding.root, R.string.gagal_selesai, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun openModuleFile(moduleId: String, fileName: String, fileUrl: String) {
        if (fileUrl.startsWith("http")) {
            binding.btnBukaFile.isEnabled = false
            lifecycleScope.launch {
                val downloaded = withContext(Dispatchers.IO) {
                    downloadToCache(moduleId, fileName, fileUrl)
                }
                binding.btnBukaFile.isEnabled = true
                if (downloaded == null) {
                    Snackbar.make(binding.root, R.string.file_tidak_terbaca, Snackbar.LENGTH_LONG).show()
                    return@launch
                }
                viewFile(downloaded)
            }
        } else {
            openExternal(fileUrl)
        }
    }

    private fun downloadToCache(moduleId: String, fileName: String, fileUrl: String): File? {
        return try {
            val safeName = fileName.replace(Regex("[/\\\\:*?\"<>|]"), "_").ifBlank { "materi" }
            val dir = File(cacheDir, "modules").apply { mkdirs() }
            val target = File(dir, "${moduleId}_$safeName")
            if (!target.exists()) {
                URL(fileUrl).openStream().use { input ->
                    FileOutputStream(target).use { output -> input.copyTo(output) }
                }
            }
            target
        } catch (e: Exception) {
            null
        }
    }

    private fun viewFile(file: File) {
        try {
            val ext = file.extension.lowercase()
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(binding.root, R.string.file_tidak_terbaca, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun openVideoLink(videoUrl: String) {
        openExternal(videoUrl)
    }

    private fun openExternal(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(binding.root, R.string.video_tidak_terbuka, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun setCompletedState() {
        binding.btnSelesai.isEnabled = false
        binding.btnSelesai.text = getString(R.string.sudah_selesai)
        binding.btnSelesai.icon = null
        binding.tvXp.text = getString(
            R.string.xp_reward,
            intent.getIntExtra(EXTRA_XP, 50)
        )
        setResult(RESULT_OK)
    }
}