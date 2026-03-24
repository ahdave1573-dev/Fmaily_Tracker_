package com.gpstracker.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.gpstracker.R;
import com.gpstracker.leader.CreateGroupActivity;
import com.gpstracker.leader.ManageMembersActivity;
import com.gpstracker.member.MyGroupsActivity;

public abstract class BaseActivity extends AppCompatActivity {

    protected BottomNavigationView bottomNavigationView;

    // --- Bottom Navigation Setup ---
    protected void setupBottomNavigation(int selectedItemId) {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(selectedItemId);
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == selectedItemId) return true;

                Intent intent = null;
                if (id == R.id.nav_home) intent = new Intent(this, UnifiedMapActivity.class);
                else if (id == R.id.nav_create_group) intent = new Intent(this, CreateGroupActivity.class);
                else if (id == R.id.nav_manage_members) intent = new Intent(this, ManageMembersActivity.class);
                else if (id == R.id.nav_my_groups) intent = new Intent(this, MyGroupsActivity.class);
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }
    }

    // --- Drawer Header Setup (AH Photo Fix) ---
    protected void setupNavigationDrawerHeader() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null && navigationView.getHeaderCount() > 0) {
            View headerView = navigationView.getHeaderView(0);

            headerView.setOnClickListener(v -> {
                Intent intent = new Intent(this, ProfileSettingsActivity.class);
                startActivity(intent);

                androidx.drawerlayout.widget.DrawerLayout drawer = findViewById(R.id.drawer_layout);
                if (drawer != null) {
                    drawer.closeDrawer(androidx.core.view.GravityCompat.START);
                }
            });

            ImageView imgNavProfile = headerView.findViewById(R.id.imgNavProfile);
            TextView tvNavName = headerView.findViewById(R.id.tvNavName);
            TextView tvNavEmail = headerView.findViewById(R.id.tvNavEmail);

            String uid = FirebaseAuth.getInstance().getUid();
            if (uid != null) {
                FirebaseDatabase.getInstance().getReference("Users").child(uid)
                        .addValueEventListener(new ValueEventListener() {
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
                                    tvNavName.setText(displayName);
                                    if (email != null && !email.isEmpty()) tvNavEmail.setText(email);

                                    if (imageUrl != null && !imageUrl.isEmpty()) {
                                        com.bumptech.glide.Glide.with(BaseActivity.this)
                                                .load(imageUrl)
                                                .circleCrop()
                                                .into(imgNavProfile);
                                    } else {
                                        Bitmap initialsBitmap = getCircularBitmapWithInitials(displayName);
                                        imgNavProfile.setImageBitmap(initialsBitmap);
                                    }
                                }
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });
            }
        }
    }
    private Bitmap getCircularBitmapWithInitials(String name) {
        int size = 160;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        paint.setColor(Color.parseColor("#4CAF50"));
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        // White Initials
        paint.setColor(Color.WHITE);
        paint.setTextSize(65f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);

        String initials = "";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            initials = (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        } else if (name.length() >= 2) {
            initials = name.substring(0, 2).toUpperCase();
        } else {
            initials = name.toUpperCase();
        }

        Paint.FontMetrics fm = paint.getFontMetrics();
        float yPos = (size / 2f) - ((fm.ascent + fm.descent) / 2f);
        canvas.drawText(initials, size / 2f, yPos, paint);

        return bitmap;
    }
}