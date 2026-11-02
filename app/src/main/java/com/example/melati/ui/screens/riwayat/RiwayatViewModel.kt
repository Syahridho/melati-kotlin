package com.example.melati.ui.screens.riwayat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melati.domain.repository.TicketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RiwayatViewModel(
    private val repository: TicketRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RiwayatUiState())
    val state: StateFlow<RiwayatUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.history.collect { items ->
                _state.update {
                    it.copy(
                        items = items,
                        isLoading = false,
                        // Item yang sedang dikonfirmasi bisa saja sudah terhapus.
                        pendingDeleteNumber = it.pendingDeleteNumber
                            ?.takeIf { number -> items.any { item -> item.number == number } }
                    )
                }
            }
        }
    }

    fun onEvent(event: RiwayatEvent) {
        when (event) {
            is RiwayatEvent.Open -> Unit // ditangani di lapisan navigasi
            is RiwayatEvent.RequestDelete -> _state.update { it.copy(pendingDeleteNumber = event.number) }
            RiwayatEvent.DismissDelete -> _state.update { it.copy(pendingDeleteNumber = null) }
            RiwayatEvent.ConfirmDelete -> confirmDelete()
            RiwayatEvent.RequestClearAll -> _state.update { it.copy(showClearAllConfirm = true) }
            RiwayatEvent.DismissClearAll -> _state.update { it.copy(showClearAllConfirm = false) }
            RiwayatEvent.ConfirmClearAll -> clearAll()
        }
    }

    private fun confirmDelete() {
        val number = _state.value.pendingDeleteNumber ?: return
        _state.update { it.copy(pendingDeleteNumber = null) }
        viewModelScope.launch { repository.removeFromHistory(number) }
    }

    private fun clearAll() {
        _state.update { it.copy(showClearAllConfirm = false) }
        viewModelScope.launch { repository.clearHistory() }
    }
}
