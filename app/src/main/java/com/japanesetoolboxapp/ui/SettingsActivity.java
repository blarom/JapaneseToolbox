package com.japanesetoolboxapp.ui;

import android.os.Bundle;

import com.japanesetoolboxapp.R;
import com.japanesetoolboxapp.resources.Utilities;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Utilities.changeThemeColor(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }
}
