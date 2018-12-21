package com.japanesetoolboxapp.data;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.os.Looper;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import com.japanesetoolboxapp.resources.Utilities;

import java.util.ArrayList;
import java.util.List;

@Database(entities = {Word.class, Verb.class, KanjiIndex.class, LatinIndex.class}, version = 39, exportSchema = false)
public abstract class JapaneseToolboxCentralRoomDatabase extends RoomDatabase {
    //Adapted from: https://github.com/googlesamples/android-architecture-components/blob/master/PersistenceContentProviderSample/app/src/main/java/com/example/android/contentprovidersample/data/SampleDatabase.java

    //return The DAO for the tables
    @SuppressWarnings("WeakerAccess")
    public abstract WordDao word();
    public abstract VerbDao verb();
    public abstract KanjiIndexDao kanjiIndex();
    public abstract LatinIndexDao latinIndex();


    //Gets the singleton instance of SampleDatabase
    private static JapaneseToolboxCentralRoomDatabase sInstance;
    public static synchronized JapaneseToolboxCentralRoomDatabase getInstance(Context context) {
        if (sInstance == null) {
            try {
                //Use this clause if you want to upgrade the database without destroying the previous database. Here, FROM_1_TO_2 is never satisfied since database version > 2.
                sInstance = Room
                        .databaseBuilder(context.getApplicationContext(), JapaneseToolboxCentralRoomDatabase.class, "japanese_toolbox_central_room_database")
                        .addMigrations(FROM_1_TO_2)
                        .build();

                sInstance.populateDatabases(context);
            } catch (Exception e) {
                //If migrations weren't set up from version X to verion X+1, do a destructive migration (rebuilds the db from sratch using the assets)
                e.printStackTrace();
                sInstance = Room
                        .databaseBuilder(context.getApplicationContext(), JapaneseToolboxCentralRoomDatabase.class, "japanese_toolbox_central_room_database")
                        .fallbackToDestructiveMigration()
                        .build();

                sInstance.populateDatabases(context);
            }
        }
        return sInstance;
    }

