package com.android.bluetoothprinter;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class PrintText extends AppCompatActivity implements View.OnClickListener {

    private TextView showText;
    private EditText inputText;
    private Button sendBtn;
    private Button printBtn;
    private PrinterAdapter adapter;
    private static String textOnView = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print_text);

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
                if (textOnView != null) {
                    showText.setText(textOnView);
                    inputText.setText("");
                }
                break;
            case R.id.text_print:
                Toast.makeText(this, "printing...", Toast.LENGTH_SHORT).show();
                String stringText = textOnView;
                adapter.startPrintText(stringText);
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
        return null;
    }
}
