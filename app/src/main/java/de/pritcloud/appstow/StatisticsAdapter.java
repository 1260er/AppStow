package de.pritcloud.appstow;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class StatisticsAdapter
        extends RecyclerView.Adapter<StatisticsAdapter.ViewHolder> {

    static final String CARD_TOTAL =
            "total";

    static final String CARD_TOP_APPS =
            "top_apps";

    static final String CARD_UNUSED_APPS =
            "unused_apps";

    static final String CARD_TOP_CATEGORIES =
            "top_categories";

    static final String CARD_TOP_SHORTCUTS =
            "top_shortcuts";

    interface OnDragStartListener {
        void onDragStart(
                RecyclerView.ViewHolder holder);
    }

    static final class Card {

        final String id;
        final String title;
        final String content;
        final boolean prominent;

        Card(
                String id,
                String title,
                String content,
                boolean prominent) {

            this.id = id;
            this.title = title;
            this.content = content;
            this.prominent = prominent;
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

        if (card.prominent) {
            holder.content.setVisibility(
                    View.GONE);

            holder.prominentContent.setVisibility(
                    View.VISIBLE);

            holder.prominentContent.setText(
                    card.content);
        } else {
            holder.prominentContent.setVisibility(
                    View.GONE);

            holder.content.setVisibility(
                    View.VISIBLE);

            holder.content.setText(
                    card.content);
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
        final TextView content;
        final TextView prominentContent;
        final ImageView dragHandle;

        ViewHolder(
                @NonNull View itemView) {

            super(itemView);

            title =
                    itemView.findViewById(
                            R.id.statisticsCardTitle);

            content =
                    itemView.findViewById(
                            R.id.statisticsCardContent);

            prominentContent =
                    itemView.findViewById(
                            R.id.statisticsCardProminentContent);

            dragHandle =
                    itemView.findViewById(
                            R.id.statisticsCardDragHandle);
        }
    }
}
