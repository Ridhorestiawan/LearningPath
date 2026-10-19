package com.ridho.learningpath.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.ridho.learningpath.R
import com.ridho.learningpath.data.model.Course
import com.ridho.learningpath.data.repository.GamificationRepository
import com.ridho.learningpath.databinding.ActivityMainBinding
import com.ridho.learningpath.ui.auth.LoginActivity
import com.ridho.learningpath.ui.course.CourseDetailActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val auth = FirebaseAuth.getInstance()
    private val gamificationRepository by lazy { GamificationRepository() }

    private var isGuru = false
    private var teacherHomeOpened = false
    private var teacherNavScheduled = false

    private companion object {
        const val TEACHER_NAV_DELAY_MS = 3500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)

        binding.toolbar.title = getString(R.string.app_name)

        checkGuruAndNavigate()
    }

    override fun onResume() {
        super.onResume()
        if (isGuru && !teacherHomeOpened) openTeacherHome()
    }

    private fun checkGuruAndNavigate() {
        lifecycleScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val user = gamificationRepository.getUser(uid)
            Log.i("LP-DIAG", "checkGuru uid=$uid isGuru=${user?.isGuru}")
            if (user?.isGuru == true) {
                isGuru = true
                binding.bottomNav.visibility = View.GONE
                // Guru diarahkan ke layar Mode Guru; Dashboard Guru dibuka lewat tombol,
                // sehingga tidak ada auto-start activity kedua saat cold start.
                openTeacherHome()
            }
        }
    }

    private fun openTeacherHome() {
        if (teacherNavScheduled || teacherHomeOpened) return
        teacherNavScheduled = true
        // Transisi UI kodeksi LAMPAU dari jendela peluncuran: navigasi fragment ≤1 dtk setelah
        // tampil memicu force-stop oleh watchdog MIUI di perangkat ini. Tunda sampai app settle.
        binding.root.postDelayed({
            if (isFinishing || isDestroyed || !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                teacherNavScheduled = false
                return@postDelayed
            }
            teacherHomeOpened = true
            Log.i("LP-DIAG", "openTeacherHome navigate (delayed 3.5s)")
            navController.navigate(R.id.teacherHomeFragment)
        }, TEACHER_NAV_DELAY_MS)
    }

    fun openCourse(course: Course) {
        val intent = Intent(this, CourseDetailActivity::class.java)
        intent.putExtra(CourseDetailActivity.EXTRA_COURSE_ID, course.id)
        intent.putExtra(CourseDetailActivity.EXTRA_COURSE_NAME, course.namaMapel)
        startActivity(intent)
    }
}