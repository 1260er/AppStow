package de.pritcloud.appstow;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Paint;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@SuppressLint("SetTextI18n")
final class ShortcutEditorDialog {

    interface Listener {
        void onSave(
                String name,
                String type,
                String target,
                Set<String> categoryIds,
                boolean favorite);
    }

    private ShortcutEditorDialog() {
    }

    static void show(
            Activity activity,
            CategoryStore categoryStore,
            ShortcutEntry existing,
            Listener listener) {

        View view = LayoutInflater.from(activity)
                .inflate(
                        R.layout.dialog_shortcut_edit,
                        null,
                        false);

        EditText name =
                view.findViewById(
                        R.id.shortcutEditName);

        Spinner type =
                view.findViewById(
                        R.id.shortcutEditType);

        EditText target =
                view.findViewById(
                        R.id.shortcutEditTarget);

        TextView networkWarning =
                view.findViewById(
                        R.id.shortcutNetworkWarning);

        TextView networkHelp =
                view.findViewById(
                        R.id.shortcutNetworkHelp);

        TextView pickCategories =
                view.findViewById(
                        R.id.shortcutPickCategories);

        CheckBox favorite =
                view.findViewById(
                        R.id.shortcutEditFavorite);

        networkHelp.setPaintFlags(
                networkHelp.getPaintFlags()
                        | Paint.UNDERLINE_TEXT_FLAG);

        networkHelp.setOnClickListener(v ->
                NetworkAccess.showNetworkHelp(
                        activity));

        String[] typeLabels = {
                activity.getString(
                        R.string.shortcut_type_website),
                activity.getString(
                        R.string.shortcut_type_webapp),
                activity.getString(
                        R.string.shortcut_type_deep_link)
        };

        ArrayAdapter<String> typeAdapter =
                new ArrayAdapter<>(
                        activity,
                        android.R.layout.simple_spinner_item,
                        typeLabels);

        typeAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        type.setAdapter(typeAdapter);

        Set<String> selectedCategories =
                new HashSet<>();

        if (existing != null) {
            name.setText(
                    existing.name);

            favorite.setChecked(
                    existing.favorite);

            selectedCategories.addAll(
                    existing.categoryIds);

            int typeIndex =
                    getTypeIndex(
                            existing.type);

            type.setSelection(
                    typeIndex);

            if (ShortcutEntry.TYPE_APP_SETTINGS.equals(
                    existing.type)) {

                target.setText(
                        "package:"
                                + existing.target);

            } else {
                target.setText(
                        existing.target);
            }

        } else {
            target.setText(
                    "https://");

            target.setSelection(
                    target.length());
        }

        updateCategoryLabel(
                activity,
                pickCategories,
                selectedCategories,
                categoryStore.getCategories());

        int[] previousType = {
                type.getSelectedItemPosition()
        };

        updateTypeUi(
                activity,
                type.getSelectedItemPosition(),
                target,
                networkWarning,
                networkHelp);

        type.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View selectedView,
                            int position,
                            long id) {

                        String currentTarget =
                                target.getText()
                                        .toString()
                                        .trim();

                        if (isWebType(
                                previousType[0])
                                && !isWebType(position)
                                && "https://".equals(
                                        currentTarget)) {

                            target.setText("");
                        }

                        if (isWebType(position)
                                && target.getText()
                                        .toString()
                                        .trim()
                                        .isEmpty()
                                && !(position == 1
                                && !NetworkAccess.hasUsableNetwork(
                                        activity))) {

                            target.setText(
                                    "https://");

                            target.setSelection(
                                    target.length());
                        }

                        previousType[0] =
                                position;

                        updateTypeUi(
                                activity,
                                position,
                                target,
                                networkWarning,
                                networkHelp);
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {
                    }
                });

        pickCategories.setOnClickListener(v ->
                showCategoryPicker(
                        activity,
                        categoryStore,
                        selectedCategories,
                        pickCategories));

        AlertDialog dialog =
                new AlertDialog.Builder(activity)
                        .setTitle(
                                existing == null
                                        ? R.string.shortcut_add_title
                                        : R.string.shortcut_edit_title)
                        .setView(view)
                        .setPositiveButton(
                                R.string.action_save,
                                null)
                        .setNegativeButton(
                                R.string.action_cancel,
                                null)
                        .create();

        dialog.setOnShowListener(ignored -> {
            int neutralColor =
                    ContextCompat.getColor(
                            activity,
                            R.color.ui_text_primary);

            dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(
                            neutralColor);

            dialog.getButton(
                            AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(
                            neutralColor);

            dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {

                        String shortcutName =
                                name.getText()
                                        .toString()
                                        .trim();

                        String shortcutType =
                                getType(
                                        type.getSelectedItemPosition());

                        String shortcutTarget =
                                target.getText()
                                        .toString()
                                        .trim();

                        if (ShortcutEntry.TYPE_WEB_APP.equals(
                                shortcutType)
                                && !NetworkAccess.hasUsableNetwork(
                                        activity)) {

                            updateTypeUi(
                                    activity,
                                    type.getSelectedItemPosition(),
                                    target,
                                    networkWarning,
                                    networkHelp);

                            Toast.makeText(
                                    activity,
                                    R.string.webapp_network_required,
                                    Toast.LENGTH_SHORT)
                                    .show();

                            return;
                        }

                        if (!isValid(
                                shortcutName,
                                shortcutType,
                                shortcutTarget)) {

                            Toast.makeText(
                                    activity,
                                    R.string.shortcut_invalid,
                                    Toast.LENGTH_SHORT).show();

                            return;
                        }

                        listener.onSave(
                                shortcutName,
                                shortcutType,
                                shortcutTarget,
                                new HashSet<>(
                                        selectedCategories),
                                favorite.isChecked());

                        dialog.dismiss();
                    });
        });

        android.view.ViewTreeObserver.OnWindowFocusChangeListener
                networkFocusListener =
                hasFocus -> {

                    if (hasFocus) {
                        updateTypeUi(
                                activity,
                                type.getSelectedItemPosition(),
                                target,
                                networkWarning,
                                networkHelp);
                    }
                };

        view.getViewTreeObserver()
                .addOnWindowFocusChangeListener(
                        networkFocusListener);

        dialog.setOnDismissListener(ignored -> {

            if (view.getViewTreeObserver()
                    .isAlive()) {

                view.getViewTreeObserver()
                        .removeOnWindowFocusChangeListener(
                                networkFocusListener);
            }
        });

        dialog.show();
    }

    private static void updateTypeUi(
            Activity activity,
            int typeIndex,
            EditText target,
            TextView networkWarning,
            TextView networkHelp) {

        target.setHint(
                typeIndex == 2
                        ? R.string.shortcut_target_deep_link_hint
                        : R.string.shortcut_target_url_hint);

        boolean missingNetwork =
                typeIndex == 1
                        && !NetworkAccess.hasUsableNetwork(
                                activity);

        if (missingNetwork) {

            if ("https://".equals(
                    target.getText()
                            .toString()
                            .trim())) {

                target.setText("");
            }

            target.setVisibility(
                    View.GONE);

            networkWarning.setVisibility(
                    View.VISIBLE);

            networkHelp.setVisibility(
                    View.VISIBLE);

            return;
        }

        networkWarning.setVisibility(
                View.GONE);

        networkHelp.setVisibility(
                View.GONE);

        target.setVisibility(
                View.VISIBLE);

        if (isWebType(typeIndex)
                && target.getText()
                        .toString()
                        .trim()
                        .isEmpty()) {

            target.setText(
                    "https://");

            target.setSelection(
                    target.length());
        }
    }

    private static void showCategoryPicker(
            Activity activity,
            CategoryStore categoryStore,
            Set<String> selectedCategories,
            TextView pickCategories) {

        List<CategoryEntry> categories =
                categoryStore.getCategories();

        if (categories.isEmpty()) {
            Toast.makeText(
                    activity,
                    R.string.category_assign_none,
                    Toast.LENGTH_LONG).show();

            return;
        }

        CharSequence[] names =
                new CharSequence[
                        categories.size()];

        boolean[] checked =
                new boolean[
                        categories.size()];

        Set<String> temporary =
                new HashSet<>(
                        selectedCategories);

        for (int i = 0;
             i < categories.size();
             i++) {

            CategoryEntry category =
                    categories.get(i);

            names[i] =
                    category.name;

            checked[i] =
                    temporary.contains(
                            category.id);
        }

        new AlertDialog.Builder(activity)
                .setTitle(
                        R.string.shortcut_categories)
                .setMultiChoiceItems(
                        names,
                        checked,
                        (dialog,
                         which,
                         isChecked) -> {

                            String id =
                                    categories
                                            .get(which)
                                            .id;

                            if (isChecked) {
                                temporary.add(id);
                            } else {
                                temporary.remove(id);
                            }
                        })
                .setPositiveButton(
                        R.string.action_save,
                        (dialog, which) -> {
                            selectedCategories.clear();

                            selectedCategories.addAll(
                                    temporary);

                            updateCategoryLabel(
                                    activity,
                                    pickCategories,
                                    selectedCategories,
                                    categories);
                        })
                .setNegativeButton(
                        R.string.action_cancel,
                        null)
                .show();
    }

    private static void updateCategoryLabel(
            Activity activity,
            TextView view,
            Set<String> selectedIds,
            List<CategoryEntry> categories) {

        if (selectedIds.isEmpty()) {
            view.setText(
                    R.string.shortcut_categories);

            return;
        }

        StringBuilder label =
                new StringBuilder();

        for (CategoryEntry category :
                categories) {

            if (!selectedIds.contains(
                    category.id)) {

                continue;
            }

            if (label.length() > 0) {
                label.append(" · ");
            }

            label.append(
                    category.name);
        }

        view.setText(
                activity.getString(
                        R.string.shortcut_categories_selected,
                        label.toString()));
    }

    private static boolean isValid(
            String name,
            String type,
            String target) {

        if (name.isEmpty()
                || target.isEmpty()) {

            return false;
        }

        if (ShortcutEntry.TYPE_WEBSITE.equals(type)
                || ShortcutEntry.TYPE_WEB_APP.equals(type)) {

            Uri uri =
                    Uri.parse(target);

            String scheme =
                    uri.getScheme();

            String host =
                    uri.getHost();

            if (scheme == null
                    || host == null
                    || host.isEmpty()) {

                return false;
            }

            scheme =
                    scheme.toLowerCase(
                            Locale.ROOT);

            if (ShortcutEntry.TYPE_WEB_APP.equals(
                    type)) {

                return "https".equals(
                        scheme);
            }

            return "http".equals(scheme)
                    || "https".equals(scheme);
        }

        return Uri.parse(target)
                .getScheme() != null;
    }

    private static boolean isWebType(
            int typeIndex) {

        return typeIndex == 0
                || typeIndex == 1;
    }

    private static int getTypeIndex(
            String type) {

        if (ShortcutEntry.TYPE_WEB_APP.equals(type)) {
            return 1;
        }

        if (ShortcutEntry.TYPE_DEEP_LINK.equals(type)
                || ShortcutEntry.TYPE_APP_SETTINGS.equals(type)) {

            return 2;
        }

        return 0;
    }

    private static String getType(
            int typeIndex) {

        switch (typeIndex) {
            case 1:
                return ShortcutEntry.TYPE_WEB_APP;

            case 2:
                return ShortcutEntry.TYPE_DEEP_LINK;

            default:
                return ShortcutEntry.TYPE_WEBSITE;
        }
    }
}
