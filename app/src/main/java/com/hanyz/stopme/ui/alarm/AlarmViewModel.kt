package com.hanyz.stopme.ui.alarm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanyz.stopme.data.TripRepository
import com.hanyz.stopme.service.TrackingService
import com.hanyz.stopme.util.LocationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AlarmViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmUiState())
    val uiState: StateFlow<AlarmUiState> = _uiState.asStateFlow()

    init {
        observeTrip()
    }

    private fun observeTrip() {
        viewModelScope.launch {
            TripRepository.tripState.collect { tripState ->
                val dest = tripState.tripConfig?.destinationStopName ?: ""
                val arrivalInfo = if (tripState.isOffline) {
                    val minutes = (tripState.etaSeconds + 59) / 60
                    "$minutes menit"
                } else {
                    LocationUtils.formatDistance(tripState.remainingDistanceMeters)
                }

                _uiState.update {
                    it.copy(
                        destinationName = dest,
                        arrivalInfoStr = arrivalInfo,
                        isOffline = tripState.isOffline,
                        alarmTitle = tripState.alarmTitle,
                        isFinalAlarm = tripState.isFinalAlarm
                    )
                }
            }
        }
    }

    // Service yang memutuskan: berhenti total (alarm terakhir) atau lanjut ke alarm berikutnya
    fun dismissAlarm(context: Context) {
        TrackingService.dismissAlarm(context)
        TripRepository.setAlarmRinging(false)
        _uiState.update { it.copy(isDismissed = true) }
    }
}
