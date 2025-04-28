package com.example.bluetooth_test

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.annotation.Nullable

class MySQLiteOpenHelper(@Nullable context: Context?) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    companion object {
        private const val DB_NAME = "User.db"
        private const val DB_VERSION = 1

        // 创建用户表
        private const val CREATE_USER = "CREATE TABLE user(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username VARCHAR(30)," +
                "password VARCHAR(30))"

        // 创建传感器数据表
        private const val CREATE_SENSOR_DATA = "CREATE TABLE sensor_data(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username VARCHAR(30)," +
                "sensor_id INTEGER," +
                "x REAL," +
                "y REAL," +
                "z REAL," +
                "timestamp LONG)"

        @SuppressLint("StaticFieldLeak")
        private var currentUsername: String? = null
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_USER)
        db.execSQL(CREATE_SENSOR_DATA)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun register(user: User): Long {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put("username", user.username)
        cv.put("password", user.password)

        val cursor = db.query("user", null, "username=?", arrayOf(user.username), null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            cursor.close()
            return -1
        }
        cursor?.close()
        return db.insert("user", null, cv)
    }

    @SuppressLint("Range")
    fun login(username: String, password: String): Boolean {
        val db = writableDatabase
        val users = db.query("user", null, "username=?", arrayOf(username), null, null, null)
        var result = false

        if (users != null) {
            while (users.moveToNext()) {
                val usernameIndex = users.getColumnIndex("username")
                val passwordIndex = users.getColumnIndex("password")

                if (usernameIndex != -1 && passwordIndex != -1) {
                    val username1 = users.getString(usernameIndex)
                    val password1 = users.getString(passwordIndex)
                    result = password1 == password
                    if (result) {
                        currentUsername = username
                    }
                    users.close()
                    return result
                } else {
                    Log.e("MySQLiteOpenHelper", "Invalid column index")
                }
            }
            users.close()
        }
        return false
    }

    fun insertSensorData(sensorData: Sensor_data): Long {
        if (currentUsername == null) {
            throw IllegalStateException("User is not logged in")
        }

        val db = writableDatabase
        val cv = ContentValues()
        cv.put("username", currentUsername)
        cv.put("sensor_id", sensorData.sensorId)
        cv.put("x", sensorData.x)
        cv.put("y", sensorData.y)
        cv.put("z", sensorData.z)
        cv.put("timestamp", System.currentTimeMillis())

        val newRowId = db.insert("sensor_data", null, cv)
        if (newRowId == -1L) {
            Log.e("MySQLiteOpenHelper", "Error inserting new sensor data")
        } else {
            Log.i("MySQLiteOpenHelper", "Sensor data inserted with row ID: $newRowId")
        }
        return newRowId
    }

    val allSensorData: Cursor
        get() {
            val db = readableDatabase
            return db.query("sensor_data", null, null, null, null, null, null)
        }

    fun getSensorDataByUsername(username: String): Cursor {
        val db = readableDatabase
        return db.query("sensor_data", null, "username=?", arrayOf(username), null, null, null)
    }

    fun getSensorDataByCurrentUser(): Cursor {
        val db = readableDatabase
        return db.query("sensor_data", null, "username=?", arrayOf(currentUsername), null, null, null)
    }
    /**
     * 获取指定时间范围内的传感器数据
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     */
    fun getSensorDataByTimeRange(startTime: Long, endTime: Long): Cursor {
        if (currentUsername == null) {
            throw IllegalStateException("User is not logged in")
        }

        val db = readableDatabase
        return db.query(
            "sensor_data",
            null,
            "timestamp BETWEEN ? AND ? AND username = ?",
            arrayOf(startTime.toString(), endTime.toString(), currentUsername),
            null,
            null,
            "timestamp ASC"
        )
    }


    /**
     * 获取最新的N条传感器数据
     * @param limit 要获取的记录数
     */
    fun getLatestSensorData(limit: Int): Cursor {
        if (currentUsername == null) {
            throw IllegalStateException("User is not logged in")
        }

        val db = readableDatabase
        return db.query(
            "sensor_data",
            null,
            "username = ?",
            arrayOf(currentUsername),
            null,
            null,
            "timestamp DESC",
            limit.toString()
        )
    }

    fun getCurrentUsername(): String? {
        return currentUsername
    }

    fun logout() {
        currentUsername = null
    }
}
