package de.pritcloud.appstow;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

final class EmojiSearchAdapter
        extends ListAdapter<
                EmojiSearchIndex.Result,
                EmojiSearchAdapter.Holder> {

    interface Listener {

        void onEmojiSelected(
                String emoji);
    }

    private static final DiffUtil.ItemCallback<
            EmojiSearchIndex.Result>
            DIFF_CALLBACK =
            new DiffUtil.ItemCallback<
                    EmojiSearchIndex.Result>() {

                @Override
                public boolean areItemsTheSame(
                        @NonNull EmojiSearchIndex.Result oldItem,
                        @NonNull EmojiSearchIndex.Result newItem) {

                    return oldItem.emoji.equals(
                            newItem.emoji);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull EmojiSearchIndex.Result oldItem,
                        @NonNull EmojiSearchIndex.Result newItem) {

                    return oldItem.emoji.equals(
                            newItem.emoji)
                            && oldItem.label.equals(
                                    newItem.label);
                }
            };

    private final Listener listener;

    EmojiSearchAdapter(
            Listener listener) {

        super(
                DIFF_CALLBACK);

        this.listener =
                listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        TextView view =
                (TextView)
                        LayoutInflater.from(
                                        parent.getContext())
                                .inflate(
                                        R.layout.item_emoji_search,
                                        parent,
                                        false);

        return new Holder(
                view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder holder,
            int position) {

        EmojiSearchIndex.Result item =
                getItem(
                        position);

        holder.text.setText(
                item.emoji);

        holder.text.setContentDescription(
                item.label
                        + " "
                        + item.emoji);

        holder.text.setOnClickListener(
                ignored ->
                        listener.onEmojiSelected(
                                item.emoji));
    }

    static final class Holder
            extends RecyclerView.ViewHolder {

        final TextView text;

        Holder(
                TextView text) {

            super(
                    text);

            this.text =
                    text;
        }
    }
}
