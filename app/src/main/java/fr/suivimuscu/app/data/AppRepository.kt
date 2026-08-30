package fr.suivimuscu.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AppRepository(private val dao: AppStateDao) {
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val state: Flow<AppState?> = dao.observe().map { value ->
        value?.let {
            runCatching { StateMigrations.toLatest(json.decodeFromString<AppState>(it)) }.getOrNull()
        }
    }

    suspend fun initialize(): AppState {
        val current = dao.get()?.let {
            runCatching { json.decodeFromString<AppState>(it) }.getOrNull()
        }
        if (current != null) {
            val migrated = StateMigrations.toLatest(current)
            if (migrated != current) save(migrated)
            return migrated
        }
        val seeded = SeedData.create()
        save(seeded)
        return seeded
    }

    suspend fun save(state: AppState) {
        dao.save(AppStateEntity(json = json.encodeToString(state)))
    }

    fun encode(state: AppState): String = json.encodeToString(state)

    fun decode(value: String): AppState {
        val decoded = json.decodeFromString<AppState>(value)
        return StateMigrations.toLatest(decoded)
    }
}
