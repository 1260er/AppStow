package de.pritcloud.appstow;

import android.app.backup.BackupAgentHelper;
import android.app.backup.FullBackupDataOutput;
import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;

/**
 * Use Android Auto Backup, not the legacy key/value backup protocol.
 * onRestoreFinished runs after the platform restores the file allowlist.
 */
public final class AppStowBackupAgent extends BackupAgentHelper {

    @Override
    public void onFullBackup(FullBackupDataOutput data)
            throws IOException {
        // Prepare the settings mirror even when the app has not been
        // launched since an update. This performs no history export.
        new SortingSettingsStore(this);
        super.onFullBackup(data);
    }

    @Override
    public void onRestoreFinished() {
        super.onRestoreFinished();
        restoreConfiguration(this);
    }

    static void restoreConfiguration(Context context) {
        SortingSettingsStore.Settings restored =
                SystemBackupSortingStore.load(context);
        if (restored == null) {
            return;
        }

        // Do NOT reconstruct usage statistics or cached weighted counts.
        // On next start MainActivity will switch automatic sorting to
        // Manual if there is no approved local usage history.
        new SortingSettingsStore(context).save(restored);

        SharedPreferences sorting = context.getSharedPreferences(
                "sorting_settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sorting.edit()
                .remove("automatic_history_approved")
                .remove("restored_auto_favorites_pending")
                .remove("last_suggestion_handled_at");
        AutomaticSortingSnapshotStore.invalidate(editor);
        editor.commit();

        context.getSharedPreferences("usage_statistics", Context.MODE_PRIVATE)
                .edit().clear().commit();
    }
}
