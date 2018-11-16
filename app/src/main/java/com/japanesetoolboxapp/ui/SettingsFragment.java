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
        SharedPreferences.OnSharedPreferenceChangeListener {

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

        // Go through all of the preferences, and set up their characteristics.
        for (int i = 0; i < prefScreen.getPreferenceCount(); i++) {
            Preference currentPreference = prefScreen.getPreference(i);
            if (currentPreference instanceof ListPreference || currentPreference instanceof EditTextPreference) {
                setSummaryForPreference(currentPreference, sharedPreferences);
                setTitleForPreference(currentPreference);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference currentPreference = findPreference(key);
        if (currentPreference != null) {
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
            try {
                float contrast = Float.parseFloat(newValue);
                error = Toast.makeText(getContext(), getString(R.string.pref_cannot_set_value_outside_ange) + " [ "
                        + getString(R.string.pref_OCR_image_contrast_min_display_value) + " : "
                        + getString(R.string.pref_OCR_image_contrast_max_display_value) + " ].", Toast.LENGTH_SHORT);
                if (contrast > Float.valueOf(getString(R.string.pref_OCR_image_contrast_max_display_value))) {
                    sharedPreferences.edit().putString(preference.getKey(), getString(R.string.pref_OCR_image_contrast_max_display_value)).apply();
                    error.show();
                }
                else if (contrast < Float.valueOf(getString(R.string.pref_OCR_image_contrast_min_display_value))) {
                    sharedPreferences.edit().putString(preference.getKey(), getString(R.string.pref_OCR_image_contrast_min_display_value)).apply();
                    error.show();
                }
            } catch (NumberFormatException nfe) {
                sharedPreferences.edit().putString(preference.getKey(), getString(R.string.pref_OCR_image_contrast_default_value)).apply();
                error = Toast.makeText(getContext(), R.string.pref_invalid_input_default_set, Toast.LENGTH_SHORT);
                error.show();
            }
        }
        else if (preference.getKey().equals(getString(R.string.pref_OCR_image_saturation_key))) {
            try {
                float contrast = Float.parseFloat(newValue);
                error = Toast.makeText(getContext(), getString(R.string.pref_cannot_set_value_outside_ange) + " [ "
                        + getString(R.string.pref_OCR_image_saturation_min_display_value) + " : "
                        + getString(R.string.pref_OCR_image_saturation_max_display_value) + " ].", Toast.LENGTH_SHORT);
                if (contrast > Float.valueOf(getString(R.string.pref_OCR_image_saturation_max_display_value))) {
                    sharedPreferences.edit().putString(preference.getKey(), getString(R.string.pref_OCR_image_saturation_max_display_value)).apply();
                    error.show();
                }
                else if (contrast < Float.valueOf(getString(R.string.pref_OCR_image_saturation_min_display_value))) {
                    sharedPreferences.edit().putString(preference.getKey(), getString(R.string.pref_OCR_image_saturation_min_display_value)).apply();
                    error.show();
                }
            } catch (NumberFormatException nfe) {
                sharedPreferences.edit().putString(preference.getKey(), getString(R.string.pref_OCR_image_saturation_default_value)).apply();
                error = Toast.makeText(getContext(), R.string.pref_invalid_input_default_set, Toast.LENGTH_SHORT);
                error.show();
            }
        }
        else if (preference.getKey().equals(getString(R.string.pref_OCR_image_brightness_key))) {

            try {
                float contrast = Float.parseFloat(newValue);
                error = Toast.makeText(getContext(), getString(R.string.pref_cannot_set_value_outside_ange) + " [ "
                        + getString(R.string.pref_OCR_image_brightness_min_display_value) + " : "
                        + getString(R.string.pref_OCR_image_brightness_max_display_value) + " ].", Toast.LENGTH_SHORT);
                if (contrast > Float.valueOf(getString(R.string.pref_OCR_image_brightness_max_display_value))) {
                    sharedPreferences.edit().putString(preference.getKey(), getString(R.string.pref_OCR_image_brightness_max_display_value)).apply();
                    error.show();
                }
                else if (contrast < Float.valueOf(getString(R.string.pref_OCR_image_brightness_min_display_value))) {
                    sharedPreferences.edit().putString(preference.getKey(), getString(R.string.pref_OCR_image_brightness_min_display_value)).apply();
                    error.show();
                }
            } catch (NumberFormatException nfe) {
                sharedPreferences.edit().putString(preference.getKey(), getString(R.string.pref_OCR_image_brightness_default_value)).apply();
                error = Toast.makeText(getContext(), R.string.pref_invalid_input_default_set, Toast.LENGTH_SHORT);
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
    private void setTitleForPreference(Preference currentPreference) {

        if (currentPreference instanceof EditTextPreference) {
            if (currentPreference.getKey().equals(getString(R.string.pref_OCR_image_contrast_key))) {
                currentPreference.setTitle(getString(R.string.pref_OCR_image_contrast_title) + " ["
                        + getString(R.string.pref_OCR_image_contrast_min_display_value) + ":"
                        + getString(R.string.pref_OCR_image_contrast_max_display_value) + "]");
            }
            else if (currentPreference.getKey().equals(getString(R.string.pref_OCR_image_saturation_key))) {
                currentPreference.setTitle(getString(R.string.pref_OCR_image_saturation_title) + " ["
                        + getString(R.string.pref_OCR_image_saturation_min_display_value) + ":"
                        + getString(R.string.pref_OCR_image_saturation_max_display_value) + "]");

            }
            else if (currentPreference.getKey().equals(getString(R.string.pref_OCR_image_brightness_key))) {
                currentPreference.setTitle(getString(R.string.pref_OCR_image_brightness_title) + " ["
                        + getString(R.string.pref_OCR_image_brightness_min_display_value) + ":"
                        + getString(R.string.pref_OCR_image_brightness_max_display_value) + "]");

            }
        }
    }


}