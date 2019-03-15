package com.japanesetoolboxapp.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;


@Entity(tableName = IndexRomaji.TABLE_NAME)
public class IndexRomaji {

    static final String TABLE_NAME = "latin_index_table";
    static final String COLUMN_ROMAJI = "romaji";
    private static final String WORD_IDS = "word_ids";

    IndexRomaji() { }

    @Ignore
    IndexRomaji(@NonNull String english, String wordIds) {
        this.romaji = english;
        this.wordIds = wordIds;
    }

    @PrimaryKey()
    @ColumnInfo(index = true, name = COLUMN_ROMAJI)
    @NonNull
    private String romaji = ".";
    public void setRomaji(@NonNull String romaji) {
        this.romaji = romaji;
    }
    @NonNull public String getRomaji() {
        return romaji;
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
