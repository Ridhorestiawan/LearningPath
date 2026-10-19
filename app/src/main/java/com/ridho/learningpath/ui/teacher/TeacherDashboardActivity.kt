package com.ridho.learningpath.ui.teacher

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.ridho.learningpath.R
import com.ridho.learningpath.data.model.User
import com.ridho.learningpath.data.repository.CourseRepository
import com.ridho.learningpath.data.repository.GamificationRepository
import com.ridho.learningpath.databinding.ActivityTeacherDashboardBinding
import com.ridho.learningpath.databinding.DialogAddCourseBinding
import kotlinx.coroutines.launch

class TeacherDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherDashboardBinding
    private val auth = FirebaseAuth.getInstance()
    private val gamificationRepository = GamificationRepository()
    private val courseRepository = CourseRepository()
    private lateinit var adapter: StudentAdapter
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = StudentAdapter { student ->
            startActivity(
                Intent(this, StudentProgressActivity::class.java)
                    .putExtra(StudentProgressActivity.EXTRA_STUDENT_ID, student.id)
                    .putExtra(StudentProgressActivity.EXTRA_STUDENT_NAME, student.nama)
            )
        }
        binding.rvStudents.layoutManager = LinearLayoutManager(this)
        binding.rvStudents.adapter = adapter

        binding.btnManageModule.setOnClickListener {
            startActivity(Intent(this, ManageModuleActivity::class.java))
        }

        binding.fabAddMapel.setOnClickListener { showAddCourseDialog() }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s?.toString() ?: "")
            }
        })

        loadData()
    }

    private fun loadData() {
        val uid = auth.currentUser?.uid
        refreshModuleCount(uid)

        listener = gamificationRepository.getStudentListener(
            onChange = { students ->
                if (students.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvError.visibility = View.GONE
                    binding.rvStudents.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.rvStudents.visibility = View.VISIBLE
                    adapter.submit(students)
                }

                binding.tvTotalSiswa.text = students.size.toString()
                val avg = if (students.isNotEmpty()) students.sumOf { it.totalPoin } / students.size else 0
                binding.tvAvgProgress.text = "$avg XP"
            },
            onError = {
                binding.tvError.visibility = View.VISIBLE
                binding.rvStudents.visibility = View.GONE
            }
        )
    }

    private fun refreshModuleCount(uid: String?) {
        lifecycleScope.launch {
            val courses = courseRepository.getCourses(uid)
            var totalModules = 0L
            for (course in courses) {
                totalModules += courseRepository.getModules(course.id).size
            }
            binding.tvTotalModul.text = totalModules.toString()
        }
    }

    private fun showAddCourseDialog() {
        val dialogBinding = DialogAddCourseBinding.inflate(layoutInflater)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.tambah_mapel)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.simpan, null)
            .setNegativeButton(R.string.batal, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val nama = dialogBinding.etMapel.text.toString().trim()
                val deskripsi = dialogBinding.etDeskripsi.text.toString().trim()
                val uid = auth.currentUser?.uid

                if (nama.isEmpty()) {
                    dialogBinding.etMapel.error = getString(R.string.nama_wajib)
                    return@setOnClickListener
                }
                if (uid == null) {
                    Snackbar.make(binding.root, R.string.login_dulu, Snackbar.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    val error = courseRepository.addCourse(nama, deskripsi, uid)
                    if (error == null) {
                        dialog.dismiss()
                        Snackbar.make(binding.root, R.string.mapel_ditambahkan, Snackbar.LENGTH_LONG).show()
                        refreshModuleCount(uid)
                    } else {
                        Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
        dialog.show()
    }

    override fun onDestroy() {
        listener?.remove()
        listener = null
        super.onDestroy()
    }
}

class StudentAdapter(
    private val onClick: (User) -> Unit
) : RecyclerView.Adapter<StudentAdapter.ViewHolder>() {

    private val allStudents = mutableListOf<User>()
    private val visibleStudents = mutableListOf<User>()

    fun submit(newStudents: List<User>) {
        allStudents.clear()
        allStudents.addAll(newStudents)
        visibleStudents.clear()
        visibleStudents.addAll(newStudents)
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        visibleStudents.clear()
        if (query.isBlank()) {
            visibleStudents.addAll(allStudents)
        } else {
            val q = query.lowercase()
            visibleStudents.addAll(allStudents.filter {
                it.nama.lowercase().contains(q) || it.email.lowercase().contains(q)
            })
        }
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAvatar: TextView = view.findViewById(R.id.tvAvatar)
        val tvNama: TextView = view.findViewById(R.id.tvNama)
        val tvInfo: TextView = view.findViewById(R.id.tvInfo)
        val tvXp: TextView = view.findViewById(R.id.tvXp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_row, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = visibleStudents.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = visibleStudents[position]
        holder.tvNama.text = user.nama
        holder.tvInfo.text = "Level ${user.level} • ${user.email}"
        holder.tvXp.text = "${user.totalPoin} XP"
        holder.tvAvatar.text = if (user.nama.isNotEmpty()) user.nama.take(1).uppercase() else "?"
        holder.itemView.setOnClickListener { onClick(user) }
    }
}