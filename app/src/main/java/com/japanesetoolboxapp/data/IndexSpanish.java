package com.japanesetoolboxapp.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;


@Entity(tableName = IndexSpanish.TABLE_NAME)
public class IndexSpanish {

    static final String TABLE_NAME = "latin_index_table";
    static final String COLUMN_SPANISH = "spanish";
    private static final String WORD_IDS = "word_ids";

    IndexSpanish() { }

    @Ignore
    IndexSpanish(@NonNull String spanish, String wordIds) {
        this.spanish = spanish;
        this.wordIds = wordIds;
    }

    @PrimaryKey()
    @ColumnInfo(index = true, name = COLUMN_SPANISH)
    @NonNull
    private String spanish = ".";
    public void setSpanish(@NonNull String spanish) {
        this.spanish = spanish;
    }
    @NonNull public String getSpanish() {
        return spanish;
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
