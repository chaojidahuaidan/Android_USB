package com.github.sunlong6666.usb_host;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyHandler extends Handler
{

    public static final int OUTPUT = 0;
    public static final int INPUT = 1;

    public static final int USB_CONNECT_SUCCESS = 2;
    public static final int USB_CONNECT_FAILED = 3;

    public static boolean USB_CONNECT_STATE = false;

    private Button bt_send,bt_sendTiming;
    private TextView tv_sendMessageShow,tv_receiveMessageShow,tv_usbDataShow;
    private Context context;
    private MainActivity mainActivity;
    MyHandler(TextView tv_sendMessageShow, TextView tv_receiveMessageShow, TextView tv_usbDataShow, Button bt_send, Button bt_sendTiming, Context context,MainActivity mainActivity)
    {
        this.bt_send = bt_send;
        this.tv_sendMessageShow = tv_sendMessageShow;
        this.tv_receiveMessageShow = tv_receiveMessageShow;
        this.tv_usbDataShow = tv_usbDataShow;
        this.bt_sendTiming = bt_sendTiming;
        this.context = context;
        this.mainActivity = mainActivity;
    }

    @Override
    public void handleMessage(@NonNull Message msg)
    {
        switch (msg.what)
        {
            case USB_CONNECT_SUCCESS:
                MyHandler.USB_CONNECT_STATE  = true;
                bt_send.setEnabled(true);
                bt_sendTiming.setEnabled(true);
                bt_send.setTextColor(Color.BLACK);
                bt_sendTiming.setTextColor(Color.BLACK);
                tv_usbDataShow.setText(msg.obj.toString());
                Toast.makeText(context,"连接成功",Toast.LENGTH_LONG).show();
                break;
            case USB_CONNECT_FAILED:
                MyHandler.USB_CONNECT_STATE  = false;
                bt_send.setEnabled(false);
                bt_sendTiming.setEnabled(false);
                bt_send.setTextColor(Color.GRAY);
                bt_sendTiming.setTextColor(Color.GRAY);
                bt_sendTiming.setText("定时发送");
                tv_usbDataShow.setText("未连接设备");
                mainActivity.closeAll();
                Toast.makeText(context,"断开连接",Toast.LENGTH_LONG).show();
                break;
            case OUTPUT:
                if (messageShowNeedRoll(tv_sendMessageShow) != 0) tv_sendMessageShow.scrollTo(0, messageShowNeedRoll(tv_sendMessageShow));
                tv_sendMessageShow.append("[TX]"+gteNowDate()+": "+msg.obj.toString()+"\n");
                break;
            case INPUT:
                if (messageShowNeedRoll(tv_receiveMessageShow) != 0) tv_receiveMessageShow.scrollTo(0, messageShowNeedRoll(tv_receiveMessageShow));
                tv_receiveMessageShow.append("[RX]"+gteNowDate()+": "+msg.obj.toString()+"\n");
                break;
        }
    }

    /**
     *
     * @return
     */
    private String gteNowDate()
    {
        SimpleDateFormat sdf = new SimpleDateFormat();// 格式化时间
        sdf.applyPattern("HH:mm:ss");// a为am/pm的标记
        Date date = new Date();// 获取当前时间
        return sdf.format(date);
    }

    /**
     *
     * @param textView
     * @return
     */
    private int messageShowNeedRoll(TextView textView)
    {
        int offset = textView.getLineCount() * textView.getLineHeight();
        if (offset > textView.getHeight()) return offset - tv_receiveMessageShow.getHeight();
        else return 0;
    }

}
