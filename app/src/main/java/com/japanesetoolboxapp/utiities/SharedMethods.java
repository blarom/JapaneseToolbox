package com.japanesetoolboxapp.utiities;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.japanesetoolboxapp.ConvertFragment;
import com.japanesetoolboxapp.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SharedMethods {

    public static void trimCache(Context context) {
        // http://stackoverflow.com/questions/10977288/clear-application-cache-on-exit-in-android
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    public static List<String[]> readCSVFileFirstRow(String filename, Context context) {

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

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =(InputMethodManager) activity.getBaseContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && activity.getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    public void makeDelay(int milliseconds){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {}
        }, milliseconds);
    }

    public static long getAvailableMemory() {
        final Runtime runtime = Runtime.getRuntime();
        final long usedMemInMB=(runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
        final long maxHeapSizeInMB=runtime.maxMemory() / 1048576L;
        final long availHeapSizeInMB = maxHeapSizeInMB - usedMemInMB;
        Log.i("Diagnosis Time","Available heap size: " + availHeapSizeInMB);
        return availHeapSizeInMB;
    }

    static public String convertToUTF8(String input_string) {

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

    public static int loadOCRImageContrastFromSharedPreferences(SharedPreferences sharedPreferences, Context context) {
        float contrastValue = Float.parseFloat(context.getResources().getString(R.string.pref_OCR_image_contrast_default_value));
        try {
            contrastValue = Float.parseFloat(sharedPreferences.getString(context.getResources().getString(R.string.pref_OCR_image_contrast_key),
                    context.getResources().getString(R.string.pref_OCR_image_contrast_default_value)));
        } catch (Exception e) {
            contrastValue = Float.parseFloat(context.getResources().getString(R.string.pref_OCR_image_contrast_default_value));
        } finally {
            contrastValue = truncateToRange(contrastValue,
                    Float.parseFloat(context.getResources().getString(R.string.pref_OCR_image_contrast_min_value)),
                    Float.parseFloat(context.getResources().getString(R.string.pref_OCR_image_contrast_max_value)));
        }
        return (int) contrastValue;
    }
    public static int loadOCRImageSaturationFromSharedPreferences(SharedPreferences sharedPreferences, Context context) {
        float saturationValue = Float.parseFloat(context.getResources().getString(R.string.pref_OCR_image_saturation_default_value));
        try {
            saturationValue = Float.parseFloat(sharedPreferences.getString(context.getResources().getString(R.string.pref_OCR_image_saturation_key),
                    context.getResources().getString(R.string.pref_OCR_image_saturation_default_value)));
        } catch (Exception e) {
            saturationValue = Float.parseFloat(context.getResources().getString(R.string.pref_OCR_image_saturation_default_value));
        } finally {
            saturationValue = truncateToRange(saturationValue,
                    Float.parseFloat(context.getResources().getString(R.string.pref_OCR_image_saturation_min_value)),
                    Float.parseFloat(context.getResources().getString(R.string.pref_OCR_image_saturation_max_value)));
        }
        return (int) saturationValue;
    }
    public static int loadOCRImageBrightnessFromSharedPreferences(SharedPreferences sharedPreferences, Context context) {
        float brightnessValue = Float.parseFloat(context.getResources().getString(R.string.pref_OCR_image_brightness_default_value));
        try {
            brightnessValue = Float.parseFloat(sharedPreferences.getString(context.getResources().getString(R.string.pref_OCR_image_brightness_key),
                    context.getResources().getString(R.string.pref_OCR_image_brightness_default_value)));
        } catch (Exception e) {
            brightnessValue = Float.parseFloat(context.getResources().getString(R.string.pref_OCR_image_brightness_default_value));
        } finally {
            brightnessValue = truncateToRange(brightnessValue,
                    Float.parseFloat(context.getResources().getString(R.string.pref_OCR_image_brightness_min_value)),
                    Float.parseFloat(context.getResources().getString(R.string.pref_OCR_image_brightness_max_value)));
        }
        return (int) brightnessValue;
    }
    public static float truncateToRange(float value, float min, float max) {
        if (value < min) value = min;
        else if (value > max) value = max;
        return value;
    }
    public static float convertContrastProgressToValue(float contrastBarValue, Context context) {
        float contrastValue = contrastBarValue
                /((float) Integer.parseInt(context.getString(R.string.pref_OCR_image_contrast_range)))
                *((float) Integer.parseInt(context.getString(R.string.pref_OCR_image_contrast_max_value)));
        return contrastValue;
    }
    public static float convertSaturationProgressToValue(float saturationBarValue, Context context) {
        float saturationValue = saturationBarValue
                /((float) Integer.parseInt(context.getString(R.string.pref_OCR_image_saturation_range)))
                *((float) Integer.parseInt(context.getString(R.string.pref_OCR_image_saturation_multipliers)));
        return saturationValue;
    }
    public static int convertBrightnessProgresToValue(int brightnessBarValue, Context context) {
        int brightnessValue = brightnessBarValue-256;
        return brightnessValue;
    }
    public static int convertContrastValueToProgress(float contrastValue, Context context) {
        float contrastBarValue = contrastValue
                *((float) Integer.parseInt(context.getString(R.string.pref_OCR_image_contrast_range)))
                /((float) Integer.parseInt(context.getString(R.string.pref_OCR_image_contrast_max_value)));
        return (int) contrastBarValue;
    }
    public static int convertSaturationValueToProgress(float saturationValue, Context context) {
        float saturationBarValue = saturationValue
                *((float) Integer.parseInt(context.getString(R.string.pref_OCR_image_saturation_range)))
                /((float) Integer.parseInt(context.getString(R.string.pref_OCR_image_saturation_multipliers)));
        return (int) saturationBarValue;
    }
    public static int convertBrightnessValueToProgress(int brightnessValue, Context context) {
        int brightnessBarValue = brightnessValue+256;
        return brightnessBarValue;
    }

    //Internet Connectivity functions
    private static Boolean mInternetIsAvailable;
    public static void TellUserIfThereIsNoInternetConnection(final Activity activity) {
        //adapted from https://stackoverflow.com/questions/43315393/android-internet-connection-timeout

        if(!internetIsAvailableCheck(activity)) {
            try {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(activity.getBaseContext(), "Failed to connect to the Internet.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public static boolean internetIsAvailableCheck(Context context) {
        //adapted from https://stackoverflow.com/questions/43315393/android-internet-connection-timeout
        final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();

        if (activeNetworkInfo != null) { // connected to the internet
            //Toast.makeText(context, activeNetworkInfo.getTypeName(), Toast.LENGTH_SHORT).show();

            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                // connected to wifi
                return isWifiInternetAvailable();
            } else if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                // connected to the mobile provider's data plan
                return true;
            }
        }
        return false;
    }
    private static boolean isWifiInternetAvailable() {
        //adapted from https://stackoverflow.com/questions/43315393/android-internet-connection-timeout
        try {
            InetAddress ipAddr = InetAddress.getByName("google.com"); //You can replace it with your name
            return !ipAddr.toString().equals("");
        } catch (Exception e) {
            return false;
        }
    }
    public static List<Object> getResultsFromWeb(String word, final Activity activity) throws IOException {

        List<Object> setOf_matchingWordCharacteristics = new ArrayList<>();
        List<Object> matchingWordCharacteristics = new ArrayList<>();
        if (word.equals("")) { return setOf_matchingWordCharacteristics; }

        //Preparing the word to be included in the url
        String prepared_word = "";
        if (ConvertFragment.TextType(word).equals("kanji")) {
            String converted_word = convertToUTF8(word);
            converted_word = converted_word.substring(2,converted_word.length());
            prepared_word = "";
            for (int i = 0; i < converted_word.length() - 1; i = i + 2) {
                prepared_word = prepared_word + "%" + converted_word.substring(i, i + 2);
            }
        }
        else {
            prepared_word = word;
        }

        //Checking for a Web connection and extracting the site code if there is one. Otherwise, returning null.
        String responseString = "";
        String inputLine;
        HttpURLConnection connection = null;
        mInternetIsAvailable = internetIsAvailableCheck(activity.getBaseContext());
        TellUserIfThereIsNoInternetConnection(activity);
        if (mInternetIsAvailable) {
            try {
                //https://stackoverflow.com/questions/35568584/android-studio-deprecated-on-httpparams-httpconnectionparams-connmanagerparams
                //String current_url = "https://www.google.co.il/search?dcr=0&source=hp&q=" + prepared_word;
                String current_url = "http://jisho.org/search/" + prepared_word;
                URL dataUrl = new URL(current_url);
                connection = (HttpURLConnection) dataUrl.openConnection();
                connection.setConnectTimeout(2000);
                connection.setReadTimeout(2000);
                connection.setInstanceFollowRedirects(true);
                // optional default is GET
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    responseString = "";
                    while ((inputLine = in.readLine()) != null)
                        responseString += inputLine + '\n';
                    in.close();
                    in = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("Diagnosis Time", "Failed to access online resources.");
                if (Looper.myLooper() == null) Looper.prepare(); //Checks if the looper already exists. If this is the case, uses the old looper
                try {
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(activity.getBaseContext(), "Failed to access online resources.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
                return setOf_matchingWordCharacteristics;
            } finally {
                try {
                    if (connection != null) {connection.disconnect(); connection = null; }
                } catch (Exception e) {
                    e.printStackTrace(); //If you want further info on failure...
                    //return null;
                }
            }
        }

        String website_code = responseString;

        //Extracting the definition from Jisho.org

        //Initializatons
        String identifier;
        int index_of_current_results_block_marker_start;
        int index_of_current_results_block_marker_end;
        String current_results_block_descriptor;

        int index_of_requested_word_kanji_start;
        int index_of_requested_word_kanji_end;
        String requested_word_kanji = "";

        int index_of_requested_word_romaji_start;
        int index_of_requested_word_romaji_end;
        String requested_word_romaji = "";

        int index_of_current_meaning_tags_start;
        int index_of_current_meaning_tags_end;
        List<String> current_meaning_tags = new ArrayList<>();

        int index_of_meanings_start = 0;
        int index_of_meanings_end = 0;
        String meanings_semicolumns;
        String meanings_commas = "";

        int current_index_in_block;
        int current_meanings_block_start;
        int current_meanings_block_end;
        List<String> current_meanings;

        if (website_code.contains("Sorry, couldn't find anything matching")
                || website_code.contains("Sorry, couldn't find any words matching")
                || (website_code.contains("Searched for") && website_code.contains("No matches for"))) {
            return setOf_matchingWordCharacteristics;
        }

        int current_index_in_site = 0;
        current_index_in_block = website_code.length()-1;
        while (true) {

            //Get start of current results block
            identifier = "div class=\"concept_light-status";
            index_of_current_results_block_marker_start = website_code.indexOf(identifier, current_index_in_site) + identifier.length();
            index_of_current_results_block_marker_end = website_code.indexOf("a class=\"light-details_link", index_of_current_results_block_marker_start);

            //Getting the start index of the current meaning block
            identifier = "concept_light-meanings medium-9 columns";
            current_meanings_block_start = website_code.indexOf(identifier, index_of_current_results_block_marker_start) + identifier.length();
            current_meanings_block_end = website_code.indexOf("a class=\"light-details_link", current_meanings_block_start);

            //Exiting the loop if the last entry from the website has been registered or if the loop restarts
            if (index_of_current_results_block_marker_start < current_index_in_site || index_of_current_results_block_marker_start == -1) break;

            //Skipping this results block if it is not a valid block
            //index_of_current_results_block_marker_end = website_code.indexOf(">", index_of_current_results_block_marker_start)-1;
            //current_results_block_descriptor = website_code.substring(index_of_current_results_block_marker_start, index_of_current_results_block_marker_end);
            //if (!current_results_block_descriptor.equals("concept_light-status")) continue;

            current_index_in_site = index_of_current_results_block_marker_start;

            //Kanji extraction
            identifier = "Sentence search for ";
            index_of_requested_word_kanji_start = website_code.indexOf(identifier, current_index_in_site) + identifier.length();
            index_of_requested_word_kanji_end = website_code.indexOf("<", index_of_requested_word_kanji_start);
            requested_word_kanji = website_code.substring(index_of_requested_word_kanji_start, index_of_requested_word_kanji_end);

            //Romaji extraction (if there is no second "Sentence search for " marker, create the Romaji value manually)
            identifier = "Sentence search for ";
            index_of_requested_word_romaji_start = website_code.indexOf(identifier, index_of_requested_word_kanji_end) + identifier.length();

            //If there is not further "Sentence search for " statement in the website, continue (to loop closure)
            if (index_of_requested_word_romaji_start == -1) { current_index_in_site = index_of_requested_word_romaji_start; continue; }

            //Otherwise:
            index_of_requested_word_romaji_end = index_of_requested_word_romaji_start;
            if (index_of_requested_word_romaji_start > current_meanings_block_start) {
                requested_word_romaji = ConvertFragment.Kana_to_Romaji_to_Kana(requested_word_kanji).get(0);
            }
            else {
                index_of_requested_word_romaji_end = website_code.indexOf("<", index_of_requested_word_romaji_start);
                requested_word_romaji = website_code.substring(index_of_requested_word_romaji_start, index_of_requested_word_romaji_end);
            }

            //If the Kanji word is in Hiragana or Katakana script, make sure that the Romaji value is correct, no matter what is extracted from the website
            if (ConvertFragment.TextType(requested_word_kanji).equals("hiragana") ||
                    ConvertFragment.TextType(requested_word_kanji).equals("katakana") ) {
                requested_word_romaji = ConvertFragment.Kana_to_Romaji_to_Kana(requested_word_kanji).get(0);
            }

            current_index_in_site = current_meanings_block_start;

            //Getting the tags and corresponding meanings
            current_index_in_block = current_meanings_block_start;
            current_meaning_tags = new ArrayList<>();
            current_meanings = new ArrayList<>();
            long index_of_next_meaning_tags_start = current_index_in_block;
            long index_of_next_meaning_tags_end = current_index_in_block;
            String next_meaning_tag;
            meanings_commas = "";
            String current_meaning_tag = "";

            while(current_index_in_block < current_meanings_block_end) {

                identifier = "<div class=\"meaning-tags\">";
                index_of_current_meaning_tags_start = website_code.indexOf(identifier, current_index_in_block) + identifier.length();
                index_of_current_meaning_tags_end = website_code.indexOf("<", index_of_current_meaning_tags_start);
                current_meaning_tag = SharedMethods.fromHtml(website_code.substring(index_of_current_meaning_tags_start, index_of_current_meaning_tags_end)).toString();

                if (current_meaning_tag.contains("Wikipedia") || current_meaning_tag.contains("Other forms") || current_meaning_tag.contains("Notes")) break;
                if (index_of_current_meaning_tags_end < current_index_in_block) break;

                index_of_next_meaning_tags_start = website_code.indexOf(identifier, index_of_current_meaning_tags_end) + identifier.length()+1;
                index_of_next_meaning_tags_end = website_code.indexOf("<", index_of_current_meaning_tags_start);
                next_meaning_tag = SharedMethods.fromHtml(website_code.substring(index_of_current_meaning_tags_start, index_of_current_meaning_tags_end)).toString();

                current_index_in_block = index_of_current_meaning_tags_end;

                //Getting the meanings for the current tag
                while (current_index_in_block < index_of_next_meaning_tags_start && current_index_in_block < current_meanings_block_end) {

                    identifier = "<span class=\"meaning-meaning\">";
                    index_of_meanings_start = website_code.indexOf(identifier, current_index_in_block) + identifier.length();
                    index_of_meanings_end = website_code.indexOf("<", index_of_meanings_start);
                    meanings_semicolumns = SharedMethods.fromHtml(website_code.substring(index_of_meanings_start, index_of_meanings_end)).toString();

                    if (index_of_meanings_start > index_of_next_meaning_tags_start) break;

                    if (!meanings_semicolumns.equals("")) {
                        meanings_commas = "";
                        for (int i = 0; i < meanings_semicolumns.length(); i++) {
                            if (meanings_semicolumns.substring(i, i + 1).equals(";")) { meanings_commas += ","; }
                            else { meanings_commas += meanings_semicolumns.substring(i, i + 1); }
                        }
                        meanings_commas = SharedMethods.fromHtml(meanings_commas).toString();
                        meanings_commas = meanings_commas.replaceAll("',", "'");

                        current_meaning_tags.add(current_meaning_tag);
                        current_meanings.add(meanings_commas);
                    }
                    current_index_in_block = index_of_meanings_end;
                }

                if (current_index_in_block < current_meanings_block_start) break;
            }

            //Preventing crashes if the website code does not supply a "meaning-tags" instance in the current block
            if (current_meaning_tags.size() == 0) {
                current_index_in_site = current_index_in_block;
                continue;
            }

            current_index_in_site = current_meanings_block_end;

            //Updating the characteristics
            matchingWordCharacteristics = new ArrayList<>();

            //Getting the Romaji value
            String matchingWordRomaji = ConvertFragment.Kana_to_Romaji_to_Kana(requested_word_romaji).get(0);
            matchingWordCharacteristics.add(matchingWordRomaji);

            //Getting the Kanji value
            String matchingWordKanji = requested_word_kanji;
            matchingWordCharacteristics.add(matchingWordKanji);

            //Getting the Alt Spellings value
            String matchingWordAltSpellings = "";
            matchingWordCharacteristics.add(matchingWordAltSpellings);

            //Getting the set of Meanings

            //Initializations
            List<Object> matchingWordCurrentMeaningsBlock = new ArrayList<>();
            List<Object> matchingWordCurrentMeaningBlocks = new ArrayList<>();
            String matchingWordMeaning;
            String matchingWordType;
            String matchingWordOpposite;
            String matchingWordSynonym;

            List<List<String>> matchingWordExplanationBlocks = new ArrayList<>();
            List<String> matchingWordCurrentExplanationsBlock;
            String matchingWordExplanation;
            String matchingWordRules;

            for (int i=0; i<current_meanings.size(); i++) {

                matchingWordCurrentMeaningsBlock = new ArrayList<>();

                //Getting the Meaning value
                matchingWordMeaning = current_meanings.get(i);
                matchingWordCurrentMeaningsBlock.add(matchingWordMeaning);

                //Getting the Type value
                matchingWordType = current_meaning_tags.get(i);
                if (matchingWordType.contains("verb")) {
                    if      (matchingWordType.contains("su ending")) {
                        if (matchingWordType.contains("intransitive")) matchingWordType = "VsuI";
                        if (matchingWordType.contains("Transitive")) matchingWordType = "VsuT";
                        else matchingWordType = "VsuI";
                    }
                    else if (matchingWordType.contains("ku ending")) {
                        if (matchingWordType.contains("intransitive")) matchingWordType = "VkuI";
                        if (matchingWordType.contains("Transitive")) matchingWordType = "VkuT";
                        else matchingWordType = "VkuI";
                    }
                    else if (matchingWordType.contains("gu ending")) {
                        if (matchingWordType.contains("intransitive")) matchingWordType = "VguI";
                        if (matchingWordType.contains("Transitive")) matchingWordType = "VguT";
                        else matchingWordType = "VguI";
                    }
                    else if (matchingWordType.contains("mu ending")) {
                        if (matchingWordType.contains("intransitive")) matchingWordType = "VmuI";
                        if (matchingWordType.contains("Transitive")) matchingWordType = "VmuT";
                        else matchingWordType = "VmuI";
                    }
                    else if (matchingWordType.contains("bu ending")) {
                        if (matchingWordType.contains("intransitive")) matchingWordType = "VbuI";
                        if (matchingWordType.contains("Transitive")) matchingWordType = "VbuT";
                        else matchingWordType = "VbuI";
                    }
                    else if (matchingWordType.contains("nu ending")) {
                        if (matchingWordType.contains("intransitive")) matchingWordType = "VnuI";
                        if (matchingWordType.contains("Transitive")) matchingWordType = "VnuT";
                        else matchingWordType = "VnuI";
                    }
                    else if (matchingWordType.contains("ru ending")) {
                        if (matchingWordType.contains("intransitive")) matchingWordType = "VrugI";
                        if (matchingWordType.contains("Transitive")) matchingWordType = "VrugT";
                        else matchingWordType = "VrugI";
                    }
                    else if (matchingWordType.contains("tsu ending")) {
                        if (matchingWordType.contains("intransitive")) matchingWordType = "VtsuI";
                        if (matchingWordType.contains("Transitive")) matchingWordType = "VtsuT";
                        else matchingWordType = "VtsuI";
                    }
                    else if (matchingWordType.contains("u ending")) {
                        if (matchingWordType.contains("intransitive")) matchingWordType = "VuI";
                        if (matchingWordType.contains("Transitive")) matchingWordType = "VuT";
                        else matchingWordType = "VuI";
                    }
                    else if (matchingWordType.contains("Ichidan")) {
                        if (matchingWordType.contains("intransitive")) matchingWordType = "VruiI";
                        if (matchingWordType.contains("Transitive")) matchingWordType = "VruiT";
                        else matchingWordType = "VruiI";
                    }
                }
                matchingWordCurrentMeaningsBlock.add(matchingWordType);

                //Getting the Opposite value
                matchingWordOpposite = "";
                matchingWordCurrentMeaningsBlock.add(matchingWordOpposite);

                //Getting the Synonym value
                matchingWordSynonym = "";
                matchingWordCurrentMeaningsBlock.add(matchingWordSynonym);

                //Getting the set of Explanations
                matchingWordCurrentExplanationsBlock = new ArrayList<>();

                //Getting the Explanation value
                matchingWordExplanation = "";
                matchingWordCurrentExplanationsBlock.add(matchingWordExplanation);

                //Getting the Rules value
                matchingWordRules = "";
                matchingWordCurrentExplanationsBlock.add(matchingWordRules);

                matchingWordExplanationBlocks.add(matchingWordCurrentExplanationsBlock);

                matchingWordCurrentMeaningsBlock.add(matchingWordExplanationBlocks);
                matchingWordCurrentMeaningBlocks.add(matchingWordCurrentMeaningsBlock);
            }

            matchingWordCharacteristics.add(matchingWordCurrentMeaningBlocks);

            setOf_matchingWordCharacteristics.add(matchingWordCharacteristics);
        }

        return setOf_matchingWordCharacteristics;
    }



}
