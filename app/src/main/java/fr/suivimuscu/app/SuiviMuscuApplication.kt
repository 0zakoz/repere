package fr.suivimuscu.app

import android.app.Application
import fr.suivimuscu.app.data.AppDatabase
import fr.suivimuscu.app.data.AppRepository
import fr.suivimuscu.app.data.AppearanceRepository

class SuiviMuscuApplication : Application() {
    val database by lazy { AppDatabase.create(this) }
    val repository by lazy { AppRepository(database.appStateDao()) }
    val appearanceRepository by lazy { AppearanceRepository(this) }
}
