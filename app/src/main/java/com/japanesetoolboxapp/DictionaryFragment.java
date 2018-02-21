package com.japanesetoolboxapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.japanesetoolboxapp.utiities.GlobalConstants;
import com.japanesetoolboxapp.utiities.SharedMethods;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class DictionaryFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<Object>>{

    // Parameters
    private List<Object> mMatchingWordCharacteristics;
    private ArrayList<String> mGlobalGrammarSpinnerList_element0;
    private String mLastSearchedWord;
    private List<List<Integer>> mMatchingWordRowColIndexList;
    private int mMaxNumberOfResultsShown = 50;
    private Activity mFragmentActivity;

    private Boolean mAppWasInBackground;
    private GrammarExpandableListAdapter mSearchResultsListAdapter;
    private ExpandableListView mSearchResultsExpandableListView;
    private List<String> mListDataHeader;
    private HashMap<String, List<String>> mListHeaderDetails;
    private HashMap<String, List<List<String>>> mListDataChild;
    private String mSearchedWord;
    private List<Object> mAsyncMatchingWordCharacteristics;
    private Boolean mInternetIsAvailable;
    private static final int JISHO_WEB_SEARCH_LOADER = 41;
    private static final String JISHO_LOADER_INPUT_EXTRA = "input";
    Boolean matchFound;
    Toast mShowOnlineResultsToast;
    private boolean searchResultsAlreadyDisplayed;
    SharedMethods mSharedMethods;

    // Fragment Lifecycle Functions
    @Override public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) this.mFragmentActivity = (Activity) context;

        // This makes sure that the container activity has implemented the callback interface. If not, it throws an exception
        try {
            mCallbackWord = (UserWantsNewSearchForSelectedWordListener) mFragmentActivity;
        } catch (ClassCastException e) {
            throw new ClassCastException(mFragmentActivity.toString() + " must implement TextClicked");
        }
        try {
            mCallbackVerb = (UserWantsToConjugateFoundVerbListener) mFragmentActivity;
        } catch (ClassCastException e) {
            throw new ClassCastException(mFragmentActivity.toString() + " must implement TextClicked");
        }

   }
    @Override public void onCreate(Bundle savedInstanceState) { //instead of onActivityCreated
        super.onCreate(savedInstanceState);
        //super.onActivityCreated(savedInstanceState);
        mSharedMethods = new SharedMethods();
        mInternetIsAvailable = SharedMethods.internetIsAvailableCheck(this.getContext());

    }
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Retain this fragment (used to save user inputs on activity creation/destruction)
        setRetainInstance(true);
        searchResultsAlreadyDisplayed = false;

        // Define that this fragment is related to fragment_dictionary.xml
        final View fragmentView = inflater.inflate(R.layout.fragment_dictionary, container, false);

        return fragmentView;
    }
    @Override public void onPause() {
        super.onPause();
        mAppWasInBackground = true;
    }
    @Override public void onResume() {
        super.onResume();

        Boolean userHasRequestedDictSearch = checkIfUserRequestedDictSearch();

        if (userHasRequestedDictSearch == null || userHasRequestedDictSearch) {
            if (getArguments() != null && !searchResultsAlreadyDisplayed) {
                String outputFromInputQueryFragment = getArguments().getString("input_to_fragment");
                searchResultsAlreadyDisplayed = true;

                //If the application is resumed (switched to), then display the last results instead of performing a new search on the last input
                if (mAppWasInBackground == null || !mAppWasInBackground) {
                    mAppWasInBackground = false;

                    // Get the row index of the words matching the user's entry
                    mMatchingWordRowColIndexList = FindMatchingWordIndex(outputFromInputQueryFragment);

                    SearchInDictionary(outputFromInputQueryFragment);
                }
            } else {
                searchResultsAlreadyDisplayed = false;
            }
        }
    }
    @Override public void onDetach() {
        super.onDetach();
        this.mFragmentActivity = null;
    }
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        // save excel results to display in spinners, load the results on activity restart
        savedInstanceState.putStringArrayList("mGlobalGrammarSpinnerList_element0", mGlobalGrammarSpinnerList_element0);
    }

    //Asynchronous methods
    @Override public Loader<List<Object>> onCreateLoader(int id, final Bundle args) {
        return new AsyncTaskLoader<List<Object>>(getContext()) {

            @Override
            protected void onStartLoading() {

                /* If no arguments were passed, we don't have a inputQueryAutoCompleteTextView to perform. Simply return. */
                if (args == null) return;

                forceLoad();
            }

            @Override
            public List<Object> loadInBackground() {

                List<Object> AsyncMatchingWordCharacteristics = new ArrayList<>();

                if (mAppWasInBackground == null || !mAppWasInBackground) {
                    if (mInternetIsAvailable) {
                        try {
                            String speechRecognizerString = args.getString(JISHO_LOADER_INPUT_EXTRA);
                            AsyncMatchingWordCharacteristics = SharedMethods.getResultsFromWeb(speechRecognizerString, getActivity());
                        } catch (IOException e) {
                            //throw new RuntimeException(e);
                        }
                    } else {
                        Log.i("Diagnosis Time", "Failed to access online resources.");
                        SharedMethods.TellUserIfThereIsNoInternetConnection(getActivity());
                        cancelLoadInBackground();
                    }
                }
                return AsyncMatchingWordCharacteristics;
            }
        };
    }
    @Override public void onLoadFinished(Loader<List<Object>> loader, List<Object> data) {
        mAsyncMatchingWordCharacteristics = data;

        if (mAppWasInBackground == null || !mAppWasInBackground) {
            Boolean showOnlineResults = getShowOnlineResultsPreference();
            if (mAsyncMatchingWordCharacteristics.size() != 0 && showOnlineResults) {
                matchFound = true;
                compareMatchingWordCharacteristics();
                displayResults(mSearchedWord);
            }

            //If no dictionary match was found, then this is probably a verb conjugation, so try that
            if (!matchFound && !mSearchedWord.equals("") && getActivity() != null) {
                if (mShowOnlineResultsToast != null) mShowOnlineResultsToast.cancel();
                getActivity().findViewById(R.id.button_searchVerb).performClick();
            }
        }
    }
    @Override public void onLoaderReset(Loader<List<Object>> loader) {}
    public Boolean getShowOnlineResultsPreference() {
        Boolean showOnlineResults = false;
        if (getActivity()!=null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            showOnlineResults = sharedPreferences.getBoolean(getString(R.string.pref_complete_local_with_online_search_key),
                    getResources().getBoolean(R.bool.pref_complete_local_with_online_search_default));
        }
        return showOnlineResults;
    }

	// Functionality Functions
    public void SearchInDictionary(String word) {

        final List<Integer> matchingWordRowIndexList = mMatchingWordRowColIndexList.get(0); // Use mMatchingWordRowColIndexList.get(1) to get columns
        mSearchedWord = word;
        mLastSearchedWord = word;

        // Run the Grammar Module on the input word
        matchFound = true;
        mMatchingWordCharacteristics = getCharacteristicsForAllHits(matchingWordRowIndexList);
        if (mMatchingWordCharacteristics.size() == 0) matchFound = false;

        // If there are no results, retrieve the results from Jisho.org
        mShowOnlineResultsToast = Toast.makeText(getContext(), getResources().getString(R.string.showOnlineResultsToastString), Toast.LENGTH_SHORT);
        mAsyncMatchingWordCharacteristics = new ArrayList<>();

        //Getting user preference for showing online results
        Boolean showOnlineResults = getShowOnlineResultsPreference();

        //Attempting to access jisho.org to complete the results found in the local dictionary
        if (getActivity()!=null && showOnlineResults) {
            if (!mSearchedWord.equals("")) mShowOnlineResultsToast.show();

            Bundle queryBundle = new Bundle();
            queryBundle.putString(JISHO_LOADER_INPUT_EXTRA, mSearchedWord);

            LoaderManager loaderManager = getActivity().getSupportLoaderManager();
            Loader<String> JishoWebSearchLoader = loaderManager.getLoader(JISHO_WEB_SEARCH_LOADER);
            if (JishoWebSearchLoader == null) loaderManager.initLoader(JISHO_WEB_SEARCH_LOADER, queryBundle, this);
            else loaderManager.restartLoader(JISHO_WEB_SEARCH_LOADER, queryBundle, this);
        }

        displayResults(mSearchedWord);

        if (!matchFound && !mSearchedWord.equals("") && !showOnlineResults) {
            if (mShowOnlineResultsToast!=null) mShowOnlineResultsToast.cancel();
            getActivity().findViewById(R.id.button_searchVerb).performClick();
        }

    }
    public void displayResults(String word) {

        // Populate the list of choices for the SearchResultsChooserSpinner. Each text element of inside the idividual spinner choices corresponds to a sub-element of the choicelist
        populateSearchResultsForDisplay(word, mMatchingWordCharacteristics);

        SharedMethods.hideSoftKeyboard(getActivity());
        // Implementing the SearchResultsChooserListView
        try {
            if (mListDataHeader != null && mListHeaderDetails != null && mListDataChild != null) {
                mSearchResultsListAdapter = new GrammarExpandableListAdapter(getContext(), mListDataHeader, mListHeaderDetails, mListDataChild);
                mSearchResultsExpandableListView = getView().findViewById(R.id.SentenceConstructionExpandableListView); //CAUSED CRASH
                mSearchResultsExpandableListView.setAdapter(mSearchResultsListAdapter);
                mSearchResultsExpandableListView.setVisibility(View.VISIBLE);
            }
        }
        catch (java.lang.NullPointerException e) {
            Intent intent = new Intent(getContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }
    private void compareMatchingWordCharacteristics() {
        List<Object> finalList = new ArrayList<>();
        List<Object> finallist_element = new ArrayList<>();
        List<Object> current_async_meaning_blocks = new ArrayList<>();
        List<Object> current_local_meaning_blocks = new ArrayList<>();
        List<Object> current_async_meaning_block;
        List<Object> current_local_meaning_block;
        String current_local_meaning = "";
        String current_local_type;
        String current_async_meaning = "";
        String current_async_romaji;
        String current_async_kanji;
        String current_local_romaji;
        String current_local_kanji;
        String current_local_altspellings;
        List<Object> current_final_meaning_blocks = new ArrayList<>();
        List<Object> FinalAsyncElements = new ArrayList<>();
        Boolean async_meaning_found_locally;

        FinalAsyncElements.addAll(mAsyncMatchingWordCharacteristics);

        for (int j = 0; j< mMatchingWordCharacteristics.size(); j++) {
            List<Object> current_match_element = (List<Object>) mMatchingWordCharacteristics.get(j);
            finallist_element = new ArrayList<>();
            current_local_romaji = (String) current_match_element.get(0);
            finallist_element.add(current_local_romaji);
            current_local_kanji = (String) current_match_element.get(1);
            finallist_element.add(current_local_kanji);
            current_local_altspellings = (String) current_match_element.get(2);
            finallist_element.add(current_local_altspellings);

            current_local_meaning_blocks = (List<Object>) current_match_element.get(3);
            current_final_meaning_blocks = new ArrayList<>();
            current_final_meaning_blocks.addAll(current_local_meaning_blocks);

            int current_index = FinalAsyncElements.size()-1;
            while (current_index >= 0 && FinalAsyncElements.size() != 0) {

                if (current_index > FinalAsyncElements.size()-1) {break;}
                List<Object> current_async_element = (List<Object>) FinalAsyncElements.get(current_index);
                current_async_romaji = (String) current_async_element.get(0);
                current_async_kanji = (String) current_async_element.get(1);
                current_async_meaning_blocks = (List<Object>) current_async_element.get(3);

                if (current_async_romaji.equals(current_local_romaji) && current_async_kanji.equals(current_local_kanji)) {

                    for (int m = 0; m< current_async_meaning_blocks.size(); m++) {
                        current_async_meaning_block = (List<Object>) current_async_meaning_blocks.get(m);
                        current_async_meaning = (String) current_async_meaning_block.get(0);

                        async_meaning_found_locally = false;
                        for (int k = 0; k< current_local_meaning_blocks.size(); k++) {
                            current_local_meaning_block = (List<Object>) current_local_meaning_blocks.get(k);
                            current_local_meaning = (String) current_local_meaning_block.get(0);

                            if (current_local_meaning.contains(current_async_meaning)) {
                                async_meaning_found_locally = true;
                                break;
                            }
                        }
                        if (!async_meaning_found_locally) {
                            current_final_meaning_blocks.add(current_async_meaning_block);
                        }
                    }
                    FinalAsyncElements.remove(current_index);
                    if (current_index == 0) break;
                }
                else {
                    current_index -= 1;
                }
            }
            finallist_element.add(current_final_meaning_blocks);
            finalList.add(finallist_element);
        }
        finalList.addAll(FinalAsyncElements);

        mMatchingWordCharacteristics = finalList;
    }
    public void populateSearchResultsForDisplay(String word, List<Object> MatchingHitsCharacteristics) {

        //Initialization
        mListDataHeader = new ArrayList<>();
        mListHeaderDetails = new HashMap<>();
        mListDataChild = new HashMap<>();

        String current_explanation = "XXX";
        String current_rule = "";
        String example_English = "";
        String example_Romaji = "";
        String example_Kanji = "";
        String current_antonym = "";
        String current_synonym = "";
        String altspellings = "";
        List<List<String>> elements_of_child = new ArrayList<>();
        List<String> texts_in_elements_of_child = new ArrayList<>();
        List<String> texts_in_elements_of_group = new ArrayList<>();
        String ListElement;
        String ListElement1;
        String ListElement2;
        String current_meaning;
        String current_type = "";
        List<Object> current_MatchingHitsCharacteristics_Meanings_Block;

        if (MatchingHitsCharacteristics.size() == 0) {
            //Create a generic answer for the empty search
            if (word.equals("")) {
                ListElement1 = getResources().getString(R.string.PleaseEnterWord);
                ListElement2 = "";
            }
            else {
                ListElement1 = getResources().getString(R.string.NoMatchFound);
                ListElement2 = "";
            }
            texts_in_elements_of_child.add(ListElement1);
            texts_in_elements_of_child.add(ListElement2);
            texts_in_elements_of_child.add("");
            texts_in_elements_of_child.add("");
            texts_in_elements_of_child.add("");
            texts_in_elements_of_child.add("");
            texts_in_elements_of_child.add("");
            texts_in_elements_of_child.add("");
            mListDataHeader.add(Integer.toString(0));
            mListHeaderDetails.put(Integer.toString(0), texts_in_elements_of_child);

            texts_in_elements_of_child = new ArrayList<>();
            texts_in_elements_of_child.add("No match found.");
            texts_in_elements_of_child.add("");
            texts_in_elements_of_child.add("");
            texts_in_elements_of_child.add("");
            texts_in_elements_of_child.add("");
            texts_in_elements_of_child.add("");
            texts_in_elements_of_child.add("");
            texts_in_elements_of_child.add("");
            elements_of_child = new ArrayList<>();
            elements_of_child.add(texts_in_elements_of_child);
            mListDataChild.put(Integer.toString(0), elements_of_child);
        }
        else {
            for (int i = 0; i< MatchingHitsCharacteristics.size(); i++) {

                List<Object> current_MatchingHitsCharacteristics = (List<Object>) MatchingHitsCharacteristics.get(i);
                String romaji_value = (String) current_MatchingHitsCharacteristics.get(0);
                String kanji_value = (String) current_MatchingHitsCharacteristics.get(1);
                String altspellings_value = (String) current_MatchingHitsCharacteristics.get(2);
                List<Object> current_MatchingHitsCharacteristics_Meaning_Blocks = (List<Object>) current_MatchingHitsCharacteristics.get(3);

                //Populate elements in mListDataHeader
                if (i> mMaxNumberOfResultsShown) {break;}

                texts_in_elements_of_group = new ArrayList<>();

                String cumulative_meaning_value = "";
                for (int j=0; j<current_MatchingHitsCharacteristics_Meaning_Blocks.size(); j++) {
                    current_MatchingHitsCharacteristics_Meanings_Block = (List<Object>) current_MatchingHitsCharacteristics_Meaning_Blocks.get(j);
                    cumulative_meaning_value += (String) current_MatchingHitsCharacteristics_Meanings_Block.get(0);
                    current_type    = (String) current_MatchingHitsCharacteristics_Meanings_Block.get(1);
                    if (j<current_MatchingHitsCharacteristics_Meaning_Blocks.size()-1) { cumulative_meaning_value += ", "; }
                }
                texts_in_elements_of_group.add(romaji_value);
                texts_in_elements_of_group.add(kanji_value);
                texts_in_elements_of_group.add(removeDuplicatesFromCommaList(cumulative_meaning_value));

                mListDataHeader.add(Integer.toString(i));
                mListHeaderDetails.put(Integer.toString(i), texts_in_elements_of_group);

                //Populate elements in mListDataChild
                elements_of_child = new ArrayList<>();

                texts_in_elements_of_child = new ArrayList<>();
                texts_in_elements_of_child.add(altspellings_value);
                texts_in_elements_of_child.add(current_type);
                elements_of_child.add(texts_in_elements_of_child);

                for (int j = 0; j< current_MatchingHitsCharacteristics_Meaning_Blocks.size(); j++) {
                    texts_in_elements_of_child = new ArrayList<>();

                    current_MatchingHitsCharacteristics_Meanings_Block = (List<Object>) current_MatchingHitsCharacteristics_Meaning_Blocks.get(j);
                    current_meaning = (String) current_MatchingHitsCharacteristics_Meanings_Block.get(0);
                    current_type    = (String) current_MatchingHitsCharacteristics_Meanings_Block.get(1);
                    current_antonym = (String) current_MatchingHitsCharacteristics_Meanings_Block.get(2);
                    current_synonym = (String) current_MatchingHitsCharacteristics_Meanings_Block.get(3);

                    texts_in_elements_of_child.add(""); //Altspellings placeholder
                    texts_in_elements_of_child.add(current_type);
                    texts_in_elements_of_child.add(current_meaning);
                    texts_in_elements_of_child.add(current_antonym);
                    texts_in_elements_of_child.add(current_synonym);

                    List<Object> current_MatchingHitsCharacteristics_Explanation_Blocks = (List<Object>) current_MatchingHitsCharacteristics_Meanings_Block.get(4);
                    for (int m=0; m<current_MatchingHitsCharacteristics_Explanation_Blocks.size(); m++) {
                        List<String> current_MatchingHitsCharacteristics_Explanations_Block = (List<String>) current_MatchingHitsCharacteristics_Explanation_Blocks.get(m);
                        current_explanation = current_MatchingHitsCharacteristics_Explanations_Block.get(0);
                        current_rule = current_MatchingHitsCharacteristics_Explanations_Block.get(1);
                        texts_in_elements_of_child.add("EXPL" + current_explanation);
                        texts_in_elements_of_child.add("RULE" + current_rule);

                        int number_of_examples = 0;
                        if (current_MatchingHitsCharacteristics_Explanations_Block.size() > 2) {
                            number_of_examples = (current_MatchingHitsCharacteristics_Explanations_Block.size() - 2) / 3;
                        }
                        for (int k = 0; k < number_of_examples; k++) {
                            example_English = current_MatchingHitsCharacteristics_Explanations_Block.get(2 + k*3);
                            example_Romaji = current_MatchingHitsCharacteristics_Explanations_Block.get(2 + k*3 + 1);
                            example_Kanji = current_MatchingHitsCharacteristics_Explanations_Block.get(2 + k*3 + 2);
                            texts_in_elements_of_child.add("EXMP" + example_English);
                            texts_in_elements_of_child.add("EXMP" + example_Romaji);
                            texts_in_elements_of_child.add("EXMP" + example_Kanji);
                        }
                    }

                    elements_of_child.add(texts_in_elements_of_child);
                }
                mListDataChild.put(Integer.toString(i), elements_of_child);

            }
        }
    }

    private class   WordClickableSpan extends ClickableSpan{
        // code extracted from http://stackoverflow.com/questions/15475907/make-parts-of-textview-clickable-not-url
        public void onClick(View textView) {
            //enter the ext as input word
            // code extracted from http://stackoverflow.com/questions/19750458/android-clickablespan-get-text-onclick

            TextView text = (TextView) textView;
            Spanned s = (Spanned) text.getText();
            int start = s.getSpanStart(this);
            int end = s.getSpanEnd(this);

            //Toast.makeText(GlobalGrammarModuleFragmentView.getContext(), "Clicked", Toast.LENGTH_SHORT).show();

            // Instead of implementing the direct text change in the InputQueryFragment (this can cause bugs in the long run), it is sent through an interface
            //This is the code that's avoided
                    //AutoCompleteTextView queryInit = (AutoCompleteTextView)InputQueryFragment.GlobalInputQueryFragment.findViewById(R.id.inputQueryAutoCompleteTextView);
                    //queryInit.setText(text.getText().subSequence(start, end));

            //The following code "initializes" the interface, since it is not necessarily called (initialized) when the grammar fragment receives the inputQueryAutoCompleteTextView and is activated
                   try {
                        mCallbackWord = (UserWantsNewSearchForSelectedWordListener) getActivity();
                   } catch (ClassCastException e) {
                        throw new ClassCastException(getActivity().toString() + " must implement TextClicked");
                   }

                //Calling the interface
                    String outputText = text.getText().subSequence(start, end).toString();
                    WordSelectedAction(outputText);

       }
        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setColor(getResources().getColor(R.color.textColorDictionarySpanClicked));
            ds.setUnderlineText(false);
       }
    }
    private class   VerbClickableSpan extends ClickableSpan {
        // code extracted from http://stackoverflow.com/questions/15475907/make-parts-of-textview-clickable-not-url
        public void onClick(View textView) {
            //enter the ext as input word
            // code extracted from http://stackoverflow.com/questions/19750458/android-clickablespan-get-text-onclick

            TextView text = (TextView) textView;
            Spanned s = (Spanned) text.getText();
            int start = s.getSpanStart(this);
            int end = s.getSpanEnd(this);

            //Toast.makeText(GlobalGrammarModuleFragmentView.getContext(), "Clicked", Toast.LENGTH_SHORT).show();

            // Instead of implementing the direct text change in the InputQueryFragment (this can cause bugs in the long run), it is sent through an interface
            //This is the code that's avoided
            //AutoCompleteTextView queryInit = (AutoCompleteTextView)InputQueryFragment.GlobalInputQueryFragment.findViewById(R.id.inputQueryAutoCompleteTextView);
            //queryInit.setText(text.getText().subSequence(start, end));

            //The following code "initializes" the interface, since it is not necessarily called (initialized) when the grammar fragment receives the inputQueryAutoCompleteTextView and is activated
            try {
                mCallbackVerb = (UserWantsToConjugateFoundVerbListener) getActivity();
            } catch (ClassCastException e) {
                throw new ClassCastException(getActivity().toString() + " must implement TextClicked");
            }

            //Calling the interface
            String outputText = text.getText().subSequence(start, end).toString();
            WordSelectedAction(outputText);
            VerbSelectedAction(outputText);

        }

        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setColor(getResources().getColor(R.color.textColorDictionarySpanClicked));
            ds.setUnderlineText(false);
        }
    }
    static public   String getValidCharacter(String input) {
        String output = input;
        for (int i = 0; i< MainActivity.SimilarsDatabase.size(); i++) {
            if (MainActivity.SimilarsDatabase.get(i)[0].equals(input)) {
                output = MainActivity.SimilarsDatabase.get(i)[1];
                break;
            }
        }
        return output;
    }

    // Interface Functions
    UserWantsNewSearchForSelectedWordListener mCallbackWord;
    interface UserWantsNewSearchForSelectedWordListener {
        // Interface used to transfer the selected word to InputQueryFragment through MainActivity
        void UserWantsNewSearchForSelectedWordFromGrammarModule(String selectedWordString);
    }
    public void WordSelectedAction(String selectedWordString) {

        // Send selectedWordString to MainActivity through the interface
        mCallbackWord.UserWantsNewSearchForSelectedWordFromGrammarModule(selectedWordString);
    }

    UserWantsToConjugateFoundVerbListener mCallbackVerb;
    interface UserWantsToConjugateFoundVerbListener {
        // Interface used to transfer the selected verb to ConjugatorFragment through MainActivity
        void UserWantsToConjugateFoundVerbFromGrammarModule(String[] selectedVerbString);
    }
    public void VerbSelectedAction(String selectedVerbString) {

        String[] output = {"verb",selectedVerbString,"fast"};
        mCallbackVerb.UserWantsToConjugateFoundVerbFromGrammarModule(output);
    }

    private Boolean checkIfUserRequestedDictSearch() {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        Boolean state = sharedPref.getBoolean(getString(R.string.requestingDictSearch), true);
        return state;
    }

	// Grammar Module Functions
    public List<List<Integer>>          FindMatchingWordIndex(String word) {

        // Initializations
        List<List<Integer>> matchingWordRowColIndexList = new ArrayList<>();
        List<Integer> matchingWordRowIndexList = new ArrayList<>();
        List<Integer> matchingWordColIndexList = new ArrayList<>();
        List<Integer> MatchesIndexesFromSortedList = new ArrayList<>();
        List<String> parsed_list;
        List<int[]> MatchList = new ArrayList<>();
        String list;
        String hit;
        String hitFirstRelevantWord;
        String concatenated_hit;
        String best_match;
        String MM_index;
        String type;
        int[] current_match_values;

        int MatchesIndexesFromSortedListSize;
        boolean found_match;
        boolean skip_this_row;
        boolean is_verb = false;
        boolean is_verb_and_latin = false;
        int word_length = word.length();
        int match_length;

        int lastIndex;
        if (MainActivity.MainDatabase != null) {lastIndex = MainActivity.MainDatabase.size() - 1;}
        else { return matchingWordRowColIndexList; }

        // converting the word to lowercase (the algorithm is not efficient if needing to search both lower and upper case)
        word = word.toLowerCase(Locale.ENGLISH);

        // If there is an "inging" verb instance, reduce it to an "ing" instance (e.g. singing >> sing)
        int verb_length = word.length();
        String verb = word;
        String verb2;
        String inglessVerb = verb;
        if (verb_length > 2 && verb.substring(verb_length-3).equals("ing")) {

            if (verb_length > 5 && verb.substring(verb_length-6).equals("inging")) {
                if (	(verb.substring(0, 2+1).equals("to ") && IsOfTypeIngIng(verb.substring(3,verb_length))) ||
                        (!verb.substring(0, 2+1).equals("to ") && IsOfTypeIngIng(verb.substring(0,verb_length)))   ) {
                    // If the verb ends with "inging" then remove the the second "ing"
                    inglessVerb = verb.substring(0,verb_length-3);
                }
            }
            else {
                verb2 = verb + "ing";
                if ((!verb2.substring(0, 2 + 1).equals("to ") || !IsOfTypeIngIng(verb2.substring(3, verb_length + 3))) &&
                        (verb2.substring(0, 2+1).equals("to ") || !IsOfTypeIngIng(verb2.substring(0, verb_length + 3)))) {
                    // If the verb does not belong to the list, then remove the ending "ing" so that it can be compared later on to the verbs excel
                    //If the verb is for e.g. to sing / sing, where verb2 = to singing / singing, then check that verb2 (without the "to ") belongs to the list, and if it does then do nothing

                    inglessVerb = verb.substring(0,verb_length-3);
                }
            }
        }
        int inglessVerb_length = inglessVerb.length();

        // getting the input type and its converted form (english/romaji/kanji/invalid)
        List<String> translationList = ConvertFragment.Kana_to_Romaji_to_Kana(word);

        String translationLatin = translationList.get(0);
        String translationHira = translationList.get(1);
        String translationKata = translationList.get(2);
        String text_type = ConvertFragment.TextType(word);

        boolean TypeisLatin   = false;
        boolean TypeisKana    = false;
        boolean TypeisKanji   = false;
        boolean TypeisNumber  = false;
        boolean TypeisInvalid = false;

        if (text_type.equals("latin") )                                     { TypeisLatin = true;}
        if (text_type.equals("hiragana") || text_type.equals("katakana") )  { TypeisKana = true;}
        if (text_type.equals("kanji") )                                     { TypeisKanji = true;}
        if (text_type.equals("number") )                                    { TypeisNumber = true;}
        if (word.contains("*") || word.contains("＊") || word.equals("") || word.equals("-") ) { TypeisInvalid = true;}

        // Performing the search
        if (!TypeisInvalid) {

            // Concatenating the input word to increase the match chances
            String concatenated_word = SpecialConcatenator(word);
            String concatenated_translationLatin = SpecialConcatenator(translationLatin);
            String concatenated_translationHira = SpecialConcatenator(translationHira);
            String concatenated_translationKata = SpecialConcatenator(translationKata);
            int concatenated_word_length = concatenated_word.length();

            // Removing any apostrophes to make user searches less strict
            word = ApostropheRemover(word);
            concatenated_word = ApostropheRemover(concatenated_word);
            concatenated_translationLatin = ApostropheRemover(concatenated_translationLatin);

            // Search for the matches in the indexed list using a custom limit-finding binary search
            List<String[]> SortedIndex = new ArrayList<>();
            int[] limits = {0, 1};
            if (TypeisLatin || TypeisKana || TypeisNumber) {
                SortedIndex = MainActivity.GrammarDatabaseIndexedLatin;

                //If the input is a verb in "to " form, remove the "to " for the search only (results will be filtered later on)
                String input_word = concatenated_word;
                if (word.length()>3) {
                    if (word.substring(0, 3).equals("to ")) {
                        input_word = concatenated_word.substring(2, concatenated_word.length());
                    }
                }

                limits = BinarySearchInLatinIndex(TypeisLatin, input_word, concatenated_translationLatin, SortedIndex);

                // If the entered word is Latin and only has one character, limit the word list to be checked later
                if (concatenated_word.length() == 1 && limits[0] != -1) {
                    for (int i = limits[0]; i <= limits[1]; i++) {
                        if (SortedIndex.get(i)[0].length()>1) {
                            limits[1] = i-1;
                            break;
                        }
                    }
                }

            } else if (TypeisKanji) {
                SortedIndex = MainActivity.GrammarDatabaseIndexedKanji;
                int relevant_column_index = 2;
                limits = BinarySearchInUTF8Index(concatenated_word, SortedIndex, relevant_column_index);
            }

            // Get the indexes of all of the results that were found using the binary search
            if (limits[0] != -1) {
                for (int i = limits[0]; i <= limits[1]; i++) {
                    parsed_list = Arrays.asList(SortedIndex.get(i)[1].split(";"));
                    for (int j = 0; j < parsed_list.size(); j++) {
                        MatchesIndexesFromSortedList.add(Integer.valueOf(parsed_list.get(j))-1);
                    }
                }
            }

            // Add search results where the "ing" is removed from an "ing" verb
            if ((TypeisLatin || TypeisKana || TypeisNumber) && !inglessVerb.equals(word)) {
                SortedIndex = MainActivity.GrammarDatabaseIndexedLatin;
                limits = BinarySearchInLatinIndex(TypeisLatin, inglessVerb, inglessVerb, SortedIndex);
                if (limits[0] != -1) {
                    for (int i = limits[0]; i <= limits[1]; i++) {
                        parsed_list = Arrays.asList(SortedIndex.get(i)[1].split(";"));
                        for (int j = 0; j < parsed_list.size(); j++) {
                            MatchesIndexesFromSortedList.add(Integer.valueOf(parsed_list.get(j))-1);
                        }
                    }
                }
            }

            MatchesIndexesFromSortedListSize = MatchesIndexesFromSortedList.size();

            // Perform the match finding in the database
            for (int rowIndex = 2; rowIndex < lastIndex; rowIndex++) {

                // Loop initializations
                list = MainActivity.MainDatabase.get(rowIndex)[GlobalConstants.GrammarModule_colIndex_Keyword];
                parsed_list = Arrays.asList(list.split(","));
                found_match = false;
                skip_this_row = true;

                // Choosing which rows to skip. For all the entries in the database, look through the binary search hits.
                if (TypeisLatin || TypeisKana || TypeisKanji || TypeisNumber) {
                    for (int i = 0; i < MatchesIndexesFromSortedListSize; i++) {
                        if (rowIndex == MatchesIndexesFromSortedList.get(i)) {
                            skip_this_row = false;
                            break;
                        }
                    }
                }
                if (list.equals("") || list.equals("-") || list.equals("KEYWORDS")) {
                    skip_this_row = true;
                }

                if (skip_this_row) { continue; }

                // If there is a word in the list that matches the input word, get the corresponding row index
                match_length = 1000;
                best_match = "";
                MM_index = MainActivity.MainDatabase.get(rowIndex)[GlobalConstants.GrammarModule_colIndex_Meaning];
                limits = BinarySearchInLatinIndex(true, MM_index, MM_index, MainActivity.MeaningsDatabase);
                type = "z";
                if (limits[0] != -1) {
                    type = MainActivity.MeaningsDatabase.get(limits[0])[2];
                }
                is_verb = type.substring(0, 1).equals("V") && !type.equals("VC");

                for (int i = 0; i < parsed_list.size(); i++) {

                    // Performing certain actions on the hit to prepare the comparison
                    hit = parsed_list.get(i).trim(); //also trims the extra space before the word

                    if (is_verb) { hit = "to " + hit; } //Add "to " to the hit if it's a verb (the "to " was removed to save memory in the database)
                    is_verb_and_latin = hit.length() > 3 && hit.substring(0, 3).equals("to ");

                    concatenated_hit = SpecialConcatenator(hit);
                    if (TypeisKanji && !ConvertFragment.TextType(concatenated_hit).equals("kanji") ) { continue; }
                    if (concatenated_hit.length() < concatenated_word_length) { continue; }
                    if (TypeisLatin && word_length == 2 && hit.length() > 2) { continue;}
                    if (TypeisLatin && hit.length() < inglessVerb_length) {continue;}

                    if (TypeisLatin) {
                        hit = hit.toLowerCase(Locale.ENGLISH);
                        concatenated_hit = concatenated_hit.toLowerCase(Locale.ENGLISH);
                    }

                    hit = ApostropheRemover(hit);
                    concatenated_hit = ApostropheRemover(concatenated_hit);

                    // Getting the first word if the hit is a sentence
                    if (hit.length() > word_length) {
                        List<String> parsed_hit = Arrays.asList(hit.split(" "));
                        if (is_verb_and_latin) { hitFirstRelevantWord = parsed_hit.get(1);}
                        else { hitFirstRelevantWord = parsed_hit.get(0); } // hitFirstWord is the first word of the hit, and shows relevance to the search priority
                    } else {
                        hitFirstRelevantWord = "";
                    }

                    // Perform the comparison to the input inputQueryAutoCompleteTextView and return the length of the shortest hit
                    // Match length is reduced every time there's a hit and the hit is shorter
                    if (       (concatenated_hit.contains(concatenated_word)
                            || (TypeisLatin && hit.equals("to " + inglessVerb))
                            || (!translationLatin.equals("") && concatenated_hit.contains(translationLatin))
                            || (!translationHira.equals("") && concatenated_hit.contains(translationHira))
                            || (!translationKata.equals("") && concatenated_hit.contains(translationKata)))) //ie. if the hit contains the input word, then do the following:
                    {
                        if (concatenated_hit.equals(concatenated_word)) {
                            best_match = concatenated_hit;
                            found_match = true;
                            match_length = best_match.length()-1; // -1 to make sure that it's listed first
                            if (is_verb_and_latin) { match_length = match_length-3;}
                            continue;
                        }
                        if (hitFirstRelevantWord.contains(concatenated_word) && hitFirstRelevantWord.length() <= match_length) {
                            best_match = hitFirstRelevantWord;
                            found_match = true;
                            match_length = best_match.length();
                            continue;
                        }
                        if (ConvertFragment.TextType(concatenated_hit).equals("latin") && hit.length() <= match_length) {
                            best_match = hit;
                            found_match = true;
                            match_length = best_match.length();
                            if (is_verb_and_latin) { match_length = match_length-3;}
                        }
                    }
                    if (found_match) {break;}
                }

                if (found_match) {
                    current_match_values = new int[2];
                    current_match_values[0] = rowIndex;
                    current_match_values[1] = match_length;
                    MatchList.add(current_match_values);
                }

            }

            // Sorting the results according to the shortest keyword as found in the above search

            // Computing the value length
            int current_row_index;
            int current_col_index;
            int current_romaji_length;
            int current_kanji_length;
            int list_size = MatchList.size();
            int[][] matches = new int[list_size][2];
            for (int i=0;i<list_size;i++) {
                matches[i][0] = MatchList.get(i)[0];
                matches[i][1] = MatchList.get(i)[1];
            }

            // Sorting
            int tempVar0;
            int tempVar1;
            for (int i=0;i<list_size;i++) { //Bubble sort
                for (int t=1;t<list_size-i;t++) {
                    if (matches[t-1][1] > matches[t][1]) {
                        tempVar0 = matches[t-1][0];
                        tempVar1 = matches[t-1][1];
                        matches[t-1][0] = matches[t][0];
                        matches[t-1][1] = matches[t][1];
                        matches[t][0] = tempVar0;
                        matches[t][1] = tempVar1;
                    }
                }
            }

            // Creating an Arraylist with the sorted values
            List<Integer> MatchList_Sorted = new ArrayList<>();
            for (int i=0;i<list_size;i++) {
                matchingWordRowIndexList.add(matches[i][0]);
                matchingWordColIndexList.add(GlobalConstants.GrammarModule_colIndex_Romaji_construction);
            }

        }

        // Return the list of matching row indexes
        matchingWordRowColIndexList.add(matchingWordRowIndexList);
        matchingWordRowColIndexList.add(matchingWordColIndexList);

        return matchingWordRowColIndexList;
    }
    static public int[]                 BinarySearchInLatinIndex(boolean TypeisLatin, String concatenated_word, String concatenated_translationLatin, List<String[]> SortedIndex) {

        String prepared_word = "";
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
        int[] result = {lower_limit,upper_limit};

        return result;
        }
    static public int[]                 BinarySearchInUTF8Index(String concatenated_word, List<String[]> SortedIndex, int relevant_column_index) {

        // Prepare the input word to be used in the following algorithm: the word is converted to its hex utf-8 value as a string, in fractional form
        String prepared_word = convertToUTF8(concatenated_word);

        // Find the upper an lower limits of the range of words that start with the current word, using a binary search pattern
        int back_index = (prepared_word.length()-2)/2; // because each hex = 2 characters, 2+ because of the "1."

        //Initialization
        int char_index;
        int list_size = SortedIndex.size();
        int mid_index;
        String last_char_prepared_word = "";
        String current_char_prepared_word = "";
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
    static public String                convertToUTF8(String input_string) {

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
    public List<Object>                 getCharacteristicsForAllHits(List<Integer> matchingWordRowIndexList) {

        int matchingWordRowIndex;

        List<Object> setOf_matchingWordCharacteristics = new ArrayList<>();
        List<Object> matchingWordCharacteristics;

        if (matchingWordRowIndexList.size() != 0) {
            for (int i=0; i<matchingWordRowIndexList.size(); i++) {
                matchingWordRowIndex = matchingWordRowIndexList.get(i);
                matchingWordCharacteristics = getCharacteristicsForEachHit(matchingWordRowIndex);
                setOf_matchingWordCharacteristics.add(matchingWordCharacteristics);
            }
        }
        return setOf_matchingWordCharacteristics;
    }
    public List<Object>                 getCharacteristicsForEachHit(int matchingWordRowIndex) {

        // Value Initializations
        int example_index;
        List<String> parsed_example_list;
        List<Object> matchingWordCharacteristics = new ArrayList<>();
        List<Object> matchingWordMeaningBlocks = new ArrayList<>();

        //Getting the Romaji value
        String matchingWordRomaji = MainActivity.MainDatabase.get(matchingWordRowIndex)[1];
        matchingWordCharacteristics.add(matchingWordRomaji);

        //Getting the Kanji value
        String matchingWordKanji = MainActivity.MainDatabase.get(matchingWordRowIndex)[2];
        matchingWordCharacteristics.add(matchingWordKanji);

        //Getting the AltSpellings value
        String matchingWordAltSpellings = MainActivity.MainDatabase.get(matchingWordRowIndex)[4];
        matchingWordCharacteristics.add(matchingWordAltSpellings);

        //Getting the set of Meanings

        //Initializations
        String matchingWordMeaning;
        String matchingWordType;
        String matchingWordOpposite;
        String matchingWordSynonym;
        List<String> matchingWordCurrentExplanationBlock;
        List<List<String>> matchingWordExplanationBlocks;
        String matchingWordExplanation;
        String matchingWordRules;
        String matchingWordExampleList;
        String[] current_meaning_characteristics;
        Boolean has_multiple_explanations;
        String ME_index;

        //Finding the meanings using the supplied index
        String MM_index = MainActivity.MainDatabase.get(matchingWordRowIndex)[3];
        List<String> MM_index_list = Arrays.asList(MM_index.split(";"));
        if (MM_index_list.size() == 0) { return matchingWordCharacteristics; }

        List<Object> matchingWordCurrentMeaningBlock;
        int current_MM_index;
        for (int i=0; i< MM_index_list.size(); i++) {
            matchingWordCurrentMeaningBlock = new ArrayList<>();
            current_MM_index = Integer.parseInt(MM_index_list.get(i))-1;
            current_meaning_characteristics = MainActivity.MeaningsDatabase.get(current_MM_index);

            //Getting the Meaning value
            matchingWordMeaning = MainActivity.MeaningsDatabase.get(current_MM_index)[1];

            //Getting the Type value
            matchingWordType = MainActivity.MeaningsDatabase.get(current_MM_index)[2];

            //Make corrections to the meaning values if the hit is a verb
            if (matchingWordType.contains("V") && !matchingWordType.equals("VC")) {
                List<String> parsed_meaning = Arrays.asList(matchingWordMeaning.split(","));
                String fixed_meaning = "";
                for (int k = 0; k < parsed_meaning.size(); k++) {
                    fixed_meaning += "to " + parsed_meaning.get(k).trim();
                    if (k < parsed_meaning.size() - 1) {
                        fixed_meaning += ", ";
                    }
                }
                matchingWordMeaning = fixed_meaning;
            }

            //Setting the Meaning and Type values in the returned list
            matchingWordCurrentMeaningBlock.add(matchingWordMeaning);
            matchingWordCurrentMeaningBlock.add(matchingWordType);

            //Getting the Opposite value
            matchingWordOpposite = MainActivity.MeaningsDatabase.get(current_MM_index)[6];
            matchingWordCurrentMeaningBlock.add(matchingWordOpposite);

            //Getting the Synonym value
            matchingWordSynonym = MainActivity.MeaningsDatabase.get(current_MM_index)[7];
            matchingWordCurrentMeaningBlock.add(matchingWordSynonym);

            //Getting the set of Explanations
            has_multiple_explanations = false;
            ME_index = "";
            if (current_meaning_characteristics[3].length() > 3) {
                if (current_meaning_characteristics[3].substring(0,3).equals("ME#")) {
                    has_multiple_explanations = true;
                    ME_index = current_meaning_characteristics[3].substring(3,current_meaning_characteristics[3].length());
                }
            }
            matchingWordExplanationBlocks = new ArrayList<>();
            if (has_multiple_explanations) {
                List<String> ME_index_list = Arrays.asList(ME_index.split(";"));
                int current_ME_index;
                for (int j=0; j<ME_index_list.size(); j++) {

                    matchingWordCurrentExplanationBlock = new ArrayList<>();
                    current_ME_index = Integer.parseInt(ME_index_list.get(j))-1;

                    //Getting the Explanation value
                    matchingWordExplanation = MainActivity.MultExplanationsDatabase.get(current_ME_index)[1];
                    matchingWordCurrentExplanationBlock.add(matchingWordExplanation);

                    //Getting the Rules value
                    matchingWordRules = MainActivity.MultExplanationsDatabase.get(current_ME_index)[2];
                    matchingWordCurrentExplanationBlock.add(matchingWordRules);

                    //Getting the Examples
                    matchingWordExampleList = MainActivity.MultExplanationsDatabase.get(current_ME_index)[3];
                    if (!matchingWordExampleList.equals("") && !matchingWordExampleList.contains("Example")) {
                        parsed_example_list = Arrays.asList(matchingWordExampleList.split(", "));
                        for (int t = 0; t < parsed_example_list.size(); t++) {
                            example_index = Integer.parseInt(parsed_example_list.get(t)) - 1;
                            matchingWordCurrentExplanationBlock.add(MainActivity.ExamplesDatabase.get(example_index)[GlobalConstants.Examples_colIndex_Example_English]);
                            matchingWordCurrentExplanationBlock.add(MainActivity.ExamplesDatabase.get(example_index)[GlobalConstants.Examples_colIndex_Example_Romaji]);
                            matchingWordCurrentExplanationBlock.add(MainActivity.ExamplesDatabase.get(example_index)[GlobalConstants.Examples_colIndex_Example_Kanji]);
                        }
                    }
                    matchingWordExplanationBlocks.add(matchingWordCurrentExplanationBlock);
                }
            }
            else {

                matchingWordCurrentExplanationBlock = new ArrayList<>();

                //Getting the Explanation value
                matchingWordExplanation = MainActivity.MeaningsDatabase.get(current_MM_index)[3];
                matchingWordCurrentExplanationBlock.add(matchingWordExplanation);

                //Getting the Rules value
                matchingWordRules = MainActivity.MeaningsDatabase.get(current_MM_index)[4];
                matchingWordCurrentExplanationBlock.add(matchingWordRules);

                //Getting the Examples
                matchingWordExampleList = MainActivity.MeaningsDatabase.get(current_MM_index)[5];
                if (!matchingWordExampleList.equals("") && !matchingWordExampleList.contains("Example")) {
                    parsed_example_list = Arrays.asList(matchingWordExampleList.split(", "));
                    for (int t = 0; t < parsed_example_list.size(); t++) {
                        example_index = Integer.parseInt(parsed_example_list.get(t)) - 1;
                        matchingWordCurrentExplanationBlock.add(MainActivity.ExamplesDatabase.get(example_index)[GlobalConstants.Examples_colIndex_Example_English]);
                        matchingWordCurrentExplanationBlock.add(MainActivity.ExamplesDatabase.get(example_index)[GlobalConstants.Examples_colIndex_Example_Romaji]);
                        matchingWordCurrentExplanationBlock.add(MainActivity.ExamplesDatabase.get(example_index)[GlobalConstants.Examples_colIndex_Example_Kanji]);
                    }
                }
                matchingWordExplanationBlocks.add(matchingWordCurrentExplanationBlock);

            }
            matchingWordCurrentMeaningBlock.add(matchingWordExplanationBlocks);

            matchingWordMeaningBlocks.add(matchingWordCurrentMeaningBlock);

        }

        matchingWordCharacteristics.add(matchingWordMeaningBlocks);

        return matchingWordCharacteristics;
    }
    public static String                SpecialConcatenator(String sentence) {
        String current_char;
        String concatenated_sentence = "";
        for (int index=0; index<sentence.length(); index++) {
            current_char = Character.toString(sentence.charAt(index));
            if (!( current_char.equals(" ")
                    || current_char.equals(".")
                    || current_char.equals("-")
                    || current_char.equals("(")
                    || current_char.equals(")")
                    || current_char.equals(":")
                    || current_char.equals("/") ) ) {
                concatenated_sentence = concatenated_sentence + current_char;
            }
        }
        return concatenated_sentence;
    }
    public static String                ApostropheRemover(String sentence) {
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
    public Boolean                      IsOfTypeIngIng(String verb) {
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
    public String                       removeDuplicatesFromCommaList(String input_list) {

        Boolean is_repeated;
        List<String> parsed_cumulative_meaning_value = Arrays.asList(input_list.split(","));
        StringBuilder final_cumulative_meaning_value = new StringBuilder("");
        List<String> final_cumulative_meaning_value_array = new ArrayList<>();
        String current_value;
        for (int j = 0; j <parsed_cumulative_meaning_value.size(); j++) {
            is_repeated = false;
            current_value = parsed_cumulative_meaning_value.get(j).trim();
            for (String s : final_cumulative_meaning_value_array) {
                if (s.equals(current_value)) { is_repeated = true; break; }
            }
            if (!is_repeated)  final_cumulative_meaning_value_array.add(current_value);
        }
        for (int j = 0; j <final_cumulative_meaning_value_array.size(); j++) {
            final_cumulative_meaning_value.append(final_cumulative_meaning_value_array.get(j).trim());
            if (j <final_cumulative_meaning_value_array.size()-1) final_cumulative_meaning_value.append(", ");
        }
        return final_cumulative_meaning_value.toString();
    }

    // ExpandableListView Functions
    private class GrammarExpandableListAdapter extends BaseExpandableListAdapter {

        private Context _context;
        private List<String> _listDataHeader;
        private HashMap<String, List<String>> _listHeaderDetails;
        private HashMap<String, List<List<String>>> _listDataChild;

        private GrammarExpandableListAdapter(Context context, List<String> listDataHeader, HashMap<String, List<String>> listHeaderDetails, HashMap<String, List<List<String>>> listDataChild) {
            this._context = context;
            this._listDataHeader = listDataHeader;
            this._listHeaderDetails = listHeaderDetails;
            this._listDataChild = listDataChild;
        }
        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return this._listDataChild.get(this._listDataHeader.get(groupPosition)).get(childPosition);
        }
        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }
        @Override
        public View getChildView(int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

            //hideSoftKeyboard();
            final List<String> childArray = (List<String>) getChild(groupPosition, childPosition);
            final List<List<String>> childrenArray = this._listDataChild.get(this._listDataHeader.get(groupPosition));
            List<String> headerDetailsArray = this._listHeaderDetails.get(this._listDataHeader.get(groupPosition));

            if (convertView == null) {
                LayoutInflater infalInflater = (LayoutInflater) this._context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = infalInflater.inflate(R.layout.custom_grammar_list_child_item, null);
            }

            //Initialization
            int start_index;
            int end_index = 0;
            int number_of_hits;
            String type = "";

            LinearLayout elements_container = convertView.findViewById(R.id.elements_container);
            elements_container.removeAllViews();
            LinearLayout ChosenItem = convertView.findViewById(R.id.child_item_chosen_item_linearlayout);
            TextView ChosenItem_RomajiX = convertView.findViewById(R.id.child_item_chosen_item_romaji);
            TextView ChosenItem_KanjiX = convertView.findViewById(R.id.child_item_chosen_item_kanji);

            //Setting the alternate spellings
            if (childPosition == 0) {
                String alternatespellings = childArray.get(0);
                if (!alternatespellings.equals("")) {
                    String htmlText = "<font face='serif' color='" +
                            getResources().getColor(R.color.textColorDictionaryAlternateSpellings) +
                            "'>" + "<b>" + "Alternate spellings: " + "</b>" + alternatespellings + "</font>";
                    Spanned spanned_alternatespellings = SharedMethods.fromHtml(htmlText);
                    TextView tv_alternatespellings = new TextView(getContext());
                    tv_alternatespellings.setText(spanned_alternatespellings);
                    tv_alternatespellings.setTextSize(14);
                    tv_alternatespellings.setTextIsSelectable(true);
                    tv_alternatespellings.setClickable(true);
                    elements_container.addView(tv_alternatespellings);
                }
            }

            //Setting the type and meaning
            type = childArray.get(1); //make sure to adjust the type condition for the user click part
            if (childPosition > 0) {
                String full_type = "";
                for (int i=0; i<MainActivity.LegendDatabase.size(); i++) {
                    if (MainActivity.LegendDatabase.get(i)[0].equals(type)) { full_type = MainActivity.LegendDatabase.get(i)[1]; break; }
                }
                if (full_type.equals("")) { full_type = type; }
                //String type_and_meaning = "[" + full_type + "] " + childArray.get(2);
                String htmlText = "<i><font color='"+
                        getResources().getColor(R.color.textColorDictionaryTypeMeaning) +
                        "'>" + "[" + full_type + "] " + "</font></i>" + "<b>" + childArray.get(2) + "</b>";
                Spanned type_and_meaning = SharedMethods.fromHtml(htmlText);
                TextView tv_type_and_meaning = new TextView(getContext());
                tv_type_and_meaning.setText(type_and_meaning);
                tv_type_and_meaning.setTextColor(getResources().getColor(R.color.textColorDictionaryTypeMeaning2));
                tv_type_and_meaning.setTextSize(15);
                //tv_type_and_meaning.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                elements_container.addView(tv_type_and_meaning);
            }

            //Showing the romaji and kanji values for user click
            if (childPosition == 0) {
                String chosenitem_romaji;
                String chosenitem_kanji;
                String pre_text;
                String after_text;

                ChosenItem.setVisibility(View.VISIBLE);

                if (type.contains("V") && !type.equals("VC")) {
                    pre_text = "Conjugate ";
                    after_text = " ";
                    chosenitem_romaji = pre_text + headerDetailsArray.get(0) + after_text;
                    SpannableString VerbSpannable = new SpannableString(chosenitem_romaji);
                    VerbSpannable.setSpan(new VerbClickableSpan(), pre_text.length(), chosenitem_romaji.length() - after_text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ChosenItem_RomajiX.setText(VerbSpannable);
                    ChosenItem_RomajiX.setTypeface(Typeface.SERIF);
                    ChosenItem_RomajiX.setTypeface(null, Typeface.BOLD_ITALIC);
                    ChosenItem_RomajiX.setTextSize(16);
                    ChosenItem_RomajiX.setMovementMethod(LinkMovementMethod.getInstance());

                    pre_text = "(";
                    after_text = ").";
                    chosenitem_kanji = pre_text + headerDetailsArray.get(1) + after_text;
                    VerbSpannable = new SpannableString(chosenitem_kanji);
                    VerbSpannable.setSpan(new VerbClickableSpan(), pre_text.length(), chosenitem_kanji.length() - after_text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ChosenItem_KanjiX.setText(VerbSpannable);
                    ChosenItem_KanjiX.setTypeface(Typeface.SERIF);
                    ChosenItem_KanjiX.setTypeface(null, Typeface.BOLD_ITALIC);
                    ChosenItem_KanjiX.setTextSize(16);
                    ChosenItem_KanjiX.setMovementMethod(LinkMovementMethod.getInstance());
                } else {

                    pre_text = "Copy ";
                    after_text = " ";
                    chosenitem_romaji = pre_text + headerDetailsArray.get(0) + after_text;
                    SpannableString WordSpannable = new SpannableString(chosenitem_romaji);
                    WordSpannable.setSpan(new WordClickableSpan(), pre_text.length(), chosenitem_romaji.length() - after_text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ChosenItem_RomajiX.setText(WordSpannable);
                    ChosenItem_RomajiX.setTypeface(Typeface.SERIF);
                    ChosenItem_RomajiX.setTypeface(null, Typeface.BOLD_ITALIC);
                    ChosenItem_RomajiX.setTextSize(16);
                    ChosenItem_RomajiX.setMovementMethod(LinkMovementMethod.getInstance());

                    pre_text = "(";
                    after_text = ") to input.";
                    chosenitem_kanji = pre_text + headerDetailsArray.get(1) + after_text;
                    WordSpannable = new SpannableString(chosenitem_kanji);
                    WordSpannable.setSpan(new WordClickableSpan(), pre_text.length(), chosenitem_kanji.length() - after_text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ChosenItem_KanjiX.setText(WordSpannable);
                    ChosenItem_KanjiX.setTypeface(Typeface.SERIF);
                    ChosenItem_KanjiX.setTypeface(null, Typeface.BOLD_ITALIC);
                    ChosenItem_KanjiX.setTextSize(16);
                    ChosenItem_KanjiX.setMovementMethod(LinkMovementMethod.getInstance());
                }
            }
            else {
                ChosenItem.setVisibility(View.GONE);
            }

            //Setting the antonym
            if (childPosition > 0) {
                String antonym = childArray.get(3);
                if (!antonym.equals("")) {
                    String OppositeXtext = "Antonyms: " + antonym;
                    SpannableString OppositeXSpannable = new SpannableString(OppositeXtext);

                    List<String> parsed_opposite_list = Arrays.asList(antonym.split(","));
                    number_of_hits = parsed_opposite_list.size();
                    for (int i = 0; i < number_of_hits; i++) {
                        if (i == 0) {
                            start_index = 10; // Start after "Antonyms: "
                            end_index = start_index + parsed_opposite_list.get(i).length();
                        } else {
                            start_index = end_index + 2;
                            end_index = start_index + parsed_opposite_list.get(i).length() - 1;
                        }
                        OppositeXSpannable.setSpan(new WordClickableSpan(), start_index, end_index, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    TextView tv_antonym = new TextView(getContext());
                    tv_antonym.setText(OppositeXSpannable);
                    tv_antonym.setMovementMethod(LinkMovementMethod.getInstance());
                    tv_antonym.setTextColor(getResources().getColor(R.color.textColorDictionaryAntonymSynonym));
                    tv_antonym.setTextSize(14);
                    tv_antonym.setTextIsSelectable(true);
                    elements_container.addView(tv_antonym);
                }
            }

            //Setting the synonym
            if (childPosition > 0) {
                String synonym = childArray.get(4);
                if (!synonym.equals("")) {
                    String SynonymXtext = "Synonyms: " + synonym;
                    SpannableString SynonymXSpannable = new SpannableString(SynonymXtext);

                    List<String> parsed_synonym_list = Arrays.asList(synonym.split(","));
                    number_of_hits = parsed_synonym_list.size();
                    for (int i = 0; i < number_of_hits; i++) {
                        if (i == 0) {
                            start_index = 10; // Start after "Synonyms: "
                            end_index = start_index + parsed_synonym_list.get(i).length();
                        } else {
                            start_index = end_index + 2;
                            end_index = start_index + parsed_synonym_list.get(i).length() - 1;
                        }
                        SynonymXSpannable.setSpan(new WordClickableSpan(), start_index, end_index, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    TextView tv_synonym = new TextView(getContext());
                    tv_synonym.setText(SynonymXSpannable);
                    tv_synonym.setMovementMethod(LinkMovementMethod.getInstance());
                    tv_synonym.setTextColor(getResources().getColor(R.color.textColorDictionaryAntonymSynonym));
                    tv_synonym.setTextSize(14);
                    tv_synonym.setTextIsSelectable(true);
                    elements_container.addView(tv_synonym);
                }
            }

            final LinearLayout examples_layout = new LinearLayout(getContext());
            LinearLayout.LayoutParams examples_layout_params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT);
            examples_layout.setOrientation(LinearLayout.VERTICAL);
            examples_layout.setLayoutParams(examples_layout_params);
            examples_layout.setVisibility(View.GONE);
            final TextView tv_examples = new TextView(getContext());
            if (childPosition > 0) {
                int number_of_remaining_elements = childArray.size() - 5;
                String current_element;
                Boolean is_first_example = true;
                for (int i = 0; i < number_of_remaining_elements; i++) {
                    current_element = childArray.get(5 + i);

                    if (current_element.length() > 4) {

                        //Setting the explanation
                        if (current_element.substring(0,4).equals("EXPL")) {
                            TextView tv_explanation = new TextView(getContext());
                            tv_explanation.setText(current_element.substring(4,current_element.length()));
                            tv_explanation.setTextColor(getResources().getColor(R.color.textColorDictionaryExplanation));
                            tv_explanation.setPadding(0,10,0,0);
                            tv_explanation.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
                            elements_container.addView(tv_explanation);
                        }

                        //Setting the rule
                        else if (current_element.substring(0,4).equals("RULE")) {
                            TextView tv_rule = new TextView(getContext());
                            tv_rule.setTextColor(getResources().getColor(R.color.textColorDictionaryRule));
                            tv_rule.setPadding(0,10,0,0);
                            tv_rule.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

                            List<String> parsedRule = Arrays.asList(current_element.substring(4,current_element.length()).split("@"));
                            String where = " where: ";
                            String final_text;
                            String intro = "";
                            if (!parsedRule.get(0).contains(":")) { intro = getResources().getString(R.string.PhraseStructure) + " "; }
                            Spanned spanned_rule;

                            if (parsedRule.size() == 1) { // If the rule doesn't have a "where" clause
                                final_text = intro + current_element.substring(4,current_element.length());
                                tv_rule.setText(final_text);
                                tv_rule.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                            } else {
                                String htmlText = "<b>" + intro + parsedRule.get(0) + "</b>" + "<font face='serif' color='"+
                                        getResources().getColor(R.color.textColorDictionaryRuleWhereClause) +
                                        "purple'>" + where + "</font>" + "<b>" + parsedRule.get(1) + "</b>";
                                spanned_rule = SharedMethods.fromHtml(htmlText);
                                tv_rule.setText(spanned_rule);
                            }

                            elements_container.addView(tv_rule);
                        }

                        //Setting the examples
                        else if (current_element.substring(0,4).equals("EXMP")) {

                            if (is_first_example) {
                                tv_examples.setText(getResources().getString(R.string.ShowExamples));
                                tv_examples.setTextColor(getResources().getColor(R.color.textColorDictionaryExamples));
                                tv_examples.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                                tv_examples.setPadding(0, 10, 0, 0);
                                elements_container.addView(tv_examples);
                                is_first_example = false;
                            }

                            final String example_English = childArray.get(5 + i);
                            if (!example_English.equals("")) {
                                TextView tv_example_English = new TextView(getContext());
                                tv_example_English.setText(example_English.substring(4,example_English.length()));
                                tv_example_English.setTextSize(14);
                                tv_example_English.setPadding(4,10,0,0);
                                tv_example_English.setTextColor(getResources().getColor(R.color.textColorDictionaryExampleEnglish));
                                examples_layout.addView(tv_example_English);
                            }

                            final String example_Romaji = childArray.get(5 + i + 1);
                            if (!example_Romaji.equals("")) {
                                TextView tv_example_Romaji = new TextView(getContext());
                                tv_example_Romaji.setText(example_Romaji.substring(4,example_Romaji.length()));
                                tv_example_Romaji.setTextSize(14);
                                tv_example_Romaji.setPadding(4,0,0,0);
                                tv_example_Romaji.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
                                tv_example_Romaji.setTextColor(getResources().getColor(R.color.textColorDictionaryExampleRomaji));
                                examples_layout.addView(tv_example_Romaji);
                            }

                            String example_Kanji = childArray.get(5 + i + 2);
                            if (!example_Kanji.equals("")) {
                                TextView tv_example_Kanji = new TextView(getContext());
                                tv_example_Kanji.setText(example_Kanji.substring(4,example_Kanji.length()));
                                tv_example_Kanji.setTextSize(14);
                                tv_example_Kanji.setPadding(4,0,0,0);
                                tv_example_Kanji.setTextColor(getResources().getColor(R.color.textColorDictionaryExampleKanji));
                                examples_layout.addView(tv_example_Kanji);
                            }


                            i = i + 2;
                        }
                    }
                }
            }

            elements_container.addView(examples_layout);

            tv_examples.setClickable(true);
            tv_examples.setFocusable(false);
            elements_container.setFocusable(false);
            convertView.setClickable(true);
            tv_examples.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (examples_layout.getVisibility() == View.VISIBLE) {
                        examples_layout.setVisibility(View.GONE);
                        tv_examples.setText("Example sentences (>show)");
                    }
                    else {
                        examples_layout.setVisibility(View.VISIBLE);
                        tv_examples.setText("Example sentences (>hide)");
                    }
                }
            });

            return convertView;
        }
        @Override
        public int getChildrenCount(int groupPosition) {
            final List<String> childArray = (List<String>) getChild(groupPosition, 0);
            if (groupPosition==0 && childArray.get(0).equals("No match found.")) {
                return 0;
            }
            else {
                return this._listDataChild.get(this._listDataHeader.get(groupPosition)).size();
            }
        }
        @Override
        public Object getGroup(int groupPosition) {
            return this._listDataHeader.get(groupPosition);
        }
        @Override
        public int getGroupCount() {
            return this._listDataHeader.size();
        }
        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }
        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

            //hideSoftKeyboard();
            String headerTitle = (String) getGroup(groupPosition);
            List<String> headerDetailsArray = this._listHeaderDetails.get(this._listDataHeader.get(groupPosition));

            if (convertView == null) {
                LayoutInflater infalInflater = (LayoutInflater) this._context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = infalInflater.inflate(R.layout.custom_grammar_list_group_item, null);
            }

            //Updating the romaji and kanji values
            String romaji_value = headerDetailsArray.get(0);
            String kanji_value = headerDetailsArray.get(1);
            TextView ExpandableListViewGroupElement_romaji_and_Kanji = convertView.findViewById(R.id.ExpandableListViewGroupElement_romaji_and_Kanji);
            String output =  romaji_value + " (" + kanji_value + ")";
            if (kanji_value.equals("")) { output =  romaji_value; }
            ExpandableListViewGroupElement_romaji_and_Kanji.setText(output);
            if (romaji_value.equals("")) { ExpandableListViewGroupElement_romaji_and_Kanji.setVisibility(View.GONE); }
            else { ExpandableListViewGroupElement_romaji_and_Kanji.setVisibility(View.VISIBLE); }

            //Updating the cumulative meaning value
            TextView ExpandableListViewGroupElement_meanings = convertView.findViewById(R.id.ExpandableListViewGroupElement_meanings);
            ExpandableListViewGroupElement_meanings.setText(headerDetailsArray.get(2));

            return convertView;
        }
        @Override
        public boolean hasStableIds() {
            return false;
        }
        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }

}