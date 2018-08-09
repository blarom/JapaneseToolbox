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
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.japanesetoolboxapp.R;
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
    @BindView(R.id.verb_chooser_spinner) Spinner mVerbChooserSpinner;
    @BindView(R.id.conjugations_chooser_spinner) Spinner mConjugationChooserSpinner;
    @BindView(R.id.verb_hint) TextView mVerbHintTextView;
    @BindView(R.id.conjugations_container) ScrollView mConjugationsContainerScrollView;
    @BindView(R.id.radio_romaji_or_kanji) RadioGroup mRomajiOrKanjiRadioButton;
    @BindView(R.id.radio_Romaji) RadioButton mRomajiRadioButton;
    @BindView(R.id.radio_Kanji) RadioButton mKanjiRadioButton;

    public static List<List<String>> mVerbListForSpinner;
    public static List<List<String>> mVerbConjugationsListForSpinner;
    static List<List<List<Integer>>> mTitleIndexes;
    static List<List<List<String>>> mTitlesList;
    static int rowIndex_of_suru;
    static int rowIndex_of_kuru;
    static int rowIndex_of_suru_in_Conj;
    static String[] row_values_suru_latin;
    static String[] row_values_suru_kanji;
    private Unbinder mBinding;
    List<List<List<List<String>>>> mVerbCharacteristicsEnglishKanji;
    List<List<List<String>>> mVerbCharacteristicsEnglish;
    List<List<List<String>>> mVerbCharacteristicsKanji;
    private String mInputQuery;
    private List<List<Integer>> mMatchingVerbRowColIndexList;
    private List<Integer> mMatchingVerbRowIndexList;
    private List<Integer> mMatchingVerbColIndexList;
    private int mRequestedConjugationSpinnerIndex;
    private String mChosenRomajiOrKanji;
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
            mVerbChooserSpinner.setVisibility(View.GONE);
            if (!TextUtils.isEmpty(mInputQuery)) SearchForConjugations();
        }

        //If the fragment is being resumed, just reload the saved data
        else {
            mVerbChooserSpinner.setVisibility(View.VISIBLE);

        }

        return rootView;
    }
    @Override public void onResume() {
        super.onResume();
        displayVerbsInVerbChooserSpinner();
    }
    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        //outState.putParcelableArrayList("saved_verb_characteristics", new ArrayList<>(mVerbCharacteristicsEnglish));
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

        mVerbCharacteristicsEnglishKanji = new ArrayList<>();
        mVerbCharacteristicsEnglish = new ArrayList<>();
        mVerbCharacteristicsKanji = new ArrayList<>();
    }

    public void SearchForConjugations() {

        getTitlesFromSheet();
        getIndexesOfMatchingVerbs();
        getVerbsCharacteristics();
        displayVerbsInVerbChooserSpinner();

    }

    private void getTitlesFromSheet() {

        mTitleIndexes = TitleIndexesFinder(MainActivity.VerbLatinConjDatabase);
        mTitlesList = TitlesFinder(mTitleIndexes, MainActivity.VerbLatinConjDatabase);
    }
    public List<List<List<String>>> TitlesFinder(List<List<List<Integer>>> TitleIndexes, List<String[]> mySheet) {

        // Get the Conjugation Titles and their indexes
        List<List<List<String>>> setOf_TitlesList = new ArrayList<>();
        List<List<String>> TitlesList = new ArrayList<>();
        List<String> Title_as_List = new ArrayList<>();
        List<String> SubTitles = new ArrayList<>();
        List<String> TitleConjugations = new ArrayList<>();
        String Title;

        for (int i=0; i<TitleIndexes.size(); i++) {

            Title = mySheet.get(0)[TitleIndexes.get(i).get(0).get(0)];
            Title_as_List.add(Title);

            for (int s=0; s<TitleIndexes.get(i).get(1).size(); s++) {
                SubTitles.add(mySheet.get(1)[TitleIndexes.get(i).get(1).get(s)]);
                TitleConjugations.add(mySheet.get(2)[TitleIndexes.get(i).get(1).get(s)]);
            }

            TitlesList.add(Title_as_List);
            TitlesList.add(SubTitles);
            TitlesList.add(TitleConjugations);
            setOf_TitlesList.add(TitlesList);

            Title_as_List = new ArrayList<>();
            SubTitles = new ArrayList<>();
            TitleConjugations = new ArrayList<>();
            TitlesList = new ArrayList<>();
        }
        return setOf_TitlesList;
    }
    public List<List<List<Integer>>> TitleIndexesFinder(List<String[]> mySheet) {

        // This function returns a List<List<List<Integer>>> where each set of Lists contains the Title index,
        // and the list of corresponding SubTitle and Conjugation indexes,
        // So that their Strings may be conveniently extracted from mySheet

        List<List<List<Integer>>> TitleIndexesList = new ArrayList<>();
        List<List<Integer>> TitleIndexes = new ArrayList<>();
        List<Integer> SubIndexes_List = new ArrayList<>();
        List<Integer> TitleIndex_asList = new ArrayList<>();
        String title;

        int sheet_length = mySheet.get(0).length;
        for (int t=0; t<sheet_length; t++) {

            title = mySheet.get(0)[t];

            if (t == 0) {
                TitleIndex_asList.add(t);
                SubIndexes_List.add(t);
            }
            else if (t == sheet_length-1) {
                if (SubIndexes_List.size() < 2) {SubIndexes_List.add(t);}       // The if clause is necessary because it registers an extra conjugation where there is none
                TitleIndexes.add(TitleIndex_asList); // Titles
                TitleIndexes.add(SubIndexes_List); // Subtitles or Conjugation Titles
                TitleIndexesList.add(TitleIndexes);
            }
            else {
                if (!title.equals("")) {
                    TitleIndexes.add(TitleIndex_asList); // Titles
                    TitleIndexes.add(SubIndexes_List); // Subtitles or Conjugation Titles
                    TitleIndexesList.add(TitleIndexes);

                    TitleIndex_asList = new ArrayList<>();
                    SubIndexes_List = new ArrayList<>();
                    TitleIndexes = new ArrayList<>();

                    TitleIndex_asList.add(t);
                }
                SubIndexes_List.add(t);
            }
        }

        return TitleIndexesList;
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
        boolean verb_is_a_conjugation_of_the_current_family = false;
        boolean verb_is_suru = false;
        boolean verb_is_a_family_conjugation = false;
        boolean suru_conjugation_is_also_a_verb = false;
        boolean first_letter_is_identical;
        boolean value_english_is_empty;
        int NumberOfSheetCols = MainActivity.VerbLatinConjDatabase.get(0).length;
        int verb_length = mInputQuery.length();
        int match_column = 0;
        int value_Exception_asInt;
        String verb2;
        String verb_to_be_processed;
        String suru_conjugation;
        String concatenated_value;
        String[] current_row_values_latin;
        String[] current_row_values_kanji;
        String[] row_values_current_family = new String[NumberOfSheetCols];
        String[] row_values_current_exceptionLatin = new String[NumberOfSheetCols];
        String[] row_values_current_exceptionKanji = new String[NumberOfSheetCols];
        String value_Family;
        String value_English;
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
        //endregion

        //region Getting the input type and its converted form (english/romaji/kanji/invalid)
        List<String> translationList = ConvertFragment.Kana_to_Romaji_to_Kana(mInputQuery);
        String translation = "";
        String text_type = ConvertFragment.TextType(mInputQuery);

        boolean TypeisLatin   = false;
        boolean TypeisKana    = false;
        boolean TypeisKanji   = false;
        boolean TypeisInvalid = false;
        boolean translationIsInvalid = false;

        if (text_type.equals("latin") )                                     { translation = translationList.get(1); TypeisLatin = true;}
        if (text_type.equals("hiragana") || text_type.equals("katakana") )  { translation = translationList.get(0); TypeisKana = true;}
        if (text_type.equals("kanji") )                                     { translation = translationList.get(1); TypeisKanji = true;}
        if ( mInputQuery.contains("*") || mInputQuery.contains("＊") || mInputQuery.equals("") ) { TypeisInvalid = true;}
        if ( translation.contains("*") || translation.contains("＊"))        { translationIsInvalid = true; }
        //endregion

        // Starting the algorithm
        if (!TypeisInvalid) {

            // 1. Initial Setup

            ;//Converting the word to lowercase (the algorithm is not efficient if needing to search both lower and upper case)
            mInputQuery = mInputQuery.toLowerCase(Locale.ENGLISH);

            // Finding the index of the last non-empty row, to make sure the algorithm does not search on empty last rows
            int lastIndex = MainActivity.VerbDatabase.size()-1;
            while (MainActivity.VerbDatabase.get(lastIndex)[0].length() == 0) { lastIndex--; }

            //region Removing the "ing" from the verb (if relevant)
            if (verb_length > 2 && mInputQuery.substring(verb_length-3).equals("ing")) {

                if (verb_length > 5 && mInputQuery.substring(verb_length-6).equals("inging")) {
                    if (	(mInputQuery.substring(0, 2+1).equals("to ") && IsOfTypeIngIng(mInputQuery.substring(3,verb_length))) ||
                            (!mInputQuery.substring(0, 2+1).equals("to ") && IsOfTypeIngIng(mInputQuery.substring(0,verb_length)))   ) {
                        // If the verb ends with "inging" then remove the the second "ing"
                        mInputQuery = mInputQuery.substring(0,verb_length-3);
                    }
                }
                else {
                    verb2 = mInputQuery + "ing";
                    if ((!verb2.substring(0, 2 + 1).equals("to ") || !IsOfTypeIngIng(verb2.substring(3, verb_length + 3))) &&
                            (verb2.substring(0, 2+1).equals("to ") || !IsOfTypeIngIng(verb2.substring(0, verb_length + 3)))) {
                        // If the verb does not belong to the list, then remove the ending "ing" so that it can be compared later on to the verbs excel
                        ;//If the verb is for e.g. to sing / sing (verb2 = to singing / singing), then check that verb2 (without the "to ") belongs to the list, and if it does then do nothing

                        mInputQuery = mInputQuery.substring(0,verb_length-3);
                    }
                }
            }
            //endregion

            //region Concatenating the verb & its translation for future use
            String concatenated_verb = Utilities.removeSpecialCharacters(mInputQuery);
            String concatenated_translation = Utilities.removeSpecialCharacters(translation);
            int concatenated_verb_length = concatenated_verb.length();
            int concatenated_translation_length = concatenated_translation.length();
            //endregion

            //region Taking care of the case where the input is a basic conjugation that will cause the app to return all verbs
            String[] title_row = new String[NumberOfSheetCols];
            List<String> parsedConj;
            boolean verb_is_conjoined = false;
            verb_to_be_processed = mInputQuery;
            String current_conjugation;
            if (TypeisLatin) {
                title_row = MainActivity.VerbLatinConjDatabase.get(2);
            }
            else if (TypeisKana) {
                title_row = MainActivity.VerbLatinConjDatabase.get(2);
                verb_to_be_processed = translation;
            }
            else if (TypeisKanji) {
                title_row = MainActivity.VerbKanjiConjDatabase.get(2);

            }
            int verb_to_be_processed_length = verb_to_be_processed.length();
            for (int column = GlobalConstants.VerbModule_colIndex_istem; column < NumberOfSheetCols; column++) {
                parsedConj = Arrays.asList(title_row[column].split("/"));
                for (int i=0;i<parsedConj.size();i++) {
                    current_conjugation = parsedConj.get(i);
                    if (verb_to_be_processed.equals(current_conjugation) && verb_to_be_processed_length > 3) {
                        verb_is_conjoined = true;
                    }
                }
            }
            //endregion

            //region Performing column dilution in order to make the search more efficient (the diluted column ArrayList is used in the Search Algorithm)
            current_length = 0;
            List<String[]> mySheetLengths = new ArrayList<>();
            if (TypeisLatin) {
                mySheetLengths = DatabaseUtilities.readCSVFileFirstRow("LineVerbsLengths - 3000 kanji.csv", getContext());
                current_length = concatenated_verb_length;
            }
            else if (TypeisKana) {
                mySheetLengths = DatabaseUtilities.readCSVFileFirstRow("LineVerbsLengths - 3000 kanji.csv", getContext());
                current_length = concatenated_translation_length;
            }
            else if (TypeisKanji) {
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
                    match_column = column;
                    for (int row = 3; row< MainActivity.VerbDatabase.size(); row++) {
                        current_row_values_latin = MainActivity.VerbDatabase.get(row);
                        value_Romaji = current_row_values_latin[GlobalConstants.VerbModule_colIndex_ustem];
                        value_Kanji = current_row_values_latin[GlobalConstants.VerbModule_colIndex_kanji];
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
                for (int current_index=3; current_index < lastIndex+1; current_index++) {
                    current_row_values_latin = MainActivity.VerbDatabase.get(current_index);
                    value_Romaji = current_row_values_latin[GlobalConstants.VerbModule_colIndex_ustem];
                    if (value_Romaji.equals("suru")) {
                        ConjugationSearchMatchingVerbRowIndexList.add(current_index);
                        ConjugationSearchMatchingVerbColIndexList.add(match_column);
                    }
                }
            }
            if (!verb_is_suru || (verb_is_suru && suru_conjugation_is_also_a_verb)) {

                current_row_values_latin = MainActivity.VerbLatinConjDatabase.get(MainActivity.VerbLatinConjDatabase.size()-1);
                current_row_values_kanji = MainActivity.VerbKanjiConjDatabase.get(MainActivity.VerbKanjiConjDatabase.size()-1);

                // Checking if the verb is a family conjugation
                for (int rowIndex = 3; rowIndex < lastIndex + 1; rowIndex++) {

                    for (int p=0; p<=GlobalConstants.VerbModule_colIndex_istem; p++) { //Padding the current_row_values with the values from the current index
                        current_row_values_latin[p] = MainActivity.VerbDatabase.get(rowIndex)[p];
                        current_row_values_kanji[p] = MainActivity.VerbDatabase.get(rowIndex)[p];
                    }

                    value_English = current_row_values_latin[GlobalConstants.VerbModule_colIndex_english];
                    value_Family = current_row_values_latin[GlobalConstants.VerbModule_colIndex_family];

                    if (!value_Family.equals("") && value_English.equals("")) {
                        if (TypeisLatin) {
                            row_values_current_family = MainActivity.VerbLatinConjDatabase.get(Integer.valueOf(current_row_values_latin[GlobalConstants.VerbModule_colIndex_istem]));
                        } else {
                            row_values_current_family = MainActivity.VerbKanjiConjDatabase.get(Integer.valueOf(current_row_values_kanji[GlobalConstants.VerbModule_colIndex_istem]));
                        }
                        for (int col=GlobalConstants.VerbModule_colIndex_istem; col<NumberOfSheetCols;col++) {
                            if (mInputQuery.equals(row_values_current_family[col])) { verb_is_a_family_conjugation = true; break; }
                        }
                    }
                }

                for (int rowIndex = 3; rowIndex < lastIndex + 1; rowIndex++) {

                    //region Loop starting parameters initialization
                    found_match = false;
                    skip_this_row = true;
                    first_letter_is_identical = false;
                    value_english_is_empty = false;

                    for (int p=0; p<=GlobalConstants.VerbModule_colIndex_istem; p++) { //Padding the current_row_values with the values from the current index
                        current_row_values_latin[p] = MainActivity.VerbDatabase.get(rowIndex)[p];
                        current_row_values_kanji[p] = MainActivity.VerbDatabase.get(rowIndex)[p];
                    }
                    //endregion

                    //region Extracting the cumulative english meanings
                    value_English = current_row_values_latin[GlobalConstants.VerbModule_colIndex_english];
                    if (value_English.equals("")) { value_english_is_empty = true; }

                    value_Kana = current_row_values_latin[GlobalConstants.VerbModule_colIndex_kana];
                    value_Kanji = current_row_values_latin[GlobalConstants.VerbModule_colIndex_kanji];
                    value_Family = current_row_values_latin[GlobalConstants.VerbModule_colIndex_family];
                    value_Romaji = current_row_values_latin[GlobalConstants.VerbModule_colIndex_ustem];

                    value_rootKanji = current_row_values_kanji[GlobalConstants.VerbModule_colIndex_rootKanji];
                    value_rootLatin = current_row_values_latin[GlobalConstants.VerbModule_colIndex_rootLatin];
                    //endregion

                    //region Add "to " to the values of value_English (the "to "s were removed in the database to free space)
                    List<String> parsed_value = Arrays.asList(value_English.split(","));
                    value_English = "to ";
                    for (int k=0; k<parsed_value.size();k++) {
                        value_English = value_English + parsed_value.get(k).trim();
                        if (k<parsed_value.size()-1) {value_English = value_English + ", to ";}
                    }
                    //endregion

                    // Skipping empty/family rows
                    if (value_Family.equals("") || value_Family.equals("-")) { continue; }

                    //region Registering the current family row
                    if (value_english_is_empty) {

                        verb_is_a_conjugation_of_the_current_family = false;
                        // Registering the row
                        if (TypeisLatin) {
                            row_values_current_family = MainActivity.VerbLatinConjDatabase.get(Integer.valueOf(current_row_values_latin[GlobalConstants.VerbModule_colIndex_istem]));
                            verb_to_be_processed = mInputQuery;
                        } else if (TypeisKana) {
                            row_values_current_family = MainActivity.VerbLatinConjDatabase.get(Integer.valueOf(current_row_values_latin[GlobalConstants.VerbModule_colIndex_istem]));
                            verb_to_be_processed = translation;
                        } else {
                            row_values_current_family = MainActivity.VerbKanjiConjDatabase.get(Integer.valueOf(current_row_values_kanji[GlobalConstants.VerbModule_colIndex_istem]));
                            verb_to_be_processed = mInputQuery;
                        }
                        if (verb_is_a_family_conjugation) {
                            for (int col = GlobalConstants.VerbModule_colIndex_istem; col < NumberOfSheetCols; col++) {
                                if (verb_to_be_processed.equals(row_values_current_family[col])) {
                                    verb_is_a_conjugation_of_the_current_family = true;
                                    match_column = col;
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
                    if ( (TypeisLatin && verb_length < 3) || (TypeisKana  && verb_length < 2) || (TypeisKanji && verb_length < 2)) {
                        diluted_columns = diluted_columns_no_conjugations;
                    }
                    //endregion

                    //region Getting the exception row if relevant
                    value_Exception = current_row_values_latin[GlobalConstants.VerbModule_colIndex_istem];
                    if (!value_Exception.equals("")) {
                        value_Exception_asInt = Integer.valueOf(value_Exception);
                        row_values_current_exceptionLatin = MainActivity.VerbLatinConjDatabase.get(value_Exception_asInt);
                        row_values_current_exceptionKanji = MainActivity.VerbKanjiConjDatabase.get(value_Exception_asInt);
                    }
                    //endregion

                    //region Preventing searches on verbs that don't have the same first kana
                    if (!value_english_is_empty) {
                        if (        TypeisLatin
                                ||  TypeisKana  && (value_Kana.charAt(0) == mInputQuery.charAt(0))
                                ||  TypeisKanji && value_Kanji.contains(mInputQuery.substring(0,1))
                                ||  value_Romaji.equals("kuru") ||  value_Romaji.equals("da"))
                        { first_letter_is_identical = true; skip_this_row = false; }
                    }
                    //endregion

                    //region Including the rows for verbs starting in o or go
                    if (!value_english_is_empty) {
                        if (((TypeisLatin && verb_length > 2) || (TypeisKana && verb_length > 1) || (TypeisKanji && verb_length > 1))
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
                        if (TypeisKana || TypeisKanji) {
                            if (value_rootKanji.length() == 1 && verb_is_a_conjugation_of_the_current_family) {
                                found_match = true;
                            }
                            if ((value_rootKanji.length() > 1 || !verb_is_a_conjugation_of_the_current_family) && !first_letter_is_identical) {
                                skip_this_row = true;
                            }
                        }
                        else {
                            if (value_rootLatin.length() == 1 && verb_is_a_conjugation_of_the_current_family) {
                                found_match = true;
                            }
                            if ((value_rootLatin.length() > 1 || !verb_is_a_conjugation_of_the_current_family) && !first_letter_is_identical) {
                                skip_this_row = true;
                            }
                        }
                    }
                    //endregion

                    //region Main Comparator Algorithm
                    if (!skip_this_row && !found_match) {
                        if (TypeisLatin) {

                            // Check if there is a hit in the english column
                            if (verb_length > 3 && (value_English.contains(concatenated_verb)
                                    || value_English.contains("to "+concatenated_verb.substring(2,concatenated_verb.length())))) {
                                found_match = true;
                                match_column = GlobalConstants.VerbModule_colIndex_english;
                            }

                            // Check if there is a hit in the romaji column
                            concatenated_value = value_Romaji.replace(" ", "");
                            if (concatenated_value.contains(concatenated_verb)) {
                                found_match = true;
                                match_column = GlobalConstants.VerbModule_colIndex_ustem;
                            }

                            // Otherwise, go over the rest of the conjugations
                            if (!found_match) {
                                if (!value_Exception.equals("")) {
                                    for (int col : diluted_columns) {

                                        // If the value at the current index has conjugation exceptions, get those from corresponding row in the Conj sheet instead of the family row
                                        if (row_values_current_exceptionLatin[col].equals("")) {
                                            value = value_rootLatin + row_values_current_family[col];
                                        } else {
                                            value = row_values_current_exceptionLatin[col];
                                        }

                                        concatenated_value = value.replace(" ", "");
                                        if (concatenated_value.contains(concatenated_verb)) {
                                            found_match = true;
                                            match_column = col;
                                        }
                                        if (found_match) {
                                            break;
                                        }
                                    }
                                } else for (int col : diluted_columns) {

                                    value = value_rootLatin + row_values_current_family[col];

                                    concatenated_value = value.replace(" ", "");
                                    if (concatenated_value.contains(concatenated_verb)) {
                                        found_match = true;
                                        match_column = col;
                                    }
                                    if (found_match) {
                                        break;
                                    }
                                }
                            }

                        } else if (TypeisKana && !translationIsInvalid) {

                            // Check if there is a hit in the romaji column
                            concatenated_value = value_Romaji.replace(" ", "");
                            if (concatenated_value.contains(concatenated_translation)) {
                                found_match = true;
                                match_column = GlobalConstants.VerbModule_colIndex_ustem;
                            }

                            // Otherwise, go over the rest of the conjugations
                            if (!found_match && !value_Exception.equals("")) {
                                for (int col : diluted_columns) {

                                    // If the value at the current index has conjugation exceptions, get those from corresponding row in the Conj sheet instead of the family row
                                    if (row_values_current_exceptionLatin[col].equals("")) {
                                        value = value_rootLatin + row_values_current_family[col];
                                    } else {
                                        value = row_values_current_exceptionLatin[col];
                                    }

                                    concatenated_value = value.replace(" ", "");
                                    if (concatenated_value.contains(concatenated_translation)) {
                                        found_match = true;
                                        match_column = col;
                                    }
                                    if (found_match) {
                                        break;
                                    }
                                }
                            } else {
                                for (int col : diluted_columns) {

                                    value = value_rootLatin + row_values_current_family[col];

                                    concatenated_value = value.replace(" ", "");
                                    if (concatenated_value.contains(concatenated_translation)) {
                                        found_match = true;
                                        match_column = col;
                                    }
                                    if (found_match) {
                                        break;
                                    }
                                }
                            }

                        } else if (TypeisKanji) {

                            // Check if there is a hit in the Kanji column
                            if (value_Kanji.contains(concatenated_verb)) {
                                found_match = true;
                                match_column = GlobalConstants.VerbModule_colIndex_kanji;
                            }

                            // Otherwise, go over the rest of the conjugations
                            if (!found_match && !value_Exception.equals("")) {
                                for (int col : diluted_columns) {

                                    // If the value at the current index has conjugation exceptions, get those from corresponding row in the Conj sheet instead of the family row
                                    if (row_values_current_exceptionKanji[col].equals("")) {
                                        value = value_rootKanji + row_values_current_family[col];
                                    } else {
                                        value = row_values_current_exceptionKanji[col];
                                    }

                                    concatenated_value = value.replace(" ", "");
                                    if (concatenated_value.contains(concatenated_verb)) {
                                        found_match = true;
                                        match_column = col;
                                    }
                                    if (found_match) {
                                        break;
                                    }
                                }
                            } else for (int col : diluted_columns) {

                                value = value_rootKanji + row_values_current_family[col];

                                concatenated_value = value.replace(" ", "");
                                if (concatenated_value.contains(concatenated_verb)) {
                                    found_match = true;
                                    match_column = col;
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
                        ConjugationSearchMatchingVerbColIndexList.add(match_column);
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
    public static String addToDefinition(String definition) {
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

    private void getVerbsCharacteristics() {

        mVerbCharacteristicsEnglishKanji = getSetOfVerbCharacteristics(mMatchingVerbRowIndexList);

        if (mVerbCharacteristicsEnglishKanji.size() != 0) {
            mVerbCharacteristicsEnglish = mVerbCharacteristicsEnglishKanji.get(0);
            mVerbCharacteristicsKanji = mVerbCharacteristicsEnglishKanji.get(1);
        }
    }
    public List<Verb> getVerbs(List<Integer> matchingVerbRowIndexList) {

        // Since there may be multiple matches for a given verb (e.g. if there are multiple kanjis/meanings),
        // then the verb info is a list of lists, each one including all the verb stems & conjugations
        // These lists of lists are retrieved from separate functions for simplicity, with each function covering a specfic range of conjugations

        ;//region Initializations
        List<List<List<List<String>>>> setOf_matchingVerbCharacteristics_English_Kanji = new ArrayList<>();
        List<List<List<String>>> setOf_matchingVerbCharacteristics = new ArrayList<>();
        int matchingVerbRowIndex;
        int PassiveTenseIndex = 0;
        int currentFamilyHeaderRowIndex = 0;
        int NumberOfSheetCols = MainActivity.VerbLatinConjDatabase.get(0).length;
        int conjLatinLength;
        int conjKanjiLength;
        List<String[]> conjugationsSheet = null;
        String current_title;
        String definition;
        String type;
        List<List<String>> matchingVerbCharacteristics;
        List<String> Basics;
        List<String> current_set;
        String[] rowValuesCurrentException = new String[NumberOfSheetCols];
        //endregion

        //region Find the Passive tense index in order to remove passive conjugations from verbs that are intransitive
        for (int j = 0; j< mTitleIndexes.size()-1; j++) {
            current_title = mTitlesList.get(j).get(0).get(0);
            if (current_title.contains("Passive (X is done to him)")) { PassiveTenseIndex = j; }
        }
        //endregion

        //region Building the set of verb characteristics for each matching verb
        if (matchingVerbRowIndexList.size() != 0) {

            List<String[]> conjugationsSheetLatin = MainActivity.VerbLatinConjDatabase;
            List<String[]> conjugationsSheetKanji = MainActivity.VerbKanjiConjDatabase;

            for (int p=0; p<matchingVerbRowIndexList.size(); p++) {
                matchingVerbRowIndex = matchingVerbRowIndexList.get(p);
                String[] currentVerbRow = MainActivity.VerbDatabase.get(matchingVerbRowIndex);
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
                        currentVerbRow[GlobalConstants.VerbModule_colIndex_altSpellings] );

                //region Finding the conjugation family relevant to the current verb
                for (int i = 2; i< MainActivity.VerbDatabase.size(); i++) {
                    String[] currentRow = MainActivity.VerbDatabase.get(i);
                    if (currentRow[GlobalConstants.VerbModule_colIndex_rootLatin].equals("")
                            && currentRow[GlobalConstants.VerbModule_colIndex_english].equals("")
                            && !currentRow[GlobalConstants.VerbModule_colIndex_family].equals("")) {

                        if (i<matchingVerbRowIndex) { currentFamilyHeaderRowIndex = i;}
                        else {break;}
                    }
                }

                String[] currentFamilyHeaderInVerbDatabase = MainActivity.VerbDatabase.get(currentFamilyHeaderRowIndex);
                int indexOfRelevantFamilyConjugations = Integer.valueOf(currentFamilyHeaderInVerbDatabase[GlobalConstants.VerbModule_colIndex_exception]);
                String[] currentConjugationsRowLatin = conjugationsSheetLatin.get(indexOfRelevantFamilyConjugations);
                String[] currentConjugationsRowKanji = conjugationsSheetKanji.get(indexOfRelevantFamilyConjugations);
                //endregion

                //region If the verb has a conjugation exceptions row, update the currentConjugationsRow with the nonempty exceptions in that row
                boolean isAnException = false;
                String valueExceptionIndicator = currentVerbRow[GlobalConstants.VerbModule_colIndex_exception];
                String[] currentConjugationExceptionsRowLatin = conjugationsSheet.get(conjugationsSheet.size()-1); //initialization to an empty String[] of the correct length;
                String[] currentConjugationExceptionsRowKanji = conjugationsSheet.get(conjugationsSheet.size()-1); //initialization to an empty String[] of the correct length;
                if (!valueExceptionIndicator.equals("")) {
                    int indexOfExceptionConjugations = Integer.valueOf(valueExceptionIndicator);
                    currentConjugationExceptionsRowLatin = conjugationsSheetLatin.get(indexOfExceptionConjugations);
                    currentConjugationExceptionsRowKanji = conjugationsSheetKanji.get(indexOfExceptionConjugations);
                    isAnException = true;
                    for (int col=0; col<NumberOfSheetCols; col++) {
                        String currentConjugationExceptionLatin = currentConjugationExceptionsRowLatin[col];
                        String currentConjugationExceptionKanji = currentConjugationExceptionsRowKanji[col];
                        if (!currentConjugationExceptionLatin.equals(""))
                            currentConjugationsRowLatin[col] = currentConjugationExceptionsRowLatin[col];
                        if (!currentConjugationExceptionKanji.equals(""))
                            currentConjugationsRowKanji[col] = currentConjugationExceptionsRowKanji[col];
                    }
                }
                //endregion

                //Getting the verb's conjugations row
                String[] matchingVerbRowLatin = conjugationsSheet.get(conjugationsSheet.size()-1); //initialization to an empty String[] of the correct length
                String[] matchingVerbRowKanji = conjugationsSheet.get(conjugationsSheet.size()-1); //initialization to an empty String[] of the correct length
                for (int col=0; col<NumberOfSheetCols; col++) {

                    if (col<GlobalConstants.VerbModule_colIndex_istem) {
                        matchingVerbRowLatin[col] = currentVerbRow[col];
                        matchingVerbRowKanji[col] = currentVerbRow[col];
                    }
                    else {

                        //If there is a conjugation exception then use it as-is, otherwise create the conjugation using the family conjugation endings
                        if (isAnException && !currentConjugationExceptionsRowLatin[col].equals("")) {
                            matchingVerbRowLatin[col] = currentConjugationExceptionsRowLatin[col];
                            matchingVerbRowKanji[col] = currentConjugationExceptionsRowKanji[col];
                        }
                        else {
                            conjLatinLength = currentConjugationsRowLatin[col].length();
                            if (conjLatinLength > 3 && currentConjugationsRowLatin[col].substring(0, 3).equals("(o)")) {
                                matchingVerbRowLatin[col] = "(o)" + currentVerb.getLatinRoot() + currentConjugationsRowLatin[col].substring(3, conjLatinLength);
                            } else
                                matchingVerbRowLatin[col] = currentVerb.getLatinRoot() + currentConjugationsRowLatin[col];

                            conjKanjiLength = currentConjugationsRowKanji[col].length();
                            if (conjLatinLength > 3 && currentConjugationsRowLatin[col].substring(0, 3).equals("(お)")) {
                                matchingVerbRowKanji[col] = "(お)" + currentVerb.getKanjiRoot() + currentConjugationsRowKanji[col].substring(3, conjKanjiLength);
                            } else
                                matchingVerbRowKanji[col] = currentVerb.getKanjiRoot() + currentConjugationsRowKanji[col];
                        }

                    }
                }

                //Putting each conjugation in the conjugation row into its appropriate category in the Verb object
                //>>>>>>>
            }






            for (int r = 0; r<2; r++) {
                if (r == 0) { conjugationsSheet = MainActivity.VerbLatinConjDatabase;}
                else { conjugationsSheet = MainActivity.VerbKanjiConjDatabase;}

                for (int p=0; p<matchingVerbRowIndexList.size(); p++) {
                    matchingVerbCharacteristics = new ArrayList<>();
                    matchingVerbRowIndex = matchingVerbRowIndexList.get(p);

                    //region Find the conjugation family relevant to the current verb
                    for (int i = 2; i< MainActivity.VerbDatabase.size(); i++) {
                        String[] currentVerbRow = MainActivity.VerbDatabase.get(i);
                        if (currentVerbRow[GlobalConstants.VerbModule_colIndex_rootLatin].equals("")
                                && currentVerbRow[GlobalConstants.VerbModule_colIndex_english].equals("")
                                && !currentVerbRow[GlobalConstants.VerbModule_colIndex_family].equals("")) {

                            if (i<matchingVerbRowIndex) { currentFamilyHeaderRowIndex = i;}
                            else {break;}
                        }
                    }

                    String[] currentFamilyHeaderInVerbDatabase = MainActivity.VerbDatabase.get(currentFamilyHeaderRowIndex);
                    int indexOfRelevantConjugations = Integer.valueOf(currentFamilyHeaderInVerbDatabase[GlobalConstants.VerbModule_colIndex_istem]);
                    String[] rowValuesCurrentFamily = conjugationsSheet.get(indexOfRelevantConjugations);
                    //endregion

                    //region Getting the row of the current matching verb & Padding the matchingVerbRow with the values from the current index


                    //If the verb has conjugation exceptions, get its corresponding conjugations row in the conjugationsSheet
                    boolean isAnException = false;
                    String valueExceptionIndicator = MainActivity.VerbDatabase.get(matchingVerbRowIndex)[GlobalConstants.VerbModule_colIndex_istem];
                    if (!valueExceptionIndicator.equals("")) {
                        int indexOfExceptionConjugations = Integer.valueOf(valueExceptionIndicator);
                        isAnException = true;
                        rowValuesCurrentException = conjugationsSheet.get(indexOfExceptionConjugations);
                    }

                    //Getting the verb's root value
                    String verbRoot;
                    if (r == 0) { verbRoot = MainActivity.VerbDatabase.get(matchingVerbRowIndex)[GlobalConstants.VerbModule_colIndex_rootLatin];}
                    else        { verbRoot = MainActivity.VerbDatabase.get(matchingVerbRowIndex)[GlobalConstants.VerbModule_colIndex_rootKanji];}

                    //Initializing the matching verb conjugations array with empty values
                    String[] matchingVerbRow = conjugationsSheet.get(conjugationsSheet.size()-1);
                    for (int col=0; col<NumberOfSheetCols; col++) {

                        if (col<GlobalConstants.VerbModule_colIndex_istem) {
                            matchingVerbRow[col] = MainActivity.VerbDatabase.get(matchingVerbRowIndex)[col]; }
                        else {
                            if (!isAnException) {
                                conjLatinLength = rowValuesCurrentFamily[col].length();
                                if (conjLatinLength > 3) {
                                    switch (rowValuesCurrentFamily[col].substring(0, 3)) {
                                        case "(o)":
                                            matchingVerbRow[col] = "(o)" + verbRoot + rowValuesCurrentFamily[col].substring(3, conjLatinLength);
                                            break;
                                        case "(お)":
                                            matchingVerbRow[col] = "(お)" + verbRoot + rowValuesCurrentFamily[col].substring(3, conjLatinLength);
                                            break;
                                        default:
                                            matchingVerbRow[col] = verbRoot + rowValuesCurrentFamily[col];
                                            break;
                                    }
                                } else {
                                    matchingVerbRow[col] = verbRoot + rowValuesCurrentFamily[col];
                                }
                            } else {
                                if (rowValuesCurrentException[col].equals("")) {
                                    conjLatinLength = rowValuesCurrentFamily[col].length();
                                    if (conjLatinLength > 3) {
                                        switch (rowValuesCurrentFamily[col].substring(0, 3)) {
                                            case "(o)":
                                                matchingVerbRow[col] = "(o)" + verbRoot + rowValuesCurrentFamily[col].substring(3, conjLatinLength);
                                                break;
                                            case "(お)":
                                                matchingVerbRow[col] = "(お)" + verbRoot + rowValuesCurrentFamily[col].substring(3, conjLatinLength);
                                                break;
                                            default:
                                                matchingVerbRow[col] = verbRoot + rowValuesCurrentFamily[col];
                                                break;
                                        }
                                    } else {
                                        matchingVerbRow[col] = verbRoot + rowValuesCurrentFamily[col];
                                    }
                                }
                                else { matchingVerbRow[col] = rowValuesCurrentException[col]; }
                            }
                        }
                    }
                    //endregion

                    //region Getting the basic verb characteristics
                    int conjugation = 0;
                    Basics = getVerbCharacteristics(matchingVerbRow, mTitleIndexes.get(conjugation).get(1));

                    definition = Basics.get(GlobalConstants.VerbModule_colIndex_english);
                    definition = addToDefinition(definition);
                    Basics.set(GlobalConstants.VerbModule_colIndex_english,definition);

                    type = Basics.get(GlobalConstants.VerbModule_colIndex_trans);
                    if (type.equals("T")) {Basics.set(GlobalConstants.VerbModule_colIndex_trans,"trans.");}
                    if (type.equals("I")) {Basics.set(GlobalConstants.VerbModule_colIndex_trans,"intrans.");}
                    if (type.equals("T/I")) {Basics.set(GlobalConstants.VerbModule_colIndex_trans,"trans./intrans.");}
                    matchingVerbCharacteristics.add(Basics);
                    //endregion

                    //region Getting the verb conjugations
                    for (int j = 0; j < mTitleIndexes.size() - 1; j++) {
                        conjugation++;
                        current_set = getVerbCharacteristics(matchingVerbRow, mTitleIndexes.get(conjugation).get(1));

                        //Intransitive verbs don't have a passive tense, so the relevant entries are removed
                        if (!type.equals("T") && conjugation == PassiveTenseIndex) {
                            for (int q = 0; q < current_set.size(); q++) {
                                current_set.set(q, "*");
                            }
                        }

                        // Cleaning the entries that contain exceptions
                        for (int q = 0; q<current_set.size();q++) {
                            if (current_set.get(q).contains("*")) {current_set.set(q,"*");}
                        }

                        matchingVerbCharacteristics.add(current_set);
                    }
                    //endregion

                    setOf_matchingVerbCharacteristics.add(matchingVerbCharacteristics);
                }
                setOf_matchingVerbCharacteristics_English_Kanji.add(setOf_matchingVerbCharacteristics);
                setOf_matchingVerbCharacteristics = new ArrayList<>();
            }
        }
        //endregion

        return new ArrayList<>();
    }
    public List<List<List<List<String>>>> getSetOfVerbCharacteristics(List<Integer> matchingVerbRowIndexList) {

        // Since there may be multiple matches for a given verb (e.g. if there are multiple kanjis/meanings),
        // then the verb info is a list of lists, each one including all the verb stems & conjugations
        // These lists of lists are retrieved from separate functions for simplicity, with each function covering a specfic range of conjugations

        ;//region Initializations
        List<List<List<List<String>>>> setOf_matchingVerbCharacteristics_English_Kanji = new ArrayList<>();
        List<List<List<String>>> setOf_matchingVerbCharacteristics = new ArrayList<>();
        int matchingVerbRowIndex;
        int PassiveTenseIndex = 0;
        int rowIndex_of_family_conj = 0;
        int NumberOfSheetCols = MainActivity.VerbLatinConjDatabase.get(0).length;
        int conj_length;
        List<String[]> mySheetConj = null;
        String current_title;
        String definition;
        String type;
        List<List<String>> matchingVerbCharacteristics;
        List<String> Basics;
        List<String> current_set;
        String[] rowValuesCurrentException = new String[NumberOfSheetCols];
        //endregion

        //region Find the Passive tense index in order to remove passive conjugations from verbs that are intransitive
        for (int j = 0; j< mTitleIndexes.size()-1; j++) {
            current_title = mTitlesList.get(j).get(0).get(0);
            if (current_title.contains("Passive (X is done to him)")) { PassiveTenseIndex = j; }
        }
        //endregion

        //region Building the set of verb characteristics for each matching verb
        if (matchingVerbRowIndexList.size() != 0) {
            for (int r = 0; r<2; r++) {
                if (r == 0) { mySheetConj = MainActivity.VerbLatinConjDatabase;}
                else { mySheetConj = MainActivity.VerbKanjiConjDatabase;}

                for (int p=0; p<matchingVerbRowIndexList.size(); p++) {
                    matchingVerbCharacteristics = new ArrayList<>();
                    matchingVerbRowIndex = matchingVerbRowIndexList.get(p);

                    //region Find the conjugation family relevant to this matching row index
                    for (int i = 2; i< MainActivity.VerbDatabase.size(); i++) {
                        if (MainActivity.VerbDatabase.get(i)[GlobalConstants.VerbModule_colIndex_rootLatin].equals("")
                                && MainActivity.VerbDatabase.get(i)[GlobalConstants.VerbModule_colIndex_english].equals("")
                                && !MainActivity.VerbDatabase.get(i)[GlobalConstants.VerbModule_colIndex_family].equals("")) {

                            if (i<matchingVerbRowIndex) { rowIndex_of_family_conj = i;}
                            else {break;}
                        }
                    }
                    String[] rowValuesCurrentFamily = mySheetConj.get(Integer.valueOf(MainActivity.VerbDatabase.get(rowIndex_of_family_conj)[GlobalConstants.VerbModule_colIndex_istem]));
                    //endregion

                    //region Getting the row of the current matching verb & Padding the matchingVerbRow with the values from the current index
                    String[] matchingVerbRow = mySheetConj.get(mySheetConj.size()-1); //Default matching verb row conjugations array with empty values

                    String value_root;
                    if (r == 0) { value_root = MainActivity.VerbDatabase.get(matchingVerbRowIndex)[GlobalConstants.VerbModule_colIndex_rootLatin];}
                    else        { value_root = MainActivity.VerbDatabase.get(matchingVerbRowIndex)[GlobalConstants.VerbModule_colIndex_rootKanji];}

                    String valueExceptionIndicator = MainActivity.VerbDatabase.get(matchingVerbRowIndex)[GlobalConstants.VerbModule_colIndex_istem];

                    boolean isAnException = false;
                    if (!valueExceptionIndicator.equals("")) {
                        isAnException = true;
                        rowValuesCurrentException = mySheetConj.get(Integer.valueOf(valueExceptionIndicator));
                    }

                    for (int col=0; col<NumberOfSheetCols; col++) {

                        if (col<GlobalConstants.VerbModule_colIndex_istem) {
                            matchingVerbRow[col] = MainActivity.VerbDatabase.get(matchingVerbRowIndex)[col]; }
                        else {
                            if (!isAnException) {
                                conj_length = rowValuesCurrentFamily[col].length();
                                if (conj_length > 3) {
                                    switch (rowValuesCurrentFamily[col].substring(0, 3)) {
                                        case "(o)":
                                            matchingVerbRow[col] = "(o)" + value_root + rowValuesCurrentFamily[col].substring(3, conj_length);
                                            break;
                                        case "(お)":
                                            matchingVerbRow[col] = "(お)" + value_root + rowValuesCurrentFamily[col].substring(3, conj_length);
                                            break;
                                        default:
                                            matchingVerbRow[col] = value_root + rowValuesCurrentFamily[col];
                                            break;
                                    }
                                } else {
                                    matchingVerbRow[col] = value_root + rowValuesCurrentFamily[col];
                                }
                            } else {
                                if (rowValuesCurrentException[col].equals("")) {
                                    conj_length = rowValuesCurrentFamily[col].length();
                                    if (conj_length > 3) {
                                        switch (rowValuesCurrentFamily[col].substring(0, 3)) {
                                            case "(o)":
                                                matchingVerbRow[col] = "(o)" + value_root + rowValuesCurrentFamily[col].substring(3, conj_length);
                                                break;
                                            case "(お)":
                                                matchingVerbRow[col] = "(お)" + value_root + rowValuesCurrentFamily[col].substring(3, conj_length);
                                                break;
                                            default:
                                                matchingVerbRow[col] = value_root + rowValuesCurrentFamily[col];
                                                break;
                                        }
                                    } else {
                                        matchingVerbRow[col] = value_root + rowValuesCurrentFamily[col];
                                    }
                                }
                                else { matchingVerbRow[col] = rowValuesCurrentException[col]; }
                            }
                        }
                    }
                    //endregion

                    //region Getting the basic verb characteristics
                    int conjugation = 0;
                    Basics = getVerbCharacteristics(matchingVerbRow, mTitleIndexes.get(conjugation).get(1));

                    definition = Basics.get(GlobalConstants.VerbModule_colIndex_english);
                    definition = addToDefinition(definition);
                    Basics.set(GlobalConstants.VerbModule_colIndex_english,definition);

                    type = Basics.get(GlobalConstants.VerbModule_colIndex_trans);
                    if (type.equals("T")) {Basics.set(GlobalConstants.VerbModule_colIndex_trans,"trans.");}
                    if (type.equals("I")) {Basics.set(GlobalConstants.VerbModule_colIndex_trans,"intrans.");}
                    if (type.equals("T/I")) {Basics.set(GlobalConstants.VerbModule_colIndex_trans,"trans./intrans.");}
                    matchingVerbCharacteristics.add(Basics);
                    //endregion

                    //region Getting the verb conjugations
                    for (int j = 0; j < mTitleIndexes.size() - 1; j++) {
                        conjugation++;
                        current_set = getVerbCharacteristics(matchingVerbRow, mTitleIndexes.get(conjugation).get(1));

                        //Intransitive verbs don't have a passive tense, so the relevant entries are removed
                        if (!type.equals("T") && conjugation == PassiveTenseIndex) {
                            for (int q = 0; q < current_set.size(); q++) {
                                current_set.set(q, "*");
                            }
                        }

                        // Cleaning the entries that contain exceptions
                        for (int q = 0; q<current_set.size();q++) {
                            if (current_set.get(q).contains("*")) {current_set.set(q,"*");}
                        }

                        matchingVerbCharacteristics.add(current_set);
                    }
                    //endregion

                    setOf_matchingVerbCharacteristics.add(matchingVerbCharacteristics);
                }
                setOf_matchingVerbCharacteristics_English_Kanji.add(setOf_matchingVerbCharacteristics);
                setOf_matchingVerbCharacteristics = new ArrayList<>();
            }
        }
        //endregion

        return setOf_matchingVerbCharacteristics_English_Kanji;
    }
    public List<String> getVerbCharacteristics(String[] matchingVerbRow, List<Integer> colIndexes) {

        // Value Initializations
        String value;
        int current_col;
        List<String> matchingVerbCharacteristics = new ArrayList<>();


        // Set the verb's corresponding characteristics as per the wanted values in the current row
        for (int j = 0; j < colIndexes.size(); j++) {

            current_col = colIndexes.get(j);
            value = matchingVerbRow[current_col];

            // Add that value to the list of characteristics for that verb
            matchingVerbCharacteristics.add(value);
        }

        return matchingVerbCharacteristics;
    }

    private void displayVerbsInVerbChooserSpinner() {

        if (mVerbCharacteristicsEnglish.size() != 0) {
            mVerbHintTextView.setVisibility(View.GONE);
        } else {
            mVerbHintTextView.setVisibility(View.VISIBLE);
        }

        // Populate the list of choices for the VerbChooserSpinner. Each text element of inside the individual spinner choices corresponds to a sub-element of the parsedPopulatedList
        mVerbListForSpinner = createVerbChooserSpinnerList(mVerbCharacteristicsEnglish);
        mVerbChooserSpinner.setAdapter(new VerbSpinnerAdapter(getContext(), R.layout.custom_verbchooser_spinner, mVerbListForSpinner));
        mVerbChooserSpinner.setVisibility(View.VISIBLE);

        // Action taken when choosing an option from the VerbChooserSpinner
        mVerbChooserSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int verbIndex, long id) {
                showSelectedVerbConjugations(verbIndex);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }
    private void showSelectedVerbConjugations(final int verbIndex) {

        if (getActivity()!=null) Utilities.hideSoftKeyboard(getActivity());
        if (mVerbCharacteristicsEnglish.size() == 0) return;

        mVerbConjugationsListForSpinner = createconjugationChooserSpinnerList(mTitlesList, mVerbCharacteristicsEnglish.get(verbIndex));
        mRequestedConjugationSpinnerIndex = getIndexOfRequestedVerbConjugationCategory(verbIndex);

        mConjugationChooserSpinner.setAdapter(new ConjugationsSpinnerAdapter(getContext(),
                R.layout.custom_conjugationchooser_spinner,
                mVerbConjugationsListForSpinner));

        //Hide the subsequent fields of there is nothing to show
        mConjugationsContainerScrollView.setVisibility(View.VISIBLE);
        if (mVerbCharacteristicsEnglish.get(verbIndex).size() == 0) {
            mConjugationsContainerScrollView.setVisibility(View.GONE);
        }

        mConjugationChooserSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, final int conjugationIndex, long id) {
                showSelectedConjugationsInCategory(verbIndex, conjugationIndex);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        // Set the second spinner to the position in the previous instance (code does not give wanted result)
        mConjugationChooserSpinner.setSelection(mRequestedConjugationSpinnerIndex - 1);
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
        boolean TypeisKanji = types.get(2);

        mChosenRomajiOrKanji = "Romaji";
        if (TypeisKanji) {
            mChosenRomajiOrKanji = "Kanji";
            mRomajiRadioButton.setChecked(false);
            mKanjiRadioButton.setChecked(true);
        } else {
            mRomajiRadioButton.setChecked(true);
            mKanjiRadioButton.setChecked(false);
        }

        displayConjugationsOfSelectedCategory(verbIndex, conjugationIndex);
    }
    private int getIndexOfRequestedVerbConjugationCategory(int position) {

        // Find the Conjugation family corresponding to the column hit from the FindMatchingVerbIndex function

        int corresponding_title_index = 1;
        int column_for_selected_row;
        int index_counter = 0;
        if (mMatchingVerbColIndexList.size() != 0) {
            column_for_selected_row = mMatchingVerbColIndexList.get(position);
            for (int i = 0; i < mTitleIndexes.size(); i++) {
                for (int j = 0; j < mTitleIndexes.get(i).get(1).size(); j++) {
                    if (column_for_selected_row == index_counter) {
                        corresponding_title_index = i;
                    }
                    index_counter++;
                }
            }
        }
        // The basics row is not relevant to the second spinner, so the Simple Form is chosen instead
        if (corresponding_title_index == 0) {
            corresponding_title_index = 1;
        }
        return corresponding_title_index;
    }
    public List<List<String>> createVerbChooserSpinnerList(List<List<List<String>>> verbCharacteristics) {

            List<List<String>> populatedlist = new ArrayList<>();
            List<String> inside_list = new ArrayList<>();
            String SpinnerText = "";
            String SpinnerText1 = "";
            String SpinnerText2 = "";

            if (verbCharacteristics.size() == 0) {
                if (mInputQuery.equals("")) {
                    SpinnerText1 = getResources().getString(R.string.PleaseEnterJapVerb);
                    SpinnerText2 = getResources().getString(R.string.CanEnterEngVerb);
                }
                else {
                    SpinnerText1 = getResources().getString(R.string.NoMatchFound);
                    SpinnerText2 = getResources().getString(R.string.SeeHints);
                }
                inside_list.add(SpinnerText1);
                inside_list.add("");
                inside_list.add(SpinnerText2);
                inside_list.add("");
                inside_list.add(SpinnerText);
                inside_list.add("");
                populatedlist.add(inside_list);
            }
            else {
                for (int i=0; i<verbCharacteristics.size(); i++) {
                    inside_list = new ArrayList<>();
                    if (verbCharacteristics.get(i)
                            .get(GlobalConstants.VerbModule_Conjugations_Basics)
                            .get(GlobalConstants.VerbModule_colIndex_prep).equals("")) { SpinnerText = ""; }
                    else { SpinnerText =
                            "[" +
                            verbCharacteristics.get(i)
                            .get(GlobalConstants.VerbModule_Conjugations_Basics)
                            .get(GlobalConstants.VerbModule_colIndex_prep)
                            + "] "; }
                    inside_list.add(SpinnerText);

                    SpinnerText =
                        verbCharacteristics.get(i)
                        .get(GlobalConstants.VerbModule_Conjugations_Basics)
                        .get(GlobalConstants.VerbModule_colIndex_kanji)
                        + " (" +
                        verbCharacteristics.get(i)
                        .get(GlobalConstants.VerbModule_Conjugations_Stems)
                        .get(GlobalConstants.VerbModule_colIndex_ustem - GlobalConstants.VerbModule_colIndex_kanji - 1) // Since the Stems list is the second list, the length of the first list must be subtracted
                        + ")"	;
                    inside_list.add(SpinnerText);

                    if (!verbCharacteristics.get(i)
                        .get(GlobalConstants.VerbModule_Conjugations_Basics)
                        .get(GlobalConstants.VerbModule_colIndex_family).equals("")) {
                        SpinnerText = verbCharacteristics.get(i)
                                .get(GlobalConstants.VerbModule_Conjugations_Basics)
                                .get(GlobalConstants.VerbModule_colIndex_family);
                        if (!verbCharacteristics.get(i)
                            .get(GlobalConstants.VerbModule_Conjugations_Basics)
                            .get(GlobalConstants.VerbModule_colIndex_trans).equals("")) {
                            SpinnerText = SpinnerText + ", "
                                    + verbCharacteristics.get(i)
                                    .get(GlobalConstants.VerbModule_Conjugations_Basics)
                                    .get(GlobalConstants.VerbModule_colIndex_trans);
                            }
                    }
                    else {
                        if (!verbCharacteristics.get(i)
                            .get(GlobalConstants.VerbModule_Conjugations_Basics)
                            .get(GlobalConstants.VerbModule_colIndex_trans).equals("")) {
                            SpinnerText =
                                    verbCharacteristics.get(i)
                                    .get(GlobalConstants.VerbModule_Conjugations_Basics)
                                    .get(GlobalConstants.VerbModule_colIndex_trans);
                            }
                        else { SpinnerText = ""; }
                    }
                    inside_list.add(SpinnerText);

                    SpinnerText =
                        verbCharacteristics.get(i)
                        .get(GlobalConstants.VerbModule_Conjugations_Basics)
                        .get(GlobalConstants.VerbModule_colIndex_english) ;
                    inside_list.add(SpinnerText);

                    SpinnerText ="";
                    inside_list.add(SpinnerText);


                    populatedlist.add(inside_list);
                }
            }
        return populatedlist;
    }
    public List<List<String>> createconjugationChooserSpinnerList(List<List<List<String>>> TitlesList, List<List<String>> chosenVerbCharacteristics) {

        // Populate the list of choices for the ConjugationChooserSpinner

        List<List<String>> populatedlist = new ArrayList<>();
        List<String> inside_list = new ArrayList<>();
        String SpinnerText;
        int index;

        if (chosenVerbCharacteristics.size() == 0) {
            SpinnerText = "No match found.";
            inside_list.add(SpinnerText);
            inside_list.add("");
            populatedlist.add(inside_list);
        }
        else {
            for (int i=1; i<TitlesList.size(); i++) {
                inside_list = new ArrayList<>();
                index = i;
                SpinnerText = index + ". " + TitlesList.get(i).get(0).get(0);
                inside_list.add(SpinnerText);

                SpinnerText = chosenVerbCharacteristics.get(i).get(0); // Display the first element in the Conjugation, e.g. PrPlA in Simple Form
                inside_list.add(SpinnerText);

                populatedlist.add(inside_list);
            }
        }
    return populatedlist;
}
    public void displayConjugationsOfSelectedCategory(int verbIndex, int conjugationIndex) {
        if (getActivity()==null) return;

        List<TextView> Tense = new ArrayList<>();
        List<LinearLayout> TenseLayout = new ArrayList<>();
        List<TextView> Tense_Result = new ArrayList<>();

        Tense.add((TextView) getActivity().findViewById(R.id.Tense0));
        Tense.add((TextView) getActivity().findViewById(R.id.Tense1));
        Tense.add((TextView) getActivity().findViewById(R.id.Tense2));
        Tense.add((TextView) getActivity().findViewById(R.id.Tense3));
        Tense.add((TextView) getActivity().findViewById(R.id.Tense4));
        Tense.add((TextView) getActivity().findViewById(R.id.Tense5));
        Tense.add((TextView) getActivity().findViewById(R.id.Tense6));
        Tense.add((TextView) getActivity().findViewById(R.id.Tense7));
        Tense.add((TextView) getActivity().findViewById(R.id.Tense8));
        Tense.add((TextView) getActivity().findViewById(R.id.Tense9));
        Tense.add((TextView) getActivity().findViewById(R.id.Tense10));
        Tense.add((TextView) getActivity().findViewById(R.id.Tense11));
        Tense.add((TextView) getActivity().findViewById(R.id.Tense12));
        Tense.add((TextView) getActivity().findViewById(R.id.Tense13));
        TenseLayout.add((LinearLayout) getActivity().findViewById(R.id.TenseLayout0));
        TenseLayout.add((LinearLayout) getActivity().findViewById(R.id.TenseLayout1));
        TenseLayout.add((LinearLayout) getActivity().findViewById(R.id.TenseLayout2));
        TenseLayout.add((LinearLayout) getActivity().findViewById(R.id.TenseLayout3));
        TenseLayout.add((LinearLayout) getActivity().findViewById(R.id.TenseLayout4));
        TenseLayout.add((LinearLayout) getActivity().findViewById(R.id.TenseLayout5));
        TenseLayout.add((LinearLayout) getActivity().findViewById(R.id.TenseLayout6));
        TenseLayout.add((LinearLayout) getActivity().findViewById(R.id.TenseLayout7));
        TenseLayout.add((LinearLayout) getActivity().findViewById(R.id.TenseLayout8));
        TenseLayout.add((LinearLayout) getActivity().findViewById(R.id.TenseLayout9));
        TenseLayout.add((LinearLayout) getActivity().findViewById(R.id.TenseLayout10));
        TenseLayout.add((LinearLayout) getActivity().findViewById(R.id.TenseLayout11));
        TenseLayout.add((LinearLayout) getActivity().findViewById(R.id.TenseLayout12));
        TenseLayout.add((LinearLayout) getActivity().findViewById(R.id.TenseLayout13));
        Tense_Result.add((TextView) getActivity().findViewById(R.id.Tense0_Result));
        Tense_Result.add((TextView) getActivity().findViewById(R.id.Tense1_Result));
        Tense_Result.add((TextView) getActivity().findViewById(R.id.Tense2_Result));
        Tense_Result.add((TextView) getActivity().findViewById(R.id.Tense3_Result));
        Tense_Result.add((TextView) getActivity().findViewById(R.id.Tense4_Result));
        Tense_Result.add((TextView) getActivity().findViewById(R.id.Tense5_Result));
        Tense_Result.add((TextView) getActivity().findViewById(R.id.Tense6_Result));
        Tense_Result.add((TextView) getActivity().findViewById(R.id.Tense7_Result));
        Tense_Result.add((TextView) getActivity().findViewById(R.id.Tense8_Result));
        Tense_Result.add((TextView) getActivity().findViewById(R.id.Tense9_Result));
        Tense_Result.add((TextView) getActivity().findViewById(R.id.Tense10_Result));
        Tense_Result.add((TextView) getActivity().findViewById(R.id.Tense11_Result));
        Tense_Result.add((TextView) getActivity().findViewById(R.id.Tense12_Result));
        Tense_Result.add((TextView) getActivity().findViewById(R.id.Tense13_Result));

        for (int i=0;i<Tense.size();i++) {
            Tense.get(i).setText("");
            TenseLayout.get(i).setVisibility(View.GONE);
            Tense_Result.get(i).setText("");
        }

        String displayed_text_English;
        String displayed_text_Kanji;

        if (mVerbCharacteristicsEnglish.get(verbIndex).size() != 0) {
            for (int i = 0; i< mTitlesList.get(conjugationIndex +1).get(1).size(); i++) {

                displayed_text_English = mVerbCharacteristicsEnglish.get(verbIndex).get(conjugationIndex +1).get(i);
                displayed_text_Kanji = mVerbCharacteristicsKanji.get(verbIndex).get(conjugationIndex +1).get(i);

                Tense.get(i).setText(mTitlesList.get(conjugationIndex + 1).get(1).get(i));
                if (mChosenRomajiOrKanji.equals("Romaji")) Tense_Result.get(i).setText(displayed_text_English);
                else Tense_Result.get(i).setText( displayed_text_Kanji );

                TenseLayout.get(i).setVisibility(View.VISIBLE);
            }
        }
    }
    private class VerbSpinnerAdapter extends ArrayAdapter<List<String>> {
        // Code adapted from http://mrbool.com/how-to-customize-spinner-in-android/28286
        VerbSpinnerAdapter(Context ctx, int txtViewResourceId, List<List<String>> list) {
            super(ctx, txtViewResourceId, list);
            }
        @Override public View getDropDownView( int position, View cnvtView, ViewGroup prnt) {
            return getCustomView(position, cnvtView, prnt);
        }
        @NonNull @Override public View getView(int pos, View cnvtView, ViewGroup prnt) {
            return getCustomView(pos, cnvtView, prnt);
        }
        View getCustomView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = LayoutInflater.from(getActivity().getBaseContext());
            View mySpinner = inflater.inflate(R.layout.custom_verbchooser_spinner, parent, false);
            TextView verbchooser_Prep = (TextView) mySpinner.findViewById(R.id.verbchooser_Prep);
            verbchooser_Prep.setText(mVerbListForSpinner.get(position).get(0));
            TextView verbchooser_Kanji_and_ustem = (TextView) mySpinner.findViewById(R.id.verbchooser_Kanji_and_ustem);
            verbchooser_Kanji_and_ustem.setText(mVerbListForSpinner.get(position).get(1));
            TextView verbchooser_Characteristics = (TextView) mySpinner.findViewById(R.id.verbchooser_Characteristics);
            verbchooser_Characteristics.setText(mVerbListForSpinner.get(position).get(2));
            TextView verbchooser_EnglishMeaning = (TextView) mySpinner.findViewById(R.id.verbchooser_EnglishMeaning);
            verbchooser_EnglishMeaning.setText(mVerbListForSpinner.get(position).get(3));

            return mySpinner;
        }
    }
    private class ConjugationsSpinnerAdapter extends ArrayAdapter<List<String>> {
        // Code adapted from http://mrbool.com/how-to-customize-spinner-in-android/28286
        ConjugationsSpinnerAdapter(Context ctx, int txtViewResourceId, List<List<String>> list) {
            super(ctx, txtViewResourceId, list);
            }
        @Override public View getDropDownView( int position, View cnvtView, ViewGroup prnt) {
            return getCustomView(position, cnvtView, prnt);
        }
        @Override public View getView(int pos, View cnvtView, ViewGroup prnt) {
            return getCustomView(pos, cnvtView, prnt);
        }
        View getCustomView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = LayoutInflater.from(getActivity().getBaseContext());
            View mySpinner = inflater.inflate(R.layout.custom_conjugationchooser_spinner, parent, false);
            TextView Upper_text = (TextView) mySpinner.findViewById(R.id.UpperPart);
            Upper_text.setText(mVerbConjugationsListForSpinner.get(position).get(0));
            TextView Lower_text = (TextView) mySpinner.findViewById(R.id.LowerPart);
            Lower_text.setText(mVerbConjugationsListForSpinner.get(position).get(1));

            return mySpinner;
        }
    }
    static public List<Boolean> FindType(String verb) {
        String text_type = ConvertFragment.TextType(verb);

        boolean TypeisLatin   = false;
        boolean TypeisKana    = false;
        boolean TypeisKanji   = false;
        boolean TypeisInvalid = false;

        if ( text_type.equals("latin") )                                     { TypeisLatin = true;}
        if ( text_type.equals("hiragana") || text_type.equals("katakana") )  { TypeisKana = true;}
        if ( text_type.equals("kanji") )                                     { TypeisKanji = true;}
        if ( verb.contains("*") || verb.contains("＊") || verb.equals("") || text_type.equals("number") ) { TypeisInvalid = true;}

        List<Boolean> types = new ArrayList<>();
        types.add(TypeisLatin);
        types.add(TypeisKana);
        types.add(TypeisKanji);
        types.add(TypeisInvalid);
        return types;
    }


}