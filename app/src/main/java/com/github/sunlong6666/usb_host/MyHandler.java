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

    public static final int OUTPUT = 0; //发送消息
    public static final int INPUT = 1; //接收消息

    public static final int USB_CONNECT_SUCCESS = 2; //USB设备连接成功
    public static final int USB_CONNECT_FAILED = 3; //USB设备连接失败或断开连接

    public static boolean USB_CONNECT_STATE = false; //当前USB设备连接状态

    private Button bt_send,bt_sendTiming; 
    private TextView tv_sendMessageShow,tv_receiveMessageShow,tv_usbDataShow;
    private Context context; //上下文
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
                MyHandler.USB_CONNECT_STATE  = true; //连接状态改变为true
                bt_send.setEnabled(true); //发送控件可以使用
                bt_sendTiming.setEnabled(true);
                bt_send.setTextColor(Color.BLACK); //定时发送控件可以使用
                bt_sendTiming.setTextColor(Color.BLACK); 
                tv_usbDataShow.setText(msg.obj.toString()); //填充意图发送过来的信息
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
                mainActivity.closeAll(); //连接断开或连接失败，执行关闭所有连接和对象的方法
                Toast.makeText(context,"断开连接",Toast.LENGTH_LONG).show();
                break;
            case OUTPUT:
                if (messageShowNeedRoll(tv_sendMessageShow) != 0) tv_sendMessageShow.scrollTo(0, messageShowNeedRoll(tv_sendMessageShow));//如果TextView填充满可使用高度就滚动到最新更新处
                tv_sendMessageShow.append("[TX]"+gteNowDate()+": "+msg.obj.toString()+"\n"); //给控件填充意图发送来的信息
                break;
            case INPUT:
                if (messageShowNeedRoll(tv_receiveMessageShow) != 0) tv_receiveMessageShow.scrollTo(0, messageShowNeedRoll(tv_receiveMessageShow));
                tv_receiveMessageShow.append("[RX]"+gteNowDate()+": "+msg.obj.toString()+"\n");
                break;
        }
    }

    /**
     * 返回格式化后的当前时间
     * @return 当前时间字符串形式
     */
    private String gteNowDate()
    {
        SimpleDateFormat sdf = new SimpleDateFormat();// 格式化时间
        sdf.applyPattern("HH:mm:ss");// 时：分：秒
        Date date = new Date();// 获取当前时间戳
        return sdf.format(date); //返回格式化后的时间戳
    }

    /**
     * 判断当前TextView是否已经填充满控件可使用高度，如果高度已满就滚动需要的距离高度
     * @param textView 需要判断的TextView控件
     * @return 已满就返回对应高度，否则就返回0
     */
    private int messageShowNeedRoll(TextView textView)
    {
        int offset = textView.getLineCount() * textView.getLineHeight(); //添加的textview数量 x 字体高度
        if (offset > textView.getHeight()) return offset - tv_receiveMessageShow.getHeight(); //如果乘积大于控件高度就返回需要滚动的距离
        else return 0; //小于就返回0
    }

}
