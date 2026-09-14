package com.example.noisewatch.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object PhotoStorageManager {

    private const val FILE_PROVIDER_AUTHORITY = "com.example.noisewatch.fileprovider"
    private const val PHOTOS_FOLDER = "photos"

    fun createPhotoUri(context: Context): Pair<Uri, File> {
        val photosDir = File(context.filesDir, PHOTOS_FOLDER)
        if (!photosDir.exists()) {
            photosDir.mkdirs()
        }
        val photoFile = File(photosDir, "noisewatch_photo_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, photoFile)
        return Pair(uri, photoFile)
    }

    fun optimizePhotoFile(file: File) {
        if (!file.exists() || file.length() == 0L) return

        try {
            val filePath = file.absolutePath
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)

            val maxDimension = 1920
            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            var bitmap = BitmapFactory.decodeFile(filePath, decodeOptions) ?: return

            // Fix orientation if needed
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }

            if (!matrix.isIdentity) {
                val rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )
                if (rotatedBitmap != bitmap) {
                    bitmap.recycle()
                    bitmap = rotatedBitmap
                }
            }

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            bitmap.recycle()
        } catch (_: Exception) {
            // If optimization fails, leave raw image untouched
        }
    }

    fun cropAndSavePhoto(
        context: Context,
        sourceFile: File,
        cropLeft: Int,
        cropTop: Int,
        cropWidth: Int,
        cropHeight: Int,
        targetWidth: Int = 1920,
        targetHeight: Int = 1080
    ): File? {
        if (!sourceFile.exists() || sourceFile.length() == 0L) return null

        return try {
            val filePath = sourceFile.absolutePath
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            var originalBitmap = BitmapFactory.decodeFile(filePath) ?: return null

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }

            if (!matrix.isIdentity) {
                val rotated = Bitmap.createBitmap(
                    originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true
                )
                if (rotated != originalBitmap) {
                    originalBitmap.recycle()
                    originalBitmap = rotated
                }
            }

            val validLeft = cropLeft.coerceIn(0, originalBitmap.width - 1)
            val validTop = cropTop.coerceIn(0, originalBitmap.height - 1)
            val validWidth = cropWidth.coerceIn(1, originalBitmap.width - validLeft)
            val validHeight = cropHeight.coerceIn(1, originalBitmap.height - validTop)

            val croppedBitmap = Bitmap.createBitmap(
                originalBitmap,
                validLeft,
                validTop,
                validWidth,
                validHeight
            )
            if (croppedBitmap != originalBitmap) {
                originalBitmap.recycle()
            }

            val finalBitmap = if (validWidth > targetWidth || validHeight > targetHeight) {
                val scaleFactor = minOf(
                    targetWidth.toFloat() / validWidth,
                    targetHeight.toFloat() / validHeight
                )
                val scaledW = (validWidth * scaleFactor).toInt().coerceAtLeast(1)
                val scaledH = (validHeight * scaleFactor).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(croppedBitmap, scaledW, scaledH, true)
            } else {
                croppedBitmap
            }

            val photosDir = File(context.filesDir, PHOTOS_FOLDER)
            if (!photosDir.exists()) photosDir.mkdirs()
            val outputFile = File(photosDir, "noisewatch_photo_${System.currentTimeMillis()}.jpg")

            FileOutputStream(outputFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            if (finalBitmap != croppedBitmap) {
                croppedBitmap.recycle()
            }
            finalBitmap.recycle()

            // Delete temporary raw source file
            if (sourceFile.exists()) {
                sourceFile.delete()
            }

            outputFile
        } catch (_: Exception) {
            null
        }
    }

    fun deletePhoto(context: Context, photoUriString: String?) {
        if (photoUriString.isNullOrBlank()) return
        try {
            val fileToDelete = if (photoUriString.startsWith("/")) {
                File(photoUriString)
            } else {
                val uri = Uri.parse(photoUriString)
                if (uri.scheme == "file") {
                    File(uri.path ?: return)
                } else {
                    val photosDir = File(context.filesDir, PHOTOS_FOLDER)
                    val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: return
                    File(photosDir, fileName)
                }
            }
            if (fileToDelete.exists() && fileToDelete.canonicalPath.startsWith(context.filesDir.canonicalPath)) {
                fileToDelete.delete()
            }
        } catch (_: Exception) {
            // Ignore deletion errors
        }
    }

    fun clearAllPhotos(context: Context) {
        try {
            val photosDir = File(context.filesDir, PHOTOS_FOLDER)
            if (photosDir.exists() && photosDir.isDirectory) {
                photosDir.listFiles()?.forEach { file ->
                    file.delete()
                }
            }
        } catch (_: Exception) {
            // Ignore clear errors
        }
    }
}
