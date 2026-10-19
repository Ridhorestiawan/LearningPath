package com.ridho.learningpath.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun register(nama: String, email: String, password: String, role: String): String? {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return "Gagal membuat akun"
            val user = mapOf(
                "nama" to nama,
                "email" to email,
                "role" to role,
                "avatar" to "",
                "level" to 1,
                "totalPoin" to 0
            )
            db.collection(COLLECTION_USERS).document(uid).set(user).await()
            null
        } catch (e: Exception) {
            e.message ?: "Terjadi kesalahan"
        }
    }

    suspend fun login(email: String, password: String): String? {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            null
        } catch (e: Exception) {
            e.message ?: "Login gagal"
        }
    }

    suspend fun resetPassword(email: String): String? {
        return try {
            auth.sendPasswordResetEmail(email).await()
            null
        } catch (e: Exception) {
            e.message ?: "Gagal mengirim link reset"
        }
    }

    suspend fun updateProfil(nama: String, avatar: String): String? {
        val uid = auth.currentUser?.uid ?: return "Tidak ada pengguna login"
        return try {
            db.collection(COLLECTION_USERS).document(uid)
                .update("nama", nama, "avatar", avatar).await()
            null
        } catch (e: Exception) {
            e.message ?: "Gagal memperbarui profil"
        }
    }

    /**
     * Mengunggah foto profil ke Firebase Storage dengan path `profile/{uid}/avatar.jpg`.
     * Gambar di-resize agar tidak terlalu besar. Mengembalikan URL unduhan, atau `null` jika gagal.
     */
    suspend fun uploadAvatar(context: Context, uri: Uri, uid: String): String? {
        return try {
            val bitmap = decodeScaledBitmap(context, uri, 512)
            val bytes = ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                out.toByteArray()
            }
            if (!bitmap.isRecycled) bitmap.recycle()
            val ref = FirebaseStorage.getInstance().reference
                .child("profile/$uid/avatar.jpg")
            ref.putBytes(bytes).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            e.message ?: "Gagal mengunggah foto"
        }
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri, maxSize: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= maxSize &&
            bounds.outHeight / (sampleSize * 2) >= maxSize
        ) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw IllegalStateException("Gambar tidak bisa dibaca")

        val maxDim = maxOf(decoded.width, decoded.height)
        if (maxDim <= maxSize) return decoded

        val newW = decoded.width * maxSize / maxDim
        val newH = decoded.height * maxSize / maxDim
        val scaled = Bitmap.createScaledBitmap(decoded, newW, newH, true)
        if (scaled != decoded) decoded.recycle()
        return scaled
    }

    fun logout() {
        auth.signOut()
    }

    companion object {
        const val COLLECTION_USERS = "users"
    }
}
