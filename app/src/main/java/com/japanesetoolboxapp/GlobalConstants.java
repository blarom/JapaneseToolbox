package com.japanesetoolboxapp;


final class GlobalConstants {
    // Defining the column title (and index) of each column in the excel files

	final static int VerbModule_Conjugations_Basics   = 0;
    final static int VerbModule_Conjugations_Stems   = 1;
	
	final static int VerbModule_colIndex_family      = ColIndexConverter("a");
    final static int VerbModule_colIndex_english     = ColIndexConverter("b");
    final static int VerbModule_colIndex_trans       = ColIndexConverter("c");
    final static int VerbModule_colIndex_prep        = ColIndexConverter("d");
    final static int VerbModule_colIndex_kana        = ColIndexConverter("e");
    final static int VerbModule_colIndex_kanji       = ColIndexConverter("f");
    final static int VerbModule_colIndex_ustem       = ColIndexConverter("g");
    final static int VerbModule_colIndex_rootKanji   = ColIndexConverter("h");
    final static int VerbModule_colIndex_rootLatin   = ColIndexConverter("i");
    final static int VerbModule_colIndex_istem       = ColIndexConverter("j");

	final static int GrammarModule_colIndex_Keyword			 	= ColIndexConverter("a");
	final static int GrammarModule_colIndex_Romaji_construction = ColIndexConverter("b");
	final static int GrammarModule_colIndex_Kanji_construction  = ColIndexConverter("c");
    final static int GrammarModule_colIndex_Meaning				= ColIndexConverter("d");
    final static int GrammarModule_colIndex_Type			    = ColIndexConverter("e");
    final static int GrammarModule_colIndex_Categories			= ColIndexConverter("f");
    final static int GrammarModule_colIndex_Explanation			= ColIndexConverter("g");
	final static int GrammarModule_colIndex_Rules				= ColIndexConverter("h");
	final static int GrammarModule_colIndex_Example1			= ColIndexConverter("i");
	final static int GrammarModule_colIndex_Example2			= ColIndexConverter("j");
	final static int GrammarModule_colIndex_Example3			= ColIndexConverter("k");

	final static int GrammarModule_colIndex_Antonym             = ColIndexConverter("l"); //true index starts at "h", but examples are inserted before
    final static int GrammarModule_colIndex_Synonym 			= ColIndexConverter("m");
    final static int GrammarModule_colIndex_ExtraKeywords    	= ColIndexConverter("n");
    final static int GrammarModule_colIndex_Title    	        = ColIndexConverter("o");

	final static int Examples_colIndex_Example_English 		    = ColIndexConverter("b");
	final static int Examples_colIndex_Example_Romaji		    = ColIndexConverter("c");
	final static int Examples_colIndex_Example_Kanji 		    = ColIndexConverter("d");


    final static int Index_full                                 = 0;

    final static int Index_across2                  		    = 1;
    final static int Index_across3                  		    = 2;
    final static int Index_across4                  		    = 3;

    final static int Index_down2                  		        = 4;
    final static int Index_down3                  		        = 5;
    final static int Index_down4                  		        = 6;

    final static int Index_three_repeat                  	    = 7;
    final static int Index_foursquare                  		    = 8;
    final static int Index_four_repeat           		        = 9;
    final static int Index_five_repeat                  		= 10;

    final static int Index_topleftout                  		    = 11;
    final static int Index_topout                  		        = 12;
    final static int Index_toprightout                  		= 13;

    final static int Index_leftout                  		    = 14;
    final static int Index_fullout                  		    = 15;
    
    final static int Index_bottomleftout                  		= 16;
    final static int Index_bottomout                  		    = 17;

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
