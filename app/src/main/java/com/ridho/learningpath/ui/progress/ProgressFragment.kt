package com.ridho.learningpath.ui.progress

import android.content.Intent
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
import com.ridho.learningpath.data.repository.CourseRepository
import com.ridho.learningpath.data.repository.GamificationRepository
import com.ridho.learningpath.databinding.FragmentProgressBinding
import com.ridho.learningpath.ui.course.CourseDetailActivity
import kotlinx.coroutines.launch

class ProgressFragment : Fragment() {

    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val courseRepository = CourseRepository()
    private val gamificationRepository = GamificationRepository()
    private lateinit var adapter: MapelProgressAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MapelProgressAdapter { course ->
            val intent = Intent(requireContext(), CourseDetailActivity::class.java)
            intent.putExtra(CourseDetailActivity.EXTRA_COURSE_ID, course.id)
            intent.putExtra(CourseDetailActivity.EXTRA_COURSE_NAME, course.namaMapel)
            startActivity(intent)
        }
        binding.rvProgress.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProgress.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadProgress()
    }

    private fun loadProgress() {
        lifecycleScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val user = gamificationRepository.getUser(uid)
            val courses = courseRepository.getCourses()
            val completedIds = gamificationRepository.getCompletedModuleIds(uid)
            val items = mutableListOf<MapelProgressItem>()

            for (course in courses) {
                val modules = courseRepository.getModules(course.id)
                val done = modules.count { it.id in completedIds }
                val total = modules.size
                val percent = if (total > 0) done * 100 / total else 0
                items.add(MapelProgressItem(course, percent, done, total))
            }

            binding.tvStatXp.text = "${user?.totalPoin ?: 0}"
            binding.tvStatModul.text = completedIds.size.toString()
            binding.tvStatLevel.text = "${user?.level ?: 1}"

            if (items.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvProgress.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvProgress.visibility = View.VISIBLE
                adapter.submit(items)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class MapelProgressItem(
    val course: Course,
    val percent: Int,
    val doneModules: Int,
    val totalModules: Int
)

class MapelProgressAdapter(
    private val onClick: (Course) -> Unit
) : RecyclerView.Adapter<MapelProgressAdapter.ViewHolder>() {

    private val items = mutableListOf<MapelProgressItem>()

    fun submit(newItems: List<MapelProgressItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvInfo: TextView = view.findViewById(R.id.tvInfo)
        val tvPercent: TextView = view.findViewById(R.id.tvPercent)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_progress_mapel, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.course.namaMapel
        holder.tvInfo.text = "${item.doneModules}/${item.totalModules} modul selesai"
        holder.tvPercent.text = "${item.percent}%"
        holder.progressBar.progress = item.percent
        holder.itemView.setOnClickListener { onClick(item.course) }
    }
}
