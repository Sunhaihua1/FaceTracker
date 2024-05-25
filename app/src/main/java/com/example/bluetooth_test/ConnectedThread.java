package com.example.bluetooth_test;

import static java.util.Collections.max;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
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
    public static int[][] sensor_state_cnt = new int[9][4]; //传感器状态存储 [传感器编号][正常/轻度/中度/重度]
    public static int[] cur_state = new int[9]; //传感器现有状态 0 1 2 3 -->>>>>>> 正常/轻度/中度/重度
    boolean[] get_var_flag = new boolean[9]; //9个传感器大于10个再求方差
    BluetoothSocket bluetoothSocket=null;
    InputStream inputStream=null;//获取输入数据
    OutputStream outputStream=null;//获取输出数据
    SharedPreferences sharedPreferences;
    public ConnectedThread(BluetoothSocket bluetoothSocket, Context context){
        this.bluetoothSocket=bluetoothSocket;
        //先新建暂时的Stream
        InputStream inputTemp=null;
        OutputStream outputTemp=null;
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
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

    }
    public void process_packet() {
//
        if (k <= 0) return;
        byte tmp = packBuffer[k - 1];
//        Log.e("TAG", Arrays.toString(packBuffer));

        byte valid = 0;
        for (int i = 0; i < k - 1; i ++) {
            valid += packBuffer[i];
//            Log.e("TAG", "i  +" + i+ " " + String.valueOf(packBuffer[i]));

        }
        if (valid == tmp) {
            int state = 0;
            float x =0, y= 0, z = 0;
            for (int i = 0,j =0; i < k - 1; i +=2,j++) {
                short t_high = (short) (packBuffer[i + 1] & 0xFF);
                t_high = (short) (t_high << 8);
                short t_short = (short) (packBuffer[i] & 0xFF);
                int tt = t_high | t_short;
                float num = (180.0F * tt) / 32768.0F;
                if ((j % 3) == 0) {
                    x = num;
                }
                else if ((j % 3) == 1) {
                    y = num;
                }
                else {

                    z = num;
                    Sensor_data sendor = new Sensor_data(x,y,z);
                    if (data_sensor.get(state).size()>15) {
                        data_sensor.get(state).poll();
                    }
                    data_sensor.get(state).add(sendor);
                    sensor_var[state][var_idx[state]] = sendor;
                    var_idx[state] ++;
                    if (var_idx[state] >= 10) {
                        get_var_flag[state] = true;
                        var_idx[state] = 0;
                    }
                    if (get_var_flag[state]) {
                        get_var(state);
                    }
//                    Log.e("TAG", "process_packet: " + state);
                    state++;
                }
//                Log.e("TAG", "i  +" + i+ " " + String.valueOf(num));

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
            float threshold = Float.parseFloat(sharedPreferences.getString(Sensor_title+i, "001"));
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
//            if(!Arrays.toString(MainActivity.wheelData).equals(Arrays.toString(lastData))){//数组是否相等的判断！！！
//                btWriteInt(MainActivity.wheelData);
//            }
//            lastData=MainActivity.wheelData;//做完一次发送重新给lastData赋值
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
