package com.japanesetoolboxapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.japanesetoolboxapp.R;
import com.japanesetoolboxapp.data.JapaneseToolboxCentralRoomDatabase;
import com.japanesetoolboxapp.data.JapaneseToolboxKanjiRoomDatabase;
import com.japanesetoolboxapp.resources.Utilities;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class SplashScreenActivity extends BaseActivity {

    private final int SPLASH_DISPLAY_LENGTH = 1000; //Miliseconds
    private Unbinder mBinding;
    @BindView(R.id.splashscreen_loading_indicator) ProgressBar mProgressBarLoadingIndicator;
    @BindView(R.id.splashscreen_time_to_load_textview) TextView mTimeToLoadTextView;
    @BindView(R.id.splashscreen_current_loading_database) TextView mLoadingDatabaseTextView;
    private boolean mLoadedCentralDb;
    private boolean mLoadedKanjiDb;
    private boolean mKanjiDbTextAlreadyLoaded;
    private CountDownTimer countDownTimer;
    private boolean mFirstTick;

    @Override protected void onCreate(Bundle savedInstanceState) {

        Log.i("Diagnosis Time", "Started Splashscreen.");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splashscreen);

        mBinding =  ButterKnife.bind(this);

        mKanjiDbTextAlreadyLoaded = false;
        mLoadingDatabaseTextView.setText(getString(R.string.loading_central_database));
        Runnable dbLoadRunnable = new Runnable() {
            @Override
            public void run() {
                mLoadedCentralDb = false;
                JapaneseToolboxCentralRoomDatabase japaneseToolboxCentralRoomDatabase = JapaneseToolboxCentralRoomDatabase.getInstance(SplashScreenActivity.this); //Required for Room
                mLoadedCentralDb = true;
            }
        };
        Thread dbLoadThread = new Thread(dbLoadRunnable);
        dbLoadThread.start();

        dbLoadRunnable = new Runnable() {
            @Override
                public void run() {
                mLoadedKanjiDb = false;
                JapaneseToolboxKanjiRoomDatabase japaneseToolboxKanjiRoomDatabase = JapaneseToolboxKanjiRoomDatabase.getInstance(SplashScreenActivity.this); //Required for Room
                mLoadedKanjiDb = true;
            }
        };
        dbLoadThread = new Thread(dbLoadRunnable);
        dbLoadThread.start();

        mFirstTick = true;

        countDownTimer = new CountDownTimer(360000, 500) {
            @Override
            public void onTick(long l) {

                if (mFirstTick) {
                    mFirstTick = false;
                    if (mTimeToLoadTextView!=null) mTimeToLoadTextView.setText(R.string.splashscreen_should_take_only_a_few_seconds);
                    hideLoadingIndicator();
                    if (mLoadingDatabaseTextView!=null) mLoadingDatabaseTextView.setVisibility(View.GONE);
                    return;
                }

                boolean finishedLoadingWordDatabases = Utilities.getAppPreferenceWordVerbDatabasesFinishedLoadingFlag(SplashScreenActivity.this);
                boolean finishedLoadingKanjiDatabases = Utilities.getAppPreferenceKanjiDatabaseFinishedLoadingFlag(SplashScreenActivity.this);
                if (!finishedLoadingWordDatabases || !finishedLoadingKanjiDatabases) {
                    if (mTimeToLoadTextView!=null) mTimeToLoadTextView.setText(R.string.database_being_installed);
                    if (mLoadingDatabaseTextView!=null) mLoadingDatabaseTextView.setVisibility(View.VISIBLE);
                    showLoadingIndicator();
                }
                else {
                    if (mTimeToLoadTextView!=null) mTimeToLoadTextView.setText(R.string.splashscreen_should_take_only_a_few_seconds);
                    hideLoadingIndicator();
                    if (mLoadingDatabaseTextView!=null) mLoadingDatabaseTextView.setVisibility(View.GONE);
                }

                if (mLoadedCentralDb && !mLoadedKanjiDb && !mKanjiDbTextAlreadyLoaded) {
                    mKanjiDbTextAlreadyLoaded = true;
                    if (mLoadingDatabaseTextView!=null) mLoadingDatabaseTextView.setText(getString(R.string.loading_kanji_database));
                }
                else if (mLoadedCentralDb && mLoadedKanjiDb) {
                    countDownTimer.onFinish();
                    countDownTimer.cancel();
                }
            }

            @Override
            public void onFinish() {
                hideLoadingIndicator();
                if (Looper.myLooper()==null) Looper.prepare();
                Toast.makeText(SplashScreenActivity.this, R.string.finished_loading_databases, Toast.LENGTH_SHORT).show();
                startMainActivity();
            }
        };
        countDownTimer.start();

    }
    @Override protected void onDestroy() {
        super.onDestroy();
        mBinding.unbind();
        countDownTimer.cancel();
    }

    private void startMainActivity() {
        Intent mainIntent = new Intent(SplashScreenActivity.this, MainActivity.class);
        SplashScreenActivity.this.overridePendingTransition(0, 0);
        SplashScreenActivity.this.startActivity(mainIntent);
        SplashScreenActivity.this.finish();
    }
    private void showLoadingIndicator() {
        if (mProgressBarLoadingIndicator!=null) mProgressBarLoadingIndicator.setVisibility(View.VISIBLE);
    }
    private void hideLoadingIndicator() {
        if (mProgressBarLoadingIndicator!=null) mProgressBarLoadingIndicator.setVisibility(View.INVISIBLE);
    }
}
