package de.pritcloud.appstow;

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

final class ShortcutAdapter
        extends RecyclerView.Adapter<ShortcutAdapter.ViewHolder> {

    interface Listener {
        void onEdit(ShortcutEntry shortcut);
        void onDelete(ShortcutEntry shortcut);
        void onFavorite(ShortcutEntry shortcut);
    }

    private final List<ShortcutEntry> shortcuts =
            new ArrayList<>();

    private final CategoryStore categoryStore;
    private final Listener listener;

    ShortcutAdapter(
            CategoryStore categoryStore,
            Listener listener) {

        this.categoryStore = categoryStore;
        this.listener = listener;
    }

    void setShortcuts(List<ShortcutEntry> items) {
        shortcuts.clear();
        shortcuts.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(
                        parent.getContext())
                .inflate(
                        R.layout.item_shortcut_management,
                        parent,
                        false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        ShortcutEntry shortcut =
                shortcuts.get(position);

        holder.name.setText(
                shortcut.name);

        String categories =
                getCategoryLabel(shortcut);

        holder.subtitle.setText(
                categories);

        holder.subtitle.setVisibility(
                categories.isEmpty()
                        ? View.INVISIBLE
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

        holder.content.setOnClickListener(v ->
                listener.onEdit(shortcut));

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

        holder.delete.setOnClickListener(v ->
                listener.onDelete(shortcut));
    }

    private String getCategoryLabel(
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

    @Override
    public int getItemCount() {
        return shortcuts.size();
    }

    static final class ViewHolder
            extends RecyclerView.ViewHolder {

        final ImageView icon;
        final View content;
        final TextView name;
        final TextView subtitle;
        final ImageButton favorite;
        final ImageButton delete;

        ViewHolder(
                @NonNull View itemView) {

            super(itemView);

            icon = itemView.findViewById(
                    R.id.shortcutIcon);

            content = itemView.findViewById(
                    R.id.shortcutContent);

            name = itemView.findViewById(
                    R.id.shortcutName);

            subtitle = itemView.findViewById(
                    R.id.shortcutSubtitle);

            favorite = itemView.findViewById(
                    R.id.shortcutFavorite);

            delete = itemView.findViewById(
                    R.id.shortcutDelete);
        }
    }
}
