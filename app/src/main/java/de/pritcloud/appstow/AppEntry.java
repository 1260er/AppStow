package de.pritcloud.appstow;

import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.util.Locale;

final class AppEntry {

    final String label;
    final String packageName;
    final ResolveInfo resolveInfo;
    final Drawable.ConstantState iconState;
    final String searchLabel;
    final String searchPackageName;
    final int contentGeneration;

    AppEntry(
            String label,
            String packageName,
            ResolveInfo resolveInfo,
            Drawable.ConstantState iconState,
            int contentGeneration) {
        this.label = label;
        this.packageName = packageName;
        this.resolveInfo = resolveInfo;
        this.iconState = iconState;
        this.searchLabel =
                label.toLowerCase(
                        Locale.ROOT);
        this.searchPackageName =
                packageName.toLowerCase(
                        Locale.ROOT);
        this.contentGeneration =
                contentGeneration;
    }
}
