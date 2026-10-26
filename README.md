# Lite Launcher

English | [Français](README.fr.md)

A minimal home screen for Chromecast with Google TV: greeting, clock, favorites, recent apps,
all apps, and quick settings. No recommendations, no ads, no background services, no network
access, no libraries.

Rule for every change: it must never hurt fluidity or memory use.

Everything below is done with the Chromecast plugged into the PC over USB. Tested on Chromecast
with Google TV (4K, Android 14).

## Connect the Chromecast over USB

USB is the most reliable connection: it does not depend on Wi-Fi, and it keeps working whatever is
on screen, even if the home screen breaks.

1. On the Chromecast: Settings > System > About, press "Android TV OS build" 7 times.
2. Settings > System > Developer options: turn on "USB debugging".
3. Leave the Chromecast's HDMI plugged into the TV (to see the screen), and plug its USB-C port
   into the PC with a **data** USB-C cable (a charge-only cable will not work). The PC port powers
   the Chromecast; if it keeps rebooting, use a USB-C hub with power passthrough.
4. On the TV, accept "Allow USB debugging?" and tick "Always allow from this computer".
5. Check it is connected:

```bash
adb devices
```

The Chromecast should appear as `device` (not `unauthorized`).

## Resume with Claude on another PC

Prerequisites: the Claude app (Code tab) and Git installed, and the Chromecast connected over USB as
above. Paste this prompt in Claude:

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

## Remote control

- Arrows to move, OK to open an app.
- Hold OK on an app to add or remove it from favorites, move it within its section, hide it,
  open its app info, or uninstall it.
- Quick settings row: Wi-Fi, Bluetooth, Display, Screensaver ("Veille"), Play Store, Settings,
  Style (accent color and tile size), and "Masquées" to show hidden apps again.
- "Récents" lists the last apps opened from Lite Launcher (no permission needed). Apps opened
  another way, such as the YouTube or Netflix buttons on the remote, are not counted.
- The screen only scrolls when the selected tile would leave it.
- Home jumps back to the top.

## Install

With the Chromecast connected over USB (see above). `-d` targets the USB device.

1. Install and set as home:

```bash
adb -d install -r app-release.apk
```

```bash
adb -d shell cmd package set-home-activity fr.lite.launcher/.MainActivity
```

2. Disable the Google home screens. All three are needed: with only `launcherx` disabled, Home falls
back to `tvlauncher`, then to a black "RecoveryActivity" screen from the setup app (`setupwraith`),
which outranks any other launcher. The shell cannot disable just that screen on Android 14, so the
whole setup package is disabled. A factory reset (Settings > System > About, or holding the button on
the Chromecast) re-enables everything automatically.

```bash
adb -d shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
```

```bash
adb -d shell pm disable-user --user 0 com.google.android.tvlauncher
```

```bash
adb -d shell pm disable-user --user 0 com.google.android.tungsten.setupwraith
```

3. Press Home on the remote: Lite Launcher should show. If the screen stays black, reconnect over
USB and run the commands in "Undo".

4. Make the whole device feel faster by halving system animations:

```bash
adb -d shell settings put global window_animation_scale 0.5
```

```bash
adb -d shell settings put global transition_animation_scale 0.5
```

```bash
adb -d shell settings put global animator_duration_scale 0.5
```

You can unplug the USB cable afterwards and power the Chromecast as usual: every setting survives
reboots.

## Chromecast optimizations

Optional, done once over USB. Every step is reversible.

### Measure first

Total memory and the biggest consumers (run before and after to compare):

```bash
adb -d shell dumpsys meminfo
```

Launcher smoothness while navigating (look at "Janky frames" and the percentiles):

```bash
adb -d shell dumpsys gfxinfo fr.lite.launcher
```

### Disable unused Google packages

List enabled packages, then decide app by app from what is actually installed:

```bash
adb -d shell pm list packages -e
```

Disable one (stops it entirely, survives reboots):

```bash
adb -d shell pm disable-user --user 0 <package>
```

Re-enable it:

```bash
adb -d shell pm enable <package>
```

Usual candidates, if present and unused: content recommendations (`com.google.android.tvrecommendations`),
Play Movies (`com.google.android.videos`), feedback (`com.google.android.feedback`), YouTube Music
(`com.google.android.youtube.tvmusic`), Play Games (`com.google.android.play.games`).
Check each name exists on the device before disabling it.

Never disable: Google Play services (`com.google.android.gms`, `com.google.android.gsf`), Play Store
(`com.android.vending`), Assistant and voice search (`com.google.android.katniss`), Cast receiver
(`com.google.android.apps.mediashell`), anything under `com.android.providers.*`, and anything you
are unsure about. When in doubt, leave it.

### Settings on the TV

- Ambient mode downloads photos and info continuously. Switch the screensaver to a simple one in
  Settings > System > Ambient mode (or Screensaver).
- Play Store > Settings > Auto-update apps: off, then update by hand from time to time. Avoids
  background installs while watching.
- Keep at least 1 to 2 GB free (Settings > System > Storage). Uninstall unused apps, clear the
  cache of large ones.
- System sounds (Settings > Sound): turning them off removes the navigation click, which arrives
  late through HDMI.
- Optional: Developer options > Background process limit. Saves memory, but apps reload more often
  when you switch back. Try it, keep it only if it feels better.

## Undo

Over USB. Re-enable the Google home screens and setup app first (otherwise Home has nowhere to go
once Lite Launcher is uninstalled):

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

Restore normal animations by running the three `settings put global` commands above with `1`
instead of `0.5`.

A Google system update can turn the stock launcher back on. If that happens, run the disable
commands again.

## Build

Requires JDK 17+ and the Android SDK (platform 34, build-tools 34).

If Gradle fails with "Unable to establish loopback connection" on Windows, the JDK cannot create
its local socket in the temp folder. Point it to a folder you own, e.g.
`JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\Users\<you>`.

```bash
gradle assembleRelease
```

The APK is at `app/build/outputs/apk/release/app-release.apk`.
