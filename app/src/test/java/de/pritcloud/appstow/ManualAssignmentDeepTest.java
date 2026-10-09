package de.pritcloud.appstow;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class ManualAssignmentDeepTest {

    private Context context;

    @Before
    public void clearStores() {

        context = RuntimeEnvironment.getApplication();

        for (String name : new String[]{
                "categories",
                "favorites",
                "shortcuts",
                "overview_order",
                "section_item_order",
                "sorting_settings"
        }) {
            context.getSharedPreferences(
                    name, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit();
        }
    }

    @Test
    public void multiCategoryAssignmentsSurviveReloadAndDeletion() {

        CategoryStore store = new CategoryStore(context);

        assertTrue(store.addCategory("Work", "W"));
        assertTrue(store.addCategory("Private", "P"));

        String work = store.getCategories().get(0).id;
        String privateId = store.getCategories().get(1).id;

        store.setAssignedCategoryIds(
                "com.example.one",
                Set.of(work, privateId));

        store.setAssignedCategoryIds(
                "com.example.two",
                Set.of(privateId));

        CategoryStore reloaded = new CategoryStore(context);

        assertEquals(
                Set.of(work, privateId),
                reloaded.getAssignedCategoryIds(
                        "com.example.one"));

        assertEquals(
                Set.of(privateId),
                reloaded.getAssignedCategoryIds(
                        "com.example.two"));

        assertTrue(reloaded.renameCategory(
                work, "Office", "O"));

        assertEquals(
                Set.of(work, privateId),
                new CategoryStore(context)
                        .getAssignedCategoryIds(
                                "com.example.one"));

        reloaded.deleteCategory(privateId);

        CategoryStore afterDeletion =
                new CategoryStore(context);

        assertEquals(
                Set.of(work),
                afterDeletion.getAssignedCategoryIds(
                        "com.example.one"));

        assertTrue(
                afterDeletion.getAssignedCategoryIds(
                        "com.example.two").isEmpty());

        assertEquals(
                1,
                afterDeletion.getCategories().size());

        assertEquals(
                "Office",
                afterDeletion.getCategories().get(0).name);
    }

    @Test
    public void removingSingleAssignmentKeepsOtherAppsUntouched() {

        CategoryStore store = new CategoryStore(context);

        assertTrue(store.addCategory("A", "A"));
        assertTrue(store.addCategory("B", "B"));

        String a = store.getCategories().get(0).id;
        String b = store.getCategories().get(1).id;

        store.setAssignedCategoryIds(
                "com.example.one", Set.of(a, b));

        store.setAssignedCategoryIds(
                "com.example.two", Set.of(b));

        store.setAssignedCategoryIds(
                "com.example.one", Set.of(a));

        CategoryStore reloaded = new CategoryStore(context);

        assertEquals(
                Set.of(a),
                reloaded.getAssignedCategoryIds(
                        "com.example.one"));

        assertEquals(
                Set.of(b),
                reloaded.getAssignedCategoryIds(
                        "com.example.two"));

        assertFalse(
                reloaded.hasAssignments(
                        "com.example.missing"));
    }

    @Test
    public void manualOrdersRemainIndependentAndPersistent() {

        OverviewOrderStore sections =
                new OverviewOrderStore(context);

        sections.saveOrderIds(Arrays.asList(
                "shortcuts",
                "category:work",
                "favorites"));

        SectionItemOrderStore items =
                new SectionItemOrderStore(context);

        items.saveOrder(
                "category:work",
                Arrays.asList(
                        "shortcut:portal",
                        "app:calendar",
                        "app:mail"));

        items.saveOrder(
                "favorites",
                Arrays.asList(
                        "app:mail",
                        "shortcut:portal"));

        assertEquals(
                Arrays.asList(
                        "shortcuts",
                        "category:work",
                        "favorites"),
                new OverviewOrderStore(context).getOrder());

        SectionItemOrderStore reloaded =
                new SectionItemOrderStore(context);

        assertEquals(
                Arrays.asList(
                        "shortcut:portal",
                        "app:calendar",
                        "app:mail"),
                reloaded.getOrderedIds(
                        "category:work",
                        Arrays.asList(
                                "app:mail",
                                "app:calendar",
                                "shortcut:portal")));

        assertEquals(
                Arrays.asList(
                        "app:mail",
                        "shortcut:portal"),
                reloaded.getOrderedIds(
                        "favorites",
                        Arrays.asList(
                                "shortcut:portal",
                                "app:mail")));

        assertEquals(
                Arrays.asList(
                        "shortcut:portal",
                        "app:mail",
                        "app:new"),
                reloaded.getOrderedIds(
                        "category:work",
                        Arrays.asList(
                                "shortcut:portal",
                                "app:mail",
                                "app:new")));

        assertEquals(
                Arrays.asList(
                        "shortcuts",
                        "category:work",
                        "favorites"),
                sections.getOrder());
    }

    @Test
    public void favoritesAndShortcutAssignmentsSurviveReload() {

        CategoryStore categories =
                new CategoryStore(context);

        assertTrue(categories.addCategory("Work", "W"));

        String work = categories.getCategories().get(0).id;

        categories.setAssignedCategoryIds(
                "com.example.mail",
                Set.of(work));

        FavoritesStore favorites =
                new FavoritesStore(context);

        favorites.replaceAll(
                Set.of("com.example.mail"));

        ShortcutStore shortcuts =
                new ShortcutStore(context);

        shortcuts.add(
                "Portal",
                ShortcutEntry.TYPE_DEEP_LINK,
                "whatsapp://send?text=hello",
                Set.of(work),
                true);

        assertTrue(
                new FavoritesStore(context)
                        .isFavorite("com.example.mail"));

        ShortcutEntry restored =
                new ShortcutStore(context)
                        .getShortcuts().get(0);

        assertTrue(restored.favorite);
        assertEquals("Portal", restored.name);
        assertEquals(
                "whatsapp://send?text=hello",
                restored.target);
        assertEquals(Set.of(work), restored.categoryIds);

        favorites.toggle("com.example.mail");

        assertFalse(
                new FavoritesStore(context)
                        .isFavorite("com.example.mail"));

        assertEquals(
                Set.of(work),
                new CategoryStore(context)
                        .getAssignedCategoryIds(
                                "com.example.mail"));
    }

    @Test
    public void sortingSettingsDoNotOverwriteManualStores() {

        CategoryStore categories =
                new CategoryStore(context);

        assertTrue(categories.addCategory("Work", "W"));

        String work = categories.getCategories().get(0).id;

        categories.setAssignedCategoryIds(
                "com.example.mail",
                Set.of(work));

        OverviewOrderStore overview =
                new OverviewOrderStore(context);

        overview.saveOrderIds(
                Arrays.asList(
                        "category:" + work,
                        "favorites",
                        "shortcuts"));

        SectionItemOrderStore items =
                new SectionItemOrderStore(context);

        items.saveOrder(
                "favorites",
                Arrays.asList(
                        "app:mail",
                        "app:calendar"));

        SortingSettingsStore settings =
                new SortingSettingsStore(context);

        settings.save(
                new SortingSettingsStore.Settings(
                        SortingSettingsStore.Mode.SEMI_AUTOMATIC,
                        true,
                        false,
                        true,
                        false,
                        false,
                        8,
                        21,
                        10,
                        false));

        assertEquals(
                SortingSettingsStore.Mode.SEMI_AUTOMATIC,
                new SortingSettingsStore(context)
                        .load().mode);

        settings.save(
                SortingSettingsStore.Settings.defaults());

        assertEquals(
                SortingSettingsStore.Mode.MANUAL,
                new SortingSettingsStore(context)
                        .load().mode);

        assertEquals(
                Set.of(work),
                new CategoryStore(context)
                        .getAssignedCategoryIds(
                                "com.example.mail"));

        assertEquals(
                Arrays.asList(
                        "category:" + work,
                        "favorites",
                        "shortcuts"),
                new OverviewOrderStore(context).getOrder());

        assertEquals(
                Arrays.asList(
                        "app:mail",
                        "app:calendar"),
                new SectionItemOrderStore(context)
                        .getOrderedIds(
                                "favorites",
                                Arrays.asList(
                                        "app:calendar",
                                        "app:mail")));
    }
}
