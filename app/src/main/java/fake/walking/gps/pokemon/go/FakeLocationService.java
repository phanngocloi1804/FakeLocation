package fake.walking.gps.pokemon.go;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by loipn on 7/24/2016.
 */
public class FakeLocationService extends Service {

    private String GPS = "gps";

    private LocationManager locationManager;
    private Timer timer;

    private WindowManager manager;
    
    private WindowManager.LayoutParams layoutParamTop;
    private CustomViewGroup viewTop;

    private WindowManager.LayoutParams layoutParamBottom;
    private CustomViewGroup viewBottom;

    private WindowManager.LayoutParams layoutParamLeft;
    private CustomViewGroup viewLeft;

    private WindowManager.LayoutParams layoutParamRight;
    private CustomViewGroup viewRight;
    
    private int SIZE = 50;
    private float STEP = 0.0002f;
    
    private Handler handler_add;
    private Handler handler_remove;
    
    class MyTimerTask extends TimerTask {

        MyTimerTask() {
        }

        public void run() {
            FakeLocationService.this.setLocation();
        }

    }

    public FakeLocationService() {

    }

    public IBinder onBind(Intent paramIntent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    public void onDestroy() {
        try {
            stop();
        } catch (Exception e) {
        }
    }

    private void pauseThread() {
        try {
            if (this.timer != null) {
                this.timer.cancel();
                this.timer = null;
            }
            try {
                this.locationManager.setTestProviderEnabled(GPS, false);
            } catch (Exception e) {
            }
            try {
                this.locationManager.removeTestProvider(GPS);
            } catch (Exception e2) {
            }
        } catch (Exception e3) {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager.getProvider(GPS) != null) {
                try {
                    locationManager.setTestProviderEnabled(GPS, false);
                    locationManager.removeTestProvider(GPS);
                } catch (Exception e4) {
                }
            }
        }
    }

    private void stop() {
        if (viewTop != null || viewBottom != null || viewLeft != null || viewRight != null) {
            Message message = new Message();
            Bundle bundle = new Bundle();
            bundle.putString("message", "abcd");
            message.setData(bundle);
            handler_remove.sendMessage(message);
        }

        pauseThread();
        stopForeground(true);
        stopSelf();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        handler_add = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                // TODO Auto-generated method stub
                super.handleMessage(msg);
                manager.addView(viewTop, layoutParamTop);
                manager.addView(viewBottom, layoutParamBottom);
                manager.addView(viewLeft, layoutParamLeft);
                manager.addView(viewRight, layoutParamRight);
//                Log.e("service", "adview");
            }
        };

        handler_remove = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                // TODO Auto-generated method stub
                super.handleMessage(msg);
                manager.removeView(viewTop);
                manager.removeView(viewBottom);
                manager.removeView(viewLeft);
                manager.removeView(viewRight);
                viewTop = null;
                viewBottom = null;
                viewLeft = null;
                viewRight = null;
