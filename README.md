# Simply Awake Project 

The **Simply Awake Project** is a revamped version of the original *Simply Awake* app, available on the Android Store. It is a simple, yet effective, meditation app designed to guide users through their mindfulness and awakening journey. The app’s content is curated by **Angello Dillulo**, whose YouTube channel [Simply Always Awake](https://www.youtube.com/@SimplyAlwaysAwake) offers valuable insights into spirituality and awakening.

### Why the Remake?

We decided to rebuild the app because maintaining the previous version had become cumbersome due to outdated libraries and the overall difficulty in updating. This new version leverages modern Android technologies, making it more scalable, maintainable, and aligned with current development standards.

---

## Key Features

### Track browsing, filtering & sorting

- **Filter by state**:  
  - Show only **downloaded** tracks  
  - Show only **favorites**  
  - Show only **never played** tracks  
  - **Hide poor audio** tracks (audio quality graded offline per track, see `tools/audio_quality/`)  
- **Filter by duration** using predefined buckets (e.g. short / medium / long sessions).  
- **Filter by categories**:  
  - Tracks are tagged via a `tagString` (e.g. *sleep, anxiety, body scan*).  
  - You can combine multiple tags using **AND** semantics (e.g. *sleep* **and** *anxiety*).
- **Sorting options**:
  - Sort by **release order** (original playlist order)
  - Sort **alphabetically** by title
  - Sort by **duration**
  - Sort by **most played**
  - Sort by **audio quality**

An “active filters” header shows which filters are currently applied and lets you remove them individually or reset everything in one tap.

### Playback & content

- **Remote Content Fetching:**  
  Retrieves meditation content from a remote server with a local caching strategy to ensure availability and performance.
- **Audio Playback Management:**  
  Provides minimal yet essential controls for audio playback, in both foreground and background.
- **Persistent Playback:**  
  Uses a `MediaSessionService`/foreground service to ensure uninterrupted playback, even when the app is in the background or when the device enters sleep mode.
- **Recent History Tracking:**  
  Automatically keeps a history of recently played meditations so users can quickly revisit previous sessions.
- **Favorites:**  
  Mark tracks as favorites, and use the **Favorites filter** to focus only on the content you return to most often.

### Offline support & storage management

- **Offline Playback Support:**  
  Download meditation tracks locally and listen to them without an internet connection.
- **Single & bulk downloads:**  
  - Download individual tracks from the track list  
  - Or **download all tracks** in one go, with progress feedback
- **Per-track download status:**  
  Each track shows whether it is not downloaded, downloading, or fully available offline.
- **Download management:**  
  - Cancel all ongoing downloads  
  - Cancel a single track download  
  - Delete a single downloaded track or **clear all** downloaded audio files
- **Storage usage overview:**  
  The app keeps track of how many track files are stored and their total size, so it’s easy to see (and clean up) offline usage.

---

## Technical Stack

- **UI Development:**  
  - Built entirely with **Jetpack Compose** (Material 3), using modern state management and theming.
- **Data Management:**  
  - **Retrofit** for remote data fetching from the content server  
  - Repository layer for separating data sources from UI logic
  - Cloud syncing done via Firestore
- **Dependency Injection:**  
  - **Koin** for dependency management and modularization.
- **Architecture:**  
  - Classic **MVVM** with a dedicated **use case layer** (e.g. filtering, downloads, history) on top of repositories.  
- **Media Playback:**  
  - Custom **MediaSessionService** implementation to manage playback and integrate with system controls & notifications.
- **Downloads:**  
  - Download pipeline built on top of Android’s **DownloadManager**, with a `TrackFileManager` and `TrackDownloadStore` to coordinate file storage and UI state.
- **Reactive Programming:**  
  - Heavy use of **Kotlin Coroutines** and **Flow** for async work, reactive UI, and observing downloads/history/favorites.

---

## Release signée via GitHub Actions

Le workflow [`.github/workflows/release.yml`](.github/workflows/release.yml) se déclenche à chaque push d'un tag de
version (`11.3`, `11.3.1`…). Il build `:app:bundleProdRelease`, le signe avec la clé d'upload Play App Signing, vérifie
que l'AAB contient bien une signature (`META-INF/*.RSA` ou `*.SF`), puis l'attache en artefact de workflow et à la
GitHub Release du tag. Le keystore n'est jamais présent dans le dépôt ni sur une machine locale : il vit en base64 dans
un secret du dépôt et n'est décodé que dans `$RUNNER_TEMP` le temps du job.

### Secrets à créer dans le dépôt

Dans *Settings → Secrets and variables → Actions → Repository secrets* :

| Secret                    | Contenu                                                                              |
|---------------------------|--------------------------------------------------------------------------------------|
| `RELEASE_KEYSTORE_BASE64` | Le fichier `upload-key.jks` encodé en base64 (voir ci-dessous)                        |
| `RELEASE_STORE_PASSWORD`  | Mot de passe du keystore                                                              |
| `RELEASE_KEY_ALIAS`       | Alias de la clé d'upload dans le keystore                                             |
| `RELEASE_KEY_PASSWORD`    | Mot de passe de la clé (facultatif : s'il est absent, celui du keystore est utilisé) |
| `BASE_SERVER_URL`         | Valeur de `baseServerUrl` pour `local.properties` (déjà utilisé par la CI des PR)     |

### Encoder le keystore en base64

Sur une seule ligne, sans retour à la ligne, puis coller le résultat dans `RELEASE_KEYSTORE_BASE64` :

```bash
base64 -w0 upload-key.jks
```

Sur macOS (`base64` BSD) :

```bash
base64 -i upload-key.jks | tr -d '\n'
```

Sur Windows (PowerShell), le résultat est copié dans le presse-papiers :

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload-key.jks")) | Set-Clipboard
```

### Publier une version

1. Mettre à jour `versionCode` et `versionName` dans `app/build.gradle.kts`, merger sur `master`.
2. Poser et pousser le tag correspondant :

```bash
git tag 11.3 && git push origin 11.3
```

3. Récupérer l'AAB signé dans la GitHub Release du tag (ou dans les artefacts du run) et l'envoyer sur la Play Console.

### Signer en local (facultatif)

`app/build.gradle.kts` lit `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` et `RELEASE_KEY_PASSWORD`
depuis les propriétés Gradle (`~/.gradle/gradle.properties` ou `-P`) puis depuis les variables d'environnement. Sans
ces valeurs, le build release reste non signé, avec un avertissement.

---

## Design Philosophy

The design stays true to the original app, aiming for practicality and simplicity. Since the app’s primary focus is on meditation and awakening, the interface is intentionally straightforward and user-friendly:

- Minimal navigation
- Dark minimal theme
- Clear visibility of track length, tags, and publication date
- Filters and sorting designed to help you **quickly find the right track for the moment**, without overwhelming the user with options.

<p align="center">
  <img width="220" alt="Login page" src="https://github.com/user-attachments/assets/8e9e6f93-4d2c-485e-a5a9-9e5d2a66f824" />
  <img
    src="https://github.com/user-attachments/assets/2c684d10-b37b-4f2f-b6ee-989370edb44c"
    alt="Track list"
    width="220" />
  <img
    src="https://github.com/user-attachments/assets/8117ebb3-f3ee-44d3-a588-85d311915958"
    alt="Filter bottom sheet"
    width="220" />
  <img
    src="https://github.com/user-attachments/assets/fd1c11b9-2290-41fe-8cb7-a2f0b45f028e"
    alt="Downloads settings"
    width="220" />
  <img
    src="https://github.com/user-attachments/assets/3afe3bcf-e6e5-4508-ae5a-222ef2e6f460"
    alt="Track details"
    width="220" />
</p>
