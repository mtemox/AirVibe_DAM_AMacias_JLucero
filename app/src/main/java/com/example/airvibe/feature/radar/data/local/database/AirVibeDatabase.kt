package com.example.airvibe.feature.radar.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.airvibe.feature.chat.data.local.dao.ChatDao
import com.example.airvibe.feature.chat.data.local.entity.ChatMessageEntity
import com.example.airvibe.feature.radar.data.local.dao.RadarDao
import com.example.airvibe.feature.radar.data.local.entity.NodeEntity

/**
 * Base de datos principal de AirVibe.
 *
 * Paso 5: añadimos la tabla `chat_messages` para soportar el
 * chat offline + broadcast. La migración es aditiva (no rompe
 * los datos del paso 2) y se mantiene el `fallbackToDestructive`
 * como red de seguridad para dev.
 */
@Database(
    entities = [
        NodeEntity::class,
        ChatMessageEntity::class,
    ],
    version = AirVibeDatabase.SCHEMA_VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AirVibeDatabase : RoomDatabase() {

    abstract fun radarDao(): RadarDao
    abstract fun chatDao(): ChatDao

    companion object {
        const val DATABASE_NAME = "airvibe.db"
        const val SCHEMA_VERSION = 2

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
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration(dropAllTables = false)
                .build()
        }

        /**
         * Migración aditiva: crea la tabla `chat_messages` con
         * los índices necesarios. Es seguro de aplicar sobre
         * una base v1 existente porque no toca ninguna tabla
         * previa.
         */
        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS chat_messages (
                        id TEXT NOT NULL PRIMARY KEY,
                        node_id TEXT NOT NULL,
                        text TEXT NOT NULL,
                        direction TEXT NOT NULL,
                        status TEXT NOT NULL,
                        kind TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        is_synced INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_node_id ON chat_messages(node_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_is_synced ON chat_messages(is_synced)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_created_at ON chat_messages(created_at)")
            }
        }
    }
}
