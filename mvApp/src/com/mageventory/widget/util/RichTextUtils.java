package com.mageventory.widget.util;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.view.TextureView;

/**
 * The utilities to help to handle link clicks in the {@link TextureView}
 * widgets with the text specified from html string <br>
 * Taken from http://stackoverflow.com/a/11417498/527759
 */
public class RichTextUtils {
    /**
     * Convert all spans in the original of the sourceType using the converter
     * 
     * @param original spanned source
     * @param sourceType the type of the spans to convert
     * @param converter converter to convert sourceType spans to the custom
     *            type
     * @return
     */
    public static <A extends CharacterStyle, B extends CharacterStyle> Spannable replaceAll(
            Spanned original, Class<A> sourceType, SpanConverter<A, B> converter) {
        SpannableString result = new SpannableString(original);
        A[] spans = result.getSpans(0, result.length(), sourceType);

        for (A span : spans) {
            int start = result.getSpanStart(span);
            int end = result.getSpanEnd(span);
            int flags = result.getSpanFlags(span);

            result.removeSpan(span);
            result.setSpan(converter.convert(span), start, end, flags);
        }

        return (result);
    }

    /**
     * Span converter from A to B
     * 
     * @param <A>
     * @param <B>
     */
    public interface SpanConverter<A extends CharacterStyle, B extends CharacterStyle> {
        B convert(A span);
    }
}
