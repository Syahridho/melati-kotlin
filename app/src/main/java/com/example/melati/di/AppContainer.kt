package com.example.melati.di

import android.content.Context
import com.example.melati.data.local.TicketHistoryStore
import com.example.melati.data.remote.MelatiApi
import com.example.melati.data.remote.NetworkModule
import com.example.melati.data.repository.TicketRepositoryImpl
import com.example.melati.domain.repository.TicketRepository

/**
 * Injeksi manual (PRD §8: tanpa framework DI). Dibuat lazily agar startup ringan.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    private val api: MelatiApi by lazy { NetworkModule.createApi() }

    private val historyStore: TicketHistoryStore by lazy { TicketHistoryStore(appContext) }

    val ticketRepository: TicketRepository by lazy {
        TicketRepositoryImpl(api, historyStore)
    }
}
