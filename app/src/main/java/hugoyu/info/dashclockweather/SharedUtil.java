package hugoyu.info.dashclockweather;

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
}
