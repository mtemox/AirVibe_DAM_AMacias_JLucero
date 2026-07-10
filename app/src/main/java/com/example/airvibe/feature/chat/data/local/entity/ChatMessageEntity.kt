package com.example.airvibe.feature.chat.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Fila de la tabla `chat_messages`.
 *
 * Cada mensaje pertenece a una conversación con un peer
 * (identificado por [nodeId]). El flag [isSynced] permite al
 * [com.example.airvibe.feature.radar.data.sync.SyncWorker]
 * (extendido en el futuro) empujar el historial a la nube.
 *
 * Decisiones de diseño:
 *
 *  - [id] es un UUID generado en el cliente. Permite deduplicar
 *    fácilmente entre el emisor y el receptor.
 *  - [nodeId] **no** es FK porque un peer puede no existir aún
 *    en `radar_nodes` cuando llega un mensaje (cold start).
 *  - Índices en [nodeId], [isSynced] y [createdAt] para que el
 *    ORDER BY de la bandeja de entrada sea barato.
 */
@Entity(
    tableName = "chat_messages",
    indices = [
        Index(value = ["node_id"]),
        Index(value = ["is_synced"]),
        Index(value = ["created_at"]),
    ],
)
data class ChatMessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "node_id")
    val nodeId: String,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "direction")
    val direction: String,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "kind")
    val kind: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "is_synced", defaultValue = "0")
    val isSynced: Boolean = false,
)
