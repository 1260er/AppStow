package de.pritcloud.appstow;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class FavoritesStoreTest {

    private Context context;

    @Before
    public void setUp() {
        context =
                RuntimeEnvironment
                        .getApplication();

        context.getSharedPreferences(
                        "favorites",
                        Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void toggleAddsAndPersistsFavorite() {
        FavoritesStore store =
                new FavoritesStore(
                        context);

        assertFalse(
                store.isFavorite(
                        "com.example.app"));

        assertTrue(
                store.toggle(
                        "com.example.app"));

        FavoritesStore reloaded =
                new FavoritesStore(
                        context);

        assertTrue(
                reloaded.isFavorite(
                        "com.example.app"));
    }

    @Test
    public void toggleRemovesAndPersistsFavorite() {
        FavoritesStore store =
                new FavoritesStore(
                        context);

        store.toggle(
                "com.example.app");

        assertFalse(
                store.toggle(
                        "com.example.app"));

        FavoritesStore reloaded =
                new FavoritesStore(
                        context);

        assertFalse(
                reloaded.isFavorite(
                        "com.example.app"));
    }
}
