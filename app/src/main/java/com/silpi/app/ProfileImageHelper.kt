package com.silpi.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.ImageView
import kotlin.math.min
import java.io.ByteArrayOutputStream

object ProfileImageHelper {
    private const val DEFAULT_PROFILE_TAG = "default_profile_image"

    fun setProfileImage(imageView: ImageView, profileImageData: String) {
        if (profileImageData.isBlank()) {
            setDefaultProfileImage(imageView)
            return
        }

        try {
            val imageBytes = Base64.decode(profileImageData, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (bitmap != null) {
                imageView.tag = null
                imageView.setBackgroundResource(R.drawable.bg_default_profile)
                imageView.setPadding(0, 0, 0, 0)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                imageView.setImageBitmap(bitmap)
            } else {
                setDefaultProfileImage(imageView)
            }
        } catch (e: IllegalArgumentException) {
            setDefaultProfileImage(imageView)
        }
    }

    private fun setDefaultProfileImage(imageView: ImageView) {
        imageView.tag = DEFAULT_PROFILE_TAG
        imageView.setBackgroundResource(R.drawable.bg_default_profile)
        imageView.setImageResource(R.drawable.ic_default_profile_person)
        imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
        val applyDefaultPadding = {
            val size = min(imageView.width, imageView.height)
            if (size > 0 && imageView.tag == DEFAULT_PROFILE_TAG) {
                val padding = (size * 0.26f).toInt()
                imageView.setPadding(padding, padding, padding, padding)
            }
        }

        if (imageView.width > 0 && imageView.height > 0) {
            applyDefaultPadding()
        } else {
            imageView.post { applyDefaultPadding() }
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
