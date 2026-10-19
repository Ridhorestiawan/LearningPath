package com.ridho.learningpath.data.model

data class User(
    val id: String = "",
    val nama: String = "",
    val email: String = "",
    val role: String = "siswa",
    val avatar: String = "",
    val level: Int = 1,
    val totalPoin: Int = 0
) {
    val isGuru: Boolean get() = role == "guru"
}