    private void populateDatabases(Context context) {

        if (word().count() == 0) {
            Utilities.setAppPreferenceWordVerbDatabasesFinishedLoadingFlag(context, false);
            beginTransaction();
            try {
                if (Looper.myLooper() == null) Looper.prepare();
                loadCentralDatabaseIntoRoomDb(context);
                Log.i("Diagnosis Time", "Loaded Room Central Database.");
                setTransactionSuccessful();
            } finally {
                endTransaction();
            }
        }
        if (latinIndex().count() == 0) {
            beginTransaction();
            try {
                if (Looper.myLooper() == null) Looper.prepare();
                loadCentralDatabaseIndexesIntoRoomDb(context);
                Log.i("Diagnosis Time", "Loaded Room Central Indexes Database.");
                setTransactionSuccessful();
            } finally {
                endTransaction();
            }
        }
        Utilities.setAppPreferenceWordVerbDatabasesFinishedLoadingFlag(context, true);

    }
    private void loadCentralDatabaseIntoRoomDb(Context context) {

        // Import the excel sheets (csv format)
        List<String[]> centralDatabase 		    = new ArrayList<>();
        List<String[]> typesDatabase            = Utilities.readCSVFile("LineTypes - 3000 kanji.csv", context);
        List<String[]> grammarDatabase          = Utilities.readCSVFile("LineGrammar - 3000 kanji.csv", context);
        List<String[]> verbsDatabase     	    = Utilities.readCSVFile("LineVerbsForGrammar - 3000 kanji.csv", context);
        List<String[]> meaningsDatabase         = Utilities.readCSVFile("LineMeanings - 3000 kanji.csv", context);
        List<String[]> multExplanationsDatabase = Utilities.readCSVFile("LineMultExplanations - 3000 kanji.csv", context);
        List<String[]> examplesDatabase         = Utilities.readCSVFile("LineExamples - 3000 kanji.csv", context);

        //Removing the titles row in each sheet
        typesDatabase.remove(0);
        grammarDatabase.remove(0);
        verbsDatabase.remove(0);

        //Adding the sheets to the central database
        centralDatabase.addAll(typesDatabase);
        centralDatabase.addAll(grammarDatabase);
        centralDatabase.addAll(verbsDatabase);

        //Checking that there were no accidental line breaks when building the database
        Utilities.checkDatabaseStructure(verbsDatabase, "Verbs Database", Utilities.NUM_COLUMNS_IN_VERBS_CSV_SHEET);
        Utilities.checkDatabaseStructure(centralDatabase, "Central Database", Utilities.NUM_COLUMNS_IN_WORDS_CSV_SHEETS);
        Utilities.checkDatabaseStructure(meaningsDatabase, "Meanings Database", Utilities.NUM_COLUMNS_IN_WORDS_CSV_SHEETS);
        Utilities.checkDatabaseStructure(multExplanationsDatabase, "Explanations Database", Utilities.NUM_COLUMNS_IN_WORDS_CSV_SHEETS);
        Utilities.checkDatabaseStructure(examplesDatabase, "Examples Database", Utilities.NUM_COLUMNS_IN_WORDS_CSV_SHEETS);

        List<Word> wordList = new ArrayList<>();
        for (int i=1; i<centralDatabase.size(); i++) {
            if (centralDatabase.get(i)[0].equals("")) break;
            Word word = Utilities.createWordFromCsvDatabases(centralDatabase, meaningsDatabase, multExplanationsDatabase, examplesDatabase, i);
            wordList.add(word);
        }
        word().insertAll(wordList);
        Log.i("Diagnosis Time","Loaded Words Database.");

        List<Verb> verbList = new ArrayList<>();
        for (int i=1; i<verbsDatabase.size(); i++) {
            if (verbsDatabase.get(i)[0].equals("")) break;
            Verb verb = Utilities.createVerbFromCsvDatabase(verbsDatabase, meaningsDatabase, i);
            verbList.add(verb);
        }
        verb().insertAll(verbList);
        Log.i("Diagnosis Time","Loaded Verbs Database.");
    }
    private void loadCentralDatabaseIndexesIntoRoomDb(Context context) {

        List<String[]> grammarDatabaseIndexLatin = Utilities.readCSVFile("LineGrammarSortedIndexLatin - 3000 kanji.csv", context);
        List<String[]> grammarDatabaseIndexedKanji = Utilities.readCSVFile("LineGrammarSortedIndexKanji - 3000 kanji.csv", context);

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
                .inMemoryDatabaseBuilder(context.getApplicationContext(),JapaneseToolboxCentralRoomDatabase.class)
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
    public List<Word> getWordsByExactRomajiAndKanjiMatch(String romaji, String kanji) {
        return word().getWordsByExactRomajiAndKanjiMatch(romaji, kanji);
    }
    public List<Word> getWordsContainingRomajiMatch(String romaji) {
        return word().getWordsContainingRomajiMatch(romaji);
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
    public LatinIndex getLatinIndexListForExactWord(String query) {
        return latinIndex().getLatinIndexByExactLatinQuery(query);
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
    public void updateVerbByVerbIdWithParams(long verbId, String activeLatinRoot, String activeKanjiRoot, String activeAltSpelling) {
        verb().updateVerbByVerbIdWithParameters(verbId, activeLatinRoot, activeKanjiRoot, activeAltSpelling);
    }
    public void updateVerb(Verb verb) {
        verb().update(verb);
    }
    public List<Verb> getVerbsByExactRomajiMatch(String query) {
        return verb().getVerbByExactRomajiMatch(query);
    }
    public List<Verb> getVerbsByKanjiQuery(String query) {
        return verb().getVerbByExactKanjiQueryMatch(query);
    }
    public List<Verb> getAllVerbs() {
        return verb().getAllVerbs();
    }

}
