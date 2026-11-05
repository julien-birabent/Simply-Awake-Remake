# Procédure de release (Play Store)

Ce document décrit comment produire et publier une nouvelle version de l'app. La signature se fait dans GitHub
Actions ([`.github/workflows/release.yml`](../.github/workflows/release.yml)) avec la clé d'upload Play App Signing,
stockée en base64 dans un secret du dépôt. Aucun keystore ni mot de passe n'est nécessaire sur un poste local.

## Vue d'ensemble

- **Play App Signing** détient la vraie clé de signature de l'app. Nous ne manipulons que la **clé d'upload**
  (`upload-key.jks`), qui sert uniquement à authentifier les AAB envoyés à la Play Console. Si elle est perdue, elle se
  réinitialise (voir [Perte ou rotation de la clé d'upload](#perte-ou-rotation-de-la-clé-dupload)).
- Le workflow se déclenche au **push d'un tag** nommé `X.Y` ou `X.Y.Z` (ex. `11.4`, `11.3.1`). Tout autre nom
  (`v11.4`, `hotfix-11.2`…) est ignoré.
- Il build `:app:bundleProdRelease`, vérifie que l'AAB est signé, puis dépose `simply-awake-<tag>.aab` dans les
  artefacts du run et dans la **GitHub Release** du tag.

## Prérequis (une seule fois)

Cinq secrets dans *Settings → Secrets and variables → Actions → Repository secrets* :

| Secret                    | Contenu                                                                              |
|---------------------------|--------------------------------------------------------------------------------------|
| `RELEASE_KEYSTORE_BASE64` | `upload-key.jks` encodé en base64, sur une seule ligne                                |
| `RELEASE_STORE_PASSWORD`  | Mot de passe du keystore                                                              |
| `RELEASE_KEY_ALIAS`       | Alias de la clé d'upload dans le keystore                                             |
| `RELEASE_KEY_PASSWORD`    | Mot de passe de la clé (facultatif : s'il est absent, celui du keystore est utilisé) |
| `BASE_SERVER_URL`         | Valeur de `baseServerUrl` pour `local.properties` (déjà utilisé par la CI des PR)     |

Le script [`tools/release/set-release-secrets.ps1`](../tools/release/set-release-secrets.ps1) pose les quatre secrets
`RELEASE_*` : il demande le chemin du keystore et les mots de passe, les vérifie avec `keytool` (il redemande tant que
le mot de passe est refusé), détecte l'alias, puis envoie le tout avec `gh secret set`. Il nécessite
[GitHub CLI](https://cli.github.com/) authentifié (`gh auth login`) et le JDK d'Android Studio.

```powershell
powershell -ExecutionPolicy Bypass -File tools/release/set-release-secrets.ps1
```

Sans le script, encoder le keystore à la main (une seule ligne, sans retour à la ligne) :

```bash
base64 -w0 upload-key.jks             # Linux / Git Bash
base64 -i upload-key.jks | tr -d '\n' # macOS
```

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload-key.jks")) | Set-Clipboard
```

Vérifier : `gh secret list --repo julien-birabent/Simply-Awake-Remake` doit lister les cinq noms.

## Publier une version

1. **Monter la version** dans [`app/build.gradle.kts`](../app/build.gradle.kts) :
   `versionCode` +1 (obligatoire, Play refuse un code déjà utilisé) et `versionName` (ex. `11.4`).
   Passer par une PR mergée sur `master`.

2. **Poser le tag sur `master`**, avec le même nom que `versionName`, et le pousser :

   ```bash
   git checkout master && git pull
   git tag 11.4 && git push origin 11.4
   ```

   Un tag déjà existant ne peut pas être réutilisé : choisir une nouvelle version (ex. `11.4.1`) plutôt que de
   déplacer le tag.

3. **Attendre le run « Release AAB »** (5 à 10 minutes), dans l'onglet *Actions* du dépôt ou :

   ```bash
   gh run watch --repo julien-birabent/Simply-Awake-Remake
   ```

4. **Récupérer `simply-awake-<tag>.aab`** sur la page *Releases* du dépôt (le workflow crée la release du tag avec des
   notes générées à partir des PR) et l'envoyer dans la Play Console :
   *Test et publication → Production → Créer une version*.

Les secrets n'ont pas à être touchés d'une release à l'autre.

## Dépannage

| Étape en échec                    | Cause probable                                                                                                   |
|-----------------------------------|------------------------------------------------------------------------------------------------------------------|
| `Check required secrets`          | Un secret manque ; son nom est dans le log.                                                                      |
| `Decode upload keystore`          | `RELEASE_KEYSTORE_BASE64` n'est pas du base64 valide (retour à la ligne, copie tronquée).                        |
| `Build signed prodRelease bundle` | Mot de passe ou alias faux (`Keystore was tampered with`, `Cannot recover key`), ou erreur de build classique.   |
| `Verify the bundle is signed`     | Le build a produit un AAB non signé : variables `RELEASE_*` incomplètes côté Gradle.                             |
| Le workflow ne démarre pas        | Nom de tag hors `X.Y` / `X.Y.Z`, ou tag posé sur un commit qui ne contient pas `release.yml`.                    |
| Play refuse l'AAB                 | `versionCode` déjà utilisé, ou AAB signé avec une autre clé d'upload que celle enregistrée dans la Play Console. |

Pour reproduire la signature en local (facultatif), `app/build.gradle.kts` lit `RELEASE_STORE_FILE`,
`RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` et `RELEASE_KEY_PASSWORD` depuis les propriétés Gradle
(`~/.gradle/gradle.properties` ou `-P`) puis les variables d'environnement. Sans ces valeurs, le build release est non
signé, avec un avertissement.

## Perte ou rotation de la clé d'upload

Mot de passe oublié ou keystore perdu : la clé d'upload se remplace sans impact pour les utilisateurs.

1. Générer un nouveau keystore (choisir un mot de passe et le ranger dans un gestionnaire de mots de passe) :

   ```bash
   keytool -genkeypair -v -keystore upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Exporter le certificat public :

   ```bash
   keytool -export -rfc -keystore upload-key.jks -alias upload -file upload_certificate.pem
   ```

3. Dans la Play Console : *Test et publication → Intégrité de l'application → Signature d'application →
   Demander la réinitialisation de la clé d'importation*, et téléverser `upload_certificate.pem`.
   Google valide sous un à deux jours ouvrés ; l'ancienne clé reste acceptée entre-temps.

4. Relancer `tools/release/set-release-secrets.ps1` avec le nouveau keystore, puis supprimer le fichier `.jks` local
   une fois une release passée.
