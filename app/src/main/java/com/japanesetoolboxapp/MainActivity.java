package com.japanesetoolboxapp;

import com.japanesetoolboxapp.utiities.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

//TODO link Japanese Toolbox to OK Google / Google Assistant, for on-the-fly lookups
//TODO when entering "tsureteiku", online results show "reteiku"
//TODO imperative display for godan verbs
//TODO Show the adjective conjugations (it will also explain to the user why certain adjectives appear in the list, based on their conjugations)
//TODO If the user enter "一", make sure the app gives all results with a bar
//TODO Give the user the option to reduce/increase the number of search results (useful when there are many results)
//TODO Display the number of results
//TODO Add the option of selecting which databases to search in (in order to limit the number of results)
//TODO Allow for different kanji versions of a verb by comparing the input verb's kanji to a list associated with every verb,
//     then applying the reverse conjugation by searching using the database's kanji instead of the user's input, and also showing the other possible kanji dict. forms
//TODO Add filtering functionality: if more than one word is entered, the results will be limited to those that include all words.
//TODO Translate the app into other European languages, and allow the user to choose the wanted language.

public class MainActivity extends FragmentActivity
								 implements InputQueryFragment.UserEnteredQueryListener,
                                            GrammarModuleFragment.UserWantsNewSearchForSelectedWordListener,
                                            GrammarModuleFragment.UserWantsToConjugateFoundVerbListener,
                                            SearchByRadicalsModuleFragment.UserWantsNewSearchForSelectedCharacterListener {

    //Globals
    InputQueryFragment inputQueryFragment;
    static List<String[]> MainDatabase;
    static List<String[]> ExamplesDatabase;
    static List<String[]> MeaningsDatabase;
    static List<String[]> MultExplanationsDatabase;
    static List<String[]> LegendDatabase;
    static List<String[]> GrammarDatabaseIndexedLatin;
    static List<String[]> GrammarDatabaseIndexedKanji;
    static List<String[]> RadicalsDatabase;
    static List<String[]> CJK_Database;
    static List<String[]> VerbDatabase;
    static List<String[]> VerbLatinConjDatabase;
    static List<String[]> VerbKanjiConjDatabase;
    static List<String[]> KanjiDict_Database;
    static List<String[]> Components_Database;
    static List<String[]> SimilarsDatabase;
    static List<List<String[]>> Array_of_Components_Databases;
    static List<String[]> RadicalsOnlyDatabase;
    static Typeface CJK_typeface;
    static String[] radical_module_user_selections;

    public static long heap_size;
    public static long heap_size_before_decomposition_loader;
    public static long heap_size_before_searchbyradical_loader;
    public static Boolean enough_memory_for_heavy_functions;
    int mSheetRowLength;
    Intent restartIntent;
    Toast mLastToast;

	@Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i("Diagnosis Time", "Started MainActivity.");

        // Start loading the database in the background
        if (MainDatabase == null) {

            //MainDatabase = getMainDatabase();
            new BackgroundDatabaseLoader().execute();
        }

        // Define that MainActivity is related to activity_masterframe.xml
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_masterframe);


        //Code allowing to bypass strict mode
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Start the fragment manager
        Configuration config = getResources().getConfiguration();

        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Load the Fragments depending on the device orientation
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (savedInstanceState == null) {
                inputQueryFragment = new InputQueryFragment();
                fragmentTransaction.add(R.id.InputQueryPlaceholder, inputQueryFragment);

                //grammarModuleFragment = new GrammarModuleFragment();
                //fragmentTransaction.add(R.id.FunctionFragmentsPlaceholder, grammarModuleFragment);

                //verbModuleFragment = new VerbModuleFragment();
                //fragmentTransaction.replace(R.id.FunctionFragmentsPlaceholder, verbModuleFragment);

                //conversionModuleFragment = new ConversionModuleFragment();
                //fragmentTransaction.add(R.id.FunctionFragmentsPlaceholder, conversionModuleFragment);

                //SearchByRadicalsModuleFragment = new SearchByRadicalsModuleFragment();
                //fragmentTransaction.add(R.id.FunctionFragmentsPlaceholder, SearchByRadicalsModuleFragment);

                //DecompositionModuleFragment = new DecompositionModuleFragment();
                //fragmentTransaction.add(R.id.FunctionFragmentsPlaceholder, DecompositionModuleFragment);
            }
        } else {
            if (savedInstanceState == null) {
                inputQueryFragment = new InputQueryFragment();
                fragmentTransaction.add(R.id.InputQueryPlaceholder, inputQueryFragment);

                //grammarModuleFragment = new GrammarModuleFragment();
                //fragmentTransaction.add(R.id.FunctionFragmentsPlaceholder, grammarModuleFragment);

                //verbModuleFragment = new VerbModuleFragment();
                //fragmentTransaction.replace(R.id.FunctionFragmentsPlaceholder, verbModuleFragment);

                //conversionModuleFragment = new ConversionModuleFragment();
                //fragmentTransaction.add(R.id.FunctionFragmentsPlaceholder, conversionModuleFragment);

                //SearchByRadicalsModuleFragment = new SearchByRadicalsModuleFragment();
                //fragmentTransaction.add(R.id.FunctionFragmentsPlaceholder, SearchByRadicalsModuleFragment);

                //DecompositionModuleFragment = new DecompositionModuleFragment();
                //fragmentTransaction.add(R.id.FunctionFragmentsPlaceholder, DecompositionModuleFragment);
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
        ;//CJK_typeface = Typeface.createFromAsset(getAssets(), "fonts/DroidSansFallback.ttf");
        ;//CJK_typeface = Typeface.createFromAsset(getAssets(), "fonts/DroidSansJapanese.ttf");
        //see https://stackoverflow.com/questions/11786553/changing-the-android-typeface-doesnt-work

    }

    @Override protected void onStart() {
        super.onStart();

//        if (GrammarDatabase.size() == 0 || GrammarDatabase == null) {
//
//            // ie. If Android killed GlobalTranslatorActivity, restart the app
//            int delay = 1;
//            Log.e("", "Restarting app");
//            if (this != null) {
//                restartIntent = this.getBaseContext().getPackageManager()
//                        .getLaunchIntentForPackage(this.getBaseContext().getPackageName());
//                PendingIntent intent = PendingIntent.getActivity(
//                        this.getBaseContext(), 0,
//                        restartIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//                AlarmManager manager = (AlarmManager) this.getBaseContext().getSystemService(Context.ALARM_SERVICE);
//                manager.set(AlarmManager.RTC, System.currentTimeMillis() + delay, intent);
//                System.exit(2);
//            }
//            else {
//                Intent intent = new Intent();
//                intent.setClass(GlobalGrammarModuleFragmentView.getContext(), SplashScreen.class);
//                startActivity(intent);
//            }
//        }


        restartIntent = this.getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(this.getBaseContext().getPackageName());

    }
 	@Override public void onSaveInstanceState(Bundle savedInstanceState) {
 		super.onSaveInstanceState(savedInstanceState);
 		
        savedInstanceState.putString("RequestedFragment", Global_Fragment_chooser_keyword);
        
 	}
    @Override protected void onStop(){
        super.onStop();
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        try {
            SharedMethods.trimCache(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
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
            if (MainDatabase == null) { MainDatabase = getMainDatabase();}
            Log.i("Diagnosis Time","Loaded MainDatabase.");

            if (ExamplesDatabase == null) { ExamplesDatabase = SharedMethods.readCSVFile("LineExamples - 3000 kanji.csv", getBaseContext()); }
            Log.i("Diagnosis Time","Loaded ExamplesDatabase.");

            if (MeaningsDatabase == null) { MeaningsDatabase = SharedMethods.readCSVFile("LineMeanings - 3000 kanji.csv", getBaseContext()); }
            Log.i("Diagnosis Time","Loaded MeaningsDatabase.");

            if (MultExplanationsDatabase == null) { MultExplanationsDatabase = SharedMethods.readCSVFile("LineMultExplanations - 3000 kanji.csv", getBaseContext()); }
            Log.i("Diagnosis Time","Loaded MultExplanationsDatabase.");

            if (LegendDatabase == null) { LegendDatabase = SharedMethods.readCSVFile("LineLegend - 3000 kanji.csv", getBaseContext()); }

            cumulative_progress = cumulative_progress + MainDatabase_size;
            showDatabaseLoadingToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Latin Database...", applicationContext);
            if (GrammarDatabaseIndexedLatin == null) { GrammarDatabaseIndexedLatin = SharedMethods.readCSVFile("LineGrammarSortedIndexLatin - 3000 kanji.csv", getBaseContext());}
            Log.i("Diagnosis Time","Loaded GrammarDatabaseIndexedLatin.");

            cumulative_progress = cumulative_progress + GrammarDatabaseIndexedLatin_size;
            showDatabaseLoadingToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Kanji Database...", applicationContext);
            if (GrammarDatabaseIndexedKanji == null) { GrammarDatabaseIndexedKanji = SharedMethods.readCSVFile("LineGrammarSortedIndexKanji - 3000 kanji.csv", getBaseContext());}
            Log.i("Diagnosis Time","Loaded GrammarDatabaseIndexedKanji.");

            cumulative_progress = cumulative_progress + GrammarDatabaseIndexedKanji_size;
            showDatabaseLoadingToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Verbs Database...", applicationContext);
            if (VerbDatabase == null || VerbDatabase.size() < 5) { VerbDatabase = SharedMethods.readCSVFile("LineVerbs - 3000 kanji.csv", getBaseContext());}
            Log.i("Diagnosis Time","Loaded VerbDatabase.");

            if (VerbLatinConjDatabase == null || VerbLatinConjDatabase.size() < 5) { VerbLatinConjDatabase = SharedMethods.readCSVFile("LineLatinConj - 3000 kanji.csv", getBaseContext());}
            Log.i("Diagnosis Time","Loaded VerbLatinConjDatabase.");

            if (VerbKanjiConjDatabase == null   || VerbKanjiConjDatabase.size() < 5)   { VerbKanjiConjDatabase = SharedMethods.readCSVFile("LineKanjiConj - 3000 kanji.csv", getBaseContext());}
            Log.i("Diagnosis Time","Loaded VerbKanjiConjDatabase.");

            if (SimilarsDatabase == null   || SimilarsDatabase.size() < 5)   { SimilarsDatabase = SharedMethods.readCSVFile("LineSimilars - 3000 kanji.csv", getBaseContext());}
            Log.i("Diagnosis Time","Loaded SimilarsDatabase.");

            heap_size = SharedMethods.getAvailableMemory();
            heap_size_before_decomposition_loader = heap_size;
            cumulative_progress = cumulative_progress + VerbDatabase_size;

            if (heap_size_before_decomposition_loader >= GlobalConstants.DECOMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE) {

                showDatabaseLoadingToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Radicals Database...", applicationContext);
                if (RadicalsDatabase == null) { RadicalsDatabase = SharedMethods.readCSVFile("LineRadicals - 3000 kanji.csv", getBaseContext());}
                Log.i("Diagnosis Time","Loaded RadicalsDatabase.");

                cumulative_progress = cumulative_progress + RadicalsDatabase_size;
                showDatabaseLoadingToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Decompositions Database...", applicationContext);
                heap_size = SharedMethods.getAvailableMemory();
                if (CJK_Database == null) { CJK_Database = SharedMethods.readCSVFile("LineCJK_Decomposition - 3000 kanji.csv", getBaseContext());}
                Log.i("Diagnosis Time", "Loaded CJK_Database.");

                cumulative_progress = cumulative_progress + CJK_Database_size;
                showDatabaseLoadingToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Characters Database...", applicationContext);
                heap_size = SharedMethods.getAvailableMemory();
                if (KanjiDict_Database == null) { KanjiDict_Database = SharedMethods.readCSVFile("LineKanjiDictionary - 3000 kanji.csv", getBaseContext());}
                Log.i("Diagnosis Time", "Loaded KanjiDict_Database.");

                heap_size = SharedMethods.getAvailableMemory();
                if (RadicalsOnlyDatabase == null) { RadicalsOnlyDatabase = SharedMethods.readCSVFile("LineRadicalsOnly - 3000 kanji.csv", getBaseContext());}
                Log.i("Diagnosis Time", "Loaded RadicalsOnlyDatabase.");

                heap_size = SharedMethods.getAvailableMemory();
                heap_size_before_searchbyradical_loader = heap_size;
                enough_memory_for_heavy_functions = true;

                cumulative_progress = cumulative_progress + (KanjiDict_Database_size+RadicalsOnlyDatabase_size);
                if (heap_size_before_searchbyradical_loader >= GlobalConstants.CHAR_COMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE) {

                    showDatabaseLoadingToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Components Database...", applicationContext);
                    if (Components_Database == null) {
                        Components_Database = SharedMethods.readCSVFile("LineComponents - 3000 kanji.csv", getBaseContext());
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
    public List<String[]> getMainDatabase() {

        // Import the excel sheets (csv format)

        List<String[]> LinemySheet 				= new ArrayList<>();
        List<String[]> LinemySheetTypes 		= SharedMethods.readCSVFile("LineTypes - 3000 kanji.csv", getBaseContext());
        List<String[]> LinemySheetGrammar 		= SharedMethods.readCSVFile("LineGrammar - 3000 kanji.csv", getBaseContext());
        List<String[]> LinemySheetVerbs     	= SharedMethods.readCSVFile("LineVerbsForGrammar - 3000 kanji.csv", getBaseContext());

        LinemySheet.addAll(LinemySheetTypes);
        LinemySheet.addAll(LinemySheetGrammar);
        LinemySheet.addAll(LinemySheetVerbs);

        if (LinemySheet.size()>0) {
            mSheetRowLength = LinemySheet.get(0).length;}

        return LinemySheet;
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

        	VerbModuleFragment verbModuleFragment = new VerbModuleFragment();
            verbModuleFragment.setArguments(bundle);

            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.FunctionFragmentsPlaceholder, verbModuleFragment);

            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        }
        else if (Global_Fragment_chooser_keyword.equals("word")) {

        	GrammarModuleFragment grammarModuleFragment = new GrammarModuleFragment();
            grammarModuleFragment.setArguments(bundle);

            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.FunctionFragmentsPlaceholder, grammarModuleFragment);

            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        }
        else if (Global_Fragment_chooser_keyword.equals("convert")) {

            ConversionModuleFragment conversionModuleFragment = new ConversionModuleFragment();
            conversionModuleFragment.setArguments(bundle);

            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.FunctionFragmentsPlaceholder, conversionModuleFragment);

            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        }
        else if (Global_Fragment_chooser_keyword.equals("radicals")  && heap_size_before_decomposition_loader >= GlobalConstants.DECOMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE) {

            SearchByRadicalsModuleFragment SearchByRadicalsModuleFragment = new SearchByRadicalsModuleFragment();
            SearchByRadicalsModuleFragment.setArguments(bundle);

            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.FunctionFragmentsPlaceholder, SearchByRadicalsModuleFragment);

            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        }
        else if (Global_Fragment_chooser_keyword.equals("decompose")  && heap_size_before_decomposition_loader >= GlobalConstants.DECOMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE) {

            DecompositionModuleFragment DecompositionModuleFragment = new DecompositionModuleFragment();
            DecompositionModuleFragment.setArguments(bundle);

            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.FunctionFragmentsPlaceholder, DecompositionModuleFragment);

            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();

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

        VerbModuleFragment verbModuleFragment = new VerbModuleFragment();
        verbModuleFragment.setArguments(bundle);

        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.FunctionFragmentsPlaceholder, verbModuleFragment);

        fragmentTransaction.addToBackStack(null);

        fragmentTransaction.commit();
    }

    public void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =(InputMethodManager) activity.getBaseContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }
}

