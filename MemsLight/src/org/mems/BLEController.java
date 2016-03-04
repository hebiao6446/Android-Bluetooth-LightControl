package org.mems;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2014/12/2.
 */
public class BLEController {
    private Context mContext;
    private BLEService mService;

    private static BLEController s_singleton = null;
    private Map<String, BLEService.BLEDeviceContext> usedDevices;

    public boolean isTest = false;

    private BLEController(Context context) {
        mContext = context.getApplicationContext();
    }

    public static synchronized BLEController getInstance(Context c) {
        if (s_singleton == null) {
            s_singleton = new BLEController(c);
        }
        return s_singleton;
    }

    public void setService(BLEService service) {
        mService = service;
    }

    public BLEService.BLEDeviceContext getDevice(String deviceAddress) {
        return mService.getUsedDevices().get(deviceAddress);
    }

    public void changeDeviceName(BLEService.BLEDeviceContext device, String name) {
        mService.changeDeviceName(device, name);
    }

    public void removeDevice(String deviceAddress) {
        mService.removeDevice(deviceAddress);
    }

    public void addDevices(List<BluetoothDevice> checkedDevices) {
        mService.addDevices(checkedDevices);
    }

    public Map<String, BLEService.BLEDeviceContext> getUsedDevices() {
        if (mService == null) return null;
        return mService.getUsedDevices();
    }

    public void connectAllDevices() {
        mService.connectAllDevices();
    }

    public int switchDevice(String address) {
        BLEService.BLEDeviceContext device = mService.getUsedDevices().get(address);
        return device.toggleStatus();
    }

    //listener method
    public interface OnDeviceListListener {
        public void onDeviceListChanged(boolean fromCB);
    }
    private OnDeviceListListener mListener = null;
    public void setOnDeviceListListener(OnDeviceListListener l) {
        mListener = l;
    }
    //listener notify
    public void refreshList() {
        if (mListener != null) {
            mListener.onDeviceListChanged(true);
        }
    }
}
