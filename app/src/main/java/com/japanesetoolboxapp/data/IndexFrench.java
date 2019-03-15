package com.japanesetoolboxapp.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;


@Entity(tableName = IndexFrench.TABLE_NAME)
public class IndexFrench {

    static final String TABLE_NAME = "latin_index_table";
    static final String COLUMN_FRENCH = "french";
    private static final String WORD_IDS = "word_ids";

    IndexFrench() { }

    @Ignore
    IndexFrench(@NonNull String french, String wordIds) {
        this.french = french;
        this.wordIds = wordIds;
    }

    @PrimaryKey()
    @ColumnInfo(index = true, name = COLUMN_FRENCH)
    @NonNull
    private String french = ".";
    public void setFrench(@NonNull String french) {
        this.french = french;
    }
    @NonNull public String getFrench() {
        return french;
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
