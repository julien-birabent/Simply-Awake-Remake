# Simply Awake Remake

Android, Kotlin, Compose, Koin, Room, Retrofit, Firestore. Les pistes audio viennent d'un serveur tiers (`api/tracks`,
`media/audio/<id>.mp3`, URL dans `local.properties` sous `baseServerUrl`) ; Firestore (`simply-awake-dev`,
`simply-awake`) est à nous.

## Référentiel de qualité des pistes

`tools/audio_quality/` : analyse hors ligne des MP3 (ffmpeg, demucs) qui produit `out/audio_quality.json` et la
collection Firestore `audioQuality`, lue par l'app pour filtrer et trier par qualité sonore. Nouvelle saison, piste
remplacée ou seuil à revoir : suivre `tools/audio_quality/PROCEDURE.md`. Métriques et calibration par écoute dans
`tools/audio_quality/README.md`.

## Release

Publication Play Store : tag `X.Y` ou `X.Y.Z` sur `master`, le workflow `release.yml` produit l'AAB signé.
Procédure, secrets et rotation de la clé d'upload : `docs/RELEASE.md`.
