package com.ridho.learningpath.data.model

data class Module(
    val id: String = "",
    val courseId: String = "",
    val judulMateri: String = "",
    val urutan: Int = 0,
    val konten: String = "",
    val xp: Int = 50,
    val fileName: String = "",
    val fileUrl: String = "",
    val videoUrl: String = ""
)
