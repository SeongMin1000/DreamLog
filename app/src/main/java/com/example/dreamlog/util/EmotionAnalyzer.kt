// EmotionAnalyzer.kt
package com.example.dreamlog.util

import android.content.Context
import android.graphics.*
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object EmotionAnalyzer {

    private lateinit var tflite: Interpreter
    private val emotions = listOf("Angry", "Disgust", "Fear", "Happy", "Sad", "Surprise", "Neutral")

    fun initModel(context: Context, modelName: String = "tflite_model.tflite") {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        val mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        tflite = Interpreter(mappedByteBuffer)
    }

    fun analyze(bitmap: Bitmap): String {
        val input = preprocess(bitmap)
        val output = Array(1) { FloatArray(emotions.size) }
        tflite.run(input, output)
        val maxIdx = output[0].indices.maxByOrNull { output[0][it] } ?: -1
        return emotions[maxIdx]
    }

    private fun preprocess(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val resized = Bitmap.createScaledBitmap(bitmap, 48, 48, true)
        val gray = Bitmap.createBitmap(48, 48, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(gray)
        val paint = Paint()
        val matrix = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(resized, 0f, 0f, paint)

        val input = Array(1) { Array(48) { Array(48) { FloatArray(1) } } }
        for (i in 0 until 48) {
            for (j in 0 until 48) {
                val pixel = gray.getPixel(j, i)
                val value = (pixel and 0xFF) / 255.0f
                input[0][i][j][0] = value
            }
        }
        return input
    }
} 
