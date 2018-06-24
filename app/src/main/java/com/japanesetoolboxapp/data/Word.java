package com.japanesetoolboxapp.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.provider.BaseColumns;

import java.util.List;


@Entity(tableName = Word.TABLE_NAME, indices = {@Index("uniqueIdentifier")})
public class Word {

    public static final String TABLE_NAME = "words_table";
    public static final String COLUMN_ID = BaseColumns._ID;
    public static final String COLUMN_WORD_KEYWORDS = "keywords";
    public static final String COLUMN_WORD_UNIQUE_ID = "uniqueIdentifier";
    public static final String COLUMN_WORD_ROMAJI = "romaji";
    public static final String COLUMN_WORD_KANJI = "kanji";
    public static final String COLUMN_WORD_ALT_SPELLINGS = "altSpellings";
    public static final String COLUMN_WORD_MEANINGS = "meanings";
    public static final String COLUMN_WORD_COMMON_STATUS = "commonStatus";

    public Word() {
    }

    @PrimaryKey()
    @ColumnInfo(index = true, name = COLUMN_ID)
    public long id;
    public long getWordId() {
        return id;
    }
    public void setWordId(long new_id) {
        id = new_id;
    }

    @ColumnInfo(name = COLUMN_WORD_KEYWORDS)
    private String keywords;
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
    public String getKeywords() {
        return keywords;
    }

    @ColumnInfo(name = COLUMN_WORD_UNIQUE_ID)
    private String uniqueIdentifier;
    public void setUniqueIdentifier(String uniqueIdentifier) {
        this.uniqueIdentifier = uniqueIdentifier;
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
    private int commonStatus; //0 for uncommon word, 1 for common word, 2 for local word (ie. might be uncommon but included anyway)
    public void setCommonStatus(int commonStatus) {
        this.commonStatus = commonStatus;
    }
    public int getCommonStatus() {
        return commonStatus;
    }

    @TypeConverters({WordDbTypeConverters.class})
    @ColumnInfo(name = COLUMN_WORD_MEANINGS)
    private List<Meaning> meanings;
    public void setMeanings(List<Meaning> meanings) {
        this.meanings = meanings;
    }
    public List<Meaning> getMeanings() {
        return meanings;
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

        private String type;
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

        @TypeConverters({WordDbTypeConverters.class})
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

            @TypeConverters({WordDbTypeConverters.class})
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
