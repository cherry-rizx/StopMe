package com.hanyz.stopme

import android.app.Application
import com.google.firebase.FirebaseApp
import com.hanyz.stopme.data.TransitRepository
import com.hanyz.stopme.service.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import java.io.File

class StopMeApplication : Application() {

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // Inisialisasi Firebase aman
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Konfigurasi osmdroid: user agent wajib, cache tile di folder cache app
        Configuration.getInstance().apply {
            userAgentValue = BuildConfig.APPLICATION_ID
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
        }

        // Muat riwayat & favorit rute dari HP
        com.hanyz.stopme.data.TripHistoryRepository.init(this)

        // Buat saluran notifikasi
        NotificationHelper.createNotificationChannels(this)

        // Parse transit data secara asinkron di background thread saat aplikasi dibuka
        applicationScope.launch {
            TransitRepository.loadTransitData(this@StopMeApplication)
        }
    }
}
