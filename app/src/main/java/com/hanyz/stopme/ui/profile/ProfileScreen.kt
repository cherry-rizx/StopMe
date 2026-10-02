package com.hanyz.stopme.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hanyz.stopme.R
import com.hanyz.stopme.ui.permission.PermissionToggleList
import com.hanyz.stopme.ui.permission.PermissionUiState
import com.hanyz.stopme.ui.theme.EmailButtonRed
import com.hanyz.stopme.ui.theme.NavyPrimary
import com.hanyz.stopme.ui.theme.SecondaryBackground
import com.hanyz.stopme.ui.theme.TextDark
import com.hanyz.stopme.ui.theme.TextSecondary
import com.hanyz.stopme.ui.theme.TextWhite

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onSignOutClick: () -> Unit,
    onConfirmSignOut: () -> Unit,
    onDismissSignOutDialog: () -> Unit,
    onSignedOutNavigate: () -> Unit,
    permissionUiState: PermissionUiState,
    onRefreshPermissions: () -> Unit,
    onAvatarClick: () -> Unit,
    onDismissPhotoSource: () -> Unit,
    onGalleryPicked: (android.net.Uri?) -> Unit,
    onCameraTaken: (android.graphics.Bitmap?) -> Unit,
    onEditNameClick: () -> Unit,
    onEditNameChange: (String) -> Unit,
    onDismissEditName: () -> Unit,
    onSaveName: () -> Unit,
    onToggleEmail: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dialog & launcher foto/nama (lihat ProfileEditing.kt)
    ProfileEditingEffects(
        uiState = uiState,
        onDismissPhotoSource = onDismissPhotoSource,
        onGalleryPicked = onGalleryPicked,
        onCameraTaken = onCameraTaken,
        onEditNameChange = onEditNameChange,
        onDismissEditName = onDismissEditName,
        onSaveName = onSaveName,
        onMessageShown = onMessageShown
    )

    LaunchedEffect(uiState.isSignedOut) {
        if (uiState.isSignedOut) {
            onSignedOutNavigate()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(id = R.string.profile_title),
                style = MaterialTheme.typography.displayMedium,
                color = NavyPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("profile_title")
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Foto profil + identitas (bisa diubah)
            ProfileIdentitySection(
                uiState = uiState,
                onAvatarClick = onAvatarClick,
                onEditNameClick = onEditNameClick,
                onToggleEmail = onToggleEmail
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Info Versi
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = SecondaryBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "StopMe",
                        style = MaterialTheme.typography.titleSmall,
                        color = NavyPrimary
                    )
                    Text(
                        text = stringResource(id = R.string.version_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Pengaturan: 3 akses yang sama dengan layar Izin awal
            Text(
                text = stringResource(id = R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                color = NavyPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_title")
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.settings_desc),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
            PermissionToggleList(
                uiState = permissionUiState,
                onRefreshPermissions = onRefreshPermissions
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Tombol "Keluar"
            Button(
                onClick = onSignOutClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmailButtonRed,
                    contentColor = TextWhite
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_profile_sign_out")
            ) {
                Text(
                    text = stringResource(id = R.string.sign_out),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Dialog Konfirmasi Keluar
    if (uiState.showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissSignOutDialog,
            shape = MaterialTheme.shapes.large,
            title = {
                Text(
                    text = stringResource(id = R.string.sign_out),
                    style = MaterialTheme.typography.titleLarge,
                    color = NavyPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.sign_out_confirm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmSignOut,
                    colors = ButtonDefaults.buttonColors(containerColor = EmailButtonRed),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.testTag("btn_confirm_sign_out")
                ) {
                    Text(text = stringResource(id = R.string.sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSignOutDialog) {
                    Text(text = stringResource(id = R.string.cancel), color = TextSecondary)
                }
            }
        )
    }
}
