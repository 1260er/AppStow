package de.pritcloud.appstow;

import android.app.AlertDialog;
import android.content.Context;

import androidx.core.content.ContextCompat;

final class FavoriteConfirmation {

    private FavoriteConfirmation() {
    }

    static void confirmRemoval(
            Context context,
            CharSequence name,
            Runnable onConfirm) {

        AlertDialog dialog =
                new AlertDialog.Builder(context)
                        .setTitle(
                                R.string.favorite_remove_title)
                        .setMessage(
                                context.getString(
                                        R.string.favorite_remove_message,
                                        name))
                        .setPositiveButton(
                                R.string.favorite_remove_confirm,
                                (currentDialog, which) ->
                                        onConfirm.run())
                        .setNegativeButton(
                                R.string.action_cancel,
                                null)
                        .create();

        dialog.setOnShowListener(ignored -> {
            int neutralColor =
                    ContextCompat.getColor(
                            context,
                            R.color.ui_text_primary);

            int dangerColor =
                    ContextCompat.getColor(
                            context,
                            R.color.ui_danger);

            dialog.getButton(
                            AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(
                            neutralColor);

            dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(
                            dangerColor);
        });

        dialog.show();
    }
}
