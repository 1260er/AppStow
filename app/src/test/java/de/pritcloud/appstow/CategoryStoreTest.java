package de.pritcloud.appstow;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CategoryStoreTest {

    private Context context;
    private CategoryStore store;

    @Before
    public void setUp() {
        context =
                RuntimeEnvironment
                        .getApplication();

        context.getSharedPreferences(
                        "categories",
                        Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();

        store =
                new CategoryStore(
                        context);
    }

    @Test
    public void assignmentResultIsDefensiveCopy() {

        assertTrue(
                store.addCategory(
                        "Work",
                        "💼"));

        String categoryId =
                store.getCategories()
                        .get(0)
                        .id;

        store.setAssignedCategoryIds(
                "com.example.app",
                Set.of(
                        categoryId));

        Set<String> returned =
                store.getAssignedCategoryIds(
                        "com.example.app");

        returned.clear();

        assertTrue(
                store.getAssignedCategoryIds(
                                "com.example.app")
                        .contains(
                                categoryId));
    }

    @Test
    public void deletingCategoryRemovesAssignments() {

        assertTrue(
                store.addCategory(
                        "Work",
                        "💼"));

        String categoryId =
                store.getCategories()
                        .get(0)
                        .id;

        store.setAssignedCategoryIds(
                "com.example.app",
                Set.of(
                        categoryId));

        store.deleteCategory(
                categoryId);

        assertTrue(
                store.getAssignedCategoryIds(
                                "com.example.app")
                        .isEmpty());
    }

    @Test
    public void categorySymbolIsRequiredAndPersists() {

        assertFalse(
                store.addCategory(
                        "Work",
                        "   "));

        assertTrue(
                store.addCategory(
                        "Work",
                        "💼"));

        CategoryEntry category =
                new CategoryStore(
                        context)
                        .getCategories()
                        .get(0);

        assertEquals(
                "💼",
                category.symbol);
    }

    @Test
    public void symbolVisibilityPreferencePersists() {

        assertFalse(
                store.areSymbolsEnabled());

        store.setSymbolsEnabled(
                true);

        assertTrue(
                new CategoryStore(
                        context)
                        .areSymbolsEnabled());
    }


    @Test
    public void duplicateCategoryNamesAreRejectedCaseInsensitive() {

        assertTrue(
                store.addCategory(
                        "Work",
                        "💼"));

        assertFalse(
                store.addCategory(
                        "work",
                        "💼"));

        assertFalse(
                store.addCategory(
                        "  WORK  ",
                        "💼"));

        assertEquals(
                1,
                store.getCategories()
                        .size());
    }
}
