package com.japanesetoolboxapp.data;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.support.annotation.VisibleForTesting;


import java.util.List;

@Database(entities = {Word.class}, version = 2)
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

                //sInstance.populateWordData(context);
                //sInstance.populateMoodRatingData();
            } catch (Exception e) {
                sInstance = Room
                        .databaseBuilder(context.getApplicationContext(), WordsRoomDatabase.class, "words_room_database")
                        .fallbackToDestructiveMigration()
                        .build();

                //sInstance.populateWordData(context);
                //sInstance.populateMoodRatingData();
            }
        }
        return sInstance;
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
    public Word getWord(Word word) {
        return word().getWordById(word.getWordId());
    }
    public void insertWordList(List<Word> wordList) {
        word().insertAll(wordList);
    }

}
