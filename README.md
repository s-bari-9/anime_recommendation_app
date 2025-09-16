# AnimeMate - Anime/Manga Recommendation App with Dating Interface

AnimeMate is a fully-featured Android application that helps users discover anime, manga, and light novels with a fun, dating app-style swipe interface. The app integrates with MyAnimeList to provide personalized recommendations, manage your watchlist, and track your viewing history.

## Features

- **Dating App Swipe Interface**:
  - Swipe RIGHT to add to your watchlist
  - Swipe LEFT to mark as not interested
  - Swipe UP to mark as watched/completed
  - Swipe DOWN to view full details

- **MyAnimeList Integration**:
  - Login with your existing MyAnimeList account
  - Automatically imports your watch history and ratings
  - Updates your MyAnimeList profile directly
  - View your anime statistics

- **Smart Recommendation Engine**:
  - Personalized recommendations based on your preferences
  - Learns from your swipe interactions
  - Combines content-based, collaborative filtering, and theme-based approaches
  - Diversity in recommendations to help you discover new content

- **Comprehensive Content Management**:
  - Organize your watchlist by anime, manga, and light novels
  - Track your watch history and completed series
  - Filter and sort your content for easy browsing
  - Rate content directly in the app

## Installation

### Requirements
- Android 8.0 (Oreo) or higher
- MyAnimeList account (free to create)
- Internet connection

### Install Steps
1. Download the latest APK from the releases section
2. Enable "Install from Unknown Sources" in your device settings if needed
3. Open the APK file to install
4. Launch the app and log in with your MyAnimeList credentials

## Usage Guide

### Initial Setup
1. Log in with your MyAnimeList account
2. Complete the profile setup with your basic information
3. Select your preferred content types (anime, manga, light novels)
4. Choose your favorite genres
5. Confirm your favorite titles from your MyAnimeList history

### Discovering Content
- Browse through personalized recommendations on the home tab
- Swipe cards to interact with recommendations
- Use the swipe tutorial for guidance (appears on first use)
- View detailed information by swiping down on a card

### Managing Your Lists
- Access your watchlist from the Watchlist tab
- View your watch history in the History tab
- Use tabs to filter by content type and status
- Update status or rating from detailed view screens

### User Profile
- View your MyAnimeList statistics
- Update your content preferences
- Manage your account settings
- Log out when needed

## Development

### Project Structure
- MVVM architecture using ViewModels and LiveData
- Repository pattern for data access
- Room database for local storage
- Retrofit for API communication
- Glide for image loading
- CardStackView for swipe interface

### Building from Source
1. Clone the repository
2. Open the project in Android Studio
3. Set up your MyAnimeList API credentials in `AnimeRecApp.kt`
4. Build and run the project

### Running Tests
- Unit tests: `./gradlew test`
- Instrumented tests: `./gradlew connectedAndroidTest`
- Memory leak check: `./memory_leak_check.sh`

## Privacy & Permissions

The app requires the following permissions:
- **Internet**: To connect to the MyAnimeList API
- **Network State**: To detect network connectivity

The app does not collect, store, or transmit any personal data beyond what is required for the MyAnimeList API integration.

## Support & Feedback

For support, feature requests, or bug reports, please open an issue in the GitHub repository or contact us at support@animemate.app.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- MyAnimeList for providing the API
- [CardStackView](https://github.com/yuyakaido/CardStackView) library for the swipe interface
- All the open-source libraries that made this project possible