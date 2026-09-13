package de.pritcloud.appstow;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SectionItemOrderStoreTest {

    private Context context;
    private SharedPreferences preferences;
    private SectionItemOrderStore store;

    @Before
    public void setUp() {
        context =
                RuntimeEnvironment
                        .getApplication();

        preferences =
                context.getSharedPreferences(
                        "section_item_order",
                        Context.MODE_PRIVATE);

        preferences.edit()
                .clear()
                .commit();

        store =
                new SectionItemOrderStore(
                        context);
    }

    @Test
    public void readKeepsSavedOrderAndAppendsNewIds() {

        store.saveOrder(
                "favorites",
                Arrays.asList(
                        "app:b",
                        "app:a"));

        String before =
                preferences.getString(
                        "orders",
                        "");

        assertEquals(
                Arrays.asList(
                        "app:b",
                        "app:a",
                        "app:c"),
                store.getOrderedIds(
                        "favorites",
                        Arrays.asList(
                                "app:a",
                                "app:b",
                                "app:c")));

        String after =
                preferences.getString(
                        "orders",
                        "");

        assertEquals(
                before,
                after);
    }

    @Test
    public void readIgnoresStaleAndDuplicateSavedIds() {

        preferences.edit()
                .putString(
                        "orders",
                        "{\"favorites\":[\"app:b\",\"app:b\",\"app:gone\",\"app:a\"]}")
                .commit();

        assertEquals(
                Arrays.asList(
                        "app:b",
                        "app:a",
                        "app:c"),
                store.getOrderedIds(
                        "favorites",
                        Arrays.asList(
                                "app:a",
                                "app:b",
                                "app:c")));
    }

    @Test
    public void saveDropsEmptyAndDuplicateIds() {

        store.saveOrder(
                "favorites",
                Arrays.asList(
                        "app:a",
                        "",
                        "app:a",
                        "app:b"));

        assertEquals(
                Arrays.asList(
                        "app:a",
                        "app:b",
                        "app:c"),
                store.getOrderedIds(
                        "favorites",
                        Arrays.asList(
                                "app:a",
                                "app:b",
                                "app:c")));
    }
}
