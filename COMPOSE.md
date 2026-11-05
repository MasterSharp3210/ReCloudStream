# Migration guide to Compose

### 1. MVI instead of MVVM

The current Android UI loosely uses the MVVM architecture. The UI invokes ViewModel functions and observes LiveData, which creates boilerplate and friction when working with Compose.

To make Compose easier to maintain, new presentation code should use MVI: the ViewModel exposes immutable UI state and accepts UI events. The UI should be reproducible from state, and interactions should be representable as events.

This decouples UI and logic within the Android application. ReCloudStream currently targets Android only.

### 2. Shared code and dependencies

The shared Compose module uses Kotlin Multiplatform source sets but currently builds only for Android. Prefer libraries supported by the source sets that consume them; the plugin library retains its own platform targets for extension compatibility.

### 3. UI changes

While migrating to Compose, preserve existing features and refresh the interface conservatively rather than redesigning it wholesale.
