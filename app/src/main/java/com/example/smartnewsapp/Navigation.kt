package com.example.smartnewsapp

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.smartnewsapp.ui.main.MainScreen
import com.example.smartnewsapp.ui.chat.ChatScreen
import com.example.smartnewsapp.ui.profile.ProfileScreen
import com.example.smartnewsapp.ui.settings.SettingsScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(onItemClick = { navKey -> backStack.add(navKey) })
        }
        entry<Chat> { navKey ->
          ChatScreen(
              articleId = navKey.articleId,
              onNavigateBack = { backStack.removeLastOrNull() }
          )
        }
        entry<Profile> {
          ProfileScreen(
              onNavigateBack = { backStack.removeLastOrNull() }
          )
        }
        entry<Settings> {
          SettingsScreen(
              onNavigateBack = { backStack.removeLastOrNull() }
          )
        }
      },
  )
}
