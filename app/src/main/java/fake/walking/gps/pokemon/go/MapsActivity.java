package fake.walking.gps.pokemon.go;

import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
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
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        View.OnClickListener {

    private String AD_UNIT_ID_FULL = "ca-app-pub-4660021458698818/3892873288";
    private String AD_UNIT_ID_BANNER = "ca-app-pub-4660021458698818/9939406881";
    private InterstitialAd interstitial;
    private LinearLayout lnlAdView;
    private AdView adView;
    private AdRequest adRequest;

    private GoogleMap mMap;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private LatLng currentLatLng;

    private TextView txtStart;

    private EditText edtSearch;
    private RelativeLayout rltSearch;

    private FloatingActionsMenu floatingActionsMenu;
    private FloatingActionButton btnRate;
    private FloatingActionButton btnShare;

    private RelativeLayout rltInfo;
    private RelativeLayout rltLocate;

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

        checkShowBannerAd();

        if (checkGPS()) {
            checkMockLocationMode();
        }
    }

    private void initView() {
        txtStart = (TextView) findViewById(R.id.txtStart);
        edtSearch = (EditText) findViewById(R.id.edtSearch);
        rltSearch = (RelativeLayout) findViewById(R.id.rltSearch);
        floatingActionsMenu = (FloatingActionsMenu) findViewById(R.id.floatingActionsMenu);
        btnRate = (FloatingActionButton) findViewById(R.id.btnRate);
        btnShare = (FloatingActionButton) findViewById(R.id.btnShare);
        rltInfo = (RelativeLayout) findViewById(R.id.rltInfo);
        rltLocate = (RelativeLayout) findViewById(R.id.rltLocate);
        lnlAdView = (LinearLayout) findViewById(R.id.lnlAdView);

        txtStart.setOnClickListener(this);
        rltSearch.setOnClickListener(this);
        btnRate.setOnClickListener(this);
        btnShare.setOnClickListener(this);
        rltInfo.setOnClickListener(this);
        rltLocate.setOnClickListener(this);

        edtSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                int result = actionId & EditorInfo.IME_MASK_ACTION;
                switch (result) {
                    case EditorInfo.IME_ACTION_SEARCH:
                        search();
                        break;
                }
                return false;
            }
        });
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
            case R.id.rltSearch:
                search();
                break;
            case R.id.btnRate:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName().toString())));
                floatingActionsMenu.collapse();
                break;
            case R.id.btnShare:
                shareApp("https://play.google.com/store/apps/details?id=" + getPackageName().toString());
                floatingActionsMenu.collapse();
                break;
            case R.id.rltInfo:
                showPopupInfo();
                break;
            case R.id.rltLocate:
                if (mMap != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17));
                }
                break;
        }
    }

    private void shareApp(String url) {
        Intent share = new Intent(android.content.Intent.ACTION_SEND);
        share.setType("text/plain");
        share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        // Add data to the intent, the receiving app will decide
        // what to do with it.
        share.putExtra(Intent.EXTRA_SUBJECT, "Use this app");
        share.putExtra(Intent.EXTRA_TEXT, url);

        startActivity(Intent.createChooser(share, "Share this app"));
    }

    private void search() {
        String search = edtSearch.getText().toString().trim();
        if (search.length() > 0) {
            if (isNetworkAvailable(this)) {
                SearchAsync searchAsync = new SearchAsync();
                searchAsync.execute(search);
            } else {
                Toast.makeText(this, "Please your internet connection", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please enter key search", Toast.LENGTH_SHORT).show();
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
            checkShowFullAd();
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
                    txtStart.setBackgroundResource(R.drawable.btn_stop);
                } else {
                    txtStart.setText("Start Fake GPS");
                    txtStart.setBackgroundResource(R.drawable.btn_start);
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

        SharedPreferences preferences = getSharedPreferences("app", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        boolean first_run = preferences.getBoolean("first_run", true);
        if (first_run) {
            showPopupInfo();
            editor.putBoolean("first_run", false);
            editor.commit();
        }
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
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17));
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

    private class SearchAsync extends AsyncTask<String, Void, List<Address>> {

        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MapsActivity.this);
            progressDialog.show();
        }

        @Override
        protected List<Address> doInBackground(String... strings) {
            List<Address> list = new ArrayList<>();

            try {
                String url = "http://maps.google.com/maps/api/geocode/json?address=" + strings[0];
                GenericUrl requestUrl = new GenericUrl(url);

                HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
                int TIME_OUT = 10 * 1000;

                HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();

                HttpRequest request = requestFactory.buildGetRequest(requestUrl);
                request.setReadTimeout(TIME_OUT);
                request.setConnectTimeout(TIME_OUT);

                HttpResponse response = request.execute();
                if (response.isSuccessStatusCode()) {
                    String result = response.parseAsString();
                    try {
                        JSONObject root = new JSONObject(result);
                        JSONArray results = root.getJSONArray("results");
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject object = results.getJSONObject(i);
                            String name = object.getString("formatted_address");
                            JSONObject geometry = object.getJSONObject("geometry");
                            JSONObject location = geometry.getJSONObject("location");
                            double lat = location.getDouble("lat");
                            double lng = location.getDouble("lng");
                            Address address = new Address(name, new LatLng(lat, lng));
                            list.add(address);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return list;
        }

        @Override
        protected void onPostExecute(List<Address> list) {
            super.onPostExecute(list);
            progressDialog.dismiss();
            edtSearch.setText("");
            showListSearchResult(list);
        }
    }

    private void showListSearchResult(final List<Address> list) {
        final Dialog dialog = new Dialog(this);

//        dialog.getWindow().clearFlags(
//                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(R.layout.popup_list);

        ListView listView = (ListView) dialog.findViewById(R.id.listView);

        SearchAdapter.Callback callback = new SearchAdapter.Callback() {
            @Override
            public void onClickItem(int position) {
                if (Util.isMyServiceRunning(MapsActivity.this)) {
                    currentLatLng = list.get(position).getLatLng();
                    SharedPreferences.Editor editorlocation = getSharedPreferences("prefs", MODE_PRIVATE).edit();
                    editorlocation.putString("lat", String.valueOf(currentLatLng.latitude));
                    editorlocation.putString("ln", String.valueOf(currentLatLng.longitude));
                    editorlocation.commit();
                } else {
                    currentLatLng = list.get(position).getLatLng();
                    setMarker();
                    if (mMap != null) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17));
                    }
                }
                dialog.dismiss();
            }
        };

        SearchAdapter adapter = new SearchAdapter(this, list, callback);
        listView.setAdapter(adapter);

        dialog.show();
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private void showPopupInfo() {
        final Dialog dialog = new Dialog(this);

//        dialog.getWindow().clearFlags(
//                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(R.layout.popup_info);

        ((TextView) dialog.findViewById(R.id.txt1)).setText(getString(R.string.guide1));
        ((TextView) dialog.findViewById(R.id.txt2)).setText(getString(R.string.guide2));
        ((TextView) dialog.findViewById(R.id.txt3)).setText(getString(R.string.guide3));
        ((TextView) dialog.findViewById(R.id.txt4)).setText(getString(R.string.guide4));
        ((TextView) dialog.findViewById(R.id.txt5)).setText(getString(R.string.guide5));
        ((TextView) dialog.findViewById(R.id.txt6)).setText(getString(R.string.guide6));

        TextView txtOk = (TextView) dialog.findViewById(R.id.txtOk);
        txtOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void initialAdmobFull() {
        adRequest = new AdRequest.Builder().build();

        interstitial = new InterstitialAd(this);
        interstitial.setAdUnitId(AD_UNIT_ID_FULL);

        interstitial.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // TODO Auto-generated method stub
                super.onAdLoaded();
                interstitial.show();
            }

            @Override
            public void onAdClosed() {
                // TODO Auto-generated method stub
                super.onAdClosed();
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // TODO Auto-generated method stub
                super.onAdFailedToLoad(errorCode);
            }

        });

        interstitial.loadAd(adRequest);
    }

    private void showFullAd() {
        if (interstitial != null) {
            if (interstitial.isLoaded()) {
                interstitial.show();
            }
        }
    }

    public void initialAdmobBanner() {
        adView = new AdView(this);
        adRequest = new AdRequest.Builder().build();
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(AD_UNIT_ID_BANNER);

        lnlAdView.addView(adView);
        adView.loadAd(adRequest);
    }

    private void checkShowFullAd() {
        SharedPreferences preferences = getSharedPreferences("admob", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        int count = preferences.getInt("full_count", 1);
        if (count % 2 == 0) {
            initialAdmobFull();
        } else {
            count++;
            editor.putInt("full_count", count);
            editor.commit();
        }
    }

    private void checkShowBannerAd() {
        SharedPreferences preferences = getSharedPreferences("admob", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        int count = preferences.getInt("banner_count", 1);
        if (count > 2) {
            initialAdmobBanner();
        } else {
            count++;
            editor.putInt("banner_count", count);
            editor.commit();
        }
    }
}
