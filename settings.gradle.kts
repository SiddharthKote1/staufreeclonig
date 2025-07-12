pluginManagement {
    repositories {
<<<<<<< HEAD
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
=======
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

>>>>>>> 8d58c39 (Initial commit)
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
<<<<<<< HEAD
    }
}

rootProject.name = "Stayfree"
include(":app")
 
=======
        // ✅ Required for Compose libraries
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "Stayfree-FinalIntegration"
include(":app")
>>>>>>> 8d58c39 (Initial commit)
