package com.japanesetoolboxapp.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;


@Entity(tableName = IndexRomaji.TABLE_NAME)
public class IndexRomaji {

    static final String TABLE_NAME = "romaji_index_table";
    static final String COLUMN_VALUE = "value";
    private static final String WORD_IDS = "word_ids";

    IndexRomaji() { }

    @Ignore
    IndexRomaji(@NonNull String english, String wordIds) {
        this.value = english;
        this.wordIds = wordIds;
    }

    @PrimaryKey()
    @ColumnInfo(index = true, name = COLUMN_VALUE)
    @NonNull
    private String value = ".";
    public void setValue(@NonNull String value) {
        this.value = value;
    }
    @NonNull public String getValue() {
        return value;
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
