package com.gpstracker.activities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.gpstracker.R;

import java.util.ArrayList;
import java.util.List;

public class ProfileSettingsActivity extends BaseActivity {

    private ImageView imgProfile;
    private EditText etCurrentPassword, etNewPassword;
    private TextView tvCreatedCount, tvJoinedCount, tvCreatedGroupNames, tvJoinedGroupNames;
    private Button btnSaveProfile;

    private FirebaseAuth mAuth;
    private DatabaseReference mUserRef;
    private ValueEventListener userListener;
    private Uri selectedImageUri;

    // ગેલેરી ખોલવા માટે
    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    imgProfile.setImageURI(uri);
                    Toast.makeText(this, "Photo selected!", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        // Sidebar and Navigation Setup
        setupNavigationDrawerHeader();

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            mUserRef = FirebaseDatabase.getInstance().getReference("Users").child(mAuth.getCurrentUser().getUid());
        }

        // Initialize Views
        imgProfile = findViewById(R.id.imgProfile);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        tvCreatedCount = findViewById(R.id.tvCreatedCount);
        tvJoinedCount = findViewById(R.id.tvJoinedCount);
        tvCreatedGroupNames = findViewById(R.id.tvCreatedGroupNames);
        tvJoinedGroupNames = findViewById(R.id.tvJoinedGroupNames);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        loadUserData();

        // બટન વગર, સીધું ઈમેજ પર ક્લિક કરવાથી ફોટો બદલાશે
        imgProfile.setOnClickListener(v -> mGetContent.launch("image/*"));

        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void loadUserData() {
        if (mUserRef == null) return;

        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String imageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                    // Determine display name: use name, fallback to email prefix
                    String displayName = name;
                    if (displayName == null || displayName.trim().isEmpty()) {
                        if (email != null && email.contains("@")) {
                            displayName = email.split("@")[0];
                        } else {
                            displayName = email != null ? email : "User";
                        }
                    }

                    if (imageUrl != null && !imageUrl.isEmpty() && selectedImageUri == null) {
                        Glide.with(ProfileSettingsActivity.this)
                                .load(imageUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .into(imgProfile);
                    } else if (selectedImageUri == null) {
                        imgProfile.setImageBitmap(getCircularBitmapWithInitials(displayName));
                    }

                    long createdCount = snapshot.child("createdGroups").getChildrenCount();
                    long joinedCount = snapshot.child("joinedGroups").getChildrenCount();
                    tvCreatedCount.setText(String.valueOf(createdCount));
                    tvJoinedCount.setText(String.valueOf(joinedCount));

                    fetchGroupNames(snapshot.child("createdGroups"), tvCreatedGroupNames, "Created: ");
                    fetchGroupNames(snapshot.child("joinedGroups"), tvJoinedGroupNames, "Joined: ");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        mUserRef.addValueEventListener(userListener);
    }

    private void fetchGroupNames(DataSnapshot groupListSnapshot, TextView tv, String prefix) {
        if (!groupListSnapshot.exists()) {
            tv.setText(prefix + "None");
            return;
        }

        List<String> names = new ArrayList<>();
        long totalGroups = groupListSnapshot.getChildrenCount();

        for (DataSnapshot ds : groupListSnapshot.getChildren()) {
            String groupId = ds.getKey();
            if (groupId == null) continue;

            FirebaseDatabase.getInstance().getReference("Groups").child(groupId).child("groupName").get()
                    .addOnSuccessListener(snapshot -> {
                        String gName = snapshot.getValue(String.class);
                        if (gName != null) names.add(gName);
                        if (names.size() == totalGroups) {
                            tv.setText(prefix + TextUtils.join(", ", names));
                        }
                    });
        }
    }

    private void saveProfile() {
        String currentPass = etCurrentPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();

        if (!TextUtils.isEmpty(newPass)) {
            if (TextUtils.isEmpty(currentPass)) {
                etCurrentPassword.setError("Current password required");
                return;
            }
            changePassword(currentPass, newPass);
        }

        if (selectedImageUri != null) {
            uploadImageToFirebase();
        } else if (TextUtils.isEmpty(newPass)) {
            Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImageToFirebase() {
        StorageReference fileRef = FirebaseStorage.getInstance().getReference()
                .child("profile_images/" + mAuth.getUid() + ".jpg");

        fileRef.putFile(selectedImageUri).addOnSuccessListener(taskSnapshot ->
                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    mUserRef.child("profileImageUrl").setValue(uri.toString());
                    Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                    selectedImageUri = null;
                })
        ).addOnFailureListener(e -> Toast.makeText(this, "Failed to upload", Toast.LENGTH_SHORT).show());
    }

    private void changePassword(String current, String newP) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        AuthCredential cred = EmailAuthProvider.getCredential(user.getEmail(), current);
        user.reauthenticate(cred).addOnSuccessListener(aVoid ->
                user.updatePassword(newP).addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Password Updated!", Toast.LENGTH_SHORT).show();
                    etCurrentPassword.setText("");
                    etNewPassword.setText("");
                })
        ).addOnFailureListener(e -> etCurrentPassword.setError("Wrong password"));
    }

    protected Bitmap getCircularBitmapWithInitials(String name) {
        int size = 150;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setColor(Color.parseColor("#4CAF50"));
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(60f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);

        String initials = "";
        if (name != null && !name.isEmpty()) {
            String[] parts = name.trim().split("\\s+");
            initials = (parts.length >= 2)
                    ? (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase()
                    : name.substring(0, Math.min(name.length(), 2)).toUpperCase();
        }

        float yPos = (size / 2f) - ((paint.getFontMetrics().ascent + paint.getFontMetrics().descent) / 2f);
        canvas.drawText(initials, size / 2f, yPos, paint);
        return bitmap;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUserRef != null && userListener != null) {
            mUserRef.removeEventListener(userListener);
        }
    }
}