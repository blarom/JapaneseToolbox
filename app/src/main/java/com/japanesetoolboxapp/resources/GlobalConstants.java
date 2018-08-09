package com.japanesetoolboxapp.resources;


public final class GlobalConstants {

    // Defining the column title (and index) of each column in the excel files
    public static final long TOTAL_REQUIRED_MEMORY_HEAP_SIZE = 45;
    public static final long DECOMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE = 23;
    public static final long CHAR_COMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE = 6;

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


    public final static int Index_full                                 = 0;

    public final static int Index_across2                  		    = 1;
    public final static int Index_across3                  		    = 2;
    public final static int Index_across4                  		    = 3;

    public final static int Index_down2                  		        = 4;
    public final static int Index_down3                  		        = 5;
    public final static int Index_down4                  		        = 6;

    public final static int Index_three_repeat                  	    = 7;
    public final static int Index_four_repeat           		        = 8;
    public final static int Index_foursquare                  		    = 9;
    public final static int Index_five_repeat                  		= 10;

    public final static int Index_topleftout                  		    = 11;
    public final static int Index_topout                  		        = 12;
    public final static int Index_toprightout                  		= 13;

    public final static int Index_leftout                  		    = 14;
    public final static int Index_fullout                  		    = 15;
    
    public final static int Index_bottomleftout                  		= 16;
    public final static int Index_bottomout                  		    = 17;

	static int ColIndexConverter(String colIndexLetter) {
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
