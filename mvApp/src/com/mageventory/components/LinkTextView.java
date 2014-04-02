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

package com.mageventory.components;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class LinkTextView extends TextView {

    private static final int LINK_COLOR = 0xFF5C5CFF;
    private Context mContext;
    private String mURL;
    private View.OnClickListener mOnClickListener;
    private boolean mLongClickOccured;

    private void init()
    {
        setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                mLongClickOccured = true;
                return false;
            }
        });
        setTextColor(LINK_COLOR);

        setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                Log.d("haha", "MotionEvent: " + event);

                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        setTextColor(Color.YELLOW);
                        return false;
                    case MotionEvent.ACTION_CANCEL:
                        setTextColor(LINK_COLOR);
                        return false;
                    case MotionEvent.ACTION_UP:
                        setTextColor(LINK_COLOR);

                        if (mLongClickOccured == false
                                || LinkTextView.this.isTextSelectable() == false)
                        {
                            if (mURL != null)
                            {
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(mURL));
                                mContext.startActivity(i);
                            }
                            else if (mOnClickListener != null)
                            {
                                mOnClickListener.onClick(v);
                            }
                        }

                        mLongClickOccured = false;
                        return false;
                }

                return false;
            }
        });
    }

    public LinkTextView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public LinkTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public LinkTextView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        mContext = context;
        init();
    }

    public void setURL(String url)
    {
        mURL = url;
        setText(Html.fromHtml("<u>" + url + "</u>"));
    }

    public void setTextAndURL(String text, String url)
    {
        mURL = url;
        setText(Html.fromHtml("<u>" + text + "</u>"));
    }

    public void setTextAndOnClickListener(String text, View.OnClickListener onClickListener)
    {
        mOnClickListener = onClickListener;
        setText(Html.fromHtml("<u>" + text + "</u>"));
    }
}
