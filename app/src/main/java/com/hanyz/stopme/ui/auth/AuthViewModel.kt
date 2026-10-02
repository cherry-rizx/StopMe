package com.hanyz.stopme.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanyz.stopme.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import android.content.Intent
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

class AuthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailInputChanged(email: String) {
        _uiState.update { it.copy(emailInput = email, errorMessage = null) }
    }

    fun onPasswordInputChanged(password: String) {
        _uiState.update { it.copy(passwordInput = password, errorMessage = null) }
    }

    fun showEmailDialog(isSignUp: Boolean) {
        _uiState.update {
            it.copy(
                showEmailDialog = true,
                isSignUpMode = isSignUp,
                errorMessage = null
            )
        }
    }

    fun dismissEmailDialog() {
        _uiState.update { it.copy(showEmailDialog = false, errorMessage = null) }
    }

    fun toggleAuthMode() {
        _uiState.update { it.copy(isSignUpMode = !it.isSignUpMode, errorMessage = null) }
    }

    fun submitEmailAuth() {
        val email = _uiState.value.emailInput.trim()
        val password = _uiState.value.passwordInput.trim()
        if (email.isEmpty() || password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Email dan kata sandi wajib diisi") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val isSignUp = _uiState.value.isSignUpMode
            val result = if (isSignUp) {
                AuthRepository.signUpWithEmail(email, password)
            } else {
                AuthRepository.signInWithEmail(email, password)
            }

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            showEmailDialog = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = friendlyAuthError(error, isSignUp))
                    }
                }
            )
        }
    }

    // Lupa kata sandi: kirim link atur ulang ke email yang diisi
    fun sendPasswordReset() {
        val email = _uiState.value.emailInput.trim()
        if (email.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Isi email terlebih dahulu, lalu tekan Lupa kata sandi.", infoMessage = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            AuthRepository.sendPasswordReset(email).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            infoMessage = "Jika email terdaftar, link untuk mengatur kata sandi sudah dikirim ke $email. Cek juga folder Spam."
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = friendlyAuthError(e, false)) }
                }
            )
        }
    }

    // Terjemahkan error Firebase menjadi pesan yang jelas bagi user
    private fun friendlyAuthError(error: Throwable, isSignUp: Boolean): String {
        val code = (error as? com.google.firebase.auth.FirebaseAuthException)?.errorCode.orEmpty()
        return when (error) {
            is com.google.firebase.FirebaseTooManyRequestsException ->
                "Terlalu banyak percobaan dari perangkat ini. Tunggu beberapa saat (bisa sampai 1 jam), lalu coba lagi."
            is com.google.firebase.FirebaseNetworkException ->
                "Tidak ada koneksi internet. Periksa jaringanmu lalu coba lagi."
            is com.google.firebase.auth.FirebaseAuthWeakPasswordException ->
                "Kata sandi terlalu lemah. Gunakan minimal 6 karakter."
            is com.google.firebase.auth.FirebaseAuthUserCollisionException ->
                "Email ini sudah terdaftar. Silakan masuk, atau gunakan tombol Google jika akun dibuat lewat Google."
            is com.google.firebase.auth.FirebaseAuthInvalidUserException -> when (code) {
                "ERROR_USER_DISABLED" -> "Akun ini dinonaktifkan."
                else -> "Akun tidak ditemukan. Periksa email atau daftar akun baru."
            }
            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> when (code) {
                "ERROR_INVALID_EMAIL" -> "Format email tidak valid."
                else -> if (isSignUp) {
                    "Data pendaftaran tidak valid. Periksa email dan kata sandi."
                } else {
                    "Email atau kata sandi salah. Jika akun ini dibuat lewat Google, masuk dengan tombol Google atau tekan \"Lupa kata sandi?\" untuk membuat kata sandi."
                }
            }
            else -> "Terjadi kesalahan. Coba lagi beberapa saat lagi."
        }
    }

    fun signInWithGoogle(context: Context, serverClientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Batas waktu: di beberapa HP jendela akun Google gagal tampil dan tidak pernah menjawab
            val result = withTimeoutOrNull(GOOGLE_TIMEOUT_MS) {
                AuthRepository.signInWithGoogleCredentialManager(context, serverClientId)
            }

            when {
                result == null -> {
                    // Tidak ada jawaban: coba jalur cadangan (metode lama)
                    _uiState.update { it.copy(isLoading = false, requestLegacyGoogleSignIn = true) }
                }
                result.isSuccess -> {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                result.exceptionOrNull() is GetCredentialCancellationException -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Login Google dibatalkan")
                    }
                }
                else -> {
                    // Gagal karena alasan lain: coba jalur cadangan
                    _uiState.update { it.copy(isLoading = false, requestLegacyGoogleSignIn = true) }
                }
            }
        }
    }

    fun consumeLegacyGoogleRequest() {
        _uiState.update { it.copy(requestLegacyGoogleSignIn = false, isLoading = true) }
    }

    // Hasil dari jendela login Google metode lama
    @Suppress("DEPRECATION")
    fun onLegacyGoogleResult(data: Intent?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                    .getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken == null) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Token Google kosong. Gunakan login email.")
                    }
                    return@launch
                }
                AuthRepository.signInWithGoogleIdToken(idToken).fold(
                    onSuccess = { _uiState.update { it.copy(isLoading = false, isSuccess = true) } },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = friendlyAuthError(e, false))
                        }
                    }
                )
            } catch (e: ApiException) {
                val message = when (e.statusCode) {
                    12501 -> "Login Google dibatalkan"
                    10 -> "Konfigurasi Google belum sesuai (SHA-1 / Web Client ID). Gunakan login email."
                    7 -> "Tidak ada koneksi internet"
                    else -> "Login Google gagal (kode ${e.statusCode}). Gunakan login email."
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = message) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Login Google gagal. Gunakan login email.")
                }
            }
        }
    }

    companion object {
        private const val GOOGLE_TIMEOUT_MS = 20_000L
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }
}
