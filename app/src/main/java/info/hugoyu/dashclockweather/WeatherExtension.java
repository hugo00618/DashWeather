package info.hugoyu.dashclockweather;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

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

    boolean isTempUnitC = true;
    String lat, lon;
    String apiKey;

    @Override
    public void onCreate() {
        super.onCreate();

        intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        intentFilter.addAction("info.hugoyu.dashclockweather.SETTINGS_CHANGED");

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
            publishWeatherInfo();
            myReceiver.setIsSettingsValid(true);
        } catch (SharedUtil.SettingInvalidException e) {
            publishErrorInfo(e);
            myReceiver.setIsSettingsValid(false);
        }
    }

    private void publishWeatherInfo() {
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
                            final int iconId = getIconId(iconName);

                            // current temperature
                            double curTempDouble = 0;
                            if (isTempUnitC) {
                                curTempDouble = curWeather.getDouble("temp_c");
                            } else {
                                curTempDouble = curWeather.getDouble("temp_f");
                            }
                            final String curTempStr = new BigDecimal(curTempDouble).setScale(0, BigDecimal.ROUND_HALF_UP).toString();

                            // current relative humidity
                            final String curRelHum = curWeather.getString("relative_humidity");

                            // description
                            final String curWeatherDesc = curWeather.getString("weather");

                            // geo info
                            String geoStr = "";
                            try {
                                geoStr = getGeoStr(Double.parseDouble(lat), Double.parseDouble(lon));
                            } catch (IOException e) {

                            }
                            final String finalGeoStr = geoStr;

                            client.get(API_CALL_ROOT + apiKey + API_CALL_QUERY_FORECAST + lat + "," + lon + ".json",
                                    null, new AsyncHttpResponseHandler() {
                                        @Override
                                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                                            try {
                                                JSONObject responseForecast = new JSONObject(new String(responseBody, "UTF-8"));
                                                JSONObject forecast = responseForecast.getJSONObject("forecast");
                                                JSONObject todayForecast = forecast.getJSONObject("simpleforecast").getJSONArray("forecastday").getJSONObject(0);

                                                String body = "";

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

                                                // Publish the extension data update.
                                                publishUpdate(new ExtensionData()
                                                        .visible(true)
                                                        .icon(iconId)
                                                        .status(curTempStr + degreeChar)
                                                        .expandedTitle(curTempStr + degreeChar + " - " + curWeatherDesc)
                                                        .expandedBody(body)
//                                        .contentDescription("Completely different text for accessibility if needed.")
                                                        .clickIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(URL_REDIRECT + lat + "," + lon))));
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

    /**
     * consumes latitude and longitude and returns location string
     *
     * @param lat latitude
     * @param lon longitude
     * @return City Name, State Name
     * @throws IOException
     */
    private String getGeoStr(double lat, double lon) throws IOException {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        List<String> providerList = locationManager.getAllProviders();
        if (providerList != null && providerList.size() > 0) {
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
            try {
                List<Address> listAddresses = geocoder.getFromLocation(lat, lon, 1);
                if (listAddresses != null && listAddresses.size() > 0) {
                    Address myAddress = listAddresses.get(0);
                    return myAddress.getLocality() + ", " + myAddress.getAdminArea();
                }
            } catch (IOException e) {
                throw e;
            }
        }

        throw new IOException();
    }
}
