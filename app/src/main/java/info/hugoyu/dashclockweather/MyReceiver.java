package info.hugoyu.dashclockweather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.apps.dashclock.api.DashClockExtension;

/**
 * Created by Hugo on 2017-07-31.
 */

public class MyReceiver extends BroadcastReceiver {

    private static final int REFRESH_INTERVAL = 15; // refresh every 15 minutes

    private static MyReceiver myReceiver;
    private static WeatherExtension weatherExtension;
    private static int tickCount = 0;
    private static boolean isSettingsValid = false;

    public MyReceiver() {

    }

    public MyReceiver(WeatherExtension weatherExtension) {
        this.weatherExtension = weatherExtension;
    }

    public static MyReceiver getInstance(WeatherExtension weatherExtension) {
        myReceiver = new MyReceiver(weatherExtension);
        return myReceiver;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch(intent.getAction()) {
            case Intent.ACTION_TIME_TICK:
                Log.d("MyReceiver", "Time ticked");
                tickCount++;
                if (tickCount % REFRESH_INTERVAL == 0 && isSettingsValid) {
                    weatherExtension.onUpdateData(DashClockExtension.UPDATE_REASON_PERIODIC);
                }
                break;
            case "info.hugoyu.dashclockweather.SETTINGS_CHANGED":
                Log.d("MyReceiver", "Settings changed");
                weatherExtension.onUpdateData(DashClockExtension.UPDATE_REASON_SETTINGS_CHANGED);
                break;
        }
    }

    public void setIsSettingsValid(boolean isSettingsValid) {
        this.isSettingsValid = isSettingsValid;
    }
}
