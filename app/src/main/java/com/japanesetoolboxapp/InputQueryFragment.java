package com.japanesetoolboxapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.japanesetoolboxapp.utiities.GlobalConstants;
import com.japanesetoolboxapp.utiities.SharedMethods;
import com.theartofdev.edmodo.cropper.CropImage;

import static android.content.Context.DOWNLOAD_SERVICE;

public class InputQueryFragment extends Fragment implements LoaderManager.LoaderCallbacks<String>,
        TextToSpeech.OnInitListener {

    //Locals
    private static final int RESULT_OK = -1;
    private static final int SPEECH_RECOGNIZER_REQUEST_CODE = 101;
    private static final int ADJUST_IMAGE_ACTIVITY_REQUEST_CODE = 201;
    private static final int WEB_SEARCH_LOADER = 42;
    private static final String SPEECH_RECOGNIZER_EXTRA = "inputQueryAutoCompleteTextView";
    private static final String TAG_TESSERACT = "tesseract_ocr";
    private static final String TAG_PERMISSIONS = "Permission error";
    private static final String DOWNLOAD_FILE_PREFS = "download_file_prefs";
    private static final String JPN_FILE_DOWNLOADING_FLAG = "jpn_file_downloading";
    private static final String ENG_FILE_DOWNLOADING_FLAG = "eng_file_downloading";
    private	String[] output = {"word","","fast"};
    String[] queryHistory;
    ArrayList<String> new_queryHistory;
    AutoCompleteTextView inputQueryAutoCompleteTextView;
    Button button_searchVerb;
    Button button_searchWord;
    Button button_choose_Convert;
    Button button_searchByRadical;
    Button button_Decompose;
    String mQueryText;
    Bitmap mImageToBeDecoded;
    TessBaseAPI mTess;
    String mInternalStorageTesseractFolderPath = "";
    private boolean mInitializedOcrApiJpn;
    private boolean mInitializedOcrApiEng;
    private String mOCRLanguage;
    private String mOCRLanguageLabel;
    Uri mPhotoURI;
    String mOcrResultString;
    private boolean firstTimeInitializedJpn;
    private boolean firstTimeInitializedEng;
    private TextToSpeech tts;
    private boolean mInternetIsAvailable;
    private long enqueue;
    private DownloadManager downloadmanager;
    private boolean hasStoragePermissions;
    private String mPhoneAppFolderTesseractDataFilepath;
    private String mDownloadsFolder;
    private int timesPressed;
    private boolean jpnOcrDataIsAvailable;
    private String mChosenTextToSpeechLanguage;
    private String mLanguageBeingDownloadedLabel;
    private String mLanguageBeingDownloaded;
    private boolean engOcrDataIsAvailable;
    private AsyncTask mTesseractOCRAsyncTask;
    private String mChosenSpeechToTextLanguage;
    private String mDownloadType;
    private boolean jpnOcrFileISDownloading;
    private boolean mJpnOcrFileIsDownloading;
    private boolean mEngOcrFileIsDownloading;
    private CropImage.ActivityResult mCropImageResult;
    private BroadcastReceiver mBroadcastReceiver;
    private boolean requestedSpeechToText;

    //Fragment Lifecycle methods
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setRetainInstance(true);
        final View InputQueryFragment = inflater.inflate(R.layout.fragment_inputquery, container, false);

        // Initializations
        mInternetIsAvailable = SharedMethods.internetIsAvailableCheck(this.getContext());
        inputQueryAutoCompleteTextView = InputQueryFragment.findViewById(R.id.query);
        inputQueryAutoCompleteTextView.setMovementMethod(new ScrollingMovementMethod());
        inputQueryAutoCompleteTextView.setLongClickable(false);
        //inputQueryAutoCompleteTextView.setTextIsSelectable(true);
        inputQueryAutoCompleteTextView.setFocusable(true);
        inputQueryAutoCompleteTextView.setFocusableInTouchMode(true);
        mQueryText = "";
        mOcrResultString = "";
        inputQueryAutoCompleteTextView.setText(mQueryText);
        firstTimeInitializedJpn = true;
        firstTimeInitializedEng = true;
        mInitializedOcrApiJpn = false;
        mInitializedOcrApiEng = false;
        timesPressed = 0;
        jpnOcrDataIsAvailable = false;
        engOcrDataIsAvailable = false;
        requestedSpeechToText = false;
        mCropImageResult = null;

        getOcrDataDownloadingStatus();

        mDownloadType = "WifiOnly";
        getLanguageParametersFromSettingsAndReinitializeOcrIfNecessary();
        setupPaths();
        setupBroadcastReceiverForDownloadedOCRData();
        registerThatUserIsRequestingDictSearch(false);
        tts = new TextToSpeech(getContext(), this);
        mLanguageBeingDownloaded = "jpn";
        ifOcrDataIsNotAvailableThenMakeItAvailable(mLanguageBeingDownloaded);
        mLanguageBeingDownloaded = "eng";
        ifOcrDataIsNotAvailableThenMakeItAvailable(mLanguageBeingDownloaded);
        initializeOcrEngineForChosenLanguage();

        // Restoring query history
        if (queryHistory == null) {
            queryHistory = new String[7];
            for (int i=0;i<queryHistory.length;i++) { queryHistory[i] = ""; } //7 elements in the array
        }
        if (getContext() != null) {
            SharedPreferences sharedPref = getContext().getSharedPreferences(getString(R.string.queryHistoryKey), Context.MODE_PRIVATE);
            String queryHistoryAsString = sharedPref.getString(getString(R.string.queryHistoryKey), "");
            if (!queryHistoryAsString.equals("")) queryHistory = TextUtils.split(queryHistoryAsString,",");
        }
        Log.i("Diagnosis Time", "Loaded Search History.");

        // Populate the history
        inputQueryAutoCompleteTextView.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //inputQueryAutoCompleteTextView.showDropDown();
                inputQueryAutoCompleteTextView.dismissDropDown();
                return false;
            }
            });

        // When Enter is clicked, do the actions described in the following function
        inputQueryAutoCompleteTextView.setOnEditorActionListener(new EditText.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String inputWordString = inputQueryAutoCompleteTextView.getText().toString();

                updateQueryHistory(inputWordString, inputQueryAutoCompleteTextView);
                inputQueryAutoCompleteTextView.dismissDropDown();

                registerThatUserIsRequestingDictSearch(true);
                onWordEntered_PerformThisFunction(inputWordString);
            }
            return true;
        } } );

        // Button listeners
        button_searchVerb = InputQueryFragment.findViewById(R.id.button_searchVerb);
        button_searchVerb.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {
            //EditText inputVerbObject = (EditText)fragmentView.findViewById(R.id.input_verb);
            String inputVerbString = inputQueryAutoCompleteTextView.getText().toString();
            mQueryText = inputVerbString;
            updateQueryHistory(inputVerbString, inputQueryAutoCompleteTextView);

            // Check if the database has finished loading. If not, make the user wait.
            while(MainActivity.VerbKanjiConjDatabase == null){
                new CountDownTimer(500, 500) {
                    public void onFinish() {
                        // When timer is finished
                        // Execute your code here
                    }

                    public void onTick(long millisUntilFinished) {
                        // millisUntilFinished    The amount of time until finished.
                    }
                }.start();
            }
            registerThatUserIsRequestingDictSearch(false);
            onVerbEntered_PerformThisFunction(SharedMethods.removeSpecialCharacters(inputVerbString));
        } } );

        button_searchWord = InputQueryFragment.findViewById(R.id.button_searchWord);
        button_searchWord.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {
            String inputWordString = inputQueryAutoCompleteTextView.getText().toString();
            mQueryText = inputWordString;
            updateQueryHistory(inputWordString, inputQueryAutoCompleteTextView);

            // Check if the database has finished loading. If not, make the user wait.
            while(MainActivity.VerbKanjiConjDatabase == null){
                new CountDownTimer(500, 500) {
                    public void onFinish() {
                        // When timer is finished
                        // Execute your code here
                    }

                    public void onTick(long millisUntilFinished) {
                        // millisUntilFinished    The amount of time until finished.
                    }
                }.start();
            }
            registerThatUserIsRequestingDictSearch(true);
            onWordEntered_PerformThisFunction(SharedMethods.removeSpecialCharacters(inputWordString));
        } } );

        button_choose_Convert = InputQueryFragment.findViewById(R.id.button_convert);
        button_choose_Convert.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {
            String inputWordString = inputQueryAutoCompleteTextView.getText().toString();

            updateQueryHistory(inputWordString, inputQueryAutoCompleteTextView);

            onConvertEntered_PerformThisFunction(inputWordString);
        } } );

        //button_searchTangorin = InputQueryFragment.findViewById(R.id.button_searchTangorin);
