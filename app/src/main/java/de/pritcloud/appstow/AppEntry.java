package de.pritcloud.appstow;

import android.content.pm.ResolveInfo;

final class AppEntry {

    final String label;
    final String packageName;
    final ResolveInfo resolveInfo;

    AppEntry(
            String label,
            String packageName,
            ResolveInfo resolveInfo) {
        this.label = label;
        this.packageName = packageName;
        this.resolveInfo = resolveInfo;
    }
}
