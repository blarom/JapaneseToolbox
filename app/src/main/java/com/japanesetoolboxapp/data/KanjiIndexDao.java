package com.japanesetoolboxapp.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface KanjiIndexDao {
    //For example: https://github.com/googlesamples/android-architecture-components/blob/master/PersistenceContentProviderSample/app/src/main/java/com/example/android/contentprovidersample/data/CheeseDao.java

    //Return number of KanjiIndexes in a table
    @Query("SELECT COUNT(*) FROM " + KanjiIndex.TABLE_NAME)
    int count();

    //Insert KanjiIndex into table
    @Insert
    long insert(KanjiIndex KanjiIndex);

    //Insert multiple KanjiIndexes into table
    @Insert
    long[] insertAll(List<KanjiIndex> KanjiIndexs);

    //Get all KanjiIndexes in the table
    @Query("SELECT * FROM " + KanjiIndex.TABLE_NAME)
    List<KanjiIndex> getAllKanjiIndexes();

    //Get a KanjiIndex by Kanji
    @Query("SELECT * FROM " + KanjiIndex.TABLE_NAME + " WHERE " + KanjiIndex.COLUMN_KANA + " = :kana")
    KanjiIndex getKanjiIndexByKanji(String kana);

    //Get a KanjiIndex list by similar query match - see: https://stackoverflow.com/questions/44234644/android-rooms-search-in-string
    @Query("SELECT * FROM " + KanjiIndex.TABLE_NAME + " WHERE " + KanjiIndex.COLUMN_KANA_IDS + " LIKE :query  || '%' ")
    List<KanjiIndex> getKanjiIndexByStartingUTF8Query(String query);

    //Get a KanjiIndex by Exact query match
    @Query("SELECT * FROM " + KanjiIndex.TABLE_NAME + " WHERE " + KanjiIndex.COLUMN_KANA_IDS + " = :query")
    KanjiIndex getKanjiIndexByExactUTF8Query(String query);

    //Delete a KanjiIndex by Kanji
    @Query("DELETE FROM " + KanjiIndex.TABLE_NAME + " WHERE " + KanjiIndex.COLUMN_KANA + " = :kana")
    int deleteKanjiIndexByKanji(String kana);

    @Delete
    void deleteKanjiIndexes(KanjiIndex... KanjiIndexes);

    //Update a KanjiIndex by Id
    @Update
    int update(KanjiIndex KanjiIndex);


}
