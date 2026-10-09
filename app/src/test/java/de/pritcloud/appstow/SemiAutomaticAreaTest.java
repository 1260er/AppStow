package de.pritcloud.appstow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class SemiAutomaticAreaTest {

    private static final List<String> MIXED_IDS =
            Arrays.asList(
                    "app:alpha",
                    "shortcut:beta");

    @Before
    public void clearState() {

        Context context =
                ApplicationProvider.getApplicationContext();

        for (String name : Arrays.asList(
                "sorting_settings",
                "categories",
                "favorites",
                "shortcuts",
                "section_item_order")) {

            context.deleteSharedPreferences(name);
        }
    }

    @Test
    public void areaFlagsTreatAppsAndShortcutsEqually() {

        Fixture fixture = new Fixture();

        // Ausschliesslich Favoriten automatisch.
        setSemi(fixture, true, false, false, false);

        assertAreas(fixture, true, false, false);

        // Kategorien steuert nur die Bereichsreihenfolge.
        setSemi(fixture, false, true, false, false);

        assertAreas(fixture, false, false, false);

        assertTrue((Boolean) call(
                fixture.adapter,
                "isSectionOrderAutomatic",
                new Class<?>[]{OverviewSection.class},
                fixture.category));

        // Kategorieinhalte, unabhaengig vom Eintragstyp.
        setSemi(fixture, false, false, true, false);

        assertAreas(fixture, false, true, false);

        // Eigene Shortcuts, unabhaengig vom Eintragstyp.
        setSemi(fixture, false, false, false, true);

        assertAreas(fixture, false, false, true);

        // Saemtliche Bereiche automatisch.
        setSemi(fixture, true, true, true, true);

        assertAreas(fixture, true, true, true);
    }

    @Test
    public void mixedCategoryItemsRankTogether() {

        Fixture fixture = new Fixture();

        setSemi(fixture, false, false, true, false);

        Set<String> selected =
                automaticIds(fixture.adapter, fixture.category);

        List<String> ranked =
                AutomaticSortEngine.rankSelectedIdsAcrossPeers(
                        MIXED_IDS,
                        selected,
                        Map.of(
                                "app:alpha", 1,
                                "shortcut:beta", 20),
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        false);

        assertEquals(
                Arrays.asList(
                        "shortcut:beta",
                        "app:alpha"),
                ranked);

        // Der Shortcut-Schalter darf die Kategorie
        // nicht beeinflussen, wenn Inhalte manuell sind.
        setSemi(fixture, false, false, false, true);

        assertTrue(
                automaticIds(fixture.adapter, fixture.category)
                        .isEmpty());
    }

    @Test
    public void manualAndFullAutomaticAreConsistent() {

        Fixture fixture = new Fixture();

        fixture.adapter.setSemiAutomaticSorting(
                false,
                false,
                false,
                false,
                false,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                false);

        assertAreas(fixture, false, false, false);

        fixture.adapter.setFullAutomaticSorting(
                Collections.emptySet(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                false);

        assertAreas(fixture, true, true, true);
    }

    @Test
    public void categoryContentsLabelUsesExistingControl() {

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {

            scenario.onActivity(activity -> {

                activity.findViewById(
                        R.id.navSorting).performClick();

                activity.findViewById(
                        R.id.sortingModeSemi).performClick();

                CheckBox contents =
                        activity.findViewById(
                                R.id.sortingSemiApps);

                assertEquals(
                        activity.getString(
                                R.string.sorting_area_apps),
                        contents.getText().toString());

                assertEquals(1, contents.getMaxLines());

                assertEquals(
                        android.text.TextUtils.TruncateAt.END,
                        contents.getEllipsize());
            });
        }
    }

    private static void assertAreas(
            Fixture fixture,
            boolean favorites,
            boolean categoryContents,
            boolean shortcuts) {

        assertArea(
                fixture.adapter,
                fixture.favorites,
                favorites);

        assertArea(
                fixture.adapter,
                fixture.category,
                categoryContents);

        assertArea(
                fixture.adapter,
                fixture.shortcuts,
                shortcuts);
    }

    private static void assertArea(
            OverviewAdapter adapter,
            OverviewSection section,
            boolean automatic) {

        assertEquals(
                automatic,
                (Boolean) call(
                        adapter,
                        "isSectionItemsFullyAutomatic",
                        new Class<?>[]{OverviewSection.class},
                        section));

        Set<String> expected =
                automatic
                        ? new HashSet<>(MIXED_IDS)
                        : Collections.emptySet();

        assertEquals(
                expected,
                automaticIds(adapter, section));

        for (String itemId : MIXED_IDS) {

            assertEquals(
                    automatic,
                    (Boolean) call(
                            adapter,
                            "isItemAutomatic",
                            new Class<?>[]{
                                    OverviewSection.class,
                                    String.class
                            },
                            section,
                            itemId));
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<String> automaticIds(
            OverviewAdapter adapter,
            OverviewSection section) {

        return (Set<String>) call(
                adapter,
                "getAutomaticItemIds",
                new Class<?>[]{
                        OverviewSection.class,
                        List.class
                },
                section,
                MIXED_IDS);
    }

    private static void setSemi(
            Fixture fixture,
            boolean favorites,
            boolean categories,
            boolean contents,
            boolean shortcuts) {

        fixture.adapter.setSemiAutomaticSorting(
                true,
                favorites,
                categories,
                contents,
                shortcuts,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                false);
    }

    private static Object call(
            OverviewAdapter adapter,
            String name,
            Class<?>[] parameterTypes,
            Object... arguments) {

        try {
            Method method =
                    OverviewAdapter.class.getDeclaredMethod(
                            name,
                            parameterTypes);

            method.setAccessible(true);

            return method.invoke(adapter, arguments);

        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    private static final class Fixture {

        final OverviewSection favorites =
                new OverviewSection(
                        "favorites", "Favorites", "Empty");

        final OverviewSection category =
                new OverviewSection(
                        "category:work", "Work", "Empty");

        final OverviewSection shortcuts =
                new OverviewSection(
                        "shortcuts", "Shortcuts", "Empty");

        final OverviewAdapter adapter;

        Fixture() {

            Context context =
                    ApplicationProvider.getApplicationContext();

            adapter = new OverviewAdapter(
                    Arrays.asList(
                            favorites,
                            category,
                            shortcuts),
                    new FavoritesStore(context),
                    new CategoryStore(context),
                    null,
                    new ShortcutStore(context),
                    new SectionItemOrderStore(context),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }
    }
}
