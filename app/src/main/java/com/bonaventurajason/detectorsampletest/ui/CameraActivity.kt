package com.bonaventurajason.detectorsampletest.ui

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.bonaventurajason.detectorsampletest.databinding.ActivityCameraBinding
import com.bonaventurajason.detectorsampletest.utils.CameraUtility
import com.bonaventurajason.detectorsampletest.utils.Constant
import com.bonaventurajason.detectorsampletest.utils.Constant.MODEL_NAME
import com.bonaventurajason.detectorsampletest.utils.Draw
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.model.CustomRemoteModel
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.linkfirebase.FirebaseModelSource
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber

const val REQUEST_CODE_PERMISSIONS = 10

class CameraActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    private lateinit var binding: ActivityCameraBinding
    private lateinit var objectDetector: ObjectDetector
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val remoteModel =
            CustomRemoteModel
                .Builder(FirebaseModelSource.Builder(MODEL_NAME).build())
                .build()

        requestPermission()

        if (CameraUtility.hasCameraPermission(this)) {
            startCamera(remoteModel)
        }


    }

    private fun startCamera(remoteModel: CustomRemoteModel) {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))

        RemoteModelManager.getInstance().isModelDownloaded(remoteModel)
            .addOnSuccessListener {
                val optionsBuilder =
                    CustomObjectDetectorOptions.Builder(remoteModel)

                val customObjectDetectorOptions = optionsBuilder
                    .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
                    .enableClassification()
                    .setClassificationConfidenceThreshold(0.5f)
                    .setMaxPerObjectLabelCount(3)
                    .build()
                objectDetector =
                    ObjectDetection.getClient(customObjectDetectorOptions)
            }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.setSurfaceProvider(binding.cameraLayout.surfaceProvider)

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), { imageProxy ->
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val image = imageProxy.image

            if (image != null) {
                val processImage = InputImage.fromMediaImage(image, rotationDegrees)

                objectDetector
                    .process(processImage)
                    .addOnSuccessListener { results ->
                        for (detectedObjects in results) {
                            Timber.d("AAA Detected Objects ${detectedObjects.labels}")

                            if (binding.parentLayout.childCount > 1) binding.parentLayout.removeViewAt(
                                1
                            )

                            val element = Draw(
                                context = this,
                                rect = detectedObjects.boundingBox,
                                text = detectedObjects.labels.firstOrNull()?.text ?: "Undefined"
                            )
                            binding.parentLayout.addView(element)

                            binding.resultValue.text =
                                detectedObjects.labels.firstOrNull()?.text ?: "Undefined"
                        }

                        imageProxy.close()
                    }
                    .addOnFailureListener {
                        Timber.d("AAA Exception ${it.message}")
                        imageProxy.close()
                    }
            }
        })

        cameraProvider.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector,
            imageAnalysis,
            preview
        )
    }

    private fun requestPermission() {
        if (CameraUtility.hasCameraPermission(this)) {
            return
        }

        EasyPermissions.requestPermissions(
            this,
            "You need to accept camera permission to use this app",
            REQUEST_CODE_PERMISSIONS,
            android.Manifest.permission.CAMERA
        )
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {}

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
}