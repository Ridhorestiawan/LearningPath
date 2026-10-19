package com.ridho.learningpath.data.model

data class Progress(
    val id: String = "",
    val userId: String = "",
    val moduleId: String = "",
    val status: String = "belum",
    val tanggalSelesai: Long = 0L
)
