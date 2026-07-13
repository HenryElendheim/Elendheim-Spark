package com.elendheim.spark

import android.app.Application
import com.elendheim.spark.data.SparkDatabase
import com.elendheim.spark.data.SparkRepository
import com.elendheim.spark.settings.SettingsRepository

/**
 * The Application. It builds the one shared "container" of long-lived objects
 * (database, repository, settings) that the whole app pulls from. This is a
 * plain, dependency-free way to wire things up without a DI framework -> easy to
 * read, easy to follow.
 */
class SparkApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Holds the app-wide singletons. Created once, in [SparkApp]. */
class AppContainer(app: Application) {
    private val database = SparkDatabase.get(app)
    val repository = SparkRepository(database.dao())
    val settings = SettingsRepository(app)
}
