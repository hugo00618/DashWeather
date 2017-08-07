package info.hugoyu.dashclockweather;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.view.View;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.model.LatLng;

import java.util.Arrays;

/**
 * Created by Hugo on 2017-08-02.
 */

public class MyPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;

    // views
    ListPreference tempUnitPref;
    SwitchPreference gpsEnabledPref;
    Preference locPref;
    EditTextPreference apiKeyPref;

    Context context;
    SharedPreferences sharedPrefs;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);

        // views
        tempUnitPref = (ListPreference) findPreference("tempUnit");
        gpsEnabledPref = (SwitchPreference) findPreference("gpsEnabled");
        locPref = findPreference("loc");
        apiKeyPref = (EditTextPreference) findPreference("apiKey");

        // models
        context = getActivity().getApplicationContext();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        // init views
        updatePreferenceSummary();

        setPreferenceListeners();
    }

    @Override
    public void onResume() {
        super.onResume();

        // listen shared preference changes
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(context, data);
                LatLng placeLatLng = place.getLatLng();

                SharedPreferences.Editor sharedPrefsEdit = sharedPrefs.edit();
                sharedPrefsEdit.putString("lat", String.valueOf(placeLatLng.latitude));
                sharedPrefsEdit.putString("lon", String.valueOf(placeLatLng.longitude));
                sharedPrefsEdit.putString("loc", (String) place.getName());
                sharedPrefsEdit.apply();
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {

            } else if (resultCode == Activity.RESULT_CANCELED) {

            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePreferenceSummary();

        switch (key) {
            case "tempUnit":
                sendSettingsChangeBroadcast("info.hugoyu.dashclockweather.ACTION_REFRESH_UI_ONLY");
                break;
            case "loc":
            case "apiKey":
                sendSettingsChangeBroadcast("info.hugoyu.dashclockweather.ACTION_REFETCH");
                break;
        }
    }

    private void sendSettingsChangeBroadcast(String action) {
        Intent intent = new Intent();
        intent.setAction(action);
        getActivity().sendBroadcast(intent);
    }

    private void updatePreferenceSummary() {
        String tempUnitValue = sharedPrefs.getString("tempUnit", "C");
        int tempUnitIdx = Arrays.asList(getResources().getStringArray(R.array.pref_temp_unit_values)).indexOf(tempUnitValue);
        tempUnitPref.setSummary(getResources().getStringArray(R.array.pref_temp_unit_entries)[tempUnitIdx]);

        if (sharedPrefs.getBoolean("gpsEnabled", false)) {
            locPref.setEnabled(false);
        } else {
            locPref.setEnabled(true);
        }
        locPref.setSummary(sharedPrefs.getString("loc", ""));

        apiKeyPref.setSummary(sharedPrefs.getString("apiKey", ""));
    }

    private void setPreferenceListeners() {
        // gps value change
        gpsEnabledPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                if ((boolean) newValue) { // location enabled
                    // if permission is not granted
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // pop error message
                        Snackbar gpsNotGrantedSnackbar = Snackbar.make(getView(),
                                getString(R.string.pref_error_title_permission_not_granted), Snackbar.LENGTH_LONG);
                        gpsNotGrantedSnackbar.setAction(getString(R.string.pref_error_button_settings),
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        // go to app's setting page
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                                        intent.setData(uri);
                                        startActivity(intent);
                                    }
                                });
                        gpsNotGrantedSnackbar.show();

                        // newValue doesn't get updated
                        return false;
                    }

                    // refresh location
                    SharedUtil.refreshLocation(context);
                }
                return true;
            }
        });

        // location click
        locPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    Intent intent =
                            new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                                    .build(getActivity());
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
                } catch (GooglePlayServicesRepairableException e) {
                    // TODO: Handle the error.
                } catch (GooglePlayServicesNotAvailableException e) {
                    // TODO: Handle the error.
                }
                return false;
            }
        });
    }
}
