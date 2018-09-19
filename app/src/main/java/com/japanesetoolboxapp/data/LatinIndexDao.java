package com.japanesetoolboxapp.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface LatinIndexDao {
    //For example: https://github.com/googlesamples/android-architecture-components/blob/master/PersistenceContentProviderSample/app/src/main/java/com/example/android/contentprovidersample/data/CheeseDao.java

    //Return number of LatinIndexes in a table
    @Query("SELECT COUNT(*) FROM " + LatinIndex.TABLE_NAME)
    int count();

    //Insert LatinIndex into table
    @Insert
    long insert(LatinIndex LatinIndex);

    //Insert multiple LatinIndexes into table
    @Insert
    long[] insertAll(List<LatinIndex> LatinIndexs);

    //Get all LatinIndexes in the table
    @Query("SELECT * FROM " + LatinIndex.TABLE_NAME)
    List<LatinIndex> getAllLatinIndexes();

    //Get a LatinIndex by Exact query match
    @Query("SELECT * FROM " + LatinIndex.TABLE_NAME + " WHERE " + LatinIndex.COLUMN_LATIN + " = :query")
    LatinIndex getLatinIndexByExactLatinQuery(String query);

    //Get a LatinIndex list by similar query match - see: https://stackoverflow.com/questions/44234644/android-rooms-search-in-string
    @Query("SELECT * FROM " + LatinIndex.TABLE_NAME + " WHERE " + LatinIndex.COLUMN_LATIN + " LIKE :query  || '%' ")
    List<LatinIndex> getLatinIndexByStartingLatinQuery(String query);

    //Delete a LatinIndex by Latin
    @Query("DELETE FROM " + LatinIndex.TABLE_NAME + " WHERE " + LatinIndex.COLUMN_LATIN + " = :latin")
    int deleteLatinIndexByLatin(String latin);

    @Delete
    void deleteLatinIndexes(LatinIndex... LatinIndexes);

    //Update a LatinIndex by Id
    @Update
    int update(LatinIndex LatinIndex);


}
