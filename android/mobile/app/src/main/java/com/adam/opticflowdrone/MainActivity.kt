package com.adam.opticflowdrone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

class MainActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView
    private lateinit var cameraExecutor: ExecutorService

    private val frameCounter = AtomicLong(0L)
    @Volatile private var lastTimestampNs: Long? = null
    @Volatile private var lastUiUpdateMs: Long = 0L

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else statusText.text = "Camera permission denied"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        statusText = findViewById(R.id.statusText)

        cameraExecutor = Executors.newSingleThreadExecutor()

        ensureCameraPermissionAndStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun ensureCameraPermissionAndStart() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) startCamera() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    val tsNs = imageProxy.imageInfo.timestamp
                    val frameId = frameCounter.incrementAndGet()

                    val prevTs = lastTimestampNs
                    val dtSec = if (prevTs == null) 0.0 else (tsNs - prevTs) / 1_000_000_000.0
                    lastTimestampNs = tsNs

                    // stub output (flow=0, quality=255)
                    val flowXPx = 0.0
                    val flowYPx = 0.0
                    val quality = 255

                    val nowMs = System.currentTimeMillis()
                    if (nowMs - lastUiUpdateMs > 250) { // ~4Hz update
                        lastUiUpdateMs = nowMs

                        val msg = "frame=$frameId dt=${"%.3f".format(dtSec)} " +
                                "size=${imageProxy.width}x${imageProxy.height}\n" +
                                "flow=($flowXPx,$flowYPx) q=$quality"

                        Log.d("OpticFlowDrone", msg)
                        runOnUiThread { statusText.text = msg }
                    }
                } catch (e: Exception) {
                    Log.e("OpticFlowDrone", "Analyzer error", e)
                } finally {
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    analysis
                )
                statusText.text = "Camera started"
            } catch (e: Exception) {
                statusText.text = "Camera bind failed: ${e.message}"
                Log.e("OpticFlowDrone", "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }
}