//                Log.e("service", "removeview");
            }
        };

        if (intent != null && intent.getIntExtra(Constants.RESPONSE_TYPE, 1) == 1) {
            stop();
        } else if (intent != null) {
            if (intent.getIntExtra(Constants.RESPONSE_TYPE, 1) == 2) {
                pauseThread();
                startFaking();
            }
        } else {
            pauseThread();
            startFaking();
        }
        return START_STICKY;
    }

    private void startFaking() {
        this.locationManager.addTestProvider(GPS, false, false, false, false, false, false, false, 1, 1);
        this.locationManager.setTestProviderEnabled(GPS, true);
        this.timer = new Timer();
        this.timer.schedule(new MyTimerTask(), 200, 1000);
    }

    @SuppressLint({"NewApi"})
    private void setLocation() {
        try {
            Location location = new Location(GPS);
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            SharedPreferences localSharedPreferences = getApplicationContext().getSharedPreferences("prefs", MODE_PRIVATE);
            double lat = Double.valueOf(localSharedPreferences.getString("lat", "0")).doubleValue();
            double ln = Double.valueOf(localSharedPreferences.getString("ln", "0")).doubleValue();
            location.setLatitude(lat);
            location.setLongitude(ln);
            location.setAccuracy(3.0f);
            location.setAltitude(0.0d);
            location.setTime(System.currentTimeMillis());
            location.setBearing(0.0f);
            this.locationManager.setTestProviderLocation(GPS, location);
            Log.d("setLocation", System.currentTimeMillis() + " " + lat);
        } catch (Exception e) {

        }
        doIt();
    }

    private void doIt() {
//        Log.e("service", "service doit");
        SharedPreferences preferences = getSharedPreferences("screen", MODE_PRIVATE);
        boolean is_show = preferences.getBoolean("is_show", false);
        if (is_show) {
            if (viewTop == null || viewBottom == null || viewLeft == null || viewBottom == null) {
                prepareView();

                Message message = new Message();
                Bundle bundle = new Bundle();
                bundle.putString("message", "abcd");
                message.setData(bundle);
                handler_add.sendMessage(message);
            }
        } else {
            if (viewTop != null || viewBottom != null || viewLeft != null || viewRight != null) {
                Message message = new Message();
                Bundle bundle = new Bundle();
                bundle.putString("message", "abcd");
                message.setData(bundle);
                handler_remove.sendMessage(message);
            }
        }
    }

    private void prepareView() {
        manager = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE));

        setViewTop();
        setViewBottom();
        setViewLeft();
        setViewRight();
    }
    
    private void setViewTop() {
        layoutParamTop = new WindowManager.LayoutParams(100, 100, 2007, 8, -2);
//        layoutParamTop.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
//        layoutParamTop.type = WindowManager.LayoutParams.TYPE_PRIORITY_PHONE;
        layoutParamTop.gravity = Gravity.TOP | Gravity.LEFT;
        layoutParamTop.flags =
                // can touch or click
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |

                        // this is to enable the notification to recieve touch events
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |

                        // Draws over status bar
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;


//        layoutParamTop.width = WindowManager.LayoutParams.MATCH_PARENT;
//        layoutParamTop.height = WindowManager.LayoutParams.MATCH_PARENT;
        float density = getResources().getDisplayMetrics().scaledDensity;
        layoutParamTop.width = (int) (SIZE * density);
        layoutParamTop.height = (int) (SIZE * density);
        layoutParamTop.x = (int) (SIZE * 2 * density);
        layoutParamTop.y = (int) (SIZE * 2 * density);
        layoutParamTop.format = PixelFormat.TRANSPARENT;

        viewTop = new CustomViewGroup(this);
        viewTop.setBackgroundResource(R.drawable.btn_top);

        viewTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences localSharedPreferences = getApplicationContext().getSharedPreferences("prefs", 0);
                double lat = Double.valueOf(localSharedPreferences.getString("lat", "0")).doubleValue();
                double ln = Double.valueOf(localSharedPreferences.getString("ln", "0")).doubleValue();
                SharedPreferences.Editor editor = localSharedPreferences.edit();
                editor.putString("lat", String.valueOf(lat + STEP));
                editor.putString("ln", String.valueOf(ln));
                editor.commit();
//                setFakeLocation();
//                Toast.makeText(FakeLocationService.this, lat + "\n" + ln, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setViewBottom() {
        layoutParamBottom = new WindowManager.LayoutParams(100, 100, 2007, 8, -2);
//        layoutParamBottom.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
//        layoutParamBottom.type = WindowManager.LayoutParams.TYPE_PRIORITY_PHONE;
        layoutParamBottom.gravity = Gravity.TOP | Gravity.LEFT;
        layoutParamBottom.flags =
                // can touch or click
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |

                        // this is to enable the notification to recieve touch events
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |

                        // Draws over status bar
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;


//        layoutParamBottom.width = WindowManager.LayoutParams.MATCH_PARENT;
//        layoutParamBottom.height = WindowManager.LayoutParams.MATCH_PARENT;
        float density = getResources().getDisplayMetrics().scaledDensity;
        layoutParamBottom.width = (int) (SIZE * density);
        layoutParamBottom.height = (int) (SIZE * density);
        layoutParamBottom.x = (int) (SIZE * 2 * density);
        layoutParamBottom.y = (int) (SIZE * 4 * density);
        layoutParamBottom.format = PixelFormat.TRANSPARENT;

        viewBottom = new CustomViewGroup(this);
        viewBottom.setBackgroundResource(R.drawable.btn_bottom);

        viewBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences localSharedPreferences = getApplicationContext().getSharedPreferences("prefs", 0);
                double lat = Double.valueOf(localSharedPreferences.getString("lat", "0")).doubleValue();
                double ln = Double.valueOf(localSharedPreferences.getString("ln", "0")).doubleValue();
                SharedPreferences.Editor editor = localSharedPreferences.edit();
                editor.putString("lat", String.valueOf(lat - STEP));
                editor.putString("ln", String.valueOf(ln));
                editor.commit();
//                setFakeLocation();
//                Toast.makeText(FakeLocationService.this, lat + "\n" + ln, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setViewLeft() {
        layoutParamLeft = new WindowManager.LayoutParams(100, 100, 2007, 8, -2);
//        layoutParamLeft.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
//        layoutParamLeft.type = WindowManager.LayoutParams.TYPE_PRIORITY_PHONE;
        layoutParamLeft.gravity = Gravity.TOP | Gravity.LEFT;
        layoutParamLeft.flags =
                // can touch or click
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |

                        // this is to enable the notification to recieve touch events
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |

                        // Draws over status bar
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;


//        layoutParamLeft.width = WindowManager.LayoutParams.MATCH_PARENT;
//        layoutParamLeft.height = WindowManager.LayoutParams.MATCH_PARENT;
        float density = getResources().getDisplayMetrics().scaledDensity;
        layoutParamLeft.width = (int) (SIZE * density);
        layoutParamLeft.height = (int) (SIZE * density);
        layoutParamLeft.x = (int) (SIZE * 1 * density);
        layoutParamLeft.y = (int) (SIZE * 3 * density);
        layoutParamLeft.format = PixelFormat.TRANSPARENT;

        viewLeft = new CustomViewGroup(this);
        viewLeft.setBackgroundResource(R.drawable.btn_left);

        viewLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences localSharedPreferences = getApplicationContext().getSharedPreferences("prefs", 0);
                double lat = Double.valueOf(localSharedPreferences.getString("lat", "0")).doubleValue();
                double ln = Double.valueOf(localSharedPreferences.getString("ln", "0")).doubleValue();
                SharedPreferences.Editor editor = localSharedPreferences.edit();
                editor.putString("lat", String.valueOf(lat));
                editor.putString("ln", String.valueOf(ln - STEP));
                editor.commit();
//                setFakeLocation();
//                Toast.makeText(FakeLocationService.this, lat + "\n" + ln, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setViewRight() {
        layoutParamRight = new WindowManager.LayoutParams(100, 100, 2007, 8, -2);
//        layoutParamRight.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
//        layoutParamRight.type = WindowManager.LayoutParams.TYPE_PRIORITY_PHONE;
        layoutParamRight.gravity = Gravity.TOP | Gravity.LEFT;
        layoutParamRight.flags =
                // can touch or click
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |

                        // this is to enable the notification to recieve touch events
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |

                        // Draws over status bar
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;


//        layoutParamRight.width = WindowManager.LayoutParams.MATCH_PARENT;
//        layoutParamRight.height = WindowManager.LayoutParams.MATCH_PARENT;
        float density = getResources().getDisplayMetrics().scaledDensity;
        layoutParamRight.width = (int) (SIZE * density);
        layoutParamRight.height = (int) (SIZE * density);
        layoutParamRight.x = (int) (SIZE * 3 * density);
        layoutParamRight.y = (int) (SIZE * 3 * density);
        layoutParamRight.format = PixelFormat.TRANSPARENT;

        viewRight = new CustomViewGroup(this);
        viewRight.setBackgroundResource(R.drawable.btn_right);

        viewRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences localSharedPreferences = getApplicationContext().getSharedPreferences("prefs", 0);
                double lat = Double.valueOf(localSharedPreferences.getString("lat", "0")).doubleValue();
                double ln = Double.valueOf(localSharedPreferences.getString("ln", "0")).doubleValue();
                SharedPreferences.Editor editor = localSharedPreferences.edit();
                editor.putString("lat", String.valueOf(lat));
                editor.putString("ln", String.valueOf(ln + STEP));
                editor.commit();
//                setFakeLocation();
//                Toast.makeText(FakeLocationService.this, lat + "\n" + ln, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

