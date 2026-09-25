package de.pritcloud.appstow;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@SuppressLint("SetTextI18n")
final class ShortcutEditorDialog {

    private static final String STATE_EXISTING_ID =
            "shortcut_existing_id";
    private static final String STATE_NAME =
            "shortcut_name";
    private static final String STATE_TYPE =
            "shortcut_type";
    private static final String STATE_TARGET =
            "shortcut_target";
    private static final String STATE_FAVORITE =
            "shortcut_favorite";
    private static final String STATE_CATEGORIES =
            "shortcut_categories";
    private static final String STATE_PICKER_OPEN =
            "shortcut_picker_open";
    private static final String STATE_PICKER_TEMP =
            "shortcut_picker_temp";

    interface Listener {
        void onSave(
                String name,
                String type,
                String target,
                Set<String> categoryIds,
                boolean favorite);
    }

    interface DraftListener {
        void onDraftChanged(Bundle draft);
        void onClosed();
    }

    private ShortcutEditorDialog() {
    }

    static String getExistingId(
            Bundle draft) {

        return draft == null
                ? null
                : draft.getString(
                        STATE_EXISTING_ID);
    }

    static void show(
            Activity activity,
            CategoryStore categoryStore,
            ShortcutEntry existing,
            Bundle restoredState,
            Listener listener,
            DraftListener draftListener) {

        Bundle draft =
                restoredState == null
                        ? new Bundle()
                        : new Bundle(
                                restoredState);

        View view =
                LayoutInflater.from(activity)
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

        type.setAdapter(
                typeAdapter);

        Set<String> selectedCategories =
                new HashSet<>();

        String initialName;
        int initialType;
        String initialTarget;
        boolean initialFavorite;

        if (restoredState != null) {

            initialName =
                    draft.getString(
                            STATE_NAME,
                            "");

            initialType =
                    draft.getInt(
                            STATE_TYPE,
                            0);

            initialTarget =
                    draft.getString(
                            STATE_TARGET,
                            "");

            initialFavorite =
                    draft.getBoolean(
                            STATE_FAVORITE,
                            false);

            selectedCategories.addAll(
                    getStringSet(
                            draft,
                            STATE_CATEGORIES));

        } else if (existing != null) {

            draft.putString(
                    STATE_EXISTING_ID,
                    existing.id);

            initialName =
                    existing.name;

            initialType =
                    getTypeIndex(
                            existing.type);

            initialFavorite =
                    existing.favorite;

            selectedCategories.addAll(
                    existing.categoryIds);

            if (ShortcutEntry.TYPE_APP_SETTINGS.equals(
                    existing.type)) {

                initialTarget =
                        "package:"
                                + existing.target;

            } else {
                initialTarget =
                        existing.target;
            }

        } else {

            initialName =
                    "";

            initialType =
                    0;

            initialTarget =
                    "https://";

            initialFavorite =
                    false;
        }

        name.setText(
                initialName);

        type.setSelection(
                initialType);

        target.setText(
                initialTarget);

        if (!initialTarget.isEmpty()) {
            target.setSelection(
                    target.length());
        }

        favorite.setChecked(
                initialFavorite);

        updateCategoryLabel(
                activity,
                pickCategories,
                selectedCategories,
                categoryStore.getCategories());

        draft.putString(
                STATE_NAME,
                name.getText()
                        .toString());

        draft.putInt(
                STATE_TYPE,
                type.getSelectedItemPosition());

        draft.putString(
                STATE_TARGET,
                target.getText()
                        .toString());

        draft.putBoolean(
                STATE_FAVORITE,
                favorite.isChecked());

        putStringSet(
                draft,
                STATE_CATEGORIES,
                selectedCategories);

        int[] previousType = {
                type.getSelectedItemPosition()
        };

        updateTypeUi(
                activity,
                type.getSelectedItemPosition(),
                target,
                networkWarning,
                networkHelp);

        watchText(
                name,
                () -> {
                    draft.putString(
                            STATE_NAME,
                            name.getText()
                                    .toString());

                    notifyDraft(
                            draftListener,
                            draft);
                });

        watchText(
                target,
                () -> {
                    draft.putString(
                            STATE_TARGET,
                            target.getText()
                                    .toString());

                    notifyDraft(
                            draftListener,
                            draft);
                });

        favorite.setOnCheckedChangeListener(
                (buttonView, checked) -> {
                    draft.putBoolean(
                            STATE_FAVORITE,
                            checked);

                    notifyDraft(
                            draftListener,
                            draft);
                });

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

                        draft.putInt(
                                STATE_TYPE,
                                position);

                        draft.putString(
                                STATE_TARGET,
                                target.getText()
                                        .toString());

                        notifyDraft(
                                draftListener,
                                draft);
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
                        pickCategories,
                        draft,
                        draftListener,
                        null));

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
                                    Toast.LENGTH_SHORT)
                                    .show();

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

        dialog.setOnDismissListener(
                ignored -> {

                    if (view.getViewTreeObserver()
                            .isAlive()) {

                        view.getViewTreeObserver()
                                .removeOnWindowFocusChangeListener(
                                        networkFocusListener);
                    }

                    draftListener.onClosed();
                });

        notifyDraft(
                draftListener,
                draft);

        dialog.show();

        if (draft.getBoolean(
                STATE_PICKER_OPEN,
                false)) {

            showCategoryPicker(
                    activity,
                    categoryStore,
                    selectedCategories,
                    pickCategories,
                    draft,
                    draftListener,
                    getStringSet(
                            draft,
                            STATE_PICKER_TEMP));
        }
    }

    private static void showCategoryPicker(
            Activity activity,
            CategoryStore categoryStore,
            Set<String> selectedCategories,
            TextView pickCategories,
            Bundle draft,
            DraftListener draftListener,
            Set<String> restoredTemporary) {

        List<CategoryEntry> categories =
                categoryStore.getCategories();

        if (categories.isEmpty()) {
            Toast.makeText(
                    activity,
                    R.string.category_assign_none,
                    Toast.LENGTH_LONG)
                    .show();

            return;
        }

        Set<String> temporary =
                restoredTemporary == null
                        ? new HashSet<>(
                                selectedCategories)
                        : new HashSet<>(
                                restoredTemporary);

        CharSequence[] names =
                new CharSequence[
                        categories.size()];

        boolean[] checked =
                new boolean[
                        categories.size()];

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

        AlertDialog picker =
                new AlertDialog.Builder(activity)
                        .setTitle(
                                R.string.shortcut_categories)
                        .setMultiChoiceItems(
                                names,
                                checked,
                                (currentDialog,
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

                                    putStringSet(
                                            draft,
                                            STATE_PICKER_TEMP,
                                            temporary);

                                    notifyDraft(
                                            draftListener,
                                            draft);
                                })
                        .setPositiveButton(
                                R.string.action_save,
                                (currentDialog,
                                 which) -> {

                                    selectedCategories.clear();

                                    selectedCategories.addAll(
                                            temporary);

                                    putStringSet(
                                            draft,
                                            STATE_CATEGORIES,
                                            selectedCategories);

                                    updateCategoryLabel(
                                            activity,
                                            pickCategories,
                                            selectedCategories,
                                            categories);
                                })
                        .setNegativeButton(
                                R.string.action_cancel,
                                null)
                        .create();

        draft.putBoolean(
                STATE_PICKER_OPEN,
                true);

        putStringSet(
                draft,
                STATE_PICKER_TEMP,
                temporary);

        notifyDraft(
                draftListener,
                draft);

        picker.setOnDismissListener(
                ignored -> {

                    draft.putBoolean(
                            STATE_PICKER_OPEN,
                            false);

                    draft.remove(
                            STATE_PICKER_TEMP);

                    notifyDraft(
                            draftListener,
                            draft);
                });

        picker.show();
    }

    private static void watchText(
            TextView input,
            Runnable callback) {

        input.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence text,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence text,
                            int start,
                            int before,
                            int count) {
                    }

                    @Override
                    public void afterTextChanged(
                            Editable editable) {

                        callback.run();
                    }
                });
    }

    private static void notifyDraft(
            DraftListener listener,
            Bundle draft) {

        listener.onDraftChanged(
                new Bundle(
                        draft));
    }

    private static void putStringSet(
            Bundle bundle,
            String key,
            Set<String> values) {

        bundle.putStringArrayList(
                key,
                new ArrayList<>(
                        values));
    }

    private static Set<String> getStringSet(
            Bundle bundle,
            String key) {

        ArrayList<String> values =
                bundle.getStringArrayList(
                        key);

        return values == null
                ? new HashSet<>()
                : new HashSet<>(
                        values);
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
