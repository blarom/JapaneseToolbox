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

@Database(entities = {KanjiCharacter.class, KanjiComponent.class}, version = 7, exportSchema = false)
public abstract class JapaneseToolboxKanjiRoomDatabase extends RoomDatabase {
    //Adapted from: https://github.com/googlesamples/android-architecture-components/blob/master/PersistenceContentProviderSample/app/src/main/java/com/example/android/contentprovidersample/data/SampleDatabase.java

    //return The DAO for the tables
    @SuppressWarnings("WeakerAccess")
    public abstract KanjiCharacterDao kanjiCharacter();
    public abstract KanjiComponentDao kanjiComponent();


    //Gets the singleton instance of SampleDatabase
    private static JapaneseToolboxKanjiRoomDatabase sInstance;
    public static synchronized JapaneseToolboxKanjiRoomDatabase getInstance(Context context) {
        if (sInstance == null) {
            try {
                //Use this clause if you want to upgrade the database without destroying the previous database. Here, FROM_1_TO_2 is never satisfied since database version > 2.
                sInstance = Room
                        .databaseBuilder(context.getApplicationContext(), JapaneseToolboxKanjiRoomDatabase.class, "japanese_toolbox_kanji_room_database")
                        .addMigrations(FROM_1_TO_2)
                        .build();

                sInstance.populateDatabases(context);
            } catch (Exception e) {
                //If migrations weren't set up from version X to verion X+1, do a destructive migration (rebuilds the db from sratch using the assets)
                e.printStackTrace();
                sInstance = Room
                        .databaseBuilder(context.getApplicationContext(), JapaneseToolboxKanjiRoomDatabase.class, "japanese_toolbox_kanji_room_database")
                        .fallbackToDestructiveMigration()
                        .build();

                sInstance.populateDatabases(context);
            }
        }
        return sInstance;
    }

