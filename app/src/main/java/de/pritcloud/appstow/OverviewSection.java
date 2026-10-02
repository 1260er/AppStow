package de.pritcloud.appstow;

final class OverviewSection {

    final String id;
    final String title;
    final String emptyMessage;
    final String gridLabel;
    final String gridSymbol;
    boolean expanded;

    OverviewSection(
            String id,
            String title,
            String emptyMessage) {
        this(id, title, emptyMessage, title, "📁");
    }

    OverviewSection(
            String id,
            String title,
            String emptyMessage,
            String gridLabel,
            String gridSymbol) {

        this.id = id;
        this.title = title;
        this.emptyMessage = emptyMessage;
        this.gridLabel = gridLabel;
        this.gridSymbol = gridSymbol;
        this.expanded = false;
    }
}
