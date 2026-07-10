package com.example.airvibe.feature.chat.domain.model

/**
 * Dirección de un mensaje dentro de una conversación. Permite a la
 * UI alinear las burbujas a la derecha o izquierda al estilo iOS.
 */
enum class MessageDirection {
    /** Mensaje que el usuario local escribió. */
    Outgoing,

    /** Mensaje recibido desde un peer por Bluetooth. */
    Incoming,
}
