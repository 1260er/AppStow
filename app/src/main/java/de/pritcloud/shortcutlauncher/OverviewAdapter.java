package de.pritcloud.shortcutlauncher;

import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

final class OverviewAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SECTION = 0;
    private static final int TYPE_MESSAGE = 1;
    private static final int TYPE_APP = 2;

    interface OnAppClickListener {
        void onAppClick(AppEntry app);
    }

    interface OnAppLongClickListener {
        void onAppLongClick(AppEntry app);
    }

    private final List<OverviewSection> sections;
    private final List<Row> rows = new ArrayList<>();
    private final List<AppEntry> favoriteApps = new ArrayList<>();

    private final PackageManager packageManager;
    private final FavoritesStore favoritesStore;
    private final OnAppClickListener clickListener;
    private final OnAppLongClickListener longClickListener;

    OverviewAdapter(
            List<OverviewSection> sections,
            PackageManager packageManager,
            FavoritesStore favoritesStore,
            OnAppClickListener clickListener,
            OnAppLongClickListener longClickListener) {

        this.sections = sections;
        this.packageManager = packageManager;
        this.favoritesStore = favoritesStore;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;

        rebuildRows();
    }

    void setFavoriteApps(List<AppEntry> apps) {
        favoriteApps.clear();
        favoriteApps.addAll(apps);

        rebuildRows();
        notifyDataSetChanged();
    }

    private void rebuildRows() {
        rows.clear();

        for (OverviewSection section : sections) {
            rows.add(Row.section(section));

            if (!section.expanded) {
                continue;
            }

            if ("favorites".equals(section.id)) {
                if (favoriteApps.isEmpty()) {
                    rows.add(Row.message(section));
                } else {
                    for (AppEntry app : favoriteApps) {
                        rows.add(Row.app(section, app));
                    }
                }
            } else {
                rows.add(Row.message(section));
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        LayoutInflater inflater =
                LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_SECTION) {
            View view = inflater.inflate(
                    R.layout.item_overview_section,
                    parent,
                    false);

            return new SectionViewHolder(view);
        }

        if (viewType == TYPE_APP) {
            View view = inflater.inflate(
                    R.layout.item_app,
                    parent,
                    false);

            return new AppViewHolder(view);
        }

        View view = inflater.inflate(
                R.layout.item_overview_message,
                parent,
                false);

        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position) {

        Row row = rows.get(position);

        if (holder instanceof SectionViewHolder) {
            bindSection(
                    (SectionViewHolder) holder,
                    row.section);
            return;
        }

        if (holder instanceof AppViewHolder) {
            bindApp(
                    (AppViewHolder) holder,
                    row.app);
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

        holder.title.setText(section.title);

        holder.chevron.setRotation(
                section.expanded ? 180f : 0f);

        holder.itemView.setOnClickListener(v -> {
            section.expanded = !section.expanded;

            rebuildRows();
            notifyDataSetChanged();
        });
    }

    private void bindApp(
            AppViewHolder holder,
            AppEntry app) {

        holder.icon.setImageDrawable(
                app.resolveInfo.loadIcon(packageManager));

        holder.name.setText(app.label);
        holder.packageName.setText(app.packageName);

        holder.favorite.setImageResource(
                R.drawable.ic_star_filled);

        holder.favorite.setContentDescription(
                holder.itemView.getContext().getString(
                        R.string.action_remove_favorite));

        holder.favorite.setOnClickListener(v -> {
            boolean stillFavorite =
                    favoritesStore.toggle(app.packageName);

            if (!stillFavorite) {
                favoriteApps.removeIf(
                        entry -> entry.packageName.equals(
                                app.packageName));

                rebuildRows();
                notifyDataSetChanged();
            }
        });

        holder.itemView.setOnClickListener(v ->
                clickListener.onAppClick(app));

        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onAppLongClick(app);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private static final class Row {

        final int type;
        final OverviewSection section;
        final AppEntry app;

        private Row(
                int type,
                OverviewSection section,
                AppEntry app) {

            this.type = type;
            this.section = section;
            this.app = app;
        }

        static Row section(OverviewSection section) {
            return new Row(
                    TYPE_SECTION,
                    section,
                    null);
        }

        static Row message(OverviewSection section) {
            return new Row(
                    TYPE_MESSAGE,
                    section,
                    null);
        }

        static Row app(
                OverviewSection section,
                AppEntry app) {

            return new Row(
                    TYPE_APP,
                    section,
                    app);
        }
    }

    static final class SectionViewHolder
            extends RecyclerView.ViewHolder {

        final TextView title;
        final ImageView chevron;

        SectionViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(
                    R.id.overviewSectionTitle);

            chevron = itemView.findViewById(
                    R.id.overviewSectionChevron);
        }
    }

    static final class MessageViewHolder
            extends RecyclerView.ViewHolder {

        final TextView message;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            message = itemView.findViewById(
                    R.id.overviewSectionMessage);
        }
    }

    static final class AppViewHolder
            extends RecyclerView.ViewHolder {

        final ImageView icon;
        final TextView name;
        final TextView packageName;
        final ImageButton favorite;

        AppViewHolder(@NonNull View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.appIcon);
            name = itemView.findViewById(R.id.appName);
            packageName = itemView.findViewById(R.id.appPackage);
            favorite = itemView.findViewById(R.id.appFavorite);
        }
    }
}
