# Projet PeerSim : Simulation et Analyse d'une synchronisation distribuée

Ce projet est une étude expérimentale d'un algorithme distribuée utilisant la plateforme de simulation PeerSim. L'algorithme distribué est une variante de l'algorithme Naimi-Trehel.
Les résultats de cette étude seront illustrés sous forme de graphiques.

## Prérequis
- **Python 3.x** et les bibliothèques Python suivantes : matplotlib, pandas, seaborn pour la production des graphiques
- **Java 1.5** minimum pour compiler Peersim
- **Le dossier PeerSim-1.0.5** qui inclut les fichiers JAR, à la racine du projet

## Structure du projet
- `src/` : Code source Java
- `config/` : Fichiers de configuration générés pour les simulations divisés selon les scénarios étudiés
- `graphs/` : Graphiques générés par l'exécutable `script_graph.py` 
- `metrics.txt` : Fichier de sortie contenant les valeurs des métriques calculées.

## Execution

### 1. Compilation du projet
* Avec un IDE tel qu'Eclipse, IntelliJ, VScode, la compilation est soit faite de manière automatique, ou à l'aide d'une commande `Build Project` intégrée.
* Pour une compilation manuelle, exécuter à la racine du projet :
```bash
javac -d out -cp ./peersim-1.0.5/peersim-1.0.5.jar src/**/*.java
```

> [!CAUTION] 
> Dans la variable `classpath` du script `run_simulations.py` : 
> Modifier les chemins du `classpath` si nécessaire pour pointer vers le répertoire contenant vos fichiers .class.
> Modifier également les chemins vers les JAR du répertoire `peersim-1.0.5̀` s'il n'est pas à la racine du projet.

### 2. Exécution des simulations
Lancer les simulations avec le script Python :
```bash 
python run_simulations.py
```
Ce script génère des fichiers de configurations dans `/configs` et les utilise pour les simulations.
À l'aide du module de contrôle `StatsCollector`, les données récoltées à chaque fin de simulation sont écrites dans le fichier `metrics.txt`

3. Visualisation des métriques
Lancer le script qui génère les graphiques pour chaque métrique :
```bash 
python graphs/run_simulations.py
```
Les graphiques sont placés dans `graphs/outputs`.