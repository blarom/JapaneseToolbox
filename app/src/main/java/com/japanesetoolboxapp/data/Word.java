package com.japanesetoolboxapp.data;

import java.util.List;

public class Word {


    private String romaji;
    private String kanji;
    private String altSpellings;
    private List<Meaning> meanings;

    public void setRomaji(String romaji) {
        this.romaji = romaji;
    }
    public String getRomaji() {
        return romaji;
    }

    public void setKanji(String kanji) {
        this.kanji = kanji;
    }
    public String getKanji() {
        return kanji;
    }

    public void setAltSpellings(String altSpellings) {
        this.altSpellings = altSpellings;
    }
    public String getAltSpellings() {
        return altSpellings;
    }

    public void setMeanings(List<Meaning> meanings) {
        this.meanings = meanings;
    }
    public List<Meaning> getMeanings() {
        return meanings;
    }


    public static class Meaning {

        private String meaning;
        private String type;
        private String antonym;
        private String synonym;
        private List<Explanation> explanations;

        public void setMeaning(String meaning) {
            this.meaning = meaning;
        }
        public String getMeaning() {
            return meaning;
        }

        public void setType(String type) {
            this.type = type;
        }
        public String getType() {
            return type;
        }

        public void setAntonym(String antonym) {
            this.antonym = antonym;
        }
        public String getAntonym() {
            return antonym;
        }

        public void setSynonym(String synonym) {
            this.synonym = synonym;
        }
        public String getSynonym() {
            return synonym;
        }

        public void setExplanations(List<Explanation> explanations) {
            this.explanations = explanations;
        }
        public List<Explanation> getExplanations() {
            return explanations;
        }


        public static class Explanation {

            private String explanation;
            private String rules;
            private List<Example> examples;

            public void setExplanation(String explanation) {
                this.explanation = explanation;
            }
            public String getExplanation() {
                return explanation;
            }

            public void setRules(String rules) {
                this.rules = rules;
            }
            public String getRules() {
                return rules;
            }

            public void setExamples(List<Example> examples) {
                this.examples = examples;
            }
            public List<Example> getExamples() {
                return examples;
            }


            public static class Example {

                private String englishSentence;
                private String romajiSentence;
                private String kanjiSentence;

                public void setEnglishSentence(String englishSentence) {
                    this.englishSentence = englishSentence;
                }
                public String getEnglishSentence() {
                    return englishSentence;
                }

                public void setRomajiSentence(String romajiSentence) {
                    this.romajiSentence = romajiSentence;
                }
                public String getRomajiSentence() {
                    return romajiSentence;
                }

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
