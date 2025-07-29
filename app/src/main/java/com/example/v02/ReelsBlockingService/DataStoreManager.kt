package com.example.v02.ReelsBlockingService

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

val Context.appSettingsDataStore: DataStore<AppSettings> by dataStore(
    fileName = "app_settings.json",
    serializer = AppSettingsSerializer
)

class DataStoreManager(private val context: Context) {

    private val dataStore: DataStore<AppSettings> = context.appSettingsDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val appSettings: StateFlow<AppSettings> = dataStore.data
        .catch { emit(AppSettings()) }
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    private suspend fun saveSettings(block: (AppSettings) -> AppSettings) {
        withContext(Dispatchers.IO) {
            dataStore.updateData { old -> block(old) }
        }
    }

    suspend fun setBlockedKeywordLists(lists: BlockedKeywordLists) {
        if (appSettings.value.accountMode == "Parent") {
            saveSettings { it.copy(blockedKeywordLists = lists) }
        } else {
            updateActiveChild { it.copy(blockedKeywordLists = lists) }
        }
    }

    suspend fun setCustomBlockedKeywords(keywords: List<String>) {
        if (appSettings.value.accountMode == "Parent") {
            saveSettings { it.copy(customBlockedKeywords = keywords) }
        } else {
            updateActiveChild { it.copy(customBlockedKeywords = keywords) }
        }
    }

// ------------------- CATEGORY BLOCKING (Parent or Child) -------------------

    suspend fun setCategoryBlocked(category: String, blocked: Boolean) {
        if (appSettings.value.accountMode == "Parent") {
            val updated = appSettings.value.blockedCategories.toMutableSet()
            if (blocked) updated.add(category) else updated.remove(category)
            saveSettings { it.copy(blockedCategories = updated) }
        } else {
            updateActiveChild { child ->
                val updated = child.blockedCategories.toMutableSet()
                if (blocked) updated.add(category) else updated.remove(category)
                child.copy(blockedCategories = updated)
            }
        }
    }

    fun isCategoryBlocked(category: String): Boolean {
        val settings = appSettings.value
        return if (settings.accountMode == "Parent") {
            settings.blockedCategories.contains(category)
        } else {
            settings.childProfiles.find { it.id == settings.activeChildId }
                ?.blockedCategories?.contains(category) ?: false
        }
    }

