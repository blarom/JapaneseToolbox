package com.japanesetoolboxapp.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.japanesetoolboxapp.R;
import com.japanesetoolboxapp.data.Word;
import com.japanesetoolboxapp.resources.GlobalConstants;
import com.japanesetoolboxapp.resources.LocaleHelper;
import com.japanesetoolboxapp.resources.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

//TODO: database upgrade
//// Test app ranking algorithm using the following words: eat, car

//TODO: features
////TODO: add wildcard characters to local searches
////TODO: add kanji character zoom in
////TODO: allow user to enter verb in gerund form (ing) and still find its
////TODO Show the adjective conjugations (it will also explain to the user why certain adjectives appear in the list, based on their conjugations)
////TODO Add filtering functionality: if more than one word is entered, the results will be limited to those that include all words.
////TODO make the DICT return kanjis having the same romaji value as that of the entered kanji, similar to the way jisho.org works

//TODO: bugs


public class MainActivity extends BaseActivity implements
        InputQueryFragment.InputQueryOperationsHandler,
        DictionaryFragment.DictionaryFragmentOperationsHandler,
        ConjugatorFragment.ConjugatorFragmentOperationsHandler,
        SearchByRadicalFragment.SearchByRadicalFragmentOperationsHandler,
        SharedPreferences.OnSharedPreferenceChangeListener,
        LoaderManager.LoaderCallbacks<Object> {


    //region Parameters
    private static final int SMALL_DATABASE_LOADER = 9512;
    @BindView(R.id.second_fragment_placeholder) FrameLayout mSecondFragmentPlaceholder;
    private String mSecondFragmentFlag;
    private InputQueryFragment mInputQueryFragment;
    private List<String[]> VerbLatinConjDatabase;
    private List<String[]> VerbKanjiConjDatabase;
    private List<String[]> SimilarsDatabase;
    private List<String[]> RadicalsOnlyDatabase;
    private Typeface CJK_typeface;

    private boolean mShowOnlineResults;
    private boolean mWaitForOnlineResults;
    private boolean mShowConjResults;
    private boolean mWaitForConjResults;
    private boolean mShowSources;
    private boolean mShowInfoBoxesOnSearch;
    private boolean mShowKanjiStructureInfo;
    private String mChosenSpeechToTextLanguage;
    private String mChosenTextToSpeechLanguage;
    private String mChosenOCRLanguage;
    private float mOcrImageDefaultContrast;
    private float mOcrImageDefaultBrightness;
    private float mOcrImageDefaultSaturation;
    private int mQueryHistorySize;
    private FragmentManager mFragmentManager;
    private Bundle mSavedInstanceState;
    private Unbinder mBinding;
    private DictionaryFragment mDictionaryFragment;
    private ConjugatorFragment mConjugatorFragment;
    private ConvertFragment mConvertFragment;
    private SearchByRadicalFragment mSearchByRadicalFragment;
    private DecomposeKanjiFragment mDecomposeKanjiFragment;
    private String mSecondFragmentCurrentlyDisplayed;
    private boolean mAlreadyLoadedSmallDatabases;
    private boolean mAllowButtonOperations;
    private List<Word> mLocalMatchingWords;
    private String mInputQuery;
    private List<String> mQueryHistory;
    private String mLanguageCode;
    //endregion


    //Lifecycle methods
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSavedInstanceState = savedInstanceState;

        Log.i("Diagnosis Time", "Started MainActivity.");
        initializeParameters();
        setupSharedPreferences();
        startLoadingDatabasesInBackground();

        setFragments();

    }
    @Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Set the Requested Fragment if it was saved from a previous instance
        if (savedInstanceState != null) {

            String savedRequestedFragment = savedInstanceState.getString(getString(R.string.requested_second_fragment));
            if (savedRequestedFragment != null) {
                mSecondFragmentFlag = savedRequestedFragment;
            }
        }
        mAllowButtonOperations = true;
    }
    @Override protected void onStart() {
        super.onStart();
        Intent restartIntent = this.getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(this.getBaseContext().getPackageName());

    }
    @Override protected void onResume() {
        super.onResume();
        mAllowButtonOperations = true;
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(getString(R.string.requested_second_fragment), mSecondFragmentFlag);
        outState.putString(getString(R.string.displayed_second_fragment), mSecondFragmentCurrentlyDisplayed);
//        if (mDictionaryFragment!=null && mDictionaryFragment.isAdded()) getSupportFragmentManager().putFragment(outState, getString(R.string.dict_fragment), mDictionaryFragment);
//        if (mConjugatorFragment!=null && mConjugatorFragment.isAdded()) getSupportFragmentManager().putFragment(outState, getString(R.string.conj_fragment), mConjugatorFragment);
//        if (mConvertFragment!=null && mConvertFragment.isAdded()) getSupportFragmentManager().putFragment(outState, getString(R.string.conv_fragment), mConvertFragment);
//        if (mSearchByRadicalFragment!=null && mSearchByRadicalFragment.isAdded()) getSupportFragmentManager().putFragment(outState, getString(R.string.srad_fragment), mSearchByRadicalFragment);
//        if (mDecomposeKanjiFragment!=null && mDecomposeKanjiFragment.isAdded()) getSupportFragmentManager().putFragment(outState, getString(R.string.dcmp_fragment), mDecomposeKanjiFragment);

        mAllowButtonOperations = false;
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        try {
            Utilities.trimCache(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        mBinding.unbind();
    }
    @Override public void onBackPressed() {

        showExitAppDialog();

        //super.onBackPressed();
//        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
//            getSupportFragmentManager().popBackStack();
//        } else {
//            super.onBackPressed();
//        }
    }
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int itemThatWasClickedId = item.getItemId();

        switch (itemThatWasClickedId) {
            case R.id.action_settings:
                Intent startSettingsActivity = new Intent(this, SettingsActivity.class);
                startSettingsActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(startSettingsActivity);
                return true;
            case R.id.action_about:
                Intent startAboutActivity = new Intent(this, AboutActivity.class);
                startAboutActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(startAboutActivity);
                return true;
            case R.id.action_clear_history:
                if (mInputQueryFragment!=null) mInputQueryFragment.clearHistory();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    //Preference methods
    @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_complete_local_with_online_search_key))) {
            setShowOnlineResults(sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_complete_local_with_online_search_default)));
        }
        else if (key.equals(getString(R.string.pref_wait_for_online_results_key))) {
            setWaitForOnlineResults(sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_wait_for_online_results_default)));
        }
        else if (key.equals(getString(R.string.pref_complete_with_conj_search_key))) {
            setShowConjResults(sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_complete_with_conj_search_default)));
        }
        else if (key.equals(getString(R.string.pref_wait_for_conj_results_key))) {
            setWaitForConjResults(sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_wait_for_conj_results_default)));
        }
        else if (key.equals(getString(R.string.pref_show_sources_key))) {
            setShowSources(sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_show_sources_default)));
        }
        else if (key.equals(getString(R.string.pref_show_info_boxes_on_search_key))) {
            setShowInfoBoxesOnSearch(sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_show_info_boxes_on_search_default)));
        }
        else if (key.equals(getString(R.string.pref_show_decomp_structure_info_key))) {
            setShowKanjiStructureInfo(sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_show_decomp_structure_info_default)));
        }
        else if (key.equals(getString(R.string.pref_query_history_size_key))) {
            mQueryHistorySize = Utilities.getPreferenceQueryHistorySize(sharedPreferences, getApplicationContext());
            updateQueryHistorySize();
        }
        else if (key.equals(getString(R.string.pref_preferred_STT_language_key))) {
            setSpeechToTextLanguage(sharedPreferences.getString(getString(R.string.pref_preferred_STT_language_key), getString(R.string.pref_language_value_japanese)));
        }
        else if (key.equals(getString(R.string.pref_preferred_TTS_language_key))) {
            setTextToSpeechLanguage(sharedPreferences.getString(getString(R.string.pref_preferred_TTS_language_key), getString(R.string.pref_language_value_japanese)));
        }
        else if (key.equals(getString(R.string.pref_preferred_OCR_language_key))) {
            setOCRLanguage(sharedPreferences.getString(getString(R.string.pref_preferred_OCR_language_key), getString(R.string.pref_language_value_japanese)));
        }
        else if (key.equals(getString(R.string.pref_OCR_image_saturation_key))) {
            mOcrImageDefaultSaturation = Utilities.loadOCRImageSaturationFromSharedPreferences(sharedPreferences, getApplicationContext());
        }
        else if (key.equals(getString(R.string.pref_OCR_image_contrast_key))) {
            mOcrImageDefaultContrast = Utilities.loadOCRImageContrastFromSharedPreferences(sharedPreferences, getApplicationContext());
        }
        else if (key.equals(getString(R.string.pref_OCR_image_brightness_key))) {
            mOcrImageDefaultBrightness = Utilities.loadOCRImageBrightnessFromSharedPreferences(sharedPreferences, getApplicationContext());
        }
    }
    private void setupSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setShowOnlineResults(sharedPreferences.getBoolean(getString(R.string.pref_complete_local_with_online_search_key),
                getResources().getBoolean(R.bool.pref_complete_local_with_online_search_default)));
        setWaitForOnlineResults(sharedPreferences.getBoolean(getString(R.string.pref_wait_for_online_results_key),
                getResources().getBoolean(R.bool.pref_wait_for_online_results_default)));
        setShowConjResults(sharedPreferences.getBoolean(getString(R.string.pref_complete_with_conj_search_key),
                getResources().getBoolean(R.bool.pref_complete_with_conj_search_default)));
        setWaitForConjResults(sharedPreferences.getBoolean(getString(R.string.pref_wait_for_conj_results_key),
                getResources().getBoolean(R.bool.pref_wait_for_conj_results_default)));
        setShowSources(sharedPreferences.getBoolean(getString(R.string.pref_show_sources_key),
                getResources().getBoolean(R.bool.pref_show_sources_default)));
        mQueryHistorySize = Utilities.getPreferenceQueryHistorySize(sharedPreferences, getApplicationContext());
        setSpeechToTextLanguage(sharedPreferences.getString(getString(R.string.pref_preferred_STT_language_key), getString(R.string.pref_language_value_japanese)));
        setTextToSpeechLanguage(sharedPreferences.getString(getString(R.string.pref_preferred_TTS_language_key), getString(R.string.pref_language_value_japanese)));
        setOCRLanguage(sharedPreferences.getString(getString(R.string.pref_preferred_OCR_language_key), getString(R.string.pref_language_value_japanese)));
        mOcrImageDefaultContrast = Utilities.loadOCRImageContrastFromSharedPreferences(sharedPreferences, getApplicationContext());
        mOcrImageDefaultSaturation = Utilities.loadOCRImageSaturationFromSharedPreferences(sharedPreferences, getApplicationContext());
        mOcrImageDefaultBrightness = Utilities.loadOCRImageBrightnessFromSharedPreferences(sharedPreferences, getApplicationContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }
    private void setShowOnlineResults(boolean state) {
        mShowOnlineResults = state;
    }
    private void setWaitForOnlineResults(boolean state) {
        mWaitForOnlineResults = state;
    }
    private void setShowConjResults(boolean state) {
        mShowConjResults = state;
    }
    private void setWaitForConjResults(boolean state) {
        mWaitForConjResults = state;
    }
    private void setShowSources(boolean state) {
        mShowSources = state;
    }
    private void setShowInfoBoxesOnSearch(boolean state) {
        mShowInfoBoxesOnSearch = state;
    }
    private void setShowKanjiStructureInfo(boolean state) {
        mShowKanjiStructureInfo = state;
    }
    private void setSpeechToTextLanguage(String language) {
        if (language.equals(getResources().getString(R.string.pref_language_value_japanese))) {
            mChosenSpeechToTextLanguage = getResources().getString(R.string.languageLocaleJapanese);
        }
        else if (language.equals(getResources().getString(R.string.pref_language_value_english))) {
            mChosenSpeechToTextLanguage = getResources().getString(R.string.languageLocaleEnglishUS);
        }
        else if (language.equals(getResources().getString(R.string.pref_language_value_french))) {
            mChosenSpeechToTextLanguage = getResources().getString(R.string.languageLocaleFrench);
        }
        else if (language.equals(getResources().getString(R.string.pref_language_value_spanish))) {
            mChosenSpeechToTextLanguage = getResources().getString(R.string.languageLocaleSpanish);
        }
        if (mInputQueryFragment!=null) mInputQueryFragment.setSTTLanguage(mChosenSpeechToTextLanguage);
    }
    private void setTextToSpeechLanguage(String language) {
        if (language.equals(getResources().getString(R.string.pref_language_value_japanese))) {
            mChosenTextToSpeechLanguage = getResources().getString(R.string.languageLocaleJapanese);
        }
        else if (language.equals(getResources().getString(R.string.pref_language_value_english))) {
            mChosenTextToSpeechLanguage = getResources().getString(R.string.languageLocaleEnglishUS);
        }
        else if (language.equals(getResources().getString(R.string.pref_language_value_french))) {
            mChosenTextToSpeechLanguage = getResources().getString(R.string.languageLocaleFrench);
        }
        else if (language.equals(getResources().getString(R.string.pref_language_value_spanish))) {
            mChosenTextToSpeechLanguage = getResources().getString(R.string.languageLocaleSpanish);
        }
    }
    private void setOCRLanguage(String language) {
        if (language.equals(getResources().getString(R.string.pref_language_value_japanese))) {
            mChosenOCRLanguage = getResources().getString(R.string.languageLocaleJapanese);
        }
        else if (language.equals(getResources().getString(R.string.pref_language_value_english))) {
            mChosenOCRLanguage = getResources().getString(R.string.languageLocaleEnglishUS);
        }
    }


    //Functionality methods
    private void initializeParameters() {

        mBinding =  ButterKnife.bind(this);
        mSecondFragmentFlag = "start";
        mAllowButtonOperations = true;

        //Code allowing to bypass strict mode
        //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        //StrictMode.setThreadPolicy(policy);

        // Remove the software keyboard if the EditText is not in focus
        findViewById(android.R.id.content).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Utilities.hideSoftKeyboard(MainActivity.this);
                v.performClick();
                return false;
            }
        });

        //Set the typeface for Chinese/Japanese fonts
        CJK_typeface = Typeface.DEFAULT;
        //CJK_typeface = Typeface.createFromAsset(getAssets(), "fonts/DroidSansFallback.ttf");
        //CJK_typeface = Typeface.createFromAsset(getAssets(), "fonts/DroidSansJapanese.ttf");
        //see https://stackoverflow.com/questions/11786553/changing-the-android-typeface-doesnt-work

        mLanguageCode = LocaleHelper.getLanguage(getApplicationContext());
    }
    private void setFragments() {

        // Get the fragment manager
        mFragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

        // Load the Fragments depending on the device orientation
        Configuration config = getResources().getConfiguration();
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (mSavedInstanceState == null) {
                mInputQueryFragment = new InputQueryFragment();
                Bundle bundle = new Bundle();
                bundle.putString(getString(R.string.pref_preferred_STT_language), mChosenSpeechToTextLanguage);
                fragmentTransaction.add(R.id.input_query_placeholder, mInputQueryFragment);
            }
        } else {
            if (mSavedInstanceState == null) {
                mInputQueryFragment = new InputQueryFragment();
                Bundle bundle = new Bundle();
                bundle.putString(getString(R.string.pref_preferred_STT_language), mChosenSpeechToTextLanguage);
                fragmentTransaction.add(R.id.input_query_placeholder, mInputQueryFragment);
            }
        }

        fragmentTransaction.commit();
    }
    private void updateInputQuery(String word, boolean keepPreviousText) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (mInputQueryFragment!=null) {
            if (keepPreviousText) mInputQueryFragment.setAppendedQuery(word);
            else mInputQueryFragment.setQuery(word);
        }

        fragmentTransaction.commit();
    }
    private void clearBackstack() {

        mFragmentManager = getSupportFragmentManager();
        if (mFragmentManager!=null && mFragmentManager.getBackStackEntryCount()>0) {
            FragmentManager.BackStackEntry entry = getSupportFragmentManager().getBackStackEntryAt(0);
            getSupportFragmentManager().popBackStack(entry.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
            getSupportFragmentManager().executePendingTransactions();
        }
    }
    private void startLoadingDatabasesInBackground() {
        if (!mAlreadyLoadedSmallDatabases) {
            LoaderManager loaderManager = getSupportLoaderManager();
            Loader<String> roomDbSearchLoader = loaderManager.getLoader(SMALL_DATABASE_LOADER);
            if (roomDbSearchLoader == null) loaderManager.initLoader(SMALL_DATABASE_LOADER, null, this);
            else loaderManager.restartLoader(SMALL_DATABASE_LOADER, null, this);
        }
    }
    private void cleanSavedData() {
        mLocalMatchingWords = null;
        mDictionaryFragment = null;
        mConjugatorFragment = null;
        mConvertFragment = null;
        mSearchByRadicalFragment = null;
        mDecomposeKanjiFragment = null;
    }
    private void updateInputQueryWithDefinition(List<Word> matchingWords, boolean fromConjSearch) {
        if (mInputQueryFragment==null) return;

        String romaji = "";
        String meaning = "";
        if ( matchingWords == null || matchingWords.size() == 0) {
            mInputQueryFragment.updateQueryDefinitionInHistory(romaji, "");
        }
        else if (fromConjSearch) {
            mInputQueryFragment.updateQueryDefinitionInHistory(
                    matchingWords.get(0).getRomaji(),
                    Utilities.getMeaningsExtract(matchingWords.get(0).getMeaningsEN(), 2)
            );
        }
        else {
            //Get the first definition matching the romaji / kanji
            for (Word word : matchingWords) {
                if (word.getRomaji().equals(mInputQuery) || word.getKanji().equals(mInputQuery)
                        || Utilities.getRomajiNoSpacesForSpecialPartsOfSpeech(word.getRomaji())
                            .equals(ConvertFragment.getLatinHiraganaKatakana(mInputQuery).get(GlobalConstants.TYPE_LATIN)) ) {
                    romaji = word.getRomaji();
                    meaning = getCurrentMeanings(word);
                    break;
                }
            }
            //If no definition was found, get the first definition matching the altSpellings
            if (romaji.equals("")) {
                for (Word word : matchingWords) {
                    List<String> altSpellings = (word.getAltSpellings() != null) ? Arrays.asList(word.getAltSpellings().split(",")) : new ArrayList<String>();
                    if (altSpellings.contains(mInputQuery)) {
                        romaji = word.getRomaji();
                        meaning = getCurrentMeanings(word);
                        break;
                    }
                }
            }
            //If no definition was found, get the first definition that includes the input query as a word in the meanings
            if (romaji.equals("")) {
                for (Word word : matchingWords) {
                    List<String> wordsInMeanings = new ArrayList<>();
                    for (Word.Meaning wordMeaning : word.getMeaningsEN()) {
                        wordsInMeanings.add(wordMeaning.getMeaning()
                                .replace(", ", ";")
                                .replace("(", "")
                                .replace(")", ""));
                    }
                    String wordsInMeaningsAsString = TextUtils.join(";", wordsInMeanings);
                    wordsInMeanings = Arrays.asList(wordsInMeaningsAsString.split(";"));
                    for (int i=0; i<wordsInMeanings.size(); i++) {
                        wordsInMeanings.set(i, wordsInMeanings.get(i).trim());
                    }
                    if (wordsInMeanings.contains(mInputQuery)) {
                        romaji = word.getRomaji();
                        meaning = getCurrentMeanings(word);
                        break;
                    }
                }
            }
            mInputQueryFragment.updateQueryDefinitionInHistory(romaji, meaning);
        }
    }
    private String getCurrentMeanings(Word word) {

        List<Word.Meaning> meanings;
        switch (mLanguageCode) {
            case "en":
                meanings = word.getMeaningsEN();
                break;
            case "fr":
                meanings = word.getMeaningsFR();
                break;
            case "es":
                meanings = word.getMeaningsES();
                break;
            default: meanings = word.getMeaningsEN();
        }

        if (meanings == null || meanings.size() == 0) {
            meanings = word.getMeaningsEN();
        }

        return Utilities.getMeaningsExtract(meanings, GlobalConstants.BALANCE_POINT_HISTORY_DISPLAY);
    }
    private void showExitAppDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setMessage(getString(R.string.sure_you_want_to_exit));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        dialog.dismiss();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }
    private void updateQueryHistorySize() {

        //Getting the history
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preferences_query_history_list), Context.MODE_PRIVATE);
        String queryHistoryAsString = sharedPref.getString(getString(R.string.preferences_query_history_list), "");
        if (queryHistoryAsString!=null && !queryHistoryAsString.equals(""))
            mQueryHistory = new ArrayList<>(Arrays.asList(queryHistoryAsString.split(GlobalConstants.QUERY_HISTORY_ELEMENTS_DELIMITER)));
        else mQueryHistory = new ArrayList<>();

        //Updating its size
        if (mQueryHistory.size() > mQueryHistorySize) mQueryHistory = mQueryHistory.subList(0, mQueryHistorySize);

        //Saving the hstory
        queryHistoryAsString = TextUtils.join(GlobalConstants.QUERY_HISTORY_ELEMENTS_DELIMITER, mQueryHistory);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.preferences_query_history_list), queryHistoryAsString);
        editor.apply();

        if (mInputQueryFragment!=null) mInputQueryFragment.updateQueryHistoryList(mQueryHistory, mQueryHistorySize);
    }


    //Asynchronous methods
    @NonNull @Override public Loader<Object> onCreateLoader(int id, final Bundle args) {

        if (id == SMALL_DATABASE_LOADER) {
            SmallDatabasesAsyncTaskLoader DbLoader = new SmallDatabasesAsyncTaskLoader(this, mAlreadyLoadedSmallDatabases);
            return DbLoader;
        }
        else return new SmallDatabasesAsyncTaskLoader(this, true);
    }
    @Override public void onLoadFinished(@NonNull Loader<Object> loader, Object data) {

        if (loader.getId() == SMALL_DATABASE_LOADER && !mAlreadyLoadedSmallDatabases && data!=null) {
            mAlreadyLoadedSmallDatabases = true;

            Object[] databases = (Object[]) data;

            SimilarsDatabase = new ArrayList((List<String[]>) databases[0]);
            VerbLatinConjDatabase = new ArrayList((List<String[]>) databases[1]);
            VerbKanjiConjDatabase = new ArrayList((List<String[]>) databases[2]);
            RadicalsOnlyDatabase = new ArrayList((List<String[]>) databases[3]);

            if (getLoaderManager()!=null) getLoaderManager().destroyLoader(SMALL_DATABASE_LOADER);
        }

    }
    @Override public void onLoaderReset(@NonNull Loader<Object> loader) {}
    private static class SmallDatabasesAsyncTaskLoader extends AsyncTaskLoader<Object> {

        private final boolean mAlreadyLoadedVerbs;

        SmallDatabasesAsyncTaskLoader(Context context, boolean mAlreadyLoadedVerbs) {
            super(context);
            this.mAlreadyLoadedVerbs = mAlreadyLoadedVerbs;
        }

        @Override
        protected void onStartLoading() {
            if (!mAlreadyLoadedVerbs) forceLoad();
        }

        @Override
        public Object loadInBackground() {

            //JapaneseToolboxRoomDatabase japaneseToolboxRoomDatabase = JapaneseToolboxRoomDatabase.getInstance(getContext()); //Required for Room
            List<String[]> SimilarsDatabase = Utilities.readCSVFile("LineSimilars - 3000 kanji.csv", getContext());
            List<String[]> VerbLatinConjDatabase = Utilities.readCSVFile("LineLatinConj - 3000 kanji.csv", getContext());
            List<String[]> VerbKanjiConjDatabase = Utilities.readCSVFile("LineKanjiConj - 3000 kanji.csv", getContext());
            List<String[]> RadicalsOnlyDatabase = Utilities.readCSVFile("LineRadicalsOnly - 3000 kanji.csv", getContext());

            Log.i("Diagnosis Time","Loaded All Small Databases.");
            return new Object[] {
                    SimilarsDatabase,
                    VerbLatinConjDatabase,
                    VerbKanjiConjDatabase,
                    RadicalsOnlyDatabase,
            };
        }

    }


    //Communication with other classes:

    //Communication with InputQueryFragment
    @Override public void onDictRequested(String query) {

        mInputQuery = query;

        if (!mAllowButtonOperations) return;
        if (SimilarsDatabase==null) {
            Toast.makeText(this, getString(R.string.please_wait_for_db_to_finish_loading), Toast.LENGTH_SHORT).show();
            return;
        }
        cleanSavedData();
        //clearBackstack();

        mSecondFragmentCurrentlyDisplayed = getString(R.string.dict_fragment);

        mSecondFragmentPlaceholder.setVisibility(View.VISIBLE);
        mSecondFragmentPlaceholder.bringToFront();

        query = Utilities.replaceInvalidKanjisWithValidOnes(query, SimilarsDatabase);

        mDictionaryFragment = new DictionaryFragment();
        Bundle bundle = new Bundle();
        bundle.putString(getString(R.string.user_query_word), query);
        bundle.putSerializable(getString(R.string.latin_conj_database), new ArrayList<>(VerbLatinConjDatabase));
        bundle.putSerializable(getString(R.string.kanji_conj_database), new ArrayList<>(VerbKanjiConjDatabase));
        mDictionaryFragment.setArguments(bundle);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.second_fragment_placeholder, mDictionaryFragment, getString(R.string.dict_fragment));

        //fragmentTransaction.addToBackStack(getString(R.string.dictonary_fragment_instance));
        fragmentTransaction.commit();
    }
    @Override public void onConjRequested(String query) {

        mInputQuery = query;

        if (!mAllowButtonOperations) return;
        cleanSavedData();
        //clearBackstack();
        if (SimilarsDatabase==null) {
            Toast.makeText(this, getString(R.string.please_wait_for_db_to_finish_loading), Toast.LENGTH_SHORT).show();
            return;
        }

        mSecondFragmentCurrentlyDisplayed = getString(R.string.conj_fragment);

        mSecondFragmentPlaceholder.setVisibility(View.VISIBLE);
        mSecondFragmentPlaceholder.bringToFront();

        mConjugatorFragment = new ConjugatorFragment();
        Bundle bundle = new Bundle();
        bundle.putString(getString(R.string.user_query_word), query);
        bundle.putSerializable(getString(R.string.latin_conj_database), new ArrayList<>(VerbLatinConjDatabase));
        bundle.putSerializable(getString(R.string.kanji_conj_database), new ArrayList<>(VerbKanjiConjDatabase));
        if (mLocalMatchingWords!=null) bundle.putParcelableArrayList(getString(R.string.words_list), new ArrayList<>(mLocalMatchingWords));
        mConjugatorFragment.setArguments(bundle);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.second_fragment_placeholder, mConjugatorFragment, getString(R.string.conj_fragment));

        //fragmentTransaction.addToBackStack(getString(R.string.conjugator_fragment_instance));
        fragmentTransaction.commit();
    }
    @Override public void onConvertRequested(String query) {

        if (!mAllowButtonOperations) return;

        cleanSavedData();

        mSecondFragmentCurrentlyDisplayed = getString(R.string.conv_fragment);

        mSecondFragmentPlaceholder.setVisibility(View.VISIBLE);
        mSecondFragmentPlaceholder.bringToFront();

        mConvertFragment = new ConvertFragment();
        Bundle bundle = new Bundle();
        bundle.putString(getString(R.string.user_query_word), query);
        mConvertFragment.setArguments(bundle);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.second_fragment_placeholder, mConvertFragment, getString(R.string.conj_fragment));

        //fragmentTransaction.addToBackStack(getString(R.string.convert_fragment_instance));
        fragmentTransaction.commit();
    }
    @Override public void onSearchByRadicalRequested(String query) {

        if (!mAllowButtonOperations) return;

        cleanSavedData();

        if (SimilarsDatabase==null) {
            Toast.makeText(this, getString(R.string.please_wait_for_db_to_finish_loading), Toast.LENGTH_SHORT).show();
            return;
        }

        mSecondFragmentCurrentlyDisplayed = getString(R.string.srad_fragment);

        mSecondFragmentPlaceholder.setVisibility(View.VISIBLE);
        mSecondFragmentPlaceholder.bringToFront();

        mSearchByRadicalFragment = new SearchByRadicalFragment();
        Bundle bundle = new Bundle();
        bundle.putString(getString(R.string.user_query_word), query);
        bundle.putSerializable(getString(R.string.rad_only_database), new ArrayList<>(RadicalsOnlyDatabase));
        bundle.putSerializable(getString(R.string.similars_database), new ArrayList<>(SimilarsDatabase));
        mSearchByRadicalFragment.setArguments(bundle);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.second_fragment_placeholder, mSearchByRadicalFragment, getString(R.string.srad_fragment));

        //fragmentTransaction.addToBackStack(getString(R.string.search_by_radical_fragment_instance));
        fragmentTransaction.commit();

    }
    @Override public void onDecomposeRequested(String query) {

        if (!mAllowButtonOperations) return;

        cleanSavedData();

        if (SimilarsDatabase==null) {
            Toast.makeText(this, getString(R.string.please_wait_for_db_to_finish_loading), Toast.LENGTH_SHORT).show();
            return;
        }

        query = Utilities.replaceInvalidKanjisWithValidOnes(query, SimilarsDatabase);

        mSecondFragmentCurrentlyDisplayed = getString(R.string.dcmp_fragment);

        mSecondFragmentPlaceholder.setVisibility(View.VISIBLE);
        mSecondFragmentPlaceholder.bringToFront();

        mDecomposeKanjiFragment = new DecomposeKanjiFragment();
        Bundle bundle = new Bundle();
        bundle.putString(getString(R.string.user_query_word), query);
        bundle.putSerializable(getString(R.string.rad_only_database), new ArrayList<>(RadicalsOnlyDatabase));

        mDecomposeKanjiFragment.setArguments(bundle);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.second_fragment_placeholder, mDecomposeKanjiFragment, getString(R.string.dcmp_fragment));

        //fragmentTransaction.addToBackStack(getString(R.string.decompose_fragment_instance));
        fragmentTransaction.commit();
    }

    //Communication with DictionaryFragment
    @Override public void onQueryTextUpdateFromDictRequested(String word) {
        updateInputQuery(word, false);
    }
    @Override public void onVerbConjugationFromDictRequested(String verb) {
        if (mInputQueryFragment!=null) mInputQueryFragment.setConjButtonSelected();
        onConjRequested(verb);
    }

    @Override public void onLocalMatchingWordsFound(List<Word> matchingWords) {
        mLocalMatchingWords = matchingWords;
    }
    @Override public void onFinalMatchingWordsFound(List<Word> matchingWords) {
        updateInputQueryWithDefinition(matchingWords, false);
    }
    @Override public void onMatchingVerbsFound(List<Word> matchingVerbsAsWords) {
        updateInputQueryWithDefinition(matchingVerbsAsWords, true);
    }

    //Communication with SearchByRadicalFragment
    @Override public void onQueryTextUpdateFromSearchByRadicalRequested(String word) {
        updateInputQuery(word, true);
    }
}

