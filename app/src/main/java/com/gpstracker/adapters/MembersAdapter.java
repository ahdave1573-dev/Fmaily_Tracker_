package com.gpstracker.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.gpstracker.R;
import com.gpstracker.activities.UnifiedMapActivity;
import com.gpstracker.models.UserModel;

import java.util.List;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> {

    private Context context;
    private List<UserModel> memberList;
    private String groupId;

    public MembersAdapter(Context context, List<UserModel> memberList, String groupId) {
        this.context = context;
        this.memberList = memberList;
        this.groupId = groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        UserModel user = memberList.get(position);

        holder.tvMemberName.setText(user.getName());
        holder.tvMemberEmail.setText(user.getEmail());

        String battery = user.getBatteryLevel() != null ? user.getBatteryLevel() : "--%";
        holder.tvBatteryInfo.setText("Battery: " + battery);

        String status = user.getStatus();
        if ("online".equalsIgnoreCase(status)) {
            holder.ivStatusDot.setColorFilter(Color.GREEN);
            holder.tvStatusText.setText("Online");
            holder.tvStatusText.setTextColor(Color.GREEN);
        } else {
            holder.ivStatusDot.setColorFilter(Color.GRAY);
            holder.tvStatusText.setText("Offline");
            holder.tvStatusText.setTextColor(Color.GRAY);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, UnifiedMapActivity.class);
            intent.putExtra("userId", user.getUserId());
            intent.putExtra("groupId", groupId);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });

        holder.btnRemoveMember.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Remove Member?")
                    .setMessage("Are you sure you want to remove " + user.getName() + " from the group?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Position pass karvi jaruri che notify karva mate
                        removeMemberFromFirebase(user, holder.getAdapterPosition());
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void removeMemberFromFirebase(UserModel user, int position) {
        if (groupId == null || groupId.isEmpty()) {
            Toast.makeText(context, "Group ID missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUserId();
        // Jo userId null hoy to email thi pending ID banavo (Activity na logic mujab)
        if (userId == null) {
            userId = "pending_" + user.getEmail().replace(".", "_");
        }

        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        final String finalUserId = userId;

        // 1. Groups/{gid}/members/{uid} mathi kadho
        mDatabase.child("Groups").child(groupId).child("members").child(finalUserId).removeValue()
                .addOnSuccessListener(aVoid -> {

                    // 2. Users/{uid}/joinedGroups/{gid} mathi kadho
                    mDatabase.child("Users").child(finalUserId).child("joinedGroups").child(groupId).removeValue();

                    // 3. UI Update: List mathi item kadho ane notify karo
                    if (position != RecyclerView.NO_POSITION && position < memberList.size()) {
                        memberList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, memberList.size());
                    }

                    Toast.makeText(context, "Member removed successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return memberList != null ? memberList.size() : 0;
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView tvMemberName, tvMemberEmail, tvStatusText, tvBatteryInfo;
        ImageView ivStatusDot;
        ImageButton btnRemoveMember;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvMemberEmail = itemView.findViewById(R.id.tvMemberEmail);
            tvStatusText = itemView.findViewById(R.id.tvStatusText);
            tvBatteryInfo = itemView.findViewById(R.id.tvBatteryInfo);
            ivStatusDot = itemView.findViewById(R.id.ivStatusDot);
            btnRemoveMember = itemView.findViewById(R.id.btnRemoveMember);
        }
    }
}