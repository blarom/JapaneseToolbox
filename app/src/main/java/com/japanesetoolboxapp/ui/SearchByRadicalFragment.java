package com.japanesetoolboxapp.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.japanesetoolboxapp.R;
import com.japanesetoolboxapp.adapters.KanjiGridRecyclerViewAdapter;
import com.japanesetoolboxapp.adapters.StructuresGridViewAdapter;
import com.japanesetoolboxapp.data.JapaneseToolboxKanjiRoomDatabase;
import com.japanesetoolboxapp.data.KanjiCharacter;
import com.japanesetoolboxapp.data.KanjiComponent;
import com.japanesetoolboxapp.resources.GlobalConstants;
import com.japanesetoolboxapp.resources.MainApplication;
import com.japanesetoolboxapp.resources.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class SearchByRadicalFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Object>,
        KanjiGridRecyclerViewAdapter.ComponentClickHandler {


    //region Parameters
    @BindView(R.id.search_by_radical_loading_indicator) ProgressBar mProgressBarLoadingIndicator;
    @BindView(R.id.search_by_radical_overall_structure_button) Button mOverallStructureButton;
    @BindView(R.id.search_by_radicals_overall_block_container) LinearLayout mOverallBlockContainerLinearLayout;
    @BindView(R.id.search_by_radical_elementA) EditText mElementAEditText;
    @BindView(R.id.search_by_radical_elementB) EditText mElementBEditText;
    @BindView(R.id.search_by_radical_elementC) EditText mElementCEditText;
    @BindView(R.id.search_by_radical_elementD) EditText mElementDEditText;
    @BindView(R.id.search_by_radical_selection_grid_container) LinearLayout mSelectionGridContainerLinearLayout;
    @BindView(R.id.search_by_radical_character_descriptor) EditText mCharacterDescriptorEditText;
    @BindView(R.id.search_by_radical_requested_component_structure) Button mComponentStructureButton;
    @BindView(R.id.search_by_radical_selection_grid) RecyclerView mSelectionGridRecyclerView;
    @BindView(R.id.search_by_radical_selection_grid_no_elements_textview) TextView mNoSelectionElementsTextView;
    @BindView(R.id.search_by_radical_results_grid_container) LinearLayout mResultsGridContainerLinearLayout;
    @BindView(R.id.search_by_radical_results_grid) RecyclerView mResultsGridRecyclerView;
    @BindView(R.id.search_by_radical_selection_grid_no_results_textview) TextView mNoResultsTextView;
    private Unbinder mBinding;
    private static final int MAX_RECYCLERVIEW_HEIGHT_DP = 320;
    private static final int ROOM_DB_COMPONENT_GRID_LOADER = 5684;
    public static final int ROOM_DB_COMPONENT_GRID_FILTER_LOADER = 4682;
    private static final int ROOM_DB_KANJI_SEARCH_LOADER = 9512;
    int mSelectedOverallStructure;
    int mSelectedComponentStructure;
    String mComponentSelectionType;
    String[] user_selections;
    List<String> mDisplayableComponentSelections;
    private String mInputQuery;
    private List<String[]> mRadicalsOnlyDatabase;
    private boolean mAlreadyLoadedSelectionGrid;
    private boolean mAlreadyLoadedKanjiSearchResults;
    private String mKanjiCharacterNameForFilter;
    private boolean mAlreadyFilteredSelectionGrid;
    private int mNumberOfComponentGridColumns;
    private List<String> mUnfilteredDisplayableComponentSelections;
    private List<String[]> mSimilarsDatabase;
    private int mSelectedEditTextId;
    private int mSelectedOverallStructureId;
    private int mTempSelectedStructureId;
    private List<String> mPrintableSearchResults;
    private KanjiGridRecyclerViewAdapter mComponentsGridAdapter;
    private KanjiGridRecyclerViewAdapter mResultsGridAdapter;
    private int mNumberOfResultGridColumns;
    private String mSelectedComponent;
    private int mMaxRecyclerViewHeightPixels;
    //endregion


    //Lifecycle methods
    @Override public void onAttach(Context context) {
        super.onAttach(context);

        searchByRadicalFragmentOperationsHandler = (SearchByRadicalFragmentOperationsHandler) context;
    }
    @Override public void onCreate(Bundle savedInstanceState) { //instead of onActivityCreated
        super.onCreate(savedInstanceState);

        getExtras();
        initializeParameters();
    }
    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_search_by_radical, container, false);

        //setRetainInstance(true);

        initializeViews(rootView);

        updateInputElements(mInputQuery);
        hideAllSections();
        return rootView;
    }
    @Override public void onResume() {
        super.onResume();
        if (getActivity()!=null) Utilities.hideSoftKeyboard(getActivity());
    }
    @Override public void onStart() {
        super.onStart();

    }
    @Override public void onDetach() {
        super.onDetach();
        if (getLoaderManager()!=null) getLoaderManager().destroyLoader(ROOM_DB_COMPONENT_GRID_LOADER);
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
        if (getActivity()!=null && MainApplication.getRefWatcher(getActivity())!=null) MainApplication.getRefWatcher(getActivity()).watch(this);
    }


    // Functionality Functions
    private void getExtras() {
        if (getArguments()!=null) {
            mInputQuery = getArguments().getString(getString(R.string.user_query_word));
            mRadicalsOnlyDatabase = (List<String[]>) getArguments().getSerializable(getString(R.string.rad_only_database));
            mSimilarsDatabase = (List<String[]>) getArguments().getSerializable(getString(R.string.similars_database));
        }
    }
    private void initializeParameters() {
        mAlreadyLoadedSelectionGrid = false;
        mSelectedComponent = "";
    }
    private void initializeViews(View rootView) {

        mBinding = ButterKnife.bind(this, rootView);

        //region Setting the Element listeners
        mElementAEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (getActivity()!=null) Utilities.hideSoftKeyboard(getActivity());
                if (hasFocus) mSelectedEditTextId = mElementAEditText.getId();
                drawBorderAroundThisEditText(mElementAEditText);
            }
        });
        mElementAEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSelectedEditTextId = mElementAEditText.getId();
                drawBorderAroundThisEditText(mElementAEditText);
            }
        });
        mElementAEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    if (getActivity()!=null) Utilities.hideSoftKeyboard(getActivity());
                }
                return false;
            }
        });

        mElementBEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (getActivity()!=null) Utilities.hideSoftKeyboard(getActivity());
                if (hasFocus) mSelectedEditTextId = mElementBEditText.getId();
                drawBorderAroundThisEditText(mElementBEditText);
            }
        });
        mElementBEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSelectedEditTextId = mElementBEditText.getId();
                drawBorderAroundThisEditText(mElementBEditText);
            }
        });
        mElementBEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    if (getActivity()!=null) Utilities.hideSoftKeyboard(getActivity());
                }
                return false;
            }
        });

        mElementCEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (getActivity()!=null) Utilities.hideSoftKeyboard(getActivity());
                if (hasFocus) mSelectedEditTextId = mElementCEditText.getId();
                drawBorderAroundThisEditText(mElementCEditText);
            }
        });
        mElementCEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSelectedEditTextId = mElementCEditText.getId();
                drawBorderAroundThisEditText(mElementCEditText);
            }
        });
        mElementCEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    if (getActivity()!=null) Utilities.hideSoftKeyboard(getActivity());
                }
                return false;
            }
        });

        mElementDEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (getActivity()!=null) Utilities.hideSoftKeyboard(getActivity());
                if (hasFocus) mSelectedEditTextId = mElementDEditText.getId();
                drawBorderAroundThisEditText(mElementDEditText);
            }
        });
        mElementDEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSelectedEditTextId = mElementDEditText.getId();
                drawBorderAroundThisEditText(mElementDEditText);
            }
        });
        mElementDEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    if (getActivity()!=null) Utilities.hideSoftKeyboard(getActivity());
                }
                return false;
            }
        });

        mSelectedEditTextId = mElementAEditText.getId();
        drawBorderAroundThisEditText(mElementAEditText);
        //endregion

        //region Setting the number of grid columns
        mNumberOfComponentGridColumns = 7;
        mNumberOfResultGridColumns = 7;
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;
            if (width<1000) {
                mNumberOfComponentGridColumns = 5;
                mNumberOfResultGridColumns = 5;
            }
        }
        //mSelectionGridView.setNumColumns(mNumberOfComponentGridColumns);
        //endregion

        //region Initializing the component structure value
        mTempSelectedStructureId = R.drawable.colored_structure_2_overlapping;
        mSelectedOverallStructureId = mTempSelectedStructureId;
        mSelectedOverallStructure = setCategoryBasedOnSelectedStructureId(mSelectedOverallStructureId);
        mSelectedComponentStructure = setCategoryBasedOnSelectedStructureId(R.drawable.colored_structure_2_left_right);
        //endregion

        //Setting the recyclerview height depending on the devcies'display density
        mMaxRecyclerViewHeightPixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MAX_RECYCLERVIEW_HEIGHT_DP, getResources().getDisplayMetrics());
    }
    private void drawBorderAroundThisEditText(EditText editText) {

        if (mElementAEditText==null || mElementBEditText==null || mElementCEditText==null || mElementDEditText==null) return;

        mElementAEditText.setBackgroundResource(0);
        mElementBEditText.setBackgroundResource(0);
        mElementCEditText.setBackgroundResource(0);
        mElementDEditText.setBackgroundResource(0);

        if (editText.getId() == mElementAEditText.getId()) mElementAEditText.setBackgroundResource(R.drawable.border_background);
        else if (editText.getId() == mElementBEditText.getId()) mElementBEditText.setBackgroundResource(R.drawable.border_background);
        else if (editText.getId() == mElementCEditText.getId()) mElementCEditText.setBackgroundResource(R.drawable.border_background);
        else if (editText.getId() == mElementDEditText.getId()) mElementDEditText.setBackgroundResource(R.drawable.border_background);
    }
    private void updateInputElements(String inputQuery) {

        //region Creating the user selections list, filling it with the user input query, and updating the input elements
        user_selections = new String[4];
        for (int i=0; i<4; i++) { user_selections[i] = ""; }

        inputQuery = Utilities.removeSpecialCharacters(inputQuery);
        int userSelectionIndex = 0;
        String currentChar;
        String text_type;
        for (int i=0; i<inputQuery.length(); i++) {
            currentChar = mInputQuery.substring(i,i+1);
            text_type = ConvertFragment.getTextType(currentChar);
            if (text_type.equals("kanji")) {
                user_selections[userSelectionIndex] = currentChar;
                userSelectionIndex++;
            }
            if (userSelectionIndex==4) break;
        }

        if (!user_selections[0].equals("")) mElementAEditText.setText(user_selections[0]);
        if (!user_selections[1].equals("")) mElementBEditText.setText(user_selections[1]);
        if (!user_selections[2].equals("")) mElementCEditText.setText(user_selections[2]);
        if (!user_selections[3].equals("")) mElementDEditText.setText(user_selections[3]);
        //endregion
    }
    private void showStructuresDialog(final String type) {

        if (getContext()==null) return;

        //region Get the dialog view
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_kanji_structures, null);
        final GridView structuresGrid = dialogView.findViewById(R.id.dialog_kanji_structures_gridview);
        final TextView requestedStructure = dialogView.findViewById(R.id.dialog_kanji_structures_requested_structure);
        //endregion

        //region Populating the grid
        final List<Integer> structureIds = new ArrayList<>();
        if (type.equals("overall")) structureIds.add(R.drawable.colored_structure_2_overlapping);
        structureIds.add(R.drawable.colored_structure_2_left_right);
        structureIds.add(R.drawable.colored_structure_3_left_center_right);
        structureIds.add(R.drawable.colored_structure_4_left_right);
        structureIds.add(R.drawable.colored_structure_2_up_down);
        structureIds.add(R.drawable.colored_structure_3_up_center_down);
        structureIds.add(R.drawable.colored_structure_4_up_down);
        structureIds.add(R.drawable.colored_structure_2_enclosing_topleft_to_bottomright);
        structureIds.add(R.drawable.colored_structure_2_enclosing_top_to_bottom);
        structureIds.add(R.drawable.colored_structure_2_enclosing_topright_to_bottomleft);
        structureIds.add(R.drawable.colored_structure_2_enclosing_left_to_right);
        structureIds.add(R.drawable.colored_structure_2_outlining);
        structureIds.add(R.drawable.colored_structure_2_enclosing_bottomleft_to_topright);
        structureIds.add(R.drawable.colored_structure_2_enclosing_bottom_to_top);
        structureIds.add(R.drawable.colored_structure_3_upwards_triangle);
        structureIds.add(R.drawable.colored_structure_4_square_repeat);
        structureIds.add(R.drawable.colored_structure_4_square);

        Drawable image = getContext().getResources().getDrawable(structureIds.get(0));
        requestedStructure.setCompoundDrawablesWithIntrinsicBounds(null, null, image, null);

        StructuresGridViewAdapter gridAdapter = new StructuresGridViewAdapter(getContext(), R.layout.list_item_structures_grid, structureIds);
        structuresGrid.setAdapter(gridAdapter);
        structuresGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
                mTempSelectedStructureId = structureIds.get(pos);
                Drawable image = getContext().getResources().getDrawable(mTempSelectedStructureId);
                requestedStructure.setCompoundDrawablesWithIntrinsicBounds(null, null, image, null);
            }
        });
        //endregion

        //region Building the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        if (type.equals("overall")) builder.setTitle("Choose the overall structure");
        else builder.setTitle("Choose the component's structure");

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                if (mTempSelectedStructureId==0) return;

                Drawable image = getContext().getResources().getDrawable(mTempSelectedStructureId);

                if (type.equals("overall")) {
                    mSelectedOverallStructureId = mTempSelectedStructureId;
                    mSelectedOverallStructure = setCategoryBasedOnSelectedStructureId(mSelectedOverallStructureId);
                    mOverallStructureButton.setCompoundDrawablesWithIntrinsicBounds(null, null, image, null);
                }
                else {
                    mSelectedComponentStructure = setCategoryBasedOnSelectedStructureId(mTempSelectedStructureId);
                    mComponentStructureButton.setCompoundDrawablesWithIntrinsicBounds(null, null, image, null);
                    startCreatingComponentKanjiGridElementsAsynchronously();
                }
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        //endregion

        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private int setCategoryBasedOnSelectedStructureId(int selectedStructureId) {

        switch (selectedStructureId) {
            case R.drawable.colored_structure_2_overlapping: return GlobalConstants.Index_full;
            case R.drawable.colored_structure_2_left_right: return GlobalConstants.Index_across2;
            case R.drawable.colored_structure_3_left_center_right: return GlobalConstants.Index_across3;
            case R.drawable.colored_structure_4_left_right: return GlobalConstants.Index_across4;
            case R.drawable.colored_structure_2_up_down: return GlobalConstants.Index_down2;
            case R.drawable.colored_structure_3_up_center_down: return GlobalConstants.Index_down3;
            case R.drawable.colored_structure_4_up_down: return GlobalConstants.Index_down4;
            case R.drawable.colored_structure_2_enclosing_topleft_to_bottomright: return GlobalConstants.Index_topleftout;
            case R.drawable.colored_structure_2_enclosing_top_to_bottom: return GlobalConstants.Index_topout;
            case R.drawable.colored_structure_2_enclosing_topright_to_bottomleft: return GlobalConstants.Index_toprightout;
            case R.drawable.colored_structure_2_enclosing_left_to_right: return GlobalConstants.Index_leftout;
            case R.drawable.colored_structure_2_outlining: return GlobalConstants.Index_fullout;
            case R.drawable.colored_structure_2_enclosing_bottomleft_to_topright: return GlobalConstants.Index_bottomleftout;
            case R.drawable.colored_structure_2_enclosing_bottom_to_top: return GlobalConstants.Index_bottomout;
            case R.drawable.colored_structure_3_upwards_triangle: return GlobalConstants.Index_three_repeat;
            case R.drawable.colored_structure_4_square_repeat: return GlobalConstants.Index_four_repeat;
            case R.drawable.colored_structure_4_square: return GlobalConstants.Index_foursquare;
            case R.drawable.colored_structure_5_hourglass: return GlobalConstants.Index_five_repeat;
            default: return 0;
        }
    }
    private void handleComponentSelection(boolean enterPressed) {

        if (!enterPressed) {
            mSelectionGridContainerLinearLayout.setVisibility(View.GONE);
            if (getActivity()!=null) Utilities.hideSoftKeyboard(getActivity());
            mSelectionGridRecyclerView.setAdapter(null);
        }

        if (!enterPressed || getView()==null) return;
        EditText edittext = getView().findViewById(mSelectedEditTextId);
        edittext.setText(mSelectedComponent);
    }
    private void showLoadingIndicator() {
        if (mProgressBarLoadingIndicator!=null) mProgressBarLoadingIndicator.setVisibility(View.VISIBLE);
    }
    private void hideLoadingIndicator() {
        if (mProgressBarLoadingIndicator!=null) mProgressBarLoadingIndicator.setVisibility(View.INVISIBLE);
    }
    private void showComponentsSelectionSection() {
        mResultsGridRecyclerView.setAdapter(null);
        if (mComponentSelectionType.equals("component")) mComponentStructureButton.setVisibility(View.VISIBLE);
        else mComponentStructureButton.setVisibility(View.GONE);
        mSelectionGridContainerLinearLayout.setVisibility(View.VISIBLE);
        mResultsGridContainerLinearLayout.setVisibility(View.GONE);
        mSelectionGridRecyclerView.setVisibility(View.GONE);
        mNoSelectionElementsTextView.setVisibility(View.GONE);
    }
    private void showResultsSection() {
        mSelectionGridRecyclerView.setAdapter(null);
        mSelectionGridContainerLinearLayout.setVisibility(View.GONE);
        mResultsGridContainerLinearLayout.setVisibility(View.VISIBLE);
        mResultsGridRecyclerView.setVisibility(View.GONE);
        mNoResultsTextView.setVisibility(View.GONE);
    }
    private void hideAllSections() {
        mSelectionGridRecyclerView.setAdapter(null);
        mResultsGridRecyclerView.setAdapter(null);
        mSelectionGridContainerLinearLayout.setVisibility(View.GONE);
        mResultsGridContainerLinearLayout.setVisibility(View.GONE);
    }
    private void showNoComponentsTextInsteadOfComponentsGrid() {
        mSelectionGridRecyclerView.setAdapter(null);
        mSelectionGridRecyclerView.setVisibility(View.GONE);
        mNoSelectionElementsTextView.setVisibility(View.VISIBLE);
    }
    private void showNoResultsTextInsteadOfResultsGrid(String text) {
        mResultsGridRecyclerView.setAdapter(null);
        mResultsGridRecyclerView.setVisibility(View.GONE);
        mNoResultsTextView.setVisibility(View.VISIBLE);
        mNoResultsTextView.setText(text);
    }
    private void showComponentsGrid() {
        //mNumberOfResultGridColumns = 7;

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), mNumberOfComponentGridColumns);
        mSelectionGridRecyclerView.setLayoutManager(layoutManager);
        if (mComponentsGridAdapter==null) mComponentsGridAdapter = new KanjiGridRecyclerViewAdapter(getContext(), this, mDisplayableComponentSelections, false);
        else mComponentsGridAdapter.setContents(mDisplayableComponentSelections);
        mSelectionGridRecyclerView.setAdapter(mComponentsGridAdapter);

        ViewGroup.LayoutParams params = mSelectionGridRecyclerView.getLayoutParams();
        if (mDisplayableComponentSelections.size() <= 56) {
            mSelectionGridRecyclerView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        else {
            params.height = mMaxRecyclerViewHeightPixels;
            mSelectionGridRecyclerView.setLayoutParams(params);
        }

        mSelectionGridRecyclerView.setVisibility(View.VISIBLE);
        mNoSelectionElementsTextView.setVisibility(View.GONE);
    }
    private void showResultsGrid() {
        //mNumberOfResultGridColumns = 7;

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), mNumberOfResultGridColumns);
        mResultsGridRecyclerView.setLayoutManager(layoutManager);
        if (mResultsGridAdapter==null) mResultsGridAdapter = new KanjiGridRecyclerViewAdapter(getContext(), this, mPrintableSearchResults, true);
        else mResultsGridAdapter.setContents(mPrintableSearchResults);
        mResultsGridRecyclerView.setAdapter(mResultsGridAdapter);

        ViewGroup.LayoutParams params = mResultsGridRecyclerView.getLayoutParams();
        if (mPrintableSearchResults.size() <= 56) {
            mResultsGridRecyclerView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        else {
            params.height = mMaxRecyclerViewHeightPixels;
            mResultsGridRecyclerView.setLayoutParams(params);
        }

        mResultsGridRecyclerView.setVisibility(View.VISIBLE);
        mNoResultsTextView.setVisibility(View.GONE);
    }
    @TargetApi(23) private static boolean isPrintable( String c ) {
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
    private void startCreatingComponentKanjiGridElementsAsynchronously() {
        if (getActivity()!=null) {
            showLoadingIndicator();
            LoaderManager loaderManager = getActivity().getSupportLoaderManager();
            Loader<String> roomDbSearchLoader = loaderManager.getLoader(ROOM_DB_COMPONENT_GRID_LOADER);
            if (roomDbSearchLoader == null) loaderManager.initLoader(ROOM_DB_COMPONENT_GRID_LOADER, null, this);
            else loaderManager.restartLoader(ROOM_DB_COMPONENT_GRID_LOADER, null, this);
        }
    }
    private void startFilteringComponentKanjiGridElementsAsynchronously() {
        if (getActivity()!=null) {
            showLoadingIndicator();
            LoaderManager loaderManager = getActivity().getSupportLoaderManager();
            Loader<String> roomDbSearchLoader = loaderManager.getLoader(ROOM_DB_COMPONENT_GRID_FILTER_LOADER);
            if (roomDbSearchLoader == null) loaderManager.initLoader(ROOM_DB_COMPONENT_GRID_FILTER_LOADER, null, this);
            else loaderManager.restartLoader(ROOM_DB_COMPONENT_GRID_FILTER_LOADER, null, this);
        }
    }
    private void startSearchingForKanjisAsynchronously(String[] elements_strings) {
        if (getActivity()!=null) {
            showLoadingIndicator();
            LoaderManager loaderManager = getActivity().getSupportLoaderManager();
            Loader<String> roomDbSearchLoader = loaderManager.getLoader(ROOM_DB_KANJI_SEARCH_LOADER);
            Bundle bundle = new Bundle();
            bundle.putStringArray(getString(R.string.search_by_radical_elements_list), elements_strings);
            if (roomDbSearchLoader == null) loaderManager.initLoader(ROOM_DB_KANJI_SEARCH_LOADER, bundle, this);
            else loaderManager.restartLoader(ROOM_DB_KANJI_SEARCH_LOADER, bundle, this);
        }
    }


    @OnClick (R.id.search_by_radical_overall_structure_button) public void onRequestedStructureButtonClick() {
        showStructuresDialog("overall");
    }
    @OnClick (R.id.search_by_radical_button_radical) public void onRadicalButtonClick() {
        if (getActivity()!=null) Utilities.hideSoftKeyboard(getActivity());

        mSelectedComponent = "";
        mComponentSelectionType = "radical";
        showComponentsSelectionSection();

        startCreatingComponentKanjiGridElementsAsynchronously();
    }
    @OnClick (R.id.search_by_radical_button_component) public void onComponentButtonClick() {
        if (getActivity()!=null) Utilities.hideSoftKeyboard(getActivity());

        mSelectedComponent = "";
        mComponentSelectionType = "component";
        showComponentsSelectionSection();

        startCreatingComponentKanjiGridElementsAsynchronously();
    }
    @OnClick (R.id.search_by_radical_button_search) public void onSearchButtonClick() {

        showResultsSection();

        String[] elements_strings = new String[4];
        elements_strings[0] = mElementAEditText.getText().toString();
        elements_strings[1] = mElementBEditText.getText().toString();
        elements_strings[2] = mElementCEditText.getText().toString();
        elements_strings[3] = mElementDEditText.getText().toString();

        startSearchingForKanjisAsynchronously(elements_strings);
    }
    @OnClick (R.id.search_by_radical_requested_component_structure) public void oRequestedComponentStructureButtonClick() {
        showStructuresDialog("component");
    }
    @OnClick (R.id.search_by_radical_button_filter) public void onFilterButtonClick() {
        if (getActivity()!=null) Utilities.hideSoftKeyboard(getActivity());
        mKanjiCharacterNameForFilter = mCharacterDescriptorEditText.getText().toString();
        startFilteringComponentKanjiGridElementsAsynchronously();
    }
    @OnClick (R.id.search_by_radical_button_selection_grid_cancel_top) public void onCancelTopButtonClick() {
        handleComponentSelection(false);
    }
    @OnClick (R.id.search_by_radical_button_selection_grid_copy_to_radical_top) public void onEnterTopButtonClick() {
        handleComponentSelection(true);
    }
    @OnClick (R.id.search_by_radical_button_selection_grid_copy_to_input_top) public void onCopyToInputTopButtonClick() {
        searchByRadicalFragmentOperationsHandler.onQueryTextUpdateFromSearchByRadicalRequested(mSelectedComponent);
    }
    @OnClick (R.id.search_by_radical_button_selection_grid_cancel_bottom) public void onCancelBottomButtonClick() {
        handleComponentSelection(false);
    }
    @OnClick (R.id.search_by_radical_button_selection_grid_copy_to_radical_bottom) public void onEnterBottomButtonClick() {
        handleComponentSelection(true);
    }
    @OnClick (R.id.search_by_radical_button_selection_grid_copy_to_input_bottom) public void onCopyToInputBottomButtonClick() {
        searchByRadicalFragmentOperationsHandler.onQueryTextUpdateFromSearchByRadicalRequested(mSelectedComponent);
    }


    //Asynchronous methods
    @NonNull @Override public Loader<Object> onCreateLoader(int id, final Bundle args) {

        if (id == ROOM_DB_COMPONENT_GRID_LOADER) {
            mAlreadyLoadedSelectionGrid = false;
            ComponentGridCreationAsyncTaskLoader gridCreationLoader = new ComponentGridCreationAsyncTaskLoader(
                    getContext(), mComponentSelectionType, mRadicalsOnlyDatabase, mSelectedComponentStructure, mKanjiCharacterNameForFilter);
            return gridCreationLoader;
        }
        else if (id == ROOM_DB_COMPONENT_GRID_FILTER_LOADER) {
            mAlreadyFilteredSelectionGrid = false;
            ComponentGridFilterAsyncTaskLoader gridFilterLoader = new ComponentGridFilterAsyncTaskLoader(
                    getContext(), mComponentSelectionType, mRadicalsOnlyDatabase, mKanjiCharacterNameForFilter, mUnfilteredDisplayableComponentSelections);
            return gridFilterLoader;
        }
        else if (id == ROOM_DB_KANJI_SEARCH_LOADER) {
            mAlreadyLoadedKanjiSearchResults = false;
            String[] elements_list = args.getStringArray(getString(R.string.search_by_radical_elements_list));
            KanjiSearchAsyncTaskLoader kanjiSearchLoader = new KanjiSearchAsyncTaskLoader(
                    getContext(), elements_list, mSelectedOverallStructure, mSimilarsDatabase);
            return kanjiSearchLoader;
        }
        else return new ComponentGridCreationAsyncTaskLoader(getContext(), "", null, 0, "");
    }
    @Override public void onLoadFinished(@NonNull Loader<Object> loader, Object data) {

        if (getContext()==null) return;

        //region Selection Grid
        if (loader.getId() == ROOM_DB_COMPONENT_GRID_LOADER && !mAlreadyLoadedSelectionGrid && data!=null) {
            mAlreadyLoadedSelectionGrid = true;

            mUnfilteredDisplayableComponentSelections = (List<String>) data;
            mDisplayableComponentSelections = (List<String>) new ArrayList(mUnfilteredDisplayableComponentSelections);

            hideLoadingIndicator();

            if (mDisplayableComponentSelections.size() != 0) showComponentsGrid();
            else showNoComponentsTextInsteadOfComponentsGrid();

            if (getLoaderManager()!=null) getLoaderManager().destroyLoader(ROOM_DB_COMPONENT_GRID_LOADER);
        }
        //endregion

        //region Selection grid filtered
        else if (loader.getId() == ROOM_DB_COMPONENT_GRID_FILTER_LOADER && !mAlreadyFilteredSelectionGrid && data!=null) {
            mAlreadyFilteredSelectionGrid = true;

            mDisplayableComponentSelections = (List<String>) data;

            hideLoadingIndicator();

            if (mDisplayableComponentSelections.size() != 0) showComponentsGrid();
            else showNoComponentsTextInsteadOfComponentsGrid();

            if (getLoaderManager()!=null) getLoaderManager().destroyLoader(ROOM_DB_COMPONENT_GRID_FILTER_LOADER);
        }
        //endregion

        //region Results grid
        else if (loader.getId() == ROOM_DB_KANJI_SEARCH_LOADER && !mAlreadyLoadedKanjiSearchResults && data!=null) {
            mAlreadyLoadedKanjiSearchResults = true;

            Object[] dataElements = (Object[]) data;

            List<String> search_results = (List<String>) dataElements[0];
            boolean searchTooBroad = (boolean) dataElements[1];

            hideLoadingIndicator();

            //Displaying only the search results that have a glyph in the font
            mPrintableSearchResults = new ArrayList<>();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                for (int i = 0; i < search_results.size(); i++) {
                    if (isPrintable(search_results.get(i).substring(0, 1))) {
                        mPrintableSearchResults.add(search_results.get(i));
                    }
                }
            }
            else {
                mPrintableSearchResults = search_results;
            }

            //Displaying only the first 400 values to prevent overload
            List<String> selections = new ArrayList<>();
            int display_limit = 400;
            for (int i = 0; i < mPrintableSearchResults.size(); i++) {
                selections.add(mPrintableSearchResults.get(i));
                if (i>display_limit) break;
            }
            mPrintableSearchResults = selections;

            //Displaying the results grid
            if (searchTooBroad) showNoResultsTextInsteadOfResultsGrid("Search too broad.");
            else if (mPrintableSearchResults.size() != 0) showResultsGrid();
            else showNoResultsTextInsteadOfResultsGrid("No results found.");

            //createSearchResultsGrid(printable_search_results, searchTooBroad);
            if (getLoaderManager()!=null) getLoaderManager().destroyLoader(ROOM_DB_COMPONENT_GRID_LOADER);
        }
        //endregion
    }
    @Override public void onLoaderReset(@NonNull Loader<Object> loader) {}
    private static class ComponentGridCreationAsyncTaskLoader extends AsyncTaskLoader<Object> {

        private static final int CHARACTER_RESULTS_GRID_MAX_NUMBER = 400;
        //region Parameters
        private final String mComponentSelectionType;
        private final List<String[]> mRadicalsOnlyDatabase;
        private final int chosen_components_list;
        private final String mKanjiCharacterNameForFilter;
        private List<String[]> sortedList;
        private JapaneseToolboxKanjiRoomDatabase mJapaneseToolboxKanjiRoomDatabase;
        //endregion

        ComponentGridCreationAsyncTaskLoader(Context context,
                                             String mComponentSelectionType,
                                             List<String[]> mRadicalsOnlyDatabase,
                                             int chosen_components_list,
                                             String mKanjiCharacterNameForFilter) {
            super(context);
            this.mComponentSelectionType = mComponentSelectionType;
            this.mRadicalsOnlyDatabase = mRadicalsOnlyDatabase;
            this.chosen_components_list = chosen_components_list;
            this.mKanjiCharacterNameForFilter = mKanjiCharacterNameForFilter;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Override
        public Object loadInBackground() {

            mJapaneseToolboxKanjiRoomDatabase = JapaneseToolboxKanjiRoomDatabase.getInstance(getContext());
            List<String> displayableComponentSelections = getSelectionGridElements();
            displayableComponentSelections = filterGridElementsAccordingToDescriptor(displayableComponentSelections);

            return displayableComponentSelections;
        }

        List<String> getSelectionGridElements() {

            if (mComponentSelectionType.equals("")) return new ArrayList<>();

            //Creating the the list to be displayed in the dialog grid
            List<String> selections = new ArrayList<>();
            String[] currentElement;

            //region If the user clicks on the radical button, then a radical selection grid is shown
            if (mComponentSelectionType.contains("radical")) {

                String[] parsed_list;
                String last_index = "0";
                for (int i = 0; i < mRadicalsOnlyDatabase.size(); i++) {
                    currentElement = mRadicalsOnlyDatabase.get(i);
                    parsed_list = currentElement[2].split(";");

                    //Skipping radicals not found in the CJK decompositons database
                    if (parsed_list.length == 3 && parsed_list[2].equals("not found in decompositions")) continue;

                    // Adding the header radical numbers to the list of radicals
                    if (!currentElement[4].equals(last_index)) {
                        if (parsed_list[0].equals("Special") || parsed_list[0].equals("Hiragana") || parsed_list[0].equals("Katakana")) {
                        } else if (parsed_list.length > 1) {
                            if (parsed_list[1].equals("variant")) {
                                selections.add(currentElement[4]);
                                selections.add(currentElement[0] + "variant");
                                last_index = currentElement[4];
                            }
                        } else {
                            selections.add(currentElement[4]);
                            selections.add(currentElement[0]);
                            last_index = currentElement[4];
                        }
                    } else if (currentElement[4].equals(last_index)) {
                        if (parsed_list.length > 1) {
                            if (parsed_list[1].equals("variant")) {
                                selections.add(currentElement[0] + "variant");
                            }
                        } else {
                            selections.add(currentElement[0]);
                        }
                        last_index = currentElement[4];
                    }
                }
            }
            //endregion

            //region If the user clicks on the component button, then then the components grid is extracted from Room
            else {
                List<String[]> fullList = new ArrayList<>();
                String[] printableResultsForCurrentElement;
                Boolean containsAtLeastOnePrintableGlyph;

                //List<KanjiComponent> kanjiComponents = mJapaneseToolboxRoomDatabase.getAllKanjiComponents();
                String componentStructure = GlobalConstants.COMPONENT_STRUCTURES_MAP.get(chosen_components_list);
                if (!TextUtils.isEmpty(componentStructure)) {

                    List<KanjiComponent> kanjiComponents = mJapaneseToolboxKanjiRoomDatabase.getKanjiComponentsByStructureName(componentStructure);
                    if (kanjiComponents != null && kanjiComponents.size()>0) {
                        KanjiComponent kanjiComponent = kanjiComponents.get(0);
                        List<KanjiComponent.AssociatedComponent> associatedComponents = kanjiComponent.getAssociatedComponents();

                        if (componentStructure.equals("full") && kanjiComponents.size()==2) {
                            associatedComponents.addAll(kanjiComponents.get(1).getAssociatedComponents());
                        }

                        for (int i = 0; i < associatedComponents.size(); i++) {
                            currentElement = new String[2];
                            currentElement[0] = associatedComponents.get(i).getComponent();
                            currentElement[1] = Integer.toString(associatedComponents.get(i).getAssociatedComponents().length());

                            containsAtLeastOnePrintableGlyph = true;

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                printableResultsForCurrentElement = associatedComponents.get(i).getAssociatedComponents().split(";");
                                for (int j = 0; j < printableResultsForCurrentElement.length; j++) {
                                    containsAtLeastOnePrintableGlyph = false;
                                    if (isPrintable(printableResultsForCurrentElement[j].substring(0, 1))) {
                                        containsAtLeastOnePrintableGlyph = true;
                                        break;
                                    }
                                }
                            }

                            if (containsAtLeastOnePrintableGlyph) { fullList.add(currentElement);}
                        }

                    }
                }

                sortedList = sortAccordingToGrowingFrequency(fullList); //Output list is in order of growing frequency, next for loop inverts this
                //sorted_list = fullList;
                for (int i = 0; i< sortedList.size(); i++) {
                    selections.add(sortedList.get(sortedList.size()-1-i)[0]);
                }
            }
            //endregion

            //region Displaying only the search results that have a glyph in the font
            List<String> displayableComponentSelections = new ArrayList<>();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                for (int i = 0; i < selections.size(); i++) {
                    if (isPrintable(selections.get(i).substring(0, 1))) {
                        displayableComponentSelections.add(selections.get(i));
                    }
                }
            }
            else {
                displayableComponentSelections = selections;
            }
            //endregion

            //region Displaying only the first XX values to prevent overload
            selections = new ArrayList<>();
            for (int i = 0; i < displayableComponentSelections.size(); i++) {
                selections.add(displayableComponentSelections.get(i));
                if (i>CHARACTER_RESULTS_GRID_MAX_NUMBER) {break;}
            }
            displayableComponentSelections = selections;
            //endregion


            return displayableComponentSelections;
        }
        List<String> filterGridElementsAccordingToDescriptor(List<String> displayableComponentSelections) {

            if (displayableComponentSelections ==null) return new ArrayList<>();
            else if (TextUtils.isEmpty(mKanjiCharacterNameForFilter)) return displayableComponentSelections;

            List<String> intersectionWithMatchingDescriptors;
            if (mComponentSelectionType.contains("radical")) {
                List<String> matchingRadicals = new ArrayList<>();
                String radical;
                String radicalNumber;
                String radicalNumberFirstElement;
                String radicalName;
                String numberStrokes;
                for (int i = 0; i < mRadicalsOnlyDatabase.size(); i++) {
                    radical = mRadicalsOnlyDatabase.get(i)[0];
                    radicalNumber = mRadicalsOnlyDatabase.get(i)[2];
                    radicalNumberFirstElement = radicalNumber.split(";")[0];
                    radicalName = mRadicalsOnlyDatabase.get(i)[3];
                    numberStrokes = mRadicalsOnlyDatabase.get(i)[4];
                    if (radical.equals(mKanjiCharacterNameForFilter)
                            || radicalNumberFirstElement.equals(mKanjiCharacterNameForFilter)
                            || radicalName.contains(mKanjiCharacterNameForFilter)
                            || numberStrokes.equals(mKanjiCharacterNameForFilter)) {
                        matchingRadicals.add(mRadicalsOnlyDatabase.get(i)[0]);
                    }
                }

                intersectionWithMatchingDescriptors = Utilities.getIntersectionOfLists(displayableComponentSelections, matchingRadicals);
            }
            else {
                List<KanjiCharacter> matchingKanjiCharactersByDescriptor = mJapaneseToolboxKanjiRoomDatabase.getKanjiCharactersByDescriptor(mKanjiCharacterNameForFilter);

                List<String> matchingCharacters = new ArrayList<>();
                for (KanjiCharacter kanjiCharacter : matchingKanjiCharactersByDescriptor) {
                    matchingCharacters.add(Utilities.convertFromUTF8Index(kanjiCharacter.getHexIdentifier()));
                }

                intersectionWithMatchingDescriptors = Utilities.getIntersectionOfLists(displayableComponentSelections, matchingCharacters);
            }

            return intersectionWithMatchingDescriptors;

        }

        // QuickSort Algorithm (adapted from http://www.vogella.com/tutorials/JavaAlgorithmsQuicksort/article.html)
        List<String[]> sortAccordingToGrowingFrequency(List<String[]> values) {
            // check for empty or null array
            if (values ==null || values.size()==0){
                return new ArrayList<>();
            }
            this.sortedList = values;
            quicksort(0, values.size() - 1);
            return sortedList;
        }
        private void quicksort(int low, int high) {
            int i = low, j = high;
            // Get the pivot element from the middle of the list
            int pivot = Integer.parseInt(sortedList.get(low + (high-low)/2)[1]);

            // Divide into component_substructures[2] lists
            while (i <= j) {
                // If the current value from the left list is smaller then the pivot
                // element then get the next element from the left list
                while (Integer.parseInt(sortedList.get(i)[1]) < pivot) {
                    i++;
                }
                // If the current value from the right list is larger then the pivot
                // element then get the next element from the right list
                while (Integer.parseInt(sortedList.get(j)[1]) > pivot) {
                    j--;
                }

                // If we have found a values in the left list which is larger then
                // the pivot element and if we have found a value in the right list
                // which is smaller then the pivot element then we exchange the
                // values.
                // As we are done we can increase i and j
                if (i <= j) {
                    exchange(i, j);
                    i++;
                    j--;
                }
            }
            // Recursion
            if (low < j)
                quicksort(low, j);
            if (i < high)
                quicksort(i, high);
        }
        private void exchange(int i, int j) {
            String[] value_to_i = {sortedList.get(j)[0], sortedList.get(j)[1]};
            String[] value_to_j = {sortedList.get(i)[0], sortedList.get(i)[1]};
            sortedList.set(i, value_to_i);
            sortedList.set(j, value_to_j);
        }

    }
    private static class ComponentGridFilterAsyncTaskLoader extends AsyncTaskLoader<Object> {

        //region Parameters
        private final String mComponentSelectionType;
        private final List<String[]> mRadicalsOnlyDatabase;
        private final String mKanjiCharacterNameForFilter;
        private List<String> mDisplayableComponentSelections;
        private List<String[]> sortedList;
        private JapaneseToolboxKanjiRoomDatabase mJapaneseToolboxKanjiRoomDatabase;
        //endregion

        ComponentGridFilterAsyncTaskLoader(Context context,
                                         String mComponentSelectionType,
                                         List<String[]> mRadicalsOnlyDatabase,
                                         String mKanjiCharacterNameForFilter,
                                           List<String> mDisplayableComponentSelections) {
            super(context);
            this.mComponentSelectionType = mComponentSelectionType;
            this.mRadicalsOnlyDatabase = mRadicalsOnlyDatabase;
            this.mKanjiCharacterNameForFilter = mKanjiCharacterNameForFilter;
            this.mDisplayableComponentSelections = mDisplayableComponentSelections;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Override
        public Object loadInBackground() {

            mJapaneseToolboxKanjiRoomDatabase = JapaneseToolboxKanjiRoomDatabase.getInstance(getContext());
            mDisplayableComponentSelections = filterGridElementsAccordingToDescriptor(mDisplayableComponentSelections);

            return mDisplayableComponentSelections;
        }

        List<String> filterGridElementsAccordingToDescriptor(List<String> displayableComponentSelections) {

            if (displayableComponentSelections ==null) return new ArrayList<>();
            else if (TextUtils.isEmpty(mKanjiCharacterNameForFilter)) return displayableComponentSelections;

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
                    radical = mRadicalsOnlyDatabase.get(i)[0];
                    radicalNumber = mRadicalsOnlyDatabase.get(i)[2];
                    radicalNumberFirstElement = radicalNumber.split(";")[0];
                    radicalName = mRadicalsOnlyDatabase.get(i)[3];
                    numberStrokes = mRadicalsOnlyDatabase.get(i)[4];
                    if (radical.equals(mKanjiCharacterNameForFilter)
                            || radicalNumberFirstElement.equals(mKanjiCharacterNameForFilter)
                            || radicalName.contains(mKanjiCharacterNameForFilter)
                            || numberStrokes.equals(mKanjiCharacterNameForFilter)) {
                        matchingRadicals.add(mRadicalsOnlyDatabase.get(i)[0]);
                        matchingRadicalNumber = radicalNumber;
                    }
                    if (radicalName.equals("") && !matchingRadicalNumber.equals("") && radicalNumberFirstElement.equals(matchingRadicalNumber)) {
                        matchingRadicals.add(mRadicalsOnlyDatabase.get(i)[0] + "variant");
                    }
                }

                intersectionWithMatchingDescriptors = Utilities.getIntersectionOfLists(displayableComponentSelections, matchingRadicals);
            }
            else {
                List<KanjiCharacter> matchingKanjiCharactersByDescriptor = mJapaneseToolboxKanjiRoomDatabase.getKanjiCharactersByDescriptor(mKanjiCharacterNameForFilter);

                List<String> matchingCharacters = new ArrayList<>();
                for (KanjiCharacter kanjiCharacter : matchingKanjiCharactersByDescriptor) {
                    matchingCharacters.add(Utilities.convertFromUTF8Index(kanjiCharacter.getHexIdentifier()));
                }

                intersectionWithMatchingDescriptors = Utilities.getIntersectionOfLists(displayableComponentSelections, matchingCharacters);
            }

            return intersectionWithMatchingDescriptors;

        }
    }
    private static class KanjiSearchAsyncTaskLoader extends AsyncTaskLoader<Object> {

        //region Parameters
        private final String[] elements_list;
        private final int mSelectedStructure;
        private final List<String[]> mSimilarsDatabase;
        private JapaneseToolboxKanjiRoomDatabase mJapaneseToolboxKanjiRoomDatabase;
        private int mMaxSizeForDuplicateRemoval;
        private boolean mSearchTooBroad;
        //endregion

        KanjiSearchAsyncTaskLoader(Context context, String[] elements_list, int mSelectedStructure, List<String[]> mSimilarsDatabase) {
            super(context);
            this.elements_list = elements_list;
            this.mSelectedStructure = mSelectedStructure;
            this.mSimilarsDatabase = mSimilarsDatabase;
        }

        @Override
        protected void onStartLoading() {
            if (elements_list!=null) forceLoad();
        }

        @Override
        public Object loadInBackground() {

            mJapaneseToolboxKanjiRoomDatabase = JapaneseToolboxKanjiRoomDatabase.getInstance(getContext());
            List<String> result = findSearchResults();

            return new Object[] {result, mSearchTooBroad};
        }

        private List<String> findSearchResults() {

            //region Initialization
            for (int j=0; j<elements_list.length; j++) {
                if (!elements_list[j].equals("")) {
                    for (int i=0; i<mSimilarsDatabase.size(); i++) {
                        if (elements_list[j].equals(mSimilarsDatabase.get(i)[0])) {
                            elements_list[j] = mSimilarsDatabase.get(i)[1];
                            break;
                        }
                    }
                }
            }

            mMaxSizeForDuplicateRemoval = 200;
            String elementA = elements_list[0];
            String elementB = elements_list[1];
            String elementC = elements_list[2];
            String elementD = elements_list[3];

            if (    (mSelectedStructure == GlobalConstants.Index_full
                    || mSelectedStructure == GlobalConstants.Index_across2
                    || mSelectedStructure == GlobalConstants.Index_down2
                    || mSelectedStructure == GlobalConstants.Index_across3
                    || mSelectedStructure == GlobalConstants.Index_down3)
                    && (elementA.equals("") && elementB.equals("") && elementC.equals("") && elementD.equals(""))) {
                mSearchTooBroad = true;
                return new ArrayList<>();
            }
            //endregion

            //region Finding the list of matches in the Full components list, that correspond to the user's input
            List<KanjiComponent.AssociatedComponent> associatedComponents = null;
            List<KanjiComponent> kanjiComponentsFull1 = mJapaneseToolboxKanjiRoomDatabase.getKanjiComponentsByStructureName("full1");
            List<KanjiComponent> kanjiComponentsFull2 = mJapaneseToolboxKanjiRoomDatabase.getKanjiComponentsByStructureName("full2");
            if (kanjiComponentsFull1 != null && kanjiComponentsFull1.size() > 0) {
                associatedComponents = kanjiComponentsFull1.get(0).getAssociatedComponents();
            }
            if (kanjiComponentsFull2 != null && kanjiComponentsFull2.size() > 0) {
                associatedComponents.addAll(kanjiComponentsFull2.get(0).getAssociatedComponents());
            }

            elementA = Utilities.removeSpecialCharacters(elementA);
            elementB = Utilities.removeSpecialCharacters(elementB);
            elementC = Utilities.removeSpecialCharacters(elementC);
            elementD = Utilities.removeSpecialCharacters(elementD);
            boolean checkForExactMatchesOfElementA = !elementA.equals("");
            boolean checkForExactMatchesOfElementB = !elementB.equals("");
            boolean checkForExactMatchesOfElementC = !elementC.equals("");
            boolean checkForExactMatchesOfElementD = !elementD.equals("");
            List<String> listOfMatchingResultsElementA = new ArrayList<>();
            List<String> listOfMatchingResultsElementB = new ArrayList<>();
            List<String> listOfMatchingResultsElementC = new ArrayList<>();
            List<String> listOfMatchingResultsElementD = new ArrayList<>();

            if (!checkForExactMatchesOfElementA || !checkForExactMatchesOfElementB || !checkForExactMatchesOfElementC || !checkForExactMatchesOfElementD) {
                List<String> listOfAllKanjis = mJapaneseToolboxKanjiRoomDatabase.getAllKanjis();
                if (!checkForExactMatchesOfElementA) listOfMatchingResultsElementA = listOfAllKanjis;
                if (!checkForExactMatchesOfElementB) listOfMatchingResultsElementB = listOfAllKanjis;
                if (!checkForExactMatchesOfElementC) listOfMatchingResultsElementC = listOfAllKanjis;
                if (!checkForExactMatchesOfElementD) listOfMatchingResultsElementD = listOfAllKanjis;
            }

            for (KanjiComponent.AssociatedComponent associatedComponent : associatedComponents) {
                if (checkForExactMatchesOfElementA && associatedComponent.getComponent().equals(elementA)) {
                    listOfMatchingResultsElementA = Arrays.asList(associatedComponent.getAssociatedComponents().split(";"));
                    checkForExactMatchesOfElementA = false;
                }
                if (checkForExactMatchesOfElementB && associatedComponent.getComponent().equals(elementB)) {
                    listOfMatchingResultsElementB = Arrays.asList(associatedComponent.getAssociatedComponents().split(";"));
                    checkForExactMatchesOfElementB = false;
                }
                if (checkForExactMatchesOfElementC && associatedComponent.getComponent().equals(elementC)) {
                    listOfMatchingResultsElementC = Arrays.asList(associatedComponent.getAssociatedComponents().split(";"));
                    checkForExactMatchesOfElementC = false;
                }
                if (checkForExactMatchesOfElementD && associatedComponent.getComponent().equals(elementD)) {
                    listOfMatchingResultsElementD = Arrays.asList(associatedComponent.getAssociatedComponents().split(";"));
                    checkForExactMatchesOfElementD = false;
                }
                if (!checkForExactMatchesOfElementA && !checkForExactMatchesOfElementB &&!checkForExactMatchesOfElementC && !checkForExactMatchesOfElementD) break;
            }
            //endregion

            //region Getting the match intersections in the Full list
            List<String> listOfIntersectingResults = new ArrayList<>();
            if      ( elementA.equals("") &&  elementB.equals("") &&  elementC.equals("") &&  elementD.equals("")) {
                listOfIntersectingResults.addAll(listOfMatchingResultsElementA);
            }
            else if ( elementA.equals("") &&  elementB.equals("") &&  elementC.equals("") && !elementD.equals("")) {
                listOfIntersectingResults.addAll(listOfMatchingResultsElementD);
            }
            else if ( elementA.equals("") &&  elementB.equals("") && !elementC.equals("") &&  elementC.equals("")) {
                listOfIntersectingResults.addAll(listOfMatchingResultsElementC);
            }
            else if ( elementA.equals("") &&  elementB.equals("") && !elementC.equals("") && !elementD.equals("")) {
                listOfIntersectingResults = Utilities.getIntersectionOfLists(listOfMatchingResultsElementC, listOfMatchingResultsElementD);
            }
            else if ( elementA.equals("") && !elementB.equals("") &&  elementC.equals("") &&  elementD.equals("")) {
                listOfIntersectingResults.addAll(listOfMatchingResultsElementB);
            }
            else if ( elementA.equals("") && !elementB.equals("") &&  elementC.equals("") && !elementD.equals("")) {
                listOfIntersectingResults = Utilities.getIntersectionOfLists(listOfMatchingResultsElementB, listOfMatchingResultsElementD);
            }
            else if ( elementA.equals("") && !elementB.equals("") && !elementC.equals("") &&  elementD.equals("")) {
                listOfIntersectingResults = Utilities.getIntersectionOfLists(listOfMatchingResultsElementB, listOfMatchingResultsElementC);
            }
            else if ( elementA.equals("") && !elementB.equals("") && !elementC.equals("") && !elementD.equals("")) {
                listOfIntersectingResults = Utilities.getIntersectionOfLists(listOfMatchingResultsElementB, listOfMatchingResultsElementC);
                listOfIntersectingResults = Utilities.getIntersectionOfLists(listOfIntersectingResults, listOfMatchingResultsElementD);
            }
            else if (!elementA.equals("") &&  elementB.equals("") &&  elementC.equals("") &&  elementD.equals("")) {
                listOfIntersectingResults.addAll(listOfMatchingResultsElementA);
            }
            else if (!elementA.equals("") &&  elementB.equals("") &&  elementC.equals("") && !elementD.equals("")) {
                listOfIntersectingResults = Utilities.getIntersectionOfLists(listOfMatchingResultsElementA, listOfMatchingResultsElementD);
            }
            else if (!elementA.equals("") &&  elementB.equals("") && !elementC.equals("") &&  elementD.equals("")) {
                listOfIntersectingResults = Utilities.getIntersectionOfLists(listOfMatchingResultsElementA, listOfMatchingResultsElementC);
            }
            else if (!elementA.equals("") &&  elementB.equals("") && !elementC.equals("") && !elementD.equals("")) {
                listOfIntersectingResults = Utilities.getIntersectionOfLists(listOfMatchingResultsElementA, listOfMatchingResultsElementC);
                listOfIntersectingResults = Utilities.getIntersectionOfLists(listOfIntersectingResults, listOfMatchingResultsElementD);
            }
            else if (!elementA.equals("") && !elementB.equals("") &&  elementC.equals("") &&  elementD.equals("")) {
                listOfIntersectingResults = Utilities.getIntersectionOfLists(listOfMatchingResultsElementA, listOfMatchingResultsElementB);
            }
            else if (!elementA.equals("") && !elementB.equals("") &&  elementC.equals("") && !elementD.equals("")) {
                listOfIntersectingResults = Utilities.getIntersectionOfLists(listOfMatchingResultsElementA, listOfMatchingResultsElementB);
                listOfIntersectingResults = Utilities.getIntersectionOfLists(listOfIntersectingResults, listOfMatchingResultsElementD);
            }
            else if (!elementA.equals("") && !elementB.equals("") && !elementC.equals("") &&  elementD.equals("")) {
                listOfIntersectingResults = Utilities.getIntersectionOfLists(listOfMatchingResultsElementA, listOfMatchingResultsElementB);
                listOfIntersectingResults = Utilities.getIntersectionOfLists(listOfIntersectingResults, listOfMatchingResultsElementC);
            }
            else if (!elementA.equals("") && !elementB.equals("") && !elementC.equals("") && !elementD.equals("")) {
                listOfIntersectingResults = Utilities.getIntersectionOfLists(listOfMatchingResultsElementA, listOfMatchingResultsElementB);
                listOfIntersectingResults = Utilities.getIntersectionOfLists(listOfIntersectingResults, listOfMatchingResultsElementC);
                listOfIntersectingResults = Utilities.getIntersectionOfLists(listOfIntersectingResults, listOfMatchingResultsElementD);
            }
            //endregion

            //region Getting the subset of characters match the user's selected structure
            List<String> listOfResultsRelevantToRequestedStructure = new ArrayList<>();
            if (mSelectedStructure != GlobalConstants.Index_full) {

                //Getting the components list relevant to the requested structure
                KanjiComponent kanjiComponentForRequestedStructure = null;
                String componentStructure = GlobalConstants.COMPONENT_STRUCTURES_MAP.get(mSelectedStructure);
                if (!TextUtils.isEmpty(componentStructure)) {
                    List<KanjiComponent> kanjiComponents = mJapaneseToolboxKanjiRoomDatabase.getKanjiComponentsByStructureName(componentStructure);
                    if (kanjiComponents != null && kanjiComponents.size() > 0) {
                        kanjiComponentForRequestedStructure = kanjiComponents.get(0);
                        associatedComponents = kanjiComponentForRequestedStructure.getAssociatedComponents();
                    }
                }
                if (kanjiComponentForRequestedStructure==null || associatedComponents==null) return new ArrayList<>();

                //Looping over all the structure's components and adding only the ones that appear in listOfIntersectingResults
                List<String> structureComponents;
                List<String> currentIntersections;
                for (KanjiComponent.AssociatedComponent associatedComponent : associatedComponents) {
                    structureComponents = Arrays.asList(associatedComponent.getAssociatedComponents().split(";"));
                    currentIntersections = Utilities.getIntersectionOfLists(listOfIntersectingResults, structureComponents);
                    listOfResultsRelevantToRequestedStructure.addAll(currentIntersections);
                }
                listOfResultsRelevantToRequestedStructure = Utilities.removeDuplicatesFromList(listOfResultsRelevantToRequestedStructure);

            }
            else {
                listOfResultsRelevantToRequestedStructure = listOfIntersectingResults;
            }
            //endregion

            return listOfResultsRelevantToRequestedStructure;
        }

    }

    //Communication with parent activity
    SearchByRadicalFragmentOperationsHandler searchByRadicalFragmentOperationsHandler;
    interface SearchByRadicalFragmentOperationsHandler {
        void onQueryTextUpdateFromSearchByRadicalRequested(String selectedWordString);
    }

    //Communication with KanjiComponentsGridRecyclerViewAdapter
    @Override public void onComponentClicked(int clickedPosition) {
        mSelectedComponent = mDisplayableComponentSelections.get(clickedPosition);
    }
    @Override public void onSearchResultClicked(int clickedPosition) {
        searchByRadicalFragmentOperationsHandler.onQueryTextUpdateFromSearchByRadicalRequested(mPrintableSearchResults.get(clickedPosition));
    }
}