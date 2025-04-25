package com.example.bluetooth_test.ui.plot

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.example.bluetooth_test.MySQLiteOpenHelper
import com.example.bluetooth_test.SettingsActivity
import com.example.bluetooth_test.databinding.ActivityPlotactivityBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import android.Manifest;
import android.annotation.SuppressLint
import android.content.pm.PackageManager;
import android.database.Cursor
import android.net.Uri
import android.os.Environment;
import android.os.Looper
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.bluetooth_test.databinding.ActivityMainBinding;
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Plotactivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlotactivityBinding
    private var mySQLiteOpenHelper: MySQLiteOpenHelper? = null
    private lateinit var thread: Thread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlotactivityBinding.inflate(layoutInflater)
        mySQLiteOpenHelper = MySQLiteOpenHelper(this)

        setContentView(binding.root)
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = binding.viewPager
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = binding.tabs
        tabs.setupWithViewPager(viewPager);
        val floatBtn: FloatingActionButton = binding.floatingActionButton
        floatBtn.setOnClickListener {
            intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        binding.saveButton.setOnClickListener {
            requestSaveLocation();
        }
    }
    private fun requestSaveLocation() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_TITLE, "SensorData.xlsx")
        }
        saveFileLauncher.launch(intent)
    }
    private val saveFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                thread = Thread {
                    this.runOnUiThread {
                        Toast.makeText(this@Plotactivity,"正在保存中，请耐心等待",Toast.LENGTH_LONG).show()
                    }

                    saveSensorDataToExcel(uri)
                    this.runOnUiThread {
                        Toast.makeText(this@Plotactivity,"保存成功",Toast.LENGTH_LONG).show()
                    }

                }
                thread.start()

            }
        }
    }
    @SuppressLint("Range")
    private fun saveSensorDataToExcel(uri: Uri) {
        val cursor: Cursor? = mySQLiteOpenHelper?.getSensorDataByCurrentUser()
        cursor?.let {
            val workbook: Workbook = XSSFWorkbook()
            val sheet: Sheet = workbook.createSheet("Sensor Data")

            // 创建标题行
            val headerRow: Row = sheet.createRow(0)
            headerRow.createCell(0).setCellValue("序号")
            headerRow.createCell(1).setCellValue("用户名")
            headerRow.createCell(2).setCellValue("传感器编号")
            headerRow.createCell(3).setCellValue("X")
            headerRow.createCell(4).setCellValue("Y")
            headerRow.createCell(5).setCellValue("Z")
            headerRow.createCell(6).setCellValue("时间")

            // 填充数据行
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            var rowIndex = 1
            while (cursor.moveToNext()) {
                val row: Row = sheet.createRow(rowIndex++)
                row.createCell(0).setCellValue(cursor.getInt(cursor.getColumnIndex("id")).toDouble())
                row.createCell(1).setCellValue(cursor.getString(cursor.getColumnIndex("username")))
                row.createCell(2).setCellValue(cursor.getInt(cursor.getColumnIndex("sensor_id")).toDouble())
                row.createCell(3).setCellValue(cursor.getFloat(cursor.getColumnIndex("x")).toDouble())
                row.createCell(4).setCellValue(cursor.getFloat(cursor.getColumnIndex("y")).toDouble())
                row.createCell(5).setCellValue(cursor.getFloat(cursor.getColumnIndex("z")).toDouble())
                val timestamp = cursor.getLong(cursor.getColumnIndex("timestamp"))
                val date = Date(timestamp)
                val formattedDate = dateFormat.format(date)
                row.createCell(6).setCellValue(formattedDate)
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