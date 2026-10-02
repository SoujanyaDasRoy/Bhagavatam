plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Keep build output out of OneDrive: syncing thousands of intermediate files slows every build.
// Finished APKs are still copied back into ./apk (see app/build.gradle.kts).
System.getenv("LOCALAPPDATA")?.let { local ->
    val outRoot = File(local, "BhagavatamBuild")
    allprojects { layout.buildDirectory.set(File(outRoot, name)) }
}
