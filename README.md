# SmartNews App

SmartNews is an intelligent, dynamic Android news reader application. 

It fetches a personalized feed of news articles and allows you to chat directly with an AI assistant to discuss the news. It features a pluggable architecture that lets you hot-swap your AI backend seamlessly between local mocks, your own custom Hermes server, or third-party providers like OpenRouter.

## Features
- **Dynamic News Feed:** Fetches and displays news articles with images.
- **AI Chat Assistant:** Discuss news articles with an AI right inside the app.
- **Pluggable Backend:** Settings page allows switching between Mock, Hermes, and OpenRouter.
- **Local Persistence:** Chat history and user settings are persisted via Room and DataStore.
- **Modern UI:** Built fully in Jetpack Compose.

## Getting Started

1. **Clone the repository**
2. **Open in Android Studio**
3. **Run the App:** It should build right away. You can run it on an emulator or a physical device.
4. **Configure Settings:** Tap the settings gear icon on the main screen to set your Active Provider (e.g., OpenRouter) and enter your API Key.

## Architecture

This app uses standard Modern Android Development (MAD) practices:
- **UI:** Jetpack Compose, Material 3
- **Dependency Injection:** Hilt/Dagger
- **Local Data:** Room (SQLite), Preferences DataStore
- **Network:** Retrofit, OkHttp, Kotlinx Serialization
- **Image Loading:** Coil
