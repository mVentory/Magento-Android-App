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

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;

import com.mageventory.bitmapfun.util.ImageWorker;

/**
 * Base dialog fragment containing various image worker management utils
 * 
 * @author Eugene Popovich
 */
public abstract class BaseDialogFragmentWithImageWorker extends BaseDialogFragment {

    /**
     * Pre-defined one imageWorker reference
     */
    protected ImageWorker imageWorker;
    /**
     * List of image workers which should be managed
     */
    protected List<ImageWorker> imageWorkers = new ArrayList<ImageWorker>();

    protected abstract void initImageWorker();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initImageWorker();
        imageWorkers.add(imageWorker);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        clearImageWorkerCaches(true);
    }

    /**
     * Clear the image worker caches
     * 
     * @param memoryOnly whether the only RAM cache should be cleared
     */
    public void clearImageWorkerCaches(boolean memoryOnly) {
        for (ImageWorker mImageWorker : imageWorkers)
        {
            if (mImageWorker != null && mImageWorker.getImageCache() != null)
            {
                mImageWorker.getImageCache().clearCaches(memoryOnly);
            }
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        setImageWorkerExitTaskEarly(false);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        setImageWorkerExitTaskEarly(true);
    }

    /**
     * Set whether the all active loading actions should be cancelled and no
     * other loading operations should be performed
     * 
     * @param exitTaskEarly
     */
    public void setImageWorkerExitTaskEarly(boolean exitTaskEarly) {
        for (ImageWorker mImageWorker : imageWorkers)
        {
            if (mImageWorker != null)
            {
                mImageWorker.setExitTasksEarly(exitTaskEarly);
            }
        }
    }
}
