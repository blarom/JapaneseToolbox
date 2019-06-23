package com.japanesetoolboxapp.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;


@Entity(tableName = IndexFrench.TABLE_NAME)
public class IndexFrench {

    static final String TABLE_NAME = "french_index_table";
    static final String COLUMN_VALUE = "value";
    private static final String WORD_IDS = "word_ids";

    IndexFrench() { }

    @Ignore
    IndexFrench(@NonNull String value, String wordIds) {
        this.value = value;
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
