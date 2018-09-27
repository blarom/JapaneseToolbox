package com.japanesetoolboxapp.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.japanesetoolboxapp.R;
import com.japanesetoolboxapp.data.Word;
import com.japanesetoolboxapp.resources.Utilities;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static android.content.Context.DOWNLOAD_SERVICE;

public class InputQueryFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<String>,
        TextToSpeech.OnInitListener {


    //region Parameters
    @BindView(R.id.query) AutoCompleteTextView mInputQueryAutoCompleteTextView;
    @BindView(R.id.button_dict) Button mDictButton;
    @BindView(R.id.button_search_by_radical) Button mSearchByRadicalButton;
    @BindView(R.id.button_decompose) Button mDecomposeButton;
    private static final int QUERY_HISTORY_MAX_SIZE = 7;
    private static final int RESULT_OK = -1;
    private static final int SPEECH_RECOGNIZER_REQUEST_CODE = 101;
    private static final int ADJUST_IMAGE_ACTIVITY_REQUEST_CODE = 201;
    private static final int GET_ROMAJI_FROM_KANJI_USING_JISHO_LOADER = 42;
    private static final int TESSERACT_OCR_LOADER = 69;
    private static final String SPEECH_RECOGNIZER_EXTRA = "inputQueryAutoCompleteTextView";
    private static final String TAG_TESSERACT = "tesseract_ocr";
    private static final String TAG_PERMISSIONS = "Permission error";
    private static final String DOWNLOAD_FILE_PREFS = "download_file_prefs";
    private static final String JPN_FILE_DOWNLOADING_FLAG = "jpn_file_downloading";
    private static final String ENG_FILE_DOWNLOADING_FLAG = "eng_file_downloading";
    List<String> mQueryHistory;
    String mInputQuery;
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
    private boolean mRequestedSpeechToText;
    private ProgressDialog mProgressDialog;
    private Unbinder mBinding;
    private boolean mAlreadyGotOcrResult;
    private boolean mAlreadyGotRomajiFromKanji;
    //endregion


    //Fragment Lifecycle methods
    @Override public void onAttach(Context context) {
        super.onAttach(context);
        inputQueryOperationsHandler = (InputQueryOperationsHandler) context;
    }
    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getExtras();
        initializeParameters();
        setupOcr();
    }
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        final View rootView = inflater.inflate(R.layout.fragment_inputquery, container, false);

        setRetainInstance(true);
        restoreQueryHistory();
        initializeViews(rootView);

        if (savedInstanceState!=null) {
            mAlreadyGotOcrResult = savedInstanceState.getBoolean(getString(R.string.saved_ocr_result_state), false);
            mAlreadyGotRomajiFromKanji = savedInstanceState.getBoolean(getString(R.string.saved_romaji_from_kanji_state), false);
        }

        return rootView;
    }
    @Override public void onResume() {
        super.onResume();

        mInputQueryAutoCompleteTextView.setText(mInputQuery);
        mInputQueryAutoCompleteTextView.clearFocus();
        if (getActivity() != null) getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        getLanguageParametersFromSettingsAndReinitializeOcrIfNecessary();
    }

    @Override public void onPause() {
        super.onPause();
        mInputQuery = mInputQueryAutoCompleteTextView.getText().toString();

    }
    @Override public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        if (getActivity() != null) {
            EditText query = getActivity().findViewById(R.id.query);
            savedInstanceState.putString("inputQueryAutoCompleteTextView", query.getText().toString());
            savedInstanceState.putBoolean(getActivity().getString(R.string.saved_ocr_result_state), mAlreadyGotOcrResult);
            savedInstanceState.putBoolean(getActivity().getString(R.string.saved_romaji_from_kanji_state), mAlreadyGotRomajiFromKanji);

        }
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
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
    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_RECOGNIZER_REQUEST_CODE) {

            if (data == null) return;
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            mInputQuery = results.get(0);
            mInputQueryAutoCompleteTextView.setText(mInputQuery);

            //Attempting to access jisho.org to get the romaji value of the requested word, if it's a Japanese word
            if (getActivity() != null && mChosenSpeechToTextLanguage.equals(getResources().getString(R.string.languageLocaleJapanese))) {
                Bundle queryBundle = new Bundle();
                queryBundle.putString(SPEECH_RECOGNIZER_EXTRA, results.get(0));

                mAlreadyGotRomajiFromKanji = false;
                LoaderManager loaderManager = getActivity().getSupportLoaderManager();
                Loader<String> webSearchLoader = loaderManager.getLoader(GET_ROMAJI_FROM_KANJI_USING_JISHO_LOADER);
                if (webSearchLoader == null)
                    loaderManager.initLoader(GET_ROMAJI_FROM_KANJI_USING_JISHO_LOADER, queryBundle, this);
                else loaderManager.restartLoader(GET_ROMAJI_FROM_KANJI_USING_JISHO_LOADER, queryBundle, this);
            }
        }
        else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            Utilities.unmuteSpeaker(getActivity());
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                mCropImageResult = result;
                sendImageToImageAdjuster(mCropImageResult);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
        else if (requestCode == ADJUST_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Uri adjustedImageUri = Uri.parse(extras.getString("returnImageUri"));
                    mImageToBeDecoded = Utilities.getBitmapFromUri(getActivity(), adjustedImageUri);
                    getOcrTextWithTesseractAndDisplayDialog(mImageToBeDecoded);
                }
            }
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


    //Functionality methods
    private void getExtras() {
        if (getArguments()!=null) {
            mChosenSpeechToTextLanguage = getArguments().getString(getString(R.string.pref_preferred_STT_language));
        }
    }
    private void initializeParameters() {

        mQueryHistory = new ArrayList<>();
        mInputQuery = "";
        mOcrResultString = "";
        firstTimeInitializedJpn = true;
        firstTimeInitializedEng = true;
        mInitializedOcrApiJpn = false;
        mInitializedOcrApiEng = false;
        timesPressed = 0;
        jpnOcrDataIsAvailable = false;
        engOcrDataIsAvailable = false;
        mRequestedSpeechToText = false;
        mCropImageResult = null;
        mAlreadyGotOcrResult = false;
        mAlreadyGotRomajiFromKanji = false;

    }
    @SuppressLint("ClickableViewAccessibility") private void initializeViews(View rootView) {

        mBinding = ButterKnife.bind(this, rootView);


        mInputQueryAutoCompleteTextView.setAdapter(new QueryInputSpinnerAdapter(
                getContext(),
                R.layout.custom_queryhistory_spinner,
                mQueryHistory));

        mInputQueryAutoCompleteTextView.setMovementMethod(new ScrollingMovementMethod());
        mInputQueryAutoCompleteTextView.setText(mInputQuery);

        mInputQueryAutoCompleteTextView.setLongClickable(false);
        //inputQueryAutoCompleteTextView.setTextIsSelectable(true);
        mInputQueryAutoCompleteTextView.setFocusable(true);
        mInputQueryAutoCompleteTextView.setFocusableInTouchMode(true);

        mInputQueryAutoCompleteTextView.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //inputQueryAutoCompleteTextView.showDropDown();
                mInputQueryAutoCompleteTextView.dismissDropDown();
                return false;
            }

        });

        mInputQueryAutoCompleteTextView.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    mInputQuery = mInputQueryAutoCompleteTextView.getText().toString();

                    updateQueryHistory();
                    mInputQueryAutoCompleteTextView.dismissDropDown();

                    registerThatUserIsRequestingDictSearch(true);

                    inputQueryOperationsHandler.onDictRequested(mInputQuery);
                }
                return true;
            } } );

        mSearchByRadicalButton.setEnabled(true);
        mDecomposeButton.setEnabled(true);
    }
    private void restoreQueryHistory() {

        if (getContext() != null) {
            SharedPreferences sharedPref = getContext().getSharedPreferences(getString(R.string.preferences_query_history_list), Context.MODE_PRIVATE);
            String queryHistoryAsString = sharedPref.getString(getString(R.string.preferences_query_history_list), "");
            if (!queryHistoryAsString.equals("")) mQueryHistory = new ArrayList<>(Arrays.asList(queryHistoryAsString.split(",")));
        }
    }
    private void sendImageToImageAdjuster(CropImage.ActivityResult result) {
        mPhotoURI = result.getUri();
        mImageToBeDecoded = Utilities.getBitmapFromUri(getActivity(), mPhotoURI);

        //Send the image Uri to the AdjustImageActivity
        Intent intent = new Intent(getActivity(), AdjustImageActivity.class);
        intent.putExtra("imageUri", mPhotoURI.toString());
        startActivityForResult(intent, ADJUST_IMAGE_ACTIVITY_REQUEST_CODE);
    }
    private void performImageCaptureAndCrop() {

        // start source picker (camera, gallery, etc..) to get image for cropping and then use the image in cropping activity
        //CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).start(getActivity());

        Utilities.muteSpeaker(getActivity());
        if (getContext() != null) CropImage.activity().start(getContext(), this); //For FragmentActivity use

    }
    private Bitmap adjustImageAfterOCR(Bitmap imageToBeDecoded) {
        //imageToBeDecoded = Utilities.adjustImageAngleAndScale(imageToBeDecoded, 0, 0.5);
        return imageToBeDecoded;
    }


    //View click listeners
    @OnClick(R.id.button_dict) public void onDictButtonClick() {
        String inputWordString = mInputQueryAutoCompleteTextView.getText().toString();
        mInputQuery = inputWordString;
        updateQueryHistory();

        registerThatUserIsRequestingDictSearch(true); //TODO: remove this?

        inputQueryOperationsHandler.onDictRequested(inputWordString);
    }
    @OnClick(R.id.button_conj) public void onSearchVerbButtonClick() {

        String inputVerbString = mInputQueryAutoCompleteTextView.getText().toString();
        mInputQuery = inputVerbString;
        updateQueryHistory();

        registerThatUserIsRequestingDictSearch(false); //TODO: remove this?

        inputQueryOperationsHandler.onConjRequested(inputVerbString);

    }
    @OnClick(R.id.button_convert) public void onConvertButtonClick() {

        mInputQuery = mInputQueryAutoCompleteTextView.getText().toString();
        updateQueryHistory();

        inputQueryOperationsHandler.onConvertRequested(mInputQuery);
    }
    @OnClick(R.id.button_search_by_radical) public void onSearchByRadicalButtonClick() {

        // Break up a Kanji to Radicals

        mInputQuery = mInputQueryAutoCompleteTextView.getText().toString();

        updateQueryHistory();

        inputQueryOperationsHandler.onSearchByRadicalRequested(Utilities.removeSpecialCharacters(mInputQuery));
    }
    @OnClick(R.id.button_decompose) public void onDecomposeButtonClick() {

        mInputQuery = mInputQueryAutoCompleteTextView.getText().toString();

        updateQueryHistory();

        inputQueryOperationsHandler.onDecomposeRequested(Utilities.removeSpecialCharacters(mInputQuery));
    }
    @OnClick(R.id.button_clear_query) public void onClearQueryButtonClick() {

        mInputQueryAutoCompleteTextView.setText("");
        mInputQuery = "";
    }
    @OnClick(R.id.button_show_history) public void onShowHistoryButtonClick() {

        updateQueryHistory();
        boolean queryHistoryIsEmpty = true;
        for (String element : mQueryHistory) {
            if (!element.equals("")) { queryHistoryIsEmpty = false; break; }
        }
        if (!queryHistoryIsEmpty) mInputQueryAutoCompleteTextView.showDropDown();

    }
    @OnClick(R.id.button_copy_query) public void onCopyQueryButtonClick() {

        mInputQuery = mInputQueryAutoCompleteTextView.getText().toString();
        if (getActivity() != null) {
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Copied Text", mInputQueryAutoCompleteTextView.getText().toString());
            if (clipboard != null) clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), getResources().getString(R.string.copiedTextToClipboard), Toast.LENGTH_SHORT).show();
        }

    }
    @OnClick(R.id.button_paste_to_query) public void onPasteToQueryButtonClick() {

        if (getActivity() != null) {
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                if (clipboard.hasPrimaryClip()) {
                    android.content.ClipDescription description = clipboard.getPrimaryClipDescription();
                    android.content.ClipData data = clipboard.getPrimaryClip();
                    if (data != null && description != null && description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))
                        mInputQueryAutoCompleteTextView.setText(String.valueOf(data.getItemAt(0).getText()));
                }
            }
        }

    }
    @OnClick(R.id.button_speech_to_text) public void onSpeechToTextButtonClick() {

        int maxResultsToReturn = 1;
        try {
            //Getting the user setting from the preferences
            if (getActivity()==null) return;
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String language = sharedPreferences.getString(getString(R.string.pref_preferred_STT_language_key), getString(R.string.pref_preferred_language_value_japanese));
            mChosenSpeechToTextLanguage = getSpeechToTextLanguageLocale(language);

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResultsToReturn);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, mChosenSpeechToTextLanguage);
            intent.putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, getResources().getString(R.string.languageLocaleEnglishUS));
            if (mChosenSpeechToTextLanguage.equals(getResources().getString(R.string.languageLocaleJapanese))) {
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getResources().getString(R.string.SpeechToTextUserPromptJapanese));
            } else if (mChosenSpeechToTextLanguage.equals(getResources().getString(R.string.languageLocaleEnglishUS))) {
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getResources().getString(R.string.SpeechToTextUserPromptEnglish));
            }
            startActivityForResult(intent, SPEECH_RECOGNIZER_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(),getResources().getString(R.string.STTACtivityNotFound),Toast.LENGTH_SHORT).show();
            String appPackageName = "com.google.android.tts";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,   Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName));
            startActivity(browserIntent);
        }

    }
    @OnClick(R.id.button_ocr) public void onOcrButtonClick() {

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

    }
    @OnClick(R.id.button_text_to_speech) public void onTextToSpeechButtonClick() {

        String queryString = mInputQueryAutoCompleteTextView.getText().toString();
        mInputQuery = queryString;
        speakOut(queryString);

    }


    //Tesseract OCR methods
    private void setupOcr() {

        mDownloadType = "WifiOnly";

        getOcrDataDownloadingStatus();
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
    }
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
        if (getActivity()==null) return;

        mTess.setImage(imageToBeDecoded);

        mAlreadyGotOcrResult = false;
        LoaderManager loaderManager = getActivity().getSupportLoaderManager();
        Loader<String> tesseractOCRLoader = loaderManager.getLoader(TESSERACT_OCR_LOADER);
        if (tesseractOCRLoader == null)
            loaderManager.initLoader(TESSERACT_OCR_LOADER, null, this);
        else loaderManager.restartLoader(TESSERACT_OCR_LOADER, null, this);

        //mTesseractOCRAsyncTask = new TesseractOCRAsyncTask(getActivity()).execute();
    }
    private void downloadTesseractDataFileToDownloadsFolder(String source, String filename) {

        if (getActivity()==null) return;

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
            Toast.makeText(getActivity(), getString(R.string.downloading_ocr_data_first_part) + mLanguageBeingDownloadedLabel
                    + getString(R.string.downloading_ocr_data_second_part), Toast.LENGTH_SHORT).show();
        }
        else {
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            Toast.makeText(getActivity(), getString(R.string.attempting_download_first_part) + mLanguageBeingDownloadedLabel
                    + getString(R.string.attempting_download_second_part), Toast.LENGTH_SHORT).show();
        }
        request.setAllowedOverRoaming(false);
        request.setTitle("Downloading " + filename);
        request.setVisibleInDownloadsUi(true);
        enqueue = downloadmanager.enqueue(request);


        //Finished download activates the broadcast receiver, that in turn initializes the Tesseract API

    }
    private void getOcrDataDownloadingStatus() {

        if (getContext() != null) {
            SharedPreferences sharedPreferences = getContext().getSharedPreferences(DOWNLOAD_FILE_PREFS, Context.MODE_PRIVATE);
            mJpnOcrFileIsDownloading = sharedPreferences.getBoolean(JPN_FILE_DOWNLOADING_FLAG, false);
            mEngOcrFileIsDownloading = sharedPreferences.getBoolean(ENG_FILE_DOWNLOADING_FLAG, false);
        }
    }
    private String getOCRLanguageFromSettings() {
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
    private String getLanguageLabel(String language) {
        if (language.equals("jpn")) return getResources().getString(R.string.language_label_japanese);
        else return getResources().getString(R.string.language_label_english);
    }
    private void setupBroadcastReceiverForDownloadedOCRData() {
        mBroadcastReceiver = new BroadcastReceiver() {
            //https://stackoverflow.com/questions/38563474/how-to-store-downloaded-image-in-internal-storage-using-download-manager-in-andr
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action) && downloadmanager!=null) {
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
                            moveTesseractDataFile(sourceLocation, targetLocation);

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
        Boolean fileExistsInAppFolder = Utilities.checkIfFileExistsInSpecificFolder(new File(mPhoneAppFolderTesseractDataFilepath), filename);
        mInitializedOcrApiJpn = false;
        if (!fileExistsInAppFolder) {
            hasStoragePermissions = Utilities.checkStoragePermission(getActivity());
            makeOcrDataAvailableInAppFolder(language);
        }
        else {
            if (language.equals("jpn")) jpnOcrDataIsAvailable = true;
            else if (language.equals("eng")) engOcrDataIsAvailable = true;
        }
    }
    private void makeOcrDataAvailableInAppFolder(String language) {

        mLanguageBeingDownloadedLabel = getLanguageLabel(language);
        String filename = language + ".traineddata";
        Boolean fileExistsInPhoneDownloadsFolder = Utilities.checkIfFileExistsInSpecificFolder(new File(mDownloadsFolder), filename);
        if (hasStoragePermissions) {
            if (fileExistsInPhoneDownloadsFolder) {
                Log.e(TAG_TESSERACT, filename + " file successfully found in Downloads folder.");
                File sourceLocation = new File(mDownloadsFolder + filename);
                File targetLocation = new File(mPhoneAppFolderTesseractDataFilepath);
                moveTesseractDataFile(sourceLocation, targetLocation);
            } else {
                jpnOcrDataIsAvailable = false;
                if (!jpnOcrFileISDownloading) askForPreferredDownloadTimeAndDownload(language, filename);
            }
        }
    }
    private void copyTesseractDataFileFromAssets(String language) {
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
    private void moveTesseractDataFile(File file, File dir) {
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
    private void askForPreferredDownloadTimeAndDownload(String language, final String filename) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.OCRDialogTitle);
        builder.setPositiveButton(R.string.DownloadDialogWifiOnly, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mDownloadType = "WifiOnly";

                Boolean hasMemory = checkIfStorageSpaceEnoughForTesseractDataOrShowApology();
                Log.e(TAG_TESSERACT, "File not found in Downloads folder.");
                //if (!fileExistsInInternalStorage) copyFileFromAssets(mOCRLanguage);
                if (hasMemory) downloadTesseractDataFileToDownloadsFolder("https://github.com/tesseract-ocr/tessdata/raw/master/", filename);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.DownloadDialogWifiAndMobile, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mDownloadType = "WifiAndMobile";

                Boolean hasMemory = checkIfStorageSpaceEnoughForTesseractDataOrShowApology();
                Log.e(TAG_TESSERACT, "File not found in Downloads folder.");
                //if (!fileExistsInInternalStorage) copyFileFromAssets(mOCRLanguage);
                if (hasMemory) downloadTesseractDataFileToDownloadsFolder("https://github.com/tesseract-ocr/tessdata/raw/master/", filename);
                dialog.dismiss();
            }
        });
        if (language.equals("jpn")) builder.setMessage(R.string.DownloadDialogMessageJPN);
        else builder.setMessage(R.string.DownloadDialogMessageENG);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private Boolean checkIfStorageSpaceEnoughForTesseractDataOrShowApology() {
        //https://inducesmile.com/android/how-to-get-android-ram-internal-and-external-memory-information/
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        long availableMemory = availableBlocks * blockSize;
        if (availableMemory<70000000) {
            String toastMessage = getString(R.string.sorry_only_have_first_part) + Utilities.formatSize(availableMemory)
                    + getString(R.string.sorry_only_have_second_part);
            Toast.makeText(getActivity(), toastMessage, Toast.LENGTH_SHORT).show();
            return false;
        }
        else return true;
    }
    private void setJpnOcrDataIsDownloadingStatus(Boolean status) {
        if (getContext() != null) {
            SharedPreferences sharedPref = getContext().getSharedPreferences(DOWNLOAD_FILE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(JPN_FILE_DOWNLOADING_FLAG, status);
            editor.apply();
        }
    }
    private void setEngOcrDataIsDownloadingStatus(Boolean status) {
        if (getContext() != null) {
            SharedPreferences sharedPref = getContext().getSharedPreferences(DOWNLOAD_FILE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(ENG_FILE_DOWNLOADING_FLAG, status);
            editor.apply();
        }
    }
    private void setOcrDataIsDownloadingStatus(String filename, Boolean status) {
        if (filename.equals("jpn.traineddata")) setJpnOcrDataIsDownloadingStatus(status);
        else if (filename.equals("eng.traineddata")) setEngOcrDataIsDownloadingStatus(status);
    }

    public void setSTTLanguage(String mChosenSpeechToTextLanguage) {
        this.mChosenSpeechToTextLanguage = mChosenSpeechToTextLanguage;
    }

    private static class TesseractOCRAsyncTaskLoader extends AsyncTaskLoader <String> {

        TessBaseAPI mTess;
        private boolean mAllowLoaderStart;

        TesseractOCRAsyncTaskLoader(Context context,
                                    TessBaseAPI mTess) {
            super(context);
            this.mTess = mTess;
        }

        @Override
        protected void onStartLoading() {
            if (mAllowLoaderStart) forceLoad();
        }

        @Override
        public String loadInBackground() {

            String result = mTess.getUTF8Text();
            //String result = mTess.getHOCRText(0);
            return result;

        }

        void setLoaderState(boolean state) {
            mAllowLoaderStart = state;
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

        mInputQuery = ocrResultsList[0];
        mInputQueryAutoCompleteTextView.setText(mInputQuery);
        ocrResultsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                mInputQueryAutoCompleteTextView.setText(ocrResultsList[position]);
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
                if (mCropImageResult != null) sendImageToImageAdjuster(mCropImageResult);
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
    private static class GetRomajiFromKanjiUsingJishoAsyncTaskLoader extends AsyncTaskLoader <String> {

        String queryText;
        private boolean requestedSpeechToText;
        private boolean internetIsAvailable;
        private boolean mAllowLoaderStart;

        GetRomajiFromKanjiUsingJishoAsyncTaskLoader(Context context,
                                                    String queryText,
                                                    boolean requestedSpeechToText) {
            super(context);
            this.queryText = queryText;
            this.requestedSpeechToText = requestedSpeechToText;
            this.internetIsAvailable = internetIsAvailable;
        }

        @Override
        protected void onStartLoading() {
            if (mAllowLoaderStart) forceLoad();
        }

        @Override
        public String loadInBackground() {

            internetIsAvailable = Utilities.internetIsAvailableCheck(getContext());

            //This method retrieves the first romaji transliteration of the kanji searched word.

            List<Word> matchingWordsFromJisho;
            if (internetIsAvailable) {
                matchingWordsFromJisho = Utilities.getWordsFromJishoOnWeb(queryText, getContext());

                if (matchingWordsFromJisho.size() != 0 && requestedSpeechToText) {
                    Word results = matchingWordsFromJisho.get(0);
                    return results.getRomaji();
                } else return null;

            }
            else return null;
        }

        void setLoaderState(boolean state) {
            mAllowLoaderStart = state;
        }
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
                View layout = inflater.inflate(R.layout.custom_queryhistory_spinner, parent, false);
                TextView pastquery = layout.findViewById(R.id.query_value);
                pastquery.setText(mQueryHistory.get(position));
                return layout;
            }
            else return null;
        }
    }
    public void updateQueryHistory() {

        // Implementing a FIFO array for the spinner
        // Add the entry at the beginning of the stack
        // If the entry is already in the spinner, remove that entry

        if (TextUtils.isEmpty(mInputQuery)) return;

        boolean alreadyExistsInHistory = false;
        for (int i = 0; i< mQueryHistory.size(); i++) {
            if (mInputQuery.equalsIgnoreCase(mQueryHistory.get(i))) {
                mQueryHistory.remove(i);
                if (mQueryHistory.size()==0) mQueryHistory.add(mInputQuery);
                else mQueryHistory.add(0, mInputQuery);
                alreadyExistsInHistory = true;
                break;
            }
        }
        if (!alreadyExistsInHistory) {
            if (mQueryHistory.size()==0) mQueryHistory.add(mInputQuery);
            else mQueryHistory.add(0, mInputQuery);
            if (mQueryHistory.size() > QUERY_HISTORY_MAX_SIZE) mQueryHistory.remove(QUERY_HISTORY_MAX_SIZE);
        }

        //Save the query history in the preferences
        if (getContext() != null) {
            String queryHistoryAsString = TextUtils.join(",", mQueryHistory);
            SharedPreferences sharedPref = getContext().getSharedPreferences(getString(R.string.preferences_query_history_list), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(getString(R.string.preferences_query_history_list), queryHistoryAsString);
            editor.apply();
        }

        // Set the dropdown main to include all past entries
        if (getActivity() != null) {

            mInputQueryAutoCompleteTextView.setAdapter(new QueryInputSpinnerAdapter(
                    getActivity().getBaseContext(),
                    R.layout.custom_queryhistory_spinner,
                    mQueryHistory));

            //For some reason the following does nothing
            mInputQueryAutoCompleteTextView.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                    String inputWordString = mInputQueryAutoCompleteTextView.getText().toString();

                    inputQueryOperationsHandler.onDictRequested(inputWordString);
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            });
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


    //Loader methods
    @NonNull @Override public Loader<String> onCreateLoader(int id, final Bundle args) {

        if (id== GET_ROMAJI_FROM_KANJI_USING_JISHO_LOADER) {
            GetRomajiFromKanjiUsingJishoAsyncTaskLoader webResultsAsyncTaskLoader = new GetRomajiFromKanjiUsingJishoAsyncTaskLoader(getContext(), mInputQuery, mRequestedSpeechToText);
            webResultsAsyncTaskLoader.setLoaderState(true);
            return webResultsAsyncTaskLoader;
        }
        else if (id==TESSERACT_OCR_LOADER && getContext()!=null) {

            mProgressDialog = new ProgressDialog(getContext());
            mProgressDialog.setTitle(getContext().getResources().getString(R.string.OCR_waitWhileProcessing));
            mProgressDialog.setMessage(getContext().getResources().getString(R.string.OCR_waitWhileProcessingExplanation));
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getContext().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mTess!=null) mTess.stop();
                    initializeTesseractAPI(mOCRLanguage);
                    if (mTesseractOCRAsyncTask!=null) mTesseractOCRAsyncTask.cancel(true);
                    dialog.dismiss();
                }
            });
            mProgressDialog.show();

            TesseractOCRAsyncTaskLoader tesseractOCRAsyncTaskLoader = new TesseractOCRAsyncTaskLoader(getContext(), mTess);
            tesseractOCRAsyncTaskLoader.setLoaderState(true);
            return tesseractOCRAsyncTaskLoader;
        }
        else {
            return new GetRomajiFromKanjiUsingJishoAsyncTaskLoader(getContext(), "", false);
        }
    }
    @Override public void onLoadFinished(@NonNull Loader<String> loader, String data) {

        if (loader.getId() == GET_ROMAJI_FROM_KANJI_USING_JISHO_LOADER & !mAlreadyGotRomajiFromKanji) {

            mAlreadyGotRomajiFromKanji = true;
            if (data != null) {
                mInputQueryAutoCompleteTextView.setText(data);
            }
            if (!mInputQueryAutoCompleteTextView.getText().toString().equals("")) {
                mDictButton.performClick();
            }
            getLoaderManager().destroyLoader(GET_ROMAJI_FROM_KANJI_USING_JISHO_LOADER);
        }
        else if (loader.getId() == TESSERACT_OCR_LOADER & !mAlreadyGotOcrResult) {
            mAlreadyGotOcrResult = true;
            if (mProgressDialog != null && mProgressDialog.isShowing()) mProgressDialog.dismiss();
            mOcrResultString = data;
            createOcrListDialog(mOcrResultString);
            getLoaderManager().destroyLoader(TESSERACT_OCR_LOADER);
        }
    }
    @Override public void onLoaderReset(@NonNull Loader<String> loader) {

    }


    //Communication with other classes:

    //Communication with parent activity:
    private InputQueryOperationsHandler inputQueryOperationsHandler;
    interface InputQueryOperationsHandler {
        void onDictRequested(String query);
        void onConjRequested(String query);
        void onConvertRequested(String query);
        void onSearchByRadicalRequested(String query);
        void onDecomposeRequested(String query);
    }
    public void setQuery(String query) {
        mInputQueryAutoCompleteTextView.setText(query);
    }


}