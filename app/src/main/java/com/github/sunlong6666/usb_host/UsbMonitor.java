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

public class UsbMonitor extends BroadcastReceiver //继承USB广播对象
{

    private static final String ACTION_USB_PERMISSION = "android.USB"; //USB设备的操作权限，可自定义
    private int ID = 1241; //USB设备生产厂商ID，用来区分选择目标USB设备，如果不是该ID的USB设备，不对其进行操作
    private UsbController usbController; //USB动作管理接口
    private UsbManager usbManager; //USB状态、管理对象
    private UsbDevice usbDevice; //USB设备
    private Context context; //上下文
    private TextView tv_usbDeviceDataShow; //USB信息数据展示控件

    /**
     * 数据初始化
     * @param usbController usb控制器接口
     * @param context 上下文
     * @param tv_usbDeviceDataShow USB信息数据展示控件
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
            IntentFilter intentFilter = new IntentFilter(); //意图过滤器
            intentFilter.addAction(ACTION_USB_PERMISSION); //添加USB设备的操作权限意图
            intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED); //添加设备接入意图
            intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED); //添加设备拔出意图
            this.context.registerReceiver(this, intentFilter); //注册添加的意图
            usbManager = (UsbManager)this.context.getSystemService(Context.USB_SERVICE); //获取USB设备管理

            if (usbManager != null)
            {
                HashMap<String,UsbDevice> list = usbManager.getDeviceList(); //获取USB设备，返回的是 UsbDevice 的Hash列表，里面是所有当前连接主机的USB设备
                for (UsbDevice usbDevice : list.values()) //遍历获取到的UsbDevice
                {
                    if (usbDevice.getVendorId() == ID) //找到目标USB设备
                    {
                        this.usbDevice = usbDevice;
                        usbController.onDeviceInsert(this, usbManager,usbDevice); //执行USB接入时接口
                        break;
                    }
                }
                tv_usbDeviceDataShow.setText("不支持该设备"); //如果列表里面没有目标USB设备，执行该操作
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
            if (usbManager.hasPermission(usbDevice))//如果有该USB设备的操作权限
            {
                usbController.onDeviceOpen(this,usbManager,usbDevice);//连接USB设备（打开USB设备）
            }
            else
            {
                usbManager.requestPermission(usbDevice,PendingIntent.getBroadcast(context, 666, new Intent(ACTION_USB_PERMISSION), 0));//如果没有USB操作权限则请求权限
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
            context.unregisterReceiver(this); //注销USB设备广播监听
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
            usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE); //获取意图中的USB设备
            switch(intent.getAction())
            {
                case UsbManager.ACTION_USB_DEVICE_ATTACHED: //USB设备接入
                    Toast.makeText(context, "设备接入", Toast.LENGTH_LONG).show();
                    if (usbDevice.getVendorId() == ID) usbController.onDeviceInsert(this, usbManager,usbDevice); //找到目标USB设备，执行USB设备接入时接口
                    else tv_usbDeviceDataShow.setText("不支持该设备"); //未找到目标USB设备
                    break;
                case UsbManager.ACTION_USB_DEVICE_DETACHED: //USB设备拔出
                    Toast.makeText(context, "设备断开", Toast.LENGTH_LONG).show();
                    usbController.onDevicePullOut(this,usbManager,usbDevice); //执行USB设备拔出时接口
                    break;
                case UsbMonitor.ACTION_USB_PERMISSION: //请求USB设备操作权限
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                    {
                        //同意USB权限
                        usbController.onDeviceOpen(this,usbManager,usbDevice); //执行连接USB设备接口
                    }
                    else
                    {
                        //拒绝USB权限
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
