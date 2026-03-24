package com.gpstracker.leader;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;
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
import com.gpstracker.adapters.MembersAdapter;
import com.gpstracker.models.UserModel;

import java.util.ArrayList;
import java.util.List;

public class ManageMembersActivity extends BaseActivity {

    private RecyclerView recyclerViewMembers;
    private MembersAdapter adapter;
    private List<UserModel> memberList;
    private DatabaseReference mDatabase;
    private ProgressBar progressBar;
    private TextView txtEmptyMsg;
    private EditText edtAddMemberEmail;
    private Button btnAddMemberByEmail;
    private Spinner spinnerGroups;

    private String targetGroupId = null;
    private List<String> groupNames = new ArrayList<>();
    private List<String> groupIds = new ArrayList<>();

    private ValueEventListener membersListener, usersListener, locationsListener;
    private DatabaseReference membersRef, usersRef, locationsRef;

    private String apkLink = "https://drive.google.com/file/d/1LUwPTorxvlEvnm6CktA68_U9MrofME8-/view?usp=sharing";
    private String brevoApiKey = "Api";
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_manage_members);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        setupBottomNavigation(R.id.nav_manage_members);
        setupNavigationDrawerHeader();

        final DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(androidx.core.view.GravityCompat.START);
            }
        });

        recyclerViewMembers = findViewById(R.id.recyclerViewMembers);
        progressBar = findViewById(R.id.progressBarMembers);
        txtEmptyMsg = findViewById(R.id.txtEmptyMsg);
        edtAddMemberEmail = findViewById(R.id.edtAddMemberEmail);
        btnAddMemberByEmail = findViewById(R.id.btnAddMemberByEmail);
        spinnerGroups = findViewById(R.id.spinnerGroups);

        recyclerViewMembers.setLayoutManager(new LinearLayoutManager(this));
        memberList = new ArrayList<>();

        targetGroupId = getIntent().getStringExtra("groupId");
        adapter = new MembersAdapter(this, memberList, targetGroupId);
        recyclerViewMembers.setAdapter(adapter);

        setupGroupSpinner();
        btnAddMemberByEmail.setOnClickListener(v -> handleAddMemberByEmail());
    }

    private void setupGroupSpinner() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        progressBar.setVisibility(View.VISIBLE);

        // Load both createdGroups and joinedGroups
        DatabaseReference userRef = mDatabase.child("Users").child(currentUserId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupIds.clear();
                groupNames.clear();

                // Collect all unique group IDs from both created and joined
                List<String> allGroupIds = new ArrayList<>();

                DataSnapshot createdSnap = snapshot.child("createdGroups");
                if (createdSnap.exists()) {
                    for (DataSnapshot gSnap : createdSnap.getChildren()) {
                        String gId = gSnap.getKey();
                        if (gId != null && !allGroupIds.contains(gId)) {
                            allGroupIds.add(gId);
                        }
                    }
                }

                DataSnapshot joinedSnap = snapshot.child("joinedGroups");
                if (joinedSnap.exists()) {
                    for (DataSnapshot gSnap : joinedSnap.getChildren()) {
                        String gId = gSnap.getKey();
                        if (gId != null && !allGroupIds.contains(gId)) {
                            allGroupIds.add(gId);
                        }
                    }
                }

                if (allGroupIds.isEmpty()) {
                    progressBar.setVisibility(View.GONE);
                    txtEmptyMsg.setVisibility(View.VISIBLE);
                    txtEmptyMsg.setText("You haven't created or joined any groups yet.");
                    return;
                }

                final long total = allGroupIds.size();
                final long[] count = {0};

                for (String gId : allGroupIds) {
                    mDatabase.child("Groups").child(gId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot groupSnap) {
                                    String name = groupSnap.child("groupName").getValue(String.class);
                                    groupIds.add(gId);
                                    groupNames.add(name != null ? name : gId);

                                    count[0]++;
                                    if (count[0] == total) {
                                        updateSpinner();
                                    }
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) { count[0]++; }
                            });
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) { progressBar.setVisibility(View.GONE); }
        });

        spinnerGroups.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                targetGroupId = groupIds.get(position);
                adapter.setGroupId(targetGroupId);
                fetchMembersForGroup(targetGroupId);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateSpinner() {
        progressBar.setVisibility(View.GONE);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, groupNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGroups.setAdapter(spinnerAdapter);

        // Pre-select if groupId was passed via Intent
        if (targetGroupId != null) {
            int index = groupIds.indexOf(targetGroupId);
            if (index >= 0) {
                spinnerGroups.setSelection(index);
            }
        }
    }

    private void handleAddMemberByEmail() {
        String emailToSearch = edtAddMemberEmail.getText().toString().trim();

        if (emailToSearch.isEmpty()) {
            Toast.makeText(this, "Enter email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (targetGroupId == null) {
            Toast.makeText(this, "No group selected", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // Step A: Firebase ma check karo ke user registered che ke nahi
        mDatabase.child("Users").orderByChild("email").equalTo(emailToSearch)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // User register che, sidho add karo
                            for (DataSnapshot userSnap : snapshot.getChildren()) {
                                String userId = userSnap.getKey();
                                addExistingUserToGroup(userId);
                            }
                        } else {
                            // User register NATHI -> 1. Email moklo, 2. Group ma entry karo
                            addUserAsPending(emailToSearch);
                            sendInvitationEmail(emailToSearch);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void addUserAsPending(String email) {
        // User login nathi, pan apane tene email na adhare group ma add kari dahi
        // Email mathi '.' kadhi nakhu kem ke Firebase key ma '.' allowed nathi
        String fakeId = "pending_" + email.replace(".", "_");

        mDatabase.child("Groups").child(targetGroupId).child("members").child(fakeId).setValue(true);

        // Ek dummy user data create karo jethi adapter ma name/email dekhay
        mDatabase.child("Users").child(fakeId).child("email").setValue(email);
        mDatabase.child("Users").child(fakeId).child("name").setValue("Pending (Invited)");
        mDatabase.child("Users").child(fakeId).child("status").setValue("Invited");
    }

    private void addExistingUserToGroup(String searchedUserId) {
        // User na joinedGroups ma aa Group ID add karo
        mDatabase.child("Users").child(searchedUserId).child("joinedGroups").child(targetGroupId).setValue(true);

        // Group na members ma aa User ID add karo
        mDatabase.child("Groups").child(targetGroupId).child("members").child(searchedUserId).setValue(true)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(ManageMembersActivity.this, "Member added successfully!", Toast.LENGTH_SHORT).show();
                        edtAddMemberEmail.setText(""); // Input box clear karo
                    } else {
                        Toast.makeText(ManageMembersActivity.this, "Failed to add member", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendInvitationEmail(String receiverEmail) {

        retrofit2.Retrofit retrofit = new retrofit2.Retrofit.Builder()
                .baseUrl("https://api.brevo.com/")
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build();

        BrevoApiService apiService = retrofit.create(BrevoApiService.class);

        String myRealApiKey = "API";

        String htmlContent = "<html><body style='font-family: Arial, sans-serif;'>" +
                "<h2 style='color: #333;'>Family Tracker Invitation</h2>" +
                "<p>You have been invited to join the <b>Family Tracker</b> app.</p>" +
                "<p>Click the button below to download and install the app:</p>" +
                "<br>" +
                "<a href='" + apkLink + "' style='padding: 12px 20px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px; font-weight: bold;'>Download APK</a>" +
                "<br><br>" +
                "<p>If the button doesn't work, copy and paste this link into your browser:<br>" + apkLink + "</p>" +
                "</body></html>";

        EmailRequest emailRequest = new EmailRequest("ahdave1573@gmail.com", receiverEmail, "Invitation to Join Family Group", htmlContent);

        apiService.sendEmail(myRealApiKey, "application/json", emailRequest).enqueue(new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {

                    Toast.makeText(ManageMembersActivity.this, "Invitation sent successfully!", Toast.LENGTH_LONG).show();
                    edtAddMemberEmail.setText("");
                } else {
                    Toast.makeText(ManageMembersActivity.this, "Failed to send invitation. Please try again.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ManageMembersActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchMembersForGroup(String groupId) {

        if (membersRef != null && membersListener != null) {
            membersRef.removeEventListener(membersListener);
        }

        progressBar.setVisibility(View.VISIBLE);
        membersRef = mDatabase.child("Groups").child(groupId).child("members");

        membersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot membersSnapshot) {
                List<String> memberUids = new ArrayList<>();
                for (DataSnapshot ds : membersSnapshot.getChildren()) {
                    memberUids.add(ds.getKey());
                }

                if (memberUids.isEmpty()) {
                    memberList.clear();
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    txtEmptyMsg.setVisibility(View.VISIBLE);
                    return;
                }
                loadMemberDetails(memberUids, groupId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        };

        membersRef.addValueEventListener(membersListener);
    }

    private void loadMemberDetails(List<String> memberUids, String groupId) {
        // જૂના લિસ્ટનર્સ ક્લીન કરો
        if (usersRef != null && usersListener != null) usersRef.removeEventListener(usersListener);
        if (locationsRef != null && locationsListener != null) locationsRef.removeEventListener(locationsListener);

        usersRef = mDatabase.child("Users");
        locationsRef = mDatabase.child("Locations").child(groupId);

        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                locationsListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot locSnapshot) {
                        memberList.clear(); // લિસ્ટ ચોખ્ખું કરો
                        for (String uid : memberUids) {
                            if (userSnapshot.hasChild(uid)) {
                                UserModel user = userSnapshot.child(uid).getValue(UserModel.class);
                                if (user != null) {
                                    // સ્ટેટસ અને બેટરી સેટ કરો
                                    if (locSnapshot.hasChild(uid)) {
                                        String status = locSnapshot.child(uid).child("status").getValue(String.class);
                                        String battery = locSnapshot.child(uid).child("batteryLevel").getValue(String.class);
                                        user.setStatus(status != null ? status : "offline");
                                        user.setBatteryLevel(battery != null ? battery : "0%");
                                    } else {
                                        user.setStatus("offline");
                                        user.setBatteryLevel("0%");
                                    }
                                    memberList.add(user);
                                }
                            }
                        }
                        progressBar.setVisibility(View.GONE);
                        txtEmptyMsg.setVisibility(memberList.isEmpty() ? View.VISIBLE : View.GONE);
                        adapter.notifyDataSetChanged();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                };
                locationsRef.addValueEventListener(locationsListener);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        usersRef.addValueEventListener(usersListener);
    }
}