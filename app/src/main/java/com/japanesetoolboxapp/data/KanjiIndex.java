package com.japanesetoolboxapp.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;


@Entity(tableName = KanjiIndex.TABLE_NAME)
public class KanjiIndex {

    static final String TABLE_NAME = "kanji_index_table";
    static final String COLUMN_KANA = "kana";
    static final String WORD_IDS = "word_ids";
    static final String COLUMN_KANA_IDS = "kana_ids";

    public KanjiIndex() {
    }

    @ColumnInfo(name = COLUMN_KANA)
    private String kana;
    public void setKana(String kana) {
        this.kana = kana;
    }
    public String getKana() {
        return kana;
    }

    @ColumnInfo(name = WORD_IDS)
    private String wordIds;
    public void setWordIds(String wordIds) {
        this.wordIds = wordIds;
    }
    public String getWordIds() {
        return wordIds;
    }

    @PrimaryKey()
    @ColumnInfo(index = true, name = COLUMN_KANA_IDS)
    @NonNull private String kanaIds = ".";
    public void setKanaIds(@NonNull String kanaIds) {
        this.kanaIds = kanaIds;
    }
    @NonNull public String getKanaIds() {
        return kanaIds;
    }

}
