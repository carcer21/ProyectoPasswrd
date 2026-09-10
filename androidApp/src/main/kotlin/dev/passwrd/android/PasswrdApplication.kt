package dev.passwrd.android

import android.app.Application
import dev.passwrd.android.di.AppContainer

class PasswrdApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
