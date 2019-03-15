package com.japanesetoolboxapp.loaders;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.japanesetoolboxapp.data.ConjugationTitle;
import com.japanesetoolboxapp.data.JapaneseToolboxCentralRoomDatabase;
import com.japanesetoolboxapp.data.Verb;
import com.japanesetoolboxapp.data.Word;
import com.japanesetoolboxapp.resources.GlobalConstants;
import com.japanesetoolboxapp.resources.Utilities;
import com.japanesetoolboxapp.ui.ConvertFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.japanesetoolboxapp.resources.GlobalConstants.TYPE_HIRAGANA;
import static com.japanesetoolboxapp.resources.GlobalConstants.TYPE_INVALID;
import static com.japanesetoolboxapp.resources.GlobalConstants.TYPE_KANJI;
import static com.japanesetoolboxapp.resources.GlobalConstants.TYPE_KATAKANA;
import static com.japanesetoolboxapp.resources.GlobalConstants.TYPE_LATIN;
import static com.japanesetoolboxapp.resources.GlobalConstants.VERB_FAMILIES_FULL_NAME_MAP;
import static com.japanesetoolboxapp.resources.GlobalConstants.VERB_FAMILY_DA;
import static com.japanesetoolboxapp.resources.GlobalConstants.VERB_FAMILY_KURU;
import static com.japanesetoolboxapp.resources.GlobalConstants.VERB_FAMILY_SURU;
import static com.japanesetoolboxapp.resources.GlobalConstants.COLUMN_VERB_ISTEM;

public class VerbSearchAsyncTaskLoader extends AsyncTaskLoader<Object> {

    //region Parameters
    private static final int MAX_NUM_RESULTS_FOR_SURU_CONJ_SEARCH = 100;
    private static final int MATCHING_CATEGORY_INDEX = 0;
    public static final int MATCHING_CONJUGATION = 1;
    private List<Verb> mCompleteVerbsList;
    private final List<Word> mWordsFromDictFragment;
    private String mInputQuery;
    private final List<ConjugationTitle> mConjugationTitles;
    private int mInputQueryTextType;
    private List<String> mInputQueryTransliterations;
    private String mInputQueryTransliteratedKanaForm;
    private String mInputQueryTransliteratedLatinFormContatenated;
    private String mInputQueryTransliteratedKanaFormContatenated;
    private int mInputQueryTransliteratedLatinFormContatenatedLength;
    private int mInputQueryLength;
    private String mInputQueryContatenated;
    private int mInputQueryContatenatedLength;
    private JapaneseToolboxCentralRoomDatabase mJapaneseToolboxCentralRoomDatabase;
    private final HashMap<String, Integer> mFamilyConjugationIndexes = new HashMap<>();
    private final List<String[]> mVerbLatinConjDatabase;
    private final List<String[]> mVerbKanjiConjDatabase;
    private final static int INDEX_ROMAJI = 0;
    private final static int INDEX_KANJI = 1;
    private final static int INDEX_HIRAGANA_FIRST_CHAR = 2;
    private final static int INDEX_LATIN_ROOT = 3;
    private final static int INDEX_KANJI_ROOT = 4;
    private final static int INDEX_ACTIVE_ALTSPELLING = 5;
    private List<Object[]> mMatchingConjugationParameters;
    //endregion

    public VerbSearchAsyncTaskLoader(Context context, String inputQuery,
                              List<ConjugationTitle> conjugationTitles,
                              List<String[]> mVerbLatinConjDatabase,
                              List<String[]> mVerbKanjiConjDatabase,
                              List<Word> mWordsFromDictFragment) {
        super(context);
        this.mInputQuery = inputQuery;
        this.mConjugationTitles = conjugationTitles;
        this.mVerbLatinConjDatabase = mVerbLatinConjDatabase;
        this.mVerbKanjiConjDatabase = mVerbKanjiConjDatabase;
        this.mWordsFromDictFragment = mWordsFromDictFragment;
    }

    @Override
    protected void onStartLoading() {
        if (!TextUtils.isEmpty(mInputQuery)) forceLoad();
    }

    @Override
    public Object loadInBackground() {

        mJapaneseToolboxCentralRoomDatabase = JapaneseToolboxCentralRoomDatabase.getInstance(getContext());
        if (mCompleteVerbsList == null || mCompleteVerbsList.size()==0) {
            mCompleteVerbsList = mJapaneseToolboxCentralRoomDatabase.getAllVerbs();
        }

        List<Verb> matchingVerbs = new ArrayList<>();
        if (!TextUtils.isEmpty(mInputQuery)) {
            setInputQueryParameters();
            getFamilyConjugationIndexes();

            List<long[]> mMatchingVerbIdsAndCols = getMatchingVerbIdsAndCols();
            mMatchingVerbIdsAndCols = sortMatchingVerbsList(mMatchingVerbIdsAndCols);
            matchingVerbs = getVerbs(mMatchingVerbIdsAndCols);
        }

        List<Word> matchingWords = new ArrayList<>();
        List<Object[]> matchingConjugationParameters = new ArrayList<>();
        for (Verb verb : matchingVerbs) {
            List<Word> words = mJapaneseToolboxCentralRoomDatabase.getWordsByExactRomajiAndKanjiMatch(verb.getRomaji(), verb.getKanji());
            if (words.size()>0) matchingWords.add(words.get(0));

            Object[] parameters = getConjugationParameters(verb, mInputQuery, mInputQueryTransliterations.get(GlobalConstants.TYPE_LATIN));
            matchingConjugationParameters.add(parameters);
        }

        return new Object[]{matchingVerbs, matchingWords, matchingConjugationParameters};
    }

