package com.hanyz.stopme.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransitDataContainer(
    val modes: List<TransitMode> = emptyList(),
    val routes: List<TransitRoute> = emptyList(),
    val stops: List<TransitStop> = emptyList(),
    @SerialName("route_stops")
    val routeStops: List<TransitRouteStop> = emptyList()
)

@Serializable
data class TransitMode(
    val id: String,
    val name: String
)

@Serializable
data class TransitRoute(
    val id: String,
    @SerialName("mode_id")
    val modeId: String,
    val code: String,
    val name: String,
    val direction: Int = 0
)

@Serializable
data class TransitStop(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double
)

@Serializable
data class TransitRouteStop(
    @SerialName("route_id")
    val routeId: String,
    @SerialName("stop_id")
    val stopId: String,
    val seq: Int,
    @SerialName("travel_sec")
    val travelSec: Int = 0
)
