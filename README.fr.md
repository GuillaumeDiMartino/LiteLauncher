# Lite Launcher

[English](README.md) | Français

Un écran d'accueil minimaliste pour Chromecast avec Google TV : salutation, horloge, favoris,
apps récentes, toutes les apps et réglages rapides. Pas de recommandations, pas de pub, pas de
service en arrière-plan, pas d'accès réseau, aucune bibliothèque.

Règle pour chaque modification : elle ne doit jamais dégrader la fluidité ni la mémoire.

Tout ce qui suit se fait avec la Chromecast branchée en USB sur le PC. Testé sur Chromecast avec
Google TV (4K, Android 14).

## Brancher la Chromecast en USB

L'USB est la connexion la plus fiable : elle ne dépend pas du Wi-Fi et continue de marcher quoi
qu'il y ait à l'écran, même si l'écran d'accueil ne s'affiche plus.

1. Sur la Chromecast : Paramètres > Système > À propos, appuie 7 fois sur « Build de l'OS Android TV ».
2. Paramètres > Système > Options pour les développeurs : active « Débogage USB ».
3. Laisse le HDMI de la Chromecast branché sur la TV (pour voir l'écran), et branche sa prise USB-C
   sur le PC avec un câble USB-C **qui transmet les données** (un câble de charge seule ne marche
   pas). Le port du PC alimente la Chromecast ; si elle redémarre en boucle, utilise un hub USB-C
   avec alimentation.
4. Sur la TV, accepte « Autoriser le débogage USB ? » et coche « Toujours autoriser sur cet ordinateur ».
5. Vérifie la connexion :

```bash
adb devices
```

La Chromecast doit apparaître avec `device` (et non `unauthorized`).

## Reprendre avec Claude sur un autre PC

Prérequis : l'app Claude (onglet Code) et Git installés, et la Chromecast branchée en USB comme
ci-dessus. Colle ce prompt dans Claude :

```text
Clone https://github.com/GuillaumeDiMartino/LiteLauncher.git puis lis le README.fr.md et le code.

Contexte : c'est un launcher Android TV ultra léger que j'ai conçu pour remplacer l'écran d'accueil Google TV de ma Chromecast avec Google TV, qui rame. Java natif sans aucune bibliothèque : salutation, horloge, sections Favoris / Récents / Applications / Réglages rapides, menu appui long (favoris, déplacer, masquer, désinstaller), couleur d'accent et taille des tuiles réglables. Le dossier preview/ contient une maquette HTML fidèle (960x540 dp) qui sert à travailler le design avant de le reporter dans le code Android.

Règle absolue : chaque modification ne doit jamais dégrader les performances ni la fluidité (pas de flou, pas de réseau, pas de service en arrière-plan, pas de bibliothèque lourde). Indique le coût de chaque idée avant de la coder, et mesure sur la Chromecast avec dumpsys gfxinfo avant de garder un changement.

Ma Chromecast est branchée en USB sur ce PC, mode développeur et débogage USB activés. Utilise adb -d.

Étapes :
1. Installe ce qu'il manque : ADB (platform-tools), JDK 17+, SDK Android (platform 34, build-tools 34), Gradle. Demande-moi avant chaque téléchargement et avant d'accepter une licence.
2. Vérifie que la Chromecast apparaît dans adb devices (j'accepterai la fenêtre sur la TV).
3. Compile l'APK en release et installe-le.
4. Avant de désactiver les écrans Google (launcherx, tvlauncher, setupwraith), demande-moi confirmation et rappelle-moi les commandes pour revenir en arrière.
5. Passe les animations système à 0.5x avec les commandes de la section Installation.
6. Mesure la mémoire avant et après (dumpsys meminfo), puis guide-moi dans la section "Optimisations Chromecast" : propose les paquets à désactiver parmi ceux réellement installés, explique chacun, et demande-moi confirmation avant chaque désactivation.
7. Pousse les modifications sur le dépôt GitHub.
```

## Télécommande

- Flèches pour se déplacer, OK pour ouvrir une app.
- Appui long sur OK sur une app : l'ajouter ou la retirer des favoris, la déplacer dans sa section,
  la masquer, ouvrir ses infos ou la désinstaller.
- Rangée Réglages rapides : Wi-Fi, Bluetooth, Affichage, Veille, Play Store, Paramètres, Style
  (couleur d'accent et taille des tuiles), et « Masquées » pour réafficher les apps masquées.
- « Récents » liste les dernières apps ouvertes depuis Lite Launcher (aucune autorisation
  nécessaire). Les apps ouvertes autrement, par exemple avec les boutons YouTube ou Netflix de la
  télécommande, ne sont pas comptées.
- L'écran ne défile que si la tuile sélectionnée en sortirait.
- Le bouton Home ramène tout en haut.

## Installation

Chromecast branchée en USB (voir plus haut). `-d` cible l'appareil branché en USB.

1. Installer le launcher et le définir comme écran d'accueil :

```bash
adb -d install -r app-release.apk
```

```bash
adb -d shell cmd package set-home-activity fr.lite.launcher/.MainActivity
```

2. Désactiver les écrans d'accueil Google. Les trois sont nécessaires : si seul `launcherx` est
désactivé, le bouton Home bascule sur `tvlauncher`, puis sur un écran noir « RecoveryActivity » de
l'assistant de configuration (`setupwraith`), prioritaire sur tout autre launcher. Android 14
n'autorise pas à désactiver seulement cet écran, donc on désactive tout l'assistant. Une
réinitialisation d'usine (Paramètres > Système > À propos, ou en maintenant le bouton de la
Chromecast) réactive tout automatiquement.

```bash
adb -d shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
```

```bash
adb -d shell pm disable-user --user 0 com.google.android.tvlauncher
```

```bash
adb -d shell pm disable-user --user 0 com.google.android.tungsten.setupwraith
```

3. Appuie sur Home : Lite Launcher doit s'afficher. Si l'écran reste noir, garde l'USB branché et
lance les commandes de la section « Revenir en arrière ».

4. Rendre toute la Chromecast plus réactive en divisant les animations système par deux :

```bash
adb -d shell settings put global window_animation_scale 0.5
```

```bash
adb -d shell settings put global transition_animation_scale 0.5
```

```bash
adb -d shell settings put global animator_duration_scale 0.5
```

Tu peux ensuite débrancher le câble USB et alimenter la Chromecast comme d'habitude : tous les
réglages survivent aux redémarrages.

## Optimisations Chromecast

Optionnel, à faire une seule fois en USB. Chaque étape est réversible.

### Mesurer d'abord

Mémoire totale et plus gros consommateurs (à lancer avant et après pour comparer) :

```bash
adb -d shell dumpsys meminfo
```

Fluidité du launcher pendant la navigation (regarde « Janky frames » et les percentiles) :

```bash
adb -d shell dumpsys gfxinfo fr.lite.launcher
```

### Désactiver les apps Google inutiles

Lister les apps actives, puis décider une par une parmi celles réellement installées :

```bash
adb -d shell pm list packages -e
```

En désactiver une (elle s'arrête complètement, même après redémarrage) :

```bash
adb -d shell pm disable-user --user 0 <paquet>
```

La réactiver :

```bash
adb -d shell pm enable <paquet>
```

Candidates habituelles, si elles sont présentes et inutilisées : recommandations de contenus
(`com.google.android.tvrecommendations`), Play Films (`com.google.android.videos`), retours
d'expérience (`com.google.android.feedback`), YouTube Music (`com.google.android.youtube.tvmusic`),
Play Jeux (`com.google.android.play.games`). Vérifie que chaque nom existe sur la Chromecast avant
de le désactiver.

À ne jamais désactiver : services Google Play (`com.google.android.gms`, `com.google.android.gsf`),
Play Store (`com.android.vending`), Assistant et recherche vocale (`com.google.android.katniss`),
réception Cast (`com.google.android.apps.mediashell`), tout ce qui commence par
`com.android.providers.`, et tout ce dont tu n'es pas sûr. Dans le doute, n'y touche pas.

### Réglages sur la TV

- Le Mode Ambiant télécharge photos et infos en continu. Remplace-le par un économiseur simple dans
  Paramètres > Système > Mode Ambiant (ou Économiseur d'écran).
- Play Store > Paramètres > Mise à jour automatique des apps : désactivée, puis mets à jour à la
  main de temps en temps. Évite les installations en fond pendant que tu regardes.
- Garde au moins 1 à 2 Go libres (Paramètres > Système > Stockage). Désinstalle les apps
  inutilisées, vide le cache des plus grosses.
- Sons système (Paramètres > Son) : les couper supprime le clic de navigation, qui arrive en retard
  à cause du HDMI.
- Optionnel : Options pour les développeurs > Limite de processus en arrière-plan. Économise de la
  mémoire, mais les apps se rechargent plus souvent quand tu y reviens. À tester, à garder seulement
  si c'est mieux.

## Revenir en arrière

En USB. Réactive d'abord les écrans d'accueil Google et l'assistant (sinon le bouton Home n'aurait
plus nulle part où aller une fois Lite Launcher désinstallé) :

```bash
adb -d shell pm enable com.google.android.tungsten.setupwraith
```

```bash
adb -d shell pm enable com.google.android.tvlauncher
```

```bash
adb -d shell pm enable com.google.android.apps.tv.launcherx
```

```bash
adb -d uninstall fr.lite.launcher
```

Pour remettre les animations normales, relance les trois commandes `settings put global` ci-dessus
avec `1` au lieu de `0.5`.

Une mise à jour système de Google peut réactiver l'écran d'accueil Google. Dans ce cas, relance
les commandes de désactivation.

## Compilation

Nécessite le JDK 17+ et le SDK Android (platform 34, build-tools 34).

Si Gradle échoue avec « Unable to establish loopback connection » sous Windows, le JDK n'arrive pas
à créer son socket local dans le dossier temporaire. Indique-lui un dossier qui t'appartient, par
exemple `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\Users\<toi>`.

```bash
gradle assembleRelease
```

L'APK se trouve dans `app/build/outputs/apk/release/app-release.apk`.
