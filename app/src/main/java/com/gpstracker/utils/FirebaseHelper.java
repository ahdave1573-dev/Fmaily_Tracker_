package com.gpstracker.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class FirebaseHelper {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    public FirebaseHelper() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    public String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        return (user != null) ? user.getUid() : null;
    }

    public void logoutUser() {
        mAuth.signOut();
    }

    public DatabaseReference getDatabaseReference() {
        return mDatabase;
    }

    public FirebaseAuth getAuth() {
        return mAuth;
    }

    /**
     * બેટરી લેવલ અપડેટ કરવા માટેની મેથડ.
     * આ મેથડ બેટરીનું સ્ટેટસ અને ટાઈમસ્ટેમ્પ પણ અપડેટ કરશે.
     */
    public void updateBatteryLevel(int level) {
        String uid = getCurrentUserId();
        if (uid != null) {
            Map<String, Object> batteryUpdate = new HashMap<>();

            // ડેટાબેઝમાં સુસંગતતા માટે
            batteryUpdate.put("batteryLevel", level + "%");
            batteryUpdate.put("batteryPercentage", level); // Integer format for logic
            batteryUpdate.put("lastBatteryUpdate", System.currentTimeMillis());

            // જો ૧૫% કે તેથી ઓછી હોય તો 'Critical' સ્ટેટસ સેટ કરો
            if (level <= 15) {
                batteryUpdate.put("batteryStatus", "Critical");
            } else {
                batteryUpdate.put("batteryStatus", "Normal");
            }

            // ફક્ત જરૂરી ફિલ્ડ્સ જ અપડેટ કરવા માટે updateChildren વાપરો
            mDatabase.child("Users").child(uid).updateChildren(batteryUpdate)
                    .addOnSuccessListener(aVoid -> android.util.Log.d("FirebaseHelper", "Battery Updated: " + level + "%"))
                    .addOnFailureListener(e -> android.util.Log.e("FirebaseHelper", "Update Failed: " + e.getMessage()));
        }
    }

    /**
     * ફેમિલી ગ્રુપના નોટિફિકેશન મેળવવા માટે ટોપિક સબસ્ક્રાઇબ કરો.
     */
    public void subscribeToFamilyTopic(String groupId) {
        if (groupId != null && !groupId.isEmpty()) {
            FirebaseMessaging.getInstance().subscribeToTopic(groupId)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            android.util.Log.d("FCM", "Subscribed to group topic: " + groupId);
                        } else {
                            android.util.Log.e("FCM", "Subscription failed for: " + groupId);
                        }
                    });
        }
    }

    public void unsubscribeFromFamilyTopic(String groupId) {
        if (groupId != null && !groupId.isEmpty()) {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(groupId)
                    .addOnSuccessListener(aVoid -> android.util.Log.d("FCM", "Unsubscribed from: " + groupId));
        }
    }

    // --- જૂની મેથડ્સ ---
    public DatabaseReference getGroupLocationsRef(String groupId) {
        return mDatabase.child("Locations").child(groupId);
    }

    public DatabaseReference getUserGroupRef(String userId) {
        return mDatabase.child("Users").child(userId).child("groupId");
    }
}