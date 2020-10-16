package com.github.sunlong6666.usb_host;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.os.Message;

public class UsbCDC
{
    private boolean connect; //USB连接状态
    private UsbInterface usbInterface; //USB设备的物理接口

    //控制传输模式通道
    private UsbEndpoint controlUsbEndpoint;
    //块传输模式通道
    private UsbEndpoint bulkInUsbEndpoint;
    private UsbEndpoint bulkOutUsbEndpoint;
    //中断传输模式通道
    private UsbEndpoint intInUsbEndpoint;
    private UsbEndpoint intOutUsbEndpoint;

    private UsbDeviceConnection usbDeviceConnection; //USB设备连接链路，用来进行设备通讯

    private Message mes; //信息包

    private MyHandler myHandler;//信息处理中心对象
    UsbCDC(MyHandler myHandler)
    {
        this.myHandler = myHandler;
    }

    /**
     * 向USB设备发送数据
     * @param message 要发送的数据，字符串类型
     * @return 数据发送结果，true代表发送成功
     */
    public boolean send(String message)
    {
        if (this.usbDeviceConnection == null) //判断USB链路是否获取到，不为空才能进行数据发送
        {
            //如果USB链路为空，执行该作用域代码
            connect = false;
            myHandler.sendEmptyMessage(MyHandler.USB_CONNECT_FAILED);
            return false;
        }
        byte[] messageBytes = message.getBytes(); //字符串转为数组
        int result = usbDeviceConnection.bulkTransfer(bulkOutUsbEndpoint,messageBytes,messageBytes.length,100);//发送数据，发送转换为数组后的数据，超时时间为100毫秒
        if ((result >= 0)) //发送数据返回值大于等于0代表发送成功
        {
            //向信息处理中心发送“发送成功”的信息，并将信息内容传递过去
            mes = new Message();
            mes.obj = new String(messageBytes);
            mes.what = MyHandler.OUTPUT;
            myHandler.sendMessage(mes);
            return true;
        }
        else
        {
            //发送失败
            connect = false;
            myHandler.sendEmptyMessage(MyHandler.USB_CONNECT_FAILED);
            return false;
        }
    }

    /**
     * 接收数据
     * @return 接收的数据内容
     */
    public String readData()
    {
        byte[] tempByte = new byte[1024];
        if (usbDeviceConnection == null) //判断USB连接链路是否为空，不为空才能进行数据接收
        {
            connect = false;
            myHandler.sendEmptyMessage(MyHandler.USB_CONNECT_FAILED);
            return null;
        }
        int i = usbDeviceConnection.bulkTransfer(bulkInUsbEndpoint,tempByte,tempByte.length,100);//读取数据，100为超时时间，接收的数据为数组类型
        if (i < 0) //小于0代表接收失败或未接收到数据，接收结果也受USB设备的影响
        {
            return null;
        }
        //将接收的数组转为字符串并返回
        byte[] messageByte = new byte[i];
        System.arraycopy(tempByte,0, messageByte,0, i);
        return new String(messageByte);
    }

    /**
     * 设置USB设备的波特率，方法内涉及算法等；在网上找的，就不写注释了，我也不太懂
     * @param paramInt 要设置波特率的数值
     * @return 设置结果，true代表设置成功
     */
    public boolean configUsb(int paramInt)
    {
        if (usbDeviceConnection != null)
        {
            byte[] arrayOfByte = new byte[8];
            usbDeviceConnection.controlTransfer(192, 95, 0, 0, arrayOfByte, 8, 1000);
            usbDeviceConnection.controlTransfer(64, 161, 0, 0, null, 0, 1000);
            long l1 = 1532620800 / paramInt;
            for (int i = 3; ; i--)
            {
                if ((l1 <= 65520L) || (i <= 0))
                {
                    long l2 = 65536L - l1;
                    int j = (short) (int) (0xFF00 & l2 | i);
                    int k = (short) (int) (0xFF & l2);
                    usbDeviceConnection.controlTransfer(64, 154, 4882, j, null, 0, 1000);
                    usbDeviceConnection.controlTransfer(64, 154, 3884, k, null, 0, 1000);
                    usbDeviceConnection.controlTransfer(192, 149, 9496, 0, arrayOfByte, 8, 1000);
                    usbDeviceConnection.controlTransfer(64, 154, 1304, 80, null, 0, 1000);
                    usbDeviceConnection.controlTransfer(64, 161, 20511, 55562, null, 0, 1000);
                    usbDeviceConnection.controlTransfer(64, 154, 4882, j, null, 0, 1000);
                    usbDeviceConnection.controlTransfer(64, 154, 3884, k, null, 0, 1000);
                    usbDeviceConnection.controlTransfer(64, 164, 0, 0, null, 0, 1000);
                    return true;
                }
                l1 >>= 3;
            }
        }
        else  return false;
    }

