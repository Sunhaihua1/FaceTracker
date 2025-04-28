package com.example.bluetooth_test.ui.video
import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.Image
import android.net.Uri
import android.opengl.Matrix
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceProcessor
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
import com.example.bluetooth_test.ConnectedThread
import com.example.bluetooth_test.MySQLiteOpenHelper
import com.example.bluetooth_test.R
import com.example.bluetooth_test.databinding.ActivityVideoRecordBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.common.util.concurrent.ListenableFuture
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Video_record : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var fabRecord: FloatingActionButton
    private lateinit var fabSave: FloatingActionButton
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var binding: ActivityVideoRecordBinding
    private var mySQLiteOpenHelper: MySQLiteOpenHelper? = null

    // Quality selector for video recording
    private lateinit var qualitySelector: QualitySelector
    // 获取已添加的 Fragment 实例
    private lateinit var fragment_plot: Chart_plot
    // 添加两个时间戳变量来记录开始和结束时间
    private var recordingStartTime: Long = 0
    private var recordingEndTime: Long = 0

    private var videoCapture: VideoCapture<Recorder>? = null
    private var isRecording = false
    private var video_recorder: Recording? = null
    private var connected_thread = ConnectedThread.getInstance();


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
        mySQLiteOpenHelper = MySQLiteOpenHelper(this)

        previewView = binding.previewView
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        fabRecord = binding.fabRecord
        fabSave = binding.fabSave
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
        fabSave.setOnClickListener{
            requestSaveLocation();
        }

    }
    //保存传感器数据
    private fun requestSaveLocation() {
        // 确保有有效的开始和结束时间
        if (recordingEndTime <= recordingStartTime) {
            runOnUiThread {
                Toast.makeText(this, "请先录制视频再保存数据", Toast.LENGTH_SHORT).show()
            }
            return
        }
        // Create timestamped name and MediaStore entry
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(recordingStartTime)

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_TITLE, "$name.xlsx")
        }
        saveFileLauncher.launch(intent)
    }
    private val saveFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                Thread {
                    this.runOnUiThread {
                        Toast.makeText(this@Video_record,"正在保存中，请耐心等待",Toast.LENGTH_LONG).show()
                    }
                    saveSensorDataToExcel(uri)
                    this.runOnUiThread {
                        Toast.makeText(this@Video_record,"保存成功",Toast.LENGTH_LONG).show()
                    }

                }.start()

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

        // 创建时间戳视图
        val timeStampView = TextView(this).apply {
            setTextColor(Color.RED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(16, 16, 16, 16)
            gravity = Gravity.BOTTOM or Gravity.END
        }
        // 添加到预览视图
        (previewView.parent as? ViewGroup)?.addView(timeStampView)

        // 更新时间戳的Runnable
        val updateTimeRunnable = object : Runnable {
            override fun run() {
                val timeText = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
                    .format(Date())
                timeStampView.text = timeText
                previewView.postDelayed(this, 50) // 每50ms更新一次
            }
        }
        previewView.post(updateTimeRunnable)

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
        connected_thread.setShouldStoreToDB(true)
        val videoCapture = this.videoCapture ?: return
        recordingStartTime = System.currentTimeMillis() // 记录开始时间

        // Create timestamped name and MediaStore entry
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(recordingStartTime)
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }
        // 创建时间戳效果



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
        connected_thread.setShouldStoreToDB(false)
        video_recorder?.let { recording ->
            recording.stop()
            video_recorder = null
            isRecording = false
            fabRecord.setImageResource(R.drawable.start_record)
        } ?: run {
            Log.w(TAG, "No active recording to stop")
        }

        recordingEndTime = System.currentTimeMillis() // 记录结束时间

    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    @SuppressLint("Range")
    private fun saveSensorDataToExcel(uri: Uri) {
        // 只查询录制时间段内的数据
        val cursor: Cursor? = mySQLiteOpenHelper?.getSensorDataByTimeRange(
            recordingStartTime,
            recordingEndTime
        )
        cursor?.let {
            val workbook: Workbook = XSSFWorkbook()
            val sheet: Sheet = workbook.createSheet("Sensor Data")

            // 创建标题行
            val headerRow: Row = sheet.createRow(0)
            headerRow.createCell(1).setCellValue("用户名")
            headerRow.createCell(2).setCellValue("传感器编号")
            headerRow.createCell(3).setCellValue("AccX")
            headerRow.createCell(4).setCellValue("AccY")
            headerRow.createCell(5).setCellValue("AccZ")
            headerRow.createCell(6).setCellValue("时间戳(毫秒)")
            headerRow.createCell(7).setCellValue("格式化时间")

            var rowIndex = 1
            while (cursor.moveToNext()) {
                val row: Row = sheet.createRow(rowIndex++)
//                row.createCell(0).setCellValue(cursor.getInt(cursor.getColumnIndex("id")).toDouble())
                row.createCell(1).setCellValue(cursor.getString(cursor.getColumnIndex("username")))
                row.createCell(2).setCellValue(cursor.getInt(cursor.getColumnIndex("sensor_id")).toDouble())
                row.createCell(3).setCellValue(cursor.getFloat(cursor.getColumnIndex("x")).toDouble())
                row.createCell(4).setCellValue(cursor.getFloat(cursor.getColumnIndex("y")).toDouble())
                row.createCell(5).setCellValue(cursor.getFloat(cursor.getColumnIndex("z")).toDouble())

                val timestamp = cursor.getLong(cursor.getColumnIndex("timestamp"))
                row.createCell(6).setCellValue(timestamp.toDouble())
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                row.createCell(7).setCellValue(dateFormat.format(Date(timestamp)))
            }
            cursor.close()

            // 保存文件到用户选择的位置
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    workbook.write(outputStream)
                    workbook.close()

                }
            } catch (e: IOException) {
                Log.e("MainActivity", "Error writing Excel file", e)

            }
        }
    }
}
