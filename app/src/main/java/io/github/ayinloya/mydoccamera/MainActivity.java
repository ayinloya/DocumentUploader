package io.github.ayinloya.mydoccamera;


import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.soundcloud.android.crop.Crop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import io.github.ayinloya.mydoccamera.api.Api;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    public static final int MY_DOC_CAMERA_PERMISSIONS_REQUEST_CAMERA = 10;
    public static final int MY_DOC_CAMERA_REQUEST = 11;
    public static final int MY_DOC_REQUEST_IMAGE_CAPTURE = 12;
    final String TAG = MainActivity.class.getSimpleName();
    private ImageView imageView;
    private ProgressBar progress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setViews();
    }

    File mfile;
    Button uploadButton;
    Button takePicButton;

    void setViews() {
        imageView = findViewById(R.id.image_preview);
        progress = findViewById(R.id.progress);
        uploadButton = findViewById(R.id.upload_btn);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadFile(mfile, "2");
            }
        });
        takePicButton = findViewById(R.id.take_pic_btn);
        takePicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prepareCamera();
            }
        });

        updateViews();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == MY_DOC_CAMERA_PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Crop.REQUEST_CROP && resultCode == RESULT_OK) {
            String path = Crop.getOutput(data).getPath();

        } else if (requestCode == MY_DOC_REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(thumbnail);
            File file = saveImage(thumbnail);
            if (file != null) {
                mfile = file.getAbsoluteFile();
            }
        }

        updateViews();
    }


    public File saveImage(Bitmap myBitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        File wallpaperDirectory = new File(getCacheDir() + getResources().getString(R.string.app_name));
        if (!wallpaperDirectory.exists()) {  // have the object build the directory structure, if needed.
            wallpaperDirectory.mkdirs();
        }

        try {
            File f = new File(wallpaperDirectory, Calendar.getInstance().getTimeInMillis() + ".jpg");
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            MediaScannerConnection.scanFile(this,
                    new String[]{f.getPath()},
                    new String[]{"image/jpeg"}, null);
            fo.close();
            Log.d("TAG", "File Saved::---&gt;" + f.getAbsolutePath());

            return f;
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return null;
    }


    private void uploadFile(final File file, String userId) {

        showProgress(true);
        //creating request body for file
        MultipartBody.Part fileImage = null;
        if (file != null) {

            RequestBody avatarImageC = RequestBody.create(MediaType.parse("multipart/form-data"), file);
            fileImage = MultipartBody.Part.createFormData("document", file.getName(), avatarImageC);
        }
        RequestBody idBody = RequestBody.create(MediaType.parse("multipart/form-data"), userId);


        //The gson builder
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();


        //creating retrofit object
        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(Api.BASE_URL)
                .build();

        //creating our api
        Api api = retrofit.create(Api.class);

        //creating a call and calling the upload image method
        Call<Void> call = api.uploadImage(fileImage, idBody);

        //finally performing the call
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                mfile = null;
                showProgress(false);
                if (response.isSuccessful()) {
                    Toast.makeText(getApplicationContext(), "File Uploaded Successfully...", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Some error occurred...", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                showProgress(false);
                t.printStackTrace();
                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showProgress(boolean show) {
        if (show) {
            uploadButton.setVisibility(View.GONE);
            takePicButton.setVisibility(View.GONE);

            progress.setVisibility(View.VISIBLE);
            return;
        }
        progress.setVisibility(View.GONE);
        updateViews();
    }

    private void updateViews() {
        if (mfile == null) {
            uploadButton.setVisibility(View.GONE);
            imageView.setVisibility(View.INVISIBLE);
        } else {
            uploadButton.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.VISIBLE);
        }
        takePicButton.setVisibility(View.VISIBLE);

    }

    private void prepareCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestCameraPermissions();
        }
    }


    private void requestCameraPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            showReason();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, MY_DOC_CAMERA_PERMISSIONS_REQUEST_CAMERA);
        }
    }


    private void showReason() {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Alert");
        alertDialog.setMessage("MyDocCamera needs to access the Camera to function.");

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "DON'T ALLOW",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "ALLOW",
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA}, MY_DOC_CAMERA_PERMISSIONS_REQUEST_CAMERA);
                    }
                });
        alertDialog.show();
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, MY_DOC_REQUEST_IMAGE_CAPTURE);
        }
    }

    private void startCamera() {
        dispatchTakePictureIntent();

//        Intent intent = new Intent(this, MyCameraActivity.class);
//        startActivity(intent);
    }


}
