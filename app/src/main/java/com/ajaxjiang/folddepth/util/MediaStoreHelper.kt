package com.ajaxjiang.folddepth.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

object MediaStoreHelper {
    /**
     * Loads the latest image (photo or screenshot) from the device's MediaStore.
     * Safely downsamples very large camera images to prevent OutOfMemory errors.
     */
    fun loadLatestGalleryImage(context: Context): ImageBitmap? {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        return try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val id = cursor.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id,
                    )

                    context.contentResolver.openInputStream(contentUri)?.use { stream ->
                        val bytes = stream.readBytes()
                        val boundsOptions = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

                        // Scale down if image is larger than 2560px to keep rendering performant
                        val maxDim = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
                        var sampleSize = 1
                        while (maxDim / sampleSize > 2560) {
                            sampleSize *= 2
                        }

                        val decodeOptions = BitmapFactory.Options().apply {
                            inSampleSize = sampleSize
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                        bmp?.asImageBitmap()
                    }
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
