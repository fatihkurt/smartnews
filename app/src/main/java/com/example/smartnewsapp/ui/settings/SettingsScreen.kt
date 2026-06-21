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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    
    var expanded by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf(settings.openRouterApiKey) }
    var modelInput by remember { mutableStateOf(settings.openRouterModel) }

    // Sync input state when flow emits initially
    LaunchedEffect(settings) {
        if (apiKeyInput.isEmpty() && settings.openRouterApiKey.isNotEmpty()) {
            apiKeyInput = settings.openRouterApiKey
        }
        if (modelInput == "openai/gpt-4o" && settings.openRouterModel != "openai/gpt-4o") {
            modelInput = settings.openRouterModel
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
            Text("Chat Provider", style = MaterialTheme.typography.titleMedium)
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = settings.activeProvider.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Active Provider") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    ChatProvider.values().forEach { provider ->
                        DropdownMenuItem(
                            text = { Text(provider.name) },
                            onClick = {
                                viewModel.updateProvider(provider)
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Divider()
            
            Text("OpenRouter Configuration", style = MaterialTheme.typography.titleMedium)
            
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = modelInput,
                onValueChange = { modelInput = it },
                label = { Text("Model (e.g., anthropic/claude-3-haiku)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(
                onClick = { viewModel.updateOpenRouterCredentials(apiKeyInput, modelInput) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save OpenRouter Credentials")
            }
        }
    }
}
