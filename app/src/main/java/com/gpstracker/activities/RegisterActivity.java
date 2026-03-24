package com.gpstracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.gpstracker.R;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmailReg, etPasswordReg;
    private Button btnRegister;
    private TextView tvLoginLink;

    private FirebaseAuth mAuth;
    // ફેરફાર: Firestore ની જગ્યાએ હવે Realtime DatabaseReference વપરાશે
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Firebase સેટઅપ
        mAuth = FirebaseAuth.getInstance();
        // "Users" નામનું મુખ્ય નોડ (Node) બનાવવા માટે
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        // XML આઈડી કનેક્ટ કરો
        etName = findViewById(R.id.etName);
        etEmailReg = findViewById(R.id.etEmailReg);
        etPasswordReg = findViewById(R.id.etPasswordReg);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        // Login લિંક પર ક્લિક કરવાથી Login પેજ પર પાછા જશે
        tvLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Register બટન પર ક્લિક કરવાથી
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewUser();
            }
        });
    }

    private void createNewUser() {
        String name = etName.getText().toString().trim();
        String email = etEmailReg.getText().toString().trim();
        String password = etPasswordReg.getText().toString().trim();

        // વેલિડેશન ચેક
        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmailReg.setError("Email is required");
            return;
        }
        if (password.length() < 6) {
            etPasswordReg.setError("Password must be >= 6 Characters");
            return;
        }

        // Firebase Auth માં નવું એકાઉન્ટ બનાવો
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Auth માં એકાઉન્ટ બની ગયું, હવે Realtime Database માં ડેટા સેવ કરો
                        String userId = mAuth.getCurrentUser().getUid();
                        saveUserDataToDatabase(userId, name, email);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Realtime Database માં ડેટા સેવ કરવા માટેનું ફંક્શન
    private void saveUserDataToDatabase(String userId, String name, String email) {
        com.gpstracker.models.UserModel user = new com.gpstracker.models.UserModel(userId, name, email);

        // Realtime Database માં ડેટા મોકલવાની રીત (.child().setValue())
        mDatabase.child(userId).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();
                    // રજિસ્ટ્રેશન પૂરું થયા પછી લૉગિન પેજ પર મોકલો
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}