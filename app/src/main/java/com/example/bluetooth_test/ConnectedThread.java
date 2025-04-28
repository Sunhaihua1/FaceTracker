package com.example.bluetooth_test;

import static java.util.Collections.max;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

//连接了蓝牙设备建立通信之后的数据交互线程类
public class ConnectedThread extends Thread{
    Queue<Byte> queueBuffer = new LinkedList<>();
    private MySQLiteOpenHelper mySQLiteOpenHelper;
    //数据库存储开关
    private volatile boolean shouldStoreToDB = false;

    public static ArrayList<Queue<Sensor_data>> data_sensor;
    static {
        // 初始化ArrayList，初始容量为3（这仅仅是内部数组的大小，并不是实际元素数量）
        data_sensor = new ArrayList<>(9);

        // 实际添加三个Queue到ArrayList中
        for (int i = 0; i < 9; i++) {
            data_sensor.add(new LinkedList<Sensor_data>());
        }
    }
    byte[] packBuffer = new byte[4096];
    int k = 0;
//    public static float data = 0;
    Sensor_data[][] sensor_var = new Sensor_data[9][10]; short[] var_idx = new short[9];//求方差数组 对应[传感器编号][xyz轴][长度]
    public int[][] sensor_state_cnt = new int[9][4]; //传感器状态存储 [传感器编号][正常/轻度/中度/重度]
    public int[] cur_state = new int[9]; //传感器现有状态 0 1 2 3 -->>>>>>> 正常/轻度/中度/重度
    boolean[] get_var_flag = new boolean[9]; //9个传感器大于10个再求方差
    BluetoothSocket bluetoothSocket=null;
    InputStream inputStream=null;//获取输入数据
    OutputStream outputStream=null;//获取输出数据
    SharedPreferences sharedPreferences;
    private Context appContext; // 使用 Application Context
    private static volatile ConnectedThread instance;
    // 提供一个公共方法供外部控制
    public void setShouldStoreToDB(boolean enable) {
        this.shouldStoreToDB = enable;
    }

