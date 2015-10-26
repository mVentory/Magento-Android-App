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

package com.mageventory.util;

import java.lang.reflect.Constructor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.Surface;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.mageventory.MyApplication;
import com.mventory.R;

/**
 * Contains various gui utils methods
 * 
 * @author Eugene Popovich
 */
public class GuiUtils {
    static final String TAG = GuiUtils.class.getSimpleName();
    static Thread mUiThread;
    static Handler mHandler;

    /**
     * Setup application
     */
    public static void setup() {
        mHandler = new Handler();
        mUiThread = Thread.currentThread();
    }

    /**
     * Post action to handler
     * 
     * @param action
     */
    public static final void post(Runnable action)
    {
        CommonUtils.debug(TAG, "post");
        mHandler.post(action);
    }

    /**
     * Post delayed action to handler
     * 
     * @param action
     * @param delayMillis
     */
    public static final void postDelayed(Runnable action, long delayMillis)
    {
        CommonUtils.debug(TAG, "post");
        mHandler.postDelayed(action, delayMillis);
    }

    /**
     * Run action in UI thread
     * 
     * @param action
     */
    public static final void runOnUiThread(Runnable action) {
        if (mHandler == null || mUiThread == null) {
            throw new IllegalStateException(
                    "GuiUtils is not configured. Did you forget to call GuiUtils.setup()?");
        }
        if (Thread.currentThread() != mUiThread) {
            CommonUtils.debug(TAG, "runOnUiThread: thread is not ui, posting action");
            mHandler.post(action);
        } else {
            CommonUtils.debug(TAG, "runOnUiThread: thread is ui, running action");
            action.run();
        }
    }

    /**
     * Alert message to user by id
     * 
     * @param messageId
     */
    public static void alert(int messageId) {
        alert(CommonUtils.getStringResource(messageId));
    }

    /**
     * Alert message to user by id with parameters
     * 
     * @param messageId
     * @param args
     */
    public static void alert(int messageId, Object... args) {
        alert(CommonUtils.getStringResource(messageId, args));
    }

    /**
     * Alert message to user
     * 
     * @param msg
     */
    public static void alert(final String msg) {
        alert(msg, null);
    }

