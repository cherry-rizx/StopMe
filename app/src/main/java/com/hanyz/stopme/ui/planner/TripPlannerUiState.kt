package com.hanyz.stopme.ui.planner

import com.hanyz.stopme.model.DirectRouteCandidate
import com.hanyz.stopme.model.TransportServiceType

data class TripPlannerUiState(
    val serviceType: TransportServiceType = TransportServiceType.TRANSJAKARTA,
    val departureQuery: String = "",
    val destinationQuery: String = "",
    val activeField: ActiveSearchField = ActiveSearchField.DEPARTURE,
    val selectedDepartureStopName: String? = null,
    val selectedDestinationStopName: String? = null,
    val filteredStopNames: List<String> = emptyList(),
    val stopRouteCodes: Map<String, String> = emptyMap(), // nama halte -> "5, 7U, 11D"
    val showAllStops: Boolean = false,                    // false = tampilkan 10 dulu
    val directRoutes: List<DirectRouteCandidate> = emptyList(),
    val selectedRouteCandidate: DirectRouteCandidate? = null,
    val showRouteSelectionSheet: Boolean = false,
    val noDirectRouteFound: Boolean = false,
    // Radius alarm slider
    val selectedRadiusIndex: Int = 3, // index default: 500m untuk bus (dari [200, 300, 400, 500, 1000, 1500, 2000]), atau index 1 (2km) untuk kereta
    val busRadiusOptions: List<Int> = listOf(200, 300, 400, 500, 1000, 1500, 2000),
    val trainRadiusOptions: List<Int> = listOf(1000, 2000, 3000, 4000, 5000, 6000, 7000),
    // Menit tersisa
    val minuteOptions: List<Int> = listOf(3, 4, 5),
    val selectedMinutesThreshold: Int = 4,
    // Mode pemicu alarm: Jarak / Waktu / Keduanya
    val selectedAlarmMode: com.hanyz.stopme.model.AlarmMode = com.hanyz.stopme.model.AlarmMode.DISTANCE,
    val isReadyToStart: Boolean = false
)

enum class ActiveSearchField {
    DEPARTURE,
    DESTINATION
}
