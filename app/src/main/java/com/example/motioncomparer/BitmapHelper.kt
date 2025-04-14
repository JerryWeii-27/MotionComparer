package com.example.motioncomparer

import android.graphics.Bitmap
import androidx.core.graphics.scale

class BitmapHelper
{
    companion object
    {
        fun scaleBitmap(
            bitmap : Bitmap,
            maxWidth : Int,
            maxHeight : Int
        ) : Bitmap
        {
            val width = bitmap.width
            val height = bitmap.height

            val widthRatio = maxWidth.toFloat() / width
            val heightRatio = maxHeight.toFloat() / height
            val scale = minOf(widthRatio, heightRatio)

            val newWidth = (width * scale).toInt()
            val newHeight = (height * scale).toInt()

            return bitmap.scale(newWidth, newHeight)
        }
    }
}