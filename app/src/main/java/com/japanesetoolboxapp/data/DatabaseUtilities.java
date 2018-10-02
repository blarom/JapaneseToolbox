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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DatabaseUtilities {

    public static final String firebaseEmail = BuildConfig.firebaseEmail;
    public static final String firebasePass = BuildConfig.firebasePass;
    private static final int WORD_SEARCH_CHAR_COUNT_THRESHOLD = 3;
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
            if (matchingWordType.contains("V") && !matchingWordType.equals("VC") && !matchingWordType.equals("VdaI")) {

                List<String> meaningElements = Arrays.asList(matchingWordMeaning.split(","));
                StringBuilder meaningFixed = new StringBuilder();
                boolean valueIsInParentheses = false;
                for (int k = 0; k < meaningElements.size(); k++) {
                    if (valueIsInParentheses) meaningFixed.append(meaningElements.get(k).trim());
                    else meaningFixed.append("to ").append(meaningElements.get(k).trim());

                    if (k < meaningElements.size() - 1) meaningFixed.append(", ");

                    if (meaningElements.get(k).contains("(") && !meaningElements.get(k).contains(")")) valueIsInParentheses = true;
                    else if (!meaningElements.get(k).contains("(") && meaningElements.get(k).contains(")")) valueIsInParentheses = false;
                }
                matchingWordMeaning = meaningFixed.toString();
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
        String type;
        String[] currentMeaningCharacteristics;

        verb.setVerbId(Integer.parseInt(verbDatabase.get(verbDbRowIndex)[0]));
        verb.setPreposition(verbDatabase.get(verbDbRowIndex)[7]);
        verb.setKanjiRoot(verbDatabase.get(verbDbRowIndex)[8]);
        verb.setLatinRoot(verbDatabase.get(verbDbRowIndex)[9]);
        verb.setExceptionIndex(verbDatabase.get(verbDbRowIndex)[10]);
        verb.setRomaji(verbDatabase.get(verbDbRowIndex)[2]);
        verb.setKana(ConvertFragment.getLatinHiraganaKatakana(verb.getRomaji()).get(1).substring(0,1));

        //Getting the family

        String MM_index = verbDatabase.get(verbDbRowIndex)[4];
        List<String> MM_index_list = Arrays.asList(MM_index.split(";"));
        if (MM_index_list.size() == 0) { return verb; }

        int current_MM_index;
        for (int i=0; i< MM_index_list.size(); i++) {

            current_MM_index = Integer.parseInt(MM_index_list.get(i)) - 1;
            currentMeaningCharacteristics = meaningsDatabase.get(current_MM_index);

            //Getting the Family value
            type = currentMeaningCharacteristics[2];
            if (i == 0 && !type.equals("")) {
                verb.setTrans(String.valueOf(type.charAt(type.length() - 1)));
                if (!type.substring(0,1).equals("V")) {
                    Log.i(Utilities.DEBUG_TAG, "Warning! Found verb with incorrect type (Meaning index:" + Integer.toString(current_MM_index) +")");
                }
                verb.setFamily(type.substring(1, type.length() - 1));
                break; //Stopping the loop at the first meaning
            }

        }

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
    public static List<Long> getMatchingWordIdsUsingRoomIndexes(String searchWord, JapaneseToolboxCentralRoomDatabase japaneseToolboxCentralRoomDatabase) {


        //region Initializations
        List<Long> matchingWordIds = new ArrayList<>();
        List<Long> MatchingWordIdsFromIndex = new ArrayList<>();
        List<String> keywordsList;
        List<long[]> MatchList = new ArrayList<>();
        String keywords;
        long[] current_match_values;
        boolean found_match;
        boolean queryIsVerbWithTo = false;
        String searchWordWithoutTo = "";
        String searchWordNoSpaces;
        //endregion

        //region Converting the searchWord to usable forms, preventing invalid characters from influencing the search results
        searchWord = searchWord.toLowerCase(Locale.ENGLISH);
        searchWord = removeApostrophes(searchWord);
        searchWord = Utilities.removeNonSpaceSpecialCharacters(searchWord);
        searchWordNoSpaces = searchWord.replace(" ", "");

        //Registering if the input query is a "to " verb
        if (searchWord.length()>3 && searchWord.substring(0,3).equals("to ")) {
            queryIsVerbWithTo = true;
            searchWordWithoutTo = searchWord.substring(3, searchWord.length());
        }
        //endregion

        //region If there is an "inging" verb instance, reduce it to an "ing" instance (e.g. singing >> sing)
        String verb2;
        String inglessVerb = searchWord;
        if (searchWord.length() > 2 && searchWord.substring(searchWord.length()-3).equals("ing")) {

            if (searchWord.length() > 5 && searchWord.substring(searchWord.length()-6).equals("inging")) {
                if (	(searchWord.substring(0, 2+1).equals("to ") && checkIfWordIsOfTypeIngIng(searchWord.substring(3,searchWord.length()))) ||
                        (!searchWord.substring(0, 2+1).equals("to ") && checkIfWordIsOfTypeIngIng(searchWord.substring(0,searchWord.length())))   ) {
                    // If the verb ends with "inging" then remove the the second "ing"
                    inglessVerb = searchWord.substring(0,searchWord.length()-3);
                }
            }
            else {
//                verb2 = searchWord + "ing";
//                if ((!verb2.substring(0, 2+1).equals("to ") || !checkIfWordIsOfTypeIngIng(verb2.substring(3, searchWord.length() + 3))) &&
//                        (verb2.substring(0, 2+1).equals("to ") || !checkIfWordIsOfTypeIngIng(verb2.substring(0, searchWord.length() + 3)))) {
//                    // If the verb does not belong to the keywords, then remove the ending "ing" so that it can be compared later on to the verbs excel
//                    //If the verb is for e.g. to sing / sing, where verb2 = to singing / singing, then check that verb2 (without the "to ") belongs to the keywords, and if it does then do nothing
//
//                    inglessVerb = searchWord.substring(0,searchWord.length()-3);
//                }
            }
        }
        //endregion

        //region Getting the input type and its converted form (english/romaji/kanji/invalid)
        List<String> translationList = ConvertFragment.getLatinHiraganaKatakana(searchWordNoSpaces);

        String searchWordTransliteratedLatin = translationList.get(0);
        String searchWordTransliteratedHiragana = translationList.get(1);
        String searchWordTransliteratedKatakana = translationList.get(2);
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

        //region Replacing a Kana input by its Romaji form
        if (TypeisKana) {
            searchWord = searchWordTransliteratedLatin;
            searchWordNoSpaces = searchWordTransliteratedLatin;
        }
        //endregion

        //region Search for the matches in the indexed keywords
        List<String> searchResultKeywordsArray = new ArrayList<>();
        List<LatinIndex> latinIndices;
        List<KanjiIndex> kanjiIndices;
        if (TypeisLatin || TypeisKana || TypeisNumber) {

            //If the input is a verb in "to " form, remove the "to " for the search only (results will be filtered later on)
            String input_word = Utilities.removeNonSpaceSpecialCharacters(searchWord);
            if (searchWord.length()>3) {
                if (searchWord.substring(0, 3).equals("to ")) {
                    input_word = searchWord.substring(2, searchWord.length());
                }
            }

            latinIndices = findQueryInLatinIndex(TypeisLatin, input_word, japaneseToolboxCentralRoomDatabase);

            if (latinIndices.size()==0) return matchingWordIds;

            // If the entered word is Latin and only has up to WORD_SEARCH_CHAR_COUNT_THRESHOLD characters, limit the word keywords to be checked later
            if ((TypeisLatin || TypeisKana) && searchWordNoSpaces.length() < WORD_SEARCH_CHAR_COUNT_THRESHOLD
                    || TypeisNumber && searchWordNoSpaces.length() < WORD_SEARCH_CHAR_COUNT_THRESHOLD-1) {
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
            kanjiIndices = findQueryInKanjiIndex(searchWordNoSpaces, japaneseToolboxCentralRoomDatabase);
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

            latinIndices = findQueryInLatinIndex(TypeisLatin, inglessVerb, japaneseToolboxCentralRoomDatabase);

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
        if ((TypeisLatin || TypeisKana) && searchWord.length() < 3) {
            onlyRetrieveShortRomajiWords = true;
        }
        //endregion

        //region Filtering the matches
        List<Word> matchingWordList = japaneseToolboxCentralRoomDatabase.getWordListByWordIds(MatchingWordIdsFromIndex);
        String romaji;
        String meanings;
        String[] meaningSet;
        StringBuilder builder;
        boolean isExactMeaningWordsMatch;
        int searchWordLength = searchWord.length();
        int romajiLength;
        for (Word word : matchingWordList) {

            found_match = false;

            //region Handling short words
            if ((TypeisLatin || TypeisKana) && onlyRetrieveShortRomajiWords) {

                //Checking if the word is an exact match to one of the words in the meanings
                builder = new StringBuilder("");
                for (Word.Meaning meaning : word.getMeanings()) {
                    builder.append(" ");
                    builder.append(meaning.getMeaning().replace(", ", " ").replace("(", " ").replace(")", " "));
                }
                meanings = builder.toString();
                meaningSet = meanings.split(" ");
                isExactMeaningWordsMatch = false;
                for (String meaningSetElement : meaningSet) {
                    if (meaningSetElement.equals(searchWord)) {
                        isExactMeaningWordsMatch = true;
                        break;
                    }
                }

                romaji = word.getRomaji();
                romajiLength = romaji.length();
                if (isExactMeaningWordsMatch
                        || searchWordLength < 3
                            && romajiLength <= searchWordLength + 1
                            && romaji.contains(searchWord)) {
                    found_match = true;
                }
                else continue;

            }
            //endregion

            //Otherwise, handling longer words
            if (!found_match) {
                keywords = word.getKeywords();
                found_match = keywords.contains(searchWord)
                        || keywords.contains(searchWordNoSpaces)
                        || keywords.contains(inglessVerb)
                        || queryIsVerbWithTo && keywords.contains(searchWordWithoutTo);
            }


//            //region Loop initializations
//            keywords = word.getKeywords();
//            if (keywords.equals("") || keywords.equals("-") || keywords.equals("KEYWORDS")) continue;
//            if (onlyRetrieveShortRomajiWords && word.getRomaji().length() > 4) continue;
//            keywordsList = Arrays.asList(keywords.split(","));
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
//            //region If there is a word in the keywords that matches the input word, get the corresponding row index
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
//                if (       (concatenated_hit.contains(searchWordNoSpaces)
//                        || (TypeisLatin && hit.equals("to " + inglessVerb))
//                        || (!translationLatin.equals("") && concatenated_hit.contains(translationLatin))
//                        || (!searchWordTransliteratedHiragana.equals("") && concatenated_hit.contains(searchWordTransliteratedHiragana))
//                        || (!searchWordTransliteratedKatakana.equals("") && concatenated_hit.contains(searchWordTransliteratedKatakana)))) //ie. if the hit contains the input word, then do the following:
//                    {
//                    if (concatenated_hit.equals(searchWordNoSpaces)) {
//                        best_match = concatenated_hit;
//                        found_match = true;
//                        match_length = best_match.length()-1; // -1 to make sure that it's listed first
//                        if (is_verb_and_latin) { match_length = match_length-3;}
//                        continue;
//                    }
//                    if (hitFirstRelevantWord.contains(searchWordNoSpaces) && hitFirstRelevantWord.length() <= match_length) {
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
    private static Boolean checkIfWordIsOfTypeIngIng(String verb) {
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
    private static String removeApostrophes(String sentence) {
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
    private static List<LatinIndex> findQueryInLatinIndex(boolean TypeisLatin, String concatenated_word, JapaneseToolboxCentralRoomDatabase japaneseToolboxCentralRoomDatabase) {

        List<LatinIndex> matchingLatinIndexes;
        if (concatenated_word.length() > 2) {
            matchingLatinIndexes = japaneseToolboxCentralRoomDatabase.getLatinIndexesListForStartingWord(concatenated_word);
            return matchingLatinIndexes;
        }
        else {
            //Preventing the index search from returning too many results and crashing the app
            matchingLatinIndexes = new ArrayList<>();
            LatinIndex index = japaneseToolboxCentralRoomDatabase.getLatinIndexListForExactWord(concatenated_word);
            if (index!=null) matchingLatinIndexes.add(index); //Only add the index if the word was found in the index
            return matchingLatinIndexes;
        }
    }
    private static List<KanjiIndex> findQueryInKanjiIndex(String concatenated_word, JapaneseToolboxCentralRoomDatabase japaneseToolboxCentralRoomDatabase) {

        // Prepare the input word to be used in the following algorithm: the word is converted to its hex utf-8 value as a string, in fractional form
        String prepared_word = convertToUTF8Index(concatenated_word);

        List<KanjiIndex> matchingKanjiIndexes = japaneseToolboxCentralRoomDatabase.getKanjiIndexesListForStartingWord(prepared_word);
        return matchingKanjiIndexes;
    }
    public static String convertToUTF8Index(String input_string) {

        byte[] byteArray = {};
        try {
            byteArray = input_string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        StringBuilder prepared_word = new StringBuilder("1.");
        for (byte b : byteArray) {
            prepared_word.append(Integer.toHexString(b & 0xFF));
        }
        return prepared_word.toString();
    }
    public static String convertFromUTF8Index(String inputHex) {

        //inspired by: https://stackoverflow.com/questions/15749475/java-string-hex-to-string-ascii-with-accentuation
        if(inputHex.length()<4) return "";
        inputHex = inputHex.toLowerCase().substring(2,inputHex.length());

        ByteBuffer buff = ByteBuffer.allocate(inputHex.length()/2);
        for (int i = 0; i < inputHex.length(); i+=2) {
            buff.put((byte)Integer.parseInt(inputHex.substring(i, i+2), 16));
        }
        buff.rewind();
        Charset cs = Charset.forName("UTF-8");
        CharBuffer cb = cs.decode(buff);

        String result = cb.toString();

        return result;
    }
}
