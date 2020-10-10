package com.github.sunlong6666.usb_host;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

public interface UsbController
{

    void onDeviceInsert(UsbMonitor usbMonitor, UsbManager usbManager, UsbDevice usbDevice);

    void onDevicePullOut(UsbMonitor usbMonitor, UsbManager usbManager, UsbDevice usbDevice);

    void onDeviceOpen(UsbMonitor usbMonitor, UsbManager usbManager, UsbDevice usbDevice);
}
