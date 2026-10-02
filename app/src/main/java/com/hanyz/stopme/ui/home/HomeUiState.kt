package com.hanyz.stopme.ui.home

import com.hanyz.stopme.model.TransportServiceType

// Layanan yang sementara disembunyikan dari Home.
// Data rute dan kode LRT tetap ada; hapus LRT dari daftar ini untuk menampilkannya lagi.
val HIDDEN_SERVICES = setOf(
    TransportServiceType.LRT
)

val VISIBLE_SERVICES = TransportServiceType.entries.filter { it !in HIDDEN_SERVICES }

data class HomeUiState(
    val searchQuery: String = "",
    val services: List<TransportServiceType> = VISIBLE_SERVICES,
    val filteredServices: List<TransportServiceType> = VISIBLE_SERVICES,
    val isTransitDataLoaded: Boolean = false,
    val userName: String = ""
)
