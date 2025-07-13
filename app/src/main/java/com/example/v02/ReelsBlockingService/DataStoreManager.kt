package com.example.v02.ReelsBlockingService

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

val Context.appSettingsDataStore: DataStore<AppSettings> by dataStore(
    fileName = "app_settings.json",
    serializer = AppSettingsSerializer
)

class DataStoreManager(private val context: Context) {

    private val dataStore: DataStore<AppSettings> = context.appSettingsDataStore

    val appSettings: StateFlow<AppSettings> = dataStore.data
        .catch { emit(AppSettings()) }
        .stateIn(
            scope = kotlinx.coroutines.GlobalScope,
            started = SharingStarted.Eagerly,
            initialValue = AppSettings()
        )

    fun getCurrentAppSettings(): AppSettings {
        return appSettings.value
    }

    val activeChildId: StateFlow<String> = appSettings.map { it.activeChildId }
        .stateIn(kotlinx.coroutines.GlobalScope, SharingStarted.Eagerly, "")

    val accountMode: StateFlow<String> = appSettings.map { it.accountMode }
        .stateIn(kotlinx.coroutines.GlobalScope, SharingStarted.Eagerly, "Parent")

    val pinCode: StateFlow<String> = appSettings.map { it.pinCode }
        .stateIn(kotlinx.coroutines.GlobalScope, SharingStarted.Eagerly, "")

    val secretQuestion: StateFlow<String> = appSettings.map { it.secretQuestion }
        .stateIn(kotlinx.coroutines.GlobalScope, SharingStarted.Eagerly, "")

    val secretAnswer: StateFlow<String> = appSettings.map { it.secretAnswer }
        .stateIn(kotlinx.coroutines.GlobalScope, SharingStarted.Eagerly, "")

    val childProfiles: StateFlow<List<ChildProfile>> = appSettings.map { it.childProfiles }
        .stateIn(kotlinx.coroutines.GlobalScope, SharingStarted.Eagerly, emptyList())

    private suspend fun saveSettings(settings: AppSettings) {
        withContext(Dispatchers.IO) {
            dataStore.updateData { settings }
        }
    }

    suspend fun setActiveChildProfile(id: String) {
        saveSettings(appSettings.value.copy(activeChildId = id))
    }

    suspend fun setAccountMode(mode: String) {
        saveSettings(appSettings.value.copy(accountMode = mode))
    }

    suspend fun setPinCode(pin: String) {
        saveSettings(appSettings.value.copy(pinCode = pin))
    }

    suspend fun setSecretQA(question: String, answer: String) {
        saveSettings(appSettings.value.copy(secretQuestion = question, secretAnswer = answer))
    }

    suspend fun addOrUpdateChildProfile(profile: ChildProfile) {
        val settings = appSettings.value
        val updatedChildren = settings.childProfiles.filter { it.id != profile.id } + profile
        saveSettings(settings.copy(childProfiles = updatedChildren))
    }

    suspend fun deleteChildProfile(id: String) {
        val settings = appSettings.value
        val updatedChildren = settings.childProfiles.filterNot { it.id == id }
        val newActiveId = if (settings.activeChildId == id) "" else settings.activeChildId
        saveSettings(settings.copy(childProfiles = updatedChildren, activeChildId = newActiveId))
    }

    private suspend fun updateActiveChild(block: (ChildProfile) -> ChildProfile) {
        val settings = appSettings.value
        val activeId = settings.activeChildId
        if (activeId.isBlank()) return

        val updatedChildren = settings.childProfiles.map {
            if (it.id == activeId) block(it) else it
        }
        saveSettings(settings.copy(childProfiles = updatedChildren))
    }

    private suspend fun updateParentApp(block: (AppSettings) -> AppSettings) {
        val settings = appSettings.value
        saveSettings(block(settings))
    }

    // Instagram
    suspend fun setInstagramReelsBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent") updateParentApp { it.copy(instagram = it.instagram.copy(reelsBlocked = enabled)) }
        else updateActiveChild { it.copy(instagram = it.instagram.copy(reelsBlocked = enabled)) }

