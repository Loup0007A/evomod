# 🧬 Evolution Mod — Minecraft Forge 1.21.1

> *« Ce ne sont pas les espèces les plus fortes qui survivent, mais celles qui s'adaptent le mieux au changement. »*

Evolution Mod soumet Minecraft aux lois de l'évolution. À chaque aube, les créatures, les plantes et même le joueur accumulent des mutations. **Votre façon de jouer devient la pression de sélection** : les mobs qui vous blessent transmettent leurs gènes, ceux que vous chassez disparaissent du pool génétique, et vos élevages deviennent de la sélection artificielle. Au fil des jours, les espèces divergent et donnent naissance à de nouvelles lignées.

## 📦 Installation

1. Installez **Minecraft 1.21.1** avec **Forge 52.0.x** ou plus récent.
2. Téléchargez le jar :
   - dans les [Releases](https://github.com/Loup0007A/evomod/releases), ou
   - directement dans le dossier [`releases/`](releases/) du dépôt (`evolutionmod-latest.jar`).
3. Copiez-le dans le dossier `mods/` de votre installation.

Le mod doit être installé sur le serveur **et** sur les clients.

## 🎮 Prise en main

| Objet / bloc | Recette | Rôle |
|---|---|---|
| **Journal d'évolution** | livre + éclat d'améthyste | Ouvre l'encyclopédie évolutive (aussi : touche **J**) |
| **Scanner d'évolution** | vitre, fer, redstone, améthyste | Clic droit sur une créature : analyse génétique détaillée. En main : génome de la créature visée en surimpression |
| **Cristal génomique** | 4 améthystes + verre (×2) | Séquence le génome d'une créature ; Maj + clic droit pour l'injecter dans une autre de la même espèce |
| **Mutagène** | œil d'araignée fermenté, poudre lumineuse, améthyste, fiole | Réactif de mutagenèse au laboratoire |
| **Laboratoire génétique** | fer, verre, cuivre, alambic | Fusion, clonage et mutagenèse de cristaux |

### Le Journal d'évolution
- **Monde** : jour, cycles, mutations, spéciations, pression environnementale (graphique) et classement des espèces les plus divergentes.
- **Espèces** : fiche complète de chaque espèce — arbre des lignées, population, naissances, chasses, courbe de divergence, traits, et tous les gènes comparés à l'ancêtre vanilla (survolez un gène pour sa description).
- **Histoire** : chronologie des spéciations et des premières observations.
- **Mon génome** : votre propre évolution.

### Le laboratoire
| Emplacement A | Emplacement B | Résultat |
|---|---|---|
| cristal plein | cristal plein (même espèce) | **Fusion** : cristal hybride (A est conservé) |
| cristal plein | cristal vierge | **Clonage** : copie de A |
| cristal plein | mutagène | **Mutagenèse** : A subit 2 à 4 mutations forcées |

Chaque opération consomme 1 redstone ou 1 poudre lumineuse.

## 🧬 Fonctionnement

### Gènes
Tous les gènes partent de la valeur **vanilla** : une créature non mutée est identique à l'originale.

| Catégorie | Gènes |
|---|---|
| Morphologie | taille, teinte, pigmentation, bioluminescence |
| Physique | vitesse, force, vitalité, carapace, agilité, ténacité |
| Métabolisme | régénération, résistance au feu, immunité, résistance au soleil (morts-vivants) |
| Comportement | agressivité (distance de détection), camouflage (invisibilité passagère) |
| Armes biologiques | venin, toucher glacial, puissance explosive (creepers) |
| Végétaux | croissance, rendement, puissance des fruits, densité du bois |

Les effets passent par des modificateurs d'attributs Minecraft (réversibles et compatibles avec les autres mods).

### Population et sélection
- Chaque espèce possède un **génome de population** dont naissent les nouveaux individus (avec une variation personnelle, et parfois un mutant spontané).
- **Sélection** : blesser ou tuer un joueur, survivre, se reproduire → l'espèce se rapproche de cet individu. Être tué par un joueur → l'espèce s'en éloigne.
- **Reproduction** : les bébés héritent d'un vrai croisement génétique de leurs parents.
- **Spéciation** : tous les 7 % de divergence avec l'ancêtre, une nouvelle lignée est nommée et annoncée.
- Le joueur évolue lui aussi (doucement), et manger de la viande mutante lui transmet un peu des gènes de la proie.

### Plantes
Arbres et cultures ont leur propre génome : pousse accélérée, récoltes supplémentaires, fruits mutants aux effets variés, feuillages bioluminescents colorés et bois mutant.

## ⌨️ Commandes

| Commande | Accès | Description |
|---|---|---|
| `/evolution` | tous | Ouvre le journal |
| `/evolution inspect [cible]` | tous | Analyse la créature visée |
| `/evolution species` | tous | Classement des espèces dans le chat |
| `/evolution history` | tous | Derniers événements |
| `/evolution cycle [n]` | op | Force n cycles d'évolution |
| `/evolution reset` | op | Efface l'histoire évolutive du monde |

## ⚙️ Configuration
`config/evolutionmod-common.toml` : taux de mutation, force de la sélection, évolution du joueur et son intensité, cycles rattrapés, évolution des plantes, annonces, drops génétiques, liste d'exclusion d'entités.

## 🔧 Compilation
```bash
./gradlew build
```
Le jar est produit dans `build/libs/`. Chaque push sur `main` compile le mod via GitHub Actions, publie une release et dépose le jar dans `releases/`.
