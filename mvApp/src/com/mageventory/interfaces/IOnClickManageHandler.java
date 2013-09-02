
package com.mageventory.interfaces;

import com.mageventory.components.ImagePreviewLayout;

/**
 * Interface for handling image operations inside
 * <code>ProductDetailsActivity</code>
 * 
 * @author Bogdan Petran
 */
public interface IOnClickManageHandler {

    /**
     * Callback method, called when the delete button was clicked
     * 
     * @param layoutToRemove will be the <code>ImagePreviewLayout</code> which
     *            will contain an image and a delete button
     * @see ImagePreviewLayout
     */
    public void onDelete(ImagePreviewLayout layoutToRemove);

    /**
     * Callback method, called when the image was clicked on
     * 
     * @param layoutToEdit will be the <code>ImagePreviewLayout</code> which
     *            will contain an image and a delete button
     * @see ImagePreviewLayout
     */
    public void onClickForEdit(ImagePreviewLayout layoutToEdit);

    /**
     * Callback method, called when the checkbox was clicked on
     * 
     * @param layoutToEdit will be the <code>ImagePreviewLayout</code> which
     *            will contain an image, a delete button and a checkbox
     * @see ImagePreviewLayout
     */
    public void onClickForMainImage(ImagePreviewLayout layoutToEdit);
}
