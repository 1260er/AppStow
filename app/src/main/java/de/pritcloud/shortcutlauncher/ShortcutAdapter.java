package de.pritcloud.shortcutlauncher;

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

    private final List<ShortcutEntry> shortcuts = new ArrayList<>();
    private final Listener listener;

    ShortcutAdapter(Listener listener) {
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

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shortcut_management, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        ShortcutEntry shortcut = shortcuts.get(position);

        holder.name.setText(shortcut.name);
        holder.subtitle.setText(getTypeLabel(shortcut) + " · " + shortcut.target);

        if (ShortcutEntry.TYPE_WEBSITE.equals(shortcut.type)) {
            holder.icon.setImageResource(R.drawable.ic_shortcut_web);
        } else if (ShortcutEntry.TYPE_WEB_APP.equals(shortcut.type)) {
            holder.icon.setImageResource(R.drawable.ic_shortcut_webapp);
        } else {
            holder.icon.setImageResource(R.drawable.ic_shortcut_action);
        }

        holder.favorite.setImageResource(
                shortcut.favorite
                        ? R.drawable.ic_star_filled
                        : R.drawable.ic_star_outline);

        holder.content.setOnClickListener(v -> listener.onEdit(shortcut));
        holder.favorite.setOnClickListener(v -> listener.onFavorite(shortcut));
        holder.delete.setOnClickListener(v -> listener.onDelete(shortcut));
    }

    private String getTypeLabel(ShortcutEntry shortcut) {
        if (ShortcutEntry.TYPE_WEBSITE.equals(shortcut.type)) return "Webseite";
        if (ShortcutEntry.TYPE_WEB_APP.equals(shortcut.type)) return "Web-App";
        if (ShortcutEntry.TYPE_APP_SETTINGS.equals(shortcut.type)) return "App-Einstellungen";
        return "Deep Link";
    }

    @Override
    public int getItemCount() {
        return shortcuts.size();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final View content;
        final TextView name;
        final TextView subtitle;
        final ImageButton favorite;
        final ImageButton delete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.shortcutIcon);
            content = itemView.findViewById(R.id.shortcutContent);
            name = itemView.findViewById(R.id.shortcutName);
            subtitle = itemView.findViewById(R.id.shortcutSubtitle);
            favorite = itemView.findViewById(R.id.shortcutFavorite);
            delete = itemView.findViewById(R.id.shortcutDelete);
        }
    }
}
