package com.github.sunlong6666.usb_host;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

public interface UsbController
{

    //USB设备接入时的接口
    void onDeviceInsert(UsbMonitor usbMonitor, UsbManager usbManager, UsbDevice usbDevice);

    //USB设备弹出时的接口
    void onDevicePullOut(UsbMonitor usbMonitor, UsbManager usbManager, UsbDevice usbDevice);

    //USB设备连接成功时的接口
    void onDeviceOpen(UsbMonitor usbMonitor, UsbManager usbManager, UsbDevice usbDevice);
}
