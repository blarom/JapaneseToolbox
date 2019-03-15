package com.japanesetoolboxapp.resources;


import java.util.HashMap;

public final class GlobalConstants {

    // Defining the column title (and index) of each column in the excel files

    public static final int COLUMN_WORD_ID                      = ColIndexConverter("a");
    public static final int COLUMN_ROMAJI                       = ColIndexConverter("b");
    public static final int COLUMN_KANJI                        = ColIndexConverter("c");
    public static final int COLUMN_ALT_SPELLINGS                = ColIndexConverter("d");
    public static final int COLUMN_COMMON                       = ColIndexConverter("e");
    public static final int COLUMN_EXTRA_KEYWORDS_JAP           = ColIndexConverter("f");
    public static final int COLUMN_PREPOSITION                  = ColIndexConverter("g");
    public static final int COLUMN_KANJI_ROOT                   = ColIndexConverter("h");
    public static final int COLUMN_LATIN_ROOT                   = ColIndexConverter("i");
    public static final int COLUMN_EXCEPTION_INDEX              = ColIndexConverter("j");
    public static final int COLUMN_MEANING_EN_INDEXES           = ColIndexConverter("k");
    public static final int COLUMN_EXTRA_KEYWORDS_EN            = ColIndexConverter("l");
    public static final int COLUMN_MEANING_FR_INDEXES           = ColIndexConverter("m");
    public static final int COLUMN_EXTRA_KEYWORDS_FR            = ColIndexConverter("n");
    public static final int COLUMN_MEANING_ES_INDEXES           = ColIndexConverter("o");
    public static final int COLUMN_EXTRA_KEYWORDS_ES            = ColIndexConverter("p");

    public static final int COLUMN_MEANINGS_MEANING             = ColIndexConverter("b");
    public static final int COLUMN_MEANINGS_TYPE                = ColIndexConverter("c");
    public static final int COLUMN_MEANINGS_EXPLANATION         = ColIndexConverter("d");
    public static final int COLUMN_MEANINGS_RULES               = ColIndexConverter("e");
    public static final int COLUMN_MEANINGS_EXAMPLES            = ColIndexConverter("f");
    public static final int COLUMN_MEANINGS_ANTONYM             = ColIndexConverter("g");
    public static final int COLUMN_MEANINGS_SYNONYM             = ColIndexConverter("h");

    public static final int COLUMN_MULT_EXPLANATIONS_ITEM       = ColIndexConverter("b");
    public static final int COLUMN_MULT_EXPLANATIONS_RULE       = ColIndexConverter("c");
    public static final int COLUMN_MULT_EXPLANATIONS_EXAMPLES   = ColIndexConverter("d");

    public final static int COLUMN_EXAMPLES_ENGLISH             = ColIndexConverter("b");
    public final static int COLUMN_EXAMPLES_ROMAJI              = ColIndexConverter("c");
    public final static int COLUMN_EXAMPLES_KANJI               = ColIndexConverter("d");

    public final static int COLUMN_VERB_ISTEM                   = ColIndexConverter("j");


    public final static int Index_full = 0;
    public final static int Index_across2 = 1;
    public final static int Index_across3 = 2;
    public final static int Index_across4 = 3;
    public final static int Index_down2 = 4;
    public final static int Index_down3 = 5;
    public final static int Index_down4 = 6;
    public final static int Index_three_repeat = 7;
    public final static int Index_four_repeat = 8;
    public final static int Index_foursquare = 9;
    public final static int Index_five_repeat = 10;
    public final static int Index_topleftout = 11;
    public final static int Index_topout = 12;
    public final static int Index_toprightout = 13;
    public final static int Index_leftout = 14;
    public final static int Index_fullout = 15;
    public final static int Index_bottomleftout = 16;
    public final static int Index_bottomout = 17;

    public static final int TYPE_LATIN = 0;
    public static final int TYPE_HIRAGANA = 1;
    public static final int TYPE_KATAKANA = 2;
    public static final int TYPE_KANJI = 3;
    public static final int VALUE_NUMBER = 4;
    public static final int TYPE_INVALID = 5;

    public static final String VERB_FAMILY_BU_GODAN = "bu";
    public static final String VERB_FAMILY_DA = "da";
    public static final String VERB_FAMILY_GU_GODAN = "gu";
    public static final String VERB_FAMILY_KU_GODAN = "ku";
    public static final String VERB_FAMILY_IKU_SPECIAL = "iku";
    public static final String VERB_FAMILY_YUKU_SPECIAL = "yuku";
    public static final String VERB_FAMILY_KURU = "kuru";
    public static final String VERB_FAMILY_MU_GODAN = "mu";
    public static final String VERB_FAMILY_NU_GODAN = "nu";
    public static final String VERB_FAMILY_RU_GODAN = "rug";
    public static final String VERB_FAMILY_RU_ICHIDAN = "rui";
    public static final String VERB_FAMILY_ARU_SPECIAL = "aru";
    public static final String VERB_FAMILY_SU_GODAN = "su";
    public static final String VERB_FAMILY_SURU = "suru";
    public static final String VERB_FAMILY_TSU_GODAN = "tsu";
    public static final String VERB_FAMILY_U_GODAN = "u";
    public static final String VERB_FAMILY_U_SPECIAL = "us";

