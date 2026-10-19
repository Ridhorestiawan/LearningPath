package com.ridho.learningpath.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.ridho.learningpath.data.model.Course
import com.ridho.learningpath.data.model.Module
import kotlinx.coroutines.tasks.await

class CourseRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Mengambil daftar mapel. Jika [guruId] diberikan, hanya mapel milik guru itu yang dikembalikan.
     */
    suspend fun getCourses(guruId: String? = null): List<Course> {
        return try {
            var query: Query = db.collection(COLLECTION_COURSES)
            if (!guruId.isNullOrBlank()) {
                query = query.whereEqualTo("guruId", guruId)
            }
            query.get().await()
                .documents
                .mapNotNull { doc ->
                    val c = doc.toObject(Course::class.java)
                    if (c != null) c.copy(id = doc.id) else null
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getModules(courseId: String): List<Module> {
        return try {
            db.collection(COLLECTION_MODULES)
                .whereEqualTo("courseId", courseId)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val m = doc.toObject(Module::class.java)
                    if (m != null) m.copy(id = doc.id) else null
                }
                .sortedBy { it.urutan }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun listenCourses(onChange: (List<Course>) -> Unit, onError: (Exception) -> Unit): ListenerRegistration {
        return db.collection(COLLECTION_COURSES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val c = doc.toObject(Course::class.java)
                    if (c != null) c.copy(id = doc.id) else null
                } ?: emptyList()
                onChange(list)
            }
    }

    suspend fun addCourse(namaMapel: String, deskripsi: String, guruId: String): String? {
        return try {
            val data = mapOf(
                "namaMapel" to namaMapel,
                "deskripsi" to deskripsi,
                "guruId" to guruId
            )
            db.collection(COLLECTION_COURSES).add(data).await()
            null
        } catch (e: Exception) {
            e.message ?: "Gagal menambah mapel"
        }
    }

    /**
     * Menghapus mapel sekaligus semua modul di dalamnya.
     */
    suspend fun deleteCourse(courseId: String): String? {
        return try {
            val modules = getModules(courseId)
            for (m in modules) {
                db.collection(COLLECTION_MODULES).document(m.id).delete().await()
            }
            db.collection(COLLECTION_COURSES).document(courseId).delete().await()
            null
        } catch (e: Exception) {
            e.message ?: "Gagal menghapus mapel"
        }
    }

    /**
     * Membuat modul baru. Mengembalikan id dokumen yang baru dibuat, atau `null` jika gagal.
     */
    suspend fun addModule(
        courseId: String,
        judulMateri: String,
        urutan: Int,
        konten: String,
        xp: Int,
        videoUrl: String = ""
    ): String? {
        return try {
            val docRef = db.collection(COLLECTION_MODULES).document()
            docRef.set(
                mapOf(
                    "courseId" to courseId,
                    "judulMateri" to judulMateri,
                    "urutan" to urutan,
                    "konten" to konten,
                    "xp" to xp,
                    "videoUrl" to videoUrl
                )
            ).await()
            docRef.id
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Menyisipkan field file ke modul yang sudah ada (dipakai setelah upload file).
     */
    suspend fun updateModuleFile(moduleId: String, fileName: String, fileUrl: String): String? {
        return try {
            db.collection(COLLECTION_MODULES).document(moduleId)
                .update("fileName", fileName, "fileUrl", fileUrl)
                .await()
            null
        } catch (e: Exception) {
            e.message ?: "Gagal menyimpan file"
        }
    }

    suspend fun updateModule(
        moduleId: String,
        judulMateri: String,
        urutan: Int,
        konten: String,
        xp: Int,
        fileName: String = "",
        fileUrl: String = "",
        videoUrl: String = ""
    ): String? {
        return try {
            val data = mapOf(
                "judulMateri" to judulMateri,
                "urutan" to urutan,
                "konten" to konten,
                "xp" to xp,
                "fileName" to fileName,
                "fileUrl" to fileUrl,
                "videoUrl" to videoUrl
            )
            db.collection(COLLECTION_MODULES).document(moduleId).update(data).await()
            null
        } catch (e: Exception) {
            e.message ?: "Gagal memperbarui modul"
        }
    }

    /**
     * Mengunggah file materi ke Firebase Storage dengan path `modules/{moduleId}/{fileName}`.
     * Mengembalikan URL unduhan publik, atau `null` jika gagal.
     */
    suspend fun uploadModuleFile(context: Context, uri: Uri, moduleId: String, fileName: String): String? {
        return try {
            val safeName = sanitizeFileName(fileName)
            val ref = FirebaseStorage.getInstance().reference
                .child("modules/$moduleId/$safeName")
            val stream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("File tidak bisa dibaca")
            stream.use { input ->
                ref.putStream(input).await()
            }
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            e.message ?: "Gagal mengunggah file"
        }
    }

    /**
     * Menghapus modul beserta file materinya (jika ada) di Firebase Storage.
     */
    suspend fun deleteModule(moduleId: String, fileName: String = ""): String? {
        return try {
            db.collection(COLLECTION_MODULES).document(moduleId).delete().await()
            if (fileName.isNotBlank()) {
                try {
                    FirebaseStorage.getInstance().reference
                        .child("modules/$moduleId/${sanitizeFileName(fileName)}")
                        .delete()
                        .await()
                } catch (e: Exception) {
                    // file tidak ada di storage — abaikan
                }
            }
            null
        } catch (e: Exception) {
            e.message ?: "Gagal menghapus modul"
        }
    }

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("[/\\\\:*?\"<>|]"), "_")

    companion object {
        const val COLLECTION_COURSES = "courses"
        const val COLLECTION_MODULES = "modules"
    }
}