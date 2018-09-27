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
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

@Database(entities = {Word.class, Verb.class, KanjiIndex.class, LatinIndex.class}, version = 23, exportSchema = false)
public abstract class JapaneseToolboxRoomDatabase extends RoomDatabase {
    //Adapted from: https://github.com/googlesamples/android-architecture-components/blob/master/PersistenceContentProviderSample/app/src/main/java/com/example/android/contentprovidersample/data/SampleDatabase.java

    //return The DAO for the tables
    @SuppressWarnings("WeakerAccess")
    public abstract WordDao word();
    public abstract VerbDao verb();
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

                if (Looper.myLooper() == null) Looper.prepare();
                Toast loadingToast = showDatabaseLoadingToast("Please wait while we update the local database... Completed 0/3.", context, null);

                loadCentralDatabaseIntoRoomDb(context);
                loadingToast = showDatabaseLoadingToast("Please wait while we update the local database... Completed 1/3.", context, loadingToast);

                loadCentralDatabaseIndexesIntoRoomDb(context);
                loadingToast = showDatabaseLoadingToast("Please wait while we update the local database... Completed 2/3.", context, loadingToast);

                setTransactionSuccessful();
            } finally {
                endTransaction();
            }
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

        // Import the excel sheets (csv format)
        List<String[]> centralDatabase 		    = new ArrayList<>();
        List<String[]> typesDatabase            = DatabaseUtilities.readCSVFile("LineTypes - 3000 kanji.csv", context);
        List<String[]> grammarDatabase          = DatabaseUtilities.readCSVFile("LineGrammar - 3000 kanji.csv", context);
        List<String[]> verbsDatabase     	    = DatabaseUtilities.readCSVFile("LineVerbsForGrammar - 3000 kanji.csv", context);
        List<String[]> meaningsDatabase         = DatabaseUtilities.readCSVFile("LineMeanings - 3000 kanji.csv", context);
        List<String[]> multExplanationsDatabase = DatabaseUtilities.readCSVFile("LineMultExplanations - 3000 kanji.csv", context);
        List<String[]> examplesDatabase         = DatabaseUtilities.readCSVFile("LineExamples - 3000 kanji.csv", context);

        //Removing the titles row in each sheet
        typesDatabase.remove(0);
        grammarDatabase.remove(0);
        verbsDatabase.remove(0);

        //Adding the sheets to the central database
        centralDatabase.addAll(typesDatabase);
        centralDatabase.addAll(grammarDatabase);
        centralDatabase.addAll(verbsDatabase);

        //Checking that there were no accidental line breaks when building the database
        DatabaseUtilities.checkDatabaseStructure(verbsDatabase, "Verbs Database", DatabaseUtilities.NUM_COLUMNS_IN_VERBS_CSV_SHEET);
        DatabaseUtilities.checkDatabaseStructure(centralDatabase, "Central Database", DatabaseUtilities.NUM_COLUMNS_IN_WORDS_CSV_SHEETS);
        DatabaseUtilities.checkDatabaseStructure(meaningsDatabase, "Meanings Database", DatabaseUtilities.NUM_COLUMNS_IN_WORDS_CSV_SHEETS);
        DatabaseUtilities.checkDatabaseStructure(multExplanationsDatabase, "Explanations Database", DatabaseUtilities.NUM_COLUMNS_IN_WORDS_CSV_SHEETS);
        DatabaseUtilities.checkDatabaseStructure(examplesDatabase, "Examples Database", DatabaseUtilities.NUM_COLUMNS_IN_WORDS_CSV_SHEETS);

        List<Word> wordList = new ArrayList<>();
        for (int i=1; i<centralDatabase.size(); i++) {
            Word word = DatabaseUtilities.createWordFromCsvDatabases(centralDatabase, meaningsDatabase, multExplanationsDatabase, examplesDatabase, i);
            wordList.add(word);
        }
        word().insertAll(wordList);
        Log.i("Diagnosis Time","Loaded Words Database.");

        List<Verb> verbList = new ArrayList<>();
        for (int i=1; i<verbsDatabase.size(); i++) {
            if (verbsDatabase.get(i)[0].equals("")) break;
            Verb verb = DatabaseUtilities.createVerbFromCsvDatabase(verbsDatabase, meaningsDatabase, i);
            verbList.add(verb);
        }
        verb().insertAll(verbList);
        Log.i("Diagnosis Time","Loaded Verbs Database.");
    }
    private void loadCentralDatabaseIndexesIntoRoomDb(Context context) {

        List<String[]> grammarDatabaseIndexLatin = DatabaseUtilities.readCSVFile("LineGrammarSortedIndexLatin - 3000 kanji.csv", context);
        List<String[]> grammarDatabaseIndexedKanji = DatabaseUtilities.readCSVFile("LineGrammarSortedIndexKanji - 3000 kanji.csv", context);

        List<LatinIndex> latinIndexList = new ArrayList<>();
        for (int i=0; i<grammarDatabaseIndexLatin.size(); i++) {
            if (TextUtils.isEmpty(grammarDatabaseIndexLatin.get(i)[0])) break;
            LatinIndex latinIndex = new LatinIndex(grammarDatabaseIndexLatin.get(i)[0], grammarDatabaseIndexLatin.get(i)[1]);
            latinIndexList.add(latinIndex);
        }
        latinIndex().insertAll(latinIndexList);

        List<KanjiIndex> kanjiIndexList = new ArrayList<>();
        for (int i=0; i<grammarDatabaseIndexedKanji.size(); i++) {
            if (TextUtils.isEmpty(grammarDatabaseIndexedKanji.get(i)[0])) break;
            KanjiIndex kanjiIndex = new KanjiIndex(grammarDatabaseIndexedKanji.get(i)[0], grammarDatabaseIndexedKanji.get(i)[1], grammarDatabaseIndexedKanji.get(i)[2]);
            kanjiIndexList.add(kanjiIndex);
        }
        kanjiIndex().insertAll(kanjiIndexList);
        Log.i("Diagnosis Time","Loaded Indexes.");
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

    public List<LatinIndex> getLatinIndexesListForStartingWord(String query) {
        return latinIndex().getLatinIndexByStartingLatinQuery(query);
    }
    public List<KanjiIndex> getKanjiIndexesListForStartingWord(String query) {
        return kanjiIndex().getKanjiIndexByStartingLatinQuery(query);
    }

    public List<Verb> getVerbListByVerbIds(List<Long> verbIds) {
        return verb().getVerbListByVerbIds(verbIds);
    }
    public Verb getVerbByVerbId(long verbId) {
        return verb().getVerbByVerbId(verbId);
    }
    public List<Verb> getVerbsByRomajiQuery(String query) {
        return verb().getVerbByExactRomajiQueryMatch(query);
    }
    public List<Verb> getVerbsByKanjiQuery(String query) {
        return verb().getVerbByExactKanjiQueryMatch(query);
    }
    public List<Verb> getAllVerbs() {
        return verb().getAllVerbs();
    }
}
