# Simply Awake Project 

The **Simply Awake Project** is a revamped version of the original *Simply Awake* app, available on the Android Store. It is a simple, yet effective, meditation app designed to guide users through their mindfulness and awakening journey. The app’s content is curated by **Angello Dillulo**, whose YouTube channel [Simply Always Awake](https://www.youtube.com/@SimplyAlwaysAwake) offers valuable insights into spirituality and awakening.

### Why the Remake?

We decided to rebuild the app because maintaining the previous version had become cumbersome due to outdated libraries and the overall difficulty in updating. This new version leverages modern technologies, making it more scalable, maintainable, and aligned with current development standards.

## Key Features

- **Remote Content Fetching:** Retrieves meditation content from a remote server with a local caching strategy to ensure availability and performance.
- **Audio Playback Management:** Provides minimal yet essential controls for audio playback, both in foreground and background modes.
- **Persistent Playback:** Utilizes foreground and background services to ensure uninterrupted playback, even when the app is in the background or when the device enters sleep mode.

## Technical Stack

- **UI Development:** Built entirely with the latest Jetpack Compose libraries (as of September 18, 2024).
- **Data Management:** Utilizes Retrofit for data fetching from the content server.
- **Dependency Injection:** Implemented using Koin for smooth and scalable dependency management.
- **Architecture:** Follows a classic MVVM architecture, with the Repository pattern as the backbone for delivering data to the ViewModels.
- **Media Playback:** Custom MediaSessionService implementation is used to manage playback, complete with system notifications.
- **Reactive Programming:** Primarily uses RxKotlin for reactive and asynchronous operations.

## Design Philosophy

The design stays true to the original app, aiming for practicality and simplicity. Since the app’s primary focus is on meditation and awakening, the interface is intentionally straightforward and user-friendly, avoiding unnecessary complexity in both form and function.
<div style="display: flex; justify-content: space-between;">
  <img src="https://github.com/user-attachments/assets/7fd941d9-bad5-48c4-8076-afbc7320d89f" alt="Screenshot_20240918_143632_Simply Awake" width="30%" />
  <img src="https://github.com/user-attachments/assets/b6fcfd1d-10bc-4969-8a42-4381eee139b1" alt="Screenshot_20240918_143619_Simply Awake" width="30%" />
  <img src="https://github.com/user-attachments/assets/1fc6ab47-10ba-4398-b32f-f39a7f4555bd" alt="Screenshot_20240918_143613_Simply Awake" width="30%" />
</div>

