package com.jarabaimport.levantamiento.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtils {

    /** Lado mayor máximo con el que se guardan las fotos (suficiente para informes, ahorra espacio). */
    const val MAX_SIDE = 1920

    /**
     * Lee una imagen (de la cámara o galería), corrige la rotación EXIF, la reduce
     * y la guarda como JPEG en [dest]. Devuelve true si se guardó correctamente.
     */
    fun normalizeToJpeg(context: Context, source: Uri, dest: File, maxSide: Int = MAX_SIDE): Boolean {
        return try {
            val rotation = context.contentResolver.openInputStream(source)?.use { exifRotation(it) } ?: 0
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSide)
            }
            val decoded = context.contentResolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return false
            val scaled = scaleDown(decoded, maxSide)
            val rotated = rotate(scaled, rotation)
            dest.parentFile?.mkdirs()
            FileOutputStream(dest).use { rotated.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Decodifica un archivo reducido para mostrarlo en pantalla o en el PDF. */
    fun decodeSampled(path: String, reqSide: Int): Bitmap? {
        return try {
            val f = File(path)
            if (!f.exists()) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0) return null
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, reqSide)
            }
            BitmapFactory.decodeFile(path, opts)
        } catch (e: Exception) {
            null
        }
    }

    private fun sampleSize(w: Int, h: Int, req: Int): Int {
        var s = 1
        while (maxOf(w, h) / (s * 2) >= req) s *= 2
        return s
    }

    private fun scaleDown(b: Bitmap, maxSide: Int): Bitmap {
        val longest = maxOf(b.width, b.height)
        if (longest <= maxSide) return b
        val f = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(b, (b.width * f).toInt(), (b.height * f).toInt(), true)
    }

    private fun exifRotation(input: InputStream): Int = try {
        when (ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: Exception) {
        0
    }

    private fun rotate(b: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return b
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
    }
}
