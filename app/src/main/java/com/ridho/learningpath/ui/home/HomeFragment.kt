package com.ridho.learningpath.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.ridho.learningpath.R
import com.ridho.learningpath.data.model.Course
import com.ridho.learningpath.data.model.Module
import com.ridho.learningpath.data.repository.CourseRepository
import com.ridho.learningpath.data.repository.GamificationRepository
import com.ridho.learningpath.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val gamificationRepository = GamificationRepository()
    private val courseRepository = CourseRepository()
    private lateinit var adapter: CourseAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CourseAdapter { course -> openCourse(course) }
        binding.rvCourses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCourses.adapter = adapter

        loadUser()
        loadCourses()
    }

    private fun loadUser() {
        lifecycleScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val user = gamificationRepository.getUser(uid)
            binding.tvGreeting.text = "Halo, ${user?.nama ?: "Pelajar"}!"
            binding.tvLevelInfo.text = "Level ${user?.level ?: 1}  •  ${user?.totalPoin ?: 0} XP"
        }
    }

    private fun loadCourses() {
        lifecycleScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val courses = courseRepository.getCourses()
            val completedIds = gamificationRepository.getCompletedModuleIds(uid)
            val progressMap = mutableMapOf<String, Pair<Int, Int>>()

            for (course in courses) {
                val modules = courseRepository.getModules(course.id)
                val done = modules.count { it.id in completedIds }
                progressMap[course.id] = done to modules.size
            }

            if (courses.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvCourses.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.rvCourses.visibility = View.VISIBLE
                val items = courses.map { course ->
                    val (done, total) = progressMap[course.id] ?: (0 to 0)
                    val percent = if (total > 0) done * 100 / total else 0
                    CourseItem(course, percent, done, total)
                }
                adapter.submit(items)
            }
        }
    }

    private fun openCourse(course: Course) {
        (activity as? com.ridho.learningpath.ui.MainActivity)?.openCourse(course)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class CourseItem(
    val course: Course,
    val percent: Int,
    val doneModules: Int,
    val totalModules: Int
)

class CourseAdapter(
    private val onClick: (Course) -> Unit
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    private val items = mutableListOf<CourseItem>()

    fun submit(newItems: List<CourseItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class CourseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvCourseName)
        val tvInfo: TextView = view.findViewById(R.id.tvCourseInfo)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val tvPercent: TextView = view.findViewById(R.id.tvPercent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.course.namaMapel
        holder.tvInfo.text = "${item.doneModules}/${item.totalModules} modul selesai"
        holder.progressBar.progress = item.percent
        holder.tvPercent.text = "${item.percent}%"
        holder.itemView.setOnClickListener { onClick(item.course) }
    }
}
