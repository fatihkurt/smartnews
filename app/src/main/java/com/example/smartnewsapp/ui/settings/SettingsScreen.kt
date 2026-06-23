package com.example.smartnewsapp.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartnewsapp.domain.ChatProvider
import com.example.smartnewsapp.domain.NewsProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    
    var chatExpanded by remember { mutableStateOf(false) }
    var newsExpanded by remember { mutableStateOf(false) }
    var openRouterApiKeyInput by remember { mutableStateOf(settings.openRouterApiKey) }
    var hermesApiKeyInput by remember { mutableStateOf(settings.hermesApiKey) }
    var hermesChatUrlInput by remember { mutableStateOf(settings.hermesChatUrl) }
    var hermesModelInput by remember { mutableStateOf(settings.hermesModel) }
    var modelInput by remember { mutableStateOf(settings.openRouterModel) }
    var newsSourceInput by remember { mutableStateOf(settings.newsSourceUrl) }

    // Sync input state when flow emits initially
    LaunchedEffect(settings) {
        if (openRouterApiKeyInput.isEmpty() && settings.openRouterApiKey.isNotEmpty()) {
            openRouterApiKeyInput = settings.openRouterApiKey
        }
        if (hermesApiKeyInput.isEmpty() && settings.hermesApiKey.isNotEmpty()) {
            hermesApiKeyInput = settings.hermesApiKey
        }
        if (hermesChatUrlInput == "https://hermes.vectororch.com/v1/chat/completions" && settings.hermesChatUrl != "https://hermes.vectororch.com/v1/chat/completions") {
            hermesChatUrlInput = settings.hermesChatUrl
        }
        if (hermesModelInput == "hermes-agent" && settings.hermesModel != "hermes-agent") {
            hermesModelInput = settings.hermesModel
        }
        if (modelInput == "openai/gpt-4o" && settings.openRouterModel != "openai/gpt-4o") {
            modelInput = settings.openRouterModel
        }
        if (newsSourceInput == "https://hermes.vectororch.com/api/news" && settings.newsSourceUrl != "https://hermes.vectororch.com/api/news") {
            newsSourceInput = settings.newsSourceUrl
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- CHAT CONFIGURATION ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Chat Configuration", style = MaterialTheme.typography.titleMedium)
                    
                    ExposedDropdownMenuBox(
                        expanded = chatExpanded,
                        onExpandedChange = { chatExpanded = !chatExpanded }
                    ) {
                        OutlinedTextField(
                            value = settings.activeProvider.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Active Chat Provider") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chatExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = chatExpanded,
                            onDismissRequest = { chatExpanded = false }
                        ) {
                            ChatProvider.values().forEach { provider ->
                                DropdownMenuItem(
                                    text = { Text(provider.name) },
                                    onClick = {
                                        viewModel.updateProvider(provider)
                                        chatExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (settings.activeProvider == ChatProvider.HERMES) {
                        OutlinedTextField(
                            value = hermesChatUrlInput,
                            onValueChange = { hermesChatUrlInput = it },
                            label = { Text("Hermes Chat Endpoint URL") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = hermesApiKeyInput,
                            onValueChange = { hermesApiKeyInput = it },
                            label = { Text("Hermes API Key (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = hermesModelInput,
                            onValueChange = { hermesModelInput = it },
                            label = { Text("Model (e.g., hermes-agent)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                viewModel.updateHermesChatUrl(hermesChatUrlInput)
                                viewModel.updateHermesApiKey(hermesApiKeyInput)
                                viewModel.updateHermesModel(hermesModelInput)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Hermes Chat Settings")
                        }
                    }

                    if (settings.activeProvider == ChatProvider.OPENROUTER) {
                        OutlinedTextField(
                            value = openRouterApiKeyInput,
                            onValueChange = { openRouterApiKeyInput = it },
                            label = { Text("OpenRouter API Key") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = modelInput,
                            onValueChange = { modelInput = it },
                            label = { Text("Model (e.g., anthropic/claude-3-haiku)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Button(
                            onClick = { viewModel.updateOpenRouterCredentials(openRouterApiKeyInput, modelInput) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save OpenRouter Credentials")
                        }
                    }
                }
            }

            // --- NEWS CONFIGURATION ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("News Configuration", style = MaterialTheme.typography.titleMedium)
                    
                    ExposedDropdownMenuBox(
                        expanded = newsExpanded,
                        onExpandedChange = { newsExpanded = !newsExpanded }
                    ) {
                        OutlinedTextField(
                            value = settings.activeNewsProvider.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Active News Provider") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = newsExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = newsExpanded,
                            onDismissRequest = { newsExpanded = false }
                        ) {
                            NewsProvider.values().forEach { provider ->
                                DropdownMenuItem(
                                    text = { Text(provider.name) },
                                    onClick = {
                                        viewModel.updateNewsProvider(provider)
                                        newsExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (settings.activeNewsProvider == NewsProvider.HERMES) {
                        OutlinedTextField(
                            value = newsSourceInput,
                            onValueChange = { newsSourceInput = it },
                            label = { Text("News Source Endpoint URL") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Button(
                            onClick = { viewModel.updateNewsSourceUrl(newsSourceInput) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save News Source URL")
                        }
                    }
                }
            }
        }
    }
}
