package com.example.bluetooth_test


import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.bluetooth_test.databinding.ActivityMainBinding
import com.tenginekit.AndroidConfig
import com.tenginekit.Face
import com.tenginekit.KitCore
import com.tenginekit.model.FaceDetectInfo
import com.tenginekit.model.FaceLandmarkInfo
import com.tenginekit.model.TenginekitPoint
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// Permission code from:
// https://stackoverflow.com/questions/67722950/android-12-new-bluetooth-permissions

class MainActivity : AppCompatActivity() {

    private val _tag = MainActivity::class.qualifiedName
    private lateinit var binding: ActivityMainBinding
    private lateinit var currentPhotoPath: String
    private val rectPaint = Paint()
    private val circlePaint = Paint()
    private val textPaint = Paint()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        Log.d(_tag, "Starting...")
        if (getSupportActionBar() != null){
            getSupportActionBar()?.hide()
        }
        rectPaint.color = Color.RED
        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeWidth = 2.0f

        circlePaint.isAntiAlias = true
        circlePaint.color = Color.GREEN
        circlePaint.strokeWidth = 5.0.toFloat()
        circlePaint.style = Paint.Style.STROKE
        circlePaint.textSize = 50F
        circlePaint.isAntiAlias = true

        textPaint.color = Color.BLUE
        textPaint.strokeWidth = 30.0.toFloat()
        textPaint.style = Paint.Style.FILL_AND_STROKE
        textPaint.textSize = 100F

        setContentView(binding.root)
        binding.btnConnect.setOnClickListener {
            intent = Intent(this@MainActivity, BluetoothActivity::class.java)
//            intent= Intent(this@MainActivity,SettingsActivity::class.java)
            startActivity(intent)
        }
        binding.takePhotoButton.setOnClickListener {
            takePhoto()
        }
    }
    private fun takePhoto() {



        val takePictureIntent = Intent("android.media.action.IMAGE_CAPTURE")


        if(!this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY))
