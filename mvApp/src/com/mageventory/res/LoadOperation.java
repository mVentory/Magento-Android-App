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

package com.mageventory.res;

import java.io.Serializable;

import android.os.Bundle;

public class LoadOperation implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private Exception exception;
    private Bundle extras;
    private int operationRequestId;
    private String[] resourceParams;
    private int resourceType;

    public LoadOperation(int operationRequestId, int resourceType, String[] resourceParams) {
        super();
        this.operationRequestId = operationRequestId;
        this.resourceType = resourceType;
        this.resourceParams = resourceParams;
    }

    public Exception getException() {
        return exception;
    }

    public Bundle getExtras() {
        return extras;
    }

    public int getOperationRequestId() {
        return operationRequestId;
    }

    public String[] getResourceParams() {
        return resourceParams;
    }

    public int getResourceType() {
        return resourceType;
    }

    public void setException(final Exception e) {
        exception = e;
    }

    public void setExtras(final Bundle extras) {
        this.extras = extras;
    }

}
