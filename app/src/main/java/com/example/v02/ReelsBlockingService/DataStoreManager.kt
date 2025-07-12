import com.example.v02.ReelsBlockingService.AppSettings
import com.example.v02.ReelsBlockingService.ChildProfile
import com.example.v02.ReelsBlockingService.DataStoreProvider
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreManager(context: Context) {

    private val dataStore = DataStoreProvider.getInstance(context)

    /* ───────── Child Profiles ───────── */

    val childProfiles: Flow<List<ChildProfile>> = dataStore.data.map { it.childProfiles }
    val activeChildId: Flow<String> = dataStore.data.map { it.activeChildId }

    suspend fun addOrUpdateChildProfile(profile: ChildProfile) {
        dataStore.updateData { current ->
            val updated = current.childProfiles.toMutableList()
            val index = updated.indexOfFirst { it.id == profile.id }
            if (index >= 0) updated[index] = profile else updated.add(profile)
            current.copy(childProfiles = updated)
        }
    }

    suspend fun deleteChildProfile(id: String) {
        dataStore.updateData { current ->
            val updated = current.childProfiles.filterNot { it.id == id }
            val newActiveId = if (current.activeChildId == id) "" else current.activeChildId
            current.copy(childProfiles = updated, activeChildId = newActiveId)
        }
    }

    suspend fun setActiveChildProfile(id: String) {
        dataStore.updateData { it.copy(activeChildId = id) }
    }

    /* ───────── Global flows ───────── */

    val appSettings: Flow<AppSettings> = dataStore.data
    val accountMode: Flow<String> = dataStore.data.map { it.accountMode }
    val pinCode: Flow<String> = dataStore.data.map { it.pinCode }
    val secretQuestion: Flow<String> = dataStore.data.map { it.secretQuestion }
    val secretAnswer: Flow<String> = dataStore.data.map { it.secretAnswer }

    /* ───────── Global setters ───────── */

    suspend fun setAccountMode(mode: String) {
        dataStore.updateData { cur -> cur.copy(accountMode = mode) }
    }

    suspend fun setPinCode(newPin: String) {
        dataStore.updateData { cur -> cur.copy(pinCode = newPin) }
    }

    suspend fun setSecretQA(question: String, answer: String) {
        dataStore.updateData { current ->
            current.copy(secretQuestion = question, secretAnswer = answer)
        }
    }

    /* ───────── Instagram ───────── */

    suspend fun setInstagramReelsBlocking(enabled: Boolean) =
        dataStore.updateData { it.copy(instagram = it.instagram.copy(reelsBlocked = enabled)) }

    suspend fun setInstagramStoriesBlocking(enabled: Boolean) =
        dataStore.updateData { it.copy(instagram = it.instagram.copy(storiesBlocked = enabled)) }

    suspend fun setInstagramExploreBlocking(enabled: Boolean) =
        dataStore.updateData { it.copy(instagram = it.instagram.copy(exploreBlocked = enabled)) }

    suspend fun setInstagramBlockTime(start: Int, end: Int) =
        dataStore.updateData {
            it.copy(instagram = it.instagram.copy(blockedStart = start, blockedEnd = end))
        }

    /* ───────── Facebook ───────── */

    suspend fun setFacebookReelsBlocking(enabled: Boolean) =
        dataStore.updateData { it.copy(facebook = it.facebook.copy(reelsBlocked = enabled)) }

    suspend fun setFacebookMarketplaceBlocking(enabled: Boolean) =
        dataStore.updateData { it.copy(facebook = it.facebook.copy(marketplaceBlocked = enabled)) }

    suspend fun setFacebookStoriesBlocking(enabled: Boolean) =
        dataStore.updateData { it.copy(facebook = it.facebook.copy(storiesBlocked = enabled)) }

    /* ───────── YouTube ───────── */

    suspend fun setYouTubeShortsBlocking(enabled: Boolean) =
        dataStore.updateData { it.copy(youtube = it.youtube.copy(shortsBlocked = enabled)) }

    suspend fun setYouTubeCommentsBlocking(enabled: Boolean) =
        dataStore.updateData { it.copy(youtube = it.youtube.copy(commentsBlocked = enabled)) }

    suspend fun setYouTubeSearchBlocking(enabled: Boolean) =
        dataStore.updateData { it.copy(youtube = it.youtube.copy(searchBlocked = enabled)) }

    suspend fun setYouTubeBlockTime(start: Int, end: Int) =
        dataStore.updateData {
            it.copy(youtube = it.youtube.copy(blockedStart = start, blockedEnd = end))
        }

    /* ───────── Twitter ───────── */

    suspend fun setTwitterExploreBlocking(enabled: Boolean) =
        dataStore.updateData { it.copy(twitter = it.twitter.copy(exploreBlocked = enabled)) }

    suspend fun setTwitterBlockTime(start: Int, end: Int) =
        dataStore.updateData {
            it.copy(twitter = it.twitter.copy(blockedStart = start, blockedEnd = end))
        }

    /* ───────── WhatsApp ───────── */

    suspend fun setWhatsAppStatusBlocking(enabled: Boolean) =
        dataStore.updateData { it.copy(whatsapp = it.whatsapp.copy(statusBlocked = enabled)) }

    suspend fun setWhatsAppBlockTime(start: Int, end: Int) =
        dataStore.updateData {
            it.copy(whatsapp = it.whatsapp.copy(blockedStart = start, blockedEnd = end))
        }

    /* ───────── Snapchat ───────── */

    suspend fun setSnapchatSpotlightBlocking(enabled: Boolean) =
        dataStore.updateData { it.copy(snapchat = it.snapchat.copy(spotlightBlocked = enabled)) }

    suspend fun setSnapchatStoriesBlocking(enabled: Boolean) =
        dataStore.updateData { it.copy(snapchat = it.snapchat.copy(storiesBlocked = enabled)) }
}
