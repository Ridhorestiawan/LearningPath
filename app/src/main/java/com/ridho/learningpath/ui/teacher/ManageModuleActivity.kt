package com.ridho.learningpath.ui.teacher

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.ridho.learningpath.R
import com.ridho.learningpath.data.model.Course
import com.ridho.learningpath.data.model.Module
import com.ridho.learningpath.data.repository.CourseRepository
import com.ridho.learningpath.databinding.ActivityManageModuleBinding
import com.ridho.learningpath.databinding.DialogAddModuleBinding
import kotlinx.coroutines.launch

class ManageModuleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageModuleBinding
    private val auth = FirebaseAuth.getInstance()
    private val courseRepository = CourseRepository()
    private var courses = listOf<Course>()
    private var selectedCourseId: String? = null
    private lateinit var adapter: ManageModuleAdapter

    private var activeDialogBinding: DialogAddModuleBinding? = null
    private var pickedFileUri: Uri? = null
    private var pickedFileName: String? = null
    private var existingFileName = ""
    private var existingFileUrl = ""
    private var clearedExistingFile = false

    private val filePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val dialogBinding = activeDialogBinding ?: return@registerForActivityResult
        if (uri != null) {
            pickedFileUri = uri
            pickedFileName = queryDisplayName(uri)
            dialogBinding.tvFileName.text = pickedFileName
            dialogBinding.tvFileName.setTextColor(getColor(R.color.black))
            dialogBinding.btnHapusFile.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageModuleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = ManageModuleAdapter(
            onEdit = { module -> showModuleDialog(module) },
            onDelete = { module -> confirmDelete(module) }
        )
        binding.rvModules.layoutManager = LinearLayoutManager(this)
        binding.rvModules.adapter = adapter

        binding.fabAdd.setOnClickListener { showModuleDialog(null) }

        loadCourses()
    }

    private fun loadCourses() {
        val uid = auth.currentUser?.uid
        lifecycleScope.launch {
            courses = courseRepository.getCourses(uid)

            if (courses.isEmpty()) {
                binding.tvEmpty.text = getString(R.string.mapel_kosong)
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvModules.visibility = View.GONE
                return@launch
            }

            val names = courses.map { it.namaMapel }
            val spinnerAdapter = ArrayAdapter(this@ManageModuleActivity, android.R.layout.simple_spinner_item, names)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCourse.adapter = spinnerAdapter
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = getString(R.string.pilih_mapel_untuk_melihat_modul)

            binding.spinnerCourse.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedCourseId = courses.getOrNull(position)?.id
                    loadModules()
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })
        }
    }

    private fun loadModules() {
        val courseId = selectedCourseId ?: return
        lifecycleScope.launch {
            val modules = courseRepository.getModules(courseId)
            binding.tvEmpty.visibility = if (modules.isEmpty()) View.VISIBLE else View.GONE
            binding.tvEmpty.text = getString(R.string.belum_ada_modul)
            binding.rvModules.visibility = if (modules.isEmpty()) View.GONE else View.VISIBLE
            adapter.submit(modules)
        }
    }

    private fun showModuleDialog(module: Module?) {
        if (module == null) {
            val courseId = selectedCourseId
            if (courseId == null) {
                Toast.makeText(this, R.string.pilih_mapel_dulu, Toast.LENGTH_SHORT).show()
                return
            }
        }

        pickedFileUri = null
        pickedFileName = null
        clearedExistingFile = false
        existingFileName = module?.fileName.orEmpty()
        existingFileUrl = module?.fileUrl.orEmpty()

        val dialogBinding = DialogAddModuleBinding.inflate(layoutInflater)
        activeDialogBinding = dialogBinding
        var moduleId: String? = null

        if (module != null) {
            moduleId = module.id
            dialogBinding.etJudul.setText(module.judulMateri)
            dialogBinding.etUrutan.setText(module.urutan.toString())
            dialogBinding.etXp.setText(module.xp.toString())
            dialogBinding.etKonten.setText(module.konten)
            dialogBinding.etVideoUrl.setText(module.videoUrl)
            if (module.fileName.isNotBlank()) {
                dialogBinding.tvFileName.text = module.fileName
                dialogBinding.btnHapusFile.visibility = View.VISIBLE
            }
        }

        dialogBinding.btnPilihFile.setOnClickListener {
            filePicker.launch(arrayOf("*/*"))
        }

        dialogBinding.btnHapusFile.setOnClickListener {
            pickedFileUri = null
            pickedFileName = null
            if (existingFileName.isNotBlank()) clearedExistingFile = true
            dialogBinding.tvFileName.text = ""
            dialogBinding.btnHapusFile.visibility = View.GONE
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (module == null) R.string.tambah_modul else R.string.edit_modul)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.simpan, null)
            .setNegativeButton(R.string.batal, null)
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val judul = dialogBinding.etJudul.text.toString().trim()
                val urutan = dialogBinding.etUrutan.text.toString().toIntOrNull() ?: 0
                val xp = dialogBinding.etXp.text.toString().toIntOrNull() ?: 0
                val konten = dialogBinding.etKonten.text.toString().trim()
                val videoUrl = dialogBinding.etVideoUrl.text.toString().trim()
                val courseId = selectedCourseId ?: return@setOnClickListener

                if (judul.isEmpty()) {
                    dialogBinding.etJudul.error = getString(R.string.judul_wajib)
                    return@setOnClickListener
                }
                if (videoUrl.isNotBlank() && !videoUrl.startsWith("http://") && !videoUrl.startsWith("https://")) {
                    dialogBinding.etVideoUrl.error = getString(R.string.link_video_tidak_valid)
                    return@setOnClickListener
                }

                saveButton.isEnabled = false
                saveModule(
                    moduleId, courseId, judul, urutan, konten, xp, videoUrl,
                    onComplete = {
                        dialog.dismiss()
                        loadModules()
                    },
                    onError = { message ->
                        saveButton.isEnabled = true
                        Toast.makeText(this@ManageModuleActivity, message, Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
        dialog.setOnDismissListener { activeDialogBinding = null }
        dialog.show()
    }

    private fun saveModule(
        moduleId: String?,
        courseId: String,
        judul: String,
        urutan: Int,
        konten: String,
        xp: Int,
        videoUrl: String,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        lifecycleScope.launch {
            val hasNewFile = pickedFileUri != null && pickedFileName != null

            var targetId = moduleId
            if (targetId == null) {
                targetId = courseRepository.addModule(courseId, judul, urutan, konten, xp, videoUrl)
                if (targetId == null) {
                    onError(getString(R.string.gagal_tambah_modul))
                    return@launch
                }
            }

            var finalName = if (clearedExistingFile) "" else existingFileName
            var finalUrl = if (clearedExistingFile) "" else existingFileUrl

            if (hasNewFile) {
                val url = courseRepository.uploadModuleFile(
                    this@ManageModuleActivity, pickedFileUri!!, targetId, pickedFileName!!
                )
                if (url == null) {
                    if (moduleId == null) {
                        courseRepository.deleteModule(targetId, "")
                    }
                    onError(getString(R.string.gagal_unggah))
                    return@launch
                }
                finalUrl = url
                finalName = pickedFileName!!
            }

            val error = if (moduleId == null) {
                courseRepository.updateModuleFile(targetId, finalName, finalUrl)
            } else {
                courseRepository.updateModule(moduleId, judul, urutan, konten, xp, finalName, finalUrl, videoUrl)
            }

            if (error == null) {
                onComplete()
            } else {
                onError(error)
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String {
        var name: String? = null
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) name = cursor.getString(0)
        }
        return name ?: uri.lastPathSegment ?: "file"
    }

    private fun confirmDelete(module: Module) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.hapus_modul)
            .setMessage(getString(R.string.konfirmasi_hapus_modul, module.judulMateri))
            .setPositiveButton(R.string.hapus) { _, _ ->
                lifecycleScope.launch {
                    val error = courseRepository.deleteModule(module.id, module.fileName)
                    if (error == null) {
                        loadModules()
                    } else {
                        Toast.makeText(this@ManageModuleActivity, error, Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton(R.string.batal, null)
            .show()
    }
}

class ManageModuleAdapter(
    private val onEdit: (Module) -> Unit,
    private val onDelete: (Module) -> Unit
) : RecyclerView.Adapter<ManageModuleAdapter.ViewHolder>() {

    private val modules = mutableListOf<Module>()

    fun submit(newModules: List<Module>) {
        modules.clear()
        modules.addAll(newModules.sortedBy { it.urutan })
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = modules.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUrutan: TextView = view.findViewById(R.id.tvUrutan)
        val tvJudul: TextView = view.findViewById(R.id.tvJudul)
        val tvInfo: TextView = view.findViewById(R.id.tvInfo)
        val tvEdit: TextView = view.findViewById(R.id.tvEdit)
        val tvDelete: TextView = view.findViewById(R.id.tvDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManageModuleAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage_module, parent, false)
        return ManageModuleAdapter.ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ManageModuleAdapter.ViewHolder, position: Int) {
        val module = modules[position]
        holder.tvUrutan.text = module.urutan.toString()
        holder.tvJudul.text = module.judulMateri
        holder.tvInfo.text = "+${module.xp} XP"
        holder.tvEdit.setOnClickListener { onEdit(module) }
        holder.tvDelete.setOnClickListener { onDelete(module) }
    }
}