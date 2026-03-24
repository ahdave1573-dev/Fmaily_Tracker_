package com.gpstracker.activities;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.content.IntentSender;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.gpstracker.R;
import com.gpstracker.leader.CreateGroupActivity;
import com.gpstracker.leader.ManageMembersActivity;
import com.gpstracker.member.MyGroupsActivity;
import com.gpstracker.models.LocationModel;
import com.gpstracker.services.LocationService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UnifiedMapActivity extends BaseActivity implements OnMapReadyCallback {

    private LatLng myCurrentLatLng;

    private GoogleMap mMap;
    private DatabaseReference mDatabase, mUserRef;
    private FirebaseAuth mAuth;
    private boolean isFirstTime = true;
    private final HashMap<String, Marker> memberMarkers = new HashMap<>();
    private String selectedGroupId;

    private Polyline roadPolyline;
    // આ લાઈન પહેલેથી હશે જ, ખાતરી કરી લો

    private static final String CHANNEL_ID = "FamilyAlertsChannel";
    private long alertListenerStartTime;
    private final Set<String> shownNotificationKeys = new HashSet<>();
    private final List<ChildEventListener> groupChildEventListeners = new ArrayList<>();
    private final List<DatabaseReference> groupNotifRefs = new ArrayList<>();

    private Spinner spinnerGroupMap;
    private List<String> groupNames = new ArrayList<>();
    private List<String> groupIds = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;

    private ValueEventListener currentGroupLocationListener;
    private DatabaseReference currentGroupRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unified_map);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            mUserRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        }
        mDatabase = FirebaseDatabase.getInstance().getReference();

        setupNavigation();
        loadJoinedGroups();
        createNotificationChannel();
        startListeningForAlerts();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        findViewById(R.id.fabMyLocation).setOnClickListener(v -> zoomToCurrentDeviceLocation());

        checkPermissionsAndStartService();
    }

    private void setupNavigation() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        findViewById(R.id.btnMenu).setOnClickListener(v -> {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        spinnerGroupMap = findViewById(R.id.spinnerGroupMap);
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, groupNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGroupMap.setAdapter(spinnerAdapter);

        spinnerGroupMap.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!groupIds.isEmpty() && position < groupIds.size()) {
                    String newGid = groupIds.get(position);
                    if (!newGid.equals(selectedGroupId)) {
                        switchGroup(newGid);
                    }
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_my_groups || id == R.id.drawer_my_groups) {
                startActivity(new Intent(this, MyGroupsActivity.class));
            } else if (id == R.id.nav_create_group) {
                startActivity(new Intent(this, CreateGroupActivity.class));
            } else if (id == R.id.nav_manage_members) {
                startActivity(new Intent(this, ManageMembersActivity.class));
            } else if (id == R.id.drawer_logout) {
                logout();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_create_group) startActivity(new Intent(this, CreateGroupActivity.class));
            else if (id == R.id.nav_manage_members) startActivity(new Intent(this, ManageMembersActivity.class));
            else if (id == R.id.nav_my_groups) startActivity(new Intent(this, MyGroupsActivity.class));
            return true;
        });
    }

    private void loadJoinedGroups() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        mDatabase.child("Users").child(uid).child("joinedGroups")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        groupNames.clear();
                        groupIds.clear();

                        if (!snapshot.exists()) {
                            selectedGroupId = null;
                            spinnerAdapter.notifyDataSetChanged();
                            zoomToCurrentDeviceLocation();
                            return;
                        }

                        final long totalGroups = snapshot.getChildrenCount();
                        final int[] loadedCount = {0};

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String gid = ds.getKey();
                            mDatabase.child("Groups").child(gid).child("groupName").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot s) {
                                    if (gid != null) {
                                        groupIds.add(gid);
                                        String name = s.getValue(String.class);
                                        groupNames.add(name != null ? name : "Unknown Group");
                                    }
                                    loadedCount[0]++;
                                    if (loadedCount[0] == totalGroups) {
                                        spinnerAdapter.notifyDataSetChanged();
                                        if (selectedGroupId == null && !groupIds.isEmpty()) {
                                            switchGroup(groupIds.get(0));
                                        }
                                    }
                                }
                                @Override public void onCancelled(@NonNull DatabaseError e) { loadedCount[0]++; }
                            });
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void switchGroup(String groupId) {
        if (selectedGroupId != null) setUserOnlineStatus(selectedGroupId, "offline");
        if (currentGroupRef != null && currentGroupLocationListener != null) {
            currentGroupRef.removeEventListener(currentGroupLocationListener);
        }
        if (mMap != null) mMap.clear();
        memberMarkers.clear();

        selectedGroupId = groupId;
        setUserOnlineStatus(selectedGroupId, "online");
        isFirstTime = true;
        fetchMembersLocations(groupId);
    }

    private void fetchMembersLocations(String groupId) {
        currentGroupRef = mDatabase.child("Locations").child(groupId);
        currentGroupLocationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (mMap == null) return;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    LocationModel model = ds.getValue(LocationModel.class);
                    if (model == null) continue;
                    LatLng loc = new LatLng(model.getLatitude(), model.getLongitude());
                    if (model.getUserId().equals(mAuth.getUid())) myCurrentLatLng = loc;
                    updateMarker(model, loc);
                }
                if (isFirstTime && myCurrentLatLng != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myCurrentLatLng, 15f));
                    isFirstTime = false;
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        currentGroupRef.addValueEventListener(currentGroupLocationListener);
    }

    private void updateMarker(LocationModel model, LatLng loc) {
        if (mMap == null) return;

        boolean isOnline = "online".equalsIgnoreCase(model.getStatus());
        BitmapDescriptor icon = getMarkerIconWithInitials(model.getName(), isOnline);

        String distanceStr = "";
        if (myCurrentLatLng != null && !model.getUserId().equals(mAuth.getUid())) {
            float[] results = new float[1];
            android.location.Location.distanceBetween(
                    myCurrentLatLng.latitude, myCurrentLatLng.longitude,
                    loc.latitude, loc.longitude,
                    results);

            float distanceInMeters = results[0];
            if (distanceInMeters < 1000) {
                distanceStr = String.format(" | Dist: %.0f m", distanceInMeters);
            } else {
                distanceStr = String.format(" | Dist: %.2f km", distanceInMeters / 1000);
            }
        }
        String snippet = "Battery: " + model.getBatteryLevel() + " | Speed: " + String.format("%.1f", model.getSpeed()) + " km/h" + distanceStr;

        if (memberMarkers.containsKey(model.getUserId())) {
            Marker m = memberMarkers.get(model.getUserId());
            if (m != null) {
                m.setPosition(loc);
                m.setIcon(icon);
                m.setSnippet(snippet);

                if (m.isInfoWindowShown()) {
                    m.showInfoWindow();
                }
            }
        } else {
            Marker m = mMap.addMarker(new MarkerOptions()
                    .position(loc)
                    .title(model.getName())
                    .snippet(snippet)
                    .icon(icon));
            if (m != null) memberMarkers.put(model.getUserId(), m);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        
        LatLng india = new LatLng(20.5937, 78.9629);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(india, 5f));

        mMap.setOnMarkerClickListener(marker -> {
            marker.showInfoWindow();

            if (myCurrentLatLng != null && !marker.getPosition().equals(myCurrentLatLng)) {
                getRoadRoute(myCurrentLatLng, marker.getPosition());
            }
            return false;
        });

        if (selectedGroupId == null) zoomToCurrentDeviceLocation();
    }

    private void zoomToCurrentDeviceLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(this).getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    myCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    if (selectedGroupId == null) {
                        mUserRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot s) {
                                String name = s.getValue(String.class);
                                if (name == null) name = "User";
                                mMap.clear();
                                mMap.addMarker(new MarkerOptions().position(myCurrentLatLng).title(name).icon(getMarkerIconWithInitials(name, true)));
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myCurrentLatLng, 15f));
                            }
                            @Override public void onCancelled(@NonNull DatabaseError e) {}
                        });
                    } else {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myCurrentLatLng, 15f));
                    }
                }
            });
        }
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "??";
        String[] words = name.trim().split("\\s+");
        if (words.length >= 2) return (words[0].substring(0,1) + words[1].substring(0,1)).toUpperCase();
        return name.substring(0, Math.min(name.length(), 2)).toUpperCase();
    }

    private BitmapDescriptor getMarkerIconWithInitials(String name, boolean isOnline) {
        int size = 120;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(isOnline ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
        canvas.drawCircle(size/2f, size/2f, size/2f, p);
        p.setColor(Color.WHITE); p.setTextSize(40f); p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD)); p.setTextAlign(Paint.Align.CENTER);
        String initials = getInitials(name);
        Paint.FontMetrics fm = p.getFontMetrics();
        canvas.drawText(initials, size/2f, (size/2f) - (fm.ascent + fm.descent)/2f, p);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    // --- Notifications Logic ---
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Family Alerts", NotificationManager.IMPORTANCE_HIGH);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void startListeningForAlerts() {
        if (mAuth.getCurrentUser() == null) return;
        alertListenerStartTime = System.currentTimeMillis();
        mUserRef.child("joinedGroups").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot groupSnap : snapshot.getChildren()) {
                    String gid = groupSnap.getKey();
                    if (gid != null) listenToGroupNotifications(gid);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void listenToGroupNotifications(String groupId) {
        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(groupId);
        ChildEventListener listener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String p) {
                Long ts = snapshot.child("timestamp").getValue(Long.class);
                String key = snapshot.getKey();
                if (ts != null && ts > alertListenerStartTime && key != null && !shownNotificationKeys.contains(key)) {
                    shownNotificationKeys.add(key);
                    showLocalNotification(snapshot.child("title").getValue(String.class), snapshot.child("message").getValue(String.class));
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };
        notifRef.addChildEventListener(listener);
        groupNotifRefs.add(notifRef);
        groupChildEventListeners.add(listener);
    }

    private void showLocalNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_menu_mylocation)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void setUserOnlineStatus(String groupId, String status) {
        if (mAuth.getCurrentUser() != null && groupId != null) {
            DatabaseReference ref = mDatabase.child("Locations").child(groupId).child(mAuth.getUid()).child("status");
            ref.setValue(status);
            if ("online".equals(status)) ref.onDisconnect().setValue("offline");
        }
    }

    private void logout() {
        stopService(new Intent(this, LocationService.class));
        mAuth.signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void checkPermissionsAndStartService() {
        String perm = Manifest.permission.ACCESS_FINE_LOCATION;
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{perm}, 1001);
        } else {
            checkLocationSettingsAndStartService();
        }
    }

    private void checkLocationSettingsAndStartService() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).build();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, response -> {
            Intent intent = new Intent(this, LocationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent); else startService(intent);
        });

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(UnifiedMapActivity.this, 1002);
                } catch (IntentSender.SendIntentException sendEx) {
                    // Ignore error
                }
            } else {
                Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1002) {
            if (resultCode == RESULT_OK) {
                Intent intent = new Intent(this, LocationService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent); else startService(intent);
            } else {
                Toast.makeText(this, "GPS is required to track location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLocationSettingsAndStartService();
        } else {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (selectedGroupId != null) setUserOnlineStatus(selectedGroupId, "offline");
    }

    @Override
    protected void onDestroy() {
        if (selectedGroupId != null) setUserOnlineStatus(selectedGroupId, "offline");
        super.onDestroy();
    }


    // ૧. રસ્તો મેળવવા માટે OSRM API નો ઉપયોગ
    private void getRoadRoute(LatLng origin, LatLng dest) {
        String coords = origin.longitude + "," + origin.latitude + ";" + dest.longitude + "," + dest.latitude;
        String url = "https://router.project-osrm.org/route/v1/driving/" + coords + "?overview=full&geometries=polyline";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://router.project-osrm.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        com.gpstracker.utils.GoogleMapsApi api = retrofit.create(com.gpstracker.utils.GoogleMapsApi.class);
        api.getFreeRoute(coords).enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
            @Override
            public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call, retrofit2.Response<com.google.gson.JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String points = response.body().getAsJsonArray("routes")
                                .get(0).getAsJsonObject()
                                .get("geometry").getAsString();
                        drawPolyline(points);
                    } catch (Exception e) {
                        Log.e("RouteErr", "Parse Error: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                Toast.makeText(UnifiedMapActivity.this, "Route error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ૨. મેપ પર રસ્તો (Blue Line) દોરવા માટે
    private void drawPolyline(String encodedPoints) {
        if (roadPolyline != null) roadPolyline.remove(); // જૂનો રસ્તો કાઢો

        roadPolyline = mMap.addPolyline(new com.google.android.gms.maps.model.PolylineOptions()
                .addAll(com.google.maps.android.PolyUtil.decode(encodedPoints))
                .width(12)
                .color(Color.BLUE)
                .jointType(com.google.android.gms.maps.model.JointType.ROUND));
    }
}