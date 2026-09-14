package de.pritcloud.appstow;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ShortcutStoreTest {

    private Context context;
    private ShortcutStore store;

    @Before
    public void setUp() {
        context =
                RuntimeEnvironment
                        .getApplication();

        context.getSharedPreferences(
                        "shortcuts",
                        Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();

        store =
                new ShortcutStore(
                        context);
    }

    @Test
    public void addAndUpdatePersistShortcut() {
        store.add(
                " Example ",
                ShortcutEntry.TYPE_WEBSITE,
                " https://example.com ",
                Set.of(
                        "cat-a"),
                false);

        ShortcutEntry added =
                store.getShortcuts()
                        .get(0);

        assertEquals(
                "Example",
                added.name);

        assertEquals(
                "https://example.com",
                added.target);

        store.update(
                added.id,
                "Updated",
                ShortcutEntry.TYPE_WEB_APP,
                "https://example.com/app",
                Set.of(
                        "cat-b"),
                true);

        ShortcutEntry updated =
                new ShortcutStore(
                        context)
                        .getShortcuts()
                        .get(0);

        assertEquals(
                "Updated",
                updated.name);

        assertEquals(
                ShortcutEntry.TYPE_WEB_APP,
                updated.type);

        assertTrue(
                updated.favorite);

        assertEquals(
                Set.of(
                        "cat-b"),
                updated.categoryIds);
    }

    @Test
    public void toggleFavoritePersists() {
        store.add(
                "Example",
                ShortcutEntry.TYPE_WEBSITE,
                "https://example.com",
                Set.of(),
                false);

        String id =
                store.getShortcuts()
                        .get(0)
                        .id;

        store.toggleFavorite(
                id);

        assertTrue(
                new ShortcutStore(
                        context)
                        .getShortcuts()
                        .get(0)
                        .favorite);

        store.toggleFavorite(
                id);

        assertFalse(
                new ShortcutStore(
                        context)
                        .getShortcuts()
                        .get(0)
                        .favorite);
    }

    @Test
    public void removeCategoryUpdatesAllAffectedShortcuts() {
        store.add(
                "One",
                ShortcutEntry.TYPE_WEBSITE,
                "https://one.example",
                Set.of(
                        "cat-a",
                        "cat-b"),
                false);

        store.add(
                "Two",
                ShortcutEntry.TYPE_WEBSITE,
                "https://two.example",
                Set.of(
                        "cat-a"),
                false);

        store.removeCategory(
                "cat-a");

        List<ShortcutEntry> shortcuts =
                new ShortcutStore(
                        context)
                        .getShortcuts();

        assertEquals(
                Set.of(
                        "cat-b"),
                shortcuts.get(0)
                        .categoryIds);

        assertTrue(
                shortcuts.get(1)
                        .categoryIds
                        .isEmpty());
    }

    @Test
    public void deletePersistsRemoval() {
        store.add(
                "One",
                ShortcutEntry.TYPE_WEBSITE,
                "https://one.example",
                Set.of(),
                false);

        store.add(
                "Two",
                ShortcutEntry.TYPE_WEBSITE,
                "https://two.example",
                Set.of(),
                false);

        String firstId =
                store.getShortcuts()
                        .get(0)
                        .id;

        store.delete(
                firstId);

        List<ShortcutEntry> shortcuts =
                new ShortcutStore(
                        context)
                        .getShortcuts();

        assertEquals(
                1,
                shortcuts.size());

        assertEquals(
                "Two",
                shortcuts.get(0)
                        .name);
    }
}
