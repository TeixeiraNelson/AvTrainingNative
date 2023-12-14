package com.example.avtrainingnative

import android.content.Context
import android.media.Image
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer


/** Helper type alias used for analysis use case callbacks */
typealias imageResult = (result: ByteArray) -> Unit

class ImageAnalyzer(
    private val cropListener: imageResult,
    private val crossCorrListener: imageResult,
    private val context: Context,
) : ImageAnalysis.Analyzer, ImageCapture.OnImageCapturedCallback() {
    private var template: ByteArray? = null

    init {
        loadTemplate()
    }

    override fun analyze(image: ImageProxy) {
        if(!MainActivity.isPictureClicked){
            image.close()
            return
        }

        // Get the image data from the ImageProxy
        val grayScaleImage = image.planes[0].buffer
        val grayScaleImageByteArray = ByteArray(grayScaleImage.remaining())
        grayScaleImage.get(grayScaleImageByteArray)

        val croppedImage = cropImage(
            grayScaleImageByteArray,
            image.width,
            image.height,
            MainActivity.cropArea.width,
            MainActivity.cropArea.height,
            MainActivity.cropCenter.x,
            MainActivity.cropCenter.y)

        cropListener.invoke(croppedImage)

        //Call the native method to perform cross corr
        val result = template?.let {
            crossCorr(
                croppedImage,
                it,
                MainActivity.cropArea.width,
                MainActivity.cropArea.height
            )
        }
        // Notify the listener with the result
        if (result != null) {
            crossCorrListener.invoke(result)
        }

        // Close the image
        image.close()
        MainActivity.isPictureClicked = false
    }

    private fun loadTemplate() {
        try {
            // Open an InputStream to read the PGM file from the assets directory
            val inputStream: InputStream = context.assets.open("images/template.pgm")

            /* String magicNumber = */
            val magicNumber = readToken(inputStream) // Parses the magic number.

            val width: Int = readInt(inputStream)
            val height: Int = readInt(inputStream)
            val maxValue = readInt(inputStream)

            val size = width * height
            var byteBuffer = ByteBuffer.allocateDirect(size)
            val bytes = ByteArray(size)

            // The loop enables reading the file by chunks (depending on the type of input
            // stream).
            var numberOfBytesRead: Int
            while (inputStream.read(bytes).also { numberOfBytesRead = it } >= 0) byteBuffer.put(
                bytes,
                0,
                numberOfBytesRead
            )

            byteBuffer.rewind()
            byteBuffer.get(bytes) // Retrieve data from ByteBuffer
            template = bytes

        } catch (ex: IOException) {
            // Handle exceptions (e.g., file not found)
            ex.printStackTrace()
            return
        }
    }


    @OptIn(ExperimentalGetImage::class)
    override fun onCaptureSuccess(imageProxy: ImageProxy) {
        val image: Image? = imageProxy.image
        imageProxy.close() // Make sure to close the image
    }

    @Throws(IOException::class)
    private fun readToken(inputStream: InputStream): String? {
        val stringBuilder = StringBuilder() // Accumulates the parsed characters.
        var b: Int // Stores the current byte read.

        // accumulates the characters as long as there is no whitespace.
        while (!Character.isWhitespace(readChar(inputStream).also {
                b = it.toByte().toInt()
            })) stringBuilder.append(
            b.toChar()
        )
        return stringBuilder.toString() // Decodes the parsed token as an integer.
    }


    @Throws(IOException::class)
    private fun readInt(inputStream: InputStream): Int {
        return readToken(inputStream)!!.toInt() // Decodes the parsed token as an integer.
    }

    @Throws(IOException::class)
    private fun readChar(inputStream: InputStream): Char {
        val character = inputStream.read().toChar()
        return if (character == '#') // A comment occurs.
        {
            // Skips next characters up to end of line.
            // char b;
            while (inputStream.read().toChar() != '\n') {
            }
            readChar(
                inputStream
            ) // Calls the method recursively to skip all subsequent comment lines if any.
        } else character
    }

    external fun cropImage(
        sourceImage: ByteArray,
        imageWidth: Int,
        imageHeight: Int,
        areaWidth: Int,
        areaHeight: Int,
        areaCenterX: Int,
        areaCenterY: Int,
    ): ByteArray

    external fun crossCorr(
        sourceImage: ByteArray,
        templateImage: ByteArray,
        imageWidth: Int,
        imageHeight: Int
    ): ByteArray
}