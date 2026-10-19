package com.ridho.learningpath.ui.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.ridho.learningpath.R
import com.ridho.learningpath.data.model.Course
import com.ridho.learningpath.data.repository.CourseRepository
import com.ridho.learningpath.data.repository.GamificationRepository
import com.ridho.learningpath.databinding.ActivityStudentProgressBinding
import kotlinx.coroutines.launch

class StudentProgressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentProgressBinding
    private val courseRepository = CourseRepository()
    private val gamificationRepository = GamificationRepository()
    private lateinit var adapter: StudentProgressAdapter

    companion object {
        const val EXTRA_STUDENT_ID = "student_id"
        const val EXTRA_STUDENT_NAME = "student_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val studentId = intent.getStringExtra(EXTRA_STUDENT_ID)
        if (studentId.isNullOrBlank()) {
            finish()
            return
        }

        binding = ActivityStudentProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val studentName = intent.getStringExtra(EXTRA_STUDENT_NAME) ?: "Siswa"
        binding.toolbar.title = studentName
        binding.tvNama.text = studentName
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = StudentProgressAdapter()
        binding.rvStudentProgress.layoutManager = LinearLayoutManager(this)
        binding.rvStudentProgress.adapter = adapter

        loadProgress(studentId)
    }

    private fun loadProgress(studentId: String) {
        binding.progressLoading.visibility = View.VISIBLE
        binding.tvError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val courses = courseRepository.getCourses()
                val completedIds = gamificationRepository.getCompletedModuleIds(studentId)
                val items = mutableListOf<StudentProgressItem>()

                for (course in courses) {
                    val modules = courseRepository.getModules(course.id)
                    val done = modules.count { it.id in completedIds }
                    val total = modules.size
                    val percent = if (total > 0) done * 100 / total else 0
                    items.add(StudentProgressItem(course, percent, done, total))
                }

                binding.progressLoading.visibility = View.GONE

                if (items.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.rvStudentProgress.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.rvStudentProgress.visibility = View.VISIBLE
                    adapter.submit(items)
                }
            } catch (e: Exception) {
                binding.progressLoading.visibility = View.GONE
                binding.rvStudentProgress.visibility = View.GONE
                binding.tvError.visibility = View.VISIBLE
            }
        }
    }
}

data class StudentProgressItem(
    val course: Course,
    val percent: Int,
    val doneModules: Int,
    val totalModules: Int
)

class StudentProgressAdapter : RecyclerView.Adapter<StudentProgressAdapter.ViewHolder>() {

    private val items = mutableListOf<StudentProgressItem>()

    fun submit(newItems: List<StudentProgressItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvInfo: TextView = view.findViewById(R.id.tvInfo)
        val progressBar: LinearProgressIndicator = view.findViewById(R.id.progressBar)
        val tvPercent: TextView = view.findViewById(R.id.tvPercent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_progress, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.course.namaMapel
        holder.tvInfo.text = "${item.doneModules}/${item.totalModules} modul selesai"
        holder.progressBar.progress = item.percent
        holder.tvPercent.text = "${item.percent}%"
    }
}