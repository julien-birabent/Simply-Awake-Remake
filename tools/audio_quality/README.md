# Qualité audio des méditations

Mesure hors ligne la qualité sonore de chaque piste du catalogue et en déduit un niveau `GOOD` / `FAIR` / `POOR`,
à stocker dans Firestore (collection `audioQuality`, un document par `id` de piste) pour filtrer et trier dans l'app.
Rien ne tourne sur l'appareil : le catalogue est fini, le serveur n'est pas à nous, on analyse les MP3 téléchargés.

## Installation

```bash
./setup.sh              # ffmpeg (brew) + venv numpy : étape 1
./setup.sh --balance    # + venv python 3.12 avec demucs/torch : étape 2 (balance voix/musique)
./setup.sh --firestore  # + firebase-admin dans .venv : upload
```

## Utilisation

```bash
export SIMPLY_AWAKE_BASE_URL="<baseServerUrl de local.properties>"

.venv/bin/python analyze.py --list                          # catalogue
.venv/bin/python analyze.py                                 # étape 1 sur tout le catalogue
.venv/bin/python analyze.py --filter 'S03|anxiety' --force  # sous-ensemble, recalcul
.venv-balance/bin/python analyze.py --balance               # étape 2 (lente)
.venv-balance/bin/python analyze.py --balance --excerpts 3 --excerpt-seconds 90
.venv/bin/python upload_firestore.py --credentials sa.json --dry-run
.venv/bin/python upload_firestore.py --credentials sa.json
```

Sorties dans `out/` : `catalog.json` (réponse `api/tracks`), `audio_quality.json` (métriques, seuils, niveaux),
`audio_quality.csv` (même chose, triée POOR en premier, pour la calibration). Les MP3 restent dans `cache/`.
Le script est incrémental : une piste déjà dans `audio_quality.json` est sautée sans `--force`, `--balance` ne
calcule que les pistes sans `voiceActivityRatio`.

## Métriques

| Symptôme | Métrique | Calcul | Seuils par défaut (GOOD / FAIR, sinon POOR) |
|---|---|---|---|
| Musique trop forte sous la voix | `voiceMusicBalanceDb` | demucs sépare `vocals` / `no_vocals` sur 3 extraits de 60 s (25, 50, 75 % de la piste), LUFS intégré de chaque stem, médiane des différences | GOOD à partir de 0 dB, FAIR en dessous, jamais POOR |
| Compression en chaîne, artefacts, fond écrasé | `crestDb` | `truePeakDbfs` moins `rmsDb` (RMS médian par 400 ms) | au moins 16 dB / 11 dB |
| Voix « boxy » | `voiceTiltDb` | sur le stem voix, niveau moyen 200-600 Hz moins niveau moyen 1,5-4 kHz | aucun : informatif, voir Calibration |
| Voix qui grésille, clics | `clicksPerMinute` | impulsions : dérivée seconde du signal à plus de 30 fois sa médiane locale sur 20 ms, fusionnées à 2 ms | aucun : informatif, voir Calibration |
| Saturation | `clippedRunsPerMinute` | suites d'au moins 2 échantillons à 0,99 ou plus | au plus 1 / 10 |
| Backing track réencodée, sourde | `bandwidthHz` | spectre moyen, médiane par bande de 250 Hz, plus haute bande à 20 dB au-dessus du plancher | aucun : informatif, voir Calibration |
| Souffle, bruit de fond | `noiseFloorDb` | 5e percentile du RMS par 400 ms ; `null` si la piste n'a pas de passage calme (écart au RMS médian sous 10 dB) | aucun : informatif, voir Calibration |
| Niveau incohérent avec le reste | `loudnessDeltaLu` | écart absolu de `integratedLufs` (ffmpeg `ebur128`) à la médiane du catalogue | aucun : à corriger par un gain dans l'app, voir Calibration |

Aussi mesurées, sans seuil : `rolloff99Hz`, `loudnessRangeLu`, `truePeakDbfs`, `rmsDb`, `silenceRatio`,
`voiceActivityRatio` (part des secondes où le stem voix dépasse -45 dBFS ; sous 0,1 la balance est `null`,
piste sans voix), `bitrateKbps`, `sampleRateHz`, `channels`.

`level` est la pire note parmi les métriques mesurées ; `issues` liste celles qui ne sont pas `GOOD`.

## Calibration

Seuils de `crestDb` et `voiceMusicBalanceDb` posés le 03/11 sur 8 écoutes de Julien ; 7 sur 8 concordent :

| Piste | Oreille | `crestDb` | balance | Modèle |
|---|---|---|---|---|
| 023 Natural Inquiry | POOR, voix très dégradée, fond trop fort, artefacts de compression | 10,1 | 1,0 | POOR |
| 018 Middle Ground | POOR, fond trop fort, voix dégradée | 10,7 | 6,1 | POOR |
| 010 Exploring Consciousness | POOR, voix boxy et granuleuse, fond trop fort et compressé | 12,0 | 3,8 | FAIR (écart) |
| 013 Standing in Awe | FAIR, musique un peu forte, creux dans le fond (compression) | 12,1 | -3,9 | FAIR |
| 006 Attention Knows | GOOD, fond un peu fort | 16,8 | 2,2 | GOOD |
| 062 Silence Within | GOOD, fond un peu fort | 18,8 | 0,8 | GOOD |
| 002 Set Aside Expectations | excellent | 20,3 | 6,8 | GOOD |
| 033 Inclusion | excellent | 46,7 | 23,1 | GOOD |

