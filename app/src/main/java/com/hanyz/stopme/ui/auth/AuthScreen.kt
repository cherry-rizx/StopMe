package com.hanyz.stopme.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hanyz.stopme.R
import com.hanyz.stopme.ui.theme.EmailButtonRed
import com.hanyz.stopme.ui.theme.GoogleButtonBlue
import com.hanyz.stopme.ui.theme.NavyPrimary
import com.hanyz.stopme.ui.theme.TextDark
import com.hanyz.stopme.ui.theme.TextSecondary
import com.hanyz.stopme.ui.theme.TextWhite

@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onGoogleClick: () -> Unit,
    onEmailClick: (isSignUp: Boolean) -> Unit,
    onDismissDialog: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onSubmitEmail: () -> Unit,
    onForgotPassword: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onSuccess()
        }
    }

    // Saat dialog email terbuka, pesan ditampilkan di dalam dialog (snackbar tertutup dialog)
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            if (!uiState.showEmailDialog) snackbarHostState.showSnackbar(it)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .widthIn(max = 600.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Judul "Selamat Datang di StopMe"
            Text(
                text = stringResource(id = R.string.welcome_title),
                style = MaterialTheme.typography.displayMedium,
                color = NavyPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("auth_title")
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Ilustrasi Registrasi dalam Lingkaran
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .clip(CircleShape)
                    .border(3.dp, NavyPrimary, CircleShape)
                    .testTag("auth_illustration"),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_ilustrasi_registrasi),
                    contentDescription = stringResource(id = R.string.welcome_subtitle),
                    modifier = Modifier.size(190.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Teks "Jangan Kelewatan Halte Tujuan Lagi"
            Text(
                text = stringResource(id = R.string.welcome_subtitle),
                style = MaterialTheme.typography.titleMedium,
                color = TextDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("auth_subtitle")
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Tombol "Continue With Google" (#4280D8)
            Button(
                onClick = onGoogleClick,
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoogleButtonBlue,
                    contentColor = TextWhite
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_continue_google")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.continue_with_google),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tombol "Continue With Email" (#E94A4D)
            Button(
                onClick = { onEmailClick(false) },
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmailButtonRed,
                    contentColor = TextWhite
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_continue_email")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_gmail),
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.continue_with_email),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Teks "Sudah Punya Akun? Masuk"
            Text(
                text = stringResource(id = R.string.already_have_account),
                style = MaterialTheme.typography.bodyMedium,
                color = NavyPrimary,
                modifier = Modifier
                    .clickable { onEmailClick(false) }
                    .padding(8.dp)
                    .testTag("btn_toggle_signin")
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Loading overlay
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NavyPrimary)
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Dialog Masuk/Daftar dengan Email
    if (uiState.showEmailDialog) {
        AlertDialog(
            onDismissRequest = onDismissDialog,
            shape = MaterialTheme.shapes.large,
            title = {
                Text(
                    text = stringResource(
                        id = if (uiState.isSignUpMode) R.string.sign_up else R.string.sign_in
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = NavyPrimary
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.emailInput,
                        onValueChange = onEmailChange,
                        label = { Text(stringResource(id = R.string.email_label)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_email")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.passwordInput,
                        onValueChange = onPasswordChange,
                        label = { Text(stringResource(id = R.string.password_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_password")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            id = if (uiState.isSignUpMode) R.string.already_have_account else R.string.dont_have_account
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier
                            .clickable { onToggleMode() }
                            .padding(vertical = 4.dp)
                    )

                    // Lupa kata sandi (hanya di mode Masuk)
                    if (!uiState.isSignUpMode) {
                        Text(
                            text = "Lupa kata sandi?",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            color = NavyPrimary,
                            modifier = Modifier
                                .clickable { onForgotPassword() }
                                .padding(vertical = 4.dp)
                                .testTag("btn_forgot_password")
                        )
                    }

                    // Pesan error / info langsung di dalam dialog
                    uiState.errorMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color(0xFFC62828),
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .testTag("auth_error_text")
                        )
                    }
                    uiState.infoMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onSubmitEmail,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.testTag("btn_submit_email")
                ) {
                    Text(
                        text = stringResource(
                            id = if (uiState.isSignUpMode) R.string.sign_up else R.string.sign_in
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDialog) {
                    Text(text = stringResource(id = R.string.cancel), color = TextSecondary)
                }
            }
        )
    }
}
