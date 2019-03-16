package com.japanesetoolboxapp.adapters;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.japanesetoolboxapp.R;
import com.japanesetoolboxapp.data.Word;
import com.japanesetoolboxapp.resources.GlobalConstants;
import com.japanesetoolboxapp.resources.Utilities;
import com.japanesetoolboxapp.ui.ConvertFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DictionaryRecyclerViewAdapter extends RecyclerView.Adapter<DictionaryRecyclerViewAdapter.DictItemViewHolder> {

    private static final String RULE_DELIMITER = "@";
    private final Context mContext;
    private final HashMap<String, String> mLegendDatabase;
    private final String mInputQuery;
    private final int mInputQueryTextType;
    private final String mInputQueryFirstLetter;
    private boolean[] mActiveMeaningLanguages;
    private boolean[] mChildIsVisible;
    private List<Word> mWordsList;
    final private DictionaryItemClickHandler mOnItemClickHandler;
    private final Typeface mDroidSansJapaneseTypeface;
    private List<String> listRomajiAndKanji;
    private List<String> listSourceInfo;
    private List<String> listMeaningExtract;
    private List<Boolean> listTypeIsVerb;
    private final LinearLayout.LayoutParams mChildLineParams;
    private final LinearLayout.LayoutParams mubChildLineParams;
    private boolean mShowSources = false;
    private String mUILanguage;

    public DictionaryRecyclerViewAdapter(Context context, DictionaryItemClickHandler listener , List<Word> wordsList, HashMap<String, String> legendDatabase, String inputQuery) {
        this.mContext = context;
        this.mWordsList = wordsList;
        this.mLegendDatabase = legendDatabase;
        this.mOnItemClickHandler = listener;
        this.mInputQuery = inputQuery;
        createVisibilityArray();

        mInputQueryTextType = ConvertFragment.getTextType(mInputQuery);
        mInputQueryFirstLetter = (mInputQuery.length()>0) ? mInputQuery.substring(0,1) : "";

        AssetManager am = mContext.getApplicationContext().getAssets();
        mDroidSansJapaneseTypeface = Typeface.createFromAsset(am, String.format(Locale.JAPAN, "fonts/%s", "DroidSansJapanese.ttf"));

        mChildLineParams = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT, 2 );
        mChildLineParams.setMargins(0, 16, 0, 16);
        mubChildLineParams = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT, 2 );
        mubChildLineParams.setMargins(128, 16, 128, 4);

        mUILanguage = "EN";
        mActiveMeaningLanguages = new boolean[]{true, false, false};

        prepareLayoutTexts();
    }

    @NonNull @Override public DictItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.list_item_dictonary, parent, false);
        view.setFocusable(true);
        return new DictItemViewHolder(view);
    }
    @Override public void onBindViewHolder(@NonNull final DictItemViewHolder holder, int position) {

        //region Setting behavior when element is clicked
        if (mChildIsVisible[position]) {
            holder.childLinearLayout.setVisibility(View.VISIBLE);
            holder.meaningsTextView.setVisibility(View.GONE);
            holder.sourceInfoTextView.setVisibility(View.GONE);
            holder.parentContainer.setBackgroundColor(mContext.getResources().getColor(R.color.colorSelectedDictResultBackground));
        }
        else {
            holder.childLinearLayout.setVisibility(View.GONE);
            holder.meaningsTextView.setVisibility(View.VISIBLE);
            if (!TextUtils.isEmpty(listSourceInfo.get(position))) holder.sourceInfoTextView.setVisibility(View.VISIBLE);
            holder.parentContainer.setBackgroundColor(Color.TRANSPARENT);
        }

        holder.romajiAndKanjiTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (holder.childLinearLayout.getVisibility() == View.VISIBLE) {
                    holder.childLinearLayout.setVisibility(View.GONE);
                    holder.meaningsTextView.setVisibility(View.VISIBLE);
                    holder.parentContainer.setBackgroundColor(Color.TRANSPARENT);
                    holder.dropdownArrowImageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_arrow_drop_down_black_24dp));
                    mChildIsVisible[holder.getAdapterPosition()] = false;
                }
                else {
                    holder.childLinearLayout.setVisibility(View.VISIBLE);
                    holder.meaningsTextView.setVisibility(View.GONE);
                    holder.parentContainer.setBackgroundColor(mContext.getResources().getColor(R.color.colorSelectedDictResultBackground));
                    holder.dropdownArrowImageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_arrow_drop_up_black_24dp));
                    mChildIsVisible[holder.getAdapterPosition()] = true;
                }
            }
        });

        if (TextUtils.isEmpty(listSourceInfo.get(position))) holder.sourceInfoTextView.setVisibility(View.GONE);
        else {
            holder.sourceInfoTextView.setVisibility(View.VISIBLE);
            holder.sourceInfoTextView.setText(listSourceInfo.get(position));
        }
        //endregion

        //region Updating the parent values
        String romaji = mWordsList.get(position).getRomaji();
        String kanji = mWordsList.get(position).getKanji();
        String alternatespellings = mWordsList.get(position).getAltSpellings();

        holder.romajiAndKanjiTextView.setText(listRomajiAndKanji.get(position));
        holder.romajiAndKanjiTextView.setTypeface(mDroidSansJapaneseTypeface, Typeface.BOLD);
        holder.romajiAndKanjiTextView.setPadding(0,16,0,4);

        if (romaji.equals("") && kanji.equals("")) { holder.romajiAndKanjiTextView.setVisibility(View.GONE); }
        else { holder.romajiAndKanjiTextView.setVisibility(View.VISIBLE); }

        holder.meaningsTextView.setText(listMeaningExtract.get(position));
        //endregion

        //region Updating the child values

        //region Initialization
        int startIndex;
        int endIndex = 0;
        holder.childElementsLinearLayout.removeAllViews();
        holder.childElementsLinearLayout.setFocusable(false);
        //endregion

        //region Showing the romaji and kanji values for user click
        if (listTypeIsVerb.get(position) && mWordsList.get(position).getIsLocal()) {
            holder.romajiChildTextView.setVisibility(View.VISIBLE);
            holder.kanjiChildTextView.setVisibility(View.VISIBLE);
        }
        else {
            holder.romajiChildTextView.setVisibility(View.GONE);
            holder.kanjiChildTextView.setVisibility(View.GONE);
        }

        if (romaji.length() > 0 && kanji.length() > 0) {
            if (listTypeIsVerb.get(position)) {
                setHyperlinksInCopyToInputLine("verb", holder.romajiChildTextView, "Conjugate ", romaji, " ");
                setHyperlinksInCopyToInputLine("verb", holder.kanjiChildTextView, "(", kanji, ").");
            } else {
                setHyperlinksInCopyToInputLine("word", holder.romajiChildTextView, "Copy ", romaji, " ");
                setHyperlinksInCopyToInputLine("word", holder.kanjiChildTextView, "(", kanji, ") to input.");
            }
        } else if (romaji.length() == 0) {
            setHyperlinksInCopyToInputLine("word", holder.romajiChildTextView, "Copy ", kanji, " to input.");
        } else {
            setHyperlinksInCopyToInputLine("word", holder.romajiChildTextView, "Copy ", romaji, " to input.");
        }
        //endregion

        //region Setting the alternate spellings
        if (!TextUtils.isEmpty(alternatespellings)) {
            String htmlText = "<font face='serif' color='" +
                    mContext.getResources().getColor(R.color.textColorDictionaryAlternateSpellings) +
                    "'>" + "<b>" + "Alternate forms: " + "</b>" + alternatespellings + "</font>";
            Spanned spanned_alternatespellings = Utilities.fromHtml(htmlText);
            TextView tv_alternatespellings = new TextView(mContext);
            tv_alternatespellings.setText(spanned_alternatespellings);
            tv_alternatespellings.setTextSize(14);
            tv_alternatespellings.setTextIsSelectable(true);
            tv_alternatespellings.setClickable(true);
            tv_alternatespellings.setTypeface(mDroidSansJapaneseTypeface, Typeface.BOLD);
            tv_alternatespellings.setPadding(0,4,0,0);
            holder.childElementsLinearLayout.addView(tv_alternatespellings);
        }
        //endregion

        //region Setting the wordMeaning elements
        if (mActiveMeaningLanguages[GlobalConstants.LANG_EN]) setMeaningsLayout(position, holder, GlobalConstants.LANG_EN);
        if (mActiveMeaningLanguages[GlobalConstants.LANG_FR]) setMeaningsLayout(position, holder, GlobalConstants.LANG_FR);
        if (mActiveMeaningLanguages[GlobalConstants.LANG_ES]) setMeaningsLayout(position, holder, GlobalConstants.LANG_ES);
        //endregion

        //endregion

    }

    private void prepareLayoutTexts() {

        listRomajiAndKanji = new ArrayList<>();
        listSourceInfo = new ArrayList<>();
        listMeaningExtract = new ArrayList<>();
        listTypeIsVerb = new ArrayList<>();

        if (mWordsList == null) return;

        for (Word word : mWordsList) {

            //region Getting the word characteristics
            String romaji = word.getRomaji();
            String kanji = word.getKanji();
            String alternatespellings = word.getAltSpellings();
            String keywords = word.getExtraKeywordsEN();
            String type = "";
            List<Word.Meaning> meanings = new ArrayList<>();
            switch (mUILanguage) {
                case "EN":
                    meanings = word.getMeaningsEN();
                    break;
                case "FR":
                    meanings = word.getMeaningsFR();
                    break;
                case "ES":
                    meanings = word.getMeaningsES();
                    break;
            }

            listMeaningExtract.add(Utilities.removeDuplicatesFromCommaList(Utilities.getMeaningsExtract(meanings, 4)));

            StringBuilder cumulative_meaning_value = new StringBuilder();
            boolean wordHasPhraseConstruction = false;
            boolean typeIsVerbConjugation = false;
            boolean typeIsiAdjectiveConjugation = false;
            boolean typeIsnaAdjectiveConjugation = false;
            boolean typeIsVerb = false;
            boolean typeIsAdverb = false;
            for (int j = 0; j< meanings.size(); j++) {
                cumulative_meaning_value.append(meanings.get(j).getMeaning());
                if (j< meanings.size()-1) { cumulative_meaning_value.append(", "); }
                if (j==0) {
                    type = meanings.get(j).getType();
                    typeIsVerbConjugation = type.equals("VC");
                    typeIsiAdjectiveConjugation = type.equals("iAC");
                    typeIsnaAdjectiveConjugation = type.equals("naAC");
                    String[] typeElements = type.split(";");
                    typeIsVerb = type.contains("V") && !type.equals("VC") && !Arrays.asList(typeElements).contains("V");
                    typeIsAdverb = type.contains("A");
                }
                if (!wordHasPhraseConstruction) wordHasPhraseConstruction = type.equals("PC");
            }
            listTypeIsVerb.add(typeIsVerb);
            //endregion

            //region Updating the parent Romaji and Kanji values
            String parentRomaji;
            if (typeIsVerbConjugation && romaji.length()>3 && romaji.substring(0,3).equals("(o)")) parentRomaji = "(o)[verb] + " + romaji.substring(3);
            else if (typeIsVerbConjugation && romaji.length()>3 && !romaji.substring(0,3).equals("(o)")) parentRomaji = "[verb] + " + romaji;
            else if (typeIsiAdjectiveConjugation) parentRomaji = "[i-adj.] + " + romaji;
            else if (typeIsnaAdjectiveConjugation) parentRomaji = "[na-adj.] + " + romaji;
            else if (typeIsAdverb & romaji.length()>2
                    && romaji.substring(romaji.length()-2).equals("ni")
                    && !romaji.substring(romaji.length()-3).equals(" ni")) parentRomaji = romaji.substring(0,romaji.length()-2) + " ni";
            else parentRomaji = romaji;

            String romajiAndKanji;
            if (romaji.equals("")) romajiAndKanji = kanji;
            else if (kanji.equals("")) romajiAndKanji = romaji;
            else romajiAndKanji = parentRomaji + " (" + kanji + ")";
            String inputQueryNoSpaces = mInputQuery.replace(" ","");
            String inputQueryLatin = ConvertFragment.getLatinHiraganaKatakana(mInputQuery).get(GlobalConstants.TYPE_LATIN);
            String romajiAndKanjiNoSpaces = romajiAndKanji.replace(" ","");
            listRomajiAndKanji.add(romajiAndKanji);
            //endregion

            //region Updating the parent Source Info
            List<String> sourceInfo = new ArrayList<>();
            if (!romajiAndKanji.contains(mInputQuery)
                    && !romajiAndKanji.contains(inputQueryNoSpaces)
                    && !romajiAndKanjiNoSpaces.contains(mInputQuery)
                    && !romajiAndKanjiNoSpaces.contains(inputQueryNoSpaces)
                    && !romajiAndKanjiNoSpaces.contains(inputQueryLatin)) {

                String latin = ConvertFragment.getLatinHiraganaKatakana(romaji).get(GlobalConstants.TYPE_LATIN);
                String hiragana = ConvertFragment.getLatinHiraganaKatakana(romaji).get(GlobalConstants.TYPE_HIRAGANA);
                String katakana = ConvertFragment.getLatinHiraganaKatakana(romaji).get(GlobalConstants.TYPE_KATAKANA);

                if (!TextUtils.isEmpty(alternatespellings) && alternatespellings.contains(mInputQuery)) {
                    String[] altSpellingElements = alternatespellings.split(",");
                    boolean isExactMatch = false;
                    for (String element : altSpellingElements) {
                        if (mInputQuery.equals(element.trim())) {
                            isExactMatch = true;
                            break;
                        }
                    }
                    if (isExactMatch) sourceInfo.add("From alt. form \"" + mInputQuery + "\".");
                    else sourceInfo.add("From alt. form containing \"" + mInputQuery + "\".");
                }
                else if (cumulative_meaning_value.toString().contains(mInputQuery) || cumulative_meaning_value.toString().contains(latin)) {
                    //Ignore words where the input query is included in the meaning
                }
                else if (keywords != null && (keywords.contains(mInputQuery) || keywords.contains(inputQueryLatin))) {
                    String[] keywordList = keywords.split(",");
                    for (String element : keywordList) {
                        String keyword = element.trim();
                        if (!romaji.contains(keyword) && !kanji.contains(keyword) && !alternatespellings.contains(keyword)
                                && !cumulative_meaning_value.toString().contains(keyword)) {
                            sourceInfo.add((typeIsVerb)? "From conjugated form \"" + keyword + "\"." : "From associated word \"" + keyword + "\".");
                            break;
                        }
                    }
                }
                else if ((mInputQueryTextType == GlobalConstants.TYPE_KANJI
                        && kanji.length() > 0 && !kanji.substring(0, 1).equals(mInputQueryFirstLetter))
                        || (mInputQueryTextType == GlobalConstants.TYPE_LATIN
                        && romaji.length() > 0 && !romaji.substring(0, 1).equals(mInputQueryFirstLetter))
                        || (mInputQueryTextType == GlobalConstants.TYPE_HIRAGANA
                        && hiragana.length() > 0 && !hiragana.substring(0, 1).equals(mInputQueryFirstLetter))
                        || (mInputQueryTextType == GlobalConstants.TYPE_KATAKANA
                        && katakana.length() > 0 && !katakana.substring(0, 1).equals(mInputQueryFirstLetter))
                ) {
                    sourceInfo.add("Derived from \"" + mInputQuery + "\".");
                }
            }

                if (mShowSources) {
                sourceInfo.add((word.getIsCommon())? mContext.getString(R.string.common_word) : mContext.getString(R.string.less_common_word));
                sourceInfo.add((word.getIsLocal()) ? mContext.getString(R.string.source_local_offline) : mContext.getString(R.string.source_edict_online));
            }

            listSourceInfo.add(TextUtils.join(" ", sourceInfo));
            //endregion

        }
    }
    private void setMeaningsLayout(int position, DictItemViewHolder holder, int language) {

        View line;
        String type;
        String fullType;
        String meaning;
        String antonym;
        String synonym;
        int startIndex;
        int endIndex = 0;
        List<Word.Meaning> meanings = new ArrayList<>();
        switch (language) {
            case GlobalConstants.LANG_EN:
                meanings = mWordsList.get(position).getMeaningsEN();
                break;
            case GlobalConstants.LANG_FR:
                meanings = mWordsList.get(position).getMeaningsFR();
                break;
            case GlobalConstants.LANG_ES:
                meanings = mWordsList.get(position).getMeaningsES();
                break;
        }

        for (Word.Meaning wordMeaning : meanings) {
            meaning = wordMeaning.getMeaning();
            type = wordMeaning.getType();
            antonym = wordMeaning.getAntonym();
            synonym = wordMeaning.getSynonym();

            line = new View(mContext);
            line.setLayoutParams(mChildLineParams);
            line.setBackgroundColor(Color.WHITE);
            holder.childElementsLinearLayout.addView(line);

            //region Setting the type and meaning
            List<String> types = new ArrayList<>();
            for (String element : type.split(";")) {
                if (mLegendDatabase.containsKey(element)) types.add(Utilities.capitalizeFirstLetter(mLegendDatabase.get(element)));
            }
            fullType = TextUtils.join(", ", types);
            if (fullType.equals("")) fullType = type;

            String typeAsHtmlText;
            if (!fullType.equals("")) {
                typeAsHtmlText = "<i><font color='" +
                        mContext.getResources().getColor(R.color.textColorDictionaryTypeMeaning) +
                        "'>" + "[" +
                        fullType +
                        "] " + "</font></i>" + "<b>" +
                        meaning +
                        "</b>";
            }
            else {
                typeAsHtmlText = "<b>" + meaning + "</b>";
            }
            Spanned type_and_meaning = Utilities.fromHtml(typeAsHtmlText);
            TextView typeAndMeaningTextView = new TextView(mContext);
            typeAndMeaningTextView.setText(type_and_meaning);
            typeAndMeaningTextView.setTextColor(mContext.getResources().getColor(R.color.textColorDictionaryTypeMeaning2));
            typeAndMeaningTextView.setTextSize(15);
            typeAndMeaningTextView.setTextIsSelectable(true);
            typeAndMeaningTextView.setPadding(0, 16, 0, 16);
            holder.childElementsLinearLayout.addView(typeAndMeaningTextView);
            //endregion

            //region Setting the antonym
            if (!antonym.equals("")) {
                String fullAntonym = "Antonyms: " + antonym;
                SpannableString fullAntonymSpannable = new SpannableString(fullAntonym);

                String[] antonymsList = antonym.split(",");
                for (int i = 0; i < antonymsList.length; i++) {
                    if (i == 0) {
                        startIndex = 10; // Start after "Antonyms: "
                        endIndex = startIndex + antonymsList[i].length();
                    } else {
                        startIndex = endIndex + 2;
                        endIndex = startIndex + antonymsList[i].length() - 1;
                    }
                    fullAntonymSpannable.setSpan(new WordClickableSpan(), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                TextView antonymSpannable = new TextView(mContext);
                antonymSpannable.setText(fullAntonymSpannable);
                antonymSpannable.setMovementMethod(LinkMovementMethod.getInstance());
                antonymSpannable.setTextColor(mContext.getResources().getColor(R.color.textColorDictionaryAntonymSynonym));
                antonymSpannable.setTextSize(14);
                antonymSpannable.setTypeface(mDroidSansJapaneseTypeface);
                antonymSpannable.setTextIsSelectable(true);
                antonymSpannable.setPadding(0, 0, 0, 16);
                holder.childElementsLinearLayout.addView(antonymSpannable);
            }
            //endregion

            //regionSetting the synonym
            if (!synonym.equals("")) {
                String fullSynonym = "Synonyms: " + synonym;
                SpannableString fullSynonymSpannable = new SpannableString(fullSynonym);

                String[] synonymsList = synonym.split(",");
                for (int i = 0; i < synonymsList.length; i++) {
                    if (i == 0) {
                        startIndex = 10; // Start after "Synonyms: "
                        endIndex = startIndex + synonymsList[i].length();
                    } else {
                        startIndex = endIndex + 2;
                        endIndex = startIndex + synonymsList[i].length() - 1;
                    }
                    fullSynonymSpannable.setSpan(new WordClickableSpan(), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                TextView synonymTextView = new TextView(mContext);
                synonymTextView.setText(fullSynonymSpannable);
                synonymTextView.setMovementMethod(LinkMovementMethod.getInstance());
                synonymTextView.setTextColor(mContext.getResources().getColor(R.color.textColorDictionaryAntonymSynonym));
                synonymTextView.setTextSize(14);
                synonymTextView.setTextIsSelectable(true);
                synonymTextView.setTypeface(mDroidSansJapaneseTypeface);
                synonymTextView.setPadding(0, 0, 0, 16);
                holder.childElementsLinearLayout.addView(synonymTextView);
            }
            //endregion

            //region Setting the explanation, rule show/hide line and examples
            List<Word.Meaning.Explanation> currentExplanations = wordMeaning.getExplanations();
            String explanation;
            String rules;
            List<Word.Meaning.Explanation.Example> examplesList;

            for (int i = 0; i < currentExplanations.size(); i++) {

                //region Adding the explanation
                explanation = currentExplanations.get(i).getExplanation();
                if (!explanation.equals("")) {
                    TextView explanationTextView = new TextView(mContext);
                    explanationTextView.setText(explanation);
                    explanationTextView.setTextColor(mContext.getResources().getColor(R.color.textColorDictionaryExplanation));
                    explanationTextView.setPadding(0, 8, 0, 0);
                    explanationTextView.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
                    explanationTextView.setTextIsSelectable(true);
                    holder.childElementsLinearLayout.addView(explanationTextView);
                }
                //endregion

                //region Adding the rules
                rules = currentExplanations.get(i).getRules();
                if (!rules.equals("")) {
                    TextView rulesTextView = new TextView(mContext);
                    rulesTextView.setTextColor(mContext.getResources().getColor(R.color.textColorDictionaryRule));
                    rulesTextView.setPadding(0, 8, 0, 0);
                    rulesTextView.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
                    rulesTextView.setTextIsSelectable(true);

                    String[] parsedRule = rules.split(RULE_DELIMITER);
                    String where = " where: ";
                    String intro = "";
                    if (!parsedRule[0].contains(":")) {
                        intro = mContext.getResources().getString(R.string.how_it_s_used_);
                    }
                    Spanned spanned_rule;

                    if (parsedRule.length == 1) { // If the rule doesn't have a "where" clause
                        typeAsHtmlText = "<b>" +
                                "<font color='" + mContext.getResources().getColor(R.color.textColorDictionaryRulePhraseStructureClause) + "'>" +
                                intro +
                                "</font>" +
                                rules +
                                "</b>";
                        spanned_rule = Utilities.fromHtml(typeAsHtmlText);
                        rulesTextView.setText(spanned_rule);
                        rulesTextView.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    } else {
                        typeAsHtmlText = "<b>" +
                                "<font color='" + mContext.getResources().getColor(R.color.textColorDictionaryRulePhraseStructureClause) + "'>" +
                                intro +
                                "</font>" +
                                parsedRule[0] +
                                "</b>" + "<font color='" + mContext.getResources().getColor(R.color.textColorDictionaryRuleWhereClause) + "'>" +
                                where +
                                "</font>" +
                                "<b>" + parsedRule[1] + "</b>";
                        spanned_rule = Utilities.fromHtml(typeAsHtmlText);
                        rulesTextView.setText(spanned_rule);
                    }

                    holder.childElementsLinearLayout.addView(rulesTextView);
                }
                //endregion

                //region Adding the examples
                examplesList = currentExplanations.get(i).getExamples();
                if (examplesList.size()>0) {

                    final List<TextView> examplesTextViews = new ArrayList<>();

                    final TextView examplesShowTextView = new TextView(mContext);
                    examplesShowTextView.setText(mContext.getResources().getString(R.string.ShowExamples));
                    examplesShowTextView.setTextColor(mContext.getResources().getColor(R.color.textColorDictionaryExamples));
                    examplesShowTextView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                    examplesShowTextView.setPadding(0, 0, 0, 8);
                    examplesShowTextView.setClickable(true);
                    examplesShowTextView.setFocusable(false);
                    examplesShowTextView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (examplesTextViews.size()>0) {
                                if (examplesTextViews.get(0).getVisibility() == View.VISIBLE) {
                                    examplesShowTextView.setText(mContext.getString(R.string.ShowExamples));
                                    for (TextView textView : examplesTextViews) {
                                        textView.setVisibility(View.GONE);
                                    }
                                }
                                else {
                                    examplesShowTextView.setText(mContext.getString(R.string.HideExamples));
                                    for (TextView textView : examplesTextViews) {
                                        textView.setVisibility(View.VISIBLE);
                                    }
                                }
                            }
                        }
                    });
                    holder.childElementsLinearLayout.addView(examplesShowTextView);

                    for (int j=0; j<examplesList.size(); j++) {

                        //Setting the English example characteristics
                        TextView englishExampleTextView = new TextView(mContext);
                        englishExampleTextView.setText(examplesList.get(j).getEnglishSentence());
                        englishExampleTextView.setTextColor(mContext.getResources().getColor(R.color.textColorDictionaryExampleEnglish));
                        englishExampleTextView.setTextSize(14);
                        englishExampleTextView.setPadding(4, 8, 0, 0);
                        englishExampleTextView.setVisibility(View.GONE);
                        englishExampleTextView.setTextIsSelectable(true);
                        examplesTextViews.add(englishExampleTextView);
                        holder.childElementsLinearLayout.addView(englishExampleTextView);

                        //Setting the Romaji example characteristics
                        TextView romajiExampleTextView = new TextView(mContext);
                        romajiExampleTextView.setText(examplesList.get(j).getRomajiSentence());
                        romajiExampleTextView.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
                        romajiExampleTextView.setTextColor(mContext.getResources().getColor(R.color.textColorDictionaryExampleRomaji));
                        romajiExampleTextView.setTextSize(14);
                        romajiExampleTextView.setPadding(4, 0, 0, 0);
                        romajiExampleTextView.setVisibility(View.GONE);
                        romajiExampleTextView.setTextIsSelectable(true);
                        examplesTextViews.add(romajiExampleTextView);
                        holder.childElementsLinearLayout.addView(romajiExampleTextView);

                        //Setting the Kanji example characteristics
                        TextView kanjiExampleTextView = new TextView(mContext);
                        kanjiExampleTextView.setText(examplesList.get(j).getKanjiSentence());
                        kanjiExampleTextView.setTextColor(mContext.getResources().getColor(R.color.textColorDictionaryExampleKanji));
                        kanjiExampleTextView.setTextSize(14);
                        kanjiExampleTextView.setVisibility(View.GONE);
                        kanjiExampleTextView.setTextIsSelectable(true);
                        kanjiExampleTextView.setTypeface(mDroidSansJapaneseTypeface);
                        kanjiExampleTextView.setPadding(4, 12, 0, 16);
                        examplesTextViews.add(kanjiExampleTextView);
                        holder.childElementsLinearLayout.addView(kanjiExampleTextView);
                    }

                }
                //endregion

                //region Adding the separator line between explanations
                if (!rules.equals("") && i < currentExplanations.size()-1) {
                    line = new View(mContext);
                    line.setLayoutParams(mubChildLineParams);
                    line.setBackgroundColor(mContext.getResources().getColor(R.color.colorPrimaryLight));
                    holder.childElementsLinearLayout.addView(line);
                }
                //endregion
            }
            //endregion

        }
    }
    private void createVisibilityArray() {
        mChildIsVisible = new boolean[mWordsList==null? 0 : mWordsList.size()];
        Arrays.fill(mChildIsVisible, false);
    }
    private void setHyperlinksInCopyToInputLine(String type, TextView textView, String before, String hyperlinkText, String after) {
        String totalText = "<b>" +
                "<font color='" + mContext.getResources().getColor(R.color.textColorSecondary) + "'>" +
                before +
                "</font>" +
                hyperlinkText +
                "<font color='" + mContext.getResources().getColor(R.color.textColorSecondary) + "'>" +
                after +
                "</font>";
        Spanned spanned_totalText = Utilities.fromHtml(totalText);
        SpannableString WordSpannable = new SpannableString(spanned_totalText);
        if (type.equals("word")) {
            WordSpannable.setSpan(new WordClickableSpan(), before.length(), spanned_totalText.length() - after.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            WordSpannable.setSpan(new VerbClickableSpan(), before.length(), spanned_totalText.length() - after.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        textView.setText(WordSpannable);
        textView.setTypeface(Typeface.SERIF);
        textView.setTypeface(null, Typeface.BOLD_ITALIC);
        textView.setTextSize(16);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
    private class WordClickableSpan extends ClickableSpan {
        // code extracted from http://stackoverflow.com/questions/15475907/make-parts-of-textview-clickable-not-url
        public void onClick(@NonNull View textView) {
            // code extracted from http://stackoverflow.com/questions/19750458/android-clickablespan-get-text-onclick

            TextView text = (TextView) textView;
            Spanned s = (Spanned) text.getText();
            int start = s.getSpanStart(this);
            int end = s.getSpanEnd(this);

            String outputText = text.getText().subSequence(start, end).toString();
            mOnItemClickHandler.onWordLinkClicked(outputText);

        }
        @Override
        public void updateDrawState(@NonNull TextPaint ds) {
            ds.setColor(mContext.getResources().getColor(R.color.textColorDictionarySpanClicked));
            ds.setUnderlineText(false);
        }
    }
    private class VerbClickableSpan extends ClickableSpan {
        // code extracted from http://stackoverflow.com/questions/15475907/make-parts-of-textview-clickable-not-url
        public void onClick(@NonNull View textView) {
            // code extracted from http://stackoverflow.com/questions/19750458/android-clickablespan-get-text-onclick

            TextView text = (TextView) textView;
            Spanned s = (Spanned) text.getText();
            int start = s.getSpanStart(this);
            int end = s.getSpanEnd(this);

            String outputText = text.getText().subSequence(start, end).toString();
            mOnItemClickHandler.onVerbLinkClicked(outputText);

        }

        @Override
        public void updateDrawState(@NonNull TextPaint ds) {
            ds.setColor(mContext.getResources().getColor(R.color.textColorDictionarySpanClicked));
            ds.setUnderlineText(false);
        }
    }

    @Override public int getItemCount() {
        return (mWordsList == null) ? 0 : mWordsList.size();
    }
    public void setContents(List<Word> wordsList) {
        mWordsList = wordsList;
        createVisibilityArray();
        prepareLayoutTexts();
        if (mWordsList != null) {
            this.notifyDataSetChanged();
        }
    }
    public void setShowSources(boolean state) {
        mShowSources = state;
    }
    public void setActiveMeaningLanguages(boolean[] activeMeaningLanguages) {
        mActiveMeaningLanguages = activeMeaningLanguages;
    }

    public class DictItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.list_item_dictionary) ConstraintLayout parentContainer;
        @BindView(R.id.list_item_romaji_and_kanji) TextView romajiAndKanjiTextView;
        @BindView(R.id.list_item_source_info) TextView sourceInfoTextView;
        @BindView(R.id.dropdown_arrow) ImageView dropdownArrowImageView;
        @BindView(R.id.list_item_meanings) TextView meaningsTextView;
        @BindView(R.id.list_item_child_linearlayout) LinearLayout childLinearLayout;
        @BindView(R.id.list_item_child_romaji) TextView romajiChildTextView;
        @BindView(R.id.list_item_child_kanji) TextView kanjiChildTextView;
        @BindView(R.id.list_item_child_elements_container) LinearLayout childElementsLinearLayout;

        DictItemViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int clickedPosition = getAdapterPosition();
            switchVisibilityOfChildLayout(clickedPosition);
        }

        private void switchVisibilityOfChildLayout(int clickedPosition) {
            if (childLinearLayout.getVisibility() == View.VISIBLE) {
                childLinearLayout.setVisibility(View.GONE);
                meaningsTextView.setVisibility(View.VISIBLE);
                if (!TextUtils.isEmpty(listSourceInfo.get(getAdapterPosition()))) sourceInfoTextView.setVisibility(View.VISIBLE);
                parentContainer.setBackgroundColor(Color.TRANSPARENT);
                dropdownArrowImageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_arrow_drop_down_black_24dp));
                mChildIsVisible[clickedPosition] = false;
            }
            else {
                childLinearLayout.setVisibility(View.VISIBLE);
                meaningsTextView.setVisibility(View.GONE);
                sourceInfoTextView.setVisibility(View.GONE);
                parentContainer.setBackgroundColor(mContext.getResources().getColor(R.color.colorSelectedDictResultBackground));
                dropdownArrowImageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_arrow_drop_up_black_24dp));
                mChildIsVisible[clickedPosition] = true;
            }
        }
    }

    public interface DictionaryItemClickHandler {
        void onWordLinkClicked(String text);
        void onVerbLinkClicked(String text);
    }
}
