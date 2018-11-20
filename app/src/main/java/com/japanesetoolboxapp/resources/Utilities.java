package com.japanesetoolboxapp.resources;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
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
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.google.firebase.database.FirebaseDatabase;
import com.japanesetoolboxapp.BuildConfig;
import com.japanesetoolboxapp.R;
import com.japanesetoolboxapp.data.JapaneseToolboxCentralRoomDatabase;
import com.japanesetoolboxapp.data.KanjiIndex;
import com.japanesetoolboxapp.data.LatinIndex;
import com.japanesetoolboxapp.data.Verb;
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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Utilities {

    private Utilities() {}

    //Constants
    public static final String firebaseEmail = BuildConfig.firebaseEmail;
    public static final String firebasePass = BuildConfig.firebasePass;
    private static final int WORD_SEARCH_CHAR_COUNT_THRESHOLD = 3;
    public static final int NUM_COLUMNS_IN_WORDS_CSV_SHEETS = 7;
    public static final int NUM_COLUMNS_IN_VERBS_CSV_SHEET = 11;
    private static final String DEBUG_TAG = "JT Debug";
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
        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
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
    @TargetApi(23) public static boolean isPrintable(String c ) {
        Paint paint=new Paint();
        //paint.setTypeface(MainActivity.CJK_typeface);
        boolean hasGlyph=true;
        hasGlyph=paint.hasGlyph(c);
        return hasGlyph;
//            Character.UnicodeBlock block = Character.UnicodeBlock.of( c );
//            return (!Character.isISOControl(c)) &&
//                    block != null &&
//                    block != Character.UnicodeBlock.SPECIALS;
    }


    //String manipulations utilities
    public static String convertToUTF8Index(String input_string) {

        byte[] byteArray = {};
        try {
            byteArray = input_string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        StringBuilder prepared_word = new StringBuilder("1.");
        for (byte b : byteArray) {
            prepared_word.append(Integer.toHexString(b & 0xFF));
        }
        return prepared_word.toString();
    }
    public static String convertFromUTF8Index(String inputHex) {

        //inspired by: https://stackoverflow.com/questions/15749475/java-string-hex-to-string-ascii-with-accentuation
        if(inputHex.length()<4) return "";
        inputHex = inputHex.toLowerCase().substring(2,inputHex.length());

        ByteBuffer buff = ByteBuffer.allocate(inputHex.length()/2);
        for (int i = 0; i < inputHex.length(); i+=2) {
            buff.put((byte)Integer.parseInt(inputHex.substring(i, i+2), 16));
        }
        buff.rewind();
        Charset cs = Charset.forName("UTF-8");
        CharBuffer cb = cs.decode(buff);

        String result = cb.toString();

        return result;
    }
    private static String removeNonSpaceSpecialCharacters(String sentence) {
        String current_char;
        StringBuilder concatenated_sentence = new StringBuilder();
        for (int index=0; index<sentence.length(); index++) {
            current_char = Character.toString(sentence.charAt(index));
            if (!(current_char.equals(".")
                    || current_char.equals("-")
                    || current_char.equals("(")
                    || current_char.equals(")")
                    || current_char.equals(":")
                    || current_char.equals("/") ) ) {
                concatenated_sentence.append(current_char);
            }
        }
        return concatenated_sentence.toString();
    }
    public static String removeSpecialCharacters(String sentence) {
        String current_char;
        StringBuilder concatenated_sentence = new StringBuilder();
        for (int index=0; index<sentence.length(); index++) {
            current_char = Character.toString(sentence.charAt(index));
            if (!( current_char.equals(" ")
                    || current_char.equals(".")
                    || current_char.equals("-")
                    || current_char.equals("(")
                    || current_char.equals(")")
                    || current_char.equals(":")
                    || current_char.equals("/") ) ) {
                concatenated_sentence.append(current_char);
            }
        }
        return concatenated_sentence.toString();
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
    public static List<String> getIntersectionOfLists(List<String> A, List<String> B) {
        //https://stackoverflow.com/questions/2400838/efficient-intersection-of-component_substructures[2]-liststring-in-java
        List<String> rtnList = new LinkedList<>();
        for(String dto : A) {
            if(B.contains(dto)) {
                rtnList.add(dto);
            }
        }
        return rtnList;
    }
    public static List<String> removeDuplicatesFromList(List<String> list) {

            /*
            int end_index = list_of_intersecting_results_temp.size();
            String current_value;
            for (int i=0; i<end_index; i++) {
                current_value = list_of_intersecting_results_temp.get(i);
                for (int j=end_index; j>i; j--) {
                    if (current_value.equals(list_of_intersecting_results_temp.get(j))) {
                        list_of_intersecting_results_temp.remove(j);
                    }
                }
            }
            list_of_intersecting_results = list_of_intersecting_results_temp;
            */

        //https://stackoverflow.com/questions/14040331/remove-duplicate-strings-in-a-list-in-java

        Set<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Iterator<String> i = list.iterator();
        while (i.hasNext()) {
            String s = i.next();
            if (set.contains(s)) {
                i.remove();
            }
            else {
                set.add(s);
            }
        }

        return new ArrayList<>(set);
    }
    public static String getVerbRoot(String verb, String family, int type) {
        String root;
        if (verb == null || verb.length()==0 || family == null || family.length()==0) {
            return "";
        }

        if (type == GlobalConstants.TYPE_LATIN) {
            switch (family) {
                case GlobalConstants.VERB_FAMILY_BU_GODAN:
                case GlobalConstants.VERB_FAMILY_GU_GODAN:
                case GlobalConstants.VERB_FAMILY_KU_GODAN:
                case GlobalConstants.VERB_FAMILY_MU_GODAN:
                case GlobalConstants.VERB_FAMILY_NU_GODAN:
                case GlobalConstants.VERB_FAMILY_RU_GODAN:
                case GlobalConstants.VERB_FAMILY_SU_GODAN:
                case GlobalConstants.VERB_FAMILY_RU_ICHIDAN:
                    root = verb.substring(0, verb.length() - 2);
                    break;
                case GlobalConstants.VERB_FAMILY_TSU_GODAN:
                    root = verb.substring(0, verb.length() - 3);
                    break;
                case GlobalConstants.VERB_FAMILY_U_GODAN:
                    root = verb.substring(0, verb.length() - 1);
                    break;
                case GlobalConstants.VERB_FAMILY_SURU:
                case GlobalConstants.VERB_FAMILY_KURU:
                    root = verb.substring(0, verb.length() - 4);
                    break;
                default:
                    root = verb;
                    break;
            }
        }
        else {
            switch (family) {
                case GlobalConstants.VERB_FAMILY_SURU:
                case GlobalConstants.VERB_FAMILY_KURU:
                    root = verb.substring(0, verb.length() - 2);
                    break;
                case GlobalConstants.VERB_FAMILY_BU_GODAN:
                case GlobalConstants.VERB_FAMILY_GU_GODAN:
                case GlobalConstants.VERB_FAMILY_KU_GODAN:
                case GlobalConstants.VERB_FAMILY_MU_GODAN:
                case GlobalConstants.VERB_FAMILY_NU_GODAN:
                case GlobalConstants.VERB_FAMILY_RU_GODAN:
                case GlobalConstants.VERB_FAMILY_SU_GODAN:
                case GlobalConstants.VERB_FAMILY_RU_ICHIDAN:
                case GlobalConstants.VERB_FAMILY_TSU_GODAN:
                case GlobalConstants.VERB_FAMILY_U_GODAN:
                    root = verb.substring(0, verb.length() - 1);
                    break;
                default:
                    root = verb;
                    break;
            }
        }

        return root;
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
    public static float convertSaturationProgressToValueOLD(float saturationBarValue, Context context) {
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
        StringBuilder prepared_word;
        if (ConvertFragment.getTextType(word) == GlobalConstants.TYPE_KANJI) {
            String converted_word = convertToUTF8Index(word);
            converted_word = converted_word.substring(2,converted_word.length());
            prepared_word = new StringBuilder();
            for (int i = 0; i < converted_word.length() - 1; i = i + 2) {
                prepared_word.append("%").append(converted_word, i, i + 2);
            }
        }
        else {
            prepared_word = new StringBuilder(word);
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

        StringBuilder responseString = new StringBuilder();
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
                responseString = new StringBuilder();
                while ((inputLine = in.readLine()) != null)
                    responseString.append(inputLine).append('\n');
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
        return responseString.toString();
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

        if (runningIndex < 0) return new ArrayList<>();

        List<Object> currentParent = new ArrayList<>();

        if (runningIndex > websiteCodeString.length()-1) {
            currentParent.add("");
            return currentParent;
        }
        String remainingWebsiteCodeString = websiteCodeString.substring(runningIndex,websiteCodeString.length());

        if (!remainingWebsiteCodeString.contains("<")) {
            currentParent.add(remainingWebsiteCodeString);
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

        List<Word> wordsList = new ArrayList<>();

        //Getting to the relevant tree section
        if (parsedData.size()<1) return new ArrayList<>();
        List<Object> htmlData = (List<Object>) parsedData.get(1);
        if (htmlData==null || htmlData.size()<3) return new ArrayList<>();
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
        if (exactBlockData==null) {

            List<Object> conceptsBlockData = (List<Object>) getElementAtHeader(primaryData,"concepts");
            if (conceptsBlockData==null) return wordsList;
            if (conceptsBlockData.size()>2) wordsList.addAll(addWordsFromBigBlock(conceptsBlockData, 1));

            return wordsList;
        }
        else if (exactBlockData.size()>2) wordsList.addAll(addWordsFromBigBlock(exactBlockData, 3));



        return wordsList;
    }
    private static List<Word> addWordsFromBigBlock(List<Object> bigBlockData, int startingSubBlock) {

        if (startingSubBlock >= bigBlockData.size()) return new ArrayList<>();

        List<Word> wordsList = new ArrayList<>();
        StringBuilder kanji;
        StringBuilder romaji;
        List<String> meaningTagsFromTree;
        List<String> meaningsFromTree;
        for (int i = startingSubBlock; i < bigBlockData.size(); i=i+2) {

            Word currentWord = new Word();

            if (!(bigBlockData.get(i) instanceof List)) break;
            List<Object> conceptLightClearFixData = (List<Object>) bigBlockData.get(i);
            if (!(conceptLightClearFixData.get(1) instanceof List)) continue;
            List<Object> conceptLightWrapperData = (List<Object>) conceptLightClearFixData.get(1);
            List<Object> conceptLightReadingsData = (List<Object>) conceptLightWrapperData.get(1);
            List<Object> conceptLightRepresentationData = (List<Object>) conceptLightReadingsData.get(1);

            //region Extracting the kanji
            kanji = new StringBuilder();
            List<Object> TextData = (List<Object>) getElementAtHeader(conceptLightRepresentationData,"text");
            if (TextData!=null && TextData.size()>1) {
                kanji = new StringBuilder();
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
                    kanji.append(currentText);
                }
            }
            else if (TextData!=null && TextData.size()>0) kanji = new StringBuilder((String) TextData.get(0));
            currentWord.setKanji(kanji.toString());
            //endregion

            //region Extracting the romaji
            romaji = new StringBuilder();
            List<Object> furiganaData = (List<Object>) conceptLightRepresentationData.get(1);
            for (int j=1; j<furiganaData.size(); j=j+2) {
                List<Object> kanji1UpData = (List<Object>) furiganaData.get(j);
                if (kanji1UpData.size()>0) romaji.append((String) kanji1UpData.get(0));
            }

            int textType = ConvertFragment.getTextType(kanji.toString());
            if (romaji.length()!=0 && (textType == GlobalConstants.TYPE_HIRAGANA || textType == GlobalConstants.TYPE_KATAKANA)) {
                //When the word is originally katakana only, the website does not display hiragana. This is corrected here.
                romaji = new StringBuilder(ConvertFragment.getLatinHiraganaKatakana(kanji.toString()).get(0));
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

                        textType = ConvertFragment.getTextType(currentValue);
                        if (currentValue.length() != 0 &&
                                (textType == GlobalConstants.TYPE_HIRAGANA || textType == GlobalConstants.TYPE_KATAKANA)) {
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

            //region Extracting the meanings (types, meanings, altSpellings)

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
                }
                if (meaningTag.contains("Wikipedia") || meaningTag.contains("Notes")) break;
                if (currentHeader.contains("meaning-wrapper")) {
                    if (meaningTag.contains("Other forms")) {
                        List<Object> meaningWrapperData = (List<Object>) meaningsWrapperData.get(j);
                        List<Object> meaningDefinitionData = (List<Object>) meaningWrapperData.get(1);
                        List<Object> meaningMeaningData = (List<Object>) getElementAtHeader(meaningDefinitionData,"meaning-meaning");
                        if (meaningMeaningData ==null || meaningMeaningData.size()==0) break;

                        //Getting the altSpellings container bock to extract from
                        StringBuilder altSpellingsContainer = new StringBuilder();
                        for (Object element : meaningMeaningData) {
                            if (element instanceof List) {
                                List<String> elementList = (List<String>) element;
                                if (elementList.size()>0) {
                                    altSpellingsContainer.append(elementList.get(0));
                                }
                            }
                        }

                        //Extracting the altSpellings using regex
                        List<String> altSpellings = new ArrayList<>();
                        Matcher m = Pattern.compile("\\b(\\w+)\\s【(\\w+)】").matcher(altSpellingsContainer.toString());
                        while (m.find()) {
                            if (!m.group(1).equals(currentWord.getKanji())) altSpellings.add(m.group(1));
                            String convertedMatch = ConvertFragment.getLatinHiraganaKatakana(m.group(2)).get(GlobalConstants.TYPE_LATIN);
                            if (!convertedMatch.equals(currentWord.getRomaji())) altSpellings.add(convertedMatch);
                        }
                        altSpellings = removeDuplicatesFromList(altSpellings);
                        currentWord.setAltSpellings(TextUtils.join(", ", altSpellings));
                        break;
                    }
                    else {
                        List<Object> meaningWrapperData = (List<Object>) meaningsWrapperData.get(j);
                        List<Object> meaningDefinitionData = (List<Object>) meaningWrapperData.get(1);
                        List<Object> meaningMeaningData = (List<Object>) getElementAtHeader(meaningDefinitionData,"meaning-meaning");
                        meaningTagsFromTree.add(meaningTag);
                        meaning = "";
                        if (meaningMeaningData !=null && meaningMeaningData.size()>0) meaning = (String) meaningMeaningData.get(0);
                        meaningsFromTree.add(reformatMeanings(meaning));
                    }
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

        String meanings_commas = meaningsOriginal.replace(";",",");
        meanings_commas = Utilities.fromHtml(meanings_commas).toString();
        meanings_commas = meanings_commas.replaceAll("',", "'");
        meanings_commas = meanings_commas.replaceAll("\",", "\"");
        meanings_commas = meanings_commas.replaceAll(",0", "'0"); //Fixes number display problems
        return meanings_commas;
    }


    //IO utilities
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


    //Database creation utilities
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
    public static void checkDatabaseStructure(List<String[]> databaseFromCsv, String databaseName, int numColumns) {
        for (String[] line : databaseFromCsv) {
            if (line.length < numColumns) {
                Log.v("JapaneseToolbox","Serious error in row [" + line[0] + "] in " + databaseName + ": CSV file row has less columns than expected! Check for accidental line breaks.");
                break;
            }
        }
    }
    public static Word createWordFromCsvDatabases(List<String[]> centralDatabase, List<String[]> meaningsDatabase, List<String[]> multExplanationsDatabase,
                                                  List<String[]> examplesDatabase, int centralDbRowIndex) {

        // Value Initializations
        int example_index;
        Word word = new Word();
        List<String> parsed_example_list;

        //Getting the index value
        int matchingWordId = Integer.parseInt(centralDatabase.get(centralDbRowIndex)[0]);
        word.setWordId(matchingWordId);

        //Getting the keywords value
        String matchingWordKeywordsList = centralDatabase.get(centralDbRowIndex)[1];
        word.setKeywords(matchingWordKeywordsList);

        //Getting the Romaji value
        String matchingWordRomaji = centralDatabase.get(centralDbRowIndex)[2];
        word.setRomaji(matchingWordRomaji);

        //Getting the Kanji value
        String matchingWordKanji = centralDatabase.get(centralDbRowIndex)[3];
        word.setKanji(matchingWordKanji);

        //Setting the unique identifier
        word.setUniqueIdentifier(matchingWordRomaji+"-"+matchingWordKanji);

        //Setting the Common Word flag
        word.setCommonStatus(2);

        //Getting the AltSpellings value
        String matchingWordAltSpellings = centralDatabase.get(centralDbRowIndex)[5];
        word.setAltSpellings(matchingWordAltSpellings);


        //regionGetting the set of Meanings

        //Initializations
        String matchingWordMeaning;
        String matchingWordType;
        String matchingWordOpposite;
        String matchingWordSynonym;
        String matchingWordExplanation;
        String matchingWordRules;
        String matchingWordExampleList;
        String[] current_meaning_characteristics;
        Boolean has_multiple_explanations;
        String ME_index;

        //Finding the meanings using the supplied index
        String MM_index = centralDatabase.get(centralDbRowIndex)[4];
        List<String> MM_index_list = Arrays.asList(MM_index.split(";"));
        if (MM_index_list.size() == 0) { return word; }

        List<Word.Meaning> meaningsList = new ArrayList<>();
        int current_MM_index;
        for (int i=0; i< MM_index_list.size(); i++) {

            Word.Meaning meaning = new Word.Meaning();
            current_MM_index = Integer.parseInt(MM_index_list.get(i))-1;
            current_meaning_characteristics = meaningsDatabase.get(current_MM_index);

            //Getting the Meaning value
            matchingWordMeaning = current_meaning_characteristics[1];
            if (matchingWordMeaning.equals("")) continue;

            //Getting the Type value
            matchingWordType = current_meaning_characteristics[2];

            //Make corrections to the meaning values if the hit is a verb
            if (matchingWordType.contains("V") && !matchingWordType.equals("VC") && !matchingWordType.equals("NV") && !matchingWordType.equals("VdaI")) {

                List<String> meaningElements = Arrays.asList(matchingWordMeaning.split(","));
                StringBuilder meaningFixed = new StringBuilder();
                boolean valueIsInParentheses = false;
                for (int k = 0; k < meaningElements.size(); k++) {
                    if (valueIsInParentheses) meaningFixed.append(meaningElements.get(k).trim());
                    else meaningFixed.append("to ").append(meaningElements.get(k).trim());

                    if (k < meaningElements.size() - 1) meaningFixed.append(", ");

                    if (meaningElements.get(k).contains("(") && !meaningElements.get(k).contains(")")) valueIsInParentheses = true;
                    else if (!meaningElements.get(k).contains("(") && meaningElements.get(k).contains(")")) valueIsInParentheses = false;
                }
                matchingWordMeaning = meaningFixed.toString();
            }

            //Setting the Meaning and Type values in the returned list
            meaning.setMeaning(matchingWordMeaning);
            meaning.setType(matchingWordType);

            //Getting the Opposite value
            matchingWordOpposite = current_meaning_characteristics[6];
            meaning.setAntonym(matchingWordOpposite);

            //Getting the Synonym value
            matchingWordSynonym = current_meaning_characteristics[7];
            meaning.setSynonym(matchingWordSynonym);

            //Getting the set of Explanations
            has_multiple_explanations = false;
            ME_index = "";
            if (current_meaning_characteristics[3].length() > 3) {
                if (current_meaning_characteristics[3].substring(0,3).equals("ME#")) {
                    has_multiple_explanations = true;
                    ME_index = current_meaning_characteristics[3].substring(3,current_meaning_characteristics[3].length());
                }
            }

            List<Word.Meaning.Explanation> explanationList = new ArrayList<>();
            if (has_multiple_explanations) {
                List<String> ME_index_list = Arrays.asList(ME_index.split(";"));
                int current_ME_index;
                for (int j=0; j<ME_index_list.size(); j++) {

                    Word.Meaning.Explanation explanation = new Word.Meaning.Explanation();

                    current_ME_index = Integer.parseInt(ME_index_list.get(j))-1;

                    //Getting the Explanation value
                    matchingWordExplanation = multExplanationsDatabase.get(current_ME_index)[1];
                    explanation.setExplanation(matchingWordExplanation);

                    //Getting the Rules value
                    matchingWordRules = multExplanationsDatabase.get(current_ME_index)[2];
                    explanation.setRules(matchingWordRules);

                    //Getting the Examples
                    matchingWordExampleList = multExplanationsDatabase.get(current_ME_index)[3];
                    List<Word.Meaning.Explanation.Example> exampleList = new ArrayList<>();
                    if (!matchingWordExampleList.equals("") && !matchingWordExampleList.contains("Example")) {
                        parsed_example_list = Arrays.asList(matchingWordExampleList.split(", "));
                        for (int t = 0; t < parsed_example_list.size(); t++) {
                            Word.Meaning.Explanation.Example example = new Word.Meaning.Explanation.Example();
                            example_index = Integer.parseInt(parsed_example_list.get(t)) - 1;
                            example.setEnglishSentence(examplesDatabase.get(example_index)[GlobalConstants.Examples_colIndex_Example_English]);
                            example.setRomajiSentence(examplesDatabase.get(example_index)[GlobalConstants.Examples_colIndex_Example_Romaji]);
                            example.setKanjiSentence(examplesDatabase.get(example_index)[GlobalConstants.Examples_colIndex_Example_Kanji]);
                            exampleList.add(example);
                        }
                    }
                    explanation.setExamples(exampleList);
                    explanationList.add(explanation);
                }
            }
            else {
                Word.Meaning.Explanation explanation = new Word.Meaning.Explanation();

                //Getting the Explanation value
                matchingWordExplanation = meaningsDatabase.get(current_MM_index)[3];
                explanation.setExplanation(matchingWordExplanation);

                //Getting the Rules value
                matchingWordRules = meaningsDatabase.get(current_MM_index)[4];
                explanation.setRules(matchingWordRules);

                //Getting the Examples
                matchingWordExampleList = meaningsDatabase.get(current_MM_index)[5];
                List<Word.Meaning.Explanation.Example> exampleList = new ArrayList<>();
                if (!matchingWordExampleList.equals("") && !matchingWordExampleList.contains("Example")) {
                    parsed_example_list = Arrays.asList(matchingWordExampleList.split(", "));
                    for (int t = 0; t < parsed_example_list.size(); t++) {
                        Word.Meaning.Explanation.Example example = new Word.Meaning.Explanation.Example();
                        example_index = Integer.parseInt(parsed_example_list.get(t)) - 1;
                        example.setEnglishSentence(examplesDatabase.get(example_index)[GlobalConstants.Examples_colIndex_Example_English]);
                        example.setRomajiSentence(examplesDatabase.get(example_index)[GlobalConstants.Examples_colIndex_Example_Romaji]);
                        example.setKanjiSentence(examplesDatabase.get(example_index)[GlobalConstants.Examples_colIndex_Example_Kanji]);
                        exampleList.add(example);
                    }
                }
                explanation.setExamples(exampleList);
                explanationList.add(explanation);
            }
            meaning.setExplanations(explanationList);
            meaningsList.add(meaning);

        }
        //endregion

        word.setMeanings(meaningsList);

        return word;
    }
    public static Verb createVerbFromCsvDatabase(List<String[]> verbDatabase, List<String[]> meaningsDatabase, int verbDbRowIndex) {

        // Value Initializations
        Verb verb = new Verb();
        String type;
        String[] currentMeaningCharacteristics;

        verb.setVerbId(Integer.parseInt(verbDatabase.get(verbDbRowIndex)[0]));
        verb.setPreposition(verbDatabase.get(verbDbRowIndex)[7]);
        verb.setKanjiRoot(verbDatabase.get(verbDbRowIndex)[8]);
        verb.setLatinRoot(verbDatabase.get(verbDbRowIndex)[9]);
        verb.setExceptionIndex(verbDatabase.get(verbDbRowIndex)[10]);
        verb.setRomaji(verbDatabase.get(verbDbRowIndex)[2]);
        verb.setKanji(verbDatabase.get(verbDbRowIndex)[3]);
        verb.setAltSpellings(verbDatabase.get(verbDbRowIndex)[5]);
        verb.setHiraganaFirstChar(ConvertFragment.getLatinHiraganaKatakana(verb.getRomaji()).get(1).substring(0,1));

        //Setting the family
        String MM_index = verbDatabase.get(verbDbRowIndex)[4];
        List<String> MM_index_list = Arrays.asList(MM_index.split(";"));
        if (MM_index_list.size() == 0) { return verb; }

        int current_MM_index;
        List<String> trans = new ArrayList<>();
        for (int i=0; i< MM_index_list.size(); i++) {

            current_MM_index = Integer.parseInt(MM_index_list.get(i)) - 1;
            currentMeaningCharacteristics = meaningsDatabase.get(current_MM_index);

            //Getting the Family value
            type = currentMeaningCharacteristics[2];

            if (type.equals("") || !type.substring(0,1).equals("V")) {
                Log.i(DEBUG_TAG, "Warning! Found verb with incorrect type (Meaning index:" + Integer.toString(current_MM_index) + ")");
                //No exception catching is made here, in order to make sure that database errors are caught before production
            }

            trans.add(String.valueOf(type.charAt(type.length() - 1)));

            if (i==0) verb.setFamily(type.substring(1, type.length() - 1));

        }

        //Setting the transitive/intransitive flag
        for (int i=0; i<trans.size(); i++) {
            if (i>0 && !trans.get(i-1).equals(trans.get(i)) ) {
                verb.setTrans("T/I");
                break;
            }
            if (i == trans.size()-1) verb.setTrans(trans.get(i));
        }


        return verb;
    }


    //Database operations utilities
    public static List<Word> getMergedWordsList(List<Word> localWords, List<Word> asyncWords) {

        List<Word> finalWordsList = new ArrayList<>();
        List<Word> finalAsyncWords = new ArrayList<>(asyncWords);
        boolean asyncMeaningFoundLocally;

        for (int j = 0; j< localWords.size(); j++) {
            Word currentLocalWord = localWords.get(j);
            Word finalWord = new Word();
            finalWord.setRomaji(currentLocalWord.getRomaji());
            finalWord.setKanji(currentLocalWord.getKanji());
            List<String> finalAltSpellings;
            if (TextUtils.isEmpty(currentLocalWord.getAltSpellings())) finalAltSpellings = new ArrayList<>();
            else {
                finalAltSpellings = new ArrayList<>(Arrays.asList(currentLocalWord.getAltSpellings().split(",")));
                for (int i = 0; i< finalAltSpellings.size(); i++) finalAltSpellings.set(0,finalAltSpellings.get(0).trim());
            }

            List<Word.Meaning> currentLocalMeanings = currentLocalWord.getMeanings();
            List<Word.Meaning> currentFinalMeanings = new ArrayList<>(currentLocalMeanings);

            int currentIndex = finalAsyncWords.size()-1;
            while (currentIndex >= 0 && finalAsyncWords.size() != 0) {

                if (currentIndex > finalAsyncWords.size()-1) break;
                Word currentAsyncWord = finalAsyncWords.get(currentIndex);

                if (    currentAsyncWord.getRomaji().equals(currentLocalWord.getRomaji().replace(" ", ""))
                        &&  currentAsyncWord.getKanji().equals(currentLocalWord.getKanji())   ) {

                    //Setting the altSpellings
                    String finalAsyncWordAltSpellings = finalAsyncWords.get(currentIndex).getAltSpellings();
                    if (!TextUtils.isEmpty(finalAsyncWordAltSpellings)) {
                        for (String altSpelling : finalAsyncWordAltSpellings.split(",")) {
                            if (!finalAltSpellings.contains(altSpelling.trim())) {
                                finalAltSpellings.add(altSpelling.trim());
                            }
                        }
                        finalWord.setAltSpellings(TextUtils.join(", ", finalAltSpellings));
                    }

                    //Setting the meanings
                    List<Word.Meaning> currentAsyncMeanings = currentAsyncWord.getMeanings();
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
                localRomaji = localWord.getRomaji().replace(" ", "");
                localKanji = localWord.getKanji();

                if ( asyncRomaji.equals(localRomaji) && asyncKanji.equals(localKanji) ) {

                    foundMatchingLocalWord = true;

                    localMeanings = localWord.getMeanings();
                    asyncMeanings = asyncWord.getMeanings();


                    //If non-identical meanings remain, it is possible that a Jisho meaning was split by types in the local database, therefore check the following:
                    StringBuilder allLocalMeanings = new StringBuilder();
                    for (Word.Meaning meaning : localMeanings) {
                        allLocalMeanings.append(meaning.getMeaning());
                        allLocalMeanings.append(", ");
                    }

                    List<String> allAsyncMeaningElements = new ArrayList<>();
                    boolean isInParenthesis;
                    StringBuilder currentElement;
                    String currentAsyncMeaning;
                    String currentAsyncMeaningChar;
                    for (Word.Meaning asyncWordMeaning : asyncMeanings) {
                        isInParenthesis = false;
                        currentElement = new StringBuilder();
                        currentAsyncMeaning = asyncWordMeaning.getMeaning();
                        for (int i=0; i<asyncWordMeaning.getMeaning().length(); i++) {
                            currentAsyncMeaningChar = currentAsyncMeaning.substring(i,i+1);
                            if (currentAsyncMeaningChar.equals("(")) isInParenthesis = true;
                            else if (currentAsyncMeaningChar.equals(")")) isInParenthesis = false;

                            if (isInParenthesis || !currentAsyncMeaningChar.equals(",")) currentElement.append(currentAsyncMeaningChar);

                            if (currentAsyncMeaningChar.equals(",") && !isInParenthesis || i == asyncWordMeaning.getMeaning().length()-1) {
                                allAsyncMeaningElements.add(currentElement.toString().trim());
                                currentElement = new StringBuilder();
                            }
                        }
                    }

                    String allLocalMeaningsAsString = allLocalMeanings.toString();
                    boolean meaningNotFoundInLocalWord = false;
                    for (String element : allAsyncMeaningElements) {
                        if (!allLocalMeaningsAsString.contains(element)) {
                            meaningNotFoundInLocalWord = true;
                            break;
                        }
                    }

                    boolean altSpellingNotFoundInLocalWord = false;
                    if (!TextUtils.isEmpty(asyncWord.getAltSpellings())) {
                        if (TextUtils.isEmpty(localWord.getAltSpellings())) altSpellingNotFoundInLocalWord = true;
                        else {
                            for (String asyncAltSpelling : asyncWord.getAltSpellings().split(",")) {
                                if (!localWord.getAltSpellings().contains(asyncAltSpelling.trim())) {
                                    altSpellingNotFoundInLocalWord = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (meaningNotFoundInLocalWord || altSpellingNotFoundInLocalWord) differentAsyncWords.add(asyncWord);

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
        //Get the length of the shortest meaning containing the word, and use it to prioritize the results

        String romaji_value = currentWord.getRomaji();
        String kanji_value = currentWord.getKanji();
        String type = currentWord.getMeanings().get(0).getType();
        boolean currentWordIsAVerb = type.length()>0 && type.substring(0,1).equals("V") && !type.equals("VC") && !type.equals("NV");

        List<Word.Meaning> currentMeanings = currentWord.getMeanings();
        String currentMeaning;
        int baseMeaningLength = 1500;
        int currentMeaningLength = baseMeaningLength;
        boolean foundMeaningLength;
        int lateMeaningPenalty = 0;
        String inputQuery;
        int lateHitInMeaningPenalty; //Adding a penalty for late hits in the meaning
        int cumulativeMeaningLength; //Using a cumulative meaning length instead of the total length, since if a word is at the start of a meaning it's more important and the hit is more likely to be relevant

        for (int j = 0; j< currentMeanings.size(); j++) {
            currentMeaning = currentMeanings.get(j).getMeaning();

            //region If the current word is not a verb
            if (!currentWordIsAVerb) {
                foundMeaningLength = false;

                inputQuery = mInputQuery;
                baseMeaningLength = 1000; //was 1500

                //If meaning has the exact word, get the length as follows
                String[] currentMeaningIndividualElements = currentMeaning.split(",");
                lateHitInMeaningPenalty = 0;
                cumulativeMeaningLength = 0;
                for (String currentMeaningElement : currentMeaningIndividualElements) {

                    String[] currentMeaningIndividualWords = currentMeaningElement.trim().split(" ");
                    for (String word : currentMeaningIndividualWords) {
                        cumulativeMeaningLength += word.length() + 2; //Added 2 to account for missing ", " instances in loop
                        if (word.equals(inputQuery)) {
                            currentMeaningLength = baseMeaningLength + lateMeaningPenalty + lateHitInMeaningPenalty + cumulativeMeaningLength - 100;
                            foundMeaningLength = true;
                            break;
                        }
                        lateHitInMeaningPenalty += 25;
                    }
                    if (foundMeaningLength) break;

                    //If meaning has the exact word but maybe in parentheses, get the length as follows
                    String[] currentMeaningIndividualWordsWithoutParentheses = currentMeaningElement.trim()
                            .replace("(","").replace(")","").split(" ");
                    lateHitInMeaningPenalty = 0;
                    cumulativeMeaningLength = 0;
                    for (String word : currentMeaningIndividualWordsWithoutParentheses) {
                        cumulativeMeaningLength += word.length() + 2; //Added 2 to account for missing ", " instances in loop
                        if (word.equals(inputQuery)) {
                            currentMeaningLength = baseMeaningLength + lateMeaningPenalty + lateHitInMeaningPenalty + cumulativeMeaningLength;
                            foundMeaningLength = true;
                            break;
                        }
                        lateHitInMeaningPenalty += 25;
                    }
                    if (foundMeaningLength) break;

                    lateHitInMeaningPenalty += 50;
                }
                if (foundMeaningLength) break;

                //If still not found, get the length of the less important results
                if (currentMeaning.contains(inputQuery) && currentMeaning.length() <= currentMeaningLength) {
                    currentMeaningLength = currentMeaningLength + currentMeaning.length();
                }
            }
            //endregion

            //region If the current word is a verb
            else {
                foundMeaningLength = false;

                String[] currentMeaningIndividualElements;
                if (!queryIsVerbWithTo) {
                    baseMeaningLength = 1000;

                    //Calculate the length first by adding "to " to the input query. If it leads to a hit, that means that this verb is relevant
                    inputQuery = "to " + mInputQuery;

                    currentMeaningIndividualElements = currentMeaning.split(",");
                    lateHitInMeaningPenalty = 0;
                    cumulativeMeaningLength = 0;
                    for (String element : currentMeaningIndividualElements) {
                        cumulativeMeaningLength += element.length() + 2; //Added 2 to account for missing ", " instances in loop
                        String trimmedElement = element.trim();
                        if (trimmedElement.equals(inputQuery)) {
                            currentMeaningLength = baseMeaningLength + lateMeaningPenalty + lateHitInMeaningPenalty + cumulativeMeaningLength - 100;
                            foundMeaningLength = true;
                            break;
                        }
                        lateHitInMeaningPenalty += 25;
                    }
                    if (foundMeaningLength) break;


                    //Otherwise, use the original query to get the length
                    inputQuery = mInputQuery;

                    currentMeaningIndividualElements = currentMeaning.split(",");
                    lateHitInMeaningPenalty = 0;
                    cumulativeMeaningLength = 0;
                    for (String element : currentMeaningIndividualElements) {
                        String trimmedElement = element.trim();
                        if (trimmedElement.equals(inputQuery)) {
                            currentMeaningLength = baseMeaningLength + lateMeaningPenalty + lateHitInMeaningPenalty + cumulativeMeaningLength - 100;
                            foundMeaningLength = true;
                            break;
                        }
                        cumulativeMeaningLength += element.length() + 2; //Added 2 to account for missing ", " instances in loop
                        lateHitInMeaningPenalty += 25;
                    }
                    if (foundMeaningLength) break;
                }
                else {
                    baseMeaningLength = 200;
                    inputQuery = mInputQuery;

                    //Get the length according to the position of the verb in the meanings list
                    currentMeaningIndividualElements = currentMeaning.split(",");
                    lateHitInMeaningPenalty = 0;
                    cumulativeMeaningLength = 0;
                    for (String element : currentMeaningIndividualElements) {
                        cumulativeMeaningLength += element.length() + 2; //Added 2 to account for missing ", " instances in loop
                        String trimmedElement = element.trim();
                        if (trimmedElement.equals(inputQuery)) {
                            currentMeaningLength = baseMeaningLength + lateMeaningPenalty + lateHitInMeaningPenalty + cumulativeMeaningLength - 100;
                            foundMeaningLength = true;
                            break;
                        }
                        lateHitInMeaningPenalty += 25;
                    }
                    if (foundMeaningLength) break;
                }

            }
            //endregion

            lateMeaningPenalty += 100;

        }

        //Get the total length
        int length = romaji_value.length() + kanji_value.length() + currentMeaningLength;

        //If the romaji or Kanji value is an exact match to the search word, then it must appear at the start of the list
        if (romaji_value.equals(mInputQuery) || kanji_value.equals(mInputQuery)) length = 0;

        return length;
    }
    /**
     * Returns the word ids that match the searchWord.
     * Performing basic filtering according to each word's keywords list if length>=4, or differently for shorter words.
     * @param inputWord
     * @param japaneseToolboxCentralRoomDatabase
     * @return
     */
    public static List<Long> getMatchingWordIdsAndDoBasicFiltering(String inputWord, JapaneseToolboxCentralRoomDatabase japaneseToolboxCentralRoomDatabase) {

        //region Initializations
        List<Long> matchingWordIds = new ArrayList<>();
        String searchWordNoSpaces;
        boolean queryIsVerbWithTo = false;
        String searchWordWithoutTo = "";
        String searchWord;
        //endregion

        //region Converting the searchWord to usable forms, preventing invalid characters from influencing the search results
        inputWord = inputWord.toLowerCase(Locale.ENGLISH);
        inputWord = removeApostrophes(inputWord);
        inputWord = Utilities.removeNonSpaceSpecialCharacters(inputWord);
        searchWord = inputWord;
        searchWordNoSpaces = searchWord.replace(" ", "");

        //Registering if the input query is a "to " verb
        if (searchWord.length()>3 && searchWord.substring(0,3).equals("to ")) {
            queryIsVerbWithTo = true;
            searchWordWithoutTo = searchWord.substring(3, searchWord.length());
        }
        //endregion

        //region If there is an "inging" verb instance, reduce it to an "ing" instance (e.g. singing >> sing)
        String verb2;
        String inglessVerb = searchWord;
        if (searchWord.length() > 2 && searchWord.substring(searchWord.length()-3).equals("ing")) {

            if (searchWord.length() > 5 && searchWord.substring(searchWord.length()-6).equals("inging")) {
                if (	(searchWord.substring(0, 3).equals("to ") && isOfTypeIngIng(searchWord.substring(3,searchWord.length()))) ||
                        (!searchWord.substring(0, 3).equals("to ") && isOfTypeIngIng(searchWord))   ) {
                    // If the verb ends with "inging" then remove the the second "ing"
                    inglessVerb = searchWord.substring(0,searchWord.length()-3);
                }
            }
            else {
//                verb2 = searchWord + "ing";
//                if ((!verb2.substring(0, 2+1).equals("to ") || !checkIfWordIsOfTypeIngIng(verb2.substring(3, searchWord.length() + 3))) &&
//                        (verb2.substring(0, 2+1).equals("to ") || !checkIfWordIsOfTypeIngIng(verb2.substring(0, searchWord.length() + 3)))) {
//                    // If the verb does not belong to the keywords, then remove the ending "ing" so that it can be compared later on to the verbs excel
//                    //If the verb is for e.g. to sing / sing, where verb2 = to singing / singing, then check that verb2 (without the "to ") belongs to the keywords, and if it does then do nothing
//
//                    inglessVerb = searchWord.substring(0,searchWord.length()-3);
//                }
            }
        }
        //endregion

        //region Getting the input type and its converted form (english/romaji/kanji/invalid)
        List<String> translationList = ConvertFragment.getLatinHiraganaKatakana(searchWordNoSpaces);

        String searchWordTransliteratedLatin = translationList.get(GlobalConstants.TYPE_LATIN);
        String searchWordTransliteratedHiragana = translationList.get(GlobalConstants.TYPE_HIRAGANA);
        String searchWordTransliteratedKatakana = translationList.get(GlobalConstants.TYPE_KATAKANA);
        int inputTextType = ConvertFragment.getTextType(searchWord);

        if (inputTextType == GlobalConstants.TYPE_INVALID) return matchingWordIds;
        //endregion

        //region Replacing a Kana input by its Romaji form
        if (inputTextType == GlobalConstants.TYPE_HIRAGANA || inputTextType == GlobalConstants.TYPE_KATAKANA) {
            searchWord = searchWordTransliteratedLatin;
            searchWordNoSpaces = searchWordTransliteratedLatin;
        }
        //endregion

        //region Getting the matches
        matchingWordIds = addNormalMatchesToMatchesList(searchWord, searchWordNoSpaces, inglessVerb, searchWordWithoutTo,
                queryIsVerbWithTo, inputTextType, matchingWordIds, japaneseToolboxCentralRoomDatabase);

        matchingWordIds = addConjugatedAdjectivesToMatchesList(searchWord, inputTextType, matchingWordIds, japaneseToolboxCentralRoomDatabase);
        //endregion

        return matchingWordIds;
    }
    private static List<Long> getMatchingWordIdsForNormalInput(boolean forceExactSearch, int inputTextType, String searchWord, String searchWordNoSpaces,
                                                               JapaneseToolboxCentralRoomDatabase japaneseToolboxCentralRoomDatabase) {

        List<Long> matchingWordIds = new ArrayList<>();

        //region Search for the matches in the indexed keywords
        List<String> searchResultIndexesArray = new ArrayList<>();
        List<LatinIndex> latinIndices;
        List<KanjiIndex> kanjiIndices;
        if (inputTextType == GlobalConstants.TYPE_LATIN || inputTextType == GlobalConstants.TYPE_HIRAGANA
                || inputTextType == GlobalConstants.TYPE_KATAKANA || inputTextType == GlobalConstants.VALUE_NUMBER) {

            //If the input is a verb in "to " form, remove the "to " for the search only (results will be filtered later on)
            String inputWord = Utilities.removeNonSpaceSpecialCharacters(searchWord);
            if (searchWord.length()>3) {
                if (searchWord.substring(0, 3).equals("to ")) {
                    inputWord = searchWordNoSpaces.substring(2, searchWordNoSpaces.length());
                }
            }
            String inputWordNoSpaces = Utilities.removeSpecialCharacters(inputWord);

            boolean exactSearch = inputWordNoSpaces.length() < 3 || forceExactSearch;
            latinIndices = findQueryInLatinIndex(inputWordNoSpaces, exactSearch, japaneseToolboxCentralRoomDatabase);

            if (latinIndices.size()==0) return matchingWordIds;

            // If the entered word is Latin and only has up to WORD_SEARCH_CHAR_COUNT_THRESHOLD characters, limit the word keywords to be checked later
            if ((   (inputTextType == GlobalConstants.TYPE_LATIN
                        || inputTextType == GlobalConstants.TYPE_HIRAGANA
                        || inputTextType == GlobalConstants.TYPE_KATAKANA)
                        && searchWordNoSpaces.length() < WORD_SEARCH_CHAR_COUNT_THRESHOLD)
                    || (inputTextType == GlobalConstants.VALUE_NUMBER
                        && searchWordNoSpaces.length() < WORD_SEARCH_CHAR_COUNT_THRESHOLD-1)) {
                for (LatinIndex latinIndex : latinIndices) {
                    if (latinIndex.getLatin().length() < WORD_SEARCH_CHAR_COUNT_THRESHOLD) {
                        searchResultIndexesArray.add(latinIndex.getWordIds());
                        break;
                    }
                }
            }
            else {
                for (LatinIndex latinIndex : latinIndices) {
                    searchResultIndexesArray.add(latinIndex.getWordIds());
                }
            }

        } else if (inputTextType == GlobalConstants.TYPE_KANJI) {
            kanjiIndices = findQueryInKanjiIndex(searchWordNoSpaces, japaneseToolboxCentralRoomDatabase);
            if (kanjiIndices.size()==0) return matchingWordIds;
            for (KanjiIndex kanjiIndex : kanjiIndices) {
                searchResultIndexesArray.add(kanjiIndex.getWordIds());
            }
        } else {
            return matchingWordIds;
        }
        //endregion

        //region Get the indexes of all of the results that were found
        List<String> indexList;
        for (String searchResultIndexes : searchResultIndexesArray) {
            indexList = Arrays.asList(searchResultIndexes.split(";"));
            for (int j = 0; j < indexList.size(); j++) {
                matchingWordIds.add(Long.valueOf(indexList.get(j)));
            }
        }
        //endregion

        return matchingWordIds;
    }
    private static List<Long> getMatchingWordIdsForInglessVerb(boolean forceExactSearch, List<Long> matchingWordIdsFromIndex, String inglessVerb,
                                                               JapaneseToolboxCentralRoomDatabase japaneseToolboxCentralRoomDatabase) {

        List<Long> newMatchingWordIdsFromIndex = new ArrayList<>(matchingWordIdsFromIndex);

        boolean exactSearch = inglessVerb.length() < 3 || forceExactSearch;
        List<LatinIndex> latinIndices = findQueryInLatinIndex(inglessVerb, exactSearch, japaneseToolboxCentralRoomDatabase);

        for (LatinIndex latinIndex : latinIndices) {
            List<String> indexList = Arrays.asList(latinIndex.getWordIds().split(";"));
            for (int j = 0; j < indexList.size(); j++) {
                newMatchingWordIdsFromIndex.add(Long.valueOf(indexList.get(j)));
            }
        }

        return newMatchingWordIdsFromIndex;
    }
    /**
     * Returns matching word ids before filtering, based on the latin an kanji indexes.
     * If the returned list of ids is too big, returns a list of exact matches only.
     * In any case, tries to add results where "ing" is removed from gerunds.
     * @param inputTextType
     * @param searchWord
     * @param searchWordNoSpaces
     * @param inglessVerb
     * @param japaneseToolboxCentralRoomDatabase
     * @return
     */
    private static List<Long> getMatchingWordIds(int inputTextType, String searchWord, String searchWordNoSpaces, String inglessVerb,
                                                 JapaneseToolboxCentralRoomDatabase japaneseToolboxCentralRoomDatabase) {

        List<Long> matchingWordIds = getMatchingWordIdsForNormalInput(false, inputTextType,
                searchWord, searchWordNoSpaces, japaneseToolboxCentralRoomDatabase);

        //If the number of matching ids is larger than MAX_SQL_VARIABLES_FOR_QUERY, perform an exact search
        if (matchingWordIds.size() > GlobalConstants.MAX_SQL_VARIABLES_FOR_QUERY) {
            matchingWordIds = getMatchingWordIdsForNormalInput(true, inputTextType,
                    searchWord, searchWordNoSpaces, japaneseToolboxCentralRoomDatabase);
        }

        //If the number of matching ids is still larger than MAX_SQL_VARIABLES_FOR_QUERY, limit the list length to MAX_SQL_VARIABLES_FOR_QUERY
        if (matchingWordIds.size() > GlobalConstants.MAX_SQL_VARIABLES_FOR_QUERY) {
            matchingWordIds = matchingWordIds.subList(0,GlobalConstants.MAX_SQL_VARIABLES_FOR_QUERY);
        }

        //Adding search results where the "ing" is removed from an "ing" verb
        if ((inputTextType == GlobalConstants.TYPE_LATIN || inputTextType == GlobalConstants.TYPE_HIRAGANA
                || inputTextType == GlobalConstants.TYPE_KATAKANA || inputTextType == GlobalConstants.VALUE_NUMBER) && !inglessVerb.equals(searchWord)) {

            List<Long> newMatchingWordIds = getMatchingWordIdsForInglessVerb(false,
                    matchingWordIds, inglessVerb, japaneseToolboxCentralRoomDatabase);

            if (matchingWordIds.size() + newMatchingWordIds.size() > GlobalConstants.MAX_SQL_VARIABLES_FOR_QUERY) {
                newMatchingWordIds = getMatchingWordIdsForInglessVerb(true,
                        matchingWordIds, inglessVerb, japaneseToolboxCentralRoomDatabase);
            }

            if (matchingWordIds.size() + newMatchingWordIds.size() <= GlobalConstants.MAX_SQL_VARIABLES_FOR_QUERY) {
                matchingWordIds.addAll(newMatchingWordIds);
            }
        }

        return matchingWordIds;
    }
    /**
     * Gets the matching word ids from the latin/kanji indexes and filters them to return only words matching the searchWord/
     * The filter checks the keywords list for matches for word of length 4 and up, and handles shorter words differently.
     * @param searchWord
     * @param searchWordNoSpaces
     * @param inglessVerb
     * @param searchWordWithoutTo
     * @param queryIsVerbWithTo
     * @param inputTextType
     * @param matchingWordIds
     * @param japaneseToolboxCentralRoomDatabase
     * @return
     */
    private static List<Long> addNormalMatchesToMatchesList(String searchWord, String searchWordNoSpaces, String inglessVerb, String searchWordWithoutTo,
                                                            boolean queryIsVerbWithTo, int inputTextType, List<Long> matchingWordIds,
                                                            JapaneseToolboxCentralRoomDatabase japaneseToolboxCentralRoomDatabase) {

        //region Initializations
        List<long[]> MatchList = new ArrayList<>();
        String keywords;
        long[] current_match_values;
        boolean foundMatch;
        //endregion

        List<Long> matchingWordIdsFromIndex = getMatchingWordIds(inputTextType, searchWord, searchWordNoSpaces, inglessVerb, japaneseToolboxCentralRoomDatabase);

        //region Limiting the database query if there are too many results (prevents long query times)
        boolean onlyRetrieveShortRomajiWords = false;
        if ((inputTextType == GlobalConstants.TYPE_LATIN || inputTextType == GlobalConstants.TYPE_HIRAGANA
                || inputTextType == GlobalConstants.TYPE_KATAKANA) && searchWord.length() < 3) {
            onlyRetrieveShortRomajiWords = true;
        }
        //endregion

        //region Filtering the matches
        List<Word> matchingWordList = japaneseToolboxCentralRoomDatabase.getWordListByWordIds(matchingWordIdsFromIndex);
        String romaji;
        String meanings;
        String altSpellings;
        String[] meaningSet;
        StringBuilder builder;
        boolean isExactMeaningWordsMatch;
        boolean isRomajiMatch;
        boolean isAltSpellingsMatch;
        int searchWordLength = searchWord.length();
        int romajiLength;
        for (Word word : matchingWordList) {

            foundMatch = false;

            //region Handling short words
            if ((inputTextType == GlobalConstants.TYPE_LATIN || inputTextType == GlobalConstants.TYPE_HIRAGANA
                    || inputTextType == GlobalConstants.TYPE_KATAKANA) && onlyRetrieveShortRomajiWords) {

                //Checking if the word is an exact match to one of the words in the meanings
                builder = new StringBuilder();
                for (Word.Meaning meaning : word.getMeanings()) {
                    builder.append(" ");
                    builder.append(meaning.getMeaning().replace(", ", " ").replace("(", " ").replace(")", " "));
                }
                meanings = builder.toString();
                meaningSet = meanings.split(" ");
                isExactMeaningWordsMatch = false;
                for (String meaningSetElement : meaningSet) {
                    if (meaningSetElement.equals(searchWord)) {
                        isExactMeaningWordsMatch = true;
                        break;
                    }
                }

                //Checking if the romaji is a match
                romaji = word.getRomaji();
                romajiLength = romaji.length();
                isRomajiMatch = romajiLength <= searchWordLength + 1 && romaji.contains(searchWord);

                //Checking if one of the elements of altSpellings is a match
                altSpellings = word.getAltSpellings();
                isAltSpellingsMatch = false;
                for (String altSpelling : altSpellings.split(",")) {
                    if (altSpelling.trim().contains(searchWord)) {
                        isAltSpellingsMatch = true;
                        break;
                    }
                }

                //Setting foundMatch
                if (isExactMeaningWordsMatch || isRomajiMatch || isAltSpellingsMatch) {
                    foundMatch = true;
                }
                else continue;

            }
            //endregion

            //Otherwise, handling longer words
            if (!foundMatch) {
                keywords = word.getKeywords().replace(" ","");
                foundMatch = keywords.contains(searchWordNoSpaces)
                        || keywords.contains(inglessVerb)
                        || queryIsVerbWithTo && keywords.contains(searchWordWithoutTo);
            }


//            //region Loop initializations
//            keywords = word.getKeywords();
//            if (keywords.equals("") || keywords.equals("-") || keywords.equals("KEYWORDS")) continue;
//            if (onlyRetrieveShortRomajiWords && word.getRomaji().length() > 4) continue;
//            keywordsList = Arrays.asList(keywords.split(","));
//            found_match = false;
//            //endregion
//
//            //region Checking if is the word is a verb
//            is_verb = false;
//            for (Word.Meaning meaning : word.getMeanings()) {
//                type = meaning.getType();
//                is_verb = type.substring(0, 1).equals("V") && !type.equals("VC");
//                if (is_verb) break;
//            }
//            //endregion
//
//            //region If there is a word in the keywords that matches the input word, get the corresponding row index
//            match_length = 1000;
//            boolean valueIsInParentheses = false;
//            for (int i = 0; i < keywordsList.size(); i++) {
//
//                // Performing certain actions on the hit to prepare the comparison
//                hit = keywordsList.get(i).trim(); //also trims the extra space before the word
//
//                //region Add "to " to the hit if it's a verb (the "to " was removed to save memory in the database)
//                if (is_verb) {
//                    //Don't add "to " if the word is an explanation in parentheses
//                    if (!valueIsInParentheses) {
//                        hit = "to " + hit;
//                    }
//                    if (hit.contains("(") && !hit.contains(")")) valueIsInParentheses = true;
//                    else if (!hit.contains("(") && hit.contains(")")) valueIsInParentheses = false;
//                }
//                //endregion
//
//                is_verb_and_latin = hit.length() > 3 && hit.substring(0, 3).equals("to ");
//
//                concatenated_hit = Utilities.removeSpecialCharacters(hit);
//                if (TypeisKanji && !ConvertFragment.getTextType(concatenated_hit).equals("kanji") ) { continue; }
//                if (concatenated_hit.length() < concatenated_word_length) { continue; }
//                if (TypeisLatin && word_length == 2 && hit.length() > 2) { continue;}
//                if (TypeisLatin && hit.length() < inglessVerb_length) {continue;}
//
//                if (TypeisLatin) {
//                    hit = hit.toLowerCase(Locale.ENGLISH);
//                    concatenated_hit = concatenated_hit.toLowerCase(Locale.ENGLISH);
//                }
//
//                hit = removeApostrophe(hit);
//                concatenated_hit = removeApostrophe(concatenated_hit);
//
//                //region Getting the first word if the hit is a sentence
//                if (hit.length() > word_length) {
//                    List<String> parsed_hit = Arrays.asList(hit.split(" "));
//                    if (is_verb_and_latin) { hitFirstRelevantWord = parsed_hit.get(1);}
//                    else { hitFirstRelevantWord = parsed_hit.get(0); } // hitFirstWord is the first word of the hit, and shows relevance to the search priority
//                } else {
//                    hitFirstRelevantWord = "";
//                }
//                //endregion
//
//                //region Perform the comparison to the input query and return the length of the shortest hit
//                // Match length is reduced every time there's a hit and the hit is shorter
//                if (       (concatenated_hit.contains(searchWordNoSpaces)
//                        || (TypeisLatin && hit.equals("to " + inglessVerb))
//                        || (!translationLatin.equals("") && concatenated_hit.contains(translationLatin))
//                        || (!searchWordTransliteratedHiragana.equals("") && concatenated_hit.contains(searchWordTransliteratedHiragana))
//                        || (!searchWordTransliteratedKatakana.equals("") && concatenated_hit.contains(searchWordTransliteratedKatakana)))) //ie. if the hit contains the input word, then do the following:
//                    {
//                    if (concatenated_hit.equals(searchWordNoSpaces)) {
//                        best_match = concatenated_hit;
//                        found_match = true;
//                        match_length = best_match.length()-1; // -1 to make sure that it's listed first
//                        if (is_verb_and_latin) { match_length = match_length-3;}
//                        continue;
//                    }
//                    if (hitFirstRelevantWord.contains(searchWordNoSpaces) && hitFirstRelevantWord.length() <= match_length) {
//                        best_match = hitFirstRelevantWord;
//                        found_match = true;
//                        match_length = best_match.length();
//                        continue;
//                    }
//                    if (ConvertFragment.getTextType(concatenated_hit).equals("latin") && hit.length() <= match_length) {
//                        best_match = hit;
//                        found_match = true;
//                        match_length = best_match.length();
//                        if (is_verb_and_latin) { match_length = match_length-3;}
//                    }
//                }
//                //endregion
//                if (found_match) {
//                    break;
//                }
//            }
//            //endregion

            if (foundMatch) {
                current_match_values = new long[2];
                current_match_values[0] = word.getWordId();
                //current_match_values[1] = (long) match_length;
                MatchList.add(current_match_values);
            }
        }

        for (int i=0;i<MatchList.size();i++) {
            matchingWordIds.add(MatchList.get(i)[0]);
        }
        //endregion

        return matchingWordIds;
    }
    private static List<Long> addConjugatedAdjectivesToMatchesList(String searchWord, int inputTextType, List<Long> matchingWordIds,
                                                                  JapaneseToolboxCentralRoomDatabase japaneseToolboxCentralRoomDatabase) {

        //Adding relevant adjectives to the list of matches if the input query is an adjective conjugation
        String input_word = Utilities.removeNonSpaceSpecialCharacters(searchWord);
        String adjectiveConjugation = "";
        String baseAdjective = "";
        boolean isPotentialAdjective = false;
        List<String> searchResultIndexesArray = new ArrayList<>();
        List<Long> matchingWordIdsFromIndex = new ArrayList<>();

        if (inputTextType == GlobalConstants.TYPE_LATIN || inputTextType == GlobalConstants.TYPE_HIRAGANA || inputTextType == GlobalConstants.TYPE_KATAKANA) {

            input_word = ConvertFragment.getLatinHiraganaKatakana(input_word).get(GlobalConstants.TYPE_LATIN);

            if (input_word.length()>9) {
                adjectiveConjugation = input_word.substring(input_word.length()-9, input_word.length());
                baseAdjective = input_word.substring(0, input_word.length()-9) + "i";
                if (adjectiveConjugation.equals("kunakatta")) isPotentialAdjective = true;
            }
            if (!isPotentialAdjective && input_word.length()>6) {
                adjectiveConjugation = input_word.substring(input_word.length()-6, input_word.length());
                baseAdjective = input_word.substring(0, input_word.length()-6) + "i";
                if (adjectiveConjugation.equals("kereba")) isPotentialAdjective = true;
            }
            if (!isPotentialAdjective && input_word.length()>5) {
                adjectiveConjugation = input_word.substring(input_word.length()-5, input_word.length());
                baseAdjective = input_word.substring(0, input_word.length()-5) + "i";
                if (adjectiveConjugation.equals("kunai")
                        || adjectiveConjugation.equals("katta")
                        || adjectiveConjugation.equals("karou")) isPotentialAdjective = true;
            }
            if (!isPotentialAdjective && input_word.length()>4) {
                adjectiveConjugation = input_word.substring(input_word.length()-4, input_word.length());
                baseAdjective = input_word.substring(0, input_word.length()-4) + "i";
                if (adjectiveConjugation.equals("kute")) isPotentialAdjective = true;
            }
            if (!isPotentialAdjective && input_word.length()>2) {
                adjectiveConjugation = input_word.substring(input_word.length()-2, input_word.length());
                if (adjectiveConjugation.equals("mi") || adjectiveConjugation.equals("ku")) {
                    isPotentialAdjective = true;
                    baseAdjective = input_word.substring(0, input_word.length()-2) + "i";
                }
                else if (adjectiveConjugation.equals("ni") || adjectiveConjugation.equals("na") || adjectiveConjugation.equals("de")) {
                    isPotentialAdjective = true;
                    baseAdjective = input_word.substring(0, input_word.length()-2);
                }
            }

            if (!isPotentialAdjective) return matchingWordIds;

            boolean exactSearch = baseAdjective.length() < 3;
            List<LatinIndex> latinIndicesForAdjective = findQueryInLatinIndex(baseAdjective, exactSearch, japaneseToolboxCentralRoomDatabase);

            if (latinIndicesForAdjective.size()==0) return matchingWordIds;

            for (LatinIndex latinIndex : latinIndicesForAdjective) {
                searchResultIndexesArray.add(latinIndex.getWordIds());
            }

        } else if (inputTextType == GlobalConstants.TYPE_KANJI) {

            if (input_word.length()>5) {
                adjectiveConjugation = input_word.substring(input_word.length()-5, input_word.length());
                baseAdjective = input_word.substring(0, input_word.length()-5) + "い";
                if (adjectiveConjugation.equals("くなかった")) isPotentialAdjective = true;
            }
            if (!isPotentialAdjective && input_word.length()>3) {
                adjectiveConjugation = input_word.substring(input_word.length()-3, input_word.length());
                baseAdjective = input_word.substring(0, input_word.length()-3) + "い";
                if (adjectiveConjugation.equals("くない")
                        || adjectiveConjugation.equals("ければ")
                        || adjectiveConjugation.equals("かった")
                        || adjectiveConjugation.equals("かろう")) isPotentialAdjective = true;
            }
            if (!isPotentialAdjective && input_word.length()>2) {
                adjectiveConjugation = input_word.substring(input_word.length()-2, input_word.length());
                baseAdjective = input_word.substring(0, input_word.length()-2) + "い";
                if (adjectiveConjugation.equals("くて")) isPotentialAdjective = true;
            }
            if (!isPotentialAdjective && input_word.length()>1) {
                adjectiveConjugation = input_word.substring(input_word.length()-1, input_word.length());
                if (adjectiveConjugation.equals("み") || adjectiveConjugation.equals("く")) {
                    isPotentialAdjective = true;
                    baseAdjective = input_word.substring(0, input_word.length()-1) + "";
                }
                else if (adjectiveConjugation.equals("に") || adjectiveConjugation.equals("な") || adjectiveConjugation.equals("で")) {
                    isPotentialAdjective = true;
                    baseAdjective = input_word.substring(0, input_word.length()-1);
                }
            }

            if (!isPotentialAdjective) return matchingWordIds;

            List<KanjiIndex> kanjiIndicesForAdjective = findQueryInKanjiIndex(baseAdjective, japaneseToolboxCentralRoomDatabase);

            if (kanjiIndicesForAdjective.size()==0) return matchingWordIds;

            for (KanjiIndex kanjiIndex : kanjiIndicesForAdjective) {
                searchResultIndexesArray.add(kanjiIndex.getWordIds());
            }

        } else {
            return matchingWordIds;
        }

        List<String> indexList;
        for (String searchResultIndexes : searchResultIndexesArray) {
            indexList = Arrays.asList(searchResultIndexes.split(";"));
            for (int j = 0; j < indexList.size(); j++) {
                matchingWordIdsFromIndex.add(Long.valueOf(indexList.get(j)));
            }
        }

        List<Word> matchingPotentialAdjectives = japaneseToolboxCentralRoomDatabase.getWordListByWordIds(matchingWordIdsFromIndex);
        boolean isAlreadyInList;
        for (Word word : matchingPotentialAdjectives) {
            isAlreadyInList = false;
            List<Word.Meaning> meaningsList = word.getMeanings();
            if (meaningsList.size()>0 &&
                    (meaningsList.get(0).getType().equals("Ai") || meaningsList.get(0).getType().equals("Ana")) ) {
                for (long id : matchingWordIds) {
                    if (id == word.getWordId()) {
                        isAlreadyInList = true;
                        break;
                    }
                }
                if (!isAlreadyInList) matchingWordIds.add(word.getWordId());
            }
        }

        return matchingWordIds;
    }
    public static String replaceInvalidKanjisWithValidOnes(String input, List<String[]> mSimilarsDatabase) {
        String output = "";
        char currentChar;
        boolean found;
        for (int i=0; i<input.length(); i++) {
            currentChar = input.charAt(i);
            found = false;
            for (int j = 0; j < mSimilarsDatabase.size(); j++) {
                if (mSimilarsDatabase.get(j).length > 0 && mSimilarsDatabase.get(j)[0].charAt(0) == currentChar) {
                    output += mSimilarsDatabase.get(j)[1].charAt(0);
                    found = true;
                    break;
                }
            }
            if (!found) output += currentChar;
        }
        return output;
    }
    private static Boolean isOfTypeIngIng(String verb) {
        Boolean answer = false;
        if (	verb.equals("accinging") || verb.equals("astringing") || verb.equals("befringing") || verb.equals("besinging") ||
                verb.equals("binging") || verb.equals("boinging") || verb.equals("bowstringing") || verb.equals("bringing") ||
                verb.equals("clinging") || verb.equals("constringing") || verb.equals("cringing") || verb.equals("dinging") ||
                verb.equals("enringing") || verb.equals("flinging") || verb.equals("folksinging") || verb.equals("fringing") ||
                verb.equals("gunslinging") || verb.equals("hamstringing") || verb.equals("handwringing") || verb.equals("hinging") ||
                verb.equals("impinging") || verb.equals("inbringing") || verb.equals("infringing") || verb.equals("kinging") ||
                verb.equals("minging") || verb.equals("mudslinging") || verb.equals("outringing") || verb.equals("outsinging") ||
                verb.equals("outspringing") || verb.equals("outswinging") || verb.equals("outwinging") || verb.equals("overswinging") ||
                verb.equals("overwinging") || verb.equals("perstringing") || verb.equals("pinging") || verb.equals("refringing") ||
                verb.equals("rehinging") || verb.equals("respringing") || verb.equals("restringing") || verb.equals("ringing") ||
                verb.equals("singing") || verb.equals("slinging") || verb.equals("springing") || verb.equals("stinging") ||
                verb.equals("stringing") || verb.equals("swinging") || verb.equals("syringing") || verb.equals("twinging") ||
                verb.equals("unhinging") || verb.equals("unkinging") || verb.equals("unslinging") || verb.equals("unstringing") ||
                verb.equals("upbringing") || verb.equals("upflinging") || verb.equals("upspringing") || verb.equals("upswinging") ||
                verb.equals("whinging") || verb.equals("winging") || verb.equals("wringing") || verb.equals("zinging") ) {
            answer = true;
        }
        return answer;
    }
    private static String removeApostrophes(String sentence) {
        String current_char;
        String concatenated_sentence = "";
        for (int index=0; index<sentence.length(); index++) {
            current_char = Character.toString(sentence.charAt(index));
            if (!( current_char.equals("'")) ) {
                concatenated_sentence = concatenated_sentence + current_char;
            }
        }
        return concatenated_sentence;
    }
    private static List<LatinIndex> findQueryInLatinIndex(String concatenated_word, boolean exactSearch, JapaneseToolboxCentralRoomDatabase japaneseToolboxCentralRoomDatabase) {

        List<LatinIndex> matchingLatinIndexes;
        if (exactSearch) {
            //Preventing the index search from returning too many results and crashing the app
            matchingLatinIndexes = new ArrayList<>();
            LatinIndex index = japaneseToolboxCentralRoomDatabase.getLatinIndexListForExactWord(concatenated_word);
            if (index!=null) matchingLatinIndexes.add(index); //Only add the index if the word was found in the index
            return matchingLatinIndexes;
        } else {
            matchingLatinIndexes = japaneseToolboxCentralRoomDatabase.getLatinIndexesListForStartingWord(concatenated_word);
            return matchingLatinIndexes;
        }
    }
    private static List<KanjiIndex> findQueryInKanjiIndex(String concatenated_word, JapaneseToolboxCentralRoomDatabase japaneseToolboxCentralRoomDatabase) {

        // Prepare the input word to be used in the following algorithm: the word is converted to its hex utf-8 value as a string, in fractional form
        String prepared_word = convertToUTF8Index(concatenated_word);

        List<KanjiIndex> matchingKanjiIndexes = japaneseToolboxCentralRoomDatabase.getKanjiIndexesListForStartingWord(prepared_word);
        return matchingKanjiIndexes;
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
    public static void setAppPreferenceKanjiDatabaseFinishedLoadingFlag(Context context, boolean flag) {
        if (context != null) {
            SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.app_preferences), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(context.getString(R.string.database_finished_loading_flag), flag);
            editor.apply();
        }
    }
    public static boolean getAppPreferenceKanjiDatabaseFinishedLoadingFlag(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.app_preferences), Context.MODE_PRIVATE);
        return sharedPref.getBoolean(context.getString(R.string.database_finished_loading_flag), false);
    }
    public static void setAppPreferenceWordVerbDatabasesFinishedLoadingFlag(Context context, boolean flag) {
        if (context != null) {
            SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.app_preferences), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(context.getString(R.string.word_and_verb_database_finished_loading_flag), flag);
            editor.apply();
        }
    }
    public static boolean getAppPreferenceWordVerbDatabasesFinishedLoadingFlag(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.app_preferences), Context.MODE_PRIVATE);
        return sharedPref.getBoolean(context.getString(R.string.word_and_verb_database_finished_loading_flag), false);
    }

}
