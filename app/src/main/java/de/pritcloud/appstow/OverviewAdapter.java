package de.pritcloud.appstow;

import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

final class OverviewAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SECTION = 0;
    private static final int TYPE_MESSAGE = 1;
    private static final int TYPE_APP = 2;
    private static final int TYPE_SHORTCUT = 3;

    private static final String CATEGORY_PREFIX =
            "category:";

    interface OnAppClickListener {
        void onAppClick(AppEntry app);
    }

    interface OnShortcutClickListener {
        void onShortcutClick(
                ShortcutEntry shortcut);
    }

    interface OnAppLongClickListener {
        void onAppLongClick(AppEntry app);
    }

    interface OnSectionDragStartListener {
        void onDragStart(
                RecyclerView.ViewHolder holder);
    }

    private final List<OverviewSection> sections;
    private final List<Row> rows =
            new ArrayList<>();

    private final List<AppEntry> allApps =
            new ArrayList<>();

    private final List<AppEntry> favoriteApps =
            new ArrayList<>();

    private final List<ShortcutEntry> allShortcuts =
            new ArrayList<>();

    private final PackageManager packageManager;
    private final FavoritesStore favoritesStore;
    private final CategoryStore categoryStore;
    private final ShortcutStore shortcutStore;

    private final OnAppClickListener appClickListener;
    private final OnShortcutClickListener shortcutClickListener;
    private final OnAppLongClickListener appLongClickListener;
    private final OnSectionDragStartListener dragStartListener;

    private boolean sortMode;

    OverviewAdapter(
            List<OverviewSection> sections,
            PackageManager packageManager,
            FavoritesStore favoritesStore,
            CategoryStore categoryStore,
            ShortcutStore shortcutStore,
            OnAppClickListener appClickListener,
            OnShortcutClickListener shortcutClickListener,
            OnAppLongClickListener appLongClickListener,
            OnSectionDragStartListener dragStartListener) {

        this.sections = sections;
        this.packageManager = packageManager;
        this.favoritesStore = favoritesStore;
        this.categoryStore = categoryStore;
        this.shortcutStore = shortcutStore;
        this.appClickListener = appClickListener;
        this.shortcutClickListener = shortcutClickListener;
        this.appLongClickListener = appLongClickListener;
        this.dragStartListener = dragStartListener;

        refreshShortcutEntries();
        rebuildRows();
    }

    void setApps(List<AppEntry> apps) {
        allApps.clear();
        allApps.addAll(apps);

        refreshFavoriteApps();
        refreshShortcutEntries();

        rebuildRows();
        notifyDataSetChanged();
    }

    private void refreshFavoriteApps() {
        favoriteApps.clear();

        for (AppEntry app : allApps) {
            if (favoritesStore.isFavorite(
                    app.packageName)) {

                favoriteApps.add(app);
            }
        }
    }

    private void refreshShortcutEntries() {
        allShortcuts.clear();
        allShortcuts.addAll(
                shortcutStore.getShortcuts());
    }

    private List<ShortcutEntry> getFavoriteShortcuts() {
        List<ShortcutEntry> result =
                new ArrayList<>();

        for (ShortcutEntry shortcut :
                allShortcuts) {

            if (shortcut.favorite) {
                result.add(shortcut);
            }
        }

        return result;
    }

    private void rebuildRows() {
        rows.clear();

        for (OverviewSection section :
                sections) {

            rows.add(
                    Row.section(section));

            if (sortMode) {
                continue;
            }

            if (!section.expanded) {
                continue;
            }

            if ("favorites".equals(
                    section.id)) {

                List<ShortcutEntry> favoriteShortcuts =
                        getFavoriteShortcuts();

                if (favoriteApps.isEmpty()
                        && favoriteShortcuts.isEmpty()) {

                    rows.add(
                            Row.message(section));

                } else {
                    for (AppEntry app :
                            favoriteApps) {

                        rows.add(
                                Row.app(
                                        section,
                                        app));
                    }

                    for (ShortcutEntry shortcut :
                            favoriteShortcuts) {

                        rows.add(
                                Row.shortcut(
                                        section,
                                        shortcut));
                    }
                }

                continue;
            }

            if (section.id.startsWith(
                    CATEGORY_PREFIX)) {

                String categoryId =
                        section.id.substring(
                                CATEGORY_PREFIX.length());

                List<AppEntry> categoryApps =
                        getCategoryApps(
                                categoryId);

                List<ShortcutEntry> categoryShortcuts =
                        getCategoryShortcuts(
                                categoryId);

                if (categoryApps.isEmpty()
                        && categoryShortcuts.isEmpty()) {

                    rows.add(
                            Row.message(section));

                } else {
                    for (AppEntry app :
                            categoryApps) {

                        rows.add(
                                Row.app(
                                        section,
                                        app));
                    }

                    for (ShortcutEntry shortcut :
                            categoryShortcuts) {

                        rows.add(
                                Row.shortcut(
                                        section,
                                        shortcut));
                    }
                }

                continue;
            }

            if ("shortcuts".equals(
                    section.id)) {

                if (allShortcuts.isEmpty()) {
                    rows.add(
                            Row.message(section));
                } else {
                    for (ShortcutEntry shortcut :
                            allShortcuts) {

                        rows.add(
                                Row.shortcut(
                                        section,
                                        shortcut));
                    }
                }

                continue;
            }

            rows.add(
                    Row.message(section));
        }
    }

    private List<AppEntry> getCategoryApps(
            String categoryId) {

        List<AppEntry> result =
                new ArrayList<>();

        for (AppEntry app : allApps) {
            Set<String> assignedIds =
                    categoryStore
                            .getAssignedCategoryIds(
                                    app.packageName);

            if (assignedIds.contains(
                    categoryId)) {

                result.add(app);
            }
        }

        return result;
    }

    private List<ShortcutEntry> getCategoryShortcuts(
            String categoryId) {

        List<ShortcutEntry> result =
                new ArrayList<>();

        for (ShortcutEntry shortcut :
                allShortcuts) {

            if (shortcut.categoryIds.contains(
                    categoryId)) {

                result.add(shortcut);
            }
        }

        return result;
    }

    @Override
    public int getItemViewType(
            int position) {

        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        LayoutInflater inflater =
                LayoutInflater.from(
                        parent.getContext());

        if (viewType == TYPE_SECTION) {
            View view =
                    inflater.inflate(
                            R.layout.item_overview_section,
                            parent,
                            false);

            return new SectionViewHolder(
                    view);
        }

        if (viewType == TYPE_APP
                || viewType == TYPE_SHORTCUT) {

            View view =
                    inflater.inflate(
                            R.layout.item_app,
                            parent,
                            false);

            return new EntryViewHolder(
                    view);
        }

        View view =
                inflater.inflate(
                        R.layout.item_overview_message,
                        parent,
                        false);

        return new MessageViewHolder(
                view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position) {

        Row row =
                rows.get(position);

        if (holder instanceof SectionViewHolder) {
            bindSection(
                    (SectionViewHolder) holder,
                    row.section);

            return;
        }

        if (holder instanceof EntryViewHolder) {
            if (row.type == TYPE_APP) {
                bindApp(
                        (EntryViewHolder) holder,
                        row.app);
            } else {
                bindShortcut(
                        (EntryViewHolder) holder,
                        row.shortcut);
            }

            return;
        }

        MessageViewHolder messageHolder =
                (MessageViewHolder) holder;

        messageHolder.message.setText(
                row.section.emptyMessage);
    }

    private void bindSection(
            SectionViewHolder holder,
            OverviewSection section) {

        holder.title.setText(
                section.title);

        holder.chevron.setVisibility(
                sortMode
                        ? View.GONE
                        : View.VISIBLE);

        holder.dragHandle.setVisibility(
                sortMode
                        ? View.VISIBLE
                        : View.GONE);

        holder.chevron.setRotation(
                section.expanded
                        ? 180f
                        : 0f);

        if (sortMode) {
            holder.itemView
                    .setOnClickListener(
                            null);

            holder.dragHandle
                    .setOnTouchListener(
                            (view, event) -> {
                                if (event.getActionMasked()
                                        == MotionEvent.ACTION_DOWN) {

                                    dragStartListener
                                            .onDragStart(
                                                    holder);
                                }

                                return false;
                            });

            return;
        }

        holder.dragHandle
                .setOnTouchListener(
                        null);

        holder.itemView
                .setOnClickListener(v -> {
                    section.expanded =
                            !section.expanded;

                    rebuildRows();
                    notifyDataSetChanged();
                });
    }

    private void bindApp(
            EntryViewHolder holder,
            AppEntry app) {

        holder.icon.setImageDrawable(
                app.resolveInfo.loadIcon(
                        packageManager));

        holder.name.setText(
                app.label);

        String categoryLabel =
                categoryStore
                        .getAssignedCategoryLabel(
                                app.packageName);

        holder.categories.setText(
                categoryLabel);

        holder.categories.setVisibility(
                categoryLabel.isEmpty()
                        ? View.INVISIBLE
                        : View.VISIBLE);

        boolean favorite =
                favoritesStore.isFavorite(
                        app.packageName);

        updateFavoriteButton(
                holder,
                favorite);

        holder.favorite
                .setOnClickListener(v -> {
                    favoritesStore.toggle(
                            app.packageName);

                    refreshFavoriteApps();
                    rebuildRows();
                    notifyDataSetChanged();
                });

        holder.itemView
                .setOnClickListener(v ->
                        appClickListener
                                .onAppClick(app));

        holder.itemView
                .setOnLongClickListener(v -> {
                    appLongClickListener
                            .onAppLongClick(app);

                    return true;
                });
    }

    private void bindShortcut(
            EntryViewHolder holder,
            ShortcutEntry shortcut) {

        if (ShortcutEntry.TYPE_WEBSITE.equals(
                shortcut.type)) {

            holder.icon.setImageResource(
                    R.drawable.ic_shortcut_web);

        } else if (ShortcutEntry.TYPE_WEB_APP.equals(
                shortcut.type)) {

            holder.icon.setImageResource(
                    R.drawable.ic_shortcut_webapp);

        } else {

            holder.icon.setImageResource(
                    R.drawable.ic_shortcut_action);
        }

        holder.name.setText(
                shortcut.name);

        String categoryLabel =
                getShortcutCategoryLabel(
                        shortcut);

        holder.categories.setText(
                categoryLabel);

        holder.categories.setVisibility(
                categoryLabel.isEmpty()
                        ? View.INVISIBLE
                        : View.VISIBLE);

        updateFavoriteButton(
                holder,
                shortcut.favorite);

        holder.favorite
                .setOnClickListener(v -> {
                    shortcutStore.toggleFavorite(
                            shortcut.id);

                    refreshShortcutEntries();
                    rebuildRows();
                    notifyDataSetChanged();
                });

        holder.itemView
                .setOnClickListener(v ->
                        shortcutClickListener
                                .onShortcutClick(
                                        shortcut));

        holder.itemView
                .setOnLongClickListener(
                        null);

        holder.itemView.setLongClickable(
                false);
    }

    private String getShortcutCategoryLabel(
            ShortcutEntry shortcut) {

        StringBuilder result =
                new StringBuilder();

        for (CategoryEntry category :
                categoryStore.getCategories()) {

            if (!shortcut.categoryIds.contains(
                    category.id)) {

                continue;
            }

            if (result.length() > 0) {
                result.append(" · ");
            }

            result.append(
                    category.name);
        }

        return result.toString();
    }

    private void updateFavoriteButton(
            EntryViewHolder holder,
            boolean favorite) {

        holder.favorite.setImageResource(
                favorite
                        ? R.drawable.ic_star_filled
                        : R.drawable.ic_star_outline);

        holder.favorite.setContentDescription(
                holder.itemView
                        .getContext()
                        .getString(
                                favorite
                                        ? R.string.action_remove_favorite
                                        : R.string.action_add_favorite));
    }

    void setSortMode(
            boolean enabled) {

        if (sortMode == enabled) {
            return;
        }

        sortMode = enabled;

        rebuildRows();
        notifyDataSetChanged();
    }

    boolean isSortMode() {
        return sortMode;
    }

    boolean moveSection(
            int fromPosition,
            int toPosition) {

        if (!sortMode
                || fromPosition < 0
                || toPosition < 0
                || fromPosition >= sections.size()
                || toPosition >= sections.size()) {

            return false;
        }

        Collections.swap(
                sections,
                fromPosition,
                toPosition);

        Collections.swap(
                rows,
                fromPosition,
                toPosition);

        notifyItemMoved(
                fromPosition,
                toPosition);

        return true;
    }

    void refreshAppRows() {
        refreshFavoriteApps();
        refreshShortcutEntries();

        rebuildRows();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private static final class Row {

        final int type;
        final OverviewSection section;
        final AppEntry app;
        final ShortcutEntry shortcut;

        private Row(
                int type,
                OverviewSection section,
                AppEntry app,
                ShortcutEntry shortcut) {

            this.type = type;
            this.section = section;
            this.app = app;
            this.shortcut = shortcut;
        }

        static Row section(
                OverviewSection section) {

            return new Row(
                    TYPE_SECTION,
                    section,
                    null,
                    null);
        }

        static Row message(
                OverviewSection section) {

            return new Row(
                    TYPE_MESSAGE,
                    section,
                    null,
                    null);
        }

        static Row app(
                OverviewSection section,
                AppEntry app) {

            return new Row(
                    TYPE_APP,
                    section,
                    app,
                    null);
        }

        static Row shortcut(
                OverviewSection section,
                ShortcutEntry shortcut) {

            return new Row(
                    TYPE_SHORTCUT,
                    section,
                    null,
                    shortcut);
        }
    }

    static final class SectionViewHolder
            extends RecyclerView.ViewHolder {

        final TextView title;
        final ImageView chevron;
        final ImageView dragHandle;

        SectionViewHolder(
                @NonNull View itemView) {

            super(itemView);

            title =
                    itemView.findViewById(
                            R.id.overviewSectionTitle);

            chevron =
                    itemView.findViewById(
                            R.id.overviewSectionChevron);

            dragHandle =
                    itemView.findViewById(
                            R.id.overviewSectionDragHandle);
        }
    }

    static final class MessageViewHolder
            extends RecyclerView.ViewHolder {

        final TextView message;

        MessageViewHolder(
                @NonNull View itemView) {

            super(itemView);

            message =
                    itemView.findViewById(
                            R.id.overviewSectionMessage);
        }
    }

    static final class EntryViewHolder
            extends RecyclerView.ViewHolder {

        final ImageView icon;
        final TextView name;
        final TextView categories;
        final ImageButton favorite;

        EntryViewHolder(
                @NonNull View itemView) {

            super(itemView);

            icon =
                    itemView.findViewById(
                            R.id.appIcon);

            name =
                    itemView.findViewById(
                            R.id.appName);

            categories =
                    itemView.findViewById(
                            R.id.appCategories);

            favorite =
                    itemView.findViewById(
                            R.id.appFavorite);
        }
    }
}
