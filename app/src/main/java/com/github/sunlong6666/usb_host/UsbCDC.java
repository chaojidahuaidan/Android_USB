package com.github.sunlong6666.usb_host;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.os.Message;

public class UsbCDC
{
    private boolean connect;
    private UsbInterface usbInterface;

    //控制端点
    private UsbEndpoint controlUsbEndpoint;
    //块输出端点
    private UsbEndpoint bulkInUsbEndpoint;
    private UsbEndpoint bulkOutUsbEndpoint;
    //中断端点
    private UsbEndpoint intInUsbEndpoint;
    private UsbEndpoint intOutUsbEndpoint;

    private UsbDeviceConnection usbDeviceConnection;

    private Message mes;

    private MyHandler myHandler;
    UsbCDC(MyHandler myHandler)
    {
        this.myHandler = myHandler;
    }

    /**
     *
     * @param message
     * @return
     */
    public boolean send(String message)
    {
        if (this.usbDeviceConnection == null)
        {
            connect = false;
            myHandler.sendEmptyMessage(MyHandler.USB_CONNECT_FAILED);
            return false;
        }
        byte[] messageBytes = message.getBytes();
        int result = usbDeviceConnection.bulkTransfer(bulkOutUsbEndpoint,messageBytes,messageBytes.length,100);
        if ((result >= 0))
        {
            mes = new Message();
            mes.obj = new String(messageBytes);
            mes.what = MyHandler.OUTPUT;
            myHandler.sendMessage(mes);
            return true;
        }
        else
        {
            connect = false;
            myHandler.sendEmptyMessage(MyHandler.USB_CONNECT_FAILED);
            return false;
        }
    }

    /**
     *
     * @return
     */
    public String readData()
    {
        byte[] tempByte = new byte[1024];
        if (usbDeviceConnection == null)
        {
            connect = false;
            myHandler.sendEmptyMessage(MyHandler.USB_CONNECT_FAILED);
            return null;
        }
        int i = usbDeviceConnection.bulkTransfer(bulkInUsbEndpoint,tempByte,tempByte.length,100);
        if (i < 0)
        {
            return null;
        }
        byte[] messageByte = new byte[i];
        System.arraycopy(tempByte,0, messageByte,0, i);
        return new String(messageByte);
    }

    /**
     *
     * @param paramInt
     * @return
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
     *
     * @param usbDevice
     * @param usbDeviceConnection
     */
    public void openCDC(UsbDevice usbDevice, UsbDeviceConnection usbDeviceConnection)
    {
        this.usbDeviceConnection = usbDeviceConnection;
        usbInterface = usbDevice.getInterface(findCDC(usbDevice));
        if (usbDeviceConnection == null)
        {
            myHandler.sendEmptyMessage(MyHandler.USB_CONNECT_FAILED);
            return;
        }
        if (!usbDeviceConnection.claimInterface(usbInterface,true))
        {
            myHandler.sendEmptyMessage(MyHandler.USB_CONNECT_FAILED);
            return;
        }
        int numberEndpoints = usbInterface.getEndpointCount();
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
