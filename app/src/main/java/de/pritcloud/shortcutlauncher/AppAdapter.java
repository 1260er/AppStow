package de.pritcloud.shortcutlauncher;

import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    private final PackageManager packageManager;
    private final OnAppClickListener clickListener;

    AppAdapter(
            PackageManager packageManager,
            OnAppClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.packageManager = packageManager;
        this.clickListener = clickListener;
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

        holder.itemView.setClickable(true);
        holder.itemView.setFocusable(true);
        holder.itemView.setOnClickListener(v ->
                clickListener.onAppClick(app));
    }

    static final class AppViewHolder
            extends RecyclerView.ViewHolder {

        final ImageView icon;
        final TextView name;
        final TextView packageName;

        AppViewHolder(@NonNull View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.appIcon);
            name = itemView.findViewById(R.id.appName);
            packageName = itemView.findViewById(R.id.appPackage);
        }
    }
}
