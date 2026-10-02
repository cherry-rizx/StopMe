package com.hanyz.stopme.ui.auth

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,     // pesan berhasil, mis. link atur ulang kata sandi terkirim
    val showEmailDialog: Boolean = false,
    val isSignUpMode: Boolean = false,
    val emailInput: String = "",
    val passwordInput: String = "",
    // Sinyal sekali pakai: minta UI membuka login Google metode lama
    val requestLegacyGoogleSignIn: Boolean = false
)
