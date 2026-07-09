package com.example.airvibe.feature.radar.data.local.database

import androidx.room.TypeConverter

/**
 * Conversores entre tipos de Kotlin y tipos primitivos que Room/SQLite
 * sabe persistir de forma nativa.
 *
 * Los enums se serializan como [String] (en lugar de ordinal) para que
 * el modelo sobreviva a reordenamientos accidentales. Las listas se
 * serializan como una cadena delimitada por un carácter poco común
 * (`\u001F` = Unit Separator), evitando colisiones con tags reales.
 */
class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String =
        value?.joinToString(separator = LIST_DELIMITER).orEmpty()

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        if (value.isNullOrEmpty()) emptyList() else value.split(LIST_DELIMITER)

    companion object {
        private const val LIST_DELIMITER = "\u001F"
    }
}
