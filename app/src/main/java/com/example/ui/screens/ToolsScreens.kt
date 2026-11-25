package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.local.BookmarkEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.QuickNoteEntity
import com.example.engine.UrlResolver
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassPillChip
import com.example.ui.components.IphoneLiquidGlassPill
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.NovaWallpaperBackground
import com.example.ui.components.consumeAllTouches
import com.example.ui.theme.LocalNovaGlassColors
import com.example.viewmodel.ReadingArticle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun GlassOverlayScaffold(
    title: String,
    isIncognito: Boolean,
    onBack: () -> Unit,
    trailingActions: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    BackHandler(onBack = onBack)
    Box(modifier = Modifier.fillMaxSize()) {
        NovaWallpaperBackground(isIncognito = isIncognito)
        Box(modifier = Modifier.matchParentSize().consumeAllTouches())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            LiquidGlassSurface(
                shape = RoundedCornerShape(24.dp),
                strong = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    GlassIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        onClick = onBack,
                        testTagName = "overlay_back_button"
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    trailingActions()
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                content()
            }
        }
    }
}

@Composable
fun BookmarksScreen(
    bookmarks: List<BookmarkEntity>,
    isIncognito: Boolean,
    onOpenUrl: (String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(bookmarks, searchQuery) {
        if (searchQuery.isBlank()) bookmarks
        else bookmarks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.url.contains(searchQuery, ignoreCase = true)
        }
    }

    GlassOverlayScaffold(
        title = "Bookmarks",
        isIncognito = isIncognito,
        onBack = onBack
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LiquidGlassSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Filter bookmarks...",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            if (filtered.isEmpty()) {
                EmptyGlassState(
                    icon = Icons.Rounded.Bookmarks,
                    title = "No Bookmarks Yet",
                    subtitle = "Tap the bookmark star in the menu on any website to save it here."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { item ->
                        LiquidGlassSurface(
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenUrl(item.url) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.url,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                GlassIconButton(
                                    icon = Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.cd_delete),
                                    onClick = { onDeleteBookmark(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    history: List<HistoryEntity>,
    isIncognito: Boolean,
    onOpenUrl: (String) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showConfirmClear by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    val filtered = remember(history, searchQuery) {
        if (searchQuery.isBlank()) history
        else history.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.url.contains(searchQuery, ignoreCase = true)
        }
    }

    GlassOverlayScaffold(
        title = "History",
        isIncognito = isIncognito,
        onBack = onBack,
        trailingActions = {
            if (history.isNotEmpty()) {
                GlassIconButton(
                    icon = Icons.Rounded.DeleteSweep,
                    contentDescription = "Clear browsing history",
                    onClick = { showConfirmClear = true },
                    testTagName = "clear_history_button"
                )
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LiquidGlassSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search browsing history...",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            if (filtered.isEmpty()) {
                EmptyGlassState(
                    icon = Icons.Rounded.History,
                    title = "Browsing History is Empty",
                    subtitle = "Websites you visit in standard tabs appear here automatically."
                )
            } else {
                Text(
                    text = "Tap to open · Swipe sideways to remove",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                )
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { item ->
                        SwipeableHistoryCard(
                            item = item,
                            formattedTime = dateFormat.format(Date(item.visitedAt)),
                            onOpen = { onOpenUrl(item.url) },
                            onDelete = { onDeleteItem(item.id) },
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(220),
                                fadeOutSpec = tween(180),
                                placementSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    if (showConfirmClear) {
        LiquidGlassModalDialog(onDismissRequest = { showConfirmClear = false }) {
            Text("Clear Browsing Data?", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This clears browsing history, WebView cache, and site cookies.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { showConfirmClear = false },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onClearAll()
                        showConfirmClear = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear All")
                }
            }
        }
    }
}

@Composable
fun QuickNotesScreen(
    notes: List<QuickNoteEntity>,
    currentPageUrl: String,
    isIncognito: Boolean,
    onSaveNote: (id: Long, title: String, content: String, sourceUrl: String) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onOpenUrl: (String) -> Unit,
    onBack: () -> Unit
) {
    var editingNote by remember { mutableStateOf<QuickNoteEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    GlassOverlayScaffold(
        title = "Quick Notes",
        isIncognito = isIncognito,
        onBack = onBack,
        trailingActions = {
            GlassIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = "New note",
                onClick = {
                    editingNote = QuickNoteEntity(
                        title = "",
                        content = "",
                        sourceUrl = if (currentPageUrl.startsWith("http")) currentPageUrl else ""
                    )
                    showEditor = true
                },
                testTagName = "add_note_button"
            )
        }
    ) {
        if (notes.isEmpty()) {
            EmptyGlassState(
                icon = Icons.Rounded.EditNote,
                title = "No Quick Notes",
                subtitle = "Jot down snippets, links, or ideas while browsing without leaving NOVA."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(notes, key = { it.id }) { note ->
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editingNote = note
                                showEditor = true
                            }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (note.sourceUrl.startsWith("http")) {
                                    GlassIconButton(
                                        icon = Icons.Rounded.OpenInBrowser,
                                        contentDescription = "Open linked webpage",
                                        onClick = { onOpenUrl(note.sourceUrl) },
                                        iconSize = 18.dp
                                    )
                                }
                                GlassIconButton(
                                    icon = Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.cd_delete),
                                    onClick = { onDeleteNote(note.id) },
                                    iconSize = 18.dp
                                )
                            }
                            if (note.content.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = note.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (note.sourceUrl.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = note.sourceUrl,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LocalNovaGlassColors.current.accentStart,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor && editingNote != null) {
        val target = editingNote!!
        var title by remember(target) { mutableStateOf(target.title) }
        var body by remember(target) { mutableStateOf(target.content) }
        var url by remember(target) { mutableStateOf(target.sourceUrl) }

        LiquidGlassModalDialog(onDismissRequest = { showEditor = false }) {
            Text(
                text = if (target.id == 0L) "New Quick Note" else "Edit Note",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Notes") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Linked URL (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { showEditor = false },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onSaveNote(target.id, title, body, url)
                        showEditor = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
fun ReadingModeScreen(
    article: ReadingArticle,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var fontSizeSp by remember { mutableIntStateOf(17) }
    var readerTheme by remember { mutableStateOf("DARK") } // DARK, SEPIA, LIGHT

    val bgColor = when (readerTheme) {
        "SEPIA" -> Color(0xFFF5ECD7)
        "LIGHT" -> Color(0xFFFAFAFC)
        else -> Color(0xFF0A0D1F)
    }
    val textColor = when (readerTheme) {
        "SEPIA" -> Color(0xFF3A2C1A)
        "LIGHT" -> Color(0xFF111827)
        else -> Color(0xFFE2E8F0)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.matchParentSize().consumeAllTouches())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                onClick = onBack,
                tint = textColor
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GlassPillChip(
                    label = "Dark",
                    active = readerTheme == "DARK",
                    onClick = { readerTheme = "DARK" }
                )
                GlassPillChip(
                    label = "Sepia",
                    active = readerTheme == "SEPIA",
                    onClick = { readerTheme = "SEPIA" }
                )
                GlassPillChip(
                    label = "Light",
                    active = readerTheme == "LIGHT",
                    onClick = { readerTheme = "LIGHT" }
                )
            }

            Row {
                GlassIconButton(
                    icon = Icons.Rounded.TextDecrease,
                    contentDescription = "Decrease font size",
                    onClick = { fontSizeSp = (fontSizeSp - 2).coerceAtLeast(13) },
                    tint = textColor
                )
                GlassIconButton(
                    icon = Icons.Rounded.TextIncrease,
                    contentDescription = "Increase font size",
                    onClick = { fontSizeSp = (fontSizeSp + 2).coerceAtMost(28) },
                    tint = textColor
                )
            }
        }

        if (article.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 14.dp)
            ) {
                Text(
                    text = article.title.ifBlank { "Reader View" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = textColor
                )
                if (article.byline.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = article.byline,
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor.copy(alpha = 0.65f)
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))

                if (article.paragraphs.isEmpty()) {
                    Text(
                        text = "No article paragraphs were detected on this page. Try opening an article or news story.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor.copy(alpha = 0.75f)
                    )
                } else {
                    article.paragraphs.forEach { para ->
                        Text(
                            text = para,
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp * 1.55f).sp,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun LiquidGlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val glass = LocalNovaGlassColors.current
    IphoneLiquidGlassPill(
        accentGlow = query.isNotEmpty(),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .padding(start = 14.dp, end = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = glass.accentStart,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.weight(1f)
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(glass.accentStart),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("liquid_glass_search_input")
                )
            }
            if (query.isNotEmpty()) {
                GlassIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Clear filter",
                    onClick = { onQueryChange("") },
                    iconSize = 18.dp
                )
            }
        }
    }
}

@Composable
private fun SwipeableHistoryCard(
    item: HistoryEntity,
    formattedTime: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalNovaGlassColors.current
    val scope = rememberCoroutineScope()
    var isDeleting by remember { mutableStateOf(false) }
    val dragX = remember { Animatable(0f) }

    val deleteScale by animateFloatAsState(
        targetValue = if (isDeleting) 0.90f else 1f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "history_delete_scale"
    )
    val deleteAlpha by animateFloatAsState(
        targetValue = if (isDeleting) 0f else (1f - (abs(dragX.value) / 380f)).coerceIn(0.28f, 1f),
        animationSpec = tween(160),
        label = "history_delete_alpha"
    )

    fun triggerDelete(dir: Float = 0f) {
        if (isDeleting) return
        isDeleting = true
        scope.launch {
            if (dir != 0f) {
                dragX.animateTo(dir * 420f, tween(180, easing = FastOutSlowInEasing))
            } else {
                delay(150L)
            }
            onDelete()
        }
    }

    val host = remember(item.url) { UrlResolver.extractHost(item.url).ifBlank { item.url } }
    val dragBlurPx = if (isDeleting) 14f else ((abs(dragX.value) - 24f).coerceAtLeast(0f) / 14f).coerceAtMost(12f)

    LiquidGlassSurface(
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (abs(dragX.value) > 2f || isDeleting) 1f else 0f)
            .graphicsLayer {
                translationX = dragX.value
                scaleX = deleteScale
                scaleY = deleteScale
                alpha = deleteAlpha
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dragBlurPx > 0.5f) {
                    renderEffect = BlurEffect(dragBlurPx, dragBlurPx, TileMode.Decal)
                }
            }
            .pointerInput(item.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(dragX.value) > 140f) {
                            triggerDelete(if (dragX.value > 0f) 1f else -1f)
                        } else {
                            scope.launch {
                                dragX.animateTo(
                                    0f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            dragX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch {
                            dragX.snapTo(dragX.value + dragAmount)
                        }
                    }
                )
            }
            .clickable(enabled = !isDeleting, onClick = onOpen)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$host · $formattedTime",
                    style = MaterialTheme.typography.labelSmall,
                    color = glass.accentStart.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            GlassIconButton(
                icon = Icons.Rounded.Delete,
                contentDescription = stringResource(R.string.cd_delete),
                onClick = { triggerDelete() }
            )
        }
    }
}

@Composable
fun EmptyGlassState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassSurface(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = LocalNovaGlassColors.current.accentStart,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
