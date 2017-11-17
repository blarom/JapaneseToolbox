package com.japanesetoolboxapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class SplashScreen extends Activity {

    private final int SPLASH_DISPLAY_LENGTH = 1500; //Miliseconds

    @Override protected void onCreate(Bundle savedInstanceState)
    {

        Log.i("Diagnosis Time", "Started Splashscreen.");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.splashscreen);

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
				/* Create an Intent that will start the main Activity. */
                Intent mainIntent = new Intent(SplashScreen.this, MainActivity.class);

                SplashScreen.this.overridePendingTransition(0, 0);
                SplashScreen.this.startActivity(mainIntent);
                SplashScreen.this.finish();
            }
        }, SPLASH_DISPLAY_LENGTH);

    }
}
