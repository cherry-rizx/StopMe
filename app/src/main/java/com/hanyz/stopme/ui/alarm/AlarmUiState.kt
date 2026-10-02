package com.hanyz.stopme.ui.alarm

data class AlarmUiState(
    val destinationName: String = "",
    val arrivalInfoStr: String = "",
    val isOffline: Boolean = false,
    val alarmTitle: String = "",        // mis. "Alarm 1 dari 2 · Jarak"
    val isFinalAlarm: Boolean = true,   // false = perjalanan lanjut setelah dimatikan
    val isDismissed: Boolean = false
)
