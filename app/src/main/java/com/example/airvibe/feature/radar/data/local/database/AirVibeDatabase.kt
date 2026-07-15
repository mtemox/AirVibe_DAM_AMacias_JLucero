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
import com.example.airvibe.feature.chat.data.local.entity.RoomMemberEntity
import com.example.airvibe.feature.chat.data.local.entity.RoomMessageEntity
import com.example.airvibe.feature.radar.data.local.dao.HandshakeRequestDao
import com.example.airvibe.feature.radar.data.local.dao.ProfileViewDao
import com.example.airvibe.feature.radar.data.local.dao.RadarDao
import com.example.airvibe.feature.radar.data.local.dao.SavedContactDao
import com.example.airvibe.feature.radar.data.local.entity.HandshakeRequestEntity
import com.example.airvibe.feature.radar.data.local.entity.NodeEntity
import com.example.airvibe.feature.radar.data.local.entity.ProfileViewEntity
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
        HandshakeRequestEntity::class,
        RoomMemberEntity::class,
        ProfileViewEntity::class,
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
    abstract fun handshakeRequestDao(): HandshakeRequestDao
    abstract fun profileViewDao(): ProfileViewDao

    companion object {
        const val DATABASE_NAME = "airvibe.db"
        const val SCHEMA_VERSION = 14

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
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                )
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

        /**
         * Feature 2 — Payload extendido (Premium).
         * Añade columnas para headline, bio, is_premium y
         * premium_catalog tanto en `radar_nodes` como en
         * `saved_contacts`. Migración aditiva: no rompe filas
         * existentes (todos los defaults son vacíos / FALSE).
         */
        private val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE radar_nodes ADD COLUMN headline TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE radar_nodes ADD COLUMN bio TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE radar_nodes ADD COLUMN is_premium INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE radar_nodes ADD COLUMN premium_catalog TEXT")

                db.execSQL("ALTER TABLE saved_contacts ADD COLUMN is_premium INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE saved_contacts ADD COLUMN premium_catalog TEXT")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_radar_nodes_is_premium ON radar_nodes(is_premium)")
            }
        }

        /**
         * Feature 3 — Handshake (Conexión y Networking P2P).
         * Crea la tabla `handshake_requests` para encolar
         * solicitudes entrantes y salientes con su estado.
         * Migración aditiva — no toca tablas previas.
         */
        private val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS handshake_requests (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        owner_id TEXT NOT NULL,
                        handshake_id TEXT NOT NULL,
                        peer_node_id TEXT NOT NULL,
                        peer_display_name TEXT NOT NULL DEFAULT '',
                        peer_headline TEXT NOT NULL DEFAULT '',
                        peer_status TEXT NOT NULL DEFAULT '',
                        peer_presence TEXT NOT NULL DEFAULT 'Online',
                        peer_tags TEXT NOT NULL DEFAULT '',
                        handshake_key TEXT NOT NULL,
                        status TEXT NOT NULL DEFAULT 'Pending',
                        direction TEXT NOT NULL DEFAULT 'Incoming',
                        created_at INTEGER NOT NULL,
                        responded_at INTEGER
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_handshake_requests_handshake_id ON handshake_requests(handshake_id)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_handshake_requests_owner_id ON handshake_requests(owner_id)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_handshake_requests_status ON handshake_requests(status)",
                )
            }
        }

        /**
         * Feature 4 — Comunicación P2P (Salas de Proximidad).
         * Crea la tabla `room_members` para mantener la lista
         * activa de Guests por sala. Migración aditiva.
         */
        private val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS room_members (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        room_id TEXT NOT NULL,
                        node_id TEXT NOT NULL,
                        display_name TEXT NOT NULL DEFAULT '',
                        role TEXT NOT NULL DEFAULT 'Guest',
                        is_active INTEGER NOT NULL DEFAULT 1,
                        joined_at INTEGER NOT NULL,
                        last_seen_at INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_room_members_room_id ON room_members(room_id)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_room_members_node_id ON room_members(node_id)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_room_members_active ON room_members(is_active)",
                )
            }
        }

        /**
         * Feature 5 — Sincronización Diferida + Analíticas Premium.
         * Crea la tabla `profile_views` (buffer local de eventos
         * de telemetría antes de empujarlos a Supabase).
         */
        private val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS profile_views (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        target_user_id TEXT NOT NULL,
                        source_node_id TEXT NOT NULL,
                        kind TEXT NOT NULL DEFAULT 'View',
                        created_at INTEGER NOT NULL,
                        is_synced INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_profile_views_target ON profile_views(target_user_id)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_profile_views_synced ON profile_views(is_synced)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_profile_views_created ON profile_views(created_at)",
                )
            }
        }
        /**
         * Feature 6 — Mensajes No Leídos.
         * Añade la columna 'is_read' a chat_messages.
         */
        private val MIGRATION_10_11: Migration = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE chat_messages ADD COLUMN is_read INTEGER NOT NULL DEFAULT 1",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_chat_messages_is_read ON chat_messages(is_read)",
                )
            }
        }

        /**
         * Feature 7 — Borrado Offline.
         * Añade la columna 'is_deleted' a chat_messages y saved_contacts.
         */
        private val MIGRATION_11_12: Migration = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE chat_messages ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_chat_messages_is_deleted ON chat_messages(is_deleted)",
                )
                db.execSQL(
                    "ALTER TABLE saved_contacts ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        /**
         * Feature 7 — Borrado Offline de Grupos.
         * Añade la columna 'is_deleted' a proximity_rooms.
         */
        private val MIGRATION_12_13: Migration = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE proximity_rooms ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_proximity_rooms_is_deleted ON proximity_rooms(is_deleted)",
                )
            }
        }

        /**
         * Feature 8 — Foto de Perfil.
         * Añade 'avatar_url' y 'avatar_base64' a 'radar_nodes' y 'saved_contacts'.
         */
        private val MIGRATION_13_14: Migration = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE radar_nodes ADD COLUMN avatar_url TEXT")
                db.execSQL("ALTER TABLE radar_nodes ADD COLUMN avatar_base64 TEXT")
                db.execSQL("ALTER TABLE saved_contacts ADD COLUMN avatar_url TEXT")
                db.execSQL("ALTER TABLE saved_contacts ADD COLUMN avatar_base64 TEXT")
            }
        }
    }
}
