package com.japanesetoolboxapp.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface KanjiCharacterDao {
    //For example: https://github.com/googlesamples/android-architecture-components/blob/master/PersistenceContentProviderSample/app/src/main/java/com/example/android/contentprovidersample/data/CheeseDao.java

    //Return number of KanjiCharacters in a table
    @Query("SELECT COUNT(*) FROM " + KanjiCharacter.TABLE_NAME)
    int count();

    //Insert KanjiCharacter into table
    @Insert
    long insert(KanjiCharacter KanjiCharacter);

    //Insert multiple KanjiCharacters into table
    @Insert
    long[] insertAll(List<KanjiCharacter> KanjiCharacters);

    //Get all KanjiCharacters in the table
    @Query("SELECT * FROM " + KanjiCharacter.TABLE_NAME)
    List<KanjiCharacter> getAllKanjiCharacters();

    //Get a KanjiCharacter by Id
    @Query("SELECT * FROM " + KanjiCharacter.TABLE_NAME + " WHERE " + KanjiCharacter.COLUMN_ID + " = :id")
    KanjiCharacter getKanjiCharacterById(long id);

    //Get a KanjiCharacter by Id
    @Query("SELECT * FROM " + KanjiCharacter.TABLE_NAME + " WHERE " + KanjiCharacter.COLUMN_KANJI_HEX_ID + " = :hexId")
    KanjiCharacter getKanjiCharacterByHexId(String hexId);

    //Get a KanjiCharacter list by Ids
    @Query("SELECT * FROM " + KanjiCharacter.TABLE_NAME + " WHERE " + KanjiCharacter.COLUMN_KANJI_HEX_ID + " IN (:hexIdList)")
    List<KanjiCharacter> getKanjiCharactersByHexIdList(List<String> hexIdList);

    //Delete a KanjiCharacter by Id
    @Query("DELETE FROM " + KanjiCharacter.TABLE_NAME + " WHERE " + KanjiCharacter.COLUMN_ID + " = :id")
    int deleteKanjiCharacterById(long id);

    @Delete
    void deleteKanjiCharacters(KanjiCharacter... KanjiCharacters);

    //Update a KanjiCharacter by Id
    @Update
    int update(KanjiCharacter KanjiCharacter);


}
