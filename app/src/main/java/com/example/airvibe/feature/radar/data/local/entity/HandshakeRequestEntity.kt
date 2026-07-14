package com.example.airvibe.feature.radar.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Estado de una solicitud de conexión P2P (Handshake).
 *
 * Esta entidad **no** es un contacto: representa el momento en
 * que A le ha pedido a B conectar, y el resultado (pendiente,
 * aceptado, rechazado). Cuando el estado pasa a `Accepted`, el
 * dominio promueve la fila a `saved_contacts` (offline-first).
 *
 * Una sola fila representa el último estado conocido entre
 * (owner_id, handshake_id) — el upsert es idempotente.
 *
 * @property id rowId interno (autogenerado).
 * @property ownerId userId de Supabase del dueño del registro.
 *   En perfiles no logueados queda como device-id estable.
 * @property handshakeId UUID generado por el emisor. Permite
 *   correlacionar `request` → `accept`/`reject` aunque el payload
 *   llegue por una sesión de Nearby distinta.
 * @property peerNodeId identificador estable del otro dispositivo.
 * @property peerDisplayName nombre del peer en el momento de la
 *   solicitud.
 * @property handshakeKey token opaco (la "llave" del feature).
 *   Se intercambia con el peer para sellar la conexión.
 * @property status `Pending`, `Accepted`, `Rejected`, `Expired`
 *   o `Cancelled`.
 * @property direction `Incoming` (yo recibí) o `Outgoing`
 *   (yo envié).
 */
@Entity(
    tableName = "handshake_requests",
    indices = [
        Index(value = ["owner_id"]),
        Index(value = ["status"]),
        Index(value = ["handshake_id"], unique = true),
    ],
)
data class HandshakeRequestEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "owner_id")
    val ownerId: String,

    @ColumnInfo(name = "handshake_id")
    val handshakeId: String,

    @ColumnInfo(name = "peer_node_id")
    val peerNodeId: String,

    @ColumnInfo(name = "peer_display_name")
    val peerDisplayName: String,

    @ColumnInfo(name = "peer_headline")
    val peerHeadline: String = "",

    @ColumnInfo(name = "peer_status")
    val peerStatus: String = "",

    @ColumnInfo(name = "peer_presence")
    val peerPresence: String = "Online",

    @ColumnInfo(name = "peer_tags")
    val peerTags: List<String> = emptyList(),

    @ColumnInfo(name = "handshake_key")
    val handshakeKey: String,

    @ColumnInfo(name = "status")
    val status: String = STATUS_PENDING,

    @ColumnInfo(name = "direction")
    val direction: String = DIRECTION_INCOMING,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "responded_at")
    val respondedAt: Long? = null,
) {
    companion object {
        const val STATUS_PENDING = "Pending"
        const val STATUS_ACCEPTED = "Accepted"
        const val STATUS_REJECTED = "Rejected"
        const val STATUS_EXPIRED = "Expired"
        const val STATUS_CANCELLED = "Cancelled"

        const val DIRECTION_INCOMING = "Incoming"
        const val DIRECTION_OUTGOING = "Outgoing"
    }
}
