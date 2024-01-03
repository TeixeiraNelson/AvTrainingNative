package com.example.avtrainingnative

import android.R.attr
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.avtrainingnative.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    private lateinit var resultImage: ImageView

    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService

    // Views
    private lateinit var overlayView: OverlayView
    private lateinit var leftButton: Button
    private lateinit var rightButton: Button
    private lateinit var bottomButton: Button
    private lateinit var upButton: Button
    private lateinit var takePicture: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        initViews()
    }

    private fun initViews() {
        overlayView = viewBinding.overlayView

        leftButton = viewBinding.translateLeft
        rightButton = viewBinding.translateRight
        bottomButton = viewBinding.translateBottom
        upButton = viewBinding.translateUp

        takePicture = viewBinding.takePicture

        resultImage = viewBinding.crossCorrResult

        leftButton.setOnClickListener {
            cropCenter.x -= translationStep
            setRectanglePositionAndDimensions()
        }

        rightButton.setOnClickListener {
            cropCenter.x += translationStep
            setRectanglePositionAndDimensions()
        }

        bottomButton.setOnClickListener {
            cropCenter.y += translationStep
            setRectanglePositionAndDimensions()
        }

        upButton.setOnClickListener {
            cropCenter.y -= translationStep
            setRectanglePositionAndDimensions()
        }

        takePicture.setOnClickListener {
            isPictureClicked = true
            takePicture.isEnabled = false
            viewBinding.progressCircular.visibility = View.VISIBLE
        }

        setRectanglePositionAndDimensions()
    }

    private fun setRectanglePositionAndDimensions() {
        overlayView.setRectDimensions(cropArea.width.toFloat(), cropArea.height.toFloat())
        overlayView.setRectCenter(cropCenter.x.toFloat(), cropCenter.y.toFloat())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(1920, 1080))
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ImageAnalyzer(
                        context = this@MainActivity,
                        cropListener = { cropResult ->
                            val bitmap = byteArrayToBitmap(cropResult, cropArea.width, cropArea.height)
                            runOnUiThread {
                                viewBinding.cropResult.setImageBitmap(bitmap)
                            }
                        },
                        crossCorrListener = { xCorrResult ->
                            val bitmap = byteArrayToBitmap(xCorrResult.resultArray, cropArea.width, cropArea.height)
                            runOnUiThread {
                                viewBinding.crossCorrResult.setImageBitmap(bitmap)
                                takePicture.isEnabled = true
                                viewBinding.snrValue.text = xCorrResult.snr.toString()
                                viewBinding.progressCircular.visibility = View.INVISIBLE
                            }
                        }
                    ))
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    fun byteArrayToBitmap(data: ByteArray, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Fill the bitmap with grayscale data
        val pixels = IntArray(width * height)
        for (i in data.indices) {
            val gray: Int = data[i].toInt() and 0xFF
            pixels[i] = 0xFF000000.toInt() or (gray shl 16) or (gray shl 8) or gray
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        return bitmap
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                android.Manifest.permission.CAMERA
            ).toTypedArray()

        // Capture settings
        var cropArea = Size(256, 256)
        var cropCenter = Point(960, 540)
        var isPictureClicked = false
        const val translationStep = 10

        // Used to load the 'avtrainingnative' library on application startup.
        init {
            System.loadLibrary("avtrainingnative")
        }
    }
}