package fake.walking.gps.pokemon.go;

import android.content.Context;
import android.content.Intent;

public class ServiceHelper {
    public static final int ERROR = 98;
    public static final int PAUSE = 3;
    public static final int START = 2;
    public static final int STOP = 1;

    public static void pauseService(Context c) {
        Intent i = new Intent(c, FakeLocationService.class);
        i.putExtra(Constants.RESPONSE_TYPE, PAUSE);
        c.startService(i);
    }

    public static void startService(Context c) {
        Intent i = new Intent(c, FakeLocationService.class);
        i.putExtra(Constants.RESPONSE_TYPE, START);
        c.startService(i);
    }

    public static void stopService(Context c) {
        Intent i = new Intent(c, FakeLocationService.class);
        i.putExtra(Constants.RESPONSE_TYPE, STOP);
        c.startService(i);
    }

    public static void stopServiceError(Context c) {
        Intent i = new Intent(c, FakeLocationService.class);
        i.putExtra(Constants.RESPONSE_TYPE, ERROR);
        c.startService(i);
    }
}
