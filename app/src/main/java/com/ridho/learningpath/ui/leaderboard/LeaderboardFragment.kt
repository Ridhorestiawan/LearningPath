package com.ridho.learningpath.ui.leaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.ridho.learningpath.R
import com.ridho.learningpath.data.model.User
import com.ridho.learningpath.data.repository.GamificationRepository
import com.ridho.learningpath.databinding.FragmentLeaderboardBinding
import com.ridho.learningpath.ui.profile.ProfileFragment

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val gamificationRepository = GamificationRepository()
    private lateinit var adapter: LeaderboardAdapter
    private var listener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = LeaderboardAdapter()
        binding.rvLeaderboard.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLeaderboard.adapter = adapter

        listener = gamificationRepository.listenLeaderboard(
            onChange = { users ->
                if (users.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.rvLeaderboard.visibility = View.GONE
                    return@listenLeaderboard
                }
                binding.emptyState.visibility = View.GONE
                binding.rvLeaderboard.visibility = View.VISIBLE

                val myUid = auth.currentUser?.uid
                val myRank = users.indexOfFirst { it.id == myUid }.let { if (it >= 0) it + 1 else -1 }

                if (myUid != null) {
                    val me = users.firstOrNull { it.id == myUid }
                    binding.cardMyRank.visibility = View.VISIBLE
                    binding.tvMyRank.text = "#${if (myRank > 0) myRank else "—"}"
                    binding.tvMyName.text = me?.nama ?: "Nama Saya"
                    binding.tvMyXp.text = "${me?.totalPoin ?: 0} XP"
                } else {
                    binding.cardMyRank.visibility = View.GONE
                }

                adapter.submit(users)
            },
            onError = { binding.emptyState.visibility = View.VISIBLE }
        )
    }

    override fun onDestroyView() {
        listener?.remove()
        listener = null
        super.onDestroyView()
        _binding = null
    }
}

class LeaderboardAdapter : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    private val users = mutableListOf<User>()

    fun submit(newUsers: List<User>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvRank)
        val tvAvatar: TextView = view.findViewById(R.id.tvAvatar)
        val tvNama: TextView = view.findViewById(R.id.tvNama)
        val tvLevel: TextView = view.findViewById(R.id.tvLevel)
        val tvXp: TextView = view.findViewById(R.id.tvXp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = users.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        val rank = position + 1
        val context = holder.itemView.context
        holder.tvRank.text = rank.toString()
        holder.tvNama.text = user.nama
        holder.tvLevel.text = "Level ${user.level}"
        holder.tvXp.text = "${user.totalPoin} XP"
        holder.tvAvatar.text = if (user.nama.isNotEmpty()) user.nama.take(1).uppercase() else "?"

        val card = holder.itemView as? com.google.android.material.card.MaterialCardView

        // Medali untuk 3 besar, angka polos untuk sisanya
        when (rank) {
            1 -> {
                holder.tvRank.setBackgroundResource(R.drawable.bg_circle_primary)
                holder.tvRank.background.setTint(context.getColor(R.color.accent))
                holder.tvRank.setTextColor(context.getColor(R.color.white))
                card?.setCardBackgroundColor(context.getColor(R.color.secondary_container))
            }
            2 -> {
                holder.tvRank.setBackgroundResource(R.drawable.bg_circle_primary)
                holder.tvRank.background.setTint(context.getColor(R.color.medal_silver))
                holder.tvRank.setTextColor(context.getColor(R.color.white))
                card?.setCardBackgroundColor(context.getColor(R.color.surface_variant))
            }
            3 -> {
                holder.tvRank.setBackgroundResource(R.drawable.bg_circle_primary)
                holder.tvRank.background.setTint(context.getColor(R.color.medal_bronze))
                holder.tvRank.setTextColor(context.getColor(R.color.white))
                card?.setCardBackgroundColor(context.getColor(R.color.surface_variant))
            }
            else -> {
                holder.tvRank.background = null
                holder.tvRank.setTextColor(context.getColor(R.color.black))
                card?.setCardBackgroundColor(context.getColor(R.color.surface_variant))
            }
        }
    }
}
