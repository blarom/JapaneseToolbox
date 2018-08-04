package com.japanesetoolboxapp.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.widget.Toast;

import com.japanesetoolboxapp.R;

public class SettingsFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_japanesetoolbox);

        PreferenceScreen prefScreen = getPreferenceScreen();
        SharedPreferences sharedPreferences = prefScreen.getSharedPreferences();

        // Go through all of the preferences, and set up their preference summary.
        for (int i = 0; i < prefScreen.getPreferenceCount(); i++) {
            Preference currentPreference = prefScreen.getPreference(i);
            if (currentPreference instanceof ListPreference || currentPreference instanceof EditTextPreference) {
                setSummaryForPreference(currentPreference, sharedPreferences);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference currentPreference = findPreference(key);
        if (currentPreference != null) {
            // Updates the summary for the preference
            if (currentPreference instanceof ListPreference || currentPreference instanceof EditTextPreference) {
                setSummaryForPreference(currentPreference, sharedPreferences);
            }
        }
    }

    private void setSummaryForPreference(Preference currentPreference, SharedPreferences sharedPreferences) {

        String currentPreferenceValue = sharedPreferences.getString(currentPreference.getKey(), "");
        if (currentPreference instanceof ListPreference) {
            ListPreference currentListPreference = (ListPreference) currentPreference;
            int prefIndex = currentListPreference.findIndexOfValue(currentPreferenceValue);
            if (prefIndex >= 0)  currentListPreference.setSummary(currentListPreference.getEntries()[prefIndex]);
        }
        else if (currentPreference instanceof EditTextPreference) {
            // For EditTextPreferences, set the summary to the value's simple string representation.
            currentPreference.setSummary("Value: " + sharedPreferences.getString(currentPreference.getKey(), ""));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        Toast error;

        if (preference.getKey().equals(getString(R.string.pref_OCR_image_contrast_key))) {
            error = Toast.makeText(getContext(), "Please select a number between 1 and 10.", Toast.LENGTH_SHORT);
            try {
                float contrast = Float.parseFloat((String) newValue);
                // If the number is outside of the acceptable range, show an error.
                if (contrast > 10 || contrast <= 1) {
                    error.show();
                    return false;
                }
            } catch (NumberFormatException nfe) {
                // If whatever the user entered can't be parsed to a number, show an error
                error.show();
                return false;
            }
        }
        else if (preference.getKey().equals(getString(R.string.pref_OCR_image_saturation_key))) {
            error = Toast.makeText(getContext(), "Please select a number between -255 and 255", Toast.LENGTH_SHORT);
            try {
                float saturation = Float.parseFloat((String) newValue);
                // If the number is outside of the acceptable range, show an error.
                if (saturation > 255 || saturation < -255) {
                    error.show();
                    return false;
                }
            } catch (NumberFormatException nfe) {
                // If whatever the user entered can't be parsed to a number, show an error
                error.show();
                return false;
            }
        }
        else if (preference.getKey().equals(getString(R.string.pref_OCR_image_brightness_key))) {
            error = Toast.makeText(getContext(), "Please select a number between -255 and 255", Toast.LENGTH_SHORT);
            try {
                float brightness = Float.parseFloat((String) newValue);
                // If the number is outside of the acceptable range, show an error.
                if (brightness > 255 || brightness < -255) {
                    error.show();
                    return false;
                }
            } catch (NumberFormatException nfe) {
                // If whatever the user entered can't be parsed to a number, show an error
                error.show();
                return false;
            }
        }
        return false;
    }


}