package com.japanesetoolboxapp.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface WordDao {
    //For example: https://github.com/googlesamples/android-architecture-components/blob/master/PersistenceContentProviderSample/app/src/main/java/com/example/android/contentprovidersample/data/CheeseDao.java

    //Return number of Words in a table
    @Query("SELECT COUNT(*) FROM " + Word.TABLE_NAME)
    int count();

    //Insert Word into table
    @Insert
    long insert(Word Word);

    //Insert multiple Words into table
    @Insert
    long[] insertAll(List<Word> Words);

    //Get all Words in the table
    @Query("SELECT * FROM " + Word.TABLE_NAME)
    List<Word> getAllWords();

    //Get a Word by Id
    @Query("SELECT * FROM " + Word.TABLE_NAME + " WHERE " + Word.COLUMN_ID + " = :id")
    Word getWordByWordId(long id);

    //Get a Word list by Ids
    @Query("SELECT * FROM " + Word.TABLE_NAME + " WHERE " + Word.COLUMN_ID + " IN (:ids)")
    List<Word> getWordListByWordIds(List<Long> ids);

    //Delete a Word by Id
    @Query("DELETE FROM " + Word.TABLE_NAME + " WHERE " + Word.COLUMN_ID + " = :id")
    int deleteWordById(long id);

    @Delete
    void deleteWords(Word... Words);

    //Update a Word by Id
    @Update
    int update(Word Word);


}
