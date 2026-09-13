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

android {
    namespace = "de.pritcloud.appstow"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "de.pritcloud.appstow"
        minSdk = 26
        targetSdk = 35
        versionCode =
            providers.environmentVariable("APPSTOW_VERSION_CODE")
                .orNull?.toIntOrNull() ?: 1
        versionName = "0.1.0"
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
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
}
