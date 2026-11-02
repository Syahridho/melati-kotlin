package com.example.melati.ui.screens.cek

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melati.R
import com.example.melati.data.remote.ApiException
import com.example.melati.domain.model.Ticket
import com.example.melati.domain.repository.TicketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CekViewModel(
    private val repository: TicketRepository,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(CekUiState())
    val state: StateFlow<CekUiState> = _state.asStateFlow()

    private var lastLoadedNumber: String? = null

    /** Dipanggil dari navigasi saat pengguna membuka tiket dari Riwayat. */
    fun loadNumber(number: String) {
        val clean = number.trim()
        if (clean.isEmpty() || clean == lastLoadedNumber) return
        _state.update { it.copy(query = clean) }
        check(clean)
    }

    fun onEvent(event: CekEvent) {
        when (event) {
            is CekEvent.QueryChanged -> _state.update {
                it.copy(query = event.value, isQueryError = false)
            }

            CekEvent.Check -> check()
            CekEvent.Retry -> check()
            CekEvent.Refresh -> refresh()

            is CekEvent.ReplyChanged -> _state.update { it.copy(replyText = event.value) }
            CekEvent.SendReply -> sendReply()

            CekEvent.RequestComplete -> _state.update { it.copy(showCompleteConfirm = true) }
            CekEvent.DismissCompleteConfirm -> _state.update { it.copy(showCompleteConfirm = false) }
            CekEvent.ConfirmComplete -> {
                _state.update { it.copy(showCompleteConfirm = false) }
                complete()
            }

            CekEvent.MessageShown -> _state.update { it.copy(message = null) }
        }
    }

    private fun check(number: String? = null) {
        val query = (number ?: _state.value.query).trim()
        if (query.isEmpty()) {
            _state.update { it.copy(isQueryError = true) }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(phase = CekPhase.Loading, isQueryError = false, message = null)
            }
            repository.checkTicket(query).fold(
                onSuccess = { ticket ->
                    lastLoadedNumber = ticket.number
                    _state.update {
                        it.copy(query = ticket.number, phase = CekPhase.Loaded(ticket))
                    }
                },
                onFailure = { throwable -> handleCheckFailure(query, throwable) }
            )
        }
    }

    private fun refresh() {
        val ticket = _state.value.ticket ?: return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            repository.checkTicket(ticket.number).fold(
                onSuccess = { updated ->
                    _state.update { it.copy(phase = CekPhase.Loaded(updated), isRefreshing = false) }
                },
                onFailure = {
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            message = context.getString(R.string.refresh_gagal)
                        )
                    }
                }
            )
        }
    }

    private fun sendReply() {
        val current = _state.value
        val ticket = current.ticket ?: return
        val text = current.replyText.trim()
        if (text.isEmpty() || ticket.status.isFinal || current.isSendingReply) return

        viewModelScope.launch {
            _state.update { it.copy(isSendingReply = true) }
            repository.replyToTicket(ticket.number, text, emptyList()).fold(
                onSuccess = {
                    _state.update {
                        it.copy(isSendingReply = false, replyText = "", message = context.getString(R.string.reply_sent))
                    }
                    reloadSilently(ticket.number)
                },
                onFailure = { throwable ->
                    _state.update {
                        it.copy(
                            isSendingReply = false,
                            message = messageFor(throwable, R.string.reply_gagal)
                        )
                    }
                }
            )
        }
    }

    private fun complete() {
        val ticket = _state.value.ticket ?: return
        if (_state.value.isCompleting) return

        viewModelScope.launch {
            _state.update { it.copy(isCompleting = true) }
            repository.completeTicket(ticket.number).fold(
                onSuccess = {
                    _state.update {
                        it.copy(isCompleting = false, message = context.getString(R.string.ticket_completed))
                    }
                    reloadSilently(ticket.number)
                },
                onFailure = { throwable ->
                    _state.update {
                        it.copy(
                            isCompleting = false,
                            message = messageFor(throwable, R.string.complete_gagal)
                        )
                    }
                }
            )
        }
    }

    private suspend fun reloadSilently(number: String) {
        repository.checkTicket(number).onSuccess { updated ->
            lastLoadedNumber = updated.number
            _state.update { it.copy(phase = CekPhase.Loaded(updated)) }
        }
    }

    private fun handleCheckFailure(query: String, throwable: Throwable) {
        val api = throwable as? ApiException
        val phase = when {
            api?.isNotFound == true -> CekPhase.NotFound(query)
            else -> CekPhase.Failed(messageFor(throwable, R.string.error_generic))
        }
        _state.update { it.copy(phase = phase) }
    }

    private fun messageFor(throwable: Throwable, fallback: Int): String {
        val api = throwable as? ApiException ?: return context.getString(fallback)
        val serverMessage = api.serverMessage
        return when {
            api.isConnectivityIssue -> context.getString(R.string.error_network)
            api.isUnauthorized -> context.getString(R.string.error_unauthorized)
            !serverMessage.isNullOrBlank() -> serverMessage
            else -> context.getString(fallback)
        }
    }
}
