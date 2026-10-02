package com.hanyz.stopme.ui.profile

import android.graphics.Bitmap

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,            // foto akun Google (cadangan)
    val photoBitmap: Bitmap? = null,         // foto pilihan sendiri dari Firestore
    val isEmailVisible: Boolean = false,     // email disembunyikan sampai diketuk
    val showEditNameDialog: Boolean = false,
    val editNameInput: String = "",
    val showPhotoSourceDialog: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val showLogoutConfirmation: Boolean = false,
    val isSignedOut: Boolean = false
) {
    // Inisial untuk avatar default, mis. "Hanyz Putra" -> "HP"
    val initials: String
        get() = name.trim().split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "?" }

    // Email disamarkan saat disembunyikan
    val emailDisplay: String
        get() = if (isEmailVisible) email else "••••••••@••••"
}
