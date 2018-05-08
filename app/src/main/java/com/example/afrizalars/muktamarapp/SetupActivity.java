package com.example.afrizalars.muktamarapp;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

public class SetupActivity extends AppCompatActivity {

    private CircleImageView ProfileImage;
    private Uri mainImageUri = null;
    private EditText setupName;
    private Button setupBtn;
    private StorageReference storageReference;
    private FirebaseAuth mAuth;
    private ProgressBar setupProgress;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        Toolbar setupToolbar = findViewById(R.id.setupToolbar);
        setSupportActionBar(setupToolbar);
        getSupportActionBar().setTitle("Account Setup");
        firebaseFirestore = FirebaseFirestore.getInstance();

        ProfileImage = findViewById(R.id.ProfilePict);
        setupBtn = findViewById(R.id.submitBtn);
        setupName = findViewById(R.id.usernameText);
        storageReference = FirebaseStorage.getInstance().getReferenceFromUrl("gs://muktamarapp.appspot.com");
        mAuth = FirebaseAuth.getInstance();
        setupProgress = findViewById(R.id.setupProgress);

        ProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

                    if (ContextCompat.checkSelfPermission(SetupActivity.this, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(SetupActivity.this,"Permission denied",Toast.LENGTH_SHORT).show();
                        ActivityCompat.requestPermissions(SetupActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);

                    } else {
                        BringImagePicker();
                    }
                } else {

                    BringImagePicker();

                }
            }
        });


        setupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String username = setupName.getText().toString();
                if (!TextUtils.isEmpty(username) && mainImageUri != null){
                    final String user_id = mAuth.getCurrentUser().getUid();
                    setupProgress.setVisibility(View.VISIBLE);
                    StorageReference image_path = storageReference.child("image_profile").child(user_id+".jpg");

                    image_path.putFile(mainImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()){
                                Uri download_uri = task.getResult().getDownloadUrl();
                                Map<String, String> userMap = new HashMap<>();
                                userMap.put("name", username);
                                userMap.put("image",download_uri.toString());

                                firebaseFirestore.collection("Users").document(user_id).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        if (task.isSuccessful()){
                                            Toast.makeText(SetupActivity.this, "Profile Changed",Toast.LENGTH_SHORT).show();

                                            Intent mainIntent = new Intent(SetupActivity.this,MainActivity.class);
                                            startActivity(mainIntent);
                                            finish();

                                        } else {
                                            String error = task.getException().getMessage();
                                            Toast.makeText(SetupActivity.this, error,Toast.LENGTH_SHORT).show();
                                        }
                                        setupProgress.setVisibility(View.INVISIBLE);

                                    }
                                });

                                Toast.makeText(SetupActivity.this, "Image is uploaded",Toast.LENGTH_SHORT).show();

                            } else {
                                String error = task.getException().getMessage();
                                Toast.makeText(SetupActivity.this, error,Toast.LENGTH_SHORT).show();
                            }

                            setupProgress.setVisibility(View.INVISIBLE);
                        }
                    });

                }
            }
        });



    }

    private void BringImagePicker() {
        // start picker to get image for cropping and then use the image in cropping activity
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1,1)
                .start(SetupActivity.this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                mainImageUri = result.getUri();
                ProfileImage.setImageURI(mainImageUri);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
}
