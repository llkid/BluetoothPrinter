package com.android.bluetoothprinter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.android.bluetoothprinter.PrintBean.PRINT_TYPE;


/**
 * 显示并打印图片类
 * shi-bash-cmd 2020/02/07
 */
public class PrintImage extends AppCompatActivity implements View.OnClickListener {

    private String TAG = this.getClass().getSimpleName();

    private static final int TAKE_PHOTO = 1;
    private ImageView picture;
    private Uri imageUri;
    public static final int CHOOSE_PHOTO = 2;

    private Button printBtn;
    private boolean isHaveimg = false;
    private static OutputStream outputStream = null;

    //匹配过的设备列表
    private List<String> mpairedDeviceList = new ArrayList<String>();

    private BluetoothAdapter mBluetoothAdapter = null;

    private BluetoothDevice mBluetoothDevice = null;

    private BluetoothSocket mBluetoothSocket = null;

    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //set得到搜索到的所有设备信息
    Set<BluetoothDevice> pairedDevices = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print_image);
        ActivityCollector.addActivity(this);

        //这里要得到显示图片的实例，不然在后面设置的时候有可能发生闪退
        picture = (ImageView) findViewById(R.id.picture);

        //右上角的设置按钮
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_img);
        setSupportActionBar(toolbar);

        printBtn = (Button) findViewById(R.id.img_print);
        printBtn.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    /**
     * 点击相应按钮获取对应的方法
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.take_photo:
                Toast.makeText(this, "You clicked take_photo", Toast.LENGTH_SHORT).show();
                takePhoto();
                isHaveimg = true;
                break;
            case R.id.choose_from_album:
                Toast.makeText(this, "You clicked choose_from_album", Toast.LENGTH_SHORT).show();
                if (ContextCompat.checkSelfPermission(PrintImage.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(PrintImage.this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                } else {
                    openAlbum();
                }
                break;
            case R.id.back_up:
                Toast.makeText(this, "You clicked back_up", Toast.LENGTH_SHORT).show();
                finish();
                break;
            default:
                break;
        }
        return true;
    }

    private void takePhoto() {
        File outputImage = new File(getExternalCacheDir(), "output_image.jpg");
        try {
            if (outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            imageUri = FileProvider.getUriForFile(PrintImage.this, "com.android.bluetoothprinter.fileprovider", outputImage);
        } else {
            imageUri =Uri.fromFile(outputImage);
        }

        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, TAKE_PHOTO);
    }

    /**
     *  打开手机相册
     */
    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_PHOTO);//打开相册
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openAlbum();
                } else {
                    Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        picture.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case CHOOSE_PHOTO:
                if (resultCode == RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        handleImageOnKitKat(data);
                    } else {
                        handleImageBeforeKitKat(data);
                    }
                }
                break;
            default:
                break;
        }
    }

    //需要api 19 以上
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void handleImageOnKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();

        if (DocumentsContract.isDocumentUri(this, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);

            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            imagePath = uri.getPath();
        }

        displayImage(imagePath);
    }

    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        displayImage(imagePath);
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        //通过Uri和selection来获取真实的图片路径
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    public  Uri getImageContentUri(String path) {
        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Images.Media._ID }, MediaStore.Images.Media.DATA + "=? ",
                new String[] { path }, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            cursor.close();
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            // 如果图片不在手机的共享图片数据库，就先把它插入。
            if (new File(path).exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, path);
                return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    private void displayImage(String imagePath) {
        if (imagePath != null) {
//            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
//            picture.setImageBitmap(bitmap);
            picture.setImageURI(getImageContentUri(imagePath));
            isHaveimg = true;
        } else {
            Toast.makeText(this, "Failed to get image", Toast.LENGTH_SHORT).show();
        }
    }

    /*private Drawable.ConstantState imageState() {
        Drawable.ConstantState state = picture.getDrawable().getCurrent().getConstantState();
        return state;
    }*/

    private Bitmap getBitmap(ImageView img) {
        Bitmap bitmap = ((BitmapDrawable)img.getDrawable()).getBitmap();
        return bitmap;
    }

    /**
     * 将获取的图片转化成bitmap形式传给imageUtil类
     * @param v button id
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.img_print:
                //将图片数据传输到打印方法中 bitmap
                if (isHaveimg) {
                    startPrintImage(getBitmap(picture));
                    Bitmap bitmap = getBitmap(picture);
                    PrintUtil printUtil = new PrintUtil();
                    byte[] tempData = printUtil.bitmap2Bytes(bitmap, Bitmap.CompressFormat.JPEG);

                    Intent intent = new Intent(this, ImageActivity.class);
                    intent.putExtra("image", tempData);
                    startActivity(intent);
                } else {
                    Toast.makeText(PrintImage.this, "没有图片可以打印", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    /**
     * 打印图片接口
     * @param bitmap bitmap转换成byte[] 类型的数据
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void startPrintImage(Bitmap bitmap) {
        if (connectPrinter()) {
            try {
                outputStream = mBluetoothSocket.getOutputStream();
                Toast.makeText(PrintImage.this, "printing...", Toast.LENGTH_SHORT).show();
                //这里是打印操作
                imageInit(outputStream, bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "打印设备连接失败", Toast.LENGTH_SHORT).show();
        }
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
