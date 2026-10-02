package com.hanyz.stopme.ui.profile

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hanyz.stopme.ui.theme.DarkNavyCard
import com.hanyz.stopme.ui.theme.NavyPrimary
import com.hanyz.stopme.ui.theme.SecondaryBackground
import com.hanyz.stopme.ui.theme.TextSecondary
import com.hanyz.stopme.ui.theme.TextWhite

// Foto, nama (bisa diedit), dan email tersembunyi
@Composable
fun ProfileIdentitySection(
    uiState: ProfileUiState,
    onAvatarClick: () -> Unit,
    onEditNameClick: () -> Unit,
    onToggleEmail: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Avatar dengan lencana kamera
        Box(modifier = Modifier.size(116.dp)) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .border(3.dp, NavyPrimary, CircleShape)
                    .background(SecondaryBackground)
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.photoBitmap != null -> Image(
                        bitmap = uiState.photoBitmap.asImageBitmap(),
                        contentDescription = uiState.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    !uiState.photoUrl.isNullOrBlank() -> AsyncImage(
                        model = uiState.photoUrl,
                        contentDescription = uiState.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> Text(
                        text = uiState.initials,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                }
                if (uiState.isSaving) {
                    CircularProgressIndicator(color = NavyPrimary, modifier = Modifier.size(40.dp))
                }
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.BottomEnd)
                    .background(NavyPrimary, CircleShape)
                    .border(2.dp, TextWhite, CircleShape)
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Ganti foto profil",
                    tint = TextWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = SecondaryBackground),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Nama + tombol edit
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = uiState.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = NavyPrimary
                    )
                    IconButton(onClick = onEditNameClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Ubah nama",
                            tint = NavyPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Email tersembunyi, tampil saat diketuk
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onToggleEmail() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = uiState.emailDisplay,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (uiState.isEmailVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (uiState.isEmailVisible) "Sembunyikan email" else "Lihat email",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Dialog pilih sumber foto, dialog edit nama, dan pesan Toast
@Composable
fun ProfileEditingEffects(
    uiState: ProfileUiState,
    onDismissPhotoSource: () -> Unit,
    onGalleryPicked: (Uri?) -> Unit,
    onCameraTaken: (Bitmap?) -> Unit,
    onEditNameChange: (String) -> Unit,
    onDismissEditName: () -> Unit,
    onSaveName: () -> Unit,
    onMessageShown: () -> Unit
) {
    val context = LocalContext.current

    // Galeri: Photo Picker (tidak butuh izin penyimpanan)
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> onGalleryPicked(uri) }

    // Kamera: pratinjau kecil, tidak butuh izin kamera karena memakai app kamera bawaan
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> onCameraTaken(bitmap) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            onMessageShown()
        }
    }

    if (uiState.showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = onDismissPhotoSource,
            title = { Text("Ganti foto profil", color = NavyPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SourceOption(
                        label = "Pilih dari galeri",
                        icon = { Icon(Icons.Filled.PhotoLibrary, null, tint = NavyPrimary) },
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                    SourceOption(
                        label = "Ambil dengan kamera",
                        icon = { Icon(Icons.Filled.CameraAlt, null, tint = NavyPrimary) },
                        onClick = {
                            try {
                                cameraLauncher.launch(null)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Kamera tidak tersedia", Toast.LENGTH_SHORT).show()
                                onDismissPhotoSource()
                            }
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismissPhotoSource) { Text("Batal", color = NavyPrimary) }
            }
        )
    }

    if (uiState.showEditNameDialog) {
        AlertDialog(
            onDismissRequest = onDismissEditName,
            title = { Text("Ubah nama", color = NavyPrimary) },
            text = {
                OutlinedTextField(
                    value = uiState.editNameInput,
                    onValueChange = onEditNameChange,
                    singleLine = true,
                    label = { Text("Nama tampilan") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = onSaveName,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavyCard, contentColor = TextWhite)
                ) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = onDismissEditName) { Text("Batal", color = NavyPrimary) }
            }
        )
    }
}

@Composable
private fun SourceOption(label: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(SecondaryBackground)
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = NavyPrimary)
    }
}
