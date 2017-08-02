package hugoyu.info.dashclockweather;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;

import java.util.Arrays;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        // views
        ListPreference tempUnitPref;
        SwitchPreference gpsEnabledPref;
        EditTextPreference latPref, lonPref;
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
            latPref = (EditTextPreference) findPreference("lat");
            lonPref = (EditTextPreference) findPreference("lon");
            apiKeyPref = (EditTextPreference) findPreference("apiKey");

            // models
            context = getActivity().getApplicationContext();
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

            // init views
            updatePreferenceSummary();
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            updatePreferenceSummary();
        }

        private void updatePreferenceSummary() {
            String tempUnitValue = sharedPrefs.getString("tempUnit", "C");
            int tempUnitIdx = Arrays.asList(getResources().getStringArray(R.array.pref_temp_unit_values)).indexOf(tempUnitValue);
            tempUnitPref.setSummary(getResources().getStringArray(R.array.pref_temp_unit_entries)[tempUnitIdx]);

            latPref.setSummary(sharedPrefs.getString("lat", ""));
            lonPref.setSummary(sharedPrefs.getString("lon", ""));
            apiKeyPref.setSummary(sharedPrefs.getString("apiKey", ""));
        }
    }
}