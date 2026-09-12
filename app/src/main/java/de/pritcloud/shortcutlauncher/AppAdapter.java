package de.pritcloud.shortcutlauncher;

import android.content.pm.PackageManager;
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

public class AppAdapter
        extends ListAdapter<AppEntry, AppAdapter.AppViewHolder> {

    interface OnAppClickListener {
        void onAppClick(AppEntry app);
    }

    interface OnAppLongClickListener {
        void onAppLongClick(AppEntry app);
    }

    private final PackageManager packageManager;
    private final FavoritesStore favoritesStore;
    private final OnAppClickListener clickListener;
    private final OnAppLongClickListener longClickListener;

    AppAdapter(
            PackageManager packageManager,
            FavoritesStore favoritesStore,
            OnAppClickListener clickListener,
            OnAppLongClickListener longClickListener) {

        super(DIFF_CALLBACK);

        this.packageManager = packageManager;
        this.favoritesStore = favoritesStore;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    private static final DiffUtil.ItemCallback<AppEntry> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AppEntry>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull AppEntry oldItem,
                        @NonNull AppEntry newItem) {

                    return oldItem.packageName.equals(
                            newItem.packageName);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull AppEntry oldItem,
                        @NonNull AppEntry newItem) {

                    return oldItem.label.equals(newItem.label)
                            && oldItem.resolveInfo.activityInfo.name.equals(
                                    newItem.resolveInfo.activityInfo.name);
                }
            };

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);

        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull AppViewHolder holder,
            int position) {

        AppEntry app = getItem(position);

        holder.icon.setImageDrawable(
                app.resolveInfo.loadIcon(packageManager));

        holder.name.setText(app.label);
        holder.packageName.setText(app.packageName);

        updateFavoriteButton(holder, app);

        holder.favorite.setOnClickListener(v -> {
            favoritesStore.toggle(app.packageName);
            updateFavoriteButton(holder, app);
        });

        holder.itemView.setClickable(true);
        holder.itemView.setFocusable(true);

        holder.itemView.setOnClickListener(v ->
                clickListener.onAppClick(app));

        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onAppLongClick(app);
            return true;
        });
    }

    private void updateFavoriteButton(
            AppViewHolder holder,
            AppEntry app) {

        boolean favorite =
                favoritesStore.isFavorite(app.packageName);

        holder.favorite.setImageResource(
                favorite
                        ? R.drawable.ic_star_filled
                        : R.drawable.ic_star_outline);

        holder.favorite.setContentDescription(
                holder.itemView.getContext().getString(
                        favorite
                                ? R.string.action_remove_favorite
                                : R.string.action_add_favorite));
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
