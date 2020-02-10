package com.android.bluetoothprinter;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.android.bluetoothprinter.PrintBean.PRINT_TYPE;


/**
 * 类说明: 打印数据类
 * shi-bash-cmd  2020/02/07
 */
public class PrintUtil {

    private String TAG = "printutil";

    private final static int LINE_BYTE_SIZE = 32;

    private static OutputStream outputStream = null;

    //匹配过的设备列表
    private List<String> mpairedDeviceList = new ArrayList<String>();

    private BluetoothAdapter mBluetoothAdapter = null;

    private BluetoothDevice mBluetoothDevice = null;

    private PrintBean mPrintBean = null;

    private BluetoothSocket mBluetoothSocket = null;

    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //set得到搜索到的所有设备信息
    Set<BluetoothDevice> pairedDevices = null;

    private ProgressDialog progressDialog = null;

    public void setOutputStream(OutputStream outputStream) {
        PrintUtil.outputStream = outputStream;
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
//                Toast.makeText(this, "不是蓝牙设备", Toast.LENGTH_SHORT).show();
                return false;
            }
            try {
                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(SPP_UUID);
                mBluetoothSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
//            Toast.makeText(this, "蓝牙未连接", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * 打印图片接口
     * @param bitmap bitmap转换成byte[] 类型的数据
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void startPrintImage(Bitmap bitmap) {
        if (mBluetoothDevice.getBluetoothClass().getDeviceClass() != PRINT_TYPE) {
            Log.e(TAG, "不是打印设备");
            return;
        }
        if(mBluetoothSocket == null){
            connectPrinter();
        } else {
            if(!mBluetoothSocket.isConnected()){
                try {
                    mBluetoothSocket.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG, "printVerifyData(),mBluetoothSocket.isConnected.");
            }
        }

        try {
            outputStream = mBluetoothSocket.getOutputStream();
            //这里是打印操作
            imageInit(outputStream, bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打印方法
     * @param sendData 输入的文本
     */
    public void startPrintText(String sendData) {
        if (connectPrinter()) {
//            Toast.makeText(this, "printing...", Toast.LENGTH_SHORT).show();

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
//            Toast.makeText(this, "打印设备连接失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 图片数据处理及打印操作
     * @param outputStream
     * @param bitmap
     */
    private void imageInit(OutputStream outputStream, Bitmap bitmap) {
        try {
            PrintUtil printUtil = new PrintUtil();
            printUtil.setOutputStream(outputStream);
            printUtil.selectCommand(PrintUtil.RESET);
            printUtil.selectCommand(PrintUtil.NORMAL);

            Bitmap tempBitmap = printUtil.convertGreyImgByFloyd(bitmap);
            byte[] imgData = printUtil.bitmap2Bytes(tempBitmap, Bitmap.CompressFormat.JPEG);

            outputStream.write(imgData, 0, imgData.length);
            //切纸
            outputStream.write(new byte[]{0x0a, 0x0a, 0x1d, 0x56, 0x01});
            outputStream.flush();
            outputStream.close();
            progressDialog.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送数据 打印文本
     */
    public void printText(String text) {
        try {
            byte[] data = text.getBytes("gbk");
            outputStream.write(data, 0, data.length);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 输出数据
     * @param command 格式指令
     */
    public void selectCommand(byte[] command) {
        try {
            outputStream.write(command);
            outputStream.flush();
        } catch (IOException e) {
            //Toast.makeText(this.context, "发送失败！", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * 复位打印机
     */
    public static final byte[] RESET = {0x1b, 0x40};

    /**
     * 左对齐
     */
    public static final byte[] ALIGN_LEFT = {0x1b, 0x61, 0x00};

    /**
     * 中间对齐
     */
    public static final byte[] ALIGN_CENTER = {0x1b, 0x61, 0x01};

    /**
     * 选择加粗模式
     */
    public static final byte[] BOLD = {0x1b, 0x45, 0x01};

    /**
     * 取消加粗模式
     */
    public static final byte[] BOLD_CANCEL = {0x1b, 0x45, 0x00};

    /**
     * 宽高加倍
     */
    public static final byte[] DOUBLE_HEIGHT_WIDTH = {0x1d, 0x21, 0x11};

    /**
     * 字体不放大
     */
    public static final byte[] NORMAL = {0x1d, 0x21, 0x00};

    /**
     * 设置默认行间距
     */
    public static final byte[] LINE_SPACING_DEFAULT = {0x1b, 0x32};

    //    /**
//     * 设置行间距
//     */
    public static final byte[] LINE_SPACING = {0x1b, 0x33, 0x50};  // 20的行间距（0，255）

    /**
     * 设置字符间距
     */
    public static final byte[] COLUMN_SPACING = {0x1b, 0x20, 0x25};

    /**
     * 取消设置字符间距
     */
    public static final byte[] COLUMN_SPACING_CANCEL = {0x1b, 0x20, 0x00};

    /**
     * 打印两列
     *
     * @param leftText  左侧文字
     * @param rightText 右侧文字
     * @return
     */
    @SuppressLint("NewApi")
    public static String printTwoData(String leftText, String rightText) {
        StringBuilder sb = new StringBuilder();
        int leftTextLength = getBytesLength(leftText);
        int rightTextLength = getBytesLength(rightText);
        sb.append(leftText);

        // 计算两侧文字中间的空格
        int marginBetweenMiddleAndRight = LINE_BYTE_SIZE - leftTextLength - rightTextLength;

        for (int i = 0; i < marginBetweenMiddleAndRight; i++) {
            sb.append(" ");
        }
        sb.append(rightText);
        return sb.toString();
    }

    /**
     * 获取数据长度
     * @param msg
     * @return
     */
    @SuppressLint("NewApi")
    private static int getBytesLength(String msg) {
        return msg.getBytes(Charset.forName("GB2312")).length;
    }

    /**
     * 图片处理
     * @param img 传入的bitmap图片源
     * @return 返回处理好的图片
     */
    public Bitmap convertGreyImgByFloyd(Bitmap img) {
        int width = img.getWidth();
        //获取位图的宽
        int height = img.getHeight();
        //获取位图的高 \
        int[] pixels = new int[width * height];
        //通过位图的大小创建像素点数组
        img.getPixels(pixels, 0, width, 0, 0, width, height);
        int[] gray = new int[height * width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];
                int red = ((grey & 0x00FF0000) >> 16);
                gray[width * i + j] = red;
            }
        }
        int e = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int g = gray[width * i + j];
                if (g >= 128) {
                    pixels[width * i + j] = 0xffffffff;
                    e = g - 255;
                } else {
                    pixels[width * i + j] = 0xff000000;
                    e = g - 0;
                }
                if (j < width - 1 && i < height - 1) {
                    //右边像素处理
                    gray[width * i + j + 1] += 3 * e / 8;
                    //下
                    gray[width * (i + 1) + j] += 3 * e / 8;
                    //右下
                    gray[width * (i + 1) + j + 1] += e / 4;
                } else if (j == width - 1 && i < height - 1) {
                    //靠右或靠下边的像素的情况
                    //下方像素处理
                    gray[width * (i + 1) + j] += 3 * e / 8;
                } else if (j < width - 1 && i == height - 1) {
                    //右边像素处理
                    gray[width * (i) + j + 1] += e / 4;
                }
            }
        }
        Bitmap mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        mBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return mBitmap;
    }

    public byte[] bitmap2Bytes(final Bitmap bitmap, final Bitmap.CompressFormat format) {
        if (bitmap == null) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(format, 100, baos);

        return baos.toByteArray();
    }

}
