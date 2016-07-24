package fake.walking.gps.pokemon.go;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        View.OnClickListener {

    private GoogleMap mMap;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private LatLng currentLatLng;

    private TextView txtStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        initView();
        initData();
    }

    private void initView() {
        txtStart = (TextView) findViewById(R.id.txtStart);

        txtStart.setOnClickListener(this);
    }

    private void initData() {
        long interval = 10 * 1000;   // 10 seconds, in milliseconds
        long fastestInterval = 1 * 1000;  // 1 second, in milliseconds
        float minDisplacement = 0;

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();


        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setInterval(interval)
                .setFastestInterval(fastestInterval)
                .setSmallestDisplacement(minDisplacement);

        setBtnStart();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.txtStart:
                pressStart();
                setBtnStart();
                break;
        }
    }

    private void pressStart() {
        if (!checkGPS()) {
            return;
        }
        if (!checkMockLocationMode()) {
            return;
        }

        SharedPreferences.Editor editorlocation = getSharedPreferences("prefs", MODE_PRIVATE).edit();
        editorlocation.putString("lat", String.valueOf(currentLatLng.latitude));
        editorlocation.putString("ln", String.valueOf(currentLatLng.longitude));
        editorlocation.commit();

        SharedPreferences preferences = getSharedPreferences("screen", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        if (Util.isMyServiceRunning(this)) {
            ServiceHelper.stopService(this);
            editor.putBoolean("is_show", false);
        } else {
            ServiceHelper.startService(this);
            editor.putBoolean("is_show", true);
        }
        editor.commit();
    }

    private void setBtnStart() {
        txtStart.setEnabled(false);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Util.isMyServiceRunning(MapsActivity.this)) {
                    txtStart.setText("Stop Fake GPS");
                } else {
                    txtStart.setText("Start Fake GPS");
                }
                txtStart.setEnabled(true);
            }
        }, 1000);
    }

    private void setMarker() {
        if (mMap != null) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Fake here"));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMapClickListener(this);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (Util.isMyServiceRunning(this)) {
            currentLatLng = latLng;
            SharedPreferences.Editor editorlocation = getSharedPreferences("prefs", MODE_PRIVATE).edit();
            editorlocation.putString("lat", String.valueOf(currentLatLng.latitude));
            editorlocation.putString("ln", String.valueOf(currentLatLng.longitude));
            editorlocation.commit();
        } else {
            currentLatLng = latLng;
            setMarker();
        }
    }

    @Override
    public void onLocationChanged(Location loc) {
//        currentLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());
//        setMarker();
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            setMarker();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            setMarker();
            if (mMap != null) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17));
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    // ###Check if GPS provider is enabled
    private boolean checkGPS() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage("Enable GPS");
            dialog.setPositiveButton("Open location setting",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(
                                DialogInterface paramDialogInterface,
                                int paramInt) {
                            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(myIntent);
                        }
                    });
            dialog.setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(
                                DialogInterface paramDialogInterface,
                                int paramInt) {

                        }
                    });
            dialog.show();
            return false;
        } else {
            return true;
        }
    }

    // ###Check if mock location is enabled
    private boolean checkMockLocationMode() {
        boolean isMockLocation = false;

        //if marshmallow
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                AppOpsManager opsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
                isMockLocation = (opsManager.checkOp(AppOpsManager.OPSTR_MOCK_LOCATION, android.os.Process.myUid(), getPackageName()) == AppOpsManager.MODE_ALLOWED);
            } catch (Exception e) {

            }
        } else {
            // in marshmallow this will always return true
            isMockLocation = !Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ALLOW_MOCK_LOCATION).equals("0");
        }

        if (!isMockLocation) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage("Enable mock location");
            dialog.setPositiveButton("Open developer setting",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(
                                DialogInterface paramDialogInterface,
                                int paramInt) {
                            Intent myIntent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                            startActivity(myIntent);
                        }
                    });
            dialog.setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(
                                DialogInterface paramDialogInterface,
                                int paramInt) {
                        }
                    });
            dialog.show();
        }

        return isMockLocation;
    }
}