    private Object[] getConjugationParameters(Verb verb, String inputQuery, String inputQueryLatin) {

        List<Verb.ConjugationCategory> conjugationCategories = verb.getConjugationCategories();
        List<Verb.ConjugationCategory.Conjugation> conjugations;
        int matchingConjugationCategoryIndex = 0;
        String matchingConjugation = "";
        boolean foundMatch = false;
        if (!inputQuery.equals(verb.getLatinRoot()) && !inputQuery.equals(verb.getKanjiRoot())) {

            //First pass - checking for conjugations that equal the input query
            for (int i=0; i<conjugationCategories.size(); i++) {
                conjugations = conjugationCategories.get(i).getConjugations();
                for (Verb.ConjugationCategory.Conjugation conjugation : conjugations) {

                    if (mInputQueryTextType == TYPE_LATIN && conjugation.getConjugationLatin().equals(inputQuery)) {
                        matchingConjugation = conjugation.getConjugationLatin();
                        foundMatch = true;
                    }
                    else if ((mInputQueryTextType == TYPE_HIRAGANA || mInputQueryTextType == TYPE_KATAKANA)
                            && conjugation.getConjugationLatin().equals(inputQueryLatin)) {
                        matchingConjugation = conjugation.getConjugationLatin();
                        foundMatch = true;
                    }
                    else if (mInputQueryTextType == TYPE_KANJI && conjugation.getConjugationKanji().equals(inputQuery)) {
                        matchingConjugation = conjugation.getConjugationKanji();
                        foundMatch = true;
                    }

                    if (foundMatch) break;
                }
                if (foundMatch) {
                    matchingConjugationCategoryIndex = i;
                    break;
                }
            }

            //Second pass - if index is still 0, checking for conjugations that contain the input query
            if (matchingConjugationCategoryIndex == 0) {
                for (int i=0; i<conjugationCategories.size(); i++) {
                    conjugations = conjugationCategories.get(i).getConjugations();
                    for (Verb.ConjugationCategory.Conjugation conjugation : conjugations) {

                        if (mInputQueryTextType == TYPE_LATIN && conjugation.getConjugationLatin().contains(inputQuery)) {
                            matchingConjugation = conjugation.getConjugationLatin();
                            foundMatch = true;
                        }
                        else if ((mInputQueryTextType == TYPE_HIRAGANA || mInputQueryTextType == TYPE_KATAKANA)
                                && conjugation.getConjugationLatin().contains(inputQueryLatin)) {
                            matchingConjugation = conjugation.getConjugationLatin();
                            foundMatch = true;
                        }
                        else if (mInputQueryTextType == TYPE_KANJI && conjugation.getConjugationKanji().contains(inputQuery)) {
                            matchingConjugation = conjugation.getConjugationKanji();
                            foundMatch = true;
                        }

                        if (foundMatch) break;
                    }
                    if (foundMatch) {
                        matchingConjugationCategoryIndex = i;
                        break;
                    }
                }
            }
        }

        Object[] parameters = new Object[2];
        parameters[MATCHING_CATEGORY_INDEX] = matchingConjugationCategoryIndex;
        parameters[MATCHING_CONJUGATION] = matchingConjugation;

        return parameters;
    }
    private void setInputQueryParameters() {

        //Converting the word to lowercase (the search algorithm is not efficient if needing to search both lower and upper case)
        mInputQuery = mInputQuery.toLowerCase(Locale.ENGLISH);
        mInputQueryLength = mInputQuery.length();

        mInputQueryTransliterations = ConvertFragment.getLatinHiraganaKatakana(mInputQuery);

        mInputQueryTextType = ConvertFragment.getTextType(mInputQuery);

        String mInputQueryTransliteratedLatinForm = mInputQueryTransliterations.get(GlobalConstants.TYPE_LATIN);
        mInputQueryTransliteratedKanaForm = mInputQueryTransliterations.get(GlobalConstants.TYPE_HIRAGANA);

        mInputQueryContatenated = Utilities.removeSpecialCharacters(mInputQuery);
        mInputQueryContatenatedLength = mInputQueryContatenated.length();
        mInputQueryTransliteratedLatinFormContatenated = Utilities.removeSpecialCharacters(mInputQueryTransliteratedLatinForm);
        mInputQueryTransliteratedKanaFormContatenated = Utilities.removeSpecialCharacters(mInputQueryTransliteratedKanaForm);
        mInputQueryTransliteratedLatinFormContatenatedLength = mInputQueryTransliteratedLatinFormContatenated.length();
    }
    private void getFamilyConjugationIndexes() {
        for (int rowIndex = 3; rowIndex < mVerbLatinConjDatabase.size(); rowIndex++) {

            if (mVerbLatinConjDatabase.get(rowIndex)[0].equals("") || !mVerbLatinConjDatabase.get(rowIndex)[1].equals("")) continue;

            if ("su godan".equals(mVerbLatinConjDatabase.get(rowIndex)[0]) && !mFamilyConjugationIndexes.containsKey(GlobalConstants.VERB_FAMILY_SU_GODAN)) {
                mFamilyConjugationIndexes.put(GlobalConstants.VERB_FAMILY_SU_GODAN, rowIndex);
            } else if ("ku godan".equals(mVerbLatinConjDatabase.get(rowIndex)[0]) && !mFamilyConjugationIndexes.containsKey(GlobalConstants.VERB_FAMILY_KU_GODAN)) {
                mFamilyConjugationIndexes.put(GlobalConstants.VERB_FAMILY_KU_GODAN, rowIndex);
            } else if ("iku special class".equals(mVerbLatinConjDatabase.get(rowIndex)[0]) && !mFamilyConjugationIndexes.containsKey(GlobalConstants.VERB_FAMILY_IKU_SPECIAL)) {
                mFamilyConjugationIndexes.put(GlobalConstants.VERB_FAMILY_IKU_SPECIAL, rowIndex);
            } else if ("yuku special class".equals(mVerbLatinConjDatabase.get(rowIndex)[0]) && !mFamilyConjugationIndexes.containsKey(GlobalConstants.VERB_FAMILY_YUKU_SPECIAL)) {
                mFamilyConjugationIndexes.put(GlobalConstants.VERB_FAMILY_YUKU_SPECIAL, rowIndex);
            } else if ("gu godan".equals(mVerbLatinConjDatabase.get(rowIndex)[0]) && !mFamilyConjugationIndexes.containsKey(GlobalConstants.VERB_FAMILY_GU_GODAN)) {
                mFamilyConjugationIndexes.put(GlobalConstants.VERB_FAMILY_GU_GODAN, rowIndex);
            } else if ("bu godan".equals(mVerbLatinConjDatabase.get(rowIndex)[0]) && !mFamilyConjugationIndexes.containsKey(GlobalConstants.VERB_FAMILY_BU_GODAN)) {
                mFamilyConjugationIndexes.put(GlobalConstants.VERB_FAMILY_BU_GODAN, rowIndex);
            } else if ("mu godan".equals(mVerbLatinConjDatabase.get(rowIndex)[0]) && !mFamilyConjugationIndexes.containsKey(GlobalConstants.VERB_FAMILY_MU_GODAN)) {
                mFamilyConjugationIndexes.put(GlobalConstants.VERB_FAMILY_MU_GODAN, rowIndex);
            } else if ("nu godan".equals(mVerbLatinConjDatabase.get(rowIndex)[0]) && !mFamilyConjugationIndexes.containsKey(GlobalConstants.VERB_FAMILY_NU_GODAN)) {
                mFamilyConjugationIndexes.put(GlobalConstants.VERB_FAMILY_NU_GODAN, rowIndex);
            } else if ("ru godan".equals(mVerbLatinConjDatabase.get(rowIndex)[0]) && !mFamilyConjugationIndexes.containsKey(GlobalConstants.VERB_FAMILY_RU_GODAN)) {
                mFamilyConjugationIndexes.put(GlobalConstants.VERB_FAMILY_RU_GODAN, rowIndex);
            } else if ("aru special class".equals(mVerbLatinConjDatabase.get(rowIndex)[0]) && !mFamilyConjugationIndexes.containsKey(GlobalConstants.VERB_FAMILY_ARU_SPECIAL)) {
                mFamilyConjugationIndexes.put(GlobalConstants.VERB_FAMILY_ARU_SPECIAL, rowIndex);
            } else if ("tsu godan".equals(mVerbLatinConjDatabase.get(rowIndex)[0]) && !mFamilyConjugationIndexes.containsKey(GlobalConstants.VERB_FAMILY_TSU_GODAN)) {
                mFamilyConjugationIndexes.put(GlobalConstants.VERB_FAMILY_TSU_GODAN, rowIndex);
            } else if ("u godan".equals(mVerbLatinConjDatabase.get(rowIndex)[0]) && !mFamilyConjugationIndexes.containsKey(GlobalConstants.VERB_FAMILY_U_GODAN)) {
                mFamilyConjugationIndexes.put(GlobalConstants.VERB_FAMILY_U_GODAN, rowIndex);
            } else if ("u special class".equals(mVerbLatinConjDatabase.get(rowIndex)[0]) && !mFamilyConjugationIndexes.containsKey(GlobalConstants.VERB_FAMILY_U_SPECIAL)) {
                mFamilyConjugationIndexes.put(GlobalConstants.VERB_FAMILY_U_SPECIAL, rowIndex);
            } else if ("ru ichidan".equals(mVerbLatinConjDatabase.get(rowIndex)[0]) && !mFamilyConjugationIndexes.containsKey(GlobalConstants.VERB_FAMILY_RU_ICHIDAN)) {
                mFamilyConjugationIndexes.put(GlobalConstants.VERB_FAMILY_RU_ICHIDAN, rowIndex);
            } else if ("desu copula".equals(mVerbLatinConjDatabase.get(rowIndex)[0]) && !mFamilyConjugationIndexes.containsKey(GlobalConstants.VERB_FAMILY_DA)) {
                mFamilyConjugationIndexes.put(GlobalConstants.VERB_FAMILY_DA, rowIndex);
            } else if ("kuru verb".equals(mVerbLatinConjDatabase.get(rowIndex)[0]) && !mFamilyConjugationIndexes.containsKey(GlobalConstants.VERB_FAMILY_KURU)) {
                mFamilyConjugationIndexes.put(GlobalConstants.VERB_FAMILY_KURU, rowIndex);
            } else if ("suru verb".equals(mVerbLatinConjDatabase.get(rowIndex)[0]) && !mFamilyConjugationIndexes.containsKey(GlobalConstants.VERB_FAMILY_SURU)) {
                mFamilyConjugationIndexes.put(GlobalConstants.VERB_FAMILY_SURU, rowIndex);
            }
        }
    }
    private String[] getVerbCharacteristicsFromAltSpelling(String trimmedAltSpelling, Verb verb) {

        String[] characteristics = new String[6];

        int altSpellingType = ConvertFragment.getTextType(trimmedAltSpelling);

        if (altSpellingType != mInputQueryTextType) return new String[]{};

        characteristics[INDEX_ROMAJI] = (altSpellingType == TYPE_LATIN)? trimmedAltSpelling : verb.getRomaji();
        characteristics[INDEX_KANJI] = (altSpellingType == TYPE_KANJI)? trimmedAltSpelling : verb.getKanji();
        characteristics[INDEX_HIRAGANA_FIRST_CHAR] =
                (altSpellingType == TYPE_HIRAGANA) ? trimmedAltSpelling.substring(0,1) :
                        ConvertFragment.getLatinHiraganaKatakana(characteristics[INDEX_ROMAJI]).get(TYPE_HIRAGANA).substring(0,1);
        characteristics[INDEX_LATIN_ROOT] = Utilities.getVerbRoot(characteristics[INDEX_ROMAJI], verb.getFamily(), TYPE_LATIN);
        characteristics[INDEX_KANJI_ROOT] = Utilities.getVerbRoot(characteristics[INDEX_KANJI], verb.getFamily(), TYPE_KANJI);
        characteristics[INDEX_ACTIVE_ALTSPELLING] = trimmedAltSpelling;

        return characteristics;
    }
    private List<long[]> getMatchingVerbIdsAndCols() {

        if (mInputQueryTextType == TYPE_INVALID || mCompleteVerbsList==null) return new ArrayList<>();

        //region Initializations
        int NumberOfSheetCols = mVerbLatinConjDatabase.get(0).length;
        List<Integer> dilutedConjugationColIndexes = new ArrayList<>();
        boolean queryIsContainedInNormalFamilyConjugation;
        boolean queryIsContainedInAKuruConjugation;
        boolean queryIsContainedInASuruConjugation;
        boolean queryIsContainedInADesuConjugation;
        boolean queryIsContainedInIruVerbConjugation;
        int exceptionIndex;
        String[] currentFamilyConjugations;
        String[] currentConjugations;
        String family;
        String romaji;
        String altSpellingsAsString;
        List<String[]> verbSearchCandidates;
        char hiraganaFirstChar;
        String latinRoot;
        String kanjiRoot;
        String conjugationValue;
        boolean foundMatch;
        boolean allowExpandedConjugationsComparison;
        int matchColumn = 0;
        boolean onlyRetrieveShortRomajiVerbs = false;
        //endregion

        //region Removing the "ing" from the query verb (if relevant)
        if (mInputQueryLength > 2 && mInputQuery.substring(mInputQueryLength -3).equals("ing")) {

            if (mInputQueryLength > 5 && mInputQuery.substring(mInputQueryLength -6).equals("inging")) {
                if (	(mInputQuery.substring(0, 2+1).equals("to ") && IsOfTypeIngIng(mInputQuery.substring(3, mInputQueryLength))) ||
                        (!mInputQuery.substring(0, 2+1).equals("to ") && IsOfTypeIngIng(mInputQuery.substring(0, mInputQueryLength)))   ) {
                    // If the verb ends with "inging" then remove the the second "ing"
                    mInputQuery = mInputQuery.substring(0, mInputQueryLength -3);
                }
            }
            else {
                String verb2WithIng = mInputQuery + "ing";
                if ((verb2WithIng.substring(0, 2 + 1).equals("to ") && IsOfTypeIngIng(verb2WithIng.substring(3, mInputQueryLength + 3))) ||
                        (!verb2WithIng.substring(0, 2 + 1).equals("to ") && IsOfTypeIngIng(verb2WithIng.substring(0, mInputQueryLength + 3)))) {
                } else {
                    // If the verb does not belong to the list, then remove the ending "ing" so that it can be compared later on to the verbs excel
                    //If the verb is for e.g. to sing / sing (verb2 = to singing / singing), then check that verb2 (without the "to ") belongs to the list, and if it does then do nothing

                    mInputQuery = mInputQuery.substring(0, mInputQueryLength -3);
                }
            }
        }
        //endregion

        //region Taking care of the case where the input is a basic conjugation that will cause the app to return too many verbs
        queryIsContainedInNormalFamilyConjugation = false;
        queryIsContainedInASuruConjugation = false;
        queryIsContainedInAKuruConjugation = false;
        queryIsContainedInADesuConjugation = false;
        queryIsContainedInIruVerbConjugation = false;
        int familyIndex;
        for (String key : mFamilyConjugationIndexes.keySet()) {
            familyIndex = mFamilyConjugationIndexes.get(key);
            switch (key) {
                case VERB_FAMILY_DA:
                    currentFamilyConjugations = mVerbLatinConjDatabase.get(familyIndex);
                    for (int column = 1; column < NumberOfSheetCols; column++) {
                        if (currentFamilyConjugations[column].equals(mInputQueryContatenated)
                                || currentFamilyConjugations[column].equals(mInputQueryTransliteratedKanaFormContatenated)) {
                            queryIsContainedInADesuConjugation = true;
                        }
                    }
                    if (queryIsContainedInADesuConjugation) break;
                    currentFamilyConjugations = mVerbKanjiConjDatabase.get(familyIndex);
                    for (int column = 1; column < NumberOfSheetCols; column++) {
                        if (currentFamilyConjugations[column].equals(mInputQueryContatenated)) {
                            queryIsContainedInADesuConjugation = true;
                            break;
                        }
                    }
                    break;
                case VERB_FAMILY_KURU:
                    currentFamilyConjugations = mVerbLatinConjDatabase.get(familyIndex);
                    for (int column = 1; column < NumberOfSheetCols; column++) {
                        if (currentFamilyConjugations[column].equals(mInputQueryContatenated)
                                || currentFamilyConjugations[column].equals(mInputQueryTransliteratedKanaFormContatenated)) {
                            queryIsContainedInAKuruConjugation = true;
                        }
                    }
                    if (queryIsContainedInAKuruConjugation) break;
                    currentFamilyConjugations = mVerbKanjiConjDatabase.get(familyIndex);
                    for (int column = 1; column < NumberOfSheetCols; column++) {
                        if (currentFamilyConjugations[column].equals(mInputQueryContatenated)) {
                            queryIsContainedInAKuruConjugation = true;
                            break;
                        }
                    }
                    break;
                case VERB_FAMILY_SURU:
                    currentFamilyConjugations = mVerbLatinConjDatabase.get(familyIndex);
                    for (int column = 1; column < NumberOfSheetCols; column++) {
                        if (currentFamilyConjugations[column].equals(mInputQueryContatenated)
                                || currentFamilyConjugations[column].equals(mInputQueryTransliteratedKanaFormContatenated)) {
                            queryIsContainedInASuruConjugation = true;
                        }
                    }
                    if (queryIsContainedInASuruConjugation) break;
                    currentFamilyConjugations = mVerbKanjiConjDatabase.get(familyIndex);
                    for (int column = 1; column < NumberOfSheetCols; column++) {
                        if (currentFamilyConjugations[column].equals(mInputQueryContatenated)) {
                            queryIsContainedInASuruConjugation = true;
                            break;
                        }
                    }
                    break;
                default:
                    currentFamilyConjugations = mVerbLatinConjDatabase.get(familyIndex);
                    for (int column = COLUMN_VERB_ISTEM; column < NumberOfSheetCols; column++) {
                        if (currentFamilyConjugations[column].contains(mInputQueryContatenated)
                                || currentFamilyConjugations[column].contains(mInputQueryTransliteratedKanaFormContatenated)) {
                            queryIsContainedInNormalFamilyConjugation = true;
                            break;
                        }
                    }
                    if (queryIsContainedInNormalFamilyConjugation) break;
                    currentFamilyConjugations = mVerbKanjiConjDatabase.get(familyIndex);
                    for (int column = COLUMN_VERB_ISTEM; column < NumberOfSheetCols; column++) {
                        if (currentFamilyConjugations[column].contains(mInputQueryContatenated)) {
                            queryIsContainedInNormalFamilyConjugation = true;
                            break;
                        }
                    }
                    break;
            }
        }
        if (queryIsContainedInASuruConjugation || queryIsContainedInAKuruConjugation || queryIsContainedInADesuConjugation) queryIsContainedInNormalFamilyConjugation = false;

        if (mInputQueryTextType == TYPE_LATIN && mInputQueryContatenated.length() < 4
                || (mInputQueryTextType == TYPE_HIRAGANA || mInputQueryTextType == TYPE_KATAKANA)
                && mInputQueryContatenated.length() < 3) {
            onlyRetrieveShortRomajiVerbs = true;
        }

        //Checking if the query is an iru verb conjugation, which could lead to too may hits
        currentFamilyConjugations = mVerbLatinConjDatabase.get(mFamilyConjugationIndexes.get("rui"));
        String currentConjugation;
        for (int column = COLUMN_VERB_ISTEM; column < NumberOfSheetCols; column++) {
            currentConjugation = "i" + currentFamilyConjugations[column];
            if (currentConjugation.contains(mInputQueryContatenated)
                    || currentConjugation.contains(mInputQueryTransliteratedKanaFormContatenated)) {
                queryIsContainedInIruVerbConjugation = true;
                break;
            }
        }
        //endregion

        //region Performing column dilution in order to make the search more efficient (the diluted column ArrayList is used in the Search Algorithm)
        int queryLengthForDilution = 0;
        List<String[]> verbConjugationMaxLengths = new ArrayList<>();
        int conjugationMaxLength;
        if (mInputQueryTextType == TYPE_LATIN) {
            verbConjugationMaxLengths = Utilities.readCSVFileFirstRow("LineVerbsLengths - 3000 kanji.csv", getContext());
            queryLengthForDilution = mInputQueryContatenatedLength;
        }
        else if (mInputQueryTextType == TYPE_HIRAGANA || mInputQueryTextType == TYPE_KATAKANA) {
            verbConjugationMaxLengths = Utilities.readCSVFileFirstRow("LineVerbsLengths - 3000 kanji.csv", getContext());
            queryLengthForDilution = mInputQueryTransliteratedLatinFormContatenatedLength;
        }
        else if (mInputQueryTextType == TYPE_KANJI) {
            verbConjugationMaxLengths = Utilities.readCSVFileFirstRow("LineVerbsKanjiLengths - 3000 kanji.csv", getContext());
            queryLengthForDilution = mInputQueryContatenatedLength;
        }

        for (int col = COLUMN_VERB_ISTEM; col < NumberOfSheetCols; col++) {
            if (!verbConjugationMaxLengths.get(0)[col].equals(""))
                conjugationMaxLength = Integer.parseInt(verbConjugationMaxLengths.get(0)[col]);
            else conjugationMaxLength = 0;

            if (conjugationMaxLength >= queryLengthForDilution) dilutedConjugationColIndexes.add(col);
        }
        //endregion

        //region Getting the matching words from the Words database and filtering for verbs.
        //For words of length>=4, The matches are determined by the word's keywords list.
        List<Word> mMatchingWords;
        if (mWordsFromDictFragment == null) {
            List<Long> mMatchingWordIds = Utilities.getMatchingWordIdsAndDoBasicFiltering(mInputQuery, mJapaneseToolboxCentralRoomDatabase);
            mMatchingWords = mJapaneseToolboxCentralRoomDatabase.getWordListByWordIds(mMatchingWordIds);
        }
        else {
            mMatchingWords = mWordsFromDictFragment;
        }
        String type;
        List<long[]> matchingVerbIdsAndColsFromBasicCharacteristics = new ArrayList<>();
        int counter = 0;
        for (Word word : mMatchingWords) {
            type = word.getMeaningsEN().get(0).getType();

            //Preventing the input query being a suru verb conjugation from overloading the results
            if (queryIsContainedInASuruConjugation && word.getRomaji().contains(" suru")) {
                if (counter > MAX_NUM_RESULTS_FOR_SURU_CONJ_SEARCH) break;
                counter++;
            }

            //Adding the word id to the candidates
            if (type.length() > 0 && type.substring(0,1).equals("V") && !type.contains("VC")) {
                matchingVerbIdsAndColsFromBasicCharacteristics.add(new long[]{word.getWordId(), 0});
            }
        }
        //endregion

        //region Adding the suru verb if the query is contained in the suru conjugations, and limiting total results
        if (queryIsContainedInASuruConjugation) {
            Word suruVerb = mJapaneseToolboxCentralRoomDatabase.getWordsByExactRomajiAndKanjiMatch("suru", "為る").get(0);
            boolean alreadyInList = false;
            for (long[] idAndCol : matchingVerbIdsAndColsFromBasicCharacteristics) {
                if (idAndCol[0] == suruVerb.getWordId()) alreadyInList = true;
            }
            if (!alreadyInList) matchingVerbIdsAndColsFromBasicCharacteristics.add(new long[]{suruVerb.getWordId(), 0});
        }
        //endregion

        //region Getting the matching verbs according in the expanded conjugations and updating the conjugation roots if an altSpelling is used
        List<long[]> matchingVerbIdsAndColsFromExpandedConjugations = new ArrayList<>();
        List<long[]> copyOfMatchingVerbIdsAndColsFromBasicCharacteristics = new ArrayList<>(matchingVerbIdsAndColsFromBasicCharacteristics);
        boolean verbAlreadyFound;
        String trimmedAltSpelling;
        for (Verb verb : mCompleteVerbsList) {

            //region Skipping verbs that were already found
            verbAlreadyFound = false;
            for (long[] idAndCol : copyOfMatchingVerbIdsAndColsFromBasicCharacteristics) {
                if (idAndCol[0] == verb.getVerbId()) {

                    //Update the active fields for the current verb according to the altSpelling
                    boolean foundAltSpelling = false;
                    for (String altSpelling : verb.getAltSpellings().split(",")) {
                        trimmedAltSpelling = altSpelling.trim();
                        if (trimmedAltSpelling.equals(mInputQueryTransliteratedLatinFormContatenated)
                                || trimmedAltSpelling.equals(mInputQueryTransliteratedKanaFormContatenated)) {
                            String[] characteristics = getVerbCharacteristicsFromAltSpelling(trimmedAltSpelling, verb);
                            if (characteristics.length == 0) continue;
                            verb.setActiveLatinRoot(characteristics[INDEX_LATIN_ROOT]);
                            verb.setActiveKanjiRoot(characteristics[INDEX_KANJI_ROOT]);
                            verb.setActiveAltSpelling(trimmedAltSpelling);
                            foundAltSpelling = true;
                            break;
                        }
                    }
                    if (!foundAltSpelling) {
                        verb.setActiveLatinRoot(verb.getLatinRoot());
                        verb.setActiveKanjiRoot(verb.getKanjiRoot());
                        verb.setActiveAltSpelling(verb.getRomaji());
                    }
                    mJapaneseToolboxCentralRoomDatabase.updateVerb(verb);

                    //Remove the verb from the candidates list since it is already in the final list
                    copyOfMatchingVerbIdsAndColsFromBasicCharacteristics.remove(idAndCol);
                    verbAlreadyFound = true;
                    break;
                }
            }
            if (verbAlreadyFound) continue;
            //endregion

            //region Loop starting parameters initialization
            foundMatch = false;
            allowExpandedConjugationsComparison = true;
            //endregion

            //region Building the list of relevant base characteristics that the algorithm will check
            //This includes the romaji/kanji/romajiroot/kanjiroot/kana1stchar, also also the altSpelling equivalents
            altSpellingsAsString = verb.getAltSpellings();
            family = verb.getFamily();
            exceptionIndex = (verb.getExceptionIndex().equals(""))? 0 : Integer.valueOf(verb.getExceptionIndex());

            verbSearchCandidates = new ArrayList<>();
            String[] characteristics = new String[6];
            characteristics[INDEX_ROMAJI] = verb.getRomaji();
            characteristics[INDEX_KANJI] = verb.getKanji();
            characteristics[INDEX_HIRAGANA_FIRST_CHAR] = verb.getHiraganaFirstChar();
            characteristics[INDEX_LATIN_ROOT] = verb.getLatinRoot();
            characteristics[INDEX_KANJI_ROOT] = verb.getKanjiRoot();
            characteristics[INDEX_ACTIVE_ALTSPELLING] = verb.getRomaji();
            verbSearchCandidates.add(characteristics);

            for (String altSpelling : altSpellingsAsString.split(",")) {

                //Initializations
                trimmedAltSpelling = altSpelling.trim();
                if (trimmedAltSpelling.equals("")) continue;

                characteristics = getVerbCharacteristicsFromAltSpelling(trimmedAltSpelling, verb);
                if (characteristics.length == 0) continue;

                verbSearchCandidates.add(characteristics);
            }
            //endregion

            //region Checking if one of the relevant base words gets a match, and registering it in the match list
            for (String[] verbSearchCandidate : verbSearchCandidates) {

                //region Getting the verb characteristics
                romaji = verbSearchCandidate[INDEX_ROMAJI];
                hiraganaFirstChar = verbSearchCandidate[INDEX_HIRAGANA_FIRST_CHAR].charAt(0);
                latinRoot = verbSearchCandidate[INDEX_LATIN_ROOT];
                kanjiRoot = verbSearchCandidate[INDEX_KANJI_ROOT];
                //endregion

                //region Only allowing searches on verbs that satisfy the following conditions (including identical 1st char, kuru/suru/da, query length)
                if (    !(     (mInputQueryTextType == TYPE_LATIN && (romaji.charAt(0) == mInputQueryContatenated.charAt(0)))
                        || ((mInputQueryTextType == TYPE_HIRAGANA || mInputQueryTextType == TYPE_KATAKANA)
                        && (hiraganaFirstChar == mInputQueryTransliteratedKanaForm.charAt(0)))
                        || (mInputQueryTextType == TYPE_KANJI && kanjiRoot.contains(mInputQueryContatenated.substring(0,1)))
                        || romaji.contains("kuru")
                        || romaji.equals("suru")
                        || romaji.equals("da") )
                        || (mInputQueryTextType == TYPE_LATIN && mInputQueryContatenated.length() < 4 && !romaji.contains(mInputQueryContatenated))
                        || ((mInputQueryTextType == TYPE_HIRAGANA || mInputQueryTextType == TYPE_KATAKANA)
                        && mInputQueryContatenated.length() < 3 && !romaji.contains(mInputQueryTransliteratedLatinFormContatenated))
                        || (mInputQueryTextType == TYPE_KANJI && mInputQueryContatenated.length() < 3 && !mInputQueryContatenated.contains(kanjiRoot))
                        || (onlyRetrieveShortRomajiVerbs && romaji.length() > 4)     ) {
                    continue;
                }
                //endregion

                //region If the verb is equal to a family conjugation, only roots with length 1 (ie. iru/aru/eru/oru/uru verbs only) or a verb with the exact romaji value are considered. This prevents too many results. This does not conflict with da or kuru.
                if (        queryIsContainedInNormalFamilyConjugation
                        && latinRoot.length() > 1
                        ||  queryIsContainedInASuruConjugation
                        && !(kanjiRoot.equals("為"))
                        ||  queryIsContainedInAKuruConjugation
                        && !romaji.contains("kuru")
                        ||  queryIsContainedInADesuConjugation
                        && !romaji.equals("da")
                        || queryIsContainedInIruVerbConjugation
                        && !romaji.equals("iru")) {

                    //If the input is suru then prevent verbs with suru in the conjugations from giving a hit, but allow other verbs with romaji suru to give a hit
                    if (romaji.contains(" suru")) {
                        allowExpandedConjugationsComparison = false;
                    }
                    //Otherwise, if the verb does not meet the above conditions, skip this verb
                    else continue;
                }
                //endregion

                //region Main Comparator Algorithm
                if (allowExpandedConjugationsComparison) {

                    //region Latin conjugations comparison
                    if (mInputQueryTextType == TYPE_LATIN) {
                        if (!mFamilyConjugationIndexes.containsKey(family)) continue;

                        currentConjugations = Arrays.copyOf(mVerbLatinConjDatabase.get(exceptionIndex), NumberOfSheetCols);
                        if (exceptionIndex != mFamilyConjugationIndexes.get(family)) {
                            currentFamilyConjugations = Arrays.copyOf(mVerbLatinConjDatabase.get(mFamilyConjugationIndexes.get(family)), NumberOfSheetCols);
                            for (int col : dilutedConjugationColIndexes) {

                                if (currentConjugations[col].equals(""))
                                    conjugationValue = latinRoot + currentFamilyConjugations[col].replace(" ", "");
                                else conjugationValue = currentConjugations[col].replace(" ", "");

                                if (conjugationValue.contains(mInputQueryContatenated)) {
                                    foundMatch = true;
                                    matchColumn = col;
                                    break;
                                }
                            }
                        }
                        else {
                            for (int col : dilutedConjugationColIndexes) {
                                conjugationValue = latinRoot + currentConjugations[col].replace(" ", "");

                                if (conjugationValue.contains(mInputQueryContatenated)) {
                                    foundMatch = true;
                                    matchColumn = col;
                                    break;
                                }
                            }
                        }
                    }
                    //endregion

                    //region Kana conjugations comparison
                    else if (mInputQueryTextType == TYPE_HIRAGANA || mInputQueryTextType == TYPE_KATAKANA) {
                        if (!mFamilyConjugationIndexes.containsKey(family)) continue;

                        currentConjugations = Arrays.copyOf(mVerbLatinConjDatabase.get(exceptionIndex), NumberOfSheetCols);
                        if (exceptionIndex != mFamilyConjugationIndexes.get(family)) {
                            currentFamilyConjugations = Arrays.copyOf(mVerbLatinConjDatabase.get(mFamilyConjugationIndexes.get(family)), NumberOfSheetCols);
                            for (int col : dilutedConjugationColIndexes) {

                                if (currentConjugations[col].equals(""))
                                    conjugationValue = latinRoot + currentFamilyConjugations[col].replace(" ", "");
                                else conjugationValue = currentConjugations[col].replace(" ", "");

                                if (conjugationValue.contains(mInputQueryTransliteratedLatinFormContatenated)) {
                                    foundMatch = true;
                                    matchColumn = col;
                                    break;
                                }
                            }
                        }
                        else {
                            for (int col : dilutedConjugationColIndexes) {
                                conjugationValue = latinRoot + currentConjugations[col].replace(" ", "");

                                if (conjugationValue.contains(mInputQueryTransliteratedLatinFormContatenated)) {
                                    foundMatch = true;
                                    matchColumn = col;
                                    break;
                                }
                            }
                        }
                    }
                    //endregion

                    //region Kanji conjugations comparison
                    else if (mInputQueryTextType == TYPE_KANJI) {
                        if (!mFamilyConjugationIndexes.containsKey(family)) continue;

                        currentConjugations = Arrays.copyOf(mVerbKanjiConjDatabase.get(exceptionIndex), NumberOfSheetCols);
                        if (exceptionIndex != mFamilyConjugationIndexes.get(family)) {
                            currentFamilyConjugations = Arrays.copyOf(mVerbKanjiConjDatabase.get(mFamilyConjugationIndexes.get(family)), NumberOfSheetCols);
                            for (int col : dilutedConjugationColIndexes) {

                                if (currentConjugations[col].equals(""))
                                    conjugationValue = kanjiRoot + currentFamilyConjugations[col].replace(" ", "");
                                else conjugationValue = currentConjugations[col].replace(" ", "");

                                if (conjugationValue.contains(mInputQuery)) {
                                    foundMatch = true;
                                    matchColumn = col;
                                    break;
                                }
                            }
                        }
                        else {
                            for (int col : dilutedConjugationColIndexes) {
                                conjugationValue = kanjiRoot + currentConjugations[col].replace(" ", "");

                                if (conjugationValue.contains(mInputQuery)) {
                                    foundMatch = true;
                                    matchColumn = col;
                                    break;
                                }
                            }
                        }
                    }
                    //endregion
                }
                //endregion

                if (foundMatch) {
                    //If a match was found for an altSpelling, update the relevant fields
                    verb.setActiveLatinRoot(latinRoot);
                    verb.setActiveKanjiRoot(kanjiRoot);
                    verb.setActiveAltSpelling(verbSearchCandidate[INDEX_ACTIVE_ALTSPELLING]);
                    mJapaneseToolboxCentralRoomDatabase.updateVerb(verb);

                    //Update the list of match ids
                    matchingVerbIdsAndColsFromExpandedConjugations.add(new long[]{verb.getVerbId(), matchColumn});

                    break;
                }
            }
            //endregion
        }
        //endregion

        List<long[]> matchingVerbIdsAndCols = new ArrayList<>();
        matchingVerbIdsAndCols.addAll(matchingVerbIdsAndColsFromBasicCharacteristics);
        matchingVerbIdsAndCols.addAll(matchingVerbIdsAndColsFromExpandedConjugations);

        return matchingVerbIdsAndCols;
    }
    private List<long[]> sortMatchingVerbsList(List<long[]> ConjugationSearchMatchingVerbRowColIndexList) {

        List<long[]> matchingVerbIndexesLengthsAndCols = new ArrayList<>();

        //region Registering if the input query is a "to " verb
        boolean queryIsVerbWithTo = false;
        String queryWordWithoutTo = "";
        if (mInputQuery.length()>3 && mInputQuery.substring(0,3).equals("to ")) {
            queryIsVerbWithTo = true;
            queryWordWithoutTo = mInputQuery.substring(3);
        }
        //endregion

        //region Replacing the Kana input word by its romaji equivalent
        String inputQuery = mInputQuery;
        int textType = ConvertFragment.getTextType(inputQuery);
        if (textType == TYPE_HIRAGANA || textType == TYPE_KATAKANA) {
            List<String> translationList = ConvertFragment.getLatinHiraganaKatakana(inputQuery.replace(" ", ""));
            inputQuery = translationList.get(0);
        }
        //endregion

        for (int i = 0; i < ConjugationSearchMatchingVerbRowColIndexList.size(); i++) {

            Word currentWord = mJapaneseToolboxCentralRoomDatabase.getWordByWordId(ConjugationSearchMatchingVerbRowColIndexList.get(i)[0]);
            if (currentWord==null) continue;

            int ranking = Utilities.getRankingFromWordAttributes(currentWord, inputQuery, queryWordWithoutTo, queryIsVerbWithTo);

            long[] currentMatchingWordIndexLengthAndCol = new long[3];
            currentMatchingWordIndexLengthAndCol[0] = i;
            currentMatchingWordIndexLengthAndCol[1] = ranking;
            currentMatchingWordIndexLengthAndCol[2] = ConjugationSearchMatchingVerbRowColIndexList.get(i)[1];

            matchingVerbIndexesLengthsAndCols.add(currentMatchingWordIndexLengthAndCol);
        }

        //Sort the results according to total length
        if (matchingVerbIndexesLengthsAndCols.size() != 0) {
            matchingVerbIndexesLengthsAndCols = Utilities.bubbleSortForThreeIntegerList(matchingVerbIndexesLengthsAndCols);
        }

        //Return the sorted list
        List<long[]> sortedList = new ArrayList<>();
        for (int i = 0; i < matchingVerbIndexesLengthsAndCols.size(); i++) {
            long sortedIndex = matchingVerbIndexesLengthsAndCols.get(i)[0];
            sortedList.add(ConjugationSearchMatchingVerbRowColIndexList.get((int) sortedIndex));
        }

        return sortedList;

    }
    private List<Verb> getVerbs(List<long[]> mMatchingVerbIdsAndCols) {

        if (mMatchingVerbIdsAndCols.size() == 0) return new ArrayList<>();

        //region Initializations
        List<Verb> verbs = new ArrayList<>();
        Word currentWord;
        Verb currentVerb;
        long matchingVerbId;
        int conjLength;
        int NumberOfSheetCols = mVerbLatinConjDatabase.get(0).length;
        String[] currentConjugationsRowLatin;
        String[] currentConjugationsRowKanji;
        List<Verb.ConjugationCategory> conjugationCategories;
        Verb.ConjugationCategory conjugationCategory;
        List<Verb.ConjugationCategory.Conjugation> conjugations;
        Verb.ConjugationCategory.Conjugation conjugation;
        List<String> conjugationSetLatin;
        List<String> conjugationSetKanji;
        List<ConjugationTitle.Subtitle> subtitles;
        int currentFamilyConjugationsIndex;
        String[] currentConjugationExceptionsRowLatin;
        String[] currentConjugationExceptionsRowKanji;
        //endregion

        //region Finding the Passive tense index in order to remove passive conjugations from verbs that are intransitive
        String currentTitle;
        int passiveTenseCategoryIndex = 0;
        for (int i = 0; i < mConjugationTitles.size(); i++) {
            currentTitle = mConjugationTitles.get(i).getTitle();
            if (currentTitle.contains("Passive (X is done to him)")) {
            }
        }
        //endregion

        //region Updating the verbs with their conjugations
        for (int p = 0; p < mMatchingVerbIdsAndCols.size(); p++) {
            matchingVerbId = mMatchingVerbIdsAndCols.get(p)[0];
            currentVerb = mJapaneseToolboxCentralRoomDatabase.getVerbByVerbId(matchingVerbId);
            currentWord = mJapaneseToolboxCentralRoomDatabase.getWordByWordId(matchingVerbId);
            if (currentWord == null || currentVerb == null
                    || !mFamilyConjugationIndexes.containsKey(currentVerb.getFamily())) continue;
            currentFamilyConjugationsIndex = mFamilyConjugationIndexes.get(currentVerb.getFamily());
            currentConjugationsRowLatin = Arrays.copyOf(mVerbLatinConjDatabase.get(currentFamilyConjugationsIndex), NumberOfSheetCols);
            currentConjugationsRowKanji = Arrays.copyOf(mVerbKanjiConjDatabase.get(currentFamilyConjugationsIndex), NumberOfSheetCols);

            //region Setting the verb's basic characteristics for display
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i< currentWord.getMeaningsEN().size(); i++) {
                if (i != 0) stringBuilder.append(", ");
                stringBuilder.append(currentWord.getMeaningsEN().get(i).getMeaning());
            }
            currentVerb.setMeaning(stringBuilder.toString());

            switch (currentVerb.getTrans()) {
                case "T": currentVerb.setTrans("trans."); break;
                case "I": currentVerb.setTrans("intrans."); break;
                case "T/I": currentVerb.setTrans("trans./intrans."); break;
            }

            if (GlobalConstants.VERB_FAMILIES_FULL_NAME_MAP.containsKey(currentVerb.getFamily())) {
                currentVerb.setFamily(VERB_FAMILIES_FULL_NAME_MAP.get(currentVerb.getFamily()));
            }
            //endregion

            //region Getting the conjugations row
            currentConjugationExceptionsRowLatin = new String[NumberOfSheetCols];
            currentConjugationExceptionsRowKanji = new String[NumberOfSheetCols];
            int indexOfExceptionConjugations = Integer.valueOf(currentVerb.getExceptionIndex());

            if (indexOfExceptionConjugations != currentFamilyConjugationsIndex) {
                currentConjugationExceptionsRowLatin = Arrays.copyOf(mVerbLatinConjDatabase.get(indexOfExceptionConjugations), NumberOfSheetCols);
                currentConjugationExceptionsRowKanji = Arrays.copyOf(mVerbKanjiConjDatabase.get(indexOfExceptionConjugations), NumberOfSheetCols);
            }
            else {
                Arrays.fill(currentConjugationExceptionsRowLatin, "");
                Arrays.fill(currentConjugationExceptionsRowKanji, "");
            }

            for (int col = COLUMN_VERB_ISTEM; col < NumberOfSheetCols; col++) {

                if (!currentConjugationExceptionsRowLatin[col].equals("")) currentConjugationsRowLatin[col] = currentConjugationExceptionsRowLatin[col];
                else {
                    conjLength = currentConjugationsRowLatin[col].length();
                    if (conjLength > 3 && currentConjugationsRowLatin[col].substring(0, 3).equals("(o)")) {
                        currentConjugationsRowLatin[col] = "(o)" + currentVerb.getActiveLatinRoot() + currentConjugationsRowLatin[col].substring(3, conjLength);
                    } else {
                        currentConjugationsRowLatin[col] = currentVerb.getActiveLatinRoot() + currentConjugationsRowLatin[col];
                    }
                }

                if (!currentConjugationExceptionsRowKanji[col].equals("")) currentConjugationsRowKanji[col] = currentConjugationExceptionsRowKanji[col];
                else {
                    conjLength = currentConjugationsRowKanji[col].length();
                    if (conjLength > 3 && currentConjugationsRowKanji[col].substring(0, 3).equals("(お)")) {
                        currentConjugationsRowKanji[col] = "(お)" + currentVerb.getActiveKanjiRoot() + currentConjugationsRowKanji[col].substring(3, conjLength);
                    } else {
                        currentConjugationsRowKanji[col] = currentVerb.getActiveKanjiRoot() + currentConjugationsRowKanji[col];
                    }
                }
            }
            //endregion

            //region Getting the verb conjugations and putting each conjugation of the conjugations row into its appropriate category
            conjugationCategories = new ArrayList<>();
            for (int categoryIndex = 1; categoryIndex < mConjugationTitles.size(); categoryIndex++) {

                //region Getting the set of Latin and Kanji conjugations according to the current category's subtitle column indexes
                subtitles = mConjugationTitles.get(categoryIndex).getSubtitles();
                int subtitleColIndex;
                conjugationSetLatin = new ArrayList<>();
                conjugationSetKanji = new ArrayList<>();
                for (int i=0; i<subtitles.size(); i++) {
                    subtitleColIndex = subtitles.get(i).getSubtitleIndex();
                    conjugationSetLatin.add(currentConjugationsRowLatin[subtitleColIndex]);
                    conjugationSetKanji.add(currentConjugationsRowKanji[subtitleColIndex]);
                }
                //endregion

                //region Intransitive verbs don't have a passive tense, so the relevant entries are removed
//                    if (!currentVerb.getTrans().equals("T") && categoryIndex == passiveTenseCategoryIndex) {
//                        for (int conjugationIndex = 0; conjugationIndex < conjugationSetLatin.size(); conjugationIndex++) {
//                            conjugationSetLatin.set(conjugationIndex, "*");
//                            conjugationSetKanji.set(conjugationIndex, "*");
//                        }
//                    }
                //endregion

                //region Cleaning the entries that contain exceptions
                for (int conjugationIndex = 0; conjugationIndex < conjugationSetLatin.size(); conjugationIndex++) {
                    if (conjugationSetLatin.get(conjugationIndex).contains("*"))
                        conjugationSetLatin.set(conjugationIndex, "*");
                    if (conjugationSetKanji.get(conjugationIndex).contains("*"))
                        conjugationSetKanji.set(conjugationIndex, "*");
                }
                //endregion

                //region Adding the conjugations to the conjugationCategory
                conjugationCategory = new Verb.ConjugationCategory();
                conjugations = new ArrayList<>();
                for (int conjugationIndex = 0; conjugationIndex < conjugationSetLatin.size(); conjugationIndex++) {
                    conjugation = new Verb.ConjugationCategory.Conjugation();
                    conjugation.setConjugationLatin(conjugationSetLatin.get(conjugationIndex));
                    conjugation.setConjugationKanji(conjugationSetKanji.get(conjugationIndex));
                    conjugations.add(conjugation);
                }
                conjugationCategory.setConjugations(conjugations);
                //endregion

                conjugationCategories.add(conjugationCategory);
            }
            //endregion

            currentVerb.setConjugationCategories(conjugationCategories);

            verbs.add(currentVerb);

            //Clearing the active fields since they're not needed anymore
            mJapaneseToolboxCentralRoomDatabase.updateVerbByVerbIdWithParams(
                    matchingVerbId,
                    "",
                    "",
                    ""
            );
        }
        //endregion

        return verbs;
    }
    private boolean IsOfTypeIngIng(String verb) {
        boolean answer = false;
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
}