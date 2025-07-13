package com.example.v02

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.v02.ReelsBlockingService.AppSettings
import com.example.v02.ReelsBlockingService.DataStoreManager
import kotlinx.coroutines.*
import android.util.Log
import android.graphics.Rect
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class InstagramBlockAccessibilityService : AccessibilityService() {

    private lateinit var dataStore: DataStoreManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var settings = AppSettings()

    private var lastActionTime = 0L
    private val debounceMillis = 200L

    override fun onServiceConnected() {
        super.onServiceConnected()
        dataStore = DataStoreManager(this)

        serviceScope.launch {
            dataStore.appSettings.collect { latest ->
                settings = latest
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        val root = rootInActiveWindow ?: return
        val nowMin = currentMinuteOfDay()

        val isParent = settings.accountMode == "Parent"
        val sourceInstagram = if (isParent) settings.instagram else getActiveChild()?.instagram
        val sourceFacebook = if (isParent) settings.facebook else getActiveChild()?.facebook
        val sourceYoutube = if (isParent) settings.youtube else getActiveChild()?.youtube
        val sourceTwitter = if (isParent) settings.twitter else getActiveChild()?.twitter
        val sourceWhatsapp = if (isParent) settings.whatsapp else getActiveChild()?.whatsapp
        val sourceSnapchat = if (isParent) settings.snapchat else getActiveChild()?.snapchat

        when (pkg) {
            "com.instagram.android" -> {
                val insta = sourceInstagram
                if (insta != null && isWithinInterval(insta.blockedStart, insta.blockedEnd, nowMin)) {
                    if (insta.reelsBlocked) blockInstagramReels(root)
                    if (insta.storiesBlocked) blockInstagramStories(root)
                    if (insta.exploreBlocked) blockInstagramExplore(root)
                }
            }

            "com.facebook.katana" -> {
                val fb = sourceFacebook
                if (fb != null && isWithinInterval(fb.blockedStart, fb.blockedEnd, nowMin)) {
                    if (fb.reelsBlocked) blockFacebookReels(root)
                    if (fb.marketplaceBlocked) blockFacebookMarketplace(root)
                    if (fb.storiesBlocked) blockFacebookStories(root)
                }
            }

            "com.google.android.youtube" -> {
                val yt = sourceYoutube
                if (yt != null && isWithinInterval(yt.blockedStart, yt.blockedEnd, nowMin)) {
                    if (yt.shortsBlocked) blockYouTubeShorts(root)
                    if (yt.commentsBlocked) blockYouTubeComments(root)
                    if (yt.searchBlocked) blockYouTubeSearch(root)
                }
            }

            "com.twitter.android" -> {
                val twitter = sourceTwitter
                if (twitter != null && isWithinInterval(twitter.blockedStart, twitter.blockedEnd, nowMin)) {
                    if (twitter.exploreBlocked) blockTwitterExplore(root)
                }
            }

            "com.whatsapp" -> {
                val wa = sourceWhatsapp
                if (wa != null && isWithinInterval(wa.blockedStart, wa.blockedEnd, nowMin)) {
                    if (wa.statusBlocked) blockWhatsAppStatus(event, root)
                }
            }

            "com.snapchat.android" -> {
                val snap = sourceSnapchat
                if (snap != null && isWithinInterval(snap.blockedStart, snap.blockedEnd, nowMin)) {
                    if (snap.spotlightBlocked) blockSnapchatSpotlight(root)
                    if (snap.storiesBlocked) blockSnapchatStories(root)
                }
            }
        }
    }

    private fun getActiveChild() = settings.childProfiles.find { it.id == settings.activeChildId }
    // Existing Instagram methods
    private fun blockInstagramReels(root: AccessibilityNodeInfo) {
        val reelView = root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/clips_swipe_refresh_container").firstOrNull()
        if (reelView != null) {
            val feedTab = root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/feed_tab").firstOrNull()
            Log.d("INSTA_BLOCKER", "Instagram Reels detected, redirecting to Feed")
            exitTheDoom(feedTab, "Instagram Reels blocked")
        }
    }

    private fun blockInstagramStories(root: AccessibilityNodeInfo) {
        val storyView = root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/reel_viewer_root").firstOrNull()
        if (storyView != null) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            val feedTab = root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/feed_tab").firstOrNull()
            Log.d("INSTA_BLOCKER", "Instagram Stories detected, redirecting to Feed")
            exitTheDoom(feedTab, "Instagram Stories blocked")
        }
    }

    private fun blockInstagramExplore(root: AccessibilityNodeInfo) {
        val exploreTab = root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/search_tab").firstOrNull()
        if (exploreTab?.isSelected == true) {
            val feedTab = root.findAccessibilityNodeInfosByViewId("com.instagram.android:id/feed_tab").firstOrNull()
            Log.d("INSTA_BLOCKER", "Instagram Explore detected, redirecting to Feed")
            exitTheDoom(feedTab, "Instagram Explore blocked")
        }
    }

    // Existing Facebook methods
    private fun blockFacebookReels(root: AccessibilityNodeInfo) {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < debounceMillis) return

        val selectedTab = findSelectedTab(root)
        val selectedDesc = selectedTab?.contentDescription?.toString()?.lowercase()?.trim()

        if (selectedDesc != null) {
            Log.d("FB_BLOCKER", "Selected Tab: $selectedDesc")

            if (selectedDesc.startsWith("video")) {
                lastActionTime = now
                val homeNode = findNodeByDesc(root, "Home")
                if (homeNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) {
                    Log.d("FB_BLOCKER", "Reels blocked → Home tab clicked")
                } else {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    Log.d("FB_BLOCKER", "Reels blocked → Fallback BACK used")
                }
            }
        }
    }

    private fun blockFacebookMarketplace(root: AccessibilityNodeInfo) {
        val selectedTab = findSelectedTab(root)
        val selectedDesc = selectedTab?.contentDescription?.toString()?.lowercase()?.trim()

        if (selectedDesc != null) {
            Log.d("FB_BLOCKER", "Selected Tab: $selectedDesc")

            if (selectedDesc.contains("marketplace")) {
                lastActionTime = System.currentTimeMillis()
                val homeNode = findNodeByDesc(root, "Home")
                if (homeNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) {
                    Log.d("FB_BLOCKER", "Marketplace blocked → Home tab clicked")
                } else {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    Log.d("FB_BLOCKER", "Marketplace blocked → Fallback BACK used")
                }
            }
        }
    }

    private fun blockFacebookStories(root: AccessibilityNodeInfo) {
        val possibleViewerIds = listOf(
            "com.facebook.katana:id/story_viewer",
            "com.facebook.katana:id/reel_viewer_root"
        )

        for (viewId in possibleViewerIds) {
            val viewerNode = root.findAccessibilityNodeInfosByViewId(viewId).firstOrNull()
            if (viewerNode != null) {
                lastActionTime = System.currentTimeMillis()
                Log.d("FB_BLOCKER", "Facebook Story viewer detected via ID: $viewId")

                performGlobalAction(GLOBAL_ACTION_BACK)
                return
            }
        }

        val storyIndicators = listOf("Reply", "Send Message", "Seen by", "Pause")
        for (indicator in storyIndicators) {
            val node = findNodeWithText(root, indicator)
            if (node != null) {
                lastActionTime = System.currentTimeMillis()
                Log.d("FB_BLOCKER", "Facebook Story content detected → indicator: $indicator")

                performGlobalAction(GLOBAL_ACTION_BACK)
                return
            }
        }
    }

    // New YouTube methods
    private fun blockYouTubeShorts(root: AccessibilityNodeInfo) {
        if (detectYTShorts(root)) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            Log.d("YT_BLOCKER", "YouTube Shorts blocked")
        }
    }

    private fun blockYouTubeComments(root: AccessibilityNodeInfo) {
        if (detectYTComments(root)) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            Log.d("YT_BLOCKER", "YouTube Comments blocked")
        }
    }

    private fun blockYouTubeSearch(root: AccessibilityNodeInfo) {
        if (detectYTSearch(root)) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            Log.d("YT_BLOCKER", "YouTube Search blocked")
        }
    }

    private fun detectYTShorts(root: AccessibilityNodeInfo): Boolean {
        val need = mutableSetOf("search", "like", "comments", "share")
        var progressBarSeen = false
        val h = resources.displayMetrics.heightPixels
        val w = resources.displayMetrics.widthPixels

        val q = LinkedBlockingQueue<AccessibilityNodeInfo>().apply { add(root) }
        while (q.isNotEmpty()) {
            val n = q.poll() ?: continue
            if (!n.isVisibleToUser) continue

            val t = n.text?.toString()?.lowercase(Locale.ROOT)
            val d = n.contentDescription?.toString()?.lowercase(Locale.ROOT)
            need.removeAll { k -> t?.contains(k) == true || d?.contains(k) == true }

            val r = Rect()
            n.getBoundsInScreen(r)
            val atBottom = r.top > h * 0.85 && r.width() > w * 0.5 && r.height() < h * 0.05
            val cls = n.className?.toString()?.lowercase() ?: ""
            val isProgress = cls.contains("progressbar") || cls.contains("view")
            if (atBottom && isProgress) progressBarSeen = true

            repeat(n.childCount) { n.getChild(it)?.let(q::add) }
        }
        return need.isEmpty() && progressBarSeen
    }

    private fun detectYTComments(root: AccessibilityNodeInfo): Boolean {
        var editFocus = false
        var timestamp = false
        var likeSeen = false
        val h = resources.displayMetrics.heightPixels
        val q: Queue<AccessibilityNodeInfo> = LinkedList()
        q.add(root)

        while (q.isNotEmpty()) {
            val n = q.poll() ?: continue
            if (!n.isVisibleToUser) continue
            val t = n.text?.toString()?.lowercase() ?: ""
            val d = n.contentDescription?.toString()?.lowercase() ?: ""
            val cls = n.className?.toString()?.lowercase() ?: ""
            val r = Rect()
            n.getBoundsInScreen(r)

            if ((cls.contains("edittext") || n.isFocusable) &&
                r.top > h * 0.6 && r.height() < h * 0.1
            ) editFocus = true

            if (Regex("\\d+ (minute|hour|day|week|month|year)s? ago").matches(t)) timestamp = true
            if (t.contains("like") || d.contains("like") ||
                t.contains("dislike") || d.contains("dislike")
            ) likeSeen = true

            if (editFocus && timestamp && likeSeen) return true
            repeat(n.childCount) { n.getChild(it)?.let(q::add) }
        }
        return false
    }

    private fun detectYTSearch(root: AccessibilityNodeInfo): Boolean {
        val q = LinkedBlockingQueue<AccessibilityNodeInfo>().apply { add(root) }
        while (q.isNotEmpty()) {
            val n = q.poll() ?: continue
            val cls = n.className?.toString()?.lowercase() ?: ""
            if (cls.contains("edittext") && n.isVisibleToUser && n.isFocused) return true
            repeat(n.childCount) { n.getChild(it)?.let(q::add) }
        }
        return false
    }

    // New Twitter/X methods
    private fun blockTwitterExplore(root: AccessibilityNodeInfo) {
        if (detectXExplore(root)) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            Log.d("X_BLOCKER", "X Explore blocked")
        }
    }

    private fun detectXExplore(root: AccessibilityNodeInfo): Boolean {
        val q = LinkedBlockingQueue<AccessibilityNodeInfo>().apply { add(root) }
        while (q.isNotEmpty()) {
            val n = q.poll() ?: continue
            if (!n.isVisibleToUser) continue
            val t = n.text?.toString()?.lowercase(Locale.ROOT) ?: ""
            val d = n.contentDescription?.toString()?.lowercase(Locale.ROOT) ?: ""
            if ((t.contains("explore") || d.contains("explore")) && n.isSelected) return true
            repeat(n.childCount) { n.getChild(it)?.let(q::add) }
        }
        return false
    }

    // New WhatsApp methods
    private fun blockWhatsAppStatus(evt: AccessibilityEvent, root: AccessibilityNodeInfo) {
        if (detectWAStatus(evt, root)) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            Log.d("WA_BLOCKER", "WhatsApp Status blocked")
        }
    }

    private fun detectWAStatus(evt: AccessibilityEvent, root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        val viewer = evt.className?.contains("StatusPlaybackActivity", true) == true
        val keywords = listOf("just now", "minutes ago", "today", "yesterday", "am", "pm", "ago", "h")

        fun dfs(n: AccessibilityNodeInfo?): Boolean {
            if (n == null) return false
            val t = n.text?.toString()?.lowercase() ?: ""
            if (keywords.any { t.contains(it) }) return true
            for (i in 0 until n.childCount) if (dfs(n.getChild(i))) return true
            return false
        }
        return viewer && dfs(root)
    }

    // New Snapchat methods
    private fun blockSnapchatSpotlight(root: AccessibilityNodeInfo) {
        if (detectSnapSpotlight(root)) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            Log.d("SNAP_BLOCKER", "Snapchat Spotlight blocked")
        }
    }

    private fun blockSnapchatStories(root: AccessibilityNodeInfo) {
        if (detectSnapStories(root)) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            Log.d("SNAP_BLOCKER", "Snapchat Stories blocked")
        }
    }

    private fun detectSnapSpotlight(root: AccessibilityNodeInfo): Boolean {
        val kw = "spotlight"
        val top = 250
        val margin = 250
        val screenW = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                .currentWindowMetrics.bounds.width()
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                .defaultDisplay.getMetrics(dm)
            dm.widthPixels
        }

        val q: Queue<AccessibilityNodeInfo> = LinkedList()
        q.add(root)
        while (q.isNotEmpty()) {
            val n = q.poll() ?: continue
            if (!n.isVisibleToUser) continue
            val t = n.text?.toString()?.trim()?.lowercase() ?: ""
            val d = n.contentDescription?.toString()?.trim()?.lowercase() ?: ""
            if (t == kw || d == kw) {
                val r = Rect()
                n.getBoundsInScreen(r)
                if (r.top < top &&
                    r.left > screenW / 2 - margin &&
                    r.right < screenW / 2 + margin
                ) return true
            }
            for (i in 0 until n.childCount) n.getChild(i)?.let(q::add)
        }
        return false
    }

    private fun detectSnapStories(root: AccessibilityNodeInfo): Boolean {
        var title = false
        var section = false
        var bubbles = 0
        val hints = listOf("friends", "following", "discover")
        val uname = Regex("^[a-z0-9_]{3,}$")

        val q: Queue<AccessibilityNodeInfo> = LinkedList()
        q.add(root)
        while (q.isNotEmpty()) {
            val n = q.poll() ?: continue
            if (!n.isVisibleToUser) continue
            val t = n.text?.toString()?.lowercase() ?: ""
            val d = n.contentDescription?.toString()?.lowercase() ?: ""
            val cls = n.className?.toString()?.lowercase() ?: ""

            if (!title && (t == "stories" || t == "my story" || d == "stories")) title = true
            if (!section && hints.any { it == t || it == d }) section = true
            if (cls.contains("imageview") && d.contains("story")) bubbles++
            if (uname.matches(t)) bubbles++

            if (title && section && bubbles >= 3) return true
            for (i in 0 until n.childCount) n.getChild(i)?.let(q::add)
        }
        return false
    }

    // Helper methods
    private fun findSelectedTab(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isSelected && node.contentDescription != null) return node

        for (i in 0 until node.childCount) {
            val result = findSelectedTab(node.getChild(i))
            if (result != null) return result
        }
        return null
    }

    private fun exitTheDoom(tab: AccessibilityNodeInfo?, reason: String) {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < debounceMillis) return
        lastActionTime = now
        val success = tab?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        Log.d("INSTA_BLOCKER", "$reason: Clicked feed tab: $success")
    }

    private fun findNodeByDesc(node: AccessibilityNodeInfo?, desc: String): AccessibilityNodeInfo? {
        if (node == null) return null
        val description = node.contentDescription?.toString()?.trim()
        if (description != null && description.contains(desc, ignoreCase = true)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val result = findNodeByDesc(child, desc)
            if (result != null) return result
        }
        return null
    }

    private fun findNodeWithText(node: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
        if (node == null) return null
        val nodeText = node.text?.toString()?.trim()
        if (nodeText != null && nodeText.contains(text, ignoreCase = true)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val result = findNodeWithText(child, text)
            if (result != null) return result
        }
        return null
    }

    private fun currentMinuteOfDay(): Int {
        val now = System.currentTimeMillis()
        val offset = TimeZone.getDefault().getOffset(now)
        return (((now + offset) / 60000) % 1440).toInt()
    }

    private fun isWithinInterval(start: Int, end: Int, minute: Int): Boolean {
        return if (start <= end) {
            minute in start..end
        } else {
            minute >= start || minute <= end
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        serviceScope.cancel()
    }
}