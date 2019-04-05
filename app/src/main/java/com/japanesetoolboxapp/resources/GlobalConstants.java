package com.japanesetoolboxapp.resources;


import com.japanesetoolboxapp.R;

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

    public final static int COLUMN_EXAMPLES_ROMAJI              = ColIndexConverter("b");
    public final static int COLUMN_EXAMPLES_KANJI               = ColIndexConverter("c");
    public final static int COLUMN_EXAMPLES_ENGLISH             = ColIndexConverter("d");
    public final static int COLUMN_EXAMPLES_FRENCH              = ColIndexConverter("e");
    public final static int COLUMN_EXAMPLES_SPANISH             = ColIndexConverter("f");

    public final static int COLUMN_VERB_ISTEM                   = ColIndexConverter("j");
    public final static int COLUMN_VERB_MASUSTEM                = ColIndexConverter("p");


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
    public static final int BALANCE_POINT_REGULAR_DISPLAY = 4;
    public static final int BALANCE_POINT_HISTORY_DISPLAY = 2;

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

    public static final int LANG_EN = 0;
    public static final int LANG_FR = 1;
    public static final int LANG_ES = 2;

    public final static HashMap<String, String> LANGUAGE_CODE_MAP = createLanguageMap();
    private static HashMap<String, String> createLanguageMap() {
        HashMap<String, String> map = new HashMap<>();
        map.put("english", "en");
        map.put("french", "fr");
        map.put("spanish", "es");
        return map;
    }

    public final static HashMap<String, Integer> VERB_CONJUGATION_TITLES = createVerbConjugationsMap();
    private static HashMap<String, Integer> createVerbConjugationsMap() {
        HashMap<String, Integer> map = new HashMap<>();

        map.put("", R.string.EmptyResult);
        map.put("Basics", R.string.verb_TitleBasics);
        map.put("Basics1", R.string.verb_Basics1);
        map.put("Basics2", R.string.verb_Basics2);
        map.put("Basics3", R.string.verb_Basics3);
        map.put("Basics4", R.string.verb_Basics4);
        map.put("Basics5", R.string.verb_Basics5);
        map.put("Basics6", R.string.verb_Basics6);
        map.put("Basics7", R.string.verb_Basics7);
        map.put("Basics8", R.string.verb_Basics8);
        map.put("Basics9", R.string.verb_Basics9);
        map.put("Basics10", R.string.verb_Basics10);
        map.put("Basics11", R.string.verb_Basics11);
        map.put("Basics12", R.string.verb_Basics12);
        map.put("Basics13", R.string.verb_Basics13);
        map.put("Basics14", R.string.verb_Basics14);
        map.put("Basics15", R.string.verb_Basics15);
        map.put("Basics16", R.string.verb_Basics16);

        map.put("TitleSimpleForm", R.string.verb_TitleSimpleForm);
        map.put("TitleProgressive", R.string.verb_TitleProgressive);
        map.put("TitlePoliteness", R.string.verb_TitlePoliteness);
        map.put("TitleRequest", R.string.verb_TitleRequest);
        map.put("TitleImperative", R.string.verb_TitleImperative);
        map.put("TitleDesire", R.string.verb_TitleDesire);
        map.put("TitleProvisional", R.string.verb_TitleProvisional);
        map.put("TitleVolitional", R.string.verb_TitleVolitional);
        map.put("TitleObligation", R.string.verb_TitleObligation);
        map.put("TitlePresumptive", R.string.verb_TitlePresumptive);
        map.put("TitleAlternative", R.string.verb_TitleAlternative);
        map.put("TitleCausativeA", R.string.verb_TitleCausativeA);
        map.put("TitleCausativePv", R.string.verb_TitleCausativePv);
        map.put("TitlePassive", R.string.verb_TitlePassive);
        map.put("TitlePotential", R.string.verb_TitlePotential);
        map.put("TitleContinuative", R.string.verb_TitleContinuative);
        map.put("TitleCompulsion", R.string.verb_TitleCompulsion);
        map.put("TitleGerund", R.string.verb_TitleGerund);

        map.put("PPr", R.string.verb_PPr);
        map.put("PPs", R.string.verb_PPs);
        map.put("PPrN", R.string.verb_PPrN);
        map.put("PPsN", R.string.verb_PPsN);
        map.put("PlPr", R.string.verb_PlPr);
        map.put("PlPs", R.string.verb_PlPs);
        map.put("PlPrN", R.string.verb_PlPrN);
        map.put("PlPsN", R.string.verb_PlPsN);
        map.put("PPrA", R.string.verb_PPrA);
        map.put("ClPr", R.string.verb_ClPr);
        map.put("ClPrA", R.string.verb_ClPrA);
        map.put("ClPrN", R.string.verb_ClPrN);
        map.put("LPl", R.string.verb_LPl);
        map.put("Hn1", R.string.verb_Hn1);
        map.put("Hn2", R.string.verb_Hn2);
        map.put("Hm1", R.string.verb_Hm1);
        map.put("Hm2", R.string.verb_Hm2);
        map.put("Pl1", R.string.verb_Pl1);
        map.put("Pl2", R.string.verb_Pl2);
        map.put("PlN", R.string.verb_PlN);
        map.put("Hn", R.string.verb_Hn);
        map.put("HnN", R.string.verb_HnN);
        map.put("PPrV", R.string.verb_PPrV);
        map.put("PPrL", R.string.verb_PPrL);
        map.put("Pr3rdPO", R.string.verb_Pr3rdPO);
        map.put("Pba", R.string.verb_Pba);
        map.put("ClN", R.string.verb_ClN);
        map.put("PNba", R.string.verb_PNba);
        map.put("Plba", R.string.verb_Plba);
        map.put("PlNba", R.string.verb_PlNba);
        map.put("Ptara", R.string.verb_Ptara);
        map.put("PNtara", R.string.verb_PNtara);
        map.put("Pltara", R.string.verb_Pltara);
        map.put("PlNtara", R.string.verb_PlNtara);
        map.put("IIWTS", R.string.verb_IIWTS);
        map.put("P", R.string.verb_P);
        map.put("Ps", R.string.verb_Ps);
        map.put("PN", R.string.verb_PN);
        map.put("PPg", R.string.verb_PPg);
        map.put("Pl", R.string.verb_Pl);
        map.put("PPr1", R.string.verb_PlPr1);
        map.put("PPr2", R.string.verb_PlPr2);
        map.put("PPrN1", R.string.verb_PlPrN1);
        map.put("PPrN2", R.string.verb_PlPrN2);
        map.put("PPrN3", R.string.verb_PlPrN3);
        map.put("PlPr1", R.string.verb_PlPr1);
        map.put("PlPr2", R.string.verb_PlPr2);
        map.put("PlPrN1", R.string.verb_PlPrN1);
        map.put("PlPrN2", R.string.verb_PlPrN2);
        map.put("PlPrN3", R.string.verb_PlPrN3);
        map.put("A", R.string.verb_A);
        map.put("N", R.string.verb_N);
        map.put("APg", R.string.verb_APg);
        map.put("NPg", R.string.verb_NPg);
        map.put("PvPg", R.string.verb_PvPg);
        map.put("PsPg", R.string.verb_PsPg);
        map.put("teform", R.string.verb_teform);

        map.put("TitleteformCmp", R.string.verb_TitleteformCmp);
        map.put("teformCmp1", R.string.verb_teformCmp1);
        map.put("teformCmp2", R.string.verb_teformCmp2);
        map.put("teformCmp3", R.string.verb_teformCmp3);
        map.put("teformCmp4", R.string.verb_teformCmp4);
        map.put("teformCmp5", R.string.verb_teformCmp5);
        map.put("teformCmp6", R.string.verb_teformCmp6);
        map.put("teformCmp7", R.string.verb_teformCmp7);
        map.put("teformCmp8", R.string.verb_teformCmp8);
        map.put("teformCmp9", R.string.verb_teformCmp9);
        map.put("teformCmp10", R.string.verb_teformCmp10);
        map.put("teformCmp11", R.string.verb_teformCmp11);
        map.put("teformCmp12", R.string.verb_teformCmp12);
        map.put("teformCmp13", R.string.verb_teformCmp13);
        map.put("teformCmp14", R.string.verb_teformCmp14);

        map.put("TitleteformConj", R.string.verb_TitleteformConj);
        map.put("teformConj1", R.string.verb_teformConj1);
        map.put("teformConj2", R.string.verb_teformConj2);
        map.put("teformConj3", R.string.verb_teformConj3);
        map.put("teformConj4", R.string.verb_teformConj4);

        map.put("TitleSFConj", R.string.verb_TitleSFConj);
        map.put("SFConj1", R.string.verb_SFConj1);
        map.put("SFConj2", R.string.verb_SFConj2);
        map.put("SFConj3", R.string.verb_SFConj3);
        map.put("SFConj4", R.string.verb_SFConj4);

        map.put("TitlemasuCmp", R.string.verb_TitlemasuCmp);
        map.put("masuCmp1", R.string.verb_masuCmp1);
        map.put("masuCmp2", R.string.verb_masuCmp2);
        map.put("masuCmp3", R.string.verb_masuCmp3);
        map.put("masuCmp4", R.string.verb_masuCmp4);
        map.put("masuCmp5", R.string.verb_masuCmp5);
        map.put("masuCmp6", R.string.verb_masuCmp6);
        map.put("masuCmp7", R.string.verb_masuCmp7);
        map.put("masuCmp8", R.string.verb_masuCmp8);
        map.put("masuCmp9", R.string.verb_masuCmp9);
        map.put("masuCmp10", R.string.verb_masuCmp10);
        map.put("masuCmp11", R.string.verb_masuCmp11);
        map.put("masuCmp12", R.string.verb_masuCmp12);

        map.put("TitleStemMisc", R.string.verb_TitleStemMisc);
        map.put("StemMisc1", R.string.verb_StemMisc1);
        map.put("StemMisc2", R.string.verb_StemMisc2);
        map.put("StemMisc3", R.string.verb_StemMisc3);
        map.put("StemMisc4", R.string.verb_StemMisc4);
        map.put("StemMisc5", R.string.verb_StemMisc5);
        map.put("StemMisc6", R.string.verb_StemMisc6);
        map.put("StemMisc7", R.string.verb_StemMisc7);
        map.put("StemMisc8", R.string.verb_StemMisc8);
        map.put("StemMisc9", R.string.verb_StemMisc9);

        map.put("TitleArch", R.string.verb_TitleArch);
        map.put("Arch1", R.string.verb_Arch1);
        map.put("Arch2", R.string.verb_Arch2);

        return map;
    }
}
