package com.hanyz.stopme.data

import com.hanyz.stopme.model.ActiveTripConfig
import com.hanyz.stopme.model.TripRealtimeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object TripRepository {

    private val _tripState = MutableStateFlow(TripRealtimeState())
    val tripState: StateFlow<TripRealtimeState> = _tripState.asStateFlow()

    private val _isAlarmRinging = MutableStateFlow(false)
    val isAlarmRinging: StateFlow<Boolean> = _isAlarmRinging.asStateFlow()

    // Memulai perjalanan baru
    fun startTrip(config: ActiveTripConfig) {
        // Catat ke riwayat (dan favorit otomatis)
        TripHistoryRepository.recordTripStart(config)

        val firstStop = config.stopsList.firstOrNull()?.stop
        val nextStop = if (config.stopsList.size > 1) config.stopsList[1].stop else firstStop
        val nextSeq = if (config.stopsList.size > 1) config.stopsList[1].seq else config.departureSeq

        _tripState.value = TripRealtimeState(
            hasActiveTrip = true,
            tripConfig = config,
            userLat = firstStop?.lat ?: 0.0,
            userLng = firstStop?.lng ?: 0.0,
            currentPassedSeq = config.departureSeq,
            nextStop = nextStop,
            nextStopSeq = nextSeq,
            remainingDistanceMeters = 0.0,
            etaSeconds = 0,
            isOffline = false,
            isAlarmTriggered = false,
            isCompleted = false
        )
        _isAlarmRinging.value = false
    }

    // Perbarui state realtime dari service
    fun updateRealtimeState(transform: (TripRealtimeState) -> TripRealtimeState) {
        _tripState.update(transform)
    }

    // Nyalakan alarm
    fun setAlarmRinging(ringing: Boolean) {
        _isAlarmRinging.value = ringing
        if (ringing) {
            _tripState.update { it.copy(isAlarmTriggered = true) }
        }
    }

    // Hentikan perjalanan
    fun stopTrip() {
        _isAlarmRinging.value = false
        _tripState.value = TripRealtimeState(hasActiveTrip = false)
    }
}
