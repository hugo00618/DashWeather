package info.hugoyu.dashclockweather;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by Hugo on 2017-08-01.
 */

public class SharedUtil {

    public static boolean isSettingValid(String lat, String lon, String apiKey) throws SettingInvalidException {

        // lat
        if (lat.equals("")) {
            throw new SettingInvalidException(SettingInvalidException.ERR_CODE_LAT_MISSING);
        }
        try {
            double latDouble = Double.parseDouble(lat);
            if (latDouble < -90 || latDouble > 90) { // lat out of range
                throw new SettingInvalidException(SettingInvalidException.ERR_CODE_LAT_INVALID);
            }
        } catch (NumberFormatException e) {
            throw new SettingInvalidException(SettingInvalidException.ERR_CODE_LAT_INVALID);
        }

        // lon
        if (lon.equals("")) {
            throw new SettingInvalidException(SettingInvalidException.ERR_CODE_LON_MISSING);
        }
        try {
            double lonDouble = Double.parseDouble(lon);
            if (lonDouble < -180 || lonDouble > 180) { // long out of range
                throw new SettingInvalidException(SettingInvalidException.ERR_CODE_LON_INVALID);
            }
        } catch (NumberFormatException e) {
            throw new SettingInvalidException(SettingInvalidException.ERR_CODE_LON_INVALID);
        }

        // api key
        if (apiKey.equals("")) {
            throw new SettingInvalidException(SettingInvalidException.ERR_CODE_API_KEY_MISSING);
        }

        return true;
    }

    static class SettingInvalidException extends Exception {

        public static final int ERR_CODE_LAT_MISSING = 1;
        public static final int ERR_CODE_LAT_INVALID = 2;
        public static final int ERR_CODE_LON_MISSING = 3;
        public static final int ERR_CODE_LON_INVALID = 4;
        public static final int ERR_CODE_API_KEY_MISSING = 5;

        int errCode;

        public SettingInvalidException(int errCode) {
            this.errCode = errCode;
        }

        public int getErrCode() {
            return errCode;
        }
    }

    /**
     * consumes latitude and longitude and returns location string
     *
     * @param lat latitude
     * @param lon longitude
     * @return City Name, State Name
     * @throws IOException
     */
    public static String getLocStr(Context context, double lat, double lon) throws IOException {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        List<String> providerList = locationManager.getAllProviders();
        if (providerList != null && providerList.size() > 0) {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
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

    /**
     * update location data and save it to SharedPreference
     * @param context
     */
    public static void refreshLocation(Context context) {
        try {
            LocationManager mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            Location autoLoc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            double latDouble = autoLoc.getLatitude();
            double lonDouble = autoLoc.getLongitude();
            String locName = SharedUtil.getLocStr(context, latDouble, lonDouble);

            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor sharedPrefsEdit = sharedPrefs.edit();
            sharedPrefsEdit.putString("lat", String.valueOf(latDouble));
            sharedPrefsEdit.putString("lon", String.valueOf(lonDouble));
            sharedPrefsEdit.putString("loc", locName);
            sharedPrefsEdit.apply();
        } catch (IOException e) {

        }
    }
}
