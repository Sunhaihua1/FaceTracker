package com.example.bluetooth_test


import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.bluetooth_test.ui.main.SettingsActivity


// Permission code from:
// https://stackoverflow.com/questions/67722950/android-12-new-bluetooth-permissions

class MainActivity : AppCompatActivity() {

    private val _tag = MainActivity::class.qualifiedName

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(_tag, "Starting...")
        if (getSupportActionBar() != null){
            getSupportActionBar()?.hide();
        }
        setContentView(R.layout.activity_main)

        val connectButton: Button = findViewById(R.id.btnConnect)
        connectButton.setOnClickListener {
            intent = Intent(this@MainActivity, BluetoothActivity::class.java)
//            intent= Intent(this@MainActivity,SettingsActivity::class.java)
            startActivity(intent)
        }

    }
}