package de.pritcloud.appstow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

final class CategoryAdapter
        extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    interface Listener {
        void onRename(CategoryEntry category);
        void onDelete(CategoryEntry category);
    }

    private final List<CategoryEntry> categories =
            new ArrayList<>();

    private final Listener listener;

    CategoryAdapter(Listener listener) {
        this.listener = listener;
    }

    void setCategories(List<CategoryEntry> items) {
        categories.clear();
        categories.addAll(items);
        notifyDataSetChanged();
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
                categories.get(position);

        holder.name.setText(category.name);

        holder.name.setOnClickListener(v ->
                listener.onRename(category));

        holder.delete.setOnClickListener(v ->
                listener.onDelete(category));
    }

    @Override
    public int getItemCount() {
        return categories.size();
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
