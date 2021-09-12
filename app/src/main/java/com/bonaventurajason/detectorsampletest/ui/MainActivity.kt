package com.bonaventurajason.detectorsampletest.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bonaventurajason.detectorsampletest.databinding.ActivityMainBinding
import com.bonaventurajason.detectorsampletest.utils.Constant.MODEL_NAME
import com.google.mlkit.common.model.CustomRemoteModel
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.linkfirebase.FirebaseModelSource
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val remoteModel = CustomRemoteModel
            .Builder(FirebaseModelSource.Builder(MODEL_NAME).build())
            .build()

        downloadModel(remoteModel)
        setOnClickListener()

        RemoteModelManager.getInstance().isModelDownloaded(remoteModel)
            .addOnSuccessListener {
                binding.camera.isEnabled = it
            }
    }

    private fun setOnClickListener() {
        binding.camera.setOnClickListener {
            startActivity(Intent(this@MainActivity, CameraActivity::class.java))
        }
    }

    private fun downloadModel(remoteModel: CustomRemoteModel) {
        val downloadOptions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        RemoteModelManager.getInstance().download(remoteModel, downloadOptions)
            .addOnSuccessListener {
                binding.camera.isEnabled = true
            }
    }
}