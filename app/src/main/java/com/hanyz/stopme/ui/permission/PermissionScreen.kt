package com.hanyz.stopme.ui.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hanyz.stopme.R
import com.hanyz.stopme.ui.theme.DarkNavyCard
import com.hanyz.stopme.ui.theme.DisabledGrey
import com.hanyz.stopme.ui.theme.NavyPrimary
import com.hanyz.stopme.ui.theme.TextDark
import com.hanyz.stopme.ui.theme.TextWhite

// Layar izin yang hanya muncul sekali saat pertama kali (onboarding)
@Composable
fun PermissionScreen(
    uiState: PermissionUiState,
    onRefreshPermissions: () -> Unit,
    onStartNowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(id = R.string.permission_title),
                style = MaterialTheme.typography.displayMedium,
                color = NavyPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("permission_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.permission_subtitle),
                style = MaterialTheme.typography.titleMedium,
                color = TextDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("permission_subtitle")
            )

            Spacer(modifier = Modifier.height(32.dp))

            PermissionToggleList(
                uiState = uiState,
                onRefreshPermissions = onRefreshPermissions
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Tombol "Start Now" (aktif setelah izin inti diberikan)
            Button(
                onClick = onStartNowClick,
                enabled = uiState.canProceed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkNavyCard,
                    contentColor = TextWhite,
                    disabledContainerColor = DisabledGrey,
                    disabledContentColor = TextWhite.copy(alpha = 0.6f)
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_permission_start_now")
            ) {
                Text(
                    text = stringResource(id = R.string.start_now),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
