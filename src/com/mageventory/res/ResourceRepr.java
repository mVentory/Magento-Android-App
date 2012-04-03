package com.mageventory.res;

import java.io.Serializable;

public class ResourceRepr implements ResourceConstants, Serializable {

	private static final long serialVersionUID = 1L;

	// public final String filepath;
	public final int state;
	public final long timestamp;
	public final boolean transaction;
	public final String resourceUri;
	public final boolean old;

	public ResourceRepr(int state, long timestamp, boolean transaction, String resourceUri, boolean old) {
		super();
		// this.filepath = filepath;
		this.state = state;
		this.timestamp = timestamp;
		this.transaction = transaction;
		this.resourceUri = resourceUri;
		this.old = old;
	}

	@Override
	public String toString() {
		return "MageventoryResource [state=" + state + ", timestamp=" + timestamp + ", transaction=" + transaction
				+ ", resourceUri=" + resourceUri + "]";
	}

	public boolean isAvailable() {
		return state == STATE_AVAILABLE;
	}

}
