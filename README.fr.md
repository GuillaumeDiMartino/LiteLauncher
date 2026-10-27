# Lite Launcher

[English](README.md) | Français

Un écran d'accueil minimaliste pour Chromecast avec Google TV : salutation, horloge, favoris,
apps récentes, toutes les apps et réglages rapides. Pas de recommandations, pas de pub, pas de
service en arrière-plan, pas d'accès réseau, aucune bibliothèque.

Règle pour chaque modification : elle ne doit jamais dégrader la fluidité ni la mémoire.

Tout ce qui suit se fait par ADB, soit avec la Chromecast branchée en USB sur le PC, soit en Wi-Fi
avec le débogage sans fil. Testé sur Chromecast avec Google TV 4K et HD (Android 14).

## Tout faire avec Claude Code

Claude Code peut faire toute l'installation à ta place : récupérer le projet, installer les outils,
compiler, se connecter à la Chromecast, installer le launcher et vérifier le résultat par capture
d'écran. Tu n'as que quelques gestes à faire sur la TV.

**Ce qu'il te faut :**
- un PC avec l'app Claude (onglet Code) ;
- la Chromecast branchée sur la TV ;
- un câble USB-C qui transmet les données, ou à défaut le PC et la Chromecast sur le même Wi-Fi.

**À faire toi-même sur la Chromecast, une seule fois :**
1. Paramètres > Système > À propos : appuie 7 fois sur « Build de l'OS Android TV ».
2. Paramètres > Système > Options pour les développeurs : active « Débogage USB » (et
   « Débogage sans fil » si tu passes par le Wi-Fi).
3. Branche la prise USB-C de la Chromecast sur le PC (le HDMI reste sur la TV).

**Puis colle ce prompt dans Claude Code :**

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

Pendant l'installation, Claude te demandera d'accepter la fenêtre d'autorisation sur la TV (coche
« Toujours autoriser »), de valider les téléchargements, puis de confirmer avant de désactiver les
écrans Google.

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

La Chromecast doit apparaître avec `device` (et non `unauthorized`). Accepte la fenêtre une seule
fois sans toucher au PC entre-temps : relancer adb ou la connexion annule la demande en cours. Si
elle reste `unauthorized`, passe par le Wi-Fi ci-dessous.

## Ou se connecter en Wi-Fi

Marche sur les deux modèles testés, y compris quand l'autorisation USB échoue. Le PC doit être sur
le même réseau que la Chromecast.

1. Mode développeur comme ci-dessus, puis Paramètres > Système > Options pour les développeurs :
   active « Débogage sans fil ».
2. Ouvre-le, choisis « Associer l'appareil avec un code », et associe avec l'adresse et le code affichés :

```bash
adb pair <IP>:<port d'association> <code>
```

3. Connecte-toi avec l'adresse de l'écran principal du débogage sans fil (un autre port) :

```bash
adb connect <IP>:<port>
```

4. Dans toutes les commandes qui suivent, remplace `-d` (USB) par `-s <IP>:<port>`.

Le débogage sans fil se coupe à chaque redémarrage ; l'association est gardée, donc la fois
suivante seule l'étape 3 est nécessaire après l'avoir réactivé. Quand tu as fini, coupe-le (ça
ferme aussi la connexion) :

```bash
adb -s <IP>:<port> shell settings put global adb_wifi_enabled 0
```

Si possible, garde aussi l'USB branché quand tu désactives les écrans d'accueil Google : en cas
d'écran noir, le débogage sans fil peut se couper, l'USB continue de marcher.

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

Chromecast connectée (voir plus haut). `-d` cible l'appareil branché en USB ; en Wi-Fi, utilise
`-s <IP>:<port>` à la place.

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

3. Appuie sur Home : Lite Launcher doit s'afficher. Si l'écran reste noir, reconnecte-toi (l'USB
est le plus fiable ici) et lance les commandes de la section « Revenir en arrière ».

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

Optionnel, à faire une seule fois en USB ou en Wi-Fi. Chaque étape est réversible.

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
- Le bouton d'alimentation de la télécommande éteint l'écran par HDMI-CEC ou infrarouge. Sur un
  écran de PC (sans CEC), il ne fait rien ; sur une TV, active le CEC (Anynet+, Bravia Sync,
  SimpLink…).
- Optionnel : Options pour les développeurs > Limite de processus en arrière-plan. Économise de la
  mémoire, mais les apps se rechargent plus souvent quand tu y reviens. À tester, à garder seulement
  si c'est mieux.

## Revenir en arrière

En USB ou en Wi-Fi. Réactive d'abord les écrans d'accueil Google et l'assistant (sinon le bouton Home n'aurait
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
