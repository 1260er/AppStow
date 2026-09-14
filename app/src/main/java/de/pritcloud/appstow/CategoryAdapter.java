package de.pritcloud.appstow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

final class CategoryAdapter
        extends ListAdapter<CategoryEntry, CategoryAdapter.CategoryViewHolder> {

    interface Listener {
        void onRename(CategoryEntry category);
        void onDelete(CategoryEntry category);
    }

    private static final DiffUtil.ItemCallback<CategoryEntry> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CategoryEntry>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull CategoryEntry oldItem,
                        @NonNull CategoryEntry newItem) {

                    return oldItem.id.equals(
                            newItem.id);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull CategoryEntry oldItem,
                        @NonNull CategoryEntry newItem) {

                    return oldItem.name.equals(
                            newItem.name)
                            && oldItem.symbol.equals(
                                    newItem.symbol);
                }
            };

    private final Listener listener;

    CategoryAdapter(Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    void setCategories(List<CategoryEntry> items) {
        submitList(
                new ArrayList<>(items));
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(
                        parent.getContext())
                .inflate(
                        R.layout.item_category,
                        parent,
                        false);

        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull CategoryViewHolder holder,
            int position) {

        CategoryEntry category =
                getItem(position);

        holder.name.setText(
                holder.itemView
                        .getContext()
                        .getString(
                                R.string.category_list_label,
                                category.symbol,
                                category.name));

        holder.name.setOnClickListener(v ->
                listener.onRename(category));

        holder.delete.setOnClickListener(v ->
                listener.onDelete(category));
    }

    static final class CategoryViewHolder
            extends RecyclerView.ViewHolder {

        final TextView name;
        final ImageButton delete;

        CategoryViewHolder(
                @NonNull View itemView) {

            super(itemView);

            name = itemView.findViewById(
                    R.id.categoryName);

            delete = itemView.findViewById(
                    R.id.categoryDelete);
        }
    }
}