    /**
     * Alert message to user
     * 
     * @param msg
     * @param context
     */
    public static void alert(final String msg, final Context context) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                LayoutInflater inflater = (LayoutInflater) MyApplication.getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.toast_notification, null);

                TextView text = (TextView) layout.findViewById(android.R.id.message);
                text.setText(msg);

                Toast toast = new Toast(MyApplication.getContext());
                toast.setView(layout);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.show();
            }
        };
        runOnUiThread(runnable);
    }

    /**
     * Show info message to user by message id
     * 
     * @param messagId
     */
    public static void info(int messagId)
    {
        info(CommonUtils.getStringResource(messagId));
    }

    /**
     * Show info message to user
     * 
     * @param msg
     */
    public static void info(final String msg)
    {
        info(msg, null);
    }

    /**
     * Show info message to user
     * 
     * @param msg
     * @param context
     */
    public static void info(final String msg, final Context context)
    {
        alert(msg, context);
    }

    /**
     * Process error and show error message to user
     * 
     * @param TAG
     * @param messageId
     * @param ex
     */
    public static void error(String TAG, int messageId, Throwable ex) {
        error(TAG, CommonUtils.getStringResource(messageId), ex);
    }

    /**
     * Process error and show error message to user
     * 
     * @param TAG
     * @param ex
     */
    public static void error(String TAG, Throwable ex) {
        error(TAG, null, ex, null);
    }

    /**
     * Process error and show error message to user
     * 
     * @param TAG
     * @param message
     * @param ex
     */
    public static void error(String TAG, String message, Throwable ex) {
        error(TAG, message, ex, null);
    }

    /**
     * Process error and show error message to user
     * 
     * @param TAG
     * @param messageId
     * @param ex
     * @param context
     */
    public static void error(String TAG, int messageId, Throwable ex, Context context) {
        error(TAG, CommonUtils.getStringResource(messageId), ex, context);
    }

    /**
     * Process error and show error message to user
     * 
     * @param TAG
     * @param message
     * @param ex
     * @param context
     */
    public static void error(String TAG, String message, Throwable ex, Context context) {
        processError(TAG, message, ex, context, true);
    }

    /**
     * Process error but don't show alert to user
     * 
     * @param TAG
     * @param ex
     */
    public static void noAlertError(String TAG, Throwable ex) {
        noAlertError(TAG, null, ex);
    }

    /**
     * Process error but don't show alert to user
     * 
     * @param TAG
     * @param message
     * @param ex
     */
    public static void noAlertError(String TAG, String message, Throwable ex) {
        processError(TAG, message, ex, null, false);
    }

    /**
     * Process error
     * 
     * @param TAG
     * @param messageId
     * @param ex
     * @param context
     * @param alertMessage
     */
    public static void processError(String TAG, int messageId, Throwable ex, Context context,
            boolean alertMessage) {
        processError(TAG, CommonUtils.getStringResource(messageId), ex, context, alertMessage);
    }

    /**
     * Process error
     * 
     * @param TAG
     * @param message
     * @param ex
     * @param context
     * @param alertMessage
     */
    public static void processError(String TAG, String message, Throwable ex, Context context,
            boolean alertMessage) {
        CommonUtils.error(TAG, message, ex);
        if (alertMessage) {
            alert(message == null ? ex.getLocalizedMessage() : message, context);
        }
    }

    /**
     * Remove the OnGlobalLayoutListener from the view
     * 
     * @param view
     * @param listener
     */
    @SuppressWarnings("deprecation")
    public static void removeGlobalOnLayoutListener(View view,
            ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (view != null) {
            ViewTreeObserver observer = view.getViewTreeObserver();
            if (null != observer && observer.isAlive()) {
                CommonUtils
                        .debug(TAG,
                                "removeGlobalOnLayoutListener: Removing global on layout listener from view...");
                if (CommonUtils.isJellyBeanOrHigher()) {
                    removeOnGlobalLayoutListenerJB(listener, observer);
                } else {
                    observer.removeGlobalOnLayoutListener(listener);
                }
            } else {
                CommonUtils.debug(TAG,
                        "removeGlobalOnLayoutListener: observer is null or not alive");
            }
        } else {
            CommonUtils.debug(TAG, "removeGlobalOnLayoutListener: view is null");
        }
    }

    @TargetApi(16)
    public static void removeOnGlobalLayoutListenerJB(
            ViewTreeObserver.OnGlobalLayoutListener listener, ViewTreeObserver observer) {
        observer.removeOnGlobalLayoutListener(listener);
    }

    /**
     * Hide keyboard from the view for focused widget
     * 
     * @param view
     */
    public static void hideKeyboard(View view) {
        try {
            View target = view.findFocus();
            if (target != null) {
                InputMethodManager imm = (InputMethodManager) MyApplication.getContext()
                        .getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(target.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }
        } catch (Exception ex) {
            GuiUtils.noAlertError(TAG, ex);
        }
    }

    /**
     * Request the keyboard showing for the specified view. Performed in post
     * delayed action
     * 
     * @param v
     */
    public static void showKeyboardDelayed(final View v) {
        v.postDelayed(new Runnable() {

            @Override
            public void run() {
                InputMethodManager keyboard = (InputMethodManager) MyApplication.getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);

                keyboard.showSoftInput(v, 0);
            }
        }, 200);
    }

    /**
     * Create new Menu object instance
     * 
     * @param context
     * @return
     */
    public static Menu newMenuInstance(Context context) {
        try {
            Class<?> menuBuilderClass = Class.forName("com.android.internal.view.menu.MenuBuilder");

            Constructor<?> constructor = menuBuilderClass.getDeclaredConstructor(Context.class);

            return (Menu) constructor.newInstance(context);

        } catch (Exception e) {
            CommonUtils.error(TAG, null, e);
        }

        return null;
    }

    /**
     * Show the message dialog with OK button
     * 
     * @param title
     * @param message
     * @param activity
     */
    public static void showMessageDialog(Integer title, Integer message, Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        if (title != null) {
            builder.setTitle(title);
        }
        if (message != null) {
            builder.setMessage(message);
        }
        builder.setPositiveButton(R.string.ok, null);
        builder.show();
    }

    /**
     * Show the message dialog with OK button
     * 
     * @param title
     * @param message
     * @param activity
     */
    public static void showMessageDialog(String title, String message, Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        if (title != null) {
            builder.setTitle(title);
        }
        if (message != null) {
            builder.setMessage(message);
        }
        builder.setPositiveButton(R.string.ok, null);
        builder.show();
    }

    /**
     * Validate basic text data (whether null or empty) and show appropriate
     * "please specify first" message if it is invalid
     * 
     * @param values
     * @param titles array of string resource codes
     * @return false if at least one field is invalid, otherwise return true
     */
    public static boolean validateBasicTextData(String[] values, int[] titles) {
        return validateBasicTextData(R.string.pleaseSpecifyFirst, values, titles);
    }

    /**
     * Validate basic text data (whether null or empty) and show appropriate
     * "please specify first" message if it is invalid
     * 
     * @param pleaseSpecifyStringResource reference to the string constant which
     *            will be shown for missing field
     * @param values
     * @param titles array of string resource codes
     * @return false if at least one field is invalid, otherwise return true
     */
    public static boolean validateBasicTextData(int pleaseSpecifyStringResource, String[] values,
            int[] titles) {
        return validateBasicTextData(pleaseSpecifyStringResource, values, titles, null, false);
    }

    /**
     * Validate basic text data (whether null or empty) and show appropriate
     * "please specify first" message if it is invalid
     * 
     * @param pleaseSpecifyStringResource reference to the string constant which
     *            will be shown for missing field
     * @param titles array of string resource codes
     * @param views to focus in case of invalid value
     * @param silent whether to show alert messages or no
     * @return false if at least one field is invalid, otherwise return true
     */
    public static <T extends TextView> boolean validateBasicTextData(
            int pleaseSpecifyStringResource, int[] titles, T[] views, boolean silent) {
        String values[] = new String[views.length];
        for (int i = 0; i < views.length; i++) {
            values[i] = views[i].getText().toString();
        }
        return validateBasicTextData(pleaseSpecifyStringResource, values, titles, views, silent);
    }

    /**
     * Validate basic text data (whether null or empty) and show appropriate
     * "please specify first" message if it is invalid
     * 
     * @param pleaseSpecifyStringResource reference to the string constant which
     *            will be shown for missing field
     * @param values the values to validate
     * @param titles array of string resource codes
     * @param views to focus in case of invalid value
     * @param silent whether to show alert messages and activate fields or no
     * @return false if at least one field is invalid, otherwise return true
     */
    public static <T extends View> boolean validateBasicTextData(int pleaseSpecifyStringResource,
            String[] values, int[] titles, T[] views, boolean silent) {
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            if (TextUtils.isEmpty(value)) {
                if (!silent) {
                    String pleaseSpecifyFirst = CommonUtils.getStringResource(
                            pleaseSpecifyStringResource, CommonUtils.getStringResource(titles[i]));
                    info(pleaseSpecifyFirst);
                    if (views != null) {
                        View view = views[i];
                        activateField(view, true, true, true);
                    }
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Validate basic text data (whether null or empty) and show appropriate
     * "please specify first" message if it is invalid
     * 
     * @param pleaseSpecifyStringResource reference to the string constant which
     *            will be shown for missing field
     * @param values the values to validate
     * @param titles array of titles strings
     * @param views to focus in case of invalid value
     * @param silent whether to show alert messages and activate fields or no
     * @return false if at least one field is invalid, otherwise return true
     */
    public static <T extends View> boolean validateBasicTextData(int pleaseSpecifyStringResource,
            String[] values, String[] titles, T[] views, boolean silent) {
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            if (TextUtils.isEmpty(value)) {
                if (!silent) {
                    String pleaseSpecifyFirst = CommonUtils.getStringResource(
                            pleaseSpecifyStringResource, titles[i]);
                    info(pleaseSpecifyFirst);
                    if (views != null) {
                        View view = views[i];
                        activateField(view, true, true, true);
                    }
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Activate the field on the screen
     * 
     * @param view
     * @param requestFocus whether to request focus
     * @param scrollTo whether to scroll to
     * @param showKeyboard
     */
    public static void activateField(View view, boolean requestFocus, boolean scrollTo,
            boolean showKeyboard) {
        if (view == null) {
            // if passed view is null
            return;
        }
        if (requestFocus) {
            view.requestFocus();
        }
        if (scrollTo) {
            Rect rect = new Rect(0, 0, view.getWidth(), view.getHeight());
            view.requestRectangleOnScreen(rect);
        }
        if (showKeyboard) {
            GuiUtils.showKeyboardDelayed(view);
        }
    }

    /**
     * Lock the activity orientation so next time it will be visible the same
     * orientation should be used. Will not work in case user
     * rotated device.
     * <br>Taken from http://stackoverflow.com/a/16599549/527759
     * 
     * @param activity the activity the orientation should be locked at
     */
    public static void lockOrientation(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        int rotation = display.getRotation();
        int height;
        int width;
        Point size = new Point();
        display.getSize(size);
        height = size.y;
        width = size.x;
        switch (rotation) {
            case Surface.ROTATION_90:
                if (width > height)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                else
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT/* reversePortait */);
                break;
            case Surface.ROTATION_180:
                if (height > width)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT/* reversePortait */);
                else
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE/* reverseLandscape */);
                break;
            case Surface.ROTATION_270:
                if (width > height)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE/* reverseLandscape */);
                else
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            default:
                if (height > width)
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                else
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }
}
