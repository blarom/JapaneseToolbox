package com.japanesetoolboxapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;

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

        // Go through all of the preferences, and set up their preference summary.
        for (int i = 0; i < prefScreen.getPreferenceCount(); i++) {
            Preference currentPreference = prefScreen.getPreference(i);
            if (currentPreference instanceof ListPreference) {
                setSummaryForListPreferences(currentPreference, sharedPreferences);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference currentPreference = findPreference(key);
        if (currentPreference != null) {
            // Updates the summary for the preference
            if (currentPreference instanceof ListPreference) {
                setSummaryForListPreferences(currentPreference, sharedPreferences);
            }
        }
    }

    private void setSummaryForListPreferences(Preference currentPreference, SharedPreferences sharedPreferences) {

        String currentPreferenceValue = sharedPreferences.getString(currentPreference.getKey(), "");
        if (currentPreference instanceof ListPreference) {
            ListPreference currentListPreference = (ListPreference) currentPreference;
            int prefIndex = currentListPreference.findIndexOfValue(currentPreferenceValue);
            if (prefIndex >= 0)  currentListPreference.setSummary(currentListPreference.getEntries()[prefIndex]);
        }
    }
}