Lecture : l'écrasement dynamique (`crestDb` sous 11) domine le jugement ; une balance entre 0 et 6 dB s'entend
(« fond un peu fort ») sans faire basculer le verdict. L'écart sur 010 vient du timbre de la voix, que `crestDb`
et la balance ne voient pas : `voiceTiltDb` est là pour ça, non seuillé tant qu'il n'est pas mesuré sur ces 8 pistes.

`voiceTiltDb` mesuré sur ces 8 pistes ne sépare rien : 010 (POOR, voix boxy) et 013 (FAIR) donnent tous deux
20,7 dB, 002 et 033 (excellentes) 18,7 et 22,4, 062 (GOOD) 44,1. Il reste informatif ; 010 reste FAIR, c'est l'écart
assumé du modèle. La balance ne produit jamais POOR : 062, remesurée à -5,0 dB sur 3 extraits, est GOOD à l'oreille
(« fond un peu fort »). La balance varie de 1 dB entre 1 et 3 extraits sur les pistes parlées en continu, de 6 dB
sur 062 dont la voix est clairsemée.

Deux écoutes de plus ont retiré deux métriques de `level` :

- 041 Conscious Substance (-29,4 LUFS, 14 LU sous la médiane, 136 clics/min mesurés) : « très bonne qualité, voix
  claire, fond pas trop fort ». Le niveau faible n'est pas un défaut de qualité une fois le volume monté :
  `loudnessDeltaLu` sert au gain par piste dans l'app, pas au filtre. Les 136 clics/min sur une piste jugée
  très bonne réfutent `clicksPerMinute` tel qu'il est calculé.
- 063 What is This? (coupée à 9,25 kHz, la plus basse du catalogue) : « claire, bonne qualité, fond un peu fort ».
  Aucune piste du catalogue n'est assez coupée pour s'entendre ; `bandwidthHz` reste informatif.

Pour recalibrer :

1. Lancer l'étape 1 sur tout le catalogue, puis `--balance` sur une dizaine de pistes jugées à l'oreille
   (5 agréables, 5 pénibles) avec `--filter`.
2. Ouvrir `out/audio_quality.csv`, vérifier quelle colonne sépare les deux groupes.
3. Ajuster dans un `thresholds.json` (même forme que `DEFAULT_THRESHOLDS` dans `analyze.py`, clés partielles
   acceptées) et relancer avec `--thresholds thresholds.json` : les niveaux sont recalculés sans réanalyser.

`noiseFloorDb` n'entre pas dans `level` : sur le catalogue réel il n'est mesurable que sur 10 pistes et vaut -21 à
-37 dBFS, le niveau du fond musical pendant les pauses de la voix, pas un souffle. Le souffle se mesure plutôt sur
le stem `no_vocals` de demucs, non implémenté.

`clicksPerMinute` n'entre pas dans `level` : sur une voix de synthèse macOS (`say`) sans défaut audible il donne
60 à 70 clics par minute, donc il ne distingue pas encore un grésillement d'une consonne. À comparer entre pistes
jugées bonnes et mauvaises ; s'il sépare les deux groupes, l'activer dans `thresholds.json`
(`{"clicksPerMinute": {"direction": "lower", "good": 5, "fair": 20}}`).

Validé sur des fichiers synthétiques : passe-bas 9 kHz à 64 kbps donne `bandwidthHz` 11 500 contre 19 000 pour
l'original à 192 kbps ; une saturation à +40 dB donne `clippingRatio` 0,49 ; des impulsions aléatoires donnent
`clicksPerMinute` 18 ; une voix `say` mixée 12 dB au-dessus d'un fond donne `voiceMusicBalanceDb` 12,0 et la même
voix 3 dB sous le fond donne -3,4. Les seuils sur de vraies méditations restent à vérifier.

Coût de `--balance` : demucs tourne sur CPU (le backend MPS d'Apple refuse le modèle), environ une fois la durée
de l'extrait, donc 2 minutes par piste avec l'extrait de 120 s par défaut.

## Firestore

`upload_firestore.py` écrit `audioQuality/<trackId>` avec `level`, `issues`, les métriques seuillées,
`bitrateKbps` et `generatedAt`. Il faut un compte de service du projet (`simply-awake-dev` ou `simply-awake`) :
console Firebase, paramètres du projet, comptes de service, générer une clé. Le fichier reste hors du dépôt.

Côté app : lire la collection au même moment que `api/tracks`, porter `audioQuality: AudioQualityLevel?` sur
`Track` (`null` tant que la piste n'est pas analysée), ajouter `AudioQualityTrackListFilter` dans
`ApplyTrackFiltersUseCase` et un `TrackSortOption.AUDIO_QUALITY_DESC`.
