package com.mageventory.fragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.mageventory.fragment.base.BaseDialogFragment;
import com.mageventory.model.Category;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.tasks.AbstractLoadCategoriesTask;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.LoadingControl;
import com.mageventory.util.SimpleViewLoadingControl;
import com.mageventory.util.Util;
import com.mventory.R;

/**
 * The categories selection dialog fragment
 * 
 * @author Eugene Popovich
 */
public class CategoriesPickerFragment extends BaseDialogFragment {
    /**
     * Tag used for logging
     */
    private static final String TAG = CategoriesPickerFragment.class.getSimpleName();
    /**
     * The key for the selected category IDs fragment argument
     */
    public static final String EXTRA_SELECTED_CATEGORY_IDS = "SELECTED_CATEGORIES";
    /**
     * The key for the dialog title fragment argument
     */
    public static final String EXTRA_DIALOG_TITLE = "DIALOG_TITLE";

    /**
     * The last started load categories task
     */
    private LoadCategoriesTask mLoadCategoriesTask;
    /**
     * Collection of the selected category IDs
     */
    protected Collection<Integer> mSelectedCategoryIds;
    /**
     * The list view to display list of categories
     */
    ListView mListView;
    /**
     * The loading control to indicate category list loading process
     */
    LoadingControl mCategoriesLoadingControl;
    /**
     * The view to indicate empty categories list
     */
    View mNoCategoriesIndicator;
    /**
     * The categories selection external listener.
     */
    CategoriesSelectionListener mCategoriesSelectionListener;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (getTargetFragment() == null) {
            // if target fragment is absent use activity as
            // CategoriesSelectionListener
            if(!CategoriesSelectionListener.class.isInstance(getActivity()))
            {
                throw new IllegalStateException("The activity should implement CategoriesSelectionListener or use the target fragment");
            }else{
                mCategoriesSelectionListener = (CategoriesSelectionListener) getActivity();
            }
        } else{
            // construct general listener for the target fragment
            mCategoriesSelectionListener = new TargetFragmentCategoriesSelectionListener();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.categories_list, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // initialize swipe to refresh layout
        final SwipeRefreshLayout refreshController = (SwipeRefreshLayout) view
                .findViewById(R.id.refreshController);
        refreshController.setColorSchemeResources(R.color.blue);
        refreshController.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                loadCategoriesList(true);
            }
        });

        mCategoriesLoadingControl = new SimpleViewLoadingControl(
                view.findViewById(R.id.categoriesLoadingIndicator)) {
            @Override
            public void setViewVisibile(boolean visible) {
                super.setViewVisibile(visible);
                if (!visible) {
                    // hide refresh controller refreshing indicator if visible
                    refreshController.setRefreshing(false);
                }
            };
        };
        mNoCategoriesIndicator = view.findViewById(R.id.noCategoriesIndicator);
        mListView = (ListView) view.findViewById(android.R.id.list);
        // initialize
        if (savedInstanceState != null) {
            mSelectedCategoryIds = savedInstanceState
                    .getIntegerArrayList(EXTRA_SELECTED_CATEGORY_IDS);
        } else if (getArguments() != null) {
            mSelectedCategoryIds = getArguments().getIntegerArrayList(EXTRA_SELECTED_CATEGORY_IDS);
        }
        view.findViewById(R.id.saveBtn).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                CategoriesListAdapter adapter = (CategoriesListAdapter) mListView.getAdapter();
                mCategoriesSelectionListener.onCategoriesSelected(adapter.mSelectedCategoryIds);
                closeDialog();
            }
        });
        view.findViewById(R.id.cancelBtn).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                closeDialog();
            }
        });
        loadCategoriesList();
    }

    /**
     * Display the categories list data
     * 
     * @param data the loaded categories list data
     */
    public void displayData(List<Category> data) {
        final ListAdapter adapter;
        if (data.size() == 0) {
            // if call is successful but there are no categories to list
            adapter = null;
            // show empty categories list indicator
            mNoCategoriesIndicator.setVisibility(View.VISIBLE);
        } else {
            // hide empty categories list indicator
            mNoCategoriesIndicator.setVisibility(View.GONE);
            adapter = new CategoriesListAdapter(data, mSelectedCategoryIds);
        }
        setListAdapter(adapter);
    }

    /**
     * Remove all items from the categories list
     */
    private void emptyList() {
        emptyList(false);
    }

    /**
     * Remove all items from the categories list
     * 
     * @param displayPlaceholder whether to display no categories indicator
     */
    private void emptyList(final boolean displayPlaceholder) {
        setListAdapter(null);
        mNoCategoriesIndicator.setVisibility(displayPlaceholder ? View.VISIBLE : View.GONE);
    }

    /**
     * Load categories list asynchronously
     */
    private void loadCategoriesList() {
        loadCategoriesList(false);
    }

    /**
     * Load categories list asynchronously
     * 
     * @param forceReload whether the cached data should be reloaded if exists
     */
    public void loadCategoriesList(final boolean forceReload) {
        CommonUtils.debug(TAG, false, "loadCategoriesList(" + forceReload
                + ");");
    
        if (mLoadCategoriesTask != null && !mLoadCategoriesTask.isFinished()) {
            // if there is an active load categories list task
            if (mLoadCategoriesTask.isForceReload() == forceReload) {
                // same loading operation is already active.
                CommonUtils.debug(TAG, "Same loading operation is already active, skipping...");
                return;
            }
            // cancel current loading task
            mLoadCategoriesTask.cancel(true);
        }
        // remember currently selected by user category IDs so selection will be
        // presetn after the list refresh
        CategoriesListAdapter adapter = (CategoriesListAdapter) mListView.getAdapter();
        if(adapter != null){
            mSelectedCategoryIds = adapter.mSelectedCategoryIds;
        }
        emptyList();
        mLoadCategoriesTask = new LoadCategoriesTask(forceReload, new SettingsSnapshot(
                getActivity().getApplicationContext()), mCategoriesLoadingControl);
        mLoadCategoriesTask.execute();
    }

    /**
     * Show the load categories failed message dialog
     */
    protected void showLoadFailureDialog() {
        // build dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final AlertDialog dialog = builder.create();

        // set title
        dialog.setTitle(getString(R.string.data_load_failure));

        // set message
        final StringBuilder message = new StringBuilder(64);
        message.append(getString(R.string.check_your_internet_retry));
        dialog.setMessage(message.toString());

        // set buttons
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.try_again),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loadCategoriesList();
                    }
                });
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onLoadFailureDialogCancel();
                    }
                });

        dialog.show();
    }

    /**
     * Action which is performed when user cancels reloading in case categories
     * load failed
     */
    protected void onLoadFailureDialogCancel() {
        // fire cancelled result and close dialog in case categories list load
        // failed and user canceled
        // action
        mCategoriesSelectionListener.onCategoriesSelectionCancelled();
        closeDialog();
    }
    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        // fire cancelled result
        mCategoriesSelectionListener.onCategoriesSelectionCancelled();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        CategoriesListAdapter adapter = (CategoriesListAdapter) mListView.getAdapter();
        outState.putIntegerArrayList(EXTRA_SELECTED_CATEGORY_IDS, new ArrayList<Integer>(
                adapter != null ? adapter.mSelectedCategoryIds : mSelectedCategoryIds));
    }

    /**
     * Set the list adapter to the activity ListView
     * 
     * @param adapter the adapter to set
     */
    protected void setListAdapter(ListAdapter adapter) {
        mListView.setAdapter(adapter);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog result = super.onCreateDialog(savedInstanceState);
        Bundle arguments = getArguments();
        String title = arguments == null ? null : arguments.getString(EXTRA_DIALOG_TITLE);
        if (TextUtils.isEmpty(title)) {
            // if the title argument is not passed use default dialog title
            result.setTitle(R.string.categories_picker_title);
        } else {
            // if the title argument is passed use it as dialog title
            result.setTitle(title);
        }
        result.setCanceledOnTouchOutside(true);
        return result;
    }

    /**
     * The interface parent activity should implement to handle categories
     * selection event. It is required only if target fragment is not specified
     */
    public static interface CategoriesSelectionListener {
        /**
         * Fired when user selected categories and pressed save
         * 
         * @param selectedCategoryIds the user selected categories ids
         */
        void onCategoriesSelected(Collection<Integer> selectedCategoryIds);

        /**
         * Fired when user cancelled categories selection
         */
        void onCategoriesSelectionCancelled();
    }

    /**
     * Implementation of the {@link CategoriesSelectionListener} to translate
     * action to the target {@link Fragment#onActivityResult(int, int, Intent)}
     * method
     */
    class TargetFragmentCategoriesSelectionListener implements CategoriesSelectionListener {

        @Override
        public void onCategoriesSelected(Collection<Integer> selectedCategoryIds) {
            // send the data to the onActivityResult method of the target
            // fragment
            Intent intent = new Intent();
            intent.putExtra(EXTRA_SELECTED_CATEGORY_IDS,
                    new ArrayList<Integer>(selectedCategoryIds));
            getTargetFragment()
                    .onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
        }

        @Override
        public void onCategoriesSelectionCancelled() {
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED,
                    null);
        }

    }
    
    /**
     * The categories adapter to be used in the categories list
     */
    class CategoriesListAdapter extends BaseAdapter {
        /**
         * The list of categories to display
         */
        List<Category> mCategories;
        /**
         * The selected category IDs
         */
        Set<Integer> mSelectedCategoryIds = new HashSet<Integer>();
        /**
         * The layout inflater to use in the getView method
         */
        LayoutInflater mInflater;
        /**
         * The settings snapshot
         */
        SettingsSnapshot mSettings;
        /**
         * The default width for one level of the category item indentation in
         * the category view
         */
        int mIndentation;

        /**
         * The color to indicate selected categories 
         */
        int mSelectedCategoryBackground;

        /**
         * @param categories The list of categories to display
         * @param selectedCategoryIds The selected category ids
         */
        CategoriesListAdapter(List<Category> categories,
                Collection<Integer> selectedCategoryIds) {
            mIndentation = getResources().getDimensionPixelSize(R.dimen.categories_indent);
            mSelectedCategoryBackground = CommonUtils
                    .getColorResource(R.color.category_selected_background);
            mCategories = categories;
            if (selectedCategoryIds != null) {
                mSelectedCategoryIds.addAll(selectedCategoryIds);
            }
            mInflater = LayoutInflater.from(getActivity());
            mSettings = new SettingsSnapshot(getActivity().getApplicationContext());
        }

        @Override
        public int getCount() {
            return mCategories.size();
        }

        @Override
        public Category getItem(int position) {
            return mCategories.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // View holder pattern implementtation
            ViewHolder vh;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.category_list_item, parent, false);
                vh = new ViewHolder(convertView);
                convertView.setTag(vh);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }
            Category item = getItem(position);
            vh.setData(item);
            return convertView;
        }

        /**
         * Class for the view holder pattern implementation
         */
        class ViewHolder {
            /**
             * The view to display category name
             */
            TextView mTextView;

            /**
             * The view to display category selected/unselected indicator
             */
            CheckBox mCheckBox;

            /**
             * The view to handle left indentation
             */
            View mIndentationView;

            /**
             * The view to indicate selected categories
             */
            View mSelectedIndicator;

            /**
             * The category related to the view holder
             */
            Category category;

            public ViewHolder(View view) {
                mTextView = (TextView) view.findViewById(android.R.id.text1);
                mCheckBox = (CheckBox) view.findViewById(android.R.id.checkbox);
                mIndentationView = view.findViewById(R.id.indentation);
                mSelectedIndicator = view.findViewById(R.id.selectedIndicator);
                view.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        int categoryId = category.getId();
                        boolean selected;
                        if (mSelectedCategoryIds.contains(categoryId)) {
                            mSelectedCategoryIds.remove(categoryId);
                            selected = false;
                        } else {
                            selected = true;
                            mSelectedCategoryIds.add(categoryId);
                        }
                        mCheckBox.setChecked(selected);
                        updatedSelectedIndicator(selected);
                    }
                });
            }

            /**
             * Set the data related to the view holder and update views
             * 
             * @param category
             */
            void setData(final Category category) {
                this.category = category;
                mTextView.setText(category.getName());
                boolean selected = mSelectedCategoryIds.contains(category.getId());
                mCheckBox.setChecked(selected);
                updatedSelectedIndicator(selected);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mIndentationView
                        .getLayoutParams();
                params.width = mIndentation * category.getIndentationLevel();
                mIndentationView.setLayoutParams(params);
            }

            /**
             * Update category selected indicator background depend on selected
             * parameter
             * 
             * @param selected whether the category is selected
             */
            public void updatedSelectedIndicator(boolean selected) {
                mSelectedIndicator.setBackgroundColor(selected ? mSelectedCategoryBackground
                        : Color.TRANSPARENT);
            }
        }
    }

    /**
     * Asynchronous task to load and display categories list
     */
    class LoadCategoriesTask extends AbstractLoadCategoriesTask {

        /**
         * The loaded categories information
         */
        List<Category> mCategories;

        /**
         * @param forceReload whether the cached data should be forced to reload
         * @param settings the settings snapshot
         * @param loadingControl the loading control to be used to indicate task
         *            activity
         */
        public LoadCategoriesTask(boolean forceReload, SettingsSnapshot settings,
                LoadingControl loadingControl) {
            super(forceReload, settings, loadingControl);
        }

        @Override
        protected void onSuccessPostExecute() {
            if (!isActivityAlive()) {
                // if activity was destroyed interrupt method
                // invocation to avoid various errors
                return;
            }
            displayData(mCategories);

        }

        @Override
        protected void onFailedPostExecute() {
            super.onFailedPostExecute();
            if (!isActivityAlive()) {
                // if activity was destroyed interrupt method
                // invocation to avoid various errors
                return;
            }
            if (!isCancelled()) {
                showLoadFailureDialog();
            }
        }

        @Override
        public boolean extraLoadAfterCategoriesDataIsLoaded() {
            super.extraLoadAfterCategoriesDataIsLoaded();
            mCategories = Util.getCategorylist(getData(), null);
            return true;
        }
    }
}