    public final static HashMap<String, String> VERB_FAMILIES_FULL_NAME_MAP = createVerbFamiliesMap();

    private static HashMap<String, String> createVerbFamiliesMap() {
        HashMap<String, String> map = new HashMap<>();
        map.put(VERB_FAMILY_SU_GODAN, "su godan");
        map.put(VERB_FAMILY_KU_GODAN, "ku godan");
        map.put(VERB_FAMILY_IKU_SPECIAL, "iku special class");
        map.put(VERB_FAMILY_YUKU_SPECIAL, "yuku special class");
        map.put(VERB_FAMILY_GU_GODAN, "gu godan");
        map.put(VERB_FAMILY_BU_GODAN, "bu godan");
        map.put(VERB_FAMILY_MU_GODAN, "mu godan");
        map.put(VERB_FAMILY_NU_GODAN, "nu godan");
        map.put(VERB_FAMILY_RU_GODAN, "ru godan");
        map.put(VERB_FAMILY_ARU_SPECIAL, "aru special class");
        map.put(VERB_FAMILY_TSU_GODAN, "tsu godan");
        map.put(VERB_FAMILY_U_GODAN, "u godan");
        map.put(VERB_FAMILY_U_SPECIAL, "u special class");
        map.put(VERB_FAMILY_RU_ICHIDAN, "ru ichidan");
        map.put(VERB_FAMILY_DA, "desu copula");
        map.put(VERB_FAMILY_KURU, "kuru verb");
        map.put(VERB_FAMILY_SURU, "suru verb");
        return map;
    }

    public static final int MAX_SQL_VARIABLES_FOR_QUERY = 500;

    public final static HashMap<Integer, String> COMPONENT_STRUCTURES_MAP = createStructureMap();
    private static HashMap<Integer, String> createStructureMap() {
        HashMap<Integer, String> map = new HashMap<>();
        map.put(Index_full, "full");
        map.put(Index_across2, "across2");
        map.put(Index_across3, "across3");
        map.put(Index_across4, "across4");
        map.put(Index_down2, "down2");
        map.put(Index_down3, "down3");
        map.put(Index_down4, "down4");
        map.put(Index_three_repeat, "repeat3special");
        map.put(Index_four_repeat, "repeat4special");
        map.put(Index_foursquare, "foursquare");
        map.put(Index_five_repeat, "repeat5special");
        map.put(Index_topleftout, "topleftout");
        map.put(Index_topout, "topout");
        map.put(Index_toprightout, "toprightout");
        map.put(Index_leftout, "leftout");
        map.put(Index_fullout, "fullout");
        map.put(Index_bottomleftout, "bottomleftout");
        map.put(Index_bottomout, "bottomout");
        return map;
    }
    public static String getKanjiStructureEquivalent(String componentDecompositionStructure) {
        switch (componentDecompositionStructure) {
            case "c":
            case "refh":
            case "refr":
            case "refv":
            case "rot":
            case "w":
            case "wa":
            case "wb":
            case "wbl":
            case "wtr":
            case "wtl":
            case "wbr":
                return "full";
            case "a2":
            case "a2m":
            case "a2t":
            case "rrefl":
            case "rrefr":
            case "rrotr":
                return "across2";
            case "d2":
            case "d2m":
            case "d2t":
            case "rrefd":
            case "rrotd":
            case "rrotu":
                return "down2";
            case "a3":
                return "across3";
            case "a4":
                return "across4";
            case "d3":
                return "down3";
            case "d4":
                return "down4";
            case "r3gw":
            case "r3tr":
                return "repeat3special";
            case "4sq":
                return "foursquare";
            case "r4sq":
                return "repeat4special";
            case "r5":
                return "repeat5special";
            case "s":
                return "fullout";
            case "sb":
                return "bottomout";
            case "sbl":
            case "sbr":
                return "bottomleftout";
            case "sl":
            case "sr":
                return "leftout";
            case "st":
            case "r3st":
                return "topout";
            case "stl":
            case "r3stl":
                return "topleftout";
            case "str":
            case "r3str":
                return "toprightout";
        }
        return "";
    }

	private static int ColIndexConverter(String colIndexLetter) {
	   int colIndexNumber = 0;
	   int value;
	   for (int i = 0; i < colIndexLetter.length(); i++) {
	       if (colIndexLetter.length() == 1) { value = (int)colIndexLetter.charAt(0) - (int)'a'; }
	       else { value = ((int)colIndexLetter.charAt(0) - (int)'a' + 1)*26 + (int)colIndexLetter.charAt(1) - (int)'a' + 1; }
	       colIndexNumber = value;
	   }
	   return colIndexNumber;
	}

    public static final String QUERY_HISTORY_ELEMENTS_DELIMITER = ";";
    public static final String QUERY_HISTORY_MEANINGS_DELIMITER = "@";
    public static final String QUERY_HISTORY_MEANINGS_DISPLAYED_DELIMITER = "~";
}
