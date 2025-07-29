pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        // ✅ JetBrains Compose
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        // ✅ JitPack for GitHub libraries
        maven("https://jitpack.io")
    }
    plugins {
        id("com.android.application") version "8.1.1"
        id("org.jetbrains.kotlin.android") version "1.9.10"
        id("org.jetbrains.compose") version "1.5.10"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        // ✅ Add here too
        maven("https://jitpack.io")
    }
}

rootProject.name = "Stayfree-FinalIntegration"
include(":app")
