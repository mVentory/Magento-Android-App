package com.mageventory.components;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

import com.mageventory.interfaces.IScrollListener;

/**
 * Used to listen for scroll events and notify
 * <code>IScrollToBottomListener</code> that the bottom of the view has been
 * reached
 * 
 * @author Bogdan Petran
 */
public class ProductDetailsScrollView extends ScrollView {

	private IScrollListener scrollToBottomListener;
	int screenHeight;

	public ProductDetailsScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		super.onScrollChanged(l, t, oldl, oldt);

		if (t + getHeight() >= getChildAt(0).getHeight()
				&& scrollToBottomListener != null) {
			scrollToBottomListener.scrolledToBottom(); // when scroll reaced
														// bottom, notify the
														// listener
		} else if (oldt + getHeight() >= getChildAt(0).getHeight()) {
			scrollToBottomListener.scrollMoved(); // don't call this method all
													// the time but only when
													// the scroll moved from the
													// bottom
		}
	}

	public IScrollListener getScrollToBottomListener() {
		return scrollToBottomListener;
	}

	public void setScrollToBottomListener(IScrollListener scrollToBottomListener) {
		this.scrollToBottomListener = scrollToBottomListener;
	}
}
