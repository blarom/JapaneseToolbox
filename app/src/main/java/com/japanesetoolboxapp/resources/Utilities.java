package com.japanesetoolboxapp.resources;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;

import com.google.firebase.database.FirebaseDatabase;
import com.japanesetoolboxapp.R;
import com.japanesetoolboxapp.data.Word;
import com.japanesetoolboxapp.ui.ConvertFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Utilities {


    public static final String DEBUG_TAG = "JT Debug";
    private static FirebaseDatabase mDatabase;

    //Activity operation utilities
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
    public static void muteSpeaker(Activity activity) {
        if (activity != null) {
            AudioManager mgr = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
            if (mgr != null) mgr.setStreamMute(AudioManager.STREAM_SYSTEM, true);
        }
    }
    public static void unmuteSpeaker(Activity activity) {
        if (activity != null) {
            AudioManager mgr = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
            if (mgr != null) mgr.setStreamMute(AudioManager.STREAM_SYSTEM, false);
        }
    }
    @NonNull public static Boolean checkIfFileExistsInSpecificFolder(File dir, String filename) {

        if (!dir.exists()&& dir.mkdirs()){
            return false;
        }
        if(dir.exists()) {
            String datafilepath = dir + "/" + filename;
            File datafile = new File(datafilepath);

            if (!datafile.exists()) {
                return false;
            }
        }
        return true;
    }
    public static boolean checkStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (activity != null) {
                if (activity.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Log.e(DEBUG_TAG, "You have permission");
                    return true;
                } else {
                    Log.e(DEBUG_TAG, "You have asked for permission");
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                    return false;
                }
            }
            else return false;
        }
        else { //you dont need to worry about these stuff below api level 23
            Log.e(DEBUG_TAG,"You already have the permission");
            return true;
        }
    }
    @NonNull public static String formatSize(long size) {
        //https://inducesmile.com/android/how-to-get-android-ram-internal-and-external-memory-information/
        String suffix = null;

        if (size >= 1024) {
            suffix = " KB";
            size /= 1024;
            if (size >= 1024) {
                suffix = " MB";
                size /= 1024;
            }
        }
        StringBuilder resultBuffer = new StringBuilder(Long.toString(size));

        int commaOffset = resultBuffer.length() - 3;
        while (commaOffset > 0) {
            resultBuffer.insert(commaOffset, ',');
            commaOffset -= 3;
        }
        if (suffix != null) resultBuffer.append(suffix);
        return resultBuffer.toString();
    }


    //Image utilities
    public static Bitmap getBitmapFromUri(Activity activity, Uri resultUri) {
        Bitmap imageToBeDecoded = null;
        try {
            //BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            //bmOptions.inJustDecodeBounds = false;
            //image = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
            if (activity != null) imageToBeDecoded = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), resultUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageToBeDecoded;
    }
    public static Bitmap adjustImageAngleAndScale(Bitmap source, float angle, double scaleFactor) {

        int newWidth = (int) Math.floor(source.getWidth()*scaleFactor);
        int newHeight = (int) Math.floor(source.getHeight()*scaleFactor);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(source, newWidth, newHeight,true);

        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(scaledBitmap , 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true); //rotated Bitmap

    }


    //String manipulations utilities
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
    public static String removeNonSpaceSpecialCharacters(String sentence) {
        String current_char;
        String concatenated_sentence = "";
        for (int index=0; index<sentence.length(); index++) {
            current_char = Character.toString(sentence.charAt(index));
            if (!(current_char.equals(".")
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
    public static String removeDuplicatesFromCommaList(String input_list) {

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


    //OCR utilities
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


    //Internet Connectivity utilities
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
            e.printStackTrace();
            return false;
        }
    }
    public static List<Word> getWordsFromJishoOnWeb(String word, final Context context) {

        if (TextUtils.isEmpty(word)) { return new ArrayList<>(); }

        //region Preparing the word to be included in the url
        String prepared_word;
        if (ConvertFragment.getTextType(word).equals("kanji")) {
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
        String website_code = getWebsiteXml(context.getString(R.string.jisho_website_url) + prepared_word);

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
    public static List<Word> cleanUpProblematicWordsFromJisho(List<Word> words) {

        List<Word> cleanWords = new ArrayList<>();
        //Clean up problematic words (e.g. that don't include a meaning)
        for (Word word : words) {
            if (word.getMeanings().size()>0) cleanWords.add(word);
        }
        return cleanWords;
    }
    private static String getWebsiteXml(String websiteUrl) {

        String responseString = "";
        String inputLine;
        HttpURLConnection connection = null;

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
        List<Object> conceptsBlockData = (List<Object>) getElementAtHeader(primaryData,"concepts");
        if (conceptsBlockData==null) return new ArrayList<>();
        //endregion

        //Extracting the list of hits
        List<Word> wordsList = new ArrayList<>();

        wordsList.addAll(addWordsFromBigBlock(exactBlockData, 3));
        wordsList.addAll(addWordsFromBigBlock(conceptsBlockData, 1));


        return wordsList;
    }
    private static List<Word> addWordsFromBigBlock(List<Object> bigBlockData, int startingSubBlock) {

        List<Word> wordsList = new ArrayList<>();
        String kanji;
        StringBuilder romaji;
        List<String> meaningTagsFromTree;
        List<String> meaningsFromTree;
        for (int i = startingSubBlock; i< bigBlockData.size(); i=i+2) {

            Word currentWord = new Word();

            if (!(bigBlockData.get(i) instanceof List)) break;
            List<Object> conceptLightClearFixData = (List<Object>) bigBlockData.get(i);
            if (!(conceptLightClearFixData.get(1) instanceof List)) break;
            List<Object> conceptLightWrapperData = (List<Object>) conceptLightClearFixData.get(1);
            List<Object> conceptLightReadingsData = (List<Object>) conceptLightWrapperData.get(1);
            List<Object> conceptLightRepresentationData = (List<Object>) conceptLightReadingsData.get(1);

            //region Extracting the kanji
            kanji = "";
            List<Object> TextData = (List<Object>) getElementAtHeader(conceptLightRepresentationData,"text");
            if (TextData!=null && TextData.size()>1) {
                kanji = "";
                for (int j=0; j<TextData.size(); j++) {
                    String currentText;
                    currentText = "";
                    if (TextData.get(j) instanceof List) {
                        List<Object> list = (List<Object>) TextData.get(j);
                        if (list.size()>0) currentText = (String) list.get(0);
                    }
                    else {
                        currentText = (String) TextData.get(j);
                        if (currentText.equals("span")) currentText = "";
                    }
                    kanji += currentText;
                }
            }
            else if (TextData!=null && TextData.size()>0) kanji = (String) TextData.get(0);
            currentWord.setKanji(kanji);
            //endregion

            //region Extracting the romaji
            romaji = new StringBuilder();
            List<Object> furiganaData = (List<Object>) conceptLightRepresentationData.get(1);
            for (int j=1; j<furiganaData.size(); j=j+2) {
                List<Object> kanji1UpData = (List<Object>) furiganaData.get(j);
                if (kanji1UpData.size()>0) romaji.append((String) kanji1UpData.get(0));
            }

            if (romaji.length()!=0 && (ConvertFragment.getTextType(kanji).equals("katakana") || ConvertFragment.getTextType(kanji).equals("hiragana"))) {
                //When the word is originally katakana only, the website does not display hiragana. This is corrected here.
                romaji = new StringBuilder(ConvertFragment.getLatinHiraganaKatakana(kanji).get(0));
            }

            List<Object> conceptLightStatusData = (List<Object>) getElementAtHeader(conceptLightWrapperData,"concept_light-status");
            if (conceptLightStatusData==null) conceptLightStatusData = (List<Object>) getElementAtHeader(conceptLightClearFixData,"concept_light-status");
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
                                (ConvertFragment.getTextType(currentValue).equals("katakana") || ConvertFragment.getTextType(currentValue).equals("hiragana"))) {
                            //When the word is originally katakana only, the website does not display hiragana. This is corrected here.
                            romaji = new StringBuilder(ConvertFragment.getLatinHiraganaKatakana(currentValue).get(0));
                            break;
                        }
                    }
                }
            }
            currentWord.setRomaji(ConvertFragment.getLatinHiraganaKatakana(romaji.toString()).get(0));
            //endregion

            currentWord.setUniqueIdentifier(currentWord.getRomaji()+"-"+kanji);

            //region Extracting the alternate spellings
            currentWord.setAltSpellings(""); //Alternate spellings, left empty for now >>> TODO: add this functionality
            //endregion

            //region Extracting the Common Word status
            if (conceptLightStatusData!=null) {
                List<Object> conceptLightCommonSuccess = (List<Object>) getElementAtHeader(conceptLightStatusData, "common success label");
                if (conceptLightCommonSuccess != null && conceptLightCommonSuccess.size() > 0) {
                    String value = (String) conceptLightCommonSuccess.get(0);
                    if (!TextUtils.isEmpty(value) && value.equalsIgnoreCase("Common word")) {
                        currentWord.setCommonStatus(1);
                    } else currentWord.setCommonStatus(0);
                } else currentWord.setCommonStatus(0);
            }
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
        meanings_commas = Utilities.fromHtml(meanings_commas).toString();
        meanings_commas = meanings_commas.replaceAll("',", "'");
        meanings_commas = meanings_commas.replaceAll(",0", "'0"); //Fixes number display problems
        return meanings_commas;
    }


    //Database utilities
    public static FirebaseDatabase getDatabase() {
        //inspired by: https://github.com/firebase/quickstart-android/issues/15
        if (mDatabase == null) {
            mDatabase = FirebaseDatabase.getInstance();
            mDatabase.setPersistenceEnabled(true);
        }
        return mDatabase;
    }
    public static String cleanIdentifierForFirebase(String string) {
        if (TextUtils.isEmpty(string)) return "";
        string = string.replaceAll("\\.","*");
        string = string.replaceAll("#","*");
        string = string.replaceAll("\\$","*");
        string = string.replaceAll("\\[","*");
        string = string.replaceAll("]","*");
        //string = string.replaceAll("\\{","*");
        //string = string.replaceAll("}","*");
        return string;
    }
    public static List<Word> getMergedWordsList(List<Word> localWords, List<Word> asyncWords) {

        List<Word> finalWordsList = new ArrayList<>();
        List<Word> finalAsyncWords = new ArrayList<>(asyncWords);
        boolean asyncMeaningFoundLocally;

        for (int j = 0; j< localWords.size(); j++) {
            Word currentLocalWord = localWords.get(j);
            Word finalWord = new Word();
            finalWord.setRomaji(currentLocalWord.getRomaji());
            finalWord.setKanji(currentLocalWord.getKanji());
            finalWord.setAltSpellings(currentLocalWord.getAltSpellings());

            List<Word.Meaning> currentLocalMeanings = currentLocalWord.getMeanings();
            List<Word.Meaning> currentFinalMeanings = new ArrayList<>(currentLocalMeanings);

            int currentIndex = finalAsyncWords.size()-1;
            while (currentIndex >= 0 && finalAsyncWords.size() != 0) {

                if (currentIndex > finalAsyncWords.size()-1) break;

                Word currentAsyncWord = finalAsyncWords.get(currentIndex);
                List<Word.Meaning> currentAsyncMeanings = currentAsyncWord.getMeanings();

                if (    currentAsyncWord.getRomaji().equals(currentLocalWord.getRomaji())
                        &&  currentAsyncWord.getKanji().equals(currentLocalWord.getKanji())   ) {

                    for (int m = 0; m< currentAsyncMeanings.size(); m++) {

                        asyncMeaningFoundLocally = false;
                        for (int k = 0; k< currentLocalMeanings.size(); k++) {

                            if (currentLocalMeanings.get(k).getMeaning()
                                    .contains( currentAsyncMeanings.get(m).getMeaning() ) ) {
                                asyncMeaningFoundLocally = true;
                                break;
                            }
                        }
                        if (!asyncMeaningFoundLocally) {
                            currentFinalMeanings.add(currentAsyncMeanings.get(m));
                        }
                    }
                    finalAsyncWords.remove(currentIndex);
                    if (currentIndex == 0) break;
                }
                else {
                    currentIndex -= 1;
                }
            }
            finalWord.setMeanings(currentFinalMeanings);
            finalWordsList.add(finalWord);
        }
        finalWordsList.addAll(finalAsyncWords);

        return finalWordsList;
    }
    public static List<Word> getDifferentAsyncWords(List<Word> localWords, List<Word> asyncWords) {

        List<Word> differentAsyncWords = new ArrayList<>();
        List<Word> remainingLocalWords = new ArrayList<>(localWords);
        List<Word.Meaning> localMeanings;
        List<Word.Meaning> asyncMeanings;
        List<Word.Meaning> remainingLocalMeanings;
        boolean foundMatchingLocalWord;
        int localMeaningIndex ;
        int localWordIndex;
        String asyncRomaji;
        String localRomaji;
        String asyncKanji;
        String localKanji;

        Word localWord;

        for (Word asyncWord : asyncWords) {

            foundMatchingLocalWord = false;
            localWordIndex = 0;

            asyncRomaji = asyncWord.getRomaji();
            asyncKanji = asyncWord.getKanji();

            while (localWordIndex < remainingLocalWords.size()) {

                localWord = remainingLocalWords.get(localWordIndex);
                localRomaji = localWord.getRomaji();
                localKanji = localWord.getKanji();

                if ( (asyncRomaji.equals(localRomaji) || ("[verb]"+asyncRomaji).equals(localRomaji))
                        && asyncKanji.equals(localKanji) ) {

                    foundMatchingLocalWord = true;

                    localMeanings = localWord.getMeanings();
                    remainingLocalMeanings = new ArrayList<>(localMeanings);
                    asyncMeanings = asyncWord.getMeanings();

                    for (int i=0; i<asyncMeanings.size(); i++) {

                        localMeaningIndex = 0;
                        while (localMeaningIndex < remainingLocalMeanings.size()) {

                            if ( remainingLocalMeanings.get(localMeaningIndex).getMeaning().equals(asyncMeanings.get(i).getMeaning()) ) {
                                remainingLocalMeanings.remove(localMeaningIndex);
                            }
                    else {
                        localMeaningIndex++;
                    }
                }
            }

            if (remainingLocalMeanings.size()>0) differentAsyncWords.add(asyncWord);

                    remainingLocalWords.remove(localWord);
                    break;
                }
                else {
                    localWordIndex++;
                }
            }

            if (!foundMatchingLocalWord) differentAsyncWords.add(asyncWord);

        }

        for (Word word : differentAsyncWords) {
            if (word.getKanji().equals("為る")) {
                differentAsyncWords.remove(word);
                break;
            }
        }

        return differentAsyncWords;
    }
    public static List<Word> getCommonWords(List<Word> wordsList) {
        List<Word> commonWords = new ArrayList<>();
        for (Word word : wordsList) {
            if (word.getCommonStatus()==1) commonWords.add(word);
        }
        return commonWords;
    }
    public static List<long[]> bubbleSortForThreeIntegerList(List<long[]> MatchList) {

        // Sorting the results according to the shortest keyword as found in the above search

        // Computing the value length
        int list_size = MatchList.size();
        long[][] matches = new long[list_size][3];
        for (int i=0;i<list_size;i++) {
            matches[i][0] = MatchList.get(i)[0];
            matches[i][1] = MatchList.get(i)[1];
            matches[i][2] = MatchList.get(i)[2];
        }

        // Sorting
        long tempVar0;
        long tempVar1;
        long tempVar2;
        for (int i=0;i<list_size;i++) { //Bubble sort
            for (int t=1;t<list_size-i;t++) {
                if (matches[t-1][1] > matches[t][1]) {
                    tempVar0 = matches[t-1][0];
                    tempVar1 = matches[t-1][1];
                    tempVar2 = matches[t-1][2];
                    matches[t-1][0] = matches[t][0];
                    matches[t-1][1] = matches[t][1];
                    matches[t-1][2] = matches[t][2];
                    matches[t][0] = tempVar0;
                    matches[t][1] = tempVar1;
                    matches[t][2] = tempVar2;
                }
            }
        }

        List<long[]> sortedMatchList = new ArrayList<>();
        long[] element;
        for (int i=0;i<list_size;i++) {
            element = new long[3];
            element[0] = matches[i][0];
            element[1] = matches[i][1];
            element[2] = matches[i][2];
            sortedMatchList.add(element);
        }

        return sortedMatchList;
    }
    public static int getLengthFromWordAttributes(Word currentWord, String mInputQuery, String queryWordWithoutTo, boolean queryIsVerbWithTo) {
        String romaji_value = currentWord.getRomaji();
        String kanji_value = currentWord.getKanji();
        String type = currentWord.getMeanings().get(0).getType();
        boolean currentWordIsAVerb = type.substring(0,1).equals("V") && !type.equals("VC");


        //Get the length of the shortest meaning containing the word, and use it to prioritize the results
        List<Word.Meaning> currentMeanings = currentWord.getMeanings();
        String currentMeaning;
        int baseMeaningLength = 1500;
        int currentMeaningLength = baseMeaningLength;
        boolean foundMeaningLength;
        int lateMeaningPenalty = 0;
        String inputQuery;

        for (int j = 0; j< currentMeanings.size(); j++) {
            currentMeaning = currentMeanings.get(j).getMeaning();

            if (!currentWordIsAVerb) {
                foundMeaningLength = false;
                if (!queryIsVerbWithTo) {
                    inputQuery = mInputQuery;
                    baseMeaningLength = 1000;
                }
                else {
                    inputQuery = queryWordWithoutTo;
                    baseMeaningLength = 1500;
                }

                String[] currentMeaningIndividualWords = currentMeaning.split(" ");
                for (String word : currentMeaningIndividualWords) {
                    if (word.equals(inputQuery)) {
                        currentMeaningLength = baseMeaningLength + lateMeaningPenalty + currentMeaning.length() - 50;
                        foundMeaningLength = true;
                        break;
                    }
                }
                if (foundMeaningLength) break;

                String[] currentMeaningIndividualWordsWithoutParentheses = currentMeaning.replace("(","").replace(")","").split(" ");
                for (String word : currentMeaningIndividualWordsWithoutParentheses) {
                    if (word.equals(inputQuery)) {
                        currentMeaningLength = baseMeaningLength + lateMeaningPenalty + currentMeaning.length();
                        foundMeaningLength = true;
                        break;
                    }
                }
                if (foundMeaningLength) break;

                if (currentMeaning.contains(inputQuery) && currentMeaning.length() <= currentMeaningLength) {
                    currentMeaningLength = currentMeaningLength + currentMeaning.length();
                }
            }
            else {
                foundMeaningLength = false;
                if (!queryIsVerbWithTo) {
                    inputQuery = mInputQuery;
                    baseMeaningLength = 1000;
                }
                else {
                    inputQuery = mInputQuery;
                    baseMeaningLength = 300;
                }

                String[] currentMeaningIndividualElements = currentMeaning.split(",");
                for (String element : currentMeaningIndividualElements) {
                    if (element.trim().equals(inputQuery)) {
                        currentMeaningLength = baseMeaningLength + lateMeaningPenalty + currentMeaning.length() - 100;
                        foundMeaningLength = true;
                        break;
                    }
                }
                if (foundMeaningLength) break;

//                currentMeaningLength = baseMeaningLength + lateMeaningPenalty + currentMeaning.length();
//                if (mInputQuery.equals(currentMeaning)) { currentMeaningLength -= 100; break; }
//                else if (currentMeaning.contains(mInputQuery)) { currentMeaningLength -= 50; break; }

            }
            lateMeaningPenalty += 50;

        }

        //Get the total length
        int length = romaji_value.length() + kanji_value.length() + currentMeaningLength;

        //If the romaji or Kanji value is an exact match to the search word, then it must appear at the start of the list
        if (romaji_value.equals(mInputQuery) || kanji_value.equals(mInputQuery)) length = 0;

        return length;
    }

    //Preference utilities
    public static Boolean getShowOnlineResultsPreference(Activity activity) {
        Boolean showOnlineResults = false;
        if (activity!=null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
            showOnlineResults = sharedPreferences.getBoolean(activity.getString(R.string.pref_complete_local_with_online_search_key),
                    activity.getResources().getBoolean(R.bool.pref_complete_local_with_online_search_default));
        }
        return showOnlineResults;
    }

}
