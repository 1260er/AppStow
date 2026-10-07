package de.pritcloud.appstow;

import android.os.LocaleList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class UiLocaleControllerTest {

    @Test
    public void emptyLocalesRepresentSystemLanguage() {

        assertEquals(
                UiLocaleController.LanguageMode.SYSTEM,
                UiLocaleController.fromLocales(
                        LocaleList.getEmptyLocaleList()));

        assertTrue(
                UiLocaleController.localesFor(
                                UiLocaleController.LanguageMode.SYSTEM)
                        .isEmpty());
    }

    @Test
    public void englishAndGermanMapToSupportedLocaleTags() {

        assertEquals(
                UiLocaleController.LanguageMode.ENGLISH,
                UiLocaleController.fromLocales(
                        LocaleList.forLanguageTags(
                                "en-US")));

        assertEquals(
                UiLocaleController.LanguageMode.GERMAN,
                UiLocaleController.fromLocales(
                        LocaleList.forLanguageTags(
                                "de-DE")));

        assertEquals(
                "en",
                UiLocaleController.localesFor(
                                UiLocaleController.LanguageMode.ENGLISH)
                        .toLanguageTags());

        assertEquals(
                "de",
                UiLocaleController.localesFor(
                                UiLocaleController.LanguageMode.GERMAN)
                        .toLanguageTags());
    }

    @Test
    public void unsupportedLocaleFallsBackToSystemSelection() {

        assertEquals(
                UiLocaleController.LanguageMode.SYSTEM,
                UiLocaleController.fromLocales(
                        LocaleList.forLanguageTags(
                                "fr")));
    }
}
