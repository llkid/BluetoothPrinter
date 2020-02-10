package com.android.bluetoothprinter;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.android.bluetoothprinter.PrintBean.PRINT_TYPE;

public class PrintText extends AppCompatActivity implements View.OnClickListener {

    private TextView showText;
    private EditText inputText;
    private Button sendBtn;
    private Button printBtn;
    private PrintUtil printUtil;
    private static String textOnView = "";
    private BluetoothAdapter mBluetoothAdapter;

    private static OutputStream outputStream = null;

    //匹配过的设备列表
    private List<String> mpairedDeviceList = new ArrayList<String>();

    private BluetoothDevice mBluetoothDevice = null;

    private BluetoothSocket mBluetoothSocket = null;

    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //set得到搜索到的所有设备信息
    Set<BluetoothDevice> pairedDevices = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print_text);
        ActivityCollector.addActivity(this);

        showText = (TextView) findViewById(R.id.text_view);
        inputText = (EditText) findViewById(R.id.input_text);
        sendBtn = (Button) findViewById(R.id.send);
        printBtn = (Button) findViewById(R.id.text_print);
        sendBtn.setOnClickListener(this);
        printBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send:
                textOnView += getText();
                showText.setText(textOnView);
                inputText.setText("");
                break;
            case R.id.text_print:
                if (!"".equals(textOnView)) {
                    startPrintText(textOnView);
                } else {
                    Toast.makeText(this, "请输入文本", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    private String getText() {
        String content = inputText.getText().toString();
        if (!"".equals(content)) {
            return content;
        }
        return "";
    }

    /**
     * 连接打印设备
     * @return 返回连接状态
     */
    private boolean connectPrinter(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter.isEnabled()) {
            String getName;
            pairedDevices = mBluetoothAdapter.getBondedDevices(); // 已经配对了的设备？
            while (mpairedDeviceList.size() > 1) {
                mpairedDeviceList.remove(1);
            }
            if (pairedDevices.size() == 0) {
                return false;  // 没有配对过的设备
            }
            for (BluetoothDevice device : pairedDevices) {
                getName = device.getAddress();
                mpairedDeviceList.add(getName);//蓝牙名
            }
            String temString = mpairedDeviceList.get(0);
            mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(temString);
            if (mBluetoothDevice.getBluetoothClass().getDeviceClass() != PRINT_TYPE) {
                Toast.makeText(this, "不是蓝牙设备", Toast.LENGTH_SHORT).show();
                return false;
            }
            try {
                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(SPP_UUID);
                mBluetoothSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "蓝牙未连接", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * 打印方法
     * @param sendData 输入的文本
     */
    private void startPrintText(String sendData) {
        if (connectPrinter()) {
            Toast.makeText(this, "printing...", Toast.LENGTH_SHORT).show();

            try {
                outputStream = mBluetoothSocket.getOutputStream();
                byte[] data = sendData.getBytes("gbk");
                outputStream.write(data, 0, data.length);
                //切纸
                outputStream.write(new byte[]{0x0a, 0x0a, 0x1d, 0x56, 0x01});
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();

            }
        } else {
            Toast.makeText(this, "打印设备连接失败", Toast.LENGTH_SHORT).show();
        }
    }

}
