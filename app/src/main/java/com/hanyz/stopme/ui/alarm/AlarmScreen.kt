package com.hanyz.stopme.ui.alarm

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanyz.stopme.R
import com.hanyz.stopme.ui.theme.DarkNavyCard
import com.hanyz.stopme.ui.theme.LightOrangeText
import com.hanyz.stopme.ui.theme.NavyPrimary
import com.hanyz.stopme.ui.theme.OrangeAccent
import com.hanyz.stopme.ui.theme.TextWhite

@Composable
fun AlarmScreen(
    uiState: AlarmUiState,
    onSlideDismissed: () -> Unit,
    onCloseActivity: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(uiState.isDismissed) {
        if (uiState.isDismissed) {
            onCloseActivity()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OrangeAccent)
            .testTag("alarm_screen_root")
    ) {
        // Kontainer kartu melengkung dengan latar Navy seperti pada desain acuan 03
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp)
                .background(
                    color = DarkNavyCard,
                    shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Bagian Atas: Peringatan & Nama Halte
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Jenis alarm, mis. "Alarm 1 dari 2 · Jarak"
                    if (uiState.alarmTitle.isNotBlank()) {
                        Text(
                            text = uiState.alarmTitle.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = LightOrangeText,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }

                    Text(
                        text = stringResource(
                            id = R.string.alarm_title_format,
                            uiState.destinationName.uppercase()
                        ),
                        style = MaterialTheme.typography.displayLarge,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("alarm_title")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = stringResource(id = R.string.arriving_in_format, uiState.arrivalInfoStr),
                        style = MaterialTheme.typography.titleLarge,
                        color = LightOrangeText,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("alarm_arrival_info")
                    )
                }

                // Bagian Tengah: Ikon Jam Alarm Besar (ic_alarm_clock)
                Box(
                    modifier = Modifier
                        .size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_alarm_clock),
                        contentDescription = "Jam Alarm",
                        modifier = Modifier
                            .size(150.dp)
                            .testTag("alarm_clock_icon")
                    )
                }

                // Bagian Bawah: Geser untuk matikan alarm & Pill Slide-to-Dismiss
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.slide_to_dismiss_instruction),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("alarm_instruction_text")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    SlideToDismissBar(
                        onDismissed = onSlideDismissed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}
