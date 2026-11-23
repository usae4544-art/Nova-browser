package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalNovaGlassColors
import com.example.ui.theme.MintSecure

/**
 * Interactive Live Preview showing exactly what happens to the browser viewport
 * when the user independently shows or hides the Top Floating Bar and Bottom Liquid Dock.
 */
@Composable
fun BrowserBarLayoutPreview(
    showTopBar: Boolean,
    showBottomBar: Boolean,
    onSetShowTopBar: (Boolean) -> Unit,
    onSetShowBottomBar: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalNovaGlassColors.current
    val haptic = LocalHapticFeedback.current

    val contentTopPadding by animateDpAsState(
        targetValue = if (showTopBar) 44.dp else 14.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "preview_top_inset"
    )
    val contentBottomPadding by animateDpAsState(
        targetValue = if (showBottomBar) 46.dp else 14.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "preview_bottom_inset"
    )

    val layoutSummary = when {
        showTopBar && showBottomBar -> "Both Top & Bottom Liquid Glass bars active"
        showTopBar && !showBottomBar -> "Top Bar only · Full-bleed bottom + corner menu handle"
        !showTopBar && showBottomBar -> "Bottom Dock only · Clean edge-to-edge top view"
        else -> "Ultra Clean Full View · Both bars hidden (corner handle active)"
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("bars_live_preview_section")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Visibility,
                    contentDescription = null,
                    tint = glass.accentStart,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Live Layout Preview",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Tap bars or switches",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Miniature Interactive Phone Browser Preview Frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            glass.accentStart.copy(alpha = 0.48f),
                            glass.glassBorderTop,
                            glass.accentEnd.copy(alpha = 0.36f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(8.dp)
        ) {
            // Miniature Webpage Viewport that dynamically expands when bars are hidden
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (glass.isDark) Color(0xFF0D1426).copy(alpha = 0.92f)
                        else Color(0xFFF8FAFC).copy(alpha = 0.94f)
                    )
            ) {
                // Simulated Live Webpage Content inside preview
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = 12.dp,
                            end = 12.dp,
                            top = contentTopPadding,
                            bottom = contentBottomPadding
                        )
                ) {
                    // Simulated webpage hero banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        glass.accentStart.copy(alpha = if (glass.isDark) 0.28f else 0.20f),
                                        glass.accentEnd.copy(alpha = if (glass.isDark) 0.24f else 0.16f)
                                    )
                                )
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.82f)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                            )
                        }
                    }

                    // Simulated webpage article rows
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.9f)
                    ) {
                        repeat(2) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = if (glass.isDark) 0.08f else 0.06f
                                        )
                                    )
                                    .padding(8.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.7f)
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                                    )
                                }
                            }
                        }
                    }
                }

                // Mini Top Floating Liquid Glass Omnibox Pill
                androidx.compose.animation.AnimatedVisibility(
                    visible = showTopBar,
                    enter = slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(RoundedCornerShape(50))
                            .background(glass.glassSurfaceStrong.copy(alpha = 0.72f))
                            .border(
                                width = 0.85.dp,
                                color = glass.accentStart.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(50)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSetShowTopBar(false)
                            }
                            .padding(horizontal = 10.dp)
                            .testTag("preview_mini_top_bar")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MintSecure,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "nova.browser",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                // Mini Bottom Floating iPhone Liquid Glass Dock
                androidx.compose.animation.AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 28.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .clip(RoundedCornerShape(50))
                            .background(glass.glassSurfaceStrong.copy(alpha = 0.74f))
                            .border(
                                width = 0.85.dp,
                                color = glass.glassBorderTop,
                                shape = RoundedCornerShape(50)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSetShowBottomBar(false)
                            }
                            .padding(horizontal = 10.dp)
                            .testTag("preview_mini_bottom_bar")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Home,
                            contentDescription = null,
                            tint = glass.accentStart,
                            modifier = Modifier.size(13.dp)
                        )
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(13.dp)
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(15.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, glass.accentStart, RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                text = "2",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // Mini Corner Quick-Access Handle Preview (shown automatically when Bottom Dock is hidden)
                androidx.compose.animation.AnimatedVisibility(
                    visible = !showBottomBar,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(glass.glassSurfaceStrong.copy(alpha = 0.80f))
                            .border(
                                width = 0.9.dp,
                                color = glass.accentStart.copy(alpha = 0.65f),
                                shape = CircleShape
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSetShowBottomBar(true)
                            }
                            .testTag("preview_corner_handle")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = "Quick menu handle preview",
                            tint = glass.accentStart,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        // Live Explanation Caption
        Text(
            text = layoutSummary,
            style = MaterialTheme.typography.bodySmall,
            color = glass.accentStart,
            modifier = Modifier.testTag("bars_preview_summary")
        )

        // Quick 1-Tap Layout Presets
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassPillChip(
                label = "Both",
                active = showTopBar && showBottomBar,
                onClick = {
                    onSetShowTopBar(true)
                    onSetShowBottomBar(true)
                },
                testTagName = "preset_bars_both",
                modifier = Modifier.weight(1f)
            )
            GlassPillChip(
                label = "Bottom",
                active = !showTopBar && showBottomBar,
                onClick = {
                    onSetShowTopBar(false)
                    onSetShowBottomBar(true)
                },
                testTagName = "preset_bars_bottom_only",
                modifier = Modifier.weight(1f)
            )
            GlassPillChip(
                label = "Top",
                active = showTopBar && !showBottomBar,
                onClick = {
                    onSetShowTopBar(true)
                    onSetShowBottomBar(false)
                },
                testTagName = "preset_bars_top_only",
                modifier = Modifier.weight(1f)
            )
            GlassPillChip(
                label = "Clean",
                active = !showTopBar && !showBottomBar,
                onClick = {
                    onSetShowTopBar(false)
                    onSetShowBottomBar(false)
                },
                testTagName = "preset_bars_clean",
                modifier = Modifier.weight(1f)
            )
        }
    }
}
