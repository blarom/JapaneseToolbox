package com.japanesetoolboxapp.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

import com.japanesetoolboxapp.resources.Utilities;

import java.util.List;

@Entity(tableName = Word.TABLE_NAME, indices = {@Index("uniqueIdentifier")})
public class Word implements Parcelable {

    public static final String TABLE_NAME = "words_table";
    public static final String COLUMN_ID = BaseColumns._ID;
    private static final String COLUMN_WORD_UNIQUE_ID = "uniqueIdentifier";
    static final String COLUMN_WORD_ROMAJI = "romaji";
    static final String COLUMN_WORD_KANJI = "kanji";
    private static final String COLUMN_WORD_ALT_SPELLINGS = "altSpellings";
    private static final String COLUMN_WORD_MEANINGS_EN = "meaningsEN";
    private static final String COLUMN_WORD_MEANINGS_FR = "meaningsFR";
    private static final String COLUMN_WORD_MEANINGS_ES = "meaningsES";
    private static final String COLUMN_WORD_EXTRA_KEYWORDS_EN = "extraKeywordsEN";
    private static final String COLUMN_WORD_EXTRA_KEYWORDS_FR = "extraKeywordsFR";
    private static final String COLUMN_WORD_EXTRA_KEYWORDS_ES = "extraKeywordsES";
    private static final String COLUMN_WORD_COMMON_STATUS = "isCommon";
    private static final String COLUMN_WORD_IS_LOCAL = "isLocal";
    private static final String COLUMN_WORD_KEYWORDS_JAP = "keywordsJAP";

    public Word() {
    }


    Word(Parcel in) {
        id = in.readLong();
        extraKeywordsEN = in.readString();
        uniqueIdentifier = in.readString();
        romaji = in.readString();
        kanji = in.readString();
        altSpellings = in.readString();
        isCommon = in.readByte() != 0;
        isLocal = in.readByte() != 0;
    }

    public static final Creator<Word> CREATOR = new Creator<Word>() {
        @Override
        public Word createFromParcel(Parcel in) {
            return new Word(in);
        }

        @Override
        public Word[] newArray(int size) {
            return new Word[size];
        }
    };

    @PrimaryKey()
    @ColumnInfo(index = true, name = COLUMN_ID)
    public long id;
    public long getWordId() {
        return id;
    }
    public void setWordId(long new_id) {
        id = new_id;
    }

    @ColumnInfo(name = COLUMN_WORD_UNIQUE_ID)
    private String uniqueIdentifier;
    public void setUniqueIdentifier(String uniqueIdentifier) {
        this.uniqueIdentifier = uniqueIdentifier;
    }
    public void setUniqueIdentifierFromDetails() {
        this.uniqueIdentifier = Utilities.cleanIdentifierForFirebase(romaji + "-" + kanji);
    }
    public String getUniqueIdentifier() {
        return uniqueIdentifier;
    }

    @ColumnInfo(name = COLUMN_WORD_ROMAJI)
    private String romaji;
    public void setRomaji(String romaji) {
        this.romaji = romaji;
    }
    public String getRomaji() {
        return romaji;
    }

    @ColumnInfo(name = COLUMN_WORD_KANJI)
    private String kanji;
    public void setKanji(String kanji) {
        this.kanji = kanji;
    }
    public String getKanji() {
        return kanji;
    }

    @ColumnInfo(name = COLUMN_WORD_ALT_SPELLINGS)
    private String altSpellings;
    public void setAltSpellings(String altSpellings) {
        this.altSpellings = altSpellings;
    }
    public String getAltSpellings() {
        return altSpellings;
    }

    @ColumnInfo(name = COLUMN_WORD_COMMON_STATUS)
    private boolean isCommon; //0 for uncommon word, 1 for common word, 2 for local word (ie. might be uncommon but included anyway)
    public void setIsCommon(boolean commonStatus) {
        this.isCommon = commonStatus;
    }
    public boolean getIsCommon() {
        return isCommon;
    }

    @ColumnInfo(name = COLUMN_WORD_KEYWORDS_JAP)
    private String extraKeywordsJAP;
    public void setExtraKeywordsJAP(String extraKeywordsJAP) {
        this.extraKeywordsJAP = extraKeywordsJAP;
    }
    public String getExtraKeywordsJAP() {
        return extraKeywordsJAP;
    }

    @ColumnInfo(name = COLUMN_WORD_IS_LOCAL)
    private boolean isLocal = true;
    public void setIsLocal(boolean isLocal) {
        this.isLocal = isLocal;
    }
    public boolean getIsLocal() {
        return isLocal;
    }

