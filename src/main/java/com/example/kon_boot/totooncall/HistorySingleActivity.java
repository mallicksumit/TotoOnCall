package com.example.kon_boot.totooncall;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback,RoutingListener{

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private  String rideId,currentUserID,CustomerId,DriverId,userDriverOrCustomer,distance;
    private TextView locationRide;
    private TextView distanceRide;
    private TextView dateRide;
    private TextView Username;
    private TextView PhoneUser;
    private ImageView imageUser;
    private RatingBar mRating;
    private DatabaseReference historyrideinfo;
    private LatLng DestinationLatlng,PickUpLatlng;
    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};//Add alternate colours when alternate routing is set ot true to get different colored route


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);
        mapFragment= (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        rideId = getIntent().getExtras().getString("rideId");
        locationRide= findViewById(R.id.ridelocation);
        distanceRide=findViewById(R.id.ridedistance);
        dateRide=findViewById(R.id.ridedate);
        Username =findViewById(R.id.username);
        PhoneUser=findViewById(R.id.userPhone);
        imageUser=findViewById(R.id.userimage);
        mRating=findViewById(R.id.ratingBar);
        currentUserID= FirebaseAuth.getInstance().getCurrentUser().getUid();
        polylines=new ArrayList<>();

        historyrideinfo= FirebaseDatabase.getInstance().getReference().child("history").child(rideId);
        getInformation();
    }

    private void getInformation() {
        historyrideinfo.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot child:dataSnapshot.getChildren())
                    {
                        if(child.getKey().equals("Customer")){
                            CustomerId=child.getValue().toString();
                            if(!CustomerId.equals(currentUserID)){
                                userDriverOrCustomer="Driver";
                                getUSerInfo("Customers",CustomerId);
                            }
                        }
                        if(child.getKey().equals("Driver")){
                            DriverId=child.getValue().toString();
                            if(!DriverId.equals(currentUserID)){
                                userDriverOrCustomer="Customers";
                               getUSerInfo("Driver",DriverId);
                               displayCustomerrelatedObjects();
                            }
                        }

                            if (child.getKey().equals("distance")){
                                distance = child.getValue().toString();
                                distanceRide.setText(distance.substring(0, Math.min(distance.length(), 5)) + " km");
                        }
                        if(child.getKey().equals("timestamp")){
                           dateRide.setText(getDate(Long.valueOf(child.getValue().toString())));

                        }
                        if(child.getKey().equals("rating")){
                            mRating.setRating(Integer.valueOf(child.getValue().toString()));

                        }
                        if(child.getKey().equals("Destination")){
                            locationRide.setText(child.getValue().toString());
                        }
                        if(child.getKey().equals("Location")){
                            PickUpLatlng=new LatLng(Double.valueOf(child.child("From").child("Lat").getValue().toString()),Double.valueOf(child.child("From").child("Long").getValue().toString()));
                            DestinationLatlng=new LatLng(Double.valueOf(child.child("To").child("Lat").getValue().toString()),Double.valueOf(child.child("To").child("Long").getValue().toString()));
                            if(DestinationLatlng!=new LatLng(0,0));
                            {
                                getRouteToMarker();
                            }

                        }

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void displayCustomerrelatedObjects() {
        mRating.setVisibility(View.VISIBLE);
        mRating.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                historyrideinfo.child("rating").setValue(rating);
                DatabaseReference mDriverRating=FirebaseDatabase.getInstance().getReference().child("Users").child("Driver").child(DriverId).child("rating");
                mDriverRating.child(rideId).setValue(rating);
            }
        });
    }


    private void getUSerInfo(String customersOrdriver, String Id) {
        DatabaseReference userOrcustomer=FirebaseDatabase.getInstance().getReference().child("Users").child(customersOrdriver).child(Id);
        userOrcustomer.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    Map<String,Object> map= (Map<String, Object>)dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        Username.setText(map.get("name").toString());
                    }
                    if(map.get("phone")!=null){
                        PhoneUser.setText(map.get("phone").toString());
                    }
                    if(map.get("profileImageUrl")!=null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(imageUser);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private String getDate(Long time) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(time*1000);
        String date = DateFormat.format("dd-MM-yyyy hh:mm", cal).toString();
        return date;
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
       mMap=googleMap;

    }


    private void getRouteToMarker() {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false) //Disabled For Now Need to check if alternate route can be used to modify.
                .waypoints(PickUpLatlng, DestinationLatlng)
                .build();
        routing.execute();
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

        LatLngBounds.Builder builder= new LatLngBounds.Builder();
        builder.include(PickUpLatlng);
        builder.include(DestinationLatlng);
        LatLngBounds bounds = builder.build();

        int width=getResources().getDisplayMetrics().widthPixels;
        int padding=(int)(width*0.2);
        CameraUpdate cameraUpdate= CameraUpdateFactory.newLatLngBounds(bounds,padding);
        mMap.animateCamera(cameraUpdate);
        mMap.addMarker(new MarkerOptions().position(PickUpLatlng).title("Pick up Location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_usermarker)));
        mMap.addMarker(new MarkerOptions().position(DestinationLatlng).title("Destination").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_rickshaw)));

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
