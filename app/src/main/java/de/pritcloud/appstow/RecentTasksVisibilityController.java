package de.pritcloud.appstow;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;

/** Applies Recents visibility only to this activity's own Android task. */
final class RecentTasksVisibilityController {
    private RecentTasksVisibilityController() {
    }

    static void apply(Activity activity, boolean showInRecents) {
        int currentTaskId = activity.getTaskId();
        if (currentTaskId < 0) {
            return;
        }

        ActivityManager manager = (ActivityManager)
                activity.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            return;
        }

        try {
            for (ActivityManager.AppTask task : manager.getAppTasks()) {
                ActivityManager.RecentTaskInfo info = task.getTaskInfo();
                // WebAppActivity uses a distinct task affinity: never change
                // another task just because it belongs to the same package.
                if (info != null && info.taskId == currentTaskId) {
                    task.setExcludeFromRecents(!showInRecents);
                    return;
                }
            }
        } catch (SecurityException | IllegalArgumentException exception) {
            // Some OEM task managers may deny or invalidate a task. The UI
            // setting remains stored and is retried at the next onStart().
        }
    }
}