    @TypeConverters({JapaneseToolboxDbTypeConverters.class})
    @ColumnInfo(name = COLUMN_WORD_MEANINGS_EN)
    private List<Meaning> meaningsEN;
    public void setMeaningsEN(List<Meaning> meaningsEN) {
        this.meaningsEN = meaningsEN;
    }
    public List<Meaning> getMeaningsEN() {
        return meaningsEN;
    }

    @ColumnInfo(name = COLUMN_WORD_EXTRA_KEYWORDS_EN)
    private String extraKeywordsEN;
    public void setExtraKeywordsEN(String extraKeywordsEN) {
        this.extraKeywordsEN = extraKeywordsEN;
    }
    public String getExtraKeywordsEN() {
        return extraKeywordsEN;
    }

    @TypeConverters({JapaneseToolboxDbTypeConverters.class})
    @ColumnInfo(name = COLUMN_WORD_MEANINGS_FR)
    private List<Meaning> meaningsFR;
    public void setMeaningsFR(List<Meaning> meaningsFR) {
        this.meaningsFR = meaningsFR;
    }
    public List<Meaning> getMeaningsFR() {
        return meaningsFR;
    }

    @ColumnInfo(name = COLUMN_WORD_EXTRA_KEYWORDS_FR)
    private String extraKeywordsFR;
    public void setExtraKeywordsFR(String extraKeywordsFR) {
        this.extraKeywordsFR = extraKeywordsFR;
    }
    public String getExtraKeywordsFR() {
        return extraKeywordsFR;
    }

    @TypeConverters({JapaneseToolboxDbTypeConverters.class})
    @ColumnInfo(name = COLUMN_WORD_MEANINGS_ES)
    private List<Meaning> meaningsES;
    public void setMeaningsES(List<Meaning> meaningsES) {
        this.meaningsES = meaningsES;
    }
    public List<Meaning> getMeaningsES() {
        return meaningsES;
    }

    @ColumnInfo(name = COLUMN_WORD_EXTRA_KEYWORDS_ES)
    private String extraKeywordsES;
    public void setExtraKeywordsES(String extraKeywordsES) {
        this.extraKeywordsES = extraKeywordsES;
    }
    public String getExtraKeywordsES() {
        return extraKeywordsES;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeString(extraKeywordsEN);
        parcel.writeString(uniqueIdentifier);
        parcel.writeString(romaji);
        parcel.writeString(kanji);
        parcel.writeString(altSpellings);
        parcel.writeByte((byte) (isCommon? 1 : 0));
        parcel.writeByte((byte) (isLocal? 1 : 0));
    }

    //@Embedded
    //Meaning meaning;

    public static class Meaning {

        //@Embedded
        //Explanation explanation;

        private String meaning;
        public void setMeaning(String meaning) {
            this.meaning = meaning;
        }
        public String getMeaning() {
            return meaning;
        }

        private String type = "";
        public void setType(String type) {
            this.type = type;
        }
        public String getType() {
            return type;
        }

        private String antonym;
        public void setAntonym(String antonym) {
            this.antonym = antonym;
        }
        public String getAntonym() {
            return antonym;
        }

        private String synonym;
        public void setSynonym(String synonym) {
            this.synonym = synonym;
        }
        public String getSynonym() {
            return synonym;
        }

        @TypeConverters({JapaneseToolboxDbTypeConverters.class})
        private List<Explanation> explanations;
        public void setExplanations(List<Explanation> explanations) {
            this.explanations = explanations;
        }
        public List<Explanation> getExplanations() {
            return explanations;
        }


        public static class Explanation {

            //@Embedded
            //Example example;

            private String explanation;
            public void setExplanation(String explanation) {
                this.explanation = explanation;
            }
            public String getExplanation() {
                return explanation;
            }

            private String rules;
            public void setRules(String rules) {
                this.rules = rules;
            }
            public String getRules() {
                return rules;
            }

            @TypeConverters({JapaneseToolboxDbTypeConverters.class})
            private List<Example> examples;
            public void setExamples(List<Example> examples) {
                this.examples = examples;
            }
            public List<Example> getExamples() {
                return examples;
            }


            public static class Example {

                private String englishSentence;
                public void setEnglishSentence(String englishSentence) {
                    this.englishSentence = englishSentence;
                }
                public String getEnglishSentence() {
                    return englishSentence;
                }

                private String romajiSentence;
                public void setRomajiSentence(String romajiSentence) {
                    this.romajiSentence = romajiSentence;
                }
                public String getRomajiSentence() {
                    return romajiSentence;
                }

                private String kanjiSentence;
                public void setKanjiSentence(String kanjiSentence) {
                    this.kanjiSentence = kanjiSentence;
                }
                public String getKanjiSentence() {
                    return kanjiSentence;
                }
            }
        }
    }
}
