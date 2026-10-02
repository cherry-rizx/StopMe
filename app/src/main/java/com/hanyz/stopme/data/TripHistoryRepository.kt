package com.hanyz.stopme.data

import android.content.Context
import android.content.SharedPreferences
import com.hanyz.stopme.model.ActiveTripConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Status akhir perjalanan di riwayat
object TripEndStatus {
    const val RUNNING = "Sedang berjalan"
    const val ALARM = "Alarm berbunyi"
    const val STOPPED = "Dihentikan manual"
}

@Serializable
data class TripHistoryEntry(
    val id: String,
    val timestampMillis: Long,
    val config: ActiveTripConfig,
    val endStatus: String = TripEndStatus.RUNNING
) {
    val routeKey: String get() = TripHistoryRepository.keyOf(config)
}

@Serializable
data class FavoriteRoute(
    val routeKey: String,
    val config: ActiveTripConfig,
    val addedManually: Boolean
)

@Serializable
data class TripHistoryStore(
    val entries: List<TripHistoryEntry> = emptyList(),   // terbaru di depan
    val favorites: List<FavoriteRoute> = emptyList(),
    val blockedAutoFavorite: Set<String> = emptySet()    // rute yang sengaja dihapus dari favorit
)

// Riwayat & favorit disimpan di HP (SharedPreferences, format JSON)
object TripHistoryRepository {
    const val AUTO_FAVORITE_THRESHOLD = 5
    private const val MAX_ENTRIES = 100
    private const val PREFS = "stopme_history"
    private const val KEY_STORE = "store_json"

    private val json = Json { ignoreUnknownKeys = true }
    private var prefs: SharedPreferences? = null

    private val _store = MutableStateFlow(TripHistoryStore())
    val store: StateFlow<TripHistoryStore> = _store.asStateFlow()

    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _store.value = try {
            p.getString(KEY_STORE, null)?.let { json.decodeFromString<TripHistoryStore>(it) }
                ?: TripHistoryStore()
        } catch (e: Exception) {
            TripHistoryStore()
        }
    }

    // Rute dianggap sama jika rute, halte naik, dan halte tujuan sama
    fun keyOf(config: ActiveTripConfig): String =
        "${config.routeId}|${config.departureSeq}|${config.destinationSeq}"

    fun usageCount(routeKey: String): Int =
        _store.value.entries.count { it.routeKey == routeKey }

    fun isFavorite(routeKey: String): Boolean =
        _store.value.favorites.any { it.routeKey == routeKey }

    // Dipanggil setiap perjalanan dimulai
    fun recordTripStart(config: ActiveTripConfig) {
        val current = _store.value
        val entry = TripHistoryEntry(
            id = System.currentTimeMillis().toString(),
            timestampMillis = System.currentTimeMillis(),
            config = config
        )
        val entries = (listOf(entry) + current.entries).take(MAX_ENTRIES)
        val key = keyOf(config)
        val count = entries.count { it.routeKey == key }

        var favorites = current.favorites
        val shouldAutoAdd = count >= AUTO_FAVORITE_THRESHOLD &&
                favorites.none { it.routeKey == key } &&
                key !in current.blockedAutoFavorite
        if (shouldAutoAdd) {
            favorites = favorites + FavoriteRoute(key, config, addedManually = false)
        }
        save(current.copy(entries = entries, favorites = favorites))
    }

    // Tandai status akhir perjalanan terakhir
    fun markLastTrip(status: String, onlyIfRunning: Boolean = false) {
        val current = _store.value
        val last = current.entries.firstOrNull() ?: return
        if (onlyIfRunning && last.endStatus != TripEndStatus.RUNNING) return
        if (last.endStatus == status) return
        val updated = listOf(last.copy(endStatus = status)) + current.entries.drop(1)
        save(current.copy(entries = updated))
    }

    // Bintang: tambah/hapus favorit secara manual
    fun toggleFavorite(config: ActiveTripConfig) {
        val current = _store.value
        val key = keyOf(config)
        val updated = if (current.favorites.any { it.routeKey == key }) {
            current.copy(
                favorites = current.favorites.filterNot { it.routeKey == key },
                blockedAutoFavorite = current.blockedAutoFavorite + key
            )
        } else {
            current.copy(
                favorites = current.favorites + FavoriteRoute(key, config, addedManually = true),
                blockedAutoFavorite = current.blockedAutoFavorite - key
            )
        }
        save(updated)
    }

    private fun save(store: TripHistoryStore) {
        _store.value = store
        try {
            prefs?.edit()?.putString(KEY_STORE, json.encodeToString(TripHistoryStore.serializer(), store))?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
