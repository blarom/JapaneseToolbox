package com.japanesetoolboxapp.data;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Typeface;
import android.os.Looper;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.List;

@Database(entities = {Word.class, KanjiIndex.class, LatinIndex.class}, version = 10)
public abstract class JapaneseToolboxRoomDatabase extends RoomDatabase {
    //Adapted from: https://github.com/googlesamples/android-architecture-components/blob/master/PersistenceContentProviderSample/app/src/main/java/com/example/android/contentprovidersample/data/SampleDatabase.java

    //return The DAO for the tables
    @SuppressWarnings("WeakerAccess")
    public abstract WordDao word();
    public abstract KanjiIndexDao kanjiIndex();
    public abstract LatinIndexDao latinIndex();


    //Gets the singleton instance of SampleDatabase
    private static JapaneseToolboxRoomDatabase sInstance;
    public static synchronized JapaneseToolboxRoomDatabase getInstance(Context context) {
        if (sInstance == null) {
            try {
                //Use this clause if you want to upgrade the database without destroying the previous database. Here, FROM_1_TO_2 is never satisfied since database version > 2.
                sInstance = Room
                        .databaseBuilder(context.getApplicationContext(), JapaneseToolboxRoomDatabase.class, "japanese_toolbox_room_database")
                        .addMigrations(FROM_1_TO_2)
                        .build();

                sInstance.populateDatabases(context);
            } catch (Exception e) {
                //If migrations weren't set up from version X to verion X+1, do a destructive migration (rebuilds the db from sratch using the assets)
                e.printStackTrace();
                sInstance = Room
                        .databaseBuilder(context.getApplicationContext(), JapaneseToolboxRoomDatabase.class, "japanese_toolbox_room_database")
                        .fallbackToDestructiveMigration()
                        .build();

                sInstance.populateDatabases(context);
            }
        }
        return sInstance;
    }

    private void populateDatabases(Context context) {
        if (word().count() == 0) {
            beginTransaction();
            try {

                Looper.prepare();
                Toast loadingToast = showDatabaseLoadingToast("Please wait while we update the local database... Completed 0/3.", context, null);

                loadCentralDatabaseIntoRoomDb(context);
                loadingToast = showDatabaseLoadingToast("Please wait while we update the local database... Completed 1/3.", context, loadingToast);

                loadCentralDatabaseIndexIntoRoomDb(context);
                loadingToast = showDatabaseLoadingToast("Please wait while we update the local database... Completed 2/3.", context, loadingToast);

                setTransactionSuccessful();
            } finally {
                endTransaction();
            }
        }
        else {
            String a="";
        }
    }
    private Toast showDatabaseLoadingToast(final String message, Context context, Toast lastToast) {
        if(lastToast != null) lastToast.cancel();

        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM|Gravity.FILL_HORIZONTAL, 0, 0);

        View toastview = toast.getView();
        //toastview.setBackgroundColor(Color.WHITE);
        toastview.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.parseColor("#803582FF"))); //Light blue
        toastview.getBackground().setAlpha(90);

        TextView toastMessage = toast.getView().findViewById(android.R.id.message);
        toastMessage.setTextColor(Color.BLACK);
        toastMessage.setTypeface(null, Typeface.BOLD);
        toastMessage.setTextSize(12);
        toast.show();

        return toast;
    }
    private void loadCentralDatabaseIntoRoomDb(Context context) {

        List<String[]> centralDatabase = DatabaseUtilities.loadCentralDatabaseFromCsv(context);
        List<String[]> meaningsDatabase = DatabaseUtilities.readCSVFile("LineMeanings - 3000 kanji.csv", context);
        List<String[]> multExplanationsDatabase = DatabaseUtilities.readCSVFile("LineMultExplanations - 3000 kanji.csv", context);
        List<String[]> examplesDatabase = DatabaseUtilities.readCSVFile("LineExamples - 3000 kanji.csv", context);

        //Checking that there were no accidental line breaks when building the database
        DatabaseUtilities.checkDatabaseStructure(centralDatabase, "Central Database");
        DatabaseUtilities.checkDatabaseStructure(meaningsDatabase, "Meanings Database");
        DatabaseUtilities.checkDatabaseStructure(multExplanationsDatabase, "Explanations Database");
        DatabaseUtilities.checkDatabaseStructure(examplesDatabase, "Examples Database");

        List<Word> wordList = new ArrayList<>();
        for (int i=1; i<centralDatabase.size(); i++) {
            com.japanesetoolboxapp.data.Word word = DatabaseUtilities.createWordFromCsvDatabases(centralDatabase, meaningsDatabase, multExplanationsDatabase, examplesDatabase, i);
            wordList.add(word);
        }
        word().insertAll(wordList);
        Log.i("Diagnosis Time","Loaded MainDatabase.");
    }
    private void loadCentralDatabaseIndexIntoRoomDb(Context context) {
    }

    //Switches the internal implementation with an empty in-memory database
    @VisibleForTesting
    public static void switchToInMemory(Context context) {
        sInstance = Room
                .inMemoryDatabaseBuilder(context.getApplicationContext(),JapaneseToolboxRoomDatabase.class)
                .build();
    }

    private static final Migration FROM_1_TO_2 = new Migration(1, 2) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Repo ADD COLUMN createdAt TEXT");
        }
    };
    public static void destroyInstance() {
        sInstance = null;
    }

    public List<Word> getAllWords() {
        return word().getAllWords();
    }
    public void updateWord(Word word) {
        word().update(word);
    }
    public Word getWordByWordId(long wordId) {
        return word().getWordByWordId(wordId);
    }
    public List<Word> getWordListByWordIds(List<Long> wordIds) {
        return word().getWordListByWordIds(wordIds);
    }
    public void insertWordList(List<Word> wordList) {
        word().insertAll(wordList);
    }
    public int getDatabaseSize() {
        return word().count();
    }
}
