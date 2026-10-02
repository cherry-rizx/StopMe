package com.hanyz.stopme.ui.activity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanyz.stopme.data.TripHistoryRepository
import com.hanyz.stopme.data.TripHistoryStore
import com.hanyz.stopme.data.TripRepository
import com.hanyz.stopme.model.ActiveTripConfig
import com.hanyz.stopme.model.TransportServiceType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.hanyz.stopme.service.TrackingService
import com.hanyz.stopme.util.LocationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ActivitiesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ActivitiesUiState())
    val uiState: StateFlow<ActivitiesUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("EEE, d MMM yyyy • HH:mm", Locale("id", "ID"))

    init {
        observeTripState()
        observeHistory()
    }

    // Ubah data riwayat mentah menjadi item tampilan
    private fun observeHistory() {
        viewModelScope.launch {
            TripHistoryRepository.store.collect { store -> applyHistory(store) }
        }
    }

    private fun applyHistory(store: TripHistoryStore) {
        val favKeys = store.favorites.map { it.routeKey }.toSet()
        val counts = store.entries.groupingBy { it.routeKey }.eachCount()

        val history = store.entries.map { e ->
            toItem(e.id, e.config, e.timestampMillis, favKeys, counts, e.endStatus)
        }
        val favorites = store.favorites.map { f ->
            val lastUse = store.entries.firstOrNull { it.routeKey == f.routeKey }
            toItem(
                id = "fav-${f.routeKey}",
                config = f.config,
                timestamp = lastUse?.timestampMillis,
                favKeys = favKeys,
                counts = counts,
                status = lastUse?.endStatus ?: "-"
            )
        }
        _uiState.update { state ->
            // Perbarui detail yang sedang terbuka agar status bintang ikut berubah
            val refreshedDetail = state.detailItem?.let { d ->
                (history + favorites).firstOrNull { it.id == d.id }
            }
            state.copy(historyItems = history, favoriteItems = favorites, detailItem = refreshedDetail)
        }
    }

    private fun toItem(
        id: String,
        config: ActiveTripConfig,
        timestamp: Long?,
        favKeys: Set<String>,
        counts: Map<String, Int>,
        status: String
    ): HistoryItemUi {
        val key = TripHistoryRepository.keyOf(config)
        return HistoryItemUi(
            id = id,
            routeKey = key,
            routeCode = config.routeCode,
            routeName = config.routeName,
            departureName = config.departureStopName,
            destinationName = config.destinationStopName,
            modeLabel = TransportServiceType.fromModeId(config.modeId).title,
            isTrain = config.isTrain,
            dateText = timestamp?.let { dateFormat.format(Date(it)) } ?: "-",
            isFavorite = key in favKeys,
            usageCount = counts[key] ?: 0,
            endStatus = status,
            stopNames = config.stopsList.map { it.stop.name },
            radiusLabel = LocationUtils.formatDistance(config.alarmRadiusMeters.toDouble()),
            minutesThreshold = config.alarmMinutesThreshold,
            config = config
        )
    }

    // Teks chip alarm sesuai mode yang dipilih
    private fun buildAlarmChip(
        config: ActiveTripConfig,
        radiusStr: String,
        distanceFired: Boolean,
        timeFired: Boolean
    ): String {
        val minutes = "${config.alarmMinutesThreshold} menit"
        return when (config.alarmMode) {
            com.hanyz.stopme.model.AlarmMode.DISTANCE -> "Alarm jarak aktif: $radiusStr"
            com.hanyz.stopme.model.AlarmMode.TIME -> "Alarm waktu aktif: $minutes sebelum tiba"
            com.hanyz.stopme.model.AlarmMode.BOTH -> {
                val progress = when {
                    distanceFired && timeFired -> " · selesai"
                    distanceFired -> " · alarm jarak sudah bunyi"
                    timeFired -> " · alarm waktu sudah bunyi"
                    else -> ""
                }
                "2 alarm: $radiusStr & $minutes$progress"
            }
        }
    }

    fun selectHistoryTab(tab: HistoryTab) {
        _uiState.update { it.copy(selectedHistoryTab = tab) }
    }

    fun toggleFavorite(item: HistoryItemUi) {
        TripHistoryRepository.toggleFavorite(item.config)
    }

    fun showDetail(item: HistoryItemUi) {
        _uiState.update { it.copy(detailItem = item) }
    }

    fun dismissDetail() {
        _uiState.update { it.copy(detailItem = null) }
    }

    // Mulai lagi perjalanan dari riwayat/favorit dengan pengaturan yang sama
    fun startFromHistory(context: Context, item: HistoryItemUi) {
        _uiState.update { it.copy(detailItem = null) }
        TripRepository.startTrip(item.config)
        TrackingService.start(context)
    }

    private fun observeTripState() {
        viewModelScope.launch {
            TripRepository.tripState.collect { tripState ->
                if (!tripState.hasActiveTrip || tripState.tripConfig == null) {
                    _uiState.update { it.copy(hasActiveTrip = false, tripConfig = null) }
                    return@collect
                }

                val config = tripState.tripConfig
                val points = config.stopsList.map { MapPoint(it.stop.lat, it.stop.lng) }
                val stops = config.stopsList.map { it.stop }
                val destStop = stops.lastOrNull()

                val radiusStr = LocationUtils.formatDistance(config.alarmRadiusMeters.toDouble())
                val remainingDistStr = LocationUtils.formatDistance(tripState.remainingDistanceMeters)
                val etaStr = LocationUtils.formatEta(tripState.etaSeconds)
                val nextName = tripState.nextStop?.name ?: config.destinationStopName

                val userPos = if (tripState.userLat != 0.0 && tripState.userLng != 0.0) {
                    MapPoint(tripState.userLat, tripState.userLng)
                } else if (stops.isNotEmpty()) {
                    MapPoint(stops.first().lat, stops.first().lng)
                } else {
                    MapPoint(-6.2088, 106.8456)
                }

                _uiState.update {
                    it.copy(
                        hasActiveTrip = true,
                        tripConfig = config,
                        userPoint = userPos,
                        routePoints = points,
                        stopsList = stops,
                        destinationStop = destStop,
                        destinationName = config.destinationStopName,
                        remainingDistanceStr = remainingDistStr,
                        etaStr = etaStr,
                        nextStopName = nextName,
                        isTrain = config.isTrain,
                        activeAlarmRadiusLabel = radiusStr,
                        activeAlarmMinutes = config.alarmMinutesThreshold,
                        alarmChipText = buildAlarmChip(
                            config, radiusStr,
                            tripState.distanceAlarmFired, tripState.timeAlarmFired
                        ),
                        isOffline = tripState.isOffline,
                        isAlarmTriggered = tripState.isAlarmTriggered
                    )
                }
            }
        }
    }

    // Ganti tampilan peta gelap/terang
    fun toggleMapStyle() {
        _uiState.update { it.copy(isDarkMap = !it.isDarkMap) }
    }

    fun stopTrip(context: Context) {
        TrackingService.stop(context)
        TripRepository.stopTrip()
    }
}
