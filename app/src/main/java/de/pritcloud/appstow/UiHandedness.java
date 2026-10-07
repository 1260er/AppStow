package de.pritcloud.appstow;

import android.content.Context;
import android.view.View;

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
}
