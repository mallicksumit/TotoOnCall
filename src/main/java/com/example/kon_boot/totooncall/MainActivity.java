package com.example.kon_boot.totooncall;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
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
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.chrono.Era;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener,RoutingListener{
    private GoogleMap mMap;
    final int LOCATION_REQUEST=1;
    Location mLastLocation, current, pasloc;
    double latitude, longitude;
    GoogleApiClient googleApiClient;
    LocationRequest mlocatiorequest;
    FirebaseAuth auth;
    Button ride;
    FirebaseUser firebaseUser;
    private String customerId="",destination;
    private LatLng destinationLatlng,pickUpLatlng;
    private int status=0;
    private Boolean isLoggingOut=false;
    private LinearLayout mCustomerInfo;
    private ImageView mCustomer;
    private TextView mCustomername,Mcustomerphone,mCustomerDestination;
    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};//Add alternate colours when alternate routing is set ot true to get different colored route

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        auth= FirebaseAuth.getInstance();
        firebaseUser =auth.getCurrentUser();
        mCustomerInfo= findViewById(R.id.CustomerInfo);
        mCustomer=findViewById(R.id.profileCustomer);
        mCustomerDestination= findViewById(R.id.CustomerDestination);
        mCustomername=findViewById(R.id.CustomerName);
        Mcustomerphone=findViewById(R.id.CustomerPhone);
        ride= findViewById(R.id.rideStatus);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        polylines = new ArrayList<>();

        ride.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (status)
                {

                    case 0: getAssignedCustomer();
                            break;
                    case 1:
                        status=2;
                        ErasePolylines();
                        if(destinationLatlng.latitude!=0.0&& destinationLatlng.longitude!=0.0) {
                        getRouteToMarker(destinationLatlng);
                        }

                        ride.setText("drive completed");
                        break;


                    case 2:
                        recordRide();
                        endRide();
                     break;
                }
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        getAssignedCustomer();

    }




    DatabaseReference AssignedCustomerRef;
    private void getAssignedCustomer() {
        String driverId= FirebaseAuth.getInstance().getCurrentUser().getUid();
         AssignedCustomerRef= FirebaseDatabase.getInstance().getReference().child("Users").child("Driver").child(driverId).child("Customer Request").child("CustomerRideId");
        AssignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    status=1;
                    customerId=dataSnapshot.getValue().toString();

                    getAssignedCustomerPickUpLocation();
                    getAssignedCustomerDestination();
                    getAssignedCustomerInfo();
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

    private void getAssignedCustomerDestination() {
            String driverId= FirebaseAuth.getInstance().getCurrentUser().getUid();
            final DatabaseReference AssignedCustomerRef= FirebaseDatabase.getInstance().getReference().child("Users").child("Driver").child(driverId).child("Customer Request");
            AssignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Map<String,Object> Smap = (Map<String,Object>) dataSnapshot.getValue();
                        if (Smap.get("Destination") != null) {
                            destination = Smap.get("Destination").toString();
                            mCustomerDestination.setText("Destination:--" + destination);
                        }
                        else{
                            mCustomerDestination.setText("Destination:--");
                        }
                        Double destinationlat = 0.0;
                        Double destinationlong = 0.0;
                        if (Smap.get("DestinationLatitude")!=null){
                            destinationlat=Double.valueOf(Smap.get("DestinationLatitude").toString());
                        }
                        if (Smap.get("DestinationLongitude")!=null){
                            destinationlong=Double.valueOf(Smap.get("DestinationLongitude").toString());
                        }
                        destinationLatlng=new LatLng(destinationlat,destinationlong);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }


    private void getAssignedCustomerInfo() {
       mCustomerInfo.setVisibility(View.VISIBLE);
       DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId);
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()&&dataSnapshot.getChildrenCount()>0)
                    {
                        Map<String,Object> map= (Map<String, Object>)dataSnapshot.getValue();
                        if(map.get("name")!=null){
                            mCustomername.setText(map.get("name").toString());
                        }
                        if(map.get("phone")!=null){

                            Mcustomerphone.setText(map.get("phone").toString());
                        }
                        if(map.get("profileImageUrl")!=null){

                            Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mCustomer);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }



    Marker pickUpMarker;
    DatabaseReference AssignedCustomerPickupLocationRef;
    private ValueEventListener AssignedCustomerPickupLocationRefListener;
    private void getAssignedCustomerPickUpLocation() {
         AssignedCustomerPickupLocationRef= FirebaseDatabase.getInstance().getReference().child("Customer Request").child(customerId).child("l");
         AssignedCustomerPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()&& customerId.equals(""))
                {
                    List<Object>map = (List<Object>) dataSnapshot.getValue();
                    double locationlst=0;
                    double locationlng=0;
                    if(map.get(0)!=null) {
                        locationlst = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(1)!=null){
                        locationlng=Double.parseDouble(map.get(1).toString());
                    }
                   pickUpLatlng=new LatLng(locationlst,locationlng);
                    pickUpMarker=  mMap.addMarker(new MarkerOptions().position(pickUpLatlng).title("Pick Up Location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_usermarker )));
                    getRouteToMarker(pickUpLatlng);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getRouteToMarker(LatLng pickUpLatlng) {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false) //Disabled For Now Need to check if alternate route can be used to modify.
                .waypoints(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()), pickUpLatlng)
                .build();
        routing.execute();
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
        getMenuInflater().inflate(R.menu.main, menu);
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
            Intent intent = new Intent(MainActivity.this,HistoryActivity.class);
            intent.putExtra("customerOrDriver","Driver");
            startActivity(intent);
            return true;

        } else if (id == R.id.nav_slideshow) {

            Intent intent= new Intent(MainActivity.this,DriverProfileSettings.class);
            startActivity(intent);
        } else if (id == R.id.nav_manage) {
            isLoggingOut=true;
            DeleteDriverData();
            auth.signOut();
            Intent intent=new Intent(MainActivity.this,FirstActivity.class);
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
                        startActivity(new Intent(MainActivity.this,  FirstActivity.class));
                        finish();
                    }
                }
            };
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void endRide() {
        ride.setText("pickedcustomer");
        ErasePolylines();

        String UserId = auth.getCurrentUser().getUid();
            DatabaseReference driverref= FirebaseDatabase.getInstance().getReference().child("Users").child("Driver").child(UserId).child("Customer Request");
            driverref.removeValue();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Customer Request");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(customerId);
        customerId="";
        if(pickUpMarker!=null)
        {
            pickUpMarker.remove();

        }




        if (pickUpMarker != null) {
            pickUpMarker.remove();
        }
        if (AssignedCustomerPickupLocationRefListener != null) {
            AssignedCustomerRef.removeEventListener(AssignedCustomerPickupLocationRefListener);
        }
        mCustomerInfo.setVisibility(View.GONE);
        mCustomername.setText("");
        Mcustomerphone.setText("");
        mCustomerDestination.setText("Destination:--");
        mCustomer.setImageResource(R.drawable.ic_menu_gallery);

    }
    private void recordRide() {
        String UserId = auth.getCurrentUser().getUid();
        DatabaseReference driverref= FirebaseDatabase.getInstance().getReference().child("Users").child("Driver").child(UserId).child("history");
        DatabaseReference customerref= FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("history");
        DatabaseReference historyref= FirebaseDatabase.getInstance().getReference().child("history");
        String requestID=historyref.push().getKey();

        driverref.child(requestID).setValue(true);
        customerref.child(requestID).setValue(true);

        HashMap map= new HashMap();
        map.put("Driver",UserId);
        map.put("Customer",customerId);
        map.put("Rating",0);
        map.put("Timestamp",getCurrentTimeStamp());
        map.put("Destination",destination);
        map.put("Location/From/Lat",pickUpLatlng.latitude);
        map.put("Location/From/Long",pickUpLatlng.longitude);
        map.put("Location/To/Lat",destinationLatlng.latitude);
        map.put("Location/To/Long",destinationLatlng.longitude);
        historyref.child(requestID).updateChildren(map);
    }

    private Long getCurrentTimeStamp() {
        Long timestamp= System.currentTimeMillis()/1000;
        return timestamp;
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

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
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
        if(getApplicationContext()!=null) {
            mLastLocation = location;
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions();
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
            mMap.animateCamera(cameraUpdate);

            String UserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("DriverAvailable");
            DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("Driver Working");
            GeoFire geoFireWorking = new GeoFire(refWorking);
            GeoFire geoFireAvailable = new GeoFire(refAvailable);
            if (customerId.equals("")) {
                geoFireWorking.removeLocation(UserId);
                geoFireAvailable.setLocation(UserId, new GeoLocation(location.getLatitude(), location.getLongitude()));

            } else {
                geoFireAvailable.removeLocation(UserId);
                geoFireWorking.setLocation(UserId, new GeoLocation(location.getLatitude(), location.getLongitude()));

            }
        }
            //Integrating the data wiith firebase

        }

    private void DeleteDriverData() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        String UserID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("DriverAvailable");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(UserID);
    }

    

    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline =mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingCancelled() {

    }
    private void ErasePolylines(){
        for(Polyline line:polylines){
            line.remove();
        }
        polylines.clear();
    }
}