    fun getBlockedCategories(): StateFlow<Set<String>> {
        return appSettings.map { settings ->
            if (settings.accountMode == "Parent") {
                settings.blockedCategories
            } else {
                settings.childProfiles.find { it.id == settings.activeChildId }
                    ?.blockedCategories ?: emptySet()
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptySet())
    }



    // ✅ ACCOUNT INFO
    val activeChildId: StateFlow<String> = appSettings.map { it.activeChildId }
        .stateIn(scope, SharingStarted.Eagerly, "")

    val accountMode: StateFlow<String> = appSettings.map { it.accountMode }
        .stateIn(scope, SharingStarted.Eagerly, "Parent")

    val pinCode: StateFlow<String> = appSettings.map { it.pinCode }
        .stateIn(scope, SharingStarted.Eagerly, "")

    val secretQuestion: StateFlow<String> = appSettings.map { it.secretQuestion }
        .stateIn(scope, SharingStarted.Eagerly, "")

    val secretAnswer: StateFlow<String> = appSettings.map { it.secretAnswer }
        .stateIn(scope, SharingStarted.Eagerly, "")

    val childProfiles: StateFlow<List<ChildProfile>> = appSettings.map { it.childProfiles }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    suspend fun setActiveChildProfile(id: String) {
        saveSettings { it.copy(activeChildId = id) }
    }

    suspend fun setAccountMode(mode: String) {
        saveSettings { it.copy(accountMode = mode) }
    }

    suspend fun setPinCode(pin: String) {
        saveSettings { it.copy(pinCode = pin) }
    }

    suspend fun setSecretQA(question: String, answer: String) {
        saveSettings { it.copy(secretQuestion = question, secretAnswer = answer) }
    }

    suspend fun addOrUpdateChildProfile(profile: ChildProfile) {
        saveSettings {
            val updatedChildren = it.childProfiles.filter { c -> c.id != profile.id } + profile
            it.copy(childProfiles = updatedChildren)
        }
    }

    suspend fun deleteChildProfile(id: String) {
        saveSettings {
            val updatedChildren = it.childProfiles.filterNot { c -> c.id == id }
            val newActiveId = if (it.activeChildId == id) "" else it.activeChildId
            it.copy(childProfiles = updatedChildren, activeChildId = newActiveId)
        }
    }

    private suspend fun updateActiveChild(block: (ChildProfile) -> ChildProfile) {
        val settings = appSettings.value
        val activeId = settings.activeChildId
        if (activeId.isBlank()) return

        val updatedChildren = settings.childProfiles.map {
            if (it.id == activeId) block(it) else it
        }
        saveSettings { settings.copy(childProfiles = updatedChildren) }
    }

    private suspend fun updateParentApp(block: (AppSettings) -> AppSettings) {
        saveSettings { settings -> block(settings) }
    }

    // ✅ ------------------- PERMANENT BLOCKING (ACCOUNT SPECIFIC) -------------------
    suspend fun setAppPermanentlyBlocked(packageName: String, blocked: Boolean) {
        val settings = appSettings.value
        if (settings.accountMode == "Parent") {
            val updated = settings.blockedApps.toMutableMap()
            if (blocked) updated[packageName] = App() else updated.remove(packageName)
            saveSettings { settings.copy(blockedApps = updated) }
        } else {
            updateActiveChild { child ->
                val updated = child.blockedApps.toMutableMap()
                if (blocked) updated[packageName] = App() else updated.remove(packageName)
                child.copy(blockedApps = updated)
            }
        }
    }

    fun isAppPermanentlyBlocked(packageName: String): Boolean {
        val settings = appSettings.value
        return if (settings.accountMode == "Parent") {
            settings.blockedApps.containsKey(packageName)
        } else {
            settings.childProfiles.find { it.id == settings.activeChildId }
                ?.blockedApps?.containsKey(packageName) ?: false
        }
    }

    fun getPermanentlyBlockedApps(): StateFlow<Set<String>> {
        return appSettings.map { settings ->
            if (settings.accountMode == "Parent") {
                settings.blockedApps.keys
            } else {
                settings.childProfiles.find { it.id == settings.activeChildId }
                    ?.blockedApps?.keys ?: emptySet()
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptySet())
    }

    // ✅ ------------------- APP TIME LIMITS -------------------
    suspend fun setAppTimeLimit(packageName: String, minutes: Int) {
        if (appSettings.value.accountMode == "Parent") {
            val settings = appSettings.value
            val updatedLimits = settings.parentAppTimeLimits.toMutableMap().apply {
                this[packageName] = minutes
            }
            saveSettings { settings.copy(parentAppTimeLimits = updatedLimits) }
        } else {
            updateActiveChild {
                val updatedLimits = it.appTimeLimits.toMutableMap().apply {
                    this[packageName] = minutes
                }
                it.copy(appTimeLimits = updatedLimits)
            }
        }
    }

    // ✅ ------------------- INSTAGRAM BLOCKING -------------------
    suspend fun setInstagramReelsBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent")
            updateParentApp { it.copy(instagram = it.instagram.copy(reelsBlocked = enabled)) }
        else updateActiveChild { it.copy(instagram = it.instagram.copy(reelsBlocked = enabled)) }

    suspend fun setInstagramStoriesBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent")
            updateParentApp { it.copy(instagram = it.instagram.copy(storiesBlocked = enabled)) }
        else updateActiveChild { it.copy(instagram = it.instagram.copy(storiesBlocked = enabled)) }

    suspend fun setInstagramExploreBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent")
            updateParentApp { it.copy(instagram = it.instagram.copy(exploreBlocked = enabled)) }
        else updateActiveChild { it.copy(instagram = it.instagram.copy(exploreBlocked = enabled)) }

    // ✅ ------------------- FACEBOOK BLOCKING -------------------
    suspend fun setFacebookReelsBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent")
            updateParentApp { it.copy(facebook = it.facebook.copy(reelsBlocked = enabled)) }
        else updateActiveChild { it.copy(facebook = it.facebook.copy(reelsBlocked = enabled)) }

    suspend fun setFacebookMarketplaceBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent")
            updateParentApp { it.copy(facebook = it.facebook.copy(marketplaceBlocked = enabled)) }
        else updateActiveChild { it.copy(facebook = it.facebook.copy(marketplaceBlocked = enabled)) }

    suspend fun setFacebookStoriesBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent")
            updateParentApp { it.copy(facebook = it.facebook.copy(storiesBlocked = enabled)) }
        else updateActiveChild { it.copy(facebook = it.facebook.copy(storiesBlocked = enabled)) }

    // ✅ ------------------- YOUTUBE BLOCKING -------------------
    suspend fun setYouTubeShortsBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent")
            updateParentApp { it.copy(youtube = it.youtube.copy(shortsBlocked = enabled)) }
        else updateActiveChild { it.copy(youtube = it.youtube.copy(shortsBlocked = enabled)) }

    suspend fun setYouTubeCommentsBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent")
            updateParentApp { it.copy(youtube = it.youtube.copy(commentsBlocked = enabled)) }
        else updateActiveChild { it.copy(youtube = it.youtube.copy(commentsBlocked = enabled)) }

    suspend fun setYouTubeSearchBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent")
            updateParentApp { it.copy(youtube = it.youtube.copy(searchBlocked = enabled)) }
        else updateActiveChild { it.copy(youtube = it.youtube.copy(searchBlocked = enabled)) }

    // ✅ ------------------- TWITTER BLOCKING -------------------
    suspend fun setTwitterExploreBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent")
            updateParentApp { it.copy(twitter = it.twitter.copy(exploreBlocked = enabled)) }
        else updateActiveChild { it.copy(twitter = it.twitter.copy(exploreBlocked = enabled)) }

    // ✅ ------------------- WHATSAPP BLOCKING -------------------
    suspend fun setWhatsAppStatusBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent")
            updateParentApp { it.copy(whatsapp = it.whatsapp.copy(statusBlocked = enabled)) }
        else updateActiveChild { it.copy(whatsapp = it.whatsapp.copy(statusBlocked = enabled)) }

    // ✅ ------------------- SNAPCHAT BLOCKING -------------------
    suspend fun setSnapchatSpotlightBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent")
            updateParentApp { it.copy(snapchat = it.snapchat.copy(spotlightBlocked = enabled)) }
        else updateActiveChild { it.copy(snapchat = it.snapchat.copy(spotlightBlocked = enabled)) }

    suspend fun setSnapchatStoriesBlocked(enabled: Boolean) =
        if (appSettings.value.accountMode == "Parent")
            updateParentApp { it.copy(snapchat = it.snapchat.copy(storiesBlocked = enabled)) }
        else updateActiveChild { it.copy(snapchat = it.snapchat.copy(storiesBlocked = enabled)) }

    // ✅ ------------------- UTILITY -------------------
    suspend fun getCurrentAppSettings(): AppSettings {
        return appSettings.first()
    }
}

