package com.mageventory.util;

import java.util.Collection;

import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;

import com.mageventory.MyApplication;

/**
 * Contains various utility methods to work with animations
 * 
 * @author Eugene Popovich
 */
public class CommonAnimationUtils {
    /**
     * Default duration for the animation in milliseconds
     */
    static final int DEFAULT_ANIMATION_DURATION = 500;
    /**
     * Hide views with the fade-out animation
     * 
     * @param runOnAnimationEnd the runnable to run when animation ends. Can be
     *            null
     * @param views the views to hide with the fade-out animation
     */
    public static void fadeOut(final Runnable runOnAnimationEnd, Collection<View> views) {
        fadeOut(runOnAnimationEnd, toArray(views));
    }

    /**
     * Hide views with the fade-out animation
     * 
     * @param runOnAnimationEnd the runnable to run when animation ends. Can be
     *            null
     * @param views the views to hide with the fade-out animation
     */
    public static void fadeOut(final Runnable runOnAnimationEnd, View... views) {
        Animation fadeOutAnimation = AnimationUtils.loadAnimation(MyApplication.getContext(),
                android.R.anim.fade_out);
        if (runOnAnimationEnd != null) {
            // if some actions should be run when animation ends
            fadeOutAnimation.setAnimationListener(new AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    runOnAnimationEnd.run();
                }
            });
        }
        for (View view : views) {
            view.startAnimation(fadeOutAnimation);
            view.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Show views with the fade-in animation
     * 
     * @param runOnAnimationEnd the runnable to run when animation ends. Can be
     *            null
     * @param views the views to show with the fade-in animation
     */
    public static void fadeIn(final Runnable runOnAnimationEnd, Collection<View> views) {
        fadeIn(runOnAnimationEnd, toArray(views));
    }

    /**
     * Show views with the fade-in animation
     * 
     * @param runOnAnimationEnd the runnable to run when animation ends. Can be
     *            null
     * @param views the views to show with the fade-in animation
     */
    public static void fadeIn(final Runnable runOnAnimationEnd, View... views) {
        Animation fadeInAnimation = AnimationUtils.loadAnimation(MyApplication.getContext(),
                android.R.anim.fade_in);
        if (runOnAnimationEnd != null) {
            // if some actions should be run when animation ends
            fadeInAnimation.setAnimationListener(new AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    runOnAnimationEnd.run();
                }
            });
        }
        for (View view : views) {
            view.startAnimation(fadeInAnimation);
            view.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Show the view with the expand animation
     * 
     * @param runOnAnimationEnd the action to run after the animation end
     * @param view the view to expand
     */
    public static void expand(final Runnable runOnAnimationEnd, View view) {
        Animation animation = getExpandAnimation(view);
        if (runOnAnimationEnd != null) {
            // if some actions should be run when animation ends
            animation.setAnimationListener(new AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    runOnAnimationEnd.run();
                }
            });
        }
        view.startAnimation(animation);
    }

    /**
     * Hide the view with the collapse animation
     * 
     * @param runOnAnimationEnd the action to run after the animation end
     * @param view the view to collapse
     */
    public static void collapse(final Runnable runOnAnimationEnd, View view) {
        Animation animation = getCollapseAnimation(view);
        if (runOnAnimationEnd != null) {
            // if some actions should be run when animation ends
            animation.setAnimationListener(new AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    runOnAnimationEnd.run();
                }
            });
        }
        view.startAnimation(animation);
    }

    /**
     * Get the expand view animation
     * <p>
     * taken from http://stackoverflow.com/a/13381228/527759
     * 
     * @param v the view to expand
     * @return
     */
    public static Animation getExpandAnimation(final View v) {
        v.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();

        v.getLayoutParams().height = 0;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1 ? LayoutParams.WRAP_CONTENT
                        : (int) (targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration(DEFAULT_ANIMATION_DURATION);
        return a;
    }

    /**
     * Get the collapse view animation
     * <p>
     * taken from http://stackoverflow.com/a/13381228/527759
     * 
     * @param v the view to collapse
     * @return
     */
    public static Animation getCollapseAnimation(final View v) {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                } else {
                    v.getLayoutParams().height = initialHeight
                            - (int) (initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration(DEFAULT_ANIMATION_DURATION);
        return a;
    }
    /**
     * Convert collection of views to array
     * 
     * @param views the collection of views to convert
     * @return array of views
     */
    public static View[] toArray(Collection<View> views) {
        return views.toArray(new View[views.size()]);
    }
}
