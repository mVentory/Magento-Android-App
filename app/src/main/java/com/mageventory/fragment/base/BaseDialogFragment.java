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

package com.mageventory.fragment.base;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mageventory.activity.base.BaseFragmentActivity;
import com.mageventory.activity.base.BaseFragmentActivity.BroadcastManager;
import com.mageventory.util.CommonUtils;
import com.mageventory.util.EventBusUtils.BroadcastReceiverRegisterHandler;
import com.mageventory.util.GuiUtils;
import com.mageventory.util.TrackerUtils;

/**
 * Base dialog fragment
 * 
 * @author Eugene Popovich
 */
public class BaseDialogFragment extends DialogFragment implements BroadcastReceiverRegisterHandler {
    static final String TAG = BaseDialogFragment.class.getSimpleName();
    static final String CATEGORY = "Dialog Fragment Lifecycle";

    private BroadcastManager mBroadcastManager = new BroadcastManager();

    protected void trackLifecycleEvent(String event) {
        CommonUtils.debug(TAG, event + ": " + getClass().getSimpleName());
        TrackerUtils.trackEvent(CATEGORY, event, getClass().getSimpleName());
    }

    public BaseDialogFragment() {
        trackLifecycleEvent("Constructor");
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        trackLifecycleEvent("onCancel");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        trackLifecycleEvent("onCreateDialog");
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        trackLifecycleEvent("onDismiss");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        trackLifecycleEvent("onAttach");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        trackLifecycleEvent("onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        trackLifecycleEvent("onCreateView");
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        trackLifecycleEvent("onDetach");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        trackLifecycleEvent("onDestroy");
        mBroadcastManager.onDestroy();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        trackLifecycleEvent("onActivityCreated");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        trackLifecycleEvent("onDestroyView");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        trackLifecycleEvent("onSaveInstanceState");
    }

    @Override
    public void onResume() {
        super.onResume();
        trackLifecycleEvent("onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        trackLifecycleEvent("onPause");
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        trackLifecycleEvent("onViewCreated");
    }

    @Override
    public void onStart() {
        super.onStart();
        trackLifecycleEvent("onStart");
        TrackerUtils.trackView(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        trackLifecycleEvent("onStop");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        trackLifecycleEvent("onConfigurationChanged");
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        trackLifecycleEvent("onActivityResult");
    }

    protected void hideKeyboard() {
        GuiUtils.hideKeyboard(getView());
    }

    /**
     * Close the dialog if showing
     */
    protected void closeDialog() {
        Dialog dialog = BaseDialogFragment.this.getDialog();
        if (dialog != null && dialog.isShowing()) {
            hideKeyboard();
            BaseDialogFragment.this.dismissAllowingStateLoss();
        }
    }

    /**
     * @return true if there is attached activity and it is alive, false
     *         otherwise
     */
    public boolean isActivityAlive() {
        return getActivity() != null && ((BaseFragmentActivity) getActivity()).isActivityAlive();
    }

    @Override
    public void addRegisteredLocalReceiver(BroadcastReceiver receiver) {
        mBroadcastManager.addRegisteredLocalReceiver(receiver);
    }
}
