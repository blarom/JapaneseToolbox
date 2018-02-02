package com.japanesetoolboxapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.util.Log;
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
import com.japanesetoolboxapp.utiities.GlobalConstants;
import com.japanesetoolboxapp.utiities.SharedMethods;

public class InputQueryFragment extends Fragment implements LoaderManager.LoaderCallbacks<String> {

    //Locals
    private static final int RESULT_OK = -1;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int WEB_SEARCH_LOADER = 42;
    private static final int SPEECH_RECOGNIZER_REQUEST_CODE = 2;
    private static final String SPEECH_RECOGNIZER_EXTRA = "query";
    private	String[] output = {"word","","fast"};
    String[] queryHistory;
    ArrayList<String> new_queryHistory;
    AutoCompleteTextView query;
    Button button_searchVerb;
    Button button_searchWord;
    Button button_choose_Convert;
    Button button_searchTangorin;
    Button button_searchByRadical;
    Button button_Decompose;
    String mQueryText;
    String mCurrentPhotoPath;
    Bitmap mImageToBeDecoded;
    private TessBaseAPI mTess;
    String datapath = "";
    private ImageView picture;

    //Fragment Lifecycle methods
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setRetainInstance(true);
        final View InputQueryFragment = inflater.inflate(R.layout.fragment_inputquery, container, false);

        // Initializations
        query = InputQueryFragment.findViewById(R.id.query);
        mQueryText = "";
        query.setText(mQueryText);

        // Initializing the Tesseract API elements
        mImageToBeDecoded = BitmapFactory.decodeResource(getResources(), R.drawable.test_image);
        String language = "jpn";
        if (getActivity()!=null) {
            datapath = getActivity().getFilesDir() + "/tesseract/";
            mTess = new TessBaseAPI();
            checkFile(new File(datapath + "tessdata/"));
        }
        mTess.init(datapath, language);

        // Restoring inputs from the savedInstanceState (if applicable)
        if (queryHistory == null) {
            queryHistory = new String[7];
            for (int i=0;i<queryHistory.length;i++) { queryHistory[i] = ""; } //7 elements in the array
        }
        Log.i("Diagnosis Time", "Loaded Search History.");

