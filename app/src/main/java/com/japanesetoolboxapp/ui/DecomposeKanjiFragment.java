package com.japanesetoolboxapp.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.japanesetoolboxapp.R;
import com.japanesetoolboxapp.data.DatabaseUtilities;
import com.japanesetoolboxapp.data.JapaneseToolboxKanjiRoomDatabase;
import com.japanesetoolboxapp.data.KanjiCharacter;
import com.japanesetoolboxapp.resources.MainApplication;
import com.japanesetoolboxapp.resources.Utilities;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class DecomposeKanjiFragment extends Fragment implements LoaderManager.LoaderCallbacks<Object>{


    //region Parameters
    @BindView(R.id.decompositionsHint) TextView mDecompositionsHint;
    @BindView(R.id.decompositionScrollView) ScrollView mDecompositionScrollView;
    @BindView(R.id.overall_block_container) LinearLayout mOverallBlockContainer;
    @BindView(R.id.decompositions_loading_indicator) ProgressBar mProgressBarLoadingIndicator;
    private Unbinder mBinding;
    private static final int ROOM_DB_KANJI_DECOMPOSITION_LOADER = 7524;
    private static final int ROOM_DB_KANJI_CHARACTER_LOADER = 4732;
    private String mInputQuery;
    private List<String[]> mRadicalsOnlyDatabase;
    private boolean mAlreadyGotFirstQuery;
    //endregion


    //Lifecycle Functions
    @Override public void onCreate(Bundle savedInstanceState) { //instead of onActivityCreated
        super.onCreate(savedInstanceState);
        getExtras();
    }
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_decompose_kanji, container, false);

        //setRetainInstance(true);

        mBinding = ButterKnife.bind(this, rootView);

        getDecomposition();

        return rootView;
    }
    @Override public void onDetach() {
        super.onDetach();
        if (getLoaderManager()!=null) getLoaderManager().destroyLoader(ROOM_DB_KANJI_CHARACTER_LOADER);
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
            mRadicalsOnlyDatabase = (List<String[]>) getArguments().getSerializable(getString(R.string.rad_only_database));
        }
    }
    public void getDecomposition() {

        // Check that the input is valid. If not, return no result.

        String text_type = ConvertFragment.getTextType(mInputQuery);

        if (!TextUtils.isEmpty(mInputQuery) && !text_type.equals("latin") && !text_type.equals("number")) {
            mDecompositionsHint.setVisibility(View.GONE);
            startGettingDecompositionAsynchronously(mInputQuery.substring(0,1), 0);
        }
        else {
            mDecompositionsHint.setVisibility(View.VISIBLE);
        }

    }
    private void displayDecomposition(String inputQuery,
                                      final int radical_iteration,
                                      List<List<String>> decomposedKanji,
                                      List<String> currentKanjiDetailedCharacteristics,
                                      List<String> currentKanjiMainRadicalInfo,
                                      int radicalIndex,
                                      int radicalIndexOriginal,
                                      List<String> currentMainRadicalDetailedCharacteristics) {

        //region Initilization
        SpannableString clickable_text;
        Spanned text;
        TextView tv;
        TextView tv1;
        TextView tv2;
        ImageView img;
        LinearLayout overall_block_linearLayout;
        LinearLayout overall_row_linearLayout;
        LinearLayout radical_gallery_linearLayout;
        LinearLayout.LayoutParams overall_block_layoutParams;
        LinearLayout.LayoutParams overall_row_layoutParams;
        LinearLayout.LayoutParams radical_gallery_layoutParams;
        LinearLayout.LayoutParams params;
        RelativeLayout.LayoutParams tv1_layoutParams;
        RelativeLayout.LayoutParams tv2_layoutParams;
        String display_text;
        String[] structure_info;

        if (getActivity()!=null) Utilities.hideSoftKeyboard(getActivity());

        Boolean character_not_found_in_KanjiDictDatabase = false;
        if (currentKanjiDetailedCharacteristics.size() == 0) { character_not_found_in_KanjiDictDatabase = true; }
        Boolean character_is_radical_or_kana = false;
        //endregion

        //region Get the radical characteristics
        String[] radical_row = null;
        if (radicalIndex != -1) {
            radical_row = mRadicalsOnlyDatabase.get(radicalIndex);
            character_is_radical_or_kana = true;
        }
        //endregion

        //region overall_block_container configuration

        // If the user clicks on a component further up in the overall_block_container chain, remove the following views
        int childCount = mOverallBlockContainer.getChildCount();
        for (int i=radical_iteration;i<childCount;i++) {
            mOverallBlockContainer.removeViewAt(radical_iteration);
        }

        //region overall_block_container > overall block configuration
        //region overall_block_container > overall block container layout configuration
        overall_block_layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        overall_block_layoutParams.gravity = Gravity.CENTER_VERTICAL|Gravity.LEFT;
        overall_block_layoutParams.setMargins(10, 20, 10, 0); // (left, top, right, bottom)

        overall_block_linearLayout = new LinearLayout(getContext());
        overall_block_linearLayout.setOrientation(LinearLayout.VERTICAL);
        overall_block_linearLayout.setLayoutParams(overall_block_layoutParams);
        //endregion

        //region overall_block_container > overall block > overall_row configuration
        //region Layout configuration
        overall_row_layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        overall_row_layoutParams.gravity = Gravity.BOTTOM|Gravity.LEFT;
        overall_row_layoutParams.setMargins(10, 0, 10, 0); // (left, top, right, bottom)

        overall_row_linearLayout = new LinearLayout(getContext());
        overall_row_linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        overall_row_linearLayout.setLayoutParams(overall_row_layoutParams);
        //endregion

        //region Configure the elements of overall_row
        //region Input value
        tv = new TextView(getContext());
        tv.setLayoutParams(overall_row_layoutParams);
        tv.setText(decomposedKanji.get(0).get(0));
        tv.setTextSize(30);
        //tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/updatedunicodefont.ttf");
        //tv.setTypeface(tf);
        tv.setTextColor(Color.parseColor("#0000FF"));
        tv.setTextIsSelectable(true);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setSelected(false);
        overall_row_linearLayout.addView(tv);
        //endregion

        //region Separator
        tv = new TextView(getContext());
        tv.setLayoutParams(overall_row_layoutParams);
        tv.setText("|");
        tv.setTextSize(36);
        tv.setPadding(0,0,0,20);
        tv.setTextColor(Color.parseColor("#0000FF"));
        tv.setTextIsSelectable(false);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setSelected(false);
        overall_row_linearLayout.addView(tv);
        //endregion

        //region Structure mImageToBeDecoded
        img = new ImageView(getContext());
        img.setLayoutParams(overall_row_layoutParams);
        img.setPadding(0,10,0,0);
        structure_info = getStructureInfo(decomposedKanji.get(0).get(1));
        img.setImageResource(Integer.parseInt(structure_info[1]));

        params = (LinearLayout.LayoutParams) img.getLayoutParams();
        params.gravity = Gravity.CENTER_VERTICAL;
        overall_row_linearLayout.addView(img);
        //endregion

        //region Separator
        tv = new TextView(getContext());
        tv.setLayoutParams(overall_row_layoutParams);
        tv.setText("|");
        tv.setTextSize(36);
        tv.setPadding(0,0,0,20);
        tv.setTextColor(Color.parseColor("#0000FF"));
        tv.setTextIsSelectable(false);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setSelected(false);
        overall_row_linearLayout.addView(tv);
        //endregion

        //region Radical gallery
        //region Layout parameters
        radical_gallery_layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        radical_gallery_layoutParams.gravity = Gravity.CENTER_VERTICAL|Gravity.LEFT;
        radical_gallery_layoutParams.setMargins(10, 0, 10, 0); // (left, top, right, bottom)
        //endregion

        //region Populating the gallery
        radical_gallery_linearLayout = new LinearLayout(getContext());
        radical_gallery_linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        radical_gallery_linearLayout.setLayoutParams(radical_gallery_layoutParams);

        for (int i = 1; i < decomposedKanji.size(); i++) {

            if (decomposedKanji.get(i).get(0).equals("")) {break;}

            // Add the component to the layout
            tv = new TextView(getContext());
            tv.setLayoutParams(radical_gallery_layoutParams);
            display_text = decomposedKanji.get(i).get(0);
            text = Utilities.fromHtml("<b><font color='#800080'>" + display_text + "</font></b>");
            clickable_text = new SpannableString(text);
            ClickableSpan Radical_Iteration_ClickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View textView) {

                    TextView tv = (TextView) textView;
                    String text = tv.getText().toString();
                    Spanned s = (Spanned) tv.getText();
                    int start = s.getSpanStart(this);
                    int end = s.getSpanEnd(this);

                    startGettingDecompositionAsynchronously(text, radical_iteration+1);
                }
                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                }
            };
            clickable_text.setSpan(Radical_Iteration_ClickableSpan, 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            //tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/updatedunicodefont.ttf");
            //tv.setTypeface(tf);
            tv.setText(clickable_text);
            tv.setTextSize(26);
            tv.setPadding(10,0,10,0);
            tv.setMovementMethod(LinkMovementMethod.getInstance());
            tv.setSelected(false);
            tv.setBackgroundColor(Color.parseColor("#ffffff"));
            tv.setAlpha((float) 0.90);
            radical_gallery_linearLayout.addView(tv);

            if (i < decomposedKanji.size()-1) {
                tv = new TextView(getContext());
                tv.setText("\u00B7");
                tv.setPadding(10,0,10,0);
                tv.setTextSize(30);
                tv.setTypeface(null, Typeface.BOLD);
                tv.setBackgroundColor(Color.parseColor("#ffffff"));
                tv.setAlpha((float) 0.90);
                radical_gallery_linearLayout.addView(tv);
            }

        }
        //endregion

        overall_row_linearLayout.addView(radical_gallery_linearLayout);
        //endregion
        //endregion

        //region Add the overall_row to the overall_block
        overall_block_linearLayout.addView(overall_row_linearLayout);
        //endregion
        //endregion

        //region overall_block_container > overall block > explanation_row configuration
        //region Configure Layout parameters
        RelativeLayout.LayoutParams explanation_block_layoutParams = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        explanation_block_layoutParams.setMargins(10, 0, 10, 0); // (left, top, right, bottom)
        explanation_block_layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        explanation_block_layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);

        RelativeLayout explanation_block = new RelativeLayout(getContext());
        explanation_block.setLayoutParams(explanation_block_layoutParams);
        //endregion

        //region Configure the elements of explanation_row
        //region Get Structure
        tv1 = new TextView(getContext());
        tv1.setText(R.string.structure_);
        tv1.setTextSize(14);
        tv1.setTextColor(Color.parseColor("#800080"));
        tv1.setTextIsSelectable(false);
        tv1.setTypeface(null, Typeface.BOLD);
        tv1.setMovementMethod(LinkMovementMethod.getInstance());
        tv1.setSelected(false);
        tv1.setBackgroundColor(Color.parseColor("#ffffff"));
        tv1.setAlpha((float) 0.90);
        tv1.setPadding(0,0,80,0);

        tv1.setId(R.id.decomposition_structure);
        tv1_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        tv1_layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        tv1_layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        explanation_block.addView(tv1,tv1_layoutParams);

        tv2 = new TextView(getContext());
        tv2.setText(structure_info[0]);
        tv2.setTextSize(14);
        tv2.setTextColor(Color.parseColor("#800080"));
        tv2.setTextIsSelectable(false);
        tv2.setTypeface(null, Typeface.NORMAL);
        tv2.setMovementMethod(LinkMovementMethod.getInstance());
        tv2.setSelected(false);
        tv2.setBackgroundColor(Color.parseColor("#ffffff"));
        tv2.setAlpha((float) 0.90);

        tv2.setId(R.id.decomposition_structure_text);
        tv2_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        tv2_layoutParams.addRule(RelativeLayout.RIGHT_OF,R.id.decomposition_structure);
        tv2_layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        explanation_block.addView(tv2,tv2_layoutParams);
        tv1_layoutParams.addRule(RelativeLayout.ALIGN_TOP,R.id.decomposition_structure_text);
        //endregion

        //region Get Meanings and Readings
        if (!character_is_radical_or_kana && character_not_found_in_KanjiDictDatabase && currentKanjiMainRadicalInfo.get(0).equals("")) {
            //region Display Characteristics: Meaning of non-Japanese or uncommon Japanese character

            tv1 = new TextView(getContext());
            tv1.setText(R.string.characteristics_);
            tv1.setTextSize(14);
            tv1.setTextColor(Color.parseColor("#800080"));
            tv1.setTextIsSelectable(false);
            tv1.setTypeface(null, Typeface.BOLD);
            tv1.setMovementMethod(LinkMovementMethod.getInstance());
            tv1.setSelected(false);

            tv1.setId(R.id.decomposition_on_meaning);
            tv1_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            tv1_layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            tv1_layoutParams.addRule(RelativeLayout.BELOW,R.id.decomposition_structure);
            explanation_block.addView(tv1,tv1_layoutParams);

            tv2 = new TextView(getContext());
            tv2.setText(R.string.this_component_is_a_CJK_character);
            tv2.setTextSize(14);
            tv2.setBackgroundColor(Color.parseColor("#ffffff"));
            tv2.setAlpha((float) 0.90);
            tv2.setTextColor(Color.parseColor("#800080"));
            tv2.setTextIsSelectable(false);
            tv2.setTypeface(null, Typeface.NORMAL);
            tv2.setMovementMethod(LinkMovementMethod.getInstance());
            tv2.setSelected(false);

            tv2.setId(R.id.decomposition_on_meaning_text);
            tv2_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            tv2_layoutParams.addRule(RelativeLayout.RIGHT_OF,R.id.decomposition_on_meaning);
            tv2_layoutParams.addRule(RelativeLayout.BELOW,R.id.decomposition_structure_text);
            tv2_layoutParams.addRule(RelativeLayout.ALIGN_LEFT,R.id.decomposition_structure_text);
            explanation_block.addView(tv2,tv2_layoutParams);
            tv1_layoutParams.addRule(RelativeLayout.ALIGN_TOP,R.id.decomposition_on_meaning_text);
            //endregion
        }
        else if (!character_is_radical_or_kana && character_not_found_in_KanjiDictDatabase && !currentKanjiMainRadicalInfo.get(0).equals("")) {
            //region Display Main Radical Info only (not readings or meanings)

            tv1 = new TextView(getContext());
            tv1.setText(R.string.radical_);
            tv1.setTextSize(14);
            tv1.setTextColor(Color.parseColor("#800080"));
            tv1.setTextIsSelectable(false);
            tv1.setTypeface(null, Typeface.BOLD);
            tv1.setMovementMethod(LinkMovementMethod.getInstance());
            tv1.setSelected(false);
            tv1.setBackgroundColor(Color.parseColor("#ffffff"));
            tv1.setAlpha((float) 0.90);

            tv1.setId(R.id.decomposition_radical);
            tv1_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            tv1_layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            tv1_layoutParams.addRule(RelativeLayout.BELOW,R.id.decomposition_structure);
            explanation_block.addView(tv1,tv1_layoutParams);

            tv2 = new TextView(getContext());
            tv2.setText(currentKanjiMainRadicalInfo.get(0));
            tv2.setTextSize(14);
            tv2.setTextColor(Color.parseColor("#800080"));
            tv2.setTextIsSelectable(false);
            tv2.setTypeface(null, Typeface.NORMAL);
            tv2.setMovementMethod(LinkMovementMethod.getInstance());
            tv2.setSelected(false);
            tv2.setBackgroundColor(Color.parseColor("#ffffff"));
            tv2.setAlpha((float) 0.90);

            tv2.setId(R.id.decomposition_radical_text);
            tv2_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            tv2_layoutParams.addRule(RelativeLayout.RIGHT_OF,R.id.decomposition_radical);
            tv2_layoutParams.addRule(RelativeLayout.BELOW,R.id.decomposition_structure_text);
            tv2_layoutParams.addRule(RelativeLayout.ALIGN_LEFT,R.id.decomposition_structure_text);
            explanation_block.addView(tv2,tv2_layoutParams);
            tv1_layoutParams.addRule(RelativeLayout.ALIGN_TOP,R.id.decomposition_radical_text);
            //endregion
        }
        else if (!character_is_radical_or_kana && !character_not_found_in_KanjiDictDatabase) {
            //region Display Main Radical Info (if available)
            if (!currentKanjiMainRadicalInfo.get(0).equals("")) {

                tv1 = new TextView(getContext());
                tv1.setText(getString(R.string.radical_));
                tv1.setTextSize(14);
                tv1.setTextColor(Color.parseColor("#800080"));
                tv1.setTextIsSelectable(false);
                tv1.setTypeface(null, Typeface.BOLD);
                tv1.setMovementMethod(LinkMovementMethod.getInstance());
                tv1.setSelected(false);
                tv1.setBackgroundColor(Color.parseColor("#ffffff"));
                tv1.setAlpha((float) 0.90);

                tv1.setId(R.id.decomposition_radical);
                tv1_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                tv1_layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                tv1_layoutParams.addRule(RelativeLayout.BELOW,R.id.decomposition_structure);
                explanation_block.addView(tv1,tv1_layoutParams);

                tv2 = new TextView(getContext());
                tv2.setText(currentKanjiMainRadicalInfo.get(0));
                tv2.setTextSize(14);
                tv2.setTextColor(Color.parseColor("#800080"));
                tv2.setTextIsSelectable(false);
                tv2.setTypeface(null, Typeface.NORMAL);
                tv2.setMovementMethod(LinkMovementMethod.getInstance());
                tv2.setSelected(false);
                tv2.setBackgroundColor(Color.parseColor("#ffffff"));
                tv2.setAlpha((float) 0.90);

                tv2.setId(R.id.decomposition_radical_text);
                tv2_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                tv2_layoutParams.addRule(RelativeLayout.RIGHT_OF,R.id.decomposition_radical);
                tv2_layoutParams.addRule(RelativeLayout.BELOW,R.id.decomposition_structure_text);
                tv2_layoutParams.addRule(RelativeLayout.ALIGN_LEFT,R.id.decomposition_structure_text);
                explanation_block.addView(tv2,tv2_layoutParams);
                tv1_layoutParams.addRule(RelativeLayout.ALIGN_TOP,R.id.decomposition_radical_text);

            }
            //endregion

            //region Display On Reading of common Japanese character
            tv1 = new TextView(getContext());
            tv1.setText(getResources().getString(R.string.QueryOnReadings));
            tv1.setTextSize(14);
            tv1.setTextColor(Color.parseColor("#800080"));
            tv1.setTextIsSelectable(false);
            tv1.setTypeface(null, Typeface.BOLD);
            tv1.setMovementMethod(LinkMovementMethod.getInstance());
            tv1.setSelected(false);
            tv1.setBackgroundColor(Color.parseColor("#ffffff"));
            tv1.setAlpha((float) 0.90);

            tv1.setId(R.id.decomposition_on_reading);
            tv1_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            tv1_layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            if (currentKanjiMainRadicalInfo.get(0).equals("")) {
                tv1_layoutParams.addRule(RelativeLayout.BELOW, R.id.decomposition_structure);
            }
            else {
                tv1_layoutParams.addRule(RelativeLayout.BELOW, R.id.decomposition_radical);
            }
            explanation_block.addView(tv1,tv1_layoutParams);

            tv2 = new TextView(getContext());
            if (currentKanjiDetailedCharacteristics.get(0).equals("")) { tv2.setText("-");} else { tv2.setText(currentKanjiDetailedCharacteristics.get(0));}
            tv2.setTextSize(14);
            tv2.setTextColor(Color.parseColor("#800080"));
            tv2.setTextIsSelectable(false);
            tv2.setTypeface(null, Typeface.NORMAL);
            tv2.setMovementMethod(LinkMovementMethod.getInstance());
            tv2.setSelected(false);
            tv2.setBackgroundColor(Color.parseColor("#ffffff"));
            tv2.setAlpha((float) 0.90);

            tv2.setId(R.id.decomposition_on_reading_text);
            tv2_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            tv2_layoutParams.addRule(RelativeLayout.RIGHT_OF,R.id.decomposition_on_reading);
            if (currentKanjiMainRadicalInfo.get(0).equals("")) {
                tv2_layoutParams.addRule(RelativeLayout.BELOW, R.id.decomposition_structure_text);
                tv2_layoutParams.addRule(RelativeLayout.ALIGN_LEFT,R.id.decomposition_structure_text);
            }
            else {
                tv2_layoutParams.addRule(RelativeLayout.BELOW, R.id.decomposition_radical_text);
                tv2_layoutParams.addRule(RelativeLayout.ALIGN_LEFT,R.id.decomposition_structure_text);
            }
            explanation_block.addView(tv2,tv2_layoutParams);
            tv1_layoutParams.addRule(RelativeLayout.ALIGN_TOP,R.id.decomposition_on_reading_text);
            //endregion

            //region Display On Meaning of common Japanese character
            tv1 = new TextView(getContext());
            tv1.setText(getResources().getString(R.string.QueryOnMeaning));
            tv1.setTextSize(14);
            tv1.setTextColor(Color.parseColor("#800080"));
            tv1.setTextIsSelectable(false);
            tv1.setTypeface(null, Typeface.BOLD);
            tv1.setMovementMethod(LinkMovementMethod.getInstance());
            tv1.setSelected(false);
            tv1.setBackgroundColor(Color.parseColor("#ffffff"));
            tv1.setAlpha((float) 0.90);

            tv1.setId(R.id.decomposition_on_meaning);
            tv1_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            tv1_layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            tv1_layoutParams.addRule(RelativeLayout.BELOW,R.id.decomposition_on_reading);
            explanation_block.addView(tv1,tv1_layoutParams);

            tv2 = new TextView(getContext());
            if (currentKanjiDetailedCharacteristics.get(2).equals("")) { tv2.setText("-");} else { tv2.setText(currentKanjiDetailedCharacteristics.get(2));}
            tv2.setTextSize(14);
            tv2.setTextColor(Color.parseColor("#800080"));
            tv2.setTextIsSelectable(false);
            tv2.setTypeface(null, Typeface.NORMAL);
            tv2.setMovementMethod(LinkMovementMethod.getInstance());
            tv2.setSelected(false);
            tv2.setBackgroundColor(Color.parseColor("#ffffff"));
            tv2.setAlpha((float) 0.90);

            tv2.setId(R.id.decomposition_on_meaning_text);
            tv2_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            tv2_layoutParams.addRule(RelativeLayout.RIGHT_OF,R.id.decomposition_on_meaning);
            tv2_layoutParams.addRule(RelativeLayout.BELOW,R.id.decomposition_on_reading_text);
            tv2_layoutParams.addRule(RelativeLayout.ALIGN_LEFT,R.id.decomposition_structure_text);
            explanation_block.addView(tv2,tv2_layoutParams);
            tv1_layoutParams.addRule(RelativeLayout.ALIGN_TOP,R.id.decomposition_on_meaning_text);
            //endregion

            //region Display Kun Reading of common Japanese character
            tv1 = new TextView(getContext());
            tv1.setText(getResources().getString(R.string.QueryKunReadings));
            tv1.setTextSize(14);
            tv1.setTextColor(Color.parseColor("#800080"));
            tv1.setTextIsSelectable(false);
            tv1.setTypeface(null, Typeface.BOLD);
            tv1.setMovementMethod(LinkMovementMethod.getInstance());
            tv1.setSelected(false);
            tv1.setBackgroundColor(Color.parseColor("#ffffff"));
            tv1.setAlpha((float) 0.90);

            tv1.setId(R.id.decomposition_kun_reading);
            tv1_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            tv1_layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            tv1_layoutParams.addRule(RelativeLayout.BELOW,R.id.decomposition_on_meaning);
            explanation_block.addView(tv1,tv1_layoutParams);

            tv2 = new TextView(getContext());
            if (currentKanjiDetailedCharacteristics.get(1).equals("")) { tv2.setText("-");} else { tv2.setText(currentKanjiDetailedCharacteristics.get(1));}
            tv2.setTextSize(14);
            tv2.setTextColor(Color.parseColor("#800080"));
            tv2.setTextIsSelectable(false);
            tv2.setTypeface(null, Typeface.NORMAL);
            tv2.setMovementMethod(LinkMovementMethod.getInstance());
            tv2.setSelected(false);
            tv2.setBackgroundColor(Color.parseColor("#ffffff"));
            tv2.setAlpha((float) 0.90);

            tv2.setId(R.id.decomposition_kun_reading_text);
            tv2_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            tv2_layoutParams.addRule(RelativeLayout.RIGHT_OF,R.id.decomposition_kun_reading);
            tv2_layoutParams.addRule(RelativeLayout.BELOW,R.id.decomposition_on_meaning_text);
            tv2_layoutParams.addRule(RelativeLayout.ALIGN_LEFT,R.id.decomposition_structure_text);
            explanation_block.addView(tv2,tv2_layoutParams);
            tv1_layoutParams.addRule(RelativeLayout.ALIGN_TOP,R.id.decomposition_kun_reading_text);
            //endregion

            //region Display Kun Meaning of common Japanese character
            tv1 = new TextView(getContext());
            tv1.setText(getResources().getString(R.string.QueryKunMeaning));
            tv1.setTextSize(14);
            tv1.setTextColor(Color.parseColor("#800080"));
            tv1.setTextIsSelectable(false);
            tv1.setTypeface(null, Typeface.BOLD);
            tv1.setMovementMethod(LinkMovementMethod.getInstance());
            tv1.setSelected(false);
            tv1.setBackgroundColor(Color.parseColor("#ffffff"));
            tv1.setAlpha((float) 0.90);

            tv1.setId(R.id.decomposition_kun_meaning);
            tv1_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            tv1_layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            tv1_layoutParams.addRule(RelativeLayout.BELOW,R.id.decomposition_kun_reading);
            explanation_block.addView(tv1,tv1_layoutParams);

            tv2 = new TextView(getContext());
            if (currentKanjiDetailedCharacteristics.get(3).equals("")) { tv2.setText("-");} else { tv2.setText(currentKanjiDetailedCharacteristics.get(3));}
            tv2.setTextSize(14);
            tv2.setTextColor(Color.parseColor("#800080"));
            tv2.setTextIsSelectable(false);
            tv2.setTypeface(null, Typeface.NORMAL);
            tv2.setMovementMethod(LinkMovementMethod.getInstance());
            tv2.setSelected(false);
            tv2.setBackgroundColor(Color.parseColor("#ffffff"));
            tv2.setAlpha((float) 0.90);

            tv2.setId(R.id.decomposition_kun_meaning_text);
            tv2_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            tv2_layoutParams.addRule(RelativeLayout.RIGHT_OF,R.id.decomposition_kun_meaning);
            tv2_layoutParams.addRule(RelativeLayout.BELOW,R.id.decomposition_kun_reading_text);
            tv2_layoutParams.addRule(RelativeLayout.ALIGN_LEFT,R.id.decomposition_structure_text);
            explanation_block.addView(tv2,tv2_layoutParams);
            tv1_layoutParams.addRule(RelativeLayout.ALIGN_TOP,R.id.decomposition_kun_meaning_text);
            //endregion
        }
        else if (character_is_radical_or_kana) {
            //region Display Radical or Kana Meanings/Readings
            tv1 = new TextView(getContext());
            tv1.setText(getString(R.string.radical_));
            tv1.setTextSize(14);
            tv1.setTextColor(Color.parseColor("#800080"));
            tv1.setTextIsSelectable(false);
            tv1.setTypeface(null, Typeface.BOLD);
            tv1.setMovementMethod(LinkMovementMethod.getInstance());
            tv1.setSelected(false);
            tv1.setBackgroundColor(Color.parseColor("#ffffff"));
            tv1.setAlpha((float) 0.90);

            tv1.setId(R.id.decomposition_radical);
            tv1_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            tv1_layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            tv1_layoutParams.addRule(RelativeLayout.BELOW,R.id.decomposition_structure);
            explanation_block.addView(tv1,tv1_layoutParams);

            tv2 = new TextView(getContext());
            if (radical_row[2].equals("Hiragana") || radical_row[2].equals("Katakana")) {
                tv2.setText(radical_row[2] + " " + radical_row[3] + ".");
                tv2.setTextSize(14);
                tv2.setTextColor(Color.parseColor("#800080"));
                tv2.setTextIsSelectable(false);
                tv2.setTypeface(null, Typeface.NORMAL);
                tv2.setMovementMethod(LinkMovementMethod.getInstance());
                tv2.setSelected(false);
                tv2.setBackgroundColor(Color.parseColor("#ffffff"));
                tv2.setAlpha((float) 0.90);

                tv2.setId(R.id.decomposition_radical_text);
                tv2_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                tv2_layoutParams.addRule(RelativeLayout.RIGHT_OF,R.id.decomposition_radical);
                tv2_layoutParams.addRule(RelativeLayout.BELOW,R.id.decomposition_structure_text);
                explanation_block.addView(tv2,tv2_layoutParams);
                tv1_layoutParams.addRule(RelativeLayout.ALIGN_TOP,R.id.decomposition_radical_text);
            }
            else if (radical_row[2].equals("Special")){
                tv2.setText("Special symbol with meaning '" + radical_row[3] + "'.");
                tv2.setTextSize(14);
                tv2.setTextColor(Color.parseColor("#800080"));
                tv2.setTextIsSelectable(false);
                tv2.setTypeface(null, Typeface.NORMAL);
                tv2.setMovementMethod(LinkMovementMethod.getInstance());
                tv2.setSelected(false);
                tv2.setBackgroundColor(Color.parseColor("#ffffff"));
                tv2.setAlpha((float) 0.90);

                tv2.setId(R.id.decomposition_radical_text);
                tv2_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                tv2_layoutParams.addRule(RelativeLayout.RIGHT_OF,R.id.decomposition_radical);
                tv2_layoutParams.addRule(RelativeLayout.BELOW,R.id.decomposition_structure_text);
                explanation_block.addView(tv2,tv2_layoutParams);
                tv1_layoutParams.addRule(RelativeLayout.ALIGN_TOP,R.id.decomposition_radical_text);
            }
            else {
                //region Get the radical characteristics from the RadialsOnlyDatabase
                List<String> parsed_number = Arrays.asList(mRadicalsOnlyDatabase.get(radicalIndex)[2].split(";"));
                String[] main_radical_row = mRadicalsOnlyDatabase.get(radicalIndexOriginal);

                String strokes = " strokes.";
                if (main_radical_row[4].equals("1")) { strokes = " stroke.";}

                if (parsed_number.size()>1) {
                    if (parsed_number.get(1).equals("alt")) {
                        tv2.setText("\""+ main_radical_row[3] + "\""+ " (Radical No. " + parsed_number.get(0) + "), " + main_radical_row[4] + strokes);
                    }
                    else if (parsed_number.get(1).equals("variant")) {
                        tv2.setText("\"" + main_radical_row[3] + "\" radical variant" + " (Radical No. " + parsed_number.get(0) + ").");
                    }
                    else if (parsed_number.get(1).equals("simplification")) {
                        tv2.setText("\"" + main_radical_row[3] + "\" (Radical No. " + parsed_number.get(0) + " simplification).");
                    }
                }
                else {
                    tv2.setText("\""+ main_radical_row[3] + "\""+ " (Radical No. " + parsed_number.get(0) + "), " + main_radical_row[4] + strokes);
                }
                tv2.setTextSize(14);
                tv2.setTextColor(Color.parseColor("#800080"));
                tv2.setTextIsSelectable(false);
                tv2.setBackgroundColor(Color.parseColor("#ffffff"));
                tv2.setAlpha((float) 0.90);
                tv2.setTypeface(null, Typeface.NORMAL);
                tv2.setMovementMethod(LinkMovementMethod.getInstance());
                tv2.setSelected(false);

                tv2.setId(R.id.decomposition_radical_text);
                tv2_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                tv2_layoutParams.addRule(RelativeLayout.RIGHT_OF,R.id.decomposition_radical);
                tv2_layoutParams.addRule(RelativeLayout.BELOW,R.id.decomposition_structure_text);
                tv2_layoutParams.addRule(RelativeLayout.ALIGN_LEFT,R.id.decomposition_structure_text);
                explanation_block.addView(tv2,tv2_layoutParams);
                tv1_layoutParams.addRule(RelativeLayout.ALIGN_TOP,R.id.decomposition_radical_text);
                //endregion

                //region Get the remaining radical characteristics (readings, meanings) from the KanjiDictDatabase
                if (currentKanjiDetailedCharacteristics.size() == 0) {
                    currentKanjiDetailedCharacteristics = currentMainRadicalDetailedCharacteristics;
                }

                //region On Reading of radical
                tv1 = new TextView(getContext());
                tv1.setText(getResources().getString(R.string.QueryOnReadings));
                tv1.setTextSize(14);
                tv1.setTextColor(Color.parseColor("#800080"));
                tv1.setTextIsSelectable(false);
                tv1.setTypeface(null, Typeface.BOLD);
                tv1.setMovementMethod(LinkMovementMethod.getInstance());
                tv1.setSelected(false);
                tv1.setBackgroundColor(Color.parseColor("#ffffff"));
                tv1.setAlpha((float) 0.90);

                tv1.setId(R.id.decomposition_on_reading);
                tv1_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                tv1_layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                tv1_layoutParams.addRule(RelativeLayout.BELOW, R.id.decomposition_radical);
                explanation_block.addView(tv1,tv1_layoutParams);

                tv2 = new TextView(getContext());
                if (currentKanjiDetailedCharacteristics.get(0).equals("")) { tv2.setText("-");} else { tv2.setText(currentKanjiDetailedCharacteristics.get(0));}
                tv2.setTextSize(14);
                tv2.setTextColor(Color.parseColor("#800080"));
                tv2.setTextIsSelectable(false);
                tv2.setTypeface(null, Typeface.NORMAL);
                tv2.setMovementMethod(LinkMovementMethod.getInstance());
                tv2.setSelected(false);
                tv2.setBackgroundColor(Color.parseColor("#ffffff"));
                tv2.setAlpha((float) 0.90);

                tv2.setId(R.id.decomposition_on_reading_text);
                tv2_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                tv2_layoutParams.addRule(RelativeLayout.RIGHT_OF,R.id.decomposition_on_reading);
                tv2_layoutParams.addRule(RelativeLayout.BELOW, R.id.decomposition_radical_text);
                tv2_layoutParams.addRule(RelativeLayout.ALIGN_LEFT,R.id.decomposition_structure_text);
                explanation_block.addView(tv2,tv2_layoutParams);
                tv1_layoutParams.addRule(RelativeLayout.ALIGN_TOP,R.id.decomposition_on_reading_text);
                //endregion

                //region On Meaning of radical
                tv1 = new TextView(getContext());
                tv1.setText(getResources().getString(R.string.QueryOnMeaning));
                tv1.setTextSize(14);
                tv1.setTextColor(Color.parseColor("#800080"));
                tv1.setTextIsSelectable(false);
                tv1.setTypeface(null, Typeface.BOLD);
                tv1.setMovementMethod(LinkMovementMethod.getInstance());
                tv1.setSelected(false);
                tv1.setBackgroundColor(Color.parseColor("#ffffff"));
                tv1.setAlpha((float) 0.90);

                tv1.setId(R.id.decomposition_on_meaning);
                tv1_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                tv1_layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                tv1_layoutParams.addRule(RelativeLayout.BELOW,R.id.decomposition_on_reading);
                explanation_block.addView(tv1,tv1_layoutParams);

                tv2 = new TextView(getContext());
                if (currentKanjiDetailedCharacteristics.get(2).equals("")) { tv2.setText("-");} else { tv2.setText(currentKanjiDetailedCharacteristics.get(2));}
                tv2.setTextSize(14);
                tv2.setTextColor(Color.parseColor("#800080"));
                tv2.setTextIsSelectable(false);
                tv2.setTypeface(null, Typeface.NORMAL);
                tv2.setMovementMethod(LinkMovementMethod.getInstance());
                tv2.setSelected(false);
                tv2.setBackgroundColor(Color.parseColor("#ffffff"));
                tv2.setAlpha((float) 0.90);

                tv2.setId(R.id.decomposition_on_meaning_text);
                tv2_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                tv2_layoutParams.addRule(RelativeLayout.RIGHT_OF,R.id.decomposition_on_meaning);
                tv2_layoutParams.addRule(RelativeLayout.BELOW,R.id.decomposition_on_reading_text);
                tv2_layoutParams.addRule(RelativeLayout.ALIGN_LEFT,R.id.decomposition_structure_text);
                explanation_block.addView(tv2,tv2_layoutParams);
                tv1_layoutParams.addRule(RelativeLayout.ALIGN_TOP,R.id.decomposition_on_meaning_text);
                //endregion

                //region Kun Reading of radical
                tv1 = new TextView(getContext());
                tv1.setText(getResources().getString(R.string.QueryKunReadings));
                tv1.setTextSize(14);
                tv1.setTextColor(Color.parseColor("#800080"));
                tv1.setTextIsSelectable(false);
                tv1.setTypeface(null, Typeface.BOLD);
                tv1.setMovementMethod(LinkMovementMethod.getInstance());
                tv1.setSelected(false);
                tv1.setBackgroundColor(Color.parseColor("#ffffff"));
                tv1.setAlpha((float) 0.90);

                tv1.setId(R.id.decomposition_kun_reading);
                tv1_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                tv1_layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                tv1_layoutParams.addRule(RelativeLayout.BELOW,R.id.decomposition_on_meaning);
                explanation_block.addView(tv1,tv1_layoutParams);

                tv2 = new TextView(getContext());
                if (currentKanjiDetailedCharacteristics.get(1).equals("")) { tv2.setText("-");} else { tv2.setText(currentKanjiDetailedCharacteristics.get(1));}
                tv2.setTextSize(14);
                tv2.setTextColor(Color.parseColor("#800080"));
                tv2.setTextIsSelectable(false);
                tv2.setTypeface(null, Typeface.NORMAL);
                tv2.setMovementMethod(LinkMovementMethod.getInstance());
                tv2.setSelected(false);
                tv2.setBackgroundColor(Color.parseColor("#ffffff"));
                tv2.setAlpha((float) 0.90);

                tv2.setId(R.id.decomposition_kun_reading_text);
                tv2_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                tv2_layoutParams.addRule(RelativeLayout.RIGHT_OF,R.id.decomposition_kun_reading);
                tv2_layoutParams.addRule(RelativeLayout.BELOW,R.id.decomposition_on_meaning_text);
                tv2_layoutParams.addRule(RelativeLayout.ALIGN_LEFT,R.id.decomposition_structure_text);
                explanation_block.addView(tv2,tv2_layoutParams);
                tv1_layoutParams.addRule(RelativeLayout.ALIGN_TOP,R.id.decomposition_kun_reading_text);
                //endregion

                //region Kun Meaning of radical
                tv1 = new TextView(getContext());
                tv1.setText(getResources().getString(R.string.QueryKunMeaning));
                tv1.setTextSize(14);
                tv1.setTextColor(Color.parseColor("#800080"));
                tv1.setTextIsSelectable(false);
                tv1.setTypeface(null, Typeface.BOLD);
                tv1.setMovementMethod(LinkMovementMethod.getInstance());
                tv1.setSelected(false);
                tv1.setBackgroundColor(Color.parseColor("#ffffff"));
                tv1.setAlpha((float) 0.90);

                tv1.setId(R.id.decomposition_kun_meaning);
                tv1_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                tv1_layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                tv1_layoutParams.addRule(RelativeLayout.BELOW,R.id.decomposition_kun_reading);
                explanation_block.addView(tv1,tv1_layoutParams);

                tv2 = new TextView(getContext());
                if (currentKanjiDetailedCharacteristics.get(3).equals("")) { tv2.setText("-");} else { tv2.setText(currentKanjiDetailedCharacteristics.get(3));}
                tv2.setTextSize(14);
                tv2.setTextColor(Color.parseColor("#800080"));
                tv2.setTextIsSelectable(false);
                tv2.setTypeface(null, Typeface.NORMAL);
                tv2.setMovementMethod(LinkMovementMethod.getInstance());
                tv2.setSelected(false);
                tv2.setBackgroundColor(Color.parseColor("#ffffff"));
                tv2.setAlpha((float) 0.90);

                tv2.setId(R.id.decomposition_kun_meaning_text);
                tv2_layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                tv2_layoutParams.addRule(RelativeLayout.RIGHT_OF,R.id.decomposition_kun_meaning);
                tv2_layoutParams.addRule(RelativeLayout.BELOW,R.id.decomposition_kun_reading_text);
                tv2_layoutParams.addRule(RelativeLayout.ALIGN_LEFT,R.id.decomposition_structure_text);
                explanation_block.addView(tv2,tv2_layoutParams);
                tv1_layoutParams.addRule(RelativeLayout.ALIGN_TOP,R.id.decomposition_kun_meaning_text);
                //endregion

                //endregion
            }
            //endregion
        }
        //endregion
        //endregion
        //endregion

        overall_block_linearLayout.addView(explanation_block);
        //endregion

        mOverallBlockContainer.addView(overall_block_linearLayout);
        //endregion

        mDecompositionScrollView.fullScroll(View.FOCUS_DOWN);
        mDecompositionScrollView.post(new Runnable() {
            @Override
            public void run() {
                mDecompositionScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }
    static public String[] getStructureInfo(String requested_structure) {
        String structureText = "";
        int structureImage = 0;

        char current_char = 'a';
        char last_char = 'a';
        int starting_index = 0;
        if (requested_structure.equals("c")) {
            structureText = "Character is considered a single component.";
            if (requested_structure.equals("c")) { structureText = "Character is one of the 35 basic CJK strokes.";}
            structureImage = R.drawable.colored_structure_1_original;
        }
        else if (requested_structure.equals("refh") || requested_structure.equals("refr")) {
            structureText = "Component is reflected along a vertical axis (horizontal reflection).";
            structureImage = R.drawable.colored_structure_1_reflected_left_right;
        }
        else if (requested_structure.equals("refv")) {
            structureText = "Component is reflected along a horizontal axis (vertical reflection).";
            structureImage = R.drawable.colored_structure_1_reflected_up_down;
        }
        else if (requested_structure.equals("rot")) {
            structureText = "Component is rotated 180 degrees.";
            structureImage = R.drawable.colored_structure_1_rotation_180;
        }
        else if (requested_structure.equals("w") || requested_structure.equals("wa") || requested_structure.equals("wb") || requested_structure.equals("wbl") || requested_structure.equals("wtr")
                || requested_structure.equals("wtl") || requested_structure.equals("wbr")) {
            structureText = "Second component is located within the first and/or the components overlap.";
            structureImage = R.drawable.colored_structure_2_overlapping;
        }
        else if (requested_structure.equals("a2") || requested_structure.equals("a2m") || requested_structure.equals("a2t")) {
            structureText = "Components are arranged from left to right, and may merge together or touch.";
            structureImage = R.drawable.colored_structure_2_left_right;
        }
        else if (requested_structure.equals("d2") || requested_structure.equals("d2m")|| requested_structure.equals("d2t")) {
            structureText = "Components are arranged from top to bottom, and may merge together or touch.";
            structureImage = R.drawable.colored_structure_2_up_down;
        }
        else if (requested_structure.equals("rrefd")) {
            structureText = "Component is repeated below and its repetition is reflected along a vertical axis (horizontal reflection).";
            structureImage = R.drawable.colored_structure_2_up_down_reflection_up_down;
        }
        else if (requested_structure.equals("rrefl")) {
            structureText = "Component is repeated to the left and its repetition is reflected along a vertical axis (horizontal reflection).";
            structureImage = R.drawable.colored_structure_2_left_right_reflection_on_left;
        }
        else if (requested_structure.equals("rrefr")) {
            structureText = "Component is repeated to the right and its repetition is reflected along a vertical axis (horizontal reflection).";
            structureImage = R.drawable.colored_structure_2_left_right_reflection_on_right;
        }
        else if (requested_structure.equals("rrotd")) {
            structureText = "Component is repeated below and its repetition is reflected along a horizontal axis (vertical reflection).";
            structureImage = R.drawable.colored_structure_2_up_down_rotation_180_on_bottom;
        }
        else if (requested_structure.equals("rrotu")) {
            structureText = "Component is repeated below and its repetition is reflected along a vertical axis (horizontal reflection).";
            structureImage = R.drawable.colored_structure_2_up_down_rotation_180_on_top;
        }
        else if (requested_structure.equals("rrotr")) {
            structureText = "Component is repeated to the right and its repetition is rotated 180 degrees.";
            structureImage = R.drawable.colored_structure_2_left_right_rotation_180_on_right;
        }
        else if (requested_structure.equals("a3")) {
            structureText = "Components are arranged from left to right, and may merge together or touch.";
            structureImage = R.drawable.colored_structure_3_left_center_right;
        }
        else if (requested_structure.equals("a4")) {
            structureText = "Components are arranged from left to right, and may merge together or touch.";
            structureImage = R.drawable.colored_structure_4_left_right;
        }
        else if (requested_structure.equals("d3")) {
            structureText = "Components are arranged from top to bottom, and may merge together or touch.";
            structureImage = R.drawable.colored_structure_3_up_center_down;
        }
        else if (requested_structure.equals("d4")) {
            structureText = "Components are arranged from top to bottom, and may merge together or touch.";
            structureImage = R.drawable.colored_structure_4_up_down;
        }
        else if (requested_structure.equals("r3gw")) {
            structureText = "Component is repeated three times in a triangle pointing downwards.";
            structureImage = R.drawable.colored_structure_3_downwards_triangle;
        }
        else if (requested_structure.equals("r3tr")) {
            structureText = "Component is repeated three times in a triangle pointing upwards.";
            structureImage = R.drawable.colored_structure_3_upwards_triangle;
        }
        else if (requested_structure.equals("r4sq")) {
            structureText = "Component is repeated four times in a square or losange";
            structureImage = R.drawable.colored_structure_4_square;
        }
        else if (requested_structure.equals("r5")) {
            structureText = "Component is repeated five times.";
            structureImage = R.drawable.colored_structure_5_losange;
        }
        else if (requested_structure.equals("s")) {
            structureText = "First component encloses the others.";
            structureImage = R.drawable.colored_structure_2_outlining;
        }
        else if (requested_structure.equals("sb")) {
            structureText = "First component surrounds the others along the bottom.";
            structureImage = R.drawable.colored_structure_2_enclosing_bottom_to_top;
        }
        else if (requested_structure.equals("sbl")) {
            structureText = "First component surrounds the others along the bottom left.";
            structureImage = R.drawable.colored_structure_2_enclosing_bottomleft_to_topright;
        }
        else if (requested_structure.equals("sbr")) {
            structureText = "First component surrounds the others along the bottom right.";
            structureImage = R.drawable.colored_structure_2_enclosing_bottomright_to_topleft;
        }
        else if (requested_structure.equals("sl")) {
            structureText = "First component surrounds the others along the left.";
            structureImage = R.drawable.colored_structure_2_enclosing_left_to_right;
        }
        else if (requested_structure.equals("sr")) {
            structureText = "First component surrounds the others along the right.";
            structureImage = R.drawable.colored_structure_2_enclosing_right_to_left;
        }
        else if (requested_structure.equals("st") || requested_structure.equals("r3st")) {
            structureText = "First component surrounds the others along the top.";
            structureImage = R.drawable.colored_structure_2_enclosing_top_to_bottom;
        }
        else if (requested_structure.equals("stl") || requested_structure.equals("r3stl")) {
            structureText = "First component surrounds the others along the top left.";
            structureImage = R.drawable.colored_structure_2_enclosing_topleft_to_bottomright;
        }
        else if (requested_structure.equals("str") || requested_structure.equals("r3str")) {
            structureText = "First component surrounds the others along the top right.";
            structureImage = R.drawable.colored_structure_2_enclosing_topright_to_bottomleft;
        }


        if (structureText.equals("")) { structureText = "**No descriptor found**"; }

        String[] structure = {structureText,Integer.toString(structureImage)};
        return structure;
    }

    private void startGettingDecompositionAsynchronously(String inputQuery, int radicalIteration) {
        if (getActivity()!=null) {
            LoaderManager loaderManager = getActivity().getSupportLoaderManager();
            Loader<String> roomDbSearchLoader = loaderManager.getLoader(ROOM_DB_KANJI_DECOMPOSITION_LOADER);
            Bundle bundle = new Bundle();
            bundle.putString(getString(R.string.decomposition_query), inputQuery);
            bundle.putInt(getString(R.string.decomposition_radical_iteration),radicalIteration);
            if (roomDbSearchLoader == null) loaderManager.initLoader(ROOM_DB_KANJI_DECOMPOSITION_LOADER, bundle, this);
            else loaderManager.restartLoader(ROOM_DB_KANJI_DECOMPOSITION_LOADER, bundle, this);
        }
    }
    private void showLoadingIndicator() {
        if (mProgressBarLoadingIndicator!=null) mProgressBarLoadingIndicator.setVisibility(View.VISIBLE);
    }
    private void hideLoadingIndicator() {
        if (mProgressBarLoadingIndicator!=null) mProgressBarLoadingIndicator.setVisibility(View.INVISIBLE);
    }

    //Asynchronous methods
    @NonNull @Override public Loader<Object> onCreateLoader(int id, final Bundle args) {

        if (id == ROOM_DB_KANJI_DECOMPOSITION_LOADER) {
            mAlreadyGotFirstQuery = false;
            showLoadingIndicator();
            String firstQuery = args.getString(getString(R.string.decomposition_query));
            int radicalIteration = args.getInt(getString(R.string.decomposition_radical_iteration));
            KanjiCharacterDecompositionAsyncTaskLoader decompositionLoader = new KanjiCharacterDecompositionAsyncTaskLoader(getContext(), firstQuery, radicalIteration, mRadicalsOnlyDatabase);
            return decompositionLoader;
        }
        else return new KanjiCharacterDecompositionAsyncTaskLoader(getContext(), "", 0, null);
    }
    @Override public void onLoadFinished(@NonNull Loader<Object> loader, Object data) {

        if (loader.getId() == ROOM_DB_KANJI_DECOMPOSITION_LOADER && !mAlreadyGotFirstQuery && data!=null) {
            mAlreadyGotFirstQuery = true;

            Object[] dataElements = (Object[]) data;

            // Search for the input in the database and retrieve the result's characteristics
            List<List<String>> decomposedKanji = (List<List<String>>) dataElements[0];
            List<String> currentKanjiDetailedCharacteristics = (List<String>) dataElements[1];
            List<String> currentKanjiMainRadicalInfo = (List<String>) dataElements[2];
            String inputQuery = (String) dataElements[3];
            int radical_iteration = (int) dataElements[4];
            int radicalIndex = (int) dataElements[5];
            int radicalIndexOriginal = (int) dataElements[6];
            List<String> currentMainRadicalDetailedCharacteristics = (List<String>) dataElements[7];
            hideLoadingIndicator();

            displayDecomposition(inputQuery.substring(0,1),
                    radical_iteration,
                    decomposedKanji,
                    currentKanjiDetailedCharacteristics,
                    currentKanjiMainRadicalInfo,
                    radicalIndex,
                    radicalIndexOriginal,
                    currentMainRadicalDetailedCharacteristics);

            if (getLoaderManager()!=null) getLoaderManager().destroyLoader(ROOM_DB_KANJI_DECOMPOSITION_LOADER);
        }
    }
    @Override public void onLoaderReset(@NonNull Loader<Object> loader) {}
    private static class KanjiCharacterDecompositionAsyncTaskLoader extends AsyncTaskLoader<Object> {

        //region Parameters
        private JapaneseToolboxKanjiRoomDatabase mJapaneseToolboxKanjiRoomDatabase;
        private final String inputQuery;
        private KanjiCharacter mCurrentKanjiCharacter;
        private final List<String[]> mRadicalsOnlyDatabase;
        private final int radicalIteration;
        //endregion

        KanjiCharacterDecompositionAsyncTaskLoader(Context context, String inputQuery, int radicalIteration, List<String[]> mRadicalsOnlyDatabase) {
            super(context);
            this.inputQuery = inputQuery;
            this.radicalIteration = radicalIteration;
            this.mRadicalsOnlyDatabase = mRadicalsOnlyDatabase;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Override
        public Object loadInBackground() {

            mJapaneseToolboxKanjiRoomDatabase = JapaneseToolboxKanjiRoomDatabase.getInstance(getContext());

            // Search for the input in the database and retrieve the result's characteristics

            List<List<String>> decomposedKanji = Decomposition(inputQuery.substring(0,1));
            List<String> currentKanjiDetailedCharacteristics = getKanjiDetailedCharacteristics(mCurrentKanjiCharacter);
            List<String> currentKanjiMainRadicalInfo = getKanjiRadicalCharacteristics(mCurrentKanjiCharacter);
            Object[] radicalInfo = getRadicalInfo();

            return new Object[] {
                    decomposedKanji,
                    currentKanjiDetailedCharacteristics,
                    currentKanjiMainRadicalInfo,
                    inputQuery,
                    radicalIteration,
                    radicalInfo[0],
                    radicalInfo[1],
                    radicalInfo[2]
            };
        }

        List<List<String>> Decomposition(String word) {

            String concatenated_input = Utilities.removeSpecialCharacters(word);
            String inputHexIdentifier = DatabaseUtilities.convertToUTF8(concatenated_input).toUpperCase();
            mCurrentKanjiCharacter = mJapaneseToolboxKanjiRoomDatabase.getKanjiCharactersByHexId(inputHexIdentifier);
            //mCurrentKanjiIndex = Collections.binarySearch(mKanjiCharacters, new KanjiCharacter(inputHexIdentifier), KanjiCharacter.hexIdentiferComparatorAscending);

            List<List<String>> decomposedKanji = new ArrayList<>();
            List<String> kanji_and_its_structure = new ArrayList<>();
            List<String> components_and_their_structure = new ArrayList<>();

            //If decompositions don't exist in the database, then this is a basic character
            if (mCurrentKanjiCharacter ==null) {
                kanji_and_its_structure.add(word);
                kanji_and_its_structure.add("c");
                decomposedKanji.add(kanji_and_its_structure);
            }

            //Otherwise, get the decompositions
            else {

                kanji_and_its_structure.add(getStringFromUTF8(mCurrentKanjiCharacter.getHexIdentifier()));
                kanji_and_its_structure.add(mCurrentKanjiCharacter.getStructure());
                decomposedKanji.add(kanji_and_its_structure);

                List<String> parsedComponents = Arrays.asList(mCurrentKanjiCharacter.getComponents().split(";"));

                String current_component;
                List<List<String>> newDecomposition = new ArrayList<>();

                for (int i = 0; i < parsedComponents.size() ; i++) {
                    current_component = parsedComponents.get(i);
                    components_and_their_structure = new ArrayList<>();

                    if (current_component.length()>0) {
                        if ((current_component.charAt(0) == '0' || current_component.charAt(0) == '1' || current_component.charAt(0) == '2' ||
                                current_component.charAt(0) == '3' || current_component.charAt(0) == '4' || current_component.charAt(0) == '5' ||
                                current_component.charAt(0) == '6' || current_component.charAt(0) == '7' || current_component.charAt(0) == '8' ||
                                current_component.charAt(0) == '9')) {

                            newDecomposition = Decomposition(current_component);

                            // Update the component structures to include the master structure
                            for (int j=1;j<newDecomposition.size();j++) { newDecomposition.get(j).set(1,newDecomposition.get(j).get(1));}

                            // Remove the first List<String> from newDecomposition so that only the decomposed components may be added to decomposedKanji
                            newDecomposition.remove(0);
                            decomposedKanji.addAll(newDecomposition);
                        }
                        else {
                            components_and_their_structure.add(current_component);
                            components_and_their_structure.add("");
                            decomposedKanji.add(components_and_their_structure);
                        }
                    }
                }
            }

            return decomposedKanji;
        }
        String getStringFromUTF8(String word) {

            String hex = word.substring(2,word.length());
            ByteBuffer buff = ByteBuffer.allocate(hex.length()/2);
            for (int i = 0; i < hex.length(); i+=2) {
                buff.put((byte)Integer.parseInt(hex.substring(i, i+2), 16));
            }
            buff.rewind();
            Charset cs = Charset.forName("UTF-8");
            CharBuffer cb = cs.decode(buff);
            String string_value_of_hex = cb.toString();

            return string_value_of_hex;
        }
        List<String> getKanjiDetailedCharacteristics(KanjiCharacter kanjiCharacter) {

            List<String> characteristics = new ArrayList<>();
            if (kanjiCharacter ==null || kanjiCharacter.getReadings()==null) return characteristics;

            List<String> parsed_list1 = Arrays.asList(kanjiCharacter.getReadings().split(";"));
            if (parsed_list1.size()==0) { parsed_list1 = new ArrayList<>(); parsed_list1.add(""); parsed_list1.add("");}
            characteristics.add(parsed_list1.get(0));
            characteristics.add(parsed_list1.get(1));

            List<String> parsed_list2 = Arrays.asList(kanjiCharacter.getMeanings().split(";"));
            if (parsed_list2.size()==0) { parsed_list2 = new ArrayList<>(); parsed_list2.add(""); parsed_list2.add("");}
            else if (parsed_list2.size()==1) { parsed_list2 = new ArrayList<>(); parsed_list2.add(""); parsed_list2.add(""); }
            characteristics.add(parsed_list2.get(0));
            if (parsed_list2.size()==2 && parsed_list2.get(1).equals("IDEM")) { parsed_list2.set(1,parsed_list2.get(0)); }
            characteristics.add(parsed_list2.get(1));
            return characteristics;
        }
        List<String> getKanjiRadicalCharacteristics(KanjiCharacter kanjiCharacter) {

            List<String> radical_characteristics = new ArrayList<>();

            if (kanjiCharacter ==null || kanjiCharacter.getRadPlusStrokes()==null) {
                radical_characteristics.add("");
            }
            else {
                List<String> parsed_list = Arrays.asList(kanjiCharacter.getRadPlusStrokes().split("\\+"));

                if (parsed_list.size()>1) {
                    if (!parsed_list.get(1).equals("0")) {
                        int radical_index = -1;
                        for (int i = 0; i < mRadicalsOnlyDatabase.size(); i++) {
                            if (parsed_list.get(0).equals(mRadicalsOnlyDatabase.get(i)[2])) {
                                radical_index = i;
                                break;
                            }
                        }
                        String text = "";
                        if (radical_index != -1) {
                            text = "Character's main radical is " +
                                    mRadicalsOnlyDatabase.get(radical_index)[0] +
                                    " (No. " +
                                    parsed_list.get(0) +
                                    ") with " +
                                    parsed_list.get(1) +
                                    " additional strokes.";
                        }
                        radical_characteristics.add(text);
                    }
                    else {radical_characteristics.add("");}
                }
                else {radical_characteristics.add("");}

            }
            return radical_characteristics;
        }
        Object[] getRadicalInfo() {

            int radicalIndex = -1;
            int mainRadicalIndex = 0;
            List<String> currentMainRadicalDetailedCharacteristics = new ArrayList<>();

            //Find the radical index
            for (int i = 0; i< mRadicalsOnlyDatabase.size(); i++) {
                if (inputQuery.equals(mRadicalsOnlyDatabase.get(i)[0])) {
                    radicalIndex = i;
                }
            }

            if (radicalIndex >= 0) {
                List<String> parsed_number = Arrays.asList(mRadicalsOnlyDatabase.get(radicalIndex)[2].split(";"));
                Boolean found_main_radical = false;
                mainRadicalIndex = radicalIndex;

                if (parsed_number.size() > 1) {
                    while (!found_main_radical) {
                        if (mRadicalsOnlyDatabase.get(mainRadicalIndex)[2].contains(";")) {
                            mainRadicalIndex--;
                        } else {
                            found_main_radical = true;
                        }
                    }
                }

                //Get the remaining radical characteristics (readings, meanings) from the KanjiDictDatabase
                String mainRadical = mRadicalsOnlyDatabase.get(mainRadicalIndex)[0];
                String radicalHexIdentifier = DatabaseUtilities.convertToUTF8(mainRadical).toUpperCase();
                KanjiCharacter kanjiCharacter = mJapaneseToolboxKanjiRoomDatabase.getKanjiCharactersByHexId(radicalHexIdentifier);
                currentMainRadicalDetailedCharacteristics = getKanjiDetailedCharacteristics(kanjiCharacter);

            }
            return new Object[]{
                    radicalIndex,
                    mainRadicalIndex,
                    currentMainRadicalDetailedCharacteristics
            };
        }

    }
}
