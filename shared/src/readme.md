# Shared module source sets

The `shared` module contains Compose UI, state, themes, and ViewModels used by the Android app.

- `commonMain` contains shared Compose code and resources.
- `androidMain` contains Android-specific UI integration and platform implementations.
- `composeResources` contains shared Compose images, fonts, and strings.

The application currently targets Android only. The plugin API's JVM and web targets are maintained separately in `library`.
