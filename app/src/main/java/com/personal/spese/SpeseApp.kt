package com.personal.spese

import android.app.Application
import com.personal.spese.di.AppContainer

class SpeseApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
