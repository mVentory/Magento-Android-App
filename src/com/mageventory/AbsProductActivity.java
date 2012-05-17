package com.mageventory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.mageventory.model.Category;
import com.mageventory.res.LoadOperation;
import com.mageventory.res.ResourceServiceHelper;
import com.mageventory.res.ResourceServiceHelper.OperationObserver;
import com.mageventory.restask.BaseTask;
import com.mageventory.util.DialogUtil;
import com.mageventory.util.DialogUtil.OnCategorySelectListener;
import com.mageventory.util.Util;

public abstract class AbsProductActivity extends Activity implements MageventoryConstants {

    // tasks

    private static class AttributeSetsAndCategories {
        public List<Map<String, Object>> attributeSets;
        public Map<String, Object> categories; // root category
    }

    protected static class LoadAttributeSetsAndCategories extends
            BaseTask<AbsProductActivity, AttributeSetsAndCategories> implements MageventoryConstants, OperationObserver {

        private AttributeSetsAndCategories myData = new AttributeSetsAndCategories();
        private boolean forceLoad = false;
        private CountDownLatch doneSignal;
        private int atrReqId = INVALID_REQUEST_ID;
        private int catReqId = INVALID_REQUEST_ID;
        private boolean atrSuccess = false;
        private boolean catSuccess = false;
        private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
        private int state = TSTATE_NEW;

        public LoadAttributeSetsAndCategories(AbsProductActivity hostActivity) {
            super(hostActivity);
        }

        public int getState() {
            return state;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            state = TSTATE_RUNNING;
            setData(myData);
        }

        @Override
        protected Integer doInBackground(Object... args) {
            if (args != null && args.length > 0 && args[0] instanceof Boolean) {
                forceLoad = (Boolean) args[0];
            }

            final AbsProductActivity host = getHost();
            if (host == null) {
                return null;
            }

            // start remote loading

            if (isCancelled()) {
                return 0;
            }

            int nlatches = 0;
            if (forceLoad || resHelper.isResourceAvailable(host, RES_CATALOG_PRODUCT_ATTRIBUTE_SET_LIST) == false) {
                resHelper.registerLoadOperationObserver(this);
                atrReqId = resHelper.loadResource(host, RES_CATALOG_PRODUCT_ATTRIBUTE_SET_LIST);
                nlatches += 1;

                host.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        host.onAttributeSetLoadStart();
                    }
                });
            } else {
                atrSuccess = true;
            }

            if (forceLoad || resHelper.isResourceAvailable(host, RES_CATALOG_CATEGORY_TREE) == false) {
                resHelper.registerLoadOperationObserver(this);
                catReqId = resHelper.loadResource(host, RES_CATALOG_CATEGORY_TREE);
                nlatches += 1;

                host.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        host.onCategoryLoadStart();
                    }
                });
            } else {
                catSuccess = true;
            }

            if (nlatches > 0) {
                doneSignal = new CountDownLatch(nlatches);
                while (true) {
                    if (isCancelled()) {
                        return 0;
                    }
                    try {
                        if (doneSignal.await(2, TimeUnit.SECONDS)) {
                            break;
                        }
                    } catch (InterruptedException e) {
                        return 0;
                    }
                }
            }
            resHelper.unregisterLoadOperationObserver(this);

            // retrieve local data

            if (isCancelled()) {
                return 0;
            }

            if (atrSuccess) {
                myData.attributeSets = resHelper.restoreResource(host, RES_CATALOG_PRODUCT_ATTRIBUTE_SET_LIST);
                host.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (myData.attributeSets != null) {
                            host.onAttributeSetLoadSuccess();
                        } else {
                            host.onAttributeSetLoadFailure();
                        }
                    }
                });
            }
            if (catSuccess) {
                myData.categories = resHelper.restoreResource(host, RES_CATALOG_CATEGORY_TREE);
                host.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (myData.categories != null) {
                            host.onCategoryLoadSuccess();
                        } else {
                            host.onCategoryLoadFailure();
                        }
                    }
                });
            }
            return 0;
        }

        @Override
        public void onLoadOperationCompleted(LoadOperation op) {
            final AbsProductActivity host = getHost();
            if (op.getOperationRequestId() == catReqId) {
                // categories
                if (op.getException() == null) {
                    catSuccess = true;
                } else {
                    if (host != null) {
                        host.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                host.onCategoryLoadFailure();
                            }
                        });
                    }
                }
            } else if (op.getOperationRequestId() == atrReqId) {
                // attributes
                if (op.getException() == null) {
                    atrSuccess = true;
                } else {
                    if (host != null) {
                        host.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                host.onAttributeSetLoadFailure();
                            }
                        });
                    }
                }
            } else {
                return;
            }
            if (host != null) {
                resHelper.stopService(host, false);
            }
            doneSignal.countDown();
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            state = TSTATE_TERMINATED;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            state = TSTATE_CANCELED;
        }

    }

    private static class LoadAttributes extends BaseTask<AbsProductActivity, List<Map<String, Object>>> implements
            MageventoryConstants, OperationObserver {

        private CountDownLatch doneSignal;
        private ResourceServiceHelper resHelper = ResourceServiceHelper.getInstance();
        private boolean forceRefresh = false;
        private int atrSetId;

        private int state = TSTATE_NEW;
        private boolean atrSuccess;
        private int atrRequestId = INVALID_REQUEST_ID;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            state = TSTATE_RUNNING;
        }

        @Override
        protected Integer doInBackground(Object... args) {
            if (args == null || args.length != 2) {
                throw new IllegalArgumentException();
            }
            if (args[0] instanceof Integer == false) {
                throw new IllegalArgumentException();
            }
            if (args[1] instanceof Boolean == false) {
                throw new IllegalArgumentException();
            }

            atrSetId = (Integer) args[0];
            forceRefresh = (Boolean) args[1];

            AbsProductActivity host = getHost();
            if (host == null) {
                return 0;
            }

            if (isCancelled()) {
                return 0;
            }

            final String[] params = new String[] { String.valueOf(atrSetId) };
            if (forceRefresh || resHelper.isResourceAvailable(host, RES_PRODUCT_ATTRIBUTE_LIST, params) == false) {
                // remote load
                doneSignal = new CountDownLatch(1);
                resHelper.registerLoadOperationObserver(this);

                atrRequestId = resHelper.loadResource(host, RES_PRODUCT_ATTRIBUTE_LIST, params);

                if (host != null) {
                    final AbsProductActivity finalHost = host;
                    host.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            finalHost.onAttributeListLoadStart();
                        }
                    });
                }

                while (true) {
                    if (isCancelled()) {
                        return 0;
                    }
                    try {
                        if (doneSignal.await(10, TimeUnit.SECONDS)) {
                            break;
                        }
                    } catch (InterruptedException e) {
                        return 0;
                    }
                }

                resHelper.unregisterLoadOperationObserver(this);
            } else {
                atrSuccess = true;
            }

            if (isCancelled()) {
                return 0;
            }

            final List<Map<String, Object>> atrs;
            if (atrSuccess) {
                atrs = resHelper.restoreResource(host, RES_PRODUCT_ATTRIBUTE_LIST, params);
            } else {
                atrs = null;
            }
            setData(atrs);

            if (isCancelled()) {
                return 0;
            }

            host = getHost();
            if (host != null) {
                final AbsProductActivity finalHost = host;
                host.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (atrs != null) {
                            finalHost.onAttributeListLoadSuccess();
                        } else {
                            finalHost.onAttributeListLoadFailure();
                        }
                    }
                });
                return 1;
            } else {
                return 0;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            state = TSTATE_TERMINATED;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            state = TSTATE_CANCELED;
        }

        @Override
        public void onLoadOperationCompleted(final LoadOperation op) {
            // final AbsProductActivity host = getHost();
            if (atrRequestId == op.getOperationRequestId()) {
                atrSuccess = op.getException() == null;
                doneSignal.countDown();
            }
        }

        public int getState() {
            return state;
        }

    }

    // icicle keys
    // private String IKEY_CATEGORY_REQID = "category request id";
    // private String IKEY_ATTRIBUTE_SET_REQID = "attribute set request id";

    // views
    protected LayoutInflater inflater;
    protected View atrListWrapperV;
    protected ViewGroup atrListV;
    protected EditText attributeSetV;
    protected EditText categoryV;
    protected TextView atrSetLabelV;
    protected TextView categoryLabelV;
    protected TextView atrListLabelV;
    protected ProgressBar atrSetProgressV;
    protected ProgressBar categoryProgressV;
    protected ProgressBar atrListProgressV;
    protected Map<String, View> atrCodeToView = new HashMap<String, View>();

    // data
    // protected int categoryId;

    protected int atrSetId = INVALID_ATTRIBUTE_SET_ID;
    protected Category category;

    // private int attributeSetRequestId = INVALID_REQUEST_ID;
    // private int categoryRequestId = INVALID_REQUEST_ID;

    // state
    protected boolean isActive = false;
    private LoadAttributeSetsAndCategories atrSetsAndCategoriesTask;
    private LoadAttributes atrsTask;
    private Dialog dialog;

    // lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentView());

        // find views
        atrListWrapperV = findViewById(R.id.attr_list_wrapper);
        attributeSetV = (EditText) findViewById(R.id.attr_set);
        atrListV = (ViewGroup) findViewById(R.id.attr_list);
        //attributeSetV = (EditText) findViewById(R.id.attr_set);   
        categoryV = (EditText) findViewById(R.id.category);
        atrListLabelV = (TextView) findViewById(R.id.attr_list_label);
        atrSetLabelV = (TextView) findViewById(R.id.atr_set_label);
        categoryLabelV = (TextView) findViewById(R.id.category_label);
        atrSetProgressV = (ProgressBar) findViewById(R.id.atr_set_progress);
        categoryProgressV = (ProgressBar) findViewById(R.id.category_progress);
        atrListProgressV = (ProgressBar) findViewById(R.id.attr_list_progress);

        inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        // state
        isActive = true;

        // attach listeners
        attachListenerToEditText(attributeSetV, new OnClickListener() {
            @Override
            public void onClick(View v) {
                showAttributeSetList();
            }
        });
        attachListenerToEditText(categoryV, new OnClickListener() {
            @Override
            public void onClick(View v) {
                showCategoryList();
            }
        });

        // load data
        loadAttributeSetsAndCategories(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActive = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActive = false;
    }

    // methods

    protected abstract int getContentView();

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState == null) {
            return;
        }
        // attributeSetRequestId = savedInstanceState.getInt(IKEY_ATTRIBUTE_SET_REQID, INVALID_REQUEST_ID);
        // categoryRequestId = savedInstanceState.getInt(IKEY_CATEGORY_REQID, INVALID_REQUEST_ID);
    }

    private void showAttributeSetList() {
        if (isActive == false) {
            return;
        }
        List<Map<String, Object>> atrSets = getAttributeSets();
        if (atrSets == null || atrSets.isEmpty()) {
            return;
        }

        // reorganize Attribute Set List
        Map<String,Object> defaultAttrSet = null;
        Map<String,Object> firstAttrSet = atrSets.get(0);
        
        int i=1;
        for(i=1;i<atrSets.size();i++)
        {
        	defaultAttrSet = atrSets.get(i);
        	if(TextUtils.equals(defaultAttrSet.get("name").toString(),"Default"))
        	{
        		atrSets.remove(0);
            	atrSets.add(0, defaultAttrSet);
            	atrSets.remove(i);
            	atrSets.add(i,firstAttrSet);
        	}        	
        }        
        
        final Dialog attrSetListDialog = DialogUtil.createListDialog(this, "Attribute sets", atrSets,
                android.R.layout.simple_list_item_1, new String[] { MAGEKEY_ATTRIBUTE_SET_NAME },
                new int[] { android.R.id.text1 }, new OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        final Object item = arg0.getAdapter().getItem(arg2);
                        @SuppressWarnings("unchecked")
                        final Map<String, Object> itemData = (Map<String, Object>) item;

                        int atrSetId;
                        try {
                            atrSetId = Integer.parseInt(itemData.get(MAGEKEY_ATTRIBUTE_SET_ID).toString());
                        } catch (Throwable e) {
                            atrSetId = INVALID_ATTRIBUTE_SET_ID;
                        }

                        dialog.dismiss();
                        selectAttributeSet(atrSetId, false);
                    }
                });
        (dialog = attrSetListDialog).show();
    }

    protected void selectAttributeSet(final int setId, final boolean forceRefresh) {
        if (setId == INVALID_ATTRIBUTE_SET_ID) {
            return;
        }

        atrSetId = setId;

        final List<Map<String, Object>> sets = getAttributeSets();
        if (sets == null) {
            return;
        }

        for (Map<String, Object> set : sets) {
            final int tmpSetId;
            try {
                tmpSetId = Integer.parseInt(set.get(MAGEKEY_ATTRIBUTE_SET_ID).toString());
            } catch (Throwable e) {
                continue;
            }
            if (tmpSetId == setId) {
                try {
                    final String atrSetName = set.get(MAGEKEY_ATTRIBUTE_SET_NAME).toString();
                    attributeSetV.setText(atrSetName);
                    
                    final Map<String, Object> rootCategory = getCategories();
                    if (rootCategory == null || rootCategory.isEmpty()) {
                        return;
                    }
                    
                    for (Category cat: Util.getCategorylist(rootCategory, null))
                    {
                    	if (cat.getName().equals(atrSetName))
                    	{
                    		category = cat;
                            categoryV.setText(cat.getFullName());
                            break;
                    	}
                    }
                    
                } catch (Throwable ignored) {
                }
                loadAttributeList(setId, forceRefresh);
                break;
            }
        }
    }

    private void showCategoryList() {
        if (isActive == false) {
            return;
        }

        final Map<String, Object> rootCategory = getCategories();
        if (rootCategory == null || rootCategory.isEmpty()) {
            return;
        }

        // XXX y: HUGE OVERHEAD... transforming category data in the main thread
        final Dialog categoryListDialog = DialogUtil.createCategoriesDialog(this, rootCategory,
                new OnCategorySelectListener() {
                    @Override
                    public boolean onCategorySelect(Category c) {
                        if (c == null) {
                            category = null;
                            categoryV.setText("");
                        } else {
                            category = c;
                            categoryV.setText(c.getFullName());
                        }
                        dialog.dismiss();
                        return true;
                    }
                }, category);
        if (categoryListDialog != null) {
            (dialog = categoryListDialog).show();
        }
    }

    // resources

    protected List<Map<String, Object>> getAttributeList() {
        if (atrsTask == null) {
            return null;
        }
        return atrsTask.getData();
    }

    private List<Map<String, Object>> getAttributeSets() {
        if (atrSetsAndCategoriesTask == null) {
            return null;
        }
        if (atrSetsAndCategoriesTask.getData() == null) {
            return null;
        }
        return atrSetsAndCategoriesTask.getData().attributeSets;
    }

    protected Map<String, Object> getCategories() {
        if (atrSetsAndCategoriesTask == null) {
            return null;
        }
        if (atrSetsAndCategoriesTask.getData() == null) {
            return null;
        }
        return atrSetsAndCategoriesTask.getData().categories;
    }

    protected void loadAttributeSetsAndCategories(final boolean refresh) {
        if (atrSetsAndCategoriesTask != null && atrSetsAndCategoriesTask.getState() == TSTATE_RUNNING) {
            // there is currently running task
            if (refresh == false) {
                return;
            }
        }
        if (atrSetsAndCategoriesTask != null) {
            atrSetsAndCategoriesTask.cancel(true);
            atrSetsAndCategoriesTask.setHost(null);
            atrSetsAndCategoriesTask = null;
        }
        atrSetsAndCategoriesTask = new LoadAttributeSetsAndCategories(this);
        atrSetsAndCategoriesTask.execute(refresh);
    }

    private void loadAttributeList(final int atrSetId, final boolean refresh) {
        if (atrsTask == null || atrsTask.getState() == TSTATE_CANCELED) {
            //
        } else {
            atrsTask.setHost(null);
            atrsTask.cancel(true);
        }
        atrsTask = new LoadAttributes();
        atrsTask.setHost(this);
        atrsTask.execute(atrSetId, refresh);
    }

    private void buildAtrList(List<Map<String, Object>> atrList) {
        removeAttributeListV();

        if (atrList == null || atrList.isEmpty()) {
            return;
        }

        showAttributeListV(false);
        
        // Thumbnail to be added at end of array of ex
        Map<String,Object> thumbnail = null;
        
        for (Map<String, Object> atr : atrList) {
        	if(!(atr.get("code").toString().contains("_thumb")))
        	{
        		final View edit = newAtrEditView(atr);
        		if(edit != null)
        			atrListV.addView(edit);
            // final String code = atr.containsKey(MAGEKEY_ATTRIBUTE_CODE) ? "" + atr.get(MAGEKEY_ATTRIBUTE_CODE) : "";
        	}
        	else
        	{
        		// Set thunmbnail to be added
        		thumbnail = atr;
        	}
        } 
        
        // add thumbnail if exists
        if(thumbnail != null)
        {
        	View edit = newAtrEditView(thumbnail);
        	if(edit != null)
        		atrListV.addView(edit);
        }                     
    }

    @SuppressWarnings("unchecked")
    protected void mapAtrDataToView(final Map<String, Object> atrData, final Object data) {
        final String atrType = (String) atrData.get(MAGEKEY_ATTRIBUTE_TYPE);
        final String atrCode = (String) atrData.get(MAGEKEY_ATTRIBUTE_CODE);
        if (TextUtils.isEmpty(atrCode) || data == null) {
            // nothing to show
            return;
        }
        final View v = atrCodeToView.get(atrCode);
        if (v == null) {
            return;
        }
        if (v instanceof EditText) {
            if ("multiselect".equalsIgnoreCase(atrType)) {
                // `data` is an array of ids
                Map<String, String> options = (Map<String, String>) v.getTag(R.id.tkey_atr_options);
                if (options != null && options instanceof Map) {

                    Set<String> selectedLabels = (Set<String>) v.getTag(R.id.tkey_atr_selected_labels);
                    if (selectedLabels == null) {
                        selectedLabels = new HashSet<String>();
                        v.setTag(R.id.tkey_atr_selected_labels, selectedLabels);
                    }

                    Set<String> selectedValues = (Set<String>) v.getTag(R.id.tkey_atr_selected);
                    if (selectedValues == null) {
                        selectedValues = new HashSet<String>();
                        v.setTag(R.id.tkey_atr_selected, selectedValues);
                    }

                    final String[] ids = data.toString().split(",");
                    for (final String id : ids) {
                        if (TextUtils.isEmpty(id)) {
                            continue;
                        }
                        for (final Map.Entry<String, String> e : options.entrySet()) {
                            if (id.equalsIgnoreCase(e.getValue())) {
                                selectedLabels.add(e.getKey());
                                selectedValues.add(id);
                            }
                        }
                    }

                    if (selectedLabels.isEmpty() == false) {
                        String s = Arrays.toString(selectedLabels.toArray());
                        ((EditText) v).setText(s);
                    } else {
                        ((EditText) v).setText("");
                    }
                }
            } else if ("date".equalsIgnoreCase(atrType)) {
                try {
                    String s;
                    s = data.toString();
                    s = s.split(" ")[0];
                    String[] parts = s.split("-");
                    s = parts[1] + '/' + parts[2] + '/' + parts[0];
                    ((EditText) v).setText(s);
                } catch (Throwable e) {
                    // NOP
                }
            } else {
                ((EditText) v).setText(data.toString());
            }
        } else if (v instanceof Spinner) {
            // `data` is id of selected option

            final Object optionsTag = v.getTag(R.id.tkey_atr_options);
            if (optionsTag != null && optionsTag instanceof Map) {
                final Map<String, String> options = (Map<String, String>) optionsTag;
                String label = null;
                for (Map.Entry<String, String> e : options.entrySet()) {
                    if (data.toString().equalsIgnoreCase(e.getValue())) {
                        label = e.getKey();
                        break;
                    }
                }
                if (label != null) {
                    final Spinner spinner = (Spinner) v;
                    for (int i = 0; i < spinner.getCount(); i++) {
                        final Object item = spinner.getItemAtPosition(i);
                        if (item != null && item.equals(label)) {
                            spinner.setSelection(i);
                            break;
                        }
                    }
                }
            }
        }
    }

    protected void removeAttributeListV() {
        atrCodeToView.clear();

        atrListWrapperV.setVisibility(View.GONE);
        atrListV.removeAllViews();
    }

    private void showAttributeListV(boolean showProgressBar) {
        atrListWrapperV.setVisibility(View.VISIBLE);
        atrListProgressV.setVisibility(showProgressBar ? View.VISIBLE : View.GONE);
    }

    @SuppressWarnings("unchecked")
    protected View newAtrEditView(Map<String, Object> atrData) {
        final String code = atrData.get(MAGEKEY_ATTRIBUTE_CODE).toString();
        final String name = atrData.get(MAGEKEY_ATTRIBUTE_INAME).toString();


        // If Product is Barcode then return null;
        if(TextUtils.equals(code, "product_barcode_"))
        {
        	return null;
        }	        

        
        if (TextUtils.isEmpty(name)) {
            // y: ?
            throw new RuntimeException("bad data...");
        }

        final String type = "" + atrData.get(MAGEKEY_ATTRIBUTE_TYPE);
        Map<String, String> options = null;
        List<String> labels = null;

        // y: actually the "dropdown" type is just a "select" type, but it's added here for clarity
        if ("boolean".equalsIgnoreCase(type) || "select".equalsIgnoreCase(type) || "multiselect".equalsIgnoreCase(type)
                || "dropdown".equalsIgnoreCase(type) || atrData.containsKey(MAGEKEY_ATTRIBUTE_IOPTIONS)) {
            final List<Object> tmp = (List<Object>) atrData.get(MAGEKEY_ATTRIBUTE_IOPTIONS);
            if (tmp != null) {
                options = new HashMap<String, String>(tmp.size());
                labels = new ArrayList<String>(tmp.size());
                for (final Object obj : tmp) {
                    if (obj == null) {
                        continue;
                    } else if (obj instanceof Map) {
                        final Map<String, Object> asMap = (Map<String, Object>) obj;
                        final Object label = asMap.get("label");
                        final Object value = asMap.get("value");
                        if (label != null && value != null) {
                            final String labelAsStr = label.toString();
                            final String valueAsStr = value.toString();
                            if (labelAsStr.length() > 0 && valueAsStr.length() > 0) {
                                options.put(labelAsStr, valueAsStr);
                                labels.add(labelAsStr);
                            }
                        }
                    }
                }
            }

            // handle boolean and select fields
            if (options != null && options.isEmpty() == false && "multiselect".equalsIgnoreCase(type) == false) {
                final View v = inflater.inflate(R.layout.product_attribute_spinner, null);
                final Spinner spinner = (Spinner) v.findViewById(R.id.spinner);
                final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                        android.R.layout.simple_spinner_dropdown_item, android.R.id.text1, labels);
                spinner.setAdapter(adapter);
                spinner.setTag(R.id.tkey_atr_code, code);
                spinner.setTag(R.id.tkey_atr_type, type);
                spinner.setTag(R.id.tkey_atr_options, options);
                boolean isRequired;
                if (atrData.containsKey(MAGEKEY_ATTRIBUTE_REQUIRED)
                        && "1".equals(atrData.get(MAGEKEY_ATTRIBUTE_REQUIRED).toString())) {
                    spinner.setTag(R.id.tkey_atr_required, Boolean.TRUE);
                    isRequired = true;
                } else {
                    spinner.setTag(R.id.tkey_atr_required, Boolean.FALSE);
                    isRequired = false;
                }
                
                final TextView label = (TextView) v.findViewById(R.id.label);
                label.setText(name + (isRequired ? "*" : ""));

                atrCodeToView.put(code, spinner);

                // atrSpinnerFields.add(spinner);
                return v;
            }
        }

        // TODO y: a lot of repetitions... move the common logic out

        // handle text fields, multiselect (special case text field), date (another special case), null, etc...

        final View v = inflater.inflate(R.layout.product_attribute_edit, null);
        EditText edit = (EditText) v.findViewById(R.id.edit);

        if ("price".equalsIgnoreCase(type)) {
            edit.setInputType(EditorInfo.TYPE_NUMBER_FLAG_DECIMAL);
        } else if ("multiselect".equalsIgnoreCase(type)) {
            if (options != null && options.isEmpty() == false) {
                edit.setTag(R.id.tkey_atr_options, options);

                final Map<String, String> finOptions = options;
                final List<String> finLabels = labels;

                edit.setFocusable(false);
                edit.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showMultiselectDialog((EditText) v, finOptions, finLabels);
                    }
                });
            }
        } else if ("date".equalsIgnoreCase(type)) {
            edit.setFocusable(false);
            edit.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDatepickerDialog((EditText) v);
                }
            });
        }

        boolean isRequired;
        if (atrData.containsKey(MAGEKEY_ATTRIBUTE_REQUIRED)
                && "1".equals(atrData.get(MAGEKEY_ATTRIBUTE_REQUIRED).toString())) {
            edit.setTag(R.id.tkey_atr_required, Boolean.TRUE);
            isRequired = true;
        } else {
            edit.setTag(R.id.tkey_atr_required, Boolean.FALSE);
            isRequired = false;
        }
        edit.setHint(name);
        edit.setTag(R.id.tkey_atr_code, code);
        edit.setTag(R.id.tkey_atr_type, type);
        
           
        atrCodeToView.put(code, edit);

        TextView label = (TextView) v.findViewById(R.id.label);
        label.setText(name + (isRequired ? "*" : ""));
        return v;
    }

    private void showDatepickerDialog(final EditText v) {
        final OnDateSetListener onDateSetL = new OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                monthOfYear += 1; // because it's from 0 to 11 for compatibility reasons
                final String date = "" + monthOfYear + "/" + dayOfMonth + "/" + year;
                v.setText(date);
            }
        };

        final Calendar c = Calendar.getInstance();

        // parse date if such is present
        try {
            final SimpleDateFormat f = new SimpleDateFormat("M/d/y");
            final Date d = f.parse(v.getText().toString());
            c.setTime(d);
        } catch (Throwable ignored) {
        }

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        final Dialog d = new DatePickerDialog(this, onDateSetL, year, month, day);
        d.show();
    }

    @SuppressWarnings("unchecked")
    private void showMultiselectDialog(final EditText v, final Map<String, String> options, final List<String> labels) {
        final CharSequence[] items = new CharSequence[labels.size()];
        for (int i = 0; i < labels.size(); i++) {
            items[i] = labels.get(i);
        }

        // say which items should be checked on start
        final boolean[] checkedItems = new boolean[labels.size()];
        final Object labelTag = v.getTag(R.id.tkey_atr_selected_labels);
        if (labelTag != null && labelTag instanceof Collection) {
            final Collection<String> selectedLabels = (Collection<String>) labelTag;
            for (int i = 0; i < labels.size(); i++) {
                if (selectedLabels.contains(labels.get(i))) {
                    checkedItems[i] = true;
                }
            }
        }

        // create the dialog
        final Dialog dialog = new AlertDialog.Builder(this).setTitle("Options").setCancelable(false)
                .setMultiChoiceItems(items, checkedItems, new OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        Object obj;

                        final Set<String> selectedValues;
                        if ((obj = v.getTag(R.id.tkey_atr_selected)) == null) {
                            selectedValues = new HashSet<String>();
                            v.setTag(R.id.tkey_atr_selected, selectedValues);
                        } else {
                            selectedValues = (Set<String>) obj;
                        }

                        final Set<String> selectedLabels;
                        if ((obj = v.getTag(R.id.tkey_atr_selected_labels)) == null) {
                            selectedLabels = new HashSet<String>();
                            v.setTag(R.id.tkey_atr_selected_labels, selectedLabels);
                        } else {
                            selectedLabels = (Set<String>) obj;
                        }

                        final String label = items[which].toString();
                        final String val = options.get(label);

                        if (isChecked) {
                            selectedValues.add(val);
                            selectedLabels.add(label);
                        } else {
                            selectedValues.remove(val);
                            selectedLabels.remove(label);
                        }
                    }
                }).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final Set<String> selectedLabels = (Set<String>) v.getTag(R.id.tkey_atr_selected_labels);
                        if (selectedLabels != null && selectedLabels.isEmpty() == false) {
                            String s = Arrays.toString(selectedLabels.toArray());
                            v.setText(s);
                        } else {
                            v.setText("");
                        }
                    }
                }).create();
        dialog.show();
    }

    // task listeners

    protected void onAttributeSetLoadStart() {
        atrSetLabelV.setTextColor(Color.GRAY);
        atrSetProgressV.setVisibility(View.VISIBLE);
        attributeSetV.setClickable(false);
        attributeSetV.setHint("Loading attribute sets...");
    }

    protected void onAttributeSetLoadFailure() {
        atrSetLabelV.setTextColor(Color.RED);
        atrSetProgressV.setVisibility(View.INVISIBLE);
        attributeSetV.setClickable(true);
        attributeSetV.setHint("Load failed... Check settings and refresh");
    }

    protected void onAttributeSetLoadSuccess() {
        atrSetLabelV.setTextColor(Color.WHITE);
        atrSetProgressV.setVisibility(View.INVISIBLE);
        attributeSetV.setClickable(true);
        attributeSetV.setHint("Click to select an attribute set...");
    }

    protected void onCategoryLoadStart() {
        categoryLabelV.setTextColor(Color.GRAY);
        categoryProgressV.setVisibility(View.VISIBLE);
        categoryV.setClickable(false);
        categoryV.setHint("Loading categories...");
    }

    protected void onCategoryLoadFailure() {
        categoryLabelV.setTextColor(Color.RED);
        categoryProgressV.setVisibility(View.INVISIBLE);
        categoryV.setClickable(true);
        categoryV.setHint("Load failed... Check settings and refresh");
    }

    protected void onCategoryLoadSuccess() {
        categoryLabelV.setTextColor(Color.WHITE);
        categoryProgressV.setVisibility(View.INVISIBLE);
        categoryV.setClickable(true);
        categoryV.setHint("Click to select a category...");
    }

    protected void onAttributeListLoadSuccess() {
        atrListLabelV.setTextColor(Color.WHITE);
        List<Map<String, Object>> atrList = getAttributeList();
        if(atrList.size() > 1)
        	buildAtrList(atrList);
        else
        {        	
        	atrListWrapperV.setVisibility(View.GONE);
        }
    }

    protected void onAttributeListLoadFailure() {
        atrListLabelV.setTextColor(Color.RED);
        atrListProgressV.setVisibility(View.GONE);
    }

    protected void onAttributeListLoadStart() {
        // clean the list
        atrListLabelV.setTextColor(Color.GRAY);
        removeAttributeListV();
        showAttributeListV(true);
    }

    
    private OnLongClickListener scanBarcodeOnClickL = new OnLongClickListener() {
		
		@Override
		public boolean onLongClick(View v) {
            Intent scanInt = new Intent("com.google.zxing.client.android.SCAN");
            startActivityForResult(scanInt, SCAN_BARCODE);
            return true;
		}
	};


    
    // helper methods

    private static void attachListenerToEditText(final EditText view, final OnClickListener onClickL) {
        // view.setOnFocusChangeListener(new OnFocusChangeListener() {
        // @Override
        // public void onFocusChange(View v, boolean hasFocus) {
        // if (hasFocus) {
        // onClickL.onClick(v);
        // }
        // }
        // });
        // view.setOnClickListener(new OnClickListener() {
        // @Override
        // public void onClick(View v) {
        // if (v.hasFocus()) {
        // onClickL.onClick(v);
        // }
        // }
        // });
        view.setOnClickListener(onClickL);
    }

}
