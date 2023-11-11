package com.example.avtrainingnative

import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Button
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

    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService

    // Views
    private lateinit var overlayView: OverlayView
    private lateinit var leftButton: Button
    private lateinit var rightButton: Button
    private lateinit var bottomButton: Button
    private lateinit var upButton: Button

    // Capture settings
    private val cropArea = Size(250, 250)
    private val cropCenter = Point(960, 540)
    private val translatationStep = 10

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

        leftButton.setOnClickListener {
            cropCenter.x -= translatationStep
            setRectanglePositionAndDimensions()
        }

        rightButton.setOnClickListener {
            cropCenter.x += translatationStep
            setRectanglePositionAndDimensions()
        }

        bottomButton.setOnClickListener {
            cropCenter.y += translatationStep
            setRectanglePositionAndDimensions()
        }

        upButton.setOnClickListener {
            cropCenter.y -= translatationStep
            setRectanglePositionAndDimensions()
        }

        setRectanglePositionAndDimensions()
    }

    private fun setRectanglePositionAndDimensions() {
        overlayView.setRectDimensions(cropArea.width.toFloat(), cropArea.height.toFloat())
        overlayView.setRectCenter(cropCenter.x.toFloat(), cropCenter.y.toFloat())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
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
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ArgbAnalyzer { argbValues ->
                        Log.d(TAG, "Average luminosity: $argbValues")
                    })
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

        // Used to load the 'avtrainingnative' library on application startup.
        init {
            System.loadLibrary("avtrainingnative")
        }
    }
}