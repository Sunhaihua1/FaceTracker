package com.example.bluetooth_test.ui.video
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.bluetooth_test.R
import com.example.bluetooth_test.databinding.ActivityVideoRecordBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.common.util.concurrent.ListenableFuture
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Video_record : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var fabRecord: FloatingActionButton
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var binding: ActivityVideoRecordBinding

    // Quality selector for video recording
    private lateinit var qualitySelector: QualitySelector
    // 获取已添加的 Fragment 实例
    private lateinit var fragment_plot: Chart_plot

    private var videoCapture: VideoCapture<Recorder>? = null
    private var isRecording = false
    private var video_recorder: Recording? = null

    // 权限请求码
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
    private final val TAG = "VideoRecordActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoRecordBinding.inflate(layoutInflater)

        setContentView(binding.root)
        if (supportActionBar != null) {
            supportActionBar!!.hide()
        }
        // Initialize executor for camera operations
        cameraExecutor = Executors.newSingleThreadExecutor()
        fragment_plot = Chart_plot.newInstance(0)
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment_plot)
            .commit()

        previewView = binding.previewView
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        fabRecord = binding.fabRecord
        // Set up quality selector (you can adjust this based on your needs)
        qualitySelector = QualitySelector.from(
            Quality.HD,
            FallbackStrategy.higherQualityOrLowerThan(Quality.FHD))
        // Request permissions if not granted
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Set up record button click listener
        fabRecord.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
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
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        // Set up preview
        val preview = Preview.Builder()
            .setTargetRotation(previewView.display.rotation)
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        // Set up video capture with Recorder
        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        // Select front camera
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, videoCapture)
        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun startRecording() {
        fragment_plot.updateText("开始采集")

        val videoCapture = this.videoCapture ?: return

        // Create timestamped name and MediaStore entry
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        // Set up recording listener
        video_recorder = videoCapture.output
            .prepareRecording(this, outputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(this@Video_record,
                        Manifest.permission.RECORD_AUDIO) ==
                    PermissionChecker.PERMISSION_GRANTED) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        isRecording = true
                        fabRecord.setImageResource(R.drawable.end_record) // Update to your stop icon
                        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
                    }
                    is VideoRecordEvent.Finalize -> {
                        isRecording = false
                        fabRecord.setImageResource(R.drawable.start_record) // Update to your record icon
                        if (!recordEvent.hasError()) {
                            Toast.makeText(this,
                                "Video saved: ${recordEvent.outputResults.outputUri}",
                                Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this,
                                "Error: ${recordEvent.error}",
                                Toast.LENGTH_SHORT).show()
                        }
                        isRecording = false
                        fabRecord.setImageResource(R.drawable.start_record)
                        video_recorder = null

                    }
                }
            }
    }

    private fun stopRecording() {
        fragment_plot.updateText("结束采集")

        video_recorder?.let { recording ->
            recording.stop()
            video_recorder = null
            isRecording = false
            fabRecord.setImageResource(R.drawable.start_record)
        } ?: run {
            Log.w(TAG, "No active recording to stop")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}