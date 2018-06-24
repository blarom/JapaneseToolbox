package com.japanesetoolboxapp.data;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.support.annotation.VisibleForTesting;


import java.util.ArrayList;
import java.util.List;

@Database(entities = {Word.class}, version = 5)
public abstract class WordsRoomDatabase extends RoomDatabase {
    //Adapted from: https://github.com/googlesamples/android-architecture-components/blob/master/PersistenceContentProviderSample/app/src/main/java/com/example/android/contentprovidersample/data/SampleDatabase.java

    //return The DAO for the Word table
    @SuppressWarnings("WeakerAccess")
    public abstract WordDao word();


    //Gets the singleton instance of SampleDatabase
    private static WordsRoomDatabase sInstance;
    public static synchronized WordsRoomDatabase getInstance(Context context) {
        if (sInstance == null) {
            try {
                sInstance = Room
                        .databaseBuilder(context.getApplicationContext(), WordsRoomDatabase.class, "words_room_database")
                        .addMigrations(FROM_1_TO_2)
                        //.fallbackToDestructiveMigration()
                        .build();

                sInstance.populateWordData(context);
            } catch (Exception e) {
                e.printStackTrace();
                sInstance = Room
                        .databaseBuilder(context.getApplicationContext(), WordsRoomDatabase.class, "words_room_database")
                        .fallbackToDestructiveMigration()
                        .build();

                //sInstance.populateWordData(context);
            }
        }
        return sInstance;
    }

    private void populateWordData(Context context) {
        if (word().count() == 0) {
            List<Word> wordList = new ArrayList<>();
            beginTransaction();
            try {
                List<String[]> centralDatabase = DatabaseUtilities.loadCentralDatabaseFromCsv(context);
                List<String[]> meaningsDatabase = DatabaseUtilities.readCSVFile("LineMeanings - 3000 kanji.csv", context);
                List<String[]> multExplanationsDatabase = DatabaseUtilities.readCSVFile("LineMultExplanations - 3000 kanji.csv", context);
                List<String[]> examplesDatabase = DatabaseUtilities.readCSVFile("LineExamples - 3000 kanji.csv", context);

                //Checking that there were no accidental line breaks when building the database
                DatabaseUtilities.checkDatabaseStructure(centralDatabase, "Central Database");
                DatabaseUtilities.checkDatabaseStructure(meaningsDatabase, "Meanings Database");
                DatabaseUtilities.checkDatabaseStructure(multExplanationsDatabase, "Explanations Database");
                DatabaseUtilities.checkDatabaseStructure(examplesDatabase, "Examples Database");

                for (int i=1; i<centralDatabase.size(); i++) {
                    com.japanesetoolboxapp.data.Word word = DatabaseUtilities.createWordFromCsvDatabases(centralDatabase, meaningsDatabase, multExplanationsDatabase, examplesDatabase, i);
                    wordList.add(word);
                }
                word().insertAll(wordList);
                setTransactionSuccessful();
            } finally {
                endTransaction();
            }
        }
        else {
            String a="";
        }
    }

    //Switches the internal implementation with an empty in-memory database
    @VisibleForTesting
    public static void switchToInMemory(Context context) {
        sInstance = Room
                .inMemoryDatabaseBuilder(context.getApplicationContext(),WordsRoomDatabase.class)
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
