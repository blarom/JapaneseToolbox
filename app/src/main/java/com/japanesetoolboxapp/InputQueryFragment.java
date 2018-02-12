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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
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
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
import com.japanesetoolboxapp.utiities.ConvolutionMatrix;
import com.japanesetoolboxapp.utiities.GlobalConstants;
import com.japanesetoolboxapp.utiities.SharedMethods;
import com.theartofdev.edmodo.cropper.CropImage;

import static android.content.Context.DOWNLOAD_SERVICE;

public class InputQueryFragment extends Fragment implements LoaderManager.LoaderCallbacks<String>,
        TextToSpeech.OnInitListener {

    //Locals
    private static final int RESULT_OK = -1;
    private static final int SPEECH_RECOGNIZER_REQUEST_CODE = 2;
    private static final int ADJUST_IMAGE_ACTIVITY_REQUEST_CODE = 3;
    private static final int WEB_SEARCH_LOADER = 42;
    private static final String SPEECH_RECOGNIZER_EXTRA = "inputQueryAutoCompleteTextView";
    private static final String TAG_TESSERACT = "tesseract_ocr";
    private static final String TAG_PERMISSIONS = "Permission error";
    private	String[] output = {"word","","fast"};
    String[] queryHistory;
    ArrayList<String> new_queryHistory;
    AutoCompleteTextView inputQueryAutoCompleteTextView;
    Button button_searchVerb;
    Button button_searchWord;
    Button button_choose_Convert;
    Button button_searchTangorin;
    Button button_searchByRadical;
    Button button_Decompose;
    String mQueryText;
    String mCurrentPhotoPath;
    Bitmap mImageToBeDecoded;
    TessBaseAPI mTess;
    String mInternalStorageTesseractFolderPath = "";
    private boolean mInitializedOcrApi;
    private String mOCRLanguage;
    Uri mPhotoURI;
    String mOcrResultString;
    private boolean firstTimeInitialized;
    private TextToSpeech tts;
    private boolean mInternetIsAvailable;
    private long enqueue;
    private DownloadManager downloadmanager;
    private boolean hasStoragePermissions;
    private String mInternalStorageTesseractDataFilepath;
    private String mFilename;
    private String mDownloadsFolder;
    private int timesPressed;
    private boolean ocrDataIsAvailable;
    private String mChosenTextToSpeechLanguage;

    //Fragment Lifecycle methods
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setRetainInstance(true);
        final View InputQueryFragment = inflater.inflate(R.layout.fragment_inputquery, container, false);

        // Initializations
        mInternetIsAvailable = SharedMethods.internetIsAvailableCheck(this.getContext());
        inputQueryAutoCompleteTextView = InputQueryFragment.findViewById(R.id.query);
        inputQueryAutoCompleteTextView.setMovementMethod(new ScrollingMovementMethod());
        mQueryText = "";
        mOcrResultString = "";
        inputQueryAutoCompleteTextView.setText(mQueryText);
        mOCRLanguage = "jpn";
        firstTimeInitialized = true;
        mInitializedOcrApi = false;
        timesPressed= 0;
        ocrDataIsAvailable = false;
        setupPaths();
        setupBroadcastReceiverForDownloadedOCRData();
        registerThatUserIsRequestingDictSearch(false);
        tts = new TextToSpeech(getContext(), this);
        ifOcrDataIsNotAvailableThenMakeItAvailable();
        if (ocrDataIsAvailable) initializeTesseractAPI();

        // Restoring inputs from the savedInstanceState (if applicable)
        if (queryHistory == null) {
            queryHistory = new String[7];
            for (int i=0;i<queryHistory.length;i++) { queryHistory[i] = ""; } //7 elements in the array
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

                DisplayQueryHistory(inputWordString, inputQueryAutoCompleteTextView);
                inputQueryAutoCompleteTextView.dismissDropDown();

                onWordEntered_PerformThisFunction(inputWordString);
            }
            else {
                //inputQueryAutoCompleteTextView.showDropDown();
            }
            return true;
        } } );

        // Button listeners
        button_searchVerb = InputQueryFragment.findViewById(R.id.button_searchVerb);
        button_searchVerb.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {
            //EditText inputVerbObject = (EditText)fragmentView.findViewById(R.id.input_verb);
            String inputVerbString = inputQueryAutoCompleteTextView.getText().toString();
            mQueryText = inputVerbString;
            DisplayQueryHistory(inputVerbString, inputQueryAutoCompleteTextView);

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

            onVerbEntered_PerformThisFunction(inputVerbString);
        } } );

        button_searchWord = InputQueryFragment.findViewById(R.id.button_searchWord);
        button_searchWord.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {
            String inputWordString = inputQueryAutoCompleteTextView.getText().toString();
            mQueryText = inputWordString;
            DisplayQueryHistory(inputWordString, inputQueryAutoCompleteTextView);

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
            onWordEntered_PerformThisFunction(inputWordString);
        } } );

        button_choose_Convert = InputQueryFragment.findViewById(R.id.button_convert);
        button_choose_Convert.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {
            String inputWordString = inputQueryAutoCompleteTextView.getText().toString();

            DisplayQueryHistory(inputWordString, inputQueryAutoCompleteTextView);

            onConvertEntered_PerformThisFunction(inputWordString);
        } } );

        //button_searchTangorin = InputQueryFragment.findViewById(R.id.button_searchTangorin);
