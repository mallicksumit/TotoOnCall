package com.example.kon_boot.totooncall;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class FirstActivity extends AppCompatActivity {

    Button Customer,Driver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
        Customer = findViewById(R.id.Customer);
        Driver =findViewById(R.id.Driver);
        Customer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FirstActivity.this,signInCustomer.class);
                startActivity(intent);
            }
        });
        Driver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1= new Intent(FirstActivity.this,signInDriver.class);
                startActivity(intent1);
            }
        });
    }

}
