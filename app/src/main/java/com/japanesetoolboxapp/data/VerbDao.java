package com.japanesetoolboxapp.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface VerbDao {
    //For example: https://github.com/googlesamples/android-architecture-components/blob/master/PersistenceContentProviderSample/app/src/main/java/com/example/android/contentprovidersample/data/CheeseDao.java

    //Return number of Verbs in a table
    @Query("SELECT COUNT(*) FROM " + Verb.TABLE_NAME)
    int count();

    //Insert Verb into table
    @Insert
    long insert(Verb Verb);

    //Insert multiple Verbs into table
    @Insert
    long[] insertAll(List<Verb> Verbs);

    //Get all Verbs in the table
    @Query("SELECT * FROM " + Verb.TABLE_NAME)
    List<Verb> getAllVerbs();

    //Get a Verb by Id
    @Query("SELECT * FROM " + Verb.TABLE_NAME + " WHERE " + Verb.COLUMN_ID + " = :id")
    Verb getVerbByVerbId(long id);

    //Get a Verb by Id
    @Query("SELECT * FROM " + Verb.TABLE_NAME + " WHERE " + Verb.COLUMN_ID + " IN (:ids)")
    List<Verb> getVerbListByVerbIds(List<Long> ids);

    //Get a List of Verbs by Exact query match in Romaji column
    @Query("SELECT * FROM " + Verb.TABLE_NAME + " WHERE " + Verb.COLUMN_VERB_ROMAJI + " = :query")
    List<Verb> getVerbByExactRomajiQueryMatch(String query);

    //Get a List of Verbs by Exact query match in Romaji column
    @Query("SELECT * FROM " + Verb.TABLE_NAME + " WHERE " + Verb.COLUMN_VERB_KANJI + " = :query")
    List<Verb> getVerbByExactKanjiQueryMatch(String query);

    //Delete a Verb by Id
    @Query("DELETE FROM " + Verb.TABLE_NAME + " WHERE " + Verb.COLUMN_ID + " = :id")
    int deleteVerbById(long id);

    @Delete
    void deleteVerbs(Verb... Verbs);

    //Update a Verb by Id
    @Update
    int update(Verb Verb);


}
