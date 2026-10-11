package de.pritcloud.appstow;

import android.content.Context;
import android.content.pm.ResolveInfo;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class ManualOverviewOrderingTest {

    private Context context;
    private ShortcutStore shortcuts;
    private SectionItemOrderStore itemOrders;
    private OverviewAdapter adapter;
    private OverviewSection favorites;
    private OverviewSection ownShortcuts;
    private List<String> itemIds;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        for (String name : new String[]{
                "shortcuts", "favorites", "categories", "section_item_order",
                "overview_order", "sorting_settings", "usage_statistics"}) {
            context.getSharedPreferences(name, Context.MODE_PRIVATE)
                    .edit().clear().commit();
        }
        new SortingSettingsStore(context).save(
                SortingSettingsStore.Settings.defaults());

        shortcuts = new ShortcutStore(context);
        shortcuts.add("Alpha", ShortcutEntry.TYPE_WEBSITE,
                "https://example.org/a", Collections.emptySet(), true);
        shortcuts.add("Beta", ShortcutEntry.TYPE_WEBSITE,
                "https://example.org/b", Collections.emptySet(), true);
        shortcuts.add("Gamma", ShortcutEntry.TYPE_WEBSITE,
                "https://example.org/c", Collections.emptySet(), true);

        itemIds = new ArrayList<>();
        for (ShortcutEntry shortcut : shortcuts.getShortcuts()) {
            itemIds.add(SectionItemOrderStore.shortcutItemId(shortcut.id));
        }

        itemOrders = new SectionItemOrderStore(context);
        favorites = new OverviewSection("favorites", "Favorites", "Empty");
        ownShortcuts = new OverviewSection("shortcuts", "Shortcuts", "Empty");
        favorites.expanded = true;
        ownShortcuts.expanded = true;

        adapter = new OverviewAdapter(
                new ArrayList<>(Arrays.asList(favorites, ownShortcuts)),
                new FavoritesStore(context),
                new CategoryStore(context),
                null,
                shortcuts,
                itemOrders,
                (section, app) -> {},
                (section, shortcut) -> {},
                shortcut -> {},
                app -> {},
                holder -> {},
                section -> {},
                () -> {},
                section -> {});
        adapter.setApps(Collections.emptyList());
    }

    private static void setItemSortSection(OverviewAdapter target, String id)
            throws Exception {
        Field field = OverviewAdapter.class.getDeclaredField("itemSortSectionId");
        field.setAccessible(true);
        field.set(target, id);
    }

    private List<String> expectedOrder() {
        return Arrays.asList(itemIds.get(1), itemIds.get(2), itemIds.get(0));
    }

    private void moveFirstToLastInSection(String sectionId) throws Exception {
        if ("favorites".equals(sectionId)) {
            ownShortcuts.expanded = false;
        } else {
            favorites.expanded = false;
        }
        adapter.refreshAppRows();
        setItemSortSection(adapter, sectionId);

        // Beide Bereichsueberschriften bleiben vorhanden.
        assertEquals(5, adapter.getItemCount());

        int firstEntry =
                "favorites".equals(sectionId) ? 1 : 2;

        // Den ersten Eintrag wirklich ans Ende verschieben.
        assertTrue(adapter.moveItem(
                firstEntry,
                firstEntry + 2));

        assertEquals(expectedOrder(),
                itemOrders.getOrderedIds(sectionId, itemIds));
    }

    @Test
    public void listFavoritesPersistBeforeDoneAndAfterRefresh() throws Exception {
        moveFirstToLastInSection("favorites");
        adapter.setApps(Collections.emptyList());
        adapter.finishItemSortMode();
        assertEquals(expectedOrder(),
                itemOrders.getOrderedIds("favorites", itemIds));
    }

    @Test
    public void gridFavoritesPersistBeforeDoneAndAfterRefresh() throws Exception {
        adapter.setGridMode(true);
        adapter.openGridSection(favorites);
        setItemSortSection(adapter, "favorites");
        assertEquals(4, adapter.getItemCount());
        assertTrue(adapter.moveItem(1, 3));
        assertEquals(expectedOrder(),
                itemOrders.getOrderedIds("favorites", itemIds));
        adapter.setApps(Collections.emptyList());
        adapter.finishItemSortMode();
        assertEquals(expectedOrder(),
                itemOrders.getOrderedIds("favorites", itemIds));
    }

    @Test
    public void ownShortcutsPersistBeforeDoneAndAfterRefresh() throws Exception {
        moveFirstToLastInSection("shortcuts");
        adapter.refreshAppRows();
        adapter.finishItemSortMode();
        assertEquals(expectedOrder(),
                itemOrders.getOrderedIds("shortcuts", itemIds));
    }

    @Test
    public void gridOwnShortcutsPersistAfterRefresh() throws Exception {
        adapter.setGridMode(true);
        adapter.openGridSection(ownShortcuts);
        setItemSortSection(adapter, "shortcuts");
        assertEquals(4, adapter.getItemCount());
        assertTrue(adapter.moveItem(1, 3));
        assertEquals(expectedOrder(),
                itemOrders.getOrderedIds("shortcuts", itemIds));
        adapter.setApps(Collections.emptyList());
        adapter.finishItemSortMode();
        assertEquals(expectedOrder(),
                itemOrders.getOrderedIds("shortcuts", itemIds));
    }

    @Test
    public void categoryShortcutsKeepManualOrderAfterRefresh() throws Exception {
        CategoryStore categories = new CategoryStore(context);
        assertTrue(categories.addCategory("Web", "📁"));
        String id = categories.getCategories().get(0).id;
        for (ShortcutEntry shortcut : shortcuts.getShortcuts()) {
            shortcuts.update(shortcut.id, shortcut.name, shortcut.type,
                    shortcut.target, Collections.singleton(id), shortcut.favorite);
        }
        OverviewSection section = new OverviewSection(
                "category:" + id, "Web", "Empty");
        section.expanded = true;
        OverviewAdapter categoryAdapter = new OverviewAdapter(
                new ArrayList<>(Collections.singletonList(section)),
                new FavoritesStore(context), categories, null,
                shortcuts, itemOrders,
                (current, app) -> {}, (current, shortcut) -> {},
                shortcut -> {}, app -> {}, holder -> {}, current -> {},
                () -> {}, current -> {});
        categoryAdapter.setApps(Collections.emptyList());
        setItemSortSection(categoryAdapter, section.id);
        assertEquals(4, categoryAdapter.getItemCount());
        assertTrue(categoryAdapter.moveItem(1, 3));
        assertEquals(expectedOrder(),
                itemOrders.getOrderedIds(section.id, itemIds));
        categoryAdapter.refreshAppRows();
        categoryAdapter.finishItemSortMode();
        assertEquals(expectedOrder(),
                itemOrders.getOrderedIds(section.id, itemIds));
    }

    @Test
    public void categoryAppsKeepManualOrderAfterRefresh() throws Exception {
        CategoryStore categories = new CategoryStore(context);
        assertTrue(categories.addCategory("Apps", "📁"));
        String id = categories.getCategories().get(0).id;
        List<AppEntry> apps = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (int index = 0; index < 3; index++) {
            String pkg = "test.manual.category." + index;
            categories.setAssignedCategoryIds(pkg, Collections.singleton(id));
            apps.add(new AppEntry("App " + index, pkg,
                    new ResolveInfo(), null, 1));
            ids.add(SectionItemOrderStore.appItemId(pkg));
        }
        OverviewSection section = new OverviewSection(
                "category:" + id, "Apps", "Empty");
        section.expanded = true;
        OverviewAdapter categoryAdapter = new OverviewAdapter(
                new ArrayList<>(Collections.singletonList(section)),
                new FavoritesStore(context), categories, null,
                shortcuts, itemOrders,
                (current, app) -> {}, (current, shortcut) -> {},
                shortcut -> {}, app -> {}, holder -> {}, current -> {},
                () -> {}, current -> {});
        categoryAdapter.setApps(apps);
        setItemSortSection(categoryAdapter, section.id);
        assertEquals(4, categoryAdapter.getItemCount());
        assertTrue(categoryAdapter.moveItem(1, 3));
        List<String> wanted = Arrays.asList(ids.get(1), ids.get(2), ids.get(0));
        assertEquals(wanted, itemOrders.getOrderedIds(section.id, ids));
        categoryAdapter.setApps(apps);
        categoryAdapter.finishItemSortMode();
        assertEquals(wanted, itemOrders.getOrderedIds(section.id, ids));
    }

    @Test
    public void favoriteRemovalKeepsPreviouslySavedOrder() throws Exception {
        moveFirstToLastInSection("favorites");
        shortcuts.toggleFavorite(itemIds.get(1).substring("shortcut:".length()));
        adapter.refreshAppRows();
        adapter.finishItemSortMode();
        assertEquals(Arrays.asList(itemIds.get(2), itemIds.get(0)),
                itemOrders.getOrderedIds("favorites",
                        Arrays.asList(itemIds.get(0), itemIds.get(2))));
    }

    private static Object field(Object object, String name) throws Exception {
        Field result = object.getClass().getDeclaredField(name);
        result.setAccessible(true);
        return result.get(object);
    }

    private static Object call(Object object, String name,
                               Class<?>[] types, Object... args) throws Exception {
        Method method = object.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(object, args);
    }

    private void checkSectionDrag(boolean gridMode) {
        // Activity-Integration: Nur sie besitzt den OverviewOrderStore und
        // kann jeden Kategorien-Drag sofort dauerhaft speichern.
        Context app = ApplicationProvider.getApplicationContext();
        app.getSharedPreferences("categories", Context.MODE_PRIVATE)
                .edit().clear().commit();
        CategoryStore categories = new CategoryStore(app);
        assertTrue(categories.addCategory("One", "📁"));
        assertTrue(categories.addCategory("Two", "📁"));
        assertTrue(categories.addCategory("Three", "📁"));
        List<CategoryEntry> entries = categories.getCategories();
        List<String> expected = Arrays.asList(
                "favorites", "category:" + entries.get(1).id,
                "category:" + entries.get(2).id,
                "category:" + entries.get(0).id, "shortcuts");

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    OverviewAdapter a = (OverviewAdapter) field(
                            activity, "overviewAdapter");
                    @SuppressWarnings("unchecked")
                    List<OverviewSection> sections =
                            (List<OverviewSection>) field(activity, "overviewSections");
                    assertEquals(5, sections.size());
                    if (gridMode) {
                        a.setGridMode(true);
                    }
                    a.setSortMode(true);

                    // Ein Drag von Position 1 auf 3 MUSS die anderen
                    // Kategorien nach vorne verschieben, nicht vertauschen.
                    assertTrue((Boolean) call(activity, "moveOverviewRow",
                            new Class<?>[]{int.class, int.class}, 1, 3));
                    assertEquals(expected, new OverviewOrderStore(app).getOrder());
                    List<String> actual = new ArrayList<>();
                    for (OverviewSection section : sections) {
                        actual.add(section.id);
                    }
                    assertEquals(expected, actual);

                    // Das erzwungene Neuladen liest direkt aus dem Store.
                    call(activity, "rebuildOverviewSections", new Class<?>[]{});
                    List<String> reloaded = new ArrayList<>();
                    for (OverviewSection section : sections) {
                        reloaded.add(section.id);
                    }
                    assertEquals(expected, reloaded);
                    a.setSortMode(false);
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            });
        }
    }

    @Test
    public void categoryDragPersistsImmediatelyInList() {
        checkSectionDrag(false);
    }

    @Test
    public void categoryDragPersistsImmediatelyInGrid() {
        checkSectionDrag(true);
    }
}
