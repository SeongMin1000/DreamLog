package com.example.dreamlog.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object EmotionAnalyzer {

    private lateinit var tflite: Interpreter
    private val emotions = listOf(
        "Angry",      // 0
        "Contempt",   // 1
        "Disgust",    // 2
        "Fear",       // 3
        "Happy",      // 4
        "Sad",        // 5
        "Surprise"    // 6
    )

    fun initModel(context: Context, modelName: String = "model.tflite") {
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
        return emotions.getOrNull(maxIdx) ?: "Unknown"
    }


    // 64x64, 3채널, 0~1 float 정규화로 변환
    private fun preprocess(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val resized = Bitmap.createScaledBitmap(bitmap, 64, 64, true)
        val input = Array(1) { Array(64) { Array(64) { FloatArray(3) } } }
        for (i in 0 until 64) {
            for (j in 0 until 64) {
                val pixel = resized.getPixel(j, i)
                input[0][i][j][0] = ((pixel shr 16) and 0xFF) / 255.0f // R
                input[0][i][j][1] = ((pixel shr 8) and 0xFF) / 255.0f  // G
                input[0][i][j][2] = (pixel and 0xFF) / 255.0f          // B
            }
        }
        return input
    }

}
