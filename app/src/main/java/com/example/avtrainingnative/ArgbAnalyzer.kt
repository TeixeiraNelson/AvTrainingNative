package com.example.avtrainingnative

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer


/** Helper type alias used for analysis use case callbacks */
typealias argbResult = (argb: ArgbResult) -> Unit
typealias imageResult = (croppedImage: Bitmap) -> Unit

class ArgbAnalyzer(private val listener: argbResult, private val imageListener: imageResult) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        // Get the ARGB data from the ImageProxy
        val argbData = image.planes[0].buffer.toByteArray()

        /*
        //Call the native method to analyze the ARGB data with OpenCV
        val result = analyzeImageOpenCV(argbData, image.width, image.height, MainActivity.cropArea.width, MainActivity.cropArea.height, MainActivity.cropCenter.x, MainActivity.cropCenter.y)
        // Notify the listener with the result
        listener.invoke(ArgbResult(result[0], result[1], result[2], result[3])) */

        //Call the native method to analyze the ARGB data
        val result = analyzeImageCpp(argbData, image.width, image.height, MainActivity.cropArea.width, MainActivity.cropArea.height, MainActivity.cropCenter.x, MainActivity.cropCenter.y)
        // Notify the listener with the result
        listener.invoke(ArgbResult(result[0], result[1], result[2], result[3]))

        // Close the image
        image.close()
    }

    private fun ByteBuffer.toByteArray(): IntArray {
        rewind()    // Rewind the buffer to zero
        val data = IntArray(remaining() / 4)
        asIntBuffer().get(data)   // Copy the buffer into an int array
        return data // Return the int array
    }


    /**
     * A native method that is implemented by the 'avtrainingnative' native library,
     * which is packaged with this application.
     */
    external fun analyzeImageOpenCV(
        argbData: IntArray,
        imageWidth: Int,
        imageHeight: Int,
        areaWidth: Int,
        areaHeight: Int,
        areaCenterX: Int,
        areaCenterY: Int
    ): DoubleArray

    /**
     * A native method that is implemented by the 'avtrainingnative' native library,
     * which is packaged with this application.
     */
    external fun analyzeImageCpp(
        argbData: IntArray,
        imageWidth: Int,
        imageHeight: Int,
        areaWidth: Int,
        areaHeight: Int,
        areaCenterX: Int,
        areaCenterY: Int
    ): DoubleArray
}

data class ArgbResult(val alpha: Double, val red: Double, val green: Double, val blue: Double)