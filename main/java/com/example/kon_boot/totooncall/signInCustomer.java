package com.example.kon_boot.totooncall;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class signInCustomer extends AppCompatActivity {
    private Button btnlogin,register;
    EditText edtemail,edtphn;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthlistener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_customer);
        mAuth= FirebaseAuth.getInstance();
        firebaseAuthlistener= new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
                if(user!=null){
                    Intent intent = new Intent(signInCustomer.this,Customer.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };

        btnlogin=findViewById(R.id.Login);
        edtemail= findViewById(R.id.email);
        edtphn= findViewById(R.id.password);
        register=findViewById(R.id.Registration);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email= edtemail.getText().toString();
                String password=edtphn.getText().toString();
                mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(signInCustomer.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            Toast.makeText(signInCustomer.this, "SignUp error", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            String UserID=mAuth.getCurrentUser().getUid();
                            DatabaseReference currentuser= FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(UserID);
                            currentuser.setValue(true);
                        }
                    }
                });
            }
        });
        btnlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email= edtemail.getText().toString();
                String password=edtphn.getText().toString();
                mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(signInCustomer.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            Toast.makeText(signInCustomer.this, "Login Failed", Toast.LENGTH_SHORT).show();
                        }

                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthlistener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(firebaseAuthlistener);
    }
    }

