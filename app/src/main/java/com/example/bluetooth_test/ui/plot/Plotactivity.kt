package com.example.bluetooth_test.ui.plot

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.example.bluetooth_test.SettingsActivity
import com.example.bluetooth_test.databinding.ActivityPlotactivityBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout

class Plotactivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlotactivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlotactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = binding.viewPager
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = binding.tabs
        tabs.setupWithViewPager(viewPager);
        val floatBtn: FloatingActionButton = binding.floatingActionButton
        floatBtn.setOnClickListener {
            intent = Intent(this, SettingsActivity::class.java)
//            intent= Intent(this@MainActivity,SettingsActivity::class.java)
            startActivity(intent)
        }
//        val sharedPreferences: SharedPreferences =
//            PreferenceManager.getDefaultSharedPreferences(this)
//        val myString = sharedPreferences.getString("Sensor8_0", "001")
//        if (myString != null) {
//            Log.e("TAG111", myString)
//        }

    }
}