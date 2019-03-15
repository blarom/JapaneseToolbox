package com.japanesetoolboxapp.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;


@Entity(tableName = IndexEnglish.TABLE_NAME)
public class IndexEnglish {

    static final String TABLE_NAME = "latin_index_table";
    static final String COLUMN_ENGLISH = "english";
    private static final String WORD_IDS = "word_ids";

    IndexEnglish() { }

    @Ignore
    IndexEnglish(@NonNull String english, String wordIds) {
        this.english = english;
        this.wordIds = wordIds;
    }

    @PrimaryKey()
    @ColumnInfo(index = true, name = COLUMN_ENGLISH)
    @NonNull
    private String english = ".";
    public void setEnglish(@NonNull String english) {
        this.english = english;
    }
    @NonNull public String getEnglish() {
        return english;
    }

    @ColumnInfo(name = WORD_IDS)
    private String wordIds;
    public void setWordIds(String wordIds) {
        this.wordIds = wordIds;
    }
    public String getWordIds() {
        return wordIds;
    }


}
