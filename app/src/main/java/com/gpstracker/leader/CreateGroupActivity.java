package com.gpstracker.leader;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.gpstracker.R;
import com.gpstracker.activities.BaseActivity;
import com.gpstracker.activities.UnifiedMapActivity;
import com.gpstracker.models.GroupModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CreateGroupActivity extends BaseActivity {

    private EditText etGroupName, etGroupPassword;
    private Button btnSaveGroup;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        // ૧. Navigation સેટઅપ
        setupBottomNavigation(R.id.nav_create_group); // આ BaseActivity માંથી આવશે
        setupNavigationDrawerHeader(); // આનાથી 'AD' લોગો અને પ્રોફાઈલ ડેટા ડ્રોઅરમાં દેખાશે

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        etGroupName = findViewById(R.id.etGroupName);
        etGroupPassword = findViewById(R.id.etGroupPassword);
        btnSaveGroup = findViewById(R.id.btnSaveGroup);

        btnSaveGroup.setOnClickListener(v -> saveGroupToFirebase());
    }

    private String generateRandomCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private void saveGroupToFirebase() {
        String groupName = etGroupName.getText().toString().trim();
        String groupPassword = etGroupPassword.getText().toString().trim();

        if (TextUtils.isEmpty(groupName)) {
            etGroupName.setError("Group Name is required!");
            return;
        }
        if (TextUtils.isEmpty(groupPassword)) {
            etGroupPassword.setError("Password is required!");
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        String groupCode = generateRandomCode(); // Using random code as a readable identifier
        String groupId = mDatabase.child("Groups").push().getKey();

        if (groupId == null) return;

        GroupModel group = new GroupModel(groupId, groupName, groupCode, groupPassword, userId);
        Map<String, Boolean> members = new HashMap<>();
        members.put(userId, true);
        group.setMembers(members);

        mDatabase.child("Groups").child(groupId).setValue(group)
                .addOnSuccessListener(aVoid -> {
                    // Update user's createdGroups and joinedGroups
                    mDatabase.child("Users").child(userId).child("createdGroups").child(groupId).setValue(true);
                    mDatabase.child("Users").child(userId).child("joinedGroups").child(groupId).setValue(true);

                    Toast.makeText(CreateGroupActivity.this, "Group Created Successfully!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreateGroupActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}