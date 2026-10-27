# Lite Launcher

[English](README.md) | Français

Un écran d'accueil minimaliste pour Chromecast avec Google TV : salutation, horloge, favoris,
apps récentes, toutes les apps et réglages rapides. Pas de recommandations, pas de pub, pas de
service en arrière-plan, pas d'accès réseau, aucune bibliothèque.

Règle pour chaque modification : elle ne doit jamais dégrader la fluidité ni la mémoire.

Testé sur Chromecast avec Google TV 4K et HD (Android 14).

Deux façons d'installer :

- **[Option 1 : avec Claude Code](#option-1--avec-claude-code)** : Claude fait tout, en Wi-Fi.
  Tu n'as que quelques gestes à faire sur la TV.
- **[Option 2 : à la main](#option-2--à-la-main)** : tu tapes toi-même les commandes ADB, en USB
  ou en Wi-Fi.

---

# Option 1 : avec Claude Code

Claude Code récupère le projet, installe les outils, compile, se connecte à la Chromecast **en
Wi-Fi**, installe le launcher et vérifie le résultat par capture d'écran.

## Ce qu'il te faut

- un PC avec l'app Claude (onglet Code) ;
- la Chromecast branchée sur la TV, sur le **même réseau Wi-Fi** que le PC.

## À faire toi-même sur la Chromecast

1. Paramètres > Système > À propos : appuie 7 fois sur « Build de l'OS Android TV ».
2. Paramètres > Système > Options pour les développeurs : active « Débogage sans fil ».
3. Ouvre « Débogage sans fil » et choisis « Associer l'appareil avec un code ». Garde cette
   fenêtre ouverte : Claude te demandera l'adresse et le code affichés.

## Le prompt à coller dans Claude Code

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

## Pendant l'installation

Claude te demandera l'adresse et le code d'association, de valider les téléchargements, puis de
confirmer avant de désactiver les écrans Google.

Le débogage sans fil se coupe à chaque redémarrage de la Chromecast. Si la connexion est perdue,
réactive-le dans les Options pour les développeurs : l'association est gardée, il suffit de donner
à Claude la nouvelle adresse:port de l'écran principal.

En dernier recours, si la Chromecast reste sur un écran noir sans connexion possible, la
réinitialisation d'usine (maintenir le bouton de la Chromecast) remet tout comme avant.

---

# Option 2 : à la main

Toutes les commandes ci-dessous utilisent `-d` (Chromecast branchée en USB). En Wi-Fi, remplace
`-d` par `-s <IP>:<port>`.

## Se connecter

### En USB (le plus fiable)

L'USB ne dépend pas du Wi-Fi et continue de marcher quoi qu'il y ait à l'écran, même si l'écran
d'accueil ne s'affiche plus.

1. Sur la Chromecast : Paramètres > Système > À propos, appuie 7 fois sur « Build de l'OS Android TV ».
2. Paramètres > Système > Options pour les développeurs : active « Débogage USB ».
3. Laisse le HDMI de la Chromecast branché sur la TV, et branche sa prise USB-C sur le PC avec un
   câble USB-C **qui transmet les données** (un câble de charge seule ne marche pas). Si elle
   redémarre en boucle, utilise un hub USB-C avec alimentation.
4. Sur la TV, accepte « Autoriser le débogage USB ? » en cochant « Toujours autoriser sur cet
   ordinateur ». Ne relance ni adb ni la connexion pendant ce temps : ça annule la demande.
5. Vérifie :

```bash
adb devices
```

La Chromecast doit apparaître avec `device` (et non `unauthorized`). Si elle reste
`unauthorized`, passe par le Wi-Fi.

### En Wi-Fi

Le PC doit être sur le même réseau que la Chromecast.

1. Mode développeur comme ci-dessus, puis Options pour les développeurs : active « Débogage sans fil ».
2. Ouvre-le, choisis « Associer l'appareil avec un code », et associe :

```bash
adb pair <IP>:<port d'association> <code>
```

3. Connecte-toi avec l'adresse de l'écran principal du débogage sans fil (un autre port) :

```bash
adb connect <IP>:<port>
```

Le débogage sans fil se coupe à chaque redémarrage ; l'association est gardée, donc la fois
suivante seule l'étape 3 est nécessaire après l'avoir réactivé. Quand tu as fini, coupe-le :

```bash
adb -s <IP>:<port> shell settings put global adb_wifi_enabled 0
```

## Installation

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
Chromecast) réactive tout automatiquement. Désactive les trois d'affilée, sans appuyer sur Home
entre-temps.

```bash
adb -d shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
```

```bash
adb -d shell pm disable-user --user 0 com.google.android.tvlauncher
```

```bash
adb -d shell pm disable-user --user 0 com.google.android.tungsten.setupwraith
```

3. Appuie sur Home : Lite Launcher doit s'afficher. Si l'écran reste noir, reconnecte-toi (l'USB
est le plus fiable ici) et lance les commandes de « Revenir en arrière ».

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

Tu peux ensuite débrancher le câble USB (ou couper le débogage sans fil) : tous les réglages
survivent aux redémarrages.

## Optimisations Chromecast

Optionnel, à faire une seule fois. Chaque étape est réversible.

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

Réactive d'abord les écrans d'accueil Google et l'assistant (sinon le bouton Home n'aurait plus
nulle part où aller une fois Lite Launcher désinstallé) :

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

Pour remettre les animations normales, relance les trois commandes `settings put global` avec `1`
au lieu de `0.5`.

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

---

# Utilisation

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
- Le bouton d'alimentation de la télécommande éteint l'écran par HDMI-CEC ou infrarouge. Sur un
  écran de PC (sans CEC), il ne fait rien ; sur une TV, active le CEC (Anynet+, Bravia Sync,
  SimpLink…).
