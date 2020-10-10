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
    private Timer timer;
    private TimerTask timerTask;

    private UsbCDC usbCDC;
    private MyHandler myHandler;
    private UsbMonitor usbMonitor;
    private TextView m_tv_sendMessageShow,m_tv_receiveMessageShow,m_tv_usbDataShow,m_tv_porterShow;
    private EditText m_et_messageText,m_et_time;
    private Button m_bt_send,m_bt_sendTiming,m_bt_clean,m_tv_porterSet;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();

        m_bt_send.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        usbCDC.send(m_et_messageText.getText().toString());
                    }
                }).start();
            }
        });

        m_bt_clean.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                m_tv_sendMessageShow.setText("");
                m_tv_receiveMessageShow.setText("");
                m_tv_sendMessageShow.scrollTo(0, 0);
                m_tv_receiveMessageShow.scrollTo(0, 0);
            }
        });

        m_bt_sendTiming.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (timer != null)
                {
                    timer.cancel();
                    timer = null;
                    m_bt_sendTiming.setTextColor(Color.BLACK);
                    m_bt_sendTiming.setText("定时发送");
                }
                else
                {
                    if (!m_et_time.getText().toString().equals(""))
                    {
                        timer = new Timer();
                        timerTask = new TimerTask()
                        {
                            @Override
                            public void run()
                            {
                                usbCDC.send(m_et_messageText.getText().toString());
                            }
                        };
                        new Thread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                timer.schedule(timerTask,0,Integer.parseInt(m_et_time.getText().toString()));
                            }
                        }).start();
                        m_bt_sendTiming.setTextColor(Color.RED);
                        m_bt_sendTiming.setText("停止");
                    }
                    else
                    {
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
                setPorter();
            }
        });
    }

    @Override
    public void onDeviceInsert(UsbMonitor usbMonitor, UsbManager usbManager, UsbDevice usbDevice)
    {
        usbMonitor.requestOpenDevice(usbDevice);
    }

    @Override
    public void onDevicePullOut(UsbMonitor usbMonitor, UsbManager usbManager, UsbDevice usbDevice)
    {
       closeAll();
       myHandler.sendEmptyMessage(MyHandler.USB_CONNECT_FAILED);
    }

    @Override
    public void onDeviceOpen(UsbMonitor usbMonitor, UsbManager usbManager, UsbDevice usbDevice)
    {
        usbCDC = new UsbCDC(myHandler);
        UsbDeviceConnection connection = usbManager.openDevice(usbDevice);
        usbCDC.openCDC(usbDevice, connection);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        closeAll();
        usbMonitor.unregister();
        myHandler.sendEmptyMessage(MyHandler.USB_CONNECT_FAILED);
    }

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
               if (usbCDC != null)
               {
                   boolean porter = usbCDC.configUsb(Integer.parseInt(items[which]));
                   m_tv_porterShow.setText(porter ? "波特率："+items[which]:"波特率：9600");
               }
               else
               {
                   Toast.makeText(MainActivity.this,"设备未连接",Toast.LENGTH_LONG).show();
               }
            }
        });
        listDialog.show();
    }

    private void initData()
    {
        myHandler = new MyHandler(m_tv_sendMessageShow,m_tv_receiveMessageShow,m_tv_usbDataShow,m_bt_send,m_bt_sendTiming,this,MainActivity.this);
        usbMonitor = new UsbMonitor(this,this,m_tv_usbDataShow);
        usbMonitor.register();
    }

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