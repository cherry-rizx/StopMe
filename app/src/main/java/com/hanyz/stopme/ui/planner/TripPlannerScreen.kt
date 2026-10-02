package com.hanyz.stopme.ui.planner

import androidx.compose.foundation.background
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanyz.stopme.R
import com.hanyz.stopme.model.DirectRouteCandidate
import com.hanyz.stopme.ui.theme.DarkNavyCard
import com.hanyz.stopme.ui.theme.DisabledGrey
import com.hanyz.stopme.ui.theme.LightGreenChip
import com.hanyz.stopme.ui.theme.LightOrangeText
import com.hanyz.stopme.ui.theme.NavyPrimary
import com.hanyz.stopme.ui.theme.OrangeAccent
import com.hanyz.stopme.ui.theme.SecondaryBackground
import com.hanyz.stopme.ui.theme.TextDark
import com.hanyz.stopme.ui.theme.TextSecondary
import com.hanyz.stopme.ui.theme.TextWhite
import com.hanyz.stopme.util.LocationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripPlannerScreen(
    uiState: TripPlannerUiState,
    onBackClick: () -> Unit,
    onActiveFieldChange: (ActiveSearchField) -> Unit,
    onDepartureQueryChange: (String) -> Unit,
    onDestinationQueryChange: (String) -> Unit,
    onStopSelected: (String) -> Unit,
    onSelectRouteCandidate: (DirectRouteCandidate) -> Unit,
    onDismissRouteSheet: () -> Unit,
    onRadiusIndexChange: (Int) -> Unit,
    onMinutesThresholdChange: (Int) -> Unit,
    onAlarmModeChange: (com.hanyz.stopme.model.AlarmMode) -> Unit,
    onStartTripClick: () -> Unit,
    onShowAllStops: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isTrain = uiState.serviceType.isTrain
    val radiusOptions = if (isTrain) uiState.trainRadiusOptions else uiState.busRadiusOptions
    val selectedRadiusMeters = radiusOptions.getOrElse(uiState.selectedRadiusIndex) { 500 }
    val radiusLabel = LocationUtils.formatDistance(selectedRadiusMeters.toDouble())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header Navy (#052659) dengan 2 Kolom Pencarian
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = NavyPrimary,
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                )
                .statusBarsPadding() // header tidak menabrak jam/ikon status bar
                .padding(start = 8.dp, end = 20.dp, top = 4.dp, bottom = 20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("btn_planner_back")
                    ) {
                        // Ukuran ikon standar Android (24dp), area sentuh 48dp dari IconButton
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Kembali",
                            tint = TextWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = uiState.serviceType.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextWhite
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Kolom 1: Halte/Stasiun Keberangkatan
                OutlinedTextField(
                    value = uiState.departureQuery,
                    onValueChange = onDepartureQueryChange,
                    placeholder = {
                        Text(
                            text = stringResource(
                                id = if (isTrain) R.string.search_departure_train else R.string.search_departure_bus
                            ),
                            color = TextWhite.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = if (isTrain) R.drawable.ic_train_white else R.drawable.ic_bus_white),
                            contentDescription = null,
                            tint = if (uiState.activeField == ActiveSearchField.DEPARTURE) OrangeAccent else TextWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = TextWhite.copy(alpha = 0.4f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = TextWhite
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("input_departure")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Kolom 2: Halte/Stasiun Tujuan
                OutlinedTextField(
                    value = uiState.destinationQuery,
                    onValueChange = onDestinationQueryChange,
                    placeholder = {
                        Text(
                            text = stringResource(
                                id = if (isTrain) R.string.search_destination_train else R.string.search_destination_bus
                            ),
                            color = TextWhite.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_lokasi),
                            contentDescription = null,
                            tint = if (uiState.activeField == ActiveSearchField.DESTINATION) OrangeAccent else TextWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = TextWhite.copy(alpha = 0.4f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = TextWhite
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("input_destination")
                )
            }
        }

        // Pesan error jika tidak ada rute langsung
        if (uiState.noDirectRouteFound) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .background(ColorErrorSoft, MaterialTheme.shapes.medium)
                    .padding(14.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.no_direct_route),
                    color = OrangeAccent,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Konten utama: jika rute sudah siap, tampilkan Seksi Alarm Radius dan Mulai Perjalanan
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // Seksi Rute Terpilih (jika sudah ada)
            uiState.selectedRouteCandidate?.let { candidate ->
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = LightGreenChip),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .clickable {
                                if (uiState.directRoutes.size > 1) {
                                    onSelectRouteCandidate(candidate)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Rute: ${candidate.route.code} · ${candidate.route.name}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = NavyPrimary
                                )
                                Text(
                                    text = "${candidate.stops.size} halte dilewati",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            if (uiState.directRoutes.size > 1) {
                                Text(
                                    text = "Ubah",
                                    color = OrangeAccent,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Seksi "Alarm Radius" (Kartu Oranye #E0490E)
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = OrangeAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("alarm_radius_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = stringResource(
                                id = if (isTrain) R.string.alarm_radius_title_train else R.string.alarm_radius_title_bus
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Pemilih mode alarm: Jarak / Waktu / Keduanya
                        AlarmModeSelector(
                            selected = uiState.selectedAlarmMode,
                            onSelect = onAlarmModeChange
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = alarmModeHint(uiState.selectedAlarmMode, uiState.selectedMinutesThreshold),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextWhite.copy(alpha = 0.9f)
                        )

                        val showDistance = uiState.selectedAlarmMode != com.hanyz.stopme.model.AlarmMode.TIME
                        val showTime = uiState.selectedAlarmMode != com.hanyz.stopme.model.AlarmMode.DISTANCE

                        if (showDistance) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Label kecil di atas knob slider
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .background(TextWhite, MaterialTheme.shapes.small)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = radiusLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = OrangeAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Slider bertitik
                        val maxIndex = (radiusOptions.size - 1).toFloat()
                        Slider(
                            value = uiState.selectedRadiusIndex.toFloat(),
                            onValueChange = { onRadiusIndexChange(it.toInt()) },
                            valueRange = 0f..maxIndex,
                            steps = radiusOptions.size - 2,
                            colors = SliderDefaults.colors(
                                thumbColor = TextWhite,
                                activeTrackColor = TextWhite,
                                inactiveTrackColor = TextWhite.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("slider_radius")
                        )

                        } // akhir showDistance

                        if (showTime) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Pilihan menit sebelum tiba (3 / 4 / 5 menit)
                        Text(
                            text = "Bunyikan saat tersisa",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextWhite.copy(alpha = 0.9f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            uiState.minuteOptions.forEach { minutes ->
                                val selected = uiState.selectedMinutesThreshold == minutes
                                FilterChip(
                                    selected = selected,
                                    onClick = { onMinutesThresholdChange(minutes) },
                                    label = {
                                        Text(
                                            text = "$minutes ${stringResource(id = R.string.minutes_suffix)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TextWhite,
                                        selectedLabelColor = OrangeAccent,
                                        containerColor = TextWhite.copy(alpha = 0.2f),
                                        labelColor = TextWhite
                                    ),
                                    border = null
                                )
                            }
                        }
                        } // akhir showTime
                    }
                }
            }

            // Label Seksi "Halte" atau "Stasiun"
            item {
                Text(
                    text = stringResource(
                        id = if (isTrain) R.string.section_stops_train else R.string.section_stops_bus
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextDark,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Daftar Halte: kartu #1C1E3A dengan ikon bus/kereta putih
            val visibleStops = if (uiState.showAllStops) {
                uiState.filteredStopNames
            } else {
                uiState.filteredStopNames.take(10)
            }

            items(visibleStops, key = { it }) { stopName ->
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = DarkNavyCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clickable { onStopSelected(stopName) }
                        .testTag("stop_item_$stopName")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isTrain) R.drawable.ic_train_white else R.drawable.ic_bus_white
                            ),
                            contentDescription = null,
                            tint = TextWhite,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        // Nama halte + nomor rute yang melewatinya, mis. "Kampung Melayu (5, 7U, 11D)"
                        val codes = uiState.stopRouteCodes[stopName].orEmpty()
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append(stopName) }
                                if (codes.isNotEmpty()) {
                                    withStyle(SpanStyle(color = TextWhite.copy(alpha = 0.65f), fontWeight = FontWeight.Normal)) {
                                        append(" ($codes)")
                                    }
                                }
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextWhite,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Tombol "Selengkapnya" bila halte lebih dari 10
            val hiddenCount = uiState.filteredStopNames.size - visibleStops.size
            if (hiddenCount > 0) {
                item(key = "btn_show_all") {
                    TextButton(
                        onClick = onShowAllStops,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("btn_show_all_stops")
                    ) {
                        Text(
                            text = "Selengkapnya ($hiddenCount ${if (isTrain) "stasiun" else "halte"} lagi)",
                            style = MaterialTheme.typography.titleSmall,
                            color = NavyPrimary
                        )
                    }
                }
            }
        }

        // Tombol "Mulai Perjalanan" di bawah
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Button(
                onClick = onStartTripClick,
                enabled = uiState.isReadyToStart,
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
                    .testTag("btn_start_trip")
            ) {
                Text(
                    text = stringResource(id = R.string.start_trip),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    // Bottom Sheet Pilihan Rute jika ada lebih dari 1 rute langsung
    if (uiState.showRouteSelectionSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismissRouteSheet,
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.choose_route_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = NavyPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                uiState.directRoutes.forEach { candidate ->
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = SecondaryBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onSelectRouteCandidate(candidate) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "${candidate.route.code} · ${candidate.route.name}",
                                style = MaterialTheme.typography.titleSmall,
                                color = NavyPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${candidate.stops.size} halte · estimasi waktu perjalanan langsung",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private val ColorErrorSoft = androidx.compose.ui.graphics.Color(0xFFFFEBEE)


// Tiga tombol pilihan mode alarm di dalam kartu oranye
@Composable
private fun AlarmModeSelector(
    selected: com.hanyz.stopme.model.AlarmMode,
    onSelect: (com.hanyz.stopme.model.AlarmMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TextWhite.copy(alpha = 0.2f), MaterialTheme.shapes.medium)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        com.hanyz.stopme.model.AlarmMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (isSelected) TextWhite else androidx.compose.ui.graphics.Color.Transparent, MaterialTheme.shapes.small)
                    .clickable { onSelect(mode) }
                    .padding(vertical = 8.dp)
                    .testTag("alarm_mode_${mode.name}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) OrangeAccent else TextWhite
                )
            }
        }
    }
}

private fun alarmModeHint(mode: com.hanyz.stopme.model.AlarmMode, minutes: Int): String = when (mode) {
    com.hanyz.stopme.model.AlarmMode.DISTANCE ->
        "Alarm bunyi saat sisa jarak mencapai radius. Jika GPS hilang (mis. MRT bawah tanah), dipakai perkiraan $minutes menit sebelum tiba."
    com.hanyz.stopme.model.AlarmMode.TIME ->
        "Alarm bunyi saat perkiraan waktu tiba tinggal beberapa menit."
    com.hanyz.stopme.model.AlarmMode.BOTH ->
        "Dua alarm terpisah: jarak dan waktu. Jika keduanya hampir bersamaan (kurang dari 30 detik), digabung jadi satu alarm yang lebih kuat."
}
