package com.japanesetoolboxapp.data;

import java.util.List;

public class Verb {

    public Verb() {}

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

    private String family;
    public String getFamily() {
        return family;
    }
    public void setFamily(String family) {
        this.family = family;
    }

    private String meaning;
    public String getMeaning() {
        return meaning;
    }
    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    private String trans;
    public String getTrans() {
        return trans;
    }
    public void setTrans(String trans) {
        this.trans = trans;
    }

    private String preposition;
    public String getPreposition() {
        return preposition;
    }
    public void setPreposition(String preposition) {
        this.preposition = preposition;
    }

    private String kana;
    public String getKana() {
        return kana;
    }
    public void setKana(String kana) {
        this.kana = kana;
    }

    private String kanji;
    public String getKanji() {
        return kanji;
    }
    public void setKanji(String kanji) {
        this.kanji = kanji;
    }

    private String romaji;
    public String getRomaji() {
        return romaji;
    }
    public void setRomaji(String romaji) {
        this.romaji = romaji;
    }

    private String kanjiRoot;
    public String getKanjiRoot() {
        return kanjiRoot;
    }
    public void setKanjiRoot(String kanjiRoot) {
        this.kanjiRoot = kanjiRoot;
    }

    private String latinRoot;
    public String getLatinRoot() {
        return latinRoot;
    }
    public void setLatinRoot(String latinRoot) {
        this.latinRoot = latinRoot;
    }

    private String exceptionIndex;
    public String getExceptionIndex() {
        return exceptionIndex;
    }
    public void setExceptionIndex(String exceptionIndex) {
        this.exceptionIndex = exceptionIndex;
    }

    private String altSpellings;
    public String getAltSpellings() {
        return altSpellings;
    }
    public void setAltSpellings(String altSpellings) {
        this.altSpellings = altSpellings;
    }

    private List<ConjugationCategory> conjugationCategories;
    public List<ConjugationCategory> getConjugationCategories() {
        return conjugationCategories;
    }
    public void setConjugationCategories(List<ConjugationCategory> conjugationCategories) {
        this.conjugationCategories = conjugationCategories;
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
