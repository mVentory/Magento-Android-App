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

import com.mageventory.util.concurent.AsyncTaskEx;

/**
 * The simple async task with the loading control
 * 
 * @author Eugene Popovich
 */
public abstract class SimpleAsyncTask extends AsyncTaskEx<Void, Void, Boolean> {
    private final LoadingControl loadingControl;
    /**
     * The flag to determine whether the task is already finished
     */
    boolean mFinished = false;

    public SimpleAsyncTask(
            LoadingControl loadingControl) {
        this.loadingControl = loadingControl;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        startLoading();
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        stopLoading();

    }

    public void startLoading() {
        if (loadingControl != null)
        {
            loadingControl.startLoading();
        }
    }

    public void stopLoading() {
        if (loadingControl != null)
        {
            loadingControl.stopLoading();
        }
        mFinished = true;
    }

    @Override
    protected final void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        try {
            if (result.booleanValue()) {
                onSuccessPostExecute();
            } else {
                onFailedPostExecute();
            }
        } finally {
            stopLoading();
        }
    }

    protected abstract void onSuccessPostExecute();

    protected void onFailedPostExecute() {
    }

    public LoadingControl getLoadingControl() {
        return loadingControl;
    }

    /**
     * Is the task already finished
     * 
     * @return true if task was finished either canceled or correctly done
     */
    public boolean isFinished() {
        return mFinished;
    }
}
