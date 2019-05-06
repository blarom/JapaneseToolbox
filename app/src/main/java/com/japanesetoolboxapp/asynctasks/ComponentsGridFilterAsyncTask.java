package com.japanesetoolboxapp.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.japanesetoolboxapp.data.JapaneseToolboxKanjiRoomDatabase;
import com.japanesetoolboxapp.data.KanjiCharacter;
import com.japanesetoolboxapp.resources.GlobalConstants;
import com.japanesetoolboxapp.resources.LocaleHelper;
import com.japanesetoolboxapp.resources.Utilities;
import com.japanesetoolboxapp.ui.ConvertFragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ComponentsGridFilterAsyncTask extends AsyncTask<Void, Void, List<String>> {

    //region Parameters
    private final String mComponentSelectionType;
    private final List<String[]> mRadicalsOnlyDatabase;
    private final String mKanjiCharacterNameForFilter;
    private List<String> mDisplayableComponentSelections;
    private JapaneseToolboxKanjiRoomDatabase mJapaneseToolboxKanjiRoomDatabase;
    //endregion
    private WeakReference<Context> contextRef;
    public ComponentsGridFilterAsyncResponseHandler listener;

    public ComponentsGridFilterAsyncTask(Context context,
                                         String mComponentSelectionType,
                                         List<String[]> mRadicalsOnlyDatabase,
                                         String mKanjiCharacterNameForFilter,
                                         List<String> mDisplayableComponentSelections, ComponentsGridFilterAsyncResponseHandler listener) {
        contextRef = new WeakReference<>(context);
        this.mComponentSelectionType = mComponentSelectionType;
        this.mRadicalsOnlyDatabase = mRadicalsOnlyDatabase;
        this.mKanjiCharacterNameForFilter = mKanjiCharacterNameForFilter;
        this.mDisplayableComponentSelections = mDisplayableComponentSelections;
        this.listener = listener;
    }

    protected void onPreExecute() {
        super.onPreExecute();
    }
    @Override
    protected List<String> doInBackground(Void... voids) {

        mJapaneseToolboxKanjiRoomDatabase = JapaneseToolboxKanjiRoomDatabase.getInstance(contextRef.get());
        mDisplayableComponentSelections = filterGridElementsAccordingToDescriptor(mDisplayableComponentSelections);

        return mDisplayableComponentSelections;
    }

    @Override
    protected void onPostExecute(List<String> words) {
        super.onPostExecute(words);
        listener.onComponentsGridFilterAsyncTaskDone(words);
    }

    public interface ComponentsGridFilterAsyncResponseHandler {
        void onComponentsGridFilterAsyncTaskDone(List<String> text);
    }

    private List<String> filterGridElementsAccordingToDescriptor(List<String> displayableComponentSelections) {

        if (displayableComponentSelections ==null) return new ArrayList<>();
        else if (TextUtils.isEmpty(mKanjiCharacterNameForFilter)) return displayableComponentSelections;

        String language = LocaleHelper.getLanguage(contextRef.get());

        List<String> intersectionWithMatchingDescriptors;
        if (mComponentSelectionType.contains("radical")) {
            List<String> matchingRadicals = new ArrayList<>();
            String radical;
            String radicalNumber;
            String radicalNumberFirstElement;
            String radicalName;
            String numberStrokes;
            String matchingRadicalNumber = "";
            for (int i = 0; i < mRadicalsOnlyDatabase.size(); i++) {

                radical = mRadicalsOnlyDatabase.get(i)[GlobalConstants.RADICAL_KANA];
                radicalNumber = mRadicalsOnlyDatabase.get(i)[GlobalConstants.RADICAL_NUM];
                radicalNumberFirstElement = radicalNumber.split(";")[0];
                radicalName = "";
                switch (language) {
                    case "en": radicalName = mRadicalsOnlyDatabase.get(i)[GlobalConstants.RADICAL_NAME_EN]; break;
                    case "fr": radicalName = mRadicalsOnlyDatabase.get(i)[GlobalConstants.RADICAL_NAME_FR]; break;
                    case "es": radicalName = mRadicalsOnlyDatabase.get(i)[GlobalConstants.RADICAL_NAME_ES]; break;
                }
                numberStrokes = mRadicalsOnlyDatabase.get(i)[GlobalConstants.RADICAL_NUM_STROKES];

                if (radical.equals(mKanjiCharacterNameForFilter)
                        || radicalNumberFirstElement.equals(mKanjiCharacterNameForFilter)
                        || radicalName.contains(mKanjiCharacterNameForFilter)
                        || numberStrokes.equals(mKanjiCharacterNameForFilter)) {
                    matchingRadicals.add(mRadicalsOnlyDatabase.get(i)[GlobalConstants.RADICAL_KANA]);
                    matchingRadicalNumber = radicalNumber;
                }
                if (radicalName.equals("") && !matchingRadicalNumber.equals("") && radicalNumberFirstElement.equals(matchingRadicalNumber)) {
                    matchingRadicals.add(mRadicalsOnlyDatabase.get(i)[GlobalConstants.RADICAL_KANA] + "variant");
                }
            }

            intersectionWithMatchingDescriptors = Utilities.getIntersectionOfLists(displayableComponentSelections, matchingRadicals);
        }
        else {
            List<KanjiCharacter> matchingKanjiCharactersByDescriptor = mJapaneseToolboxKanjiRoomDatabase.getKanjiCharactersByDescriptor(mKanjiCharacterNameForFilter);

            List<KanjiCharacter> matchingKanjiCharactersByMeaning = new ArrayList<>();
            switch (LocaleHelper.getLanguage(contextRef.get())) {
                case "en":
                    matchingKanjiCharactersByMeaning = mJapaneseToolboxKanjiRoomDatabase.getKanjiCharactersByMeaningEN(mKanjiCharacterNameForFilter);
                    break;
                case "fr":
                    matchingKanjiCharactersByMeaning = mJapaneseToolboxKanjiRoomDatabase.getKanjiCharactersByMeaningFR(mKanjiCharacterNameForFilter);
                    break;
                case "es":
                    matchingKanjiCharactersByMeaning = mJapaneseToolboxKanjiRoomDatabase.getKanjiCharactersByMeaningES(mKanjiCharacterNameForFilter);
                    break;
            }
            matchingKanjiCharactersByDescriptor.addAll(matchingKanjiCharactersByMeaning);

            String hiraganaDescriptor = ConvertFragment.getLatinHiraganaKatakana(mKanjiCharacterNameForFilter).get(GlobalConstants.TYPE_HIRAGANA);
            matchingKanjiCharactersByDescriptor.addAll(mJapaneseToolboxKanjiRoomDatabase.getKanjiCharactersByKanaDescriptor(hiraganaDescriptor));

            String katakanaDescriptor = ConvertFragment.getLatinHiraganaKatakana(mKanjiCharacterNameForFilter).get(GlobalConstants.TYPE_KATAKANA);
            matchingKanjiCharactersByDescriptor.addAll(mJapaneseToolboxKanjiRoomDatabase.getKanjiCharactersByKanaDescriptor(katakanaDescriptor));

            List<String> matchingCharacters = new ArrayList<>();
            for (KanjiCharacter kanjiCharacter : matchingKanjiCharactersByDescriptor) {
                matchingCharacters.add(Utilities.convertFromUTF8Index(kanjiCharacter.getHexIdentifier()));
            }

            intersectionWithMatchingDescriptors = Utilities.getIntersectionOfLists(displayableComponentSelections, matchingCharacters);
        }

        return intersectionWithMatchingDescriptors;

    }
}
