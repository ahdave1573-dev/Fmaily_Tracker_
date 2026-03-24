package com.gpstracker.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.gpstracker.R;
import com.gpstracker.activities.UnifiedMapActivity;
import com.gpstracker.models.GroupModel;

import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    private Context context;
    private List<GroupModel> groupList;
    private String currentUserId;
    private DatabaseReference mDatabase;

    public GroupAdapter(Context context, List<GroupModel> groupList, String currentUserId) {
        this.context = context;
        this.groupList = groupList;
        this.currentUserId = currentUserId;
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        GroupModel group = groupList.get(position);
        holder.tvGroupName.setText(group.getGroupName());
        holder.tvGroupCode.setText("Code: " + group.getGroupCode());

        String groupId = group.getGroupId();
        if (groupId == null) groupId = group.getGroupCode();

        final String finalGroupId = groupId;

        holder.switchShareLocationGroup.setOnCheckedChangeListener(null);
        holder.switchShareLocationGroup.setChecked(group.isSharingLocation);
        
        holder.switchShareLocationGroup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            group.isSharingLocation = isChecked;
            mDatabase.child("Users").child(currentUserId).child("sharingStatus").child(finalGroupId).setValue(isChecked);
            
            DatabaseReference statusRef = mDatabase.child("Locations").child(finalGroupId).child(currentUserId).child("status");
            if (isChecked) {
                statusRef.setValue("online");
                Toast.makeText(context, "Sharing location with " + group.getGroupName(), Toast.LENGTH_SHORT).show();
            } else {
                statusRef.setValue("offline");
                Toast.makeText(context, "Hidden location from " + group.getGroupName(), Toast.LENGTH_SHORT).show();
            }
        });

        // Leave group functionality
        holder.btnLeaveGroup.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                .setTitle("Leave Group")
                .setMessage("Are you sure you want to leave " + group.getGroupName() + "?")
                .setPositiveButton("Leave", (dialog, which) -> {
                    mDatabase.child("Users").child(currentUserId).child("joinedGroups").child(finalGroupId).removeValue();
                    mDatabase.child("Groups").child(finalGroupId).child("members").child(currentUserId).removeValue();
                    mDatabase.child("Locations").child(finalGroupId).child(currentUserId).removeValue();
                    Toast.makeText(context, "Left group: " + group.getGroupName(), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        fetchMemberCounts(finalGroupId, holder);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, UnifiedMapActivity.class);
            intent.putExtra("groupId", finalGroupId);
            context.startActivity(intent);
        });
    }

    @Override
    public void onViewRecycled(@NonNull GroupViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.totalMembersRef != null && holder.totalMembersListener != null) {
            holder.totalMembersRef.removeEventListener(holder.totalMembersListener);
        }
        if (holder.onlineMembersRef != null && holder.onlineMembersListener != null) {
            holder.onlineMembersRef.removeEventListener(holder.onlineMembersListener);
        }
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    public static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView tvGroupName, tvGroupCode, tvTotalMembers, tvOnlineMembers;
        SwitchCompat switchShareLocationGroup;
        ImageButton btnLeaveGroup;

        ValueEventListener totalMembersListener;
        ValueEventListener onlineMembersListener;
        DatabaseReference totalMembersRef;
        DatabaseReference onlineMembersRef;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            tvGroupCode = itemView.findViewById(R.id.tvGroupCode);
            tvTotalMembers = itemView.findViewById(R.id.tvTotalMembers);
            tvOnlineMembers = itemView.findViewById(R.id.tvOnlineMembers);
            switchShareLocationGroup = itemView.findViewById(R.id.switchShareLocationGroup);
            btnLeaveGroup = itemView.findViewById(R.id.btnLeaveGroup);
        }
    }

    private void fetchMemberCounts(String groupId, GroupViewHolder holder) {
        if (holder.totalMembersRef != null && holder.totalMembersListener != null) {
            holder.totalMembersRef.removeEventListener(holder.totalMembersListener);
        }
        if (holder.onlineMembersRef != null && holder.onlineMembersListener != null) {
            holder.onlineMembersRef.removeEventListener(holder.onlineMembersListener);
        }

        holder.totalMembersRef = mDatabase.child("Groups").child(groupId).child("members");
        holder.totalMembersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long total = snapshot.getChildrenCount();
                holder.tvTotalMembers.setText("Total: " + total);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        holder.totalMembersRef.addValueEventListener(holder.totalMembersListener);

        holder.onlineMembersRef = mDatabase.child("Locations").child(groupId);
        holder.onlineMembersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int onlineCount = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String status = ds.child("status").getValue(String.class);
                    if ("online".equalsIgnoreCase(status)) {
                        onlineCount++;
                    }
                }
                holder.tvOnlineMembers.setText("Online: " + onlineCount);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        holder.onlineMembersRef.addValueEventListener(holder.onlineMembersListener);
    }
}