//        button_searchTangorin.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {
//            // Search using Tangorin
//            String queryString = inputQueryAutoCompleteTextView.getText().toString();
//
//            updateQueryHistory(queryString, inputQueryAutoCompleteTextView);
//
//            String website = "http://www.tangorin.com/general/" + queryString;
//            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(website));
//            startActivity(intent);
//        } } );

        button_searchByRadical = InputQueryFragment.findViewById(R.id.button_searchByRadical);
        button_searchByRadical.setEnabled(true);
        button_searchByRadical.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {
            // Break up a Kanji to Radicals

            String inputWordString = inputQueryAutoCompleteTextView.getText().toString();

            updateQueryHistory(inputWordString, inputQueryAutoCompleteTextView);

            // Check if the database has finished loading. If not, make the user wait.
            while(MainActivity.RadicalsOnlyDatabase == null && MainActivity.enough_memory_for_heavy_functions) {
                new CountDownTimer(200000, 10000) {
                    public void onFinish() {
                    }
                    public void onTick(long millisUntilFinished) {
                        // millisUntilFinished    The amount of time until finished.
                    }
                }.start();
                //heap_size = AvailableMemory();
            }
            Log.i("Diagnosis Time","Starting radical module.");

            // If the app memory is too low to load the radicals and decomposition databases, make the searchByRadical and Decompose buttons inactive
            if (MainActivity.heap_size_before_searchbyradical_loader < GlobalConstants.CHAR_COMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE) {
                Toast.makeText(getActivity(), "Sorry, your device does not have enough memory to run this function.", Toast.LENGTH_LONG).show();
            }
            else {
                onSearchByRadicalsEntered_PerformThisFunction(SharedMethods.removeSpecialCharacters(inputWordString));
            }
        } } );

        button_Decompose = InputQueryFragment.findViewById(R.id.button_Decompose);
        button_Decompose.setEnabled(true);
        button_Decompose.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {
            // Break up a Kanji to Radicals

            String inputWordString = inputQueryAutoCompleteTextView.getText().toString();

            updateQueryHistory(inputWordString, inputQueryAutoCompleteTextView);

            // Check if the database has finished loading. If not, make the user wait.
            while(MainActivity.RadicalsOnlyDatabase == null && MainActivity.enough_memory_for_heavy_functions) {
                new CountDownTimer(200000, 10000) {
                    public void onFinish() {
                    }
                    public void onTick(long millisUntilFinished) {
                        // millisUntilFinished    The amount of time until finished.
                    }
                }.start();
                //heap_size = AvailableMemory();
            }
            Log.i("Diagnosis Time","Starting decomposition module.");

            // If the app memory is too low to load the radicals and decomposition databases, make the searchByRadical and Decompose buttons inactive
            if (MainActivity.heap_size_before_decomposition_loader < GlobalConstants.DECOMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE) {
                Toast.makeText(getActivity(), "Sorry, your device does not have enough memory to run this function.", Toast.LENGTH_LONG).show();
            }
            else {
                onDecomposeEntered_PerformThisFunction(SharedMethods.removeSpecialCharacters(inputWordString));
            }
        } } );

        final Button button_ClearQuery = InputQueryFragment.findViewById(R.id.clearQuery);
        button_ClearQuery.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {
            inputQueryAutoCompleteTextView.setText("");
            mQueryText = "";
        } } );

        final Button button_ShowHistory = InputQueryFragment.findViewById(R.id.showHistory);
        button_ShowHistory.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {

            String queryString = inputQueryAutoCompleteTextView.getText().toString();
            mQueryText = queryString;
            updateQueryHistory(queryString, inputQueryAutoCompleteTextView);
            boolean queryHistoryIsEmpty = true;
            for (String element : queryHistory) {
                if (!element.equals("")) { queryHistoryIsEmpty = false; }
            }
            if (!queryHistoryIsEmpty) inputQueryAutoCompleteTextView.showDropDown();
        } } );

        final ImageView button_Copy = InputQueryFragment.findViewById(R.id.copyQuery);
        button_Copy.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            String queryString = inputQueryAutoCompleteTextView.getText().toString();
            mQueryText = queryString;
            if (getActivity() != null) {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Copied Text", inputQueryAutoCompleteTextView.getText().toString());
                if (clipboard != null) clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), getResources().getString(R.string.copiedTextToClipboard), Toast.LENGTH_SHORT).show();
            }
        } } );

        final ImageView button_Paste = InputQueryFragment.findViewById(R.id.pasteToQuery);
        button_Paste.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {

            if (getActivity() != null) {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    if (clipboard.hasPrimaryClip()) {
                        android.content.ClipDescription description = clipboard.getPrimaryClipDescription();
                        android.content.ClipData data = clipboard.getPrimaryClip();
                        if (data != null && description != null && description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))
                            inputQueryAutoCompleteTextView.setText(String.valueOf(data.getItemAt(0).getText()));
                    }
                }
            }

        } } );

        final ImageView button_SpeechToText = InputQueryFragment.findViewById(R.id.getTextThroughSpeech);
        button_SpeechToText.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {

            int maxResultsToReturn = 1;
            try {
                //Getting the user setting from the preferences
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String language = sharedPreferences.getString(getString(R.string.pref_preferred_STT_language_key), getString(R.string.pref_preferred_language_value_japanese));
                mChosenSpeechToTextLanguage = getSpeechToTextLanguageLocale(language);

                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResultsToReturn);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, mChosenSpeechToTextLanguage);
                intent.putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, getResources().getString(R.string.languageLocaleEnglishUS));
                if (MainActivity.mChosenSpeechToTextLanguage.equals(getResources().getString(R.string.languageLocaleJapanese))) {
                    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getResources().getString(R.string.SpeechToTextUserPromptJapanese));
                } else if (MainActivity.mChosenSpeechToTextLanguage.equals(getResources().getString(R.string.languageLocaleEnglishUS))) {
                    intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getResources().getString(R.string.SpeechToTextUserPromptEnglish));
                }
                startActivityForResult(intent, SPEECH_RECOGNIZER_REQUEST_CODE);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getContext(),getResources().getString(R.string.STTACtivityNotFound),Toast.LENGTH_SHORT).show();
                String appPackageName = "com.google.android.tts";
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,   Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName));
                startActivity(browserIntent);
            }
        } } );

        final ImageView button_OCR = InputQueryFragment.findViewById(R.id.getTextThroughCamera);
        button_OCR.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            getOcrDataDownloadingStatus();
            if ((mOCRLanguage.equals("eng") && mEngOcrFileIsDownloading) || (mOCRLanguage.equals("jpn") && mJpnOcrFileIsDownloading) ) {
                Toast toast = Toast.makeText(getContext(),getResources().getString(R.string.OCR_downloading), Toast.LENGTH_SHORT);
                toast.show();
            }
            else {
                if (mInitializedOcrApiJpn && mOCRLanguage.equals("jpn")) {
                    String toastString = "";
                    if (firstTimeInitializedJpn) {
                        toastString = getResources().getString(R.string.OCRinstructionsJPN);
                        Toast toast = Toast.makeText(getActivity(), toastString, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
                        toast.show();
                        firstTimeInitializedJpn = false;
                    }
                    timesPressed = 0;
                    performImageCaptureAndCrop();
                } else if (mInitializedOcrApiEng && mOCRLanguage.equals("eng")) {
                    String toastString = "";
                    if (firstTimeInitializedEng) {
                        toastString = getResources().getString(R.string.OCRinstructionsENG);
                        Toast toast = Toast.makeText(getActivity(), toastString, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
                        toast.show();
                        firstTimeInitializedEng = false;
                    }
                    timesPressed = 0;
                    performImageCaptureAndCrop();
                } else {
                    if (timesPressed <= 3) {
                        mLanguageBeingDownloaded = "jpn";
                        ifOcrDataIsNotAvailableThenMakeItAvailable(mLanguageBeingDownloaded);
                        mLanguageBeingDownloaded = "eng";
                        ifOcrDataIsNotAvailableThenMakeItAvailable(mLanguageBeingDownloaded);

                        initializeOcrEngineForChosenLanguage();

                        mInitializedOcrApiJpn = true;
                        timesPressed++; //Prevents multiple clicks on the button from freezing the app
                    }
                    Toast toast = Toast.makeText(getContext(), getResources().getString(R.string.OCR_reinitializing), Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        } } );

        final ImageView button_TextToSpeech = InputQueryFragment.findViewById(R.id.speakQuery);
        button_TextToSpeech.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            String queryString = inputQueryAutoCompleteTextView.getText().toString();
            mQueryText = queryString;
            speakOut(queryString);
        } } );

        return InputQueryFragment;
    }
    @Override public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof UserEnteredQueryListener) {
            userEnteredQueryListener = (UserEnteredQueryListener) context;
        }
        else {
            throw new ClassCastException(context.toString()
                    + " must implement InputQueryFragment.UserEnteredQueryListener");
        }

    }
    @Override public void onResume() {
        super.onResume();
        inputQueryAutoCompleteTextView.setText(mQueryText);
        inputQueryAutoCompleteTextView.clearFocus();
        if (getActivity() != null) getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        getLanguageParametersFromSettingsAndReinitializeOcrIfNecessary();
    }
    @Override public void onDestroy() {
        super.onDestroy();
        if (mTess != null) mTess.end();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (getActivity() != null && mBroadcastReceiver != null) getActivity().unregisterReceiver(mBroadcastReceiver);

    }
    @Override public void onPause() {
        super.onPause();
        mQueryText = inputQueryAutoCompleteTextView.getText().toString();

        //Save the query history
        if (getContext() != null) {
            String queryHistoryAsString = TextUtils.join(",", queryHistory);
            SharedPreferences sharedPref = getContext().getSharedPreferences(getString(R.string.queryHistoryKey), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(getString(R.string.queryHistoryKey), queryHistoryAsString);
            editor.apply();
        }
    }
    @Override public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        if (getActivity() != null) {
            EditText query = getActivity().findViewById(R.id.query);
            savedInstanceState.putString("inputQueryAutoCompleteTextView", query.getText().toString());
        }
        savedInstanceState.putStringArray("queryHistory", queryHistory);
    }
    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_RECOGNIZER_REQUEST_CODE) {

            if (data == null) return;
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            mQueryText = results.get(0);
            inputQueryAutoCompleteTextView.setText(mQueryText);

            //Attempting to access jisho.org to get the romaji value of the requested word, if it's a Japanese word
            if (getActivity() != null && MainActivity.mChosenSpeechToTextLanguage.equals(getResources().getString(R.string.languageLocaleJapanese))) {
                Bundle queryBundle = new Bundle();
                queryBundle.putString(SPEECH_RECOGNIZER_EXTRA, results.get(0));

                LoaderManager loaderManager = getActivity().getSupportLoaderManager();
                Loader<String> WebSearchLoader = loaderManager.getLoader(WEB_SEARCH_LOADER);
                if (WebSearchLoader == null)
                    loaderManager.initLoader(WEB_SEARCH_LOADER, queryBundle, this);
                else loaderManager.restartLoader(WEB_SEARCH_LOADER, queryBundle, this);
            }
        }
        else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                mCropImageResult = result;
                adjustImageBeforeOCR(mCropImageResult);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
        else if (requestCode == ADJUST_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Uri adjustedImageUri = Uri.parse(extras.getString("returnImageUri"));
                    mImageToBeDecoded = getImageFromUri(adjustedImageUri);
                    getOcrTextWithTesseractAndDisplayDialog(mImageToBeDecoded);
                }
            }
        }
    }

    //Image methods
    private void adjustImageBeforeOCR(CropImage.ActivityResult result) {
        mPhotoURI = result.getUri();
        mImageToBeDecoded = getImageFromUri(mPhotoURI);

        //Send the image Uri to the AdjustImageActivity
        Intent intent = new Intent(getActivity(), AdjustImageActivity.class);
        intent.putExtra("imageUri", mPhotoURI.toString());
        startActivityForResult(intent, ADJUST_IMAGE_ACTIVITY_REQUEST_CODE);
    }
    private void performImageCaptureAndCrop() {

        // start source picker (camera, gallery, etc..) to get image for cropping and then use the image in cropping activity
        //CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(getActivity());
        if (getContext() != null) CropImage.activity().start(getContext(), this); //For FragmentActivity use

    }
    private Bitmap getImageFromUri(Uri resultUri) {
        Bitmap imageToBeDecoded = null;
        try {
            //BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            //bmOptions.inJustDecodeBounds = false;
            //image = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
            if (getActivity() != null) imageToBeDecoded = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), resultUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageToBeDecoded;
    }
    private Bitmap adjustImageAfterOCR(Bitmap imageToBeDecoded) {
        //imageToBeDecoded = adjustImageAngleAndScale(imageToBeDecoded, 0, 0.5);
        return imageToBeDecoded;
    }
    private Bitmap adjustImageAngleAndScale(Bitmap source, float angle, double scaleFactor) {

        int newWidth = (int) Math.floor(source.getWidth()*scaleFactor);
        int newHeight = (int) Math.floor(source.getHeight()*scaleFactor);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(source, newWidth, newHeight,true);

        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(scaledBitmap , 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true); //rotated Bitmap

    }

    //Setup methods
    private void getOcrDataDownloadingStatus() {

        if (getContext() != null) {
            SharedPreferences sharedPreferences = getContext().getSharedPreferences(DOWNLOAD_FILE_PREFS, Context.MODE_PRIVATE);
            mJpnOcrFileIsDownloading = sharedPreferences.getBoolean(JPN_FILE_DOWNLOADING_FLAG, false);
            mEngOcrFileIsDownloading = sharedPreferences.getBoolean(ENG_FILE_DOWNLOADING_FLAG, false);
        }
    }
    public String getOCRLanguageFromSettings() {
        if (getActivity() != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String language = sharedPreferences.getString(getString(R.string.pref_preferred_OCR_language_key), getString(R.string.pref_preferred_language_value_japanese));

            if (language.equals(getResources().getString(R.string.pref_preferred_language_value_japanese))) {
                return "jpn";
            } else if (language.equals(getResources().getString(R.string.pref_preferred_language_value_english))) {
                return "eng";
            } else return "jpn";
        }
        else return "jpn";
    }
    private void setupPaths() {
        if (getActivity() != null) {
            //mInternalStorageTesseractFolderPath = Environment.getExternalStoragePublicDirectory(Environment.).getAbsolutePath() + "/";
            mInternalStorageTesseractFolderPath = getActivity().getFilesDir() + "/tesseract/";
            mPhoneAppFolderTesseractDataFilepath = mInternalStorageTesseractFolderPath + "tessdata/";
            mDownloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/";
        }
    }
    private void getLanguageParametersFromSettingsAndReinitializeOcrIfNecessary() {
        String newLanguage = getOCRLanguageFromSettings();
        if (mOCRLanguage != null && !mOCRLanguage.equals(newLanguage)) initializeTesseractAPI(newLanguage);
        mOCRLanguage = newLanguage;
        mOCRLanguageLabel = getLanguageLabel(mOCRLanguage);
    }
    public String getLanguageLabel(String language) {
        if (language.equals("jpn")) return getResources().getString(R.string.language_label_japanese);
        else return getResources().getString(R.string.language_label_english);
    }
    public void setupBroadcastReceiverForDownloadedOCRData() {
        mBroadcastReceiver = new BroadcastReceiver() {
            //https://stackoverflow.com/questions/38563474/how-to-store-downloaded-image-in-internal-storage-using-download-manager-in-andr
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(enqueue);
                    Cursor c = downloadmanager.query(query);
                    if (c.moveToFirst()) {
                        int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {

                            jpnOcrFileISDownloading = false;
                            setOcrDataIsDownloadingStatus(mLanguageBeingDownloaded+".traineddata", false);

                            String uriString = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                            Uri a = Uri.parse(uriString);
                            File d = new File(a.getPath());
                            // copy file from external to internal will easily available on net use google.
                            //String sdCard = Environment.getExternalStorageDirectory().toString();
                            File sourceLocation = new File(mDownloadsFolder + "/" + mLanguageBeingDownloaded + ".traineddata");
                            File targetLocation = new File(mPhoneAppFolderTesseractDataFilepath);
                            moveFile(sourceLocation, targetLocation);

                            initializeOcrEngineForChosenLanguage();
                        }
                    }
                }
            }
        };
        if (getActivity() != null) getActivity().registerReceiver(mBroadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }
    private void initializeOcrEngineForChosenLanguage() {
        mOCRLanguage = getOCRLanguageFromSettings();
        if (mOCRLanguage.equals("jpn") && jpnOcrDataIsAvailable) initializeTesseractAPI(mOCRLanguage);
        else if (mOCRLanguage.equals("eng") && engOcrDataIsAvailable) initializeTesseractAPI(mOCRLanguage);
    }
    private void ifOcrDataIsNotAvailableThenMakeItAvailable(String language) {

        String filename = language + ".traineddata";
        Boolean fileExistsInAppFolder = checkIfFileExistsInSpecificFolder(new File(mPhoneAppFolderTesseractDataFilepath), filename);
        mInitializedOcrApiJpn = false;
        if (!fileExistsInAppFolder) {
            hasStoragePermissions = checkStoragePermission();
            makeOcrDataAvailableInAppFolder(language);
        }
        else {
            if (language.equals("jpn")) jpnOcrDataIsAvailable = true;
            else if (language.equals("eng")) engOcrDataIsAvailable = true;
        }
    }
    public void makeOcrDataAvailableInAppFolder(String language) {

        mLanguageBeingDownloadedLabel = getLanguageLabel(language);
        String filename = language + ".traineddata";
        Boolean fileExistsInPhoneDownloadsFolder = checkIfFileExistsInSpecificFolder(new File(mDownloadsFolder), filename);
        if (hasStoragePermissions) {
            if (fileExistsInPhoneDownloadsFolder) {
                Log.e(TAG_TESSERACT, filename + " file successfully found in Downloads folder.");
                File sourceLocation = new File(mDownloadsFolder + filename);
                File targetLocation = new File(mPhoneAppFolderTesseractDataFilepath);
                moveFile(sourceLocation, targetLocation);
            } else {
                jpnOcrDataIsAvailable = false;
                if (!jpnOcrFileISDownloading) askForPreferredDownloadTimeAndDownload(language, filename);
            }
        }
    }
    @NonNull private Boolean checkIfFileExistsInSpecificFolder(File dir, String filename) {

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
    private void copyFileFromAssets(String language) {
        try {
            String filepath = mInternalStorageTesseractFolderPath + "/tessdata/" + language + ".traineddata";
            AssetManager assetManager = getActivity().getAssets();

            InputStream instream = assetManager.open("tessdata/" +language+ ".traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }

            outstream.flush();
            outstream.close();
            instream.close();

            File file = new File(filepath);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
    private void moveFile(File file, File dir) {
        File newFile = new File(dir, file.getName());
        FileChannel outputChannel = null;
        FileChannel inputChannel = null;
        //TODO: make sure the file completely finished downloading before allowing the program to continue
        try {
            outputChannel = new FileOutputStream(newFile).getChannel();
            inputChannel = new FileInputStream(file).getChannel();
            inputChannel.transferTo(0, inputChannel.size(), outputChannel);
            inputChannel.close();
            jpnOcrDataIsAvailable = true;
            Toast.makeText(getContext(),getResources().getString(R.string.OCR_copy_data), Toast.LENGTH_SHORT).show();
            Log.v(TAG_TESSERACT, "Successfully moved data file to app folder.");
            //file.delete();
        } catch (IOException e) {
            jpnOcrDataIsAvailable = false;
            Log.v(TAG_TESSERACT, "Copy file failed.");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputChannel != null) inputChannel.close();
                if (outputChannel != null) outputChannel.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

    }
    public boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (getActivity() != null) {
                if (getActivity().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG_PERMISSIONS, "You have permission");
                    return true;
                } else {
                    Log.e(TAG_PERMISSIONS, "You have asked for permission");
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                    return false;
                }
            }
            else return false;
        }
        else { //you dont need to worry about these stuff below api level 23
            Log.e(TAG_PERMISSIONS,"You already have the permission");
            return true;
        }
    }
    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            hasStoragePermissions = true;

            Log.e(TAG_PERMISSIONS,"Returned from permission request.");
            makeOcrDataAvailableInAppFolder(mLanguageBeingDownloaded);
            if (!mInitializedOcrApiJpn) initializeOcrEngineForChosenLanguage();
        }
    }
    public void askForPreferredDownloadTimeAndDownload(String language, final String filename) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.OCRDialogTitle);
        builder.setPositiveButton(R.string.DownloadDialogWifiOnly, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mDownloadType = "WifiOnly";

                Boolean hasMemory = checkIfStorageSpaceTooLowOrShowApology();
                Log.e(TAG_TESSERACT, "File not found in Downloads folder.");
                //if (!fileExistsInInternalStorage) copyFileFromAssets(mOCRLanguage);
                if (hasMemory) downloadFileToDownloadsFolder("https://github.com/tesseract-ocr/tessdata/raw/master/", filename);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.DownloadDialogWifiAndMobile, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mDownloadType = "WifiAndMobile";

                Boolean hasMemory = checkIfStorageSpaceTooLowOrShowApology();
                Log.e(TAG_TESSERACT, "File not found in Downloads folder.");
                //if (!fileExistsInInternalStorage) copyFileFromAssets(mOCRLanguage);
                if (hasMemory) downloadFileToDownloadsFolder("https://github.com/tesseract-ocr/tessdata/raw/master/", filename);
                dialog.dismiss();
            }
        });
        if (language.equals("jpn")) builder.setMessage(R.string.DownloadDialogMessageJPN);
        else builder.setMessage(R.string.DownloadDialogMessageENG);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    public Boolean checkIfStorageSpaceTooLowOrShowApology() {
        //https://inducesmile.com/android/how-to-get-android-ram-internal-and-external-memory-information/
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        long availableMemory = availableBlocks * blockSize;
        if (availableMemory<70000000) {
            String toastMessage = "Sorry, you only have " + formatSize(availableMemory) + "MB left in internal memory and can't install the OCR data." +
                    "Please clear some app data and try again.";
            Toast.makeText(getActivity(), toastMessage, Toast.LENGTH_SHORT).show();
            return false;
        }
        else return true;
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

    //Tesseract OCR methods
    private void initializeTesseractAPI(String language) {
        //mImageToBeDecoded = BitmapFactory.decodeResource(getResources(), R.drawable.test_image);

        mInitializedOcrApiJpn = false;
        if (getActivity()!=null) {
            //Download language files from https://github.com/tesseract-ocr/tesseract/wiki/Data-Files
            try {
                mTess = new TessBaseAPI();
                mTess.init(mInternalStorageTesseractFolderPath, language);
                if (language.equals("jpn") ) mInitializedOcrApiJpn = true;
                else if (language.equals("eng") ) mInitializedOcrApiEng = true;
                Log.e(TAG_TESSERACT, "Initialized Tesseract.");
            } catch (Exception e) {
                Log.e(TAG_TESSERACT, "Failed to initialize Tesseract.");
                Toast.makeText(getContext(),getResources().getString(R.string.OCR_failed), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }
    public void getOcrTextWithTesseractAndDisplayDialog(Bitmap imageToBeDecoded){
        mTess.setImage(imageToBeDecoded);
        mTesseractOCRAsyncTask = new TesseractOCRAsyncTask(getActivity()).execute();
    }
    private void downloadFileToDownloadsFolder(String source, String filename) {

        setOcrDataIsDownloadingStatus(filename, true);
        Log.e(TAG_TESSERACT, "Attempting file download");

        String url = source + filename;

        //https://stackoverflow.com/questions/38563474/how-to-store-downloaded-image-in-internal-storage-using-download-manager-in-andr
        downloadmanager = (DownloadManager) getActivity().getSystemService(DOWNLOAD_SERVICE);
        Uri downloadUri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(downloadUri);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
        if (mDownloadType.equals("WifiOnly")) {
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
            Toast.makeText(getActivity(), "Downloading the " + mLanguageBeingDownloadedLabel + " OCR data on Wifi only.", Toast.LENGTH_SHORT).show();
        }
        else {
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            Toast.makeText(getActivity(), "Attempting to download the " + mLanguageBeingDownloadedLabel + " OCR data. Please wait.", Toast.LENGTH_SHORT).show();
        }
        request.setAllowedOverRoaming(false);
        request.setTitle("Downloading " + filename);
        request.setVisibleInDownloadsUi(true);
        enqueue = downloadmanager.enqueue(request);


        //Finished download activates the broadcast receiver, that in turn initializes the Tesseract API

    }
    private class TesseractOCRAsyncTask extends AsyncTask {

        private Activity mActivity;
        private ProgressDialog mProgressDialog;

        TesseractOCRAsyncTask(Activity activity) {
            this.mActivity = activity;
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(mActivity);
            mProgressDialog.setTitle(getResources().getString(R.string.OCR_waitWhileProcessing));
            mProgressDialog.setMessage(getResources().getString(R.string.OCR_waitWhileProcessingExplanation));
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mTess.stop();
                    initializeTesseractAPI(mOCRLanguage);
                    mTesseractOCRAsyncTask.cancel(true);
                    dialog.dismiss();
                }
            });
//            mProgressDialog.setButton(DialogInterface.BUTTON_NEUTRAL, getResources().getString(R.string.readjust), new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    mTess.stop();
//                    initializeTesseractAPI(mOCRLanguage);
//                    mTesseractOCRAsyncTask.cancel(true);
//                    if (mCropImageResult != null) adjustImageBeforeOCR(mCropImageResult);
//                    dialog.dismiss();
//                }
//            });
            mProgressDialog.show();
            super.onPreExecute();
        }
        @Override
        protected String doInBackground(Object[] params) {
            String result = mTess.getUTF8Text();
            //String result = mTess.getHOCRText(0);
            return result;
        }
        @Override
        protected void onPostExecute(Object result) {
            super.onPostExecute(result);
            if (mProgressDialog != null && mProgressDialog.isShowing()) mProgressDialog.dismiss();
            mOcrResultString = (String) result;
            createOcrListDialog(mOcrResultString);
        }
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP) private void createOcrListDialog(String ocrResult) {

        if (getActivity()==null) return;

        mImageToBeDecoded = adjustImageAfterOCR(mImageToBeDecoded);

        //Setting the elements in the dialog
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View dialogView = inflater.inflate(R.layout.custom_ocr_dialog, null);

        ImageView ocrPictureHolder = dialogView.findViewById(R.id.ocrPicture);
        ocrPictureHolder.setImageBitmap(mImageToBeDecoded);
        //CropImageView cropImageView = dialogView.findViewById(R.id.cropImageView);
        //cropImageView.setImageUriAsync(mPhotoURI);
        //mImageToBeDecoded = cropImageView.getCroppedImage();

        ListView ocrResultsListView = dialogView.findViewById(R.id.ocrResultsList);
        final String ocrResultsList[] = ocrResult.split("\\r\\n|\\n|\\r");
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<> (getActivity(), android.R.layout.simple_list_item_1, ocrResultsList);

        mQueryText = ocrResultsList[0];
        inputQueryAutoCompleteTextView.setText(mQueryText);
        ocrResultsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                inputQueryAutoCompleteTextView.setText(ocrResultsList[position]);
            }
        });
        ocrResultsListView.setAdapter(arrayAdapter);

        //Building the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.OCRDialogTitle);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        builder.setNeutralButton(R.string.readjust,new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (mCropImageResult != null) adjustImageBeforeOCR(mCropImageResult);
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setView(dialogView);
        }
        else { builder.setMessage(R.string.LowVersionDialogMessage); }
        AlertDialog dialog = builder.create();

        //scaleImage(ocrPictureHolder, 1);
        dialog.show();
    }

    //TextToSpeech methods
    @Override public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            setTTSLanguage();
        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }
    private void setTTSLanguage() {
        int result;
        //Getting the user setting from the preferences
        if (getActivity() != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String language = sharedPreferences.getString(getString(R.string.pref_preferred_TTS_language_key), getString(R.string.pref_preferred_language_value_japanese));
            mChosenTextToSpeechLanguage = getTextToSpeechLanguageLocale(language);

            //Setting the language
            if (mChosenTextToSpeechLanguage.equals(getResources().getString(R.string.languageLocaleJapanese))) {
                result = tts.setLanguage(Locale.JAPAN);
            } else if (mChosenTextToSpeechLanguage.equals(getResources().getString(R.string.languageLocaleEnglishUS))) {
                result = tts.setLanguage(Locale.US);
            } else result = tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported, set to default English.");
                tts.setLanguage(Locale.US);
            }
        }
    }
    private void speakOut(String text) {
        setTTSLanguage();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }
    public String getTextToSpeechLanguageLocale(String language) {
        if (language.equals(getResources().getString(R.string.pref_preferred_language_value_japanese))) {
            return getResources().getString(R.string.languageLocaleJapanese);
        }
        else if (language.equals(getResources().getString(R.string.pref_preferred_language_value_english))) {
            return getResources().getString(R.string.languageLocaleEnglishUS);
        }
        else return getResources().getString(R.string.languageLocaleEnglishUS);
    }

    //SpeechToText methods
    public String getSpeechToTextLanguageLocale(String language) {
        if (language.equals(getResources().getString(R.string.pref_preferred_language_value_japanese))) {
            return getResources().getString(R.string.languageLocaleJapanese);
        }
        else if (language.equals(getResources().getString(R.string.pref_preferred_language_value_english))) {
            return getResources().getString(R.string.languageLocaleEnglishUS);
        }
        else return getResources().getString(R.string.languageLocaleEnglishUS);
    }
    @Override public Loader<String> onCreateLoader(int id, final Bundle args) {
        return new AsyncTaskLoader<String>(getContext()) {

            @Override
            protected void onStartLoading() {
                /* If no arguments were passed, we don't have a inputQueryAutoCompleteTextView to perform. Simply return. */
                if (args == null) return;
                forceLoad();
            }

            @Override
            public String loadInBackground() {
                //This method retrieves the first romaji transliteration of the kanji searched word.

                List<Object> AsyncMatchingWordCharacteristics = new ArrayList<>();
                if (mInternetIsAvailable) {
                    try {
                        String speechRecognizerString = args.getString(SPEECH_RECOGNIZER_EXTRA);
                        AsyncMatchingWordCharacteristics = SharedMethods.getResultsFromWeb(speechRecognizerString, getActivity());
                    } catch (IOException e) {
                        cancelLoadInBackground();
                        //throw new RuntimeException(e);
                    }
                    if (AsyncMatchingWordCharacteristics.size() != 0 && requestedSpeechToText) {
                        List<String> results = (List<String>) AsyncMatchingWordCharacteristics.get(0);
                        mQueryText = results.get(0);
                        return mQueryText;
                    } else return null;
                }
                else return null;
            }
        };
    }
    @Override public void onLoadFinished(Loader<String> loader, String data) {
        if (data!=null) {
            inputQueryAutoCompleteTextView.setText(data);
        }
        if (!inputQueryAutoCompleteTextView.getText().toString().equals("")) {
            button_searchWord.performClick();
        }
    }
    @Override public void onLoaderReset(Loader<String> loader) {

    }

    //Query input methods
    private class QueryInputSpinnerAdapter extends ArrayAdapter<String> {
    // Code adapted from http://mrbool.com/how-to-customize-spinner-in-android/28286
        QueryInputSpinnerAdapter(Context ctx, int txtViewResourceId, List<String> list) {
            super(ctx, txtViewResourceId, list);
            }
        @Override
        public View getDropDownView( int position, View cnvtView, ViewGroup prnt) {
            return getCustomView(position, cnvtView, prnt);
        }
        @NonNull
        @Override
        public View getView(int pos, View cnvtView, @NonNull ViewGroup prnt) {
            return getCustomView(pos, cnvtView, prnt);
        }
        View getCustomView(int position, View convertView, ViewGroup parent) {

            if (getActivity() != null) {
                LayoutInflater inflater = LayoutInflater.from(getActivity().getBaseContext());
                View mySpinner = inflater.inflate(R.layout.custom_queryhistory_spinner, parent, false);
                TextView pastquery = mySpinner.findViewById(R.id.pastQuery);
                pastquery.setText(new_queryHistory.get(position));
                return mySpinner;
            }
            else return null;
        }
    }
    public void updateQueryHistory(String queryStr, final AutoCompleteTextView query) {

        // Implementing a FIFO array for the spinner
        // Add the entry at the beginning of the stack
        // If the entry is already in the spinner, remove that entry

        if (!queryHistory[0].equals(queryStr)) { // if the inputQueryAutoCompleteTextView hasn't changed, don't change the dropdown list
            String temp;
            int i = queryHistory.length-1;
            while ( i>0 ) {
                temp = queryHistory[i-1];
                queryHistory[i] = temp;
                if (queryHistory[i].equalsIgnoreCase(queryStr)) { queryHistory[i]=""; }
                i--;
            }
            queryHistory[0]=queryStr;
        }

        new_queryHistory = new ArrayList<>();
        for (String aQueryHistory : queryHistory) {
            if (!aQueryHistory.equals("")) {
                new_queryHistory.add(aQueryHistory);
            }
        }

        // Set the dropdown main to include all past entries
        if (getActivity() != null) {

            query.setAdapter(new QueryInputSpinnerAdapter(
                    getActivity().getBaseContext(),
                    R.layout.custom_queryhistory_spinner,
                    new_queryHistory));

            //For some reason the following does nothing
            query.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                    String inputWordString = query.getText().toString();
                    onWordEntered_PerformThisFunction(inputWordString);
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            });
        }
    }
    public void setNewQuery(String outputFromGrammarModuleFragment) {
        if (getActivity() != null) {
            AutoCompleteTextView queryInit = getActivity().findViewById(R.id.query);
            queryInit.setText(outputFromGrammarModuleFragment);
        }
    }
    private void registerThatUserIsRequestingDictSearch(Boolean state) {
        if (getActivity() != null) {
            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(getString(R.string.requestingDictSearch), state);
            editor.apply();
        }
    }
    public void setJpnOcrDataIsDownloadingStatus(Boolean status) {
        if (getContext() != null) {
            SharedPreferences sharedPref = getContext().getSharedPreferences(DOWNLOAD_FILE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(JPN_FILE_DOWNLOADING_FLAG, status);
            editor.apply();
        }
    }
    public void setEngOcrDataIsDownloadingStatus(Boolean status) {
        if (getContext() != null) {
            SharedPreferences sharedPref = getContext().getSharedPreferences(DOWNLOAD_FILE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(ENG_FILE_DOWNLOADING_FLAG, status);
            editor.apply();
        }
    }
    public void setOcrDataIsDownloadingStatus(String filename, Boolean status) {
        if (filename.equals("jpn.traineddata")) setJpnOcrDataIsDownloadingStatus(status);
        else if (filename.equals("eng.traineddata")) setEngOcrDataIsDownloadingStatus(status);
    }

    //Interface methods
    private UserEnteredQueryListener userEnteredQueryListener;
    interface UserEnteredQueryListener {
        // Interface used to transfer the verb to ConjugatorFragment or DictionaryFragment
        void OnQueryEnteredSwitchToRelevantFragment(String[] output);
    }

    public void onVerbEntered_PerformThisFunction(String inputVerbString) {

        // Send inputVerbString and SearchType to MainActivity through the interface
        output[0] = "verb";
        output[1] = inputVerbString;
        output[2] = "deep";
        userEnteredQueryListener.OnQueryEnteredSwitchToRelevantFragment(output);
    }
    public void onWordEntered_PerformThisFunction(String inputWordString) {

        // Send inputWordString and SearchType to MainActivity through the interface
        output[0] = "word";
        output[1] = inputWordString;
        output[2] = "fast";
        userEnteredQueryListener.OnQueryEnteredSwitchToRelevantFragment(output);
    }
    public void onConvertEntered_PerformThisFunction(String inputWordString) {

        // Send inputWordString and SearchType to MainActivity through the interface
        output[0] = "convert";
        output[1] = inputWordString;
        output[2] = "fast";
        userEnteredQueryListener.OnQueryEnteredSwitchToRelevantFragment(output);
    }
    public void onSearchByRadicalsEntered_PerformThisFunction(String inputWordString) {

        // Send inputWordString and SearchType to MainActivity through the interface
        output[0] = "radicals";
        output[1] = inputWordString;
        output[2] = "fast";
        userEnteredQueryListener.OnQueryEnteredSwitchToRelevantFragment(output);
    }
    public void onDecomposeEntered_PerformThisFunction(String inputWordString) {

        // Send inputWordString and SearchType to MainActivity through the interface
        output[0] = "decompose";
        output[1] = inputWordString;
        output[2] = "fast";
        userEnteredQueryListener.OnQueryEnteredSwitchToRelevantFragment(output);
    }

}