//        button_searchTangorin.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {
//            // Search using Tangorin
//            String queryString = inputQueryAutoCompleteTextView.getText().toString();
//
//            DisplayQueryHistory(queryString, inputQueryAutoCompleteTextView);
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

            DisplayQueryHistory(inputWordString, inputQueryAutoCompleteTextView);

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
                onSearchByRadicalsEntered_PerformThisFunction(inputWordString);
            }
        } } );

        button_Decompose = InputQueryFragment.findViewById(R.id.button_Decompose);
        button_Decompose.setEnabled(true);
        button_Decompose.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {
            // Break up a Kanji to Radicals

            String inputWordString = inputQueryAutoCompleteTextView.getText().toString();

            DisplayQueryHistory(inputWordString, inputQueryAutoCompleteTextView);

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
                onDecomposeEntered_PerformThisFunction(inputWordString);
            }
        } } );

        final Button button_ClearQuery = InputQueryFragment.findViewById(R.id.clearQuery);
        button_ClearQuery.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {
            inputQueryAutoCompleteTextView.setText("");
        } } );

        final Button button_ShowHistory = InputQueryFragment.findViewById(R.id.showHistory);
        button_ShowHistory.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {

            String queryString = inputQueryAutoCompleteTextView.getText().toString();
            DisplayQueryHistory(queryString, inputQueryAutoCompleteTextView);
            boolean queryHistoryIsEmpty = true;
            for (String element : queryHistory) {
                if (!element.equals("")) { queryHistoryIsEmpty = false; }
            }
            if (!queryHistoryIsEmpty) {
                inputQueryAutoCompleteTextView.showDropDown();}
        } } );

        final ImageView button_SpeechToText = InputQueryFragment.findViewById(R.id.getTextThroughSpeech);
        button_SpeechToText.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {

            int maxResultsToReturn = 1;
            try {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResultsToReturn);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, MainActivity.mChosenSpeechToTextLanguage);
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

        final ImageView button_ImageToText = InputQueryFragment.findViewById(R.id.getTextThroughCamera);
        button_ImageToText.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            if (mInitializedOcrApi) {
                if (firstTimeInitialized) {
                    Toast toast = Toast.makeText(getActivity(), getResources().getString(R.string.OCRinstructions), Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
                    toast.show();
                    firstTimeInitialized = false;
                }
                timesPressed = 0;
                performImageCaptureAndCrop();
            }
            else {
                if (timesPressed <= 3) {
                    ifOcrDataIsNotAvailableThenMakeItAvailable();
                    initializeTesseractAPI();
                    mInitializedOcrApi = true;
                    timesPressed++; //Prevents multiple clicks on the button from freezing the app
                }
                Toast toast = Toast.makeText(getContext(),getResources().getString(R.string.OCR_reinitializing), Toast.LENGTH_SHORT);
                toast.show();
            }
        } } );

        final ImageView button_TextToSpeech = InputQueryFragment.findViewById(R.id.speakQuery);
        button_TextToSpeech.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
            speakOut(inputQueryAutoCompleteTextView.getText().toString());
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
    }
    @Override public void onDestroy() {
        super.onDestroy();
        if (mTess != null) mTess.end();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        EditText query = getActivity().findViewById(R.id.query);
        savedInstanceState.putString("inputQueryAutoCompleteTextView", query.getText().toString());

        /*RadioButton radio_FastSearch = (RadioButton)GlobalInputQueryFragment.findViewById(R.id.radio_FastSearch);
        if (radio_FastSearch.isChecked()) { savedInstanceState.putBoolean("radio_FastSearch", true); }
        else							  { savedInstanceState.putBoolean("radio_FastSearch", false); }*/

        savedInstanceState.putStringArray("queryHistory", queryHistory);
    }
    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_RECOGNIZER_REQUEST_CODE) {
            if (data == null) return;
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            mQueryText = results.get(0);
            inputQueryAutoCompleteTextView.setText(mQueryText);
            registerThatUserIsRequestingDictSearch(true);

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
                registerThatUserIsRequestingDictSearch(true);
                mPhotoURI = result.getUri();
                mImageToBeDecoded = getImageFromUri(mPhotoURI);

                //Send the image Uri to the AdjustImageActivity
                Intent intent = new Intent(getActivity(), AdjustImageActivity.class);
                intent.putExtra("imageUri", mPhotoURI.toString());
                startActivityForResult(intent, ADJUST_IMAGE_ACTIVITY_REQUEST_CODE);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
        else if (requestCode == ADJUST_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                registerThatUserIsRequestingDictSearch(true);
                //mImageToBeDecoded = adjustImageBeforeOCR(mImageToBeDecoded);
                Bundle extras = data.getExtras();
                Uri adjustedImageUri = Uri.parse(extras.getString("returnImageUri"));
                mImageToBeDecoded = getImageFromUri(adjustedImageUri);
                getOcrTextWithTesseractAndDisplayDialog(mImageToBeDecoded);
            }
        }
    }

    //Image methods
    private void performImageCaptureAndCrop() {

        // start source picker (camera, gallery, etc..) to get image for cropping and then use the image in cropping activity
        //CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(getActivity());
        CropImage.activity().start(getContext(), this); //For FragmentActivity use

    }
    private Uri getImageUriFromFile(String path) {
        File f = new File(path);
        Uri contentUri = Uri.fromFile(f);
        return contentUri;
    }
    private Bitmap getImageFromUri(Uri resultUri) {
        Bitmap imageToBeDecoded = null;
        try {
            //BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            //bmOptions.inJustDecodeBounds = false;
            //image = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
            imageToBeDecoded = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), resultUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageToBeDecoded;
    }

    //Tesseract OCR methods
    public void setupBroadcastReceiverForDownloadedOCRData() {
        BroadcastReceiver receiver = new BroadcastReceiver() {
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

                            String uriString = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                            Uri a = Uri.parse(uriString);
                            File d = new File(a.getPath());
                            // copy file from external to internal will esaily avalible on net use google.
                            //String sdCard = Environment.getExternalStorageDirectory().toString();
                            File sourceLocation = new File(mDownloadsFolder + "/" + mFilename);
                            File targetLocation = new File(mInternalStorageTesseractDataFilepath);
                            moveFile(sourceLocation, targetLocation);
                            if (ocrDataIsAvailable) initializeTesseractAPI();
                        }
                    }
                }
            }
        };
        getActivity().registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }
    private void setupPaths() {
        //mInternalStorageTesseractFolderPath = Environment.getExternalStoragePublicDirectory(Environment.).getAbsolutePath() + "/";
        mInternalStorageTesseractFolderPath = getActivity().getFilesDir() + "/tesseract/";
        mInternalStorageTesseractDataFilepath = mInternalStorageTesseractFolderPath + "tessdata/";
        mFilename = mOCRLanguage + ".traineddata";
        mDownloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/";
    }
    private void initializeTesseractAPI() {
        mImageToBeDecoded = BitmapFactory.decodeResource(getResources(), R.drawable.test_image);

        mInitializedOcrApi = false;
        if (getActivity()!=null) {
            //Download language files from https://github.com/tesseract-ocr/tesseract/wiki/Data-Files
            try {
                mTess = new TessBaseAPI();
                mTess.init(mInternalStorageTesseractFolderPath, mOCRLanguage);
                mInitializedOcrApi = true;
                Log.e(TAG_TESSERACT, "Initialized Tesseract.");
            } catch (Exception e) {
                Log.e(TAG_TESSERACT, "Failed to initialize Tesseract.");
                Toast.makeText(getContext(),getResources().getString(R.string.OCR_failed), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }
    private void ifOcrDataIsNotAvailableThenMakeItAvailable() {

        Boolean fileExistsInInternalStorage = checkIfFileExistsInSpecificFolder(new File(mInternalStorageTesseractDataFilepath), mFilename);
        if (!fileExistsInInternalStorage) {
            hasStoragePermissions = checkStoragePermission();
            makeOcrDataAvailableInAppFolder();
        }
        else ocrDataIsAvailable = true;
    }
    public void makeOcrDataAvailableInAppFolder() {
        Boolean fileExistsInDownloadsFolder = checkIfFileExistsInSpecificFolder(new File(mDownloadsFolder), mFilename);
        if (hasStoragePermissions) {
            if (fileExistsInDownloadsFolder) {
                Log.e(TAG_TESSERACT, "File successfully found in Downloads folder.");
                File sourceLocation = new File(mDownloadsFolder + mFilename);
                File targetLocation = new File(mInternalStorageTesseractDataFilepath);
                moveFile(sourceLocation, targetLocation);
            } else {

                ocrDataIsAvailable = false;
                Log.e(TAG_TESSERACT, "File not found in Downloads folder.");
                //if (!fileExistsInInternalStorage) copyFileFromAssets(mOCRLanguage);
                Toast.makeText(getActivity(), "Attempting to download the Japanese OCR data", Toast.LENGTH_SHORT).show();
                downloadFileToDownloadsFolder(mOCRLanguage);
            }
        }
    }
    public void getOcrTextWithTesseractAndDisplayDialog(Bitmap imageToBeDecoded){
        String OCRresult = null;
        mTess.setImage(imageToBeDecoded);
        new TesseractOCRAsyncTask(getActivity()).execute();
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
    private void downloadFileToDownloadsFolder(String mOCRLanguage) {

        Log.e(TAG_TESSERACT, "Attempting file download");
        String url = "https://github.com/tesseract-ocr/tessdata/raw/master/" + mFilename;

        //https://stackoverflow.com/questions/38563474/how-to-store-downloaded-image-in-internal-storage-using-download-manager-in-andr
        downloadmanager = (DownloadManager) getActivity().getSystemService(DOWNLOAD_SERVICE);
        Uri downloadUri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(downloadUri);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, mFilename);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        request.setAllowedOverRoaming(false);
        request.setTitle("Downloading Tesseract OCR data");
        request.setDescription("Japanese");
        request.setVisibleInDownloadsUi(true);
        enqueue = downloadmanager.enqueue(request);

        //Finished download activates the broadcast receiver, that in turn initializes the Tesseract API

    }
    public boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (getActivity().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG_PERMISSIONS,"You have permission");
                return true;
            } else {
                Log.e(TAG_PERMISSIONS,"You have asked for permission");
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //you dont need to worry about these stuff below api level 23
            Log.e(TAG_PERMISSIONS,"You already have the permission");
            return true;
        }
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            hasStoragePermissions = true;

            Log.e(TAG_PERMISSIONS,"Returned from permission request.");
            makeOcrDataAvailableInAppFolder();
            if (!mInitializedOcrApi) initializeTesseractAPI();
        }
    }
    private void moveFile(File file, File dir) {
        File newFile = new File(dir, file.getName());
        FileChannel outputChannel = null;
        FileChannel inputChannel = null;
        try {
            outputChannel = new FileOutputStream(newFile).getChannel();
            inputChannel = new FileInputStream(file).getChannel();
            inputChannel.transferTo(0, inputChannel.size(), outputChannel);
            inputChannel.close();
            ocrDataIsAvailable = true;
            Toast.makeText(getContext(),getResources().getString(R.string.OCR_copy_data), Toast.LENGTH_SHORT).show();
            Log.v(TAG_TESSERACT, "Successfully moved data file to app folder.");
            //file.delete();
        } catch (IOException e) {
            ocrDataIsAvailable = false;
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
        Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap , 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

        return rotatedBitmap;
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
            mProgressDialog.setTitle("Wait while processing....");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Object[] params) {
            String result = mTess.getUTF8Text();
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
        builder.setTitle(R.string.DialogTitle);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setView(dialogView);
        }
        else { builder.setMessage(R.string.DialogMessage); }
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
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String language = sharedPreferences.getString(getString(R.string.pref_preferred_TTS_language_key), getString(R.string.pref_preferred_language_value_japanese));
        setTextToSpeechLanguage(language);

        //Setting the language
        if (mChosenTextToSpeechLanguage.equals(getResources().getString(R.string.languageLocaleJapanese))) {
            result = tts.setLanguage(Locale.JAPAN);
        }
        else if (mChosenTextToSpeechLanguage.equals(getResources().getString(R.string.languageLocaleEnglishUS))) {
            result = tts.setLanguage(Locale.US);
        }
        else result = tts.setLanguage(Locale.US);

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("TTS", "This Language is not supported, set to default English.");
            tts.setLanguage(Locale.US);
        }
    }
    private void speakOut(String text) {
        setTTSLanguage();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }
    public void setTextToSpeechLanguage(String language) {
        if (language.equals(getResources().getString(R.string.pref_preferred_language_value_japanese))) {
            mChosenTextToSpeechLanguage = getResources().getString(R.string.languageLocaleJapanese);
        }
        else if (language.equals(getResources().getString(R.string.pref_preferred_language_value_english))) {
            mChosenTextToSpeechLanguage = getResources().getString(R.string.languageLocaleEnglishUS);
        }
    }

    //Query input methods
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
                        //throw new RuntimeException(e);
                    }
                    if (AsyncMatchingWordCharacteristics.size() != 0) {
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
    private class QueryInputSpinnerAdapter extends ArrayAdapter<String> {
    // Code adapted from http://mrbool.com/how-to-customize-spinner-in-android/28286
        public QueryInputSpinnerAdapter(Context ctx, int txtViewResourceId, List<String> list) {
            super(ctx, txtViewResourceId, list);
            }
        @Override
        public View getDropDownView( int position, View cnvtView, ViewGroup prnt) {
            return getCustomView(position, cnvtView, prnt);
        }
        @Override
        public View getView(int pos, View cnvtView, ViewGroup prnt) {
            return getCustomView(pos, cnvtView, prnt);
        }
        public View getCustomView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = LayoutInflater.from(getActivity().getBaseContext());
            View mySpinner = inflater.inflate(R.layout.custom_queryhistory_spinner, parent, false);
            TextView pastquery = mySpinner.findViewById(R.id.pastQuery);
            pastquery.setText(new_queryHistory.get(position));

            return mySpinner;
        }
    }
    public void DisplayQueryHistory(String queryStr, final AutoCompleteTextView query) {

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

                new_queryHistory = new ArrayList<>();
                for (String aQueryHistory : queryHistory) {
                    if (!aQueryHistory.equals("")) {
                        new_queryHistory.add(aQueryHistory);
                    }
                }
            }

        // Set the dropdown main to include all past entries
            query.setAdapter(new QueryInputSpinnerAdapter(
                    getActivity().getBaseContext(),
                    R.layout.custom_queryhistory_spinner,
                    new_queryHistory));

            //For some reason the following does nothing
            query.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1,int position, long id) {
                    String inputWordString = query.getText().toString();
                    onWordEntered_PerformThisFunction(inputWordString);
                }
                @Override
                public void onNothingSelected(AdapterView<?> arg0) { }
            });
    }
    public void setNewQuery(String outputFromGrammarModuleFragment) {
        AutoCompleteTextView queryInit = getActivity().findViewById(R.id.query);
        queryInit.setText(outputFromGrammarModuleFragment);
    }
    private void registerThatUserIsRequestingDictSearch(Boolean state) {
        SharedPreferences sharedPref = getContext().getSharedPreferences(getString(R.string.requestingDictSearch), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.requestingDictSearch), state);
        editor.apply();
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
        registerThatUserIsRequestingDictSearch(true);
        userEnteredQueryListener.OnQueryEnteredSwitchToRelevantFragment(output);
    }
    public void onWordEntered_PerformThisFunction(String inputWordString) {

        // Send inputWordString and SearchType to MainActivity through the interface
        output[0] = "word";
        output[1] = inputWordString;
        output[2] = "fast";
        registerThatUserIsRequestingDictSearch(false);
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