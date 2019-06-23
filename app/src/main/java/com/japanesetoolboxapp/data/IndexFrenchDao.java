package com.japanesetoolboxapp.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface IndexFrenchDao {
    //For example: https://github.com/googlesamples/android-architecture-components/blob/master/PersistenceContentProviderSample/app/src/main/java/com/example/android/contentprovidersample/data/CheeseDao.java

    //Return number of IndexFrench in a table
    @Query("SELECT COUNT(*) FROM " + IndexFrench.TABLE_NAME)
    int count();

    //Insert IndexFrench into table
    @Insert
    long insert(IndexFrench index);

    //Insert multiple LatinIndexes into table
    @Insert
    long[] insertAll(List<IndexFrench> indices);

    //Get all IndexFrench in the table
    @Query("SELECT * FROM " + IndexFrench.TABLE_NAME)
    List<IndexFrench> getAllLatinIndexes();

    //Get a IndexFrench by Exact query match
    @Query("SELECT * FROM " + IndexFrench.TABLE_NAME + " WHERE " + IndexFrench.COLUMN_VALUE + " LIKE :query")
    IndexFrench getIndexByExactQuery(String query);

    //Get a IndexFrench list by similar latin index query match - see: https://stackoverflow.com/questions/44234644/android-rooms-search-in-string
    @Query("SELECT * FROM " + IndexFrench.TABLE_NAME + " WHERE " + IndexFrench.COLUMN_VALUE + " LIKE :query  || '%' ")
    List<IndexFrench> getIndexByStartingQuery(String query);

    //Delete a IndexFrench by Latin
    @Query("DELETE FROM " + IndexFrench.TABLE_NAME + " WHERE " + IndexFrench.COLUMN_VALUE + " = :latin")
    int deletIndexByLatin(String latin);

    @Delete
    void deleteIndexes(IndexFrench... indices);

    //Update a IndexFrench by Id
    @Update
    int update(IndexFrench IndexFrench);


}