    public ConnectedThread(BluetoothSocket bluetoothSocket, Context context){
        this.bluetoothSocket=bluetoothSocket;
        //先新建暂时的Stream
        InputStream inputTemp=null;
        OutputStream outputTemp=null;
        this.appContext = context.getApplicationContext(); // 使用 Application Context

        try {
            inputTemp=this.bluetoothSocket.getInputStream();
            outputTemp=this.bluetoothSocket.getOutputStream();
        } catch (IOException e) {
            try {
                bluetoothSocket.close();//出错就关闭线程吧
            } catch (IOException ex) {}
        }
        inputStream=inputTemp;
        outputStream=outputTemp;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        mySQLiteOpenHelper = new MySQLiteOpenHelper(appContext);
    }
    // 获取单例（双重校验锁，线程安全）
    // 初始化单例（必须调用一次）
    public static synchronized void initialize(BluetoothSocket socket, Context context) {
        if (instance == null) {
            instance = new ConnectedThread(socket, context);
        } else {
            // 可选：如果已存在实例，更新Socket（根据需求决定）
            instance.updateSocket(socket);
        }
    }
    // 获取单例（前提是已经初始化）
    public static ConnectedThread getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ConnectedThread must be initialized first!");
        }
        return instance;
    }
    // 更新Socket（如果需要）
    private void updateSocket(BluetoothSocket newSocket) {
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
            bluetoothSocket = newSocket;
            // 重新初始化流
            inputStream = newSocket.getInputStream();
            outputStream = newSocket.getOutputStream();
        } catch (IOException e) {
            Log.e("ConnectedThread", "Failed to update socket", e);
        }
    }

    public void process_packet() {
//
        if (k <= 0) return;
        byte tmp = packBuffer[k - 1];
        byte valid = 0;
        for (int i = 0; i < k - 1; i ++) {
            valid += packBuffer[i];
        }
        if (valid == tmp) {
            int state = 0;
            float x =0, y= 0, z = 0;
            for (int i = 0,j =0; i < k - 1; i +=2,j++) {
                short t_high = (short) (packBuffer[i + 1] & 0xFF);
                t_high = (short) (t_high << 8);
                short t_short = (short) (packBuffer[i] & 0xFF);
                int tt = t_high | t_short;
                float num = (156.8F * tt) / 32768.0F;
                if ((j % 3) == 0) {
                    x = num;
                }
                else if ((j % 3) == 1) {
                    y = num;
                }
                else {
                    z = num;
                    Sensor_data sensor = new Sensor_data(state,x,y,z);
                    if (data_sensor.get(state).size()>15) {
                        data_sensor.get(state).poll();
                    }
                    data_sensor.get(state).add(sensor);
                    // 只在开关开启时存储到数据库
                    if (shouldStoreToDB) {
                        long sensorDataId = mySQLiteOpenHelper.insertSensorData(sensor);
                        Log.i("ConnectedThread", "Sensor data inserted with ID: " + sensorDataId);
                    }
                    sensor_var[state][var_idx[state]] = sensor;
                    var_idx[state] ++;
                    if (var_idx[state] >= 10) {
                        get_var_flag[state] = true;
                        var_idx[state] = 0;
                    }
                    if (get_var_flag[state]) {
                        get_var(state);
                    }
                    state++;
                }

            }
            // 0 1 2 3 4 5 6 7 8 9
        }


    }

    private void get_var(int state) {
        float[] xValues = new float[10];
        float[] yValues = new float[10];
        float[] zValues = new float[10];

        // 获取第state个传感器的数据
        for (int i = 0; i < 10; i++) {
            xValues[i] = sensor_var[state][i].x;
            yValues[i] = sensor_var[state][i].y;
            zValues[i] = sensor_var[state][i].z;
        }

        // 计算均值
        float xMean = calculateMean(xValues);
        float yMean = calculateMean(yValues);
        float zMean = calculateMean(zValues);

        // 计算方差
        float xVariance = calculateVariance(xValues, xMean);
        float yVariance = calculateVariance(yValues, yMean);
        float zVariance = calculateVariance(zValues, zMean);
        float maxVariance = Math.max(xVariance, Math.max(yVariance, zVariance));
        Log.e("TAG", "get_var_x: "+xVariance);
        Log.e("TAG", "get_var_y: "+yVariance);
        Log.e("TAG", "get_var_z: "+zVariance);
        String Sensor_title= "Sensor"+(state+1) + "_";

        for (int i = 0; i < 4; i ++) {
            String tmp = sharedPreferences.getString(Sensor_title+i, "001");
            float threshold;
            if (tmp.equals("MAX")) {
                threshold = 2e9F;
            }
            else threshold = Float.parseFloat(tmp);
            Log.e("TAG111", String.valueOf(threshold));
            if (maxVariance <= threshold) {
                sensor_state_cnt[state][i] ++;
                cur_state[state] = i;
                Log.e("TAG", String.valueOf(sensor_state_cnt[state][i]));
                Log.e("TAG", "cur_state" + i);

                break;
            }
        }



    }

    private float calculateMean(float[] values) {
        float sum = 0;
        for (float value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    private float calculateVariance(float[] values, float mean) {
        float sum = 0;
        for (float value : values) {
            sum += Math.pow(value - mean, 2);
        }
        return sum / values.length;
    }

    @Override
    public void run() {
        super.run();
        byte[] arrayOfByte = new byte[1024];

        while(true){
            //发送数据
            try {
                int i = inputStream.read(arrayOfByte);
//                Log.e("TAG", String.valueOf(i));
                for (int j = 0; j < i; j ++) {
                    queueBuffer.add(arrayOfByte[j]);
                }
                while (!queueBuffer.isEmpty()) {
                    byte b = queueBuffer.poll().byteValue();
                    if (b == (byte) 0xA5) {
                        k = 0;
                    }
                    else if (b == (byte) 0x5A) {
                        process_packet();
                    }
                    else {
                        packBuffer[k ++] = b;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void btWriteInt(int[] intData){
        for(int sendInt:intData){
            try {
                outputStream.write(sendInt);
            } catch (IOException e) {}
        }
    }

    //自定义的发送字符串的函数
    public void btWriteString(String string){
        for(byte sendData:string.getBytes()){
            try {
                outputStream.write(sendData);//outputStream发送字节的函数
            } catch (IOException e) {}
        }
    }

    //自定义的关闭Socket线程的函数
    public void cancel(){
        try {
            bluetoothSocket.close();
        } catch (IOException e) {}
    }
}
