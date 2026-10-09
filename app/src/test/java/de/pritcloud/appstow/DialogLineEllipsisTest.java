package de.pritcloud.appstow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class DialogLineEllipsisTest {

    @Test
    public void eachLongEntryIsShortenedWithoutLosingRows() {

        Context context =
                ApplicationProvider.getApplicationContext();

        EllipsizedLinesTextView text =
                new EllipsizedLinesTextView(context);

        String original =
                "Office:\n"
                        + "1. OpenDocument Reader\n"
                        + "2. Inter Profile Sharing\n"
                        + "\n"
                        + "Media:\n"
                        + "3. MiX";

        SpannableString styled =
                new SpannableString(original);

        int coloredStart =
                original.indexOf("2. Inter");

        styled.setSpan(
                new ForegroundColorSpan(Color.RED),
                coloredStart,
                coloredStart + "2. Inter Profile Sharing".length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        text.setComparisonText(styled);

        // Robolectric verwendet eine vereinfachte
        // Schriftbreitenberechnung. Daher waehlen
        // wir eine Breite relativ zu den Messwerten.
        float shortWidth =
                text.getPaint().measureText("Office:");

        float longWidth =
                text.getPaint().measureText(
                        "1. OpenDocument Reader");

        assertTrue(longWidth > shortWidth);

        int narrowWidth = (int) Math.floor(
                (shortWidth + longWidth) / 2f);

        assertTrue(narrowWidth > shortWidth);
        assertTrue(narrowWidth < longWidth);

        text.layout(0, 0, narrowWidth, 600);

        String displayed =
                text.getText().toString();

        String[] lines =
                displayed.split("\n", -1);

        assertEquals(6, lines.length);
        assertEquals("Office:", lines[0]);
        assertTrue(
                "Spaltenbreite=" + text.getWidth()
                        + ", Textbreite="
                        + text.getPaint().measureText(
                                "1. OpenDocument Reader")
                        + ", Zeile=[" + lines[1] + "]"
                        + ", Gesamtausgabe=[" + displayed + "]",
                lines[1].endsWith("\u2026"));
        assertTrue(lines[2].endsWith("\u2026"));
        assertEquals("", lines[3]);
        assertEquals("Media:", lines[4]);
        assertEquals("3. MiX", lines[5]);

        // Farbmarkierung nach der Kürzung erhalten.
        int displayedStart =
                displayed.indexOf("2. ");

        assertTrue(displayedStart >= 0);

        ForegroundColorSpan[] spans =
                ((Spanned) text.getText()).getSpans(
                        displayedStart,
                        displayedStart + 2,
                        ForegroundColorSpan.class);

        assertTrue(spans.length > 0);
        assertEquals(
                Color.RED,
                spans[0].getForegroundColor());

        int ellipsisPosition =
                displayed.indexOf(
                        "…",
                        displayedStart);

        assertTrue(
                ellipsisPosition > displayedStart);

        ForegroundColorSpan[] ellipsisColors =
                ((Spanned) text.getText()).getSpans(
                        ellipsisPosition,
                        ellipsisPosition + 1,
                        ForegroundColorSpan.class);

        assertTrue(ellipsisColors.length > 0);

        assertEquals(
                Color.RED,
                ellipsisColors[0].getForegroundColor());

        // Für Barrierefreiheit bleibt der gesamte
        // ungekürzte Inhalt zugänglich.
        assertEquals(
                original,
                text.getContentDescription().toString());

        // Größere Spaltenbreite zeigt Namen wieder ganz.
        text.layout(0, 0, 900, 600);

        assertEquals(
                original,
                text.getText().toString());
    }

    @Test
    public void allComparisonColumnsUseLineEllipsis() {

        Context context =
                ApplicationProvider.getApplicationContext();

        View dialog = LayoutInflater.from(context)
                .inflate(
                        R.layout.dialog_sorting_suggestions,
                        null,
                        false);

        int[] columns = {
                R.id.suggestionFavoriteAssignmentCurrent,
                R.id.suggestionFavoriteAssignmentPreview,
                R.id.suggestionFavoriteOrderCurrent,
                R.id.suggestionFavoriteOrderPreview,
                R.id.suggestionCategoriesCurrent,
                R.id.suggestionCategoriesPreview,
                R.id.suggestionAppsCurrent,
                R.id.suggestionAppsPreview,
                R.id.suggestionShortcutsCurrent,
                R.id.suggestionShortcutsPreview
        };

        for (int id : columns) {
            assertTrue(
                    dialog.findViewById(id)
                            instanceof EllipsizedLinesTextView);
        }

        int[] choices = {
                R.id.suggestionApplyFavoriteAssignment,
                R.id.suggestionApplyFavoriteOrder,
                R.id.suggestionApplyCategories,
                R.id.suggestionApplyApps,
                R.id.suggestionApplyShortcuts
        };

        for (int id : choices) {

            CheckBox option =
                    dialog.findViewById(id);

            assertEquals(1, option.getMaxLines());

            assertEquals(
                    android.text.TextUtils.TruncateAt.END,
                    option.getEllipsize());
        }
    }
}
