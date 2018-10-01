package com.example.kon_boot.totooncall;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileSettings extends AppCompatActivity {


     private Button btnconfirm,btnback;
     private ImageView profile;
     private EditText edtname,edtphn;
     private FirebaseAuth mAuth;
     private DatabaseReference databaseReference;
     private  String userId;
     private String Name,Phone,ProfileImageUrl;
     private Uri REsultUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);
        edtname=  findViewById(R.id.Name);
        edtphn= findViewById(R.id.Phone);
        profile= findViewById(R.id.profile);
        btnback=findViewById(R.id.back);
        btnconfirm=findViewById(R.id.confirm);


        mAuth=FirebaseAuth.getInstance();
        userId=mAuth.getCurrentUser().getUid();
        databaseReference= FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userId);
        getUserInfo();
        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,1);
            }
        });
        btnconfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserInformation();
            }
        });
        btnback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                return;
            }
        });
    }
    private  void getUserInfo()
    {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()&&dataSnapshot.getChildrenCount()>0)
                {
                    Map<String,Object> map= (Map<String, Object>)dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        Name=map.get("name").toString();
                        edtname.setText(Name);
                    }
                    if(map.get("phone")!=null){
                        Phone=map.get("phone").toString();
                        edtphn.setText(Phone);
                    }
                    if(map.get("profileImageUrl")!=null){
                        ProfileImageUrl= map.get("profileImageUrl").toString();
                        Glide.with(getApplication()).load(ProfileImageUrl).into(profile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1 && resultCode==RESULT_OK)
        {
            final Uri imageUri= data.getData();
            REsultUri= imageUri;
            profile.setImageURI(REsultUri);
        }
    }

    private void saveUserInformation() {
        Name=edtname.getText().toString();
        Phone=edtphn.getText().toString();

        Map userInfo= new HashMap();
        userInfo.put("name",Name);
        userInfo.put("phone",Phone);
        databaseReference.updateChildren(userInfo);
        if(REsultUri!=null)
        {
            final StorageReference filepath= FirebaseStorage.getInstance().getReference().child("profileImages").child(userId);
            Bitmap bitmap= null;

            try {
                bitmap= MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(),REsultUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ByteArrayOutputStream boas= new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG,20,boas);
            byte[] data =boas.toByteArray();

            UploadTask uploadTask= filepath.putBytes(data);

            filepath.putFile(REsultUri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    return  filepath.getDownloadUrl();

                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if(task.isSuccessful()) {
                        Uri download = task.getResult();
                        Map newImage= new HashMap();
                        newImage.put("profileImageUrl",download.toString());
                        databaseReference.updateChildren(newImage);
                        finish();
                        return;
                    }
                    else{
                        Toast.makeText(ProfileSettings.this, "Error Uploading: "+task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    String downloadUrl= taskSnapshot.getStorage().getDownloadUrl().toString();


                    Map newImage= new HashMap();
                    newImage.put("profileImageUrl",downloadUrl.toString());
                    databaseReference.updateChildren(newImage);
                    finish();
                    return;
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                            finish();
                            return;
                        }

            });
        }
        else{
            finish();
        }
    }
}
