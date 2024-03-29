/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mageventory.widget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.mageventory.util.CommonUtils;
import com.mageventory.util.GuiUtils;

/**
 * A concrete BaseAdapter that is backed by an array of arbitrary objects. By
 * default this class expects that the provided resource id references a single
 * TextView. If you want to use a more complex layout, use the constructors that
 * also takes a field id. That field id should reference a TextView in the
 * larger layout resource.
 * <p>
 * However the TextView is referenced, it will be filled with the toString() of
 * each object in the array. You can add lists or arrays of custom objects.
 * Override the toString() method of your objects to determine what text will be
 * displayed for the item in the list.
 * <p>
 * To use something other than TextViews for the array display, for instance,
 * ImageViews, or to have some of data besides toString() results fill the
 * views, override {@link #getView(int, View, ViewGroup)} to return the type of
 * view you want.
 */
public class CustomArrayAdapter<T> extends BaseAdapter implements Filterable {

    public static final String TAG = CustomArrayAdapter.class.getSimpleName();
    /**
     * Contains the list of objects that represent the data of this
     * ArrayAdapter. The content of this list is referred to as "the array" in
     * the documentation.
     */
    private List<T> mObjects;

    /**
     * Lock used to modify the content of {@link #mObjects}. Any write operation
     * performed on the array should be synchronized on this lock. This lock is
     * also used by the filter (see {@link #getFilter()} to make a synchronized
     * copy of the original array of data.
     */
    private final Object mLock = new Object();

    /**
     * The resource indicating what views to inflate to display the content of
     * this array adapter.
     */
    private int mResource;

    /**
     * The resource indicating what views to inflate to display the content of
     * this array adapter in a drop down widget.
     */
    private int mDropDownResource;

    /**
     * If the inflated resource is not a TextView, {@link #mFieldId} is used to
     * find a TextView inside the inflated views hierarchy. This field must
     * contain the identifier that matches the one defined in the resource file.
     */
    private int mFieldId = 0;

    /**
     * Indicates whether or not {@link #notifyDataSetChanged()} must be called
     * whenever {@link #mObjects} is modified.
     */
    private boolean mNotifyOnChange = true;

    private Context mContext;

    // A copy of the original mObjects array, initialized from and then used
    // instead as soon as
    // the mFilter ArrayFilter is used. mObjects will then only contain the
    // filtered values.
    private ArrayList<T> mOriginalValues;
    private ArrayFilter mFilter;

    private LayoutInflater mInflater;

    /**
     * Constructor
     * 
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a TextView
     *            to use when instantiating views.
     */
    public CustomArrayAdapter(Context context, int resource) {
        init(context, resource, 0, new ArrayList<T>());
    }

    /**
     * Constructor
     * 
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a layout to
     *            use when instantiating views.
     * @param textViewResourceId The id of the TextView within the layout
     *            resource to be populated
     */
    public CustomArrayAdapter(Context context, int resource, int textViewResourceId) {
        init(context, resource, textViewResourceId, new ArrayList<T>());
    }

    /**
     * Constructor
     * 
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a TextView
     *            to use when instantiating views.
     * @param objects The objects to represent in the ListView.
     */
    public CustomArrayAdapter(Context context, int resource, T[] objects) {
        init(context, resource, 0, Arrays.asList(objects));
    }

    /**
     * Constructor
     * 
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a layout to
     *            use when instantiating views.
     * @param textViewResourceId The id of the TextView within the layout
     *            resource to be populated
     * @param objects The objects to represent in the ListView.
     */
    public CustomArrayAdapter(Context context, int resource, int textViewResourceId, T[] objects) {
        init(context, resource, textViewResourceId, Arrays.asList(objects));
    }

    /**
     * Constructor
     * 
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a TextView
     *            to use when instantiating views.
     * @param objects The objects to represent in the ListView.
     */
    public CustomArrayAdapter(Context context, int resource, List<T> objects) {
        init(context, resource, 0, objects);
    }

    /**
     * Constructor
     * 
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a layout to
     *            use when instantiating views.
     * @param textViewResourceId The id of the TextView within the layout
     *            resource to be populated
     * @param objects The objects to represent in the ListView.
     */
    public CustomArrayAdapter(Context context, int resource, int textViewResourceId, List<T> objects) {
        init(context, resource, textViewResourceId, objects);
    }

