package com.hanyz.stopme.ui.activity

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hanyz.stopme.R
import com.hanyz.stopme.ui.theme.DarkNavyCard
import com.hanyz.stopme.ui.theme.DisabledGrey
import com.hanyz.stopme.ui.theme.LightGreenChip
import com.hanyz.stopme.ui.theme.NavyPrimary
import com.hanyz.stopme.ui.theme.OrangeAccent
import com.hanyz.stopme.ui.theme.SecondaryBackground
import com.hanyz.stopme.ui.theme.TextDark
import com.hanyz.stopme.ui.theme.TextSecondary
import com.hanyz.stopme.ui.theme.TextWhite

@Composable
fun ActivitiesScreen(
    uiState: ActivitiesUiState,
    onBackClick: () -> Unit,
    onToggleMapStyle: () -> Unit,
    onStopTripClick: () -> Unit,
    onHistoryTabSelected: (HistoryTab) -> Unit,
    onToggleFavorite: (HistoryItemUi) -> Unit,
    onShowHistoryDetail: (HistoryItemUi) -> Unit,
    onDismissHistoryDetail: () -> Unit,
    onStartFromHistory: (HistoryItemUi) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!uiState.hasActiveTrip) {
        // Tidak ada perjalanan aktif: tampilkan Riwayat & Favorit Rute
        HistoryContent(
            uiState = uiState,
            onTabSelected = onHistoryTabSelected,
            onToggleFavorite = onToggleFavorite,
            onShowDetail = onShowHistoryDetail,
            onDismissDetail = onDismissHistoryDetail,
            onStartFromHistory = onStartFromHistory,
            modifier = modifier
        )
        return
    }

    // Dinaikkan setiap tombol "lokasi saya" ditekan
    var recenterRequest by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Bagian Atas: Peta OpenStreetMap (osmdroid)
        OsmMapView(
            routePoints = uiState.routePoints,
            stops = uiState.stopsList,
            destination = uiState.destinationStop,
            userPoint = uiState.userPoint,
            isDarkMap = uiState.isDarkMap,
            recenterRequest = recenterRequest,
            modifier = Modifier.fillMaxSize()
        )

        // Tombol Bulat Putih di Atas Peta
        // 1. Tombol Back (Kiri Atas)
        FloatingActionButton(
            onClick = onBackClick,
            shape = CircleShape,
            containerColor = Color.White,
            contentColor = NavyPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 40.dp)
                .size(46.dp)
                .testTag("btn_map_back")
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_back),
                contentDescription = "Kembali",
                modifier = Modifier.size(22.dp)
            )
        }

        // 2. Tombol Lapisan Peta & Lokasi Saya (Kanan Atas)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 20.dp, top = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FloatingActionButton(
                onClick = onToggleMapStyle,
                shape = CircleShape,
                containerColor = Color.White,
                contentColor = if (uiState.isDarkMap) NavyPrimary else OrangeAccent,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .size(46.dp)
                    .testTag("btn_map_layer")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_map_layer),
                    contentDescription = "Lapisan Peta",
                    modifier = Modifier.size(22.dp)
                )
            }

            FloatingActionButton(
                onClick = { recenterRequest++ },
                shape = CircleShape,
                containerColor = Color.White,
                contentColor = NavyPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .size(46.dp)
                    .testTag("btn_my_location")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_my_location),
                    contentDescription = "Pusatkan Lokasi",
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Bottom Sheet Putih Bersudut Membulat 32dp di Bawah Layar
        Card(
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .testTag("activities_bottom_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Drag handle bar
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .background(DisabledGrey, RoundedCornerShape(2.dp))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Baris Status & Judul Tujuan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.heading_to, uiState.destinationName),
                            style = MaterialTheme.typography.titleLarge,
                            color = NavyPrimary
                        )
                    }

                    // Status Badge (Offline Mode / GPS Aktif)
                    if (uiState.isOffline) {
                        Box(
                            modifier = Modifier
                                .background(OrangeAccent.copy(alpha = 0.15f), MaterialTheme.shapes.small)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.offline_mode_badge),
                                color = OrangeAccent,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Kartu Tiga Kolom: Sisa Jarak, ETA, Halte/Stasiun Berikutnya
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = SecondaryBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Kolom 1: Sisa Jarak
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(id = R.string.remaining_distance),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.remainingDistanceStr,
                                style = MaterialTheme.typography.titleMedium,
                                color = NavyPrimary
                            )
                        }

                        // Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(36.dp)
                                .background(DisabledGrey.copy(alpha = 0.5f))
                        )

                        // Kolom 2: ETA
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(id = R.string.eta_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.etaStr,
                                style = MaterialTheme.typography.titleMedium,
                                color = OrangeAccent
                            )
                        }

                        // Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(36.dp)
                                .background(DisabledGrey.copy(alpha = 0.5f))
                        )

                        // Kolom 3: Halte Berikutnya
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(
                                text = stringResource(
                                    id = if (uiState.isTrain) R.string.next_stop_label_train else R.string.next_stop_label_bus
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.nextStopName,
                                style = MaterialTheme.typography.titleSmall,
                                color = NavyPrimary,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Chip Hijau dengan ic_bell_aktif
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LightGreenChip, MaterialTheme.shapes.medium)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_bell_aktif),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.alarmChipText,
                            style = MaterialTheme.typography.labelMedium,
                            color = NavyPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tombol Abu "Matikan Alarm" (menghentikan perjalanan)
                Button(
                    onClick = onStopTripClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DisabledGrey,
                        contentColor = TextDark
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_turn_off_alarm")
                ) {
                    Text(
                        text = stringResource(id = R.string.turn_off_alarm),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}
