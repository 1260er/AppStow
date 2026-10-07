package de.pritcloud.appstow;

import android.app.LocaleManager;
import android.content.Context;
import android.os.LocaleList;

final class UiLocaleController {

    enum LanguageMode {
        SYSTEM,
        ENGLISH,
        GERMAN
    }

    private UiLocaleController() {
    }

    static LanguageMode getLanguageMode(
            Context context) {

        LocaleManager localeManager =
                context.getSystemService(
                        LocaleManager.class);

        return fromLocales(
                localeManager.getApplicationLocales());
    }

    static void apply(
            Context context,
            LanguageMode languageMode) {

        if (context == null
                || languageMode == null) {

            throw new IllegalArgumentException(
                    "Locale context and mode are required.");
        }

        LocaleManager localeManager =
                context.getSystemService(
                        LocaleManager.class);

        localeManager.setApplicationLocales(
                localesFor(
                        languageMode));
    }

    static LanguageMode fromLocales(
            LocaleList locales) {

        if (locales == null
                || locales.isEmpty()) {

            return LanguageMode.SYSTEM;
        }

        String language =
                locales.get(0)
                        .getLanguage();

        if ("de".equalsIgnoreCase(
                language)) {

            return LanguageMode.GERMAN;
        }

        if ("en".equalsIgnoreCase(
                language)) {

            return LanguageMode.ENGLISH;
        }

        return LanguageMode.SYSTEM;
    }

    static LocaleList localesFor(
            LanguageMode languageMode) {

        if (languageMode == null) {
            throw new IllegalArgumentException(
                    "Language mode is required.");
        }

        if (languageMode
                == LanguageMode.GERMAN) {

            return LocaleList.forLanguageTags(
                    "de");
        }

        if (languageMode
                == LanguageMode.ENGLISH) {

            return LocaleList.forLanguageTags(
                    "en");
        }

        return LocaleList.getEmptyLocaleList();
    }
}
