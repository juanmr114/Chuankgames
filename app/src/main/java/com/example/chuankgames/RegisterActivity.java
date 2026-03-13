package com.example.chuankgames;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registro_view);

        TextView tvVolverLogin=findViewById(R.id.tvVolverLogin);
        tvVolverLogin.setOnClickListener(v->{
            Intent intent=new Intent(RegisterActivity.this, MainActivity.class);
            startActivity(intent);
        });


    }
}
