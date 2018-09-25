package com.japanesetoolboxapp.resources;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.japanesetoolboxapp.data.DatabaseUtilities;
import com.livefront.bridge.Bridge;
import com.livefront.bridge.SavedStateHandler;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import icepick.Icepick;

public class MainApplication extends Application {

    private static final boolean ALLOW_USE_OF_LEAK_CANARY = false;

    @Override public void onCreate() {
        super.onCreate();

        activateLeakCanary();
        activateBridgeAndIcePick();
        activateFirebase();

    }


    private RefWatcher refWatcher;
    public static RefWatcher getRefWatcher(Context context) {
        MainApplication application = (MainApplication) context.getApplicationContext();
        return application.refWatcher;
    }
    private void activateLeakCanary() {

        if (!ALLOW_USE_OF_LEAK_CANARY) return;

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        refWatcher = LeakCanary.install(this);
        // Normal app init code...
    }
    private void activateBridgeAndIcePick() {

        Bridge.initialize(getApplicationContext(), new SavedStateHandler() {
            @Override
            public void saveInstanceState(@NonNull Object target, @NonNull Bundle state) {
                Icepick.saveInstanceState(target, state);
            }

            @Override
            public void restoreInstanceState(@NonNull Object target, @Nullable Bundle state) {
                Icepick.restoreInstanceState(target, state);
            }
        });
    }
    private void activateFirebase() {

        /* Enable disk persistence  */
        FirebaseDatabase database = Utilities.getDatabase();
        //FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseAuth.signInWithEmailAndPassword(DatabaseUtilities.firebaseEmail, DatabaseUtilities.firebasePass);

        /*Note: two ways are demonstrated here to set the persistence of Firebase without problems: as a singleton (Utilities) or using an activity that loads before all others*/
    }
}