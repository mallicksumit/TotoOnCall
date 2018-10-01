package com.example.kon_boot.totooncall;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Customer extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {
    private GoogleMap mMap;
    Location mLastLocation, current, pasloc;
    double latitude, longitude;
    GoogleApiClient googleApiClient;
    LocationRequest mlocatiorequest;
    private Button mRequest;
    private LatLng pickuplocation;
    private LatLng destinationLatlng;
    private double radius=1;
    private  boolean driverFound=false;
    private RatingBar mRatingBar;
    private boolean RequestBol=false;
    private String ID,destination,requestService;
    FirebaseAuth auth;
    private Marker mDriver,pickupmarker;
    GeoQuery geoQuery;
    private DatabaseReference driverlocref;
    private  ValueEventListener driverLocListener;
    private RadioGroup radioGroup;
    private LinearLayout mDriverInfo;
    private ImageView mDriverimage;
    private TextView mDrivername,MDriverphone,mCar,mCarNo;


    FirebaseUser firebaseUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        auth= FirebaseAuth.getInstance();
        firebaseUser =auth.getCurrentUser();
        mDriverInfo= findViewById(R.id.DriverInfo);
        mDriverimage=findViewById(R.id.profileDriver);
        mDrivername=findViewById(R.id.DriverName);
        MDriverphone=findViewById(R.id.DriverPhone);
        mCar=findViewById(R.id.DriverCar);
        mCarNo=findViewById(R.id.DriverCarNo);
        radioGroup=findViewById(R.id.radiogroup);
        radioGroup.check(R.id.Reserved);
        mRatingBar=findViewById(R.id.ratingBar);
        mRequest=findViewById(R.id.calldriver);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


  //Default Destination for if user dont enter any loc
        destinationLatlng = new LatLng(0.0,0.0);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (RequestBol)
                {
                   endRide();
                }else {

                    int selectId= radioGroup.getCheckedRadioButtonId();
                    final RadioButton radiobutton= findViewById(selectId);
                    if(radiobutton.getText()== null){
                        return;
                    }
                    requestService= radiobutton.getText().toString();
                    RequestBol=true;
                    String UserId = auth.getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Customer Request");
                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(UserId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));


                    pickuplocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    pickupmarker=mMap.addMarker(new MarkerOptions().position(pickuplocation).title("Pick Up Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_usermarker )));

                    mRequest.setText("Getting Your Driver....");
                    getClosestDriver();
                }
            }
        });
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                destination=place.getName().toString();
                destinationLatlng=place.getLatLng();

            }

            @Override
            public void onError(Status status) {
                Toast.makeText(Customer.this, status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                // TODO: Handle the error.
            }
        });

    }

    private void getClosestDriver() {
        DatabaseReference driverLoc= FirebaseDatabase.getInstance().getReference().child("DriverAvailable");
        GeoFire geoFire =new GeoFire(driverLoc);
        geoQuery=geoFire.queryAtLocation(new GeoLocation(pickuplocation.latitude,pickuplocation.longitude),radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound && RequestBol) {
                    DatabaseReference mCustomerDatabase= FirebaseDatabase.getInstance().getReference().child("Users").child("Driver").child(key);
                    mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()&& dataSnapshot.getChildrenCount()>0)
                            {
                                Map<String,Object> drivermap= (Map<String, Object>)dataSnapshot.getValue();

                                if(driverFound){
                                    return;
                                }

                                if(drivermap.get("Service").equals(requestService)){
                                    driverFound = true;
                                    ID=dataSnapshot.getKey();


                                    DatabaseReference driverref= FirebaseDatabase.getInstance().getReference().child("Users").child("Driver").child(ID).child("Customer Request");
                                    String customerId= FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    HashMap map= new HashMap();
                                    map.put("CustomerRideId",customerId);
                                    map.put("Destination",destination);
                                    map.put("DestinationLatitude",destinationLatlng.latitude);
                                    map.put("DestinationLongitude",destinationLatlng.longitude);
                                    driverref.updateChildren(map);
                                    getDriverLocation();
                                    getDriverInfo();
                                    gethasRideEnded();
                                    mRequest.setText("Looking for Driver Location....");
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });


                }
            }

            @Override
            public void onKeyExited(String key) {


            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {


            }

            @Override
            public void onGeoQueryReady() {
             if(!driverFound){
                 radius++;
                 getClosestDriver();
             }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {


            }
        });
    }

    private void getDriverInfo() {
        mDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReference().child("Users").child("Driver").child(ID);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()&&dataSnapshot.getChildrenCount()>0)
                {

                    if(dataSnapshot.child("name")!=null){
                        mDrivername.setText(dataSnapshot.child("name").toString());
                    }
                    if(dataSnapshot.child("phone")!=null){

                        MDriverphone.setText(dataSnapshot.child("phone").toString());
                    }
                    if(dataSnapshot.child("Car")!=null){

                        mCar.setText(dataSnapshot.child("Car").toString());
                    }
                    if(dataSnapshot.child("CarNo")!=null){

                        mCarNo.setText(dataSnapshot.child("CarNo").toString());
                    }
                    if(dataSnapshot.child("profileImageUrl")!=null){

                        Glide.with(getApplication()).load(dataSnapshot.child("profileImageUrl").toString()).into(mDriverimage);
                    }
                    int ratingsum=0;
                    float totalrating=0;
                    float avg;
                    for(DataSnapshot child:dataSnapshot.child("rating").getChildren())
                    {
                        ratingsum=ratingsum+Integer.valueOf(child.getValue().toString());
                        totalrating++;
                    }
                    if(totalrating!=0)
                    {
                        avg=ratingsum/totalrating;
                        mRatingBar.setRating(avg);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    private  DatabaseReference driverHasEndedRef;
    private ValueEventListener driveHasEndedListenerRef;
    private void gethasRideEnded() {
         driverHasEndedRef= FirebaseDatabase.getInstance().getReference().child("Users").child("Driver").child(ID).child("Customer Request").child("CustomerRideId");
        driveHasEndedListenerRef=driverHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {

                }
                else {
                    endRide();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void endRide() {
        RequestBol=false;
        geoQuery.removeAllListeners();
        driverlocref.removeEventListener(driverLocListener);
        driverHasEndedRef.removeEventListener(driveHasEndedListenerRef);
        if(ID!=null)
        {
            DatabaseReference driverref= FirebaseDatabase.getInstance().getReference().child("Users").child("Driver").child(ID).child("Customer Request");
            driverref.removeValue();
            ID=null;
        }
        driverFound=false;
        radius=1;
        if(pickupmarker!=null)
        {
            pickupmarker.remove();

        }
        if(mDriver!=null)
        {
            mDriver.remove();

        }
        String UserId = auth.getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Customer Request");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(UserId);
        mRequest.setText("CALL TOTO");

        mDriverInfo.setVisibility(View.GONE);
        mDrivername.setText("");
        MDriverphone.setText("");
        mCar.setText("");
        mCarNo.setText("");
        mDriverimage.setImageResource(R.mipmap.ic_rickshaw);
    }

    public void getDriverLocation()
    {
         driverlocref= FirebaseDatabase.getInstance().getReference().child("Driver Working").child(ID).child("l");
        driverLocListener=driverlocref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()&& RequestBol)
                {
                    List<Object> map =(List<Object>) dataSnapshot.getValue();
                    double locationlst=0;
                    double locationlng=0;
                    if(map.get(0)!=null) {
                        locationlst = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1)!=null){
                        locationlng=Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatlng=new LatLng(locationlst,locationlng);
                    if(mDriver!=null){
                        mDriver.remove();
                    }
                    Location loc1= new Location("");
                    loc1.setLatitude(pickuplocation.latitude);
                    loc1.setLongitude(pickuplocation.longitude);

                    Location loc2= new Location("");
                    loc2.setLatitude(driverLatlng.latitude);
                    loc2.setLongitude(driverLatlng.longitude);

                    float distance=loc1.distanceTo(loc2);

                    if(distance<100)
                    {
                        mRequest.setText("Driver  is  Here ");
                    }
                    else {
                        mRequest.setText("Driver Found: " + String.valueOf(distance));
                    }
                    mDriver= mMap.addMarker(new MarkerOptions().position(driverLatlng).title("Your Driver").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_rickshaw )));

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.customer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {
            Intent intent = new Intent(Customer.this,HistoryActivity.class);
            intent.putExtra("customerOrDriver","Customers");
            startActivity(intent);
            return true;

        } else if (id == R.id.nav_slideshow) {
            Intent intent= new Intent(Customer.this,ProfileSettings.class);
            startActivity(intent);
            return true ;

        } else if (id == R.id.nav_manage) {
            auth.signOut();
            Intent intent=new Intent(Customer.this,FirstActivity.class);
            startActivity(intent);
            finish();

// this listener will be called when there is change in firebase user session
            FirebaseAuth.AuthStateListener authListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user == null) {
                        // user auth state is changed - user is null
                        // launch login activity
                        startActivity(new Intent(Customer.this,  FirstActivity.class));
                        finish();
                    }
                }
            };
        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    public Customer() {
        super();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        mlocatiorequest = new LocationRequest();
        mlocatiorequest.setInterval(1000);
        mlocatiorequest.setFastestInterval(1000);
        mlocatiorequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mlocatiorequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation=location;
        LatLng latLng= new LatLng(location.getLatitude(),location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
        mMap.animateCamera(cameraUpdate);

        //Integrating the data with firebase
        String UserId=auth.getCurrentUser().getUid();
        DatabaseReference ref= FirebaseDatabase.getInstance().getReference("DriverAvailable");
        }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }
    @Override
    protected void onStop() {
        super.onStop();


    }

}
