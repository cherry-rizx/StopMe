package com.hanyz.stopme.ui.permission

data class PermissionUiState(
    val isLocationGranted: Boolean = false,
    val isNotificationGranted: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    val canProceed: Boolean = false
)
