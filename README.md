# 🧬 Evolution Mod — Minecraft Forge 1.21.1

> *"La vie ne fait pas ce qu'on lui dit. Elle évolue."*

## Vue d'ensemble

**Evolution Mod** soumet Minecraft aux lois de la biologie évolutive. Chaque mob, chaque arbre, chaque bûche et chaque fruit accumule des **mutations génétiques** au fil des jours. Le monde n'est jamais statique : après quelques centaines de jours, vos zombies pourraient être des colosses bioluminescents venimeux, et vos chênes pourraient porter des fruits téléportants.

---

## 🏗️ Architecture du mod

```
com.evolutionmod/
│
├── EvolutionMod.java                  ← Point d'entrée Forge
│
├── genetics/
│   ├── Gene.java                      ← Unité génétique de base (valeur + mutation)
│   ├── Genome.java                    ← Génome complet (50+ gènes par entité)
│   └── GeneticEvent.java              ← Historique des événements évolutifs
│
├── core/
│   ├── EvolutionCapability.java       ← Attache un génome à chaque LivingEntity
│   ├── EvolutionManager.java          ← Orchestre les cycles d'évolution quotidiens
│   └── EvolutionCommand.java          ← Commandes /evolution
│
├── events/
│   ├── DayChangeEventHandler.java     ← Déclenche l'évolution à chaque aube
│   └── EntityEvolutionHandler.java    ← Applique les traits génétiques in-game
│
├── world/
│   ├── TreeEvolutionHandler.java      ← Évolution arbres, bûches, fruits
│   └── EvolutionDataSavedData.java    ← Persistance (world/data/)
│
├── network/
│   ├── EvolutionNetwork.java          ← Canal de synchronisation client/serveur
│   ├── SyncGenomePacket.java          ← Sync génome entité → client
│   └── EvolutionEventPacket.java      ← Broadcast des speciations
│
└── data/
    └── MutantMeatItem.java            ← Item viande mutante avec effets génétiques
```

---

## 🧬 Système de Gènes

### Types de gènes (50+)

**Morphologiques** (apparence)
| Gène | Plage | Effet |
|------|-------|-------|
| `BODY_SIZE` | 0.1 – 6.0 | Taille via attribut `SCALE` |
| `COLOR_HUE` | 0 – 360 | Teinte HSV (stockée en NBT) |
| `COLOR_SATURATION` | 0 – 1 | Saturation de couleur |
| `BIOLUMINESCENCE` | 0 – 15 | Glow tag + remplacement feuilles |
| `APPENDAGE_COUNT` | 0 – 8 | Appendices supplémentaires |
| `EYE_TYPE` | 0 – 4 | Normal / Multiple / Absent / IR |
| `SKIN_TEXTURE` | 0 – 5 | Normal / Écailleux / Épineux / Lumineux |

**Capacités**
| Gène | Plage | Effet in-game |
|------|-------|---------------|
| `SPEED` | 0.1 – 5.0 | `MOVEMENT_SPEED` × 0.1 |
| `STRENGTH` | 0.1 – 10.0 | `ATTACK_DAMAGE` |
| `DEFENSE` | 0 – 1 | `KNOCKBACK_RESISTANCE` + réduction dégâts |
| `HEALTH_MAX` | 1 – 500 | `MAX_HEALTH` |
| `FLIGHT` | 0 – 2 | Gravité réduite (planement, vol actif) |
| `VENOMOUS` | 0 – 10 | Poison appliqué à chaque attaque |
| `REGENERATION` | 0 – 2 | Effet Regeneration passif |

**Végétaux** (arbres uniquement)
| Gène | Plage | Effet |
|------|-------|-------|
| `FRUIT_EFFECT` | 0 – 20 | Effets de potion en mangeant |
| `WOOD_HARDNESS` | 0 – 10 | Durabilité des outils + drop spécial |
| `BIOLUMINESCENCE` | 0 – 15 | Remplacement feuilles → Shroomlight |
| `HEIGHT_MAX` | 1 – 30 | Hauteur maximale à la pousse |

---

## ⚙️ Mécanique d'évolution

### Cycle quotidien
Chaque aube Minecraft (toutes les 24 000 ticks ≈ 20 min réelles) :

