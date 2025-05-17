package com.example.kyc_kakaohackaton;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ImageButton imageButton;  // 이미지 버튼 객체

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  // activity_main.xml에 이미지 버튼이 있어야 함

        imageButton = findViewById(R.id.imagebutton);  // XML에 있는 ID와 동일하게

        imageButton.setOnClickListener(v -> {
            // MainActivity2로 전환
            Intent intent = new Intent(MainActivity.this, MainActivity2.class);
            startActivity(intent);
        });
    }
}
