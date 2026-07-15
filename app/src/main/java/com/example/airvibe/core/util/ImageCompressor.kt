package com.example.airvibe.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.roundToInt

object ImageCompressor {

    /**
     * Comprime una imagen a Base64 manteniendo el tamaño menor a un máximo de kilobytes.
     * Ideal para el payload de Bluetooth.
     */
    fun compressToBase64(context: Context, uri: Uri, maxKilobytes: Int = 20, maxResolution: Int = 100): String? {
        return runCatching {
            val bitmap = decodeSampledBitmapFromUri(context, uri, maxResolution, maxResolution) ?: return null
            
            var quality = 80
            var base64String = ""
            var compressedBytes: ByteArray
            
            do {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                compressedBytes = outputStream.toByteArray()
                base64String = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
                quality -= 10
            } while (compressedBytes.size > maxKilobytes * 1024 && quality > 10)
            
            base64String
        }.getOrNull()
    }

    /**
     * Comprime una imagen a un ByteArray manteniendo una resolución razonable, para subir a Supabase.
     */
    fun compressForUpload(context: Context, uri: Uri, maxResolution: Int = 1080): ByteArray? {
        return runCatching {
            val bitmap = decodeSampledBitmapFromUri(context, uri, maxResolution, maxResolution) ?: return null
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.toByteArray()
        }.getOrNull()
    }

    private fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        var inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        
        // Primero, decodificamos solo los bordes para obtener las dimensiones
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()

        // Calculamos inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // Decodificamos la imagen real usando inSampleSize
        options.inJustDecodeBounds = false
        inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()

        // Opcional: Escalar exactamente si inSampleSize no es suficiente
        return if (bitmap != null && (bitmap.width > reqWidth || bitmap.height > reqHeight)) {
            val ratio = minOf(reqWidth.toFloat() / bitmap.width, reqHeight.toFloat() / bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).roundToInt(),
                (bitmap.height * ratio).roundToInt(),
                true
            )
        } else {
            bitmap
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
