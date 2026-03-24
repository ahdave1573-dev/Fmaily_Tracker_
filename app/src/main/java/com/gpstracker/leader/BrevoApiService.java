package com.gpstracker.leader;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface BrevoApiService {
    @POST("v3/smtp/email")
    Call<Void> sendEmail(
            @Header("api-key") String apiKey,
            @Header("Content-Type") String contentType,
            @Body EmailRequest emailRequest
    );
}