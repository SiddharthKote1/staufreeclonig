pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        // ✅ JetBrains Compose repository (required)
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    plugins {
        id("com.android.application") version "8.11.0"
        id("org.jetbrains.kotlin.android") version "1.9.10"
        id("org.jetbrains.compose") version "1.5.10"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // ✅ Required for Compose libraries
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "Stayfree-FinalIntegration"
include(":app")
