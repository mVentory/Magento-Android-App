package com.mageventory.widget;

import java.util.LinkedList;
import java.util.List;

import android.view.View;
import android.view.View.OnClickListener;

import com.mageventory.util.CommonAnimationUtils;

/**
 * The controller for the expandable view with expand collapse animation
 * 
 * @author Eugene Popovich
 */
public class ExpandingViewController {
    /**
     * An enum describing possible controller states
     */
    public enum State {
        /**
         * The state for the expanded view
         */
        EXPANDED,
        /**
         * The state for the collapsed view
         */
        COLLAPSED,
    }

    /**
     * The controller view which adjust collapsed/expanded state on click
     */
    View mExpandCollapseController;
    /**
     * The view which can be expanded/collapsed
     */
    View mExpandingView;
    /**
     * The expanded indicator view
     */
    View mExpandedIndicator;
    /**
     * The collapsed indicator view
     */
    View mCollapsedIndicator;
    /**
     * The current controller state
     */
    State mCurrentState;

    /**
     * Set the current controller state. Possible states can be found in
     * {@link State} enumeration. Setting state will adjust visibility of
     * different views with various animation
     * 
     * @param state the state to select
     */
    void setState(final State state, final boolean animate) {
        if (mCurrentState == state) {
            return;
        }
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
                List<View> containers = new LinkedList<View>();
                switch (state) {
                    case EXPANDED:
                        if (animate) {
                            CommonAnimationUtils.fadeIn(null, mExpandedIndicator);
                        } else {
                            containers.add(mExpandedIndicator);
                        }
                        if (animate) {
                            CommonAnimationUtils.fadeIn(null, mExpandedIndicator);
                        } else {
                            containers.add(mExpandedIndicator);
                        }
                        break;
                    case COLLAPSED:
                        if (animate) {
                            CommonAnimationUtils.fadeIn(null, mCollapsedIndicator);
                        } else {
                            containers.add(mCollapsedIndicator);
                        }
                        break;
                    default:
                        break;
                }
                for (View view : containers) {
                    view.setVisibility(View.VISIBLE);
                }
            }
        };
        // if state was specified before we need to hide previous state
        // widgets
        if (mCurrentState != null) {
            // views which should be hidden when animation ends
            List<View> containers = new LinkedList<View>();
            switch (mCurrentState) {
                case EXPANDED:
                    containers.add(mExpandedIndicator);
                    if (animate) {
                        CommonAnimationUtils.collapse(null, mExpandingView);
                    } else {
                        mExpandingView.setVisibility(View.GONE);
                    }
                    break;
                case COLLAPSED:
                    containers.add(mCollapsedIndicator);
                    if (animate) {
                        CommonAnimationUtils.expand(null, mExpandingView);
                    } else {
                        mExpandingView.setVisibility(View.VISIBLE);
                    }
                    break;
                default:
                    break;
            }
            Runnable runAfterAnimationEnd = new Runnable() {

                @Override
                public void run() {
                    if (mCurrentState != state) {
                        // if state was changed again during animation run
                        return;
                    }
                    // run scheduled operation to show new state
                    // widgets when the hiding widget animation ends
                    showNewStateWidgetsRunnable.run();
                }
            };
            if (animate) {
                CommonAnimationUtils.fadeOut(runAfterAnimationEnd, containers);
            } else {
                for (View view : containers) {
                    view.setVisibility(View.INVISIBLE);
                }
                runAfterAnimationEnd.run();
            }
        } else {
            // reset visibility of containers to fix possible invalid
            // appearance after the activity state restore
            List<View> containersToGone = new LinkedList<View>();
            List<View> containers = new LinkedList<View>();
            containersToGone.add(mExpandingView);
            containers.add(mExpandedIndicator);
            containers.add(mCollapsedIndicator);
            for (View view : containersToGone) {
                view.setVisibility(View.GONE);
            }
            for (View view : containers) {
                view.setVisibility(View.INVISIBLE);
            }
            // run widget for the new state showing operation explicitly if
            // previous state is null
            showNewStateWidgetsRunnable.run();
        }

        mCurrentState = state;
    }

    /**
     * @param rootView the root view which contains all the reqruied components
     * @param expandCollapseControllerId an id of the controller view which
     *            adjust collapsed/expanded state on click
     * @param expandingViewId an id of the view which can be expanded/collapsed
     * @param expandedIndicatorId an id of the expanded indicator view
     * @param collapsedIndicatorId an id of the collapsed indicator view
     */
    public ExpandingViewController(View rootView, int expandCollapseControllerId,
            int expandingViewId, int expandedIndicatorId, int collapsedIndicatorId) {
        this(rootView, expandCollapseControllerId, expandingViewId, expandedIndicatorId, collapsedIndicatorId, State.COLLAPSED);
    }
    public ExpandingViewController(View rootView, int expandCollapseControllerId,
            int expandingViewId, int expandedIndicatorId, int collapsedIndicatorId, State state) {
        this(rootView.findViewById(expandCollapseControllerId), rootView
                .findViewById(expandingViewId), rootView.findViewById(expandedIndicatorId),
                rootView.findViewById(collapsedIndicatorId), state);
    }

    /**
     * @param expandCollapseController The controller view which adjust
     *            collapsed/expanded state on click
     * @param expandingView The view which can be expanded/collapsed
     * @param expandedIndicatorThe expanded indicator view
     * @param collapsedIndicator The collapsed indicator view
     * @param state The current controller state
     */
    public ExpandingViewController(View expandCollapseController, View expandingView,
            View expandedIndicator, View collapsedIndicator, State state) {
        super();
        mExpandCollapseController = expandCollapseController;
        mExpandingView = expandingView;
        mExpandedIndicator = expandedIndicator;
        mCollapsedIndicator = collapsedIndicator;
        setState(state, false);
        mExpandCollapseController.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                setState(mCurrentState == State.EXPANDED ? State.COLLAPSED : State.EXPANDED, true);
            }
        });
    }

}
