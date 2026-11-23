package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.BookmarkEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.QuickNoteEntity
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassPillChip
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.NovaWallpaperBackground
import com.example.ui.theme.LocalNovaGlassColors
import com.example.viewmodel.ReadingArticle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter bookmarks...") },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search browsing history...") },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (filtered.isEmpty()) {
                EmptyGlassState(
                    icon = Icons.Rounded.History,
                    title = "Browsing History is Empty",
                    subtitle = "Websites you visit in standard tabs appear here automatically."
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
                                    Text(
                                        text = item.url,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = dateFormat.format(Date(item.visitedAt)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                    )
                                }
                                GlassIconButton(
                                    icon = Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.cd_delete),
                                    onClick = { onDeleteItem(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmClear) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            title = { Text("Clear Browsing Data?") },
            text = { Text("This clears browsing history, WebView cache, and site cookies.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showConfirmClear = false
                    }
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) {
                    Text("Cancel")
                }
            }
        )
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

        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (target.id == 0L) "New Quick Note" else "Edit Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("Notes") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Linked URL (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveNote(target.id, title, body, url)
                        showEditor = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditor = false }) {
                    Text("Cancel")
                }
            }
        )
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
