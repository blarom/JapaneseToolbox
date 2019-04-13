package com.japanesetoolboxapp.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
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
import com.japanesetoolboxapp.data.ConjugationTitle;
import com.japanesetoolboxapp.data.FirebaseDao;
import com.japanesetoolboxapp.data.Verb;
import com.japanesetoolboxapp.data.Word;
import com.japanesetoolboxapp.loaders.JMDictESResultsAsyncTaskLoader;
import com.japanesetoolboxapp.loaders.JMDictFRResultsAsyncTaskLoader;
import com.japanesetoolboxapp.loaders.JishoResultsAsyncTaskLoader;
import com.japanesetoolboxapp.loaders.RoomDbWordSearchAsyncTaskLoader;
import com.japanesetoolboxapp.loaders.VerbSearchAsyncTaskLoader;
import com.japanesetoolboxapp.resources.GlobalConstants;
import com.japanesetoolboxapp.resources.LocaleHelper;
import com.japanesetoolboxapp.resources.MainApplication;
import com.japanesetoolboxapp.resources.Utilities;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class DictionaryFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Object>,
        FirebaseDao.FirebaseOperationsHandler,
        DictionaryRecyclerViewAdapter.DictionaryItemClickHandler {


    private static final int WORD_RESULTS_MAX_RESPONSE_DELAY = 2000;
    private static final String DEBUG_TAG = "JT DEBUG";
    public static final int MAX_NUM_WORDS_TO_SHARE = 30;
    //region Parameters
    @BindView(R.id.dictionary_recyclerview) RecyclerView mDictionaryRecyclerView;
    @BindView(R.id.word_hint) TextView mHintTextView;
    @BindView(R.id.dict_results_loading_indicator) ProgressBar mProgressBarLoadingIndicator;
    private static final int MAX_NUMBER_RESULTS_SHOWN = 50;
    private String mInputQuery;
    private static final int JISHO_WEB_SEARCH_LOADER = 41;
    private static final int ROOM_DB_SEARCH_LOADER = 42;
    private static final int JMDICTFR_WEB_SEARCH_LOADER = 43;
    private static final int JMDICTES_WEB_SEARCH_LOADER = 44;
    private static final int VERB_SEARCH_LOADER = 45;
    private Toast mShowOnlineResultsToast;
    private List<Word> mLocalMatchingWordsList;
    private List<Word> mMergedMatchingWordsList;
    private FirebaseDao mFirebaseDao;
    private Unbinder mBinding;
    private boolean mAlreadyLoadedRoomResults;
    private boolean mAlreadyLoadedJishoResults;
    private boolean mAlreadyLoadedJMDictFRResults;
    private boolean mAlreadyLoadedJMDictESResults;
    private boolean mAlreadyLoadedVerbs;
    private List<String[]> mVerbLatinConjDatabase;
    private List<String[]> mVerbKanjiConjDatabase;
    private List<ConjugationTitle> mConjugationTitles;
    private DictionaryRecyclerViewAdapter mDictionaryRecyclerViewAdapter;
    private List<Word> mJishoMatchingWordsList;
    private List<Word> mDifferentJishoWords;
    private List<Word> mMatchingWordsFromVerbs;
    private boolean mAlreadyDisplayedResults;
    private boolean mOverrideDisplayConditions;
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
    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        //outState.putParcelableArrayList(getString(R.string.saved_local_results), new ArrayList<>(mLocalMatchingWordsList)); //causes cash because parcel too big, can limit with sublist
        //outState.putParcelableArrayList(getString(R.string.saved_merged_results), new ArrayList<>(mMergedMatchingWordsList));
        outState.putString(getString(R.string.saved_input_query), mInputQuery);

        destroyLoaders();

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
    @NonNull @Override public Loader<Object> onCreateLoader(int id, final Bundle args) {

        String inputQuery = "";
        if (args!=null && args.getString(getString(R.string.saved_input_query))!=null) {
            inputQuery = args.getString(getString(R.string.saved_input_query));
            if (inputQuery != null) inputQuery = inputQuery.toLowerCase();
        }

        if (id == JISHO_WEB_SEARCH_LOADER) {
            JishoResultsAsyncTaskLoader asyncTaskLoader = new JishoResultsAsyncTaskLoader(getContext(), inputQuery);
            asyncTaskLoader.setLoaderState(true);
            return asyncTaskLoader;
        }
        else if (id == JMDICTFR_WEB_SEARCH_LOADER) {
            JMDictFRResultsAsyncTaskLoader asyncTaskLoader = new JMDictFRResultsAsyncTaskLoader(getContext(), inputQuery);
            asyncTaskLoader.setLoaderState(true);
            return asyncTaskLoader;
        }
        else if (id == JMDICTES_WEB_SEARCH_LOADER) {
            JMDictESResultsAsyncTaskLoader asyncTaskLoader = new JMDictESResultsAsyncTaskLoader(getContext(), inputQuery);
            asyncTaskLoader.setLoaderState(true);
            return asyncTaskLoader;
        }
        else if (id == ROOM_DB_SEARCH_LOADER){
            RoomDbWordSearchAsyncTaskLoader roomDbSearchLoader = new RoomDbWordSearchAsyncTaskLoader(getContext(), inputQuery);
            return roomDbSearchLoader;
        }
        else if (id == VERB_SEARCH_LOADER){
            VerbSearchAsyncTaskLoader verbSearchLoader = new VerbSearchAsyncTaskLoader(
                    getContext(), inputQuery, mConjugationTitles, mVerbLatinConjDatabase, mVerbKanjiConjDatabase, new ArrayList<Word>());
            return verbSearchLoader;
        }
        else return new RoomDbWordSearchAsyncTaskLoader(getContext(), "");
    }
    @Override public void onLoadFinished(@NonNull Loader<Object> loader, Object data) {

        if (getContext() == null) return;

        if (loader.getId() == ROOM_DB_SEARCH_LOADER && !mAlreadyLoadedRoomResults) {
            List<Word> loaderResultWordsList = (List<Word>) data;
            mAlreadyLoadedRoomResults = true;
            mLocalMatchingWordsList = loaderResultWordsList;
            mLocalMatchingWordsList = sortWordsAccordingToRanking(mLocalMatchingWordsList);

            int maxIndex = mLocalMatchingWordsList.size()> MAX_NUM_WORDS_TO_SHARE ? MAX_NUM_WORDS_TO_SHARE : mLocalMatchingWordsList.size();
            dictionaryFragmentOperationsHandler.onLocalMatchingWordsFound(mLocalMatchingWordsList.subList(0,maxIndex));

            Log.i(DEBUG_TAG, "Displaying Room words");
            displayMergedWordsToUser();

            if (getLoaderManager()!=null) getLoaderManager().destroyLoader(ROOM_DB_SEARCH_LOADER);
        }
        else if (loader.getId() == JISHO_WEB_SEARCH_LOADER && !mAlreadyLoadedJishoResults) {
            List<Word> loaderResultWordsList = (List<Word>) data;
            mAlreadyLoadedJishoResults = true;

            mJishoMatchingWordsList = Utilities.removeEdictExceptionsFromJisho(mJishoMatchingWordsList);
            mJishoMatchingWordsList = Utilities.cleanUpProblematicWordsFromJisho(loaderResultWordsList);
            for (Word word : mJishoMatchingWordsList) word.setIsLocal(false);

            if (!Utilities.getPreferenceShowOnlineResults(getActivity())) mJishoMatchingWordsList = new ArrayList<>();

            if (mJishoMatchingWordsList.size() != 0) {
                mDifferentJishoWords = Utilities.getDifferentAsyncWords(mLocalMatchingWordsList, mJishoMatchingWordsList);
                if (mDifferentJishoWords.size()>0) {
                    updateFirebaseDbWithJishoWords(Utilities.getCommonWords(mDifferentJishoWords));
                    if (Utilities.wordsAreSimilar(mDifferentJishoWords.get(0), mInputQuery)) {
                        updateFirebaseDbWithJishoWords(mDifferentJishoWords.subList(0, 1)); //If the word was searched for then it is useful even if it's not defined as common
                    }
                }
            }

            Log.i(DEBUG_TAG, "Displaying Jisho merged words");
            displayMergedWordsToUser();

            if (getLoaderManager()!=null) getLoaderManager().destroyLoader(JISHO_WEB_SEARCH_LOADER);

        }
        else if (loader.getId() == JMDICTFR_WEB_SEARCH_LOADER && !mAlreadyLoadedJMDictFRResults) {
            List<Word> loaderResultWordsList = (List<Word>) data;
            mAlreadyLoadedJMDictFRResults = true;

            List<Word> JMDictFRWords = Utilities.cleanUpProblematicWordsFromJisho(loaderResultWordsList);

            if (JMDictFRWords.size() > 0) {
                mMergedMatchingWordsList = Utilities.getMergedWordsList(mLocalMatchingWordsList, JMDictFRWords, "FR");
                mMergedMatchingWordsList = sortWordsAccordingToRanking(mMergedMatchingWordsList);

                List<Word> differentWords = Utilities.getDifferentAsyncWords(mLocalMatchingWordsList, JMDictFRWords);

                if (differentWords.size() > 0) {
                    updateFirebaseDbWithJishoWords(differentWords);
                }
            }
            if (getLoaderManager()!=null) getLoaderManager().destroyLoader(JMDICTFR_WEB_SEARCH_LOADER);
        }
        else if (loader.getId() == VERB_SEARCH_LOADER && !mAlreadyLoadedVerbs && data!=null) {
            mAlreadyLoadedVerbs = true;
            Object[] dataElements = (Object[]) data;
            List<Verb> mMatchingVerbs = (List<Verb>) dataElements[0];
            mMatchingWordsFromVerbs = (List<Word>) dataElements[1];
            List<Object[]> mMatchingConjugationParameters = (List<Object[]>) dataElements[2];

            //Adapting the words list to include information used for proper display in the results list
            for (int i = 0; i < mMatchingWordsFromVerbs.size(); i++) {
                Word word = mMatchingWordsFromVerbs.get(i);
                String matchingConjugation = (String) mMatchingConjugationParameters.get(i)[VerbSearchAsyncTaskLoader.MATCHING_CONJUGATION];
                word.setIsLocal(true);
                word.setExtraKeywordsEN(word.getExtraKeywordsEN() + ", " + matchingConjugation);
            }

            Log.i(DEBUG_TAG, "Displaying Verb merged words");
            displayMergedWordsToUser();

            if (getLoaderManager()!=null) getLoaderManager().destroyLoader(VERB_SEARCH_LOADER);
        }
    }
    @Override public void onLoaderReset(@NonNull Loader<Object> loader) {}


	//Functionality methods
    private void getExtras() {
        if (getArguments()!=null) {
            mInputQuery = getArguments().getString(getString(R.string.user_query_word));
            mVerbLatinConjDatabase = (List<String[]>) getArguments().getSerializable(getString(R.string.latin_conj_database));
            mVerbKanjiConjDatabase = (List<String[]>) getArguments().getSerializable(getString(R.string.kanji_conj_database));
        }
    }
    private void initializeParameters() {

        mFirebaseDao = new FirebaseDao(this);

        mLocalMatchingWordsList = new ArrayList<>();
        mMergedMatchingWordsList = new ArrayList<>();

        mAlreadyLoadedRoomResults = false;
        mAlreadyLoadedJishoResults = false;
        mAlreadyLoadedJMDictFRResults = false;
        mAlreadyLoadedJMDictESResults = false;

        mConjugationTitles = Utilities.getConjugationTitles(mVerbLatinConjDatabase, getContext());
    }
    private void initializeViews(View rootView) {
        mBinding = ButterKnife.bind(this, rootView);

        mDictionaryRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        mDictionaryRecyclerView.setNestedScrollingEnabled(true);
        mDictionaryRecyclerViewAdapter = new DictionaryRecyclerViewAdapter(getContext(), this, null, mInputQuery, LocaleHelper.getLanguage(getContext()));
        mDictionaryRecyclerView.setAdapter(mDictionaryRecyclerViewAdapter);
    }
    private void getQuerySearchResults() {

        if (getContext()==null || getActivity()==null) return;

        mAlreadyDisplayedResults = false;
        mLocalMatchingWordsList = new ArrayList<>();
        mJishoMatchingWordsList = new ArrayList<>();
        mMergedMatchingWordsList = new ArrayList<>();
        mDifferentJishoWords = new ArrayList<>();
        mMergedMatchingWordsList = new ArrayList<>();
        mMatchingWordsFromVerbs = new ArrayList<>();
        if (!TextUtils.isEmpty(mInputQuery)) {

            showLoadingIndicator();

            mDictionaryRecyclerViewAdapter.setShowSources(Utilities.getPreferenceShowSources(getActivity()));

            findMatchingWordsInRoomDb();
            if (Utilities.getPreferenceShowConjResults(getActivity())) {
                startReverseConjSearchForMatchingVerbs();
            }
            if (Utilities.getPreferenceShowOnlineResults(getActivity())) {
                startSearchingForWordsInJisho();
                //startSearchingForWordsInJMDictFR();
                //startSearchingForWordsInJMDictES();
            }

            //Preventing computation/connectivity delays from freezing the UI thread
            mOverrideDisplayConditions = false;
            new Handler().postDelayed(new Runnable() {
                @Override public void run() {
                    mOverrideDisplayConditions = true;
                    Log.i(DEBUG_TAG, "Displaying merged words at WORD_RESULTS_MAX_RESPONSE_DELAY");
                    if (!mAlreadyDisplayedResults) displayMergedWordsToUser();
                }
            }, WORD_RESULTS_MAX_RESPONSE_DELAY);
        }
        else showEmptySearchResults();
    }
    private void findMatchingWordsInRoomDb() {
        if (getActivity()!=null) {
            LoaderManager loaderManager = getActivity().getSupportLoaderManager();
            Loader<String> roomDbSearchLoader = loaderManager.getLoader(ROOM_DB_SEARCH_LOADER);
            Bundle bundle = new Bundle();
            bundle.putString(getString(R.string.saved_input_query), mInputQuery);
            if (roomDbSearchLoader == null) loaderManager.initLoader(ROOM_DB_SEARCH_LOADER, bundle, this);
            else loaderManager.restartLoader(ROOM_DB_SEARCH_LOADER, bundle, this);
        }
    }
    private void startReverseConjSearchForMatchingVerbs() {
        if (getActivity()!=null) {
            LoaderManager loaderManager = getActivity().getSupportLoaderManager();
            Loader<String> verbSearchLoader = loaderManager.getLoader(VERB_SEARCH_LOADER);
            Bundle bundle = new Bundle();
            bundle.putString(getString(R.string.saved_input_query), mInputQuery);
            if (verbSearchLoader == null) loaderManager.initLoader(VERB_SEARCH_LOADER, bundle, this);
            else loaderManager.restartLoader(VERB_SEARCH_LOADER, bundle, this);
        }
    }
    private void showEmptySearchResults() {
        displayResults(new ArrayList<Word>());
    }
    private void displayMergedWordsToUser() {

        if (getContext()==null || getActivity()==null) return;

        boolean showOnlineResults = Utilities.getPreferenceShowOnlineResults(getActivity());
        boolean showConjResults = Utilities.getPreferenceShowConjResults(getActivity());
        boolean waitForOnlineResults = Utilities.getPreferenceWaitForOnlineResults(getActivity());
        boolean waitForConjResults = Utilities.getPreferenceWaitForConjResults(getActivity());

        if (!showOnlineResults) mJishoMatchingWordsList = new ArrayList<>();
        if (!showConjResults) mMatchingWordsFromVerbs = new ArrayList<>();

        if (mAlreadyLoadedRoomResults &&
                (       showOnlineResults && showConjResults &&
                                (!waitForOnlineResults && !waitForConjResults)
                                || (!waitForOnlineResults && waitForConjResults && mAlreadyLoadedVerbs)
                                || (waitForOnlineResults && !waitForConjResults && mAlreadyLoadedJishoResults)
                                || (waitForOnlineResults && waitForConjResults && mAlreadyLoadedJishoResults && mAlreadyLoadedVerbs)
                ) || (  showOnlineResults && !showConjResults && (!waitForOnlineResults || mAlreadyLoadedJishoResults)
                ) || (  !showOnlineResults && showConjResults && (!waitForConjResults || mAlreadyLoadedVerbs)
                ) || (  !showOnlineResults && !showConjResults)
            || mOverrideDisplayConditions) {

            Log.i(DEBUG_TAG, "Display successful");
            mAlreadyDisplayedResults = true;
            hideLoadingIndicator();

            //Getting the word lists
            mMergedMatchingWordsList = Utilities.getMergedWordsList(mLocalMatchingWordsList, mJishoMatchingWordsList, "");
            mMergedMatchingWordsList = Utilities.getMergedWordsList(mMergedMatchingWordsList, mMatchingWordsFromVerbs, "");
            mMergedMatchingWordsList = sortWordsAccordingToRanking(mMergedMatchingWordsList);

            String text = getString(R.string.found) + " "
                    + Integer.toString(mLocalMatchingWordsList.size())
                    + " "
                    + ((mLocalMatchingWordsList.size()==1)? getString(R.string.local_result) : getString(R.string.local_results));

            if (showOnlineResults && mAlreadyLoadedJishoResults) {
                if (showConjResults) text += ", ";
                else text += " " + getString(R.string.and) + " ";

                switch (mDifferentJishoWords.size()) {
                    case 0:
                        text += getString(R.string.no_new_online_results);
                        break;
                    case 1:
                        text += getString(R.string.one_new_or_fuller_online_result);
                        break;
                    default:
                        text += Integer.toString(mDifferentJishoWords.size()) + " " + getString(R.string.new_or_fuller_online_results);
                        break;
                }
            }

            if (showConjResults && mAlreadyLoadedVerbs) {
                if (showOnlineResults && mAlreadyLoadedJishoResults) text += ", "+getString(R.string.and)+" ";
                else if (showOnlineResults) text += ", ";
                else text += " "+getString(R.string.and)+" ";

                text += getString(R.string.including)+" ";

                switch (mMatchingWordsFromVerbs.size()) {
                    case 0:
                        text += getString(R.string.no_verb);
                        break;
                    case 1:
                        text += getString(R.string.one_verb);
                        break;
                    default:
                        text += Integer.toString(mMatchingWordsFromVerbs.size()) + " " + getString(R.string.verbs);
                        break;
                }
                text += " " + getString(R.string.with_conjugations_matching_the_search_word);
            }

            text += ".";

            displayResults(mMergedMatchingWordsList);

            int maxIndex = mLocalMatchingWordsList.size()> MAX_NUM_WORDS_TO_SHARE ? MAX_NUM_WORDS_TO_SHARE : mLocalMatchingWordsList.size();
            dictionaryFragmentOperationsHandler.onFinalMatchingWordsFound(mMergedMatchingWordsList.subList(0,maxIndex));

            if (mLocalMatchingWordsList.size()==0) {
                //performConjSearch();
            }

            if (Utilities.getPreferenceShowInfoBoxesOnSearch(getActivity())) Toast.makeText(getContext(), text, Toast.LENGTH_LONG).show();

        }

    }
    private void startSearchingForWordsInJisho() {

        if (!TextUtils.isEmpty(mInputQuery)) {

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
    private void startSearchingForWordsInJMDictFR() {

        if (!TextUtils.isEmpty(mInputQuery)) {

            if (getActivity() != null) {

                Bundle queryBundle = new Bundle();
                queryBundle.putString(getString(R.string.saved_input_query), mInputQuery);

                LoaderManager loaderManager = getActivity().getSupportLoaderManager();
                Loader<String> webSearchLoader = loaderManager.getLoader(JMDICTFR_WEB_SEARCH_LOADER);
                if (webSearchLoader == null) loaderManager.initLoader(JMDICTFR_WEB_SEARCH_LOADER, queryBundle, this);
                else loaderManager.restartLoader(JMDICTFR_WEB_SEARCH_LOADER, queryBundle, this);
            }
        }

    }
    private void startSearchingForWordsInJMDictES() {

        if (!TextUtils.isEmpty(mInputQuery)) {

            if (getActivity() != null) {

                Bundle queryBundle = new Bundle();
                queryBundle.putString(getString(R.string.saved_input_query), mInputQuery);

                LoaderManager loaderManager = getActivity().getSupportLoaderManager();
                Loader<String> webSearchLoader = loaderManager.getLoader(JMDICTES_WEB_SEARCH_LOADER);
                if (webSearchLoader == null) loaderManager.initLoader(JMDICTES_WEB_SEARCH_LOADER, queryBundle, this);
                else loaderManager.restartLoader(JMDICTES_WEB_SEARCH_LOADER, queryBundle, this);
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

    }
    private List<Word> sortWordsAccordingToRanking(List<Word> wordsList) {

        if (wordsList == null || wordsList.size()==0) return new ArrayList<>();

        List<long[]> matchingWordIndexesAndLengths = new ArrayList<>();

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
        int inputTextType = ConvertFragment.getTextType(inputQuery);
        if (inputTextType == GlobalConstants.TYPE_HIRAGANA || inputTextType == GlobalConstants.TYPE_KATAKANA) {
            List<String> translationList = ConvertFragment.getLatinHiraganaKatakana(inputQuery.replace(" ", ""));
            inputQuery = translationList.get(0);
        }
        //endregion

        for (int i = 0; i < wordsList.size(); i++) {

            Word currentWord = wordsList.get(i);
            if (currentWord==null) continue;

            String language = LocaleHelper.getLanguage(getContext());
            int ranking = Utilities.getRankingFromWordAttributes(currentWord, inputQuery, queryWordWithoutTo, queryIsVerbWithTo, language);

            long[] currentMatchingWordIndexAndLength = new long[3];
            currentMatchingWordIndexAndLength[0] = i;
            currentMatchingWordIndexAndLength[1] = ranking;

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
        mFirebaseDao.updateObjectsOrCreateThemInFirebaseDb(wordsList);
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
        void onFinalMatchingWordsFound(List<Word> matchingWords);
    }
    public void setQuery(String query) {
        mInputQuery = query;
        mAlreadyLoadedRoomResults = false;
        mAlreadyLoadedJishoResults = false;
        mAlreadyLoadedJMDictFRResults = false;
        mAlreadyLoadedJMDictESResults = false;
        getQuerySearchResults();
    }

    //Communication with Firebase DAO
    @Override public void onWordsListFound(List<Word> wordsList) {

    }
}