    /**
     * 获取收发数据的通道
     * @param usbDevice USB设备
     * @param usbDeviceConnection USB连接链路
     */
    public void openCDC(UsbDevice usbDevice, UsbDeviceConnection usbDeviceConnection)
    {
        this.usbDeviceConnection = usbDeviceConnection;
        usbInterface = usbDevice.getInterface(findCDC(usbDevice)); //获取USB设备接口
        if (usbDeviceConnection == null) //判断USB设备链路是否为空
        {
            myHandler.sendEmptyMessage(MyHandler.USB_CONNECT_FAILED);
            return;
        }
        if (!usbDeviceConnection.claimInterface(usbInterface,true)) //USB设备链路绑定获取到的接口
        {
            myHandler.sendEmptyMessage(MyHandler.USB_CONNECT_FAILED);
            return;
        }
        int numberEndpoints = usbInterface.getEndpointCount(); //获取USB设备接口的数据传输通道数量
        for (int num = 0; num <= numberEndpoints-1; num++)
        {
            UsbEndpoint usbEndpoint = usbInterface.getEndpoint(num);
            switch (usbEndpoint.getType())
            {
                //USB控制
                case UsbConstants.USB_ENDPOINT_XFER_CONTROL:
                    controlUsbEndpoint = usbEndpoint;
                    break;
                //USB传输
                case UsbConstants.USB_ENDPOINT_XFER_BULK:
                    switch (usbEndpoint.getDirection())
                    {
                        case UsbConstants.USB_DIR_OUT:
                            bulkOutUsbEndpoint = usbEndpoint;
                            break;
                        case UsbConstants.USB_DIR_IN:
                            bulkInUsbEndpoint = usbEndpoint;
                            break;
                    }
                    break;
                //USB中断
                case UsbConstants.USB_ENDPOINT_XFER_INT:
                    switch (usbEndpoint.getDirection())
                    {
                        case UsbConstants.USB_DIR_OUT:
                            intOutUsbEndpoint = usbEndpoint;
                            break;
                        case UsbConstants.USB_DIR_IN:
                            intInUsbEndpoint = usbEndpoint;
                            break;
                    }
                    break;
            }
        }
        if (bulkOutUsbEndpoint != null && bulkInUsbEndpoint != null)
        {
            connect = true;
            String usbData = "Name:"+usbDevice.getDeviceName()+"\nID:"+usbDevice.getDeviceId()+"    VID:"+usbDevice.getVendorId()+"    PID:"+usbDevice.getProductId();
            mes = new Message();
            mes.obj = usbData;
            mes.what = MyHandler.USB_CONNECT_SUCCESS;
            myHandler.sendMessage(mes);
            threadReadData.start();
        }
        else
        {
            connect = false;
            myHandler.sendEmptyMessage(MyHandler.USB_CONNECT_FAILED);
        }
    }

    /**
     *
     */
    private Thread threadReadData = new Thread(new Runnable()
    {
        String message = "";
        @Override
        public void run()
        {
            while (connect)
            {
                String temMes = readData();
                if (temMes != null)
                {
                    message = message+temMes;
                    continue;
                }
                else
                {
                    if (!message.equals(""))
                    {
                        mes = new Message();
                        mes.obj = message;
                        mes.what = MyHandler.INPUT;
                        myHandler.sendMessage(mes);
                        message = "";
                    }
                }
            }
        }
    });

    /**
     *
     * @param usbDevice
     * @return
     */
    private int findCDC(UsbDevice usbDevice)
    {
        int interfaceCount = usbDevice.getInterfaceCount();
        for (int count = 0; count < interfaceCount; ++count)
        {
            if (usbDevice.getInterface(count).getInterfaceClass() == UsbConstants.USB_CLASS_CDC_DATA)
            {
                return count;
            }
        }
        return -1;
    }

    public void close()
    {
        connect = false;
        usbDeviceConnection.releaseInterface(usbInterface);
        usbDeviceConnection.close();
        usbDeviceConnection = null;
        bulkOutUsbEndpoint = null;
        bulkInUsbEndpoint = null;
    }

}
