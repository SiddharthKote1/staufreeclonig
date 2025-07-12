package com.example.v02.ReelsBlockingService

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AppSettings(
    val isParentMode: Boolean = true,
    val pinCode: String = "",
    val secretQuestion: String = "",
    val secretAnswer: String = "",
    val accountMode: String = "Parent",
    val activeChildId: String = "",
    val instagram: App = App(),
    val facebook: App = App(),
    val youtube: YouTubeApp = YouTubeApp(),
    val twitter: TwitterApp = TwitterApp(),
    val whatsapp: WhatsAppApp = WhatsAppApp(),
    val snapchat: SnapchatApp = SnapchatApp(),
    val childProfiles: List<ChildProfile> = emptyList()
)

@Serializable
data class App(
    val reelsBlocked: Boolean = false,
    val storiesBlocked: Boolean = false,
    val exploreBlocked: Boolean = false,
    val marketplaceBlocked: Boolean = false,
    val blockedStart: Int = 0,
    val blockedEnd: Int = 1439
)

@Serializable
data class YouTubeApp(
    val shortsBlocked: Boolean = false,
    val commentsBlocked: Boolean = false,
    val searchBlocked: Boolean = false,
    val blockedStart: Int = 0,
    val blockedEnd: Int = 1439
)

@Serializable
data class TwitterApp(
    val exploreBlocked: Boolean = false,
    val blockedStart: Int = 0,
    val blockedEnd: Int = 1439
)

@Serializable
data class WhatsAppApp(
    val statusBlocked: Boolean = false,
    val blockedStart: Int = 0,
    val blockedEnd: Int = 1439
)

@Serializable
data class SnapchatApp(
    val spotlightBlocked: Boolean = false,
    val storiesBlocked: Boolean = false,
    val blockedStart: Int = 0,
    val blockedEnd: Int = 1439
)

@Serializable
data class ChildProfile(
    val name: String,
    val id: String = UUID.randomUUID().toString()
)









