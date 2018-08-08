package com.japanesetoolboxapp.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.japanesetoolboxapp.resources.Utilities;

public class FirebaseAppClass extends android.app.Application {
    //Inpired by: https://stackoverflow.com/questions/37753991/com-google-firebase-database-databaseexception-calls-to-setpersistenceenabled
    @Override public void onCreate() {
        super.onCreate();
        /* Enable disk persistence  */
        FirebaseDatabase database = Utilities.getDatabase();
        //FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseAuth.signInWithEmailAndPassword(DatabaseUtilities.firebaseEmail, DatabaseUtilities.firebasePass);

        /*Note: two ways are demonstrated here to set the persistence of Firebase without problems: as a singleton (Utilities) or using an activity that loads before all others*/
    }
}
