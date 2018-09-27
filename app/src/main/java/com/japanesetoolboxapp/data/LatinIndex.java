package com.japanesetoolboxapp.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;


@Entity(tableName = LatinIndex.TABLE_NAME)
public class LatinIndex {

    static final String TABLE_NAME = "latin_index_table";
    static final String COLUMN_LATIN = "latin";
    static final String WORD_IDS = "word_ids";

    LatinIndex() { }

    @Ignore
    LatinIndex(@NonNull String latin, String wordIds) {
        this.latin = latin;
        this.wordIds = wordIds;
    }

    @PrimaryKey()
    @ColumnInfo(index = true, name = COLUMN_LATIN)
    @NonNull
    private String latin = ".";
    public void setLatin(@NonNull String latin) {
        this.latin = latin;
    }
    @NonNull public String getLatin() {
        return latin;
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
