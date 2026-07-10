package com.example.airvibe.feature.chat.domain.model

/**
 * Mensaje inmutable que forma parte del historial de chat
 * con un peer descubierto por el radar.
 *
 * El modelo es de **dominio**: la capa de presentación lo consume
 * directamente sin necesidad de mapping adicional. Las
 * conversiones Entity ↔ Domain se hacen en `data/mapper`.
 *
 * @property id            Identificador único universal (UUID).
 * @property nodeId        Identificador estable del peer (perfil).
 * @property text          Contenido textual del mensaje.
 * @property direction     Si fue enviado por el usuario o recibido.
 * @property status        Estado del ciclo de vida.
 * @property kind          Subtipo (texto, invitación, sistema).
 * @property createdAt     Timestamp de creación (epoch millis).
 * @property isSynced      Bandera para el motor de sync a Supabase.
 */
data class ChatMessage(
    val id: String,
    val nodeId: String,
    val text: String,
    val direction: MessageDirection,
    val status: MessageStatus,
    val kind: MessageKind,
    val createdAt: Long,
    val isSynced: Boolean,
)
