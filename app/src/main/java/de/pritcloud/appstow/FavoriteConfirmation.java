package de.pritcloud.appstow;

import android.app.AlertDialog;
import android.content.Context;

final class FavoriteConfirmation {

    private FavoriteConfirmation() {
    }

    static void confirmRemoval(
            Context context,
            CharSequence name,
            Runnable onConfirm) {

        new AlertDialog.Builder(context)
                .setTitle(
                        R.string.favorite_remove_title)
                .setMessage(
                        context.getString(
                                R.string.favorite_remove_message,
                                name))
                .setPositiveButton(
                        R.string.favorite_remove_confirm,
                        (dialog, which) ->
                                onConfirm.run())
                .setNegativeButton(
                        R.string.action_cancel,
                        null)
                .show();
    }
}
