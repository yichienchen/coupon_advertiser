package com.example.coupon_advertiser;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.annotation.RequiresApi;

import java.util.Arrays;

import static com.example.coupon_advertiser.MainActivity.AdvertiseCallbacks_map;
import static com.example.coupon_advertiser.MainActivity.TAG;
import static com.example.coupon_advertiser.MainActivity.byteMerger;
import static com.example.coupon_advertiser.MainActivity.date_;
import static com.example.coupon_advertiser.MainActivity.id_byte;
import static com.example.coupon_advertiser.MainActivity.mAdvertiseCallback;
import static com.example.coupon_advertiser.MainActivity.mBluetoothLeAdvertiser;
import static com.example.coupon_advertiser.MainActivity.startAdvButton;
import static com.example.coupon_advertiser.MainActivity.stopAdvButton;
import static com.example.coupon_advertiser.MainActivity.total_data;


public class Service_Adv extends Service {
    static int packet_num;
    static int id_num;
    static int pdu_len;

    static byte[][] data_legacy;


    @RequiresApi(api = Build.VERSION_CODES.O)
    public Service_Adv() {

        startAdvertising();
        stopAdvButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopAdvertising();
                stopSelf();
            }
        });
        startAdvButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startAdvertising();
            }
        });
    }



    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    public void startAdvertising(){
        Log.e(TAG, "Service: Starting Advertising");
        data_legacy = Adv_data_seg();

        if (mAdvertiseCallback == null) {
            if (mBluetoothLeAdvertiser != null) {
                for (int q=0;q<data_legacy.length;q++){
                    startBroadcast(q);
                }

            }
        }

        startAdvButton.setVisibility(View.INVISIBLE);
        stopAdvButton.setVisibility(View.VISIBLE);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void startBroadcast(Integer order) {
        String localName =  String.valueOf(1) ;
        BluetoothAdapter.getDefaultAdapter().setName(localName);

        AdvertiseSettings settings = buildAdvertiseSettings();
        AdvertiseData advertiseData = buildAdvertiseData(order);
        AdvertiseData scanResponse = buildAdvertiseData_scan_response(order);
        mBluetoothLeAdvertiser.startAdvertising(settings, advertiseData , scanResponse , new Service_Adv.MyAdvertiseCallback(order));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void stopAdvertising(){
        if (mBluetoothLeAdvertiser != null) {
            for (int q=0;q<data_legacy.length;q++){
                stopBroadcast(q);
            }
            mAdvertiseCallback = null;
        }
        stopAdvButton.setVisibility(View.INVISIBLE);
        startAdvButton.setVisibility(View.VISIBLE);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void stopBroadcast(Integer order) {
        final AdvertiseCallback adCallback = AdvertiseCallbacks_map.get(order);
        if (adCallback != null) {
            try {
                if (mBluetoothLeAdvertiser != null) {
                    mBluetoothLeAdvertiser.stopAdvertising(adCallback);
                }
                else {
                    Log.w(TAG,"Not able to stop broadcast; mBtAdvertiser is null");
                }
            }
            catch(RuntimeException e) { // Can happen if BT adapter is not in ON state
                Log.w(TAG,"Not able to stop broadcast; BT state: {}");
            }
            AdvertiseCallbacks_map.remove(order);
        }
        Log.e(TAG,order +" Advertising successfully stopped");


    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static byte[][] Adv_data_seg(){
        pdu_len=48;  //+3: without name
        if((total_data.length()+5)%pdu_len!=0){
            packet_num = total_data.length()/pdu_len+1;
        }else {
            packet_num = total_data.length()/pdu_len;
        }

        StringBuilder data = new StringBuilder(total_data);
        for(int c=data.length();c%pdu_len!=0;c++){
            data.append("0");
        }

        byte[] data_byte = data.toString().getBytes();
        data_byte = byteMerger(date_,data_byte);
        byte[][] adv_byte = new byte[packet_num][pdu_len+id_byte.length+2];


        for (int counter = 0 ; counter <packet_num ; counter++) {
            adv_byte[counter][0]= intToByte(counter+1);
            adv_byte[counter][1]= intToByte(packet_num);
            System.arraycopy(id_byte, 0, adv_byte[counter], 2, id_byte.length);
            if((counter+1)*pdu_len<=data_byte.length){
                byte[] register = Arrays.copyOfRange(data_byte, counter*pdu_len ,(counter+1)*pdu_len);
                System.arraycopy(register, 0, adv_byte[counter], id_byte.length+2, register.length);
            }else {
                byte[] register = Arrays.copyOfRange(data_byte, counter*pdu_len ,data_byte.length);
                System.arraycopy(register, 0, adv_byte[counter], id_byte.length+2, register.length);
            }
        }

//        for (int counter = 0 ; counter <packet_num ; counter++) {
//            Log.e(TAG,counter + " adv_byte: " + byte2HexStr(adv_byte[counter]));
//        }

        return adv_byte;
    }


    //BLE 4.0
    public static class MyAdvertiseCallback extends AdvertiseCallback {
        private final Integer _order;
        MyAdvertiseCallback(Integer order) {
            _order = order;
        }
        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e(TAG, "Advertising failed errorCode: "+errorCode);
            switch (errorCode) {
                case ADVERTISE_FAILED_ALREADY_STARTED:
                    Log.e(TAG,"ADVERTISE_FAILED_ALREADY_STARTED");
                    break;
                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    Log.e(TAG,"ADVERTISE_FAILED_DATA_TOO_LARGE");
                    break;
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    Log.e(TAG,"ADVERTISE_FAILED_FEATURE_UNSUPPORTED");
                    break;
                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    Log.e(TAG,"ADVERTISE_FAILED_INTERNAL_ERROR");
                    break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    Log.e(TAG,"ADVERTISE_FAILED_TOO_MANY_ADVERTISERS");
                    break;
                default:
                    Log.e(TAG,"Unhandled error : "+errorCode);
            }
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.e(TAG, _order +" Advertising successfully started");
            AdvertiseCallbacks_map.put(_order, this);
        }
    }

    static AdvertiseData buildAdvertiseData(Integer order) {
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeDeviceName(false);
        dataBuilder.setIncludeTxPowerLevel(false);
        dataBuilder.addManufacturerData(0xffff,Arrays.copyOfRange(data_legacy[order],0,27));
        return dataBuilder.build();
    }

    static AdvertiseData buildAdvertiseData_scan_response(Integer order) {
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.addManufacturerData(0xfff1,Arrays.copyOfRange(data_legacy[order],27,54));
        return dataBuilder.build();
    }

    public static AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(false)
                .setTimeout(0);
        return settingsBuilder.build();
    }

    static byte intToByte(int x) {
        return (byte) x;
    }

    static String byte2HexStr(byte[] b) {
        String stmp="";
        StringBuilder sb = new StringBuilder("");

        for (int n=0;n<b.length;n++) {
            stmp = Integer.toHexString(b[n] & 0xFF);
            sb.append((stmp.length()==1)? "0"+stmp : stmp);
        }
        return sb.toString().trim();
    }


}