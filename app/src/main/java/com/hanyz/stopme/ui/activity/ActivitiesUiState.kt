package com.hanyz.stopme.ui.activity

import com.hanyz.stopme.model.ActiveTripConfig
import com.hanyz.stopme.model.TransitStop

// Satu baris di daftar Riwayat / Favorit
data class HistoryItemUi(
    val id: String,
    val routeKey: String,
    val routeCode: String,
    val routeName: String,
    val departureName: String,
    val destinationName: String,
    val modeLabel: String,
    val isTrain: Boolean,
    val dateText: String,
    val isFavorite: Boolean,
    val usageCount: Int,
    val endStatus: String,
    val stopNames: List<String>,
    val radiusLabel: String,
    val minutesThreshold: Int,
    val config: ActiveTripConfig
)

enum class HistoryTab { RIWAYAT, FAVORIT }

// Titik koordinat sederhana agar UI tidak bergantung pada library peta
data class MapPoint(val lat: Double, val lng: Double)

data class ActivitiesUiState(
    val hasActiveTrip: Boolean = false,
    val tripConfig: ActiveTripConfig? = null,
    val userPoint: MapPoint = MapPoint(-6.2088, 106.8456), // Jakarta center default
    val routePoints: List<MapPoint> = emptyList(),
    val stopsList: List<TransitStop> = emptyList(),
    val destinationStop: TransitStop? = null,
    val destinationName: String = "",
    val remainingDistanceStr: String = "0 m",
    val etaStr: String = "0 mnt",
    val nextStopName: String = "-",
    val isTrain: Boolean = false,
    val activeAlarmRadiusLabel: String = "",
    val activeAlarmMinutes: Int = 4,
    val isOffline: Boolean = false,
    val isAlarmTriggered: Boolean = false,
    val isDarkMap: Boolean = true,
    val alarmChipText: String = "",   // mis. "Mode Keduanya · 500 m & 4 menit · Alarm 1 sudah bunyi"
    // Riwayat & Favorit (tampil saat tidak ada perjalanan aktif)
    val selectedHistoryTab: HistoryTab = HistoryTab.RIWAYAT,
    val historyItems: List<HistoryItemUi> = emptyList(),
    val favoriteItems: List<HistoryItemUi> = emptyList(),
    val detailItem: HistoryItemUi? = null
)
