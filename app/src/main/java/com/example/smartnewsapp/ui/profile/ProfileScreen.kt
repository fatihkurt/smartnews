package com.example.smartnewsapp.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

val ALL_CATEGORIES = listOf(
    "Technology", "Business", "Finance", "Politics", "Health", "Science", 
    "Entertainment", "Sports", "World News", "Local News", "Startups", 
    "Artificial Intelligence", "Cryptocurrency", "Environment", "Climate Change", 
    "Automotive", "Real Estate", "Fashion", "Art & Culture", "Travel", 
    "Food & Dining", "Education", "Law & Crime", "Gaming", "E-sports", 
    "Books & Literature", "Music", "Movies & TV", "Fitness", "Space Exploration", "History"
)

val LANGUAGES = listOf("English", "Spanish", "French", "German", "Turkish", "Italian", "Portuguese")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val interests by viewModel.interestProfile.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var expandedLanguage by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Interests") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            settings?.let { currentSettings ->
                item {
                    Text("Language Preference", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = expandedLanguage,
                        onExpandedChange = { expandedLanguage = !expandedLanguage }
                    ) {
                        OutlinedTextField(
                            value = currentSettings.selectedLanguage,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLanguage) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedLanguage,
                            onDismissRequest = { expandedLanguage = false }
                        ) {
                            LANGUAGES.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang) },
                                    onClick = {
                                        viewModel.updateLanguage(lang)
                                        expandedLanguage = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Explicit Interests (Categories)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ALL_CATEGORIES.forEach { category ->
                            val isSelected = currentSettings.selectedCategories.contains(category)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newSet = if (isSelected) {
                                        currentSettings.selectedCategories - category
                                    } else {
                                        currentSettings.selectedCategories + category
                                    }
                                    viewModel.updateCategories(newSet)
                                },
                                label = { Text(category) }
                            )
                        }
                    }
                }
            }

            if (interests.isNotEmpty()) {
                item {
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Implicit Interest Graph (Auto-learned)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(interests) { interest ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = interest.keyword, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Score: ${String.format("%.2f", interest.score)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (interest.score > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
