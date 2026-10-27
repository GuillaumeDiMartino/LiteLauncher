# Lite Launcher

English | [Français](README.fr.md)

A minimal home screen for Chromecast with Google TV: greeting, clock, favorites, recent apps,
all apps, and quick settings. No recommendations, no ads, no background services, no network
access, no libraries.

Rule for every change: it must never hurt fluidity or memory use.

Everything below is done over ADB, either with the Chromecast plugged into the PC over USB, or
over Wi-Fi with wireless debugging. Tested on Chromecast with Google TV 4K and HD (Android 14).

## Do everything with Claude Code

Claude Code can do the whole installation for you: get the project, install the tools, build,
connect to the Chromecast, install the launcher and check the result with a screenshot. You only
have a few things to do on the TV.

**You need:**
- a PC with the Claude app (Code tab);
- the Chromecast plugged into the TV;
- a USB-C data cable, or else the PC and the Chromecast on the same Wi-Fi.

**On the Chromecast, once:**
1. Settings > System > About: press "Android TV OS build" 7 times.
2. Settings > System > Developer options: turn on "USB debugging" (and "Wireless debugging" if you
   go over Wi-Fi).
3. Plug the Chromecast's USB-C port into the PC (HDMI stays on the TV).

**Then paste this prompt in Claude Code** (in French; Claude answers in the language you use):

```text
Installe Lite Launcher sur ma Chromecast avec Google TV : https://github.com/GuillaumeDiMartino/LiteLauncher

Contexte : c'est un écran d'accueil Android TV ultra léger (Java natif, aucune bibliothèque) qui remplace l'écran Google TV, trop lourd. Lis README.fr.md et le code avant de commencer.

Règles :
- Chaque modification ne doit jamais dégrader les performances ni la fluidité (pas de flou, pas de réseau, pas de service en arrière-plan, pas de bibliothèque lourde). Indique le coût de chaque idée avant de la coder et mesure sur la Chromecast avec dumpsys gfxinfo avant de garder un changement.
- Demande-moi avant chaque téléchargement, avant d'accepter une licence, et avant de désactiver quoi que ce soit sur la Chromecast.
- Dis-moi clairement ce que je dois faire moi-même sur la TV (appuyer, accepter, lire un code).

Étapes :
1. Récupère le projet (git clone, ou l'archive ZIP de GitHub si Git n'est pas installé).
2. Installe ce qui manque dans un dossier à part : ADB (platform-tools), JDK 17+, SDK Android (platform 34, build-tools 34), Gradle 8.7. Si Gradle échoue avec "Unable to establish loopback connection", applique la solution de la section Compilation.
3. Compile l'APK en release.
4. Connecte-toi à la Chromecast : d'abord en USB (adb -d). Pendant que j'accepte la fenêtre d'autorisation sur la TV, ne relance ni adb ni la connexion. Si elle reste "unauthorized", passe en Wi-Fi : je te donnerai l'adresse et le code d'association du débogage sans fil.
5. Inspecte la Chromecast : modèle, version d'Android, mémoire disponible, et liste des écrans d'accueil (cmd package query-activities pour la catégorie HOME).
6. Installe l'APK et définis-le comme écran d'accueil.
7. Montre-moi quels écrans Google tu vas désactiver (normalement launcherx, tvlauncher, setupwraith) et pourquoi, rappelle-moi les commandes pour revenir en arrière, puis attends mon accord. Garde l'USB branché si possible pendant cette étape.
8. Appuie sur Home par ADB, fais une capture d'écran et vérifie que Lite Launcher s'affiche. En cas d'écran noir, rouvre Lite Launcher par ADB et explique-moi.
9. Passe les animations système à 0.5x et mesure la mémoire disponible avant/après.
10. Propose-moi les optimisations de la section "Optimisations Chromecast" adaptées aux apps réellement installées, une par une.
11. À la fin, coupe le débogage sans fil s'il a été utilisé et résume ce qui a été fait et comment tout annuler.
```

During the install, Claude asks you to accept the authorization prompt on the TV (tick "Always
allow"), to approve downloads, and to confirm before disabling the Google home screens.

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

The Chromecast should appear as `device` (not `unauthorized`). Accept the prompt only once and
leave the PC alone meanwhile: restarting adb or reconnecting cancels the pending prompt. If it stays
`unauthorized`, use Wi-Fi below.

## Or connect over Wi-Fi

Works on both models tested, including when USB authorization fails. The PC must be on the same
network as the Chromecast.

1. Developer mode as above, then Settings > System > Developer options: turn on "Wireless debugging".
2. Open it, choose "Pair device with pairing code", and pair with the address and code shown:

```bash
adb pair <IP>:<pairing port> <code>
```

3. Connect with the address shown on the main Wireless debugging screen (a different port):

```bash
adb connect <IP>:<port>
```

4. In every command below, replace `-d` (USB) with `-s <IP>:<port>`.

Wireless debugging turns itself off at each reboot; pairing is kept, so next time only step 3 is
needed after turning it back on. When you are done, turn it off (it also closes the connection):

```bash
adb -s <IP>:<port> shell settings put global adb_wifi_enabled 0
```

Keep a USB connection when possible while disabling the Google home screens: if the screen goes
black, Wi-Fi debugging can drop, USB keeps working.

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

With the Chromecast connected (see above). `-d` targets the USB device; over Wi-Fi use
`-s <IP>:<port>` instead.

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

Optional, done once over USB or Wi-Fi. Every step is reversible.

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
- The remote's power button turns the screen off through HDMI-CEC or infrared. On a PC monitor
  (no CEC) it does nothing; on a TV, enable CEC (Anynet+, Bravia Sync, SimpLink...).
- Optional: Developer options > Background process limit. Saves memory, but apps reload more often
  when you switch back. Try it, keep it only if it feels better.

## Undo

Over USB or Wi-Fi. Re-enable the Google home screens and setup app first (otherwise Home has nowhere to go
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
