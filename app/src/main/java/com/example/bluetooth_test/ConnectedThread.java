package com.example.bluetooth_test;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.bluetooth_test.MainActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

//连接了蓝牙设备建立通信之后的数据交互线程类
public class ConnectedThread extends Thread{
    Queue<Byte> queueBuffer = new LinkedList();
    public static Queue<Sensor_data> data_sensor = new LinkedList();
    byte[] packBuffer = new byte[1024];
    int k = 0;
    public static float data = 0;
    BluetoothSocket bluetoothSocket=null;
    InputStream inputStream=null;//获取输入数据
    OutputStream outputStream=null;//获取输出数据
    public ConnectedThread(BluetoothSocket bluetoothSocket){
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
    }
    public void process_packet() {
//         Log.e("TAG", "INTO");
//
        if (k <= 0) return;
        byte tmp = packBuffer[k - 1];
//        Log.e("TAG", String.valueOf(tmp));

        byte valid = 0;
        for (int i = 0; i < k - 1; i ++) {
            valid += packBuffer[i];
//            Log.e("TAG", "i  +" + i+ " " + String.valueOf(packBuffer[i]));

        }
        Sensor_data sendor = new Sensor_data();
        if (valid == tmp) {
            for (int i = 0; i < k - 1; i +=2) {
                float num = (180.0F * ((short) packBuffer[i + 1] << 8 | 0xFF & (short) packBuffer[0]) / 32768.0F) ;
                if ((i % 3) == 0) {
                    sendor.x = num;
                }
                else if ((i % 3) == 1) {
                    sendor.y = num;
                }
                else {
                    sendor.z = num;
                }

//                Log.e("TAG", "i  +" + i+ " " + String.valueOf(num));
                data = num;
            }
            if (data_sensor.size() > 15) {
                data_sensor.poll();
            }
            data_sensor.add(sendor);


        }


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
