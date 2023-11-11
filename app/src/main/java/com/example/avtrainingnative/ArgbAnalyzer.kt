package com.example.avtrainingnative

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer


/** Helper type alias used for analysis use case callbacks */
typealias argbResult = (argb: ArgbResult) -> Unit

class ArgbAnalyzer(private val listener: argbResult) : ImageAnalysis.Analyzer {
    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

    override fun analyze(image: ImageProxy) {


        //listener(luma)

        image.close()
    }


    /**
     * A native method that is implemented by the 'avtrainingnative' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String
}

data class ArgbResult(val alpha: Double, val red: Double, val green: Double, val blue: Double)