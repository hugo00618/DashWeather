package info.hugoyu.dashclockweather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.apps.dashclock.api.DashClockExtension;

/**
 * Created by Hugo on 2017-08-02.
 */

public class SettingsChangeReceiver extends BroadcastReceiver {

    WeatherExtension weatherExtension;

    public SettingsChangeReceiver() {

    }

    public SettingsChangeReceiver(WeatherExtension weatherExtension) {
        this.weatherExtension = weatherExtension;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("info.hugoyu.dashclockweather.SETTINGS_CHANGED")) {
            Log.d("DashWeather", "updated from settings receiver");
            weatherExtension.onUpdateData(DashClockExtension.UPDATE_REASON_SETTINGS_CHANGED);
        }
    }
}
