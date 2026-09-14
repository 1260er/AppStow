package de.pritcloud.appstow;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EmojiSearchIndexTest {

    @Test
    public void normalizesGermanSearchText() {

        assertEquals(
                "lacheln strasse",
                EmojiSearchIndex.normalizeForSearch(
                        "Lächeln Straße"));
    }

    @Test
    public void matchesPrefixesInAnyOrder() {

        assertTrue(
                EmojiSearchIndex.matchesTerms(
                        "auto fahrzeug rot red car",
                        "rot aut"));
    }

    @Test
    public void rejectsMissingSearchTerm() {

        assertFalse(
                EmojiSearchIndex.matchesTerms(
                        "auto fahrzeug rot red car",
                        "hund"));
    }
}
