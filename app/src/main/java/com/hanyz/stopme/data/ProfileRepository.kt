package com.hanyz.stopme.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class StoredProfile(
    val name: String?,
    val photo: Bitmap?
)

// Profil disimpan di Firestore: users/{uid} -> { name, photoBase64 }
// Foto dikecilkan ke 256x256 JPEG agar muat di dokumen Firestore (batas 1 MB)
object ProfileRepository {
    private const val COLLECTION = "users"
    private const val PHOTO_SIZE = 256

    private fun uid(): String? = FirebaseAuth.getInstance().currentUser?.uid

    suspend fun load(): Result<StoredProfile> {
        val id = uid() ?: return Result.failure(Exception("Belum login"))
        return try {
            val doc = FirebaseFirestore.getInstance().collection(COLLECTION).document(id).get().await()
            val name = doc.getString("name")
            val photo = doc.getString("photoBase64")?.let { decodeBase64(it) }
            Result.success(StoredProfile(name, photo))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveName(name: String): Result<Unit> {
        val user = FirebaseAuth.getInstance().currentUser ?: return Result.failure(Exception("Belum login"))
        return try {
            // Nama juga disimpan di akun Firebase agar terbawa ke HP lain
            user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).await()
            FirebaseFirestore.getInstance().collection(COLLECTION).document(user.uid)
                .set(mapOf("name" to name, "updatedAt" to System.currentTimeMillis()), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Mengembalikan foto yang sudah diproses (persegi 256 px) bila berhasil disimpan
    suspend fun savePhoto(source: Bitmap): Result<Bitmap> {
        val id = uid() ?: return Result.failure(Exception("Belum login"))
        return try {
            val (processed, base64) = withContext(Dispatchers.Default) {
                val square = centerSquare(source)
                val scaled = Bitmap.createScaledBitmap(square, PHOTO_SIZE, PHOTO_SIZE, true)
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
                scaled to Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            }
            FirebaseFirestore.getInstance().collection(COLLECTION).document(id)
                .set(mapOf("photoBase64" to base64, "updatedAt" to System.currentTimeMillis()), SetOptions.merge())
                .await()
            Result.success(processed)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun centerSquare(src: Bitmap): Bitmap {
        val size = minOf(src.width, src.height)
        val x = (src.width - size) / 2
        val y = (src.height - size) / 2
        return Bitmap.createBitmap(src, x, y, size, size)
    }

    private fun decodeBase64(data: String): Bitmap? = try {
        val bytes = Base64.decode(data, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}
