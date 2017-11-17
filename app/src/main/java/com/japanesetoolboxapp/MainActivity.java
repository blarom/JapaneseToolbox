package com.japanesetoolboxapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

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

    static long total_required_heap_size = 45;
    static long decomposition_min_heap_size = 23;
    static long components_min_heap_size = 6;
    static long heap_size;
    static long heap_size_before_decomposition_loader;
    static long heap_size_before_searchbyradical_loader;
    static Boolean enough_memory_for_heavy_functions;

    static int SheetRowLength;
    static int NumbersSheetSize;
    public static Intent restartIntent;

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

	    	/*FrameLayout frame_verb = (FrameLayout) findViewById(R.id.translator_placeholder2);
	    	frame_verb.setVisibility(View.GONE);
	    	FrameLayout frame_word = (FrameLayout) findViewById(R.id.translator_placeholder3);
	    	frame_word.setVisibility(View.GONE);
	    	FrameLayout frame_convert = (FrameLayout) findViewById(R.id.translator_placeholder4);
	    	frame_convert.setVisibility(View.GONE);
            FrameLayout frame_radicals = (FrameLayout) findViewById(R.id.translator_placeholder5);
            frame_radicals.setVisibility(View.GONE);*/

        fragmentTransaction.commit();

        // Remove the software keyboard if the EditText is not in focus
        findViewById(android.R.id.content).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideSoftKeyboard();
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
            trimCache(this);
        } catch (Exception e) {
            // TODO Auto-generated catch block
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
    public static void trimCache(Context context) {
        // http://stackoverflow.com/questions/10977288/clear-application-cache-on-exit-in-android
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }
    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    Toast lastToast;
    public void showToast(final String message) {
        runOnUiThread(new Runnable() {
            public void run()
            {
                if(lastToast != null) {lastToast.cancel();}

                Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
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

                lastToast = toast;
            }
        });
    }
    private class BackgroundDatabaseLoader extends AsyncTask<Void, Void, Void> {

        @Override protected void onPreExecute() {
            super.onPreExecute();
            Log.i("Diagnosis Time","Started background database loading.");
        }

        protected Void doInBackground(Void... params) {

            heap_size = AvailableMemory();
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

            int cumulative_progress = 0;
            showToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Main Database...");
            if (MainDatabase == null) { MainDatabase = getMainDatabase();}
            Log.i("Diagnosis Time","Loaded MainDatabase.");

            if (ExamplesDatabase == null) { ExamplesDatabase = readCSVFile("LineExamples - 3000 kanji.csv", getBaseContext()); }
            Log.i("Diagnosis Time","Loaded ExamplesDatabase.");

            if (MeaningsDatabase == null) { MeaningsDatabase = readCSVFile("LineMeanings - 3000 kanji.csv", getBaseContext()); }
            Log.i("Diagnosis Time","Loaded MeaningsDatabase.");

            if (MultExplanationsDatabase == null) { MultExplanationsDatabase = readCSVFile("LineMultExplanations - 3000 kanji.csv", getBaseContext()); }
            Log.i("Diagnosis Time","Loaded MultExplanationsDatabase.");

            if (LegendDatabase == null) { LegendDatabase = readCSVFile("LineLegend - 3000 kanji.csv", getBaseContext()); }

            cumulative_progress = cumulative_progress + MainDatabase_size;
            showToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Latin Database...");
            if (GrammarDatabaseIndexedLatin == null) { GrammarDatabaseIndexedLatin = readCSVFile("LineGrammarSortedIndexLatin - 3000 kanji.csv", getBaseContext());}
            Log.i("Diagnosis Time","Loaded GrammarDatabaseIndexedLatin.");

            cumulative_progress = cumulative_progress + GrammarDatabaseIndexedLatin_size;
            showToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Kanji Database...");
            if (GrammarDatabaseIndexedKanji == null) { GrammarDatabaseIndexedKanji = readCSVFile("LineGrammarSortedIndexKanji - 3000 kanji.csv", getBaseContext());}
            Log.i("Diagnosis Time","Loaded GrammarDatabaseIndexedKanji.");

            cumulative_progress = cumulative_progress + GrammarDatabaseIndexedKanji_size;
            showToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Verbs Database...");
            if (VerbDatabase == null || VerbDatabase.size() < 5) { VerbDatabase = MainActivity.readCSVFile("LineVerbs - 3000 kanji.csv", getBaseContext());}
            Log.i("Diagnosis Time","Loaded VerbDatabase.");

            if (VerbLatinConjDatabase == null || VerbLatinConjDatabase.size() < 5) { VerbLatinConjDatabase = MainActivity.readCSVFile("LineLatinConj - 3000 kanji.csv", getBaseContext());}
            Log.i("Diagnosis Time","Loaded VerbLatinConjDatabase.");

            if (VerbKanjiConjDatabase == null   || VerbKanjiConjDatabase.size() < 5)   { VerbKanjiConjDatabase = MainActivity.readCSVFile("LineKanjiConj - 3000 kanji.csv", getBaseContext());}
            Log.i("Diagnosis Time","Loaded VerbKanjiConjDatabase.");

            if (SimilarsDatabase == null   || SimilarsDatabase.size() < 5)   { SimilarsDatabase = MainActivity.readCSVFile("LineSimilars - 3000 kanji.csv", getBaseContext());}
            Log.i("Diagnosis Time","Loaded SimilarsDatabase.");

            heap_size = AvailableMemory();
            heap_size_before_decomposition_loader = heap_size;
            cumulative_progress = cumulative_progress + VerbDatabase_size;

            if (heap_size_before_decomposition_loader >= decomposition_min_heap_size) {

                showToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Radicals Database...");
                if (RadicalsDatabase == null) { RadicalsDatabase = readCSVFile("LineRadicals - 3000 kanji.csv", getBaseContext());}
                Log.i("Diagnosis Time","Loaded RadicalsDatabase.");

                cumulative_progress = cumulative_progress + RadicalsDatabase_size;
                showToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Decompositions Database...");
                heap_size = AvailableMemory();
                if (CJK_Database == null) { CJK_Database = readCSVFile("LineCJK_Decomposition - 3000 kanji.csv", getBaseContext());}
                Log.i("Diagnosis Time", "Loaded CJK_Database.");

                cumulative_progress = cumulative_progress + CJK_Database_size;
                showToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Characters Database...");
                heap_size = AvailableMemory();
                if (KanjiDict_Database == null) { KanjiDict_Database = readCSVFile("LineKanjiDictionary - 3000 kanji.csv", getBaseContext());}
                Log.i("Diagnosis Time", "Loaded KanjiDict_Database.");

                heap_size = AvailableMemory();
                if (RadicalsOnlyDatabase == null) { RadicalsOnlyDatabase = readCSVFile("LineRadicalsOnly - 3000 kanji.csv", getBaseContext());}
                Log.i("Diagnosis Time", "Loaded RadicalsOnlyDatabase.");

                heap_size = AvailableMemory();
                heap_size_before_searchbyradical_loader = heap_size;
                enough_memory_for_heavy_functions = true;

                cumulative_progress = cumulative_progress + (KanjiDict_Database_size+RadicalsOnlyDatabase_size);
                if (heap_size_before_searchbyradical_loader >= components_min_heap_size) {

                    showToast("Progress: " + Math.round(100*cumulative_progress/total_assets_size) + "%. Loading Components Database...");
                    if (Components_Database == null) {
                        Components_Database = readCSVFile("LineComponents - 3000 kanji.csv", getBaseContext());
                        List<String[]> temp = new ArrayList<>();
                        Array_of_Components_Databases = new ArrayList<>();
                        for (int i=0; i<Components_Database.size();i++) {
                            if (!Components_Database.get(i)[0].equals("") && Components_Database.get(i)[1].equals("")) {
                                if (i>1) {Array_of_Components_Databases.add(temp);}
                                temp = new ArrayList<>();
                            }
                            if (!Components_Database.get(i)[0].equals("") && !Components_Database.get(i)[1].equals("")) {temp.add(Components_Database.get(i));}
                        }
                        temp = null;
                        Components_Database = null;
                    }
                    Log.i("Diagnosis Time", "Loaded Components_Database.");

                    showToast("Progress: 100%. Loaded all databases.");
                }
                else {
                    showToast("Stopped at " + Math.round(100*cumulative_progress/total_assets_size) + "% due to low memory.");
                }

            }
            else {
                enough_memory_for_heavy_functions = false;
                showToast("Stopped at " + Math.round(100*cumulative_progress/total_assets_size) + "% due to low memory.");
            }
            heap_size = AvailableMemory();

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

    public long AvailableMemory() {
        final Runtime runtime = Runtime.getRuntime();
        final long usedMemInMB=(runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
        final long maxHeapSizeInMB=runtime.maxMemory() / 1048576L;
        final long availHeapSizeInMB = maxHeapSizeInMB - usedMemInMB;
        Log.i("Diagnosis Time","Available heap size: " + availHeapSizeInMB);
        return availHeapSizeInMB;
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
        else if (Global_Fragment_chooser_keyword.equals("radicals")  && heap_size_before_decomposition_loader >= decomposition_min_heap_size) {

            SearchByRadicalsModuleFragment SearchByRadicalsModuleFragment = new SearchByRadicalsModuleFragment();
            SearchByRadicalsModuleFragment.setArguments(bundle);

            android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();
            android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.FunctionFragmentsPlaceholder, SearchByRadicalsModuleFragment);

            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        }
        else if (Global_Fragment_chooser_keyword.equals("decompose")  && heap_size_before_decomposition_loader >= decomposition_min_heap_size) {

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

    public List<String[]> getMainDatabase() {

        // Import the excel sheets (csv format)

        List<String[]> LinemySheet 				= new ArrayList<>();
        List<String[]> LinemySheetTypes 		= readCSVFile("LineTypes - 3000 kanji.csv", getBaseContext());
        List<String[]> LinemySheetGrammar 		= readCSVFile("LineGrammar - 3000 kanji.csv", getBaseContext());
        List<String[]> LinemySheetVerbs     	= readCSVFile("LineVerbsForGrammar - 3000 kanji.csv", getBaseContext());

        LinemySheet.addAll(LinemySheetTypes);
        LinemySheet.addAll(LinemySheetGrammar);
        LinemySheet.addAll(LinemySheetVerbs);

        if (LinemySheet.size()>0) {SheetRowLength = LinemySheet.get(0).length;}

        return LinemySheet;
    }
    public static List<String[]> readCSVFile(String filename, Context context) {

        List<String[]> mySheet = new ArrayList<>();

        // OpenCSV implementation
        //                String next[] = null;
        //                CSVReader reader = null;
        //                try {
        //                    reader = new CSVReader(new InputStreamReader(GlobalTranslatorActivity.getAssets().open(filename)));
        //                } catch (IOException e) {
        //                    e.printStackTrace();
        //                }
        //                if (reader != null) {
        //                    for (; ; ) {
        //                        try {
        //                            next = reader.readNext();
        //                        } catch (IOException e) {
        //                            e.printStackTrace();
        //                        }
        //                        if (next != null) {
        //                            mySheet.add(next);
        //                        } else {
        //                            break;
        //                        }
        //                    }
        //                }
        //                try {
        //                    reader.close();
        //                } catch (IOException e) {
        //                    e.printStackTrace();
        //                }

        // "|" Parser implementation

        BufferedReader fileReader = null;

        int line_number = 0;
        try {
            String line;
            fileReader = new BufferedReader(new InputStreamReader(context.getAssets().open(filename)));

            while ((line = fileReader.readLine()) != null) {
                String[] tokens = line.split("\\|",-1);
                if (tokens.length > 0) {
                    mySheet.add(tokens);
                    line_number++;
                }
            }
        } catch (Exception e) {
            System.out.println("Error in CsvFileReader !!!");
            e.printStackTrace();
            Log.i("Diagnosis Time","Error in CsvFileReader opening Loaded DecompositionDatabase_PART4. Line number:"+line_number);
        } finally {
            try {
                if (fileReader != null) {fileReader.close();}
            } catch (IOException e) {
                System.out.println("Error while closing fileReader !!!");
                e.printStackTrace();
                Log.i("Diagnosis Time","Error in CsvFileReader closing Loaded DecompositionDatabase_PART4.");
            }
        }

        return mySheet;
    }
    public static  List<String[]> readCSVFileFirstRow(String filename, Context context) {

        List<String[]> mySheetFirstRow = new ArrayList<>();

        //OpenCSV implementation
        //				  String firstrow[] = null;
        //                String next[] = null;
        //                CSVReader reader = null;
        //
        //                try {
        //                    reader = new CSVReader(new InputStreamReader(GlobalTranslatorActivity.getAssets().open(filename)));
        //                } catch (IOException e) {
        //                    e.printStackTrace();
        //                }
        //
        //                if (reader != null) {
        //                    try {
        //                        firstrow = reader.readNext();
        //                    } catch (IOException e) {
        //                        e.printStackTrace();
        //                    }
        //                    if (firstrow != null) {
        //                        mySheetFirstRow.add(firstrow);
        //                    }
        //                }
        //
        //                try {
        //                    reader.close();
        //                } catch (IOException e) {
        //                    e.printStackTrace();
        //                }

        // "|" Parser implementation

        BufferedReader fileReader = null;

        try {
            String line;
            fileReader = new BufferedReader(new InputStreamReader(context.getAssets().open(filename)));

            line = fileReader.readLine();
            String[] tokens = line.split("\\|",-1);
            if (tokens.length > 0) {
                mySheetFirstRow.add(tokens);
            }
        } catch (Exception e) {
            System.out.println("Error in CsvFileReader !!!");
            e.printStackTrace();
        } finally {
            try {
                if (fileReader != null) {fileReader.close();}
            } catch (IOException e) {
                System.out.println("Error while closing fileReader !!!");
                e.printStackTrace();
            }
        }

        return mySheetFirstRow;
    }

    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(source);
        }
    }
    public void hideSoftKeyboard() {
        InputMethodManager inputMethodManager =(InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }
}

