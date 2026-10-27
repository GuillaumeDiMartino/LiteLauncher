# Lite Launcher

English | [Français](README.fr.md)

A minimal home screen for Chromecast with Google TV: greeting, clock, favorites, recent apps,
all apps, and quick settings. No recommendations, no ads, no background services, no network
access, no libraries.

Rule for every change: it must never hurt fluidity or memory use.

Tested on Chromecast with Google TV 4K and HD (Android 14).

Two ways to install:

- **[Option 1: with Claude Code](#option-1-with-claude-code)**: Claude does everything, over
  Wi-Fi. You only do a few things on the TV.
- **[Option 2: by hand](#option-2-by-hand)**: you type the ADB commands yourself, over USB or Wi-Fi.

---

# Option 1: with Claude Code

Claude Code gets the project, installs the tools, builds, connects to the Chromecast **over
Wi-Fi**, installs the launcher and checks the result with a screenshot.

## You need

- a PC with the Claude app (Code tab);
- the Chromecast plugged into the TV, on the **same Wi-Fi network** as the PC.

## On the Chromecast, yourself

1. Settings > System > About: press "Android TV OS build" 7 times.
2. Settings > System > Developer options: turn on "Wireless debugging".
3. Open "Wireless debugging" and choose "Pair device with pairing code". Keep this screen open:
   Claude will ask for the address and code it shows.

## The prompt to paste in Claude Code

The prompt is in French; Claude answers in the language you use.

```text
Installe Lite Launcher sur ma Chromecast avec Google TV : https://github.com/GuillaumeDiMartino/LiteLauncher

Contexte : c'est un écran d'accueil Android TV ultra léger (Java natif, aucune bibliothèque) qui remplace l'écran Google TV, trop lourd. Lis README.fr.md (Option 1) et le code avant de commencer.

Connexion : uniquement en Wi-Fi, avec le débogage sans fil de la Chromecast. Je te donnerai l'adresse:port et le code d'association affichés dans « Associer l'appareil avec un code », puis tu utilises adb pair, adb connect et adb -s <adresse> pour toutes les commandes.

Règles :
- Chaque modification ne doit jamais dégrader les performances ni la fluidité (pas de flou, pas de réseau, pas de service en arrière-plan, pas de bibliothèque lourde). Indique le coût de chaque idée avant de la coder et mesure sur la Chromecast avec dumpsys gfxinfo avant de garder un changement.
- Demande-moi avant chaque téléchargement, avant d'accepter une licence, et avant de désactiver quoi que ce soit sur la Chromecast.
- Dis-moi clairement ce que je dois faire moi-même sur la TV.

Étapes :
1. Récupère le projet (git clone, ou l'archive ZIP de GitHub si Git n'est pas installé).
2. Installe ce qui manque dans un dossier à part : ADB (platform-tools), JDK 17+, SDK Android (platform 34, build-tools 34), Gradle 8.7. Si Gradle échoue avec "Unable to establish loopback connection", applique la solution de la section Compilation.
3. Compile l'APK en release.
4. Demande-moi l'adresse et le code d'association, puis associe et connecte-toi en Wi-Fi.
5. Inspecte la Chromecast : modèle, version d'Android, mémoire disponible, et liste des écrans d'accueil (cmd package query-activities pour la catégorie HOME).
6. Installe l'APK et définis-le comme écran d'accueil (cmd package set-home-activity).
7. Montre-moi les écrans Google à désactiver (normalement launcherx, tvlauncher et setupwraith) et pourquoi, rappelle-moi les commandes pour revenir en arrière, puis attends mon accord. Désactive-les ensuite tous les trois dans une seule commande adb shell, suivie aussitôt de set-home-activity : si un seul est désactivé, la Chromecast affiche un écran noir et le débogage sans fil peut se couper.
8. Appuie sur Home par ADB, fais une capture d'écran et vérifie que Lite Launcher s'affiche. En cas d'écran noir, rouvre Lite Launcher avec am start et explique-moi.
9. Passe les animations système à 0.5x et mesure la mémoire disponible avant/après.
10. Propose-moi les optimisations de la section « Optimisations Chromecast » adaptées aux apps réellement installées, une par une.
11. À la fin, coupe le débogage sans fil (settings put global adb_wifi_enabled 0) et résume ce qui a été fait et comment tout annuler.
```

## During the install

Claude asks you for the address and pairing code, to approve downloads, then to confirm before
disabling the Google home screens.

Wireless debugging turns itself off at each Chromecast reboot. If the connection drops, turn it
back on in Developer options: pairing is kept, just give Claude the new address:port from the
main screen.

As a last resort, if the Chromecast stays on a black screen with no way to connect, a factory reset
(hold the button on the Chromecast) puts everything back.

---

# Option 2: by hand

All commands below use `-d` (Chromecast plugged in over USB). Over Wi-Fi, replace `-d` with
`-s <IP>:<port>`.

## Connect

### Over USB (most reliable)

USB does not depend on Wi-Fi and keeps working whatever is on screen, even if the home screen
breaks.

1. On the Chromecast: Settings > System > About, press "Android TV OS build" 7 times.
2. Settings > System > Developer options: turn on "USB debugging".
3. Leave the Chromecast's HDMI plugged into the TV, and plug its USB-C port into the PC with a
   **data** USB-C cable (a charge-only cable will not work). If it keeps rebooting, use a USB-C hub
   with power passthrough.
4. On the TV, accept "Allow USB debugging?" and tick "Always allow from this computer". Do not
   restart adb or reconnect meanwhile: that cancels the pending prompt.
5. Check:

```bash
adb devices
```

The Chromecast should appear as `device` (not `unauthorized`). If it stays `unauthorized`, use
Wi-Fi.

### Over Wi-Fi

The PC must be on the same network as the Chromecast.

1. Developer mode as above, then Developer options: turn on "Wireless debugging".
2. Open it, choose "Pair device with pairing code", and pair:

```bash
adb pair <IP>:<pairing port> <code>
```

3. Connect with the address shown on the main Wireless debugging screen (a different port):

```bash
adb connect <IP>:<port>
```

Wireless debugging turns itself off at each reboot; pairing is kept, so next time only step 3 is
needed after turning it back on. When you are done, turn it off:

```bash
adb -s <IP>:<port> shell settings put global adb_wifi_enabled 0
```

## Install

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
the Chromecast) re-enables everything automatically. Disable all three in a row, without pressing
Home in between.

```bash
adb -d shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
```

```bash
adb -d shell pm disable-user --user 0 com.google.android.tvlauncher
```

```bash
adb -d shell pm disable-user --user 0 com.google.android.tungsten.setupwraith
```

3. Press Home on the remote: Lite Launcher should show. If the screen stays black, reconnect (USB
is the most reliable here) and run the commands in "Undo".

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

You can unplug the USB cable (or turn wireless debugging off) afterwards: every setting survives
reboots.

## Chromecast optimizations

Optional, done once. Every step is reversible.

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

Re-enable the Google home screens and setup app first (otherwise Home has nowhere to go once Lite
Launcher is uninstalled):

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

Restore normal animations by running the three `settings put global` commands with `1` instead of
`0.5`.

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

---

# Usage

- Arrows to move, OK to open an app.
- Hold OK on an app to add or remove it from favorites, move it within its section, hide it,
  open its app info, or uninstall it.
- Quick settings row: Wi-Fi, Bluetooth, Display, Screensaver ("Veille"), Play Store, Settings,
  Style (accent color and tile size), and "Masquées" to show hidden apps again.
- "Récents" lists the last apps opened from Lite Launcher (no permission needed). Apps opened
  another way, such as the YouTube or Netflix buttons on the remote, are not counted.
- The screen only scrolls when the selected tile would leave it.
- Home jumps back to the top.
- The remote's power button turns the screen off through HDMI-CEC or infrared. On a PC monitor
  (no CEC) it does nothing; on a TV, enable CEC (Anynet+, Bravia Sync, SimpLink...).
