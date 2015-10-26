package com.mageventory.widget;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ClipData;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.mageventory.util.CommonUtils;
import com.mventory.R;

/**
 * Components set to manage the search words related views and functionality
 * 
 * @author Eugene Popovich
 */
public class SearchWordsSet {
    /**
     * Tag used for logging
     */
    static final String TAG = SearchWordsSet.class.getSimpleName();
    /**
     * The adapter for the list view which stores words used for search
     */
    WordsAdapter mUseForSearchAdapter;
    /**
     * The original query which contains all possible words for the search
     */
    String mOriginalQuery;
    /**
     * The actual search query. The list view which stores words used for search
     * will be filled from this.
     */
    String mQuery;

    /**
     * Set the data which should be used by fragment
     * 
     * @param query the actual query which is used for search
     * @param originalQuery the original query which contains all possible words
     *            which may be used for search
     */
    public void setData(String query, String originalQuery) {
        mQuery = query;
        mOriginalQuery = originalQuery;
    }

    /**
     * Initialize the search words related views
     * 
     * @param view the parent view container
     * @param savedInstanceState the previously saved instanc state
     * @param activity the parent activity
     */
    public void init(View view, Bundle savedInstanceState, Activity activity) {
        // all possible words which may be used for query
        List<String> possibleWords = CommonUtils.getUniqueWords(mOriginalQuery, true);
        // the words which are currently used for search
        List<String> actualWords = CommonUtils.getUniqueWords(mQuery, true);
        // the list of words which are absennt in the actualWords but
        // present in the possibleWords
        List<String> missingWords = new ArrayList<String>();
        for (String possibleWord : possibleWords) {
            String lcPossibleWord = possibleWord.toLowerCase();
            // the flag indicating word found in the actualWords
            boolean found = false;
            for (String word : actualWords) {
                if (TextUtils.equals(word.toLowerCase(), lcPossibleWord)) {
                    // set the flag and interrupt the loop
                    found = true;
                    break;
                }
            }
            if (!found) {
                // the word is absent in the actualWords
                missingWords.add(possibleWord);
            }
        }
        WordsAdapter possibleWordsAdapter = new WordsAdapter(
                CommonUtils.getColorResource(R.color.search_option_possible), activity);
        possibleWordsAdapter.addAll(missingWords);
        mUseForSearchAdapter = new WordsAdapter(null, activity);
        mUseForSearchAdapter.addAll(actualWords);

        ListView possibleWordsList = (ListView) view.findViewById(R.id.left);
        possibleWordsList.setAdapter(possibleWordsAdapter);
        ListView useForSearchList = (ListView) view.findViewById(R.id.right);
        useForSearchList.setAdapter(mUseForSearchAdapter);

        possibleWordsList.setOnDragListener(new WordDragListener(possibleWordsList,
                mUseForSearchAdapter, possibleWordsAdapter));
        useForSearchList.setOnDragListener(new WordDragListener(useForSearchList,
                possibleWordsAdapter, mUseForSearchAdapter));

    }

    /**
     * Get all the currently selected words
     * 
     * @return
     */
    public List<String> getSelectedWords() {
        List<String> selectedWords = new ArrayList<String>(mUseForSearchAdapter.getCount());
        // get all the selected words
        for (int i = 0, size = mUseForSearchAdapter.getCount(); i < size; i++) {
            selectedWords.add(mUseForSearchAdapter.getItem(i));
        }
        return selectedWords;
    }
    /**
     * WordDragListener will handle dragged word views being dropped on the drop
     * area
     */
    private class WordDragListener implements OnDragListener {
        /**
         * The adapter from where the word should be removed on drop event
         */
        WordsAdapter mCopyFromAdapter;
        /**
         * The adapter to where the word should be added on drop event
         */
        WordsAdapter mCopyToAdapter;
        /**
         * The list where the drop action should be handled
         */
        ListView mCopyToList;

        /**
         * @param list the list where the drop action should be handled
         * @param copyFromAdapter the adapter from where the word should be
         *            removed on drop event
         * @param copyToAdapter the list where the drop action should be handled
         */
        WordDragListener(ListView list, WordsAdapter copyFromAdapter, WordsAdapter copyToAdapter) {
            mCopyToList = list;
            mCopyFromAdapter = copyFromAdapter;
            mCopyToAdapter = copyToAdapter;
        }

        @Override
        public boolean onDrag(View v, DragEvent event) {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    // no action necessary
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
                    // no action necessary
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    // no action necessary
                    break;
                case DragEvent.ACTION_DROP:
                    // handle the dragged view being dropped over a drop view

                    // get the dropping view
                    TextView view = (TextView) event.getLocalState();
                    // get the dropping word
                    String word = view.getText().toString();
                    if (mCopyFromAdapter.getPosition(word) != -1) {
                        // if the word is present in the copyFromAdapter. This
                        // mean word is not dropped to the copy to list itself

                        // remove text from the source adapter
                        mCopyFromAdapter.remove(word);

                        // get the index where the text should be inserted to
                        // the target adapter. It depends on the event
                        // coordinates
                        int ind = -1;
                        for (int i = 0; i < mCopyToList.getChildCount(); i++) {
                            View child = mCopyToList.getChildAt(i);
                            // get the middle Y coordinate of the list child
                            float middle = child.getY() + child.getHeight() / 2;
                            if (middle > event.getY()) {
                                // if the middle of the child is more than event
                                // Y coordinate interrupt the loop
                                break;
                            }
                            // remember view position, if loop will end it will
                            // be the last view position the word should be
                            // inserted
                            ind = i;
                        }
                        // get the adapter index from the view position
                        ind += mCopyToList.getFirstVisiblePosition() + 1;
                        // insert word to target adapter
                        mCopyToAdapter.insert(word, ind);
                    }
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    // no action necessary
                    break;
                default:
                    break;
            }
            return true;
        }
    }

    /**
     * The list adapter to represent words used for search options
     */
    public class WordsAdapter extends ArrayAdapter<String> {
        /**
         * The alternative text colors for the view
         */
        Integer mAlternateTextColor;

        /**
         * @param alternateTextColor the alternative text colors for the view
         */
        public WordsAdapter(Integer alternateTextColor, Activity activity) {
            super(activity, R.layout.searh_keyword_option, android.R.id.text1);
            mAlternateTextColor = alternateTextColor;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View result = super.getView(position, convertView, parent);
            if (convertView == null && mAlternateTextColor != null) {
                ((TextView) result.findViewById(android.R.id.text1))
                        .setTextColor(mAlternateTextColor);
            }
            result.setOnTouchListener(new WordTouchListener());
            return result;
        }

        private final class WordTouchListener implements OnTouchListener {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    ClipData data = ClipData.newPlainText("", "");
                    View tv = view.findViewById(android.R.id.text1);
                    DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(tv);
                    // start dragging the item touched
                    tv.startDrag(data, shadowBuilder, tv, 0);
                    return true;
                } else {
                    return false;
                }
            }
        }
    }
}
