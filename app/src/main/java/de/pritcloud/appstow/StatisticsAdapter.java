package de.pritcloud.appstow;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class StatisticsAdapter
        extends RecyclerView.Adapter<StatisticsAdapter.ViewHolder> {

    static final String CARD_TOP_SHORTCUTS =
            "top_shortcuts";

    static final String CARD_TOP_CATEGORIES =
            "top_categories";

    static final String CARD_TOP_APPS =
            "top_apps";

    static final String CARD_CUSTOM_SHORTCUTS =
            "custom_shortcuts";

    static final String CARD_UNUSED_APPS =
            "unused_apps";

    static final String CARD_TOTAL =
            "total";

    interface OnDragStartListener {
        void onDragStart(
                RecyclerView.ViewHolder holder);
    }

    static final class Row {

        final String label;
        final String count;
        final boolean showFavoriteSlot;
        final boolean favorite;

        Row(
                String label,
                String count,
                boolean showFavoriteSlot,
                boolean favorite) {

            this.label = label;
            this.count = count;
            this.showFavoriteSlot = showFavoriteSlot;
            this.favorite = favorite;
        }

        static Row message(
                String text) {

            return new Row(
                    text,
                    null,
                    false,
                    false);
        }
    }

    static final class Card {

        final String id;
        final String title;
        final List<Row> rows;
        final String prominentContent;

        Card(
                String id,
                String title,
                List<Row> rows,
                String prominentContent) {

            this.id = id;
            this.title = title;
            this.rows =
                    new ArrayList<>(
                            rows);
            this.prominentContent =
                    prominentContent;
        }
    }

    private final List<Card> cards =
            new ArrayList<>();

    private final OnDragStartListener dragStartListener;

    private boolean sortMode;

    StatisticsAdapter(
            OnDragStartListener dragStartListener) {

        this.dragStartListener =
                dragStartListener;
    }

    void setCards(
            List<Card> newCards) {

        cards.clear();
        cards.addAll(
                newCards);

        notifyDataSetChanged();
    }

    void setSortMode(
            boolean enabled) {

        if (sortMode == enabled) {
            return;
        }

        sortMode = enabled;
        notifyDataSetChanged();
    }

    boolean moveCard(
            int fromPosition,
            int toPosition) {

        if (!sortMode
                || fromPosition
                == RecyclerView.NO_POSITION
                || toPosition
                == RecyclerView.NO_POSITION
                || fromPosition < 0
                || toPosition < 0
                || fromPosition >= cards.size()
                || toPosition >= cards.size()) {

            return false;
        }

        Collections.swap(
                cards,
                fromPosition,
                toPosition);

        notifyItemMoved(
                fromPosition,
                toPosition);

        return true;
    }

    List<String> getOrder() {
        List<String> order =
                new ArrayList<>();

        for (Card card :
                cards) {

            order.add(
                    card.id);
        }

        return order;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view =
                LayoutInflater.from(
                                parent.getContext())
                        .inflate(
                                R.layout.item_statistics_card,
                                parent,
                                false);

        return new ViewHolder(
                view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        Card card =
                cards.get(
                        position);

        holder.title.setText(
                card.title);

        holder.rows.removeAllViews();

        if (card.prominentContent != null) {
            holder.rows.setVisibility(
                    View.GONE);

            holder.prominentContent.setVisibility(
                    View.VISIBLE);

            holder.prominentContent.setText(
                    card.prominentContent);

        } else {
            holder.prominentContent.setVisibility(
                    View.GONE);

            holder.rows.setVisibility(
                    View.VISIBLE);

            for (Row row :
                    card.rows) {

                View rowView =
                        LayoutInflater.from(
                                        holder.itemView.getContext())
                                .inflate(
                                        R.layout.item_statistics_row,
                                        holder.rows,
                                        false);

                TextView favorite =
                        rowView.findViewById(
                                R.id.statisticsRowFavorite);

                TextView label =
                        rowView.findViewById(
                                R.id.statisticsRowLabel);

                TextView count =
                        rowView.findViewById(
                                R.id.statisticsRowCount);

                label.setText(
                        row.label);

                favorite.setVisibility(
                        row.favorite
                                ? View.VISIBLE
                                : View.INVISIBLE);

                favorite.setText(
                        row.favorite
                                ? "★"
                                : "");

                if (row.count == null) {
                    count.setVisibility(
                            View.GONE);
                } else {
                    count.setVisibility(
                            View.VISIBLE);

                    count.setText(
                            row.count);
                }

                holder.rows.addView(
                        rowView);
            }
        }

        holder.dragHandle.setVisibility(
                sortMode
                        ? View.VISIBLE
                        : View.GONE);

        if (sortMode) {
            holder.dragHandle.setOnTouchListener(
                    (view, event) -> {

                        if (event.getActionMasked()
                                == MotionEvent.ACTION_DOWN) {

                            dragStartListener.onDragStart(
                                    holder);

                            return true;
                        }

                        return false;
                    });
        } else {
            holder.dragHandle.setOnTouchListener(
                    null);
        }
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    static final class ViewHolder
            extends RecyclerView.ViewHolder {

        final TextView title;
        final LinearLayout rows;
        final TextView prominentContent;
        final ImageView dragHandle;

        ViewHolder(
                @NonNull View itemView) {

            super(itemView);

            title =
                    itemView.findViewById(
                            R.id.statisticsCardTitle);

            rows =
                    itemView.findViewById(
                            R.id.statisticsCardRows);

            prominentContent =
                    itemView.findViewById(
                            R.id.statisticsCardProminentContent);

            dragHandle =
                    itemView.findViewById(
                            R.id.statisticsCardDragHandle);
        }
    }
}
