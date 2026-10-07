package de.pritcloud.appstow;

import android.content.Context;
import android.content.res.Configuration;

final class UiThemeController {

    private UiThemeController() {
    }

    static Context wrap(
            Context context) {

        UiSettingsStore.ThemeMode themeMode =
                new UiSettingsStore(
                        context)
                        .load()
                        .themeMode;

        return wrap(
                context,
                themeMode);
    }

    static Context wrap(
            Context context,
            UiSettingsStore.ThemeMode themeMode) {

        if (context == null
                || themeMode == null) {

            throw new IllegalArgumentException(
                    "Theme context and mode are required.");
        }

        if (themeMode
                == UiSettingsStore.ThemeMode.SYSTEM) {

            return context;
        }

        Configuration configuration =
                new Configuration(
                        context.getResources()
                                .getConfiguration());

        int requestedNightMode =
                themeMode
                        == UiSettingsStore.ThemeMode.DARK
                        ? Configuration.UI_MODE_NIGHT_YES
                        : Configuration.UI_MODE_NIGHT_NO;

        configuration.uiMode =
                (configuration.uiMode
                        & ~Configuration.UI_MODE_NIGHT_MASK)
                        | requestedNightMode;

        return context.createConfigurationContext(
                configuration);
    }
}
