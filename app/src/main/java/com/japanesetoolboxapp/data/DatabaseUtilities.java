package com.japanesetoolboxapp.data;


import android.content.Context;
import android.util.Log;

import com.japanesetoolboxapp.BuildConfig;
import com.japanesetoolboxapp.ui.ConvertFragment;
import com.japanesetoolboxapp.resources.GlobalConstants;
import com.japanesetoolboxapp.resources.Utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DatabaseUtilities {

    public static final String firebaseEmail = BuildConfig.firebaseEmail;
    public static final String firebasePass = BuildConfig.firebasePass;
    public static final int WORD_SEARCH_CHAR_COUNT_THRESHOLD = 3;
    public static final int NUM_COLUMNS_IN_WORDS_CSV_SHEETS = 7;
    public static final int NUM_COLUMNS_IN_VERBS_CSV_SHEET = 11;

    public static void checkDatabaseStructure(List<String[]> databaseFromCsv, String databaseName, int numColumns) {
        for (String[] line : databaseFromCsv) {
            if (line.length < numColumns) {
                Log.v("JapaneseToolbox","Serious error in row [" + line[0] + "] in " + databaseName + ": CSV file row has less columns than expected! Check for accidental line breaks.");
                break;
            }
        }
    }
    public static Word createWordFromCsvDatabases(List<String[]> centralDatabase,
                                                  List<String[]> meaningsDatabase,
                                                  List<String[]> multExplanationsDatabase,
                                                  List<String[]> examplesDatabase,
                                                  int centralDbRowIndex) {

        // Value Initializations
        int example_index;
        Word word = new Word();
        List<String> parsed_example_list;

        //Getting the index value
        int matchingWordId = Integer.parseInt(centralDatabase.get(centralDbRowIndex)[0]);
        word.setWordId(matchingWordId);

        //Getting the keywords value
        String matchingWordKeywordsList = centralDatabase.get(centralDbRowIndex)[1];
        word.setKeywords(matchingWordKeywordsList);

        //Getting the Romaji value
        String matchingWordRomaji = centralDatabase.get(centralDbRowIndex)[2];
        word.setRomaji(matchingWordRomaji);

        //Getting the Kanji value
        String matchingWordKanji = centralDatabase.get(centralDbRowIndex)[3];
        word.setKanji(matchingWordKanji);

        //Setting the unique identifier
        word.setUniqueIdentifier(matchingWordRomaji+"-"+matchingWordKanji);

        //Setting the Common Word flag
        word.setCommonStatus(2);

        //Getting the AltSpellings value
        String matchingWordAltSpellings = centralDatabase.get(centralDbRowIndex)[5];
        word.setAltSpellings(matchingWordAltSpellings);


        //regionGetting the set of Meanings

        //Initializations
        String matchingWordMeaning;
        String matchingWordType;
        String matchingWordOpposite;
        String matchingWordSynonym;
        String matchingWordExplanation;
        String matchingWordRules;
        String matchingWordExampleList;
        String[] current_meaning_characteristics;
        Boolean has_multiple_explanations;
        String ME_index;

        //Finding the meanings using the supplied index
        String MM_index = centralDatabase.get(centralDbRowIndex)[4];
        List<String> MM_index_list = Arrays.asList(MM_index.split(";"));
        if (MM_index_list.size() == 0) { return word; }

        List<Word.Meaning> meaningsList = new ArrayList<>();
        int current_MM_index;
        for (int i=0; i< MM_index_list.size(); i++) {

            Word.Meaning meaning = new Word.Meaning();
            current_MM_index = Integer.parseInt(MM_index_list.get(i))-1;
            current_meaning_characteristics = meaningsDatabase.get(current_MM_index);

            //Getting the Meaning value
            matchingWordMeaning = current_meaning_characteristics[1];

            //Getting the Type value
            matchingWordType = current_meaning_characteristics[2];

            //Make corrections to the meaning values if the hit is a verb
            if (matchingWordType.contains("V") && !matchingWordType.equals("VC")) {
                List<String> parsed_meaning = Arrays.asList(matchingWordMeaning.split(","));
                String fixed_meaning = "";
                boolean valueIsInParentheses = false;
                for (int k = 0; k < parsed_meaning.size(); k++) {
                    if (valueIsInParentheses) {
                        fixed_meaning += parsed_meaning.get(k).trim();
                    }
                    else {
                        fixed_meaning += "to " + parsed_meaning.get(k).trim();
                    }

                    if (k < parsed_meaning.size() - 1) {
                        fixed_meaning += ", ";
                    }

                    if (parsed_meaning.get(k).contains("(") && !parsed_meaning.get(k).contains(")")) {
                        valueIsInParentheses = true;
                    }
                    else if (!parsed_meaning.get(k).contains("(") && parsed_meaning.get(k).contains(")")) {
                        valueIsInParentheses = false;
                    }
                }
                matchingWordMeaning = fixed_meaning;
            }

            //Setting the Meaning and Type values in the returned list
            meaning.setMeaning(matchingWordMeaning);
            meaning.setType(matchingWordType);

            //Getting the Opposite value
            matchingWordOpposite = current_meaning_characteristics[6];
            meaning.setAntonym(matchingWordOpposite);

            //Getting the Synonym value
            matchingWordSynonym = current_meaning_characteristics[7];
            meaning.setSynonym(matchingWordSynonym);

            //Getting the set of Explanations
            has_multiple_explanations = false;
            ME_index = "";
            if (current_meaning_characteristics[3].length() > 3) {
                if (current_meaning_characteristics[3].substring(0,3).equals("ME#")) {
                    has_multiple_explanations = true;
                    ME_index = current_meaning_characteristics[3].substring(3,current_meaning_characteristics[3].length());
                }
            }

            List<Word.Meaning.Explanation> explanationList = new ArrayList<>();
            if (has_multiple_explanations) {
                List<String> ME_index_list = Arrays.asList(ME_index.split(";"));
                int current_ME_index;
                for (int j=0; j<ME_index_list.size(); j++) {

                    Word.Meaning.Explanation explanation = new Word.Meaning.Explanation();

                    current_ME_index = Integer.parseInt(ME_index_list.get(j))-1;

                    //Getting the Explanation value
                    matchingWordExplanation = multExplanationsDatabase.get(current_ME_index)[1];
                    explanation.setExplanation(matchingWordExplanation);

                    //Getting the Rules value
                    matchingWordRules = multExplanationsDatabase.get(current_ME_index)[2];
                    explanation.setRules(matchingWordRules);

                    //Getting the Examples
                    matchingWordExampleList = multExplanationsDatabase.get(current_ME_index)[3];
                    List<Word.Meaning.Explanation.Example> exampleList = new ArrayList<>();
                    if (!matchingWordExampleList.equals("") && !matchingWordExampleList.contains("Example")) {
                        parsed_example_list = Arrays.asList(matchingWordExampleList.split(", "));
                        for (int t = 0; t < parsed_example_list.size(); t++) {
                            Word.Meaning.Explanation.Example example = new Word.Meaning.Explanation.Example();
                            example_index = Integer.parseInt(parsed_example_list.get(t)) - 1;
                            example.setEnglishSentence(examplesDatabase.get(example_index)[GlobalConstants.Examples_colIndex_Example_English]);
                            example.setRomajiSentence(examplesDatabase.get(example_index)[GlobalConstants.Examples_colIndex_Example_Romaji]);
                            example.setKanjiSentence(examplesDatabase.get(example_index)[GlobalConstants.Examples_colIndex_Example_Kanji]);
                            exampleList.add(example);
                        }
                    }
                    explanation.setExamples(exampleList);
                    explanationList.add(explanation);
                }
            }
            else {
                Word.Meaning.Explanation explanation = new Word.Meaning.Explanation();

                //Getting the Explanation value
                matchingWordExplanation = meaningsDatabase.get(current_MM_index)[3];
                explanation.setExplanation(matchingWordExplanation);

                //Getting the Rules value
                matchingWordRules = meaningsDatabase.get(current_MM_index)[4];
                explanation.setRules(matchingWordRules);

                //Getting the Examples
                matchingWordExampleList = meaningsDatabase.get(current_MM_index)[5];
                List<Word.Meaning.Explanation.Example> exampleList = new ArrayList<>();
                if (!matchingWordExampleList.equals("") && !matchingWordExampleList.contains("Example")) {
                    parsed_example_list = Arrays.asList(matchingWordExampleList.split(", "));
                    for (int t = 0; t < parsed_example_list.size(); t++) {
                        Word.Meaning.Explanation.Example example = new Word.Meaning.Explanation.Example();
                        example_index = Integer.parseInt(parsed_example_list.get(t)) - 1;
                        example.setEnglishSentence(examplesDatabase.get(example_index)[GlobalConstants.Examples_colIndex_Example_English]);
                        example.setRomajiSentence(examplesDatabase.get(example_index)[GlobalConstants.Examples_colIndex_Example_Romaji]);
                        example.setKanjiSentence(examplesDatabase.get(example_index)[GlobalConstants.Examples_colIndex_Example_Kanji]);
                        exampleList.add(example);
                    }
                }
                explanation.setExamples(exampleList);
                explanationList.add(explanation);
            }
            meaning.setExplanations(explanationList);
            meaningsList.add(meaning);

        }
        //endregion

        word.setMeanings(meaningsList);

        return word;
    }
    public static Verb createVerbFromCsvDatabase(List<String[]> verbDatabase, List<String[]> meaningsDatabase, int verbDbRowIndex) {

        // Value Initializations
        Verb verb = new Verb();
        String meanings;
        String type;
        String[] currentMeaningCharacteristics;

        verb.setVerbId(Integer.parseInt(verbDatabase.get(verbDbRowIndex)[0]));
        verb.setPreposition(verbDatabase.get(verbDbRowIndex)[7]);
        verb.setKanji(verbDatabase.get(verbDbRowIndex)[3]);
        verb.setRomaji(verbDatabase.get(verbDbRowIndex)[2]);
        verb.setKanjiRoot(verbDatabase.get(verbDbRowIndex)[8]);
        verb.setLatinRoot(verbDatabase.get(verbDbRowIndex)[9]);
        verb.setExceptionIndex(verbDatabase.get(verbDbRowIndex)[10]);
        verb.setAltSpellings(verbDatabase.get(verbDbRowIndex)[5]);
        verb.setKana(ConvertFragment.getLatinHiraganaKatakana(verb.getRomaji()).get(1));

        //Getting the meanings

        String MM_index = verbDatabase.get(verbDbRowIndex)[4];
        List<String> MM_index_list = Arrays.asList(MM_index.split(";"));
        if (MM_index_list.size() == 0) { return verb; }

        int current_MM_index;
        StringBuilder stringBuilder = new StringBuilder("");
        for (int i=0; i< MM_index_list.size(); i++) {

            current_MM_index = Integer.parseInt(MM_index_list.get(i)) - 1;
            currentMeaningCharacteristics = meaningsDatabase.get(current_MM_index);

            //Getting the Meaning value
            meanings = currentMeaningCharacteristics[1];
            if (!verb.getFamily().equals("da")) 
            if (i != 0) stringBuilder.append(", ");
            meanings = meanings.replace(", ", ", to ");
            stringBuilder.append(meanings);

            //Getting the Type value
            type = currentMeaningCharacteristics[2];
            if (i == 0 && !type.equals("")) {
                verb.setTrans(String.valueOf(type.charAt(type.length() - 1)));
                if (!type.substring(0,1).equals("V")) {
                    Log.i(Utilities.DEBUG_TAG, "Warning! Found verb with incorrect type (Meaning index:" + Integer.toString(current_MM_index) +")");
                }
                verb.setFamily(type.substring(1, type.length() - 1));
            }
        }

        verb.setMeaning(stringBuilder.toString());

        return verb;
    }
    public static List<String[]> readCSVFile(String filename, Context context) {

        List<String[]> mySheet = new ArrayList<>();

        // OpenCSV implementation
        //                String next[] = null;
        //                CSVReader reader = null;
        //                try {
        //                    reader = new CSVReader(new InputStreamReader(GlobalTranslatorActivity.getAssets().open(filename)));
        //                } catch (IOException e) {
        //                    e.printStackTrace();
        //                }
        //                if (reader != null) {
        //                    for (; ; ) {
        //                        try {
        //                            next = reader.readNext();
        //                        } catch (IOException e) {
        //                            e.printStackTrace();
        //                        }
        //                        if (next != null) {
        //                            mySheet.add(next);
        //                        } else {
        //                            break;
        //                        }
        //                    }
        //                }
        //                try {
        //                    reader.close();
        //                } catch (IOException e) {
        //                    e.printStackTrace();
        //                }

        // "|" Parser implementation

        BufferedReader fileReader = null;

        int line_number = 0;
        try {
            String line;
            fileReader = new BufferedReader(new InputStreamReader(context.getAssets().open(filename)));

            while ((line = fileReader.readLine()) != null) {
                String[] tokens = line.split("\\|",-1);
                if (tokens.length > 0) {
                    mySheet.add(tokens);
                    line_number++;
                }
            }
        } catch (Exception e) {
            System.out.println("Error in CsvFileReader !!!");
            e.printStackTrace();
            Log.i("Diagnosis Time","Error in CsvFileReader opening Loaded DecompositionDatabase_PART4. Line number:"+line_number);
        } finally {
            try {
                if (fileReader != null) {fileReader.close();}
            } catch (IOException e) {
                System.out.println("Error while closing fileReader !!!");
                e.printStackTrace();
                Log.i("Diagnosis Time","Error in CsvFileReader closing Loaded DecompositionDatabase_PART4.");
            }
        }

        return mySheet;
    }
    public static List<String[]> readCSVFileFirstRow(String filename, Context context) {

        List<String[]> mySheetFirstRow = new ArrayList<>();

        //OpenCSV implementation
        //				  String firstrow[] = null;
        //                String next[] = null;
        //                CSVReader reader = null;
        //
        //                try {
        //                    reader = new CSVReader(new InputStreamReader(GlobalTranslatorActivity.getAssets().open(filename)));
        //                } catch (IOException e) {
        //                    e.printStackTrace();
        //                }
        //
        //                if (reader != null) {
        //                    try {
        //                        firstrow = reader.readNext();
        //                    } catch (IOException e) {
        //                        e.printStackTrace();
        //                    }
        //                    if (firstrow != null) {
        //                        mySheetFirstRow.add(firstrow);
        //                    }
        //                }
        //
        //                try {
        //                    reader.close();
        //                } catch (IOException e) {
        //                    e.printStackTrace();
        //                }

        // "|" Parser implementation

        BufferedReader fileReader = null;

        try {
            String line;
            fileReader = new BufferedReader(new InputStreamReader(context.getAssets().open(filename)));

            line = fileReader.readLine();
            String[] tokens = line.split("\\|",-1);
            if (tokens.length > 0) {
                mySheetFirstRow.add(tokens);
            }
        } catch (Exception e) {
            System.out.println("Error in CsvFileReader !!!");
            e.printStackTrace();
        } finally {
            try {
                if (fileReader != null) {fileReader.close();}
            } catch (IOException e) {
                System.out.println("Error while closing fileReader !!!");
                e.printStackTrace();
            }
        }

        return mySheetFirstRow;
    }
    public static List<Long> getMatchingWordIdsUsingRoomIndexes(String searchWord, JapaneseToolboxRoomDatabase japaneseToolboxRoomDatabase) {


        //region Initializations
        List<Long> matchingWordIds = new ArrayList<>();
        List<Long> MatchingWordIdsFromIndex = new ArrayList<>();
        List<String> keywordsList;
        List<long[]> MatchList = new ArrayList<>();
        String list;
        String hit;
        String hitFirstRelevantWord;
        String concatenated_hit;
        String best_match;
        String type;
        long[] current_match_values;

        boolean found_match;
        boolean is_verb;
        boolean is_verb_and_latin;
        int word_length = searchWord.length();
        int match_length;
        //endregion

        // converting the word to lowercase (the algorithm is not efficient if needing to search both lower and upper case)
        searchWord = searchWord.toLowerCase(Locale.ENGLISH);

        //region If there is an "inging" verb instance, reduce it to an "ing" instance (e.g. singing >> sing)
        int verb_length = searchWord.length();
        String verb = searchWord;
        String verb2;
        String inglessVerb = verb;
        if (verb_length > 2 && verb.substring(verb_length-3).equals("ing")) {

            if (verb_length > 5 && verb.substring(verb_length-6).equals("inging")) {
                if (	(verb.substring(0, 2+1).equals("to ") && checkIfWordIsOfTypeIngIng(verb.substring(3,verb_length))) ||
                        (!verb.substring(0, 2+1).equals("to ") && checkIfWordIsOfTypeIngIng(verb.substring(0,verb_length)))   ) {
                    // If the verb ends with "inging" then remove the the second "ing"
                    inglessVerb = verb.substring(0,verb_length-3);
                }
            }
            else {
                verb2 = verb + "ing";
                if ((!verb2.substring(0, 2 + 1).equals("to ") || !checkIfWordIsOfTypeIngIng(verb2.substring(3, verb_length + 3))) &&
                        (verb2.substring(0, 2+1).equals("to ") || !checkIfWordIsOfTypeIngIng(verb2.substring(0, verb_length + 3)))) {
                    // If the verb does not belong to the list, then remove the ending "ing" so that it can be compared later on to the verbs excel
                    //If the verb is for e.g. to sing / sing, where verb2 = to singing / singing, then check that verb2 (without the "to ") belongs to the list, and if it does then do nothing

                    inglessVerb = verb.substring(0,verb_length-3);
                }
            }
        }
        int inglessVerb_length = inglessVerb.length();
        //endregion

        //region getting the input type and its converted form (english/romaji/kanji/invalid)
        List<String> translationList = ConvertFragment.getLatinHiraganaKatakana(searchWord);

        String translationLatin = translationList.get(0);
        String translationHira = translationList.get(1);
        String translationKata = translationList.get(2);
        String text_type = ConvertFragment.getTextType(searchWord);

        boolean TypeisLatin   = false;
        boolean TypeisKana    = false;
        boolean TypeisKanji   = false;
        boolean TypeisNumber  = false;
        boolean TypeisInvalid = false;

        if (text_type.equals("latin") )                                     { TypeisLatin = true;}
        if (text_type.equals("hiragana") || text_type.equals("katakana") )  { TypeisKana = true;}
        if (text_type.equals("kanji") )                                     { TypeisKanji = true;}
        if (text_type.equals("number") )                                    { TypeisNumber = true;}
        if (searchWord.contains("*") || searchWord.contains("＊") || searchWord.equals("") || searchWord.equals("-") ) { TypeisInvalid = true;}

        if (TypeisInvalid) return matchingWordIds;
        //endregion

        //region Concatenating the input word to increase the match chances
        String concatenated_word = Utilities.removeSpecialCharacters(searchWord);
        String concatenated_translationLatin = Utilities.removeSpecialCharacters(translationLatin);
        String concatenated_translationHira = Utilities.removeSpecialCharacters(translationHira);
        String concatenated_translationKata = Utilities.removeSpecialCharacters(translationKata);
        int concatenated_word_length = concatenated_word.length();
        //endregion

        //region Removing any apostrophes to make user searches less strict
        searchWord = removeApostrophe(searchWord);
        concatenated_word = removeApostrophe(concatenated_word);
        concatenated_translationLatin = removeApostrophe(concatenated_translationLatin);
        //endregion

        //region Search for the matches in the indexed list
        List<String> searchResultKeywordsArray = new ArrayList<>();
        List<LatinIndex> latinIndices;
        List<KanjiIndex> kanjiIndices;
        if (TypeisLatin || TypeisKana || TypeisNumber) {

            //If the input is a verb in "to " form, remove the "to " for the search only (results will be filtered later on)
            String input_word = concatenated_word;
            if (searchWord.length()>3) {
                if (searchWord.substring(0, 3).equals("to ")) {
                    input_word = concatenated_word.substring(2, concatenated_word.length());
                }
            }

            latinIndices = findQueryInLatinIndex(TypeisLatin, input_word, concatenated_translationLatin, japaneseToolboxRoomDatabase);

            if (latinIndices.size()==0) return matchingWordIds;

            // If the entered word is Latin and only has up to WORD_SEARCH_CHAR_COUNT_THRESHOLD characters, limit the word list to be checked later
            if (TypeisLatin && concatenated_word.length() < WORD_SEARCH_CHAR_COUNT_THRESHOLD
                    || TypeisKana && concatenated_translationLatin.length() < WORD_SEARCH_CHAR_COUNT_THRESHOLD
                    || TypeisNumber && concatenated_word.length() < WORD_SEARCH_CHAR_COUNT_THRESHOLD-1) {
                for (LatinIndex latinIndex : latinIndices) {
                    if (latinIndex.getLatin().length() < WORD_SEARCH_CHAR_COUNT_THRESHOLD) {
                        searchResultKeywordsArray.add(latinIndex.getWordIds());
                        break;
                    }
                }
            }
            else {
                for (LatinIndex latinIndex : latinIndices) {
                    searchResultKeywordsArray.add(latinIndex.getWordIds());
                }
            }

        } else if (TypeisKanji) {
            kanjiIndices = findQueryInKanjiIndex(concatenated_word, japaneseToolboxRoomDatabase);
            if (kanjiIndices.size()==0) return matchingWordIds;
            for (KanjiIndex kanjiIndex : kanjiIndices) {
                searchResultKeywordsArray.add(kanjiIndex.getWordIds());
            }
        } else {
            return matchingWordIds;
        }
        //endregion

        //region Get the indexes of all of the results that were found
        for (String searchResultKeywords : searchResultKeywordsArray) {
            keywordsList = Arrays.asList(searchResultKeywords.split(";"));
            for (int j = 0; j < keywordsList.size(); j++) {
                MatchingWordIdsFromIndex.add(Long.valueOf(keywordsList.get(j)));
            }
        }
        //endregion

        //region Add search results where the "ing" is removed from an "ing" verb
        if ((TypeisLatin || TypeisKana || TypeisNumber) && !inglessVerb.equals(searchWord)) {

            latinIndices = findQueryInLatinIndex(TypeisLatin, inglessVerb, concatenated_translationLatin, japaneseToolboxRoomDatabase);

            for (LatinIndex latinIndex : latinIndices) {
                keywordsList = Arrays.asList(latinIndex.getWordIds().split(";"));
                for (int j = 0; j < keywordsList.size(); j++) {
                    MatchingWordIdsFromIndex.add(Long.valueOf(keywordsList.get(j)));
                }
            }
        }
        //endregion

        //region Limiting the database query if there are too many results (prevents long query times)
        boolean onlyRetrieveShortRomajiWords = false;
        if (TypeisLatin && searchWord.length() < 4 || TypeisKana && searchWord.length() < 3) {
            onlyRetrieveShortRomajiWords = true;
        }
        //endregion

        //region Filtering the matches
        List<Word> matchingWordList = japaneseToolboxRoomDatabase.getWordListByWordIds(MatchingWordIdsFromIndex);
        for (Word word : matchingWordList) {

            list = word.getKeywords();
            if (list.equals("") || list.equals("-") || list.equals("KEYWORDS")) continue;
            if (onlyRetrieveShortRomajiWords && word.getRomaji().length() > 4) continue;

            found_match = list.contains(searchWord);


//            //region Loop initializations
//            list = word.getKeywords();
//            if (list.equals("") || list.equals("-") || list.equals("KEYWORDS")) continue;
//            if (onlyRetrieveShortRomajiWords && word.getRomaji().length() > 4) continue;
//            keywordsList = Arrays.asList(list.split(","));
//            found_match = false;
//            //endregion
//
//            //region Checking if is the word is a verb
//            is_verb = false;
//            for (Word.Meaning meaning : word.getMeanings()) {
//                type = meaning.getType();
//                is_verb = type.substring(0, 1).equals("V") && !type.equals("VC");
//                if (is_verb) break;
//            }
//            //endregion
//
//            //region If there is a word in the list that matches the input word, get the corresponding row index
//            match_length = 1000;
//            boolean valueIsInParentheses = false;
//            for (int i = 0; i < keywordsList.size(); i++) {
//
//                // Performing certain actions on the hit to prepare the comparison
//                hit = keywordsList.get(i).trim(); //also trims the extra space before the word
//
//                //region Add "to " to the hit if it's a verb (the "to " was removed to save memory in the database)
//                if (is_verb) {
//                    //Don't add "to " if the word is an explanation in parentheses
//                    if (!valueIsInParentheses) {
//                        hit = "to " + hit;
//                    }
//                    if (hit.contains("(") && !hit.contains(")")) valueIsInParentheses = true;
//                    else if (!hit.contains("(") && hit.contains(")")) valueIsInParentheses = false;
//                }
//                //endregion
//
//                is_verb_and_latin = hit.length() > 3 && hit.substring(0, 3).equals("to ");
//
//                concatenated_hit = Utilities.removeSpecialCharacters(hit);
//                if (TypeisKanji && !ConvertFragment.getTextType(concatenated_hit).equals("kanji") ) { continue; }
//                if (concatenated_hit.length() < concatenated_word_length) { continue; }
//                if (TypeisLatin && word_length == 2 && hit.length() > 2) { continue;}
//                if (TypeisLatin && hit.length() < inglessVerb_length) {continue;}
//
//                if (TypeisLatin) {
//                    hit = hit.toLowerCase(Locale.ENGLISH);
//                    concatenated_hit = concatenated_hit.toLowerCase(Locale.ENGLISH);
//                }
//
//                hit = removeApostrophe(hit);
//                concatenated_hit = removeApostrophe(concatenated_hit);
//
//                //region Getting the first word if the hit is a sentence
//                if (hit.length() > word_length) {
//                    List<String> parsed_hit = Arrays.asList(hit.split(" "));
//                    if (is_verb_and_latin) { hitFirstRelevantWord = parsed_hit.get(1);}
//                    else { hitFirstRelevantWord = parsed_hit.get(0); } // hitFirstWord is the first word of the hit, and shows relevance to the search priority
//                } else {
//                    hitFirstRelevantWord = "";
//                }
//                //endregion
//
//                //region Perform the comparison to the input query and return the length of the shortest hit
//                // Match length is reduced every time there's a hit and the hit is shorter
//                if (       (concatenated_hit.contains(concatenated_word)
//                        || (TypeisLatin && hit.equals("to " + inglessVerb))
//                        || (!translationLatin.equals("") && concatenated_hit.contains(translationLatin))
//                        || (!translationHira.equals("") && concatenated_hit.contains(translationHira))
//                        || (!translationKata.equals("") && concatenated_hit.contains(translationKata)))) //ie. if the hit contains the input word, then do the following:
//                    {
//                    if (concatenated_hit.equals(concatenated_word)) {
//                        best_match = concatenated_hit;
//                        found_match = true;
//                        match_length = best_match.length()-1; // -1 to make sure that it's listed first
//                        if (is_verb_and_latin) { match_length = match_length-3;}
//                        continue;
//                    }
//                    if (hitFirstRelevantWord.contains(concatenated_word) && hitFirstRelevantWord.length() <= match_length) {
//                        best_match = hitFirstRelevantWord;
//                        found_match = true;
//                        match_length = best_match.length();
//                        continue;
//                    }
//                    if (ConvertFragment.getTextType(concatenated_hit).equals("latin") && hit.length() <= match_length) {
//                        best_match = hit;
//                        found_match = true;
//                        match_length = best_match.length();
//                        if (is_verb_and_latin) { match_length = match_length-3;}
//                    }
//                }
//                //endregion
//                if (found_match) {
//                    break;
//                }
//            }
//            //endregion

            if (found_match) {
                current_match_values = new long[2];
                current_match_values[0] = word.getWordId();
                //current_match_values[1] = (long) match_length;
                MatchList.add(current_match_values);
            }
        }

        for (int i=0;i<MatchList.size();i++) {
            matchingWordIds.add(MatchList.get(i)[0]);
        }

        return matchingWordIds;
    }
    public static String replaceInvalidKanjisWithValidOnes(String input, List<String[]> mSimilarsDatabase) {
        String output = "";
        char currentChar;
        boolean found;
        for (int i=0; i<input.length(); i++) {
            currentChar = input.charAt(i);
            found = false;
            for (int j = 0; j < mSimilarsDatabase.size(); j++) {
                if (mSimilarsDatabase.get(j).length > 0 && mSimilarsDatabase.get(j)[0].charAt(0) == currentChar) {
                    output += mSimilarsDatabase.get(j)[1].charAt(0);
                    found = true;
                    break;
                }
            }
            if (!found) output += currentChar;
        }
        return output;
    }
    public static Boolean checkIfWordIsOfTypeIngIng(String verb) {
        Boolean answer = false;
        if (	verb.equals("accinging") || verb.equals("astringing") || verb.equals("befringing") || verb.equals("besinging") ||
                verb.equals("binging") || verb.equals("boinging") || verb.equals("bowstringing") || verb.equals("bringing") ||
                verb.equals("clinging") || verb.equals("constringing") || verb.equals("cringing") || verb.equals("dinging") ||
                verb.equals("enringing") || verb.equals("flinging") || verb.equals("folksinging") || verb.equals("fringing") ||
                verb.equals("gunslinging") || verb.equals("hamstringing") || verb.equals("handwringing") || verb.equals("hinging") ||
                verb.equals("impinging") || verb.equals("inbringing") || verb.equals("infringing") || verb.equals("kinging") ||
                verb.equals("minging") || verb.equals("mudslinging") || verb.equals("outringing") || verb.equals("outsinging") ||
                verb.equals("outspringing") || verb.equals("outswinging") || verb.equals("outwinging") || verb.equals("overswinging") ||
                verb.equals("overwinging") || verb.equals("perstringing") || verb.equals("pinging") || verb.equals("refringing") ||
                verb.equals("rehinging") || verb.equals("respringing") || verb.equals("restringing") || verb.equals("ringing") ||
                verb.equals("singing") || verb.equals("slinging") || verb.equals("springing") || verb.equals("stinging") ||
                verb.equals("stringing") || verb.equals("swinging") || verb.equals("syringing") || verb.equals("twinging") ||
                verb.equals("unhinging") || verb.equals("unkinging") || verb.equals("unslinging") || verb.equals("unstringing") ||
                verb.equals("upbringing") || verb.equals("upflinging") || verb.equals("upspringing") || verb.equals("upswinging") ||
                verb.equals("whinging") || verb.equals("winging") || verb.equals("wringing") || verb.equals("zinging") ) {
            answer = true;
        }
        return answer;
    }
    public static String removeApostrophe(String sentence) {
        String current_char;
        String concatenated_sentence = "";
        for (int index=0; index<sentence.length(); index++) {
            current_char = Character.toString(sentence.charAt(index));
            if (!( current_char.equals("'")) ) {
                concatenated_sentence = concatenated_sentence + current_char;
            }
        }
        return concatenated_sentence;
    }
    public static int[] binarySearchInLatinIndex(boolean TypeisLatin, String concatenated_word, String concatenated_translationLatin, List<String[]> SortedIndex) {

        String prepared_word;
        // Prepare the input word to be used in the following algorithm (it must be only in latin script)
        if (TypeisLatin) { prepared_word = concatenated_word.toLowerCase(Locale.ENGLISH); }
        else { prepared_word = concatenated_translationLatin.toLowerCase(Locale.ENGLISH); }

        // Find the upper an lower limits of the range of words that start with the current word, using a binary search pattern
        int back_index = prepared_word.length();
        int char_index;
        int list_size = SortedIndex.size();
        int mid_index;
        String current_char_prepared_word;
        String current_char_mid;
        String current_indexed_word;
        boolean target_is_lower;
        boolean target_is_higher;
        boolean found_lower_limit;
        boolean found_upper_limit;

        int lower_limit = 0;
        int upper_limit = list_size-1;
        int lower_bound_of_lower_limit;
        int upper_bound_of_lower_limit;
        int lower_bound_of_upper_limit;
        int upper_bound_of_upper_limit;
        int divider;

        while (back_index > 0) {

            char_index = prepared_word.length()-back_index;
            current_char_prepared_word = String.valueOf(prepared_word.charAt(char_index));

            lower_bound_of_lower_limit = lower_limit;
            upper_bound_of_lower_limit = upper_limit;
            found_lower_limit = false;
            while (!found_lower_limit) {
                target_is_lower = false;
                target_is_higher = false;
                if (current_char_prepared_word.equals("a")) { break; }  // Since there is no letter lower than a, no lower limit needs to be found
                mid_index = (int) Math.floor(lower_bound_of_lower_limit + (upper_bound_of_lower_limit-lower_bound_of_lower_limit)/2);

                current_indexed_word = String.valueOf(SortedIndex.get(mid_index)[0]);
                if (char_index >= current_indexed_word.length()) { current_char_mid = "a"; } //Prevents words shorter than the input word from crashing the algorithm
                else { current_char_mid = String.valueOf(SortedIndex.get(mid_index)[0].charAt(char_index)).toLowerCase(Locale.ENGLISH); }

                if      (current_char_prepared_word.compareTo(current_char_mid) < 0) { target_is_lower = true; }
                else if (current_char_prepared_word.compareTo(current_char_mid) > 0) { target_is_higher = true; }

                if      (target_is_lower)   { upper_bound_of_lower_limit = mid_index; }
                else if (target_is_higher)  {
                    if (lower_bound_of_lower_limit == mid_index) {lower_bound_of_lower_limit = mid_index+1;}
                    else {lower_bound_of_lower_limit = mid_index;}
                }
                else {
                    divider = 1;
                    while(current_char_prepared_word.compareTo(current_char_mid) == 0) {

                        upper_bound_of_lower_limit = mid_index;
                        mid_index = (int) Math.floor(lower_bound_of_lower_limit + (mid_index-lower_bound_of_lower_limit)/2^divider);
                        if (upper_bound_of_lower_limit <= mid_index) {
                            //lower_bound_of_lower_limit = lower_limit;
                            break;
                        }

                        current_indexed_word = String.valueOf(SortedIndex.get(mid_index)[0]);
                        if (char_index >= current_indexed_word.length()) { current_char_mid = "a"; } //Prevents words shorter than the input word from crashing the algorithm
                        else { current_char_mid = String.valueOf(SortedIndex.get(mid_index)[0].charAt(char_index)); }

                        divider++;
                    }
                }
                if (upper_bound_of_lower_limit <= lower_bound_of_lower_limit) {
                    lower_limit = lower_bound_of_lower_limit;
                    found_lower_limit = true;
                }
            }

            lower_bound_of_upper_limit = lower_limit;
            upper_bound_of_upper_limit = upper_limit;
            found_upper_limit = false;
            while (!found_upper_limit) {

                target_is_lower = false;
                target_is_higher = false;
                if (current_char_prepared_word.equals("z")) { break; }  // Since there is no letter higher than z, no upper limit can be found
                mid_index = (int) Math.floor(upper_bound_of_upper_limit - (upper_bound_of_upper_limit-lower_bound_of_upper_limit)/2);

                current_indexed_word = String.valueOf(SortedIndex.get(mid_index)[0]);
                if (char_index >= current_indexed_word.length()) { current_char_mid = "a"; } //Prevents words shorter than the input word from crashing the algorithm
                else { current_char_mid = String.valueOf(SortedIndex.get(mid_index)[0].charAt(char_index)).toLowerCase(Locale.ENGLISH); }

                if      (current_char_prepared_word.compareTo(current_char_mid) < 0) { target_is_lower = true; }
                else if (current_char_prepared_word.compareTo(current_char_mid) > 0) { target_is_higher = true; }

                if (target_is_lower)   {
                    if (upper_bound_of_upper_limit == mid_index) {upper_bound_of_upper_limit = mid_index-1;}
                    else {upper_bound_of_upper_limit = mid_index;}
                }
                else if (target_is_higher)  { lower_bound_of_upper_limit = mid_index; }
                else {
                    divider = 1;
                    while(current_char_prepared_word.compareTo(current_char_mid) == 0) {
                        lower_bound_of_upper_limit = mid_index;
                        mid_index = (int) Math.floor(upper_bound_of_upper_limit - (upper_bound_of_upper_limit-mid_index)/2^divider);
                        if (mid_index > upper_limit) { upper_bound_of_upper_limit = upper_limit; break;}
                        else if (lower_bound_of_upper_limit >= mid_index) { upper_bound_of_upper_limit = upper_limit; break;}

                        current_indexed_word = String.valueOf(SortedIndex.get(mid_index)[0]);

                        if (char_index >= current_indexed_word.length()) { current_char_mid = "a"; } //Prevents words shorter than the input word from crashing the algorithm
                        else { current_char_mid = String.valueOf(SortedIndex.get(mid_index)[0].charAt(char_index)).toLowerCase(Locale.ENGLISH); }

                        divider++;
                    }
                }
                if (upper_bound_of_upper_limit <= lower_bound_of_upper_limit) {
                    upper_limit = upper_bound_of_upper_limit;
                    found_upper_limit = true;
                }
            }
            back_index--;
            if (upper_limit == lower_limit) {break;}
        }

        //returning result
        return new int[]{lower_limit,upper_limit};
    }
    public static int[] binarySearchInUTF8Index(String concatenated_word, List<String[]> SortedIndex, int relevant_column_index) {

        // Prepare the input word to be used in the following algorithm: the word is converted to its hex utf-8 value as a string, in fractional form
        String prepared_word = convertToUTF8(concatenated_word);

        // Find the upper an lower limits of the range of words that start with the current word, using a binary search pattern
        int back_index = (prepared_word.length()-2)/2; // because each hex = 2 characters, 2+ because of the "1."

        //Initialization
        int char_index;
        int list_size = SortedIndex.size();
        int mid_index;
        String current_char_prepared_word;
        String current_char_mid;
        String current_indexed_word;
        boolean target_is_lower;
        boolean target_is_higher;
        boolean found_lower_limit;
        boolean found_upper_limit;

        int lower_limit = 0;
        int upper_limit = list_size-1;
        int lower_bound_of_lower_limit;
        int upper_bound_of_lower_limit;
        int lower_bound_of_upper_limit;
        int upper_bound_of_upper_limit;
        int divider;
        int decoded_current_char_prepared_word;
        String current_value;

        while (back_index > 0) {
            char_index = prepared_word.length()-2*back_index; // because each hex = 2 characters, 2+ because of the "1."
            current_char_prepared_word = String.valueOf(prepared_word.substring(char_index, char_index+2));
            decoded_current_char_prepared_word = Integer.decode("0x" + current_char_prepared_word);

            lower_bound_of_lower_limit = lower_limit;
            upper_bound_of_lower_limit = upper_limit;
            found_lower_limit = false;
            while (!found_lower_limit) {
                target_is_lower = false;
                target_is_higher = false;

                if (current_char_prepared_word.equals("00")) { break; }  // Since there is no letter lower than 00, no lower limit needs to be found
                mid_index = (int) Math.floor(lower_bound_of_lower_limit + (upper_bound_of_lower_limit-lower_bound_of_lower_limit)/2);

                current_value = SortedIndex.get(mid_index)[relevant_column_index];
                if (!current_value.substring(0,1).equals("1")) {current_value = convertToUTF8(current_value);}

                current_indexed_word = String.valueOf(current_value);
                if (char_index >= current_indexed_word.length()) { current_char_mid = "00"; } //Prevents words shorter than the input word from crashing the algorithm
                else {
                    current_char_mid = SortedIndex.get(mid_index)[relevant_column_index];
                    if (!current_char_mid.substring(0,1).equals("1")) {current_char_mid = convertToUTF8(current_char_mid);}
                    current_char_mid = String.valueOf(current_char_mid.substring(char_index, char_index + 2));
                }

                if      (decoded_current_char_prepared_word - Integer.decode("0x" + current_char_mid) < 0) { target_is_lower = true; }
                else if (decoded_current_char_prepared_word - Integer.decode("0x" + current_char_mid) > 0) { target_is_higher = true; }

                if      (target_is_lower)   { upper_bound_of_lower_limit = mid_index; }
                else if (target_is_higher)  {
                    if (lower_bound_of_lower_limit == mid_index) {lower_bound_of_lower_limit = mid_index+1;}
                    else {lower_bound_of_lower_limit = mid_index;}
                }
                else {
                    divider = 1;
                    while(decoded_current_char_prepared_word - Integer.decode("0x" + current_char_mid) == 0) {

                        upper_bound_of_lower_limit = mid_index;
                        mid_index = (int) Math.floor(lower_bound_of_lower_limit + (mid_index-lower_bound_of_lower_limit)/2^divider);
                        if (upper_bound_of_lower_limit <= mid_index) {
                            //lower_bound_of_lower_limit = lower_limit;
                            break;
                        }

                        current_indexed_word = String.valueOf(SortedIndex.get(mid_index)[relevant_column_index]);
                        if (!current_indexed_word.substring(0,1).equals("1")) {current_indexed_word = convertToUTF8(current_indexed_word);}

                        if (char_index >= current_indexed_word.length()) { current_char_mid = "00"; } //Prevents words shorter than the input word from crashing the algorithm
                        else {
                            current_char_mid = SortedIndex.get(mid_index)[relevant_column_index];
                            if (!current_char_mid.substring(0,1).equals("1")) {current_char_mid = convertToUTF8(current_char_mid);}
                            current_char_mid = String.valueOf(current_char_mid.substring(char_index, char_index + 2));
                        }

                        divider++;
                    }
                }
                if (upper_bound_of_lower_limit == 0) {
                    lower_limit = upper_bound_of_lower_limit;
                    found_lower_limit = true;
                }
                else if (upper_bound_of_lower_limit <= lower_bound_of_lower_limit) {
                    lower_limit = lower_bound_of_lower_limit;
                    found_lower_limit = true;
                }
            }

            lower_bound_of_upper_limit = lower_limit;
            upper_bound_of_upper_limit = upper_limit;
            found_upper_limit = false;
            while (!found_upper_limit) {
                target_is_lower = false;
                target_is_higher = false;
                if (current_char_prepared_word.equals("FF")) { break; }  // Since there is no letter higher than FFFF, no upper limit can be found
                mid_index = (int) Math.floor(upper_bound_of_upper_limit - (upper_bound_of_upper_limit-lower_bound_of_upper_limit)/2);

                current_indexed_word = String.valueOf(SortedIndex.get(mid_index)[relevant_column_index]);
                if (!current_indexed_word.substring(0,1).equals("1")) {current_indexed_word = convertToUTF8(current_indexed_word);}

                if (char_index >= current_indexed_word.length()) { current_char_mid = "00"; } //Prevents words shorter than the input word from crashing the algorithm
                else {
                    current_char_mid = SortedIndex.get(mid_index)[relevant_column_index];
                    if (!current_char_mid.substring(0,1).equals("1")) {current_char_mid = convertToUTF8(current_char_mid);}
                    current_char_mid = String.valueOf(current_char_mid.substring(char_index, char_index + 2));
                }

                if      (decoded_current_char_prepared_word - Integer.decode("0x" + current_char_mid) < 0) { target_is_lower = true; }
                else if (decoded_current_char_prepared_word - Integer.decode("0x" + current_char_mid) > 0) { target_is_higher = true; }

                if (target_is_lower)   {
                    if (upper_bound_of_upper_limit == mid_index) {upper_bound_of_upper_limit = mid_index-1;}
                    else {upper_bound_of_upper_limit = mid_index;}
                }
                else if (target_is_higher)  { lower_bound_of_upper_limit = mid_index; }
                else {
                    divider = 1;
                    while(decoded_current_char_prepared_word - Integer.decode("0x" + current_char_mid) == 0) {
                        lower_bound_of_upper_limit = mid_index;

                        mid_index = (int) Math.floor(upper_bound_of_upper_limit - (upper_bound_of_upper_limit-mid_index)/2^divider);
                        if (mid_index > upper_limit) { upper_bound_of_upper_limit = upper_limit; break;}
                        else if (lower_bound_of_upper_limit >= mid_index || SortedIndex.get(mid_index).length<relevant_column_index-1) { upper_bound_of_upper_limit = upper_limit; break;}

                        current_indexed_word = String.valueOf(SortedIndex.get(mid_index)[relevant_column_index]);
                        if (!current_indexed_word.substring(0,1).equals("1")) {current_indexed_word = convertToUTF8(current_indexed_word);}

                        if (char_index >= current_indexed_word.length()) { current_char_mid = "00"; } //Prevents words shorter than the input word from crashing the algorithm
                        else {
                            current_char_mid = SortedIndex.get(mid_index)[relevant_column_index];
                            if (!current_char_mid.substring(0,1).equals("1")) {current_char_mid = convertToUTF8(current_char_mid);}
                            current_char_mid = String.valueOf(current_char_mid.substring(char_index, char_index + 2));
                        }

                        divider++;
                    }
                }
                if (lower_bound_of_upper_limit == list_size) {
                    upper_limit = lower_bound_of_upper_limit;
                    found_upper_limit = true;
                }
                else if (upper_bound_of_upper_limit <= lower_bound_of_upper_limit) {
                    upper_limit = upper_bound_of_upper_limit;
                    found_upper_limit = true;
                }
            }
            back_index--;
            if (upper_limit == lower_limit) {break;}
        }
        int[] result = {lower_limit,upper_limit};

        String result_string = SortedIndex.get(result[0])[relevant_column_index];
        if (!result_string.substring(0,1).equals("1")) {result_string = convertToUTF8(result_string).toUpperCase();}

        //Sanity check - if this fails, return -1 as a failed search
        if (result[0] == result[1] && !result_string.contains(prepared_word.substring(2,prepared_word.length()).toUpperCase())) { result[0] = -1; result[1] = -1; }

        return result;
    }
    public static List<LatinIndex> findQueryInLatinIndex(boolean TypeisLatin, String concatenated_word,
                                                         String concatenated_translationLatin, JapaneseToolboxRoomDatabase japaneseToolboxRoomDatabase) {

        String prepared_word;
        // Prepare the input word to be used in the following algorithm (it must be only in latin script)
        if (TypeisLatin) { prepared_word = concatenated_word.toLowerCase(Locale.ENGLISH); }
        else { prepared_word = concatenated_translationLatin.toLowerCase(Locale.ENGLISH); }

        List<LatinIndex> matchingLatinIndexes = japaneseToolboxRoomDatabase.getLatinIndexesListForStartingWord(prepared_word);
        return matchingLatinIndexes;
    }
    public static List<KanjiIndex> findQueryInKanjiIndex(String concatenated_word, JapaneseToolboxRoomDatabase japaneseToolboxRoomDatabase) {

        // Prepare the input word to be used in the following algorithm: the word is converted to its hex utf-8 value as a string, in fractional form
        String prepared_word = convertToUTF8(concatenated_word);

        List<KanjiIndex> matchingKanjiIndexes = japaneseToolboxRoomDatabase.getKanjiIndexesListForStartingWord(prepared_word);
        return matchingKanjiIndexes;
    }
    public static String convertToUTF8(String input_string) {

        byte[] byteArray = {};
        try {
            byteArray = input_string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String prepared_word = "1.";
        for (byte b : byteArray) {
            prepared_word = prepared_word + Integer.toHexString(b & 0xFF);
        }
        return prepared_word;
    }
}
