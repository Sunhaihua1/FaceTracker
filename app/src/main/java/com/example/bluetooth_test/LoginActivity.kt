package com.example.bluetooth_test

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class LoginActivity : AppCompatActivity() {
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var register: Button
    private lateinit var login: Button
    //使用MySQLiteOpenHelper类中的方法
    private var mySQLiteOpenHelper: MySQLiteOpenHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        mySQLiteOpenHelper = MySQLiteOpenHelper(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        if (getSupportActionBar() != null){
            getSupportActionBar()?.hide();
        }
        username = findViewById<EditText>(R.id.lusername)
        password = findViewById<EditText>(R.id.lpassword)

        //初始化获取的布局元素
        register = findViewById(R.id.register);
        login = findViewById(R.id.login)
        login.setOnClickListener {
            login();
        }
        register.setOnClickListener {
            intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    fun login() {
        val n = username.text.toString()
        val p = password.text.toString()
        if (n == "") {
            Toast.makeText(this, "请输入账号", Toast.LENGTH_SHORT).show()
        } else if (p == "") {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
        } else {
            val login = mySQLiteOpenHelper!!.login(n, p)
            if (login) {
                Toast.makeText(this, "登录成功！", Toast.LENGTH_SHORT).show()
                val home = Intent(this@LoginActivity, MainActivity::class.java)
                val bundle = Bundle()
                bundle.putString("username", username.text.toString())
                home.putExtras(bundle)
                username.setText("")
                password.setText("")
                startActivity(home)
            } else {
                Toast.makeText(this, "账号或密码错误，请重新输入", Toast.LENGTH_SHORT).show()
                password.setText("")
            }
        }

    }
}