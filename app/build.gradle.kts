plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// APK files are named Bhagavatam-debug.apk and Bhagavatam-release.apk.
base { archivesName.set("Bhagavatam") }

android {
    namespace = "com.bhagavatam.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bhagavatam.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.4.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Private app, not on the Play Store: sign with the local debug key so the APK installs directly.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/*.version", "DebugProbesKt.bin")
    }
    dependenciesInfo {
        includeInApk = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    // Installs the Compose baseline profiles on sideloaded APKs, so scrolling is smooth from the first launch.
    implementation(libs.androidx.profileinstaller)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation("junit:junit:4.13.2")
}

// Copy finished APKs to <project>/apk so they are easy to find (the build folder lives outside OneDrive).
val copyApks = tasks.register<Copy>("copyApks") {
    from(layout.buildDirectory.dir("outputs/apk")) { include("**/*.apk") }
    from(layout.buildDirectory.dir("intermediates/apk")) { include("**/*.apk") }
    into(rootProject.layout.projectDirectory.dir("apk"))
    eachFile { path = name }
    includeEmptyDirs = false
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.configureEach {
    if (name == "assembleDebug" || name == "assembleRelease") finalizedBy(copyApks)
}