```
1. onDayChange() déclenché par DayChangeEventHandler
2. Calcul de la pression environnementale globale :
   pression = min(1.0, 0.05 + (jour / 1000) × 0.95)
3. Pour chaque LivingEntity chargée :
   a. Récupérer le génome via Capability
   b. applyDailyMutation(pression, jour)
   c. Appliquer les mutations aux attributs Minecraft
4. Évolution des génomes d'espèces d'arbres
5. Sauvegarde (EvolutionDataSavedData)
```

### Types de mutations

```
Mutation ponctuelle (85%)  → δ ∼ N(0, σ)    petite variation
Mutation modérée   (12%)  → δ ∼ U(-r, r)    variation moyenne  
Mutation radicale  (3%)   → saut vers valeur aléatoire dans [min, max]
```

### Spéciation
Quand une entité atteint **50 mutations cumulées**, elle a 15% de chances de diverger vers une nouvelle lignée. Sa lignée devient par exemple :
```
zombie → zombie>NeoMorphus → zombie>NeoMorphus>OmegaVex
```
Un message est broadcast à tous les joueurs.

### Dérive génétique
Indépendamment de la pression sélective, une dérive neutre de 5% par jour mène à une divergence lente même sans pression.

---

## 🎮 Commandes

| Commande | Accès | Description |
|----------|-------|-------------|
| `/evolution status` | Tous | Statistiques mondiales d'évolution |
| `/evolution inspect` | Tous | Génome de l'entité regardée |
| `/evolution history` | Tous | Historique des speciations |
| `/evolution genome` | Tous | Message sur le joueur |
| `/evolution accelerate` | OP | Force 5 cycles d'évolution |

---

## 🍖 Drops Génétiques

### Cristal Génomique
- Drop aléatoire de tout mob ayant **20+ mutations**
- Contient les données génétiques complètes (NBT)
- Peut être utilisé pour étudier l'évolution

### Viande Mutante
- Drop des mobs à génome divergent
- Effets selon les gènes du mob tué :
  - Venin élevé → Poison au joueur
  - Bioluminescence → Vision nocturne
  - Force élevée → Buff de force temporaire
  - 50+ mutations → Effet aléatoire (chaos)

### Bois Muté
- Drop des bûches d'arbres à `WOOD_HARDNESS > 7`
- Renommé et coloré selon les mutations
- NBT contient le génome de l'arbre

---

## 🏗️ Installation

### Prérequis
- Java 21+
- Minecraft 1.21.1
- Forge 52.0.7

### Build
```bash
./gradlew build
```
Le `.jar` est dans `build/libs/evolutionmod-1.0.0.jar`

### Installation
Copier dans `mods/` de votre installation Forge 1.21.1.

---

## 🔧 Extensions possibles

Ce mod est conçu pour être étendu :

### Renderer de couleurs génétiques
Créer un `IEntityRenderer` custom qui lit `GeneticColorRGB` du NBT persistant et applique la teinte HSV comme overlay de texture.

### Mixins pour comportements avancés
- `MixinZombie` : modifier les animations selon `APPENDAGE_COUNT`
- `MixinSpider` : vrai climbing selon le gène `CLIMBING`
- `MixinEntity` : vol actif selon `FLIGHT`

### Blocs custom
- `BioluminescentLeavesBlock` : feuilles lumineuses avec `getLightEmission()` basé sur le gène
- `MutantLogBlock` : bûches colorées selon `COLOR_HUE`

### GUI de séquençage génomique
Un écran de microscope pour analyser les cristaux génomiques et voir l'arbre phylogénétique.

### Sélection naturelle active
Implémenter un système où les entités moins adaptées ont un taux de mortalité plus élevé, forçant une vraie sélection darwinienne.

---

## 📊 Paramètres d'équilibrage

Dans `EvolutionManager.java` :
```java
// Vitesse d'évolution (0.02 = 2% de mutations par gène par jour par défaut)
double mutationRate = 0.02 + new Random().nextDouble() * 0.03;

// Pression environnementale maximale
globalEnvironmentalPressure = Math.min(1.0, 0.05 + (newDay / 1000.0) * 0.95);

// Seuil de spéciation
if (totalMutations % 50 == 0 && random.nextDouble() < 0.15)
```

---

*"Ce ne sont pas les espèces les plus fortes qui survivent, ni les plus intelligentes, mais celles qui s'adaptent le mieux au changement."*  
— Charles Darwin
