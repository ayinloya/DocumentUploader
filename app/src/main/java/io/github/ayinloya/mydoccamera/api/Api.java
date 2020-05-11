package io.github.ayinloya.mydoccamera.api;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface Api {

    String BASE_URL = "https://stage.appruve.co/v1/verifications/test/";


    @Multipart
    @POST("file_upload")
    Call<Void> uploadImage(@Part MultipartBody.Part document, @Part("user_id") RequestBody userId);

}