package com.japanesetoolboxapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.japanesetoolboxapp.R;
import com.japanesetoolboxapp.data.JapaneseToolboxRoomDatabase;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class SplashScreenActivity extends Activity {

    private final int SPLASH_DISPLAY_LENGTH = 1000; //Miliseconds
    private Unbinder mBinding;
    @BindView(R.id.splashscreen_loading_indicator) ProgressBar mProgressBarLoadingIndicator;

    @Override protected void onCreate(Bundle savedInstanceState) {

        Log.i("Diagnosis Time", "Started Splashscreen.");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splashscreen);

        mBinding =  ButterKnife.bind(this);



        Runnable dbLoadRunnable = new Runnable() {
            @Override
            public void run() {

                showLoadingIndicator();
                JapaneseToolboxRoomDatabase japaneseToolboxRoomDatabase = JapaneseToolboxRoomDatabase.getInstance(SplashScreenActivity.this); //Required for Room
                hideLoadingIndicator();

                Intent mainIntent = new Intent(SplashScreenActivity.this, MainActivity.class);
                SplashScreenActivity.this.overridePendingTransition(0, 0);
                SplashScreenActivity.this.startActivity(mainIntent);
                SplashScreenActivity.this.finish();
            }
        };
        Thread dbLoadThread = new Thread(dbLoadRunnable);
        dbLoadThread.start();


//        new Handler().postDelayed(new Runnable(){
//            @Override
//            public void run() {
//
//                Intent mainIntent = new Intent(SplashScreenActivity.this, MainActivity.class);
//                SplashScreenActivity.this.overridePendingTransition(0, 0);
//                SplashScreenActivity.this.startActivity(mainIntent);
//                SplashScreenActivity.this.finish();
//            }
//        }, SPLASH_DISPLAY_LENGTH);

    }
    @Override protected void onDestroy() {
        super.onDestroy();
        mBinding.unbind();
    }

    private void showLoadingIndicator() {
        if (mProgressBarLoadingIndicator!=null) mProgressBarLoadingIndicator.setVisibility(View.VISIBLE);
    }
    private void hideLoadingIndicator() {
        if (mProgressBarLoadingIndicator!=null) mProgressBarLoadingIndicator.setVisibility(View.INVISIBLE);
    }
}
