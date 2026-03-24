package com.gpstracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.gpstracker.R;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // ૨ સેકન્ડના હોલ્ડ પછી લોગિન સ્ટેટસ ચેક થશે (Splash Screen effect)
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                checkLoginStatus();
            }
        }, 2000);
    }

    private void checkLoginStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // જો યુઝર લોગિન ન હોય તો Login screen પર મોકલો
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        } else {
            // એડમિન ચેક કાઢી નાખ્યું છે, હવે દરેક યુઝર સીધો મેપ પર જશે
            startActivity(new Intent(MainActivity.this, UnifiedMapActivity.class));
        }
        finish();
    }
}