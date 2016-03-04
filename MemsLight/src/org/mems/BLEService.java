package org.mems;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//测试用例：1：停止程序，重连OK；2. 卸载重进，重连OK；3.无数次退出重连OK；
//  4. 硬件重连？


/** 主动连接管理：
 * 1. Service alive: connect: is ok.
 *                  disconnect: just let it disconnect.===>if user want to control it, check connection, (do connect and let user wait if didn't connect).
 * 2. Service die(onDestroy): disconnect.
 *
 * 被动连接管理：
 * 1. 下位断开: 上位跟断并重置变量；
 * 2. 下拉自动连接：上位也自动更新变量；
 *
 * 自动恢复：
 * 1. 若无连接，或突然失联，要从历史中恢复
 */
public class BLEService extends Service {

    public static final String BROADCAST_USED_DEVICE_ADDED = "BROADCAST_USED_DEVICE_ADDED";
    public static final String BROADCAST_CONNECT_STATUS_CHANGED = "BROADCAST_CONNECT_STATUS_CHANGED";
    public static final String BROADCAST_USABLE_STATUS_CHANGED = "BROADCAST_USABLE_STATUS_CHANGED";
    public static final String BROADCAST_READWRITE_FINISH_CHANGED = "BROADCAST_READWRITE_FINISH_CHANGED";

    private static final String LOG_TAG = BLEService.class.getSimpleName();

    // TODO !!!!!!!!!!!!!!!!!!!!!
    private static final UUID SERVICE_LIGHT_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");

    private static final UUID CHARACTERISTIC_LIGHT_UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_SCHEDULE_UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_SCENE_UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb");

    private static final String DEVICE_PREF = "DEVICE_PREF";
    // 保存已添加到列表中的所有设备地址
    private static final String USED_DEVICE_ADDRESSES = "USED_DEVICE_ADDRESSES";
    // 保存设备名字
    private static final String USED_DEVICE_NAMES_PREFIX = "USED_DEVICE_NAME_";
    private static final int MSG_WRITE_RESULT = 1;
    private static final int MSG_READ_RESULT = 2;
    private static final int MSG_REFRESH_UI = 3;
    private static final int MSG_RECONNECT_BLE = 4;
    private static final int MSG_REGET_SERVICE = 5;
    private static final int MSG_GATT_RECONNECT = 6;

    private final IBinder binder = new LocalBinder();

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private SharedPreferences preferences;

