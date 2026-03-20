# SuperPeachSis

**SuperPeachSis** est un jeu de plateforme de type "Endless Runner" pour Android, développé en Java. Le projet met l'accent sur des interactions entre l'utilisateur et le matériel du smartphone (capteurs et tactile).

## Fonctionnalités

- **Gameplay Dynamique** : Un défilement infini avec une difficulté progressive (vitesse accrue au fil du temps).
- **Système de Parallaxe** : Un arrière-plan généré aléatoirement à chaque début de partie pour une immersion visuelle variée.
- **Interactions** :
    - **Slash Tactile** : Tracez un trait sur l'écran avec votre doigt pour détruire les blocs.
    - **Capteur de Lumière** : Couvrez le capteur de luminosité de votre téléphone pour éliminer instantanément toutes les scies à l'écran.

## Architecture du Projet

Le projet est organisé de manière modulaire pour faciliter la maintenance :
- `domain/model/` : Entités du jeu (Player, Enemy, Block).
- `ui/` : Activités Android et la vue principale (`GameView`).
- `utils/` : Gestionnaire de sprites, caméra et gestion des collisions.
- `assets/` : Ressources graphiques classées par catégories (tiles, backgrounds, characters, enemies).

## Captures d'écran

*(Captures d'écran)*

## Installation

1. Clonez le dépôt : ```git clone https://github.com/Balivern/SuperPeachSis.git```
2. Ouvrez le projet dans **Android Studio**.
3. Compilez et lancez sur un émulateur ou un appareil physique (recommandé pour tester les capteurs).

## Licence

Ce projet est sous licence MIT. Voir le fichier [LICENSE](LICENSE) pour plus de détails.
