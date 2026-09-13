package de.pritcloud.appstow;

import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

final class AppEntry {

    final String label;
    final String packageName;
    final ResolveInfo resolveInfo;
    final Drawable.ConstantState iconState;

    AppEntry(
            String label,
            String packageName,
            ResolveInfo resolveInfo,
            Drawable.ConstantState iconState) {
        this.label = label;
        this.packageName = packageName;
        this.resolveInfo = resolveInfo;
        this.iconState = iconState;
    }
}
