package com.example.airvibe.feature.chat.domain.model

/**
 * Tipos de mensaje soportados por AirVibe.
 *
 * Hoy distinguimos entre un mensaje de texto normal y una
 * "invitación a grupo" generada por la acción **Broadcast** del
 * chat. Reservamos [System] para futuros eventos automatizados
 * (bienvenidas, kick, etc.) sin romper el esquema.
 */
enum class MessageKind {
    /** Mensaje de chat normal. */
    Text,

    /** Invitación broadcast a un grupo / sala. */
    GroupInvite,

    /** Mensaje generado por el sistema (futuro). */
    System,
}
