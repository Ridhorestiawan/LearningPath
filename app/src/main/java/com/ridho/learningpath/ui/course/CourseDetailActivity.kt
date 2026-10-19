package com.ridho.learningpath.ui.course

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.ridho.learningpath.R
import com.ridho.learningpath.data.model.Module
import com.ridho.learningpath.data.repository.CourseRepository
import com.ridho.learningpath.data.repository.GamificationRepository
import com.ridho.learningpath.databinding.ActivityCourseDetailBinding
import kotlinx.coroutines.launch

class CourseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCourseDetailBinding
    private val auth = FirebaseAuth.getInstance()
    private val courseRepository = CourseRepository()
    private val gamificationRepository = GamificationRepository()
    private lateinit var adapter: ModuleAdapter
    private var completedIds = setOf<String>()

    companion object {
        const val EXTRA_COURSE_ID = "course_id"
        const val EXTRA_COURSE_NAME = "course_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val courseId = intent.getStringExtra(EXTRA_COURSE_ID)
        if (courseId.isNullOrBlank()) {
            finish()
            return
        }

        binding = ActivityCourseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val courseName = intent.getStringExtra(EXTRA_COURSE_NAME) ?: "Mata Pelajaran"
        binding.toolbar.title = courseName
        binding.tvTitle.text = courseName
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = ModuleAdapter { module -> openModule(module) }
        binding.rvModules.layoutManager = LinearLayoutManager(this)
        binding.rvModules.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadModules()
    }

    private fun loadModules() {
        val courseId = intent.getStringExtra(EXTRA_COURSE_ID) ?: return
        binding.progressLoading.visibility = View.VISIBLE
        binding.tvError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val uid = auth.currentUser?.uid
                if (uid == null) {
                    binding.tvError.visibility = View.VISIBLE
                    binding.progressLoading.visibility = View.GONE
                    return@launch
                }

                completedIds = gamificationRepository.getCompletedModuleIds(uid)
                val modules = courseRepository.getModules(courseId)

                binding.progressLoading.visibility = View.GONE

                if (modules.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.rvModules.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.rvModules.visibility = View.VISIBLE
                }

                val done = modules.count { it.id in completedIds }
                val total = modules.size
                val percent = if (total > 0) done * 100 / total else 0

                binding.tvProgressInfo.text = getString(R.string.x_of_y_modul_selesai, done, total)
                binding.progressBar.progress = percent
                binding.tvPercent.text = "$percent%"

                adapter.submit(modules, completedIds)
            } catch (e: Exception) {
                binding.progressLoading.visibility = View.GONE
                binding.rvModules.visibility = View.GONE
                binding.tvError.visibility = View.VISIBLE
            }
        }
    }

    private fun openModule(module: Module) {
        val intent = Intent(this, ModuleDetailActivity::class.java).apply {
            putExtra(ModuleDetailActivity.EXTRA_MODULE_ID, module.id)
            putExtra(ModuleDetailActivity.EXTRA_JUDUL, module.judulMateri)
            putExtra(ModuleDetailActivity.EXTRA_KONTEN, module.konten)
            putExtra(ModuleDetailActivity.EXTRA_XP, module.xp)
            putExtra(ModuleDetailActivity.EXTRA_IS_COMPLETED, module.id in completedIds)
            putExtra(ModuleDetailActivity.EXTRA_FILE_NAME, module.fileName)
            putExtra(ModuleDetailActivity.EXTRA_FILE_URL, module.fileUrl)
            putExtra(ModuleDetailActivity.EXTRA_VIDEO_URL, module.videoUrl)
        }
        startActivity(intent)
    }
}

class ModuleAdapter(
    private val onClick: (Module) -> Unit
) : RecyclerView.Adapter<ModuleAdapter.ModuleViewHolder>() {

    private val modules = mutableListOf<Module>()
    private val completedIds = mutableSetOf<String>()

    fun submit(newModules: List<Module>, newCompleted: Set<String>) {
        modules.clear()
        modules.addAll(newModules)
        completedIds.clear()
        completedIds.addAll(newCompleted)
        notifyDataSetChanged()
    }

    class ModuleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUrutan: TextView = view.findViewById(R.id.tvUrutan)
        val tvJudul: TextView = view.findViewById(R.id.tvJudulModul)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvXp: TextView = view.findViewById(R.id.tvXp)
        val tvIconStatus: TextView = view.findViewById(R.id.tvIconStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_module, parent, false)
        return ModuleViewHolder(view)
    }

    override fun getItemCount(): Int = modules.size

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        val module = modules[position]
        val isCompleted = module.id in completedIds

        holder.tvUrutan.text = module.urutan.toString()
        holder.tvJudul.text = module.judulMateri
        holder.tvXp.text = "+${module.xp} XP"

        if (isCompleted) {
            holder.tvStatus.text = holder.itemView.context.getString(R.string.selesai)
            holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.success))
            holder.tvIconStatus.text = "\u2713"
            holder.tvIconStatus.setTextColor(holder.itemView.context.getColor(R.color.success))
            holder.tvUrutan.setBackgroundResource(R.color.success)
        } else {
            holder.tvStatus.text = holder.itemView.context.getString(R.string.belum_dikerjakan)
            holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.gray))
            holder.tvIconStatus.text = "\u25CB"
            holder.tvIconStatus.setTextColor(holder.itemView.context.getColor(R.color.gray))
            holder.tvUrutan.setBackgroundResource(R.color.primary)
        }

        holder.itemView.setOnClickListener { onClick(module) }
    }
}
