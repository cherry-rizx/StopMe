package com.hanyz.stopme.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanyz.stopme.data.AuthRepository
import com.hanyz.stopme.data.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val user = AuthRepository.currentUser
        _uiState.update {
            it.copy(
                name = user?.name ?: "Pengguna StopMe",
                email = user?.email?.ifBlank { null } ?: "Belum ada email",
                photoUrl = user?.photoUrl
            )
        }
        // Ambil nama & foto tersimpan dari Firestore
        viewModelScope.launch {
            ProfileRepository.load().onSuccess { stored ->
                _uiState.update {
                    it.copy(
                        name = stored.name?.ifBlank { null } ?: it.name,
                        photoBitmap = stored.photo ?: it.photoBitmap
                    )
                }
            }
        }
    }

    // ---------- Email ----------
    fun toggleEmailVisibility() {
        _uiState.update { it.copy(isEmailVisible = !it.isEmailVisible) }
    }

    // ---------- Nama ----------
    fun showEditName() {
        _uiState.update { it.copy(showEditNameDialog = true, editNameInput = it.name) }
    }

    fun onEditNameChanged(value: String) {
        _uiState.update { it.copy(editNameInput = value.take(40)) }
    }

    fun dismissEditName() {
        _uiState.update { it.copy(showEditNameDialog = false) }
    }

    fun saveName() {
        val newName = _uiState.value.editNameInput.trim()
        if (newName.isEmpty()) {
            _uiState.update { it.copy(message = "Nama tidak boleh kosong") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, showEditNameDialog = false) }
            ProfileRepository.saveName(newName).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, name = newName, message = "Nama berhasil diperbarui") }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSaving = false, message = "Gagal menyimpan nama: ${e.localizedMessage}") }
                }
            )
        }
    }

    // ---------- Foto ----------
    fun showPhotoSource() {
        _uiState.update { it.copy(showPhotoSourceDialog = true) }
    }

    fun dismissPhotoSource() {
        _uiState.update { it.copy(showPhotoSourceDialog = false) }
    }

    // Foto dari galeri (Photo Picker)
    fun onGalleryPhotoPicked(context: Context, uri: Uri?) {
        _uiState.update { it.copy(showPhotoSourceDialog = false) }
        if (uri == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val bitmap = withContext(Dispatchers.IO) { decodeScaled(context, uri) }
            if (bitmap == null) {
                _uiState.update { it.copy(isSaving = false, message = "Foto tidak bisa dibaca") }
                return@launch
            }
            uploadPhoto(bitmap)
        }
    }

    // Foto dari kamera (pratinjau kecil, sudah cukup untuk avatar)
    fun onCameraPhotoTaken(bitmap: Bitmap?) {
        _uiState.update { it.copy(showPhotoSourceDialog = false) }
        if (bitmap == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            uploadPhoto(bitmap)
        }
    }

    private suspend fun uploadPhoto(bitmap: Bitmap) {
        ProfileRepository.savePhoto(bitmap).fold(
            onSuccess = { processed ->
                _uiState.update { it.copy(isSaving = false, photoBitmap = processed, message = "Foto profil diperbarui") }
            },
            onFailure = { e ->
                _uiState.update { it.copy(isSaving = false, message = "Gagal menyimpan foto: ${e.localizedMessage}") }
            }
        )
    }

    // Baca gambar dengan ukuran diperkecil agar hemat memori; ImageDecoder ikut memutar sesuai EXIF
    private fun decodeScaled(context: Context, uri: Uri): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val maxSide = maxOf(info.size.width, info.size.height)
                if (maxSide > 1024) decoder.setTargetSampleSize(maxSide / 1024)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inSampleSize = 4 })
            }
        }
    } catch (e: Exception) {
        null
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    // ---------- Keluar ----------
    fun showLogoutDialog() {
        _uiState.update { it.copy(showLogoutConfirmation = true) }
    }

    fun dismissLogoutDialog() {
        _uiState.update { it.copy(showLogoutConfirmation = false) }
    }

    fun confirmSignOut() {
        AuthRepository.signOut()
        _uiState.update {
            it.copy(
                showLogoutConfirmation = false,
                isSignedOut = true
            )
        }
    }
}
