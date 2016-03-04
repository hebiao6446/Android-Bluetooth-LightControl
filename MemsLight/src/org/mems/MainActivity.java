package org.mems;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.mems.BLEService.BLEDeviceContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity implements BLEController.OnDeviceListListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    // 进度对话框的ID
    private static final int DIALOG_ID_PROGRESS = 1;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;

    private List<BLEDeviceContext> usedDevices = new ArrayList<BLEDeviceContext>();
    private MyDeviceListAdapter listAdapter;
    private ListView deviceList;
    private ImageButton switchall_btn;
	private AlertDialog mDialogBuilder;
    private BLEController mController = null;

    private boolean mDoneInit = false;
    // 扫描到的设备
    private List<BluetoothDevice> unusedDevices = Collections.synchronizedList(new ArrayList<BluetoothDevice>());

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            removeMessages(1);
            removeMessages(2);
            if (msg.what == 1 || msg.what == 2) {
                if (mController.isTest) {
                    bluetoothAdapter.cancelDiscovery();
                }
                else {
                    bluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }
            
            if (mDialogBuilder != null && mDialogBuilder.isShowing()) {
            	return;
            }
            
            dismissDialog(DIALOG_ID_PROGRESS);

            if (unusedDevices.size() <= 0) {
                Toast.makeText(MainActivity.this, "未找到LED灯", Toast.LENGTH_LONG).show();
            }
            else {
                showDeviceChooser();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        mController = BLEController.getInstance(this);

        switchall_btn = (ImageButton)findViewById(R.id.switchall_btn);
        deviceList = (ListView) findViewById(R.id.device_list);
        listAdapter = new MyDeviceListAdapter(this);
        deviceList.setAdapter(listAdapter);

        // 检查蓝牙设置
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // 让用户启用蓝牙
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.w(LOG_TAG, "需要用户启动蓝牙");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(enableBtIntent);
            return;
        }

        Intent serviceIntent = new Intent(this, BLEService.class);
        startService(serviceIntent);

        if (mController.isTest) {
            // Register the BroadcastReceiver
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView

                if (!mController.getUsedDevices().containsKey(device.getAddress())) {
                    unusedDevices.add(device);

                    mHandler.removeMessages(1);
                    mHandler.sendEmptyMessage(2);
                }
            }
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        onDeviceListChanged(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.w(LOG_TAG, "onStart: registerReceiver");

//        IntentFilter filter = new IntentFilter();
//        filter.addAction(BLEService.BROADCAST_USED_DEVICE_ADDED);
//        registerReceiver(broadcastReceiver, filter);

        mController.setOnDeviceListListener(this);
    }

    @Override
    protected void onStop() {
        Log.w(LOG_TAG, "onStop: unregister broadcast.");
//        unregisterReceiver(broadcastReceiver);

        mController.setOnDeviceListListener(null);
        super.onStop();
    }

    @SuppressWarnings("deprecation")
    private void showDeviceChooser() {
        final int size = unusedDevices.size();
        final String[] deviceNames = new String[size];
        final boolean[] checkeds = new boolean[size];

        Log.d(LOG_TAG, "showDeviceChooser.found device count="+size);

        for (int i = 0; i < size; i++) {
            BluetoothDevice device = unusedDevices.get(i);
            deviceNames[i] = device.getName() + "[" + device.getAddress() + "]";
            checkeds[i] = (size == 1) ? true : false;//如果只有一个设备帮用户打钩
        }

        mDialogBuilder = new AlertDialog.Builder(MainActivity.this).setTitle("请选择智能蓝牙灯泡")
                .setMultiChoiceItems(deviceNames, checkeds, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    }
                }).setPositiveButton("连接", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialoginterface, int which) {
                        dialoginterface.dismiss();
                        
                        for (int i = 0; i < checkeds.length; i++) {
                            if (checkeds[i]) {
                            	final List<BluetoothDevice> checkedDevices = new ArrayList<BluetoothDevice>();
                            	checkedDevices.clear();
                            	checkedDevices.add(unusedDevices.get(i));
                                mHandler.postDelayed(new Runnable() {
									
									@Override
									public void run() {
										//
										mController.addDevices(checkedDevices);
									}
								}, 1000+(i*500));
                            }
                        }
                        
                    }
                }).create();
        mDialogBuilder.show();
    }

    public void onOpenHelp(View view) {
        String URL_PRE_LOCAL="file:///android_asset/help/";
        Intent intent = new Intent();
        intent.setClass(this, HelpWebSite.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        intent.putExtra(HelpWebSite.EXTRA_TITLE, "操作简介");
        intent.putExtra(HelpWebSite.EXTRA_HTML,URL_PRE_LOCAL+"help.html");
        startActivity(intent);
    }

    // 显示未使用设备列表，让用户添加
    public void onAddDevice(View view) {

        unusedDevices.clear();

        Log.i(LOG_TAG, "开始扫描");
//        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        if (mController.isTest) {
            if (!bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.startDiscovery();
            }
        }
        else {
            bluetoothAdapter.startLeScan(mLeScanCallback);
        }

        mHandler.sendEmptyMessageDelayed(1, 3000); // 扫描30秒

        showDialog(DIALOG_ID_PROGRESS);
    }

    public void onSwitchAll(View view) {
        ImageButton btn = (ImageButton) view;
        boolean newStatus = !btn.isActivated();
        btn.setActivated(newStatus);

        Map<String, BLEDeviceContext> devices = mController.getUsedDevices();
        for (BLEDeviceContext d : devices.values()) {
            d.syncStatus(newStatus);//btn.isActivated());
        }
    }

    public void onReconnect(View view) {
        String address = (String) (view.findViewById(R.id.device_name)).getTag();
        BLEDeviceContext device = mController.getUsedDevices().get(address);
        if (device.connected == BluetoothProfile.STATE_DISCONNECTED) {
            Log.d(LOG_TAG, "WILL do reconnect!");
            device.reconnect();
        }
        else {
            Log.d(LOG_TAG, "not in disconnected, maybe already in connecting!");
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_ID_PROGRESS:
                ProgressDialog dialog = new ProgressDialog(this);
                dialog.setMessage("正在扫描设备请稍后...");
                dialog.setIndeterminate(true);
                dialog.setCancelable(false);
                return dialog;
        }
        return null;
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.i(LOG_TAG, "搜索到设备：" + device);
            Map<String, BLEService.BLEDeviceContext> serviceUsedDevice = mController.getUsedDevices();
            if (serviceUsedDevice == null || serviceUsedDevice.keySet() == null) {
                Log.i(LOG_TAG, "onLeScan ble=null");
                return;
            }
            Iterator<String> belSet = serviceUsedDevice.keySet().iterator();
            while(belSet.hasNext()) {
            	Log.d("temp", "used address="+belSet.next());
            }
            if (!serviceUsedDevice.containsKey(device.getAddress())) {

//            	Iterator<String> belSet = unusedDevices.keySet().iterator();
                for (int i= 0; i < unusedDevices.size(); i++) {
                	Log.d("temp", "unusedDevices address2="+unusedDevices.get(i));
                }
                if (!unusedDevices.contains(device)) {
                    unusedDevices.add(device);
                }

                getLambType(scanRecord);
//                mHandler.removeMessages(1);
//                mHandler.sendEmptyMessage(2);
            }
        }
    };

    void getLambType(byte[] scanRecord) {
        byte[] encodeByte = new byte[8];
        int y = 0;
        for (int count = encodeByte.length + 1; count <= 16; count++) {
            if (y < encodeByte.length) {
                encodeByte[y] = scanRecord[count];
                y++;
            }
        }
        byte[] decodeByte = new byte[7];
        boolean checksum = arrayCrcDecode(7, encodeByte,
                decodeByte);
        if (checksum) {
            byte macByte[] = new byte[6], lampByte;
            for (int idx = 0; idx < 6; idx++) {
                macByte[idx] = decodeByte[idx];
            }
            lampByte = decodeByte[6];
            // 信号为负数，转化为绝对值
//            listener.RFstarBLEManageListener(device,
//                    Math.abs(rssi), encodeByte,
//                    (int) (lampByte & 0xff));
            Log.d("temp", "lampByte="+(int)(lampByte & 0xff));
        }
    }

    private boolean arrayCrcDecode(int arrayLengh, byte[] arrayEncode,
                                   byte[] arrayDecode) {
        boolean checkout = false;
        // 生成新的数组
        for (int idx = 0; idx < arrayLengh; idx++) {
            arrayDecode[idx] = (byte) (arrayEncode[0] ^ arrayEncode[idx + 1]);
        }
        // 计算crcChecksum
        byte crcChecksum = this.CRC_Checksum(arrayLengh, arrayDecode);
        if (crcChecksum == arrayEncode[0]) {
            checkout = true;
        }
        return checkout;
    }

    private byte CRC_Checksum(int arrayLengh, byte[] array) {
        int i, j;
        byte crcPassword[] = { 'C', 'h', 'e', 'c', 'k', 'A', 'e', 's' };
        byte CRC_Checkout = 0x0;
        for (i = 0; i < arrayLengh; i++) {
            byte CRC_Temp = array[i];
            for (j = 0; j < 8; j++) {
                if (((int) CRC_Temp & 0x01) == 1) {
                    CRC_Checkout = (byte) (CRC_Checkout ^ crcPassword[j]);
                }
                CRC_Temp = (byte) (CRC_Temp >> 1);
            }
        }
        return CRC_Checkout;
    }

    /**
     * Adapter list/adapter, list item event.
     * @param view
     */
    public void onSettings(View view) {
        String address = (String) (view).getTag();
        Intent intent = new Intent(this, LEDSettingsActivity.class);
        intent.putExtra("device", address);
        startActivity(intent);
    }

    public void onSwitchOne(View view) {
        String address = (String) (view ).getTag();

        int newStatus = mController.switchDevice(address);
        if (newStatus != -1) {
            ImageButton btn = (ImageButton) view;
            Log.d(LOG_TAG, "onSwitchOne::btn newStatus = " + newStatus);
            btn.setActivated(newStatus == 1 ? true : false);
        }
    }

    @Override
    public void onDeviceListChanged(boolean fromCallback) {
        if (fromCallback && !mDoneInit) {
            mDoneInit = true;
            mController.connectAllDevices();
        }

        // 刷一次
        Map<String, BLEDeviceContext> devices = mController.getUsedDevices();
        if (devices != null && listAdapter != null) {
            usedDevices.clear();
            usedDevices.addAll(devices.values());
            listAdapter.notifyDataSetChanged();

            int on = 0;
            int off = 0;
        	Iterator<BLEDeviceContext> itValues = devices.values().iterator();
            while (itValues.hasNext()){
            	final BLEDeviceContext bleDevice = itValues.next();

            	if (bleDevice.lightOn) {
            		on++;
            	}
            	else {
            		off++;
            	}
            }
            if (on == 0) {
            	switchall_btn.setActivated(false);
            } else
            if (off == 0){
            	switchall_btn.setActivated(true);
            }
        }
    }

    private class MyDeviceListAdapter extends BaseAdapter {
        private SharedPreferences mSharedPref = null;
        private LayoutInflater mInflater;

        public MyDeviceListAdapter(Context context) {
//            super(context);//, R.layout.activity_main_device_item, usedDevices);
            mSharedPref = context.getSharedPreferences(SharedPrefManager.PRE_FILE_TIME, Context.MODE_PRIVATE);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return usedDevices != null ? usedDevices.size() : 0;
        }

        @Override
        public BLEDeviceContext getItem(int i) {
            return (usedDevices != null ? usedDevices.get(i) : null);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            ViewHolder viewHolder = null;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                view = (View) mInflater.inflate(R.layout.activity_main_device_item, parent, false);
                viewHolder.change_status = (ImageButton) view.findViewById(R.id.change_status);
                viewHolder.device_settings = (ImageButton) view.findViewById(R.id.device_settings);
                viewHolder.device_name = (TextView) view.findViewById(R.id.device_name);
                viewHolder.device_status = (TextView) view.findViewById(R.id.device_status);
                viewHolder.alarm_indicator = (ImageView) view.findViewById(R.id.alarm_indicator);
                view.setTag(viewHolder);
            } else {
                view = (View) convertView;
                viewHolder = (ViewHolder) view.getTag();
            }

            BLEDeviceContext item = getItem(position);

            viewHolder.device_name.setText(item.name);
            String status =  item.serviceReady ? getControllableString(item.busy, viewHolder.alarm_indicator, item.address) : getConnectString(item.connected);
            viewHolder.device_status.setText(status);

            viewHolder.change_status.setTag(item.address);
            viewHolder.device_settings.setTag(item.address);
            viewHolder.device_name.setTag(item.address);
            Log.d("temp", "item.lightOn=" + item.lightOn);
            viewHolder.change_status.setActivated(item.lightOn);
            return view;
        }

        private String getControllableString(boolean busy, ImageView alarm_indicator, String address) {
            if (busy) {
                alarm_indicator.setVisibility(View.GONE);
                return "开灯中...";
            }else {
                if (mSharedPref.getBoolean(SharedPrefManager.PRE_KEY_timeOnStatus+address, false) ||
                        mSharedPref.getBoolean(SharedPrefManager.PRE_KEY_timeOffStatus+address, false)) {
                    alarm_indicator.setVisibility(View.VISIBLE);
                }
                else {
                    alarm_indicator.setVisibility(View.GONE);
                }
                return "可以控制";
            }
        }

        private String getConnectString(int connected) {
            switch(connected) {
                case BluetoothProfile.STATE_CONNECTED:
                    return "已连接";
                case BluetoothProfile.STATE_DISCONNECTED:
                    return "未连接，点击此处重连";
                case BluetoothProfile.STATE_CONNECTING:
                    return "正在连接";
                case BluetoothProfile.STATE_DISCONNECTING:
                    return "正在断开";
            }
            return "未知";
        }

        class ViewHolder {
            public ImageButton change_status;
            public ImageButton device_settings;
            public TextView device_name;
            public TextView device_status;
            public ImageView alarm_indicator;
        }
    }


}
