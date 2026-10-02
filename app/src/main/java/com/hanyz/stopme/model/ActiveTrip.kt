package com.hanyz.stopme.model

import kotlinx.serialization.Serializable

enum class TransportServiceType(
    val title: String,
    val modeIds: List<String>,
    val isTrain: Boolean,
    val stopLabel: String,
    val stopSearchPlaceholder: String
) {
    TRANSJAKARTA("Transjakarta", listOf("transjakarta"), false, "Halte", "Cari Halte"),
    JAKLINGKO("Jaklingko", listOf("mikrotrans"), false, "Halte", "Cari Halte"),
    KRL("KAI Commuter", listOf("krl"), true, "Stasiun", "Cari Stasiun"),
    MRT("MRT", listOf("mrt"), true, "Stasiun", "Cari Stasiun"),
    // LRT sementara disembunyikan dari Home (lihat HIDDEN_SERVICES di HomeUiState.kt).
    // Data dan logikanya tetap ada agar mudah diaktifkan kembali.
    LRT("LRT", listOf("lrt_jakarta", "lrt_jabodebek"), true, "Stasiun", "Cari Stasiun");

    companion object {
        fun fromModeId(modeId: String): TransportServiceType {
            return entries.find { modeId in it.modeIds } ?: TRANSJAKARTA
        }
    }
}

@Serializable
data class StopWithSeq(
    val stop: TransitStop,
    val seq: Int,
    val travelSecFromPrevious: Int
)

data class DirectRouteCandidate(
    val route: TransitRoute,
    val departureSeq: Int,
    val destinationSeq: Int,
    val stops: List<StopWithSeq>
)

// Mode pemicu alarm yang dipilih user
@Serializable
enum class AlarmMode(val label: String) {
    DISTANCE("Jarak"),
    TIME("Waktu"),
    BOTH("Keduanya")
}

@Serializable
data class ActiveTripConfig(
    val routeId: String,
    val routeCode: String,
    val routeName: String,
    val modeId: String,
    val isTrain: Boolean,
    val departureStopName: String,
    val destinationStopName: String,
    val departureSeq: Int,
    val destinationSeq: Int,
    val stopsList: List<StopWithSeq>,
    val alarmRadiusMeters: Int,
    val alarmMinutesThreshold: Int,
    val alarmMode: AlarmMode = AlarmMode.BOTH // default untuk riwayat lama
)

data class TripRealtimeState(
    val hasActiveTrip: Boolean = false,
    val tripConfig: ActiveTripConfig? = null,
    val userLat: Double = 0.0,
    val userLng: Double = 0.0,
    val userSpeedMps: Double = 0.0,
    val currentPassedSeq: Int = 0,
    val nextStop: TransitStop? = null,
    val nextStopSeq: Int = 0,
    val remainingDistanceMeters: Double = 0.0,
    val etaSeconds: Int = 0,
    val isOffline: Boolean = false,
    val isAlarmTriggered: Boolean = false,
    val isCompleted: Boolean = false,
    // Info alarm yang sedang/terakhir berbunyi
    val alarmTitle: String = "",
    val isFinalAlarm: Boolean = true,
    val distanceAlarmFired: Boolean = false,
    val timeAlarmFired: Boolean = false
)
