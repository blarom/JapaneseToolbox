package com.japanesetoolboxapp;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.japanesetoolboxapp.data.DatabaseUtilities;
import com.japanesetoolboxapp.data.Word;
import com.japanesetoolboxapp.data.WordsRoomDatabase;
import com.japanesetoolboxapp.utiities.*;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

//TODO: database upgrade
////TODO make the LOCAL room db update itself with the list<Words> if the app has been reinstalled
////TODO create objects for the Latin and Kanji index tables for faster loading
////TODO adapt the code to use the LOCAL room db instead of the local List<String> objects, including index calls
////TODO create a VBA function that updates the list of words in the excel from firebase
////TODO query the room database to get results from the common words
////TODO merge the results of the local database to the results
////TODO display to user

//TODO: features
////TODO when joining online results, compare verb[space]suru with verb[no space]suru, and show verb[space]suru to user
////TODO make ranking in in dict put special concat hits at end of list or invalidate them
////TODO add root as keyword for suru verbs
////TODO implement short description for long definitions
////TODO indicate if word is common in search results
////TODO indicate source as local/jisho in search results
////TODO Show the adjective conjugations (it will also explain to the user why certain adjectives appear in the list, based on their conjugations)
////TODO If the user enter "一", make sure the app gives all results with a bar
////TODO Give the user the option to reduce/increase the number of search results (useful when there are many results)
////TODO Add the option of selecting which databases to search in (in order to limit the number of results)
////TODO Allow for different kanji versions of a verb by comparing the input verb's kanji to a list associated with every verb,
////     then applying the reverse conjugation by searching using the database's kanji instead of the user's input, and also showing the other possible kanji dict. forms
////TODO Display the number of results
////TODO Add filtering functionality: if more than one word is entered, the results will be limited to those that include all words.
////TODO Translate the app into other European languages, and allow the user to choose the wanted language.

//TODO: bugs
////TODO not found "koto ga dekiru" & "te oku" in local db
////TODO searching for "福" through left radical yields no results, since it does not point to spirit radical, just radical variant
////TODO "kamoshi" returns no josho.org results
////TODO "hanashinagara" returns conjugated verb, but hiragana equivalent doesn't
////TODO imperative display for godan verbs

