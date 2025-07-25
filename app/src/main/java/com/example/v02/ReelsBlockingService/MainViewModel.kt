package com.example.v02.ReelsBlockingService

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStoreManager = DataStoreManager(application)

    val childProfiles = dataStoreManager.childProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val activeChildId = dataStoreManager.activeChildId
    val accountMode = dataStoreManager.accountMode
    val pinCode = dataStoreManager.pinCode
    val secretQuestion = dataStoreManager.secretQuestion
    val secretAnswer = dataStoreManager.secretAnswer

    val hasPin: Flow<Boolean> = pinCode.map { it.isNotBlank() }
    val hasSecretQA: Flow<Boolean> = secretQuestion.map { it.isNotBlank() }

    val appSettings = dataStoreManager.appSettings

    val activeChild: StateFlow<ChildProfile?> = appSettings
        .map { settings -> settings.childProfiles.find { it.id == settings.activeChildId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isParentMode: Flow<Boolean> = appSettings.map { it.accountMode == "Parent" }

    // Account Setup
    suspend fun addOrUpdateChild(profile: ChildProfile) =
        dataStoreManager.addOrUpdateChildProfile(profile)

    suspend fun deleteChild(id: String) =
        dataStoreManager.deleteChildProfile(id)

    suspend fun setActiveChild(id: String) =
        dataStoreManager.setActiveChildProfile(id)

    suspend fun setAccountMode(mode: String) =
        dataStoreManager.setAccountMode(mode)

    suspend fun setPinCode(pin: String) =
        dataStoreManager.setPinCode(pin)

    suspend fun setSecretQA(q: String, a: String) =
        dataStoreManager.setSecretQA(q, a)

    suspend fun isSecretAnswerCorrect(answer: String): Boolean {
        val stored = secretAnswer.first()
        return stored.isNotEmpty() && stored.equals(answer.trim(), ignoreCase = true)
    }

    // Feature Block Config (Child-specific or Parent)
    private val currentInstagram: Flow<App> = appSettings.map {
        if (it.accountMode == "Parent") it.instagram
        else it.childProfiles.find { c -> c.id == it.activeChildId }?.instagram ?: App()
    }

    private val currentFacebook: Flow<App> = appSettings.map {
        if (it.accountMode == "Parent") it.facebook
        else it.childProfiles.find { c -> c.id == it.activeChildId }?.facebook ?: App()
    }

    private val currentYouTube: Flow<YouTubeApp> = appSettings.map {
        if (it.accountMode == "Parent") it.youtube
        else it.childProfiles.find { c -> c.id == it.activeChildId }?.youtube ?: YouTubeApp()
    }

    private val currentTwitter: Flow<TwitterApp> = appSettings.map {
        if (it.accountMode == "Parent") it.twitter
        else it.childProfiles.find { c -> c.id == it.activeChildId }?.twitter ?: TwitterApp()
    }

    private val currentWhatsApp: Flow<WhatsAppApp> = appSettings.map {
        if (it.accountMode == "Parent") it.whatsapp
        else it.childProfiles.find { c -> c.id == it.activeChildId }?.whatsapp ?: WhatsAppApp()
    }

    private val currentSnapchat: Flow<SnapchatApp> = appSettings.map {
        if (it.accountMode == "Parent") it.snapchat
        else it.childProfiles.find { c -> c.id == it.activeChildId }?.snapchat ?: SnapchatApp()
    }

    // Instagram
    val isReelsBlockingEnabled = currentInstagram.map { it.reelsBlocked }
    val isStoriesBlockingEnabled = currentInstagram.map { it.storiesBlocked }
    val isExploreBlockingEnabled = currentInstagram.map { it.exploreBlocked }

    fun setReelsBlockingEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStoreManager.setInstagramReelsBlocked(enabled)
    }

    fun setStoriesBlockingEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStoreManager.setInstagramStoriesBlocked(enabled)
    }

    fun setExploreBlockingEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStoreManager.setInstagramExploreBlocked(enabled)
    }

    // Facebook
    val isFBReelsBlockingEnabled = currentFacebook.map { it.reelsBlocked }
    val isFBMarketplaceBlockingEnabled = currentFacebook.map { it.marketplaceBlocked }
    val isFBStoriesBlockingEnabled = currentFacebook.map { it.storiesBlocked }

    fun setFBReelsBlockingEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStoreManager.setFacebookReelsBlocked(enabled)
    }

    fun setFBMarketplaceBlockingEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStoreManager.setFacebookMarketplaceBlocked(enabled)
    }

    fun setFBStoriesBlockingEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStoreManager.setFacebookStoriesBlocked(enabled)
    }

    // YouTube
    val isYTShortsBlockingEnabled = currentYouTube.map { it.shortsBlocked }
    val isYTCommentsBlockingEnabled = currentYouTube.map { it.commentsBlocked }
    val isYTSearchBlockingEnabled = currentYouTube.map { it.searchBlocked }

    fun setYTShortsBlockingEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStoreManager.setYouTubeShortsBlocked(enabled)
    }

    fun setYTCommentsBlockingEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStoreManager.setYouTubeCommentsBlocked(enabled)
    }

    fun setYTSearchBlockingEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStoreManager.setYouTubeSearchBlocked(enabled)
    }

    // Twitter
    val isTwitterExploreBlockingEnabled = currentTwitter.map { it.exploreBlocked }

    fun setTwitterExploreBlockingEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStoreManager.setTwitterExploreBlocked(enabled)
    }

    // WhatsApp
    val isWhatsAppStatusBlockingEnabled = currentWhatsApp.map { it.statusBlocked }

    fun setWhatsAppStatusBlocked(enabled: Boolean) = viewModelScope.launch {
        dataStoreManager.setWhatsAppStatusBlocked(enabled)
    }

    // Snapchat
    val isSnapchatSpotlightBlockingEnabled = currentSnapchat.map { it.spotlightBlocked }
    val isSnapchatStoriesBlockingEnabled = currentSnapchat.map { it.storiesBlocked }

    fun setSnapchatSpotlightBlockingEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStoreManager.setSnapchatSpotlightBlocked(enabled)
    }

    fun setSnapchatStoriesBlockingEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStoreManager.setSnapchatStoriesBlocked(enabled)
    }

    // App Time Limits (used in AppLimitsScreen + SetLimitScreen)
    fun getAppTimeLimits(): Flow<Map<String, Int>> {
        return appSettings.map { settings ->
            if (settings.accountMode == "Parent") {
                settings.parentAppTimeLimits
            } else {
                settings.childProfiles.find { it.id == settings.activeChildId }?.appTimeLimits
                    ?: emptyMap()
            }
        }
    }

    fun getBlockedCategories(): Flow<Map<String, Boolean>> =
        dataStoreManager.getBlockedCategories()

    fun setCategoryBlocked(category: String, blocked: Boolean) =
        viewModelScope.launch {
            dataStoreManager.setCategoryBlocked(category, blocked)
        }



    fun setAppTimeLimit(packageName: String, minutes: Int) = viewModelScope.launch {
        dataStoreManager.setAppTimeLimit(packageName, minutes)
    }
}