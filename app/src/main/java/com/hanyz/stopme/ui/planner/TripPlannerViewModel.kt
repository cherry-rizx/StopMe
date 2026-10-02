package com.hanyz.stopme.ui.planner

import androidx.lifecycle.ViewModel
import com.hanyz.stopme.data.TransitRepository
import com.hanyz.stopme.data.TripRepository
import com.hanyz.stopme.model.ActiveTripConfig
import com.hanyz.stopme.model.DirectRouteCandidate
import com.hanyz.stopme.model.TransportServiceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TripPlannerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TripPlannerUiState())
    val uiState: StateFlow<TripPlannerUiState> = _uiState.asStateFlow()

    fun initService(serviceType: TransportServiceType) {
        val defaultRadiusIdx = if (serviceType.isTrain) 1 else 3 // 2km untuk kereta, 500m untuk bus
        _uiState.update {
            it.copy(
                serviceType = serviceType,
                selectedRadiusIndex = defaultRadiusIdx,
                selectedMinutesThreshold = 4
            )
        }
        updateFilteredStops()
    }

    fun onActiveFieldChanged(field: ActiveSearchField) {
        _uiState.update { it.copy(activeField = field) }
        updateFilteredStops()
    }

    fun onDepartureQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                departureQuery = query,
                activeField = ActiveSearchField.DEPARTURE,
                selectedDepartureStopName = null
            )
        }
        updateFilteredStops()
    }

    fun onDestinationQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                destinationQuery = query,
                activeField = ActiveSearchField.DESTINATION,
                selectedDestinationStopName = null
            )
        }
        updateFilteredStops()
    }

    fun onStopSelected(stopName: String) {
        val currentField = _uiState.value.activeField
        if (currentField == ActiveSearchField.DEPARTURE) {
            _uiState.update {
                it.copy(
                    selectedDepartureStopName = stopName,
                    departureQuery = stopName,
                    activeField = ActiveSearchField.DESTINATION
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    selectedDestinationStopName = stopName,
                    destinationQuery = stopName
                )
            }
        }

        updateFilteredStops()
        checkAndFindDirectRoutes()
    }

    fun onShowAllStops() {
        _uiState.update { it.copy(showAllStops = true) }
    }

    private fun updateFilteredStops() {
        val state = _uiState.value
        val query = if (state.activeField == ActiveSearchField.DEPARTURE) {
            state.departureQuery
        } else {
            state.destinationQuery
        }
        val modeIds = state.serviceType.modeIds
        val stops = TransitRepository.getUniqueStopNames(modeIds, query)
        val codes = stops.associateWith { name ->
            TransitRepository.getRouteCodesForStopName(name, modeIds).joinToString(", ")
        }
        // Setiap pencarian baru kembali menampilkan 10 halte teratas
        _uiState.update { it.copy(filteredStopNames = stops, stopRouteCodes = codes, showAllStops = false) }
    }

    private fun checkAndFindDirectRoutes() {
        val dep = _uiState.value.selectedDepartureStopName
        val dest = _uiState.value.selectedDestinationStopName
        val modeIds = _uiState.value.serviceType.modeIds

        if (dep != null && dest != null && dep != dest) {
            val directRoutes = TransitRepository.findDirectRoutes(dep, dest, modeIds)
            if (directRoutes.isEmpty()) {
                _uiState.update {
                    it.copy(
                        directRoutes = emptyList(),
                        selectedRouteCandidate = null,
                        noDirectRouteFound = true,
                        showRouteSelectionSheet = false,
                        isReadyToStart = false
                    )
                }
            } else if (directRoutes.size == 1) {
                _uiState.update {
                    it.copy(
                        directRoutes = directRoutes,
                        selectedRouteCandidate = directRoutes.first(),
                        noDirectRouteFound = false,
                        showRouteSelectionSheet = false,
                        isReadyToStart = true
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        directRoutes = directRoutes,
                        selectedRouteCandidate = directRoutes.first(),
                        noDirectRouteFound = false,
                        showRouteSelectionSheet = true,
                        isReadyToStart = true
                    )
                }
            }
        }
    }

    fun selectRouteCandidate(candidate: DirectRouteCandidate) {
        _uiState.update {
            it.copy(
                selectedRouteCandidate = candidate,
                showRouteSelectionSheet = false,
                isReadyToStart = true
            )
        }
    }

    fun dismissRouteSelectionSheet() {
        _uiState.update { it.copy(showRouteSelectionSheet = false) }
    }

    fun onRadiusIndexChanged(index: Int) {
        _uiState.update { it.copy(selectedRadiusIndex = index) }
    }

    fun onAlarmModeChanged(mode: com.hanyz.stopme.model.AlarmMode) {
        _uiState.update { it.copy(selectedAlarmMode = mode) }
    }

    fun onMinutesThresholdChanged(minutes: Int) {
        _uiState.update { it.copy(selectedMinutesThreshold = minutes) }
    }

    // Buat konfigurasi dan simpan ke TripRepository
    fun startTrip(): Boolean {
        val state = _uiState.value
        val candidate = state.selectedRouteCandidate ?: return false
        val depName = state.selectedDepartureStopName ?: return false
        val destName = state.selectedDestinationStopName ?: return false

        val radiusOptions = if (state.serviceType.isTrain) state.trainRadiusOptions else state.busRadiusOptions
        val radiusMeters = radiusOptions.getOrElse(state.selectedRadiusIndex) { 500 }

        val config = ActiveTripConfig(
            routeId = candidate.route.id,
            routeCode = candidate.route.code,
            routeName = candidate.route.name,
            modeId = candidate.route.modeId,
            isTrain = state.serviceType.isTrain,
            departureStopName = depName,
            destinationStopName = destName,
            departureSeq = candidate.departureSeq,
            destinationSeq = candidate.destinationSeq,
            stopsList = candidate.stops,
            alarmRadiusMeters = radiusMeters,
            alarmMinutesThreshold = state.selectedMinutesThreshold,
            alarmMode = state.selectedAlarmMode
        )

        TripRepository.startTrip(config)
        return true
    }
}
