package com.mageventory.util;

import java.util.Collection;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;

import com.mageventory.MyApplication;

/**
 * Contains various utility methods to work with animations
 * 
 * @author Eugene Popovich
 */
public class CommonAnimationUtils {
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
     * Convert collection of views to array
     * 
     * @param views the collection of views to convert
     * @return array of views
     */
    public static View[] toArray(Collection<View> views) {
        return views.toArray(new View[views.size()]);
    }
}
