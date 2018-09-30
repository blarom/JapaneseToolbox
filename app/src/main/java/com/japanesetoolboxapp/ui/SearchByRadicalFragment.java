package com.japanesetoolboxapp.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.japanesetoolboxapp.R;
import com.japanesetoolboxapp.data.DatabaseUtilities;
import com.japanesetoolboxapp.data.JapaneseToolboxRoomDatabase;
import com.japanesetoolboxapp.data.KanjiComponent;
import com.japanesetoolboxapp.resources.GlobalConstants;
import com.japanesetoolboxapp.resources.MainApplication;
import com.japanesetoolboxapp.resources.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class SearchByRadicalFragment extends Fragment implements LoaderManager.LoaderCallbacks<Object> {


    //region Parameters
    @BindView(R.id.search_by_radical_loading_indicator) ProgressBar mProgressBarLoadingIndicator;
    @BindView(R.id.radicals_overall_block_container) LinearLayout mOverallBlockContainerLinearLayout;
    private Unbinder mBinding;
    private static final int ROOM_DB_COMPONENT_GRID_LOADER = 5684;
    private static final int ROOM_DB_KANJI_SEARCH_LOADER = 9512;
    ImageView selected_structure_overlapping;
    ImageView selected_structure_across;
    ImageView selected_structure_down;
    ImageView selected_structure_enclosure;
    ImageView selected_structure_multiple;
    ImageView[] selected_substructures;

    ImageView component_structure_across;
    ImageView component_structure_down;
    ImageView component_structure_enclosure;
    ImageView component_structure_multiple;
    ImageView[] component_substructures;

    GridView grid;
    GridView searchResultsGrid;
    TextView[] user_selections_textviews;
    EditText[] elements;
    Button search_for_char;
    Button[] radicals;
    Button[] components;

    LinearLayout.LayoutParams overall_block_container_layoutParams;

    LinearLayout userselections_block_linearLayout;
    LinearLayout.LayoutParams userselections_overall_block_layoutParams;

    LinearLayout selected_structures_selection_block_linearlayout;
    LinearLayout.LayoutParams selected_structures_selection_block_layoutparams;

    LinearLayout component_structures_selection_block_linearlayout;
    LinearLayout.LayoutParams component_structures_selection_block_layoutparams;

    LinearLayout grid_block_linearLayout;
    LinearLayout.LayoutParams grid_block_layoutParams;

    LinearLayout search_results_block_linearLayout;
    LinearLayout.LayoutParams search_results_block_layoutParams;

    LinearLayout input_row_enter_cancel_top;
    LinearLayout input_row_enter_cancel_bottom;

    int number_of_default_views_in_selection_block;
    int number_of_views_added_in_block;
    int grid_row_height;
    int selected_item_position;
    int chosen_selected_category_index;
    int chosen_selected_subcategory_index;
    int chosen_component_category_index;
    int chosen_component_subcategory_index;
    int selected_structure;
    int chosen_components_list;
    int current_row_index;
    int[] user_selections_radical_positions;
    int[] user_selections_component_positions;

    Boolean show_grid;
    Boolean[] user_selection_is_highlighted;
    String mComponentSelectionType;
    String[] user_selections;
    List<String> mDisplayableComponentSelections;
    private String mInputQuery;
    private List<String[]> mRadicalsOnlyDatabase;
    //private List<List<String[]>> mArrayOfComponentsDatabases;
    private String[] radical_module_user_selections;
    private boolean mAlreadyLoadedSelectionGrid;
    private boolean mAlreadyLoadedKanjiSearchResults;
    private List<KanjiComponent> mKanjiComponents;
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
    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_search_by_radical, container, false);

        //setRetainInstance(true);

        mBinding = ButterKnife.bind(this, rootView);

        return rootView;
    }
    @Override public void onStart() {
        super.onStart();

        //TODO: insert the input query into the fields of the kanji finder
        getKanjiFromRadicals(mInputQuery);
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
        }
    }
    private void initializeParameters() {
        mAlreadyLoadedSelectionGrid = false;
    }
    public void getKanjiFromRadicals(String inputQuery) {

        // Initialization
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int densityDpi = (int)(metrics.density);
        int empty_space_size = 15*densityDpi;

        //resetLinearLayoutViews(overall_block_container, 1);
        mOverallBlockContainerLinearLayout.removeAllViews();

        mOverallBlockContainerLinearLayout.setBackgroundColor(getResources().getColor(R.color.White));
        mOverallBlockContainerLinearLayout.setAlpha((float) 0.90);

        mOverallBlockContainerLinearLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideSoftKeyboard();
                return false;
            }
        });

        userselections_overall_block_layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        userselections_overall_block_layoutParams.gravity = Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL;
        userselections_overall_block_layoutParams.setMargins(10, 5, 10, 0); // (left, top, right, bottom)

        userselections_block_linearLayout = new LinearLayout(getContext());
        userselections_block_linearLayout.setOrientation(LinearLayout.VERTICAL);
        userselections_block_linearLayout.setLayoutParams(userselections_overall_block_layoutParams);
        userselections_block_linearLayout.setFocusable(true);
        userselections_block_linearLayout.setClickable(true);

        mOverallBlockContainerLinearLayout.addView(userselections_block_linearLayout);

        userselections_block_linearLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideSoftKeyboard();
                return false;
            }
        });

        userselections_block_linearLayout.removeAllViews();

        makeText(getResources().getString(R.string.ChooseTheStructure), userselections_overall_block_layoutParams, userselections_block_linearLayout);

        createUserInputFields();

        createSearchResultsBlock();
    }

    ////User Interface functions
    public void createUserInputFields() {

        //Initializations
        user_selections = new String[4];
        if (radical_module_user_selections != null) {
            System.arraycopy(radical_module_user_selections, 0, user_selections, 0, 4);
        }
        else {
            for (int i=0; i<4; i++) { user_selections[i] = ""; }
        }

        elements = new EditText[4];
        radicals = new Button[4];
        components = new Button[4];
        elements[0] = makeEditText(user_selections[0], "Element 1");
        elements[1] = makeEditText(user_selections[1], "Element 2");
        elements[2] = makeEditText(user_selections[2], "Element 3");
        elements[3] = makeEditText(user_selections[3], "Element 4");
        radicals[0] = makeCharacterSelectionButton("radical", 0);
        radicals[1] = makeCharacterSelectionButton("radical", 0);
        radicals[2] = makeCharacterSelectionButton("radical", 0);
        radicals[3] = makeCharacterSelectionButton("radical", 0);
        components[0] = makeCharacterSelectionButton("component", 0);
        components[1] = makeCharacterSelectionButton("component", 0);
        components[2] = makeCharacterSelectionButton("component", 0);
        components[3] = makeCharacterSelectionButton("component", 0);
        selected_substructures = new ImageView[9];
        for (int i=0;i<9;i++) { selected_substructures[i] = null; }
        component_substructures = new ImageView[9];
        for (int i=0;i<9;i++) { component_substructures[i] = null; }


        show_grid = false;
        number_of_default_views_in_selection_block = 6;
        user_selection_is_highlighted = new Boolean[4];
        user_selections_radical_positions = new int[4];
        user_selections_component_positions = new int[4];
        user_selections_textviews = new TextView[4];
        for (int i=0;i<4;i++) {
            user_selection_is_highlighted[i] = false;
            user_selections_radical_positions[i] = 0;
            user_selections_component_positions[i] = 0;
            user_selections_textviews[i] = null;
        }
        current_row_index = 0;

        //Creating the user structure preference block
        createSelectedStructureSelectionBlock();

        //Creating the user text input rows
        for (int i=0;i<4;i++) { createInputFieldsRow(elements[i], radicals[i], components[i]); }

        //Setting click listeners for the rows
        for (int i=0;i<4;i++) { setClickListenersForRow(elements[i], radicals[i], components[i]); }

    }
    public void createSelectedStructureSelectionBlock() {

        // Creating the structure and substrucutre selection block
        selected_structures_selection_block_layoutparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        selected_structures_selection_block_layoutparams.gravity = Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL;
        selected_structures_selection_block_layoutparams.setMargins(30, 5, 30, 0); // (left, top, right, bottom)

        selected_structures_selection_block_linearlayout = new LinearLayout(getContext());
        selected_structures_selection_block_linearlayout.setOrientation(LinearLayout.VERTICAL);
        selected_structures_selection_block_linearlayout.setLayoutParams(selected_structures_selection_block_layoutparams);
        selected_structures_selection_block_linearlayout.setFocusable(true);
        selected_structures_selection_block_linearlayout.setClickable(true);

        // Configuring the layout of category_chooser_row
        LinearLayout.LayoutParams selected_category_chooser_row_layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        selected_category_chooser_row_layoutParams.setMargins(10, 20, 10, 10); // (left, top, right, bottom)
        selected_category_chooser_row_layoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;

        LinearLayout selected_category_chooser_row_Layout = new LinearLayout(getContext());
        selected_category_chooser_row_Layout.setLayoutParams(selected_category_chooser_row_layoutParams);
        selected_category_chooser_row_Layout.setOrientation(LinearLayout.HORIZONTAL);
        selected_category_chooser_row_Layout.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        selected_category_chooser_row_Layout.setFocusable(true);
        selected_category_chooser_row_Layout.setClickable(true);
        selected_category_chooser_row_Layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideSoftKeyboard();
                return false;
            }
        });

        selected_structures_selection_block_linearlayout.addView(selected_category_chooser_row_Layout);
        userselections_block_linearLayout.addView(selected_structures_selection_block_linearlayout);

        int side_padding = 30;
        int top_padding = 0;

        selected_structure_overlapping = new ImageView(getContext());
        selected_structure_overlapping.setPadding(side_padding,top_padding,side_padding,0);
        selected_structure_overlapping.setImageResource(R.drawable.colored_structure_2_overlapping);
        selected_structure_overlapping.setClickable(true);
        selected_structure_overlapping.setId(View.generateViewId());
        selected_category_chooser_row_Layout.addView(selected_structure_overlapping);

        selected_structure_across = new ImageView(getContext());
        selected_structure_across.setPadding(side_padding,top_padding,side_padding,0);
        selected_structure_across.setImageResource(R.drawable.colored_structure_2_left_right);
        selected_structure_across.setClickable(true);
        selected_structure_across.setId(View.generateViewId());
        selected_category_chooser_row_Layout.addView(selected_structure_across);

        selected_structure_down = new ImageView(getContext());
        selected_structure_down.setPadding(side_padding,top_padding,side_padding,0);
        selected_structure_down.setImageResource(R.drawable.colored_structure_2_up_down);
        selected_structure_down.setClickable(true);
        selected_structure_down.setId(View.generateViewId());
        selected_category_chooser_row_Layout.addView(selected_structure_down);

        selected_structure_enclosure = new ImageView(getContext());
        selected_structure_enclosure.setPadding(side_padding,top_padding,side_padding,0);
        selected_structure_enclosure.setImageResource(R.drawable.colored_structure_2_enclosing_topleft_to_bottomright);
        selected_structure_enclosure.setClickable(true);
        selected_structure_enclosure.setId(View.generateViewId());
        selected_category_chooser_row_Layout.addView(selected_structure_enclosure);

        selected_structure_multiple = new ImageView(getContext());
        selected_structure_multiple.setPadding(side_padding,top_padding,side_padding,0);
        selected_structure_multiple.setImageResource(R.drawable.colored_structure_3_upwards_triangle);
        selected_structure_multiple.setClickable(true);
        selected_structure_multiple.setId(View.generateViewId());
        selected_category_chooser_row_Layout.addView(selected_structure_multiple);

        // Setting the click listeners
        selected_structure_overlapping.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chosen_selected_category_index = 0;
                hideSoftKeyboard();
                resetLinearLayoutViews(selected_structures_selection_block_linearlayout,1);
                selected_structure = setCategoryBasedOnUserSelection(chosen_selected_category_index, 0);
                setStructureColorFilterToGreen(selected_structure_overlapping, selected_substructures[0]);
            }
        });
        selected_structure_across.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chosen_selected_category_index = 1;
                int[] structure = {R.drawable.colored_structure_2_left_right,
                        R.drawable.colored_structure_3_left_center_right,
                        R.drawable.colored_structure_4_left_right,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0};
                hideSoftKeyboard();
                resetLinearLayoutViews(selected_structures_selection_block_linearlayout,1);
                createSelectedSubStructuresLayoutUponStructureSelection(selected_structure_across, structure);
                setStructureColorFilterToGreen(selected_structure_across, selected_substructures[0]);
            }
        });
        selected_structure_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chosen_selected_category_index = 2;
                int[] structure = {R.drawable.colored_structure_2_up_down,
                        R.drawable.colored_structure_3_up_center_down,
                        R.drawable.colored_structure_4_up_down,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0};
                hideSoftKeyboard();
                resetLinearLayoutViews(selected_structures_selection_block_linearlayout,1);
                createSelectedSubStructuresLayoutUponStructureSelection(selected_structure_down, structure);
                setStructureColorFilterToGreen(selected_structure_down, selected_substructures[0]);
            }
        });
        selected_structure_enclosure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chosen_selected_category_index = 3;
                int[] structure = {
                        R.drawable.colored_structure_2_enclosing_topleft_to_bottomright,
                        R.drawable.colored_structure_2_enclosing_top_to_bottom,
                        R.drawable.colored_structure_2_enclosing_topright_to_bottomleft,
                        R.drawable.colored_structure_2_enclosing_left_to_right,
                        R.drawable.colored_structure_2_outlining,
                        R.drawable.colored_structure_2_enclosing_bottomleft_to_topright,
                        R.drawable.colored_structure_2_enclosing_bottom_to_top,
                        0,0};
                hideSoftKeyboard();
                resetLinearLayoutViews(selected_structures_selection_block_linearlayout,1);
                createSelectedSubStructuresLayoutUponStructureSelection(selected_structure_enclosure, structure);
                hideSoftKeyboard();
                setStructureColorFilterToGreen(selected_structure_enclosure, selected_substructures[0]);
            }
        });
        selected_structure_multiple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chosen_selected_category_index = 4;
                int[] structure = {
                        R.drawable.colored_structure_3_upwards_triangle,
                        R.drawable.colored_structure_4_square_repeat,
                        R.drawable.colored_structure_4_square,
                        0,//R.drawable.colored_structure_5_hourglass,
                        0,
                        0,
                        0,
                        0,
                        0};
                hideSoftKeyboard();
                resetLinearLayoutViews(selected_structures_selection_block_linearlayout,1);
                createSelectedSubStructuresLayoutUponStructureSelection(selected_structure_multiple, structure);
                setStructureColorFilterToGreen(selected_structure_multiple, selected_substructures[0]);
            }
        });

        selected_structure_overlapping.performClick();
    }
    public void createSelectedSubStructuresLayoutUponStructureSelection(final ImageView chosen_selected_structure, int[] structure) {

        // Configuring the layout of the container View
        LinearLayout.LayoutParams local_row_container_layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        local_row_container_layoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;
        LinearLayout local_row_container_Layout = new LinearLayout(getContext());
        local_row_container_Layout.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);

        // Configuring the layout of the enclosing ScrollView
        HorizontalScrollView.LayoutParams scrollview_layoutParams = new HorizontalScrollView.LayoutParams(HorizontalScrollView.LayoutParams.WRAP_CONTENT, HorizontalScrollView.LayoutParams.WRAP_CONTENT);
        scrollview_layoutParams.setMargins(0, 10, 0, 5); // (left, top, right, bottom)
        scrollview_layoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;

        HorizontalScrollView scrollview_layout = new HorizontalScrollView(getContext());
        scrollview_layout.setLayoutParams(scrollview_layoutParams);
        scrollview_layout.setBackgroundColor(getResources().getColor(R.color.White));
        scrollview_layout.setAlpha((float) 0.90);

        // Configuring the layout of local_row
        LinearLayout.LayoutParams local_row_layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        local_row_layoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;
        local_row_layoutParams.setMargins(10, 10, 10, 10); // (left, top, right, bottom)

        LinearLayout local_row_linearLayout = new LinearLayout(getContext());
        local_row_linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        local_row_linearLayout.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        local_row_linearLayout.setLayoutParams(local_row_layoutParams);

        // Configuring the elements of local_row
        selected_substructures = new ImageView[9];
        for (int i=0;i<9;i++) { selected_substructures[i] = null; }

        chosen_selected_subcategory_index = 0;
        for (int i=0; i<9; i++) {
            if (structure[i] != 0) {
                selected_substructures[i] = makeConstructionImage(structure[i], local_row_layoutParams, local_row_linearLayout);
                selected_substructures[i].setId(View.generateViewId());

                final int index = i;
                selected_substructures[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        for (int j=0;j<9;j++) {
                            if (selected_substructures[j] != null && selected_substructures[j].getId() == selected_substructures[index].getId()) {
                                chosen_selected_subcategory_index = j;
                                break;
                            }
                        }
                        setStructureColorFilterToGreen(chosen_selected_structure, selected_substructures[index]);
                        selected_structure = setCategoryBasedOnUserSelection(chosen_selected_category_index, chosen_selected_subcategory_index);
                    }
                });
            }
        }
        selected_substructures[0].performClick();

        scrollview_layout.addView(local_row_linearLayout);
        local_row_container_Layout.addView(scrollview_layout);
        selected_structures_selection_block_linearlayout.addView(local_row_container_Layout);

    }
    public void createInputFieldsRow(final EditText element, final Button radical, final Button component) {

        LinearLayout.LayoutParams row_layout_layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        row_layout_layoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;
        row_layout_layoutParams.setMargins(0, 0, 0, 0); // (left, top, right, bottom)

        LinearLayout row_layout_linearLayout = new LinearLayout(getContext());
        row_layout_linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        row_layout_linearLayout.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        row_layout_linearLayout.setLayoutParams(row_layout_layoutParams);

        row_layout_linearLayout.addView(element);
        row_layout_linearLayout.addView(radical);
        row_layout_linearLayout.addView(component);

        row_layout_layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;

        userselections_block_linearLayout.addView(row_layout_linearLayout);

    }
    public void setClickListenersForRow(final EditText element, final Button radical, final Button component) {
            element.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                        hideSoftKeyboard();
                    }
                    return false;
                }
            });
            element.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    elements[current_row_index].selectAll();
                    hideSoftKeyboard();

                }
            });

            radical.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    hideSoftKeyboard();

                    for (int i=0;i<4;i++) {

                        components[i].getBackground().clearColorFilter();
                        radicals[i].getBackground().clearColorFilter();
                        if (radical.getId() == radicals[i].getId()) {
                            current_row_index = i;
                            radical.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.GREEN));
                        }
                    }

                    resetLinearLayoutViews(userselections_block_linearLayout, number_of_default_views_in_selection_block);
                    number_of_views_added_in_block = 1;
                    mComponentSelectionType = "radical";
                    createSelectionGridBlock(element);

                }
            });
            component.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    hideSoftKeyboard();
                    resetLinearLayoutViews(userselections_block_linearLayout, number_of_default_views_in_selection_block);
                    number_of_views_added_in_block = 1;

                    for (int i=0;i<4;i++) {
                        components[i].getBackground().clearColorFilter();
                        radicals[i].getBackground().clearColorFilter();
                        if (component.getId() == components[i].getId()) {
                            current_row_index = i;
                            component.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, Color.GREEN));
                        }
                    }
                    mComponentSelectionType = "component";
                    createComponentStructuresSelectionBlock(element);

                }
            });
        }

    public void createSelectionGridBlock(EditText element) {

        show_grid = false;
        createGridBlock();
        createEnterCancelRow_Top(element);
        createEnterCancelRow_Bottom(element);
        startCreatingSelectionGridElementsAsynchronously();

    }
    private void startCreatingSelectionGridElementsAsynchronously() {
        if (getActivity()!=null) {
            showLoadingIndicator();
            LoaderManager loaderManager = getActivity().getSupportLoaderManager();
            Loader<String> roomDbSearchLoader = loaderManager.getLoader(ROOM_DB_COMPONENT_GRID_LOADER);
            if (roomDbSearchLoader == null) loaderManager.initLoader(ROOM_DB_COMPONENT_GRID_LOADER, null, this);
            else loaderManager.restartLoader(ROOM_DB_COMPONENT_GRID_LOADER, null, this);
        }
    }
    public void displayGridElements() {

        //Making the grid container
        LinearLayout.LayoutParams grid_container_layoutParams = new LinearLayout.LayoutParams(1000, ViewGroup.LayoutParams.WRAP_CONTENT); // (1000, 500);
        grid_container_layoutParams.gravity = Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL;
        int num_columns = 7;
        grid_container_layoutParams.setMargins(10,0,10,0);
        final LinearLayout grid_container = new LinearLayout(getContext());
        grid_container.setLayoutParams(grid_container_layoutParams);
        grid_container.setOrientation(LinearLayout.HORIZONTAL);
        grid_container.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL);
        grid_container.setSelected(false);

        grid_container.removeAllViews();

        //If the screen is small, change the width of the container
        WindowManager wm = (WindowManager) getContext().getSystemService(getContext().WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        if (width<1000) {
            grid_container_layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
            num_columns = 5;
        }

        //Setting the grid parameters
        GridView.LayoutParams grid_layoutParams = new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        grid_layoutParams.height = GridLayout.LayoutParams.MATCH_PARENT;
        grid_layoutParams.width  = GridLayout.LayoutParams.MATCH_PARENT;

        grid = new GridView(getContext());
        grid.setLayoutParams(grid_layoutParams);
        grid.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        grid.setSelected(false);
        grid.setPadding(10,10,10,10);
        grid.setNumColumns(num_columns);
        grid.setMinimumHeight(100);
        grid.setColumnWidth(10);

        final float density = getContext().getResources().getDisplayMetrics().density;
        grid_row_height = (int) (40 * density + 0.5f);

        //Setting the grid
        if (0 < mDisplayableComponentSelections.size() && mDisplayableComponentSelections.size() < num_columns) {

            //Setting the layout
            LinearLayout.LayoutParams selectionsLine_layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            selectionsLine_layoutParams.gravity = Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL;
            selectionsLine_layoutParams.setMargins(10, 10, 10, 0); // (left, top, right, bottom)

            LinearLayout selectionsLine_linearLayout = new LinearLayout(getContext());
            selectionsLine_linearLayout.setOrientation(LinearLayout.HORIZONTAL);
            selectionsLine_linearLayout.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            selectionsLine_linearLayout.setLayoutParams(selectionsLine_layoutParams);

            //Updating the textviews in the layout
            for (int i = 0; i< mDisplayableComponentSelections.size(); i++) {

                TextView tv = new TextView(getContext());
                tv.setText(mDisplayableComponentSelections.get(i));
                setDefaultGridElementTextCharacteristics(tv);
                setActionPerformedOnGridElementTextClick(tv, i);
                selectionsLine_linearLayout.addView(tv);

                tv = new TextView(getContext());
                tv.setText("   ");
                if (i < mDisplayableComponentSelections.size()) {selectionsLine_linearLayout.addView(tv);}
            }

            grid_container.addView(selectionsLine_linearLayout);
            grid_container_layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            grid_container_layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        }
        else if (mDisplayableComponentSelections.size() != 0) {
            ArrayAdapter gridview_adapter =  new ArrayAdapter<String>(getContext(), R.layout.custom_radical_selection_grid_element, mDisplayableComponentSelections) {
                @Override
                public View getView(final int position,View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);

                    //Defining the font of each element depending on its characteristics
                    final TextView tv = (TextView) view;
                    setDefaultGridElementTextCharacteristics(tv);

                    //Defining what happens when a user clicks on an element
                    setActionPerformedOnGridElementTextClick(tv, position);

                    return tv;
                }
            };
            grid.setAdapter(gridview_adapter);
            setDynamicHeight(grid, grid_layoutParams, grid_container, grid_container_layoutParams, num_columns);
            grid_container.addView(grid);
        }
        else {
            makeText("No Grid Elements Found", grid_container_layoutParams, grid_container);
        }


        if (mDisplayableComponentSelections.size() != 0) { grid_block_linearLayout.addView(grid_container); }
    }
    public void createEnterCancelRow_Top(final EditText element) {

        Button button_enter_top = makeCharacterSelectionButton("enter", 0);
        Button button_cancel_top = makeCharacterSelectionButton("cancel", 0);
        input_row_enter_cancel_top = makeInputRow(button_enter_top, button_cancel_top);

        grid_block_linearLayout.addView(input_row_enter_cancel_top);
        grid_block_layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;

        button_enter_top.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftKeyboard();
                hideGrid();
                element.setText(user_selections[current_row_index]);
                if (component_structures_selection_block_linearlayout != null) { component_structures_selection_block_linearlayout.setVisibility(View.GONE);}
            }
        });
        button_cancel_top.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftKeyboard();
                hideGrid();
                if (component_structures_selection_block_linearlayout != null) { component_structures_selection_block_linearlayout.setVisibility(View.GONE); }
            }
        });
    }
    public void createEnterCancelRow_Bottom(final EditText element) {

        Button button_enter_bottom = makeCharacterSelectionButton("enter", 0);
        Button button_cancel_bottom = makeCharacterSelectionButton("cancel",0);
        input_row_enter_cancel_bottom = makeInputRow(button_enter_bottom, button_cancel_bottom);

        button_enter_bottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftKeyboard();
                hideGrid();
                element.setText(user_selections[current_row_index]);
                if (component_structures_selection_block_linearlayout!=null) component_structures_selection_block_linearlayout.setVisibility(View.GONE);
            }
        });
        button_cancel_bottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftKeyboard();
                hideGrid();
                if (component_structures_selection_block_linearlayout!=null) component_structures_selection_block_linearlayout.setVisibility(View.GONE);
            }
        });
    }
    public void createGridBlock() {

        if (grid_block_linearLayout != null) { grid_block_linearLayout.removeAllViews(); }

        grid_block_layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        grid_block_layoutParams.gravity = Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL;
        grid_block_layoutParams.setMargins(10, 5, 10, 0); // (left, top, right, bottom)

        grid_block_linearLayout = new LinearLayout(getContext());
        grid_block_linearLayout.setOrientation(LinearLayout.VERTICAL);
        grid_block_linearLayout.setLayoutParams(grid_block_layoutParams);
        grid_block_linearLayout.setFocusable(true);
        grid_block_linearLayout.setClickable(true);

        grid_block_linearLayout.removeAllViews();

        userselections_block_linearLayout.addView(grid_block_linearLayout);
    }
    public void setDefaultGridElementTextCharacteristics(TextView tv) {

        //No layout params are to be set for the tv textview, this will cause a crash in the gridview due to params conflict

        String tv_text = tv.getText().toString();
        tv.setHeight(grid_row_height);
        //tv.setTypeface(MainActivity.CJK_typeface, Typeface.NORMAL);

        if (tv_text.contains("0") || tv_text.contains("1") || tv_text.contains("2") || tv_text.contains("3")
                || tv_text.contains("4") || tv_text.contains("5") || tv_text.contains("6")
                || tv_text.contains("7") || tv_text.contains("8") || tv_text.contains("9")) {
            tv.setTextSize(24);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextColor(Color.RED);
        }
        else if (tv_text.contains("variant")) {
            tv.setTextSize(28);
            tv.setText(tv_text.substring(0,1));
            tv.setTextColor(Color.GREEN);
        }
        else {
            tv.setTextSize(28);
            tv.setText(tv_text);
            tv.setTextColor(Color.BLUE);
            //tv.setTextColor(Color.parseColor("#800080"));
        }
    }
    public void setActionPerformedOnGridElementTextClick(final TextView tv, final int position) {
            String tv_text = tv.getText().toString();
            if (!(tv_text.contains("0") || tv_text.contains("1") || tv_text.contains("2") || tv_text.contains("3")
                    || tv_text.contains("4") || tv_text.contains("5") || tv_text.contains("6")
                    || tv_text.contains("7") || tv_text.contains("8") || tv_text.contains("9"))) {
                tv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Getting the user selection

                        //Removing the highlight from the previously selected textview
                        if (user_selections_textviews[current_row_index] != null){
                            setDefaultGridElementTextCharacteristics(user_selections_textviews[current_row_index]);
                        }

                        //Updating the user selected textviews with the new user choice and highlighting it
                        user_selections[current_row_index] = tv.getText().toString();
                        if (mComponentSelectionType.contains("radical")) { user_selections_radical_positions[current_row_index] = position;}
                        else { user_selections_component_positions[current_row_index] = position;}
                        user_selections_textviews[current_row_index] = tv;
                        tv.setTextColor(Color.parseColor("#800080"));

                        radical_module_user_selections = new String[4];
                        radical_module_user_selections = Arrays.copyOf(user_selections, 4);

                    }
                });
            }
        }

    public void createComponentStructuresSelectionBlock(final EditText element) {

        //Creating the structure and substructure selection block
        component_structures_selection_block_layoutparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        component_structures_selection_block_layoutparams.gravity = Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL;
        component_structures_selection_block_layoutparams.setMargins(30, 5, 30, 0); // (left, top, right, bottom)

        component_structures_selection_block_linearlayout = new LinearLayout(getContext());
        component_structures_selection_block_linearlayout.setOrientation(LinearLayout.VERTICAL);
        component_structures_selection_block_linearlayout.setLayoutParams(component_structures_selection_block_layoutparams);
        component_structures_selection_block_linearlayout.setFocusable(true);
        component_structures_selection_block_linearlayout.setClickable(true);
        component_structures_selection_block_linearlayout.setVisibility(View.VISIBLE);

        component_structures_selection_block_linearlayout.removeAllViews();

        // Configuring the layout of category_chooser_row
        LinearLayout.LayoutParams category_chooser_row_layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        category_chooser_row_layoutParams.setMargins(10, 20, 10, 10); // (left, top, right, bottom)
        category_chooser_row_layoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;

        LinearLayout category_chooser_row_Layout = new LinearLayout(getContext());
        category_chooser_row_Layout.setLayoutParams(category_chooser_row_layoutParams);
        category_chooser_row_Layout.setOrientation(LinearLayout.HORIZONTAL);
        category_chooser_row_Layout.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        category_chooser_row_Layout.setFocusable(true);
        category_chooser_row_Layout.setClickable(true);
        category_chooser_row_Layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideSoftKeyboard();
                return false;
            }
        });

        resetLinearLayoutViews(userselections_block_linearLayout, number_of_default_views_in_selection_block + number_of_views_added_in_block);

        component_structures_selection_block_linearlayout.addView(category_chooser_row_Layout);
        userselections_block_linearLayout.addView(component_structures_selection_block_linearlayout);

        int side_padding = 30;
        int top_padding = 0;

        component_structure_across = new ImageView(getContext());
        component_structure_across.setPadding(side_padding,top_padding,side_padding,0);
        component_structure_across.setImageResource(R.drawable.colored_structure_2_left_right);
        component_structure_across.setClickable(true);
        component_structure_across.setId(View.generateViewId());
        category_chooser_row_Layout.addView(component_structure_across);

        component_structure_down = new ImageView(getContext());
        component_structure_down.setPadding(side_padding,top_padding,side_padding,0);
        component_structure_down.setImageResource(R.drawable.colored_structure_2_up_down);
        component_structure_down.setClickable(true);
        component_structure_down.setId(View.generateViewId());
        category_chooser_row_Layout.addView(component_structure_down);

        component_structure_enclosure = new ImageView(getContext());
        component_structure_enclosure.setPadding(side_padding,top_padding,side_padding,0);
        component_structure_enclosure.setImageResource(R.drawable.colored_structure_2_enclosing_topleft_to_bottomright);
        component_structure_enclosure.setClickable(true);
        component_structure_enclosure.setId(View.generateViewId());
        category_chooser_row_Layout.addView(component_structure_enclosure);

        component_structure_multiple = new ImageView(getContext());
        component_structure_multiple.setPadding(side_padding,top_padding,side_padding,0);
        component_structure_multiple.setImageResource(R.drawable.colored_structure_3_upwards_triangle);
        component_structure_multiple.setClickable(true);
        component_structure_multiple.setId(View.generateViewId());
        category_chooser_row_Layout.addView(component_structure_multiple);

        // Setting the click listeners
        component_structure_across.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chosen_component_category_index = 1;
                int[] structure = {R.drawable.colored_structure_2_left_right,
                        R.drawable.colored_structure_3_left_center_right,
                        R.drawable.colored_structure_4_left_right,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0};
                hideSoftKeyboard();
                resetLinearLayoutViews(userselections_block_linearLayout, number_of_default_views_in_selection_block + number_of_views_added_in_block);
                resetLinearLayoutViews(component_structures_selection_block_linearlayout, 1);
                createComponentSubStructuresLayoutUponStructureSelection(component_structure_across, structure, element);
                setStructureColorFilterToGreen(component_structure_across, component_substructures[0]);
                if (component_substructures[0]!=null) component_substructures[0].performClick();
            }
        });
        component_structure_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chosen_component_category_index = 2;
                int[] structure = {R.drawable.colored_structure_2_up_down,
                        R.drawable.colored_structure_3_up_center_down,
                        R.drawable.colored_structure_4_up_down,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0};
                hideSoftKeyboard();
                resetLinearLayoutViews(userselections_block_linearLayout, number_of_default_views_in_selection_block + number_of_views_added_in_block);
                resetLinearLayoutViews(component_structures_selection_block_linearlayout, 1);
                createComponentSubStructuresLayoutUponStructureSelection(component_structure_down, structure, element);
                setStructureColorFilterToGreen(component_structure_down, component_substructures[0]);
                if (component_substructures[0]!=null) component_substructures[0].performClick();
            }
        });
        component_structure_enclosure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chosen_component_category_index = 3;
                int[] structure = {
                        R.drawable.colored_structure_2_enclosing_topleft_to_bottomright,
                        R.drawable.colored_structure_2_enclosing_top_to_bottom,
                        R.drawable.colored_structure_2_enclosing_topright_to_bottomleft,
                        R.drawable.colored_structure_2_enclosing_left_to_right,
                        R.drawable.colored_structure_2_outlining,
                        R.drawable.colored_structure_2_enclosing_bottomleft_to_topright,
                        R.drawable.colored_structure_2_enclosing_bottom_to_top,
                        0,0};
                hideSoftKeyboard();
                resetLinearLayoutViews(userselections_block_linearLayout, number_of_default_views_in_selection_block + number_of_views_added_in_block);
                resetLinearLayoutViews(component_structures_selection_block_linearlayout, 1);
                createComponentSubStructuresLayoutUponStructureSelection(component_structure_enclosure, structure, element);
                hideSoftKeyboard();
                setStructureColorFilterToGreen(component_structure_enclosure, component_substructures[0]);
                if (component_substructures[0]!=null) component_substructures[0].performClick();
            }
        });
        component_structure_multiple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chosen_component_category_index = 4;
                int[] structure = {
                        R.drawable.colored_structure_3_upwards_triangle,
                        R.drawable.colored_structure_4_square_repeat,
                        R.drawable.colored_structure_4_square,
                        0,//R.drawable.colored_structure_5_hourglass,
                        0,
                        0,
                        0,
                        0,
                        0};
                hideSoftKeyboard();
                resetLinearLayoutViews(userselections_block_linearLayout, number_of_default_views_in_selection_block + number_of_views_added_in_block);
                resetLinearLayoutViews(component_structures_selection_block_linearlayout, 1);
                createComponentSubStructuresLayoutUponStructureSelection(component_structure_multiple, structure, element);
                setStructureColorFilterToGreen(component_structure_multiple, component_substructures[0]);
                if (component_substructures[0]!=null) component_substructures[0].performClick();
            }
        });

        component_structure_across.performClick();


    }
    public void createComponentSubStructuresLayoutUponStructureSelection(final ImageView chosen_component_structure, int[] structure, final EditText element) {

        // Configuring the layout of the container View
        LinearLayout.LayoutParams local_row_container_layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        local_row_container_layoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;
        LinearLayout local_row_container_Layout = new LinearLayout(getContext());
        local_row_container_Layout.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);

        // Configuring the layout of the enclosing ScrollView
        HorizontalScrollView.LayoutParams scrollview_layoutParams = new HorizontalScrollView.LayoutParams(HorizontalScrollView.LayoutParams.WRAP_CONTENT, HorizontalScrollView.LayoutParams.WRAP_CONTENT);
        scrollview_layoutParams.setMargins(0, 10, 0, 5); // (left, top, right, bottom)
        scrollview_layoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;

        HorizontalScrollView scrollview_layout = new HorizontalScrollView(getContext());
        scrollview_layout.setLayoutParams(scrollview_layoutParams);
        scrollview_layout.setBackgroundColor(Color.WHITE);
        scrollview_layout.setAlpha((float) 0.90);

        // Configuring the layout of local_row
        LinearLayout.LayoutParams local_row_layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        local_row_layoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;
        local_row_layoutParams.setMargins(10, 10, 10, 10); // (left, top, right, bottom)

        LinearLayout local_row_linearLayout = new LinearLayout(getContext());
        local_row_linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        local_row_linearLayout.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        local_row_linearLayout.setLayoutParams(local_row_layoutParams);

        // Configuring the elements of local_row

        chosen_component_subcategory_index = 1;
        for (int i=0; i<9; i++) {
            if (structure[i] != 0) {
                component_substructures[i] = makeConstructionImage(structure[i], local_row_layoutParams, local_row_linearLayout);
                component_substructures[i].setId(View.generateViewId());
                final int index = i;
                component_substructures[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        for (int j=0;j<9;j++) {
                            if (component_substructures[j] != null && component_substructures[j].getId() == component_substructures[index].getId()) {
                                chosen_component_subcategory_index = j;
                                break;
                            }
                        }

                        resetLinearLayoutViews(userselections_block_linearLayout, number_of_default_views_in_selection_block + number_of_views_added_in_block);
                        setStructureColorFilterToGreen(chosen_component_structure, component_substructures[index]);
                        chosen_components_list = setCategoryBasedOnUserSelection(chosen_component_category_index, chosen_component_subcategory_index);
                        createSelectionGridBlock(element);

                    }
                });
            }
        }
        //component_substructures[0].performClick();

        scrollview_layout.addView(local_row_linearLayout);
        local_row_container_Layout.addView(scrollview_layout);
        component_structures_selection_block_linearlayout.addView(local_row_container_Layout);

    }
    public int setCategoryBasedOnUserSelection(int chosen_category_index, int chosen_subcategory_index){

            //Getting the search category based on the user selection
            int returned_components_list = 0;
            if (chosen_category_index == 0) { returned_components_list = GlobalConstants.Index_full; }
            if (chosen_category_index == 1) {
                if      (chosen_subcategory_index == 0) { returned_components_list = GlobalConstants.Index_across2; }
                else if (chosen_subcategory_index == 1) { returned_components_list = GlobalConstants.Index_across3; }
                else if (chosen_subcategory_index == 2) { returned_components_list = GlobalConstants.Index_across4; }
            }
            else if (chosen_category_index == 2) {
                if      (chosen_subcategory_index == 0) { returned_components_list = GlobalConstants.Index_down2; }
                else if (chosen_subcategory_index == 1) { returned_components_list = GlobalConstants.Index_down3; }
                else if (chosen_subcategory_index == 2) { returned_components_list = GlobalConstants.Index_down4; }
            }
            else if (chosen_category_index == 3) {
                if      (chosen_subcategory_index == 0)       { returned_components_list = GlobalConstants.Index_topleftout;}
                else if (chosen_subcategory_index == 1)       { returned_components_list = GlobalConstants.Index_topout;}
                else if (chosen_subcategory_index == 2)       { returned_components_list = GlobalConstants.Index_toprightout;}
                else if (chosen_subcategory_index == 3)       { returned_components_list = GlobalConstants.Index_leftout;}
                else if (chosen_subcategory_index == 4)       { returned_components_list = GlobalConstants.Index_fullout;}
                else if (chosen_subcategory_index == 5)       { returned_components_list = GlobalConstants.Index_bottomleftout;}
                else if (chosen_subcategory_index == 6)       { returned_components_list = GlobalConstants.Index_bottomout;}
            }
            else if (chosen_category_index == 4) {
                if      (chosen_subcategory_index == 0)       { returned_components_list = GlobalConstants.Index_three_repeat; }
                else if (chosen_subcategory_index == 1)       { returned_components_list = GlobalConstants.Index_foursquare; }
                else if (chosen_subcategory_index == 2)       { returned_components_list = GlobalConstants.Index_four_repeat; }
                else if (chosen_subcategory_index == 3)       { returned_components_list = GlobalConstants.Index_five_repeat; }
            }

            return returned_components_list;
        }

    public void createSearchResultsBlock() {

        //Creating the layout
        search_results_block_layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        search_results_block_layoutParams.gravity = Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL;
        search_results_block_layoutParams.setMargins(30, 5, 30, 0); // (left, top, right, bottom)

        search_results_block_linearLayout = new LinearLayout(getContext());
        search_results_block_linearLayout.setOrientation(LinearLayout.VERTICAL);
        search_results_block_linearLayout.setLayoutParams(search_results_block_layoutParams);
        search_results_block_linearLayout.setFocusable(true);
        search_results_block_linearLayout.setClickable(true);

        //Creating the search button and Getting the search results when the user presses it
        search_for_char = makeCharacterSelectionButton("Search", 400);
        search_results_block_linearLayout.addView(search_for_char);

        mOverallBlockContainerLinearLayout.addView(search_results_block_linearLayout);

        search_for_char.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {

            resetLinearLayoutViews(search_results_block_linearLayout, 1);
            makeText("Results", search_results_block_layoutParams, search_results_block_linearLayout);

            String[] elements_strings = new String[4];
            for (int i=0;i<4;i++) {
                if (elements[i] != null) {
                    elements_strings[i] = elements[i].getText().toString();
                    user_selections[i] = elements[i].getText().toString();
                }
            }

            startSearchingForKanjisAsynchronously(elements_strings);

        } });

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
    public void createSearchResultsGrid(final List<String> printable_search_results) {

                LinearLayout.LayoutParams grid_container_layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                grid_container_layoutParams.height = 200;
                grid_container_layoutParams.gravity = Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL;
                grid_container_layoutParams.setMargins(10,0,10,0);
                int num_columns = 7;

                //If the screen is small, change the width of the container
                WindowManager wm = (WindowManager) getContext().getSystemService(getContext().WINDOW_SERVICE);
                Display display = wm.getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int width = size.x;
                int height = size.y;
                if (width<1000) {
                    grid_container_layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
                    num_columns = 5;
                }

                LinearLayout grid_container = new LinearLayout(getContext());
                grid_container.setLayoutParams(grid_container_layoutParams);
                grid_container.setOrientation(LinearLayout.VERTICAL);
                grid_container.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL);
                grid_container.setSelected(false);

                //Set the grid parameters
                GridView.LayoutParams searchResultsGrid_layoutParams = new GridView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                searchResultsGrid_layoutParams.height = 200;

                searchResultsGrid = new GridView(getContext());
                searchResultsGrid.setLayoutParams(searchResultsGrid_layoutParams);
                searchResultsGrid.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                searchResultsGrid.setSelected(false);
                searchResultsGrid.setPadding(10,10,10,10);
                searchResultsGrid.setNumColumns(num_columns);
                searchResultsGrid.setMinimumHeight(110);
                searchResultsGrid.setColumnWidth(10);

                //Create the grid
                if (0 < printable_search_results.size() && printable_search_results.size() <= num_columns) {

                    LinearLayout.LayoutParams searchResultsLine_layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
                    searchResultsLine_layoutParams.gravity = Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL;
                    searchResultsLine_layoutParams.setMargins(10, 10, 10, 0); // (left, top, right, bottom)

                    LinearLayout searchResultsLine_linearLayout = new LinearLayout(getContext());
                    searchResultsLine_linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                    searchResultsLine_linearLayout.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                    searchResultsLine_linearLayout.setLayoutParams(searchResultsLine_layoutParams);

                    for (int i=0; i<printable_search_results.size(); i++) {

                        final TextView tv = new TextView(getContext());
                        //tv.setTypeface(MainActivity.CJK_typeface, Typeface.NORMAL);
                        tv.setTextSize(32);
                        tv.setText(printable_search_results.get(i));
                        tv.setPadding(30,0,30,0);
                        tv.setTextColor(Color.parseColor("#800080"));
                        tv.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                //The following code "initializes" the interface, since it is not necessarily called (initialized) when the grammar fragment receives the inputQueryAutoCompleteTextView and is activated
                                try {
                                    searchByRadicalFragmentOperationsHandler = (SearchByRadicalFragmentOperationsHandler) getActivity();
                                } catch (ClassCastException e) {
                                    throw new ClassCastException(getActivity().toString() + " must implement TextClicked");
                                }

                                String outputText = tv.getText().toString();
                                searchByRadicalFragmentOperationsHandler.onQueryTextUpdateFromSearchByRadicalRequested(outputText);
                            }
                        });
                        searchResultsLine_linearLayout.addView(tv);
                    }
                    grid_container.addView(searchResultsLine_linearLayout);
                    search_results_block_linearLayout.addView(grid_container);
                }
                else if (printable_search_results.size() != 0) {

                    //Creating the last row in the list for centering
                    int remainder = printable_search_results.size();
                    while (remainder > num_columns-1) {
                        remainder = remainder - num_columns;
                    }
                    final List<String> remainder_list = new ArrayList<>();
                    if (remainder != 0) {
                        for (int i=0; i<remainder;i++) {
                            remainder_list.add(printable_search_results.get(printable_search_results.size()-remainder+i));
                        }
                        for (int i=0; i<remainder;i++) {
                            printable_search_results.remove(printable_search_results.size()-1);
                        }
                    }

                    LinearLayout.LayoutParams searchResultsLine_layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
                    searchResultsLine_layoutParams.gravity = Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL;
                    searchResultsLine_layoutParams.setMargins(10, 0, 10, 0); // (left, top, right, bottom)

                    LinearLayout lastSearchResultsLine_linearLayout = new LinearLayout(getContext());
                    lastSearchResultsLine_linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                    lastSearchResultsLine_linearLayout.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
                    lastSearchResultsLine_linearLayout.setLayoutParams(searchResultsLine_layoutParams);

                    final TextView[] tv_lastline = new TextView[remainder_list.size()];
                    for (int i=0; i<remainder_list.size(); i++) {

                        final TextView tv = new TextView(getContext());
                        //tv.setTypeface(MainActivity.CJK_typeface, Typeface.NORMAL);
                        tv.setTextSize(32);
                        tv.setText(remainder_list.get(i));
                        tv.setPadding(30,0,30,0);
                        tv.setTextColor(Color.parseColor("#800080"));
                        tv_lastline[i] = tv;
                        tv.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                //The following code "initializes" the interface, since it is not necessarily called (initialized) when the grammar fragment receives the inputQueryAutoCompleteTextView and is activated
                                try {
                                    searchByRadicalFragmentOperationsHandler = (SearchByRadicalFragmentOperationsHandler) getActivity();
                                } catch (ClassCastException e) {
                                    throw new ClassCastException(getActivity().toString() + " must implement TextClicked");
                                }

                                String outputText = tv.getText().toString();
                                searchByRadicalFragmentOperationsHandler.onQueryTextUpdateFromSearchByRadicalRequested(outputText);

                                int num_children = searchResultsGrid.getChildCount();
                                TextView tv_grid;
                                for (int i=0;i<num_children;i++) {
                                    tv_grid = (TextView) searchResultsGrid.getChildAt(i);
                                    tv_grid.setTextColor(getResources().getColor(R.color.textColorCompositionGridElementDefault));
                                }

                                for (int i=0; i<remainder_list.size();i++) {
                                    tv_lastline[i].setTextColor(getResources().getColor(R.color.textColorCompositionGridElementDefault));
                                }

                                tv.setTextColor(getResources().getColor(R.color.textColorCompositionGridElementSelected));
                            }
                        });
                        lastSearchResultsLine_linearLayout.addView(tv);
                    }

                    searchResultsGrid.setAdapter(new ArrayAdapter<String>(getContext(), R.layout.custom_radical_results_grid_element, printable_search_results) {
                        public View getView(final int position, View convertView, ViewGroup parent) {
                            View view = super.getView(position, convertView, parent);
                            final TextView tv = (TextView) view;
                            //tv.setTypeface(MainActivity.CJK_typeface, Typeface.NORMAL);
                            tv.setTextSize(32);
                            tv.setText(printable_search_results.get(position));
                            tv.setTextColor(getResources().getColor(R.color.textColorCompositionGridElementDefault));

                            tv.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    //Getting the user selection
                                    //selected_item_position = position;
                                    //grid.setSelection((int) (grid.getAdapter()).getItemId(selected_item_position));

                                    //The following code "initializes" the interface, since it is not necessarily called (initialized) when the grammar fragment receives the inputQueryAutoCompleteTextView and is activated
                                    try {
                                        searchByRadicalFragmentOperationsHandler = (SearchByRadicalFragmentOperationsHandler) getActivity();
                                    } catch (ClassCastException e) {
                                        throw new ClassCastException(getActivity().toString() + " must implement TextClicked");
                                    }

                                    //Calling the interface
                                    //String outputText = printable_search_results.get(selected_item_position);
                                    String outputText = tv.getText().toString();
                                    searchByRadicalFragmentOperationsHandler.onQueryTextUpdateFromSearchByRadicalRequested(outputText);


                                    int num_children = searchResultsGrid.getChildCount();
                                    TextView tv_grid;
                                    for (int i=0;i<num_children;i++) {
                                        tv_grid = (TextView) searchResultsGrid.getChildAt(i);
                                        tv_grid.setTextColor(getResources().getColor(R.color.textColorCompositionGridElementDefault));
                                    }
                                    for (int i=0; i<remainder_list.size();i++) {
                                        tv_lastline[i].setTextColor(getResources().getColor(R.color.textColorCompositionGridElementDefault));
                                    }
                                    tv.setTextColor(getResources().getColor(R.color.textColorCompositionGridElementSelected));

                                }
                            });
                            return tv;
                        }
                    });

                    setDynamicHeight(searchResultsGrid, searchResultsGrid_layoutParams, grid_container, grid_container_layoutParams, num_columns);

                    grid_container.addView(searchResultsGrid);
                    search_results_block_linearLayout.addView(grid_container);
                    search_results_block_linearLayout.addView(lastSearchResultsLine_linearLayout);
                }
                else {
                    makeText("No Results Found",grid_container_layoutParams,grid_container);
                    search_results_block_linearLayout.addView(grid_container);
                }
            }

    private void showLoadingIndicator() {
        if (mProgressBarLoadingIndicator!=null) mProgressBarLoadingIndicator.setVisibility(View.VISIBLE);
    }
    private void hideLoadingIndicator() {
        if (mProgressBarLoadingIndicator!=null) mProgressBarLoadingIndicator.setVisibility(View.INVISIBLE);
    }



    // Layout Subfunctions
    public ImageView makeConstructionImage(int image_descriptor, LinearLayout.LayoutParams layoutParams, LinearLayout linearLayout) {

        ImageView img = new ImageView(getContext());
        img.setLayoutParams(layoutParams);
        img.setPadding(20,20,20,20); // (left, top, right, bottom)
        img.setImageResource(image_descriptor);
        img.setClickable(true);

        LinearLayout.LayoutParams image_params = (LinearLayout.LayoutParams) img.getLayoutParams();
        image_params.setMargins(10, 0, 10, 0); // (left, top, right, bottom)
        image_params.gravity = Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL;

        linearLayout.addView(img);
        return img;
    }
    public void resetLinearLayoutViews(LinearLayout linearLayout, int number_of_wanted_leftover_children_views) {

        int childCount = linearLayout.getChildCount();
        if (childCount > number_of_wanted_leftover_children_views) {
            while (childCount > number_of_wanted_leftover_children_views) {
                linearLayout.removeViewAt(number_of_wanted_leftover_children_views);
                childCount = linearLayout.getChildCount();
            }
        }

    }
    public void setStructureColorFilterToGreen(ImageView chosen_structure, ImageView chosen_substructure) {

        if (selected_structure_overlapping != null) {selected_structure_overlapping.clearColorFilter();}
        if (selected_structure_across != null)      {selected_structure_across.clearColorFilter();}
        if (selected_structure_down != null)        {selected_structure_down.clearColorFilter();}
        if (selected_structure_enclosure != null)   {selected_structure_enclosure.clearColorFilter();}
        if (selected_structure_multiple != null)    {selected_structure_multiple.clearColorFilter();}

        if (component_structure_across != null)     {component_structure_across.clearColorFilter();}
        if (component_structure_down != null)       {component_structure_down.clearColorFilter();}
        if (component_structure_enclosure != null)  {component_structure_enclosure.clearColorFilter();}
        if (component_structure_multiple != null)   {component_structure_multiple.clearColorFilter();}

        if (component_structure_across != null && chosen_structure.getId() == component_structure_across.getId()) {
            component_structure_across.setColorFilter(Color.GREEN, PorterDuff.Mode.LIGHTEN);
            highlightSubStructure(component_substructures, chosen_substructure);
        }
        else if (component_structure_down != null && chosen_structure.getId() == component_structure_down.getId()) {
            component_structure_down.setColorFilter(Color.GREEN, PorterDuff.Mode.LIGHTEN);
            highlightSubStructure(component_substructures, chosen_substructure);
        }
        else if (component_structure_enclosure != null && chosen_structure.getId() == component_structure_enclosure.getId()) {
            component_structure_enclosure.setColorFilter(Color.GREEN, PorterDuff.Mode.LIGHTEN);
            highlightSubStructure(component_substructures, chosen_substructure);
        }
        else if (component_structure_multiple != null && chosen_structure.getId() == component_structure_multiple.getId()) {
            component_structure_multiple.setColorFilter(Color.GREEN, PorterDuff.Mode.LIGHTEN);
            highlightSubStructure(component_substructures, chosen_substructure);
        }

        else if (selected_structure_overlapping != null && chosen_structure.getId() == selected_structure_overlapping.getId()) {
            selected_structure_overlapping.setColorFilter(Color.GREEN, PorterDuff.Mode.LIGHTEN);
        }
        else if (selected_structure_across != null && chosen_structure.getId() == selected_structure_across.getId()) {
            selected_structure_across.setColorFilter(Color.GREEN, PorterDuff.Mode.LIGHTEN);
            highlightSubStructure(selected_substructures, chosen_substructure);
        }
        else if (selected_structure_down != null && chosen_structure.getId() == selected_structure_down.getId()) {
            selected_structure_down.setColorFilter(Color.GREEN, PorterDuff.Mode.LIGHTEN);
            highlightSubStructure(selected_substructures, chosen_substructure);
        }
        else if (selected_structure_enclosure != null && chosen_structure.getId() == selected_structure_enclosure.getId()) {
            selected_structure_enclosure.setColorFilter(Color.GREEN, PorterDuff.Mode.LIGHTEN);
            highlightSubStructure(selected_substructures, chosen_substructure);
        }
        else if (selected_structure_multiple != null && chosen_structure.getId() == selected_structure_multiple.getId()) {
            selected_structure_multiple.setColorFilter(Color.GREEN, PorterDuff.Mode.LIGHTEN);
            highlightSubStructure(selected_substructures, chosen_substructure);
        }
    }
    public void highlightSubStructure(ImageView[] substructures, ImageView chosen_substructure) {
        for (int i=0; i<9; i++) {
            if (substructures[i] != null) {
                if (chosen_substructure.getId() == substructures[i].getId()) {
                    substructures[i].setColorFilter(Color.GREEN, PorterDuff.Mode.LIGHTEN);
                } else {
                    substructures[i].clearColorFilter();
                }
            }
        }
    }
    public void makeText(String text, LinearLayout.LayoutParams layoutParams, LinearLayout linearLayout) {
        TextView tv = new TextView(getContext());
        tv.setLayoutParams(layoutParams);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tv.setText(text);
        tv.setTextSize(16);
        //tv.setTextColor(getResources().getColor(R.color.textColorCompositionGridElementDefault));
        tv.setTextColor(getResources().getColor(R.color.colorPrimaryLight));
        tv.setTextIsSelectable(false);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setSelected(false);
        //tv.setTypeface(MainActivity.CJK_typeface, Typeface.NORMAL);
        tv.setBackgroundColor(getResources().getColor(R.color.White));
        tv.setAlpha((float) 0.90);
        tv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideSoftKeyboard();
                return false;
            }
        });
        linearLayout.addView(tv);
    }
    public LinearLayout makeInputRow(View view1, View view2) {

        LinearLayout.LayoutParams row_layout_layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        row_layout_layoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;
        row_layout_layoutParams.setMargins(0, 10, 0, 10); // (left, top, right, bottom)

        LinearLayout row_layout_linearLayout = new LinearLayout(getContext());
        row_layout_linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        row_layout_linearLayout.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        row_layout_linearLayout.setLayoutParams(row_layout_layoutParams);

        row_layout_linearLayout.addView(view1);
        row_layout_linearLayout.addView(view2);

        row_layout_layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;

        return row_layout_linearLayout;

    }
    public Button makeCharacterSelectionButton(String text, int width) {

        //Setting the layout params here prevents the button from adopting the layout params of its enclosing view
        LinearLayout.LayoutParams character_chooser_params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        character_chooser_params.gravity = Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL;


        Button character_chooser = new Button(getContext());
        character_chooser.setText(text);
        character_chooser.setLayoutParams(character_chooser_params);
        if (width != 0) {character_chooser.setWidth(width);}
        character_chooser.setHeight(10);
        character_chooser.setTextSize(12);
        character_chooser.setTextColor(getResources().getColor(R.color.Black));
        character_chooser.setTextIsSelectable(false);
        character_chooser.setTypeface(null, Typeface.BOLD);
        character_chooser.setSelected(false);
        character_chooser.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        character_chooser.setId(View.generateViewId());

        return character_chooser;
    }
    public EditText makeEditText(String text, String ghost_text) {

        EditText autocomplete_input = new EditText(getContext());
        autocomplete_input.setLayoutParams(userselections_overall_block_layoutParams);
        autocomplete_input.setText(text);
        autocomplete_input.setHeight(6);
        autocomplete_input.setSingleLine(true);
        autocomplete_input.setHint(ghost_text);
        autocomplete_input.setTextSize(16);
        autocomplete_input.setTextColor(getResources().getColor(R.color.textColorCompositionGridElementDefault));
        autocomplete_input.setTextIsSelectable(false);
        autocomplete_input.setTypeface(null, Typeface.BOLD);
        autocomplete_input.setMovementMethod(LinkMovementMethod.getInstance());
        autocomplete_input.setSelected(false);
        autocomplete_input.setBackgroundColor(getResources().getColor(R.color.White));
        autocomplete_input.setAlpha((float) 0.90);
        autocomplete_input.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        autocomplete_input.setId(View.generateViewId());
        //autocomplete_input.setSelectAllOnFocus(true);

        return autocomplete_input;
    }
    public void hideSoftKeyboard() {
        InputMethodManager inputMethodManager =(InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (getActivity().getCurrentFocus() != null) {
            inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
            //inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
        }
    }
    public void hideGrid() {
        show_grid = false;
        grid_block_linearLayout.setVisibility((View.GONE));
    }

    @TargetApi(23) public boolean isPrintable( String c ) {
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
    private void setDynamicHeight(GridView gridView, GridView.LayoutParams grid_params, LinearLayout grid_container, LinearLayout.LayoutParams grid_container_params, int num_columns) {
        ListAdapter gridViewAdapter = gridView.getAdapter();
        if (gridViewAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        int items = gridViewAdapter.getCount();
        int rows = 0;

        View listItem = gridViewAdapter.getView(1, null, gridView); //take the second element of the grid for measurements, since the first element is smaller
        listItem.measure(0, 0);
        totalHeight = listItem.getMeasuredHeight();

        double x = 1;
        if( items > num_columns ){
            int remainder = items;
            while (remainder>=num_columns) { remainder -= num_columns; }
            x = items/num_columns;

            //The following condition prevents the gridview from showing an extra empty row if items is a component_structure_multiple of num_columns
            if (remainder>0) rows = (int) Math.floor(x + 1);
            else rows = (int) Math.floor(x + 0.5);

            totalHeight = totalHeight*rows;
        }

        grid_container_params.height = totalHeight;
        grid_container.setLayoutParams(grid_container_params);

        if (x>1) {
            grid_params.height = totalHeight+15;
        }
        else {
            grid_params.height = totalHeight+30;
        }

        //grid_params.height = GridLayout.LayoutParams.MATCH_PARENT;
        //grid_params.width  = GridLayout.LayoutParams.MATCH_PARENT;
        gridView.setLayoutParams(grid_params);
    }


    //Asynchronous methods
    @NonNull @Override public Loader<Object> onCreateLoader(int id, final Bundle args) {

        if (id == ROOM_DB_COMPONENT_GRID_LOADER) {
            mAlreadyLoadedSelectionGrid = false;
            ComponentGridCreationAsyncTaskLoader gridCreationLoader = new ComponentGridCreationAsyncTaskLoader(
                    getContext(), mComponentSelectionType, mRadicalsOnlyDatabase, chosen_components_list);
            return gridCreationLoader;
        }
        else if (id == ROOM_DB_KANJI_SEARCH_LOADER) {
            mAlreadyLoadedKanjiSearchResults = false;
            String[] elements_list = args.getStringArray(getString(R.string.search_by_radical_elements_list));
            KanjiSearchAsyncTaskLoader kanjiSearchLoader = new KanjiSearchAsyncTaskLoader(
                    getContext(), elements_list, selected_structure);
            return kanjiSearchLoader;
        }
        else return new ComponentGridCreationAsyncTaskLoader(getContext(), "", null, 0);
    }
    @Override public void onLoadFinished(@NonNull Loader<Object> loader, Object data) {

        if (loader.getId() == ROOM_DB_COMPONENT_GRID_LOADER && !mAlreadyLoadedSelectionGrid && data!=null) {
            mAlreadyLoadedSelectionGrid = true;

            mDisplayableComponentSelections = (List<String>) data;

            hideLoadingIndicator();
            displayGridElements();
            grid_block_linearLayout.addView(input_row_enter_cancel_bottom);

            if (getLoaderManager()!=null) getLoaderManager().destroyLoader(ROOM_DB_COMPONENT_GRID_LOADER);
        }
        if (loader.getId() == ROOM_DB_KANJI_SEARCH_LOADER && !mAlreadyLoadedKanjiSearchResults && data!=null) {
            mAlreadyLoadedKanjiSearchResults = true;

            List<String> search_results = (List<String>) data;

            hideLoadingIndicator();

            //Displaying only the search results that have a glyph in the font
            List<String> printable_search_results;
            printable_search_results = new ArrayList<>();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                for (int i = 0; i < search_results.size(); i++) {
                    if (isPrintable(search_results.get(i).substring(0, 1))) {
                        printable_search_results.add(search_results.get(i));
                    }
                }
            }
            else {
                printable_search_results = search_results;
            }

            //Displaying only the first 400 values to prevent overload
            List<String> selections = new ArrayList<>();
            int display_limit = 400;
            for (int i = 0; i < printable_search_results.size(); i++) {
                selections.add(printable_search_results.get(i));
                if (i>display_limit) {break;}
            }
            printable_search_results = selections;

            createSearchResultsGrid(printable_search_results);
            if (getLoaderManager()!=null) getLoaderManager().destroyLoader(ROOM_DB_COMPONENT_GRID_LOADER);
        }
    }
    @Override public void onLoaderReset(@NonNull Loader<Object> loader) {}
    private static class ComponentGridCreationAsyncTaskLoader extends AsyncTaskLoader<Object> {

        //region Parameters
        private final String mComponentSelectionType;
        private final List<String[]> mRadicalsOnlyDatabase;
        private final int chosen_components_list;
        private List<String[]> sortedList;
        private JapaneseToolboxRoomDatabase mJapaneseToolboxRoomDatabase;
        //endregion

        ComponentGridCreationAsyncTaskLoader(Context context,
                                             String mComponentSelectionType,
                                             List<String[]> mRadicalsOnlyDatabase,
                                             int chosen_components_list) {
            super(context);
            this.mComponentSelectionType = mComponentSelectionType;
            this.mRadicalsOnlyDatabase = mRadicalsOnlyDatabase;
            this.chosen_components_list = chosen_components_list;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Override
        public Object loadInBackground() {

            mJapaneseToolboxRoomDatabase = JapaneseToolboxRoomDatabase.getInstance(getContext());
            List<String> mDisplayableComponentSelections = getSelectionGridElements();

            return mDisplayableComponentSelections;
        }

        List<String> getSelectionGridElements() {

            if (mComponentSelectionType.equals("")) return new ArrayList<>();

            //Creating the the list to be displayed in the dialog grid
            List<String> selections = new ArrayList<>();
            String[] currentElement;

            //If the user clicks on the radical button, then a radical selection grid is shown
            if (mComponentSelectionType.contains("radical")) {

                List<String> parsed_list;
                String last_index = "0";
                for (int i = 0; i < mRadicalsOnlyDatabase.size(); i++) {
                    currentElement = mRadicalsOnlyDatabase.get(i);
                    parsed_list = Arrays.asList(currentElement[2].split(";"));

                    // Adding the header radical numbers to the list of radicals
                    if (!currentElement[4].equals(last_index)) {
                        if (parsed_list.get(0).equals("Special") || parsed_list.get(0).equals("Hiragana") || parsed_list.get(0).equals("Katakana")) {
                        } else if (parsed_list.size() > 1) {
                            if (parsed_list.get(1).equals("variant")) {
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
                        if (parsed_list.size() > 1) {
                            if (parsed_list.get(1).equals("variant")) {
                                selections.add(currentElement[0] + "variant");
                            }
                        } else {
                            selections.add(currentElement[0]);
                        }
                        last_index = currentElement[4];
                    }
                }
            }

            //If the user clicks on the component button, then then the components grid is extracted from Room
            else {
                List<String[]> fullList = new ArrayList<>();
                String[] printableResultsForCurrentElement;
                Boolean containsAtLeastOnePrintableGlyph;

                //List<KanjiComponent> kanjiComponents = mJapaneseToolboxRoomDatabase.getAllKanjiComponents();
                String componentStructure = GlobalConstants.COMPONENT_STRUCTURES_MAP.get(chosen_components_list);
                if (!TextUtils.isEmpty(componentStructure)) {

                    List<KanjiComponent> kanjiComponents = mJapaneseToolboxRoomDatabase.getKanjiComponentsByStructureName(componentStructure);
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

            //Displaying only the search results that have a glyph in the font
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

            //Displaying only the first XX values to prevent overload
            int display_limit = 400;
            selections = new ArrayList<>();
            for (int i = 0; i < displayableComponentSelections.size(); i++) {
                selections.add(displayableComponentSelections.get(i));
                if (i>display_limit) {break;}
            }
            displayableComponentSelections = selections;


            return displayableComponentSelections;
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
        @TargetApi(23) boolean isPrintable(String c) {
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
    private static class KanjiSearchAsyncTaskLoader extends AsyncTaskLoader<Object> {

        //region Parameters
        private final String[] elements_list;
        private final int selected_structure;
        private JapaneseToolboxRoomDatabase mJapaneseToolboxRoomDatabase;
        private int max_size_for_duplicate_removal;
        //endregion

        KanjiSearchAsyncTaskLoader(Context context, String[] elements_list, int selected_structure) {
            super(context);
            this.elements_list = elements_list;
            this.selected_structure = selected_structure;
        }

        @Override
        protected void onStartLoading() {
            if (elements_list!=null) forceLoad();
        }

        @Override
        public Object loadInBackground() {

            mJapaneseToolboxRoomDatabase = JapaneseToolboxRoomDatabase.getInstance(getContext());
            List<String> result = findSearchResults();

            return result;
        }

        private List<String> findSearchResults() {

            String elementA = elements_list[0];
            String elementB = elements_list[1];
            String elementC = elements_list[2];
            String elementD = elements_list[3];

            //Initialization
            List<String> list_of_matching_results_elementA = new ArrayList<>();
            List<String> list_of_matching_results_elementB = new ArrayList<>();
            List<String> list_of_matching_results_elementC = new ArrayList<>();
            List<String> list_of_matching_results_elementD = new ArrayList<>();
            String concatenated_input;
            List<String> list_of_intersecting_results = new ArrayList<>();
            max_size_for_duplicate_removal = 200;

            int requested_structure;
            if (selected_structure != GlobalConstants.Index_full && (elementA.equals("") && elementB.equals("") && elementC.equals("") && elementD.equals(""))) {
                requested_structure = selected_structure;
            }
            else {
                requested_structure = GlobalConstants.Index_full;
            }

            KanjiComponent kanjiComponentForRequestedStructure = null;
            List<KanjiComponent.AssociatedComponent> associatedComponents = null;
            String componentStructure = GlobalConstants.COMPONENT_STRUCTURES_MAP.get(requested_structure);
            if (!TextUtils.isEmpty(componentStructure)) {
                List<KanjiComponent> kanjiComponents = mJapaneseToolboxRoomDatabase.getKanjiComponentsByStructureName(componentStructure);
                if (kanjiComponents != null && kanjiComponents.size() > 0) {
                    kanjiComponentForRequestedStructure = kanjiComponents.get(0);
                    if (componentStructure.equals("full") && kanjiComponents.size()==2) {
                        associatedComponents = kanjiComponentForRequestedStructure.getAssociatedComponents();
                        associatedComponents.addAll(kanjiComponents.get(1).getAssociatedComponents());
                    }
                }
            }
            if (kanjiComponentForRequestedStructure==null) return new ArrayList<>();

            //region Finding the list of matches corresponding to the user's input
            if(!elementA.equals("")) {
                concatenated_input = Utilities.removeSpecialCharacters(elementA);

                list_of_matching_results_elementA = getMatchingResultsForSingleElementInStructure(concatenated_input, kanjiComponentForRequestedStructure);
//
//                limits = DatabaseUtilities.binarySearchInUTF8Index(concatenated_input, mArrayOfComponentsDatabases.get(requested_structure), relevant_column_index);
//
//                if (limits[0] == limits[1] && limits[0] == -1) { }
//                else {
//                    list_of_matching_results_elementA = Arrays.asList(mArrayOfComponentsDatabases.get(requested_structure).get(limits[0])[1].split(";"));
//                }
            }
            else {
                list_of_matching_results_elementA = getMatchingResultsForAllElementsInStructure(kanjiComponentForRequestedStructure);
//                for (int i = 0; i< mArrayOfComponentsDatabases.get(requested_structure).size(); i++) {
//                    local_matches = Arrays.asList(mArrayOfComponentsDatabases.get(requested_structure).get(i)[1].split(";"));
//                    list_of_matching_results_elementA.addAll(local_matches);
//                    if (list_of_matching_results_elementA.size()<max_size_for_duplicate_removal) {list_of_matching_results_elementA = removeDuplicatesFromList(list_of_matching_results_elementA);}
//                }
            }
            if(!elementB.equals("")) {
                concatenated_input = Utilities.removeSpecialCharacters(elementB);
                list_of_matching_results_elementB = getMatchingResultsForSingleElementInStructure(concatenated_input, kanjiComponentForRequestedStructure);
            }
            else {
                list_of_matching_results_elementB = getMatchingResultsForAllElementsInStructure(kanjiComponentForRequestedStructure);
            }
            if(!elementC.equals("")) {
                concatenated_input = Utilities.removeSpecialCharacters(elementC);
                list_of_matching_results_elementC = getMatchingResultsForSingleElementInStructure(concatenated_input, kanjiComponentForRequestedStructure);
            }
            else {
                list_of_matching_results_elementC = getMatchingResultsForAllElementsInStructure(kanjiComponentForRequestedStructure);
            }
            if(!elementD.equals("")) {
                concatenated_input = Utilities.removeSpecialCharacters(elementD);
                list_of_matching_results_elementD = getMatchingResultsForSingleElementInStructure(concatenated_input, kanjiComponentForRequestedStructure);
            }
            else {
                list_of_matching_results_elementD = getMatchingResultsForAllElementsInStructure(kanjiComponentForRequestedStructure);
            }
            //endregion

            //region Getting the match intersections
            if      ( elementA.equals("") &&  elementB.equals("") &&  elementC.equals("") &&  elementD.equals("")) {
                list_of_intersecting_results.addAll(list_of_matching_results_elementA);
            }
            else if ( elementA.equals("") &&  elementB.equals("") &&  elementC.equals("") && !elementD.equals("")) {
                list_of_intersecting_results.addAll(list_of_matching_results_elementD);
            }
            else if ( elementA.equals("") &&  elementB.equals("") && !elementC.equals("") &&  elementC.equals("")) {
                list_of_intersecting_results.addAll(list_of_matching_results_elementC);
            }
            else if ( elementA.equals("") &&  elementB.equals("") && !elementC.equals("") && !elementD.equals("")) {
                list_of_intersecting_results = getIntersectionOfLists(list_of_matching_results_elementC, list_of_matching_results_elementD);
            }
            else if ( elementA.equals("") && !elementB.equals("") &&  elementC.equals("") &&  elementD.equals("")) {
                list_of_intersecting_results.addAll(list_of_matching_results_elementB);
            }
            else if ( elementA.equals("") && !elementB.equals("") &&  elementC.equals("") && !elementD.equals("")) {
                list_of_intersecting_results = getIntersectionOfLists(list_of_matching_results_elementB, list_of_matching_results_elementD);
            }
            else if ( elementA.equals("") && !elementB.equals("") && !elementC.equals("") &&  elementD.equals("")) {
                list_of_intersecting_results = getIntersectionOfLists(list_of_matching_results_elementB, list_of_matching_results_elementC);
            }
            else if ( elementA.equals("") && !elementB.equals("") && !elementC.equals("") && !elementD.equals("")) {
                list_of_intersecting_results = getIntersectionOfLists(list_of_matching_results_elementB, list_of_matching_results_elementC);
                list_of_intersecting_results = getIntersectionOfLists(list_of_intersecting_results, list_of_matching_results_elementD);
            }
            else if (!elementA.equals("") &&  elementB.equals("") &&  elementC.equals("") &&  elementD.equals("")) {
                list_of_intersecting_results.addAll(list_of_matching_results_elementA);
            }
            else if (!elementA.equals("") &&  elementB.equals("") &&  elementC.equals("") && !elementD.equals("")) {
                list_of_intersecting_results = getIntersectionOfLists(list_of_matching_results_elementA, list_of_matching_results_elementD);
            }
            else if (!elementA.equals("") &&  elementB.equals("") && !elementC.equals("") &&  elementD.equals("")) {
                list_of_intersecting_results = getIntersectionOfLists(list_of_matching_results_elementA, list_of_matching_results_elementC);
            }
            else if (!elementA.equals("") &&  elementB.equals("") && !elementC.equals("") && !elementD.equals("")) {
                list_of_intersecting_results = getIntersectionOfLists(list_of_matching_results_elementA, list_of_matching_results_elementC);
                list_of_intersecting_results = getIntersectionOfLists(list_of_intersecting_results, list_of_matching_results_elementD);
            }
            else if (!elementA.equals("") && !elementB.equals("") &&  elementC.equals("") &&  elementD.equals("")) {
                list_of_intersecting_results = getIntersectionOfLists(list_of_matching_results_elementA, list_of_matching_results_elementB);
            }
            else if (!elementA.equals("") && !elementB.equals("") &&  elementC.equals("") && !elementD.equals("")) {
                list_of_intersecting_results = getIntersectionOfLists(list_of_matching_results_elementA, list_of_matching_results_elementB);
                list_of_intersecting_results = getIntersectionOfLists(list_of_intersecting_results, list_of_matching_results_elementD);
            }
            else if (!elementA.equals("") && !elementB.equals("") && !elementC.equals("") &&  elementD.equals("")) {
                list_of_intersecting_results = getIntersectionOfLists(list_of_matching_results_elementA, list_of_matching_results_elementB);
                list_of_intersecting_results = getIntersectionOfLists(list_of_intersecting_results, list_of_matching_results_elementC);
            }
            else if (!elementA.equals("") && !elementB.equals("") && !elementC.equals("") && !elementD.equals("")) {
                list_of_intersecting_results = getIntersectionOfLists(list_of_matching_results_elementA, list_of_matching_results_elementB);
                list_of_intersecting_results = getIntersectionOfLists(list_of_intersecting_results, list_of_matching_results_elementC);
                list_of_intersecting_results = getIntersectionOfLists(list_of_intersecting_results, list_of_matching_results_elementD);
            }
            //endregion

            //Returning only the structures that match the user's selected_structure
            List<String> list_of_intersecting_results_and_structure = new ArrayList<>();
            if (selected_structure != GlobalConstants.Index_full && !(elementA.equals("") && elementB.equals("") && elementC.equals("") && elementD.equals(""))) {
                List<String> complete_results_for_given_structure = new ArrayList<>();
                List<String> previous_array = new ArrayList<>();
                List<String> current_array = new ArrayList<>();

                for (int i = 0; i < associatedComponents.size(); i++) {
                    current_array = Arrays.asList(associatedComponents.get(i).getAssociatedComponents().split(";"));
                    for (String x : current_array){
                        if (!previous_array.contains(x)) complete_results_for_given_structure.add(DatabaseUtilities.convertToUTF8(x));
                    }
                    previous_array = current_array;
                }

                java.util.Collections.sort(complete_results_for_given_structure);

                for (String x : list_of_intersecting_results) {
                    String converted = DatabaseUtilities.convertToUTF8(x);
                    int index = java.util.Collections.binarySearch(complete_results_for_given_structure, converted);
                    if (index >= 0) { list_of_intersecting_results_and_structure.add(x); }
                }
            }
            else {
                list_of_intersecting_results_and_structure = list_of_intersecting_results;
            }

            return list_of_intersecting_results_and_structure;
        }
        List<String> getIntersectionOfLists(List<String> A, List<String> B) {
            //https://stackoverflow.com/questions/2400838/efficient-intersection-of-component_substructures[2]-liststring-in-java
            List<String> rtnList = new LinkedList<>();
            for(String dto : A) {
                if(B.contains(dto)) {
                    rtnList.add(dto);
                }
            }
            return rtnList;
        }
        List<String> removeDuplicatesFromList(List<String> list) {

            /*
            int end_index = list_of_intersecting_results_temp.size();
            String current_value;
            for (int i=0; i<end_index; i++) {
                current_value = list_of_intersecting_results_temp.get(i);
                for (int j=end_index; j>i; j--) {
                    if (current_value.equals(list_of_intersecting_results_temp.get(j))) {
                        list_of_intersecting_results_temp.remove(j);
                    }
                }
            }
            list_of_intersecting_results = list_of_intersecting_results_temp;
            */

            //https://stackoverflow.com/questions/14040331/remove-duplicate-strings-in-a-list-in-java

            Set<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            Iterator<String> i = list.iterator();
            while (i.hasNext()) {
                String s = i.next();
                if (set.contains(s)) {
                    i.remove();
                }
                else {
                    set.add(s);
                }
            }

            return new ArrayList<>(set);
        }
        List<String> getMatchingResultsForSingleElementInStructure(String concatenated_input, KanjiComponent kanjiComponent) {

            List<KanjiComponent.AssociatedComponent> associatedComponents = kanjiComponent.getAssociatedComponents();

            for (KanjiComponent.AssociatedComponent associatedComponent : associatedComponents) {
                if (associatedComponent.getComponent().equals(concatenated_input)) {
                    return Arrays.asList(associatedComponent.getAssociatedComponents().split(";"));
                }
            }
            return new ArrayList<>();
        }
        List<String> getMatchingResultsForAllElementsInStructure(KanjiComponent kanjiComponent) {

            List<KanjiComponent.AssociatedComponent> associatedComponents = kanjiComponent.getAssociatedComponents();

            List<String> list_of_matching_results_for_element = new ArrayList<>();
            for (KanjiComponent.AssociatedComponent associatedComponent : associatedComponents) {
                list_of_matching_results_for_element.addAll(Arrays.asList(associatedComponent.getAssociatedComponents().split(";")));
                if (list_of_matching_results_for_element.size()<max_size_for_duplicate_removal) {
                    list_of_matching_results_for_element = removeDuplicatesFromList(list_of_matching_results_for_element);
                }
            }
            return list_of_matching_results_for_element;
        }

    }

    //Communication with parent activity
    SearchByRadicalFragmentOperationsHandler searchByRadicalFragmentOperationsHandler;
    interface SearchByRadicalFragmentOperationsHandler {
        void onQueryTextUpdateFromSearchByRadicalRequested(String selectedWordString);
    }

}

