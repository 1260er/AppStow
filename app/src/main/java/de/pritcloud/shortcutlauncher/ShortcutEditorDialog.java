package de.pritcloud.shortcutlauncher;

import android.app.Activity;
import android.app.AlertDialog;
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
            List<AppEntry> apps,
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

        TextView pickApp =
                view.findViewById(
                        R.id.shortcutPickApp);

        TextView pickCategories =
                view.findViewById(
                        R.id.shortcutPickCategories);

        CheckBox favorite =
                view.findViewById(
                        R.id.shortcutEditFavorite);

        String[] typeLabels = {
                activity.getString(
                        R.string.shortcut_type_website),
                activity.getString(
                        R.string.shortcut_type_webapp),
                activity.getString(
                        R.string.shortcut_type_app_settings),
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

        String[] selectedPackage = {""};

        if (existing != null) {
            name.setText(existing.name);
            favorite.setChecked(existing.favorite);

            selectedCategories.addAll(
                    existing.categoryIds);

            int typeIndex =
                    getTypeIndex(existing.type);

            type.setSelection(typeIndex);

            if (isAppTargetType(typeIndex)) {
                selectedPackage[0] =
                        existing.target;
            } else {
                target.setText(
                        existing.target);
            }
        }

        updateSelectedAppLabel(
                activity,
                pickApp,
                selectedPackage[0],
                apps);

        updateCategoryLabel(
                activity,
                pickCategories,
                selectedCategories,
                categoryStore.getCategories());

        int[] previousType = {
                type.getSelectedItemPosition()
        };

        updateTypeUi(
                type.getSelectedItemPosition(),
                target,
                pickApp);

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

                        if (previousType[0] == 1
                                && position != 1
                                && "https://".equals(
                                        currentTarget)) {

                            target.setText("");
                        }

                        if (position == 1
                                && target.getText()
                                        .toString()
                                        .trim()
                                        .isEmpty()) {

                            target.setText(
                                    "https://");

                            target.setSelection(
                                    target.length());
                        }

                        previousType[0] =
                                position;

                        updateTypeUi(
                                position,
                                target,
                                pickApp);
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {
                    }
                });

        pickApp.setOnClickListener(v ->
                showAppPicker(
                        activity,
                        apps,
                        selectedPackage,
                        pickApp));

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

                        int typeIndex =
                                type.getSelectedItemPosition();

                        String shortcutType =
                                getType(typeIndex);

                        String shortcutTarget =
                                isAppTargetType(typeIndex)
                                        ? selectedPackage[0]
                                        : target.getText()
                                                .toString()
                                                .trim();

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

        dialog.show();
    }

    private static void updateTypeUi(
            int typeIndex,
            EditText target,
            TextView pickApp) {

        boolean appTarget =
                isAppTargetType(typeIndex);

        target.setVisibility(
                appTarget
                        ? View.GONE
                        : View.VISIBLE);

        pickApp.setVisibility(
                appTarget
                        ? View.VISIBLE
                        : View.GONE);

        if (!appTarget) {
            target.setHint(
                    typeIndex == 3
                            ? R.string.shortcut_target_deep_link_hint
                            : R.string.shortcut_target_url_hint);
        }
    }

    private static void showAppPicker(
            Activity activity,
            List<AppEntry> apps,
            String[] selectedPackage,
            TextView pickApp) {

        if (apps.isEmpty()) {
            Toast.makeText(
                    activity,
                    R.string.shortcut_no_apps,
                    Toast.LENGTH_SHORT).show();

            return;
        }

        CharSequence[] names =
                new CharSequence[
                        apps.size()];

        for (int i = 0;
             i < apps.size();
             i++) {

            AppEntry app =
                    apps.get(i);

            names[i] =
                    app.label
                            + " · "
                            + app.packageName;
        }

        new AlertDialog.Builder(activity)
                .setTitle(
                        R.string.shortcut_pick_app)
                .setItems(
                        names,
                        (dialog, which) -> {
                            AppEntry app =
                                    apps.get(which);

                            selectedPackage[0] =
                                    app.packageName;

                            updateSelectedAppLabel(
                                    activity,
                                    pickApp,
                                    selectedPackage[0],
                                    apps);
                        })
                .setNegativeButton(
                        R.string.action_cancel,
                        null)
                .show();
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

    private static void updateSelectedAppLabel(
            Activity activity,
            TextView view,
            String packageName,
            List<AppEntry> apps) {

        if (packageName == null
                || packageName.isEmpty()) {

            view.setText(
                    R.string.shortcut_pick_app);

            return;
        }

        String label =
                packageName;

        for (AppEntry app : apps) {
            if (app.packageName.equals(
                    packageName)) {

                label =
                        app.label;

                break;
            }
        }

        view.setText(
                activity.getString(
                        R.string.shortcut_app_selected,
                        label));
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

            if (scheme == null) {
                return false;
            }

            scheme =
                    scheme.toLowerCase(
                            Locale.ROOT);

            return "http".equals(scheme)
                    || "https".equals(scheme);
        }

        if (ShortcutEntry.TYPE_DEEP_LINK.equals(type)) {
            return Uri.parse(target)
                    .getScheme() != null;
        }

        return true;
    }

    private static boolean isAppTargetType(
            int typeIndex) {

        return typeIndex == 2;
    }

    private static int getTypeIndex(
            String type) {

        if (ShortcutEntry.TYPE_WEB_APP.equals(type)) {
            return 1;
        }

        if (ShortcutEntry.TYPE_APP_SETTINGS.equals(type)) {
            return 2;
        }

        if (ShortcutEntry.TYPE_DEEP_LINK.equals(type)) {
            return 3;
        }

        return 0;
    }

    private static String getType(
            int typeIndex) {

        switch (typeIndex) {
            case 1:
                return ShortcutEntry.TYPE_WEB_APP;

            case 2:
                return ShortcutEntry.TYPE_APP_SETTINGS;

            case 3:
                return ShortcutEntry.TYPE_DEEP_LINK;

            default:
                return ShortcutEntry.TYPE_WEBSITE;
        }
    }
}
