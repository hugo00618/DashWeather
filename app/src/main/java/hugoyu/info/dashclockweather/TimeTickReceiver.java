package hugoyu.info.dashclockweather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.apps.dashclock.api.DashClockExtension;

/**
 * Created by Hugo on 2017-07-31.
 */

public class TimeTickReceiver extends BroadcastReceiver {

    static final int REFRESH_INTERVAL = 15; // refresh every 15 minutes

    WeatherExtension weatherExtension;
    static TimeTickReceiver timeTickReceiver;
    static int tickCount = 0;


    private TimeTickReceiver(WeatherExtension weatherExtension) {
        this.weatherExtension = weatherExtension;
    }

    public static TimeTickReceiver getInstance(WeatherExtension weatherExtension) {
        timeTickReceiver = new TimeTickReceiver(weatherExtension);
        return timeTickReceiver;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
            tickCount++;
            if (tickCount % REFRESH_INTERVAL == 0) {
                Log.d("DashWeather", "updated from receiver");
                weatherExtension.onUpdateData(DashClockExtension.UPDATE_REASON_PERIODIC);
            }
        }
    }
}
