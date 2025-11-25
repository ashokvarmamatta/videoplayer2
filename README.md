# ProPlayer - A Feature-Rich Video Player for Android

**ProPlayer** is a modern, feature-rich video player application for Android, built entirely with **Jetpack Compose** and following the latest Android development best practices. It offers a clean, intuitive user interface for browsing and playing local video files, along with a powerful set of playback and management features.



## ✨ Features

ProPlayer is packed with features designed for a seamless and customizable viewing experience.

### 🎬 Core Playback Features
* **High-Performance Playback:** Utilizes **ExoPlayer** for reliable and efficient playback of a wide range of video formats.
* **Gesture Controls:**
    * **Double Tap:** Quickly fast-forward or rewind by double-tapping on the right or left side of the screen.
    * **Long Press:** Temporarily play video at 2x speed by long-pressing the screen.
* **Playback Speed Control:** Choose from multiple playback speeds (0.25x to 2.0x) for a customized viewing pace.
* **Subtitle Support:** Easily load external subtitle files (e.g., SRT, VTT) for any video.
* **Picture-in-Picture (PiP):** Continue watching your video in a floating window while multitasking.
* **Screen Lock:** Prevent accidental touches and interruptions by locking the screen during playback.
* **Auto-Play Next:** Automatically plays the next video in the current folder or playlist.
* **Aspect Ratio Memory:** Remembers your preferred aspect ratio for all videos.

### 📂 Media Management & Organization
* **Browse by Folders:** Automatically scans and organizes your videos into their respective folders.
* **All Videos View:** Access a complete list of all video files on your device.
* **Custom Playlists:**
    * Create, rename, and delete playlists.
    * Easily add videos to any playlist from the main library or the player itself.
* **File Operations:**
    * **Rename:** Rename video files directly within the app.
    * **Delete:** Delete videos from your device storage.

### 🎨 Personalization & Settings
* **Multi-Language Support:** The app interface is available in multiple languages, with an easy-to-use in-app language switcher.
* **View Toggle:** Switch between a compact List View and a visual Grid View for browsing media.
* **Theme & UI:**
    * **Dynamic Thumbnails:** Uses **Coil 3** to efficiently load video frames as thumbnails.
    * **Orientation Control:** Set the default screen orientation to Automatic, Landscape, or Portrait.
* **Playback Preferences:**
    * **Remember Brightness:** Automatically restores the last-used brightness level for each video.
    * **Remember Background Play:** Remembers your preference for playing audio in the background.
* **Decoder Selection:** Choose between Hardware and Software decoders for optimal performance or compatibility.

### 🛠 App Utilities
* **Share App:** Easily share the app with friends.
* **Rate App:** A direct link to the app's page on the Google Play Store.

---

## 🛠️ Technical Stack & Architecture

This project is a showcase of modern Android development, built with a focus on scalability, maintainability, and performance.

* **Architecture:** Follows a clean **MVVM (Model-View-ViewModel)** architecture, ensuring a clear separation of concerns between the UI, business logic, and data layers.
* **UI Framework:** Built 100% with **Jetpack Compose**, Android's modern declarative UI toolkit.
* **Dependency Injection:** Uses **Koin** for managing dependencies throughout the application, making the codebase modular and easy to test.
* **Asynchronous Programming:** Leverages **Kotlin Coroutines** and **Flow** for managing background tasks, data streams, and ensuring a responsive, non-blocking UI.
* **Data Persistence:**
    * **Room Database:** For robust, local storage of playlists and user-created data.
    * **SharedPreferences:** For storing user settings and preferences, managed via a clean `SettingsRepository`.
* **Media Playback:** **ExoPlayer (Media3)**, the standard for powerful and customizable media playback on Android.
* **Navigation:** Implemented using a custom state-driven approach within Jetpack Compose for handling navigation between different screens and states.
* **Image & Thumbnail Loading:** **Coil 3**, a lightweight and efficient image loading library for Compose, with a VideoFrameDecoder to display video thumbnails.
* **Data Source:** Fetches media efficiently from the Android `MediaStore` API.

---

## 🏗️ Code Highlights

* **Clean Repository Pattern:** The `VideoRepository` and `PlaylistRepository` abstract the data sources (MediaStore, Room DB), providing a clean API for the ViewModels.
* **State Management:** UI state is managed reactively using `StateFlow` in the ViewModels, which is collected and observed in the Composable UI. This ensures the UI always reflects the current state of the application data.
* **Modular DI with Koin:** The `AppModule.kt` clearly defines how dependencies like ViewModels, Repositories, and the Room database are constructed and provided throughout the app.
* **Robust Player Lifecycle:** The `VideoPlayerViewModel` correctly handles the ExoPlayer lifecycle, including initialization, release, and state management for events like PIP mode, app pausing/resuming, and configuration changes.
* **Extensible Settings:** A dedicated `SettingsRepository` and `SettingsViewModel` provide a solid foundation for adding new user-configurable options in a clean and maintainable way.

---

## 🚀 How to Build

1.  **Clone this repository:**
    ```bash
    git clone [https://github.com/yourusername/ProPlayer.git](https://github.com/yourusername/ProPlayer.git)
    ```
2.  Open the project in the latest stable version of **Android Studio**.
3.  Let Gradle sync and download the required dependencies.
4.  Run the app on an Android device or emulator (**API 21+**).
