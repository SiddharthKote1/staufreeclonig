package com.example.v02.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.v02.ReelsBlockingService.MainViewModel

data class AppBlockingCategory(
    val appName: String,
    val features: List<BlockingFeature>
)

data class BlockingFeature(
    val name: String,
    val description: String,
    val isEnabled: Boolean,
    val onToggle: (Boolean) -> Unit
)

@Composable
fun InAppBlockingScreen(viewModel: MainViewModel = viewModel()) {
    // Instagram states
    val isReelsBlockingEnabled by viewModel.isReelsBlockingEnabled.collectAsState(initial = false)
    val isStoriesBlockingEnabled by viewModel.isStoriesBlockingEnabled.collectAsState(initial = false)
    val isExploreBlockingEnabled by viewModel.isExploreBlockingEnabled.collectAsState(initial = false)

    // Facebook states
    val isFBReelsBlockingEnabled by viewModel.isFBReelsBlockingEnabled.collectAsState(initial = false)
    val isFBMarketplaceBlockingEnabled by viewModel.isFBMarketplaceBlockingEnabled.collectAsState(initial = false)
    val isFBStoriesBlockingEnabled by viewModel.isFBStoriesBlockingEnabled.collectAsState(initial = false)

    // YouTube states
    val isYTShortsBlockingEnabled by viewModel.isYTShortsBlockingEnabled.collectAsState(initial = false)
    val isYTCommentsBlockingEnabled by viewModel.isYTCommentsBlockingEnabled.collectAsState(initial = false)
    val isYTSearchBlockingEnabled by viewModel.isYTSearchBlockingEnabled.collectAsState(initial = false)

    // Twitter states
    val isTwitterExploreBlockingEnabled by viewModel.isTwitterExploreBlockingEnabled.collectAsState(initial = false)

    // WhatsApp states
    val isWhatsAppStatusBlockingEnabled by viewModel.isWhatsAppStatusBlockingEnabled.collectAsState(initial = false)

    // Snapchat states
    val isSnapchatSpotlightBlockingEnabled by viewModel.isSnapchatSpotlightBlockingEnabled.collectAsState(initial = false)
    val isSnapchatStoriesBlockingEnabled by viewModel.isSnapchatStoriesBlockingEnabled.collectAsState(initial = false)

    val appCategories = listOf(
        AppBlockingCategory(
            appName = "Instagram",
            features = listOf(
                BlockingFeature(
                    name = "Block Reels",
                    description = "Automatically navigates away from Reels to keep you focused.",
                    isEnabled = isReelsBlockingEnabled,
                    onToggle = { viewModel.setReelsBlockingEnabled(it) }
                ),
                BlockingFeature(
                    name = "Block Stories",
                    description = "Automatically exits Stories view to avoid distractions.",
                    isEnabled = isStoriesBlockingEnabled,
                    onToggle = { viewModel.setStoriesBlockingEnabled(it) }
                ),
                BlockingFeature(
                    name = "Block Explore",
                    description = "Automatically navigates away from Explore to keep you focused.",
                    isEnabled = isExploreBlockingEnabled,
                    onToggle = { viewModel.setExploreBlockingEnabled(it) }
                )
            )
        ),
        AppBlockingCategory(
            appName = "Facebook",
            features = listOf(
                BlockingFeature(
                    name = "Block Reels",
                    description = "Automatically navigates away from Facebook Reels to help you stay focused.",
                    isEnabled = isFBReelsBlockingEnabled,
                    onToggle = { viewModel.setFBReelsBlockingEnabled(it) }
                ),
                BlockingFeature(
                    name = "Block Marketplace",
                    description = "Automatically exits Marketplace tab to minimize distractions.",
                    isEnabled = isFBMarketplaceBlockingEnabled,
                    onToggle = { viewModel.setFBMarketplaceBlockingEnabled(it) }
                ),
                BlockingFeature(
                    name = "Block Stories",
                    description = "Automatically closes Facebook Stories to help reduce distractions.",
                    isEnabled = isFBStoriesBlockingEnabled,
                    onToggle = { viewModel.setFBStoriesBlockingEnabled(it) }
                )
            )
        ),
        AppBlockingCategory(
            appName = "YouTube",
            features = listOf(
                BlockingFeature(
                    name = "Block Shorts",
                    description = "Automatically navigates away from YouTube Shorts to maintain focus.",
                    isEnabled = isYTShortsBlockingEnabled,
                    onToggle = { viewModel.setYTShortsBlockingEnabled(it) }
                ),
                BlockingFeature(
                    name = "Block Comments",
                    description = "Prevents viewing and interacting with video comments.",
                    isEnabled = isYTCommentsBlockingEnabled,
                    onToggle = { viewModel.setYTCommentsBlockingEnabled(it) }
                ),
                BlockingFeature(
                    name = "Block Search",
                    description = "Prevents using the search functionality to reduce browsing time.",
                    isEnabled = isYTSearchBlockingEnabled,
                    onToggle = { viewModel.setYTSearchBlockingEnabled(it) }
                )
            )
        ),
        AppBlockingCategory(
            appName = "X (Twitter)",
            features = listOf(
                BlockingFeature(
                    name = "Block Explore Tab",
                    description = "Prevents access to the Explore tab to reduce distractions.",
                    isEnabled = isTwitterExploreBlockingEnabled,
                    onToggle = { viewModel.setTwitterExploreBlockingEnabled(it) }
                )
            )
        ),
        AppBlockingCategory(
            appName = "WhatsApp",
            features = listOf(
                BlockingFeature(
                    name = "Block Status",
                    description = "Prevents viewing WhatsApp Status updates to reduce social media consumption.",
                    isEnabled = isWhatsAppStatusBlockingEnabled,
                    onToggle = { viewModel.setWhatsAppStatusBlocked(it) }

                )
            )
        ),
        AppBlockingCategory(
            appName = "Snapchat",
            features = listOf(
                BlockingFeature(
                    name = "Block Spotlight",
                    description = "Prevents access to Snapchat Spotlight content to avoid endless scrolling.",
                    isEnabled = isSnapchatSpotlightBlockingEnabled,
                    onToggle = { viewModel.setSnapchatSpotlightBlockingEnabled(it) }
                ),
                BlockingFeature(
                    name = "Block Stories",
                    description = "Prevents viewing Snapchat Stories to maintain focus.",
                    isEnabled = isSnapchatStoriesBlockingEnabled,
                    onToggle = { viewModel.setSnapchatStoriesBlockingEnabled(it) }
                )
            )
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "In-App Feature Blocking",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Block distracting features within apps",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Text(
            text = "Configure App Features",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(appCategories) { category ->
                AppBlockingCategoryCard(category = category)
            }

            // Add some bottom padding for better scrolling
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun AppBlockingCategoryCard(category: AppBlockingCategory) {
    val enabledCount = category.features.count { it.isEnabled }
    val totalCount = category.features.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.appName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                if (enabledCount > 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = "$enabledCount/$totalCount blocked",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            category.features.forEach { feature ->
                BlockingFeatureItem(feature = feature)
                if (feature != category.features.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun BlockingFeatureItem(feature: BlockingFeature) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feature.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = feature.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Switch(
                checked = feature.isEnabled,
                onCheckedChange = feature.onToggle
            )
        }
    }
}