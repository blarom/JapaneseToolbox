package com.japanesetoolboxapp.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.japanesetoolboxapp.R;
import com.japanesetoolboxapp.data.DatabaseUtilities;
import com.japanesetoolboxapp.data.FirebaseDao;
import com.japanesetoolboxapp.data.JapaneseToolboxRoomDatabase;
import com.japanesetoolboxapp.data.Word;
import com.japanesetoolboxapp.resources.MainApplication;
import com.japanesetoolboxapp.resources.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class DictionaryFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<List<Word>>,
        FirebaseDao.FirebaseOperationsHandler {


    //region Parameters
    @BindView(R.id.SentenceConstructionExpandableListView) ExpandableListView mSearchResultsExpandableListView;
    @BindView(R.id.dict_results_loading_indicator) ProgressBar mProgressBarLoadingIndicator;
    private static final int MAX_NUMBER_RESULTS_SHOWN = 50;
    private List<String> mExpandableListDataHeader;
    private HashMap<String, List<String>> mExpandableListHeaderDetails;
    private HashMap<String, List<List<String>>> mExpandableListDataChild;
    private String mInputQuery;
    private Boolean mInternetIsAvailable;
    private static final int JISHO_WEB_SEARCH_LOADER = 41;
    private static final int ROOM_DB_SEARCH_LOADER = 42;
    Toast mShowOnlineResultsToast;
    private List<Word> mLocalMatchingWordsList;
    private List<Word> mMergedMatchingWordsList;
    JapaneseToolboxRoomDatabase mJapaneseToolboxRoomDatabase;
    private Boolean mShowOnlineResults;
    private FirebaseDao mFirebaseDao;
    private Unbinder mBinding;
    private boolean mAlreadyLoadedRoomResults;
    private boolean mAlreadyLoadedJishoResults;
    private List<String[]> mLegendDatabase;
    //endregion


    //Fragment Lifecycle methods
    @Override public void onAttach(Context context) {
        super.onAttach(context);
        dictionaryFragmentOperationsHandler = (DictionaryFragmentOperationsHandler) context;
   }
    @Override public void onCreate(Bundle savedInstanceState) { //instead of onActivityCreated
        super.onCreate(savedInstanceState);

        getExtras();
        if (getContext() != null) mInternetIsAvailable = Utilities.internetIsAvailableCheck(getContext());
        initializeParameters();

    }
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //setRetainInstance(true);
        final View rootView = inflater.inflate(R.layout.fragment_dictionary, container, false);

        mBinding = ButterKnife.bind(this, rootView);

        if (savedInstanceState==null) {
            getQuerySearchResults();
        }

        //If the fragment is being resumed, just reload the saved data
        else {
            mInputQuery = savedInstanceState.getString(getString(R.string.saved_input_query));
            mLocalMatchingWordsList = savedInstanceState.getParcelableArrayList(getString(R.string.saved_local_results));
            mMergedMatchingWordsList = savedInstanceState.getParcelableArrayList(getString(R.string.saved_merged_results));
        }

        return rootView;
    }
    @Override public void onPause() {
        super.onPause();
    }
    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(getString(R.string.saved_local_results), new ArrayList<>(mLocalMatchingWordsList));
        outState.putParcelableArrayList(getString(R.string.saved_merged_results), new ArrayList<>(mMergedMatchingWordsList));
        outState.putString(getString(R.string.saved_input_query), mInputQuery);

        destroyLoaders();

    }
    @Override public void onResume() {
        super.onResume();
    }
    @Override public void onDetach() {
        super.onDetach();
        destroyLoaders();
        mFirebaseDao.removeListeners();
        destroyLoaders();
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }
    @Override public void onDestroy() {
        super.onDestroy();
        if (getActivity()!=null && MainApplication.getRefWatcher(getActivity())!=null) MainApplication.getRefWatcher(getActivity()).watch(this);
    }


    //Asynchronous methods
    @NonNull @Override public Loader<List<Word>> onCreateLoader(int id, final Bundle args) {

        String inputQuery = "";
        if (args!=null && args.getString(getString(R.string.saved_input_query))!=null) {
            inputQuery = args.getString(getString(R.string.saved_input_query));
        }

        if (id == JISHO_WEB_SEARCH_LOADER) {
            JishoResultsAsyncTaskLoader jishoResultsAsyncTaskLoader = new JishoResultsAsyncTaskLoader(getContext(), inputQuery, mInternetIsAvailable);
            jishoResultsAsyncTaskLoader.setLoaderState(true);
            return jishoResultsAsyncTaskLoader;
        }
        else if (id == ROOM_DB_SEARCH_LOADER){
            RoomDbWordSearchAsyncTaskLoader roomDbSearchLoader = new RoomDbWordSearchAsyncTaskLoader(getContext(), inputQuery);
            return roomDbSearchLoader;
        }
        else return new RoomDbWordSearchAsyncTaskLoader(getContext(), "");
    }
    @Override public void onLoadFinished(@NonNull Loader<List<Word>> loader, List<Word> loaderResultWordsList) {

        hideLoadingIndicator();
        if (loader.getId() == ROOM_DB_SEARCH_LOADER && !mAlreadyLoadedRoomResults) {
            mAlreadyLoadedRoomResults = true;
            mLocalMatchingWordsList = loaderResultWordsList;

            //Displaying the local results
            displayWordsToUser(mLocalMatchingWordsList);

            //If wanted, update the results with words from Jisho.org
            mShowOnlineResults = Utilities.getShowOnlineResultsPreference(getActivity());
            if (mShowOnlineResults) startSearchingForJishoWords();

            //Otherwise (if online results are unwanted), if there are no local results to display then try the reverse verb search
            else if (mLocalMatchingWordsList.size()==0) performConjSearch();

            if (getLoaderManager()!=null) getLoaderManager().destroyLoader(ROOM_DB_SEARCH_LOADER);
        }
        else if (loader.getId() == JISHO_WEB_SEARCH_LOADER && !mAlreadyLoadedJishoResults) {
            mAlreadyLoadedJishoResults = true;

            List<Word> jishoWords = Utilities.cleanUpProblematicWordsFromJisho(loaderResultWordsList);

            //If wanted, update the results with words from Jisho.org by merging the lists, otherwise clear the jisho results
            Boolean showOnlineResults = Utilities.getShowOnlineResultsPreference(getActivity());
            if (!showOnlineResults) jishoWords = new ArrayList<>();

            if (jishoWords.size() != 0) {
                mMergedMatchingWordsList = Utilities.getMergedWordsList(mLocalMatchingWordsList, jishoWords);
                mMergedMatchingWordsList = sortWordsAccordingToRomajiAndKanjiLengths(mMergedMatchingWordsList);

                List<Word> differentJishoWords = Utilities.getDifferentAsyncWords(mLocalMatchingWordsList, jishoWords);
                updateFirebaseDbWithJishoWords(differentJishoWords);

                displayResultsInListView(mMergedMatchingWordsList);
            }
            else {
                //if there are no jisho results (for whatever reason) then display only the local results
                if (mLocalMatchingWordsList.size()!=0) {
                    //The results should have already been displayed, therefore the following line is commented out
                    //displayResultsInListView(mLocalMatchingWordsList);
                }

                //if there are no jisho results (for whatever reason) and no local results to display, then try the reverse verb search on the input
                else {
                    performConjSearch();
                }
            }

            if (getLoaderManager()!=null) getLoaderManager().destroyLoader(JISHO_WEB_SEARCH_LOADER);

        }

    }
    @Override public void onLoaderReset(@NonNull Loader<List<Word>> loader) {}
    private static class JishoResultsAsyncTaskLoader extends AsyncTaskLoader <List<Word>> {

        String mQuery;
        private boolean internetIsAvailable;
        private boolean mAllowLoaderStart;

        JishoResultsAsyncTaskLoader(Context context, String query, boolean internetIsAvailable) {
            super(context);
            this.mQuery = query;
            this.internetIsAvailable = internetIsAvailable;
        }

        @Override
        protected void onStartLoading() {
            if (mAllowLoaderStart) forceLoad();
        }

        @Override
        public List<Word> loadInBackground() {

            List<Word> matchingWordsFromJisho = new ArrayList<>();

            if (internetIsAvailable && !TextUtils.isEmpty(mQuery)) {
                matchingWordsFromJisho = Utilities.getWordsFromJishoOnWeb(mQuery, getContext());
            } else {
                Log.i("Diagnosis Time", "Failed to access online resources.");
                if (Looper.myLooper()==null) Looper.prepare();
                Toast.makeText(getContext(), R.string.failed_to_connect_to_internet, Toast.LENGTH_SHORT).show();
                cancelLoadInBackground();
            }
            return matchingWordsFromJisho;
        }

        void setLoaderState(boolean state) {
            mAllowLoaderStart = state;
        }
    }
    private static class RoomDbWordSearchAsyncTaskLoader extends AsyncTaskLoader <List<Word>> {

        String mSearchWord;
        private List<Long> mMatchingWordIds;

        RoomDbWordSearchAsyncTaskLoader(Context context, String searchWord) {
            super(context);
            mSearchWord = searchWord;
        }

        @Override
        protected void onStartLoading() {
            if (!TextUtils.isEmpty(mSearchWord)) forceLoad();
        }

        @Override
        public List<Word> loadInBackground() {

            List<Word> localMatchingWordsList = new ArrayList<>();
            if (!TextUtils.isEmpty(mSearchWord)) {
                JapaneseToolboxRoomDatabase japaneseToolboxRoomDatabase = JapaneseToolboxRoomDatabase.getInstance(getContext());
                mMatchingWordIds = DatabaseUtilities.getMatchingWordIdsUsingRoomIndexes(mSearchWord, japaneseToolboxRoomDatabase);
                localMatchingWordsList = japaneseToolboxRoomDatabase.getWordListByWordIds(mMatchingWordIds);
            }

            return localMatchingWordsList;
        }
    }


	//Functionality methods
    private void getExtras() {
        if (getArguments()!=null) {
            mInputQuery = getArguments().getString(getString(R.string.user_query_word));
            mLegendDatabase = (List<String[]>) getArguments().getSerializable(getString(R.string.legend_database));
        }
    }
    private void initializeParameters() {

        mFirebaseDao = new FirebaseDao(getContext(), this);
        mInputQuery = Utilities.removeSpecialCharacters(mInputQuery);

        mJapaneseToolboxRoomDatabase = JapaneseToolboxRoomDatabase.getInstance(getContext());
        //mWordsInRoomDatabase = mJapaneseToolboxRoomDatabase.getAllWords();

        mLocalMatchingWordsList = new ArrayList<>();
        mMergedMatchingWordsList = new ArrayList<>();

        mAlreadyLoadedRoomResults = false;
        mAlreadyLoadedJishoResults = false;
    }
    private void getQuerySearchResults() {
        if (!TextUtils.isEmpty(mInputQuery)) findMatchingWordsInRoomDb();
        else showEmptySearchResults();
    }
    private void findMatchingWordsInRoomDb() {
        if (getActivity()!=null) {
            showLoadingIndicator();
            LoaderManager loaderManager = getActivity().getSupportLoaderManager();
            Loader<String> roomDbSearchLoader = loaderManager.getLoader(ROOM_DB_SEARCH_LOADER);
            Bundle bundle = new Bundle();
            bundle.putString(getString(R.string.saved_input_query), mInputQuery);
            if (roomDbSearchLoader == null) loaderManager.initLoader(ROOM_DB_SEARCH_LOADER, bundle, this);
            else loaderManager.restartLoader(ROOM_DB_SEARCH_LOADER, bundle, this);
        }
    }
    private void showEmptySearchResults() {
        displayResultsInListView(new ArrayList<Word>());
    }
    private void displayWordsToUser(List<Word> localMatchingWordsList) {

        localMatchingWordsList = sortWordsAccordingToRomajiAndKanjiLengths(localMatchingWordsList);
        displayResultsInListView(localMatchingWordsList);

    }
    private void startSearchingForJishoWords() {

        if (!TextUtils.isEmpty(mInputQuery)) {

            mShowOnlineResultsToast = Toast.makeText(getContext(), getResources().getString(R.string.showOnlineResultsToastString), Toast.LENGTH_SHORT);
            mShowOnlineResultsToast.show();

            if (getActivity() != null) {

                Bundle queryBundle = new Bundle();
                queryBundle.putString(getString(R.string.saved_input_query), mInputQuery);

                //Attempting to access jisho.org to complete the results found in the local dictionary
                LoaderManager loaderManager = getActivity().getSupportLoaderManager();
                Loader<String> JishoWebSearchLoader = loaderManager.getLoader(JISHO_WEB_SEARCH_LOADER);
                if (JishoWebSearchLoader == null) loaderManager.initLoader(JISHO_WEB_SEARCH_LOADER, queryBundle, this);
                else loaderManager.restartLoader(JISHO_WEB_SEARCH_LOADER, queryBundle, this);
            }
        }

    }
    private void performConjSearch() {

        destroyLoaders();

        if (!TextUtils.isEmpty(mInputQuery)) {
            if (mShowOnlineResultsToast!=null) mShowOnlineResultsToast.cancel();
            dictionaryFragmentOperationsHandler.onVerbConjugationFromDictRequested(mInputQuery);
        }
    }
    private void displayResultsInListView(List<Word> wordsList) {

        // Populate the list of choices for the SearchResultsChooserSpinner. Each text element of inside the idividual spinner choices corresponds to a sub-element of the choicelist
        createExpandableListViewContentsFromWordsList(wordsList);
        showExpandableListViewWithContents();
    }
    private void createExpandableListViewContentsFromWordsList(List<Word> wordsList) {

        if (getContext()==null) return;
        
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
            if (mInputQuery.equals("")) {
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
                headerElements.add(Utilities.removeDuplicatesFromCommaList(cumulative_meaning_value));

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
    private void showExpandableListViewWithContents() {

        if (getActivity()!=null) Utilities.hideSoftKeyboard(getActivity());

        // Implementing the SearchResultsChooserListView
        try {
            if (mExpandableListDataHeader != null
                    && mExpandableListHeaderDetails != null
                    && mExpandableListDataChild != null
                    && getView() != null) {

                GrammarExpandableListAdapter mSearchResultsListAdapter =
                        new GrammarExpandableListAdapter(
                                getContext(),
                                mExpandableListDataHeader,
                                mExpandableListHeaderDetails,
                                mExpandableListDataChild);

                mSearchResultsExpandableListView.setAdapter(mSearchResultsListAdapter);
                mSearchResultsExpandableListView.setVisibility(View.VISIBLE);
            }
        }
        catch (NullPointerException e) {
            //If a NullPointerException happens, restart activity since the list cannot be displayed
            Intent intent = new Intent(getContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }
    private List<Word> sortWordsAccordingToRomajiAndKanjiLengths(List<Word> wordsList) {

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
                if (currentMeaning.contains(mInputQuery) && currentMeaning.length() <= currentMeaningLength) {
                    currentMeaningLength = currentMeaning.length();
                }
            }

            //Get the total length
            int length = romaji_value.length() + kanji_value.length() + currentMeaningLength;

            //If the romaji or Kanji value is an exact match to the search word, then it must appear at the start of the list
            if (romaji_value.equals(mInputQuery) || kanji_value.equals(mInputQuery)) length = 0;

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
    private List<int[]> bubbleSortForTwoIntegerList(List<int[]> MatchList) {

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
    private void updateFirebaseDbWithJishoWords(List<Word> wordsList) {
        mFirebaseDao.updateObjectsOrCreateThemInFirebaseDb(Utilities.getCommonWords(wordsList));
    }
    private void destroyLoaders() {
        LoaderManager loaderManager = getLoaderManager();
        if (loaderManager!=null) {
            loaderManager.destroyLoader(ROOM_DB_SEARCH_LOADER);
            loaderManager.destroyLoader(JISHO_WEB_SEARCH_LOADER);
        }
    }
    private void showLoadingIndicator() {
        if (mProgressBarLoadingIndicator!=null) mProgressBarLoadingIndicator.setVisibility(View.VISIBLE);
    }
    private void hideLoadingIndicator() {
        if (mProgressBarLoadingIndicator!=null) mProgressBarLoadingIndicator.setVisibility(View.INVISIBLE);
    }
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
                    Spanned spanned_alternatespellings = Utilities.fromHtml(htmlText);
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
                for (int i=0; i<mLegendDatabase.size(); i++) {
                    if (mLegendDatabase.get(i)[0].equals(type)) { full_type = mLegendDatabase.get(i)[1]; break; }
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
                Spanned type_and_meaning = Utilities.fromHtml(htmlText);
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
                                    spanned_rule = Utilities.fromHtml(htmlText);
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
                                    spanned_rule = Utilities.fromHtml(htmlText);
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
            Spanned spanned_totalText = Utilities.fromHtml(totalText);
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
    private class WordClickableSpan extends ClickableSpan {
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
                dictionaryFragmentOperationsHandler = (DictionaryFragmentOperationsHandler) getActivity();
            } catch (ClassCastException e) {
                throw new ClassCastException(getActivity().toString() + " must implement TextClicked");
            }

            //Calling the interface
            String outputText = text.getText().subSequence(start, end).toString();
            dictionaryFragmentOperationsHandler.onQueryTextUpdateFromDictRequested(outputText);

        }
        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setColor(getResources().getColor(R.color.textColorDictionarySpanClicked));
            ds.setUnderlineText(false);
        }
    }
    private class VerbClickableSpan extends ClickableSpan {
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

            String outputText = text.getText().subSequence(start, end).toString();
            dictionaryFragmentOperationsHandler.onQueryTextUpdateFromDictRequested(outputText);
            dictionaryFragmentOperationsHandler.onVerbConjugationFromDictRequested(outputText);

        }

        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setColor(getResources().getColor(R.color.textColorDictionarySpanClicked));
            ds.setUnderlineText(false);
        }
    }


    //Communication with other classes

    //Communication with parent activity
    private DictionaryFragmentOperationsHandler dictionaryFragmentOperationsHandler;
    interface DictionaryFragmentOperationsHandler {
        void onQueryTextUpdateFromDictRequested(String selectedWordString);
        void onVerbConjugationFromDictRequested(String selectedVerbString);
    }
    public void setQuery(String query) {
        mInputQuery = query;
        mAlreadyLoadedRoomResults = false;
        mAlreadyLoadedJishoResults = false;
        getQuerySearchResults();
    }

    //Communication with Firebase DAO
    @Override public void onWordsListFound(List<Word> wordsList) {

    }
}