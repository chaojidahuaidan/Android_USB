package com.github.sunlong6666.usb_host;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements UsbController
{
    private Timer timer; //计时器对象
    private TimerTask timerTask; //计时器任务对象

    private UsbCDC usbCDC; //当前连接的USB设备对象
    private MyHandler myHandler; //消息处理中心对象
    private UsbMonitor usbMonitor; //USB监听广播对象
    private TextView m_tv_sendMessageShow,m_tv_receiveMessageShow,m_tv_usbDataShow,m_tv_porterShow;
    private EditText m_et_messageText,m_et_time;
    private Button m_bt_send,m_bt_sendTiming,m_bt_clean,m_tv_porterSet;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView(); //实例化当前页面控件
        initData(); //加载初始数据

        m_bt_send.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //开启新线程进行数据发送
                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        usbCDC.send(m_et_messageText.getText().toString()); //向当前连接的USB设备发送消息
                    }
                }).start();
            }
        });

        m_bt_clean.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                m_tv_sendMessageShow.setText(""); //清除发送的消息文本
                m_tv_receiveMessageShow.setText(""); //清除接收的消息文本
                m_tv_sendMessageShow.scrollTo(0, 0); //发送的消息文本回滚到最顶部
                m_tv_receiveMessageShow.scrollTo(0, 0); //接收的消息文本回滚到最顶部
            }
        });

        m_bt_sendTiming.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //判断计时器是否为空，不为就代表正在执行计时任务，就停止当前任务
                if (timer != null)
                {
                    timer.cancel(); //停止计时器
                    timer = null; //计时器设为空
                    m_bt_sendTiming.setTextColor(Color.BLACK); //改变定时发送控件的颜色
                    m_bt_sendTiming.setText("定时发送"); //控件 恢复为“定时发送”
                }
                else
                {
                    //如果为空，就开始定时发送任务
                    if (!m_et_time.getText().toString().equals("")) //获取定时任务的时间间隔
                    {
                        timer = new Timer(); //创建定时器
                        timerTask = new TimerTask() //创建定时任务
                        {
                            @Override
                            public void run()
                            {   
                                //定时任务要执行的内容
                                usbCDC.send(m_et_messageText.getText().toString());
                            }
                        };
                        new Thread(new Runnable() //开启新线程执行 开启计时器
                        {
                            @Override
                            public void run()
                            {
                                //开启计时器
                                timer.schedule(timerTask,0,Integer.parseInt(m_et_time.getText().toString()));
                            }
                        }).start();
                        m_bt_sendTiming.setTextColor(Color.RED);
                        m_bt_sendTiming.setText("停止"); //计时器开始后“定时发送”控件就改变颜色和字体
                    }
                    else
                    {
                        //判断计时器是否为空，如果为空，就执行该作用域内容
                        Toast.makeText(MainActivity.this,"定时不能为空",Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        m_tv_porterSet.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //展示设置波特率的diaog对象
                setPorter();
            }
        });
    }

    @Override
    public void onDeviceInsert(UsbMonitor usbMonitor, UsbManager usbManager, UsbDevice usbDevice)
    {
        usbMonitor.requestOpenDevice(usbDevice); //请求USB连接权限
    }

    @Override
    public void onDevicePullOut(UsbMonitor usbMonitor, UsbManager usbManager, UsbDevice usbDevice)
    {
       closeAll(); //执行关闭所有连接的方法
       myHandler.sendEmptyMessage(MyHandler.USB_CONNECT_FAILED); //向消息中心发送 断开连接 信息
    }

    @Override
    public void onDeviceOpen(UsbMonitor usbMonitor, UsbManager usbManager, UsbDevice usbDevice)
    {
        usbCDC = new UsbCDC(myHandler); //创建USB连接的对象
        UsbDeviceConnection connection = usbManager.openDevice(usbDevice); //获取此USB链路
        usbCDC.openCDC(usbDevice, connection); //连接USB设备（打开USB设备）
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        closeAll();//执行关闭所有连接的方法
        myHandler.sendEmptyMessage(MyHandler.USB_CONNECT_FAILED);//向消息中心发送 断开连接 信息
    }

    //关闭所有连接
    public void closeAll()
    {
        if (usbCDC != null)
        {
            usbCDC.close();
            usbCDC = null;
        }
        if (timer != null)
        {
            timer.cancel();
            timer = null;
        }
    }

    //展示设置波特率dialog的对象，用一个AlertDialog让用户进行选择比特率
    private void setPorter()
    {
        final String[] items = {"2400","4800","9600","19200","38400","57600","115200","230400","460800","1700000","2300000","3400000"};
        AlertDialog.Builder listDialog = new AlertDialog.Builder(MainActivity.this);
        listDialog.setTitle("设置波特率");
        listDialog.setItems(items, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                //判断当前USB设备是否连接，连接之后才可以设置波特率
               if (usbCDC != null)
               {
                   boolean porter = usbCDC.configUsb(Integer.parseInt(items[which])); //执行设置波特率的对象，返回true代表设置成功
                   m_tv_porterShow.setText(porter ? "波特率："+items[which]:"波特率：9600"); //设置波特率对象返回true才改变控件字体，否则设置失败，不改变控件字体
               }
               else
               {
                    //判断当前USB设备是否连接，未连接则提示 设备未连接
                   Toast.makeText(MainActivity.this,"设备未连接",Toast.LENGTH_LONG).show();
               }
            }
        });
        listDialog.show(); //展示dialog
    }

    //加载数据
    private void initData()
    {
        myHandler = new MyHandler(m_tv_sendMessageShow,m_tv_receiveMessageShow,m_tv_usbDataShow,m_bt_send,m_bt_sendTiming,this,MainActivity.this); //实例化消息处理中心
        usbMonitor = new UsbMonitor(this,this,m_tv_usbDataShow); //实例化USB广播监听
        usbMonitor.register(); //注册USB广播监听，注册之后，才可以正常监听USB设备
    }

    //实例化控件
    private void initView()
    {
        m_tv_receiveMessageShow = (TextView)findViewById(R.id.m_tv_receiveMessageShow);
        m_tv_receiveMessageShow.setMovementMethod(ScrollingMovementMethod.getInstance());
        m_tv_sendMessageShow = (TextView)findViewById(R.id.m_tv_sendMessageShow);
        m_tv_sendMessageShow.setMovementMethod(ScrollingMovementMethod.getInstance());
        m_tv_usbDataShow = (TextView)findViewById(R.id.m_tv_usbDataShow);
        m_et_messageText = (EditText)findViewById(R.id.m_et_messageText);
        m_et_time = (EditText)findViewById(R.id.m_et_time);
        m_bt_sendTiming = (Button)findViewById(R.id.m_bt_sendTiming);
        m_bt_clean = (Button)findViewById(R.id.m_bt_clean);
        m_bt_send = (Button)findViewById(R.id.m_bt_send);
        m_tv_porterShow = (TextView)findViewById(R.id.m_tv_porterShow);
        m_tv_porterSet = (Button)findViewById(R.id.m_bt_porterSet);
    }

}