        // Populate the history
        query.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //query.showDropDown();
                query.dismissDropDown();
                return false;
            }
            });

        // When Enter is clicked, do the actions described in the following function
        query.setOnEditorActionListener( new EditText.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String inputWordString = query.getText().toString();

                DisplayQueryHistory(inputWordString, query);
                query.dismissDropDown();


                button_searchVerb.getBackground().clearColorFilter();
                button_searchWord.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.GREEN));
                button_choose_Convert.getBackground().clearColorFilter();
                button_searchTangorin.getBackground().clearColorFilter();
                button_searchByRadical.getBackground().clearColorFilter();
                button_Decompose.getBackground().clearColorFilter();

                if (MainActivity.heap_size_before_decomposition_loader < GlobalConstants.DECOMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE) {
                    button_Decompose.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.RED));
                }
                if (MainActivity.heap_size_before_searchbyradical_loader < GlobalConstants.CHAR_COMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE) {
                    button_searchByRadical.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.RED));
                }

                onWordEntered_PerformThisFunction(inputWordString);
            }
            else {
                //query.showDropDown();
            }
            return true;
        } } );

        // Button listeners
        button_searchVerb = InputQueryFragment.findViewById(R.id.button_searchVerb);
        button_searchVerb.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {
            //EditText inputVerbObject = (EditText)fragmentView.findViewById(R.id.input_verb);
            String inputVerbString = query.getText().toString();

            DisplayQueryHistory(inputVerbString, query);

            button_searchVerb.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.GREEN));
            button_searchWord.getBackground().clearColorFilter();
            button_choose_Convert.getBackground().clearColorFilter();
            button_searchTangorin.getBackground().clearColorFilter();
            button_searchByRadical.getBackground().clearColorFilter();
            button_Decompose.getBackground().clearColorFilter();

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

            if (MainActivity.heap_size_before_decomposition_loader < GlobalConstants.DECOMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE) {
                button_Decompose.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.RED));
            }
            if (MainActivity.heap_size_before_searchbyradical_loader < GlobalConstants.CHAR_COMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE) {
                button_searchByRadical.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.RED));
            }

            onVerbEntered_PerformThisFunction(inputVerbString);
        } } );

        button_searchWord = InputQueryFragment.findViewById(R.id.button_searchWord);
        button_searchWord.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {
            String inputWordString = query.getText().toString();

            DisplayQueryHistory(inputWordString, query);

            button_searchVerb.getBackground().clearColorFilter();
            button_searchWord.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.GREEN));
            button_choose_Convert.getBackground().clearColorFilter();
            button_searchTangorin.getBackground().clearColorFilter();
            button_searchByRadical.getBackground().clearColorFilter();
            button_Decompose.getBackground().clearColorFilter();

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

            if (MainActivity.heap_size_before_decomposition_loader < GlobalConstants.DECOMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE) {
                button_Decompose.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.RED));
            }
            if (MainActivity.heap_size_before_searchbyradical_loader < GlobalConstants.CHAR_COMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE) {
                button_searchByRadical.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.RED));
            }

            onWordEntered_PerformThisFunction(inputWordString);
        } } );

        button_choose_Convert = InputQueryFragment.findViewById(R.id.button_convert);
        button_choose_Convert.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {
            String inputWordString = query.getText().toString();

            DisplayQueryHistory(inputWordString, query);

            button_searchVerb.getBackground().clearColorFilter();
            button_searchWord.getBackground().clearColorFilter();
            button_choose_Convert.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.GREEN));
            button_searchTangorin.getBackground().clearColorFilter();
            button_searchByRadical.getBackground().clearColorFilter();
            button_Decompose.getBackground().clearColorFilter();

            if (MainActivity.heap_size_before_decomposition_loader < GlobalConstants.DECOMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE) {
                button_Decompose.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.RED));
            }
            if (MainActivity.heap_size_before_searchbyradical_loader < GlobalConstants.CHAR_COMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE) {
                button_searchByRadical.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.RED));
            }

            onConvertEntered_PerformThisFunction(inputWordString);
        } } );

        button_searchTangorin = InputQueryFragment.findViewById(R.id.button_searchTangorin);
        button_searchTangorin.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {
            // Search using Tangorin
            String queryString = query.getText().toString();

            DisplayQueryHistory(queryString, query);

            button_searchVerb.getBackground().clearColorFilter();
            button_searchWord.getBackground().clearColorFilter();
            button_choose_Convert.getBackground().clearColorFilter();
            button_searchTangorin.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.GREEN));
            button_searchByRadical.getBackground().clearColorFilter();
            button_Decompose.getBackground().clearColorFilter();

            if (MainActivity.heap_size_before_decomposition_loader < GlobalConstants.DECOMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE) {
                button_Decompose.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.RED));
            }
            if (MainActivity.heap_size_before_searchbyradical_loader < GlobalConstants.CHAR_COMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE) {
                button_searchByRadical.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.RED));
            }

            String website = "http://www.tangorin.com/general/" + queryString;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(website));
            startActivity(intent);
        } } );

        button_searchByRadical = InputQueryFragment.findViewById(R.id.button_searchByRadical);
        button_searchByRadical.setEnabled(true);
        button_searchByRadical.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {
            // Break up a Kanji to Radicals

            String inputWordString = query.getText().toString();

            DisplayQueryHistory(inputWordString, query);

            button_searchVerb.getBackground().clearColorFilter();
            button_searchWord.getBackground().clearColorFilter();
            button_choose_Convert.getBackground().clearColorFilter();
            button_searchTangorin.getBackground().clearColorFilter();
            button_searchByRadical.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.GREEN));
            button_Decompose.getBackground().clearColorFilter();

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
                button_searchByRadical.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.RED));
            }
            else {
                button_searchByRadical.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.GREEN));
                onSearchByRadicalsEntered_PerformThisFunction(inputWordString);
            }
        } } );

        button_Decompose = InputQueryFragment.findViewById(R.id.button_Decompose);
        button_Decompose.setEnabled(true);
        button_Decompose.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {
            // Break up a Kanji to Radicals

            String inputWordString = query.getText().toString();

            DisplayQueryHistory(inputWordString, query);

            button_searchByRadical.getBackground().clearColorFilter();
            button_Decompose.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.GREEN));
            button_searchVerb.getBackground().clearColorFilter();
            button_searchWord.getBackground().clearColorFilter();
            button_choose_Convert.getBackground().clearColorFilter();
            button_searchTangorin.getBackground().clearColorFilter();

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
            if (MainActivity.heap_size_before_searchbyradical_loader < GlobalConstants.CHAR_COMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE) {
                button_searchByRadical.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.RED));
            }
            if (MainActivity.heap_size_before_decomposition_loader < GlobalConstants.DECOMPOSITION_FUNCTION_REQUIRED_MEMORY_HEAP_SIZE) {
                Toast.makeText(getActivity(), "Sorry, your device does not have enough memory to run this function.", Toast.LENGTH_LONG).show();
                button_Decompose.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.RED));
            }
            else {
                button_Decompose.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.GREEN));
                onDecomposeEntered_PerformThisFunction(inputWordString);
            }
        } } );

        Button button_ClearQuery = InputQueryFragment.findViewById(R.id.clearQuery);
        button_ClearQuery.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {
            query.setText("");
        } } );

        Button button_ShowHistory = InputQueryFragment.findViewById(R.id.showHistory);
        button_ShowHistory.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {

            String queryString = query.getText().toString();
            DisplayQueryHistory(queryString, query);
            boolean queryHistoryIsEmpty = true;
            for (String element : queryHistory) {
                if (!element.equals("")) { queryHistoryIsEmpty = false; }
            }
            if (!queryHistoryIsEmpty) {query.showDropDown();}
        } } );

        Button button_Speech = InputQueryFragment.findViewById(R.id.getTextThroughSpeech);
        button_Speech.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {

            int maxResultsToReturn = 1;
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResultsToReturn);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, MainActivity.mChosenSpeechToTextLanguage);
            intent.putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, getResources().getString(R.string.SpeechLanguageEnglish));
            if (MainActivity.mChosenSpeechToTextLanguage.equals(getResources().getString(R.string.SpeechLanguageJapanese))) {
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getResources().getString(R.string.SpeechUserPromptJapanese));
            }
            else if (MainActivity.mChosenSpeechToTextLanguage.equals(getResources().getString(R.string.SpeechLanguageEnglish))) {
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getResources().getString(R.string.SpeechUserPromptEnglish));
            }
            startActivityForResult(intent, SPEECH_RECOGNIZER_REQUEST_CODE);
        } } );

        Button button_Camera = InputQueryFragment.findViewById(R.id.getTextThroughCamera);
        button_Camera.setOnClickListener( new View.OnClickListener() { public void onClick(View v) {
            dispatchTakePictureIntent();
        } } );

        return InputQueryFragment;
    }
    @Override public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof UserEnteredQueryListener) {
            userEnteredQueryListener = (UserEnteredQueryListener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement InputQueryFragment.UserEnteredQueryListener");
        }

    }
    @Override public void onResume() {
        super.onResume();
        query.setText(mQueryText);
    }
    @Override public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        EditText query = (EditText)getActivity().findViewById(R.id.query);
        savedInstanceState.putString("query", query.getText().toString());

        /*RadioButton radio_FastSearch = (RadioButton)GlobalInputQueryFragment.findViewById(R.id.radio_FastSearch);
        if (radio_FastSearch.isChecked()) { savedInstanceState.putBoolean("radio_FastSearch", true); }
        else							  { savedInstanceState.putBoolean("radio_FastSearch", false); }*/

        savedInstanceState.putStringArray("queryHistory", queryHistory);
    }
    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            //Bundle extras = data.getExtras();
            //Bitmap imageBitmap = (Bitmap) extras.get("data");
            //mImageView.setImageBitmap(imageBitmap);
            galleryAddPic();
            createOcrDialog();
        }
        else if (requestCode == SPEECH_RECOGNIZER_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data == null) return;
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            mQueryText = results.get(0);
            query.setText(mQueryText);

            //Attempting to access jisho.org to get the romaji value of the requested word, if it's a Japanese word
            if (getActivity()!=null && MainActivity.mChosenSpeechToTextLanguage.equals(getResources().getString(R.string.SpeechLanguageJapanese))) {
                Bundle queryBundle = new Bundle();
                queryBundle.putString(SPEECH_RECOGNIZER_EXTRA, results.get(0));

                LoaderManager loaderManager = getActivity().getSupportLoaderManager();
                Loader<String> WebSearchLoader = loaderManager.getLoader(WEB_SEARCH_LOADER);
                if (WebSearchLoader == null)  loaderManager.initLoader(WEB_SEARCH_LOADER, queryBundle, this);
                else loaderManager.restartLoader(WEB_SEARCH_LOADER, queryBundle, this);
            }
        }

    }

    //Image methods
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (getActivity()!=null && takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(getActivity(), "com.japanesetoolboxapp", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }
    private Uri getOcrPictureUri() {
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        return contentUri;
    }
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(getOcrPictureUri());
        if (getActivity()!=null) getActivity().sendBroadcast(mediaScanIntent);
    }
    @Nullable private File createImageFile() throws IOException {
        if (getActivity()==null) return null;
        // Create an mImageToBeDecoded file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }
    private void scaleImage(ImageView pictureHolder, double scaleFactor) {
        // Get the dimensions of the View
        int targetW = (int) Math.floor(pictureHolder.getWidth()*scaleFactor);
        int targetH = (int) Math.floor(pictureHolder.getHeight()*scaleFactor);

        int ih = pictureHolder.getMeasuredHeight();//height of imageView
        int iw = pictureHolder.getMeasuredWidth();//width of imageView
        int iH = pictureHolder.getDrawable().getIntrinsicHeight();//original height of underlying image
        int iW = pictureHolder.getDrawable().getIntrinsicWidth();//original width of underlying image

        if (ih/iH <= iw/iW) iw = iW*ih/iH;//rescaled width of image within ImageView
        else ih = iH*iw/iW;//rescaled height of image within ImageView

        pictureHolder.setMinimumHeight(targetH);
        pictureHolder.setMinimumWidth(targetW);

//        // Get the dimensions of the bitmap
//        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
//        bmOptions.inJustDecodeBounds = true;
//        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
//        int photoW = bmOptions.outWidth;
//        int photoH = bmOptions.outHeight;
//
//        // Determine how much to scale down the image
//        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);
//
//        // Decode the image file into a Bitmap sized to fill the View
//        bmOptions.inJustDecodeBounds = false;
//        bmOptions.inSampleSize = scaleFactor;
//        bmOptions.inPurgeable = true;
//
//        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
//        pictureHolder.setImageBitmap(bitmap);
    }
    private Bitmap adjustBitmap(Bitmap source, float angle, double scaleFactor) {

        int newWidth = (int) Math.floor(source.getWidth()*scaleFactor);
        int newHeight = (int) Math.floor(source.getHeight()*scaleFactor);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(source, newWidth, newHeight,true);

        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap , 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

        return rotatedBitmap;
    }
    private Bitmap changeBitmapContrastBrightness(Bitmap bmp, float contrast, float brightness) {
        //https://stackoverflow.com/questions/12891520/how-to-programmatically-change-contrast-of-a-bitmap-in-android
        /*
         * @param bmp input bitmap
         * @param contrast 0..10 1 is default
         * @param brightness -255..255 0 is default
         * @return new bitmap
         */
        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        contrast, 0, 0, 0, brightness,
                        0, contrast, 0, 0, brightness,
                        0, 0, contrast, 0, brightness,
                        0, 0, 0, 1, 0
                });

        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

        Canvas canvas = new Canvas(ret);

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bmp, 0, 0, paint);

        return ret;
    }

    //Tesseract methods
    public String processImage(Bitmap imageToBeDecoded){
        String OCRresult = null;
        mTess.setImage(imageToBeDecoded);
        OCRresult = mTess.getUTF8Text();
        //query.setText(OCRresult);
        return OCRresult;
    }
    private void checkFile(File dir) {
        if (!dir.exists()&& dir.mkdirs()){
            copyFiles();
        }
        if(dir.exists()) {
            String datafilepath = datapath+ "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);

            if (!datafile.exists()) {
                copyFiles();
            }
        }
    }
    private void copyFiles() {
        try {
            String filepath = datapath + "/tessdata/eng.traineddata";
            AssetManager assetManager = getActivity().getAssets();

            InputStream instream = assetManager.open("tessdata/eng.traineddata");
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
    @TargetApi(Build.VERSION_CODES.LOLLIPOP) private void createOcrDialog() {

        if (getActivity()==null) return;

        //Performing OCR on the input image
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = false;
        mImageToBeDecoded = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mImageToBeDecoded = adjustBitmap(mImageToBeDecoded, 90, 0.2);
        mImageToBeDecoded = changeBitmapContrastBrightness(mImageToBeDecoded, 1, 0);
        String ocrResultString = processImage(mImageToBeDecoded);
        mImageToBeDecoded = adjustBitmap(mImageToBeDecoded, 0, 0.5);

        //Setting the elements in the dialog
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View dialogView = inflater.inflate(R.layout.custom_ocr_dialog, null);

        ImageView ocrPictureHolder = dialogView.findViewById(R.id.ocrPicture);
        //ocrPictureHolder.setImageURI(getOcrPictureUri());
        //ocrPictureHolder.setRotation(90);
        ocrPictureHolder.setImageBitmap(mImageToBeDecoded);

        ListView ocrResultsListView =  dialogView.findViewById(R.id.ocrResultsList);
        final String ocrResultsList[] = ocrResultString.split("\\r\\n|\\n|\\r");
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<> (getActivity(), android.R.layout.simple_list_item_1, ocrResultsList);

        query.setText(ocrResultsList[0]);
        ocrResultsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                query.setText(ocrResultsList[position]);
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

        scaleImage(ocrPictureHolder, 1);
        dialog.show();
    }

    //Asyncronous methods
    @Override public Loader<String> onCreateLoader(int id, final Bundle args) {
        return new AsyncTaskLoader<String>(getContext()) {

            @Override
            protected void onStartLoading() {

                /* If no arguments were passed, we don't have a query to perform. Simply return. */
                if (args == null) return;
                forceLoad();
            }

            @Override
            public String loadInBackground() {
                //This method retrieves the first romaji transliteration of the kanji searched word.

                List<Object> AsyncMatchingWordCharacteristics = new ArrayList<>();
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
        };
    }
    @Override public void onLoadFinished(Loader<String> loader, String data) {
        if (data!=null) {
            query.setText(data);
        }
        if (!query.getText().toString().equals("")) {
            button_searchWord.performClick();
        }
    }
    @Override public void onLoaderReset(Loader<String> loader) {

    }

    //Query display methods
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
            TextView pastquery = (TextView) mySpinner.findViewById(R.id.pastQuery);
            pastquery.setText(new_queryHistory.get(position));

            return mySpinner;
        }
    }
    public void DisplayQueryHistory(String queryStr, final AutoCompleteTextView query) {

        // Implementing a FIFO array for the spinner
        // Add the entry at the beginning of the stack
        // If the entry is already in the spinner, remove that entry

            if (!queryHistory[0].equals(queryStr)) { // if the query hasn't changed, don't change the dropdown list
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

    //Interface methods
    private UserEnteredQueryListener userEnteredQueryListener;
    interface UserEnteredQueryListener {
        // Interface used to transfer the verb to ConjugatorFragment or DictionaryFragment
        void OnQueryEnteredSwitchToRelevantFragment(String[] output);
    }
//    @Override public void OnUserSelectedWordsUpdateInputQuery(String output) {
//        mQueryText = output;
//        query.setText(mQueryText);
//    }

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