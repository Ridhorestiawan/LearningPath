package com.ridho.learningpath.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.ridho.learningpath.R
import com.ridho.learningpath.data.model.Achievement
import com.ridho.learningpath.data.repository.AuthRepository
import com.ridho.learningpath.data.repository.GamificationRepository
import com.ridho.learningpath.databinding.FragmentProfileBinding
import com.ridho.learningpath.ui.auth.LoginActivity
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val gamificationRepository = GamificationRepository()
    private lateinit var badgeAdapter: BadgeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        badgeAdapter = BadgeAdapter()
        binding.rvBadges.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvBadges.adapter = badgeAdapter

        loadProfile()
        binding.btnEditProfil.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfilActivity::class.java))
        }
        binding.btnLogout.setOnClickListener {
            AuthRepository().logout()
            startActivity(Intent(requireContext(), LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val user = gamificationRepository.getUser(uid) ?: return@launch

            if (user.avatar.isNotBlank()) {
                binding.ivAvatar.visibility = View.VISIBLE
                binding.tvAvatar.visibility = View.GONE
                Glide.with(this@ProfileFragment)
                    .load(user.avatar)
                    .circleCrop()
                    .into(binding.ivAvatar)
            } else {
                binding.ivAvatar.visibility = View.GONE
                binding.tvAvatar.visibility = View.VISIBLE
                binding.tvAvatar.text = if (user.nama.isNotEmpty()) user.nama.take(1).uppercase() else "R"
            }
            binding.tvNama.text = user.nama
            binding.tvRole.text = if (user.isGuru) "Guru" else "Siswa"
            binding.tvLevel.text = user.level.toString()
            binding.tvXp.text = user.totalPoin.toString()

            val allBadges = gamificationRepository.getAchievements()
            val ownedIds = gamificationRepository.getUserAchievementIds(uid)
            binding.tvBadgeCount.text = ownedIds.size.toString()

            val badgeItems = allBadges.map { a ->
                BadgeItem(a, a.id in ownedIds)
            }
            badgeAdapter.submit(badgeItems)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class BadgeItem(
    val achievement: Achievement,
    val owned: Boolean
)

class BadgeAdapter : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    private val items = mutableListOf<BadgeItem>()

    fun submit(newItems: List<BadgeItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class BadgeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvBadgeIcon)
        val tvNama: TextView = view.findViewById(R.id.tvBadgeNama)
        val tvSyarat: TextView = view.findViewById(R.id.tvBadgeSyarat)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_badge, parent, false)
        return BadgeViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val item = items[position]
        holder.tvNama.text = item.achievement.namaBadge
        holder.tvSyarat.text = "Min. ${item.achievement.syaratPoin} XP"

        if (!item.owned) {
            holder.tvIcon.alpha = 0.25f
            holder.tvNama.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
            holder.tvIcon.text = "🔒"
        } else {
            holder.tvIcon.alpha = 1f
            holder.tvNama.setTextColor(holder.itemView.context.getColor(com.ridho.learningpath.R.color.black))
            holder.tvIcon.text = "🏅"
        }
    }
}
