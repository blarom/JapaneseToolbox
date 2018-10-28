package com.japanesetoolboxapp.ui;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.japanesetoolboxapp.R;
import com.japanesetoolboxapp.data.ConjugationTitle;
import com.japanesetoolboxapp.data.JapaneseToolboxCentralRoomDatabase;
import com.japanesetoolboxapp.data.Verb;
import com.japanesetoolboxapp.data.Word;
import com.japanesetoolboxapp.resources.GlobalConstants;
import com.japanesetoolboxapp.resources.MainApplication;
import com.japanesetoolboxapp.resources.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class ConjugatorFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Object> {


    //region Parameters
    private static final int ROOM_DB_VERB_SEARCH_LOADER = 6542;

    @BindView(R.id.verb_chooser_spinner) Spinner mVerbChooserSpinner;
    @BindView(R.id.conjugations_chooser_spinner) Spinner mConjugationChooserSpinner;
    @BindView(R.id.verb_hint) TextView mVerbHintTextView;
    @BindView(R.id.verb_results_loading_indicator) ProgressBar mProgressBarLoadingIndicator;
    @BindView(R.id.conjugations_container) ScrollView mConjugationsContainerScrollView;
    @BindView(R.id.radio_romaji_or_kanji) RadioGroup mRomajiOrKanjiRadioButton;
    @BindView(R.id.radio_Romaji) RadioButton mRomajiRadioButton;
    @BindView(R.id.radio_Kanji) RadioButton mKanjiRadioButton;

    @BindView(R.id.Tense0) TextView mConjugationDisplayTense0;
    @BindView(R.id.Tense1) TextView mConjugationDisplayTense1;
    @BindView(R.id.Tense2) TextView mConjugationDisplayTense2;
    @BindView(R.id.Tense3) TextView mConjugationDisplayTense3;
    @BindView(R.id.Tense4) TextView mConjugationDisplayTense4;
    @BindView(R.id.Tense5) TextView mConjugationDisplayTense5;
    @BindView(R.id.Tense6) TextView mConjugationDisplayTense6;
    @BindView(R.id.Tense7) TextView mConjugationDisplayTense7;
    @BindView(R.id.Tense8) TextView mConjugationDisplayTense8;
    @BindView(R.id.Tense9) TextView mConjugationDisplayTense9;
    @BindView(R.id.Tense10) TextView mConjugationDisplayTense10;
    @BindView(R.id.Tense11) TextView mConjugationDisplayTense11;
    @BindView(R.id.Tense12) TextView mConjugationDisplayTense12;
    @BindView(R.id.Tense13) TextView mConjugationDisplayTense13;

    @BindView(R.id.TenseLayout0) LinearLayout mConjugationDisplayTenseLayout0;
    @BindView(R.id.TenseLayout1) LinearLayout mConjugationDisplayTenseLayout1;
    @BindView(R.id.TenseLayout2) LinearLayout mConjugationDisplayTenseLayout2;
    @BindView(R.id.TenseLayout3) LinearLayout mConjugationDisplayTenseLayout3;
    @BindView(R.id.TenseLayout4) LinearLayout mConjugationDisplayTenseLayout4;
    @BindView(R.id.TenseLayout5) LinearLayout mConjugationDisplayTenseLayout5;
    @BindView(R.id.TenseLayout6) LinearLayout mConjugationDisplayTenseLayout6;
    @BindView(R.id.TenseLayout7) LinearLayout mConjugationDisplayTenseLayout7;
    @BindView(R.id.TenseLayout8) LinearLayout mConjugationDisplayTenseLayout8;
    @BindView(R.id.TenseLayout9) LinearLayout mConjugationDisplayTenseLayout9;
    @BindView(R.id.TenseLayout10) LinearLayout mConjugationDisplayTenseLayout10;
    @BindView(R.id.TenseLayout11) LinearLayout mConjugationDisplayTenseLayout11;
    @BindView(R.id.TenseLayout12) LinearLayout mConjugationDisplayTenseLayout12;
    @BindView(R.id.TenseLayout13) LinearLayout mConjugationDisplayTenseLayout13;

    @BindView(R.id.Tense0_Result) TextView mConjugationDisplayTenseResult0;
    @BindView(R.id.Tense1_Result) TextView mConjugationDisplayTenseResult1;
    @BindView(R.id.Tense2_Result) TextView mConjugationDisplayTenseResult2;
    @BindView(R.id.Tense3_Result) TextView mConjugationDisplayTenseResult3;
    @BindView(R.id.Tense4_Result) TextView mConjugationDisplayTenseResult4;
    @BindView(R.id.Tense5_Result) TextView mConjugationDisplayTenseResult5;
    @BindView(R.id.Tense6_Result) TextView mConjugationDisplayTenseResult6;
    @BindView(R.id.Tense7_Result) TextView mConjugationDisplayTenseResult7;
    @BindView(R.id.Tense8_Result) TextView mConjugationDisplayTenseResult8;
    @BindView(R.id.Tense9_Result) TextView mConjugationDisplayTenseResult9;
    @BindView(R.id.Tense10_Result) TextView mConjugationDisplayTenseResult10;
    @BindView(R.id.Tense11_Result) TextView mConjugationDisplayTenseResult11;
    @BindView(R.id.Tense12_Result) TextView mConjugationDisplayTenseResult12;
    @BindView(R.id.Tense13_Result) TextView mConjugationDisplayTenseResult13;

    private Unbinder mBinding;
    private String mInputQuery;
    private int mSelectedConjugationCategoryIndex;
    private String mChosenRomajiOrKanji;
    private List<Verb> mMatchingVerbs;
    private List<ConjugationTitle> mConjugationTitles;
    private int mInputQueryTextType;
    private List<String> mInputQueryTransliterations;
    private boolean mInputQueryIsInvalid;
    private boolean mAlreadyLoadedRoomResults;
    private List<Verb> mCompleteVerbsList;
    private List<String[]> mVerbLatinConjDatabase;
    private List<String[]> mVerbKanjiConjDatabase;
    private List<Word> mWordsFromDictFragment;
    private Typeface mDroidSansJapaneseTypeface;
    //endregion


    //Lifecycle Functions
    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getExtras();
        initializeParameters();
    }
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //setRetainInstance(true); //causes memory leaks
        View rootView = inflater.inflate(R.layout.fragment_conjugator, container, false);

        mBinding = ButterKnife.bind(this, rootView);

        if (!TextUtils.isEmpty(mInputQuery)) SearchForConjugations();
        else showHint();

        if (getContext()!=null) {
            AssetManager am = getContext().getApplicationContext().getAssets();
            mDroidSansJapaneseTypeface = Typeface.createFromAsset(am, String.format(Locale.JAPAN, "fonts/%s", "DroidSansJapanese.ttf"));
        }

        return rootView;
    }
    @Override public void onResume() {
        super.onResume();
    }
    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }
    @Override public void onPause() {
        super.onPause();
    }
    @Override public void onDetach() {
        super.onDetach();
        if (getLoaderManager()!=null) getLoaderManager().destroyLoader(ROOM_DB_VERB_SEARCH_LOADER);
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
        if (getActivity()!=null && MainApplication.getRefWatcher(getActivity())!=null) MainApplication.getRefWatcher(getActivity()).watch(this);
    }


	//Functionality Functions
    private void getExtras() {
        if (getArguments()!=null) {
            mInputQuery = getArguments().getString(getString(R.string.user_query_word));
            mVerbLatinConjDatabase = (List<String[]>) getArguments().getSerializable(getString(R.string.latin_conj_database));
            mVerbKanjiConjDatabase = (List<String[]>) getArguments().getSerializable(getString(R.string.kanji_conj_database));
            mWordsFromDictFragment = getArguments().getParcelableArrayList(getString(R.string.words_list));
        }
    }
    private void initializeParameters() {

        mMatchingVerbs = new ArrayList<>();
        mCompleteVerbsList = new ArrayList<>();
    }
    public void SearchForConjugations() {

        hideAll();
        mConjugationTitles = getConjugationTitles();
        getInputQueryParameters();
        findMatchingVerbsInRoomDb();

    }
    private List<ConjugationTitle> getConjugationTitles() {

        String[] titlesRow = mVerbLatinConjDatabase.get(0);
        String[] subtitlesRow = mVerbLatinConjDatabase.get(1);
        String[] endingsRow = mVerbLatinConjDatabase.get(2);
        int sheetLength = titlesRow.length;
        List<ConjugationTitle> conjugationTitles = new ArrayList<>();
        List<ConjugationTitle.Subtitle> subtitles = new ArrayList<>();
        ConjugationTitle conjugationTitle = new ConjugationTitle();

        for (int col = 0; col < sheetLength; col++) {

            if (col == 0) {
                conjugationTitle.setTitle(titlesRow[col]);
                conjugationTitle.setTitleIndex(col);

                ConjugationTitle.Subtitle subtitle = new ConjugationTitle.Subtitle();
                subtitle.setSubtitle(subtitlesRow[col]);
                subtitle.setSubtitleIndex(col);
                subtitles.add(subtitle);
            }
            else if (col == sheetLength -1) {
                conjugationTitle.setSubtitles(subtitles);
                conjugationTitles.add(conjugationTitle);
            }
            else {
                if (!titlesRow[col].equals("")) {

                    conjugationTitle.setSubtitles(subtitles);
                    conjugationTitles.add(conjugationTitle);

                    conjugationTitle = new ConjugationTitle();
                    subtitles = new ArrayList<>();

                    conjugationTitle.setTitle(titlesRow[col]);
                    conjugationTitle.setTitleIndex(col);

                }

                ConjugationTitle.Subtitle subtitle = new ConjugationTitle.Subtitle();
                subtitle.setSubtitle(subtitlesRow[col]);
                subtitle.setEnding(endingsRow[col]);
                subtitle.setSubtitleIndex(col);
                subtitles.add(subtitle);
            }
        }

        return conjugationTitles;
    }
    private void findMatchingVerbsInRoomDb() {
        if (getActivity()!=null && !mInputQueryIsInvalid) {
            showLoadingIndicator();
            LoaderManager loaderManager = getActivity().getSupportLoaderManager();
            Loader<String> roomDbSearchLoader = loaderManager.getLoader(ROOM_DB_VERB_SEARCH_LOADER);
            Bundle bundle = new Bundle();
            bundle.putString(getString(R.string.saved_input_query), mInputQuery);
            if (roomDbSearchLoader == null) loaderManager.initLoader(ROOM_DB_VERB_SEARCH_LOADER, bundle, this);
            else loaderManager.restartLoader(ROOM_DB_VERB_SEARCH_LOADER, bundle, this);
        }
    }
    private void getInputQueryParameters() {

        //Converting the word to lowercase (the search algorithm is not efficient if needing to search both lower and upper case)
        mInputQuery = mInputQuery.toLowerCase(Locale.ENGLISH);

        mInputQueryTransliterations = ConvertFragment.getLatinHiraganaKatakana(mInputQuery);

        mInputQueryTextType = ConvertFragment.getTextType(mInputQuery);
    }
    private void displayVerbsInVerbChooserSpinner() {

        if (getActivity()==null) return;

        hideLoadingIndicator();
        if (mMatchingVerbs.size() != 0) {
            showResults();
            mVerbChooserSpinner.setAdapter(new VerbSpinnerAdapter(getContext(), R.layout.custom_verbchooser_spinner, mMatchingVerbs));
            mVerbChooserSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1, int verbIndex, long id) {
                    showSelectedVerbConjugations(verbIndex);
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            });
        } else showHint();

    }
    private void showSelectedVerbConjugations(final int verbIndex) {

        if (getActivity()!=null) Utilities.hideSoftKeyboard(getActivity());
        if (mMatchingVerbs.size() == 0) return;

        //Showing the verb conjugations
        Verb verb = mMatchingVerbs.get(verbIndex);
        List<Verb.ConjugationCategory> conjugationCategories = verb.getConjugationCategories();
        List<ConjugationTitle> conjugationTitles = new ArrayList<>(mConjugationTitles);
        conjugationTitles.remove(0);
        mConjugationChooserSpinner.setAdapter(new ConjugationsSpinnerAdapter(
                getContext(),
                R.layout.custom_conjugationchooser_spinner,
                conjugationCategories,
                conjugationTitles));
        mConjugationChooserSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, final int conjugationIndex, long id) {
                mSelectedConjugationCategoryIndex = conjugationIndex;
                showSelectedConjugationsInCategory(verbIndex, conjugationIndex);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        //Hiding the subsequent fields of there is nothing to show
        mConjugationsContainerScrollView.setVisibility(View.VISIBLE);
        if (verb.getConjugationCategories().size() == 0) {
            mConjugationsContainerScrollView.setVisibility(View.GONE);
        }


        //Setting the conjugation spinner to the position of the first item matching the user query
        List<Verb.ConjugationCategory.Conjugation> conjugations;
        int matchingConjugationCategoryIndex = 0;
        boolean foundMatch = false;
        if (!mInputQuery.equals(verb.getLatinRoot()) && !mInputQuery.equals(verb.getKanjiRoot())) {
            for (int i=0; i<conjugationCategories.size(); i++) {
                conjugations = conjugationCategories.get(i).getConjugations();
                for (Verb.ConjugationCategory.Conjugation conjugation : conjugations) {
                    
                    if (mInputQueryTextType == GlobalConstants.VALUE_LATIN && conjugation.getConjugationLatin().equals(mInputQuery)) {
                        foundMatch = true;
                    }
                    else if ((mInputQueryTextType == GlobalConstants.VALUE_HIRAGANA || mInputQueryTextType == GlobalConstants.VALUE_KATAKANA)
                        && conjugation.getConjugationLatin().equals(mInputQueryTransliterations.get(0))) {
                        foundMatch = true;
                    }
                    else if (mInputQueryTextType == GlobalConstants.VALUE_KANJI
                        && conjugation.getConjugationKanji().equals(mInputQuery)) {
                        foundMatch = true;
                    }

                    if (foundMatch) break;
                }
                if (foundMatch) {
                    matchingConjugationCategoryIndex = i;
                    break;
                }
            }
        }
        mConjugationChooserSpinner.setSelection(matchingConjugationCategoryIndex, false);

    }
    private void showSelectedConjugationsInCategory(final int verbIndex, final int conjugationIndex) {

        // Getting the user choice for displaying the conjugations in Romaji or Kanji

        mRomajiOrKanjiRadioButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (getActivity()==null) return;

                RadioButton checkedRadioButton = getActivity().findViewById(checkedId);

                // Define an action depending on the given boolean, ie. depending on the checked RadioButton ID
                mChosenRomajiOrKanji = "";
                switch (checkedRadioButton.getId()) {
                    case R.id.radio_Romaji:
                        if (checkedRadioButton.isChecked()) {
                            mChosenRomajiOrKanji = "Romaji";
                        }
                        break;
                    case R.id.radio_Kanji:
                        if (checkedRadioButton.isChecked()) {
                            mChosenRomajiOrKanji = "Kanji";
                        }
                        break;
                }

                displayConjugationsOfSelectedCategory(verbIndex, conjugationIndex);
            }
        });

        mChosenRomajiOrKanji = "Romaji";
        if (ConvertFragment.getTextType(mInputQuery) == GlobalConstants.VALUE_KANJI) {
            mChosenRomajiOrKanji = "Kanji";
            mRomajiRadioButton.setChecked(false);
            mKanjiRadioButton.setChecked(true);
        } else {
            mRomajiRadioButton.setChecked(true);
            mKanjiRadioButton.setChecked(false);
        }

        displayConjugationsOfSelectedCategory(verbIndex, conjugationIndex);
    }
    public void displayConjugationsOfSelectedCategory(int verbIndex, int conjugationIndex) {
        if (getActivity()==null) return;

        List<TextView> Tense = new ArrayList<>();
        List<LinearLayout> TenseLayout = new ArrayList<>();
        List<TextView> Tense_Result = new ArrayList<>();

        Tense.add(mConjugationDisplayTense0);
        Tense.add(mConjugationDisplayTense1);
        Tense.add(mConjugationDisplayTense2);
        Tense.add(mConjugationDisplayTense3);
        Tense.add(mConjugationDisplayTense4);
        Tense.add(mConjugationDisplayTense5);
        Tense.add(mConjugationDisplayTense6);
        Tense.add(mConjugationDisplayTense7);
        Tense.add(mConjugationDisplayTense8);
        Tense.add(mConjugationDisplayTense9);
        Tense.add(mConjugationDisplayTense10);
        Tense.add(mConjugationDisplayTense11);
        Tense.add(mConjugationDisplayTense12);
        Tense.add(mConjugationDisplayTense13);

        TenseLayout.add(mConjugationDisplayTenseLayout0);
        TenseLayout.add(mConjugationDisplayTenseLayout1);
        TenseLayout.add(mConjugationDisplayTenseLayout2);
        TenseLayout.add(mConjugationDisplayTenseLayout3);
        TenseLayout.add(mConjugationDisplayTenseLayout4);
        TenseLayout.add(mConjugationDisplayTenseLayout5);
        TenseLayout.add(mConjugationDisplayTenseLayout6);
        TenseLayout.add(mConjugationDisplayTenseLayout7);
        TenseLayout.add(mConjugationDisplayTenseLayout8);
        TenseLayout.add(mConjugationDisplayTenseLayout9);
        TenseLayout.add(mConjugationDisplayTenseLayout10);
        TenseLayout.add(mConjugationDisplayTenseLayout11);
        TenseLayout.add(mConjugationDisplayTenseLayout12);
        TenseLayout.add(mConjugationDisplayTenseLayout13);

        Tense_Result.add(mConjugationDisplayTenseResult0);
        Tense_Result.add(mConjugationDisplayTenseResult1);
        Tense_Result.add(mConjugationDisplayTenseResult2);
        Tense_Result.add(mConjugationDisplayTenseResult3);
        Tense_Result.add(mConjugationDisplayTenseResult4);
        Tense_Result.add(mConjugationDisplayTenseResult5);
        Tense_Result.add(mConjugationDisplayTenseResult6);
        Tense_Result.add(mConjugationDisplayTenseResult7);
        Tense_Result.add(mConjugationDisplayTenseResult8);
        Tense_Result.add(mConjugationDisplayTenseResult9);
        Tense_Result.add(mConjugationDisplayTenseResult10);
        Tense_Result.add(mConjugationDisplayTenseResult11);
        Tense_Result.add(mConjugationDisplayTenseResult12);
        Tense_Result.add(mConjugationDisplayTenseResult13);

        for (int i=0;i<Tense.size();i++) {
            Tense.get(i).setText("");
            Tense.get(i).setPadding(0,8,0,0);
            TenseLayout.get(i).setVisibility(View.GONE);
            Tense_Result.get(i).setText("");
            if (mChosenRomajiOrKanji.equals("Romaji")) Tense_Result.get(i).setTypeface(null, Typeface.BOLD);
            else if (mDroidSansJapaneseTypeface!=null) {
                Tense_Result.get(i).setTypeface(mDroidSansJapaneseTypeface);
            }
        }

        Verb verb = mMatchingVerbs.get(verbIndex);
        Verb.ConjugationCategory conjugationCategory = verb.getConjugationCategories().get(conjugationIndex);
        List<Verb.ConjugationCategory.Conjugation> conjugations = conjugationCategory.getConjugations();

        for (int i = 0; i < conjugations.size(); i++) {

            Tense.get(i).setText(mConjugationTitles.get(conjugationIndex+1).getSubtitles().get(i).getTense());

            if (mChosenRomajiOrKanji.equals("Romaji")) Tense_Result.get(i).setText(conjugations.get(i).getConjugationLatin());
            else Tense_Result.get(i).setText(conjugations.get(i).getConjugationKanji());

            TenseLayout.get(i).setVisibility(View.VISIBLE);
        }
    }
    private void showLoadingIndicator() {
        if (mProgressBarLoadingIndicator!=null) mProgressBarLoadingIndicator.setVisibility(View.VISIBLE);
    }
    private void hideLoadingIndicator() {
        if (mProgressBarLoadingIndicator!=null) mProgressBarLoadingIndicator.setVisibility(View.INVISIBLE);
    }
    private void hideAll() {
        mVerbHintTextView.setVisibility(View.GONE);
        mVerbChooserSpinner.setVisibility(View.GONE);
        mConjugationsContainerScrollView.setVisibility(View.GONE);
    }
    private void showHint() {
        mVerbHintTextView.setVisibility(View.VISIBLE);
        mVerbChooserSpinner.setVisibility(View.GONE);
        mConjugationsContainerScrollView.setVisibility(View.GONE);
    }
    private void showResults() {
        mVerbHintTextView.setVisibility(View.GONE);
        mVerbChooserSpinner.setVisibility(View.VISIBLE);
        mConjugationsContainerScrollView.setVisibility(View.VISIBLE);
    }

    private class VerbSpinnerAdapter extends ArrayAdapter<Verb> {
        // Code adapted from http://mrbool.com/how-to-customize-spinner-in-android/28286

        List<Verb> verbs;

        VerbSpinnerAdapter(Context ctx, int txtViewResourceId, List<Verb> verbs) {
            super(ctx, txtViewResourceId, verbs);
            this.verbs = verbs;
        }
        @Override public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }
        @NonNull @Override public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }
        View getCustomView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = LayoutInflater.from(getContext());
            View mySpinner = inflater.inflate(R.layout.custom_verbchooser_spinner, parent, false);

            String SpinnerText;
            Verb verb = verbs.get(position);

            //Setting the preposition
            if (verb.getPreposition().equals("")) SpinnerText = "";
            else SpinnerText = "[" + verb.getPreposition() + "] ";
            TextView verbchooser_Prep = mySpinner.findViewById(R.id.verbchooser_Prep);
            verbchooser_Prep.setText(SpinnerText);

            //Setting the Kanji and Romaji
            SpinnerText = verb.getKanji() + " (" + verb.getRomaji() + ")";
            TextView verbchooser_Kanji_and_ustem = mySpinner.findViewById(R.id.verbchooser_Kanji_and_ustem);
            verbchooser_Kanji_and_ustem.setText(SpinnerText);

            //Setting the trans./intrans.
            if (!verb.getFamily().equals("")) {
                SpinnerText = verb.getFamily();
                if (!verb.getTrans().equals("")) SpinnerText = SpinnerText + ", " + verb.getTrans();
            }
            else {
                if (!verb.getTrans().equals("")) SpinnerText = verb.getTrans();
                else { SpinnerText = ""; }
            }
            TextView verbchooser_Characteristics = mySpinner.findViewById(R.id.verbchooser_Characteristics);
            verbchooser_Characteristics.setText(SpinnerText);

            //Setting the meaning
            SpinnerText = verb.getMeaning();
            TextView verbchooser_EnglishMeaning = mySpinner.findViewById(R.id.verbchooser_EnglishMeaning);
            verbchooser_EnglishMeaning.setText(SpinnerText);

            return mySpinner;
        }
    }
    private class ConjugationsSpinnerAdapter extends ArrayAdapter<Verb.ConjugationCategory> {
        // Code adapted from http://mrbool.com/how-to-customize-spinner-in-android/28286

        List<Verb.ConjugationCategory> conjugationCategories;
        private final List<ConjugationTitle> conjugationTitles;

        ConjugationsSpinnerAdapter(Context ctx, int txtViewResourceId,
                                   List<Verb.ConjugationCategory> conjugationCategories,
                                   List<ConjugationTitle> conjugationTitles) {
            super(ctx, txtViewResourceId, conjugationCategories);
            this.conjugationCategories = conjugationCategories;
            this.conjugationTitles = conjugationTitles;
        }

        @Override public View getDropDownView(int position, View cnvtView, @NonNull ViewGroup parent) {
            return getCustomView(position, cnvtView, parent);
        }

        @NonNull @Override public View getView(int pos, View cnvtView, @NonNull ViewGroup parent) {
            return getCustomView(pos, cnvtView, parent);
        }

        View getCustomView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = LayoutInflater.from(getContext());
            View mySpinner = inflater.inflate(R.layout.custom_conjugationchooser_spinner, parent, false);

            String SpinnerText;
            Verb.ConjugationCategory conjugationCategory = conjugationCategories.get(position);

            //Setting the title
            int shownPosition = position+1;
            SpinnerText = shownPosition + ". " + conjugationTitles.get(position).getTitle();
            TextView Upper_text = mySpinner.findViewById(R.id.UpperPart);
            Upper_text.setText(SpinnerText);
            Upper_text.setTypeface(mDroidSansJapaneseTypeface, Typeface.BOLD);

            //Displaying the first element in the Conjugation, e.g. PrPlA in Simple Form
            SpinnerText = conjugationCategory.getConjugations().get(0).getConjugationLatin();
            TextView Lower_text = mySpinner.findViewById(R.id.LowerPart);
            Lower_text.setText(SpinnerText);

            return mySpinner;
        }
    }


    //Asynchronous methods
    @NonNull @Override public Loader<Object> onCreateLoader(int id, final Bundle args) {

        String inputQuery = "";
        if (args!=null && args.getString(getString(R.string.saved_input_query))!=null) {
            inputQuery = args.getString(getString(R.string.saved_input_query));
        }

        if (id == ROOM_DB_VERB_SEARCH_LOADER){
            VerbSearchAsyncTaskLoader roomDbVerbSearchLoader = new VerbSearchAsyncTaskLoader(
                    getContext(), mCompleteVerbsList, inputQuery, mConjugationTitles, mVerbLatinConjDatabase, mVerbKanjiConjDatabase, mWordsFromDictFragment);
            return roomDbVerbSearchLoader;
        }
        else return new VerbSearchAsyncTaskLoader(getContext(), null, "", null, null, null, null);
    }
    @Override public void onLoadFinished(@NonNull Loader<Object> loader, Object data) {

        if (loader.getId() == ROOM_DB_VERB_SEARCH_LOADER && !mAlreadyLoadedRoomResults && data!=null) {
            mAlreadyLoadedRoomResults = true;
            mMatchingVerbs = (List<Verb>) data;

            //Displaying the local results
            displayVerbsInVerbChooserSpinner();

            if (getLoaderManager()!=null) getLoaderManager().destroyLoader(ROOM_DB_VERB_SEARCH_LOADER);
        }

    }
    @Override public void onLoaderReset(@NonNull Loader<Object> loader) {}
    private static class VerbSearchAsyncTaskLoader extends AsyncTaskLoader<Object> {

        //region Parameters
        private List<Verb> mCompleteVerbsList;
        private final List<Word> mWordsFromDictFragment;
        private String mInputQuery;
        private List<ConjugationTitle> mConjugationTitles;
        private int mInputQueryTextType;
        private List<String> mInputQueryTransliterations;
        private boolean mInputQueryTransliterationIsInvalid;
        private String mInputQueryTransliteratedLatinForm;
        private String mInputQueryTransliteratedKanaForm;
        private String mInputQueryTransliteratedLatinFormContatenated;
        private String mInputQueryTransliteratedKanaFormContatenated;
        private int mInputQueryTransliteratedLatinFormContatenatedLength;
        private int mInputQueryLength;
        private String mInputQueryContatenated;
        private int mInputQueryContatenatedLength;
        private List<long[]> mMatchingVerbIdsAndCols;
        private JapaneseToolboxCentralRoomDatabase mJapaneseToolboxCentralRoomDatabase;
        private HashMap<String, Integer> mFamilyConjugationIndexes = new HashMap<>();
        private int mInputQueryTransliteratedKanaFormContatenatedLength;
        private List<String[]> mVerbLatinConjDatabase;
        private List<String[]> mVerbKanjiConjDatabase;
        //endregion

        VerbSearchAsyncTaskLoader(Context context, List<Verb> completeVerbsList, String inputQuery,
                                  List<ConjugationTitle> conjugationTitles,
                                  List<String[]> mVerbLatinConjDatabase,
                                  List<String[]> mVerbKanjiConjDatabase,
                                  List<Word> mWordsFromDictFragment) {
            super(context);
            this.mCompleteVerbsList = completeVerbsList;
            this.mInputQuery = inputQuery;
            this.mConjugationTitles = conjugationTitles;
            this.mVerbLatinConjDatabase = mVerbLatinConjDatabase;
            this.mVerbKanjiConjDatabase = mVerbKanjiConjDatabase;
            this.mWordsFromDictFragment = mWordsFromDictFragment;
        }

        @Override
        protected void onStartLoading() {
            if (!TextUtils.isEmpty(mInputQuery)) forceLoad();
        }

        @Override
        public Object loadInBackground() {

            mJapaneseToolboxCentralRoomDatabase = JapaneseToolboxCentralRoomDatabase.getInstance(getContext());
            if (mCompleteVerbsList.size()==0) mCompleteVerbsList = mJapaneseToolboxCentralRoomDatabase.getAllVerbs();

            List<Verb> mMatchingVerbs = new ArrayList<>();
            if (!TextUtils.isEmpty(mInputQuery)) {
                setInputQueryParameters();
                getFamilyConjugationIndexes();

                mMatchingVerbIdsAndCols = getMatchingVerbIdsAndCols();
                mMatchingVerbIdsAndCols = sortMatchingVerbsList(mMatchingVerbIdsAndCols);
                mMatchingVerbs = getVerbs(mMatchingVerbIdsAndCols);
            }

            return mMatchingVerbs;
        }

        private void setInputQueryParameters() {

            //Converting the word to lowercase (the search algorithm is not efficient if needing to search both lower and upper case)
            mInputQuery = mInputQuery.toLowerCase(Locale.ENGLISH);
            mInputQueryLength = mInputQuery.length();

            mInputQueryTransliterations = ConvertFragment.getLatinHiraganaKatakana(mInputQuery);

            String transliterationRelevantForSearch = "";
            mInputQueryTextType = ConvertFragment.getTextType(mInputQuery);
            mInputQueryTransliterationIsInvalid =  transliterationRelevantForSearch.contains("*") || transliterationRelevantForSearch.contains("＊");

            mInputQueryTransliteratedLatinForm = mInputQueryTransliterations.get(0);
            mInputQueryTransliteratedKanaForm = mInputQueryTransliterations.get(1);

            mInputQueryContatenated = Utilities.removeSpecialCharacters(mInputQuery);
            mInputQueryContatenatedLength = mInputQueryContatenated.length();
            mInputQueryTransliteratedLatinFormContatenated = Utilities.removeSpecialCharacters(mInputQueryTransliteratedLatinForm);
            mInputQueryTransliteratedKanaFormContatenated = Utilities.removeSpecialCharacters(mInputQueryTransliteratedKanaForm);
            mInputQueryTransliteratedLatinFormContatenatedLength = mInputQueryTransliteratedLatinFormContatenated.length();
            mInputQueryTransliteratedKanaFormContatenatedLength = mInputQueryTransliteratedKanaFormContatenated.length();
        }
        private void getFamilyConjugationIndexes() {
            for (int rowIndex = 3; rowIndex < mVerbLatinConjDatabase.size(); rowIndex++) {

                if (mVerbLatinConjDatabase.get(rowIndex)[0].equals("") || !mVerbLatinConjDatabase.get(rowIndex)[1].equals("")) continue;

                switch (mVerbLatinConjDatabase.get(rowIndex)[0]) {
                    case "su godan": mFamilyConjugationIndexes.put("su", rowIndex);
                    case "ku godan": mFamilyConjugationIndexes.put("ku", rowIndex);
                    case "gu godan": mFamilyConjugationIndexes.put("gu", rowIndex);
                    case "bu godan": mFamilyConjugationIndexes.put("bu", rowIndex);
                    case "mu godan": mFamilyConjugationIndexes.put("mu", rowIndex);
                    case "nu godan": mFamilyConjugationIndexes.put("nu", rowIndex);
                    case "ru godan": mFamilyConjugationIndexes.put("rug", rowIndex);
                    case "tsu godan": mFamilyConjugationIndexes.put("tsu", rowIndex);
                    case "u godan": mFamilyConjugationIndexes.put("u", rowIndex);
                    case "ru ichidan": mFamilyConjugationIndexes.put("rui", rowIndex);
                    case "desu copula": mFamilyConjugationIndexes.put("da", rowIndex);
                    case "kuru verb": mFamilyConjugationIndexes.put("kuru", rowIndex);
                    case "suru verb": mFamilyConjugationIndexes.put("suru", rowIndex);
                }
            }
        }
        private List<long[]> getMatchingVerbIdsAndCols() {

            if (mInputQueryTextType == GlobalConstants.VALUE_INVALID || mCompleteVerbsList==null) return new ArrayList<>();

            //region Initializations
            int NumberOfSheetCols = mVerbLatinConjDatabase.get(0).length;
            List<Integer> dilutedConjugationColIndexes = new ArrayList<>();
            boolean queryIsContainedInNormalFamilyConjugation = false;
            boolean queryIsContainedInAKuruConjugation = false;
            boolean queryIsContainedInASuruConjugation = false;
            boolean queryIsContainedInADesuConjugation = false;
            boolean queryIsContainedInIruVerbConjugation = false;
            int exceptionIndex;
            String[] currentFamilyConjugations;
            String[] currentConjugations;
            String family;
            String romaji;
            String hiraganaFirstChar;
            String latinRoot;
            String kanjiRoot;
            String conjugationValue;
            boolean foundMatch;
            boolean allowExpandedConjugationsComparison;
            int matchColumn = 0;
            boolean onlyRetrieveShortRomajiVerbs = false;
            //endregion

            //region Removing the "ing" from the query verb (if relevant)
            if (mInputQueryLength > 2 && mInputQuery.substring(mInputQueryLength -3).equals("ing")) {

                if (mInputQueryLength > 5 && mInputQuery.substring(mInputQueryLength -6).equals("inging")) {
                    if (	(mInputQuery.substring(0, 2+1).equals("to ") && IsOfTypeIngIng(mInputQuery.substring(3, mInputQueryLength))) ||
                            (!mInputQuery.substring(0, 2+1).equals("to ") && IsOfTypeIngIng(mInputQuery.substring(0, mInputQueryLength)))   ) {
                        // If the verb ends with "inging" then remove the the second "ing"
                        mInputQuery = mInputQuery.substring(0, mInputQueryLength -3);
                    }
                }
                else {
                    String verb2WithIng = mInputQuery + "ing";
                    if ((verb2WithIng.substring(0, 2 + 1).equals("to ") && IsOfTypeIngIng(verb2WithIng.substring(3, mInputQueryLength + 3))) ||
                            (!verb2WithIng.substring(0, 2 + 1).equals("to ") && IsOfTypeIngIng(verb2WithIng.substring(0, mInputQueryLength + 3)))) {
                    } else {
                        // If the verb does not belong to the list, then remove the ending "ing" so that it can be compared later on to the verbs excel
                        ;//If the verb is for e.g. to sing / sing (verb2 = to singing / singing), then check that verb2 (without the "to ") belongs to the list, and if it does then do nothing

                        mInputQuery = mInputQuery.substring(0, mInputQueryLength -3);
                    }
                }
            }
            //endregion

            //region Taking care of the case where the input is a basic conjugation that will cause the app to return too many verbs
            queryIsContainedInNormalFamilyConjugation = false;
            queryIsContainedInASuruConjugation = false;
            queryIsContainedInAKuruConjugation = false;
            queryIsContainedInADesuConjugation = false;
            queryIsContainedInIruVerbConjugation = false;
            int familyIndex;
            for (String key : mFamilyConjugationIndexes.keySet()) {
                familyIndex = mFamilyConjugationIndexes.get(key);
                switch (key) {
                    case "da":
                        currentFamilyConjugations = mVerbLatinConjDatabase.get(familyIndex);
                        for (int column = 1; column < NumberOfSheetCols; column++) {
                            if (currentFamilyConjugations[column].equals(mInputQueryContatenated)
                                    || currentFamilyConjugations[column].equals(mInputQueryTransliteratedKanaFormContatenated)) {
                                queryIsContainedInADesuConjugation = true;
                            }
                        }
                        if (queryIsContainedInADesuConjugation) break;
                        currentFamilyConjugations = mVerbKanjiConjDatabase.get(familyIndex);
                        for (int column = 1; column < NumberOfSheetCols; column++) {
                            if (currentFamilyConjugations[column].equals(mInputQueryContatenated)) {
                                queryIsContainedInADesuConjugation = true;
                                break;
                            }
                        }
                        break;
                    case "kuru":
                        currentFamilyConjugations = mVerbLatinConjDatabase.get(familyIndex);
                        for (int column = 1; column < NumberOfSheetCols; column++) {
                            if (currentFamilyConjugations[column].equals(mInputQueryContatenated)
                                    || currentFamilyConjugations[column].equals(mInputQueryTransliteratedKanaFormContatenated)) {
                                queryIsContainedInAKuruConjugation = true;
                            }
                        }
                        if (queryIsContainedInAKuruConjugation) break;
                        currentFamilyConjugations = mVerbKanjiConjDatabase.get(familyIndex);
                        for (int column = 1; column < NumberOfSheetCols; column++) {
                            if (currentFamilyConjugations[column].equals(mInputQueryContatenated)) {
                                queryIsContainedInAKuruConjugation = true;
                                break;
                            }
                        }
                        break;
                    case "suru":
                        currentFamilyConjugations = mVerbLatinConjDatabase.get(familyIndex);
                        for (int column = 1; column < NumberOfSheetCols; column++) {
                            if (currentFamilyConjugations[column].equals(mInputQueryContatenated)
                                    || currentFamilyConjugations[column].equals(mInputQueryTransliteratedKanaFormContatenated)) {
                                queryIsContainedInASuruConjugation = true;
                            }
                        }
                        if (queryIsContainedInASuruConjugation) break;
                        currentFamilyConjugations = mVerbKanjiConjDatabase.get(familyIndex);
                        for (int column = 1; column < NumberOfSheetCols; column++) {
                            if (currentFamilyConjugations[column].equals(mInputQueryContatenated)) {
                                queryIsContainedInASuruConjugation = true;
                                break;
                            }
                        }
                        break;
                    default:
                        currentFamilyConjugations = mVerbLatinConjDatabase.get(familyIndex);
                        for (int column = GlobalConstants.VerbModule_colIndex_istem; column < NumberOfSheetCols; column++) {
                            if (currentFamilyConjugations[column].contains(mInputQueryContatenated)
                                    || currentFamilyConjugations[column].contains(mInputQueryTransliteratedKanaFormContatenated)) {
                                queryIsContainedInNormalFamilyConjugation = true;
                                break;
                            }
                        }
                        if (queryIsContainedInNormalFamilyConjugation) break;
                        currentFamilyConjugations = mVerbKanjiConjDatabase.get(familyIndex);
                        for (int column = GlobalConstants.VerbModule_colIndex_istem; column < NumberOfSheetCols; column++) {
                            if (currentFamilyConjugations[column].contains(mInputQueryContatenated)) {
                                queryIsContainedInNormalFamilyConjugation = true;
                                break;
                            }
                        }
                        break;
                }
            }
            if (queryIsContainedInASuruConjugation || queryIsContainedInAKuruConjugation || queryIsContainedInADesuConjugation) queryIsContainedInNormalFamilyConjugation = false;

            if (mInputQueryTextType == GlobalConstants.VALUE_LATIN && mInputQueryContatenated.length() < 4
                    || (mInputQueryTextType == GlobalConstants.VALUE_HIRAGANA || mInputQueryTextType == GlobalConstants.VALUE_KATAKANA)
                    && mInputQueryContatenated.length() < 3) {
                onlyRetrieveShortRomajiVerbs = true;
            }

            //Checking if the query is an iru verb conjugation, which could lead to too may hits
            currentFamilyConjugations = mVerbLatinConjDatabase.get(mFamilyConjugationIndexes.get("rui"));
            String currentConjugation;
            for (int column = GlobalConstants.VerbModule_colIndex_istem; column < NumberOfSheetCols; column++) {
                currentConjugation = "i" + currentFamilyConjugations[column];
                if (currentConjugation.contains(mInputQueryContatenated)
                        || currentConjugation.contains(mInputQueryTransliteratedKanaFormContatenated)) {
                    queryIsContainedInIruVerbConjugation = true;
                    break;
                }
            }
            //endregion

            //region Performing column dilution in order to make the search more efficient (the diluted column ArrayList is used in the Search Algorithm)
            int queryLengthForDilution = 0;
            List<String[]> verbConjugationMaxLengths = new ArrayList<>();
            int conjugationMaxLength;
            if (mInputQueryTextType == GlobalConstants.VALUE_LATIN) {
                verbConjugationMaxLengths = Utilities.readCSVFileFirstRow("LineVerbsLengths - 3000 kanji.csv", getContext());
                queryLengthForDilution = mInputQueryContatenatedLength;
            }
            else if (mInputQueryTextType == GlobalConstants.VALUE_HIRAGANA || mInputQueryTextType == GlobalConstants.VALUE_KATAKANA) {
                verbConjugationMaxLengths = Utilities.readCSVFileFirstRow("LineVerbsLengths - 3000 kanji.csv", getContext());
                queryLengthForDilution = mInputQueryTransliteratedLatinFormContatenatedLength;
            }
            else if (mInputQueryTextType == GlobalConstants.VALUE_KANJI) {
                verbConjugationMaxLengths = Utilities.readCSVFileFirstRow("LineVerbsKanjiLengths - 3000 kanji.csv", getContext());
                queryLengthForDilution = mInputQueryContatenatedLength;
            }

            for (int col = GlobalConstants.VerbModule_colIndex_istem; col < NumberOfSheetCols; col++) {
                if (!verbConjugationMaxLengths.get(0)[col].equals(""))
                    conjugationMaxLength = Integer.parseInt(verbConjugationMaxLengths.get(0)[col]);
                else conjugationMaxLength = 0;

                if (conjugationMaxLength >= queryLengthForDilution) dilutedConjugationColIndexes.add(col);
            }
            //endregion

            //region Getting the matching words from the Words database and filtering for verbs
            List<Word> mMatchingWords;
            if (mWordsFromDictFragment == null) {
                List<Long> mMatchingWordIds = Utilities.getMatchingWordIdsUsingRoomIndexes(mInputQuery, mJapaneseToolboxCentralRoomDatabase);
                mMatchingWords = mJapaneseToolboxCentralRoomDatabase.getWordListByWordIds(mMatchingWordIds);
            }
            else {
                mMatchingWords = mWordsFromDictFragment;
            }
            String type;
            List<long[]> matchingVerbIdsAndColsFromBasicCharacteristics = new ArrayList<>();
            for (Word word : mMatchingWords) {
                type = word.getMeanings().get(0).getType();
                if (type.length() > 0 && type.substring(0,1).equals("V") && !type.equals("VC")) {
                    matchingVerbIdsAndColsFromBasicCharacteristics.add(new long[]{word.getWordId(), 0});
                }
            }
            //endregion

            //region Adding the suru verb if the query is contained in the suru conjugations
            if (queryIsContainedInASuruConjugation) {
                Word suruVerb = mJapaneseToolboxCentralRoomDatabase.getWordsByExactRomajiAndKanjiMatch("suru", "為る").get(0);
                boolean alreadyInList = false;
                for (long[] idAndCol : matchingVerbIdsAndColsFromBasicCharacteristics) {
                    if (idAndCol[0] == suruVerb.getWordId()) alreadyInList = true;
                }
                if (!alreadyInList) matchingVerbIdsAndColsFromBasicCharacteristics.add(new long[]{suruVerb.getWordId(), 0});
            }
            //endregion

            //region Getting the matching verbs according in the expanded conjugations
            List<long[]> matchingVerbIdsAndColsFromExpandedConjugations = new ArrayList<>();
            List<long[]> copyOfMatchingVerbIdsAndColsFromBasicCharacteristics = new ArrayList<>(matchingVerbIdsAndColsFromBasicCharacteristics);
            boolean verbAlreadyFound;
            for (Verb verb : mCompleteVerbsList) {

                //region Skipping verbs that were already found
                verbAlreadyFound = false;
                for (long[] idAndCol : copyOfMatchingVerbIdsAndColsFromBasicCharacteristics) {
                    if (idAndCol[0] == verb.getVerbId()) {
                        copyOfMatchingVerbIdsAndColsFromBasicCharacteristics.remove(idAndCol);
                        verbAlreadyFound = true;
                        break;
                    }
                }
                if (verbAlreadyFound) continue;
                //endregion

                //region Loop starting parameters initialization
                foundMatch = false;
                allowExpandedConjugationsComparison = true;
                //endregion

                //region Getting the verb characteristics
                family = verb.getFamily();
                romaji = verb.getRomaji();
                hiraganaFirstChar = verb.getKana();
                latinRoot = verb.getLatinRoot();
                kanjiRoot = verb.getKanjiRoot();
                exceptionIndex = (verb.getExceptionIndex().equals(""))? 0 : Integer.valueOf(verb.getExceptionIndex());
                //endregion

                //region Only allowing searches on verbs that satisfy the following conditions (including identical 1st char, kuru/suru/da, query length)
                if (    !(     (mInputQueryTextType == GlobalConstants.VALUE_LATIN && (romaji.charAt(0) == mInputQueryContatenated.charAt(0)))
                            || ((mInputQueryTextType == GlobalConstants.VALUE_HIRAGANA || mInputQueryTextType == GlobalConstants.VALUE_KATAKANA)
                                && (hiraganaFirstChar.charAt(0) == mInputQueryTransliteratedKanaForm.charAt(0)))
                            || (mInputQueryTextType == GlobalConstants.VALUE_KANJI && kanjiRoot.contains(mInputQueryContatenated.substring(0,1)))
                            || romaji.contains("kuru")
                            || romaji.equals("suru")
                            || romaji.equals("da") )
                        || (mInputQueryTextType == GlobalConstants.VALUE_LATIN && mInputQueryContatenated.length() < 4 && !romaji.contains(mInputQueryContatenated))
                        || ((mInputQueryTextType == GlobalConstants.VALUE_HIRAGANA || mInputQueryTextType == GlobalConstants.VALUE_KATAKANA)
                            && mInputQueryContatenated.length() < 3 && !romaji.contains(mInputQueryTransliteratedLatinFormContatenated))
                        || (mInputQueryTextType == GlobalConstants.VALUE_KANJI && mInputQueryContatenated.length() < 3 && !mInputQueryContatenated.contains(kanjiRoot))
                        || (onlyRetrieveShortRomajiVerbs && romaji.length() > 4)     ) {
                    continue;
                }
                //endregion

                //region If the verb is equal to a family conjugation, only roots with length 1 (ie. iru/aru/eru/oru/uru verbs only) or a verb with the exact romaji value are considered. This prevents too many results. This does not conflict with da or kuru.
                if (        queryIsContainedInNormalFamilyConjugation
                            && latinRoot.length() > 1
                        ||  queryIsContainedInASuruConjugation
                            && !(kanjiRoot.equals("為"))
                        ||  queryIsContainedInAKuruConjugation
                            && !romaji.contains("kuru")
                        ||  queryIsContainedInADesuConjugation
                            && !romaji.equals("da")
                        || queryIsContainedInIruVerbConjugation
                            && !romaji.equals("iru")) {

                    //If the input is suru then prevent verbs with suru in the conjugations from giving a hit, but allow other verbs with romaji suru to give a hit
                    if (romaji.contains(" suru")) {
                        allowExpandedConjugationsComparison = false;
                    }
                    //Otherwise, if the verb does not meet the above conditions, skip this verb
                    else continue;
                }
                //endregion

                //region Main Comparator Algorithm
                if (allowExpandedConjugationsComparison) {

                    //region Latin conjugations comparison
                    if (mInputQueryTextType == GlobalConstants.VALUE_LATIN) {
                        if (!mFamilyConjugationIndexes.containsKey(family)) continue;

                        currentConjugations = Arrays.copyOf(mVerbLatinConjDatabase.get(exceptionIndex), NumberOfSheetCols);
                        if (exceptionIndex != mFamilyConjugationIndexes.get(family)) {
                            currentFamilyConjugations = Arrays.copyOf(mVerbLatinConjDatabase.get(mFamilyConjugationIndexes.get(family)), NumberOfSheetCols);
                            for (int col : dilutedConjugationColIndexes) {

                                if (currentConjugations[col].equals(""))
                                    conjugationValue = latinRoot + currentFamilyConjugations[col].replace(" ", "");
                                else conjugationValue = currentConjugations[col].replace(" ", "");

                                if (conjugationValue.contains(mInputQueryContatenated)) {
                                    foundMatch = true;
                                    matchColumn = col;
                                    break;
                                }
                            }
                        }
                        else {
                            for (int col : dilutedConjugationColIndexes) {
                                conjugationValue = latinRoot + currentConjugations[col].replace(" ", "");

                                if (conjugationValue.contains(mInputQueryContatenated)) {
                                    foundMatch = true;
                                    matchColumn = col;
                                    break;
                                }
                            }
                        }
                    }
                    //endregion

                    //region Kana conjugations comparison
                    else if ((mInputQueryTextType == GlobalConstants.VALUE_HIRAGANA || mInputQueryTextType == GlobalConstants.VALUE_KATAKANA) && !mInputQueryTransliterationIsInvalid) {
                        if (!mFamilyConjugationIndexes.containsKey(family)) continue;

                        currentConjugations = Arrays.copyOf(mVerbLatinConjDatabase.get(exceptionIndex), NumberOfSheetCols);
                        if (exceptionIndex != mFamilyConjugationIndexes.get(family)) {
                            currentFamilyConjugations = Arrays.copyOf(mVerbLatinConjDatabase.get(mFamilyConjugationIndexes.get(family)), NumberOfSheetCols);
                            for (int col : dilutedConjugationColIndexes) {

                                if (currentConjugations[col].equals(""))
                                    conjugationValue = latinRoot + currentFamilyConjugations[col].replace(" ", "");
                                else conjugationValue = currentConjugations[col].replace(" ", "");

                                if (conjugationValue.contains(mInputQueryTransliteratedLatinFormContatenated)) {
                                    foundMatch = true;
                                    matchColumn = col;
                                    break;
                                }
                            }
                        }
                        else {
                            for (int col : dilutedConjugationColIndexes) {
                                conjugationValue = latinRoot + currentConjugations[col].replace(" ", "");

                                if (conjugationValue.contains(mInputQueryTransliteratedLatinFormContatenated)) {
                                    foundMatch = true;
                                    matchColumn = col;
                                    break;
                                }
                            }
                        }
                    }
                    //endregion

                    //region Kanji conjugations comparison
                    else if (mInputQueryTextType == GlobalConstants.VALUE_KANJI) {
                        if (!mFamilyConjugationIndexes.containsKey(family)) continue;

                        currentConjugations = Arrays.copyOf(mVerbKanjiConjDatabase.get(exceptionIndex), NumberOfSheetCols);
                        if (exceptionIndex != mFamilyConjugationIndexes.get(family)) {
                            currentFamilyConjugations = Arrays.copyOf(mVerbKanjiConjDatabase.get(mFamilyConjugationIndexes.get(family)), NumberOfSheetCols);
                            for (int col : dilutedConjugationColIndexes) {

                                if (currentConjugations[col].equals(""))
                                    conjugationValue = kanjiRoot + currentFamilyConjugations[col].replace(" ", "");
                                else conjugationValue = currentConjugations[col].replace(" ", "");

                                if (conjugationValue.contains(mInputQuery)) {
                                    foundMatch = true;
                                    matchColumn = col;
                                    break;
                                }
                            }
                        }
                        else {
                            for (int col : dilutedConjugationColIndexes) {
                                conjugationValue = kanjiRoot + currentConjugations[col].replace(" ", "");

                                if (conjugationValue.contains(mInputQuery)) {
                                    foundMatch = true;
                                    matchColumn = col;
                                    break;
                                }
                            }
                        }
                    }
                    //endregion
                }
                //endregion

                if (foundMatch) {
                    matchingVerbIdsAndColsFromExpandedConjugations.add(new long[]{verb.getVerbId(), matchColumn});
                }
            }
            //endregion

            List<long[]> matchingVerbIdsAndCols = new ArrayList<>();
            matchingVerbIdsAndCols.addAll(matchingVerbIdsAndColsFromBasicCharacteristics);
            matchingVerbIdsAndCols.addAll(matchingVerbIdsAndColsFromExpandedConjugations);

            return matchingVerbIdsAndCols;
        }
        private List<long[]> sortMatchingVerbsList(List<long[]> ConjugationSearchMatchingVerbRowColIndexList) {

            List<long[]> matchingVerbIndexesLengthsAndCols = new ArrayList<>();

            //region Registering if the input query is a "to " verb
            boolean queryIsVerbWithTo = false;
            String queryWordWithoutTo = "";
            if (mInputQuery.length()>3 && mInputQuery.substring(0,3).equals("to ")) {
                queryIsVerbWithTo = true;
                queryWordWithoutTo = mInputQuery.substring(3, mInputQuery.length());
            }
            //endregion

            //region Replacing the Kana input word by its romaji equivalent
            String inputQuery = mInputQuery;
            int textType = ConvertFragment.getTextType(inputQuery);
            if (textType == GlobalConstants.VALUE_HIRAGANA || textType == GlobalConstants.VALUE_KATAKANA) {
                List<String> translationList = ConvertFragment.getLatinHiraganaKatakana(inputQuery.replace(" ", ""));
                inputQuery = translationList.get(0);
            }
            //endregion

            for (int i = 0; i < ConjugationSearchMatchingVerbRowColIndexList.size(); i++) {

                Word currentWord = mJapaneseToolboxCentralRoomDatabase.getWordByWordId(ConjugationSearchMatchingVerbRowColIndexList.get(i)[0]);
                if (currentWord==null) continue;

                int length = Utilities.getLengthFromWordAttributes(currentWord, inputQuery, queryWordWithoutTo, queryIsVerbWithTo);

                long[] currentMatchingWordIndexLengthAndCol = new long[3];
                currentMatchingWordIndexLengthAndCol[0] = i;
                currentMatchingWordIndexLengthAndCol[1] = length;
                currentMatchingWordIndexLengthAndCol[2] = ConjugationSearchMatchingVerbRowColIndexList.get(i)[1];

                matchingVerbIndexesLengthsAndCols.add(currentMatchingWordIndexLengthAndCol);
            }

            //Sort the results according to total length
            if (matchingVerbIndexesLengthsAndCols.size() != 0) {
                matchingVerbIndexesLengthsAndCols = Utilities.bubbleSortForThreeIntegerList(matchingVerbIndexesLengthsAndCols);
            }

            //Return the sorted list
            List<long[]> sortedList = new ArrayList<>();
            for (int i = 0; i < matchingVerbIndexesLengthsAndCols.size(); i++) {
                long sortedIndex = matchingVerbIndexesLengthsAndCols.get(i)[0];
                sortedList.add(ConjugationSearchMatchingVerbRowColIndexList.get((int) sortedIndex));
            }

            return sortedList;

        }
        private List<Verb> getVerbs(List<long[]> mMatchingVerbIdsAndCols) {

            if (mMatchingVerbIdsAndCols.size() == 0) return new ArrayList<>();

            //region Initializations
            List<Verb> verbs = new ArrayList<>();
            Word currentWord;
            Verb currentVerb;
            long matchingVerbId;
            int conjLength;
            int NumberOfSheetCols = mVerbLatinConjDatabase.get(0).length;
            String[] currentConjugationsRowLatin;
            String[] currentConjugationsRowKanji;
            List<Verb.ConjugationCategory> conjugationCategories;
            Verb.ConjugationCategory conjugationCategory;
            List<Verb.ConjugationCategory.Conjugation> conjugations;
            Verb.ConjugationCategory.Conjugation conjugation;
            List<String> conjugationSetLatin;
            List<String> conjugationSetKanji;
            List<ConjugationTitle.Subtitle> subtitles;
            int currentFamilyConjugationsIndex;
            String[] currentConjugationExceptionsRowLatin;
            String[] currentConjugationExceptionsRowKanji;
            //endregion

            //region Finding the Passive tense index in order to remove passive conjugations from verbs that are intransitive
            String currentTitle;
            int passiveTenseCategoryIndex = 0;
            for (int i = 0; i < mConjugationTitles.size(); i++) {
                currentTitle = mConjugationTitles.get(i).getTitle();
                if (currentTitle.contains("Passive (X is done to him)")) { passiveTenseCategoryIndex = i; }
            }
            //endregion

            //region Updating the verbs with their conjugations
            for (int p = 0; p < mMatchingVerbIdsAndCols.size(); p++) {
                matchingVerbId = mMatchingVerbIdsAndCols.get(p)[0];
                currentVerb = mJapaneseToolboxCentralRoomDatabase.getVerbByVerbId(matchingVerbId);
                currentWord = mJapaneseToolboxCentralRoomDatabase.getWordByWordId(matchingVerbId);
                currentFamilyConjugationsIndex = mFamilyConjugationIndexes.get(currentVerb.getFamily());
                currentConjugationsRowLatin = Arrays.copyOf(mVerbLatinConjDatabase.get(currentFamilyConjugationsIndex), NumberOfSheetCols);
                currentConjugationsRowKanji = Arrays.copyOf(mVerbKanjiConjDatabase.get(currentFamilyConjugationsIndex), NumberOfSheetCols);

                //region Setting the verb's basic characteristics for display
                currentVerb.setKanji(currentWord.getKanji());
                currentVerb.setAltSpellings(currentWord.getAltSpellings());

                StringBuilder stringBuilder = new StringBuilder();
                for (int i=0; i< currentWord.getMeanings().size(); i++) {
                    if (i != 0) stringBuilder.append(", ");
                    stringBuilder.append(currentWord.getMeanings().get(i).getMeaning());
                }
                currentVerb.setMeaning(stringBuilder.toString());

                switch (currentVerb.getTrans()) {
                    case "T": currentVerb.setTrans("trans."); break;
                    case "I": currentVerb.setTrans("intrans."); break;
                    case "T/I": currentVerb.setTrans("trans./intrans."); break;
                }

                switch (currentVerb.getFamily()) {
                    case "su": currentVerb.setFamily("su godan"); break;
                    case "ku": currentVerb.setFamily("ku godan"); break;
                    case "gu": currentVerb.setFamily("gu godan"); break;
                    case "bu": currentVerb.setFamily("bu godan"); break;
                    case "mu": currentVerb.setFamily("mu godan"); break;
                    case "nu": currentVerb.setFamily("nu godan"); break;
                    case "rug": currentVerb.setFamily("ru godan"); break;
                    case "tsu": currentVerb.setFamily("tsu godan"); break;
                    case "u": currentVerb.setFamily("u godan"); break;
                    case "rui": currentVerb.setFamily("ru ichidan"); break;
                    case "da": currentVerb.setFamily("desu copula"); break;
                    case "kuru": currentVerb.setFamily("kuru verb"); break;
                    case "suru": currentVerb.setFamily("suru verb"); break;
                }
                //endregion

                //region Getting the conjugations row
                currentConjugationExceptionsRowLatin = new String[NumberOfSheetCols];
                currentConjugationExceptionsRowKanji = new String[NumberOfSheetCols];
                int indexOfExceptionConjugations = Integer.valueOf(currentVerb.getExceptionIndex());

                if (indexOfExceptionConjugations != currentFamilyConjugationsIndex) {
                    currentConjugationExceptionsRowLatin = Arrays.copyOf(mVerbLatinConjDatabase.get(indexOfExceptionConjugations), NumberOfSheetCols);
                    currentConjugationExceptionsRowKanji = Arrays.copyOf(mVerbKanjiConjDatabase.get(indexOfExceptionConjugations), NumberOfSheetCols);
                }
                else {
                    Arrays.fill(currentConjugationExceptionsRowLatin, "");
                    Arrays.fill(currentConjugationExceptionsRowKanji, "");
                }

                for (int col = GlobalConstants.VerbModule_colIndex_istem; col < NumberOfSheetCols; col++) {

                    if (!currentConjugationExceptionsRowLatin[col].equals("")) currentConjugationsRowLatin[col] = currentConjugationExceptionsRowLatin[col];
                    else {
                        conjLength = currentConjugationsRowLatin[col].length();
                        if (conjLength > 3 && currentConjugationsRowLatin[col].substring(0, 3).equals("(o)")) {
                            currentConjugationsRowLatin[col] = "(o)" + currentVerb.getLatinRoot() + currentConjugationsRowLatin[col].substring(3, conjLength);
                        } else {
                            currentConjugationsRowLatin[col] = currentVerb.getLatinRoot() + currentConjugationsRowLatin[col];
                        }
                    }

                    if (!currentConjugationExceptionsRowKanji[col].equals("")) currentConjugationsRowKanji[col] = currentConjugationExceptionsRowKanji[col];
                    else {
                        conjLength = currentConjugationsRowKanji[col].length();
                        if (conjLength > 3 && currentConjugationsRowKanji[col].substring(0, 3).equals("(お)")) {
                            currentConjugationsRowKanji[col] = "(お)" + currentVerb.getKanjiRoot() + currentConjugationsRowKanji[col].substring(3, conjLength);
                        } else {
                            currentConjugationsRowKanji[col] = currentVerb.getKanjiRoot() + currentConjugationsRowKanji[col];
                        }
                    }
                }
                //endregion

                //region Getting the verb conjugations and putting each conjugation of the conjugations row into its appropriate category
                conjugationCategories = new ArrayList<>();
                for (int categoryIndex = 1; categoryIndex < mConjugationTitles.size(); categoryIndex++) {

                    //region Getting the set of Latin and Kanji conjugations according to the current category's subtitle column indexes
                    subtitles = mConjugationTitles.get(categoryIndex).getSubtitles();
                    int subtitleColIndex;
                    conjugationSetLatin = new ArrayList<>();
                    conjugationSetKanji = new ArrayList<>();
                    for (int i=0; i<subtitles.size(); i++) {
                        subtitleColIndex = subtitles.get(i).getSubtitleIndex();
                        conjugationSetLatin.add(currentConjugationsRowLatin[subtitleColIndex]);
                        conjugationSetKanji.add(currentConjugationsRowKanji[subtitleColIndex]);
                    }
                    //endregion

                    //region Intransitive verbs don't have a passive tense, so the relevant entries are removed
                    if (!currentVerb.getTrans().equals("T") && categoryIndex == passiveTenseCategoryIndex) {
                        for (int conjugationIndex = 0; conjugationIndex < conjugationSetLatin.size(); conjugationIndex++) {
                            conjugationSetLatin.set(conjugationIndex, "*");
                            conjugationSetKanji.set(conjugationIndex, "*");
                        }
                    }
                    //endregion

                    //region Cleaning the entries that contain exceptions
                    for (int conjugationIndex = 0; conjugationIndex < conjugationSetLatin.size(); conjugationIndex++) {
                        if (conjugationSetLatin.get(conjugationIndex).contains("*"))
                            conjugationSetLatin.set(conjugationIndex, "*");
                        if (conjugationSetKanji.get(conjugationIndex).contains("*"))
                            conjugationSetKanji.set(conjugationIndex, "*");
                    }
                    //endregion

                    //region Adding the conjugations to the conjugationCategory
                    conjugationCategory = new Verb.ConjugationCategory();
                    conjugations = new ArrayList<>();
                    for (int conjugationIndex = 0; conjugationIndex < conjugationSetLatin.size(); conjugationIndex++) {
                        conjugation = new Verb.ConjugationCategory.Conjugation();
                        conjugation.setConjugationLatin(conjugationSetLatin.get(conjugationIndex));
                        conjugation.setConjugationKanji(conjugationSetKanji.get(conjugationIndex));
                        conjugations.add(conjugation);
                    }
                    conjugationCategory.setConjugations(conjugations);
                    //endregion

                    conjugationCategories.add(conjugationCategory);
                }
                //endregion

                currentVerb.setConjugationCategories(conjugationCategories);

                verbs.add(currentVerb);
            }
            //endregion

            return verbs;
        }
        private Boolean IsOfTypeIngIng(String verb) {
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
    }

}