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

    private static final int TYPE_LIST = 0;
    private static final int TYPE_GRID = 1;

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
    private boolean gridMode;

    CategoryAdapter(Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    boolean isGridMode() {
        return gridMode;
    }

    void setGridMode(
            boolean enabled) {

        if (gridMode == enabled) {
            return;
        }

        gridMode = enabled;
        notifyDataSetChanged();
    }

    void setCategories(
            List<CategoryEntry> items) {

        submitList(
                new ArrayList<>(
                        items));
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
    public CategoryViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        int layout =
                viewType == TYPE_GRID
                        ? R.layout.item_category_grid
                        : R.layout.item_category;

        View view =
                LayoutInflater.from(
                                parent.getContext())
                        .inflate(
                                layout,
                                parent,
                                false);

        UiHandedness.applyContainer(
                parent.getContext(),
                view);

        if (viewType != TYPE_GRID) {

            UiHandedness.applyTextEdge(
                    parent.getContext(),
                    view.findViewById(
                            R.id.categoryName));
        }

        return new CategoryViewHolder(
                view,
                viewType == TYPE_GRID);
    }

    @Override
    public void onBindViewHolder(
            @NonNull CategoryViewHolder holder,
            int position) {

        CategoryEntry category =
                getItem(
                        position);

        if (holder.gridTile) {

            holder.symbol.setText(
                    category.symbol);

            holder.name.setText(
                    category.name);

            holder.name.setOnClickListener(
                    null);

            holder.itemView.setOnClickListener(v ->
                    listener.onRename(
                            category));

        } else {

            holder.name.setText(
                    holder.itemView
                            .getContext()
                            .getString(
                                    R.string.category_list_label,
                                    category.symbol,
                                    category.name));

            holder.name.setOnClickListener(v ->
                    listener.onRename(
                            category));

            holder.itemView.setOnClickListener(
                    null);
        }

        holder.delete.setOnClickListener(v ->
                listener.onDelete(
                        category));
    }

    static final class CategoryViewHolder
            extends RecyclerView.ViewHolder {

        final TextView symbol;
        final TextView name;
        final ImageButton delete;
        final boolean gridTile;

        CategoryViewHolder(
                @NonNull View itemView,
                boolean gridTile) {

            super(itemView);

            this.gridTile =
                    gridTile;

            symbol =
                    itemView.findViewById(
                            R.id.categorySymbol);

            name =
                    itemView.findViewById(
                            R.id.categoryName);

            delete =
                    itemView.findViewById(
                            R.id.categoryDelete);
        }
    }
}