public class MainActivity extends AppCompatActivity implements InputQueryFragment.UserEnteredQueryListener,
                                            DictionaryFragment.UserWantsNewSearchForSelectedWordListener,
                                            DictionaryFragment.UserWantsToConjugateFoundVerbListener,
                                            ComposeKanjiFragment.UserWantsNewSearchForSelectedCharacterListener,
                                            SharedPreferences.OnSharedPreferenceChangeListener {

    //Globals
    InputQueryFragment inputQueryFragment;
    static List<String[]> MainDatabase;
    static List<String[]> ExamplesDatabase;
    static List<String[]> MeaningsDatabase;
    static List<String[]> MultExplanationsDatabase;
    static List<String[]> LegendDatabase;
    public static List<String[]> GrammarDatabaseIndexedLatin;
    public static List<String[]> GrammarDatabaseIndexedKanji;
    static List<String[]> RadicalsDatabase;
    static List<String[]> CJK_Database;
    static List<String[]> VerbDatabase;
    static List<String[]> VerbLatinConjDatabase;
    static List<String[]> VerbKanjiConjDatabase;
    static List<String[]> KanjiDict_Database;
    static List<String[]> Components_Database;
    public static List<String[]> SimilarsDatabase;
    static List<List<String[]>> Array_of_Components_Databases;
    static List<String[]> RadicalsOnlyDatabase;
    static Typeface CJK_typeface;
    static String[] radical_module_user_selections;

    public static long heap_size;
    public static long heap_size_before_decomposition_loader;
    public static long heap_size_before_searchbyradical_loader;
    public static Boolean enough_memory_for_heavy_functions;
    Intent restartIntent;
    Toast mLastToast;
    private FirebaseAuth mFirebaseAuth;

    //Preferences Globals
    public static Boolean mShowOnlineResults;
    public static String mChosenSpeechToTextLanguage;
    public static String mChosenTextToSpeechLanguage;
    public static String mChosenOCRLanguage;
    public static boolean activityResumedFromSTTOrOCR;
    private float mOcrImageDefaultContrast;
    private float mOcrImageDefaultBrightness;
    private float mOcrImageDefaultSaturation;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i("Diagnosis Time", "Started MainActivity.");
        setupSharedPreferences();
        activityResumedFromSTTOrOCR = false;


        // Start loading the local database in the background
        if (MainDatabase == null) {
            new BackgroundDatabaseLoader().execute();
        }

        // Define that MainActivity is related to activity_masterframe.xml
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        //Setting up Firebase
        FirebaseDatabase firebaseDb = FirebaseDatabase.getInstance();
        firebaseDb.setPersistenceEnabled(true);
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseAuth.signInWithEmailAndPassword(DatabaseUtilities.firebaseEmail, DatabaseUtilities.firebasePass);

        //Code allowing to bypass strict mode
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Start the fragment manager
        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Load the Fragments depending on the device orientation
        Configuration config = getResources().getConfiguration();
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (savedInstanceState == null) {
                inputQueryFragment = new InputQueryFragment();
                fragmentTransaction.add(R.id.InputQueryPlaceholder, inputQueryFragment);
            }
        } else {
            if (savedInstanceState == null) {
                inputQueryFragment = new InputQueryFragment();
                fragmentTransaction.add(R.id.InputQueryPlaceholder, inputQueryFragment);
            }
        }

        fragmentTransaction.commit();

        // Remove the software keyboard if the EditText is not in focus
        findViewById(android.R.id.content).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                SharedMethods.hideSoftKeyboard(MainActivity.this);
                return false;
            }
        });

        // Set the Requested Fragment if it was saved from a previous instance
        Global_Fragment_chooser_keyword = "start";
        if (savedInstanceState != null) {
            String savedRequestedFragment = savedInstanceState.getString("RequestedFragment");
            if (savedRequestedFragment != null) {
                Global_Fragment_chooser_keyword = savedRequestedFragment;
            }
        }

        //Set the typeface for Chinese/Japanese fonts
        CJK_typeface = Typeface.DEFAULT;
        //CJK_typeface = Typeface.createFromAsset(getAssets(), "fonts/DroidSansFallback.ttf");
        //CJK_typeface = Typeface.createFromAsset(getAssets(), "fonts/DroidSansJapanese.ttf");
        //see https://stackoverflow.com/questions/11786553/changing-the-android-typeface-doesnt-work

    }
    @Override protected void onStart() {
        super.onStart();
        restartIntent = this.getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(this.getBaseContext().getPackageName());

    }
    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
 		super.onSaveInstanceState(savedInstanceState);
 		
        savedInstanceState.putString("RequestedFragment", Global_Fragment_chooser_keyword);
 	}
    @Override protected void onResume() {
        super.onResume();
        //activityResumedFromSTTOrOCR = false;
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        try {
            SharedMethods.trimCache(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }
    @Override public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_complete_local_with_online_search_key))) {
            setShowOnlineResults(sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_complete_local_with_online_search_default)));
        }
        else if (key.equals(getString(R.string.pref_preferred_STT_language_key))) {
            setSpeechToTextLanguage(sharedPreferences.getString(getString(R.string.pref_preferred_STT_language_key), getString(R.string.pref_preferred_language_value_japanese)));
        }
        else if (key.equals(getString(R.string.pref_preferred_TTS_language_key))) {
            setTextToSpeechLanguage(sharedPreferences.getString(getString(R.string.pref_preferred_TTS_language_key), getString(R.string.pref_preferred_language_value_japanese)));
        }
        else if (key.equals(getString(R.string.pref_preferred_OCR_language_key))) {
            setOCRLanguage(sharedPreferences.getString(getString(R.string.pref_preferred_OCR_language_key), getString(R.string.pref_preferred_language_value_japanese)));
        }
        else if (key.equals(getString(R.string.pref_OCR_image_saturation_key))) {
            mOcrImageDefaultSaturation = SharedMethods.loadOCRImageSaturationFromSharedPreferences(sharedPreferences, getApplicationContext());
        }
        else if (key.equals(getString(R.string.pref_OCR_image_contrast_key))) {
            mOcrImageDefaultContrast = SharedMethods.loadOCRImageContrastFromSharedPreferences(sharedPreferences, getApplicationContext());
        }
        else if (key.equals(getString(R.string.pref_OCR_image_brightness_key))) {
            mOcrImageDefaultBrightness = SharedMethods.loadOCRImageBrightnessFromSharedPreferences(sharedPreferences, getApplicationContext());
        }
    }
    private void setupSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setShowOnlineResults(sharedPreferences.getBoolean(getString(R.string.pref_complete_local_with_online_search_key),
                getResources().getBoolean(R.bool.pref_complete_local_with_online_search_default)));
        setSpeechToTextLanguage(sharedPreferences.getString(getString(R.string.pref_preferred_STT_language_key), getString(R.string.pref_preferred_language_value_japanese)));
        setTextToSpeechLanguage(sharedPreferences.getString(getString(R.string.pref_preferred_TTS_language_key), getString(R.string.pref_preferred_language_value_japanese)));
        setOCRLanguage(sharedPreferences.getString(getString(R.string.pref_preferred_OCR_language_key), getString(R.string.pref_preferred_language_value_japanese)));
        mOcrImageDefaultContrast = SharedMethods.loadOCRImageContrastFromSharedPreferences(sharedPreferences, getApplicationContext());
        mOcrImageDefaultSaturation = SharedMethods.loadOCRImageSaturationFromSharedPreferences(sharedPreferences, getApplicationContext());
        mOcrImageDefaultBrightness = SharedMethods.loadOCRImageBrightnessFromSharedPreferences(sharedPreferences, getApplicationContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }
    public void setShowOnlineResults(boolean showCourse) {
        mShowOnlineResults = showCourse;
    }
    public void setSpeechToTextLanguage(String language) {
        if (language.equals(getResources().getString(R.string.pref_preferred_language_value_japanese))) {
            mChosenSpeechToTextLanguage = getResources().getString(R.string.languageLocaleJapanese);
        }
        else if (language.equals(getResources().getString(R.string.pref_preferred_language_value_english))) {
            mChosenSpeechToTextLanguage = getResources().getString(R.string.languageLocaleEnglishUS);
        }
    }
    public void setTextToSpeechLanguage(String language) {
        if (language.equals(getResources().getString(R.string.pref_preferred_language_value_japanese))) {
            mChosenTextToSpeechLanguage = getResources().getString(R.string.languageLocaleJapanese);
        }
        else if (language.equals(getResources().getString(R.string.pref_preferred_language_value_english))) {
            mChosenTextToSpeechLanguage = getResources().getString(R.string.languageLocaleEnglishUS);
        }
    }
    public void setOCRLanguage(String language) {
        if (language.equals(getResources().getString(R.string.pref_preferred_language_value_japanese))) {
            mChosenOCRLanguage = getResources().getString(R.string.languageLocaleJapanese);
        }
        else if (language.equals(getResources().getString(R.string.pref_preferred_language_value_english))) {
            mChosenOCRLanguage = getResources().getString(R.string.languageLocaleEnglishUS);
        }
    }

    public void showDatabaseLoadingToast(final String message, final Context context) {
        runOnUiThread(new Runnable() {
            public void run()
            {
                if(mLastToast != null) {
                    mLastToast.cancel();}

                Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM|Gravity.FILL_HORIZONTAL, 0, 0);

                View toastview = toast.getView();
                //toastview.setBackgroundColor(Color.WHITE);
                toastview.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.parseColor("#803582FF"))); //Light blue
                toastview.getBackground().setAlpha(90);

                TextView toastMessage = toast.getView().findViewById(android.R.id.message);
                toastMessage.setTextColor(Color.BLACK);
                toastMessage.setTypeface(null, Typeface.BOLD);
                toastMessage.setTextSize(12);
                toast.show();

                mLastToast = toast;
            }
        });
    }

    private class BackgroundDatabaseLoader extends AsyncTask<Void, Void, Void> {
        @Override protected void onPreExecute() {
            super.onPreExecute();
            Log.i("Diagnosis Time","Started background database loading.");
        }
        protected Void doInBackground(Void... params) {

            WordsRoomDatabase wordsRoomDatabase = WordsRoomDatabase.getInstance(getBaseContext());
            Word word = wordsRoomDatabase.getWordByWordId(2114);

            heap_size = SharedMethods.getAvailableMemory();
            enough_memory_for_heavy_functions = true;
            //Sizes based on file size, not on number of rows
            int CJK_Database_size = 2051;
            int GrammarDatabaseIndexedLatin_size = 1755;
            int RadicalsDatabase_size = 353;
            int GrammarDatabaseIndexedKanji_size = 372;
            int MainDatabase_size = 32+294+163 + 332 + 15;
            int VerbDatabase_size = 179;
            int KanjiDict_Database_size = 123;
            int RadicalsOnlyDatabase_size = 16;
            int total_assets_size_main_functions = GrammarDatabaseIndexedLatin_size+GrammarDatabaseIndexedKanji_size+MainDatabase_size+VerbDatabase_size;
            int total_assets_size_radical_functions = CJK_Database_size+RadicalsDatabase_size+KanjiDict_Database_size+RadicalsOnlyDatabase_size;
            int total_assets_size = total_assets_size_main_functions+total_assets_size_radical_functions;

            Context applicationContext = getApplicationContext();

            int cumulative_progress = 0;
            showDatabaseLoadingToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Main Database...", applicationContext);
            if (MainDatabase == null) { MainDatabase = DatabaseUtilities.loadCentralDatabaseFromCsv(getBaseContext());}
            Log.i("Diagnosis Time","Loaded MainDatabase.");

            if (ExamplesDatabase == null) { ExamplesDatabase = DatabaseUtilities.readCSVFile("LineExamples - 3000 kanji.csv", getBaseContext()); }
            Log.i("Diagnosis Time","Loaded ExamplesDatabase.");

            if (MeaningsDatabase == null) { MeaningsDatabase = DatabaseUtilities.readCSVFile("LineMeanings - 3000 kanji.csv", getBaseContext()); }
            Log.i("Diagnosis Time","Loaded MeaningsDatabase.");

            if (MultExplanationsDatabase == null) { MultExplanationsDatabase = DatabaseUtilities.readCSVFile("LineMultExplanations - 3000 kanji.csv", getBaseContext()); }
            Log.i("Diagnosis Time","Loaded MultExplanationsDatabase.");

            if (LegendDatabase == null) { LegendDatabase = DatabaseUtilities.readCSVFile("LineLegend - 3000 kanji.csv", getBaseContext()); }

            cumulative_progress = cumulative_progress + MainDatabase_size;
            showDatabaseLoadingToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Latin Database...", applicationContext);
            if (GrammarDatabaseIndexedLatin == null) { GrammarDatabaseIndexedLatin = DatabaseUtilities.readCSVFile("LineGrammarSortedIndexLatin - 3000 kanji.csv", getBaseContext());}
            Log.i("Diagnosis Time","Loaded GrammarDatabaseIndexedLatin.");

            cumulative_progress = cumulative_progress + GrammarDatabaseIndexedLatin_size;
            showDatabaseLoadingToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Kanji Database...", applicationContext);
            if (GrammarDatabaseIndexedKanji == null) { GrammarDatabaseIndexedKanji = DatabaseUtilities.readCSVFile("LineGrammarSortedIndexKanji - 3000 kanji.csv", getBaseContext());}
            Log.i("Diagnosis Time","Loaded GrammarDatabaseIndexedKanji.");

            cumulative_progress = cumulative_progress + GrammarDatabaseIndexedKanji_size;
            showDatabaseLoadingToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Verbs Database...", applicationContext);
            if (VerbDatabase == null || VerbDatabase.size() < 5) { VerbDatabase = DatabaseUtilities.readCSVFile("LineVerbs - 3000 kanji.csv", getBaseContext());}
            Log.i("Diagnosis Time","Loaded VerbDatabase.");

            if (VerbLatinConjDatabase == null || VerbLatinConjDatabase.size() < 5) { VerbLatinConjDatabase = DatabaseUtilities.readCSVFile("LineLatinConj - 3000 kanji.csv", getBaseContext());}
            Log.i("Diagnosis Time","Loaded VerbLatinConjDatabase.");

            if (VerbKanjiConjDatabase == null   || VerbKanjiConjDatabase.size() < 5)   { VerbKanjiConjDatabase = DatabaseUtilities.readCSVFile("LineKanjiConj - 3000 kanji.csv", getBaseContext());}
            Log.i("Diagnosis Time","Loaded VerbKanjiConjDatabase.");

            if (SimilarsDatabase == null   || SimilarsDatabase.size() < 5)   { SimilarsDatabase = DatabaseUtilities.readCSVFile("LineSimilars - 3000 kanji.csv", getBaseContext());}
            Log.i("Diagnosis Time","Loaded SimilarsDatabase.");

            heap_size = SharedMethods.getAvailableMemory();
            heap_size_before_decomposition_loader = heap_size;
            cumulative_progress = cumulative_progress + VerbDatabase_size;

            if (heap_size_before_decomposition_loader >= GlobalConstants.DECOMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE) {

                showDatabaseLoadingToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Radicals Database...", applicationContext);
                if (RadicalsDatabase == null) { RadicalsDatabase = DatabaseUtilities.readCSVFile("LineRadicals - 3000 kanji.csv", getBaseContext());}
                Log.i("Diagnosis Time","Loaded RadicalsDatabase.");

                cumulative_progress = cumulative_progress + RadicalsDatabase_size;
                showDatabaseLoadingToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Decompositions Database...", applicationContext);
                heap_size = SharedMethods.getAvailableMemory();
                if (CJK_Database == null) { CJK_Database = DatabaseUtilities.readCSVFile("LineCJK_Decomposition - 3000 kanji.csv", getBaseContext());}
                Log.i("Diagnosis Time", "Loaded CJK_Database.");

                cumulative_progress = cumulative_progress + CJK_Database_size;
                showDatabaseLoadingToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Characters Database...", applicationContext);
                heap_size = SharedMethods.getAvailableMemory();
                if (KanjiDict_Database == null) { KanjiDict_Database = DatabaseUtilities.readCSVFile("LineKanjiDictionary - 3000 kanji.csv", getBaseContext());}
                Log.i("Diagnosis Time", "Loaded KanjiDict_Database.");

                heap_size = SharedMethods.getAvailableMemory();
                if (RadicalsOnlyDatabase == null) { RadicalsOnlyDatabase = DatabaseUtilities.readCSVFile("LineRadicalsOnly - 3000 kanji.csv", getBaseContext());}
                Log.i("Diagnosis Time", "Loaded RadicalsOnlyDatabase.");

                heap_size = SharedMethods.getAvailableMemory();
                heap_size_before_searchbyradical_loader = heap_size;
                enough_memory_for_heavy_functions = true;

                cumulative_progress = cumulative_progress + (KanjiDict_Database_size+RadicalsOnlyDatabase_size);
                if (heap_size_before_searchbyradical_loader >= GlobalConstants.CHAR_COMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE) {

                    showDatabaseLoadingToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Components Database...", applicationContext);
                    if (Components_Database == null) {
                        Components_Database = DatabaseUtilities.readCSVFile("LineComponents - 3000 kanji.csv", getBaseContext());
                        List<String[]> temp = new ArrayList<>();
                        Array_of_Components_Databases = new ArrayList<>();
                        for (int i=0; i<Components_Database.size();i++) {
                            if (!Components_Database.get(i)[0].equals("") && Components_Database.get(i)[1].equals("")) {
                                if (i>1) {Array_of_Components_Databases.add(temp);}
                                temp = new ArrayList<>();
                            }
                            if (!Components_Database.get(i)[0].equals("") && !Components_Database.get(i)[1].equals("")) {temp.add(Components_Database.get(i));}
                        }
                        Array_of_Components_Databases.add(temp);
                        temp = null;
                        Components_Database = null;
                    }
                    Log.i("Diagnosis Time", "Loaded Components_Database.");

                    showDatabaseLoadingToast("Progress: 100%. Loaded all databases.", applicationContext);
                }
                else {
                    showDatabaseLoadingToast("Stopped at " + Math.round(100*cumulative_progress/total_assets_size) + "% due to low memory.", applicationContext);
                }

            }
            else {
                enough_memory_for_heavy_functions = false;
                showDatabaseLoadingToast("Stopped at " + Math.round(100*cumulative_progress/total_assets_size) + "% due to low memory.", applicationContext);
            }
            heap_size = SharedMethods.getAvailableMemory();

            Log.i("Diagnosis Time","Loaded All Databases.");
            return null;
        }
        protected void onProgressUpdate(Integer... progress) {
            //setProgressPercent(progress[0]);
        }
        protected void onPostExecute() {
            Log.i("Diagnosis Time","Loadeded all databases.");
        }
    }

	private String Global_Fragment_chooser_keyword;
    public void OnQueryEnteredSwitchToRelevantFragment(String[] outputFromInputQueryFragment) {

        // outputFromInputQueryFragment includes: [fragment keyword, user input, additional control keyword (unused)]

    	if (Global_Fragment_chooser_keyword.equals("start"))
    			{ Global_Fragment_chooser_keyword = outputFromInputQueryFragment[0]; }
    	else  	{ Global_Fragment_chooser_keyword = outputFromInputQueryFragment[0]; }

        FrameLayout frame = findViewById(R.id.FunctionFragmentsPlaceholder);
        frame.setVisibility(View.VISIBLE);
        frame.bringToFront();

        // Prepare the input for the fragment
        Bundle bundle = new Bundle();
        bundle.putString("input_to_fragment", outputFromInputQueryFragment[1]);

        if (Global_Fragment_chooser_keyword.equals("verb")) {

        	ConjugatorFragment conjugatorFragment = new ConjugatorFragment();
            conjugatorFragment.setArguments(bundle);

            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.FunctionFragmentsPlaceholder, conjugatorFragment);

            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commitAllowingStateLoss ();
        }
        else if (Global_Fragment_chooser_keyword.equals("word")) {

        	DictionaryFragment dictionaryFragment = new DictionaryFragment();
            dictionaryFragment.setArguments(bundle);

            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.FunctionFragmentsPlaceholder, dictionaryFragment);

            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commitAllowingStateLoss ();
        }
        else if (Global_Fragment_chooser_keyword.equals("convert")) {

            ConvertFragment convertFragment = new ConvertFragment();
            convertFragment.setArguments(bundle);

            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.FunctionFragmentsPlaceholder, convertFragment);

            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commitAllowingStateLoss ();
        }
        else if (Global_Fragment_chooser_keyword.equals("radicals")  && heap_size_before_decomposition_loader >= GlobalConstants.DECOMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE) {

            ComposeKanjiFragment ComposeKanjiFragment = new ComposeKanjiFragment();
            ComposeKanjiFragment.setArguments(bundle);

            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.FunctionFragmentsPlaceholder, ComposeKanjiFragment);

            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commitAllowingStateLoss ();
        }
        else if (Global_Fragment_chooser_keyword.equals("decompose")  && heap_size_before_decomposition_loader >= GlobalConstants.DECOMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE) {

            DecomposeKanjiFragment DecomposeKanjiFragment = new DecomposeKanjiFragment();
            DecomposeKanjiFragment.setArguments(bundle);

            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.FunctionFragmentsPlaceholder, DecomposeKanjiFragment);

            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commitAllowingStateLoss ();

        }
    }
    public void UserWantsNewSearchForSelectedWordFromGrammarModule(String outputFromGrammarModuleFragment) {

        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        //InputQueryFragment inputQueryFragment = new InputQueryFragment();
        inputQueryFragment.setNewQuery(outputFromGrammarModuleFragment);
        fragmentTransaction.commit();

    }
    public void UserWantsToSearchForSelectedResultFromRadicalsModule(String outputFromRadicalsModuleFragment) {

        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        //InputQueryFragment inputQueryFragment = new InputQueryFragment();
        inputQueryFragment.setNewQuery(outputFromRadicalsModuleFragment);
        fragmentTransaction.commit();
    }
    public void UserWantsToConjugateFoundVerbFromGrammarModule(String[] outputFromGrammarModuleFragment) {

        Bundle bundle = new Bundle();
        bundle.putString("input_to_fragment", outputFromGrammarModuleFragment[1]);

        ConjugatorFragment conjugatorFragment = new ConjugatorFragment();
        conjugatorFragment.setArguments(bundle);

        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.FunctionFragmentsPlaceholder, conjugatorFragment);

        fragmentTransaction.addToBackStack(null);

        fragmentTransaction.commit();
    }
}

