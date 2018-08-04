package com.japanesetoolboxapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.japanesetoolboxapp.R;
import com.japanesetoolboxapp.data.DatabaseUtilities;

public class SplashScreenActivity extends Activity {

    private final int SPLASH_DISPLAY_LENGTH = 2000; //Miliseconds

    @Override protected void onCreate(Bundle savedInstanceState) {

        Log.i("Diagnosis Time", "Started Splashscreen.");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splashscreen);

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
				/* Create an Intent that will start the main Activity. */
                Intent mainIntent = new Intent(SplashScreenActivity.this, MainActivity.class);

                SplashScreenActivity.this.overridePendingTransition(0, 0);
                SplashScreenActivity.this.startActivity(mainIntent);
                SplashScreenActivity.this.finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
        
        //Setting up Firebase
        FirebaseDatabase firebaseDb = FirebaseDatabase.getInstance();
        firebaseDb.setPersistenceEnabled(true);
        FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseAuth.signInWithEmailAndPassword(DatabaseUtilities.firebaseEmail, DatabaseUtilities.firebasePass);
    }
}
