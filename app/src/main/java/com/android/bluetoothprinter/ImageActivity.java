package com.android.bluetoothprinter;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

public class ImageActivity extends AppCompatActivity {

    private Bitmap bitmap;
    private Intent intent;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        ActivityCollector.addActivity(this);

        imageView = (ImageView) findViewById(R.id.imageView);
        intent = getIntent();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (intent != null) {
                    byte[] buff = intent.getByteArrayExtra("image");
//                    bitmap = intent.getParcelableExtra("image");
//                    Log.e("TAG", String.valueOf(bitmap));
                    assert buff != null;
                    bitmap = BitmapFactory.decodeByteArray(buff, 0, buff.length);
                    imageView.setImageBitmap(bitmap);
                } else {
                    imageView.setImageResource(R.drawable.img_1);
                }
            }
        });
    }
}
