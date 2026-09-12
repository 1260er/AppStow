package de.pritcloud.shortcutlauncher;

final class OverviewSection {

    final String id;
    final String title;
    final String emptyMessage;
    boolean expanded;

    OverviewSection(
            String id,
            String title,
            String emptyMessage) {
        this.id = id;
        this.title = title;
        this.emptyMessage = emptyMessage;
        this.expanded = false;
    }
}
