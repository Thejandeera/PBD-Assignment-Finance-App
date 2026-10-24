package com.clarity.finance.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileImageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val avatarsDir = File(context.filesDir, "profile_avatars").apply {
        if (!exists()) mkdirs()
    }

    fun saveProfileImage(userId: String, sourceUri: Uri): String? {
        return try {
            val destination = File(avatarsDir, "${userId}_avatar.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            destination.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getProfileImageFile(userId: String): File? {
        val file = File(avatarsDir, "${userId}_avatar.jpg")
        return if (file.exists() && file.length() > 0) file else null
    }

    fun deleteProfileImage(userId: String): Boolean {
        val file = File(avatarsDir, "${userId}_avatar.jpg")
        return if (file.exists()) file.delete() else true
    }

    companion object {
        fun loadBitmap(filePath: String?): ImageBitmap? {
            if (filePath.isNullOrBlank()) return null
            val file = File(filePath)
            if (!file.exists() || file.length() == 0L) return null
            return try {
                // Decode with inSampleSize if too large
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }
}
