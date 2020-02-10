package com.android.bluetoothprinter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;
import android.view.View;

import java.util.Set;


/**
 * 类说明:蓝牙设备的实体类
 * shi-bash-cmd  2020/02/06
 */
public class PrintBean {
    public static final int PRINT_TYPE = 1664;
    //蓝牙-名称
    public String name;
    //蓝牙-地址
    public String address;
    //蓝牙-设备类型
    public int type;
    //蓝牙-是否已经匹配
    public boolean isConnect;

    public Set<BluetoothDevice> pairedDevices;

    public BluetoothAdapter bluetoothAdapter;

    BluetoothDevice device;

    /**
     *
     * @param device 蓝牙设备对象
     * @param adapter 蓝牙适配器
     */
    public PrintBean(BluetoothDevice device, BluetoothAdapter adapter) {
        this.name = TextUtils.isEmpty(device.getName()) ? "未知" : device.getName();
        this.address = device.getAddress();
        this.isConnect = device.getBondState() == BluetoothDevice.BOND_BONDED;
        this.type = device.getBluetoothClass().getDeviceClass();
        this.pairedDevices = adapter.getBondedDevices();
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public Set<BluetoothDevice> getPairedDevices() {
        return pairedDevices;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    /**
     * 260-电脑
     * 1664-打印机
     * 524-智能手机
     *
     * @return
     */
    public int getTypeIcon() {
        if (type == BluetoothClass.Device.Major.COMPUTER) {
            return R.drawable.ic_computer_black_24dp;
        } else if (type == PRINT_TYPE) {
            return R.drawable.ic_local_printshop_black_24dp;
        } else if (type == 524) {
            return R.drawable.ic_phone_android_black_24dp;
        } else {
            return R.drawable.ic_issue;
        }
    }

    public String getDeviceType(View view) {
        if (type == PRINT_TYPE) {
            view.setSelected(true);
            return isConnect ? "选择打印" : "点击连接";
        } else {
            view.setSelected(false);
            return "非打印设备";
        }
    }

    public int getType() {
        return type;
    }

    public boolean isConnect() {
        return isConnect;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setConnect(boolean connect) {
        isConnect = connect;
    }

}