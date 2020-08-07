package com.example.coupon_advertiser;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static com.example.coupon_advertiser.Service_Adv.byte2HexStr;

public class MainActivity extends AppCompatActivity {

    static String TAG = "chien";

    static ImageButton startAdvButton,stopAdvButton;

    EditText ettitle,etdate,etcoupon;

    public static String title,date,coupon,total_data;

    int year,mon,day;

    static byte[] date_ = new byte[4];


    static byte[] id_byte = new byte[4];

    Intent adv_service;

    static AdvertiseCallback mAdvertiseCallback;
    static BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    static BluetoothAdapter mBluetoothAdapter;
    static Map<Integer, AdvertiseCallback> AdvertiseCallbacks_map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();
        element();
    }

    private void initialize() {
        if (mBluetoothLeAdvertiser == null) {
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
                if (bluetoothAdapter != null) {
                    mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
                }
            }
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void element() {


        ettitle = findViewById(R.id.ettitle);
        etdate = findViewById(R.id.etdate);
        etcoupon = findViewById(R.id.etcoupon);

        new Random().nextBytes(id_byte);


        final SharedPreferences SP = getApplicationContext().getSharedPreferences("data",0);
        ettitle.setText(SP.getString("TITLE",null));
        etdate.setText(SP.getString("DATE",null));
        etcoupon.setText(SP.getString("COUPON",null));



        ettitle.setEnabled(true);
        etdate.setEnabled(true);
        etcoupon.setEnabled(true);


        startAdvButton = findViewById(R.id.StartAdvButton);
        startAdvButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SharedPrefesSAVE(1,ettitle.getText().toString());
                SharedPrefesSAVE(2,etdate.getText().toString());
                SharedPrefesSAVE(3,etcoupon.getText().toString());

                title = ettitle.getText().toString();
                date = etdate.getText().toString();
                year = Integer.parseInt(date.substring(0,4));
                mon = Integer.parseInt(date.substring(4,6));
                day = Integer.parseInt(date.substring(6,8));
                date_ = byteMerger(bigIntToByteArray(year) , bigIntToByteArray(mon)) ;
                date_ = byteMerger(date_ , bigIntToByteArray(day));
                date_ = byteMerger( bigIntToByteArray(0),date_);

                Log.e(TAG,"date: " + byte2HexStr(date_));

                coupon = etcoupon.getText().toString();
                if(title.length() + date.length() + coupon.length() != 0){
                    total_data = title + ":" + coupon+":";
                    Log.e(TAG,"total_data: " + total_data);
                    startService(adv_service);
                    Toast.makeText(MainActivity.this,"Start advertising coupon!",Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(MainActivity.this,"Please enter coupon!",Toast.LENGTH_SHORT).show();
                }
            }
        });
        stopAdvButton = findViewById(R.id.StopAdvButton);
        stopAdvButton.setVisibility(View.INVISIBLE);

        AdvertiseCallbacks_map = new TreeMap<>();

        adv_service = new Intent(MainActivity.this, Service_Adv.class);
    }

    public void SharedPrefesSAVE(int type,String value){
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("data",0);
        SharedPreferences.Editor prefEDIT = prefs.edit();
        switch (type){
            case 1 :
                prefEDIT.putString("TITLE",value);
                break;
            case 2 :
                prefEDIT.putString("DATE",value);
                break;
            case 3 :
                prefEDIT.putString("COUPON",value);
                break;
//            case 7 :
//                prefEDIT.putString("CARD",value);
//                break;
        }

        prefEDIT.apply();
    }

    private static byte[] bigIntToByteArray(final int i) {
        BigInteger bigInt = BigInteger.valueOf(i);
        return bigInt.toByteArray();
    }

    public static byte[] byteMerger(byte[] byte_1, byte[] byte_2) {
        byte[] byte_3 = new byte[byte_1.length + byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }

}
