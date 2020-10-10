package com.github.sunlong6666.usb_host;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.widget.TextView;
import android.widget.Toast;
import java.util.HashMap;

public class UsbMonitor extends BroadcastReceiver
{

    private static final String ACTION_USB_PERMISSION = "android.USB";
    private int ID = 1241;
    private UsbController usbController;
    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private Context context;
    private TextView tv_usbDeviceDataShow;

    /**
     * 数据初始化
     * @param usbController usb控制器接口
     * @param context 上下文
     */
    UsbMonitor(UsbController usbController,Context context,TextView tv_usbDeviceDataShow)
    {
        this.usbController = usbController;
        this.context = context;
        this.tv_usbDeviceDataShow = tv_usbDeviceDataShow;
    }

    /**
     * 注册USB广播监听、USB权限
     */
    public void register()
    {
        if (this.context != null)
        {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_USB_PERMISSION);
            intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            this.context.registerReceiver(this, intentFilter);
            usbManager = (UsbManager)this.context.getSystemService(Context.USB_SERVICE);

            if (usbManager != null)
            {
                HashMap<String,UsbDevice> list = usbManager.getDeviceList();
                for (UsbDevice usbDevice : list.values())
                {
                    if (usbDevice.getVendorId() == ID)
                    {
                        this.usbDevice = usbDevice;
                        usbController.onDeviceInsert(this, usbManager,usbDevice);
                        break;
                    }
                }
                tv_usbDeviceDataShow.setText("不支持该设备");
            }

        }
    }

    /**
     * 请求打开此USB设备的权限
     * @param usbDevice usb设备
     */
    public void requestOpenDevice(UsbDevice usbDevice)
    {
        if (usbManager != null)
        {
            if (usbManager.hasPermission(usbDevice))
            {
                usbController.onDeviceOpen(this,usbManager,usbDevice);
            }
            else
            {
                usbManager.requestPermission(usbDevice,PendingIntent.getBroadcast(context, 666, new Intent(ACTION_USB_PERMISSION), 0));
            }
        }
    }

    /**
     * 注销USB广播监听
     */
    public void unregister()
    {
        if (context != null)
        {
            context.unregisterReceiver(this);
            context = null;
            usbManager = null;
            usbController = null;
        }
    }

    /**
     * 广播事务处理中心
     * @param context 上下文
     * @param intent 意图
     */
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent.getExtras() != null && !intent.getExtras().isEmpty())
        {
            usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            switch(intent.getAction())
            {
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    Toast.makeText(context, "设备接入", Toast.LENGTH_LONG).show();
                    if (usbDevice.getVendorId() == ID) usbController.onDeviceInsert(this, usbManager,usbDevice);
                    else tv_usbDeviceDataShow.setText("不支持该设备");
                    break;
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    Toast.makeText(context, "设备断开", Toast.LENGTH_LONG).show();
                    usbController.onDevicePullOut(this,usbManager,usbDevice);
                    break;
                case UsbMonitor.ACTION_USB_PERMISSION:
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                    {
                        usbController.onDeviceOpen(this,usbManager,usbDevice);
                    }
                    else
                    {
                        Toast.makeText(context, "拒绝USB权限！", Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        }
        else
        {
            Toast.makeText(this.context,"请检查USB设备！",Toast.LENGTH_LONG).show();
        }
    }


}
