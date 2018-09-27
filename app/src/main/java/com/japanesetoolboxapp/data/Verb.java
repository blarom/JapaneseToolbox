package com.japanesetoolboxapp.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

@Entity(tableName = Verb.TABLE_NAME)
public class Verb implements Parcelable {

    public static final String TABLE_NAME = "verbs_table";
    public static final String COLUMN_ID = "verb_id";
    static final String COLUMN_VERB_FAMILY = "family";
    static final String COLUMN_VERB_MEANING = "meaning";
    static final String COLUMN_VERB_TRANSITIVE = "transitive";
    static final String COLUMN_VERB_PREPOSITION = "preposition";
    static final String COLUMN_VERB_KANA = "kana";
    static final String COLUMN_VERB_KANJI = "kanji";
    static final String COLUMN_VERB_ROMAJI = "romaji";
    static final String COLUMN_VERB_KANJIROOT = "kanjiroot";
    static final String COLUMN_VERB_LATINROOT = "latinroot";
    static final String COLUMN_VERB_EXCEPTIONINDEX = "exceptionindex";
    static final String COLUMN_VERB_ALTSPELLINGS = "altspellings";
    static final String COLUMN_VERB_CONJUGATIONCATEGORIES = "conjugationcategories";

    public Verb() {}

    @Ignore
    public Verb(String family, String meaning, String trans, String preposition,
                String kana, String kanji, String romaji, String kanjiRoot,
                String latinRoot, String exceptionIndex, String altSpellings) {
        this.family = family;
        this.meaning = meaning;
        this.trans = trans;
        this.preposition = preposition;
        this.kana = kana;
        this.kanji = kanji;
        this.romaji = romaji;
        this.kanjiRoot = kanjiRoot;
        this.latinRoot = latinRoot;
        this.exceptionIndex = exceptionIndex;
        this.altSpellings = altSpellings;
    }


    protected Verb(Parcel in) {
        verb_id = in.readLong();
        family = in.readString();
        meaning = in.readString();
        trans = in.readString();
        preposition = in.readString();
        kana = in.readString();
        kanji = in.readString();
        romaji = in.readString();
        kanjiRoot = in.readString();
        latinRoot = in.readString();
        exceptionIndex = in.readString();
        altSpellings = in.readString();
    }

    public static final Creator<Verb> CREATOR = new Creator<Verb>() {
        @Override
        public Verb createFromParcel(Parcel in) {
            return new Verb(in);
        }

        @Override
        public Verb[] newArray(int size) {
            return new Verb[size];
        }
    };

    @PrimaryKey()
    @ColumnInfo(index = true, name = COLUMN_ID)
    public long verb_id;
    public long getVerbId() {
        return verb_id;
    }
    public void setVerbId(long verb_id) {
        this.verb_id = verb_id;
    }

    @ColumnInfo(name = COLUMN_VERB_FAMILY)
    private String family = "";
    public String getFamily() {
        return family;
    }
    public void setFamily(String family) {
        this.family = family;
    }

    @ColumnInfo(name = COLUMN_VERB_MEANING)
    private String meaning = "";
    public String getMeaning() {
        return meaning;
    }
    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    @ColumnInfo(name = COLUMN_VERB_TRANSITIVE)
    private String trans = "";
    public String getTrans() {
        return trans;
    }
    public void setTrans(String trans) {
        this.trans = trans;
    }

    @ColumnInfo(name = COLUMN_VERB_PREPOSITION)
    private String preposition = "";
    public String getPreposition() {
        return preposition;
    }
    public void setPreposition(String preposition) {
        this.preposition = preposition;
    }

    @ColumnInfo(name = COLUMN_VERB_KANA)
    private String kana = "";
    public String getKana() {
        return kana;
    }
    public void setKana(String kana) {
        this.kana = kana;
    }

    @ColumnInfo(name = COLUMN_VERB_KANJI)
    private String kanji = "";
    public String getKanji() {
        return kanji;
    }
    public void setKanji(String kanji) {
        this.kanji = kanji;
    }

    @ColumnInfo(name = COLUMN_VERB_ROMAJI)
    private String romaji = "";
    public String getRomaji() {
        return romaji;
    }
    public void setRomaji(String romaji) {
        this.romaji = romaji;
    }

    @ColumnInfo(name = COLUMN_VERB_KANJIROOT)
    private String kanjiRoot = "";
    public String getKanjiRoot() {
        return kanjiRoot;
    }
    public void setKanjiRoot(String kanjiRoot) {
        this.kanjiRoot = kanjiRoot;
    }

    @ColumnInfo(name = COLUMN_VERB_LATINROOT)
    private String latinRoot = "";
    public String getLatinRoot() {
        return latinRoot;
    }
    public void setLatinRoot(String latinRoot) {
        this.latinRoot = latinRoot;
    }

    @ColumnInfo(name = COLUMN_VERB_EXCEPTIONINDEX)
    private String exceptionIndex = "";
    public String getExceptionIndex() {
        return exceptionIndex;
    }
    public void setExceptionIndex(String exceptionIndex) {
        this.exceptionIndex = exceptionIndex;
    }

    @ColumnInfo(name = COLUMN_VERB_ALTSPELLINGS)
    private String altSpellings = "";
    public String getAltSpellings() {
        return altSpellings;
    }
    public void setAltSpellings(String altSpellings) {
        this.altSpellings = altSpellings;
    }

    @Ignore
    @ColumnInfo(name = COLUMN_VERB_CONJUGATIONCATEGORIES)
    private List<ConjugationCategory> conjugationCategories;
    public List<ConjugationCategory> getConjugationCategories() {
        return conjugationCategories;
    }
    public void setConjugationCategories(List<ConjugationCategory> conjugationCategories) {
        this.conjugationCategories = conjugationCategories;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(verb_id);
        parcel.writeString(family);
        parcel.writeString(meaning);
        parcel.writeString(trans);
        parcel.writeString(preposition);
        parcel.writeString(kana);
        parcel.writeString(kanji);
        parcel.writeString(romaji);
        parcel.writeString(kanjiRoot);
        parcel.writeString(latinRoot);
        parcel.writeString(exceptionIndex);
        parcel.writeString(altSpellings);
    }

    public static class ConjugationCategory {

        private List<Conjugation> conjugations;
        public List<Conjugation> getConjugations() {
            return conjugations;
        }
        public void setConjugations(List<Conjugation> conjugations) {
            this.conjugations = conjugations;
        }

        public static class Conjugation {

            private String conjugationLatin;
            public String getConjugationLatin() {
                return conjugationLatin;
            }
            public void setConjugationLatin(String conjugationLatin) {
                this.conjugationLatin = conjugationLatin;
            }

            private String conjugationKanji;
            public String getConjugationKanji() {
                return conjugationKanji;
            }
            public void setConjugationKanji(String conjugationKanji) {
                this.conjugationKanji = conjugationKanji;
            }
        }

    }
}
