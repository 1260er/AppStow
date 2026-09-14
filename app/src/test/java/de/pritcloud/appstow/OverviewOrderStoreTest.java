package de.pritcloud.appstow;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class OverviewOrderStoreTest {

    private Context context;
    private OverviewOrderStore store;

    @Before
    public void setUp() {
        context =
                RuntimeEnvironment
                        .getApplication();

        context.getSharedPreferences(
                        "overview_order",
                        Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();

        store =
                new OverviewOrderStore(
                        context);
    }

    @Test
    public void saveOrderPersistsSectionIds() {
        List<OverviewSection> sections =
                new ArrayList<>();

        sections.add(
                new OverviewSection(
                        "shortcuts",
                        "Shortcuts",
                        "Empty"));

        sections.add(
                new OverviewSection(
                        "favorites",
                        "Favorites",
                        "Empty"));

        sections.add(
                new OverviewSection(
                        "category:work",
                        "Work",
                        "Empty"));

        store.saveOrder(
                sections);

        OverviewOrderStore reloaded =
                new OverviewOrderStore(
                        context);

        assertEquals(
                Arrays.asList(
                        "shortcuts",
                        "favorites",
                        "category:work"),
                reloaded.getOrder());
    }

    @Test
    public void malformedStoredOrderFallsBackToEmpty() {
        context.getSharedPreferences(
                        "overview_order",
                        Context.MODE_PRIVATE)
                .edit()
                .putString(
                        "section_order",
                        "{broken")
                .commit();

        assertTrue(
                new OverviewOrderStore(
                        context)
                        .getOrder()
                        .isEmpty());
    }
}
