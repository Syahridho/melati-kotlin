package com.example.melati

import android.app.Application
import com.example.melati.di.AppContainer

class MelatiApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}
