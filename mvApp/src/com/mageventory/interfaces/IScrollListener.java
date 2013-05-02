package com.mageventory.interfaces;

/**
 * @author Bogdan Petran
 */
public interface IScrollListener {

	/**
	 * Called when the scroll reached the bottom of the <code>ScrollView</code>
	 */
	public void scrolledToBottom();

	/**
	 * Called when the scroll moved but did not reached the bottom of the
	 * <code>ScrollView</code>
	 */
	public void scrollMoved();
}
