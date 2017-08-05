package info.hugoyu.dashclockweather;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.util.Log;

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

        // listen shared preference changes
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(context, data);
                LatLng placeLatLng = place.getLatLng();

                SharedPreferences.Editor sharedPrefsEdit = sharedPrefs.edit();
                sharedPrefsEdit.putString("lat", String.valueOf(placeLatLng.latitude));
                sharedPrefsEdit.putString("lon", String.valueOf(placeLatLng.longitude));
                sharedPrefsEdit.putString("loc", (String) place.getName());
                sharedPrefsEdit.apply();

                Log.d("new lat" , PreferenceManager.getDefaultSharedPreferences(context).getString("lat", ""));
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {

            } else if (resultCode == Activity.RESULT_CANCELED) {

            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePreferenceSummary();

        Intent intent = new Intent();
        intent.setAction("info.hugoyu.dashclockweather.SETTINGS_CHANGED");
        getActivity().sendBroadcast(intent);
    }

    private void updatePreferenceSummary() {
        String tempUnitValue = sharedPrefs.getString("tempUnit", "C");
        int tempUnitIdx = Arrays.asList(getResources().getStringArray(R.array.pref_temp_unit_values)).indexOf(tempUnitValue);
        tempUnitPref.setSummary(getResources().getStringArray(R.array.pref_temp_unit_entries)[tempUnitIdx]);
        locPref.setSummary(sharedPrefs.getString("loc", ""));
        apiKeyPref.setSummary(sharedPrefs.getString("apiKey", ""));
    }
}
