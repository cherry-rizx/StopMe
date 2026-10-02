package com.hanyz.stopme.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.hanyz.stopme.data.TripRepository
import com.hanyz.stopme.model.ActiveTripConfig
import com.hanyz.stopme.model.AlarmMode
import com.hanyz.stopme.ui.alarm.AlarmActivity
import com.hanyz.stopme.util.LocationUtils
import java.util.LinkedList

class TrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var isTracking = false

    private val lastFiveSpeeds = LinkedList<Float>()
    private var lastValidGpsTimestamp: Long = 0L
    private var lastPassedStopTimestamp: Long = 0L

    private var currentPassedSeq: Int = 0
    private var isAlarmTriggered: Boolean = false

    // Status alarm per perjalanan
    private var distanceFired = false
    private var timeFired = false
    private var finalFired = false
    private var nextStopReminderSent = false  // pengingat "halte berikutnya tujuanmu" sudah dikirim
    private var movementStarted = false      // kendaraan sudah benar-benar bergerak
    private var skipVotes = 0                // konfirmasi sebelum menganggap halte terlewati tanpa masuk radius
    private var savedAlarmVolume: Int? = null

    // Riwayat sisa jarak untuk menghitung kecepatan rata-rata efektif (termasuk berhenti & macet)
    private val progressHistory = java.util.ArrayDeque<Pair<Long, Double>>() // volume asli, dikembalikan setelah alarm kuat
    private var headsUpEscalationRunnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    // Handler offline countdown timer (cek setiap detik)
    private val offlineCheckRunnable = object : Runnable {
        override fun run() {
            checkOfflineCountdown()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopTrackingAndSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_DISMISS_ALARM) {
            dismissCurrentAlarm()
            return START_NOT_STICKY
        }

        val config = TripRepository.tripState.value.tripConfig
        if (config == null) {
            // Tidak ada perjalanan aktif (mis. proses dimulai ulang sistem).
            // Tetap penuhi syarat startForeground agar tidak crash, lalu berhenti.
            try {
                startForeground(
                    NotificationHelper.TRACKING_NOTIF_ID,
                    NotificationHelper.buildTrackingNotification(this, "-", "-", "-")
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            stopTrackingAndSelf()
            return START_NOT_STICKY
        }

        if (!isTracking) {
            startTracking(config)
        }

        return START_NOT_STICKY
    }

    private fun startTracking(config: ActiveTripConfig) {
        isTracking = true
        currentPassedSeq = config.departureSeq
        isAlarmTriggered = false
        distanceFired = false
        timeFired = false
        finalFired = false
        nextStopReminderSent = false
        movementStarted = false
        skipVotes = 0
        lastFiveSpeeds.clear()
        progressHistory.clear()
        lastValidGpsTimestamp = System.currentTimeMillis()
        lastPassedStopTimestamp = System.currentTimeMillis()

        val initialNotif = NotificationHelper.buildTrackingNotification(
            this,
            config.destinationStopName,
            "Memulai...",
            "Menghitung..."
        )
        try {
            startForeground(NotificationHelper.TRACKING_NOTIF_ID, initialNotif)
        } catch (e: Exception) {
            // Biasanya karena izin lokasi dicabut: hentikan dengan aman
            e.printStackTrace()
            stopTrackingAndSelf()
            return
        }

        startLocationUpdates()
        handler.post(offlineCheckRunnable)
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(3000)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                // Filter akurasi GPS: abaikan jika > 100m
                if (location.hasAccuracy() && location.accuracy > 100f) {
                    return
                }

                lastValidGpsTimestamp = System.currentTimeMillis()
                processLocationUpdate(location)
            }
        }
    }

    private fun processLocationUpdate(location: Location) {
        val config = TripRepository.tripState.value.tripConfig ?: return

        // Kecepatan: abaikan fix kurang akurat dan batasi lonjakan tidak wajar, lalu pakai median
        val maxSpeed = if (config.isTrain) MAX_TRAIN_SPEED else MAX_BUS_SPEED
        if (location.hasSpeed() && (!location.hasAccuracy() || location.accuracy <= 50f)) {
            if (lastFiveSpeeds.size >= 5) lastFiveSpeeds.poll()
            lastFiveSpeeds.offer(location.speed.coerceIn(0f, maxSpeed))
        }
        val speed = medianSpeed()

        // Hitung mundur offline baru boleh berjalan setelah kendaraan benar-benar bergerak
        if (!movementStarted && speed > MOVING_SPEED) {
            movementStarted = true
            lastPassedStopTimestamp = System.currentTimeMillis()
        }

        val remainingStops = config.stopsList.filter { it.seq > currentPassedSeq }
        if (remainingStops.isEmpty()) {
            // Sudah sampai tujuan: pastikan alarm terakhir tetap berbunyi
            if (!finalFired && !TripRepository.isAlarmRinging.value) {
                fireAlarm(config, "Sudah tiba di tujuan", isFinal = true, strong = true)
            }
            return
        }

        val nextStopItem = remainingStops.first()
        val passThreshold = if (config.isTrain) 300.0 else 150.0
        val distToNext = LocationUtils.haversineDistanceMeters(
            location.latitude, location.longitude,
            nextStopItem.stop.lat, nextStopItem.stop.lng
        )

        // Halte terlewati: masuk radius halte, atau halte sesudahnya lebih dekat 2 kali berturut-turut.
        // Maksimal satu halte per pembaruan agar guncangan GPS tidak melompati banyak halte.
        if (distToNext <= passThreshold) {
            markStopPassed(nextStopItem.seq)
        } else if (remainingStops.size > 1) {
            val afterNext = remainingStops[1]
            val distToAfterNext = LocationUtils.haversineDistanceMeters(
                location.latitude, location.longitude,
                afterNext.stop.lat, afterNext.stop.lng
            )
            if (distToAfterNext < distToNext) {
                skipVotes++
                if (skipVotes >= 2) markStopPassed(nextStopItem.seq)
            } else {
                skipVotes = 0
            }
        }

        // Sisa jarak dan ETA
        val activeRemainingStops = config.stopsList.filter { it.seq > currentPassedSeq }
        val targetNextStop = activeRemainingStops.firstOrNull()?.stop ?: config.stopsList.last().stop
        val targetNextSeq = activeRemainingStops.firstOrNull()?.seq ?: config.destinationSeq

        var totalRemainingDist = 0.0
        if (activeRemainingStops.isNotEmpty()) {
            val first = activeRemainingStops.first()
            totalRemainingDist += LocationUtils.haversineDistanceMeters(
                location.latitude, location.longitude, first.stop.lat, first.stop.lng
            )
            for (k in 0 until (activeRemainingStops.size - 1)) {
                val s1 = activeRemainingStops[k].stop
                val s2 = activeRemainingStops[k + 1].stop
                totalRemainingDist += LocationUtils.haversineDistanceMeters(s1.lat, s1.lng, s2.lat, s2.lng)
            }
        }

        val sumTravelSec = activeRemainingStops.sumOf { it.travelSecFromPrevious }

        // Perkiraan tiba:
        // 1) Utama: kecepatan rata-rata efektif ~3 menit terakhir, sudah termasuk berhenti di halte dan macet.
        // 2) Awal perjalanan (data < 90 detik): kecepatan saat ini + 30 detik per halte yang masih akan disinggahi.
        // 3) Diam / kecepatan tidak diketahui: jadwal antar halte.
        val effectiveSpeed = updateEffectiveSpeed(totalRemainingDist)
        val stopsAhead = (activeRemainingStops.size - 1).coerceAtLeast(0)
        val etaSec = when {
            effectiveSpeed != null && totalRemainingDist > 0 ->
                (totalRemainingDist / effectiveSpeed).toInt()
            speed > MOVING_SPEED && totalRemainingDist > 0 ->
                (totalRemainingDist / speed).toInt() + STOP_DWELL_ESTIMATE_SEC * stopsAhead
            else -> sumTravelSec
        }

        TripRepository.updateRealtimeState { current ->
            current.copy(
                userLat = location.latitude,
                userLng = location.longitude,
                userSpeedMps = speed,
                currentPassedSeq = currentPassedSeq,
                nextStop = targetNextStop,
                nextStopSeq = targetNextSeq,
                remainingDistanceMeters = totalRemainingDist,
                etaSeconds = etaSec,
                isOffline = false
            )
        }

        val notif = NotificationHelper.buildTrackingNotification(
            this,
            config.destinationStopName,
            LocationUtils.formatDistance(totalRemainingDist),
            LocationUtils.formatEta(etaSec)
        )
        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifManager.notify(NotificationHelper.TRACKING_NOTIF_ID, notif)

        evaluateAlarms(config, totalRemainingDist, etaSec, speed, targetNextSeq, offline = false)
    }

    // Simpan sisa jarak per waktu, lalu hitung kemajuan rata-rata dalam jendela 3 menit terakhir.
    // Mengembalikan null bila data belum cukup (< 90 detik) atau kendaraan praktis tidak maju.
    private fun updateEffectiveSpeed(remainingMeters: Double): Double? {
        if (!movementStarted) return null
        val now = System.currentTimeMillis()
        progressHistory.addLast(now to remainingMeters)
        while (progressHistory.isNotEmpty() && now - progressHistory.first().first > EFFECTIVE_WINDOW_MS) {
            progressHistory.removeFirst()
        }
        val oldest = progressHistory.firstOrNull() ?: return null
        val elapsedSec = (now - oldest.first) / 1000.0
        if (elapsedSec < MIN_EFFECTIVE_WINDOW_SEC) return null
        val progressed = oldest.second - remainingMeters
        val eff = progressed / elapsedSec
        return if (eff > MIN_EFFECTIVE_SPEED) eff else null
    }

    private fun markStopPassed(seq: Int) {
        currentPassedSeq = seq
        lastPassedStopTimestamp = System.currentTimeMillis()
        skipVotes = 0
        // Melewati halte juga bukti kendaraan sudah bergerak
        movementStarted = true
    }

    private fun medianSpeed(): Double {
        if (lastFiveSpeeds.isEmpty()) return 0.0
        val sorted = lastFiveSpeeds.sorted()
        return sorted[sorted.size / 2].toDouble()
    }

    // Mode offline (>30 detik tanpa GPS valid): perkiraan waktu dari jadwal antar halte
    private fun checkOfflineCountdown() {
        val config = TripRepository.tripState.value.tripConfig ?: return
        val now = System.currentTimeMillis()
        val isOffline = (now - lastValidGpsTimestamp) > 30000
        if (!isOffline) return

        val activeRemainingStops = config.stopsList.filter { it.seq > currentPassedSeq }
        val sumTravelSec = activeRemainingStops.sumOf { it.travelSecFromPrevious }
        // Belum bergerak (mis. masih menunggu di halte): hitung mundur belum dimulai
        val elapsed = if (movementStarted) ((now - lastPassedStopTimestamp) / 1000).toInt() else 0
        val countdownSec = (sumTravelSec - elapsed).coerceAtLeast(0)

        TripRepository.updateRealtimeState { current ->
            current.copy(isOffline = true, etaSeconds = countdownSec)
        }

        if (movementStarted) {
            val nextSeq = activeRemainingStops.firstOrNull()?.seq ?: config.destinationSeq
            evaluateAlarms(config, null, countdownSec, 0.0, nextSeq, offline = true)
        }
    }

    // Pemicu alarm sesuai mode yang dipilih user
    private fun evaluateAlarms(
        config: ActiveTripConfig,
        remainingMeters: Double?,
        etaSec: Int,
        speed: Double,
        nextSeq: Int,
        offline: Boolean
    ) {
        if (finalFired || TripRepository.isAlarmRinging.value) return

        val radius = config.alarmRadiusMeters.toDouble()
        val timeLimitSec = config.alarmMinutesThreshold * 60
        val distReached = !offline && remainingMeters != null && remainingMeters <= radius
        val timeReached = etaSec <= timeLimitSec

        when (config.alarmMode) {
            AlarmMode.DISTANCE -> {
                // Saat GPS hilang, jarak tidak bisa diukur: pakai perkiraan waktu sebagai cadangan
                val reached = if (offline) timeReached else distReached
                if (reached) {
                    val title = if (offline) "Alarm Jarak (perkiraan waktu)" else "Alarm Jarak"
                    distanceFired = true
                    fireAlarm(config, title, isFinal = true, strong = false)
                    return
                }
            }

            AlarmMode.TIME -> {
                if (timeReached) {
                    timeFired = true
                    fireAlarm(config, "Alarm Waktu", isFinal = true, strong = false)
                    return
                }
            }

            AlarmMode.BOTH -> {
                if (!distanceFired && !timeFired) {
                    when {
                        distReached && timeReached -> {
                            fireCombined(config); return
                        }
                        distReached -> {
                            // Perkirakan kapan alarm waktu akan tercapai
                            val secsToTime = if (speed > MOVING_SPEED) etaSec - timeLimitSec else Int.MAX_VALUE
                            if (secsToTime <= MERGE_WINDOW_SEC) {
                                fireCombined(config)
                            } else {
                                distanceFired = true
                                fireAlarm(config, "Alarm 1 dari 2 · Jarak", isFinal = false, strong = false)
                            }
                            return
                        }
                        timeReached -> {
                            // Perkirakan kapan alarm jarak akan tercapai
                            val secsToDist = if (!offline && remainingMeters != null && speed > MOVING_SPEED) {
                                ((remainingMeters - radius) / speed).toInt()
                            } else Int.MAX_VALUE
                            if (secsToDist <= MERGE_WINDOW_SEC) {
                                fireCombined(config)
                            } else {
                                timeFired = true
                                fireAlarm(config, "Alarm 1 dari 2 · Waktu", isFinal = false, strong = false)
                            }
                            return
                        }
                    }
                } else if (distanceFired && !timeFired && timeReached) {
                    timeFired = true
                    fireAlarm(config, "Alarm 2 dari 2 · Waktu", isFinal = true, strong = false)
                    return
                } else if (timeFired && !distanceFired && distReached) {
                    distanceFired = true
                    fireAlarm(config, "Alarm 2 dari 2 · Jarak", isFinal = true, strong = false)
                    return
                }
            }
        }

        // Pengaman: sudah melewati halte tepat sebelum tujuan tetapi belum ada alarm sama sekali.
        // Hanya notifikasi + getar singkat; perjalanan tetap lanjut sampai alarm pilihan user bunyi.
        val passedAtLeastOne = currentPassedSeq > config.departureSeq
        val noAlarmYet = !distanceFired && !timeFired
        if (!nextStopReminderSent && noAlarmYet && passedAtLeastOne && nextSeq >= config.destinationSeq) {
            nextStopReminderSent = true
            sendNextStopReminder(config)
        }
    }

    private fun sendNextStopReminder(config: ActiveTripConfig) {
        try {
            val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notifManager.notify(
                NotificationHelper.REMINDER_NOTIF_ID,
                NotificationHelper.buildNextStopReminder(this, config.destinationStopName)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        vibrateShort()
    }

    private fun fireCombined(config: ActiveTripConfig) {
        distanceFired = true
        timeFired = true
        fireAlarm(config, "Alarm Jarak & Waktu", isFinal = true, strong = true)
    }

    private fun fireAlarm(config: ActiveTripConfig, title: String, isFinal: Boolean, strong: Boolean) {
        if (isFinal) finalFired = true
        TripRepository.updateRealtimeState { current ->
            current.copy(
                alarmTitle = title,
                isFinalAlarm = isFinal,
                distanceAlarmFired = distanceFired,
                timeAlarmFired = timeFired
            )
        }
        triggerAlarmAlert(config, title, strong)
    }

    // Peringatan: layar nyala -> heads-up dulu (eskalasi 20 detik), layar mati -> langsung alarm penuh
    private fun triggerAlarmAlert(config: ActiveTripConfig, title: String, strong: Boolean) {
        isAlarmTriggered = true
        TripRepository.setAlarmRinging(true)
        com.hanyz.stopme.data.TripHistoryRepository.markLastTrip(com.hanyz.stopme.data.TripEndStatus.ALARM)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isInteractive) {
            val distStr = LocationUtils.formatDistance(TripRepository.tripState.value.remainingDistanceMeters)
            val headsUpNotif = NotificationHelper.buildHeadsUpAlarmNotification(
                this, config.destinationStopName, distStr, title
            )
            val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notifManager.notify(NotificationHelper.ALARM_NOTIF_ID, headsUpNotif)

            if (strong) startLoopingVibration(strong = true) else vibrateShort()

            headsUpEscalationRunnable = Runnable {
                if (TripRepository.isAlarmRinging.value) startFullAlarm(config, title, strong)
            }
            handler.postDelayed(headsUpEscalationRunnable!!, 20000)
        } else {
            startFullAlarm(config, title, strong)
        }
    }

    private fun startFullAlarm(config: ActiveTripConfig, title: String, strong: Boolean) {
        headsUpEscalationRunnable?.let { handler.removeCallbacks(it) }

        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifManager.cancel(NotificationHelper.ALARM_NOTIF_ID)

        // Full-screen intent: cara resmi membuka layar alarm saat HP terkunci
        val distStr = LocationUtils.formatDistance(TripRepository.tripState.value.remainingDistanceMeters)
        val fullAlarmNotif = NotificationHelper.buildHeadsUpAlarmNotification(
            this, config.destinationStopName, distStr, title
        )
        notifManager.notify(NotificationHelper.FULL_ALARM_NOTIF_ID, fullAlarmNotif)

        try {
            val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(alarmIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (strong) raiseAlarmVolume()
        startLoopingAlarmSound()
        startLoopingVibration(strong)
    }

    // Matikan alarm yang sedang bunyi; perjalanan berlanjut jika masih ada alarm berikutnya
    private fun dismissCurrentAlarm() {
        headsUpEscalationRunnable?.let { handler.removeCallbacks(it) }
        stopSoundAndVibration()
        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifManager.cancel(NotificationHelper.ALARM_NOTIF_ID)
        notifManager.cancel(NotificationHelper.FULL_ALARM_NOTIF_ID)
        TripRepository.setAlarmRinging(false)

        if (finalFired || !isTracking) {
            stopTrackingAndSelf()
        }
    }

    // Alarm gabungan: naikkan volume alarm ke maksimum, dikembalikan saat alarm berhenti
    private fun raiseAlarmVolume() {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (savedAlarmVolume == null) savedAlarmVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun restoreAlarmVolume() {
        val original = savedAlarmVolume ?: return
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.setStreamVolume(AudioManager.STREAM_ALARM, original, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        savedAlarmVolume = null
    }

    private fun startLoopingAlarmSound() {
        try {
            if (mediaPlayer == null) {
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(this@TrackingService, alarmUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startLoopingVibration(strong: Boolean = false) {
        // Alarm gabungan: getar lebih panjang dengan kekuatan penuh
        val timings = if (strong) longArrayOf(0, 1500, 300, 1500, 300) else longArrayOf(0, 1000, 500, 1000, 500)
        val amplitudes = if (strong) intArrayOf(0, 255, 0, 255, 0) else intArrayOf(0, 180, 0, 180, 0)
        try {
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, 1))
        } catch (e: Exception) {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(timings, 1)
        }
    }

    private fun vibrateShort() {
        vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun stopSoundAndVibration() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        vibrator?.cancel()
        restoreAlarmVolume()
    }

    private fun stopTrackingAndSelf() {
        isTracking = false
        headsUpEscalationRunnable?.let { handler.removeCallbacks(it) }
        handler.removeCallbacks(offlineCheckRunnable)
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopSoundAndVibration()

        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifManager.cancel(NotificationHelper.ALARM_NOTIF_ID)
        notifManager.cancel(NotificationHelper.FULL_ALARM_NOTIF_ID)
        notifManager.cancel(NotificationHelper.REMINDER_NOTIF_ID)

        // Jika berhenti sebelum alarm berbunyi, catat sebagai dihentikan manual
        com.hanyz.stopme.data.TripHistoryRepository.markLastTrip(
            com.hanyz.stopme.data.TripEndStatus.STOPPED,
            onlyIfRunning = true
        )

        TripRepository.stopTrip()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopTrackingAndSelf()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_SERVICE = "com.hanyz.stopme.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.hanyz.stopme.STOP_SERVICE"
        const val ACTION_DISMISS_ALARM = "com.hanyz.stopme.DISMISS_ALARM"

        private const val MOVING_SPEED = 2.0            // m/s, di bawah ini dianggap diam
        private const val MAX_BUS_SPEED = 25f            // m/s (~90 km/jam), batas lonjakan GPS
        private const val MAX_TRAIN_SPEED = 33f          // m/s (~120 km/jam)
        private const val MERGE_WINDOW_SEC = 30          // dua alarm berdekatan digabung
        private const val EFFECTIVE_WINDOW_MS = 180_000L  // jendela kecepatan efektif: 3 menit
        private const val MIN_EFFECTIVE_WINDOW_SEC = 90.0 // minimal data sebelum kecepatan efektif dipakai
        private const val MIN_EFFECTIVE_SPEED = 0.5       // m/s, di bawah ini dianggap tidak maju
        private const val STOP_DWELL_ESTIMATE_SEC = 30    // perkiraan lama berhenti per halte

        // Dipanggil dari layar alarm saat slide-to-dismiss
        fun dismissAlarm(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_DISMISS_ALARM
            }
            context.startService(intent)
        }

        fun start(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TrackingService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
