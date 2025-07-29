package com.example.v02.ReelsBlockingService

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStoreManager = DataStoreManager(application)

    // ✅ ------------------- ACCOUNT INFO -------------------
    val childProfiles = dataStoreManager.childProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    val blockedCategories: StateFlow<Set<String>> =
        dataStoreManager.getBlockedCategories()

    fun setCategoryBlocked(category: String, isBlocked: Boolean) = viewModelScope.launch {
        dataStoreManager.setCategoryBlocked(category, isBlocked)
    }

    fun isCategoryBlocked(category: String): Boolean {
        return dataStoreManager.isCategoryBlocked(category)
    }


    fun setAccountMode(mode: String) = viewModelScope.launch {
        dataStoreManager.setAccountMode(mode)
    }

    suspend fun addOrUpdateChild(profile: ChildProfile) =
        dataStoreManager.addOrUpdateChildProfile(profile)

    suspend fun deleteChild(id: String) =
        dataStoreManager.deleteChildProfile(id)

    suspend fun setActiveChild(id: String) =
        dataStoreManager.setActiveChildProfile(id)

    suspend fun setPinCode(pin: String) =
        dataStoreManager.setPinCode(pin)

    suspend fun setSecretQA(q: String, a: String) =
        dataStoreManager.setSecretQA(q, a)

    suspend fun isSecretAnswerCorrect(answer: String): Boolean {
        val stored = secretAnswer.first()
        return stored.isNotEmpty() && stored.equals(answer.trim(), ignoreCase = true)
    }

    // ✅ ------------------- KEYWORD BLOCKING -------------------
    // ✅ ------------------- KEYWORD BLOCKING -------------------
    val blockedKeywordLists: StateFlow<BlockedKeywordLists> =
        appSettings.map { settings ->
            if (settings.accountMode == "Parent") {
                settings.blockedKeywordLists
            } else {
                settings.childProfiles.find { it.id == settings.activeChildId }?.blockedKeywordLists
                    ?: BlockedKeywordLists()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BlockedKeywordLists())

    val customBlockedKeywords: StateFlow<List<String>> =
        appSettings.map { settings ->
            if (settings.accountMode == "Parent") {
                settings.customBlockedKeywords
            } else {
                settings.childProfiles.find { it.id == settings.activeChildId }?.customBlockedKeywords
                    ?: emptyList()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setBlockedKeywordLists(lists: BlockedKeywordLists) = viewModelScope.launch {
        dataStoreManager.setBlockedKeywordLists(lists)
    }

    fun setCustomBlockedKeywords(keywords: List<String>) = viewModelScope.launch {
        dataStoreManager.setCustomBlockedKeywords(keywords)
    }

    // ✅ ------------------- PERMANENT BLOCKING -------------------
    val permanentlyBlockedApps: StateFlow<Set<String>> =
        dataStoreManager.getPermanentlyBlockedApps()

    fun isAppPermanentlyBlocked(packageName: String): Boolean {
        return dataStoreManager.isAppPermanentlyBlocked(packageName)
    }

    fun setAppPermanentlyBlocked(packageName: String, isBlocked: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setAppPermanentlyBlocked(packageName, isBlocked)
        }
    }

    // ✅ ------------------- CURRENT APP STATE HELPERS -------------------
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

    // ✅ ------------------- INSTAGRAM BLOCKING -------------------
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

    // ✅ ------------------- FACEBOOK BLOCKING -------------------
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

    // ✅ ------------------- YOUTUBE BLOCKING -------------------
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

    // ✅ ------------------- TWITTER BLOCKING -------------------
    val isTwitterExploreBlockingEnabled = currentTwitter.map { it.exploreBlocked }

    fun setTwitterExploreBlockingEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStoreManager.setTwitterExploreBlocked(enabled)
    }

    // ✅ ------------------- WHATSAPP BLOCKING -------------------
    val isWhatsAppStatusBlockingEnabled = currentWhatsApp.map { it.statusBlocked }

    fun setWhatsAppStatusBlocked(enabled: Boolean) = viewModelScope.launch {
        dataStoreManager.setWhatsAppStatusBlocked(enabled)
    }

    // ✅ ------------------- SNAPCHAT BLOCKING -------------------
    val isSnapchatSpotlightBlockingEnabled = currentSnapchat.map { it.spotlightBlocked }
    val isSnapchatStoriesBlockingEnabled = currentSnapchat.map { it.storiesBlocked }

    fun setSnapchatSpotlightBlockingEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStoreManager.setSnapchatSpotlightBlocked(enabled)
    }

    fun setSnapchatStoriesBlockingEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStoreManager.setSnapchatStoriesBlocked(enabled)
    }

    // ✅ ------------------- APP TIME LIMITS -------------------
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

    fun setAppTimeLimit(packageName: String, minutes: Int) = viewModelScope.launch {
        dataStoreManager.setAppTimeLimit(packageName, minutes)
    }
}