    suspend fun setInstagramStoriesBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent") updateParentApp { it.copy(instagram = it.instagram.copy(storiesBlocked = enabled)) }
        else updateActiveChild { it.copy(instagram = it.instagram.copy(storiesBlocked = enabled)) }

    suspend fun setInstagramExploreBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent") updateParentApp { it.copy(instagram = it.instagram.copy(exploreBlocked = enabled)) }
        else updateActiveChild { it.copy(instagram = it.instagram.copy(exploreBlocked = enabled)) }

    // Facebook
    suspend fun setFacebookReelsBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent") updateParentApp { it.copy(facebook = it.facebook.copy(reelsBlocked = enabled)) }
        else updateActiveChild { it.copy(facebook = it.facebook.copy(reelsBlocked = enabled)) }

    suspend fun setFacebookMarketplaceBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent") updateParentApp { it.copy(facebook = it.facebook.copy(marketplaceBlocked = enabled)) }
        else updateActiveChild { it.copy(facebook = it.facebook.copy(marketplaceBlocked = enabled)) }

    suspend fun setFacebookStoriesBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent") updateParentApp { it.copy(facebook = it.facebook.copy(storiesBlocked = enabled)) }
        else updateActiveChild { it.copy(facebook = it.facebook.copy(storiesBlocked = enabled)) }

    // YouTube
    suspend fun setYouTubeShortsBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent") updateParentApp { it.copy(youtube = it.youtube.copy(shortsBlocked = enabled)) }
        else updateActiveChild { it.copy(youtube = it.youtube.copy(shortsBlocked = enabled)) }

    suspend fun setYouTubeCommentsBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent") updateParentApp { it.copy(youtube = it.youtube.copy(commentsBlocked = enabled)) }
        else updateActiveChild { it.copy(youtube = it.youtube.copy(commentsBlocked = enabled)) }

    suspend fun setYouTubeSearchBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent") updateParentApp { it.copy(youtube = it.youtube.copy(searchBlocked = enabled)) }
        else updateActiveChild { it.copy(youtube = it.youtube.copy(searchBlocked = enabled)) }

    // Twitter
    suspend fun setTwitterExploreBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent") updateParentApp { it.copy(twitter = it.twitter.copy(exploreBlocked = enabled)) }
        else updateActiveChild { it.copy(twitter = it.twitter.copy(exploreBlocked = enabled)) }

    // WhatsApp
    suspend fun setWhatsAppStatusBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent") updateParentApp { it.copy(whatsapp = it.whatsapp.copy(statusBlocked = enabled)) }
        else updateActiveChild { it.copy(whatsapp = it.whatsapp.copy(statusBlocked = enabled)) }

    // Snapchat
    suspend fun setSnapchatSpotlightBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent") updateParentApp { it.copy(snapchat = it.snapchat.copy(spotlightBlocked = enabled)) }
        else updateActiveChild { it.copy(snapchat = it.snapchat.copy(spotlightBlocked = enabled)) }

    suspend fun setSnapchatStoriesBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent") updateParentApp { it.copy(snapchat = it.snapchat.copy(storiesBlocked = enabled)) }
        else updateActiveChild { it.copy(snapchat = it.snapchat.copy(storiesBlocked = enabled)) }

    // App Time Limits
    suspend fun setAppTimeLimit(packageName: String, minutes: Int) {
        if (appSettings.value.accountMode == "Parent") {
            // Update parent time limits
            val settings = appSettings.value
            val updatedLimits = settings.parentAppTimeLimits.toMutableMap().apply {
                this[packageName] = minutes
            }
            saveSettings(settings.copy(parentAppTimeLimits = updatedLimits))
        } else {
            // Update child time limits
            updateActiveChild {
                val updatedLimits = it.appTimeLimits.toMutableMap().apply {
                    this[packageName] = minutes
                }
                it.copy(appTimeLimits = updatedLimits)
            }
        }
    }
}

