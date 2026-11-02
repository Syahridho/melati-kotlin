package com.example.melati.ui.screens.riwayat

import com.example.melati.domain.model.TicketSummary

data class RiwayatUiState(
    val items: List<TicketSummary> = emptyList(),
    val isLoading: Boolean = true,
    val pendingDeleteNumber: String? = null,
    val showClearAllConfirm: Boolean = false
)

sealed interface RiwayatEvent {
    data class Open(val number: String) : RiwayatEvent
    data class RequestDelete(val number: String) : RiwayatEvent
    data object ConfirmDelete : RiwayatEvent
    data object DismissDelete : RiwayatEvent
    data object RequestClearAll : RiwayatEvent
    data object ConfirmClearAll : RiwayatEvent
    data object DismissClearAll : RiwayatEvent
}
