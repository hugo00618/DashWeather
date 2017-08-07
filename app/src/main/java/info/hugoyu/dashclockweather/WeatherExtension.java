package info.hugoyu.dashclockweather;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;

import cz.msebera.android.httpclient.Header;

/**
 * Created by Hugo on 2017-07-31.
 */

public class WeatherExtension extends DashClockExtension {
    public static String API_CALL_ROOT = "http://api.wunderground.com/api/";
    public static String API_CALL_QUERY_FORECAST = "/forecast/q/";
    public static String API_CALL_QUERY_CURRENT = "/conditions/q/";
    public static String URL_REDIRECT = "https://weather.com/weather/today/l/";

    public static char degreeChar = (char) 0x00B0; // character for °

    IntentFilter intentFilter;
    MyReceiver myReceiver;

    // weather info
    int iconId;
    double curTempDoubleC, curTempDoubleF;
    String curWeatherDesc;
    String body;

    // pref
    boolean isTempUnitC = true;
    String lat, lon;
    String apiKey;

    @Override
    public void onCreate() {
        super.onCreate();

        intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK);

        myReceiver = MyReceiver.getInstance(this);
        registerReceiver(myReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myReceiver);
    }

    @Override
    protected void onUpdateData(int reason) {
        // Get preference value.
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        isTempUnitC = sharedPrefs.getString("tempUnit", "C").equals("C");
        lat = sharedPrefs.getString("lat", "");
        lon = sharedPrefs.getString("lon", "");
        apiKey = sharedPrefs.getString("apiKey", "");

        try {
            SharedUtil.isSettingValid(lat, lon, apiKey);

            myReceiver.setIsSettingsValid(true);

            switch (reason) {
                case MyReceiver.UPDATE_REASON_UI_PREF_CHANGED:
                    publishWeatherInfo();
                    break;
                default:
                    fetchAndPublishWeatherInfo();
                    break;
            }
        } catch (SharedUtil.SettingInvalidException e) {
            publishErrorInfo(e);
            myReceiver.setIsSettingsValid(false);
        }


    }

    private void fetchAndPublishWeatherInfo() {
        final AsyncHttpClient client = new AsyncHttpClient();

        client.get(API_CALL_ROOT + apiKey + API_CALL_QUERY_CURRENT + lat + "," + lon + ".json",
                null, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        try {
                            JSONObject responseCur = new JSONObject(new String(responseBody, "UTF-8"));
                            JSONObject curWeather = responseCur.getJSONObject("current_observation");

                            // icon
                            String iconUrl = curWeather.getString("icon_url");
                            String iconNameWithExt = iconUrl.substring(iconUrl.lastIndexOf('/') + 1);
                            String iconName = iconNameWithExt.substring(0, iconNameWithExt.lastIndexOf('.'));
                            iconId = getIconId(iconName);

                            // current temperature
                            curTempDoubleC = curWeather.getDouble("temp_c");
                            curTempDoubleF = curWeather.getDouble("temp_f");

                            // current relative humidity
                            final String curRelHum = curWeather.getString("relative_humidity");

                            // description
                            curWeatherDesc = curWeather.getString("weather");

                            // geo info
                            String locStr = "";
                            try {
                                locStr = SharedUtil.getLocStr(getApplicationContext(), Double.parseDouble(lat), Double.parseDouble(lon));
                            } catch (IOException e) {

                            }
                            final String finalGeoStr = locStr;

                            client.get(API_CALL_ROOT + apiKey + API_CALL_QUERY_FORECAST + lat + "," + lon + ".json",
                                    null, new AsyncHttpResponseHandler() {
                                        @Override
                                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                                            try {
                                                JSONObject responseForecast = new JSONObject(new String(responseBody, "UTF-8"));
                                                JSONObject forecast = responseForecast.getJSONObject("forecast");
                                                JSONObject todayForecast = forecast.getJSONObject("simpleforecast").getJSONArray("forecastday").getJSONObject(0);

                                                body = "";

                                                // high/low
                                                String highTemp = "", lowTemp = "";
                                                if (isTempUnitC) {
                                                    highTemp = todayForecast.getJSONObject("high").getString("celsius");
                                                    lowTemp = todayForecast.getJSONObject("low").getString("celsius");
                                                } else {
                                                    highTemp = todayForecast.getJSONObject("high").getString("fahrenheit");
                                                    lowTemp = todayForecast.getJSONObject("low").getString("fahrenheit");
                                                }
                                                body += highTemp + degreeChar + "/" + lowTemp + degreeChar;
                                                // geo
                                                body += "    ";
                                                body += finalGeoStr;
                                                // humidity
//                                                body += "\n" + getString(R.string.rel_humidity) + ": " + curRelHum;
                                                // chance of rain
//                                                String chanceOfRain = todayForecast.getString("pop");
//                                                body += "\n" + getString(R.string.chance_of_rain) + ": " + chanceOfRain + "%";

                                                publishWeatherInfo();

                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void onFailure(int statusCode, Header[] headers,
                                                              byte[] responseBody, Throwable error) {

                                        }
                                    });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers,
                                          byte[] responseBody, Throwable error) {

                    }
                });
    }

    private void publishWeatherInfo() {
        String curTempStr;
        if (isTempUnitC) {
            curTempStr = new BigDecimal(curTempDoubleC).setScale(0, BigDecimal.ROUND_HALF_UP).toString();
        } else {
            curTempStr = new BigDecimal(curTempDoubleF).setScale(0, BigDecimal.ROUND_HALF_UP).toString();
        }

        publishUpdate(new ExtensionData()
                .visible(true)
                .icon(iconId)
                .status(curTempStr + degreeChar)
                .expandedTitle(curTempStr + degreeChar + " - " + curWeatherDesc)
                .expandedBody(body)
//                                        .contentDescription("Completely different text for accessibility if needed.")
                .clickIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(URL_REDIRECT + lat + "," + lon))));
    }

    private void publishErrorInfo(SharedUtil.SettingInvalidException e) {
        String errorTitle = getString(R.string.error_title) + ": ";
        String errorBody = getString(R.string.error_body);

        switch (e.getErrCode()) {
            case SharedUtil.SettingInvalidException.ERR_CODE_LAT_MISSING:
            case SharedUtil.SettingInvalidException.ERR_CODE_LAT_INVALID:
            case SharedUtil.SettingInvalidException.ERR_CODE_LON_MISSING:
            case SharedUtil.SettingInvalidException.ERR_CODE_LON_INVALID:
                errorTitle += getString(R.string.pref_loc_title);
                break;
            case SharedUtil.SettingInvalidException.ERR_CODE_API_KEY_MISSING:
                errorTitle += getString(R.string.pref_api_key_title);
                break;
        }
        publishUpdate(new ExtensionData()
                .visible(true)
                .icon(R.mipmap.ic_error)
                .status(getString(R.string.error_title))
                .expandedTitle(errorTitle)
                .expandedBody(errorBody)
//                                        .contentDescription("Completely different text for accessibility if needed.")
                .clickIntent(new Intent(this, MyPreferenceActivity.class)));
    }

    /**
     * @param iconName name of the icon in API response "icon_url" tag
     * @return drawable icon id
     */
    public static int getIconId(String iconName) {
        int iconId = -1;
        switch (iconName) {
            case "chanceflurries": // snow_dn
            case "nt_chanceflurries":
            case "chancesnow":
            case "nt_chancesnow":
            case "flurries":
            case "nt_flurries":
            case "snow":
            case "nt_snow":
                iconId = R.mipmap.ic_snow_dn;
                break;
            case "chancesleet": // freeze_rain_dn
            case "nt_chancesleet":
            case "sleet":
            case "nt_sleet":
                iconId = R.mipmap.ic_snow_dn;
                break;
            case "chancerain": // rain_dn
            case "nt_ancerain":
            case "rain":
            case "nt_rain":
                iconId = R.mipmap.ic_rain_dn;
                break;
            case "chancetstorms": // thunderstorm_dn
            case "nt_chancetstorms":
            case "tstorms":
            case "nt_tstorms":
            case "unknown":
            case "nt_unknown":
                iconId = R.mipmap.ic_thunder_dn;
                break;
            case "clear": // clear_d
            case "sunny":
                iconId = R.mipmap.ic_clear_d;
                break;
            case "nt_clear": // clear_n
            case "nt_sunny":
                iconId = R.mipmap.ic_clear_n;
                break;
            case "cloudy": // cloud_dn
            case "nt_cloudy": // cloud_dn
                iconId = R.mipmap.ic_cloud_dn;
                break;
            case "fog": // fog_dn
            case "nt_fog":
            case "hazy":
            case "nt_hazy":
                iconId = R.mipmap.ic_fog_dn;
                break;
            case "mostlycloudy": // part_cloud_d
            case "mostlysunny":
            case "partlycloudy":
            case "partlysunny":
                iconId = R.mipmap.ic_part_cloud_d;
                break;
            case "nt_mostlycloudy": // part_cloud_n
            case "nt_mostlysunny":
            case "nt_partlycloudy":
            case "nt_partlysunny":
                iconId = R.mipmap.ic_part_cloud_n;
                break;
        }
        return iconId;
    }
}
