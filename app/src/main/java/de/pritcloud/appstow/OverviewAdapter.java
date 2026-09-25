package de.pritcloud.appstow;

import android.annotation.SuppressLint;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    interface OnShortcutLongClickListener {
        void onShortcutLongClick(
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

    private final FavoritesStore favoritesStore;
    private final CategoryStore categoryStore;
    private final AppIconLoader appIconLoader;
    private final ShortcutStore shortcutStore;
    private final SectionItemOrderStore sectionItemOrderStore;

    private final OnAppClickListener appClickListener;
    private final OnShortcutClickListener shortcutClickListener;
    private final OnShortcutLongClickListener shortcutLongClickListener;
    private final OnAppLongClickListener appLongClickListener;
    private final OnSectionDragStartListener dragStartListener;

    private boolean sortMode;
    private String itemSortSectionId;

    OverviewAdapter(
            List<OverviewSection> sections,
            FavoritesStore favoritesStore,
            CategoryStore categoryStore,
            AppIconLoader appIconLoader,
            ShortcutStore shortcutStore,
            SectionItemOrderStore sectionItemOrderStore,
            OnAppClickListener appClickListener,
            OnShortcutClickListener shortcutClickListener,
            OnShortcutLongClickListener shortcutLongClickListener,
            OnAppLongClickListener appLongClickListener,
            OnSectionDragStartListener dragStartListener) {

        this.sections = sections;
        this.favoritesStore = favoritesStore;
        this.categoryStore = categoryStore;
        this.appIconLoader = appIconLoader;
        this.shortcutStore = shortcutStore;
        this.sectionItemOrderStore = sectionItemOrderStore;
        this.appClickListener = appClickListener;
        this.shortcutClickListener = shortcutClickListener;
        this.shortcutLongClickListener = shortcutLongClickListener;
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
        notifyStructureChanged();
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
                    addOrderedEntries(
                            section,
                            favoriteApps,
                            favoriteShortcuts);
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
                    addOrderedEntries(
                            section,
                            categoryApps,
                            categoryShortcuts);
                }

                continue;
            }

            if ("shortcuts".equals(
                    section.id)) {

                if (allShortcuts.isEmpty()) {
                    rows.add(
                            Row.message(section));
                } else {
                    addOrderedEntries(
                            section,
                            Collections.emptyList(),
                            allShortcuts);
                }

                continue;
            }

            rows.add(
                    Row.message(section));
        }
    }

    private void addOrderedEntries(
            OverviewSection section,
            List<AppEntry> sectionApps,
            List<ShortcutEntry> sectionShortcuts) {

        List<String> currentIds =
                new ArrayList<>();

        Map<String, Row> rowsById =
                new HashMap<>();

        for (AppEntry app : sectionApps) {
            String id =
                    SectionItemOrderStore.appItemId(
                            app.packageName);

            currentIds.add(id);

            rowsById.put(
                    id,
                    Row.app(
                            section,
                            app));
        }

        for (ShortcutEntry shortcut :
                sectionShortcuts) {

            String id =
                    SectionItemOrderStore.shortcutItemId(
                            shortcut.id);

            currentIds.add(id);

            rowsById.put(
                    id,
                    Row.shortcut(
                            section,
                            shortcut));
        }

        List<String> orderedIds =
                sectionItemOrderStore.getOrderedIds(
                        section.id,
                        currentIds);

        for (String id : orderedIds) {
            Row row =
                    rowsById.get(id);

            if (row != null) {
                rows.add(row);
            }
        }
    }

    private List<AppEntry> getCategoryApps(
            String categoryId) {

        List<AppEntry> result =
                new ArrayList<>();

        for (AppEntry app : allApps) {
            if (categoryStore
                    .isAssignedToCategory(
                            app.packageName,
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
                        row.section,
                        row.app);
            } else {
                bindShortcut(
                        (EntryViewHolder) holder,
                        row.section,
                        row.shortcut);
            }

            return;
        }

        MessageViewHolder messageHolder =
                (MessageViewHolder) holder;

        messageHolder.message.setText(
                row.section.emptyMessage);
    }

    private void collapseOtherSections(
            OverviewSection activeSection) {

        for (OverviewSection section : sections) {
            if (section != activeSection) {
                section.expanded = false;
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void bindSection(
            SectionViewHolder holder,
            OverviewSection section) {

        holder.title.setText(
                section.title);

        boolean itemSortMode =
                section.id.equals(
                        itemSortSectionId);

        holder.sortButton.setVisibility(
                sortMode
                        ? View.GONE
                        : View.VISIBLE);

        holder.sortButton.setImageResource(
                itemSortMode
                        ? R.drawable.ic_done
                        : R.drawable.ic_sort_overview);

        holder.sortButton.setContentDescription(
                holder.itemView.getContext().getString(
                        itemSortMode
                                ? R.string.action_finish_section_item_sorting
                                : R.string.action_sort_section_items));

        holder.chevron.setVisibility(
                sortMode || itemSortMode
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

        holder.sortButton.setOnClickListener(v -> {
            if (itemSortMode) {
                saveItemOrder();
                itemSortSectionId = null;
            } else {
                collapseOtherSections(section);

                itemSortSectionId =
                        section.id;

                section.expanded = true;
            }

            rebuildRows();
            notifyStructureChanged();
        });

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

        if (itemSortMode) {
            holder.itemView
                    .setOnClickListener(
                            null);
        } else {
            holder.itemView
                    .setOnClickListener(v -> {
                        boolean expand =
                                !section.expanded;

                        if (expand) {
                            collapseOtherSections(
                                    section);
                        }

                        section.expanded =
                                expand;

                        rebuildRows();
                        notifyStructureChanged();
                    });
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void bindApp(
            EntryViewHolder holder,
            OverviewSection section,
            AppEntry app) {

        boolean itemSortMode =
                section.id.equals(
                        itemSortSectionId);

        appIconLoader.bind(
                holder.icon,
                app);

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

        holder.favorite.setVisibility(
                itemSortMode
                        ? View.GONE
                        : View.VISIBLE);

        holder.itemDragHandle.setVisibility(
                itemSortMode
                        ? View.VISIBLE
                        : View.GONE);

        if (itemSortMode) {
            holder.favorite.setOnClickListener(null);

            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnLongClickListener(null);
            holder.itemView.setLongClickable(false);

            holder.itemDragHandle.setOnTouchListener(
                    (view, event) -> {
                        if (event.getActionMasked()
                                == MotionEvent.ACTION_DOWN) {

                            dragStartListener.onDragStart(
                                    holder);
                        }

                        return false;
                    });

        } else {
            holder.itemDragHandle.setOnTouchListener(null);

            holder.favorite
                    .setOnClickListener(v -> {
                        Runnable toggleFavorite = () -> {
                            favoritesStore.toggle(
                                    app.packageName);

                            refreshFavoriteApps();
                            rebuildRows();
                            notifyStructureChanged();
                        };

                        if (!favoritesStore.isFavorite(
                                app.packageName)) {

                            toggleFavorite.run();
                            return;
                        }

                        FavoriteConfirmation.confirmRemoval(
                                holder.itemView.getContext(),
                                app.label,
                                toggleFavorite);
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

            holder.itemView.setLongClickable(true);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void bindShortcut(
            EntryViewHolder holder,
            OverviewSection section,
            ShortcutEntry shortcut) {

        boolean itemSortMode =
                section.id.equals(
                        itemSortSectionId);

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
                categoryStore.getCategoryLabel(
                        shortcut.categoryIds);

        holder.categories.setText(
                categoryLabel);

        holder.categories.setVisibility(
                categoryLabel.isEmpty()
                        ? View.INVISIBLE
                        : View.VISIBLE);

        updateFavoriteButton(
                holder,
                shortcut.favorite);

        holder.favorite.setVisibility(
                itemSortMode
                        ? View.GONE
                        : View.VISIBLE);

        holder.itemDragHandle.setVisibility(
                itemSortMode
                        ? View.VISIBLE
                        : View.GONE);

        if (itemSortMode) {
            holder.favorite.setOnClickListener(null);
            holder.itemView.setOnClickListener(null);

            holder.itemDragHandle.setOnTouchListener(
                    (view, event) -> {
                        if (event.getActionMasked()
                                == MotionEvent.ACTION_DOWN) {

                            dragStartListener.onDragStart(
                                    holder);
                        }

                        return false;
                    });

        } else {
            holder.itemDragHandle.setOnTouchListener(null);

            holder.favorite
                    .setOnClickListener(v -> {
                        Runnable toggleFavorite = () -> {
                            shortcutStore.toggleFavorite(
                                    shortcut.id);

                            refreshShortcutEntries();
                            rebuildRows();
                            notifyStructureChanged();
                        };

                        if (!shortcut.favorite) {
                            toggleFavorite.run();
                            return;
                        }

                        FavoriteConfirmation.confirmRemoval(
                                holder.itemView.getContext(),
                                shortcut.name,
                                toggleFavorite);
                    });

            holder.itemView
                    .setOnClickListener(v ->
                            shortcutClickListener
                                    .onShortcutClick(
                                            shortcut));
        }

        if (itemSortMode) {
            holder.itemView.setOnLongClickListener(null);
            holder.itemView.setLongClickable(false);
        } else {
            holder.itemView.setOnLongClickListener(v -> {
                shortcutLongClickListener
                        .onShortcutLongClick(
                                shortcut);

                return true;
            });

            holder.itemView.setLongClickable(true);
        }
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

        if (enabled
                && itemSortSectionId != null) {

            saveItemOrder();
            itemSortSectionId = null;
        }

        sortMode = enabled;

        rebuildRows();
        notifyStructureChanged();
    }

    boolean isSortMode() {
        return sortMode;
    }

    void finishItemSortMode() {
        if (itemSortSectionId == null) {
            return;
        }

        saveItemOrder();
        itemSortSectionId = null;

        rebuildRows();
        notifyStructureChanged();
    }

    boolean isItemSortMode() {
        return itemSortSectionId != null;
    }

    String getItemSortSectionId() {
        return itemSortSectionId;
    }

    boolean moveItem(
            int fromPosition,
            int toPosition) {

        if (itemSortSectionId == null
                || fromPosition < 0
                || toPosition < 0
                || fromPosition >= rows.size()
                || toPosition >= rows.size()) {

            return false;
        }

        Row source =
                rows.get(fromPosition);

        Row target =
                rows.get(toPosition);

        if (!source.isEntry()
                || !target.isEntry()
                || !itemSortSectionId.equals(
                        source.section.id)
                || !itemSortSectionId.equals(
                        target.section.id)) {

            return false;
        }

        Collections.swap(
                rows,
                fromPosition,
                toPosition);

        notifyItemMoved(
                fromPosition,
                toPosition);

        return true;
    }

    void saveItemOrder() {
        if (itemSortSectionId == null) {
            return;
        }

        List<String> itemIds =
                new ArrayList<>();

        for (Row row : rows) {
            if (row.isEntry()
                    && itemSortSectionId.equals(
                            row.section.id)) {

                itemIds.add(
                        row.itemId());
            }
        }

        sectionItemOrderStore.saveOrder(
                itemSortSectionId,
                itemIds);
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
        notifyStructureChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void notifyStructureChanged() {
        // rebuildRows() can change row count, type and position across
        // multiple sections at once. A full refresh is intentional here.
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

        boolean isEntry() {
            return type == TYPE_APP
                    || type == TYPE_SHORTCUT;
        }

        String itemId() {
            if (type == TYPE_APP) {
                return SectionItemOrderStore.appItemId(
                        app.packageName);
            }

            return SectionItemOrderStore.shortcutItemId(
                    shortcut.id);
        }
    }

    static final class SectionViewHolder
            extends RecyclerView.ViewHolder {

        final TextView title;
        final ImageButton sortButton;
        final ImageView chevron;
        final ImageView dragHandle;

        SectionViewHolder(
                @NonNull View itemView) {

            super(itemView);

            title =
                    itemView.findViewById(
                            R.id.overviewSectionTitle);

            sortButton =
                    itemView.findViewById(
                            R.id.overviewSectionSort);

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
        final ImageView itemDragHandle;

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

            itemDragHandle =
                    itemView.findViewById(
                            R.id.overviewItemDragHandle);
        }
    }
}
