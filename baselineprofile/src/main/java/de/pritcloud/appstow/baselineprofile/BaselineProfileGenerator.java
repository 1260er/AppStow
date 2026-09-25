package de.pritcloud.appstow.baselineprofile;

import androidx.benchmark.macro.BaselineProfileConfig;
import androidx.benchmark.macro.junit4.BaselineProfileRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import kotlin.Unit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class BaselineProfileGenerator {

    private static final String PACKAGE_NAME =
            "de.pritcloud.appstow";

    @Rule
    public final BaselineProfileRule baselineProfileRule =
            new BaselineProfileRule();

    @Test
    public void startup() {

        BaselineProfileConfig config =
                new BaselineProfileConfig.Builder(
                        PACKAGE_NAME,
                        scope -> {
                            scope.startActivityAndWait();
                            return Unit.INSTANCE;
                        })
                        .setIncludeInStartupProfile(true)
                        .setStrictStability(false)
                        .build();

        baselineProfileRule.collectWithResults(
                config);
    }
}
