plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "pl.creativesstudio"
    compileSdk = 34

    defaultConfig {
        applicationId = "pl.creativesstudio"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources {
            excludes.addAll(
                listOf(
                    "META-INF/LICENSE.md",
                    "META-INF/LICENSE",
                    "META-INF/LICENSE.txt",
                    "META-INF/NOTICE",
                    "META-INF/NOTICE.txt",
                    "META-INF/LICENSE-notice.md"
                )
            )
        }
    }

}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.junit.jupiter)
    testImplementation(libs.junit)
    testImplementation(libs.core)
    testImplementation(libs.core)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation ("com.google.android.gms:play-services-location:21.0.1")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.12.1")
    testImplementation("org.mockito:mockito-core:5.14.1")
    testImplementation("org.mockito:mockito-inline:5.1.1")
    testImplementation("androidx.test:core:1.4.0")
    testImplementation("androidx.test.ext:junit:1.1.3")
    // Robolectric and test dependencies
    testImplementation("org.robolectric:robolectric:4.12.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("org.mockito:mockito-core:5.14.1")
    testImplementation("org.mockito:mockito-inline:5.1.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    // Google Play Services for Maps
    testImplementation("com.google.android.gms:play-services-maps:19.0.0")
    testImplementation("com.google.android.gms:play-services-location:21.0.1")

}