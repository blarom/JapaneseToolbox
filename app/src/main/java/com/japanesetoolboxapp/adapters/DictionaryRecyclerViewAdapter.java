package com.japanesetoolboxapp.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.japanesetoolboxapp.R;
import com.japanesetoolboxapp.data.Word;
import com.japanesetoolboxapp.resources.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DictionaryRecyclerViewAdapter extends RecyclerView.Adapter<DictionaryRecyclerViewAdapter.DictItemViewHolder> {

    private final Context mContext;
    private final List<String[]> mLegendDatabase;
    private boolean[] mChildIsVisible;
    private List<Word> mWordsList;
    final private DictionaryItemClickHandler mOnItemClickHandler;

    public DictionaryRecyclerViewAdapter(Context context, DictionaryItemClickHandler listener , List<Word> wordsList, List<String[]> legendDatabase) {
        this.mContext = context;
        this.mWordsList = wordsList;
        this.mLegendDatabase = legendDatabase;
        this.mOnItemClickHandler = listener;
        createVisibilityArray();
    }

    @NonNull @Override public DictItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.list_item_dictonary, parent, false);
        view.setFocusable(true);
        return new DictItemViewHolder(view);
    }
    @Override public void onBindViewHolder(@NonNull DictItemViewHolder holder, int position) {

        holder.childLinearLayout.setVisibility( mChildIsVisible[position]? View.VISIBLE : View.GONE);

        //region Getting the word characteristics
        String romaji = mWordsList.get(position).getRomaji();
        String kanji = mWordsList.get(position).getKanji();
        String alternatespellings = mWordsList.get(position).getAltSpellings();
        String type;
        String fullType;
        String meaning;
        String antonym;
        String synonym;
        List<Word.Meaning> meanings = mWordsList.get(position).getMeanings();

        StringBuilder cumulative_meaning_value = new StringBuilder();
        boolean typeIsVerbConjugation = false;
        boolean typeIsVerb = false;
        for (int j = 0; j< meanings.size(); j++) {
            cumulative_meaning_value.append(meanings.get(j).getMeaning());
            if (j< meanings.size()-1) { cumulative_meaning_value.append(", "); }
            if (j==0) {
                type = meanings.get(j).getType();
                typeIsVerbConjugation = type.equals("VC");
                typeIsVerb = type.contains("V") && !type.equals("VC");
            }
        }
        //endregion

        //region Updating the parent values
        String parentRomaji = (typeIsVerbConjugation)? "[verb]" + romaji : romaji;
        String romajiAndKanji =  parentRomaji + " (" + kanji + ")";
        if (kanji.equals("")) { romajiAndKanji = romaji; }
        holder.romajiAndKanjiTextView.setText(romajiAndKanji);

        if (romaji.equals("")) { holder.romajiAndKanjiTextView.setVisibility(View.GONE); }
        else { holder.romajiAndKanjiTextView.setVisibility(View.VISIBLE); }

        holder.meaningsTextView.setText(Utilities.removeDuplicatesFromCommaList(cumulative_meaning_value.toString()));
        //endregion

        //region Updating the child values

        //region Initialization
        int startIndex;
        int endIndex = 0;
        holder.childElementsLinearLayout.removeAllViews();
        holder.childElementsLinearLayout.setFocusable(false);
        //endregion

        //region Showing the romaji and kanji values for user click
        if (typeIsVerb) {
            setHyperlinksInCopyToInputLine("verb", holder.romajiChildTextView, "Conjugate ", romaji, " ");
            setHyperlinksInCopyToInputLine("verb", holder.kanjiChildTextView, "(", kanji, ").");
        } else {
            setHyperlinksInCopyToInputLine("word", holder.romajiChildTextView, "Copy ", romaji, " ");
            setHyperlinksInCopyToInputLine("word", holder.kanjiChildTextView, "(", kanji, ") to input.");
        }
        //endregion

        //regionSetting the alternate spellings
        if (!alternatespellings.equals("")) {
            String htmlText = "<font face='serif' color='" +
                    mContext.getResources().getColor(R.color.textColorDictionaryAlternateSpellings) +
                    "'>" + "<b>" + "Alternate spellings: " + "</b>" + alternatespellings + "</font>";
            Spanned spanned_alternatespellings = Utilities.fromHtml(htmlText);
            TextView tv_alternatespellings = new TextView(mContext);
            tv_alternatespellings.setText(spanned_alternatespellings);
            tv_alternatespellings.setTextSize(14);
            tv_alternatespellings.setTextIsSelectable(true);
            tv_alternatespellings.setClickable(true);
            holder.childElementsLinearLayout.addView(tv_alternatespellings);
        }
        //endregion

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT, 2 );
        params.setMargins(0, 8, 0, 4);
        View line;

        //region Setting the wordMeaning elements
        for (Word.Meaning wordMeaning : meanings) {
            meaning = wordMeaning.getMeaning();
            type = wordMeaning.getType();
            antonym = wordMeaning.getAntonym();
            synonym = wordMeaning.getSynonym();

            line = new View(mContext);
            line.setLayoutParams(params);
            line.setBackgroundColor(Color.WHITE);
            holder.childElementsLinearLayout.addView(line);

            //region Setting the type and meaning
            fullType = "";
            for (int i=0; i<mLegendDatabase.size(); i++) {
                if (mLegendDatabase.get(i)[0].equals(type)) { fullType = mLegendDatabase.get(i)[1]; break; }
            }
            if (fullType.equals("")) { fullType = type; }

            String htmlText = "<i><font color='"+
                    mContext.getResources().getColor(R.color.textColorDictionaryTypeMeaning) +
                    "'>" + "[" +
                    fullType +
                    "] " + "</font></i>" + "<b>" +
                    meaning +
                    "</b>";
            Spanned type_and_meaning = Utilities.fromHtml(htmlText);
            TextView typeAndMeaningTextView = new TextView(mContext);
            typeAndMeaningTextView.setText(type_and_meaning);
            typeAndMeaningTextView.setTextColor(mContext.getResources().getColor(R.color.textColorDictionaryTypeMeaning2));
            typeAndMeaningTextView.setTextSize(15);
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
                antonymSpannable.setTextIsSelectable(true);
                holder.childElementsLinearLayout.addView(antonymSpannable);
            }
            //endregion

            //regionSetting the synonym

            if (!synonym.equals("")) {
                String fullSynonym = "Synonyms: " + synonym;
                SpannableString fullSynonymSpannable = new SpannableString(fullSynonym);

                String[] synonymsList = antonym.split(",");
                for (int i = 0; i < synonymsList.length; i++) {
                    if (i == 0) {
                        startIndex = 10; // Start after "Antonyms: "
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
                holder.childElementsLinearLayout.addView(synonymTextView);
            }
            //endregion

            //region Setting the explanation, rule show/hide line and examples
            List<Word.Meaning.Explanation> currentExplanations = wordMeaning.getExplanations();
            String explanation;
            String rules;
            List<Word.Meaning.Explanation.Example> examplesList;

            for (int i = 0; i < currentExplanations.size(); i++) {

                explanation = currentExplanations.get(i).getExplanation();
                if (!explanation.equals("")) {
                    TextView explanationTextView = new TextView(mContext);
                    explanationTextView.setText(explanation);
                    explanationTextView.setTextColor(mContext.getResources().getColor(R.color.textColorDictionaryExplanation));
                    explanationTextView.setPadding(0, 10, 0, 0);
                    explanationTextView.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
                    holder.childElementsLinearLayout.addView(explanationTextView);
                }

                rules = currentExplanations.get(i).getRules();
                if (!rules.equals("")) {
                    TextView rulesTextView = new TextView(mContext);
                    rulesTextView.setTextColor(mContext.getResources().getColor(R.color.textColorDictionaryRule));
                    rulesTextView.setPadding(0, 30, 0, 0);
                    rulesTextView.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);

                    String[] parsedRule = rules.split("@");
                    String where = " where: ";
                    String intro = "";
                    if (!parsedRule[0].contains(":")) {
                        intro = mContext.getResources().getString(R.string.PhraseStructure) + " ";
                    }
                    Spanned spanned_rule;

                    if (parsedRule.length == 1) { // If the rule doesn't have a "where" clause
                        htmlText = "<b>" +
                                "<font color='" + mContext.getResources().getColor(R.color.textColorDictionaryRulePhraseStructureClause) + "'>" +
                                intro +
                                "</font>" +
                                rules;
                        spanned_rule = Utilities.fromHtml(htmlText);
                        rulesTextView.setText(spanned_rule);
                        rulesTextView.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    } else {
                        htmlText = "<b>" +
                                "<font color='" + mContext.getResources().getColor(R.color.textColorDictionaryRulePhraseStructureClause) + "'>" +
                                intro +
                                "</font>" +
                                parsedRule[0] +
                                "</b>" + "<font color='" + mContext.getResources().getColor(R.color.textColorDictionaryRuleWhereClause) + "'>" +
                                where +
                                "</font>" +
                                "<b>" + parsedRule[1] + "</b>";
                        spanned_rule = Utilities.fromHtml(htmlText);
                        rulesTextView.setText(spanned_rule);
                    }

                    holder.childElementsLinearLayout.addView(rulesTextView);
                }

                examplesList = currentExplanations.get(i).getExamples();
                if (examplesList.size()>0) {

                    final List<TextView> examplesTextViews = new ArrayList<>();

                    final TextView examplesShowTextView = new TextView(mContext);
                    examplesShowTextView.setText(mContext.getResources().getString(R.string.ShowExamples));
                    examplesShowTextView.setTextColor(mContext.getResources().getColor(R.color.textColorDictionaryExamples));
                    examplesShowTextView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                    examplesShowTextView.setPadding(0, 10, 0, 0);
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
                        englishExampleTextView.setPadding(4, 15, 0, 0);
                        englishExampleTextView.setVisibility(View.GONE);
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
                        examplesTextViews.add(romajiExampleTextView);
                        holder.childElementsLinearLayout.addView(romajiExampleTextView);

                        //Setting the Kanji example characteristics
                        TextView kanjiExampleTextView = new TextView(mContext);
                        kanjiExampleTextView.setText(examplesList.get(j).getKanjiSentence());
                        kanjiExampleTextView.setTextColor(mContext.getResources().getColor(R.color.textColorDictionaryExampleKanji));
                        kanjiExampleTextView.setTextSize(14);
                        kanjiExampleTextView.setPadding(4, 0, 0, 0);
                        kanjiExampleTextView.setVisibility(View.GONE);
                        examplesTextViews.add(kanjiExampleTextView);
                        holder.childElementsLinearLayout.addView(kanjiExampleTextView);
                    }

                }
            }
            //endregion

        }
        //endregion

        //endregion

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
        public void onClick(View textView) {
            // code extracted from http://stackoverflow.com/questions/19750458/android-clickablespan-get-text-onclick

            TextView text = (TextView) textView;
            Spanned s = (Spanned) text.getText();
            int start = s.getSpanStart(this);
            int end = s.getSpanEnd(this);

            String outputText = text.getText().subSequence(start, end).toString();
            mOnItemClickHandler.onWordLinkClicked(outputText);

        }
        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setColor(mContext.getResources().getColor(R.color.textColorDictionarySpanClicked));
            ds.setUnderlineText(false);
        }
    }
    private class VerbClickableSpan extends ClickableSpan {
        // code extracted from http://stackoverflow.com/questions/15475907/make-parts-of-textview-clickable-not-url
        public void onClick(View textView) {
            // code extracted from http://stackoverflow.com/questions/19750458/android-clickablespan-get-text-onclick

            TextView text = (TextView) textView;
            Spanned s = (Spanned) text.getText();
            int start = s.getSpanStart(this);
            int end = s.getSpanEnd(this);

            String outputText = text.getText().subSequence(start, end).toString();
            mOnItemClickHandler.onVerbLinkClicked(outputText);

        }

        @Override
        public void updateDrawState(TextPaint ds) {
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
        if (mWordsList != null) {
            this.notifyDataSetChanged();
        }
    }
    private void createVisibilityArray() {
        mChildIsVisible = new boolean[mWordsList==null? 0 : mWordsList.size()];
        Arrays.fill(mChildIsVisible, false);
    }

    public class DictItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.list_item_romaji_and_kanji) TextView romajiAndKanjiTextView;
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
                mChildIsVisible[clickedPosition] = false;
            }
            else {
                childLinearLayout.setVisibility(View.VISIBLE);
                mChildIsVisible[clickedPosition] = true;
            }
        }
    }

    public interface DictionaryItemClickHandler {
        void onWordLinkClicked(String text);
        void onVerbLinkClicked(String text);
    }
}
