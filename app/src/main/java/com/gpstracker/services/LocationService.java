package com.gpstracker.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.gpstracker.R;
import com.gpstracker.activities.UnifiedMapActivity;
import com.gpstracker.models.LocationModel;
import com.gpstracker.models.UserModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LocationService extends Service {

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private List<String> sharingEnabledGroupIds = new ArrayList<>();
    private int lastAlertLevel = 100;
    private String userName = null; // Actual user name from Firebase

    private static final String CHANNEL_ID = "LocationServiceChannel";
    private static final String TAG = "LocationService";

    private static final String PREFS_NAME = "GPS_TRACKER_PREFS";
    private static final String KEY_LAST_LEVEL = "last_notified_battery_level";

    // આને static બનાવો જેથી સર્વિસ ચાલુ હોય ત્યાં સુધી તેની વેલ્યુ મેમરીમાં સચવાય
    private static int lastNotifiedLevel = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        createNotificationChannel();
        startForeground(1, getStickyNotification("Sharing your live location..."));

        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            setupUserListener(uid);
            // Fetch user name from Firebase
            mDatabase.child("Users").child(uid).child("name").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String name = snapshot.getValue(String.class);
                    if (name != null && !name.trim().isEmpty()) {
                        userName = name;
                    } else {
                        // Fallback to email prefix
                        String email = mAuth.getCurrentUser().getEmail();
                        if (email != null && email.contains("@")) {
                            userName = email.split("@")[0];
                        } else {
                            userName = email;
                        }
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
        startLocationTracking();
    }

    private void setupUserListener(String userId) {
        mDatabase.child("Users").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                sharingEnabledGroupIds.clear();
                if (snapshot.exists()) {
                    UserModel user = snapshot.getValue(UserModel.class);
                    if (user != null && user.getJoinedGroups() != null) {
                        Map<String, Object> sharingStatus = user.getSharingStatus();

                        for (String groupId : user.getJoinedGroups().keySet()) {
                            boolean isSharing = false; // Default false રાખો

                            if (sharingStatus != null && sharingStatus.containsKey(groupId)) {
                                isSharing = Boolean.TRUE.equals(sharingStatus.get(groupId));
                            }

                            if (isSharing) {
                                sharingEnabledGroupIds.add(groupId);
                            }
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private Location lastLocation = null; // છેલ્લું લોકેશન સ્ટોર કરવા
    private float totalDistance = 0;      // કુલ અંતર (મીટરમાં)

    private void saveLocationToDatabase(Location currentLocation) {
        if (mAuth.getCurrentUser() != null && !sharingEnabledGroupIds.isEmpty()) {

            // ૧. સાચું બેટરી લેવલ મેળવો
            Intent batteryStatus = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level = -1;
            if (batteryStatus != null) {
                int rawLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                level = (int) ((rawLevel / (float) scale) * 100);
            }

            // ૨. Firebase Update (લોકેશન અને સ્પીડ)
            float speedKmH = (currentLocation.getSpeed() * 3.6f);
            if (lastLocation != null) totalDistance += lastLocation.distanceTo(currentLocation);
            lastLocation = currentLocation;

            for (String groupId : sharingEnabledGroupIds) {
                String displayName = userName != null ? userName : mAuth.getCurrentUser().getEmail();
                Map<String, Object> locationUpdates = new HashMap<>();
                locationUpdates.put("userId", mAuth.getCurrentUser().getUid());
                locationUpdates.put("name", displayName);
                locationUpdates.put("latitude", currentLocation.getLatitude());
                locationUpdates.put("longitude", currentLocation.getLongitude());
                locationUpdates.put("timestamp", System.currentTimeMillis());
                locationUpdates.put("batteryLevel", level + "%");
                locationUpdates.put("speed", speedKmH);
                locationUpdates.put("address", getAddressFromLatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
                locationUpdates.put("distance", totalDistance);

                mDatabase.child("Locations").child(groupId).child(mAuth.getCurrentUser().getUid()).updateChildren(locationUpdates);
            }

            if (level == 85 || level == 15 || level == 10 || level == 5) {

                if (level != lastNotifiedLevel) {

                    lastNotifiedLevel = level;

                    for (String groupId : sharingEnabledGroupIds) {
                        sendNotificationToGroup(groupId, mAuth.getCurrentUser().getEmail(), level + "%");
                    }

                    showLocalBatteryNotification(level + "%");

                    Log.d("BatteryFix", "Notification Sent & Locked for: " + level + "%");
                }
            } else {

                if (level != lastNotifiedLevel) {
                    lastNotifiedLevel = level;
                }
            }
        }
    }

    private void showLocalBatteryNotification(String batteryLevel) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String alertChannelId = "BatteryAlertChannel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    alertChannelId, "Battery Alerts", NotificationManager.IMPORTANCE_HIGH);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, alertChannelId)
                .setContentTitle("Battery Low!")
                .setContentText("Your phone battery is " + batteryLevel + "%. Please connect a charger.")
                .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (manager != null) {
            manager.notify(2, builder.build());
        }
    }

    private void startLocationTracking() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    saveLocationToDatabase(location);
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied: " + e.getMessage());
        }
    }

    private String getAddressFromLatLng(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (Exception e) { Log.e(TAG, "Geocoder error"); }
        return "Unknown Location";
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Location Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification getStickyNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GPS Tracker Active")
                .setContentText(text)
                // અહીં 'R.drawable.your_logo' માં તમારા લોગોનું નામ લખો
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build();
    }

    private void sendNotificationToGroup(String groupId, String email, String bat) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "Low Battery");
        data.put("message", email + " has " + bat + " battery left.");
        data.put("timestamp", System.currentTimeMillis());
        mDatabase.child("Notifications").child(groupId).push().setValue(data);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }

    @Override
    public void onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}