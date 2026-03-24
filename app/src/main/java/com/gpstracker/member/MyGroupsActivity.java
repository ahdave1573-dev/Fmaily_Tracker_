package com.gpstracker.member;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.gpstracker.R;
import com.gpstracker.activities.BaseActivity;
import com.gpstracker.adapters.GroupAdapter;
import com.gpstracker.models.GroupModel;
import com.gpstracker.models.UserModel;

import java.util.ArrayList;
import java.util.List;

public class MyGroupsActivity extends BaseActivity {

    private RecyclerView rvMyGroups;
    private LinearLayout emptyStateLayout;
    private Button btnExplore;
    private GroupAdapter adapter;
    private List<GroupModel> groupList;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_groups);

        // --- 1. Navigation Drawer & Bottom Nav Setup ---
        // BaseActivity માં રહેલી મેથડ્સનો ઉપયોગ
        setupNavigationDrawerHeader();
        setupBottomNavigation(R.id.nav_my_groups);

        // Toolbar સેટઅપ (Drawer ખોલવા માટે)
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false); // HeaderTitle આપણે જાતે આપ્યું છે
        }

        // Drawer toggle બટન (ત્રણ લીટા વાળું મેનુ) સેટ કરવા માટે
        toolbar.setNavigationOnClickListener(v -> {
            // BaseActivity માં જો DrawerLayout ડિકલેર હોય તો તે ખોલશે
            openDrawer();
        });

        // --- 2. Firebase & UI Initialization ---
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        rvMyGroups = findViewById(R.id.rvMyGroups);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        btnExplore = findViewById(R.id.btnExplore);

        rvMyGroups.setLayoutManager(new LinearLayoutManager(this));
        groupList = new ArrayList<>();

        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        adapter = new GroupAdapter(this, groupList, userId);
        rvMyGroups.setAdapter(adapter);

        if (btnExplore != null) {
            btnExplore.setOnClickListener(v -> {
                Toast.makeText(this, "Explore Groups Clicked!", Toast.LENGTH_SHORT).show();
                // Intent intent = new Intent(this, JoinGroupActivity.class);
                // startActivity(intent);
            });
        }

        loadMyGroups();
    }
    public void openDrawer() {
        androidx.drawerlayout.widget.DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer != null) {
            drawer.openDrawer(androidx.core.view.GravityCompat.START);
        }
    }

    private void loadMyGroups() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();

        mDatabase.child("Users").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        groupList.clear();
                        if (snapshot.exists()) {
                            UserModel user = snapshot.getValue(UserModel.class);
                            if (user != null && user.getJoinedGroups() != null && !user.getJoinedGroups().isEmpty()) {
                                long totalGroups = user.getJoinedGroups().size();
                                final long[] loadedCount = {0};
                                java.util.Map<String, Object> sharing = user.getSharingStatus();

                                for (String groupId : user.getJoinedGroups().keySet()) {
                                    if (groupId != null) {
                                        boolean isSharing = false;
                                        if (sharing != null && sharing.containsKey(groupId)) {
                                            isSharing = Boolean.TRUE.equals(sharing.get(groupId));
                                        }
                                        fetchGroupDetails(groupId, totalGroups, loadedCount, isSharing);
                                    }
                                }
                            } else {
                                showEmptyState();
                            }
                        } else {
                            showEmptyState();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MyGroupsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchGroupDetails(String groupId, long totalGroups, long[] loadedCount, boolean isSharing) {
        mDatabase.child("Groups").child(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    GroupModel group = snapshot.getValue(GroupModel.class);
                    if (group != null) {
                        group.isSharingLocation = isSharing;
                        groupList.add(group);
                    }
                }
                loadedCount[0]++;
                checkEmptyState(totalGroups, loadedCount[0]);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadedCount[0]++;
                checkEmptyState(totalGroups, loadedCount[0]);
            }
        });
    }

    private void checkEmptyState(long total, long loaded) {
        if (loaded == total) {
            if (groupList.isEmpty()) {
                showEmptyState();
            } else {
                rvMyGroups.setVisibility(View.VISIBLE);
                emptyStateLayout.setVisibility(View.GONE);
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void showEmptyState() {
        rvMyGroups.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
    }
}