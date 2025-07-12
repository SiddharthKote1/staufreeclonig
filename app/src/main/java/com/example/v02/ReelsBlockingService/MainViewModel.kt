package com.example.v02.ReelsBlockingService

import DataStoreManager
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStoreManager = DataStoreManager(application)

    val childProfiles: StateFlow<List<ChildProfile>> =
        dataStoreManager.childProfiles.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            emptyList()
        )

    val accountMode = dataStoreManager.accountMode
    val pinCode = dataStoreManager.pinCode
    val secretQuestion = dataStoreManager.secretQuestion
    val secretAnswer = dataStoreManager.secretAnswer
    val activeChildId = dataStoreManager.activeChildId

    suspend fun setAccountMode(mode: String) = dataStoreManager.setAccountMode(mode)
    suspend fun setPinCode(newPin: String) = dataStoreManager.setPinCode(newPin)
    suspend fun setSecretQA(question: String, answer: String) =
        dataStoreManager.setSecretQA(question, answer)

    suspend fun addOrUpdateChild(profile: ChildProfile) =
        dataStoreManager.addOrUpdateChildProfile(profile)

    suspend fun deleteChild(id: String) = dataStoreManager.deleteChildProfile(id)
    suspend fun setActiveChild(id: String) = dataStoreManager.setActiveChildProfile(id)

    suspend fun isSecretAnswerCorrect(answer: String): Boolean {
        val stored = secretAnswer.first()
        return stored.isNotEmpty() && answer.lowercase().trim() == stored
    }

    /* Instagram */
    val isReelsBlockingEnabled: Flow<Boolean> =
        dataStoreManager.appSettings.map { it.instagram.reelsBlocked }
    val isStoriesBlockingEnabled: Flow<Boolean> =
        dataStoreManager.appSettings.map { it.instagram.storiesBlocked }
    val isExploreBlockingEnabled: Flow<Boolean> =
        dataStoreManager.appSettings.map { it.instagram.exploreBlocked }

    fun setReelsBlockingEnabled(enabled: Boolean) =
        viewModelScope.launch { dataStoreManager.setInstagramReelsBlocking(enabled) }

    fun setStoriesBlockingEnabled(enabled: Boolean) =
        viewModelScope.launch { dataStoreManager.setInstagramStoriesBlocking(enabled) }

    fun setExploreBlockingEnabled(enabled: Boolean) =
        viewModelScope.launch { dataStoreManager.setInstagramExploreBlocking(enabled) }

    fun setInstagramBlockTime(start: Int, end: Int) =
        viewModelScope.launch { dataStoreManager.setInstagramBlockTime(start, end) }

    /* Facebook */
    val isFBReelsBlockingEnabled: Flow<Boolean> =
        dataStoreManager.appSettings.map { it.facebook.reelsBlocked }
    val isFBMarketplaceBlockingEnabled: Flow<Boolean> =
        dataStoreManager.appSettings.map { it.facebook.marketplaceBlocked }
    val isFBStoriesBlockingEnabled: Flow<Boolean> =
        dataStoreManager.appSettings.map { it.facebook.storiesBlocked }

    fun setFBReelsBlockingEnabled(enabled: Boolean) =
        viewModelScope.launch { dataStoreManager.setFacebookReelsBlocking(enabled) }

    fun setFBMarketplaceBlockingEnabled(enabled: Boolean) =
        viewModelScope.launch { dataStoreManager.setFacebookMarketplaceBlocking(enabled) }

    fun setFBStoriesBlockingEnabled(enabled: Boolean) =
        viewModelScope.launch { dataStoreManager.setFacebookStoriesBlocking(enabled) }

    /* YouTube */
    val isYTShortsBlockingEnabled: Flow<Boolean> =
        dataStoreManager.appSettings.map { it.youtube.shortsBlocked }
    val isYTCommentsBlockingEnabled: Flow<Boolean> =
        dataStoreManager.appSettings.map { it.youtube.commentsBlocked }
    val isYTSearchBlockingEnabled: Flow<Boolean> =
        dataStoreManager.appSettings.map { it.youtube.searchBlocked }

    fun setYTShortsBlockingEnabled(enabled: Boolean) =
        viewModelScope.launch { dataStoreManager.setYouTubeShortsBlocking(enabled) }

    fun setYTCommentsBlockingEnabled(enabled: Boolean) =
        viewModelScope.launch { dataStoreManager.setYouTubeCommentsBlocking(enabled) }

    fun setYTSearchBlockingEnabled(enabled: Boolean) =
        viewModelScope.launch { dataStoreManager.setYouTubeSearchBlocking(enabled) }

    fun setYouTubeBlockTime(start: Int, end: Int) =
        viewModelScope.launch { dataStoreManager.setYouTubeBlockTime(start, end) }

    /* Twitter */
    val isTwitterExploreBlockingEnabled: Flow<Boolean> =
        dataStoreManager.appSettings.map { it.twitter.exploreBlocked }

    fun setTwitterExploreBlockingEnabled(enabled: Boolean) =
        viewModelScope.launch { dataStoreManager.setTwitterExploreBlocking(enabled) }

    fun setTwitterBlockTime(start: Int, end: Int) =
        viewModelScope.launch { dataStoreManager.setTwitterBlockTime(start, end) }

    /* WhatsApp */
    val isWhatsAppStatusBlockingEnabled: Flow<Boolean> =
        dataStoreManager.appSettings.map { it.whatsapp.statusBlocked }

    fun setWhatsAppStatusBlockingEnabled(enabled: Boolean) =
        viewModelScope.launch { dataStoreManager.setWhatsAppStatusBlocking(enabled) }

    fun setWhatsAppBlockTime(start: Int, end: Int) =
        viewModelScope.launch { dataStoreManager.setWhatsAppBlockTime(start, end) }

    /* Snapchat */
    val isSnapchatSpotlightBlockingEnabled: Flow<Boolean> =
        dataStoreManager.appSettings.map { it.snapchat.spotlightBlocked }
    val isSnapchatStoriesBlockingEnabled: Flow<Boolean> =
        dataStoreManager.appSettings.map { it.snapchat.storiesBlocked }

    fun setSnapchatSpotlightBlockingEnabled(enabled: Boolean) =
        viewModelScope.launch { dataStoreManager.setSnapchatSpotlightBlocking(enabled) }

    fun setSnapchatStoriesBlockingEnabled(enabled: Boolean) =
        viewModelScope.launch { dataStoreManager.setSnapchatStoriesBlocking(enabled) }

    val hasPin: Flow<Boolean> = pinCode.map { it.isNotBlank() }
    val hasSecretQA: Flow<Boolean> = secretQuestion.map { it.isNotBlank() }
}

