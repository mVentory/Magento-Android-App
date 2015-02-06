/* Copyright (c) 2014 mVentory Ltd. (http://mventory.com)
 * 
* License       http://creativecommons.org/licenses/by-nc-nd/4.0/
* 
* NonCommercial — You may not use the material for commercial purposes. 
* NoDerivatives — If you compile, transform, or build upon the material,
* you may not distribute the modified material. 
* Attribution — You must give appropriate credit, provide a link to the license,
* and indicate if changes were made. You may do so in any reasonable manner, 
* but not in any way that suggests the licensor endorses you or your use. 
*/
package com.mageventory.activity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mageventory.MageventoryConstants;
import com.mventory.R;
import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.fragment.base.BaseFragment;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.EventBusUtils;
import com.mageventory.util.EventBusUtils.EventType;
import com.mageventory.util.EventBusUtils.GeneralBroadcastEventHandler;
import com.viewpagerindicator.CirclePageIndicator;
import com.viewpagerindicator.PageIndicator;

/**
 * Welcom screen activity
 * 
 * @author Eugene Popovich
 */
public class WelcomeActivity extends BaseFragmentActivity implements MageventoryConstants,
        GeneralBroadcastEventHandler {
    static final String TAG = WelcomeActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new WelcomeUiFragment()).commit();
        }
        EventBusUtils.registerOnGeneralEventBroadcastReceiver(TAG, this, this);
    }
    
    @Override
    public void onGeneralBroadcastEvent(EventType eventType, Intent extra) {
        switch (eventType) {
            case PROFILE_CONFIGURED:
                if (isActivityAlive()) {
                    finish();
                }
                break;
            default:
                break;
        }
    }

    public static class WelcomeUiFragment extends BaseFragment {
        /**
         * An enum describing possible WelcomeUiFragment states
         */
        enum State {
            /**
             * The state for the information slides display functionality
             */
            SLIDES,
            /**
             * The state for the My Store information display functionality
             */
            MY_STORE,
            /**
             * The state for the No action selected in My Store information
             * display functionality
             */
            MY_STORE_NO,
            /**
             * The state for the Yes action selected in My Store information
             * display functionality
             */
            MY_STORE_YES,
            /**
             * The state for the Demo store information display functionality
             */
            DEMO_STORE,
            /**
             * The state for the More information display functionality
             */
            MORE_INFO;
            /**
             * Check whether the state is related to My Store flow
             * 
             * @param state
             * @return
             */
            public static boolean isMyStoreFlowRelatedState(State state) {
                return state == MY_STORE || state == MY_STORE_YES || state == MY_STORE_NO;
            }

            /**
             * Check whether the state is of message with list type
             * 
             * @param state
             * @return
             */
            public static boolean isMessageWithListRelatedState(State state) {
                return state == MY_STORE_YES || state == MY_STORE_NO;
            }
        }

        /**
         * The current fragment state
         */
        State mCurrentState;
        /**
         * The view which indicates selected one of My Store states
         */
        View mConnectToMyStoreIndicator;
        /**
         * The view which indicates selected {@link State#DEMO_STORE} state
         */
        View mConnectToDemoStoreIndicator;
        /**
         * The first part of view which indicates selected
         * {@link State#MORE_INFO} state
         */
        View mMoreInfoIndicator1;
        /**
         * The second part of view which indicates selected
         * {@link State#MORE_INFO} state
         */
        View mMoreInfoIndicator2;
        /**
         * The view for the states with simple messages
         */
        View mMessageView;

        /**
         * The view to display numbered list items in the message related states
         */
        LinearLayout mMessageListView;
        
        /**
         * The view to display text in the message related states
         */
        TextView mMessageTextView;
        /**
         * The view with slides for the {@link State#SLIDES} state
         */
        View mSlidesView;
        /**
         * The view with question and choices for the {@link State#MY_STORE} state
         */
        View mQuestionView;
        /**
         * The root scroll container
         */
        ScrollView mScroll;
        /**
         * The view pager for the slides shown in the {@link State#SLIDES} state
         */
        private ViewPager mSlidesPager;
        /**
         * The adapter for the slides pager
         */
        private SlidesAdapter mSlidesAdapter;
        /**
         * Reference to the active instance of {@link ChangeSlideRunnable}
         */
        ChangeSlideRunnable mChangeSlideRunnable;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);
            View view = getActivity().getLayoutInflater().inflate(R.layout.welcome, container,
                    false);
            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            init(view);
        }

        public void init(View view) {
            mScroll = (ScrollView) view.findViewById(R.id.scroll);
            /*
             * Initialize buttons
             */
            // initialize on click listener for buttons
            OnClickListener clickListener = new OnClickListener() {

                @Override
                public void onClick(View v) {
                    switch (v.getId()) {
                        case R.id.connectToDemoStoreButton:
                            setState(State.DEMO_STORE);
                            break;
                        case R.id.connectToMyStoreButton:
                            setState(State.MY_STORE);
                            break;
                        case R.id.moreInfoButton:
                            setState(State.MORE_INFO);
                            break;
                        case R.id.storeAdminYesButton:
                            setState(State.MY_STORE_YES);
                            break;
                        case R.id.storeAdminNoButton:
                            setState(State.MY_STORE_NO);
                            break;
                        case R.id.slideLeftBtn:
                            cancelAutomaticSlideChangeIfNecessary();
                            if (mSlidesPager.getCurrentItem() > 0) {
                                mSlidesPager
                                        .setCurrentItem(mSlidesPager.getCurrentItem() - 1, true);
                            } else {
                                mSlidesPager.setCurrentItem(mSlidesAdapter.getCount() - 1, true);
                            }
                            break;
                        case R.id.slideRightBtn:
                            cancelAutomaticSlideChangeIfNecessary();
                            changeSlideToNext();
                            break;
                    }
                }
            };
            Button connectToMyStoreButton = (Button) view.findViewById(R.id.connectToMyStoreButton);
            connectToMyStoreButton.setText(Html.fromHtml(getString(R.string.connect_to_my_store)));
            connectToMyStoreButton.setOnClickListener(clickListener);

            Button connectToDemoStoreButton = (Button) view
                    .findViewById(R.id.connectToDemoStoreButton);
            connectToDemoStoreButton.setText(Html
                    .fromHtml(getString(R.string.connect_to_demo_store)));
            connectToDemoStoreButton.setOnClickListener(clickListener);

            view.findViewById(R.id.moreInfoButton).setOnClickListener(clickListener);
            view.findViewById(R.id.storeAdminYesButton).setOnClickListener(clickListener);
            view.findViewById(R.id.storeAdminNoButton).setOnClickListener(clickListener);

            /*
             * Initialize state related views
             */
            mMessageView = view.findViewById(R.id.messageView);
            mMessageListView = (LinearLayout) view.findViewById(R.id.listItems);
            mMessageTextView = (TextView) view.findViewById(R.id.message);
            mMessageTextView.setMovementMethod(LinkMovementMethod.getInstance());

            mConnectToMyStoreIndicator = view.findViewById(R.id.connectToMyStoreIndicator);
            mConnectToDemoStoreIndicator = view.findViewById(R.id.connectToDemoStoreIndicator);
            mMoreInfoIndicator1 = view.findViewById(R.id.moreInfoIndicator1);
            mMoreInfoIndicator2 = view.findViewById(R.id.moreInfoIndicator2);
            mSlidesView = view.findViewById(R.id.slidesView);
            mQuestionView = view.findViewById(R.id.questionView);

            // initialize slides
            List<Slide> slides = new ArrayList<Slide>();
            slides.add(new Slide(R.string.welcome_load_edit_products, R.drawable.welcome_slide_product));
            slides.add(new Slide(R.string.welcome_register_sales, R.drawable.welcome_slide_stock));
            slides.add(new Slide(R.string.welcome_manage_orders, R.drawable.welcome_slide_orders));
            mSlidesPager = (ViewPager) view.findViewById(R.id.slides);
            mSlidesAdapter = new SlidesAdapter(slides);
            mSlidesPager.setAdapter(mSlidesAdapter);

            // view pager indicator initialization
            PageIndicator indicator = (CirclePageIndicator) view.findViewById(R.id.indicator);
            indicator.setViewPager(mSlidesPager);

            // manual slide left/right buttons initialization
            view.findViewById(R.id.slideLeftBtn).setOnClickListener(clickListener);
            view.findViewById(R.id.slideRightBtn).setOnClickListener(clickListener);

            // hack to prevent root scroll view to scroll when view pager is
            // touched
            mSlidesPager.setOnTouchListener(new OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent ev) {
                    switch (ev.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_DOWN:
                            mScroll.requestDisallowInterceptTouchEvent(true);
                            cancelAutomaticSlideChangeIfNecessary();
                            break;
                        case MotionEvent.ACTION_UP:
                            mScroll.requestDisallowInterceptTouchEvent(false);
                            break;
                    }
                    return false;
                }
            });

            // select the default SLIDES state
            setState(State.SLIDES);
        }

        /**
         * Change the active slide to the next one. If end is reached change the
         * slide to the first one
         */
        public void changeSlideToNext() {
            int currentItem = mSlidesPager.getCurrentItem();
            if (currentItem < mSlidesAdapter.getCount() - 1) {
                // if last slide is not yet reached
                mSlidesPager.setCurrentItem(currentItem + 1, true);
            } else {
                // if last slide is reached
                mSlidesPager.setCurrentItem(0, true);
            }
        }

        /**
         * Cancel automatic periodic slide change if there is an active one
         */
        public void cancelAutomaticSlideChangeIfNecessary() {
            if (mChangeSlideRunnable != null) {
                mChangeSlideRunnable.cancel();
                mChangeSlideRunnable = null;
            }
        }

        /**
         * Set the current fragment state. Possible states can be found in
         * {@link State} enumeration. Setting state will adjust visibility of
         * different views with various animation
         * 
         * @param state the state to select
         */
        void setState(final State state) {
            if (mCurrentState == state && state != State.SLIDES) {
                // reset the state to slides if the same state is selected again
                setState(State.SLIDES);
                return;
            }
            Animation fadeOutAnimation = AnimationUtils.loadAnimation(getActivity(),
                    android.R.anim.fade_out);
            // remember current state such as it is used for various checks
            // later in the showNewStateWidgetsRunnable
            final State previousState = mCurrentState;
            final Runnable showNewStateWidgetsRunnable = new Runnable() {
                /**
                 * Flag to prevent from running same actions twice
                 */
                boolean mStarted = false;
                @Override
                public void run() {
                    if (mStarted) {
                        // if was already run before
                        return;
                    }
                    // set the flag that action was already run
                    mStarted = true;
                    mMessageTextView.setVisibility(View.VISIBLE);
                    Animation fadeInAnimation = AnimationUtils.loadAnimation(getActivity(),
                            android.R.anim.fade_in);
                    List<View> containers = new LinkedList<View>();
                    switch (state) {
                        case DEMO_STORE:
                            mMessageTextView.setText(Html.fromHtml(CommonUtils
                                    .getStringResource(R.string.start_free_trial_message)));
                            containers.add(mMessageView);
                            containers.add(mConnectToDemoStoreIndicator);
                            break;
                        case MORE_INFO:
                            mMessageTextView.setText(Html.fromHtml(CommonUtils
                                    .getStringResource(R.string.more_info_message)));
                            containers.add(mMessageView);
                            containers.add(mMoreInfoIndicator1);
                            containers.add(mMoreInfoIndicator2);
                            break;
                        case SLIDES:
                            if (mChangeSlideRunnable == null) {
                                mChangeSlideRunnable = new ChangeSlideRunnable().schedule();
                            }
                            containers.add(mSlidesView);
                            break;
                        case MY_STORE:
                            containers.add(mQuestionView);
                            if (!State.isMyStoreFlowRelatedState(previousState)) {
                                containers.add(mConnectToMyStoreIndicator);
                            }
                            break;
                        case MY_STORE_NO:
                        case MY_STORE_YES:
                            containers.add(mMessageView);
                            mMessageListView.setVisibility(View.VISIBLE);
                            mMessageTextView
                                    .setText(Html.fromHtml(CommonUtils
                                            .getStringResource(state == State.MY_STORE_NO ? R.string.answer_not_store_admin
                                                    : R.string.answer_store_admin)));
                            if(state == State.MY_STORE_NO){
                                mMessageTextView.setVisibility(View.GONE);
                            }
                            mMessageListView.removeAllViews();
                            int listResource = state == State.MY_STORE_YES ? R.array.answer_store_admin_list
                                    : R.array.answer_not_store_admin_list;
                            if (isActivityAlive()) {
                            	// initialize numbered list content
                                String[] items = getActivity().getResources().getStringArray(
                                        listResource);
                                LayoutInflater inflater = LayoutInflater.from(getActivity());
                                int count = 1;
                                for (String item : items) {
                                    View itemView = inflater.inflate(
                                            R.layout.welcome_list_item, mMessageListView, false);
                                    TextView numberView = (TextView) itemView
                                            .findViewById(R.id.number);
                                    numberView.setText(CommonUtils.getStringResource(
                                            R.string.welcome_number_list_pattern, count++));
                                    TextView messageView = (TextView) itemView
                                            .findViewById(R.id.message);
                                    messageView.setText(Html.fromHtml(item));
                                    mMessageListView.addView(itemView);
                                }
                            }
                            if (!State.isMyStoreFlowRelatedState(previousState)) {
                                containers.add(mConnectToMyStoreIndicator);
                            }
                            break;
                        default:
                            break;
                    }
                    for (View container : containers) {
                        container.startAnimation(fadeInAnimation);
                        container.setVisibility(View.VISIBLE);
                    }
                }
            };
            // if state was specified before we need to hide previous state
            // widgets
            if (mCurrentState != null) {
            	// views which should be hidden when animation ends
                final List<View> containersToGone = new LinkedList<View>();
                fadeOutAnimation.setAnimationListener(new AnimationListener() {

                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        // run scheduled operation to show new state
                        // widgets when the hiding widget animation ends
                        showNewStateWidgetsRunnable.run();
                        for (View view : containersToGone) {
                            view.setVisibility(View.GONE);
                        }
                    }
                });
                List<View> containers = new LinkedList<View>();
                switch (mCurrentState) {
                    case DEMO_STORE:
                        containers.add(mMessageView);
                        containers.add(mConnectToDemoStoreIndicator);
                        break;
                    case MORE_INFO:
                        containers.add(mMessageView);
                        containers.add(mMoreInfoIndicator1);
                        containers.add(mMoreInfoIndicator2);
                        break;
                    case SLIDES:
                        containers.add(mSlidesView);
                        cancelAutomaticSlideChangeIfNecessary();
                        break;
                    case MY_STORE:
                        containers.add(mQuestionView);
                        if (!State.isMyStoreFlowRelatedState(state)) {
                            containers.add(mConnectToMyStoreIndicator);
                        }
                        break;
                    case MY_STORE_YES:
                    case MY_STORE_NO:
                        containers.add(mMessageView);
                        containersToGone.add(mMessageListView);
                        if (!State.isMyStoreFlowRelatedState(state)) {
                            containers.add(mConnectToMyStoreIndicator);
                        }
                        break;
                    default:
                        break;
                }
                for (View container : containers) {
                    container.startAnimation(fadeOutAnimation);
                    container.setVisibility(View.INVISIBLE);
                }
            }

            // run widget for the new state showing operation explicitly if
            // previous state is null
            if (mCurrentState == null) {
                showNewStateWidgetsRunnable.run();
            }
            mCurrentState = state;
        }

        @Override
        public void onPause() {
            super.onPause();
            // cancel automatic slides change
            if (mChangeSlideRunnable != null) {
                mChangeSlideRunnable.cancel();
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            // restart automatic slides change if necessary
            if (mChangeSlideRunnable != null) {
                mChangeSlideRunnable.schedule();
            }
        }

        /**
         * Implementation of the automatic slides change controller
         */
        private class ChangeSlideRunnable implements Runnable {

            /**
             * Default slides change interval
             */
            private static final int CHANGE_SLIDE_INTERVAL = 4000;

            @Override
            public void run() {
                changeSlideToNext();
                // schedule next slide change
                mSlidesPager.postDelayed(mChangeSlideRunnable, CHANGE_SLIDE_INTERVAL);
            }

            /**
             * Cancel all scheduled change slide actions
             */
            public void cancel() {
                mSlidesPager.removeCallbacks(this);
            }

            /**
             * Schedule the automatic slide changing
             * 
             * @return current instance of the {@link ChangeSlideRunnable}
             */
            public ChangeSlideRunnable schedule() {
                // first all other scheduled actions should be cancelled
                cancel();
                mSlidesPager.postDelayed(this, CHANGE_SLIDE_INTERVAL);
                return this;
            }
        }

        /**
         * Simple object to hold single slide related resource information
         */
        class Slide {
            /**
             * Reference to the string constant related to the slide
             */
            int textResource;
            /**
             * Reference to the image related to the slide
             */
            int imageResource;

            /**
             * @param textResource Reference to the string constant related to
             *            the slide
             * @param imageResource Reference to the image related to the slide
             */
            public Slide(int textResource, int imageResource) {
                super();
                this.textResource = textResource;
                this.imageResource = imageResource;
            }
        }

        /**
         * {@link ViewPager} adapter for the slides
         */
        class SlidesAdapter extends PagerAdapter {

            /**
             * List of available slides
             */
            List<Slide> mSlides;
            /**
             * Queue to store unused (removed from container) views so it can be
             * reused in future
             */
            Queue<View> mUnusedViews = new LinkedList<View>();

            /**
             * @param slides List of available slides
             */
            public SlidesAdapter(List<Slide> slides) {
                mSlides = slides;
            }

            @Override
            public int getCount() {
                return mSlides.size();
            }


            /**
             * ViewHolder to implement View Holder pattern
             */
            class ViewHolder {
                /**
                 * Related ImageView
                 */
                ImageView image;
                /**
                 * Related TextView
                 */
                TextView text;
            }

            @Override
            public Object instantiateItem(View collection, int position) {
                ViewHolder holder;
                View convertView;
                if (!mUnusedViews.isEmpty()) {
                    // if there are available removed views which may be reused
                    CommonUtils.debug(TAG, "Reusing view from the queue");
                    convertView = mUnusedViews.poll();
                } else {
                    convertView = null;
                }
                if(convertView == null){
                    convertView = LayoutInflater.from(getActivity()).inflate(
                            R.layout.welcome_slide, null);
                    // initialize view holder
                    holder = new ViewHolder();
                    holder.image = (ImageView) convertView.findViewById(R.id.image);
                    holder.text = (TextView) convertView.findViewById(R.id.description);
                    convertView.setTag(holder);
                } else {
                    // retrieve view holder
                    holder = (ViewHolder) convertView.getTag();
                }
                Slide item = mSlides.get(position);
                holder.image.setImageResource(item.imageResource);
                holder.text.setText(item.textResource);

                ((ViewPager) collection).addView(convertView, 0);

                return convertView;
            }

            @Override
            public void destroyItem(View collection, int position, Object view) {
                View theView = (View) view;
                ((ViewPager) collection).removeView(theView);
                // offer the removed view for future reuse
                mUnusedViews.offer(theView);
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == ((View) object);
            }

        }
    }

}
