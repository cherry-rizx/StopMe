package com.hanyz.stopme.ui.activity

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hanyz.stopme.ui.theme.DarkNavyCard
import com.hanyz.stopme.ui.theme.NavyPrimary
import com.hanyz.stopme.ui.theme.OrangeAccent
import com.hanyz.stopme.ui.theme.SecondaryBackground
import com.hanyz.stopme.ui.theme.TextSecondary
import com.hanyz.stopme.ui.theme.TextWhite

private val StarYellow = Color(0xFFF5B301)

// Konten tab Aktivitas saat tidak ada perjalanan aktif
@Composable
fun HistoryContent(
    uiState: ActivitiesUiState,
    onTabSelected: (HistoryTab) -> Unit,
    onToggleFavorite: (HistoryItemUi) -> Unit,
    onShowDetail: (HistoryItemUi) -> Unit,
    onDismissDetail: () -> Unit,
    onStartFromHistory: (HistoryItemUi) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header: Riwayat | Favorit Rute
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .background(SecondaryBackground, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TabPill(
                label = "Riwayat",
                icon = Icons.Outlined.History,
                selected = uiState.selectedHistoryTab == HistoryTab.RIWAYAT,
                onClick = { onTabSelected(HistoryTab.RIWAYAT) },
                modifier = Modifier.weight(1f)
            )
            TabPill(
                label = "Favorit Rute",
                icon = Icons.Filled.Star,
                selected = uiState.selectedHistoryTab == HistoryTab.FAVORIT,
                onClick = { onTabSelected(HistoryTab.FAVORIT) },
                modifier = Modifier.weight(1f)
            )
        }

        val items = if (uiState.selectedHistoryTab == HistoryTab.RIWAYAT) {
            uiState.historyItems
        } else {
            uiState.favoriteItems
        }

        if (items.isEmpty()) {
            EmptyHistory(isFavoriteTab = uiState.selectedHistoryTab == HistoryTab.FAVORIT)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.id }) { item ->
                    HistoryCard(
                        item = item,
                        onToggleFavorite = { onToggleFavorite(item) },
                        onDetail = { onShowDetail(item) },
                        onStart = { onStartFromHistory(item) }
                    )
                }
            }
        }
    }

    uiState.detailItem?.let { item ->
        HistoryDetailDialog(
            item = item,
            onDismiss = onDismissDetail,
            onToggleFavorite = { onToggleFavorite(item) },
            onStart = { onStartFromHistory(item) }
        )
    }
}

@Composable
private fun TabPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(if (selected) NavyPrimary else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) TextWhite else NavyPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) TextWhite else NavyPrimary
        )
    }
}

@Composable
private fun EmptyHistory(isFavoriteTab: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(SecondaryBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavoriteTab) Icons.Outlined.StarBorder else Icons.Outlined.History,
                    contentDescription = null,
                    tint = NavyPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isFavoriteTab) "Belum ada rute favorit" else "Belum ada riwayat perjalanan",
                style = MaterialTheme.typography.titleLarge,
                color = NavyPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isFavoriteTab) {
                    "Rute yang kamu pakai 5 kali akan otomatis masuk ke sini. Kamu juga bisa menekan ikon bintang di Riwayat."
                } else {
                    "Pilih layanan dan atur tujuan di halaman Home untuk memulai perjalanan pertamamu."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HistoryCard(
    item: HistoryItemUi,
    onToggleFavorite: () -> Unit,
    onDetail: () -> Unit,
    onStart: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RouteCodeBadge(code = item.routeCode)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.modeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                FavoriteStar(isFavorite = item.isFavorite, onClick = onToggleFavorite)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${item.departureName} → ${item.destinationName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.dateText} • ${item.endStatus}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDetail,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Detail", color = NavyPrimary)
                }
                Button(
                    onClick = onStart,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavyCard, contentColor = TextWhite),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Mulai")
                }
            }
        }
    }
}

@Composable
private fun RouteCodeBadge(code: String) {
    Text(
        text = code,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = TextWhite,
        modifier = Modifier
            .background(NavyPrimary, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun FavoriteStar(isFavorite: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = if (isFavorite) "Hapus dari favorit" else "Tambah ke favorit",
            tint = if (isFavorite) StarYellow else TextSecondary
        )
    }
}

@Composable
private fun HistoryDetailDialog(
    item: HistoryItemUi,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onStart: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RouteCodeBadge(code = item.routeCode)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.routeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = NavyPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                FavoriteStar(isFavorite = item.isFavorite, onClick = onToggleFavorite)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                DetailRow("Naik", item.departureName)
                DetailRow("Tujuan", item.destinationName)
                DetailRow("Layanan", item.modeLabel)
                DetailRow("Terakhir", item.dateText)
                DetailRow("Dipakai", "${item.usageCount} kali")
                DetailRow("Status", item.endStatus)
                DetailRow(
                    "Alarm",
                    when (item.config.alarmMode) {
                        com.hanyz.stopme.model.AlarmMode.DISTANCE -> "Jarak · ${item.radiusLabel}"
                        com.hanyz.stopme.model.AlarmMode.TIME -> "Waktu · ${item.minutesThreshold} menit sebelum tiba"
                        com.hanyz.stopme.model.AlarmMode.BOTH -> "Keduanya · ${item.radiusLabel} & ${item.minutesThreshold} menit"
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Halte yang dilewati (${item.stopNames.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = NavyPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                item.stopNames.forEachIndexed { index, name ->
                    val isEdge = index == 0 || index == item.stopNames.lastIndex
                    Text(
                        text = "${index + 1}. $name",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isEdge) FontWeight.Bold else FontWeight.Normal,
                        color = if (index == item.stopNames.lastIndex) OrangeAccent else NavyPrimary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = DarkNavyCard, contentColor = TextWhite)
            ) {
                Text("Mulai")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = NavyPrimary)
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.width(76.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = NavyPrimary
        )
    }
}
