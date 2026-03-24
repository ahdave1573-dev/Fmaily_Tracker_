package com.gpstracker.utils;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface GoogleMapsApi {

    // OSRM ફ્રી સર્વિસ માટેનું નવું ફંક્શન (Billing ની જરૂર નથી)
    @GET("route/v1/driving/{coords}?overview=full&geometries=polyline")
    Call<JsonObject> getFreeRoute(@Path("coords") String coords);

    // જૂનું ગૂગલ ફંક્શન (જો બિલિંગ હોય તો જ કામ કરશે)
    /*
    @GET("maps/api/directions/json")
    Call<JsonObject> getDirections(
            @Query("origin") String origin,
            @Query("destination") String destination,
            @Query("key") String apiKey
    );
    */
}