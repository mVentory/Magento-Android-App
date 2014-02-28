
package com.mageventory.util;

import com.mageventory.util.concurent.AsyncTaskEx;

/**
 * The simple async task with the loading control
 * 
 * @author Eugene Popovich
 */
public abstract class SimpleAsyncTask extends AsyncTaskEx<Void, Void, Boolean> {
    private final LoadingControl loadingControl;

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
    }

    @Override
    protected final void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        stopLoading();
        if (result.booleanValue())
        {
            onSuccessPostExecute();
        } else
        {
            onFailedPostExecute();
        }
    }

    protected abstract void onSuccessPostExecute();

    protected void onFailedPostExecute() {
    }

    public LoadingControl getLoadingControl() {
        return loadingControl;
    }
}
