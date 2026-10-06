package de.pritcloud.appstow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

final class ShortcutAdapter
        extends ListAdapter<ShortcutEntry, ShortcutAdapter.ViewHolder> {

    private static final int TYPE_LIST = 0;
    private static final int TYPE_GRID = 1;

    interface Listener {
        void onEdit(ShortcutEntry shortcut);
        void onDelete(ShortcutEntry shortcut);
        void onFavorite(ShortcutEntry shortcut);
    }

    private static final DiffUtil.ItemCallback<ShortcutEntry> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ShortcutEntry>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull ShortcutEntry oldItem,
                        @NonNull ShortcutEntry newItem) {

                    return oldItem.id.equals(
                            newItem.id);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull ShortcutEntry oldItem,
                        @NonNull ShortcutEntry newItem) {

                    return oldItem.name.equals(
                            newItem.name)
                            && oldItem.type.equals(
                                    newItem.type)
                            && oldItem.target.equals(
                                    newItem.target)
                            && oldItem.favorite
                            == newItem.favorite
                            && oldItem.categoryIds.equals(
                                    newItem.categoryIds);
                }
            };

    private final CategoryStore categoryStore;
    private final Listener listener;

    private boolean favoriteEditingEnabled = true;
    private boolean gridMode;

    ShortcutAdapter(
            CategoryStore categoryStore,
            Listener listener) {

        super(DIFF_CALLBACK);

        this.categoryStore =
                categoryStore;

        this.listener =
                listener;
    }

    boolean isGridMode() {
        return gridMode;
    }

    void setGridMode(
            boolean enabled) {

        if (gridMode == enabled) {
            return;
        }

        gridMode =
                enabled;

        notifyDataSetChanged();
    }

    void setFavoriteEditingEnabled(
            boolean enabled) {

        if (favoriteEditingEnabled
                == enabled) {

            return;
        }

        favoriteEditingEnabled =
                enabled;

        notifyDataSetChanged();
    }

    void setShortcuts(
            List<ShortcutEntry> items) {

        submitList(
                new ArrayList<>(
                        items));
    }

    void refreshVisibleState() {

        if (getItemCount() > 0) {

            notifyItemRangeChanged(
                    0,
                    getItemCount());
        }
    }

    @Override
    public int getItemViewType(
            int position) {

        return gridMode
                ? TYPE_GRID
                : TYPE_LIST;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        int layout =
                viewType == TYPE_GRID
                        ? R.layout.item_shortcut_management_grid
                        : R.layout.item_shortcut_management;

        View view =
                LayoutInflater.from(
                                parent.getContext())
                        .inflate(
                                layout,
                                parent,
                                false);

        return new ViewHolder(
                view,
                viewType == TYPE_GRID);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        ShortcutEntry shortcut =
                getItem(
                        position);

        holder.name.setText(
                shortcut.name);

        String categories =
                categoryStore
                        .getCategoryLabel(
                                shortcut.categoryIds);

        holder.subtitle.setText(
                categories);

        holder.subtitle.setVisibility(
                categories.isEmpty()
                        ? holder.gridTile
                                ? View.GONE
                                : View.INVISIBLE
                        : View.VISIBLE);

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

        holder.favorite.setImageResource(
                shortcut.favorite
                        ? R.drawable.ic_star_filled
                        : R.drawable.ic_star_outline);

        holder.favorite.setContentDescription(
                holder.itemView
                        .getContext()
                        .getString(
                                shortcut.favorite
                                        ? R.string.action_remove_favorite
                                        : R.string.action_add_favorite));

        holder.content.setOnClickListener(v ->
                listener.onEdit(
                        shortcut));

        holder.favorite.setVisibility(
                favoriteEditingEnabled
                        ? View.VISIBLE
                        : View.GONE);

        if (favoriteEditingEnabled) {

            holder.favorite.setOnClickListener(v -> {

                if (!shortcut.favorite) {

                    listener.onFavorite(
                            shortcut);

                    return;
                }

                FavoriteConfirmation.confirmRemoval(
                        holder.itemView.getContext(),
                        shortcut.name,
                        () ->
                                listener.onFavorite(
                                        shortcut));
            });

        } else {

            holder.favorite.setOnClickListener(
                    null);
        }

        holder.delete.setOnClickListener(v ->
                listener.onDelete(
                        shortcut));
    }

    static final class ViewHolder
            extends RecyclerView.ViewHolder {

        final ImageView icon;
        final View content;
        final TextView name;
        final TextView subtitle;
        final ImageButton favorite;
        final ImageButton delete;
        final boolean gridTile;

        ViewHolder(
                @NonNull View itemView,
                boolean gridTile) {

            super(itemView);

            this.gridTile =
                    gridTile;

            icon =
                    itemView.findViewById(
                            R.id.shortcutIcon);

            content =
                    itemView.findViewById(
                            R.id.shortcutContent);

            name =
                    itemView.findViewById(
                            R.id.shortcutName);

            subtitle =
                    itemView.findViewById(
                            R.id.shortcutSubtitle);

            favorite =
                    itemView.findViewById(
                            R.id.shortcutFavorite);

            delete =
                    itemView.findViewById(
                            R.id.shortcutDelete);
        }
    }
}
