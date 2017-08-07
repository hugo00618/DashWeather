package info.hugoyu.dashclockweather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.apps.dashclock.api.DashClockExtension;

/**
 * Created by Hugo on 2017-07-31.
 */

public class MyReceiver extends BroadcastReceiver {

    public static final int UPDATE_REASON_UI_PREF_CHANGED = 618;

    private static final int REFRESH_INTERVAL_WEATHER = 15; // refresh weather every 15 minutes
    private static final int REFRESH_INTERVAL_LOCATION = 60; // refresh location every 60 minutes

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
        switch (intent.getAction()) {
            case Intent.ACTION_TIME_TICK:
                Log.d("MyReceiver", "Time ticked");
                tickCount++;

                if (tickCount % REFRESH_INTERVAL_LOCATION == 0) {
                    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
                    boolean isGpsEnabled = sharedPrefs.getBoolean("gpsEnabled", false);

                    if (isGpsEnabled) {
                        SharedUtil.refreshLocation(context);
                    }
                }

                if (tickCount % REFRESH_INTERVAL_WEATHER == 0 && isSettingsValid) {
                    weatherExtension.onUpdateData(DashClockExtension.UPDATE_REASON_PERIODIC);
                }
                break;
            case "info.hugoyu.dashclockweather.ACTION_REFETCH":
                Log.d("MyReceiver", "Settings changed");
                weatherExtension.onUpdateData(DashClockExtension.UPDATE_REASON_SETTINGS_CHANGED);
                break;
            case "info.hugoyu.dashclockweather.ACTION_REFRESH_UI_ONLY":
                Log.d("MyReceiver", "UI pref changed");
                weatherExtension.onUpdateData(UPDATE_REASON_UI_PREF_CHANGED);
                break;
        }
    }

    public void setIsSettingsValid(boolean isSettingsValid) {
        this.isSettingsValid = isSettingsValid;
    }
}
