package com.example.amirulislam.nabilsproject;


import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    Button findDistanceButton=null;
    TextView distanceTextView=null;
    public static final String TAG = MapsActivity.class.getSimpleName();
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;




    Account userAccountDetails=null;
    ChildWallet childWalletDetails=null;

    ArrayList<String> walletList=null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();


        findDistanceButton=(Button)findViewById(R.id.btnFindPath);
        distanceTextView=(TextView)findViewById(R.id.radius);
        //get the wallet details and maintain
        getDetailsFromPreviousIntent();



        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        //**************************
        builder.setAlwaysShow(true); //this is the key ingredient
        //**************************
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    MapsActivity.this,
                                    REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        break;
                }
            }
        });

        //listener for the button
        findDistanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                String dist=distanceTextView.getText().toString();
                dist=dist.substring(0,dist.length()-2);

                childWalletDetails.setRadius(Integer.parseInt(dist));
                Intent showmodifywalletIntent = new Intent(getApplicationContext(), ShowModifyWalletDetailsActivity.class);


                showmodifywalletIntent.putExtra("ShowModifyWalletDetails",childWalletDetails);
                //also put account details
                showmodifywalletIntent.putExtra("userAccountDetails",userAccountDetails);
                //also send the list of wallets as they will be used later
                showmodifywalletIntent.putExtra("walletIDsUnderThisAccount",walletList);


                /*mapActivityIntent.putExtra("ShowModifyWalletDetails",childWalletDetails);
                mapActivityIntent.putExtra("walletIDsUnderThisAccount",walletList);*/
                startActivity(showmodifywalletIntent);


            }
        });


    }//end of oncreate


    protected void getDetailsFromPreviousIntent() {
        //get data from what has been passed
        Intent parentHomeIntent = getIntent();
        userAccountDetails = (Account) parentHomeIntent.getSerializableExtra("userAccountDetails");
        childWalletDetails=(ChildWallet) parentHomeIntent.getSerializableExtra("ShowModifyWalletDetails");


        ArrayList<String> walletIDsList=getIntent().getStringArrayListExtra("walletIDsUnderThisAccount");
        walletList=walletIDsList;

    }



    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }


    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    private void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());
        final double currentLatitude = location.getLatitude();
        final double currentLongitude = location.getLongitude();
        final LatLng latLng = new LatLng(currentLatitude, currentLongitude);
        //mMap.addMarker(new MarkerOptions().position(new LatLng(currentLatitude, currentLongitude)).title("Current Location"));
        final MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title("I am here!");


        mMap.addMarker(options);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,13.0f));

       // Toast.makeText(getApplicationContext(),"touch kar kahi bhi", Toast.LENGTH_SHORT).show();
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {


                mMap.clear();
                mMap.addMarker(options);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,11.0f));
            //    Toast.makeText(getApplicationContext(), point.toString(), Toast.LENGTH_SHORT).show();

                // int radiusM =50000; // your radius in meters
                double latitude =point.latitude; // your center latitude
                double longitude = point.longitude; // your center longitude

                double diff_lat  = -currentLatitude +latitude;
                double diff_long = -currentLongitude + longitude;
                double a = (Math.sin((Math.toRadians(diff_lat)/2.0)) * Math.sin((Math.toRadians(diff_lat)/2.0)) ) + Math.cos(Math.toRadians(currentLatitude))*Math.cos(Math.toRadians(latitude))*Math.sin((Math.toRadians(diff_long)/2))*Math.sin((Math.toRadians(diff_long)/2));
                double c = 2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));
          //      Toast.makeText(getApplicationContext(), "radius is  "+c, Toast.LENGTH_SHORT).show();
                double radiusM =  Math.abs(6371000*c);
        //        Toast.makeText(getApplicationContext(), "radius is  "+radiusM, Toast.LENGTH_SHORT).show();
                ((TextView) findViewById(R.id.radius)).setText(""+ (int)radiusM/1000 +"Km");

                //LatLng latLng = new LatLng(latitude,longitude);
                CircleOptions circleOptions = new CircleOptions().center(latLng).radius(radiusM).strokeColor(Color.BLUE).fillColor(Color.CYAN);

                Circle circle = mMap.addCircle(circleOptions);





                //mMap.addGroundOverlay(new GroundOverlayOptions().image(bmD).position(latLng,radiusM*2,radiusM*2).transparency(0.4f));
                //PolylineOptions polylineOptions = new PolylineOptions().
                //      geodesic(true).
                //    color(Color.BLUE).
                //  width(10);




            }
        });
        // mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    @Override
    public void onConnected(Bundle bundle) {
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
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        else {
            handleNewLocation(location);
        }
    }
    @Override
    public void onConnectionSuspended(int i) {
    }
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }

    }
    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

}