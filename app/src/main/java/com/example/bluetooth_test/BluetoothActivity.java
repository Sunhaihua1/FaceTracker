package com.example.bluetooth_test;

import static com.example.bluetooth_test.ConnectThread.bluetoothSocket;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.bluetooth_test.ui.plot.Plotactivity;

import org.apache.poi.ss.formula.functions.T;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothActivity extends AppCompatActivity {

    public static UUID MY_UUID= UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//符合UUID格式就行。
    private String TAG = "test";

    private Button back=null;
    private Button next=null;
    private Button refresh =null;
    private ListView btList=null;
    Intent intent=null;
    //蓝牙操作
    private Handler handler = new Handler();
    private BluetoothAdapter bluetoothAdapter=null;
    private ArrayList<String> devicesNames=new ArrayList<>();
    private ArrayList<String> devicesNamestest=new ArrayList<>();

    private Set<BluetoothDevice> pairedDevices = null;
    private ArrayList<BluetoothDevice> readyDevices=new ArrayList<>();
    private ArrayAdapter<String> btNames=null;
    private SingBroadcastReceiver mReceiver = null;

    //自定义线程类的初始化
    static ConnectThread connectThread=null;
    static ConnectedThread connectedThread=null;






    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }

        requestPermission();
        initView();



        back=(Button) findViewById(R.id.back);
//        btList=(ListView) findViewById(R.id.btList);
        next=(Button)findViewById(R.id.next);
//        refresh = (Button)findViewById(R.id.refresh);
        Context context = this;

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //                    bluetoothSocket.close();
                finish();
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bluetoothSocket==null || !bluetoothSocket.isConnected()){//先判断连接上了
                    Toast.makeText(BluetoothActivity.this,"未建立连接",Toast.LENGTH_SHORT).show();
                }
                else {
                    if(bluetoothSocket!=null&&bluetoothSocket.isConnected()){//先判断连接上了
                        connectedThread=new ConnectedThread(bluetoothSocket,context);
                        connectedThread.start();
                        Toast.makeText(BluetoothActivity.this,"已开启数据线程",Toast.LENGTH_SHORT).show();
                    }

                    intent=new Intent(BluetoothActivity.this, Plotactivity.class);
                    startActivity(intent);

                }
            }
        });

        //列表项目点击事件，点击蓝牙设备名称，然后连接
        btList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //看看点击蓝牙设备是否可行
                //Toast.makeText(BluetoothActivity.this, "点击了"+readyDevices.get(position).getName(), Toast.LENGTH_SHORT).show();

                //做连接操作
                //先判断是否有连接，我们只要一个连接，在这个软件内只允许有一个连接
                if(connectThread!=null){//如果不为空，就断开这个连接
                    connectThread.cancel();
                    connectThread=null;
//                    next.setText("(未连接)查看状态");
                }
                //开始连接新的设备对象
                connectThread=new ConnectThread(readyDevices.get(position));
                connectThread.start();//start（）函数开启线程，执行操作
                Toast.makeText(BluetoothActivity.this, "正在连接"+readyDevices.get(position).getName(), Toast.LENGTH_SHORT).show();
//                next.setText("查看面部状态");
            }
        });

    }

    private void initView(){

        refresh = (Button)findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanBle();
            }
        });

        btNames = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,devicesNames);
        btList=(ListView) findViewById(R.id.btList);
        btList.setAdapter(btNames);
    }

//    BluetoothAdapter.LeScanCallback oldBtsc = new BluetoothAdapter.LeScanCallback() {
//        @Override
//        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
//            Log.d(TAG,"here is onLeScan");
//            if(device.getName() != null && !readyDevices.contains(device)){
//                readyDevices.add(device);
//                devicesNames.add(device.getName());
//                btNames.notifyDataSetChanged();
//                Log.d(TAG,"there are many devices!");
//            }
//        }
//    };

    @SuppressLint("MissingPermission")
    private void scanBle(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
            Intent enable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enable,1);
        }else {
            pairedDevices = bluetoothAdapter.getBondedDevices();
            if(!pairedDevices.isEmpty()){
                for(BluetoothDevice device:pairedDevices){
                    if(!readyDevices.contains(device) ){
                        readyDevices.add(device);
                        devicesNames.add(device.getName());
                        btNames.notifyDataSetChanged();
                    }
                }
            }


            mReceiver = new SingBroadcastReceiver();
            bluetoothAdapter.startDiscovery();
            IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver,intentFilter);

        }

//        bluetoothAdapter.startLeScan(oldBtsc);
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                Log.d(TAG,"here is run");
//                bluetoothAdapter.stopLeScan(oldBtsc);
//            }
//        },8000);

    }

    class SingBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action) ){
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a Toast
                if(!readyDevices.contains(device) && device.getName() != null){
                    readyDevices.add(device);
                    devicesNames.add(device.getName());
                    btNames.notifyDataSetChanged();
                }
            }
        }
    }


    //更精确的位置权限要求
    private void requestPermission() {
        //动态申请是否有必要看sdk版本哈
        if (Build.VERSION.SDK_INT < 23){return;}
        //判断是否有权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            //请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }
    }



}