    private ExecutorService generalWorker = Executors.newSingleThreadExecutor();
    private ExecutorService readWriteWorker = Executors.newSingleThreadExecutor();

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_WRITE_RESULT:
                    //Toast.makeText(BLEService.this, (msg.arg1 == 1) ? "写成功" : "写失败", Toast.LENGTH_SHORT).show();
                    //去掉所有重连，全部靠用户点击重连. 自动只会增加复杂度，往往也连不上。
//                    if (msg.arg1 == 0) {
//                        Message newMsg = mHandler.obtainMessage(MSG_RECONNECT_BLE);
//                        newMsg.obj = msg.obj;
//                        sendMessage(newMsg);
//                    }
                    mController.refreshList();
                    return;
                case MSG_READ_RESULT:
                    //Toast.makeText(BLEService.this, (msg.arg1 == 1) ? "读成功" : "读失败", Toast.LENGTH_SHORT).show();
                    mController.refreshList();
                    return;
                case MSG_REFRESH_UI:
                    // update serviceReady
                    mController.refreshList();
                    return;
                case MSG_GATT_RECONNECT: {
                    String addr = (String) msg.obj;
                    if (usedDevices != null) {
                        BLEDeviceContext devices = usedDevices.get(addr);
                        devices.connect(true);
                    }
                    return;
                }
                case MSG_REGET_SERVICE:{
                    String addr = (String) msg.obj;
                    if (usedDevices != null) {
                        BLEDeviceContext devices = usedDevices.get(addr);
                        devices.discoverService();
                    }
                    return;
                }
                case MSG_RECONNECT_BLE: {
                    String addr = (String) msg.obj;
                    if (usedDevices != null) {
                        BLEDeviceContext devices = usedDevices.get(addr);
//                		if (devices.connected) {
//                            Toast.makeText(BLEService.this, "已连接，无需重连.", Toast.LENGTH_SHORT).show();
//                            return;
//                        }

                        devices.connect(false);//don't use old gatt here, its invalidate now.
                    }
                    return;
                }
            }
            super.handleMessage(msg);
        }
    };

    // 用户已使用的设备
    private Map<String, BLEDeviceContext> usedDevices = new ConcurrentHashMap<String, BLEDeviceContext>();
    private Set<String> usedDeviceAddresses = new HashSet<String>();

    private BLEController mController = null;
    private boolean mInitialized = false;

    @Override
    public void onCreate() {
        super.onCreate();

        mController = BLEController.getInstance(this);
        mController.setService(this);
        preferences = getSharedPreferences(DEVICE_PREF, MODE_PRIVATE);

        Log.i(LOG_TAG, "onCreate");
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

//        mTimer = new Timer(true);
        if (!mInitialized) {
            mInitialized = true;
            loadPersistedDevices();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOG_TAG, "onStartCommand");
        if (intent != null && intent.getBooleanExtra(ShutdownReceiver.EXTRA_SHUTDOWN, false)) {
            stopSelf();
        }
        else {
            refreshMainActivity();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(LOG_TAG, "onBind");
        // 连接到所有设备
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(LOG_TAG, "onUnbind");
        // 断开到所有设备的连接
        disconnectAllDevices();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "onDestroy");
        // 关闭所有设备
        disconnectAllDevices();
        closeAllGatt();
        super.onDestroy();
    }

    private synchronized void loadPersistedDevices() {
        // 已使用的设备的地址列表
//        SharedPreferences preferences = getSharedPreferences(DEVICE_PREF, MODE_PRIVATE);
        String devices = preferences.getString(USED_DEVICE_ADDRESSES, "");
        Log.i("temp", "加载设备个数：" + devices);//.toString());

        if (devices.length() > 0) {
            String devices2[] = devices.split(",");

            for (int i = 0; i < devices2.length; i++) {
                usedDeviceAddresses.add(devices2[i]);
            }
        }

        Log.i(LOG_TAG, "已使用的设备地址：" + usedDeviceAddresses.toString());
        if (usedDeviceAddresses.size() > 0) {
            for (String address : usedDeviceAddresses) {
                buildDeviceContext(bluetoothAdapter.getRemoteDevice(address));
            }

            mHandler.sendEmptyMessage(MSG_REFRESH_UI);
        }
    }

    // TODO private?
    public synchronized void addDevices(List<BluetoothDevice> devices) {
    	Editor editor = preferences.edit();
        for (BluetoothDevice device : devices) {
            BLEDeviceContext context = buildDeviceContext(device);

            editor.putString(USED_DEVICE_NAMES_PREFIX + device.getAddress(), device.getName()).apply();
            context.connect(true); // TODO 延时，任务化？
        }

        persistUsedDevices();
    }

    public synchronized void removeDevice(String address) {
        BLEDeviceContext bleDeviceContext = usedDevices.remove(address);
        bleDeviceContext.disconnect();
        bleDeviceContext.close();
        persistUsedDevices();
    }

    private synchronized void persistUsedDevices() {
        if (usedDeviceAddresses == null) {
            return;
        }
        usedDeviceAddresses.clear();
        usedDeviceAddresses.addAll(usedDevices.keySet());

        Iterator<String> it = usedDeviceAddresses.iterator();
        StringBuffer sb = new StringBuffer();
        while (it.hasNext()) {
            sb.append(it.next());
            sb.append(",");
        }

        Log.i(LOG_TAG, "更新-已使用的设备地址：" + sb.toString());
        Editor edit = preferences.edit();
        edit.putString(USED_DEVICE_ADDRESSES, sb.toString()).commit();
        Log.i("temp", "存后设备个数：" + preferences.getString(USED_DEVICE_ADDRESSES, ""));//.toString());

        mHandler.sendEmptyMessage(MSG_REFRESH_UI);
    }

    private BLEDeviceContext buildDeviceContext(BluetoothDevice device) {
        Log.i(LOG_TAG, "useDevice: add=" + device.getAddress());
//        SharedPreferences preferences = getSharedPreferences(DEVICE_PREF, MODE_PRIVATE);

        BLEDeviceContext deviceStatus = new BLEDeviceContext();
        deviceStatus.address = device.getAddress();
        deviceStatus.name = preferences.getString(USED_DEVICE_NAMES_PREFIX + device.getAddress(), device.getName());
        Log.i(LOG_TAG, "useDevice: name=" + deviceStatus.name);
        usedDevices.put(device.getAddress(), deviceStatus);

        return deviceStatus;
    }

    public Map<String, BLEDeviceContext> getUsedDevices() {
        return usedDevices;
    }

    public void changeDeviceName(BLEDeviceContext device, String name) {
        device.name = name;

        Editor edit = getSharedPreferences(DEVICE_PREF, MODE_PRIVATE).edit();
        edit.putString(USED_DEVICE_NAMES_PREFIX + device.address, name);
        edit.commit();

        mHandler.sendEmptyMessage(MSG_REFRESH_UI);
    }

    public void connectAllDevices() {
        for (BLEDeviceContext dc : usedDevices.values()) {
            // TODO 延时？
            if (dc.connected != BluetoothProfile.STATE_DISCONNECTED) continue;
            dc.connect(false);
        }
    }

    private void disconnectAllDevices() {
        Log.d(LOG_TAG, "disconnectAllDevices:");
        for (BLEDeviceContext dc : usedDevices.values()) {
            // TODO 延时？
            dc.disconnect();
        }
    }

    private void closeAllGatt() {
        // TODO 延时？
        Log.d(LOG_TAG, "closeAllGatt:");
        for (BLEDeviceContext dc : usedDevices.values()) {
            dc.close();
        }
    }

    public class BLEDeviceContext {
		public volatile String address; // 设备MAC
        public volatile String name; // 备注名称
        public volatile int connected = BluetoothProfile.STATE_DISCONNECTED;
        public volatile boolean serviceReady = false;//GATT is not null.

        public volatile boolean busy = false;//is reading or writing..0:unused and can write. 1: iswriting/reading;

        private volatile BluetoothGatt gatt;
        private volatile BluetoothGattCharacteristic characteristicLight;
        private volatile BluetoothGattCharacteristic characteristicSchedule;
        private volatile BluetoothGattCharacteristic characteristicScene;

        public long lastRetrytime = 0;

        // 当前状态
        public volatile boolean lightOn = false;
        // 当前颜色
        public volatile byte red = (byte) 0x00;
        public volatile byte green = (byte) 0x00;
        public volatile byte blue = (byte) 0x00;

        // TODO ？多个体设备使用一个回调还是多个回调
        private BluetoothGattCallback mBTgattCallback = new MyBluetoothGattCallback(this);


        private synchronized void connect(boolean usingExistedGatt) {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

            if (!usingExistedGatt) {
                Log.i(LOG_TAG, "connect: forced to use a new GATT. gatt!=null?"+(gatt!=null));
                boolean auto = true;
                if (gatt != null) {
                    auto = false;//周二的原版硬件连接非常快，当时是false.
                    gatt.disconnect();
                    close();
                }
                // create a new one
                //#####这个不要有，若回调没有过来时，用户再通过UI点击list item时，可以重连。connecting重连会被自己返回。
//                connected = BluetoothProfile.STATE_CONNECTING;
                //第二个参数为True很重要，这样Callback里onConnectionStateChange才会过来connected, 之后才能做discoverService
                gatt = device.connectGatt(BLEService.this, auto, mBTgattCallback);
                Log.d(LOG_TAG, "connect: forced to use a new GATT, and got it.");
//                boolean result = gatt.connect();
//                if (!result) {
//                    Log.e(LOG_TAG, "gatt.connect 11: but return false");
//                }
            } else {
            	if (gatt != null) {
            		int connectionState = bluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
                    connected = connectionState;//更新状态
                    if (connectionState == BluetoothProfile.STATE_CONNECTED) {
                        Log.i(LOG_TAG, "connect: GATT already connected, lets discoverService");
                        boolean result = gatt.discoverServices();
                        if (!result) {
                            Log.e(LOG_TAG, "connect: discoverServices return false");
                        }

                        return;
                    } else if (connectionState == BluetoothProfile.STATE_CONNECTING) {
                        Log.i(LOG_TAG, "connect: GATT connecting, waiting...");
                    } else if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.i(LOG_TAG, "connect: GATT disConnected, do::gatt.connect()...");
                        connected = BluetoothProfile.STATE_CONNECTING;
                        boolean result = gatt.connect();
                        if (!result) {
                            Log.e(LOG_TAG, "gatt.connect 21: but return false");
                        }
                    }
                } else {
                    Log.i(LOG_TAG, "connect: Create a new GATT connection");
//                    connected = BluetoothProfile.STATE_CONNECTING;
                    gatt = device.connectGatt(BLEService.this, true, mBTgattCallback);
//                    boolean result = gatt.connect();
//                    if (!result) {
//                        Log.e(LOG_TAG, "gatt.connect 22: but return false");
//                    }
                }
            }
            mHandler.sendEmptyMessage(MSG_REFRESH_UI);
        }

        private synchronized void disconnect() {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

            int connectionState = bluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
            if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(LOG_TAG, "disconnect: GATT already disconnected");
            } else {
                if (gatt != null) {
                    gatt.disconnect();
                }
            }

            connected = connectionState;
            mHandler.sendEmptyMessage(MSG_REFRESH_UI);
        }

        //1。 从设置-》应用管理-》关闭程序-》onDestroy->点击BLE listitem->调这里-->用connect(false)重连成功。
        //2. 从应用设置->移除->(此时硬件灯还亮着)-->重新扫描->连接成功，灯可控。
        //3. 设备自动断开时，不要去重连.(onConnectionStateChanged)
        public void reconnect() {
            connect(false);
        }

        private void close() {
            Log.i(LOG_TAG, "close: closing gatt");
            if (gatt == null) {
                return;
            }
            gatt.close();
            resetAll();
        }

        private void resetAll() {
            gatt = null;
            serviceReady = false;
            busy = false;
            connected = BluetoothProfile.STATE_DISCONNECTED;
            characteristicLight = null;
            characteristicSchedule = null;
            characteristicScene = null;
        }

        private void discoverService() {
            connect(true);
        }

        //用自己的GATT，re get service.
        private void getCharacteristics(BluetoothGatt newGatt) {
            BluetoothGatt gatt = this.gatt;
            if (newGatt == null) {
                //
            }
            Log.d("temp", "we are the same??"+(gatt == this.gatt));
            BluetoothGattService service = gatt.getService(SERVICE_LIGHT_UUID);
            if (service == null) {
                Log.e(LOG_TAG, "getCharacteristics: cannot find service");
                return;
            }
            characteristicLight = service.getCharacteristic(CHARACTERISTIC_LIGHT_UUID);
            if (characteristicLight == null) {
                Log.e(LOG_TAG, "getCharacteristics: cannot find characteristic:" + CHARACTERISTIC_LIGHT_UUID);
            }
            else {
                Log.i(LOG_TAG, "发现特征值:" + CHARACTERISTIC_LIGHT_UUID);
            }

            characteristicSchedule = service.getCharacteristic(CHARACTERISTIC_SCHEDULE_UUID);
            if (characteristicSchedule == null) {
                Log.e(LOG_TAG, "getCharacteristics: cannot find characteristic:" + CHARACTERISTIC_SCHEDULE_UUID);
            }
            else {
                Log.i(LOG_TAG, "发现特征值:" + CHARACTERISTIC_SCHEDULE_UUID);
            }

            characteristicScene = service.getCharacteristic(CHARACTERISTIC_SCENE_UUID);
            if (characteristicScene == null) {
                Log.e(LOG_TAG, "getCharacteristics: cannot find character:" + CHARACTERISTIC_SCENE_UUID);
            }
            else {
                Log.i(LOG_TAG, "发现特征值:" + CHARACTERISTIC_SCENE_UUID);
            }
        }
        
        public void changeColor(int color) {
        	if (!isBleReady("changeColor", characteristicLight)) {
        		return;
        	}

			int red2, blue2, green2;
			red2 = Color.red(color);
			green2 = Color.green(color);
			blue2 = Color.blue(color);
			
			if(red2 == 255)
			{
				green2 = (Color.green(color))/3;
				blue2 = (Color.blue(color))/3;
			}
			else if(green2 == 255)
			{
				red2 = (Color.red(color))/3;
				blue2 = (Color.blue(color))/3;
			}
			else if(blue2 == 255)
			{
				red2 = (Color.red(color))/3;
				green2 = (Color.green(color))/3;
			}
				
            red = (byte) red2;
            green = (byte)green2;
            blue = (byte) blue2;
            
			Log.d(LOG_TAG, " RGB颜色    red:" + red2 + "  green:" + green2 + " blue:" + blue2);

            if (lightOn == false) {
                lightOn = true;
                mHandler.sendEmptyMessage(MSG_REFRESH_UI);
            }

            byte[] value = new byte[]{ red, green, blue, (byte) 0x00, (byte) 0x64};
            readWriteWorker.submit(new Task(value, this, characteristicLight));
        }

        public int syncStatus(boolean newStatus) {
            if (lightOn != newStatus) {
                return toggleStatus();
            }
            return 0;
        }

		public int toggleStatus() {
			if (!isBleReady("changeStatus", characteristicLight)) {
        		return -1;
        	}

            lightOn = !lightOn;
            Log.d("temp", "item.lightOn="+ lightOn);
            Log.i(LOG_TAG, lightOn ? "准备亮灯" : "准备灭灯");
            byte[] value = new byte[]{(byte)0x00, (byte)0x00, (byte)0x00, (byte) 0xff, lightOn ? (byte) 0x64 : (byte) 0x00};
            readWriteWorker.submit(new Task(value, this, characteristicLight));

            refreshMainActivity();
            return lightOn ? 1 : 0;
        }

        public int changeScene(int position) {
            if (!isBleReady("changeScene", characteristicScene)) {
                return -1;
            }

            byte[] scene = {0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x20, 0x30, 0x31, 0x32, 0x33, 0x34};

            byte[] value = new byte[]{scene[position]};
            readWriteWorker.submit(new Task(value, this, characteristicScene));
            return 0;
        }

        private int countLight(int color) {
            int colorArray[] = new int[3];
            colorArray[0] = Color.red(color);
            colorArray[1] = Color.green(color);
            colorArray[2] = Color.blue(color);
            int hsl[] = new int[3];
            ColorTools.RGB2HSL(colorArray[0], colorArray[1], colorArray[2], hsl);
            return 100 * hsl[2] / 255;
        }

        public int changeBright(int color, int progress) {
        	if (!isBleReady("changeBright", characteristicLight)) {
        		return -1;
        	}

//            int color = SharedPrefManager.getColor(BLEService.this);

            if (progress <= 14) progress = 15;
            if (progress >= 100) progress = 99;

            // 控制
            int newColor = ColorTools.restoreColor2(progress, color);

            int red2, green2, blue2;
            red2 = Color.red(newColor);
            blue2 = Color.blue(newColor);
            green2 = Color.green(newColor);
           /* 
            if(red2 == 255)
			{
				green2 = (Color.green(color))/3;
				blue2 = (Color.blue(color))/3;
			}
			else if(green2 == 255)
			{
				red2 = (Color.red(color))/3;
				blue2 = (Color.blue(color))/3;
			}
			else if(blue2 == 255)
			{
				red2 = (Color.red(color))/3;
				green2 = (Color.green(color))/3;
			}
*/
            Log.d("temp", "66666666  light value  "
                    + countLight(color) + "%");
            Log.d(LOG_TAG, "22222 rgb    red:" + red2 + "  green:" + green2 + " blue:" + blue2);
            byte[] value = new byte[]{(byte)red2, (byte)green2, (byte)blue2, (byte) 0x00, (byte) 0x64};
            if (lightOn == false) {
                lightOn = true;
                mHandler.sendEmptyMessage(MSG_REFRESH_UI);
            }
            readWriteWorker.submit(new Task(value, this, characteristicLight));
            return 0;
        }

        public int stopScheduleOn() {
            if (!isBleReady("stopScheduleOn", characteristicSchedule)) {
                return -1;
            }
            Log.d("schedule", "stopScheduleOn");
            byte[] value = new byte[]{0x00, 0x00, 0x00, 0x01};
            readWriteWorker.submit(new Task(value, this, characteristicSchedule));
            return 0;
        }

        public int stopScheduleOff() {
            if (!isBleReady("stopScheduleOff", characteristicSchedule)) {
                return -1;
            }
            Log.d("schedule", "stopScheduleOff");
            byte[] value = new byte[]{0x00, 0x00, 0x00, 0x00};
            readWriteWorker.submit(new Task(value, this, characteristicSchedule));
            return 0;
        }

        public int startScheduleOn(int hourOfDay, int minute, int speed) {
            if (!isBleReady("startScheduleOn", characteristicSchedule)) {
                return -1;
            }
            Calendar c = new GregorianCalendar();
            c.set(Calendar.HOUR_OF_DAY, hourOfDay);
            c.set(Calendar.MINUTE, minute);

            Calendar now = new GregorianCalendar();
            if (c.before(now)) {
                c.set(Calendar.DATE, c.get(Calendar.DATE) + 1);
            }
            long minutes = (c.getTimeInMillis() - now.getTimeInMillis()) / 1000 / 60 % (60 * 24);
            Log.i(LOG_TAG, "时间差（分）" + minutes);
            int h = (int) minutes / 60;
            int m = (int) minutes % 60;

            byte[] speedvalue = {0x20, 0x23, 0x26};
            Log.d("schedule", "startScheduleOn::"+hourOfDay+","+minute+","+speed);
            byte[] value = new byte[]{(byte) h, (byte) m, speedvalue[speed], 0x01};
            readWriteWorker.submit(new Task(value, this, characteristicSchedule));
            return 0;
        }

        public int startScheduleOff(int hourOfDay, int minute, int speed) {
            if (!isBleReady("startScheduleOff", characteristicSchedule)) {
                return -1;
            }
            Calendar c = new GregorianCalendar();
            c.set(Calendar.HOUR_OF_DAY, hourOfDay);
            c.set(Calendar.MINUTE, minute);

            Calendar now = new GregorianCalendar();
            if (c.before(now)) {
                c.set(Calendar.DATE, c.get(Calendar.DATE) + 1);
            }
            long minutes = (c.getTimeInMillis() - now.getTimeInMillis()) / 1000 / 60 % (60 * 24);
            Log.i(LOG_TAG, "时间差（分）" + minutes);
            int h = (int) minutes / 60;
            int m = (int) minutes % 60;

            byte[] speedvalue = {0x20, 0x23, 0x26};
            Log.d("schedule", "startScheduleOff::"+hourOfDay+","+minute+","+speed);
            byte[] value = new byte[]{(byte) h, (byte) m, speedvalue[speed], 0x00};
            readWriteWorker.submit(new Task(value, this, characteristicSchedule));
            return 0;
        }

        private boolean isBleReady(String string, BluetoothGattCharacteristic characteristic) {
        	if (busy) {
        		Log.e(LOG_TAG, string+" busy now....");
        		//Toast.makeText(BLEService.this, "设备繁忙", Toast.LENGTH_SHORT).show();
        		return false;
        	}

        	if (characteristic == null) {
            	Log.e(LOG_TAG, string+" characteristicLight =null");
            	Toast.makeText(BLEService.this, "未准备好，请稍后再试", Toast.LENGTH_SHORT).show();
                //已经有点击重连了，不要自动重连
//            	Message msg = mHandler.obtainMessage(MSG_REGET_SERVICE);
//            	msg.obj = address;
//            	mHandler.sendMessage(msg);
                return false;
            }
        	return true;
		}
    }

    private class MyBluetoothGattCallback extends BluetoothGattCallback {

        private BLEDeviceContext deviceContext;

        public MyBluetoothGattCallback(BLEDeviceContext deviceContext) {
            this.deviceContext = deviceContext;
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            boolean connectStatusChanged = (deviceContext.connected != newState);

            deviceContext.connected = newState;

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(LOG_TAG, "onConnectionStateChange: connected | " + deviceContext.address);
                
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                    	//Toast.makeText(BLEService.this, deviceContext.name+"连接成功", Toast.LENGTH_SHORT).show();
                    }
                });

                // 不在回调线程中调用
                generalWorker.submit(new Runnable() {
                    @Override
                    public void run() {
                        boolean discoverServicesResult = gatt.discoverServices();
                        if (!discoverServicesResult) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(BLEService.this, deviceContext.name+"发现服务失败!", Toast.LENGTH_LONG).show();
                                }
                            });
                            Log.w(LOG_TAG, "调用发现服务直接返回false");
                        }
                    }
                });

                deviceContext.busy = false;
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w(LOG_TAG, "onConnectionStateChange: disconnected | " + deviceContext.address);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(BLEService.this, deviceContext.name+"已断开, 可点击重连.", Toast.LENGTH_LONG).show();
                    }
                });

                long currentTimeMillis = System.currentTimeMillis();
                if (currentTimeMillis - deviceContext.lastRetrytime > 5000) {// > 5s, let's reconnect.
                    deviceContext.lastRetrytime = currentTimeMillis;
//                    tryReconnect(deviceContext.address);

                }

                //做如下重置，为了下一次可成功重连，因为 service::onDestroy或移除都 走了disconnect+close.下次便可重连.
                //其实在每次重连要产生新的gatt对象时，都把上次的gatt close.，所以这里可以不必close.
                deviceContext.disconnect();
                deviceContext.serviceReady = false;
                deviceContext.close();//难道这个不得不做？在disconnect时，有助于用户点重连成功的概率吗
            } else {
                Log.e(LOG_TAG, "onConnectionStateChange: unknown: " + newState + " | " + deviceContext.address);
            }

            //update UI.
            if (connectStatusChanged) {
            	refreshMainActivity();
            }

            Log.i(LOG_TAG, "onConnectionStateChange---->out");
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	deviceContext.busy = false;
                Log.i(LOG_TAG, "onServicesDiscovered： success | " + deviceContext.address);
                deviceContext.serviceReady = true;
                // 不在回调线程中调用
                generalWorker.submit(new Runnable() {

                    @Override
                    public void run() {
                        deviceContext.getCharacteristics(gatt);
                    }
                });
            } else {
                Log.e(LOG_TAG, "onServicesDiscovered： fail " + status + "| " + deviceContext.address);
                deviceContext.serviceReady = false;
            }

            refreshMainActivity();
        }


		@Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Message msg = mHandler.obtainMessage(MSG_READ_RESULT);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(LOG_TAG, characteristic.toString());
                msg.arg1 = 1;
            } else {
                msg.arg1 = 0;
            }

            deviceContext.busy = false;
            mHandler.sendMessage(msg);
        }

        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Message msg = mHandler.obtainMessage(MSG_WRITE_RESULT);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(LOG_TAG, "onCharacteristicWrite: success | " + deviceContext.address);
                msg.arg1 = 1;
            } else {
                Log.e(LOG_TAG, "onCharacteristicWrite: fail " + status + " | " + deviceContext.address);
                msg.obj = deviceContext.address;
                msg.arg1 = 0;
            }

            deviceContext.busy = false;

            mHandler.sendMessage(msg);
        }
    }

    private void tryReconnect(String address) {
        Message msg = mHandler.obtainMessage(MSG_GATT_RECONNECT);
        msg.obj = address;
        mHandler.sendMessage(msg);
    }

    private class Task implements Runnable {

        private final byte[] value;
        private final BLEDeviceContext deviceContext;
        private BluetoothGattCharacteristic characteristic;

        public Task(byte[] value, BLEDeviceContext deviceContext, BluetoothGattCharacteristic characteristic) {
            super();
            this.value = value;
            this.deviceContext = deviceContext;
            this.characteristic = characteristic;
        }

        @Override
        public void run() {
            int count = 0;
            while (deviceContext.busy && count < 4) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
                count++;
            }

            if (!deviceContext.busy) {
            	deviceContext.busy = true;
                characteristic.setValue(value);
            	deviceContext.gatt.writeCharacteristic(characteristic);
            }
            else {
            	Log.e(LOG_TAG, "task busy now....");
            }

            refreshMainActivity();
        }
    }

    private void refreshMainActivity() {
        new Thread(new Runnable() {

            @Override
            public void run() {

                mHandler.sendEmptyMessage(MSG_REFRESH_UI);
            }
        }).start();
    }


    public class LocalBinder extends Binder {
        public BLEService getService() {
            // 返回Service实例，让客户端直接调用Service的方法
            return BLEService.this;
        }
    }

}