//        if(takePictureIntent.resolveActivity(packageManager) == null)  //华为手机未能识别到相机
        {
            Toast.makeText(this,"open camera failed",Toast.LENGTH_SHORT).show()
        }else{
            // 创建保存照片的文件
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                Toast.makeText(this, "Error occurred while creating the file", Toast.LENGTH_SHORT).show()
                null
            }
            // 继续只有当文件成功创建时
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    "com.example.bluetooth_test.fileprovider",
                    it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                takePhotoLauncher.launch(takePictureIntent)
            }
        }






    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // 创建一个唯一的文件名
        //val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val existingFile = File(storageDir,"OUT_JPEG.jpg")

        if(existingFile.exists()){
            existingFile.delete()
        }
        return File.createTempFile(
//            "JPEG_${timeStamp}_",
            "Face",
            ".jpg",
            storageDir
        ).apply {
            // 保存文件路径以便在显示照片时使用
            currentPhotoPath = absolutePath
            Toast.makeText(this@MainActivity,"image is stored in : $currentPhotoPath",Toast.LENGTH_LONG).show()
        }
    }

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            setPic()
        } else {
            Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setPic() {
        // 获取 ImageView 的目标尺寸
        val targetW: Int = binding.imageView.width
        val targetH: Int = binding.imageView.height

        // 获取保存的图像的尺寸
        val bmOptions = BitmapFactory.Options().apply {
            // 读取图片尺寸
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(currentPhotoPath, this)
            val photoW: Int = outWidth
            val photoH: Int = outHeight
            // 确定缩放比例
            val scaleFactor: Int = Math.max(1, Math.min(photoW / targetW, photoH / targetH))
            // 解析图片时，设置缩放比例
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
        }

        // 解码文件为 Bitmap 并设置到 ImageView
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions)?.also { bitmap ->
            run {
                val out_bitmap = Bitmap.createBitmap(
                    bitmap.width,
                    bitmap.height,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(out_bitmap)

                KitCore.init(
                    this,
                    AndroidConfig
                        .create()
                        .setNormalMode()
                        .setDefaultFunc()
                        .setInputImageFormat(AndroidConfig.ImageFormat.RGBA)
                        .setInputImageSize(bitmap.width, bitmap.height)
                        .setOutputImageSize(bitmap.width as Int, bitmap.height as Int)
                )
                val data: ByteArray = bitmap2Bytes(bitmap)
                val faceDetect: Face.FaceDetect = Face.detect(data)
                var faceDetectInfos: List<FaceDetectInfo> = ArrayList()
                var landmarkInfos: List<FaceLandmarkInfo> = ArrayList()
                if (faceDetect.getFaceCount() > 0) {
                    faceDetectInfos = faceDetect.getDetectInfos()
                    landmarkInfos = faceDetect.landmark2d()
                }
                canvas.drawBitmap(bitmap, 0.0f, 0.0f, null)

                Log.d("#####", "Face Num: " + faceDetectInfos!!.size)
                Log.d("#####", "Face size  " + bitmap.width + " " +  bitmap.height.toString())

                if (faceDetectInfos != null && faceDetectInfos.size > 0) {
                    val face_landmarks: List<List<TenginekitPoint>> = ArrayList()
                    for (i in faceDetectInfos.indices) {
                        var rect: Rect? = Rect()
                        rect = faceDetectInfos[i].asRect()
                        canvas.drawRect(rect, rectPaint)
                        Log.d("#####", "Point size  " + landmarkInfos[i].landmarks.size)
                        canvas.drawText("1",landmarkInfos[i].landmarks[73].X,landmarkInfos[i].landmarks[73].Y,textPaint)
                        canvas.drawText("2",landmarkInfos[i].landmarks[89].X,landmarkInfos[i].landmarks[89].Y,textPaint)
                        canvas.drawText("3",(landmarkInfos[i].landmarks[36].X + landmarkInfos[i].landmarks[138].X) / 2,(landmarkInfos[i].landmarks[36].Y + landmarkInfos[i].landmarks[138].Y) / 2,textPaint)
                        canvas.drawText("4",(landmarkInfos[i].landmarks[52].X + landmarkInfos[i].landmarks[154].X) / 2,(landmarkInfos[i].landmarks[52].Y + landmarkInfos[i].landmarks[154].Y) / 2,textPaint)
                        canvas.drawText("5",landmarkInfos[i].landmarks[181].X,landmarkInfos[i].landmarks[181].Y,textPaint)
                        canvas.drawText("6",landmarkInfos[i].landmarks[189].X,landmarkInfos[i].landmarks[189].Y,textPaint)
                        canvas.drawText("7",(landmarkInfos[i].landmarks[58].X + landmarkInfos[i].landmarks[199].X) / 2,(landmarkInfos[i].landmarks[58].Y + landmarkInfos[i].landmarks[199].Y) / 2,textPaint)
                        canvas.drawText("8",(landmarkInfos[i].landmarks[41].X + landmarkInfos[i].landmarks[203].X) / 2,(landmarkInfos[i].landmarks[41].Y + landmarkInfos[i].landmarks[203].Y) / 2,textPaint)

                        for (j in landmarkInfos[i].landmarks.indices) {
                            val x = landmarkInfos[i].landmarks[j].X
                            val y = landmarkInfos[i].landmarks[j].Y
                            Log.d("#####", "Face point " + x + " " +  y.toString())
                            canvas.drawCircle(x, y, 2F, circlePaint)
                        }
                    }
                }
                binding.imageView.setImageBitmap(out_bitmap)
                KitCore.release()
            }
        }
    }

    private fun bitmap2Bytes(image: Bitmap): ByteArray {
        // calculate how many bytes our image consists of
        val bytes = image.byteCount
        val buffer = ByteBuffer.allocate(bytes) // Create a new buffer
        image.copyPixelsToBuffer(buffer) // Move the byte data to the buffer
        return buffer.array()
    }

}