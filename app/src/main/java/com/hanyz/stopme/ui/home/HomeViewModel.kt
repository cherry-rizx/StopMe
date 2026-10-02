package com.hanyz.stopme.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanyz.stopme.data.AuthRepository
import com.hanyz.stopme.data.TransitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserInfo()
        observeTransitData()
    }

    private fun loadUserInfo() {
        val user = AuthRepository.currentUser
        _uiState.update { it.copy(userName = user?.name ?: "") }
    }

    private fun observeTransitData() {
        viewModelScope.launch {
            TransitRepository.isLoaded.collect { loaded ->
                _uiState.update { it.copy(isTransitDataLoaded = loaded) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filtered = if (query.isBlank()) {
                current.services
            } else {
                current.services.filter {
                    it.title.lowercase().contains(query.trim().lowercase())
                }
            }
            current.copy(searchQuery = query, filteredServices = filtered)
        }
    }
}
