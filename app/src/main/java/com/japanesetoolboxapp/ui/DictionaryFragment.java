package com.japanesetoolboxapp.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.japanesetoolboxapp.R;
import com.japanesetoolboxapp.adapters.DictionaryRecyclerViewAdapter;
import com.japanesetoolboxapp.data.FirebaseDao;
import com.japanesetoolboxapp.data.JapaneseToolboxCentralRoomDatabase;
import com.japanesetoolboxapp.data.Word;
import com.japanesetoolboxapp.resources.MainApplication;
import com.japanesetoolboxapp.resources.Utilities;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class DictionaryFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<List<Word>>,
        FirebaseDao.FirebaseOperationsHandler, DictionaryRecyclerViewAdapter.DictionaryItemClickHandler {


    //region Parameters
    @BindView(R.id.dictionary_recyclerview) RecyclerView mDictionaryRecyclerView;
    @BindView(R.id.word_hint) TextView mHintTextView;
    @BindView(R.id.dict_results_loading_indicator) ProgressBar mProgressBarLoadingIndicator;
    private static final int MAX_NUMBER_RESULTS_SHOWN = 50;
    private String mInputQuery;
    private static final int JISHO_WEB_SEARCH_LOADER = 41;
    private static final int ROOM_DB_SEARCH_LOADER = 42;
    Toast mShowOnlineResultsToast;
    private List<Word> mLocalMatchingWordsList;
    private List<Word> mMergedMatchingWordsList;
    JapaneseToolboxCentralRoomDatabase mJapaneseToolboxCentralRoomDatabase;
    private Boolean mShowOnlineResults;
    private FirebaseDao mFirebaseDao;
    private Unbinder mBinding;
    private boolean mAlreadyLoadedRoomResults;
    private boolean mAlreadyLoadedJishoResults;
    private List<String[]> mLegendDatabase;
    private DictionaryRecyclerViewAdapter mDictionaryRecyclerViewAdapter;
    //endregion


    //Fragment Lifecycle methods
    @Override public void onAttach(Context context) {
        super.onAttach(context);
        dictionaryFragmentOperationsHandler = (DictionaryFragmentOperationsHandler) context;
   }
    @Override public void onCreate(Bundle savedInstanceState) { //instead of onActivityCreated
        super.onCreate(savedInstanceState);

        getExtras();
        initializeParameters();

    }
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //setRetainInstance(true);
        final View rootView = inflater.inflate(R.layout.fragment_dictionary, container, false);

        initializeViews(rootView);

        getQuerySearchResults();

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
            JishoResultsAsyncTaskLoader jishoResultsAsyncTaskLoader = new JishoResultsAsyncTaskLoader(getContext(), inputQuery);
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
            mLocalMatchingWordsList = sortWordsAccordingToLengths(mLocalMatchingWordsList);

            //Update the MainActivity with the matching words
            dictionaryFragmentOperationsHandler.onLocalMatchingWordsFound(mLocalMatchingWordsList);

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
                mMergedMatchingWordsList = sortWordsAccordingToLengths(mMergedMatchingWordsList);

                List<Word> differentJishoWords = Utilities.getDifferentAsyncWords(mLocalMatchingWordsList, jishoWords);
                if (differentJishoWords.size()==0) Toast.makeText(getContext(), R.string.no_new_words_or_meanings_found_online, Toast.LENGTH_SHORT).show();
                updateFirebaseDbWithJishoWords(differentJishoWords);

                displayResults(mMergedMatchingWordsList);
            }
            else {
                //if there are no jisho results (for whatever reason) then display only the local results
                if (mLocalMatchingWordsList.size()!=0) {
                    //The results should have already been displayed, therefore the following line is commented out
                    //displayResultsInListView(mLocalMatchingWordsList);
                }

                //if there are no jisho results (for whatever reason) and no local results to display, then try the reverse verb search on the input
                else {
                    Toast.makeText(getContext(), R.string.no_matching_words_online, Toast.LENGTH_SHORT).show();
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

        JishoResultsAsyncTaskLoader(Context context, String query) {
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

            internetIsAvailable = Utilities.internetIsAvailableCheck(getContext());

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
                JapaneseToolboxCentralRoomDatabase japaneseToolboxCentralRoomDatabase = JapaneseToolboxCentralRoomDatabase.getInstance(getContext());
                mMatchingWordIds = Utilities.getMatchingWordIdsUsingRoomIndexes(mSearchWord, japaneseToolboxCentralRoomDatabase);
                localMatchingWordsList = japaneseToolboxCentralRoomDatabase.getWordListByWordIds(mMatchingWordIds);
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

        mLocalMatchingWordsList = new ArrayList<>();
        mMergedMatchingWordsList = new ArrayList<>();

        mAlreadyLoadedRoomResults = false;
        mAlreadyLoadedJishoResults = false;
    }
    private void initializeViews(View rootView) {
        mBinding = ButterKnife.bind(this, rootView);

        mDictionaryRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        mDictionaryRecyclerView.setNestedScrollingEnabled(true);
        mDictionaryRecyclerViewAdapter = new DictionaryRecyclerViewAdapter(getContext(), this, null, mLegendDatabase);
        mDictionaryRecyclerView.setAdapter(mDictionaryRecyclerViewAdapter);
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
        displayResults(new ArrayList<Word>());
    }
    private void displayWordsToUser(List<Word> localMatchingWordsList) {

        localMatchingWordsList = sortWordsAccordingToLengths(localMatchingWordsList);
        displayResults(localMatchingWordsList);

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
    private void displayResults(List<Word> wordsList) {

        if (getActivity()!=null) Utilities.hideSoftKeyboard(getActivity());

        if (wordsList.size()>MAX_NUMBER_RESULTS_SHOWN) {
            List<Word> displayedWords = wordsList.subList(0,MAX_NUMBER_RESULTS_SHOWN);
            mDictionaryRecyclerViewAdapter.setContents(displayedWords);
        }
        else mDictionaryRecyclerViewAdapter.setContents(wordsList);

        if (wordsList.size()>0) {
            mHintTextView.setVisibility(View.GONE);
            mDictionaryRecyclerView.setVisibility(View.VISIBLE);
        }
        else {
            if (mInputQuery.equals("")) mHintTextView.setText(Utilities.fromHtml(getResources().getString(R.string.please_enter_word)));
            else mHintTextView.setText(Utilities.fromHtml(getResources().getString(R.string.no_match_found)));
            mHintTextView.setVisibility(View.VISIBLE);
            mDictionaryRecyclerView.setVisibility(View.GONE);
        }

        // Populate the list of choices for the SearchResultsChooserSpinner. Each text element of inside the individual spinner choices corresponds to a sub-element of the choicelist
        //createExpandableListViewContentsFromWordsList(wordsList);
        //showExpandableListViewWithContents();
    }
    private List<Word> sortWordsAccordingToLengths(List<Word> wordsList) {

        if (wordsList == null || wordsList.size()==0) return new ArrayList<>();

        List<long[]> matchingWordIndexesAndLengths = new ArrayList<>();

        //region Registering if the input query is a "to " verb
        boolean queryIsVerbWithTo = false;
        String queryWordWithoutTo = "";
        if (mInputQuery.length()>3 && mInputQuery.substring(0,3).equals("to ")) {
            queryIsVerbWithTo = true;
            queryWordWithoutTo = mInputQuery.substring(3, mInputQuery.length());
        }
        //endregion

        //region Replacing the Kana input word by its romaji equivalent
        String inputQuery = mInputQuery;
        String text_type = ConvertFragment.getTextType(inputQuery);
        if (text_type.equals("hiragana") || text_type.equals("katakana")) {
            List<String> translationList = ConvertFragment.getLatinHiraganaKatakana(inputQuery.replace(" ", ""));
            inputQuery = translationList.get(0);
        }
        //endregion

        for (int i = 0; i < wordsList.size(); i++) {

            Word currentWord = wordsList.get(i);
            if (currentWord==null) continue;

            int length = Utilities.getLengthFromWordAttributes(currentWord, inputQuery, queryWordWithoutTo, queryIsVerbWithTo);

            long[] currentMatchingWordIndexAndLength = new long[3];
            currentMatchingWordIndexAndLength[0] = i;
            currentMatchingWordIndexAndLength[1] = length;
            currentMatchingWordIndexAndLength[2] = 0;

            matchingWordIndexesAndLengths.add(currentMatchingWordIndexAndLength);
        }

        //Sort the results according to total length
        if (matchingWordIndexesAndLengths.size() != 0) {
            matchingWordIndexesAndLengths = Utilities.bubbleSortForThreeIntegerList(matchingWordIndexesAndLengths);
        }

        //Return the sorted list
        List<Word> sortedWordsList = new ArrayList<>();
        for (int i = 0; i < matchingWordIndexesAndLengths.size(); i++) {
            long sortedIndex = matchingWordIndexesAndLengths.get(i)[0];
            sortedWordsList.add(wordsList.get((int) sortedIndex));
        }

        return sortedWordsList;
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
        if (mHintTextView!=null) mHintTextView.setVisibility(View.GONE);
    }
    private void hideLoadingIndicator() {
        if (mProgressBarLoadingIndicator!=null) mProgressBarLoadingIndicator.setVisibility(View.INVISIBLE);
    }


    //Communication with other classes

    //Communication with DictionaryRecyclerViewAdapter
    @Override public void onWordLinkClicked(String text) {
        dictionaryFragmentOperationsHandler.onQueryTextUpdateFromDictRequested(text);
    }
    @Override public void onVerbLinkClicked(String text) {
        dictionaryFragmentOperationsHandler.onQueryTextUpdateFromDictRequested(text);
        dictionaryFragmentOperationsHandler.onVerbConjugationFromDictRequested(text);
    }

    //Communication with parent activity
    private DictionaryFragmentOperationsHandler dictionaryFragmentOperationsHandler;
    interface DictionaryFragmentOperationsHandler {
        void onQueryTextUpdateFromDictRequested(String selectedWordString);
        void onVerbConjugationFromDictRequested(String selectedVerbString);
        void onLocalMatchingWordsFound(List<Word> matchingWords);
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