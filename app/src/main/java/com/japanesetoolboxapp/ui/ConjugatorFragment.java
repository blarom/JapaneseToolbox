package com.japanesetoolboxapp.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.japanesetoolboxapp.R;
import com.japanesetoolboxapp.data.ConjugationTitle;
import com.japanesetoolboxapp.data.DatabaseUtilities;
import com.japanesetoolboxapp.data.Verb;
import com.japanesetoolboxapp.resources.GlobalConstants;
import com.japanesetoolboxapp.resources.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class ConjugatorFragment extends Fragment {
	
    //region Parameters
    @BindView(R.id.verb_chooser_spinner_container) RelativeLayout mVerbChooserSpinnerContainer;
    @BindView(R.id.verb_chooser_spinner) Spinner mVerbChooserSpinner;
    @BindView(R.id.conjugations_chooser_spinner) Spinner mConjugationChooserSpinner;
    @BindView(R.id.verb_hint) TextView mVerbHintTextView;
    @BindView(R.id.conjugations_container) ScrollView mConjugationsContainerScrollView;
    @BindView(R.id.radio_romaji_or_kanji) RadioGroup mRomajiOrKanjiRadioButton;
    @BindView(R.id.radio_Romaji) RadioButton mRomajiRadioButton;
    @BindView(R.id.radio_Kanji) RadioButton mKanjiRadioButton;

    @BindView(R.id.Tense0) TextView mConjugationDisplayTense0;
    @BindView(R.id.Tense1) TextView mConjugationDisplayTense1;
    @BindView(R.id.Tense2) TextView mConjugationDisplayTense2;
    @BindView(R.id.Tense3) TextView mConjugationDisplayTense3;
    @BindView(R.id.Tense4) TextView mConjugationDisplayTense4;
    @BindView(R.id.Tense5) TextView mConjugationDisplayTense5;
    @BindView(R.id.Tense6) TextView mConjugationDisplayTense6;
    @BindView(R.id.Tense7) TextView mConjugationDisplayTense7;
    @BindView(R.id.Tense8) TextView mConjugationDisplayTense8;
    @BindView(R.id.Tense9) TextView mConjugationDisplayTense9;
    @BindView(R.id.Tense10) TextView mConjugationDisplayTense10;
    @BindView(R.id.Tense11) TextView mConjugationDisplayTense11;
    @BindView(R.id.Tense12) TextView mConjugationDisplayTense12;
    @BindView(R.id.Tense13) TextView mConjugationDisplayTense13;

    @BindView(R.id.TenseLayout0) LinearLayout mConjugationDisplayTenseLayout0;
    @BindView(R.id.TenseLayout1) LinearLayout mConjugationDisplayTenseLayout1;
    @BindView(R.id.TenseLayout2) LinearLayout mConjugationDisplayTenseLayout2;
    @BindView(R.id.TenseLayout3) LinearLayout mConjugationDisplayTenseLayout3;
    @BindView(R.id.TenseLayout4) LinearLayout mConjugationDisplayTenseLayout4;
    @BindView(R.id.TenseLayout5) LinearLayout mConjugationDisplayTenseLayout5;
    @BindView(R.id.TenseLayout6) LinearLayout mConjugationDisplayTenseLayout6;
    @BindView(R.id.TenseLayout7) LinearLayout mConjugationDisplayTenseLayout7;
    @BindView(R.id.TenseLayout8) LinearLayout mConjugationDisplayTenseLayout8;
    @BindView(R.id.TenseLayout9) LinearLayout mConjugationDisplayTenseLayout9;
    @BindView(R.id.TenseLayout10) LinearLayout mConjugationDisplayTenseLayout10;
    @BindView(R.id.TenseLayout11) LinearLayout mConjugationDisplayTenseLayout11;
    @BindView(R.id.TenseLayout12) LinearLayout mConjugationDisplayTenseLayout12;
    @BindView(R.id.TenseLayout13) LinearLayout mConjugationDisplayTenseLayout13;

    @BindView(R.id.Tense0_Result) TextView mConjugationDisplayTenseResult0;
    @BindView(R.id.Tense1_Result) TextView mConjugationDisplayTenseResult1;
    @BindView(R.id.Tense2_Result) TextView mConjugationDisplayTenseResult2;
    @BindView(R.id.Tense3_Result) TextView mConjugationDisplayTenseResult3;
    @BindView(R.id.Tense4_Result) TextView mConjugationDisplayTenseResult4;
    @BindView(R.id.Tense5_Result) TextView mConjugationDisplayTenseResult5;
    @BindView(R.id.Tense6_Result) TextView mConjugationDisplayTenseResult6;
    @BindView(R.id.Tense7_Result) TextView mConjugationDisplayTenseResult7;
    @BindView(R.id.Tense8_Result) TextView mConjugationDisplayTenseResult8;
    @BindView(R.id.Tense9_Result) TextView mConjugationDisplayTenseResult9;
    @BindView(R.id.Tense10_Result) TextView mConjugationDisplayTenseResult10;
    @BindView(R.id.Tense11_Result) TextView mConjugationDisplayTenseResult11;
    @BindView(R.id.Tense12_Result) TextView mConjugationDisplayTenseResult12;
    @BindView(R.id.Tense13_Result) TextView mConjugationDisplayTenseResult13;

    static int rowIndex_of_suru;
    static int rowIndex_of_kuru;
    static int rowIndex_of_suru_in_Conj;
    static String[] row_values_suru_latin;
    static String[] row_values_suru_kanji;
    private Unbinder mBinding;
    private String mInputQuery;
    private List<List<Integer>> mMatchingVerbRowColIndexList;
    private List<Integer> mMatchingVerbRowIndexList;
    private List<Integer> mMatchingVerbColIndexList;
    private int mSelectedConjugationCategoryIndex;
    private String mChosenRomajiOrKanji;
    private List<Verb> mMatchingVerbs;
    private List<ConjugationTitle> mConjugationTitles;
    private String mInputQueryTextType;
    private List<String> mInputQueryTransliterations;
    private boolean mInputQueryIsLatin;
    private boolean mInputQueryIsKana;
    private boolean mInputQueryIsKanji;
    private boolean mInputQueryIsInvalid;
    private boolean mInputQueryTransliterationIsInvalid;
    private String mInputQueryTransliteratedToLatin;
    private String mTransliterationRelevantForSearch;
    //endregion


    //Lifecycle Functions
    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getExtras();
        initializeParameters();
    }
    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        setRetainInstance(true);
        View rootView = inflater.inflate(R.layout.fragment_conjugator, container, false);

        mBinding = ButterKnife.bind(this, rootView);

        if (savedInstanceState==null) {
            if (!TextUtils.isEmpty(mInputQuery)) {
                setInputQueryParameters();
                SearchForConjugations();
            }
        }

        //If the fragment is being resumed, just reload the saved data
        else {
            mMatchingVerbRowIndexList = savedInstanceState.getIntegerArrayList(getString(R.string.saved_matching_verbs));
            mMatchingVerbs = getVerbs(mMatchingVerbRowIndexList);
            displayVerbsInVerbChooserSpinner();
        }

        return rootView;
    }
    @Override public void onResume() {
        super.onResume();
        displayVerbsInVerbChooserSpinner();
    }
    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntegerArrayList(getString(R.string.saved_matching_verbs), new ArrayList<>(mMatchingVerbRowIndexList));
    }
    @Override public void onPause() {
        super.onPause();
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }


	//Functionality Functions
    private void getExtras() {
        if (getArguments()!=null) {
            mInputQuery = getArguments().getString(getString(R.string.user_query_word));
        }
    }
    private void initializeParameters() {

        mMatchingVerbs = new ArrayList<>();
    }
    public void SearchForConjugations() {

        mVerbChooserSpinnerContainer.setVisibility(View.GONE);
        mConjugationsContainerScrollView.setVisibility(View.GONE);
        mConjugationTitles = getConjugationTitles();
        getIndexesOfMatchingVerbs();
        mMatchingVerbs = getVerbs(mMatchingVerbRowIndexList);
        displayVerbsInVerbChooserSpinner();

    }
    private List<ConjugationTitle> getConjugationTitles() {

        String[] titlesRow = MainActivity.VerbLatinConjDatabase.get(0);
        String[] subtitlesRow = MainActivity.VerbLatinConjDatabase.get(1);
        String[] endingsRow = MainActivity.VerbLatinConjDatabase.get(2);
        int sheetLength = titlesRow.length;
        List<ConjugationTitle> conjugationTitles = new ArrayList<>();
        List<ConjugationTitle.Subtitle> subtitles = new ArrayList<>();
        ConjugationTitle conjugationTitle = new ConjugationTitle();

        for (int col = 0; col < sheetLength; col++) {

            if (col == 0) {
                conjugationTitle.setTitle(titlesRow[col]);
                conjugationTitle.setTitleIndex(col);

                ConjugationTitle.Subtitle subtitle = new ConjugationTitle.Subtitle();
                subtitle.setSubtitle(subtitlesRow[col]);
                subtitle.setSubtitleIndex(col);
                subtitles.add(subtitle);
            }
            else if (col == sheetLength -1) {
                conjugationTitle.setSubtitles(subtitles);
                conjugationTitles.add(conjugationTitle);
            }
            else {
                if (!titlesRow[col].equals("")) {

                    conjugationTitle.setSubtitles(subtitles);
                    conjugationTitles.add(conjugationTitle);

                    conjugationTitle = new ConjugationTitle();
                    subtitles = new ArrayList<>();

                    conjugationTitle.setTitle(titlesRow[col]);
                    conjugationTitle.setTitleIndex(col);

                }

                ConjugationTitle.Subtitle subtitle = new ConjugationTitle.Subtitle();
                subtitle.setSubtitle(subtitlesRow[col]);
                subtitle.setEnding(endingsRow[col]);
                subtitle.setSubtitleIndex(col);
                subtitles.add(subtitle);
            }
        }

        return conjugationTitles;
    }
    private void setInputQueryParameters() {

        //Converting the word to lowercase (the search algorithm is not efficient if needing to search both lower and upper case)
        mInputQuery = mInputQuery.toLowerCase(Locale.ENGLISH);

        mInputQueryTransliterations = ConvertFragment.Kana_to_Romaji_to_Kana(mInputQuery);
        mInputQueryTransliteratedToLatin = mInputQueryTransliterations.get(0);

        String transliterationRelevantForSearch = "";
        mInputQueryTextType = ConvertFragment.TextType(mInputQuery);
        mInputQueryIsLatin = mInputQueryTextType.equals("latin");
        mInputQueryIsKana = mInputQueryTextType.equals("hiragana") || mInputQueryTextType.equals("katakana");
        mInputQueryIsKanji = mInputQueryTextType.equals("kanji");
        mInputQueryIsInvalid =  mInputQuery.contains("*") || mInputQuery.contains("＊") || mInputQuery.equals("");
        mInputQueryTransliterationIsInvalid =  transliterationRelevantForSearch.contains("*") || transliterationRelevantForSearch.contains("＊");

        mTransliterationRelevantForSearch = "";
        if (mInputQueryIsLatin) mTransliterationRelevantForSearch = mInputQueryTransliterations.get(1);
        if (mInputQueryIsKana)  mTransliterationRelevantForSearch = mInputQueryTransliterations.get(0);
        if (mInputQueryIsKanji) mTransliterationRelevantForSearch = mInputQueryTransliterations.get(1);
    }
    private void getIndexesOfMatchingVerbs() {

        // Get the row index of the verbs matching the user's entry
        mMatchingVerbRowColIndexList = FindMatchingVerbIndex();
        mMatchingVerbRowIndexList = mMatchingVerbRowColIndexList.get(0);
        mMatchingVerbColIndexList = mMatchingVerbRowColIndexList.get(1);
    }
    public List<List<Integer>> FindMatchingVerbIndex() {

        //region Value Initializations
        List<Integer> matchingVerbRowIndexList = new ArrayList<>();
        List<Integer> matchingVerbColIndexList = new ArrayList<>();
        List<Integer> ConjugationSearchMatchingVerbRowIndexList = new ArrayList<>();
        List<Integer> ConjugationSearchMatchingVerbColIndexList = new ArrayList<>();
        List<List<Integer>> matchingVerbRowColIndexList = new ArrayList<>();
        List<String> suru_conjugation_verb =  new ArrayList<>();
        boolean found_match;
        boolean skip_this_row;
        boolean verbIsAConjugationOfTheCurrentFamily = false;
        boolean verb_is_suru = false;
        boolean verb_is_a_family_conjugation = false;
        boolean suru_conjugation_is_also_a_verb = false;
        boolean first_letter_is_identical;
        boolean valueEnglishIsEmpty;
        int NumberOfSheetCols = MainActivity.VerbLatinConjDatabase.get(0).length;
        int verb_length = mInputQuery.length();
        int matchColumn = 0;
        int value_Exception_asInt;
        String verb2;
        String verbToBeProcessed;
        String suru_conjugation;
        String concatenated_value;
        String[] currentRowValuesLatin;
        String[] currentRowValuesKanji;
        String[] owValuesCurrentFamily = new String[NumberOfSheetCols];
        String[] row_values_current_exceptionLatin = new String[NumberOfSheetCols];
        String[] row_values_current_exceptionKanji = new String[NumberOfSheetCols];
        String valueFamily;
        String valueEnglish;
        String value_Romaji;
        String value_Kana;
        String value_Kanji;
        String value_rootLatin;
        String value_rootKanji;
        String value_Exception;
        String value;
        List<Integer> diluted_columns_true_verb = new ArrayList<>();
        List<Integer> diluted_columns_no_conjugations = new ArrayList<>();
        List<Integer> diluted_columns;
        int max_length;
        int current_length;
        String inputQuery = mInputQuery;
        //endregion

        // Starting the algorithm
        if (!mInputQueryIsInvalid) {

            // 1. Initial Setup

            ;// Finding the index of the last non-empty row, to make sure the algorithm does not search on empty last rows
            int lastIndex = MainActivity.VerbDatabase.size()-1;
            while (MainActivity.VerbDatabase.get(lastIndex)[0].length() == 0) { lastIndex--; }

            //region Removing the "ing" from the verb (if relevant)
            if (verb_length > 2 && inputQuery.substring(verb_length-3).equals("ing")) {

                if (verb_length > 5 && inputQuery.substring(verb_length-6).equals("inging")) {
                    if (	(inputQuery.substring(0, 2+1).equals("to ") && IsOfTypeIngIng(inputQuery.substring(3,verb_length))) ||
                            (!inputQuery.substring(0, 2+1).equals("to ") && IsOfTypeIngIng(inputQuery.substring(0,verb_length)))   ) {
                        // If the verb ends with "inging" then remove the the second "ing"
                        inputQuery = inputQuery.substring(0,verb_length-3);
                    }
                }
                else {
                    verb2 = inputQuery + "ing";
                    if ((!verb2.substring(0, 2 + 1).equals("to ") || !IsOfTypeIngIng(verb2.substring(3, verb_length + 3))) &&
                            (verb2.substring(0, 2+1).equals("to ") || !IsOfTypeIngIng(verb2.substring(0, verb_length + 3)))) {
                        // If the verb does not belong to the list, then remove the ending "ing" so that it can be compared later on to the verbs excel
                        ;//If the verb is for e.g. to sing / sing (verb2 = to singing / singing), then check that verb2 (without the "to ") belongs to the list, and if it does then do nothing

                        inputQuery = inputQuery.substring(0,verb_length-3);
                    }
                }
            }
            //endregion

            //region Concatenating the verb & its translation for future use
            String concatenated_verb = Utilities.removeSpecialCharacters(inputQuery);
            String concatenated_translation = Utilities.removeSpecialCharacters(mTransliterationRelevantForSearch);
            int concatenated_verb_length = concatenated_verb.length();
            int concatenated_translation_length = concatenated_translation.length();
            //endregion

            //region Taking care of the case where the input is a basic conjugation that will cause the app to return all verbs
            String[] title_row = new String[NumberOfSheetCols];
            List<String> parsedConj;
            boolean verb_is_conjoined = false;
            verbToBeProcessed = inputQuery;
            String current_conjugation;
            if (mInputQueryIsLatin) {
                title_row = MainActivity.VerbLatinConjDatabase.get(2);
            }
            else if (mInputQueryIsKana) {
                title_row = MainActivity.VerbLatinConjDatabase.get(2);
                verbToBeProcessed = mTransliterationRelevantForSearch;
            }
            else if (mInputQueryIsKanji) {
                title_row = MainActivity.VerbKanjiConjDatabase.get(2);

            }
            int verb_to_be_processed_length = verbToBeProcessed.length();
            for (int column = GlobalConstants.VerbModule_colIndex_istem; column < NumberOfSheetCols; column++) {
                parsedConj = Arrays.asList(title_row[column].split("/"));
                for (int i=0;i<parsedConj.size();i++) {
                    current_conjugation = parsedConj.get(i);
                    if (verbToBeProcessed.equals(current_conjugation) && verb_to_be_processed_length > 3) {
                        verb_is_conjoined = true;
                    }
                }
            }
            //endregion

            //region Performing column dilution in order to make the search more efficient (the diluted column ArrayList is used in the Search Algorithm)
            current_length = 0;
            List<String[]> mySheetLengths = new ArrayList<>();
            if (mInputQueryIsLatin) {
                mySheetLengths = DatabaseUtilities.readCSVFileFirstRow("LineVerbsLengths - 3000 kanji.csv", getContext());
                current_length = concatenated_verb_length;
            }
            else if (mInputQueryIsKana) {
                mySheetLengths = DatabaseUtilities.readCSVFileFirstRow("LineVerbsLengths - 3000 kanji.csv", getContext());
                current_length = concatenated_translation_length;
            }
            else if (mInputQueryIsKanji) {
                mySheetLengths = DatabaseUtilities.readCSVFileFirstRow("LineVerbsKanjiLengths - 3000 kanji.csv", getContext());
                current_length = concatenated_verb_length;
            }
            for (int col = GlobalConstants.VerbModule_colIndex_istem; col < NumberOfSheetCols; col++) {
                if (!mySheetLengths.get(0)[col].equals("")) { max_length = Integer.parseInt(mySheetLengths.get(0)[col]); }
                else { max_length = 0; }
                if (max_length >= current_length) { diluted_columns_true_verb.add(col); }
            }
            //endregion

            //region Checking to see if the verb is suru
            rowIndex_of_suru = 0;
            rowIndex_of_kuru = 0;
            for (int current_index = 3; current_index < MainActivity.VerbDatabase.size(); current_index++) {
                value_Romaji = MainActivity.VerbDatabase.get(current_index)[GlobalConstants.VerbModule_colIndex_ustem];
                if (value_Romaji.equals("suru")) { rowIndex_of_suru = current_index; break;}
            }
            rowIndex_of_suru_in_Conj = Integer.valueOf(MainActivity.VerbDatabase.get(rowIndex_of_suru-1)[GlobalConstants.VerbModule_colIndex_istem]); //The conjugations are taken from the family row
            row_values_suru_latin = MainActivity.VerbLatinConjDatabase.get(rowIndex_of_suru_in_Conj);
            row_values_suru_kanji = MainActivity.VerbKanjiConjDatabase.get(rowIndex_of_suru_in_Conj);

            String concatenated_suru_conjugation;
            for (int column:diluted_columns_true_verb) {
                suru_conjugation = row_values_suru_latin[column];
                concatenated_suru_conjugation = suru_conjugation.replace(" ", "");
                if (concatenated_verb.equals(concatenated_suru_conjugation) || concatenated_translation.equals(concatenated_suru_conjugation)) {
                    verb_is_suru = true;
                    matchColumn = column;
                    for (int row = 3; row< MainActivity.VerbDatabase.size(); row++) {
                        currentRowValuesLatin = MainActivity.VerbDatabase.get(row);
                        value_Romaji = currentRowValuesLatin[GlobalConstants.VerbModule_colIndex_ustem];
                        value_Kanji = currentRowValuesLatin[GlobalConstants.VerbModule_colIndex_kanji];
                        if (suru_conjugation.equals(value_Romaji)) {
                            suru_conjugation_is_also_a_verb = true;
                            suru_conjugation_verb.add(value_Kanji);
                        }
                    }
                }
            }
            //endregion

            // 2. Search algorithm
            if (verb_is_suru) {
                for (int currentIndex=3; currentIndex < lastIndex+1; currentIndex++) {
                    currentRowValuesLatin = MainActivity.VerbDatabase.get(currentIndex);
                    value_Romaji = currentRowValuesLatin[GlobalConstants.VerbModule_colIndex_ustem];
                    if (value_Romaji.equals("suru")) {
                        ConjugationSearchMatchingVerbRowIndexList.add(currentIndex);
                        ConjugationSearchMatchingVerbColIndexList.add(matchColumn);
                    }
                }
            }
            if (!verb_is_suru || (verb_is_suru && suru_conjugation_is_also_a_verb)) {

                currentRowValuesLatin = MainActivity.VerbLatinConjDatabase.get(MainActivity.VerbLatinConjDatabase.size()-1);
                currentRowValuesKanji = MainActivity.VerbKanjiConjDatabase.get(MainActivity.VerbKanjiConjDatabase.size()-1);

                // Checking if the verb is a family conjugation
                for (int rowIndex = 3; rowIndex < lastIndex + 1; rowIndex++) {

                    for (int p=0; p<=GlobalConstants.VerbModule_colIndex_istem; p++) { //Padding the current_row_values with the values from the current index
                        currentRowValuesLatin[p] = MainActivity.VerbDatabase.get(rowIndex)[p];
                        currentRowValuesKanji[p] = MainActivity.VerbDatabase.get(rowIndex)[p];
                    }

                    valueEnglish = currentRowValuesLatin[GlobalConstants.VerbModule_colIndex_english];
                    valueFamily = currentRowValuesLatin[GlobalConstants.VerbModule_colIndex_family];

                    if (!valueFamily.equals("") && valueEnglish.equals("")) {
                        if (mInputQueryIsLatin) {
                            owValuesCurrentFamily = MainActivity.VerbLatinConjDatabase.get(Integer.valueOf(currentRowValuesLatin[GlobalConstants.VerbModule_colIndex_istem]));
                        } else {
                            owValuesCurrentFamily = MainActivity.VerbKanjiConjDatabase.get(Integer.valueOf(currentRowValuesKanji[GlobalConstants.VerbModule_colIndex_istem]));
                        }
                        for (int col=GlobalConstants.VerbModule_colIndex_istem; col<NumberOfSheetCols;col++) {
                            if (inputQuery.equals(owValuesCurrentFamily[col])) { verb_is_a_family_conjugation = true; break; }
                        }
                    }
                }

                for (int rowIndex = 3; rowIndex < lastIndex + 1; rowIndex++) {

                    //region Loop starting parameters initialization
                    found_match = false;
                    skip_this_row = true;
                    first_letter_is_identical = false;
                    valueEnglishIsEmpty = false;

                    for (int p=0; p<=GlobalConstants.VerbModule_colIndex_istem; p++) { //Padding the current_row_values with the values from the current index
                        currentRowValuesLatin[p] = MainActivity.VerbDatabase.get(rowIndex)[p];
                        currentRowValuesKanji[p] = MainActivity.VerbDatabase.get(rowIndex)[p];
                    }
                    //endregion

                    //region Extracting the cumulative english meanings
                    valueEnglish = currentRowValuesLatin[GlobalConstants.VerbModule_colIndex_english];
                    if (valueEnglish.equals("")) { valueEnglishIsEmpty = true; }

                    value_Kana = currentRowValuesLatin[GlobalConstants.VerbModule_colIndex_kana];
                    value_Kanji = currentRowValuesLatin[GlobalConstants.VerbModule_colIndex_kanji];
                    valueFamily = currentRowValuesLatin[GlobalConstants.VerbModule_colIndex_family];
                    value_Romaji = currentRowValuesLatin[GlobalConstants.VerbModule_colIndex_ustem];

                    value_rootKanji = currentRowValuesKanji[GlobalConstants.VerbModule_colIndex_rootKanji];
                    value_rootLatin = currentRowValuesLatin[GlobalConstants.VerbModule_colIndex_rootLatin];
                    //endregion

                    //region Add "to " to the values of valueEnglish (the "to "s were removed in the database to free space)
                    List<String> parsed_value = Arrays.asList(valueEnglish.split(","));
                    valueEnglish = "to ";
                    for (int k=0; k<parsed_value.size();k++) {
                        valueEnglish = valueEnglish + parsed_value.get(k).trim();
                        if (k<parsed_value.size()-1) {valueEnglish = valueEnglish + ", to ";}
                    }
                    //endregion

                    // Skipping empty/family rows
                    if (valueFamily.equals("") || valueFamily.equals("-")) { continue; }

                    //region Registering the current family row
                    if (valueEnglishIsEmpty) {

                        verbIsAConjugationOfTheCurrentFamily = false;
                        // Registering the row
                        if (mInputQueryIsLatin) {
                            owValuesCurrentFamily = MainActivity.VerbLatinConjDatabase.get(Integer.valueOf(currentRowValuesLatin[GlobalConstants.VerbModule_colIndex_istem]));
                            verbToBeProcessed = inputQuery;
                        } else if (mInputQueryIsKana) {
                            owValuesCurrentFamily = MainActivity.VerbLatinConjDatabase.get(Integer.valueOf(currentRowValuesLatin[GlobalConstants.VerbModule_colIndex_istem]));
                            verbToBeProcessed = mTransliterationRelevantForSearch;
                        } else {
                            owValuesCurrentFamily = MainActivity.VerbKanjiConjDatabase.get(Integer.valueOf(currentRowValuesKanji[GlobalConstants.VerbModule_colIndex_istem]));
                            verbToBeProcessed = inputQuery;
                        }
                        if (verb_is_a_family_conjugation) {
                            for (int col = GlobalConstants.VerbModule_colIndex_istem; col < NumberOfSheetCols; col++) {
                                if (verbToBeProcessed.equals(owValuesCurrentFamily[col])) {
                                    verbIsAConjugationOfTheCurrentFamily = true;
                                    matchColumn = col;
                                    break;
                                }
                            }
                        }

                        // Removing the particles in the columns to reflect user error in the input verb
                        //TODO Remove the particles in the columns to reflect user error in the input verb

                        // Removing the polite "o" in the columns
                        //TODO Remove the polite "o" in the columns

                        // Family rows are not relevant for searches
                    }
                    //endregion

                    //region If the verb appears as a conjoined verb in one of the conjugations, don't use the whole conjugations row
                    if (!verb_is_conjoined) { diluted_columns = diluted_columns_true_verb; }
                    else { diluted_columns = diluted_columns_no_conjugations; }
                    //endregion

                    //region Preventing conjugation searches on short verbs
                    if ( (mInputQueryIsLatin && verb_length < 3) || (mInputQueryIsKana  && verb_length < 2) || (mInputQueryIsKanji && verb_length < 2)) {
                        diluted_columns = diluted_columns_no_conjugations;
                    }
                    //endregion

                    //region Getting the exception row if relevant
                    value_Exception = currentRowValuesLatin[GlobalConstants.VerbModule_colIndex_istem];
                    if (!value_Exception.equals("")) {
                        value_Exception_asInt = Integer.valueOf(value_Exception);
                        row_values_current_exceptionLatin = MainActivity.VerbLatinConjDatabase.get(value_Exception_asInt);
                        row_values_current_exceptionKanji = MainActivity.VerbKanjiConjDatabase.get(value_Exception_asInt);
                    }
                    //endregion

                    //region Preventing searches on verbs that don't have the same first kana
                    if (!valueEnglishIsEmpty) {
                        if (        mInputQueryIsLatin
                                ||  mInputQueryIsKana  && (value_Kana.charAt(0) == inputQuery.charAt(0))
                                ||  mInputQueryIsKanji && value_Kanji.contains(inputQuery.substring(0,1))
                                ||  value_Romaji.equals("kuru") ||  value_Romaji.equals("da"))
                        { first_letter_is_identical = true; skip_this_row = false; }
                    }
                    //endregion

                    //region Including the rows for verbs starting in o or go
                    if (!valueEnglishIsEmpty) {
                        if (((mInputQueryIsLatin && verb_length > 2) || (mInputQueryIsKana && verb_length > 1) || (mInputQueryIsKanji && verb_length > 1))
                                && (value_Kanji.charAt(0) == 'お' || value_Kanji.charAt(0) == 'ご' || value_Kanji.charAt(0) == '御')) {
                            skip_this_row = false;
                        }
                    }
                    //endregion

                    //region If the verb is a suru conjugation but is also a valid verb, don't skip this row
                    if (verb_is_suru && suru_conjugation_is_also_a_verb) {
                        skip_this_row = true;
                        for (String suru_conj_as_verb:suru_conjugation_verb)
                            if (value_Kanji.equals(suru_conj_as_verb)) { skip_this_row = false; break;}
                        if (skip_this_row  || value_Romaji.equals("suru")) {continue;}
                    }
                    //endregion

                    //region If the verb is equal to a family conjugation, only roots with length 1 (ie. iru/aru/eru/oru/uru verbs only) or a verb with the exact romaji value are considered. This prevents too many results. This does not conflict with da or kuru.
                    if (verb_is_a_family_conjugation) {
                        if (mInputQueryIsKana || mInputQueryIsKanji) {
                            if (value_rootKanji.length() == 1 && verbIsAConjugationOfTheCurrentFamily) {
                                found_match = true;
                            }
                            if ((value_rootKanji.length() > 1 || !verbIsAConjugationOfTheCurrentFamily) && !first_letter_is_identical) {
                                skip_this_row = true;
                            }
                        }
                        else {
                            if (value_rootLatin.length() == 1 && verbIsAConjugationOfTheCurrentFamily) {
                                found_match = true;
                            }
                            if ((value_rootLatin.length() > 1 || !verbIsAConjugationOfTheCurrentFamily) && !first_letter_is_identical) {
                                skip_this_row = true;
                            }
                        }
                    }
                    //endregion

                    //region Main Comparator Algorithm
                    if (!skip_this_row && !found_match) {
                        if (mInputQueryIsLatin) {

                            // Check if there is a hit in the english column
                            if (verb_length > 3 && (valueEnglish.contains(concatenated_verb)
                                    || valueEnglish.contains("to "+concatenated_verb.substring(2,concatenated_verb.length())))) {
                                found_match = true;
                                matchColumn = GlobalConstants.VerbModule_colIndex_english;
                            }

                            // Check if there is a hit in the romaji column
                            concatenated_value = value_Romaji.replace(" ", "");
                            if (concatenated_value.contains(concatenated_verb)) {
                                found_match = true;
                                matchColumn = GlobalConstants.VerbModule_colIndex_ustem;
                            }

                            // Otherwise, go over the rest of the conjugations
                            if (!found_match) {
                                if (!value_Exception.equals("")) {
                                    for (int col : diluted_columns) {

                                        // If the value at the current index has conjugation exceptions, get those from corresponding row in the Conj sheet instead of the family row
                                        if (row_values_current_exceptionLatin[col].equals("")) {
                                            value = value_rootLatin + owValuesCurrentFamily[col];
                                        } else {
                                            value = row_values_current_exceptionLatin[col];
                                        }

                                        concatenated_value = value.replace(" ", "");
                                        if (concatenated_value.contains(concatenated_verb)) {
                                            found_match = true;
                                            matchColumn = col;
                                        }
                                        if (found_match) {
                                            break;
                                        }
                                    }
                                } else for (int col : diluted_columns) {

                                    value = value_rootLatin + owValuesCurrentFamily[col];

                                    concatenated_value = value.replace(" ", "");
                                    if (concatenated_value.contains(concatenated_verb)) {
                                        found_match = true;
                                        matchColumn = col;
                                    }
                                    if (found_match) {
                                        break;
                                    }
                                }
                            }

                        } else if (mInputQueryIsKana && !mInputQueryTransliterationIsInvalid) {

                            // Check if there is a hit in the romaji column
                            concatenated_value = value_Romaji.replace(" ", "");
                            if (concatenated_value.contains(concatenated_translation)) {
                                found_match = true;
                                matchColumn = GlobalConstants.VerbModule_colIndex_ustem;
                            }

                            // Otherwise, go over the rest of the conjugations
                            if (!found_match && !value_Exception.equals("")) {
                                for (int col : diluted_columns) {

                                    // If the value at the current index has conjugation exceptions, get those from corresponding row in the Conj sheet instead of the family row
                                    if (row_values_current_exceptionLatin[col].equals("")) {
                                        value = value_rootLatin + owValuesCurrentFamily[col];
                                    } else {
                                        value = row_values_current_exceptionLatin[col];
                                    }

                                    concatenated_value = value.replace(" ", "");
                                    if (concatenated_value.contains(concatenated_translation)) {
                                        found_match = true;
                                        matchColumn = col;
                                    }
                                    if (found_match) {
                                        break;
                                    }
                                }
                            } else {
                                for (int col : diluted_columns) {

                                    value = value_rootLatin + owValuesCurrentFamily[col];

                                    concatenated_value = value.replace(" ", "");
                                    if (concatenated_value.contains(concatenated_translation)) {
                                        found_match = true;
                                        matchColumn = col;
                                    }
                                    if (found_match) {
                                        break;
                                    }
                                }
                            }

                        } else if (mInputQueryIsKanji) {

                            // Check if there is a hit in the Kanji column
                            if (value_Kanji.contains(concatenated_verb)) {
                                found_match = true;
                                matchColumn = GlobalConstants.VerbModule_colIndex_kanji;
                            }

                            // Otherwise, go over the rest of the conjugations
                            if (!found_match && !value_Exception.equals("")) {
                                for (int col : diluted_columns) {

                                    // If the value at the current index has conjugation exceptions, get those from corresponding row in the Conj sheet instead of the family row
                                    if (row_values_current_exceptionKanji[col].equals("")) {
                                        value = value_rootKanji + owValuesCurrentFamily[col];
                                    } else {
                                        value = row_values_current_exceptionKanji[col];
                                    }

                                    concatenated_value = value.replace(" ", "");
                                    if (concatenated_value.contains(concatenated_verb)) {
                                        found_match = true;
                                        matchColumn = col;
                                    }
                                    if (found_match) {
                                        break;
                                    }
                                }
                            } else for (int col : diluted_columns) {

                                value = value_rootKanji + owValuesCurrentFamily[col];

                                concatenated_value = value.replace(" ", "");
                                if (concatenated_value.contains(concatenated_verb)) {
                                    found_match = true;
                                    matchColumn = col;
                                }
                                if (found_match) {
                                    break;
                                }
                            }
                        }
                    }
                    //endregion

                    if (found_match) {
                        ConjugationSearchMatchingVerbRowIndexList.add(rowIndex);
                        ConjugationSearchMatchingVerbColIndexList.add(matchColumn);
                    }
                }
            }


            //region 3. Sorting the results according to the length of the Kana and Kanji values

            ;// 3a. Computing the value length
            int current_row_index;
            int current_col_index;
            int current_romaji_length;
            int current_kanji_length;
            int list_size = ConjugationSearchMatchingVerbRowIndexList.size();
            int[][] total_length = new int[list_size][3];
            for (int i=0;i<list_size;i++) {
                current_row_index = ConjugationSearchMatchingVerbRowIndexList.get(i);
                current_col_index = ConjugationSearchMatchingVerbColIndexList.get(i);
                current_romaji_length = MainActivity.VerbDatabase.get(current_row_index)[GlobalConstants.VerbModule_colIndex_ustem].length();
                current_kanji_length = MainActivity.VerbDatabase.get(current_row_index)[GlobalConstants.VerbModule_colIndex_kanji].length();
                total_length[i][0] = current_row_index;
                total_length[i][1] = current_col_index;
                total_length[i][2] = current_romaji_length+current_kanji_length;
            }

            // 3b. Sorting
            int tempVar0;
            int tempVar1;
            int tempVar2;
            for (int i=0;i<list_size;i++) { //Bubble sort
                for (int t=1;t<list_size-i;t++) {
                    if (total_length[t-1][2] > total_length[t][2]) {
                        tempVar0 = total_length[t-1][0];
                        tempVar1 = total_length[t-1][1];
                        tempVar2 = total_length[t-1][2];
                        total_length[t-1][0] = total_length[t][0];
                        total_length[t-1][1] = total_length[t][1];
                        total_length[t-1][2] = total_length[t][2];
                        total_length[t][0] = tempVar0;
                        total_length[t][1] = tempVar1;
                        total_length[t][2] = tempVar2;
                    }
                }
            }

            // 3c. Creating an Arraylist with the sorted values
            List<Integer> ConjugationSearchMatchingVerbRowIndexList_Sorted = new ArrayList<>();
            List<Integer> ConjugationSearchMatchingVerbColIndexList_Sorted = new ArrayList<>();

            for (int i=0;i<list_size;i++) {
                ConjugationSearchMatchingVerbRowIndexList_Sorted.add(total_length[i][0]);
                ConjugationSearchMatchingVerbColIndexList_Sorted.add(total_length[i][1]);
            }
            //endregion


            // 4. Finalization
            matchingVerbRowIndexList = ConjugationSearchMatchingVerbRowIndexList_Sorted;
            matchingVerbColIndexList = ConjugationSearchMatchingVerbColIndexList_Sorted;
        }

        // Returning the list of matching row indexes
        matchingVerbRowColIndexList.add(matchingVerbRowIndexList);
        matchingVerbRowColIndexList.add(matchingVerbColIndexList);
        return matchingVerbRowColIndexList;
    }
    public Boolean IsOfTypeIngIng(String verb) {
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
    public static String insertToInMeanings(String definition) {
        definition = "to " + definition;
        char current_char;
        boolean parenthesis = false;
        boolean double_parenthesis = false;
        int definition_index = 0;
        if (!definition.equals("")) {
            for (int i=0;i<definition.length();i++) {
                if (definition_index<definition.length()) {
                    current_char = definition.charAt(definition_index);
                    if (current_char == '(') {
                        if (!double_parenthesis) {parenthesis = true;}
                        if (parenthesis) {double_parenthesis = true;}
                    }
                    if (current_char == ')') {
                        if (double_parenthesis) {double_parenthesis = false;}
                        if (!double_parenthesis) {parenthesis = false;}
                    }
                    if (current_char == ',' && !parenthesis) {
                        definition = definition.substring(0,definition_index+1) + " to" + definition.substring(definition_index+1,definition.length());
                        definition_index = definition_index+3;
                    }
                }
                definition_index++;
            }
        }
        return definition;
    }
    public List<Verb> getVerbs(List<Integer> matchingVerbRowIndexList) {

        //region Find the Passive tense index in order to remove passive conjugations from verbs that are intransitive
        String currentTitle;
        int passiveTenseCategoryIndex = 0;
        for (int i = 0; i < mConjugationTitles.size(); i++) {
            currentTitle = mConjugationTitles.get(i).getTitle();
            if (currentTitle.contains("Passive (X is done to him)")) { passiveTenseCategoryIndex = i; }
        }
        //endregion

        //region Building the set of verb characteristics for each matching verb
        List<Verb> verbs = new ArrayList<>();
        if (matchingVerbRowIndexList.size() != 0) {

            //region Initializations
            int matchingVerbRowIndex;
            int currentFamilyHeaderRowIndex = 0;
            int conjLatinLength;
            int conjKanjiLength;
            List<String[]> conjugationsSheetLatin = MainActivity.VerbLatinConjDatabase;
            List<String[]> conjugationsSheetKanji = MainActivity.VerbKanjiConjDatabase;
            int NumberOfSheetCols = conjugationsSheetLatin.get(0).length;
            String[] currentConjugationsRowLatin;
            String[] currentConjugationsRowKanji;
            String[] currentVerbRow;
            List<Verb.ConjugationCategory> conjugationCategories;
            Verb.ConjugationCategory conjugationCategory;
            List<Verb.ConjugationCategory.Conjugation> conjugations;
            Verb.ConjugationCategory.Conjugation conjugation;
            List<String> conjugationSetLatin;
            List<String> conjugationSetKanji;
            List<ConjugationTitle.Subtitle> subtitles;
            //endregion

            for (int p = 0; p < matchingVerbRowIndexList.size(); p++) {
                matchingVerbRowIndex = matchingVerbRowIndexList.get(p);
                currentVerbRow = MainActivity.VerbDatabase.get(matchingVerbRowIndex);

                //region Setting the verb's basic characteristics
                Verb currentVerb = new Verb(
                        currentVerbRow[GlobalConstants.VerbModule_colIndex_family],
                        currentVerbRow[GlobalConstants.VerbModule_colIndex_english],
                        currentVerbRow[GlobalConstants.VerbModule_colIndex_trans],
                        currentVerbRow[GlobalConstants.VerbModule_colIndex_prep],
                        currentVerbRow[GlobalConstants.VerbModule_colIndex_kana],
                        currentVerbRow[GlobalConstants.VerbModule_colIndex_kanji],
                        currentVerbRow[GlobalConstants.VerbModule_colIndex_ustem],
                        currentVerbRow[GlobalConstants.VerbModule_colIndex_rootKanji],
                        currentVerbRow[GlobalConstants.VerbModule_colIndex_rootLatin],
                        currentVerbRow[GlobalConstants.VerbModule_colIndex_exception],
                        currentVerbRow[GlobalConstants.VerbModule_colIndex_altSpellings]);


                currentVerb.setMeaning(insertToInMeanings(currentVerb.getMeaning()));
                switch (currentVerb.getTrans()) {
                    case "T":
                        currentVerb.setTrans("trans.");
                        break;
                    case "I":
                        currentVerb.setTrans("intrans.");
                        break;
                    case "T/I":
                        currentVerb.setTrans("trans./intrans.");
                        break;
                }
                //endregion

                //region Finding the conjugation family relevant to the current verb
                for (int i = 2; i < MainActivity.VerbDatabase.size(); i++) {
                    String[] currentRow = MainActivity.VerbDatabase.get(i);
                    if (currentRow[GlobalConstants.VerbModule_colIndex_rootLatin].equals("")
                            && currentRow[GlobalConstants.VerbModule_colIndex_english].equals("")
                            && !currentRow[GlobalConstants.VerbModule_colIndex_family].equals("")) {

                        if (i < matchingVerbRowIndex) {
                            currentFamilyHeaderRowIndex = i;
                        } else {
                            break;
                        }
                    }
                }

                String[] currentFamilyHeaderInVerbDatabase = MainActivity.VerbDatabase.get(currentFamilyHeaderRowIndex);
                int indexOfRelevantFamilyConjugations = Integer.valueOf(currentFamilyHeaderInVerbDatabase[GlobalConstants.VerbModule_colIndex_exception]);
                currentConjugationsRowLatin = conjugationsSheetLatin.get(indexOfRelevantFamilyConjugations);
                currentConjugationsRowKanji = conjugationsSheetKanji.get(indexOfRelevantFamilyConjugations);
                //endregion

                //region If the verb has a conjugation exceptions row, update the currentConjugationsRow with the nonempty exceptions in that row
                boolean isAnException = false;
                String valueExceptionIndicator = currentVerbRow[GlobalConstants.VerbModule_colIndex_exception];
                String[] currentConjugationExceptionsRowLatin = new String[NumberOfSheetCols];
                String[] currentConjugationExceptionsRowKanji = new String[NumberOfSheetCols];
                if (!valueExceptionIndicator.equals("")) {
                    int indexOfExceptionConjugations = Integer.valueOf(valueExceptionIndicator);
                    currentConjugationExceptionsRowLatin = conjugationsSheetLatin.get(indexOfExceptionConjugations);
                    currentConjugationExceptionsRowKanji = conjugationsSheetKanji.get(indexOfExceptionConjugations);
                    isAnException = true;
                    for (int col = 0; col < NumberOfSheetCols; col++) {

                        String currentConjugationExceptionLatin = currentConjugationExceptionsRowLatin[col];
                        if (!currentConjugationExceptionLatin.equals(""))
                            currentConjugationsRowLatin[col] = currentConjugationExceptionsRowLatin[col];

                        String currentConjugationExceptionKanji = currentConjugationExceptionsRowKanji[col];
                        if (!currentConjugationExceptionKanji.equals(""))
                            currentConjugationsRowKanji[col] = currentConjugationExceptionsRowKanji[col];
                    }
                }
                //endregion

                //region Getting the verb's conjugations row
                String[] matchingVerbConjugationsRowLatin = new String[NumberOfSheetCols];
                String[] matchingVerbConjugationsRowKanji = new String[NumberOfSheetCols];
                for (int col = 0; col < NumberOfSheetCols; col++) {

                    if (col < GlobalConstants.VerbModule_colIndex_istem) {
                        matchingVerbConjugationsRowLatin[col] = currentVerbRow[col];
                        matchingVerbConjugationsRowKanji[col] = currentVerbRow[col];
                    } else {

                        //If there is a conjugation exception then use it as-is, otherwise create the conjugation using the family conjugation endings
                        if (isAnException && !currentConjugationExceptionsRowLatin[col].equals("")) {
                            matchingVerbConjugationsRowLatin[col] = currentConjugationExceptionsRowLatin[col];
                            matchingVerbConjugationsRowKanji[col] = currentConjugationExceptionsRowKanji[col];
                        } else {
                            conjLatinLength = currentConjugationsRowLatin[col].length();
                            if (conjLatinLength > 3 && currentConjugationsRowLatin[col].substring(0, 3).equals("(o)")) {
                                matchingVerbConjugationsRowLatin[col] = "(o)" + currentVerb.getLatinRoot() + currentConjugationsRowLatin[col].substring(3, conjLatinLength);
                            } else
                                matchingVerbConjugationsRowLatin[col] = currentVerb.getLatinRoot() + currentConjugationsRowLatin[col];

                            conjKanjiLength = currentConjugationsRowKanji[col].length();
                            if (conjLatinLength > 3 && currentConjugationsRowLatin[col].substring(0, 3).equals("(お)")) {
                                matchingVerbConjugationsRowKanji[col] = "(お)" + currentVerb.getKanjiRoot() + currentConjugationsRowKanji[col].substring(3, conjKanjiLength);
                            } else
                                matchingVerbConjugationsRowKanji[col] = currentVerb.getKanjiRoot() + currentConjugationsRowKanji[col];
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
                        conjugationSetLatin.add(matchingVerbConjugationsRowLatin[subtitleColIndex]);
                        conjugationSetKanji.add(matchingVerbConjugationsRowKanji[subtitleColIndex]);
                    }
                    //endregion

                    //region Intransitive verbs don't have a passive tense, so the relevant entries are removed
                    if (!currentVerb.getTrans().equals("T") && categoryIndex == passiveTenseCategoryIndex) {
                        for (int conjugationIndex = 0; conjugationIndex < conjugationSetLatin.size(); conjugationIndex++) {
                            conjugationSetLatin.set(conjugationIndex, "*");
                            conjugationSetKanji.set(conjugationIndex, "*");
                        }
                    }
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
            }

        }

        return verbs;
    }

    private void displayVerbsInVerbChooserSpinner() {

        if (mMatchingVerbs.size() != 0) {
            mVerbHintTextView.setVisibility(View.GONE);
            mVerbChooserSpinner.setAdapter(new VerbSpinnerAdapter(getContext(), R.layout.custom_verbchooser_spinner, mMatchingVerbs));
            mVerbChooserSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1, int verbIndex, long id) {
                    showSelectedVerbConjugations(verbIndex);
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            });
            mVerbChooserSpinnerContainer.setVisibility(View.VISIBLE);
            mConjugationsContainerScrollView.setVisibility(View.VISIBLE);
        } else {
            mVerbHintTextView.setVisibility(View.VISIBLE);
            mVerbChooserSpinnerContainer.setVisibility(View.GONE);
            mConjugationsContainerScrollView.setVisibility(View.GONE);
        }

    }
    private void showSelectedVerbConjugations(final int verbIndex) {

        if (getActivity()!=null) Utilities.hideSoftKeyboard(getActivity());
        if (mMatchingVerbs.size() == 0) return;

        //Showing the verb conjugations
        Verb verb = mMatchingVerbs.get(verbIndex);
        List<Verb.ConjugationCategory> conjugationCategories = verb.getConjugationCategories();
        List<ConjugationTitle> conjugationTitles = new ArrayList<>(mConjugationTitles);
        conjugationTitles.remove(0);
        mConjugationChooserSpinner.setAdapter(new ConjugationsSpinnerAdapter(
                getContext(),
                R.layout.custom_conjugationchooser_spinner,
                conjugationCategories,
                conjugationTitles));
        mConjugationChooserSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, final int conjugationIndex, long id) {
                mSelectedConjugationCategoryIndex = conjugationIndex;
                showSelectedConjugationsInCategory(verbIndex, conjugationIndex);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        //Hiding the subsequent fields of there is nothing to show
        mConjugationsContainerScrollView.setVisibility(View.VISIBLE);
        if (verb.getConjugationCategories().size() == 0) {
            mConjugationsContainerScrollView.setVisibility(View.GONE);
        }


        //Setting the conjugation spinner to the position of the first item matching the user query
        List<Verb.ConjugationCategory.Conjugation> conjugations;
        int matchingConjugationCategoryIndex = 0;
        boolean foundMatch = false;
        for (int i=0; i<conjugationCategories.size(); i++) {
            conjugations = conjugationCategories.get(i).getConjugations();
            for (Verb.ConjugationCategory.Conjugation conjugation : conjugations) {
                if (mInputQueryIsLatin && conjugation.getConjugationLatin().equals(mInputQueryTransliteratedToLatin)) foundMatch = true;
                else if (mInputQueryIsKanji && conjugation.getConjugationKanji().equals(mInputQuery)) foundMatch = true;
                if (foundMatch) break;
            }
            if (foundMatch) {
                matchingConjugationCategoryIndex = i;
                break;
            }
        }
        mConjugationChooserSpinner.setSelection(matchingConjugationCategoryIndex);

        //Setting the conjugation spinner to the position that was previously selected
        //mConjugationChooserSpinner.setSelection(mSelectedConjugationCategoryIndex);
    }
    private void showSelectedConjugationsInCategory(final int verbIndex, final int conjugationIndex) {

        // Getting the user choice for displaying the conjugations in Romaji or Kanji

        mRomajiOrKanjiRadioButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (getActivity()==null) return;

                RadioButton checkedRadioButton = getActivity().findViewById(checkedId);

                // Define an action depending on the given boolean, ie. depending on the checked RadioButton ID
                mChosenRomajiOrKanji = "";
                switch (checkedRadioButton.getId()) {
                    case R.id.radio_Romaji:
                        if (checkedRadioButton.isChecked()) {
                            mChosenRomajiOrKanji = "Romaji";
                        }
                        break;
                    case R.id.radio_Kanji:
                        if (checkedRadioButton.isChecked()) {
                            mChosenRomajiOrKanji = "Kanji";
                        }
                        break;
                }

                displayConjugationsOfSelectedCategory(verbIndex, conjugationIndex);
            }
        });


        List<Boolean> types = FindType(mInputQuery);
        boolean mInputQueryIsKanji = types.get(2);

        mChosenRomajiOrKanji = "Romaji";
        if (mInputQueryIsKanji) {
            mChosenRomajiOrKanji = "Kanji";
            mRomajiRadioButton.setChecked(false);
            mKanjiRadioButton.setChecked(true);
        } else {
            mRomajiRadioButton.setChecked(true);
            mKanjiRadioButton.setChecked(false);
        }

        displayConjugationsOfSelectedCategory(verbIndex, conjugationIndex);
    }
    public void displayConjugationsOfSelectedCategory(int verbIndex, int conjugationIndex) {
        if (getActivity()==null) return;

        List<TextView> Tense = new ArrayList<>();
        List<LinearLayout> TenseLayout = new ArrayList<>();
        List<TextView> Tense_Result = new ArrayList<>();

        Tense.add(mConjugationDisplayTense0);
        Tense.add(mConjugationDisplayTense1);
        Tense.add(mConjugationDisplayTense2);
        Tense.add(mConjugationDisplayTense3);
        Tense.add(mConjugationDisplayTense4);
        Tense.add(mConjugationDisplayTense5);
        Tense.add(mConjugationDisplayTense6);
        Tense.add(mConjugationDisplayTense7);
        Tense.add(mConjugationDisplayTense8);
        Tense.add(mConjugationDisplayTense9);
        Tense.add(mConjugationDisplayTense10);
        Tense.add(mConjugationDisplayTense11);
        Tense.add(mConjugationDisplayTense12);
        Tense.add(mConjugationDisplayTense13);

        TenseLayout.add(mConjugationDisplayTenseLayout0);
        TenseLayout.add(mConjugationDisplayTenseLayout1);
        TenseLayout.add(mConjugationDisplayTenseLayout2);
        TenseLayout.add(mConjugationDisplayTenseLayout3);
        TenseLayout.add(mConjugationDisplayTenseLayout4);
        TenseLayout.add(mConjugationDisplayTenseLayout5);
        TenseLayout.add(mConjugationDisplayTenseLayout6);
        TenseLayout.add(mConjugationDisplayTenseLayout7);
        TenseLayout.add(mConjugationDisplayTenseLayout8);
        TenseLayout.add(mConjugationDisplayTenseLayout9);
        TenseLayout.add(mConjugationDisplayTenseLayout10);
        TenseLayout.add(mConjugationDisplayTenseLayout11);
        TenseLayout.add(mConjugationDisplayTenseLayout12);
        TenseLayout.add(mConjugationDisplayTenseLayout13);

        Tense_Result.add(mConjugationDisplayTenseResult0);
        Tense_Result.add(mConjugationDisplayTenseResult1);
        Tense_Result.add(mConjugationDisplayTenseResult2);
        Tense_Result.add(mConjugationDisplayTenseResult3);
        Tense_Result.add(mConjugationDisplayTenseResult4);
        Tense_Result.add(mConjugationDisplayTenseResult5);
        Tense_Result.add(mConjugationDisplayTenseResult6);
        Tense_Result.add(mConjugationDisplayTenseResult7);
        Tense_Result.add(mConjugationDisplayTenseResult8);
        Tense_Result.add(mConjugationDisplayTenseResult9);
        Tense_Result.add(mConjugationDisplayTenseResult10);
        Tense_Result.add(mConjugationDisplayTenseResult11);
        Tense_Result.add(mConjugationDisplayTenseResult12);
        Tense_Result.add(mConjugationDisplayTenseResult13);

        for (int i=0;i<Tense.size();i++) {
            Tense.get(i).setText("");
            TenseLayout.get(i).setVisibility(View.GONE);
            Tense_Result.get(i).setText("");
        }

        Verb verb = mMatchingVerbs.get(verbIndex);
        Verb.ConjugationCategory conjugationCategory = verb.getConjugationCategories().get(conjugationIndex);
        List<Verb.ConjugationCategory.Conjugation> conjugations = conjugationCategory.getConjugations();

        for (int i = 0; i < conjugations.size(); i++) {

            Tense.get(i).setText(mConjugationTitles.get(conjugationIndex+1).getSubtitles().get(i).getTense());

            if (mChosenRomajiOrKanji.equals("Romaji")) Tense_Result.get(i).setText(conjugations.get(i).getConjugationLatin());
            else Tense_Result.get(i).setText(conjugations.get(i).getConjugationKanji());

            TenseLayout.get(i).setVisibility(View.VISIBLE);
        }
    }
    private class VerbSpinnerAdapter extends ArrayAdapter<Verb> {
        // Code adapted from http://mrbool.com/how-to-customize-spinner-in-android/28286

        List<Verb> verbs;

        VerbSpinnerAdapter(Context ctx, int txtViewResourceId, List<Verb> verbs) {
            super(ctx, txtViewResourceId, verbs);
            this.verbs = verbs;
        }
        @Override public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }
        @NonNull @Override public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }
        View getCustomView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = LayoutInflater.from(getContext());
            View mySpinner = inflater.inflate(R.layout.custom_verbchooser_spinner, parent, false);

            String SpinnerText;
            Verb verb = verbs.get(position);

            //Setting the preposition
            if (verb.getPreposition().equals("")) SpinnerText = "";
            else SpinnerText = "[" + verb.getPreposition() + "] ";
            TextView verbchooser_Prep = mySpinner.findViewById(R.id.verbchooser_Prep);
            verbchooser_Prep.setText(SpinnerText);

            //Setting the Kanji and Romaji
            SpinnerText = verb.getKanji() + " (" + verb.getRomaji() + ")";
            TextView verbchooser_Kanji_and_ustem = mySpinner.findViewById(R.id.verbchooser_Kanji_and_ustem);
            verbchooser_Kanji_and_ustem.setText(SpinnerText);

            //Setting the trans./intrans.
            if (!verb.getFamily().equals("")) {
                SpinnerText = verb.getFamily();
                if (!verb.getTrans().equals("")) SpinnerText = SpinnerText + ", " + verb.getTrans();
            }
            else {
                if (!verb.getTrans().equals("")) SpinnerText = verb.getTrans();
                else { SpinnerText = ""; }
            }
            TextView verbchooser_Characteristics = mySpinner.findViewById(R.id.verbchooser_Characteristics);
            verbchooser_Characteristics.setText(SpinnerText);

            //Setting the meaning
            SpinnerText = verb.getMeaning();
            TextView verbchooser_EnglishMeaning = mySpinner.findViewById(R.id.verbchooser_EnglishMeaning);
            verbchooser_EnglishMeaning.setText(SpinnerText);

            return mySpinner;
        }
    }
    private class ConjugationsSpinnerAdapter extends ArrayAdapter<Verb.ConjugationCategory> {
        // Code adapted from http://mrbool.com/how-to-customize-spinner-in-android/28286

        List<Verb.ConjugationCategory> conjugationCategories;
        private final List<ConjugationTitle> conjugationTitles;

        ConjugationsSpinnerAdapter(Context ctx, int txtViewResourceId,
                                   List<Verb.ConjugationCategory> conjugationCategories,
                                   List<ConjugationTitle> conjugationTitles) {
            super(ctx, txtViewResourceId, conjugationCategories);
            this.conjugationCategories = conjugationCategories;
            this.conjugationTitles = conjugationTitles;
        }

        @Override public View getDropDownView(int position, View cnvtView, @NonNull ViewGroup parent) {
            return getCustomView(position, cnvtView, parent);
        }

        @NonNull @Override public View getView(int pos, View cnvtView, @NonNull ViewGroup parent) {
            return getCustomView(pos, cnvtView, parent);
        }

        View getCustomView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = LayoutInflater.from(getContext());
            View mySpinner = inflater.inflate(R.layout.custom_conjugationchooser_spinner, parent, false);

            String SpinnerText;
            Verb.ConjugationCategory conjugationCategory = conjugationCategories.get(position);

            //Setting the title
            int shownPosition = position+1;
            SpinnerText = shownPosition + ". " + conjugationTitles.get(position).getTitle();
            TextView Upper_text = mySpinner.findViewById(R.id.UpperPart);
            Upper_text.setText(SpinnerText);

            //Displaying the first element in the Conjugation, e.g. PrPlA in Simple Form
            SpinnerText = conjugationCategory.getConjugations().get(0).getConjugationLatin();
            TextView Lower_text = mySpinner.findViewById(R.id.LowerPart);
            Lower_text.setText(SpinnerText);

            return mySpinner;
        }
    }
    static public List<Boolean> FindType(String verb) {
        String text_type = ConvertFragment.TextType(verb);

        boolean mInputQueryIsLatin   = false;
        boolean mInputQueryIsKana    = false;
        boolean mInputQueryIsKanji   = false;
        boolean mInputQueryIsInvalid = false;

        if ( text_type.equals("latin") )                                     { mInputQueryIsLatin = true;}
        if ( text_type.equals("hiragana") || text_type.equals("katakana") )  { mInputQueryIsKana = true;}
        if ( text_type.equals("kanji") )                                     { mInputQueryIsKanji = true;}
        if ( verb.contains("*") || verb.contains("＊") || verb.equals("") || text_type.equals("number") ) { mInputQueryIsInvalid = true;}

        List<Boolean> types = new ArrayList<>();
        types.add(mInputQueryIsLatin);
        types.add(mInputQueryIsKana);
        types.add(mInputQueryIsKanji);
        types.add(mInputQueryIsInvalid);
        return types;
    }

}