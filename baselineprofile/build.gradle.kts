plugins {
    id("com.android.test")
    id("androidx.baselineprofile")
}

android {
    namespace = "de.pritcloud.appstow.baselineprofile"
    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        minSdk = 33
        targetSdk = 36
        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    testOptions {
        managedDevices {
            devices {
                create<com.android.build.api.dsl.ManagedVirtualDevice>(
                    "pixel6Api33"
                ) {
                    device = "Pixel 6"
                    apiLevel = 33
                    systemImageSource = "aosp"
                }
            }
        }
    }
}

baselineProfile {
    managedDevices += "pixel6Api33"
    useConnectedDevices = false
}

dependencies {
    implementation(
        "androidx.benchmark:benchmark-macro-junit4:1.5.0"
    )
    implementation(
        "androidx.test.ext:junit:1.3.0"
    )
    implementation(
        "androidx.test:runner:1.7.0"
    )
}
