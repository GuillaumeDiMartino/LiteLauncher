# Lite Launcher

A minimal home screen for Chromecast with Google TV: greeting, clock, favorites, recent apps,
all apps, and quick settings. No recommendations, no ads, no background services, no network
access, no libraries.

Rule for every change: it must never hurt fluidity or memory use.

## Resume with Claude on another PC

Prerequisites: the Claude app (Code tab) and Git installed. On the Chromecast, enable developer mode
(Settings > System > About > press "Android TV OS build" 7 times), turn on USB debugging, and note its IP.

Paste this prompt in Claude, replacing `[IP]`:

```text
Clone https://github.com/GuillaumeDiMartino/LiteLauncher.git puis lis le README et le code.

Contexte : c'est un launcher Android TV ultra léger que j'ai conçu pour remplacer l'écran d'accueil Google TV de ma Chromecast avec Google TV, qui rame. Java natif sans aucune bibliothèque : salutation, horloge, sections Favoris / Récents / Applications / Réglages rapides, menu appui long (favoris, déplacer, masquer, désinstaller), couleur d'accent et taille des tuiles réglables. Le dossier preview/ contient une maquette HTML fidèle (960x540 dp) qui sert à travailler le design avant de le reporter dans le code Android.

Règle absolue : chaque modification ne doit jamais dégrader les performances ni la fluidité (pas de flou, pas de réseau, pas de service en arrière-plan, pas de bibliothèque lourde). Indique le coût de chaque idée avant de la coder.

Ma Chromecast est à l'IP [IP], mode développeur et débogage USB activés, sur le même Wi-Fi que ce PC.

Étapes :
1. Installe ce qu'il manque : ADB (platform-tools), JDK 17+, SDK Android (platform 34, build-tools 34), Gradle. Demande-moi avant chaque téléchargement.
2. Connecte-toi à la Chromecast avec adb connect (j'accepterai la fenêtre sur la TV).
3. Récupère les vraies bannières TV et icônes de mes apps installées, et branche-les dans preview/index.html à la place des logos d'exemple, avec la liste réelle de mes apps.
4. Lance l'aperçu dans le navigateur intégré pour que je valide le design.
5. Compile l'APK en release et installe-le sur la Chromecast.
6. Donne l'accès aux statistiques d'usage (rangée Récents) et passe les animations système à 0.5x, avec les commandes ADB de la section Install.
7. Avant de désactiver le launcher Google (com.google.android.apps.tv.launcherx), demande-moi confirmation et rappelle-moi la commande pour revenir en arrière.
8. Pousse les modifications sur le dépôt GitHub.
```

## Remote control

- Arrows to move, OK to open an app.
- Hold OK on an app to add or remove it from favorites, move it within its section, hide it,
  open its app info, or uninstall it.
- Quick settings row: Wi-Fi, Bluetooth, Display, Screensaver ("Veille"), Play Store, Settings,
  Style (accent color and tile size), and "Masquées" to show hidden apps again.
- "Récents" lists the apps used in the last 7 days. It only appears once usage access is
  granted (see Install).
- Home jumps back to the top.

## Install

1. On the Chromecast, open Settings > System > About and press "Android TV OS build" 7 times.
2. Go to Settings > System > Developer options and turn on "USB debugging".
3. Find the Chromecast IP address in Settings > Network & Internet.
4. On the PC (same Wi-Fi network):

```bash
adb connect <IP>:5555
```

Accept the prompt on the TV, then:

```bash
adb install -r app-release.apk
```

```bash
adb shell cmd package set-home-activity fr.lite.launcher/.MainActivity
```

```bash
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
```

The last command disables the stock Google TV launcher. That is what frees most of the RAM.

Allow the "Récents" row to read which apps were used (read only when you return home, no polling):

```bash
adb shell appops set fr.lite.launcher GET_USAGE_STATS allow
```

Make the whole device feel faster by halving system animations:

```bash
adb shell settings put global window_animation_scale 0.5
```

```bash
adb shell settings put global transition_animation_scale 0.5
```

```bash
adb shell settings put global animator_duration_scale 0.5
```

## Undo

```bash
adb shell pm enable com.google.android.apps.tv.launcherx
```

```bash
adb uninstall fr.lite.launcher
```

Restore normal animations by running the three `settings put global` commands above with `1`
instead of `0.5`.

A Google system update can turn the stock launcher back on. If that happens, run the disable command again.

## Build

Requires JDK 17+ and the Android SDK (platform 34, build-tools 34).

```bash
gradle assembleRelease
```

The APK is at `app/build/outputs/apk/release/app-release.apk`.
