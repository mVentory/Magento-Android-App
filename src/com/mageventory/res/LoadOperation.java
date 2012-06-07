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

	public LoadOperation(int operationRequestId, int resourceType,
			String[] resourceParams) {
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
