package com.example.airvibe.feature.radar.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.airvibe.feature.radar.data.local.dao.RadarDao
import com.example.airvibe.feature.radar.data.local.entity.NodeEntity

/**
 * Base de datos principal de AirVibe. Hoy contiene una sola tabla
 * (`radar_nodes`) que persiste los perfiles descubiertos por el radar
 * y se replica en segundo plano a Supabase desde un Worker (paso 4).
 *
 * El versionado es manual: cualquier cambio en el esquema exigirá
 * una migración o un bump de [version] con `fallbackToDestructiveMigration`
 * desactivado en producción.
 */
@Database(
    entities = [NodeEntity::class],
    version = AirVibeDatabase.SCHEMA_VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AirVibeDatabase : RoomDatabase() {

    abstract fun radarDao(): RadarDao

    companion object {
        const val DATABASE_NAME = "airvibe.db"
        const val SCHEMA_VERSION = 1

        @Volatile
        private var instance: AirVibeDatabase? = null

        fun getInstance(context: Context): AirVibeDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(context: Context): AirVibeDatabase {
            return Room.databaseBuilder(
                context = context,
                klass = AirVibeDatabase::class.java,
                name = DATABASE_NAME,
            )
                .fallbackToDestructiveMigration(dropAllTables = false)
                .build()
        }
    }
}
