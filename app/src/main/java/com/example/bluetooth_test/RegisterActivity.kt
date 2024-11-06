package com.example.bluetooth_test

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        mySQLiteOpenHelper =
            MySQLiteOpenHelper(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        username = findViewById<EditText>(R.id.rusername)
        password = findViewById<EditText>(R.id.rpassword)

        //初始化获取的布局元素
        register = findViewById(R.id.mregister);
        register.setOnClickListener {
            register()
        }
    }

    //获取注册按钮，预先定义，下方输入框同理
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var register: Button
    //使用MySQLiteOpenHelper类中的方法
    private var mySQLiteOpenHelper: MySQLiteOpenHelper? = null

    //控制注册按钮点击完后不给点击了
    var flag = false

    //跳转回登录页面
    fun gobak(view: View?) {
//        val gl = Intent(this@RegisterActivity, LoginActivity::class.java)
//        startActivity(gl)
        finish();
    }

    //注册逻辑实现
    fun register() {
        val ru = username!!.text.toString()
        val rps = password!!.text.toString()
        val user: com.example.bluetooth_test.User = User(ru, rps)
        if (ru == "") {
            Toast.makeText(this, "账号不能为空！", Toast.LENGTH_SHORT).show()
        } else if (rps == "") {
            Toast.makeText(this, "密码不能为空！", Toast.LENGTH_SHORT).show()
        }
        else{
            val r1 = mySQLiteOpenHelper?.register(user);
            if(r1?.toInt() !=-1){
                register.setEnabled(false);
                Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, "注册失败,用户名已存在！", Toast.LENGTH_SHORT).show();
            }
        }
    }

    override fun onPointerCaptureChanged(hasCapture: Boolean) {
        super.onPointerCaptureChanged(hasCapture)
    }
}