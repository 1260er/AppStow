plugins { id("com.android.application") }

val releaseStoreFile =
    providers.environmentVariable("APPSTOW_KEYSTORE_PATH").orNull
val releaseStorePassword =
    providers.environmentVariable("APPSTOW_KEYSTORE_PASSWORD").orNull
val releaseKeyAlias =
    providers.environmentVariable("APPSTOW_KEY_ALIAS").orNull
val releaseKeyPassword =
    providers.environmentVariable("APPSTOW_KEY_PASSWORD").orNull

val releaseSigningConfigured = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

val releaseSigningRequired =
    providers.environmentVariable("APPSTOW_REQUIRE_RELEASE_SIGNING")
        .orNull
        ?.equals("true", ignoreCase = true) == true

if (releaseSigningRequired && !releaseSigningConfigured) {
    throw GradleException(
        "Release signing is required but not fully configured."
    )
}

android {
    namespace = "de.pritcloud.appstow"
    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    defaultConfig {
        applicationId = "de.pritcloud.appstow"
        minSdk = 26
        targetSdk = 36
        versionCode =
            providers.environmentVariable("APPSTOW_VERSION_CODE")
                .orNull?.toIntOrNull() ?: 1
        versionName = "0.1.1"
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(requireNotNull(releaseStoreFile))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)

                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"

            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }

        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")

    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
}
