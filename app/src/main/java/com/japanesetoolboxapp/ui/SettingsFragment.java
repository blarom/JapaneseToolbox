package com.japanesetoolboxapp.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
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
                checkIfValueIsInRangeOrWarnUser(currentPreference, sharedPreferences);
                setSummaryForPreference(currentPreference, sharedPreferences);
            }
        }
    }
    private void checkIfValueIsInRangeOrWarnUser(Preference preference, SharedPreferences sharedPreferences) {
        String newValue = sharedPreferences.getString(preference.getKey(), "");

        Toast error;

        if (preference.getKey().equals(getString(R.string.pref_OCR_image_contrast_key))) {
            error = Toast.makeText(getContext(), "Values outside of the range [ "
                    + getString(R.string.pref_OCR_image_contrast_min_display_value)
                    + " : "
                    + getString(R.string.pref_OCR_image_contrast_max_display_value)
                    + " ] are ignored.", Toast.LENGTH_SHORT);
            try {
                float contrast = Float.parseFloat((String) newValue);
                // If the number is outside of the acceptable range, show an error.
                if (contrast > Float.valueOf(getString(R.string.pref_OCR_image_contrast_max_display_value))
                        || contrast < Float.valueOf(getString(R.string.pref_OCR_image_contrast_min_display_value))) {
                    error.show();
                }
            } catch (NumberFormatException nfe) {
                // If whatever the user entered can't be parsed to a number, show an error
                error.show();
            }
        }
        else if (preference.getKey().equals(getString(R.string.pref_OCR_image_saturation_key))) {
            error = Toast.makeText(getContext(), "Values outside of the range [ "
                    + getString(R.string.pref_OCR_image_saturation_min_display_value)
                    + " : "
                    + getString(R.string.pref_OCR_image_saturation_max_display_value)
                    + " ] are ignored.", Toast.LENGTH_SHORT);
            try {
                float saturation = Float.parseFloat((String) newValue);
                // If the number is outside of the acceptable range, show an error.
                if (saturation > Float.valueOf(getString(R.string.pref_OCR_image_saturation_max_display_value))
                        || saturation < Float.valueOf(getString(R.string.pref_OCR_image_saturation_min_display_value))) {
                    error.show();
                }
            } catch (NumberFormatException nfe) {
                // If whatever the user entered can't be parsed to a number, show an error
                error.show();
            }
        }
        else if (preference.getKey().equals(getString(R.string.pref_OCR_image_brightness_key))) {
            error = Toast.makeText(getContext(), "Values outside of the range [ "
                    + getString(R.string.pref_OCR_image_brightness_min_display_value)
                    + " : "
                    + getString(R.string.pref_OCR_image_brightness_max_display_value)
                    + " ] are ignored.", Toast.LENGTH_SHORT);
            try {
                float brightness = Float.parseFloat((String) newValue);
                // If the number is outside of the acceptable range, show an error.
                if (brightness > Float.valueOf(getString(R.string.pref_OCR_image_brightness_max_display_value))
                        || brightness < Float.valueOf(getString(R.string.pref_OCR_image_brightness_min_display_value))) {
                    error.show();
                }
            } catch (NumberFormatException nfe) {
                // If whatever the user entered can't be parsed to a number, show an error
                error.show();
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
            error = Toast.makeText(getContext(), "Please select a number between "
                    + getString(R.string.pref_OCR_image_contrast_min_display_value)
                    + " and "
                    + getString(R.string.pref_OCR_image_contrast_max_display_value)
                    + ".", Toast.LENGTH_SHORT);
            try {
                float contrast = Float.parseFloat((String) newValue);
                // If the number is outside of the acceptable range, show an error.
                if (contrast > Integer.valueOf(getString(R.string.pref_OCR_image_contrast_max_display_value))
                        || contrast < Integer.valueOf(getString(R.string.pref_OCR_image_contrast_min_display_value))) {
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
            error = Toast.makeText(getContext(), "Please select a number between "
                    + getString(R.string.pref_OCR_image_saturation_min_display_value)
                    + " and "
                    + getString(R.string.pref_OCR_image_saturation_max_display_value)
                    + ".", Toast.LENGTH_SHORT);
            try {
                float saturation = Float.parseFloat((String) newValue);
                // If the number is outside of the acceptable range, show an error.
                if (saturation > Integer.valueOf(getString(R.string.pref_OCR_image_saturation_max_display_value))
                        || saturation < Integer.valueOf(getString(R.string.pref_OCR_image_saturation_min_display_value))) {
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
            error = Toast.makeText(getContext(), "Please select a number between "
                    + getString(R.string.pref_OCR_image_brightness_min_display_value)
                    + " and "
                    + getString(R.string.pref_OCR_image_brightness_max_display_value)
                    + ".", Toast.LENGTH_SHORT);
            try {
                float brightness = Float.parseFloat((String) newValue);
                // If the number is outside of the acceptable range, show an error.
                if (brightness > Integer.valueOf(getString(R.string.pref_OCR_image_brightness_max_display_value))
                        || brightness < Integer.valueOf(getString(R.string.pref_OCR_image_brightness_min_display_value))) {
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