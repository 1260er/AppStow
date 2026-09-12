package de.pritcloud.shortcutlauncher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

final class OverviewAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SECTION = 0;
    private static final int TYPE_MESSAGE = 1;

    private final List<OverviewSection> sections;
    private final List<Row> rows = new ArrayList<>();

    OverviewAdapter(List<OverviewSection> sections) {
        this.sections = sections;
        rebuildRows();
    }

    private void rebuildRows() {
        rows.clear();

        for (OverviewSection section : sections) {
            rows.add(new Row(TYPE_SECTION, section));

            if (section.expanded) {
                rows.add(new Row(TYPE_MESSAGE, section));
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        LayoutInflater inflater =
                LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_SECTION) {
            View view = inflater.inflate(
                    R.layout.item_overview_section,
                    parent,
                    false);

            return new SectionViewHolder(view);
        }

        View view = inflater.inflate(
                R.layout.item_overview_message,
                parent,
                false);

        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position) {

        Row row = rows.get(position);
        OverviewSection section = row.section;

        if (holder instanceof SectionViewHolder) {
            SectionViewHolder sectionHolder =
                    (SectionViewHolder) holder;

            sectionHolder.title.setText(section.title);
            sectionHolder.chevron.setRotation(
                    section.expanded ? 180f : 0f);

            sectionHolder.itemView.setOnClickListener(v -> {
                section.expanded = !section.expanded;
                rebuildRows();
                notifyDataSetChanged();
            });
        } else {
            MessageViewHolder messageHolder =
                    (MessageViewHolder) holder;

            messageHolder.message.setText(
                    section.emptyMessage);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private static final class Row {
        final int type;
        final OverviewSection section;

        Row(int type, OverviewSection section) {
            this.type = type;
            this.section = section;
        }
    }

    static final class SectionViewHolder
            extends RecyclerView.ViewHolder {

        final TextView title;
        final ImageView chevron;

        SectionViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(
                    R.id.overviewSectionTitle);

            chevron = itemView.findViewById(
                    R.id.overviewSectionChevron);
        }
    }

    static final class MessageViewHolder
            extends RecyclerView.ViewHolder {

        final TextView message;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            message = itemView.findViewById(
                    R.id.overviewSectionMessage);
        }
    }
}
