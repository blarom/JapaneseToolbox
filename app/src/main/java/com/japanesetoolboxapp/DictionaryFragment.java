package com.japanesetoolboxapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
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

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.japanesetoolboxapp.data.DatabaseUtilities;
import com.japanesetoolboxapp.data.Word;
import com.japanesetoolboxapp.data.WordsRoomDatabase;
import com.japanesetoolboxapp.utiities.GlobalConstants;
import com.japanesetoolboxapp.utiities.SharedMethods;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class DictionaryFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<Word>>{

    // Parameters
    private static final String FIREBASE_DEBUG = "japaneseToolboxFirebase";
    private final static int MAX_NUMBER_RESULTS_SHOWN = 50;
    private Activity mFragmentActivity;

    private Boolean mAppWasInBackground;
    private List<String> mExpandableListDataHeader;
    private HashMap<String, List<String>> mExpandableListHeaderDetails;
    private HashMap<String, List<List<String>>> mExpandableListDataChild;
    private String mSearchedWord;
    private Boolean mInternetIsAvailable;
    private static final int JISHO_WEB_SEARCH_LOADER = 41;
    private static final int ROOM_DB_SEARCH_LOADER = 42;
    private static final String JISHO_LOADER_INPUT_EXTRA = "input";
    Boolean matchFound;
    Toast mShowOnlineResultsToast;
    SharedMethods mSharedMethods;
    private List<Word> mLocalMatchingWordsList;
    private DatabaseReference mFirebaseDbReference;
    private DatabaseReference mWordReference;
    private Word mWordFromFirebase;
    WordsRoomDatabase mWordsRoomDatabase;
    private Boolean mShowOnlineResults;

    //Fragment Lifecycle methods
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
        mSharedMethods = new SharedMethods();
        if (getContext() != null) mInternetIsAvailable = SharedMethods.internetIsAvailableCheck(getContext());

        FirebaseDatabase firebaseDb = FirebaseDatabase.getInstance();
        mFirebaseDbReference = firebaseDb.getReference();

        mWordsRoomDatabase = WordsRoomDatabase.getInstance(getContext());
        //mWordsInRoomDatabase = mWordsRoomDatabase.getAllWords();
    }
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Retain this fragment (used to save user inputs on activity creation/destruction)
        setRetainInstance(true);
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
            if (getArguments() != null) {
                mSearchedWord = getArguments().getString("input_to_fragment");
                registerThatUserIsRequestingDictSearch(false);

                //If the application is resumed (switched to), then display the last results instead of performing a new search on the last input
                if (mAppWasInBackground == null || !mAppWasInBackground) {
                    mAppWasInBackground = false;

                    // Get the row index of the words matching the user's entry

                    if (getActivity()!=null) {
                        LoaderManager loaderManager = getActivity().getSupportLoaderManager();
                        Loader<String> roomDbSearchLoader = loaderManager.getLoader(ROOM_DB_SEARCH_LOADER);
                        if (roomDbSearchLoader == null) loaderManager.initLoader(ROOM_DB_SEARCH_LOADER, null, this);
                        else loaderManager.restartLoader(ROOM_DB_SEARCH_LOADER, null, this);

                    }
                }
            }
        }
    }
    @Override public void onDetach() {
        super.onDetach();
        this.mFragmentActivity = null;
    }
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        // save excel results to display in spinners, load the results on activity restart
        //savedInstanceState.putStringArrayList("mGlobalGrammarSpinnerList_element0", mGlobalGrammarSpinnerList_element0);
    }

    //Asynchronous methods
    @NonNull @Override public Loader<List<Word>> onCreateLoader(int id, final Bundle args) {

        if (id == JISHO_WEB_SEARCH_LOADER) {
            WebResultsAsyncTaskLoader webResultsAsyncTaskLoader = new WebResultsAsyncTaskLoader(getContext(), mSearchedWord, mAppWasInBackground, mInternetIsAvailable);
            webResultsAsyncTaskLoader.setLoaderState(true);
            return webResultsAsyncTaskLoader;
        }
        else if (id == ROOM_DB_SEARCH_LOADER){
            RoomDbSearchAsyncTaskLoader roomDbSearchLoader = new RoomDbSearchAsyncTaskLoader(getContext(), mSearchedWord);
            return roomDbSearchLoader;
        }
        else return new RoomDbSearchAsyncTaskLoader(getContext(), "");
    }
    @Override public void onLoadFinished(@NonNull Loader<List<Word>> loader, List<Word> loaderResultWordsList) {

        if (loader.getId() == JISHO_WEB_SEARCH_LOADER) {
            if (mAppWasInBackground == null || !mAppWasInBackground) {
                Boolean showOnlineResults = getShowOnlineResultsPreference();
                if (loaderResultWordsList.size() != 0 && showOnlineResults) {
                    matchFound = true;
                    List<Word> totalWords = mergeWordLists(mLocalMatchingWordsList, loaderResultWordsList);
                    totalWords = sortWordsAccordingToRomajiAndKanjiLengths(mSearchedWord, totalWords);

                    updateFirebaseDbWithJishoWords(loaderResultWordsList);
                    setupFullWordsListListenerFromFirebase();

                    displayResults(mSearchedWord, totalWords);
                }

                //If no dictionary match was found, then this is probably a verb conjugation, so try that
                if (!matchFound && !mSearchedWord.equals("") && getActivity() != null) {
                    if (mShowOnlineResultsToast != null) mShowOnlineResultsToast.cancel();
                    //Toast.makeText(getContext(), "No match found, looking up conjugations.", Toast.LENGTH_SHORT).show();
                    getActivity().findViewById(R.id.button_searchVerb).performClick();
                }
            }
        }
        else if (loader.getId() == ROOM_DB_SEARCH_LOADER){
            mLocalMatchingWordsList = loaderResultWordsList;
            displayWordsToUser(mSearchedWord, loaderResultWordsList);
        }
    }
    @Override public void onLoaderReset(@NonNull Loader<List<Word>> loader) {}
    private static class WebResultsAsyncTaskLoader extends AsyncTaskLoader <List<Word>> {

        String speechRecognizerString;
        private boolean appWasInBackground;
        private boolean internetIsAvailable;
        private boolean mAllowLoaderStart;

        WebResultsAsyncTaskLoader(Context context,
                                  String speechRecognizerString,
                                  boolean appWasInBackground,
                                  boolean internetIsAvailable) {
            super(context);
            this.speechRecognizerString = speechRecognizerString;
            this.appWasInBackground = appWasInBackground;
            this.internetIsAvailable = internetIsAvailable;
        }

        @Override
        protected void onStartLoading() {
            if (mAllowLoaderStart) forceLoad();
        }

        @Override
        public List<Word> loadInBackground() {

            List<Word> matchingWordsFromJisho = new ArrayList<>();

            if (!appWasInBackground) {
                if (internetIsAvailable) {
                    matchingWordsFromJisho = SharedMethods.getWordsFromJishoOnWeb(speechRecognizerString, getContext());
                } else {
                    Log.i("Diagnosis Time", "Failed to access online resources.");
                    Looper.prepare();
                    Toast.makeText(getContext(), "Failed to connect to the Internet.", Toast.LENGTH_SHORT).show();
                    cancelLoadInBackground();
                }
            }
            return matchingWordsFromJisho;
        }

        void setLoaderState(boolean state) {
            mAllowLoaderStart = state;
        }
    }
    private static class RoomDbSearchAsyncTaskLoader extends AsyncTaskLoader <List<Word>> {

        String mSearchWord;
        private List<Long> mMatchingWordIds;

        RoomDbSearchAsyncTaskLoader(Context context, String searchWord) {
            super(context);
            mSearchWord = searchWord;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Override
        public List<Word> loadInBackground() {

            List<Word> localMatchingWordsList = new ArrayList<>();
            if (!TextUtils.isEmpty(mSearchWord)) {
                WordsRoomDatabase wordsRoomDatabase = WordsRoomDatabase.getInstance(getContext());
                mMatchingWordIds = DatabaseUtilities.FindMatchingWordIndex(mSearchWord, wordsRoomDatabase);
                localMatchingWordsList = wordsRoomDatabase.getWordListByWordIds(mMatchingWordIds);
            }

            return localMatchingWordsList;
        }
    }

	//Functionality methods
    public void displayWordsToUser(String word, List<Word> localMatchingWordsList) {

        //Displaying the local results
        String searchedWord = SharedMethods.removeSpecialCharacters(word);
        localMatchingWordsList = sortWordsAccordingToRomajiAndKanjiLengths(searchedWord, localMatchingWordsList);
        displayResults(searchedWord, localMatchingWordsList);

        //If allowed, update the results with words from Jisho.org
        mShowOnlineResults = getShowOnlineResultsPreference();
        if (mShowOnlineResults) {
            mShowOnlineResultsToast = Toast.makeText(getContext(), getResources().getString(R.string.showOnlineResultsToastString), Toast.LENGTH_SHORT);

            if (getActivity() != null) {
                if (!searchedWord.equals("")) mShowOnlineResultsToast.show();

                Bundle queryBundle = new Bundle();
                queryBundle.putString(JISHO_LOADER_INPUT_EXTRA, searchedWord);

                //Attempting to access jisho.org to complete the results found in the local dictionary
                LoaderManager loaderManager = getActivity().getSupportLoaderManager();
                Loader<String> JishoWebSearchLoader = loaderManager.getLoader(JISHO_WEB_SEARCH_LOADER);
                if (JishoWebSearchLoader == null) loaderManager.initLoader(JISHO_WEB_SEARCH_LOADER, queryBundle, this);
                else loaderManager.restartLoader(JISHO_WEB_SEARCH_LOADER, queryBundle, this);
            }
        }

        //If there are no local results to display and online results are unwanted, try the reverse verb search
        if (localMatchingWordsList.size() == 0 && !searchedWord.equals("") && !mShowOnlineResults) {
            if (mShowOnlineResultsToast!=null) mShowOnlineResultsToast.cancel();
            getActivity().findViewById(R.id.button_searchVerb).performClick();
        }
    }
    private List<Word> sortWordsAccordingToRomajiAndKanjiLengths(String searchWord, List<Word> wordsList) {

        List<int[]> matchingWordIndexesAndLengths = new ArrayList<>();
        for (int i = 0; i < wordsList.size(); i++) {

            Word currentWord = wordsList.get(i);
            String romaji_value = currentWord.getRomaji();
            String kanji_value = currentWord.getKanji();

            //Get the length of the shortest meaning containing the word, and use it to prioritize the results
            List<Word.Meaning> currentMeanings = currentWord.getMeanings();
            String currentMeaning;
            int currentMeaningLength = 1000;
            for (int j = 0; j< currentMeanings.size(); j++) {
                currentMeaning = currentMeanings.get(j).getMeaning();
                if (currentMeaning.contains(searchWord) && currentMeaning.length() <= currentMeaningLength) {
                    currentMeaningLength = currentMeaning.length();
                }
            }

            //Get the total length
            int length = romaji_value.length() + kanji_value.length() + currentMeaningLength;

            //If the romaji or Kanji value is an exact match to the search word, then it must appear at the start of the list
            if (romaji_value.equals(searchWord) || kanji_value.equals(searchWord)) length = 0;

            int[] currentMatchingWordIndexAndLength = new int[2];
            currentMatchingWordIndexAndLength[0] = i;
            currentMatchingWordIndexAndLength[1] = length;

            matchingWordIndexesAndLengths.add(currentMatchingWordIndexAndLength);
        }

        //Sort the results according to total length
        if (matchingWordIndexesAndLengths.size() != 0) {
            matchingWordIndexesAndLengths = bubbleSortForTwoIntegerList(matchingWordIndexesAndLengths);
        }

        //Return the sorted list
        List<Word> sortedWordsList = new ArrayList<>();
        for (int i = 0; i < matchingWordIndexesAndLengths.size(); i++) {
            int sortedIndex = matchingWordIndexesAndLengths.get(i)[0];
            sortedWordsList.add(wordsList.get(sortedIndex));
        }

        return sortedWordsList;
    }
    public void displayResults(String searchWord, List<Word> wordsList) {

        // Populate the list of choices for the SearchResultsChooserSpinner. Each text element of inside the idividual spinner choices corresponds to a sub-element of the choicelist
        createExpandableListViewContentsFromWordsList(searchWord, wordsList);

        if (getActivity()!=null) SharedMethods.hideSoftKeyboard(getActivity());

        // Implementing the SearchResultsChooserListView
        try {
            if (mExpandableListDataHeader != null && mExpandableListHeaderDetails != null && mExpandableListDataChild != null && getView() != null) {
                GrammarExpandableListAdapter mSearchResultsListAdapter = new GrammarExpandableListAdapter(getContext(),
                        mExpandableListDataHeader, mExpandableListHeaderDetails, mExpandableListDataChild);
                ExpandableListView mSearchResultsExpandableListView = getView().findViewById(R.id.SentenceConstructionExpandableListView);
                mSearchResultsExpandableListView.setAdapter(mSearchResultsListAdapter);
                mSearchResultsExpandableListView.setVisibility(View.VISIBLE);
            }
        }
        catch (java.lang.NullPointerException e) {
            //If a NullPointerException happens, restart activity since the list cannot be diplayed
            Intent intent = new Intent(getContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }
    private List<Word> mergeWordLists(List<Word> localWords, List<Word> asyncWords) {

        List<Word> finalWordsList = new ArrayList<>();
        List<Word> finalAsyncWords = new ArrayList<>(asyncWords);
        Boolean async_meaning_found_locally;

        for (int j = 0; j< localWords.size(); j++) {
            Word currentLocalWord = localWords.get(j);
            Word finalWord = new Word();
            finalWord.setRomaji(currentLocalWord.getRomaji());
            finalWord.setKanji(currentLocalWord.getKanji());
            finalWord.setAltSpellings(currentLocalWord.getAltSpellings());

            List<Word.Meaning> current_local_meanings = currentLocalWord.getMeanings();
            List<Word.Meaning> current_final_meanings = new ArrayList<>(current_local_meanings);

            int current_index = finalAsyncWords.size()-1;
            while (current_index >= 0 && finalAsyncWords.size() != 0) {

                if (current_index > finalAsyncWords.size()-1) {break;}
                Word currentAsyncWord = finalAsyncWords.get(current_index);
                List<Word.Meaning> current_async_meanings = currentAsyncWord.getMeanings();

                if (    currentAsyncWord.getRomaji().equals(currentLocalWord.getRomaji())
                    &&  currentAsyncWord.getKanji() .equals(currentLocalWord.getKanji())   ) {

                    for (int m = 0; m< current_async_meanings.size(); m++) {

                        async_meaning_found_locally = false;
                        for (int k = 0; k< current_local_meanings.size(); k++) {

                            if (current_local_meanings.get(k).getMeaning()
                                    .contains( current_async_meanings.get(m).getMeaning() ) ) {
                                async_meaning_found_locally = true;
                                break;
                            }
                        }
                        if (!async_meaning_found_locally) {
                            current_final_meanings.add(current_async_meanings.get(m));
                        }
                    }
                    finalAsyncWords.remove(current_index);
                    if (current_index == 0) break;
                }
                else {
                    current_index -= 1;
                }
            }
            finalWord.setMeanings(current_final_meanings);
            finalWordsList.add(finalWord);
        }
        finalWordsList.addAll(finalAsyncWords);

        return finalWordsList;
    }
    public void createExpandableListViewContentsFromWordsList(String searchWord, List<Word> wordsList) {

        //Initialization
        mExpandableListDataHeader = new ArrayList<>();
        mExpandableListHeaderDetails = new HashMap<>();
        mExpandableListDataChild = new HashMap<>();
        List<List<String>> childElements;
        List<String> childSubElements = new ArrayList<>();
        List<String> headerElements;

        if (wordsList.size() == 0) {
            //Create a generic answer for the empty search
            String ListElement1;
            String ListElement2;
            if (searchWord.equals("")) {
                ListElement1 = getResources().getString(R.string.PleaseEnterWord);
                ListElement2 = "";
            }
            else {
                ListElement1 = getResources().getString(R.string.NoMatchFound);
                ListElement2 = "";
            }
            childSubElements.add(ListElement1);
            childSubElements.add(ListElement2);
            childSubElements.add("");
            childSubElements.add("");
            childSubElements.add("");
            childSubElements.add("");
            childSubElements.add("");
            childSubElements.add("");
            mExpandableListDataHeader.add(Integer.toString(0));
            mExpandableListHeaderDetails.put(Integer.toString(0), childSubElements);

            childSubElements = new ArrayList<>();
            childSubElements.add("No match found.");
            childSubElements.add("");
            childSubElements.add("");
            childSubElements.add("");
            childSubElements.add("");
            childSubElements.add("");
            childSubElements.add("");
            childSubElements.add("");
            childElements = new ArrayList<>();
            childElements.add(childSubElements);
            mExpandableListDataChild.put(Integer.toString(0), childElements);
        }
        else {
            for (int i = 0; i< wordsList.size(); i++) {

                Word currentWord = wordsList.get(i);
                List<Word.Meaning> currentMeanings = currentWord.getMeanings();

                //Populate elements in mListDataHeader
                if (i> MAX_NUMBER_RESULTS_SHOWN) {break;}

                headerElements = new ArrayList<>();

                String cumulative_meaning_value = "";
                for (int j = 0; j< currentMeanings.size(); j++) {
                    cumulative_meaning_value += currentMeanings.get(j).getMeaning();
                    if (j< currentMeanings.size()-1) { cumulative_meaning_value += ", "; }
                }
                headerElements.add(currentWord.getRomaji());
                headerElements.add(currentWord.getKanji());
                headerElements.add(removeDuplicatesFromCommaList(cumulative_meaning_value));

                mExpandableListDataHeader.add(Integer.toString(i));
                mExpandableListHeaderDetails.put(Integer.toString(i), headerElements);

                //Populate elements in mListDataChild
                childElements = new ArrayList<>();

                childSubElements = new ArrayList<>();
                childSubElements.add(currentWord.getAltSpellings());
                if (currentMeanings.size()>0) {
                    childSubElements.add(currentMeanings.get(currentMeanings.size() - 1).getType());
                }
                else {
                    childSubElements.add("");
                }

                childElements.add(childSubElements);

                for (int j = 0; j< currentMeanings.size(); j++) {
                    childSubElements = new ArrayList<>();
                    int element_index = 0;

                    childSubElements.add(""); element_index++; //Altspellings placeholder
                    childSubElements.add(currentMeanings.get(j).getType()); element_index++;
                    childSubElements.add(currentMeanings.get(j).getMeaning()); element_index++;
                    childSubElements.add(currentMeanings.get(j).getAntonym()); element_index++;
                    childSubElements.add(currentMeanings.get(j).getSynonym()); element_index++;

                    List<Word.Meaning.Explanation> currentExplanations = currentMeanings.get(j).getExplanations();
                    for (int m = 0; m< currentExplanations.size(); m++) {

                        childSubElements.add("EXPL" + currentExplanations.get(m).getExplanation());
                        element_index++;

                        childSubElements.add("RULE" + currentExplanations.get(m).getRules());
                        element_index++;

                        List<Word.Meaning.Explanation.Example> currentExamples = currentExplanations.get(m).getExamples();
                        int show_examples_element_index = 0;
                        String new_show_examples_element;
                        if (currentExamples.size() > 0) {
                            childSubElements.add("SHOW EXAMPLES AT INDEXES:");
                            show_examples_element_index = element_index;
                            element_index++;
                        }

                        for (int k = 0; k < currentExamples.size(); k++) {
                            childSubElements.add("EXEN" + currentExamples.get(k).getEnglishSentence());
                            new_show_examples_element = childSubElements.get(show_examples_element_index) + element_index + ":";
                            childSubElements.set(show_examples_element_index, new_show_examples_element);
                            element_index++;

                            childSubElements.add("EXRO" + currentExamples.get(k).getRomajiSentence());
                            new_show_examples_element = childSubElements.get(show_examples_element_index) + element_index + ":";
                            childSubElements.set(show_examples_element_index, new_show_examples_element);
                            element_index++;

                            childSubElements.add("EXKJ" + currentExamples.get(k).getKanjiSentence());
                            new_show_examples_element = childSubElements.get(show_examples_element_index) + element_index + ":";
                            childSubElements.set(show_examples_element_index, new_show_examples_element);
                            element_index++;
                        }

                    }

                    childElements.add(childSubElements);
                }
                mExpandableListDataChild.put(Integer.toString(i), childElements);

            }
        }
    }
    private void registerThatUserIsRequestingDictSearch(Boolean state) {
        if (getActivity() != null) {
            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(getString(R.string.requestingDictSearch), state);
            editor.apply();
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
    public Boolean getShowOnlineResultsPreference() {
        Boolean showOnlineResults = false;
        if (getActivity()!=null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            showOnlineResults = sharedPreferences.getBoolean(getString(R.string.pref_complete_local_with_online_search_key),
                    getResources().getBoolean(R.bool.pref_complete_local_with_online_search_default));
        }
        return showOnlineResults;
    }

    //Interface methods
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
        Boolean state;
        if (getActivity()!=null) {
            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
            state = sharedPref.getBoolean(getString(R.string.requestingDictSearch), false);
            return state;
        }
        else return true;
    }

	//Grammar Module methods
    private List<int[]>                 bubbleSortForTwoIntegerList(List<int[]> MatchList) {

        // Sorting the results according to the shortest keyword as found in the above search

        // Computing the value length
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

        List<int[]> sortedMatchList = new ArrayList<>();
        int[] element;
        for (int i=0;i<list_size;i++) {
            element = new int[2];
            element[0] = matches[i][0];
            element[1] = matches[i][1];
            sortedMatchList.add(element);
        }

        return sortedMatchList;
    }
    static public int[]                 binarySearchInLatinIndex(boolean TypeisLatin, String concatenated_word, String concatenated_translationLatin, List<String[]> SortedIndex) {

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
    static public int[]                 binarySearchInUTF8Index(String concatenated_word, List<String[]> SortedIndex, int relevant_column_index) {

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

    //ExpandableListView methods
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

            final List<String> childArray = (List<String>) getChild(groupPosition, childPosition);
            final List<List<String>> childrenArray = this._listDataChild.get(this._listDataHeader.get(groupPosition));
            List<String> headerDetailsArray = this._listHeaderDetails.get(this._listDataHeader.get(groupPosition));

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) this._context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                if (inflater==null) return null;
                convertView = inflater.inflate(R.layout.custom_grammar_list_child_item, null);
            }

            //regionInitialization
            int start_index;
            int end_index = 0;
            int number_of_hits;
            String type;

            LinearLayout elements_container = convertView.findViewById(R.id.elements_container);
            elements_container.removeAllViews();
            LinearLayout ChosenItem = convertView.findViewById(R.id.child_item_chosen_item_linearlayout);
            TextView ChosenItem_RomajiX = convertView.findViewById(R.id.child_item_chosen_item_romaji);
            TextView ChosenItem_KanjiX = convertView.findViewById(R.id.child_item_chosen_item_kanji);
            //endregion

            //regionSetting the alternate spellings
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
            //endregion

            //regionSetting the type and meaning
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
                        "'>" + "[" +
                        full_type +
                        "] " + "</font></i>" + "<b>" +
                        childArray.get(2) +
                        "</b>";
                Spanned type_and_meaning = SharedMethods.fromHtml(htmlText);
                TextView tv_type_and_meaning = new TextView(getContext());
                tv_type_and_meaning.setText(type_and_meaning);
                tv_type_and_meaning.setTextColor(getResources().getColor(R.color.textColorDictionaryTypeMeaning2));
                tv_type_and_meaning.setTextSize(15);
                //tv_type_and_meaning.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                elements_container.addView(tv_type_and_meaning);
            }
            //endregion

            //regionShowing the romaji and kanji values for user click
            if (childPosition == 0) {

                ChosenItem.setVisibility(View.VISIBLE);

                if (type.contains("V") && !type.equals("VC")) {
                    setHyperlinksInCopyToInputLine("verb", ChosenItem_RomajiX, "Conjugate ", headerDetailsArray.get(0), " ");
                    setHyperlinksInCopyToInputLine("verb", ChosenItem_KanjiX, "(", headerDetailsArray.get(1), ").");
                } else {
                    setHyperlinksInCopyToInputLine("word", ChosenItem_RomajiX, "Copy ", headerDetailsArray.get(0), " ");
                    setHyperlinksInCopyToInputLine("word", ChosenItem_KanjiX, "(", headerDetailsArray.get(1), ") to input.");
                }
            }
            else {
                ChosenItem.setVisibility(View.GONE);
            }
            //endregion

            //regionSetting the antonym
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
            //endregion

            //regionSetting the synonym
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
            //endregion

            //regionSetting the explanation, rule show/hide line and examples
            if (childPosition > 0) {
                String current_element;

                final List<TextView> elements = new ArrayList<>();
                for (int i=0; i<childArray.size(); i++) {
                    final TextView current_element_TextView = new TextView(getContext());
                    elements.add(current_element_TextView);
                }

                for (int i = 5; i < childArray.size(); i++) {
                    current_element = childArray.get(i);
                    final int currentPosition = i;

                    if (current_element.length() > 4) {

                        elements_container.addView(elements.get(i));

                        //Setting the explanation characteristics
                        switch (current_element.substring(0, 4)) {
                            case "EXPL":
                                elements.get(i).setText(current_element.substring(4, current_element.length()));
                                elements.get(i).setTextColor(getResources().getColor(R.color.textColorDictionaryExplanation));
                                elements.get(i).setPadding(0, 10, 0, 0);
                                elements.get(i).setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
                                break;

                            //Setting the rule characteristics
                            case "RULE":
                                elements.get(i).setTextColor(getResources().getColor(R.color.textColorDictionaryRule));
                                elements.get(i).setPadding(0, 30, 0, 0);
                                elements.get(i).setTypeface(Typeface.DEFAULT, Typeface.BOLD);

                                List<String> parsedRule = Arrays.asList(current_element.substring(4, current_element.length()).split("@"));
                                String where = " where: ";
                                String intro = "";
                                if (!parsedRule.get(0).contains(":")) {
                                    intro = getResources().getString(R.string.PhraseStructure) + " ";
                                }
                                Spanned spanned_rule;

                                if (parsedRule.size() == 1) { // If the rule doesn't have a "where" clause
                                    String htmlText = "<b>" +
                                            "<font color='" + getResources().getColor(R.color.textColorDictionaryRulePhraseStructureClause) + "'>" +
                                            intro +
                                            "</font>" +
                                            current_element.substring(4, current_element.length());
                                    spanned_rule = SharedMethods.fromHtml(htmlText);
                                    elements.get(i).setText(spanned_rule);
                                    elements.get(i).setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                                } else {
                                    String htmlText = "<b>" +
                                            "<font color='" + getResources().getColor(R.color.textColorDictionaryRulePhraseStructureClause) + "'>" +
                                            intro +
                                            "</font>" +
                                            parsedRule.get(0) +
                                            "</b>" + "<font color='" + getResources().getColor(R.color.textColorDictionaryRuleWhereClause) + "'>" +
                                            where +
                                            "</font>" +
                                            "<b>" + parsedRule.get(1) + "</b>";
                                    spanned_rule = SharedMethods.fromHtml(htmlText);
                                    elements.get(i).setText(spanned_rule);
                                }
                                break;

                            //Setting the show/hide examples line characteristics
                            case "SHOW":

                                //If there are no examples, hide this line
                                if (current_element.equals("SHOW EXAMPLES AT INDEXES:"))
                                    elements.get(i).setVisibility(View.GONE);

                                elements.get(i).setText(getResources().getString(R.string.ShowExamples));
                                elements.get(i).setTextColor(getResources().getColor(R.color.textColorDictionaryExamples));
                                elements.get(i).setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                                elements.get(i).setPadding(0, 10, 0, 0);
                                elements.get(i).setClickable(true);
                                elements.get(i).setFocusable(false);

                                final List<String> exampleSentencesIndexesForCurrentRule = Arrays.asList(current_element.split(":"));

                                elements.get(i).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        int index;

                                        //Check if the example sentences are hidden and set the indicator (if the first is hidden, then so are the others)
                                        if (exampleSentencesIndexesForCurrentRule.size() > 1) {
                                            index = Integer.parseInt(exampleSentencesIndexesForCurrentRule.get(1));
                                            if (elements.get(index).getVisibility() == View.VISIBLE) {
                                                elements.get(currentPosition).setText(getResources().getString(R.string.ShowExamples));
                                            } else {
                                                elements.get(currentPosition).setText(getResources().getString(R.string.HideExamples));
                                            }
                                        }

                                        //In any case, reverse GONE <> VISIBLE on click
                                        for (int j = 1; j < exampleSentencesIndexesForCurrentRule.size(); j++) {
                                            if (!exampleSentencesIndexesForCurrentRule.get(j).equals("")) {
                                                index = Integer.parseInt(exampleSentencesIndexesForCurrentRule.get(j));
                                                reverseVisibility(elements.get(index));
                                            }
                                        }
                                    }
                                });
                                break;

                            //Setting the English example characteristics
                            case "EXEN":
                                elements.get(i).setText(current_element.substring(4, current_element.length()));
                                elements.get(i).setTextColor(getResources().getColor(R.color.textColorDictionaryExampleEnglish));
                                elements.get(i).setTextSize(14);
                                elements.get(i).setPadding(4, 15, 0, 0);
                                elements.get(i).setVisibility(View.GONE);
                                break;

                            //Setting the Romaji example characteristics
                            case "EXRO":
                                elements.get(i).setText(current_element.substring(4, current_element.length()));
                                elements.get(i).setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
                                elements.get(i).setTextColor(getResources().getColor(R.color.textColorDictionaryExampleRomaji));
                                elements.get(i).setTextSize(14);
                                elements.get(i).setPadding(4, 0, 0, 0);
                                elements.get(i).setVisibility(View.GONE);
                                break;

                            //Setting the Kanji example characteristics
                            case "EXKJ":
                                elements.get(i).setText(current_element.substring(4, current_element.length()));
                                elements.get(i).setTextColor(getResources().getColor(R.color.textColorDictionaryExampleKanji));
                                elements.get(i).setTextSize(14);
                                elements.get(i).setPadding(4, 0, 0, 0);
                                elements.get(i).setVisibility(View.GONE);
                                break;
                        }
                    }
                }
            }
            //endregion

            elements_container.setFocusable(false);
            convertView.setClickable(true);

            return convertView;
        }
        void setHyperlinksInCopyToInputLine(String type, TextView textView, String before, String hyperlinkText, String after) {
            String totalText = "<b>" +
                    "<font color='" + getResources().getColor(R.color.textColorSecondary) + "'>" +
                    before +
                    "</font>" +
                    hyperlinkText +
                    "<font color='" + getResources().getColor(R.color.textColorSecondary) + "'>" +
                    after +
                    "</font>";
            Spanned spanned_totalText = SharedMethods.fromHtml(totalText);
            SpannableString WordSpannable = new SpannableString(spanned_totalText);
            if (type.equals("word")) {
                WordSpannable.setSpan(new WordClickableSpan(), before.length(), spanned_totalText.length() - after.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                WordSpannable.setSpan(new VerbClickableSpan(), before.length(), spanned_totalText.length() - after.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            textView.setText(WordSpannable);
            textView.setTypeface(Typeface.SERIF);
            textView.setTypeface(null, Typeface.BOLD_ITALIC);
            textView.setTextSize(16);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        }
        void reverseVisibility(TextView textView) {
            if (textView.getVisibility() == View.VISIBLE) {
                textView.setVisibility(View.GONE);
            }
            else {
                textView.setVisibility(View.VISIBLE);
            }
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
                LayoutInflater inflater = (LayoutInflater) this._context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                if (inflater ==null) return null;
                convertView = inflater.inflate(R.layout.custom_grammar_list_group_item, null);
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

    //Firebase methods
    private void updateFirebaseDbWithJishoWords(List<Word> asyncMatchingWords) {

        for (int i=0; i<asyncMatchingWords.size(); i++) {
            Word word = asyncMatchingWords.get(i);
            if (word.getCommonStatus()==1) writeWordToFirebaseDb(word);
        }

    }
    private void writeWordToFirebaseDb(Word word) {
        word.setUniqueIdentifier(word.getRomaji()+"-"+word.getKanji());
        mFirebaseDbReference.child("wordsList").child(word.getUniqueIdentifier()).setValue(word);

        //To update only specific information in the child:
        //mFirebaseDbReference.child("wordsList").child(word.getUniqueIdentifier()).child("romaji").setValue(newValue);
    }
    private void updateWordInFirebase(Word word, String key, String value) {
        mFirebaseDbReference.child("wordsList")
                .child(word.getUniqueIdentifier())
                .child(key)
                .setValue(value);
    }
    private Word setupWordListenerFromFirebase(String uniqueIdentifier) {

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference wordRef = rootRef.child("wordsList").child(uniqueIdentifier);

        ValueEventListener eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Word> wordsList = new ArrayList<>();
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    String value = ds.getValue(String.class);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };
        wordRef.addListenerForSingleValueEvent(eventListener);


//        mWordReference = FirebaseDatabase.getInstance().getReference(uniqueIdentifier);
//        mWordFromFirebase = new Word();
//
//        ChildEventListener wordListener = new ChildEventListener() {
//            @Override
//            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//                String a="";
//                if (dataSnapshot.exists()) {
//                    mWordFromFirebase = dataSnapshot.getValue(Word.class);
//                }
//            }
//
//            @Override
//            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//                String a="";
//                if (dataSnapshot.exists()) {
//                    mWordFromFirebase = dataSnapshot.getValue(Word.class);
//                }
//            }
//
//            @Override
//            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
//
//            }
//
//            @Override
//            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Log.w(FIREBASE_DEBUG, "Failed to get Word from database", databaseError.toException());
//            }
//        };
//
//        mWordReference.addChildEventListener(wordListener);

        return mWordFromFirebase;
    }
    private void setupFullWordsListListenerFromFirebase() {

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference wordsListRef = rootRef.child("wordsList");
        ValueEventListener eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Word> wordsList = new ArrayList<>();
                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    Word word = ds.getValue(Word.class);
                    wordsList.add(word);
                }
                String a="";
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };

        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Word word = dataSnapshot.getValue(Word.class);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Word word = dataSnapshot.getValue(Word.class);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(FIREBASE_DEBUG, "Failed to get Word from database", databaseError.toException());
            }
        };

        wordsListRef.addChildEventListener(childEventListener);
        //wordsListRef.addListenerForSingleValueEvent(eventListener);

    }
    private void deleteWordFromFirebase(Word word) {
        mWordReference = FirebaseDatabase.getInstance().getReference(word.getUniqueIdentifier());
    }
}