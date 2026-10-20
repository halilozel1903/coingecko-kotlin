plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.halilozel1903.coingecko.sample"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.halilozel1903.coingecko.sample"
        // java.time (all-time high dates, chart labels) without desugaring.
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        // Optional: a free CoinGecko Demo key from the environment. Empty (the default) uses the keyless public tier.
        val demoKey = providers.environmentVariable("COINGECKO_DEMO_API_KEY").orElse("").get()
        buildConfigField("String", "COINGECKO_DEMO_API_KEY", "\"" + demoKey.replace("\"", "") + "\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/INDEX.LIST",
                "/META-INF/DEPENDENCIES",
                "/META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "/META-INF/versions/9/module-info.class",
                "/META-INF/versions/9/previous-compilation-data.bin",
            )
        }
    }
}

dependencies {
    implementation(project(":coingecko"))
    // The sample shares OkHttp with Coil; without this line the client uses Ktor's CIO engine.
    implementation(libs.ktor.client.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
