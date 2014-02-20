package com.mageventory.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.text.TextUtils;
import android.widget.AutoCompleteTextView;

import com.mageventory.widget.AutoCompleteTextViewCompoundArrayAdapter;

/**
 * Various utils for the operations with input cache
 * 
 * @author Eugene Popovich
 */
public class InputCacheUtils {

    private static final int MAX_INPUT_CACHE_LIST_SIZE = 100;
    public static final String DICTONARY_SUFFIX = "_dic";

    public static void initAutoCompleteTextViewWithAdapterFromInputCache(String attributeKey,
            Map<String, List<String>> inputCache, AutoCompleteTextView autoCompleteTextView, Context context) {
        List<String> values = inputCache.get(attributeKey);
        /* Associate auto completion adapter with the "name" edit text */
        if (values != null) {
            initInputCacheDictionaryForAttributeIfAbsent(attributeKey, inputCache);
            List<Object> data = new ArrayList<Object>(values);

            AutoCompleteTextViewCompoundArrayAdapter.HorizontalArrayAdapter adapter = new AutoCompleteTextViewCompoundArrayAdapter.HorizontalArrayAdapter(
                    context, inputCache.get(attributeKey + DICTONARY_SUFFIX), autoCompleteTextView);
            data.add(0, adapter);

            final AutoCompleteTextViewCompoundArrayAdapter nameAdapter = new AutoCompleteTextViewCompoundArrayAdapter(
                    context, data, autoCompleteTextView);
            autoCompleteTextView.setAdapter(nameAdapter);
        }
    }

    /**
     * Init input cache dictionary for attribute if it is not yer initialized.
     * Necessary for the first run and before the input cache save operation
     * 
     * @param attributeKey
     * @param inputCache
     */
    private static void initInputCacheDictionaryForAttributeIfAbsent(String attributeKey,
            Map<String, List<String>> inputCache) {
        List<String> dictionaryList = getInputCacheValues(attributeKey + DICTONARY_SUFFIX,
                inputCache);
        if (dictionaryList.isEmpty())
        {
            List<String> list = getInputCacheValues(attributeKey, inputCache);
            for (int i = list.size() - 1; i >= 0; i--) {
                String value = list.get(i);
                if (addWordsToInputCacheList(value, dictionaryList) <= 1) {
                    list.remove(i);
                }
            }
        }
    }

    /**
     * Add new words to input cache dictionary for the attribute key
     * 
     * @param attributeKey
     * @param value
     * @return number of words
     */
    private static int addWordsToInputCacheList(String attributeKey, String value,
            Map<String, List<String>> inputCache) {
        if (TextUtils.isEmpty(value))
            return 0;
        List<String> list = getInputCacheValues(attributeKey, inputCache);
        return addWordsToInputCacheList(value, list);
    }

    /**
     * Add new words to input cache dictionary list
     * 
     * @param value
     * @param list
     * @return
     */
    private static int addWordsToInputCacheList(String value, List<String> list) {
        String[] words = AutoCompleteTextViewCompoundArrayAdapter.splitToWords(value);
        for (String word : words) {
            if (word.length() >= 3) {
                addValueToInputCacheList(word, list);
            }
        }
        return words.length;
    }

    /**
     * Helper function. Allows to add a new value to the input cache list
     * associated with a given attribute key.
     * 
     * @param attributeKey
     * @param value
     * @param inputCache
     */
    public static void addValueToInputCacheList(String attributeKey, String value,
            Map<String, List<String>> inputCache) {
        /* Don't store empty values in the cache. */
        if (TextUtils.isEmpty(value))
            return;

        // Do not add single word, we have dictionary for it
        if (addWordsToInputCacheList(attributeKey + DICTONARY_SUFFIX, value, inputCache) <= 1) {
            return;
        }

        List<String> list = getInputCacheValues(attributeKey, inputCache);

        addValueToInputCacheList(value, list);
    }

    /**
     * Add the value to the input cache list and check whether the list size
     * doesn't exceed MAX_IPUT_CACHE_LIST_SIZE
     * 
     * @param value
     * @param list
     */
    private static void addValueToInputCacheList(String value, List<String> list) {
        /*
         * Remove the value if it's already on the list. Then re-add it on the
         * first position.
         */
        list.remove(value);
        list.add(0, value);

        /*
         * If after addition of an element list size exceeds 100 then remove the
         * last element.
         */
        if (list.size() > MAX_INPUT_CACHE_LIST_SIZE)
            list.remove(MAX_INPUT_CACHE_LIST_SIZE);
    }

    /**
     * Get or create input cache values from the input cache for the attribute
     * key
     * 
     * @param attributeKey
     * @param inputCache
     * @return
     */
    private static List<String> getInputCacheValues(String attributeKey,
            Map<String, List<String>> inputCache) {
        List<String> list = inputCache.get(attributeKey);

        if (list == null) {
            list = new ArrayList<String>();
            inputCache.put(attributeKey, list);
        }
        return list;
    }
}
