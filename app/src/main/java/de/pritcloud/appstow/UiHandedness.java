package de.pritcloud.appstow;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

final class UiHandedness {

    private UiHandedness() {
    }

    static boolean isLeft(
            Context context) {

        return new UiSettingsStore(
                context)
                .load()
                .controlSide
                == UiSettingsStore.ControlSide.LEFT;
    }

    static int layoutDirection(
            Context context) {

        return isLeft(
                context)
                ? View.LAYOUT_DIRECTION_RTL
                : View.LAYOUT_DIRECTION_LTR;
    }

    static void applyContainer(
            Context context,
            View view) {

        if (view == null) {
            return;
        }

        view.setLayoutDirection(
                layoutDirection(
                        context));
    }

    static void applyTextEdge(
            Context context,
            TextView... textViews) {

        if (textViews == null) {
            return;
        }

        int horizontalGravity =
                isLeft(
                        context)
                        ? Gravity.RIGHT
                        : Gravity.LEFT;

        for (TextView textView :
                textViews) {

            if (textView == null) {
                continue;
            }

            int verticalGravity =
                    textView.getGravity()
                            & Gravity.VERTICAL_GRAVITY_MASK;

            textView.setGravity(
                    horizontalGravity
                            | verticalGravity);
        }
    }
}
