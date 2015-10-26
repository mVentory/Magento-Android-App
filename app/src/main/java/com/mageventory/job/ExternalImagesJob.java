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

package com.mageventory.job;

public class ExternalImagesJob {

    public long mTimestamp;
    /*
     * The product code can be either SKU or a barcode. We do need an SKU to
     * upload an image.
     */
    public String mProductCode;
    public String mSKU;
    public long mProfileID;
    public int mAttemptsCount;

    public ExternalImagesJob(long timestamp, String productCode, String sku, long profileID,
            int attemptsCount)
    {
        mTimestamp = timestamp;
        mProductCode = productCode;
        mSKU = sku;
        mProfileID = profileID;
        mAttemptsCount = attemptsCount;
    }

    @Override
    public String toString() {
        return "" + mTimestamp + ", " + mProductCode + ", " + mSKU + ", " + mProfileID + ", "
                + mAttemptsCount;
    }
}
