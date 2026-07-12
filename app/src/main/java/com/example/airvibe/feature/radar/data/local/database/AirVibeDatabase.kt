package com.example.airvibe.feature.radar.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.airvibe.feature.chat.data.local.dao.ChatDao
import com.example.airvibe.feature.chat.data.local.dao.ProximityRoomDao
import com.example.airvibe.feature.chat.data.local.entity.ChatMessageEntity
import com.example.airvibe.feature.chat.data.local.entity.ProximityRoomEntity
import com.example.airvibe.feature.chat.data.local.entity.RoomMessageEntity
import com.example.airvibe.feature.radar.data.local.dao.RadarDao
import com.example.airvibe.feature.radar.data.local.dao.SavedContactDao
import com.example.airvibe.feature.radar.data.local.entity.NodeEntity
import com.example.airvibe.feature.radar.data.local.entity.SavedContactEntity

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
        SavedContactEntity::class,
        ChatMessageEntity::class,
        ProximityRoomEntity::class,
        RoomMessageEntity::class,
    ],
    version = AirVibeDatabase.SCHEMA_VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AirVibeDatabase : RoomDatabase() {

    abstract fun radarDao(): RadarDao
    abstract fun savedContactDao(): SavedContactDao
    abstract fun chatDao(): ChatDao
    abstract fun proximityRoomDao(): ProximityRoomDao

    companion object {
        const val DATABASE_NAME = "airvibe.db"
        const val SCHEMA_VERSION = 6

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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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

        private val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS proximity_rooms (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        host_node_id TEXT NOT NULL,
                        host_name TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        joined INTEGER NOT NULL DEFAULT 0,
                        is_host INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS room_messages (
                        id TEXT NOT NULL PRIMARY KEY,
                        room_id TEXT NOT NULL,
                        sender_node_id TEXT NOT NULL,
                        sender_name TEXT NOT NULL,
                        text TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_room_messages_room_id ON room_messages(room_id)")
            }
        }

        private val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS saved_contacts (
                        node_id TEXT NOT NULL PRIMARY KEY,
                        display_name TEXT NOT NULL,
                        headline TEXT NOT NULL,
                        bio TEXT NOT NULL,
                        status TEXT NOT NULL,
                        presence TEXT NOT NULL,
                        tags TEXT NOT NULL,
                        accent_color_argb INTEGER NOT NULL,
                        added_by_peer INTEGER NOT NULL DEFAULT 0,
                        is_synced INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO saved_contacts (
                        node_id, display_name, headline, bio, status, presence, tags,
                        accent_color_argb, added_by_peer, is_synced, created_at, updated_at
                    )
                    SELECT
                        id, display_name, status, detail, status, presence, tags,
                        accent_color_argb, 0, is_synced, created_at, updated_at
                    FROM radar_nodes
                    WHERE is_favorite = 1
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE proximity_rooms ADD COLUMN is_synced INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE room_messages ADD COLUMN is_synced INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_proximity_rooms_is_synced ON proximity_rooms(is_synced)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_room_messages_is_synced ON room_messages(is_synced)",
                )
            }
        }

        private val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // is_own: set at write-time so messages remain correctly attributed after reinstall
                db.execSQL(
                    "ALTER TABLE room_messages ADD COLUMN is_own INTEGER NOT NULL DEFAULT 0",
                )
            }
        }
    }
}
