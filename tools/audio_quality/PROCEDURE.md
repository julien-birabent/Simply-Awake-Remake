# Mettre à jour le référentiel de qualité des pistes

Référentiel : `tools/audio_quality/out/audio_quality.json` (versionné) et la collection Firestore `audioQuality`
(un document par `id` de piste, mêmes champs). L'app lit Firestore ; le JSON est la source que l'on régénère et
que l'on envoie. Les métriques et les seuils sont décrits dans `README.md` ; cette page ne donne que la marche à suivre.

## Quand

- Le catalogue `api/tracks` a de nouvelles pistes (nouvelle saison).
- Un fichier `media/audio/<id>.mp3` a été remplacé (remaster) : le script le détecte en comparant la taille distante
  (`HEAD`, `Content-Length`) à `fileBytes` du JSON, et réanalyse la piste.
- Les seuils changent après une écoute (voir `README.md`, section Calibration).

## Pré-requis

- `./setup.sh --all` une fois (ffmpeg, `.venv`, `.venv-balance` avec demucs, `firebase-admin`). Les venv sont
  ignorés par git ; `cache/` (les MP3, 2,3 Go) aussi.
- `export SIMPLY_AWAKE_BASE_URL="<baseServerUrl de local.properties>"`.
- Une clé de compte de service Firebase par projet, hors du dépôt :
  `~/.config/simply-awake/sa-dev.json` (`simply-awake-dev`) et `~/.config/simply-awake/sa-prod.json`
  (`simply-awake`). Console Firebase, Paramètres du projet, Comptes de service, Générer une nouvelle clé privée.

## Étapes

Toutes les commandes depuis `tools/audio_quality/`.

1. Voir ce qui a changé :

   ```bash
   .venv/bin/python analyze.py --list | wc -l
   git diff --stat out/catalog.json
   ```

   `--list` réécrit `out/catalog.json` ; le diff git montre les pistes ajoutées ou renommées.

2. Étape 1 (ffmpeg, environ 10 s par piste, 4 en parallèle) : analyse les pistes nouvelles ou modifiées, saute les
   autres.

   ```bash
   .venv/bin/python analyze.py
   ```

3. Étape 2 (demucs, environ 3 min par piste sur CPU) : balance voix/musique et timbre de voix des pistes qui n'en
   ont pas encore.

   ```bash
   .venv-balance/bin/python analyze.py --balance
   ```

   Si la dernière ligne du journal mentionne `voiceActivityRatio` nul ou une balance `null` pour une piste parlée,
   relancer celle-ci avec plus d'extraits : `--balance --force --excerpts 5 --filter '<id ou nom>'`.

4. Relire `out/audio_quality.csv` (trié POOR en premier) pour les pistes nouvelles : `level`, `issues`, `crestDb`,
   `voiceMusicBalanceDb`. Une piste nouvelle POOR ou FAIR s'écoute une minute avant publication ; si l'oreille
   contredit le modèle, ajuster les seuils dans `thresholds.json` (clés partielles acceptées, même forme que
   `DEFAULT_THRESHOLDS` dans `analyze.py`), relancer `analyze.py --thresholds thresholds.json` (recalcul des
   niveaux sans réanalyse) et noter l'écoute dans le tableau de calibration du `README.md`.

5. Envoyer :

   ```bash
   .venv/bin/python upload_firestore.py --credentials ~/.config/simply-awake/sa-dev.json --dry-run
   .venv/bin/python upload_firestore.py --credentials ~/.config/simply-awake/sa-dev.json
   .venv/bin/python upload_firestore.py --credentials ~/.config/simply-awake/sa-prod.json
   ```

   Prod seulement après vérification dans l'app sur le flavor `dev`. L'envoi remplace chaque document entier
   (`set`), il n'efface pas les documents des pistes retirées du catalogue : les supprimer à la main dans la console
   si une piste disparaît.

6. Commit : `out/catalog.json`, `out/audio_quality.json`, `out/audio_quality.csv`, et `README.md` si la calibration
   a bougé. Message : `chore(audio-quality): refresh track quality for <saison ou pistes>`.

## Pièges

- `analyze.py` et `upload_firestore.py` tournent avec `.venv/bin/python` ; `--balance` exige `.venv-balance/bin/python`
  (demucs n'est que là, et il force le CPU : le backend MPS d'Apple refuse le modèle).
- Ne jamais lancer deux `analyze.py` en même temps sur le même `out/` : chacun recharge le JSON au départ et le
  réécrit après chaque piste, le dernier écrase l'autre.
- Changer les seuils dans `DEFAULT_THRESHOLDS` pendant qu'un `--balance` tourne ne change rien à ce run : relancer
  `analyze.py` sans option à la fin pour recalculer les niveaux.
- Le serveur des MP3 n'est pas à nous : une piste réencodée côté serveur se détecte par sa taille, pas par
  `updateDate` du catalogue, qui n'est pas fiable.