    /**
     * Adds the specified object at the end of the array.
     * 
     * @param object The object to add at the end of the array.
     */
    public void add(T object) {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                mOriginalValues.add(object);
            } else {
                mObjects.add(object);
            }
        }
        if (mNotifyOnChange)
            notifyDataSetChanged();
    }

    /**
     * Adds the specified Collection at the end of the array.
     * 
     * @param collection The Collection to add at the end of the array.
     */
    public void addAll(Collection<? extends T> collection) {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                mOriginalValues.addAll(collection);
            } else {
                mObjects.addAll(collection);
            }
        }
        if (mNotifyOnChange)
            notifyDataSetChanged();
    }

    /**
     * Adds the specified items at the end of the array.
     * 
     * @param items The items to add at the end of the array.
     */
    public void addAll(T... items) {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                Collections.addAll(mOriginalValues, items);
            } else {
                Collections.addAll(mObjects, items);
            }
        }
        if (mNotifyOnChange)
            notifyDataSetChanged();
    }

    /**
     * Inserts the specified object at the specified index in the array.
     * 
     * @param object The object to insert into the array.
     * @param index The index at which the object must be inserted.
     */
    public void insert(T object, int index) {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                mOriginalValues.add(index, object);
            } else {
                mObjects.add(index, object);
            }
        }
        if (mNotifyOnChange)
            notifyDataSetChanged();
    }

    /**
     * Removes the specified object from the array.
     * 
     * @param object The object to remove.
     */
    public void remove(T object) {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                mOriginalValues.remove(object);
            } else {
                mObjects.remove(object);
            }
        }
        if (mNotifyOnChange)
            notifyDataSetChanged();
    }

    /**
     * Remove all elements from the list.
     */
    public void clear() {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                mOriginalValues.clear();
            } else {
                mObjects.clear();
            }
        }
        if (mNotifyOnChange)
            notifyDataSetChanged();
    }

    /**
     * Sorts the content of this adapter using the specified comparator.
     * 
     * @param comparator The comparator used to sort the objects contained in
     *            this adapter.
     */
    public void sort(Comparator<? super T> comparator) {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                Collections.sort(mOriginalValues, comparator);
            } else {
                Collections.sort(mObjects, comparator);
            }
        }
        if (mNotifyOnChange)
            notifyDataSetChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        mNotifyOnChange = true;
    }

    /**
     * Control whether methods that change the list ({@link #add},
     * {@link #insert}, {@link #remove}, {@link #clear}) automatically call
     * {@link #notifyDataSetChanged}. If set to false, caller must manually call
     * notifyDataSetChanged() to have the changes reflected in the attached
     * view. The default is true, and calling notifyDataSetChanged() resets the
     * flag to true.
     * 
     * @param notifyOnChange if true, modifications to the list will
     *            automatically call {@link #notifyDataSetChanged}
     */
    public void setNotifyOnChange(boolean notifyOnChange) {
        mNotifyOnChange = notifyOnChange;
    }

    private void init(Context context, int resource, int textViewResourceId, List<T> objects) {
        mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResource = mDropDownResource = resource;
        mObjects = objects;
        mFieldId = textViewResourceId;
    }

    /**
     * Returns the context associated with this array adapter. The context is
     * used to create views from the resource passed to the constructor.
     * 
     * @return The Context associated with this adapter.
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCount() {
        return mObjects.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getItem(int position) {
        return mObjects.get(position);
    }

    /**
     * Returns the position of the specified item in the array.
     * 
     * @param item The item to retrieve the position of.
     * @return The position of the specified item.
     */
    public int getPosition(T item) {
        return mObjects.indexOf(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, mResource);
    }

    private View createViewFromResource(int position, View convertView, ViewGroup parent,
            int resource) {
        View view;
        TextView text;

        if (convertView == null) {
            view = mInflater.inflate(resource, parent, false);
        } else {
            view = convertView;
        }

        try {
            if (mFieldId == 0) {
                // If no custom field is assigned, assume the whole resource is
                // a TextView
                text = (TextView) view;
            } else {
                // Otherwise, find the TextView field within the layout
                text = (TextView) view.findViewById(mFieldId);
            }
        } catch (ClassCastException e) {
            Log.e("ArrayAdapter", "You must supply a resource ID for a TextView");
            throw new IllegalStateException(
                    "ArrayAdapter requires the resource ID to be a TextView", e);
        }

        T item = getItem(position);
        if (item instanceof CharSequence) {
            text.setText((CharSequence) item);
        } else {
            text.setText(item.toString());
        }

        return view;
    }

    /**
     * <p>
     * Sets the layout resource to create the drop down views.
     * </p>
     * 
     * @param resource the layout resource defining the drop down views
     * @see #getDropDownView(int, android.view.View, android.view.ViewGroup)
     */
    public void setDropDownViewResource(int resource) {
        this.mDropDownResource = resource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, mDropDownResource);
    }

    /**
     * Creates a new ArrayAdapter from external resources. The content of the
     * array is obtained through
     * {@link android.content.res.Resources#getTextArray(int)}.
     * 
     * @param context The application's environment.
     * @param textArrayResId The identifier of the array to use as the data
     *            source.
     * @param textViewResId The identifier of the layout used to create views.
     * @return An ArrayAdapter<CharSequence>.
     */
    public static CustomArrayAdapter<CharSequence> createFromResource(Context context,
            int textArrayResId, int textViewResId) {
        CharSequence[] strings = context.getResources().getTextArray(textArrayResId);
        return new CustomArrayAdapter<CharSequence>(context, textViewResId, strings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new ArrayFilter();
        }
        return mFilter;
    }

    public void setFilter(ArrayFilter filter) {
        mFilter = filter;
    }

    protected CharSequence processPrefix(CharSequence prefix) {
        return prefix;
    }
    /**
     * <p>
     * An array filter constrains the content of the array adapter with a
     * prefix. Each item that does not start with the supplied prefix is removed
     * from the list.
     * </p>
     */
    public class ArrayFilter extends Filter {

        protected T wrapValueWithPrefix(T value, String prefix) {
            return value;
        }

        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            try {
                FilterResults results = new FilterResults();

                prefix = processPrefix(prefix);

                if (mOriginalValues == null) {
                    synchronized (mLock) {
                        mOriginalValues = new ArrayList<T>(mObjects);
                    }
                }

                if (prefix == null || prefix.length() == 0) {
                    ArrayList<T> list;
                    synchronized (mLock) {
                        list = new ArrayList<T>(mOriginalValues);
                    }
                    results.values = list;
                    results.count = list.size();
                } else {
                    String prefixString = prefix.toString().toLowerCase();

                    ArrayList<T> values;
                    synchronized (mLock) {
                        values = new ArrayList<T>(mOriginalValues);
                    }

                    final int count = values.size();
                    final ArrayList<T> newValues = new ArrayList<T>();

                    for (int i = 0; i < count; i++) {
                        final T value = values.get(i);
                        if (value instanceof Filterable) {
                            final Filterable filterable = (Filterable) value;
                            Filter filter = filterable.getFilter();
                            if (filter != null && filter instanceof CustomArrayAdapter.ArrayFilter) {
                                CustomArrayAdapter<?>.ArrayFilter afilter = (CustomArrayAdapter<?>.ArrayFilter) filter;
                                FilterResults fr = afilter.performFiltering(prefix);
                                CommonUtils.debug(TAG,
                                        "performFiltering: Class: %1$s; mObjects: %2$b",
                                        CustomArrayAdapter.this.getClass().getSimpleName(),
                                        fr.values == null);
                                afilter.publishResults(prefixString, fr);
                                if (fr.count > 0) {
                                    newValues.add(value);
                                }
                            }
                        } else {
                            final String valueText = value.toString().toLowerCase();
                            final String prefixStringOriginal = prefix.toString();
                            // First match against the whole, non-splitted value
                            if (valueText.startsWith(prefixString)) {
                                newValues.add(wrapValueWithPrefix(value, prefixStringOriginal));
                            } else {
                                final String[] words = valueText.split(" ");
                                final int wordCount = words.length;

                                // Start at index 0, in case valueText starts
                                // with
                                // space(s)
                                for (int k = 0; k < wordCount; k++) {
                                    if (words[k].startsWith(prefixString)) {
                                        newValues.add(value);
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    results.values = newValues;
                    results.count = newValues.size();
                }

                return results;
            } catch (Throwable t) {
                CommonUtils.error(TAG, t);
                throw new RuntimeException(t);
            }
        }

        @Override
        protected void publishResults(CharSequence constraint, final FilterResults results) {
            try {
                // synchronize adapter to avoid situations when data used in
                // HorizontalListView.onLayout method become outdated
                synchronized (CustomArrayAdapter.this) {
                    // noinspection unchecked
                    mObjects = (List<T>) results.values;
                    CommonUtils.debug(TAG, "publishResults: Class: %1$s; mObjects: %2$b",
                            CustomArrayAdapter.this.getClass().getSimpleName(), mObjects == null);
                }
                if (results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            } catch (Exception ex) {
                GuiUtils.error(TAG, ex);
            }
        }
    }
}

