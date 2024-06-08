package com.example.bluetooth_test;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;
public class MySQLiteOpenHelper extends SQLiteOpenHelper {
    //数据库名字
    private static final String DB_NAME = "User.db";
    //创建用户表
    private static final String CREATE_USER = "create table user(id integer primary key autoincrement," +
            "username varchar(30)," +
            "password varchar(30))";

    //运行程序时，Android Studio帮你创建数据库，只会执行一次
    public MySQLiteOpenHelper(@Nullable Context context) {
        super(context,DB_NAME,null,1);
    }
    //创建数据表
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_USER);
    }
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
    }
    public long register(User u){
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("username ",u.getUsername());
        cv.put("password",u.getPassword());
        if (db.query("user",null, "username like?", new String[]{u.username}, null, null, null) != null) {
            return -1;
        }
        return db.insert("user",null,cv);
    }
    public boolean login(String username,String password){
        SQLiteDatabase db = getWritableDatabase();
        boolean result = false;
        Cursor users = db.query("user", null, "username like?", new String[]{username}, null, null, null);
        if(users!=null){
            while (users.moveToNext()){
                @SuppressLint("Range") String username1 = users.getString(users.getColumnIndex("username"));
                Log.i("users", "login: "+username1);
                String password1 = users.getString(2);
                Log.i("users", "login: "+password1);
                result = password1.equals(password);
                return result;
            }
        }
        return false;
    }

}