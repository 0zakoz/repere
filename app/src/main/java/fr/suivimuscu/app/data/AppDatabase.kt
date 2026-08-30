package fr.suivimuscu.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val id: Int = 1,
    val json: String,
)

@Dao
interface AppStateDao {
    @Query("SELECT json FROM app_state WHERE id = 1")
    fun observe(): Flow<String?>

    @Query("SELECT json FROM app_state WHERE id = 1")
    suspend fun get(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: AppStateEntity)
}

@Database(entities = [AppStateEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appStateDao(): AppStateDao

    companion object {
        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "suivi-muscu.db",
        ).build()
    }
}
