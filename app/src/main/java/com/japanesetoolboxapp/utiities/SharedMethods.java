package com.japanesetoolboxapp.utiities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.japanesetoolboxapp.ConvertFragment;
import com.japanesetoolboxapp.R;
import com.japanesetoolboxapp.data.Word;

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

    //Activity operation functions
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
    private static boolean deleteDir(File dir) {
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
        return dir != null && dir.delete();
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
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =(InputMethodManager) activity.getBaseContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && activity.getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    //Character manipulations module
    private static String convertToUTF8(String input_string) {

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
    public static String removeSpecialCharacters(String sentence) {
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
    @SuppressWarnings("deprecation") public static Spanned fromHtml(String source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(source);
        }
    }

    //OCR module
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
    private static float truncateToRange(float value, float min, float max) {
        if (value < min) value = min;
        else if (value > max) value = max;
        return value;
    }
    public static float convertContrastProgressToValue(float contrastBarValue, Context context) {
        return contrastBarValue
                /((float) Integer.parseInt(context.getString(R.string.pref_OCR_image_contrast_range)))
                *((float) Integer.parseInt(context.getString(R.string.pref_OCR_image_contrast_max_value)));
    }
    public static float convertSaturationProgressToValue(float saturationBarValue, Context context) {
        return saturationBarValue
                /((float) Integer.parseInt(context.getString(R.string.pref_OCR_image_saturation_range)))
                *((float) Integer.parseInt(context.getString(R.string.pref_OCR_image_saturation_multipliers)));
    }
    public static int convertBrightnessProgressToValue(int brightnessBarValue, Context context) {
        return brightnessBarValue-256;
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
        return brightnessValue+256;
    }

    //Internet Connectivity functions
    private static Boolean mInternetIsAvailable;
    public static boolean internetIsAvailableCheck(Context context) {
        //adapted from https://stackoverflow.com/questions/43315393/android-internet-connection-timeout
        final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr == null) return false;

        NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();

        if (activeNetworkInfo != null) { // connected to the internet
            //Toast.makeText(context, activeNetworkInfo.getTypeName(), Toast.LENGTH_SHORT).show();

            if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                // connected to wifi
                return isWifiInternetAvailable();
            } else return activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE;
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
    public static List<Object> getResultsFromJishoOnWeb(String word, final Activity activity) {

        List<Object> setOf_matchingWordCharacteristics = new ArrayList<>();
        if (word.equals("")) { return setOf_matchingWordCharacteristics; }

        //region Preparing the word to be included in the url
        String prepared_word;
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
        //endregion

        //region Getting the Jisho.org ebsite code
        String website_code = getWebsiteXml(activity.getResources().getString(R.string.jisho_website_url) + prepared_word, activity);
        if ((website_code != null && website_code.equals("")) || website_code == null) return setOf_matchingWordCharacteristics;

        if (website_code.length() == 0
                ||website_code.contains("Sorry, couldn't find anything matching")
                || website_code.contains("Sorry, couldn't find any words matching")
                || (website_code.contains("Searched for") && website_code.contains("No matches for"))) {
            return setOf_matchingWordCharacteristics;
        }
        //endregion

        //region Parsing the website code to match the app's results tree
        List<Object> parsedData = parseJishoWebsiteToTree(website_code);
        setOf_matchingWordCharacteristics = adaptJishoTreeToResultsTree(parsedData);
        //setOf_matchingWordCharacteristics = parseAndAdaptJishoWebsiteToResultsTree(website_code);
        //endregion

        return setOf_matchingWordCharacteristics;
    }
    public static List<Word> getWordsFromJishoOnWeb(String word, final Context context) {

        if (word.equals("")) { return new ArrayList<>(); }

        //region Preparing the word to be included in the url
        String prepared_word;
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
        //endregion

        //Getting the Jisho.org website code
        String website_code = getWebsiteXml(context.getResources().getString(R.string.jisho_website_url) + prepared_word, context);

        //Returning nothing if there was a problem getting results
        if ((website_code != null && website_code.equals(""))
                || website_code == null
                || website_code.length() == 0
                || website_code.contains("Sorry, couldn't find anything matching")
                || website_code.contains("Sorry, couldn't find any words matching")
                || (website_code.contains("Searched for") && website_code.contains("No matches for"))) {
            return new ArrayList<>();
        }

        //Parsing the website code and mapping it to a List<Word>
        List<Object> parsedData = parseJishoWebsiteToTree(website_code);
        List<Word> wordsList = adaptJishoTreeToWordsList(parsedData);

        return wordsList;
    }
    private static String getWebsiteXml(String websiteUrl, final Context context) {

        String responseString = "";
        String inputLine;
        HttpURLConnection connection = null;
        mInternetIsAvailable = internetIsAvailableCheck(context);

        if (!mInternetIsAvailable) return responseString;

        try {
            //https://stackoverflow.com/questions/35568584/android-studio-deprecated-on-httpparams-httpconnectionparams-connmanagerparams
            //String current_url = "https://www.google.co.il/search?dcr=0&source=hp&q=" + prepared_word;
            URL dataUrl = new URL(websiteUrl);
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
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("Diagnosis Time", "Failed to access online resources.");
            return null;
        } finally {
            try {
                if (connection != null) {connection.disconnect(); }
            } catch (Exception e) {
                e.printStackTrace(); //If you want further info on failure...
            }
        }
        return responseString;
    }
    private static List<Object> parseJishoWebsiteToTree(String website_code) {

        runningIndex = 0;
        int initial_offset = 15; //Skips <!DOCTYPE html>
        websiteCodeString = website_code.substring(initial_offset, website_code.length());
        List<Object> parsedWebsiteTree = getChildren();

        return parsedWebsiteTree;
    }
    private static int runningIndex = 0;
    private static String websiteCodeString = "";
    private static List<Object> getChildren() {

        List<Object> currentParent = new ArrayList<>();
        if (runningIndex > websiteCodeString.length()-1) {
            currentParent.add("");
            return currentParent;
        }
        String remainingwebsiteCodeString = websiteCodeString.substring(runningIndex,websiteCodeString.length());

        if (!remainingwebsiteCodeString.contains("<")) {
            currentParent.add(remainingwebsiteCodeString);
            return currentParent;
        }

        while (0 <= runningIndex && runningIndex < websiteCodeString.length()) {

            //Getting the next header characteristics
            int nextHeaderStart = websiteCodeString.indexOf("<", runningIndex);
            if (nextHeaderStart==-1) return currentParent;
            int nextHeaderEnd = websiteCodeString.indexOf(">", nextHeaderStart);
            String currentHeader = websiteCodeString.substring(nextHeaderStart + 1, nextHeaderEnd);

            //Log.i("Diagnosis Time", "Current child: " + runningIndex + ", " + currentHeader);

            //If there is String text before the next header, add it to the list and continue to the header
            if (nextHeaderStart != runningIndex) {
                String currentText = websiteCodeString.substring(runningIndex, nextHeaderStart);
                StringBuilder validText = new StringBuilder("");
                for (int i=0; i<currentText.length(); i++) {
                    if (i<currentText.length()-1 && currentText.substring(i,i+1).equals("\n")) { i++; continue;}
                    validText.append(currentText.charAt(i));
                }
                String validTextString = validText.toString().trim();
//                boolean isOnlyWhiteSpace = true;
//                for (int i=0; i<validTextString.length(); i++) {
//                    if (!Character.isWhitespace(validTextString.charAt(i))) {isOnlyWhiteSpace = false; break;}
//                }
                if (!TextUtils.isEmpty(validTextString)) currentParent.add(validTextString);
                runningIndex = nextHeaderStart;
            }

            //If the header is of type "<XXX/>" then there is no subtree. In this case add the header to the tree and move to next subtree.
            if (websiteCodeString.substring(nextHeaderEnd - 1, nextHeaderEnd + 1).equals("/>")) {
                currentParent.add(currentHeader);
                runningIndex = nextHeaderEnd + 1;
            }

            //If the header is of type "<XXX>" then:
            // - if the header is <br> there is no substree and the header should be treated as text
            else if (currentHeader.equals("br")) {
                currentParent.add("<br>");
                runningIndex = nextHeaderEnd + 1;
            }
            // - if the header is a tail, move up the stack
            else if (currentHeader.substring(0,1).equals("/")) {
                int endOfTail = websiteCodeString.indexOf(">", nextHeaderStart);
                runningIndex = endOfTail+1;
                return currentParent;
            }
            // - if the header is <!-- XXX> then this is a comment and should be ignored
            else if (currentHeader.contains("!--")) {
                int endOfComment = websiteCodeString.indexOf("-->", runningIndex);
                runningIndex = endOfComment+3;
            }
            //If the subtree is valid and is not the <head> subtree, add it to the tree
            else if (currentHeader.equals("head")) {
                currentParent.add(currentHeader);
                currentParent.add("");
                runningIndex = websiteCodeString.indexOf("</head>") + 7;
            }
            // - if the header is not <br> then there is a subtree and the methods recurses
            else {
                currentParent.add(currentHeader);
                runningIndex = nextHeaderEnd+1;
                List<Object> subtree = getChildren();
                currentParent.add(subtree);
            }

        }

        return currentParent;
    }
    private static List<Word> adaptJishoTreeToWordsList(List<Object> parsedData) {

        //region Getting to the relevant tree section
        if (parsedData.size()<1) return new ArrayList<>();
        List<Object> htmlData = (List<Object>) parsedData.get(1);
        List<Object> bodyData = (List<Object>) htmlData.get(3);
        List<Object> pageContainerData = (List<Object>) getElementAtHeader(bodyData,"page_container");
        if (pageContainerData==null) return new ArrayList<>();
        List<Object> large12ColumnsData = (List<Object>) getElementAtHeader(pageContainerData,"large-12 columns");
        if (large12ColumnsData==null) return new ArrayList<>();
        List<Object> mainResultsData = (List<Object>) getElementAtHeader(large12ColumnsData,"main_results");
        if (mainResultsData==null) return new ArrayList<>();
        List<Object> rowData = (List<Object>) getElementAtHeader(mainResultsData,"row");
        if (rowData==null) return new ArrayList<>();
        List<Object> primaryData = (List<Object>) getElementAtHeader(rowData,"primary");
        if (primaryData==null) return new ArrayList<>();
        List<Object> exactBlockData = (List<Object>) getElementAtHeader(primaryData,"exact_block");
        if (exactBlockData==null) return new ArrayList<>();
        //endregion

        //Extracting the list of hits
        String kanji;
        String romaji;
        List<String> meaningTagsFromTree;
        List<String> meaningsFromTree;
        List<Word> wordsList = new ArrayList<>();

        for (int i=3; i<exactBlockData.size(); i=i+2) {

            Word currentWord = new Word();

            List<Object> conceptLightClearFixData = (List<Object>) exactBlockData.get(i);

            //region Extracting the Romaji, Kanji and Altternate Spellings
            List<Object> conceptLightWrapperData = (List<Object>) conceptLightClearFixData.get(1);
            List<Object> conceptLightReadingsData = (List<Object>) conceptLightWrapperData.get(1);
            List<Object> conceptLightRepresentationData = (List<Object>) conceptLightReadingsData.get(1);

            romaji = "";
            List<Object> furiganaData = (List<Object>) conceptLightRepresentationData.get(1);
            for (int j=1; j<furiganaData.size(); j=j+2) {
                List<Object> kanji1UpData = (List<Object>) furiganaData.get(j);
                if (kanji1UpData.size()>0) romaji += (String) kanji1UpData.get(0);
            }

            kanji = "";
            List<Object> TextData = (List<Object>) getElementAtHeader(conceptLightRepresentationData,"text");
            if (TextData!=null && TextData.size()>1) {
                kanji = "";
                for (int j=0; j<TextData.size(); j++) {
                    String currentText;
                    if (TextData.get(j) instanceof List) {
                        List<Object> list = (List<Object>) TextData.get(j);
                        currentText = (String) list.get(0);
                    }
                    else {
                        currentText = (String) TextData.get(j);
                        if (currentText.equals("span")) currentText = "";
                    }
                    kanji += currentText;
                }
            }
            else if (TextData!=null && TextData.size()>0) kanji = (String) TextData.get(0);

            if (romaji.length()!=0 &&
                    (ConvertFragment.TextType(kanji).equals("katakana") || ConvertFragment.TextType(kanji).equals("hiragana"))) {
                //When the word is originally katakana only, the website does not display hiragana. This is corrected here.
                romaji = ConvertFragment.Kana_to_Romaji_to_Kana(kanji).get(0);
            }

            List<Object> conceptLightStatusData = (List<Object>) getElementAtHeader(conceptLightWrapperData,"concept_light-status");
            if (conceptLightStatusData!=null) {
                List<Object> ulClassData = (List<Object>) getElementAtHeader(conceptLightStatusData, "ul class");
                if (ulClassData != null) {
                    for (int j = 1; j < ulClassData.size(); j = j + 2) {
                        List<Object> li = (List<Object>) ulClassData.get(j);
                        List<Object> aRef = (List<Object>) li.get(1);
                        String sentenceSearchFor = (String) aRef.get(0);
                        String currentValue = "";
                        if (sentenceSearchFor.length() > 20 && sentenceSearchFor.contains("Sentence search for")) {
                            currentValue = sentenceSearchFor.substring(20, sentenceSearchFor.length());
                        }
                        if (currentValue.length() != 0 &&
                                (ConvertFragment.TextType(currentValue).equals("katakana") || ConvertFragment.TextType(currentValue).equals("hiragana"))) {
                            //When the word is originally katakana only, the website does not display hiragana. This is corrected here.
                            romaji = ConvertFragment.Kana_to_Romaji_to_Kana(currentValue).get(0);
                            break;
                        }
                    }
                }
            }
            //If romaji data not found at the current tree node, try one node above it
            conceptLightStatusData = (List<Object>) getElementAtHeader(conceptLightClearFixData,"concept_light-status");
            if (conceptLightStatusData!=null) {
                List<Object> ulClassData = (List<Object>) getElementAtHeader(conceptLightStatusData, "ul class");
                if (ulClassData != null) {
                    for (int j = 1; j < ulClassData.size(); j = j + 2) {
                        List<Object> li = (List<Object>) ulClassData.get(j);
                        List<Object> aRef = (List<Object>) li.get(1);
                        String sentenceSearchFor = (String) aRef.get(0);
                        String currentValue = "";
                        if (sentenceSearchFor.length() > 20 && sentenceSearchFor.contains("Sentence search for")) {
                            currentValue = sentenceSearchFor.substring(20, sentenceSearchFor.length());
                        }
                        if (currentValue.length() != 0 &&
                                (ConvertFragment.TextType(currentValue).equals("katakana") || ConvertFragment.TextType(currentValue).equals("hiragana"))) {
                            //When the word is originally katakana only, the website does not display hiragana. This is corrected here.
                            romaji = ConvertFragment.Kana_to_Romaji_to_Kana(currentValue).get(0);
                            break;
                        }
                    }
                }
            }

            currentWord.setRomaji(ConvertFragment.Kana_to_Romaji_to_Kana(romaji).get(0));
            currentWord.setKanji(kanji);
            currentWord.setAltSpellings(""); //Alternate spellings, left empty for now >>> TODO: add this functionality
            //endregion

            //region Extracting the meanings

            List<Object> conceptLightMeaningsData = (List<Object>) getElementAtHeader(conceptLightClearFixData,"concept_light-meanings medium-9 columns");
            if (conceptLightMeaningsData==null) continue;
            List<Object> meaningsWrapperData = (List<Object>) conceptLightMeaningsData.get(1);

            String currentHeader = "";
            String meaningTag = "";
            String meaning;
            meaningTagsFromTree = new ArrayList<>();
            meaningsFromTree = new ArrayList<>();
            for (int j=0; j<meaningsWrapperData.size(); j++) {

                if (j%2==0) { currentHeader = (String) meaningsWrapperData.get(j); continue;}

                if (currentHeader.contains("meaning-tags")) {
                    List<Object> meaningsTagsData = (List<Object>) meaningsWrapperData.get(j);
                    meaningTag = "";
                    if (meaningsTagsData.size()>0) meaningTag = (String) meaningsTagsData.get(0);
                    if (meaningTag.contains("Wikipedia") || meaningTag.contains("Other forms") || meaningTag.contains("Notes")) break;
                }
                if (currentHeader.contains("meaning-wrapper")) {
                    List<Object> meaningWrapperData = (List<Object>) meaningsWrapperData.get(j);
                    List<Object> meaningDefinitionData = (List<Object>) meaningWrapperData.get(1);
                    List<Object> meaningMeaningata = (List<Object>) getElementAtHeader(meaningDefinitionData,"meaning-meaning");
                    meaningTagsFromTree.add(meaningTag);
                    meaning = "";
                    if (meaningMeaningata!=null && meaningMeaningata.size()>0) meaning = (String) meaningMeaningata.get(0);
                    meaningsFromTree.add(reformatMeanings(meaning));
                }
            }

            List<Word.Meaning> wordMeaningsList = new ArrayList<>();
            for (int j=0; j<meaningsFromTree.size(); j++) {

                Word.Meaning wordMeaning = new Word.Meaning();

                //Getting the Meaning value
                String matchingWordMeaning = meaningsFromTree.get(j);
                wordMeaning.setMeaning(matchingWordMeaning);

                //Getting the Type value
                String matchingWordType = meaningTagsFromTree.get(j);
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
                wordMeaning.setType(matchingWordType);

                //Getting the Opposite value
                String matchingWordOpposite = ""; //TODO: See if this can be extracted from the site
                wordMeaning.setAntonym(matchingWordOpposite);

                //Getting the Synonym value
                String matchingWordSynonym = ""; //TODO: See if this can be extracted from the site
                wordMeaning.setSynonym(matchingWordSynonym);

                //Getting the set of Explanations
                List<Word.Meaning.Explanation> explanationsList = new ArrayList<>();
                Word.Meaning.Explanation explanation = new Word.Meaning.Explanation();

                //Getting the Explanation value
                String matchingWordExplanation = "";
                explanation.setExplanation(matchingWordExplanation);

                //Getting the Rules value
                String matchingWordRules = "";
                explanation.setRules(matchingWordRules);

                //Getting the examples
                List<Word.Meaning.Explanation.Example> examplesList = new ArrayList<>();
                explanation.setExamples(examplesList);

                explanationsList.add(explanation);

                wordMeaning.setExplanations(explanationsList);
                wordMeaningsList.add(wordMeaning);
            }

            currentWord.setMeanings(wordMeaningsList);
            //endregion

            wordsList.add(currentWord);
        }

        return wordsList;
    }
    private static List<Object> adaptJishoTreeToResultsTree(List<Object> parsedData) {

        //Getting to the relevant tree section
        if (parsedData.size()<1) return new ArrayList<>();
        List<Object> htmlData = (List<Object>) parsedData.get(1);
        List<Object> bodyData = (List<Object>) htmlData.get(3);
        List<Object> pageContainerData = (List<Object>) getElementAtHeader(bodyData,"page_container");
        if (pageContainerData==null) return new ArrayList<>();
        List<Object> large12ColumnsData = (List<Object>) getElementAtHeader(pageContainerData,"large-12 columns");
        if (large12ColumnsData==null) return new ArrayList<>();
        List<Object> mainResultsData = (List<Object>) getElementAtHeader(large12ColumnsData,"main_results");
        if (mainResultsData==null) return new ArrayList<>();
        List<Object> rowData = (List<Object>) getElementAtHeader(mainResultsData,"row");
        if (rowData==null) return new ArrayList<>();
        List<Object> primaryData = (List<Object>) getElementAtHeader(rowData,"primary");
        if (primaryData==null) return new ArrayList<>();
        List<Object> exactBlockData = (List<Object>) getElementAtHeader(primaryData,"exact_block");
        if (exactBlockData==null) return new ArrayList<>();

        //Extracting the list of hits
        String kanji;
        String romaji;
        List<String> meaningTags;
        List<String> meanings;

        List<Object> setOf_matchingWordCharacteristics = new ArrayList<>();

        for (int i=3; i<exactBlockData.size(); i=i+2) {

            List<Object> currentMatchingWordCharacteristics = new ArrayList<>();

            List<Object> conceptLightClearFixData = (List<Object>) exactBlockData.get(i);

            //Extracting the romaji and Kanji
            List<Object> conceptLightWrapperData = (List<Object>) conceptLightClearFixData.get(1);

            List<Object> conceptLightReadingsData = (List<Object>) conceptLightWrapperData.get(1);
            List<Object> conceptLightRepresentationData = (List<Object>) conceptLightReadingsData.get(1);

            romaji = "";
            List<Object> furiganaData = (List<Object>) conceptLightRepresentationData.get(1);
            for (int j=1; j<furiganaData.size(); j=j+2) {
                List<Object> kanji1UpData = (List<Object>) furiganaData.get(j);
                if (kanji1UpData.size()>0) romaji += (String) kanji1UpData.get(0);
            }

            kanji = "";
            List<Object> TextData = (List<Object>) getElementAtHeader(conceptLightRepresentationData,"text");
            if (TextData!=null && TextData.size()>1) {
                kanji = "";
                for (int j=0; j<TextData.size(); j++) {
                    String currentText;
                    if (TextData.get(j) instanceof List) {
                        List<Object> list = (List<Object>) TextData.get(j);
                        currentText = (String) list.get(0);
                    }
                    else {
                        currentText = (String) TextData.get(j);
                        if (currentText.equals("span")) currentText = "";
                    }
                    kanji += currentText;
                }
            }
            else if (TextData!=null && TextData.size()>0) kanji = (String) TextData.get(0);

            if (romaji.length()!=0 &&
                    (ConvertFragment.TextType(kanji).equals("katakana") || ConvertFragment.TextType(kanji).equals("hiragana"))) {
                //When the word is originally katakana only, the website does not display hiragana. This is corrected here.
                romaji = ConvertFragment.Kana_to_Romaji_to_Kana(kanji).get(0);
            }

            List<Object> conceptLightStatusData = (List<Object>) getElementAtHeader(conceptLightWrapperData,"concept_light-status");
            if (conceptLightStatusData!=null) {
                List<Object> ulClassData = (List<Object>) getElementAtHeader(conceptLightStatusData, "ul class");
                if (ulClassData != null) {
                    for (int j = 1; j < ulClassData.size(); j = j + 2) {
                        List<Object> li = (List<Object>) ulClassData.get(j);
                        List<Object> aRef = (List<Object>) li.get(1);
                        String sentenceSearchFor = (String) aRef.get(0);
                        String currentValue = "";
                        if (sentenceSearchFor.length() > 20 && sentenceSearchFor.contains("Sentence search for")) {
                            currentValue = sentenceSearchFor.substring(20, sentenceSearchFor.length());
                        }
                        if (currentValue.length() != 0 &&
                                (ConvertFragment.TextType(currentValue).equals("katakana") || ConvertFragment.TextType(currentValue).equals("hiragana"))) {
                            //When the word is originally katakana only, the website does not display hiragana. This is corrected here.
                            romaji = ConvertFragment.Kana_to_Romaji_to_Kana(currentValue).get(0);
                            break;
                        }
                    }
                }
            }
            //If romaji data not found at the current tree node, try one node above it
            conceptLightStatusData = (List<Object>) getElementAtHeader(conceptLightClearFixData,"concept_light-status");
            if (conceptLightStatusData!=null) {
                List<Object> ulClassData = (List<Object>) getElementAtHeader(conceptLightStatusData, "ul class");
                if (ulClassData != null) {
                    for (int j = 1; j < ulClassData.size(); j = j + 2) {
                        List<Object> li = (List<Object>) ulClassData.get(j);
                        List<Object> aRef = (List<Object>) li.get(1);
                        String sentenceSearchFor = (String) aRef.get(0);
                        String currentValue = "";
                        if (sentenceSearchFor.length() > 20 && sentenceSearchFor.contains("Sentence search for")) {
                            currentValue = sentenceSearchFor.substring(20, sentenceSearchFor.length());
                        }
                        if (currentValue.length() != 0 &&
                                (ConvertFragment.TextType(currentValue).equals("katakana") || ConvertFragment.TextType(currentValue).equals("hiragana"))) {
                            //When the word is originally katakana only, the website does not display hiragana. This is corrected here.
                            romaji = ConvertFragment.Kana_to_Romaji_to_Kana(currentValue).get(0);
                            break;
                        }
                    }
                }
            }

            //Extracting the meanings

            List<Object> conceptLightMeaningsData = (List<Object>) getElementAtHeader(conceptLightClearFixData,"concept_light-meanings medium-9 columns");
            if (conceptLightMeaningsData==null) continue;
            List<Object> meaningsWrapperData = (List<Object>) conceptLightMeaningsData.get(1);

            String currentHeader = "";
            String meaningTag = "";
            String meaning;
            meaningTags = new ArrayList<>();
            meanings = new ArrayList<>();
            for (int j=0; j<meaningsWrapperData.size(); j++) {

                //currentMeaningsWrapper = (List<Object>) meaningsWrapperData.get(j);
                //currentHeader = (String) currentMeaningsWrapper.get(j);
                if (j%2==0) { currentHeader = (String) meaningsWrapperData.get(j); continue;}

                if (currentHeader.contains("meaning-tags")) {
                    List<Object> meaningsTagsData = (List<Object>) meaningsWrapperData.get(j);
                    meaningTag = "";
                    if (meaningsTagsData.size()>0) meaningTag = (String) meaningsTagsData.get(0);
                    if (meaningTag.contains("Wikipedia") || meaningTag.contains("Other forms") || meaningTag.contains("Notes")) break;
                }
                if (currentHeader.contains("meaning-wrapper")) {
                    List<Object> meaningWrapperData = (List<Object>) meaningsWrapperData.get(j);
                    List<Object> meaningDefinitionData = (List<Object>) meaningWrapperData.get(1);
                    List<Object> meaningMeaningata = (List<Object>) getElementAtHeader(meaningDefinitionData,"meaning-meaning");
                    meaningTags.add(meaningTag);
                    meaning = "";
                    if (meaningMeaningata!=null && meaningMeaningata.size()>0) meaning = (String) meaningMeaningata.get(0);
                    meanings.add(reformatMeanings(meaning));
                }
            }

            currentMatchingWordCharacteristics.add(ConvertFragment.Kana_to_Romaji_to_Kana(romaji).get(0));
            currentMatchingWordCharacteristics.add(kanji);
            currentMatchingWordCharacteristics.add(""); //Alternate spellings, left empty for now >>> TODO: add this functionality

            List<Object> matchingWordCurrentMeaningBlocks = new ArrayList<>();
            List<Object> matchingWordCurrentMeaningsBlock;
            List<List<String>> matchingWordExplanationBlocks = new ArrayList<>();
            List<String> matchingWordCurrentExplanationsBlock;
            String matchingWordMeaning;
            String matchingWordType;
            String matchingWordOpposite;
            String matchingWordSynonym;
            String matchingWordExplanation;
            String matchingWordRules;
            for (int j=0; j<meanings.size(); j++) {

                matchingWordCurrentMeaningsBlock = new ArrayList<>();

                //Getting the Meaning value
                matchingWordMeaning = meanings.get(j);
                matchingWordCurrentMeaningsBlock.add(matchingWordMeaning);

                //Getting the Type value
                matchingWordType = meaningTags.get(j);
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
                matchingWordOpposite = ""; //TODO: See if this can be extracted from the site
                matchingWordCurrentMeaningsBlock.add(matchingWordOpposite);

                //Getting the Synonym value
                matchingWordSynonym = ""; //TODO: See if this can be extracted from the site
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

            currentMatchingWordCharacteristics.add(matchingWordCurrentMeaningBlocks);
            setOf_matchingWordCharacteristics.add(currentMatchingWordCharacteristics);
        }


        return setOf_matchingWordCharacteristics;
    }
    private static Object getElementAtHeader(List<Object> list, String header) {
        for (int i=0; i<list.size()-1; i++) {
            if (i%2==0 && ((String)list.get(i)).contains(header)) return list.get(i+1);
        }
        return null;
    }
    private static String reformatMeanings(String meaningsOriginal) {
        String meanings_commas = "";
        for (int i = 0; i < meaningsOriginal.length(); i++) {
            if (meaningsOriginal.substring(i, i + 1).equals(";")) { meanings_commas += ","; }
            else { meanings_commas += meaningsOriginal.substring(i, i + 1); }
        }
        meanings_commas = SharedMethods.fromHtml(meanings_commas).toString();
        meanings_commas = meanings_commas.replaceAll("',", "'");
        return meanings_commas;
    }
}
