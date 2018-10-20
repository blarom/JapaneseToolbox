package com.japanesetoolboxapp.resources;


import java.util.HashMap;

public final class GlobalConstants {

    // Defining the column title (and index) of each column in the excel files

	public final static int VerbModule_Conjugations_Basics   = 0;
    public final static int VerbModule_Conjugations_Stems   = 1;
	
	public final static int VerbModule_colIndex_family      = ColIndexConverter("a");
    public final static int VerbModule_colIndex_english     = ColIndexConverter("b");
    public final static int VerbModule_colIndex_trans       = ColIndexConverter("c");
    public final static int VerbModule_colIndex_prep        = ColIndexConverter("d");
    public final static int VerbModule_colIndex_kana        = ColIndexConverter("e");
    public final static int VerbModule_colIndex_kanji       = ColIndexConverter("f");
    public final static int VerbModule_colIndex_ustem       = ColIndexConverter("g");
    public final static int VerbModule_colIndex_rootKanji   = ColIndexConverter("h");
    public final static int VerbModule_colIndex_rootLatin   = ColIndexConverter("i");
    public final static int VerbModule_colIndex_exception   = ColIndexConverter("j");
    public final static int VerbModule_colIndex_altSpellings = ColIndexConverter("k");
    public final static int VerbModule_colIndex_istem       = ColIndexConverter("j");

    public final static int GrammarModule_colIndex_Id			 	= ColIndexConverter("a");
    public final static int GrammarModule_colIndex_Keyword			 	= ColIndexConverter("b");
	public final static int GrammarModule_colIndex_Romaji_construction = ColIndexConverter("c");
	public final static int GrammarModule_colIndex_Kanji_construction  = ColIndexConverter("d");
    public final static int GrammarModule_colIndex_Meaning				= ColIndexConverter("e");
    public final static int GrammarModule_colIndex_Type			    = ColIndexConverter("k");
    public final static int GrammarModule_colIndex_Categories			= ColIndexConverter("g");
    public final static int GrammarModule_colIndex_Explanation			= ColIndexConverter("h");
	public final static int GrammarModule_colIndex_Rules				= ColIndexConverter("i");
	public final static int GrammarModule_colIndex_Example1			= ColIndexConverter("j");
	public final static int GrammarModule_colIndex_Example2			= ColIndexConverter("k");
	public final static int GrammarModule_colIndex_Example3			= ColIndexConverter("l");

	public final static int GrammarModule_colIndex_Antonym             = ColIndexConverter("m"); //true index starts at "h", but examples are inserted before
    public final static int GrammarModule_colIndex_Synonym 			= ColIndexConverter("n");
    public final static int GrammarModule_colIndex_ExtraKeywords    	= ColIndexConverter("o");
    public final static int GrammarModule_colIndex_Title    	        = ColIndexConverter("p");

	public final static int Examples_colIndex_Example_English 		    = ColIndexConverter("b");
	public final static int Examples_colIndex_Example_Romaji		    = ColIndexConverter("c");
	public final static int Examples_colIndex_Example_Kanji 		    = ColIndexConverter("d");


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

    public final static HashMap<Integer, String> COMPONENT_STRUCTURES_MAP = createStructureMap();

    public static final int VALUE_LATIN = 0;
    public static final int VALUE_HIRAGANA = 1;
    public static final int VALUE_KATAKANA = 2;
    public static final int VALUE_KANJI = 3;
    public static final int VALUE_NUMBER = 4;
    public static final int VALUE_INVALID = 5;

    public static final int MAX_SQL_VARIABLES_FOR_QUERY = 500;

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

}
