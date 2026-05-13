package com.silpi.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.ImageView
import java.io.ByteArrayOutputStream

object ProfileImageHelper {
    fun setProfileImage(imageView: ImageView, profileImageData: String) {
        if (profileImageData.isBlank()) {
            imageView.setImageResource(R.drawable.padang)
            return
        }

        try {
            val imageBytes = Base64.decode(profileImageData, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
            } else {
                imageView.setImageResource(R.drawable.padang)
            }
        } catch (e: IllegalArgumentException) {
            imageView.setImageResource(R.drawable.padang)
        }
    }

    fun encodeProfileImage(context: Context, imageUri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(imageUri) ?: return ""
        val bitmap = inputStream.use { BitmapFactory.decodeStream(it) } ?: return ""
        val resizedBitmap = resizeBitmap(bitmap, 360)
        val outputStream = ByteArrayOutputStream()

        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 72, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val ratio = if (width > height) {
            maxSize.toFloat() / width.toFloat()
        } else {
            maxSize.toFloat() / height.toFloat()
        }

        return Bitmap.createScaledBitmap(
                bitmap,
                (width * ratio).toInt(),
                (height * ratio).toInt(),
                true
        )
    }
}
