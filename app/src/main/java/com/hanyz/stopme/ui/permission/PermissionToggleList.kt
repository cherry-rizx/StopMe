package com.hanyz.stopme.ui.permission

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hanyz.stopme.R
import com.hanyz.stopme.ui.theme.ActiveToggleGreen
import com.hanyz.stopme.ui.theme.DisabledGrey
import com.hanyz.stopme.ui.theme.NavyPrimary
import com.hanyz.stopme.ui.theme.TextWhite

// Daftar 3 kartu izin, dipakai di layar Izin (pertama kali) dan di Profil > Pengaturan
@Composable
fun PermissionToggleList(
    uiState: PermissionUiState,
    onRefreshPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { onRefreshPermissions() }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onRefreshPermissions()
        // Android 14+: layar alarm di atas lockscreen butuh izin full-screen intent
        if (granted && Build.VERSION.SDK_INT >= 34) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!nm.canUseFullScreenIntent()) {
                openSettings(context, Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, withPackage = true)
            }
        }
    }

    // Cek ulang setiap layar kembali aktif (mis. setelah dari Pengaturan HP)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onRefreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PermissionCard(
            iconRes = R.drawable.ic_lokasi,
            title = stringResource(id = R.string.permission_location_title),
            description = stringResource(id = R.string.permission_location_desc),
            isGranted = uiState.isLocationGranted,
            onToggleChange = { turnOn ->
                if (turnOn) {
                    locationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                } else {
                    // Android tidak mengizinkan app mencabut izinnya sendiri
                    openAppDetails(context)
                }
            },
            testTag = "toggle_location_permission"
        )

        PermissionCard(
            iconRes = R.drawable.ic_notifikasi,
            title = stringResource(id = R.string.permission_notification_title),
            description = stringResource(id = R.string.permission_notification_desc),
            isGranted = uiState.isNotificationGranted,
            onToggleChange = { turnOn ->
                if (turnOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openAppDetails(context)
                }
            },
            testTag = "toggle_notification_permission"
        )

        PermissionCard(
            iconRes = R.drawable.ic_bell_aktif,
            title = stringResource(id = R.string.permission_battery_title),
            description = stringResource(id = R.string.permission_battery_desc),
            isGranted = uiState.isBatteryOptimizationIgnored,
            onToggleChange = { turnOn ->
                if (turnOn) {
                    openSettings(context, Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, withPackage = true)
                } else {
                    openSettings(context, Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, withPackage = false)
                }
            },
            testTag = "toggle_battery_permission"
        )
    }
}

private fun openAppDetails(context: Context) {
    openSettings(context, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, withPackage = true)
}

private fun openSettings(context: Context, action: String, withPackage: Boolean) {
    try {
        val intent = Intent(action).apply {
            if (withPackage) data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Cadangan: buka halaman info aplikasi
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {
        }
    }
}

@Composable
private fun PermissionCard(
    iconRes: Int,
    title: String,
    description: String,
    isGranted: Boolean,
    onToggleChange: (Boolean) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = NavyPrimary),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(TextWhite.copy(alpha = 0.15f), MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextWhite.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = isGranted,
                onCheckedChange = onToggleChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextWhite,
                    checkedTrackColor = ActiveToggleGreen,
                    uncheckedThumbColor = DisabledGrey,
                    uncheckedTrackColor = TextWhite.copy(alpha = 0.3f)
                ),
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}
