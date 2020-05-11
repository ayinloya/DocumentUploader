package io.github.ayinloya.mydoccamera.api;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface Api {

    String BASE_URL = "https://stage.appruve.co/v1/verifications/test/";

    //this is our multipart request
    //we have two parameters on is name and other one is description
    @Multipart
    @POST("file_upload")
    Call<FileResponse> uploadImage(@Part("document\"; filename=\"document.jpg\" ") RequestBody file, @Part("user_id") RequestBody desc);

}