package com.hanyz.stopme.data

import android.content.Context
import com.hanyz.stopme.model.DirectRouteCandidate
import com.hanyz.stopme.model.StopWithSeq
import com.hanyz.stopme.model.TransitDataContainer
import com.hanyz.stopme.model.TransitMode
import com.hanyz.stopme.model.TransitRoute
import com.hanyz.stopme.model.TransitRouteStop
import com.hanyz.stopme.model.TransitStop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

object TransitRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Indeks cepat di memori
    private var stopsById: Map<String, TransitStop> = emptyMap()
    private var routeStopsByRouteId: Map<String, List<TransitRouteStop>> = emptyMap()
    private var stopIdsByName: Map<String, List<String>> = emptyMap()
    private var routesById: Map<String, TransitRoute> = emptyMap()
    private var modesById: Map<String, TransitMode> = emptyMap()
    private var stopNamesByMode: Map<String, List<String>> = emptyMap()
    private var routeIdsByStopId: Map<String, Set<String>> = emptyMap()

    // Muat dan parse JSON di background thread
    suspend fun loadTransitData(context: Context) {
        if (_isLoaded.value) return
        _isLoading.value = true

        withContext(Dispatchers.IO) {
            try {
                val jsonString = context.assets.open("transit_data.json")
                    .bufferedReader()
                    .use { it.readText() }

                val container: TransitDataContainer = json.decodeFromString(jsonString)

                stopsById = container.stops.associateBy { it.id }
                routesById = container.routes.associateBy { it.id }
                modesById = container.modes.associateBy { it.id }

                // Group route_stops per route_id terurut seq
                routeStopsByRouteId = container.routeStops
                    .groupBy { it.routeId }
                    .mapValues { (_, stops) -> stops.sortedBy { it.seq } }

                // Rute apa saja yang melewati tiap halte
                val tempRoutesByStop = mutableMapOf<String, MutableSet<String>>()
                for (rs in container.routeStops) {
                    tempRoutesByStop.getOrPut(rs.stopId) { mutableSetOf() }.add(rs.routeId)
                }
                routeIdsByStopId = tempRoutesByStop

                // Daftar stop_id per nama halte (case-insensitive)
                val tempStopIdsByName = mutableMapOf<String, MutableList<String>>()
                for (stop in container.stops) {
                    val key = stop.name.trim().lowercase()
                    tempStopIdsByName.getOrPut(key) { mutableListOf() }.add(stop.id)
                }
                stopIdsByName = tempStopIdsByName

                // Indeks nama unik halte per mode
                val tempStopNamesByMode = mutableMapOf<String, MutableSet<String>>()
                for ((routeId, rStops) in routeStopsByRouteId) {
                    val route = routesById[routeId] ?: continue
                    val set = tempStopNamesByMode.getOrPut(route.modeId) { mutableSetOf() }
                    for (rs in rStops) {
                        val stop = stopsById[rs.stopId]
                        if (stop != null) {
                            set.add(stop.name)
                        }
                    }
                }
                stopNamesByMode = tempStopNamesByMode.mapValues { (_, names) ->
                    names.sorted()
                }

                _isLoaded.value = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Ambil daftar nama halte unik berdasarkan kumpulan mode_id dan filter kata kunci
    fun getUniqueStopNames(modeIds: List<String>, query: String = ""): List<String> {
        val allNames = mutableSetOf<String>()
        for (modeId in modeIds) {
            stopNamesByMode[modeId]?.let { allNames.addAll(it) }
        }
        val trimmed = query.trim().lowercase()
        return allNames.filter {
            if (trimmed.isEmpty()) true else it.lowercase().contains(trimmed)
        }.sorted()
    }

    // Nomor rute yang melewati sebuah nama halte.
    // Aturan bus: halte BRT (id diawali "TJ-G") -> nomor Transjakarta saja;
    // bus stop pinggir jalan -> nomor Transjakarta + Mikrotrans. Kereta -> nama lin.
    fun getRouteCodesForStopName(stopName: String, modeIds: List<String>): List<String> {
        val ids = stopIdsByName[stopName.trim().lowercase()] ?: return emptyList()
        val isBusService = modeIds.any { it == "transjakarta" || it == "mikrotrans" }
        val codes = linkedSetOf<String>()
        for (sid in ids) {
            val isBrtHalte = sid.startsWith("TJ-G")
            for (rid in routeIdsByStopId[sid].orEmpty()) {
                val route = routesById[rid] ?: continue
                val allowed = if (isBusService) {
                    route.modeId == "transjakarta" || (route.modeId == "mikrotrans" && !isBrtHalte)
                } else {
                    route.modeId in modeIds
                }
                if (allowed) codes.add(route.code)
            }
        }
        return codes.sortedWith(routeCodeComparator)
    }

    private val digitsRegex = Regex("\\d+")

    // Urutan alami: 1, 2, 2A, 10 ... lalu JAK01, JAK02 ...
    private val routeCodeComparator = Comparator<String> { a, b ->
        val ga = if (a.startsWith("JAK")) 1 else 0
        val gb = if (b.startsWith("JAK")) 1 else 0
        if (ga != gb) return@Comparator ga - gb
        val na = digitsRegex.find(a)?.value?.toIntOrNull() ?: Int.MAX_VALUE
        val nb = digitsRegex.find(b)?.value?.toIntOrNull() ?: Int.MAX_VALUE
        if (na != nb) na.compareTo(nb) else a.compareTo(b)
    }

    // Cari rute langsung yang memuat halte keberangkatan pada seq i dan tujuan pada seq j > i
    fun findDirectRoutes(
        departureStopName: String,
        destinationStopName: String,
        modeIds: List<String>
    ): List<DirectRouteCandidate> {
        val depKey = departureStopName.trim().lowercase()
        val destKey = destinationStopName.trim().lowercase()
        val depStopIds = stopIdsByName[depKey]?.toSet() ?: return emptyList()
        val destStopIds = stopIdsByName[destKey]?.toSet() ?: return emptyList()

        val results = mutableListOf<DirectRouteCandidate>()

        for (route in routesById.values) {
            if (route.modeId !in modeIds) continue
            val rStops = routeStopsByRouteId[route.id] ?: continue

            // Cari kemunculan halte keberangkatan dan tujuan
            var bestDepIndex = -1
            var bestDestIndex = -1

            for (i in rStops.indices) {
                if (rStops[i].stopId in depStopIds) {
                    for (j in (i + 1) until rStops.size) {
                        if (rStops[j].stopId in destStopIds) {
                            bestDepIndex = i
                            bestDestIndex = j
                            break
                        }
                    }
                }
                if (bestDepIndex != -1) break
            }

            if (bestDepIndex != -1 && bestDestIndex != -1 && bestDestIndex > bestDepIndex) {
                val subStops = rStops.subList(bestDepIndex, bestDestIndex + 1).mapNotNull { rs ->
                    val stop = stopsById[rs.stopId]
                    if (stop != null) {
                        StopWithSeq(
                            stop = stop,
                            seq = rs.seq,
                            travelSecFromPrevious = rs.travelSec
                        )
                    } else null
                }
                results.add(
                    DirectRouteCandidate(
                        route = route,
                        departureSeq = rStops[bestDepIndex].seq,
                        destinationSeq = rStops[bestDestIndex].seq,
                        stops = subStops
                    )
                )
            }
        }

        return results
    }

    fun getStopById(stopId: String): TransitStop? = stopsById[stopId]
}
