package com.mageventory.client;

public class RetryAfterLoginException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RetryAfterLoginException() {
		super();
	}

	public RetryAfterLoginException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public RetryAfterLoginException(String detailMessage) {
		super(detailMessage);
	}

	public RetryAfterLoginException(Throwable throwable) {
		super(throwable);
	}

}
