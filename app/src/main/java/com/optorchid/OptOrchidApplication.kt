package com.optorchid

import android.app.Application
import com.optorchid.core.di.AppContainer

class OptOrchidApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
