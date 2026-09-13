package de.pritcloud.appstow;

import java.util.HashSet;
import java.util.Set;

final class ShortcutEntry {

    static final String TYPE_WEBSITE = "website";
    static final String TYPE_WEB_APP = "web_app";
    static final String TYPE_APP_SETTINGS = "app_settings";
    static final String TYPE_DEEP_LINK = "deep_link";

    final String id;
    final String name;
    final String type;
    final String target;
    final Set<String> categoryIds;
    final boolean favorite;

    ShortcutEntry(
            String id,
            String name,
            String type,
            String target,
            Set<String> categoryIds,
            boolean favorite) {

        this.id = id;
        this.name = name;
        this.type = type;
        this.target = target;
        this.categoryIds = new HashSet<>(categoryIds);
        this.favorite = favorite;
    }
}
