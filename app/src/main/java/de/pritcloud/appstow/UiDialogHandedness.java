package de.pritcloud.appstow;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewParent;

final class UiDialogHandedness {

    private UiDialogHandedness() {
    }

    static AlertDialog show(
            Context context,
            AlertDialog dialog) {

        if (context == null
                || dialog == null) {

            throw new IllegalArgumentException(
                    "Dialog context and dialog are required.");
        }

        dialog.show();

        apply(
                context,
                dialog);

        return dialog;
    }

    static void apply(
            Context context,
            AlertDialog dialog) {

        if (context == null
                || dialog == null) {

            return;
        }

        View anchor =
                dialog.getButton(
                        AlertDialog.BUTTON_POSITIVE);

        if (anchor == null) {
            anchor =
                    dialog.getButton(
                            AlertDialog.BUTTON_NEGATIVE);
        }

        if (anchor == null) {
            anchor =
                    dialog.getButton(
                            AlertDialog.BUTTON_NEUTRAL);
        }

        if (anchor == null) {
            return;
        }

        ViewParent parent =
                anchor.getParent();

        if (parent instanceof View) {

            ((View) parent)
                    .setLayoutDirection(
                            UiHandedness.layoutDirection(
                                    context));
        }
    }
}
