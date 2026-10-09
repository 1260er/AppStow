package de.pritcloud.appstow;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.widget.TextView;

import java.text.BreakIterator;
import java.util.Locale;

public final class EllipsizedLinesTextView extends TextView {

    private CharSequence fullContent = "";

    public EllipsizedLinesTextView(Context context) {
        super(context);
        initialize();
    }

    public EllipsizedLinesTextView(
            Context context,
            AttributeSet attrs) {

        super(context, attrs);
        initialize();
    }

    private void initialize() {
        // Explizite Zeilenumbrueche bleiben bestehen,
        // automatische Zeilenumbrueche werden vermieden.
        setHorizontallyScrolling(true);
    }

    void setComparisonText(CharSequence content) {
        fullContent = content == null ? "" : content;

        // TalkBack soll den ungekürzten Inhalt erhalten.
        setContentDescription(fullContent);

        updateDisplay();
    }

    @Override
    protected void onSizeChanged(
            int width,
            int height,
            int oldWidth,
            int oldHeight) {

        super.onSizeChanged(
                width,
                height,
                oldWidth,
                oldHeight);

        if (width != oldWidth) {
            updateDisplay();
        }
    }

    private void updateDisplay() {

        if (getWidth() <= 0) {
            super.setText(
                    fullContent,
                    BufferType.SPANNABLE);
            return;
        }

        float available = Math.max(
                0,
                getWidth()
                        - getCompoundPaddingLeft()
                        - getCompoundPaddingRight());

        String plain = fullContent.toString();

        SpannableStringBuilder displayed =
                new SpannableStringBuilder();

        int start = 0;

        while (start <= plain.length()) {

            int newline = plain.indexOf(
                    "\n",
                    start);

            int end = newline >= 0
                    ? newline
                    : plain.length();

            CharSequence line =
                    fullContent.subSequence(start, end);

            displayed.append(
                    ellipsizeOneLine(
                            line,
                            available));

            if (newline < 0) {
                break;
            }

            displayed.append("\n");
            start = newline + 1;
        }

        super.setText(
                displayed,
                BufferType.SPANNABLE);
    }

    private CharSequence ellipsizeOneLine(
            CharSequence line,
            float available) {

        if (line.length() == 0
                || getPaint().measureText(
                        line.toString()) <= available) {

            return line;
        }

        final String ellipsis = "\u2026";

        float ellipsisWidth =
                getPaint().measureText(
                        ellipsis);

        if (available <= ellipsisWidth) {
            return ellipsis;
        }

        float textWidth =
                available - ellipsisWidth;

        BreakIterator iterator =
                BreakIterator.getCharacterInstance(
                        Locale.ROOT);

        iterator.setText(
                line.toString());

        int prefixEnd = 0;

        for (int boundary = iterator.first();
             (boundary = iterator.next())
                     != BreakIterator.DONE;) {

            if (getPaint().measureText(
                    line.subSequence(
                            0,
                            boundary).toString()) > textWidth) {

                break;
            }

            prefixEnd = boundary;
        }

        SpannableStringBuilder result =
                new SpannableStringBuilder(
                        line.subSequence(
                                0,
                                prefixEnd));

        int ellipsisStart =
                result.length();

        result.append(
                ellipsis);

        // Bei einer farbig hervorgehobenen Zeile
        // wird auch das Auslassungszeichen eingefärbt.
        if (line instanceof Spanned) {

            Spanned styled =
                    (Spanned) line;

            int sample =
                    Math.min(
                            prefixEnd,
                            line.length() - 1);

            ForegroundColorSpan[] colors =
                    styled.getSpans(
                            sample,
                            sample + 1,
                            ForegroundColorSpan.class);

            for (ForegroundColorSpan color : colors) {

                result.setSpan(
                        new ForegroundColorSpan(
                                color.getForegroundColor()),
                        ellipsisStart,
                        result.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        return result;
    }
}
