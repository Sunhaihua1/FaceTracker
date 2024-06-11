package com.example.bluetooth_test


import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.bluetooth_test.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// Permission code from:
// https://stackoverflow.com/questions/67722950/android-12-new-bluetooth-permissions

class MainActivity : AppCompatActivity() {

    private val _tag = MainActivity::class.qualifiedName
    private lateinit var binding: ActivityMainBinding
    private lateinit var currentPhotoPath: String

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        Log.d(_tag, "Starting...")
        if (getSupportActionBar() != null){
            getSupportActionBar()?.hide()
        }

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
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // 确保有相机应用程序可以处理意图
        takePictureIntent.resolveActivity(packageManager)?.also {
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
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            // 保存文件路径以便在显示照片时使用
            currentPhotoPath = absolutePath
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
            binding.imageView.setImageBitmap(bitmap)
        }
    }

    companion object {
        private const val REQUEST_WRITE_STORAGE = 112
    }

}