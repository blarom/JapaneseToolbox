package com.japanesetoolboxapp.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.provider.BaseColumns;

import java.util.Comparator;

@Entity(tableName = KanjiCharacter.TABLE_NAME, indices = {@Index("hexIdentifier")})
public class KanjiCharacter  {

    public static final String TABLE_NAME = "kanji_characters_table";
    public static final String COLUMN_ID = BaseColumns._ID;
    static final String COLUMN_KANJI = "kanji";
    static final String COLUMN_KANJI_HEX_ID = "hexIdentifier";
    static final String COLUMN_KANJI_STRUCTURE = "structure";
    static final String COLUMN_KANJI_COMPONENTS = "components";
    static final String COLUMN_KANJI_READINGS = "readings";
    static final String COLUMN_KANJI_MEANINGS = "meanings";
    static final String COLUMN_KANJI_RADICAL_PLUS_STROKES = "radPlusStrokes";

    public KanjiCharacter() {
    }

    @Ignore
    public KanjiCharacter(String hexId) {
        this.hexIdentifier = hexId;
    }

    @Ignore
    public KanjiCharacter(String hexId, String structure, String components) {
        this.hexIdentifier = hexId;
        this.structure = structure;
        this.components = components;
    }

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(index = true, name = COLUMN_ID)
    public long id;
    public long getKanjiCharacterId() {
        return id;
    }
    public void setKanjiCharacterId(long id) {
        this.id = id;
    }

    @ColumnInfo(name = COLUMN_KANJI)
    private String kanji;
    public String getKanji() {
        return kanji;
    }
    public void setKanji(String kanji) {
        this.kanji = kanji;
    }

    @ColumnInfo(name = COLUMN_KANJI_HEX_ID)
    private String hexIdentifier;
    public String getHexIdentifier() {
        return hexIdentifier;
    }
    public void setHexIdentifier(String hexIdentifier) {
        this.hexIdentifier = hexIdentifier;
    }

    @ColumnInfo(name = COLUMN_KANJI_STRUCTURE)
    private String structure;
    public void setStructure(String structure) {
        this.structure = structure;
    }
    public String getStructure() {
        return structure;
    }

    @ColumnInfo(name = COLUMN_KANJI_COMPONENTS)
    private String components;
    public void setComponents(String components) {
        this.components = components;
    }
    public String getComponents() {
        return components;
    }

    @ColumnInfo(name = COLUMN_KANJI_READINGS)
    private String readings;
    public void setReadings(String readings) {
        this.readings = readings;
    }
    public String getReadings() {
        return readings;
    }

    @ColumnInfo(name = COLUMN_KANJI_MEANINGS)
    private String meanings;
    public void setMeanings(String meanings) {
        this.meanings = meanings;
    }
    public String getMeanings() {
        return meanings;
    }

    @ColumnInfo(name = COLUMN_KANJI_RADICAL_PLUS_STROKES)
    private String radPlusStrokes;
    public void setRadPlusStrokes(String radPlusStrokes) {
        this.radPlusStrokes = radPlusStrokes;
    }
    public String getRadPlusStrokes() {
        return radPlusStrokes;
    }

    public static Comparator<KanjiCharacter> hexIdentiferComparatorAscending = new Comparator<KanjiCharacter>() {
        public int compare(KanjiCharacter u1, KanjiCharacter u2)
        {
            return u1.getHexIdentifier().compareTo(u2.getHexIdentifier());
        }
    };
}