    private void populateDatabases(Context context) {

        Utilities.setAppPreferenceKanjiDatabaseFinishedLoadingFlag(context, false);

        if (kanjiCharacter().count() == 0) {
            beginTransaction();
            try {
                if (Looper.myLooper() == null) Looper.prepare();
                loadKanjiCharactersIntoRoomDb(context);
                Log.i("Diagnosis Time", "Loaded Room Kanji Characters Database.");
                setTransactionSuccessful();
            } finally {
                endTransaction();
            }
        }
        if (kanjiComponent().count() == 0) {
            beginTransaction();
            try {
                if (Looper.myLooper() == null) Looper.prepare();
                loadKanjiComponentsIntoRoomDb(context);
                Log.i("Diagnosis Time", "Loaded Room Kanji Components Database.");
                setTransactionSuccessful();
            } finally {
                endTransaction();
            }

        }
        Utilities.setAppPreferenceKanjiDatabaseFinishedLoadingFlag(context, true);
    }
    private void loadKanjiCharactersIntoRoomDb(Context context) {


        List<String[]> CJK_Database = Utilities.readCSVFile("LineCJK_Decomposition - 3000 kanji.csv", context);
        List<String[]> KanjiDict_Database = Utilities.readCSVFile("LineKanjiDictionary - 3000 kanji.csv", context);
        List<String[]> RadicalsDatabase = Utilities.readCSVFile("LineRadicals - 3000 kanji.csv", context);

        List<KanjiCharacter> kanjiCharacterList = new ArrayList<>();
        for (int i=0; i<CJK_Database.size(); i++) {
            if (TextUtils.isEmpty(CJK_Database.get(i)[0])) break;
            KanjiCharacter kanjiCharacter = new KanjiCharacter(CJK_Database.get(i)[0], CJK_Database.get(i)[1], CJK_Database.get(i)[2]);
            kanjiCharacter.setKanji(Utilities.convertFromUTF8Index(kanjiCharacter.getHexIdentifier()));
            kanjiCharacterList.add(kanjiCharacter);
        }
        kanjiCharacter().insertAll(kanjiCharacterList);

        for (int i=0; i<KanjiDict_Database.size(); i++) {
            if (TextUtils.isEmpty(KanjiDict_Database.get(i)[0])) break;
            KanjiCharacter kanjiCharacter = kanjiCharacter().getKanjiCharacterByHexId(KanjiDict_Database.get(i)[0]);
            if (kanjiCharacter!=null) {
                kanjiCharacter.setReadings(KanjiDict_Database.get(i)[1]);
                kanjiCharacter.setMeanings(KanjiDict_Database.get(i)[2]);
                kanjiCharacter().update(kanjiCharacter);
            }
        }

        for (int i=0; i<RadicalsDatabase.size(); i++) {
            if (TextUtils.isEmpty(RadicalsDatabase.get(i)[0])) break;
            KanjiCharacter kanjiCharacter = kanjiCharacter().getKanjiCharacterByHexId(RadicalsDatabase.get(i)[0]);
            if (kanjiCharacter!=null) {
                kanjiCharacter.setRadPlusStrokes(RadicalsDatabase.get(i)[1]);
                kanjiCharacter().update(kanjiCharacter);
            }
        }

        Log.i("Diagnosis Time","Loaded Kanji Characters Database.");
    }
    private void loadKanjiComponentsIntoRoomDb(Context context) {

        List<String[]> Components_Database = Utilities.readCSVFile("LineComponents - 3000 kanji.csv", context);

        KanjiComponent kanjiComponent = new KanjiComponent("full1");
        List<KanjiComponent> kanjiComponents = new ArrayList<>();
        List<KanjiComponent.AssociatedComponent> associatedComponents = new ArrayList<>();
        String firstElement;
        String secondElement;
        for (int i=0; i<Components_Database.size();i++) {

            firstElement = Components_Database.get(i)[0];
            secondElement = Components_Database.get(i)[1];

            if (TextUtils.isEmpty(firstElement)) break;

            if (!firstElement.equals("") && secondElement.equals("") || i == Components_Database.size()-1 || i==3000) {

                kanjiComponent.setAssociatedComponents(associatedComponents);
                associatedComponents = new ArrayList<>();
                if (i>1) kanjiComponents.add(kanjiComponent);

                if (i==3000) kanjiComponent = new KanjiComponent("full2");
                else if (i < Components_Database.size()-1) kanjiComponent = new KanjiComponent(firstElement);
            }
            if (!firstElement.equals("") && !secondElement.equals("")) {
                KanjiComponent.AssociatedComponent associatedComponent = new KanjiComponent.AssociatedComponent();
                associatedComponent.setComponent(firstElement);
                associatedComponent.setAssociatedComponents(secondElement);
                associatedComponents.add(associatedComponent);
            }
        }
        kanjiComponent().insertAll(kanjiComponents);

        Log.i("Diagnosis Time","Loaded Kanji Components Database.");
    }

    //Switches the internal implementation with an empty in-memory database
    @VisibleForTesting
    public static void switchToInMemory(Context context) {
        sInstance = Room
                .inMemoryDatabaseBuilder(context.getApplicationContext(),JapaneseToolboxKanjiRoomDatabase.class)
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

    public List<KanjiCharacter> getKanjiCharactersByHexIdList(List<String> queryHexList) {
        return kanjiCharacter().getKanjiCharactersByHexIdList(queryHexList);
    }
    public List<KanjiCharacter> getKanjiCharactersByDescriptor(String query) {
        return kanjiCharacter().getKanjiCharactersByDescriptor(query);
    }
    public List<String> getAllKanjis() {
        return kanjiCharacter().getAllKanjis();
    }

    public List<KanjiCharacter> getAllKanjiCharacters() {
        return kanjiCharacter().getAllKanjiCharacters();
    }
    public KanjiCharacter getKanjiCharacterByHexId(String queryHex) {
        return kanjiCharacter().getKanjiCharacterByHexId(queryHex);
    }

    public List<KanjiComponent> getKanjiComponentsByStructureName(String structure) {
        return kanjiComponent().getKanjiComponentsByStructure(structure);
    }
    public KanjiComponent getKanjiComponentsById(long id) {
        return kanjiComponent().getKanjiComponentById(id);
    }
    public List<KanjiComponent> getAllKanjiComponents() {
        return kanjiComponent().getAllKanjiComponents();
    }

}
