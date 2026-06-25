package com.example.smartnewsapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.smartnewsapp.data.local.Article

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    articleId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val article by viewModel.article.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var autoScrollEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(articleId) {
        viewModel.initArticle(articleId)
    }

    LaunchedEffect(messages.size, isTyping) {
        if (autoScrollEnabled && messages.isNotEmpty()) {
            val targetIndex = if (isTyping) {
                messages.size + 1
            } else {
                val lastUserMsgIndex = messages.indexOfLast { it.isUser }
                if (lastUserMsgIndex != -1) lastUserMsgIndex + 1 else messages.size
            }
            if (targetIndex > 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat: ${article?.title?.take(20) ?: "..."}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { 
                            inputText = it
                            autoScrollEnabled = true
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask Hermes about this article...") }
                    )
                    TextButton(
                        onClick = {
                            autoScrollEnabled = true
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        },
                        enabled = inputText.isNotBlank() && !isTyping
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                article?.let {
                    ArticleDetailHeader(
                        article = it,
                        onSuggestedQuestionClick = { q ->
                            autoScrollEnabled = true
                            viewModel.sendMessage(q)
                        },
                        onLike = {
                            val keywords = listOf(it.category, it.source.name) + (it.metadata.tags ?: emptyList())
                            viewModel.handleArticleUpvote(it.id, keywords)
                        },
                        onDislike = {
                            val keywords = listOf(it.category, it.source.name) + (it.metadata.tags ?: emptyList())
                            viewModel.handleArticleDownvote(it.id, keywords)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            items(messages) { msg ->
                MessageBubble(text = msg.message, isUser = msg.isUser, provider = msg.provider)
            }
            if (isTyping) {
                item {
                    Text("Hermes is typing...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(text: String, isUser: Boolean, provider: String = "") {
    val clipboardManager = LocalClipboardManager.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser && provider.isNotBlank()) {
            Text(
                text = provider,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
            )
        }
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .widthIn(max = 280.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        clipboardManager.setText(AnnotatedString(text))
                    }
                )
        ) {
            SelectionContainer {
                Text(
                    text = text,
                    modifier = Modifier.padding(12.dp),
                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArticleDetailHeader(
    article: Article,
    onSuggestedQuestionClick: (String) -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    SelectionContainer {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            if (article.media.image.url != null && article.media.image.type != "none" && article.media.image.type != "fallback_category") {
                AsyncImage(
                    model = article.media.image.url,
                    contentDescription = article.media.image.alt ?: "Article Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.LightGray, RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "By ${article.source.name} • ${article.category.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row {
                    val isLiked = article.feedback == 1
                    val isDisliked = article.feedback == -1

                    IconButton(onClick = onLike, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.ThumbUp, 
                            contentDescription = "Like", 
                            modifier = Modifier.size(16.dp), 
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                    IconButton(onClick = onDislike, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.ThumbDown, 
                            contentDescription = "Dislike", 
                            modifier = Modifier.size(16.dp), 
                            tint = if (isDisliked) MaterialTheme.colorScheme.error else Color.Gray
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = article.content.expanded,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Why it matters highlighted box
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Why it matters",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = article.content.whyItMatters,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            
            // Source URL
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Source: ${article.source.url}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    try {
                        uriHandler.openUri(article.source.url)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            )

            // Assistant Prompts
            article.assistantPrompts?.suggestedQuestions?.let { questions ->
                if (questions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Ask Hermes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        questions.forEach { question ->
                            SuggestionChip(
                                onClick = { onSuggestedQuestionClick(question) },
                                label = { Text(question) }
                            )
                        }
                    }
                }
            }
        }
    }
}
