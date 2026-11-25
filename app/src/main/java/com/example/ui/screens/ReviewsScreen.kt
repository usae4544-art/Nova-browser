package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ReviewEntity
import com.example.data.local.UserAccountEntity
import com.example.ui.components.GlassIconButton
import com.example.ui.components.IphoneLiquidGlassPill
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.LiquidProgressBar
import com.example.ui.components.LocalSakugaVfx
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.LocalNovaGlassColors
import com.example.ui.theme.MintSecure
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReviewsScreen(
    reviews: List<ReviewEntity>,
    ownReview: ReviewEntity?,
    userAccount: UserAccountEntity?,
    isIncognito: Boolean,
    onSubmitReview: (rating: Int, comment: String) -> Unit,
    onLikeReview: (Long) -> Unit,
    onDeleteReview: (Long) -> Unit,
    onOpenAccount: () -> Unit,
    onBack: () -> Unit
) {
    val glass = LocalNovaGlassColors.current
    val sakugaVfx = LocalSakugaVfx.current
    val scope = rememberCoroutineScope()

    var selectedRating by remember { mutableIntStateOf(ownReview?.rating ?: 5) }
    var commentText by remember { mutableStateOf(ownReview?.comment.orEmpty()) }
    var submitButtonCenter by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(ownReview) {
        if (ownReview != null) {
            selectedRating = ownReview.rating
            commentText = ownReview.comment
        }
    }

    val totalReviews = reviews.size.coerceAtLeast(1)
    val avgScore = if (reviews.isEmpty()) 4.9 else reviews.map { it.rating }.average()
    val formattedScore = String.format(Locale.US, "%.1f", avgScore)

    // Distribution breakdown
    val count5 = reviews.count { it.rating == 5 }
    val count4 = reviews.count { it.rating == 4 }
    val count3 = reviews.count { it.rating == 3 }
    val count2 = reviews.count { it.rating == 2 }
    val count1 = reviews.count { it.rating == 1 }

    GlassOverlayScaffold(
        title = "Reviews & Ratings",
        isIncognito = isIncognito,
        onBack = onBack,
        trailingActions = {
            GlassIconButton(
                icon = Icons.Rounded.AccountCircle,
                contentDescription = "User Account",
                onClick = onOpenAccount,
                testTagName = "reviews_account_button"
            )
        }
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Overall Rating Summary Glass Card
            item {
                LiquidGlassSurface(
                    shape = RoundedCornerShape(26.dp),
                    strong = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reviews_summary_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = formattedScore,
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            brush = Brush.horizontalGradient(
                                                listOf(glass.accentStart, glass.accentEnd)
                                            )
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "/ 5",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    repeat(5) { i ->
                                        Icon(
                                            imageVector = Icons.Rounded.Star,
                                            contentDescription = null,
                                            tint = if (i < avgScore.toInt()) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "$totalReviews verified community reviews",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // Star Distribution Bars
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.width(140.dp)
                            ) {
                                StarDistributionRow(star = 5, count = count5, total = totalReviews)
                                StarDistributionRow(star = 4, count = count4, total = totalReviews)
                                StarDistributionRow(star = 3, count = count3, total = totalReviews)
                                StarDistributionRow(star = 2, count = count2, total = totalReviews)
                                StarDistributionRow(star = 1, count = count1, total = totalReviews)
                            }
                        }
                    }
                }
            }

            // Interactive Write a Review / Edit Review Section
            item {
                LiquidGlassSurface(
                    shape = RoundedCornerShape(26.dp),
                    accentBorder = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("write_review_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.RateReview,
                                    contentDescription = null,
                                    tint = glass.accentStart,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (ownReview != null) "Update Your Review" else "Rate Nova Browser",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }

                            if (userAccount != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(glass.glassSurfaceStrong)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(parseHexColor(userAccount.avatarColorHex))
                                    ) {
                                        Text(
                                            text = userAccount.displayName.take(1).uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = userAccount.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                TextButton(
                                    onClick = onOpenAccount,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Login to sync",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = glass.accentStart
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Animated 5-Star Selection Row
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (starIndex in 1..5) {
                                val isSelected = starIndex <= selectedRating
                                val starScale = remember { Animatable(1f) }
                                var starCenter by remember { mutableStateOf(Offset.Zero) }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp)
                                        .scale(starScale.value)
                                        .onGloballyPositioned { starCenter = it.boundsInRoot().center }
                                        .clickable {
                                            selectedRating = starIndex
                                            sakugaVfx?.triggerMicroSpark(starCenter)
                                            scope.launch {
                                                starScale.snapTo(1.35f)
                                                starScale.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessMedium
                                                    )
                                                )
                                            }
                                        }
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                        contentDescription = "$starIndex Star",
                                        tint = if (isSelected) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }

                        val feelingLabel = when (selectedRating) {
                            1 -> "Needs improvement"
                            2 -> "Fair experience"
                            3 -> "Good browser"
                            4 -> "Great & fast!"
                            5 -> "Outstanding masterpiece!"
                            else -> ""
                        }
                        Text(
                            text = feelingLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.accentStart,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Frosted Glass Review Comment Field with Typing Sakuga Trigger
                        var commentBoxCenter by remember { mutableStateOf(Offset.Zero) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 84.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(glass.glassSurfaceStrong)
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(glass.glassBorder, glass.glassBorderStrong)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .onGloballyPositioned { commentBoxCenter = it.boundsInRoot().center }
                                .padding(12.dp)
                        ) {
                            if (commentText.isEmpty()) {
                                Text(
                                    text = "Write your feedback, feature wishes, or experience...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                )
                            }
                            BasicTextField(
                                value = commentText,
                                onValueChange = { next ->
                                    if (next.length <= 500) {
                                        commentText = next
                                        sakugaVfx?.triggerTypewriterKeyAccent(commentBoxCenter)
                                    }
                                },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(glass.accentStart),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 60.dp)
                                    .testTag("review_comment_input")
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text(
                                text = "${commentText.length}/500",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (ownReview != null) {
                                    OutlinedButton(
                                        onClick = { onDeleteReview(ownReview.id) },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Delete review",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Delete", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                Button(
                                    onClick = {
                                        sakugaVfx?.triggerImpactBurst(submitButtonCenter)
                                        onSubmitReview(selectedRating, commentText)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = glass.accentStart,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .onGloballyPositioned { submitButtonCenter = it.boundsInRoot().center }
                                        .testTag("submit_review_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (ownReview != null) "Update" else "Post Review",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Community Reviews Title
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    Text(
                        text = "All Community Reviews",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(glass.accentStart.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${reviews.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.accentStart,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Reviews List
            items(reviews, key = { it.id }) { review ->
                CommunityReviewCard(
                    review = review,
                    onLike = { onLikeReview(review.id) }
                )
            }
        }
    }
}

@Composable
private fun StarDistributionRow(star: Int, count: Int, total: Int) {
    val glass = LocalNovaGlassColors.current
    val frac = if (total > 0) count.toFloat() / total.toFloat() else 0f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "$star★",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(18.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(glass.glassBorderStrong)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(frac)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(AmberWarning, glass.accentStart)
                        )
                    )
            )
        }
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(22.dp)
        )
    }
}

@Composable
private fun CommunityReviewCard(
    review: ReviewEntity,
    onLike: () -> Unit
) {
    val glass = LocalNovaGlassColors.current
    val sakugaVfx = LocalSakugaVfx.current
    var likeCenter by remember { mutableStateOf(Offset.Zero) }
    var hasLikedLocally by remember { mutableStateOf(false) }

    LiquidGlassSurface(
        shape = RoundedCornerShape(20.dp),
        accentBorder = review.isOwnReview,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("review_card_${review.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(parseHexColor(review.authorAvatarHex))
                    ) {
                        Text(
                            text = review.authorName.take(1).uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = review.authorName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            if (review.isOwnReview) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(glass.accentStart.copy(alpha = 0.20f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "You",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = glass.accentStart,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Verified user",
                                    tint = MintSecure,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = formatTimestamp(review.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Star Rating
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(5) { i ->
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = if (i < review.rating) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Like / Helpful interaction
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(glass.glassSurfaceStrong)
                        .onGloballyPositioned { likeCenter = it.boundsInRoot().center }
                        .clickable {
                            if (!hasLikedLocally) {
                                hasLikedLocally = true
                                sakugaVfx?.triggerMicroSpark(likeCenter)
                                onLike()
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (hasLikedLocally) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Helpful",
                        tint = if (hasLikedLocally) Color(0xFFFF5FA8) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${review.likesCount + if (hasLikedLocally) 1 else 0}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun parseHexColor(hex: String): Color {
    return runCatching {
        val clean = hex.removePrefix("#")
        val colorLong = clean.toLong(16)
        if (clean.length == 6) Color(colorLong or 0x00000000FF000000)
        else Color(colorLong)
    }.getOrDefault(Color(0xFF8B5CF6))
}

private fun formatTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 30 -> SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(timestamp))
        days > 0 -> "$days d ago"
        hours > 0 -> "$hours h ago"
        minutes > 0 -> "$minutes m ago"
        else -> "Just now"
    }
}
