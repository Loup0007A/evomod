package com.evolutionmod.genetics;

import com.evolutionmod.EvolutionMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.*;

/**
 * Le génome complet d'une entité ou d'une plante.
 * Contient tous les gènes, gère les mutations et la reproduction.
 * Chaque espèce démarre avec un génome de base qui diverge au fil du temps.
 */
public class Genome {

    private final Map<Gene.GeneType, Gene> genes = new EnumMap<>(Gene.GeneType.class);
    private String speciesLineage;  // Lignée évolutive (ex: "zombie>mutant_A>colossus")
    private int totalMutations;     // Nombre total de mutations accumulées
    private long dayBorn;           // Jour de naissance/création
    private double fitnessScore;    // Score d'adaptation à l'environnement

    // Dérive génétique : compteur de dérive pour chaque espèce
    private static final Map<String, Integer> SPECIES_DRIFT = new HashMap<>();

    public Genome(String baseSpecies) {
        this.speciesLineage = baseSpecies;
        this.totalMutations = 0;
        this.dayBorn = 0;
        this.fitnessScore = 1.0;
    }

    /**
     * Initialise le génome avec des valeurs de base pour une espèce donnée.
     */
    public static Genome createForSpecies(String entityType) {
        Genome genome = new Genome(entityType);
        Gene.GeneType[] relevantGenes = getGenesForSpecies(entityType);

        for (Gene.GeneType geneType : relevantGenes) {
            double[] bounds = Gene.getValueBounds(geneType);
            double baseValue = getBaseValueForSpecies(entityType, geneType);
            double dominance = 0.5 + new Random().nextDouble() * 0.3;
            double mutationRate = 0.02 + new Random().nextDouble() * 0.03; // 2-5% par jour
            double mutationRange = (bounds[1] - bounds[0]) * 0.15;

            genome.genes.put(geneType, new Gene(geneType, baseValue, dominance, mutationRate, mutationRange));
        }

        return genome;
    }

    /**
     * Applique les mutations de la journée.
     * environmentalPressure : 0.0 = environnement stable, 1.0 = pression maximale
     */
    public List<GeneticEvent> applyDailyMutation(double environmentalPressure, long currentDay) {
        List<GeneticEvent> events = new ArrayList<>();
        Random random = new Random();

        for (Map.Entry<Gene.GeneType, Gene> entry : genes.entrySet()) {
            Gene gene = entry.getValue();
            boolean mutated = gene.tryMutate(environmentalPressure);

            if (mutated) {
                totalMutations++;
                GeneticEvent event = new GeneticEvent(
                    GeneticEvent.EventType.MUTATION,
                    gene.getType(),
                    gene.getValue(),
                    currentDay
                );
                events.add(event);

                // Vérifier si une mutation radicale déclenche une spéciation
                if (totalMutations % 50 == 0 && random.nextDouble() < 0.15) {
                    String newLineage = speciesLineage + ">" + generateMutantName(currentDay);
                    speciesLineage = newLineage;
                    events.add(new GeneticEvent(
                        GeneticEvent.EventType.SPECIATION,
                        gene.getType(),
                        0,
                        currentDay
                    ));
                    EvolutionMod.LOGGER.info("Spéciation ! Nouvelle lignée: {}", newLineage);
                }
            }
        }

        // Dérive génétique neutre (accumulation de petits changements sans pression)
        if (random.nextDouble() < 0.05 * environmentalPressure) {
            applyGeneticDrift();
        }

        return events;
    }

    /**
     * Croise deux génomes pour créer un enfant (reproduction sexuée simulée).
     */
    public Genome reproduceWith(Genome other, long currentDay) {
        Genome child = new Genome(this.speciesLineage);
        child.dayBorn = currentDay;

        for (Gene.GeneType type : this.genes.keySet()) {
            Gene myGene = this.genes.get(type);
            Gene otherGene = other.genes.get(type);

            if (myGene != null && otherGene != null) {
                child.genes.put(type, myGene.crossWith(otherGene));
            } else if (myGene != null) {
                child.genes.put(type, myGene);
            } else if (otherGene != null) {
                child.genes.put(type, otherGene);
            }
        }

        // Héritage de la lignée du parent le plus divergent
        if (this.totalMutations > other.totalMutations) {
            child.speciesLineage = this.speciesLineage;
        } else {
            child.speciesLineage = other.speciesLineage;
        }

        return child;
    }

    /**
     * Dérive génétique : accumulation aléatoire de mutations neutres.
     */
    private void applyGeneticDrift() {
        Random random = new Random();
        List<Gene.GeneType> geneTypes = new ArrayList<>(genes.keySet());
        if (!geneTypes.isEmpty()) {
            Gene.GeneType randomType = geneTypes.get(random.nextInt(geneTypes.size()));
            genes.get(randomType).tryMutate(0.5);
        }
    }

    private String generateMutantName(long day) {
        String[] prefixes = {"Proto", "Neo", "Hyper", "Ultra", "Crypto", "Xeno", "Meta", "Omega"};
        String[] suffixes = {"morphus", "rex", "vex", "flux", "form", "oid", "kin", "born"};
        Random random = new Random(day + totalMutations);
        return prefixes[random.nextInt(prefixes.length)] + suffixes[random.nextInt(suffixes.length)];
    }

    /**
     * Retourne les types de gènes pertinents selon l'espèce.
     */
    private static Gene.GeneType[] getGenesForSpecies(String entityType) {
        return switch (entityType.toLowerCase()) {
            case "zombie", "skeleton", "husk", "stray" -> new Gene.GeneType[]{
                Gene.GeneType.BODY_SIZE, Gene.GeneType.COLOR_HUE, Gene.GeneType.COLOR_SATURATION,
                Gene.GeneType.SPEED, Gene.GeneType.STRENGTH, Gene.GeneType.HEALTH_MAX,
                Gene.GeneType.AGGRESSION, Gene.GeneType.NOCTURNAL, Gene.GeneType.VENOMOUS,
                Gene.GeneType.FIRE_RESISTANCE, Gene.GeneType.REGENERATION, Gene.GeneType.BIOLUMINESCENCE,
                Gene.GeneType.APPENDAGE_COUNT, Gene.GeneType.EYE_TYPE, Gene.GeneType.SKIN_TEXTURE,
                Gene.GeneType.PACK_BEHAVIOR, Gene.GeneType.INTELLIGENCE
            };
            case "creeper" -> new Gene.GeneType[]{
                Gene.GeneType.BODY_SIZE, Gene.GeneType.COLOR_HUE, Gene.GeneType.COLOR_SATURATION,
                Gene.GeneType.SPEED, Gene.GeneType.STRENGTH, Gene.GeneType.STEALTH,
                Gene.GeneType.AGGRESSION, Gene.GeneType.BIOLUMINESCENCE, Gene.GeneType.EXPLOSION_POWER,
                Gene.GeneType.FIRE_RESISTANCE, Gene.GeneType.PATTERN_TYPE
            };
            case "spider", "cave_spider" -> new Gene.GeneType[]{
                Gene.GeneType.BODY_SIZE, Gene.GeneType.COLOR_HUE, Gene.GeneType.COLOR_SATURATION,
                Gene.GeneType.SPEED, Gene.GeneType.CLIMBING, Gene.GeneType.VENOMOUS,
                Gene.GeneType.AGGRESSION, Gene.GeneType.STEALTH, Gene.GeneType.APPENDAGE_COUNT,
                Gene.GeneType.EYE_TYPE, Gene.GeneType.JUMP_HEIGHT, Gene.GeneType.INTELLIGENCE
            };
            case "cow", "pig", "sheep", "chicken" -> new Gene.GeneType[]{
                Gene.GeneType.BODY_SIZE, Gene.GeneType.COLOR_HUE, Gene.GeneType.COLOR_SATURATION,
                Gene.GeneType.SPEED, Gene.GeneType.HEALTH_MAX, Gene.GeneType.METABOLISM_RATE,
                Gene.GeneType.PACK_BEHAVIOR, Gene.GeneType.DIET_TYPE, Gene.GeneType.COLD_RESISTANCE,
                Gene.GeneType.PATTERN_TYPE, Gene.GeneType.LIMB_LENGTH, Gene.GeneType.SKIN_TEXTURE
            };
            case "wolf", "fox" -> new Gene.GeneType[]{
                Gene.GeneType.BODY_SIZE, Gene.GeneType.COLOR_HUE, Gene.GeneType.SPEED,
                Gene.GeneType.STRENGTH, Gene.GeneType.INTELLIGENCE, Gene.GeneType.PACK_BEHAVIOR,
                Gene.GeneType.AGGRESSION, Gene.GeneType.TERRITORY_SIZE, Gene.GeneType.NOCTURNAL,
                Gene.GeneType.STEALTH, Gene.GeneType.DIET_TYPE, Gene.GeneType.REGENERATION
            };
            case "oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry" -> new Gene.GeneType[]{
                Gene.GeneType.HEIGHT_MAX, Gene.GeneType.COLOR_HUE, Gene.GeneType.COLOR_SATURATION,
                Gene.GeneType.WOOD_HARDNESS, Gene.GeneType.LEAF_SHAPE, Gene.GeneType.GROWTH_SPEED,
                Gene.GeneType.ROOT_SPREAD, Gene.GeneType.PHOTOSYNTHESIS, Gene.GeneType.SEASONAL_ADAPT,
                Gene.GeneType.FRUIT_COLOR, Gene.GeneType.FRUIT_SIZE, Gene.GeneType.FRUIT_EFFECT,
                Gene.GeneType.BIOLUMINESCENCE
            };
            case "wheat", "carrot", "potato", "beetroot" -> new Gene.GeneType[]{
                Gene.GeneType.GROWTH_SPEED, Gene.GeneType.FRUIT_SIZE, Gene.GeneType.FRUIT_COLOR,
                Gene.GeneType.FRUIT_EFFECT, Gene.GeneType.PHOTOSYNTHESIS, Gene.GeneType.SEASONAL_ADAPT,
                Gene.GeneType.COLD_RESISTANCE, Gene.GeneType.ROOT_SPREAD
            };
            case "sugar_cane", "melon", "pumpkin", "cactus" -> new Gene.GeneType[]{
                Gene.GeneType.HEIGHT_MAX, Gene.GeneType.GROWTH_SPEED, Gene.GeneType.FRUIT_SIZE,
                Gene.GeneType.FRUIT_COLOR, Gene.GeneType.FRUIT_EFFECT, Gene.GeneType.PHOTOSYNTHESIS,
                Gene.GeneType.ROOT_SPREAD, Gene.GeneType.SEASONAL_ADAPT
            };
            // Profil dédié au joueur : gènes pertinents seulement, et surtout des valeurs
            // de départ proches du neutre (voir getBaseValueForSpecies) — sans ce cas
            // explicite, "player" tombait dans le `default` ci-dessous qui démarre TOUS
            // les gènes à leur valeur médiane (ex: BODY_SIZE ≈ 3.0 sur une plage 0.1-6.0),
            // ce qui rendait le joueur géant dès la connexion.
            case "player" -> new Gene.GeneType[]{
                Gene.GeneType.BODY_SIZE, Gene.GeneType.SPEED, Gene.GeneType.STRENGTH,
                Gene.GeneType.HEALTH_MAX, Gene.GeneType.DEFENSE, Gene.GeneType.JUMP_HEIGHT,
                Gene.GeneType.SWIM_SPEED, Gene.GeneType.REGENERATION, Gene.GeneType.FIRE_RESISTANCE,
                Gene.GeneType.COLD_RESISTANCE, Gene.GeneType.POISON_RESISTANCE,
                Gene.GeneType.METABOLISM_RATE, Gene.GeneType.NOCTURNAL
            };
            default -> Gene.GeneType.values();
        };
    }

    /**
     * Valeurs de base adaptées à chaque espèce pour un départ réaliste.
     *
     * Pour le joueur spécifiquement, on part de valeurs proches du "vanilla"
     * (taille ≈1, vitesse/force/vie proches des standards Minecraft) plutôt
     * que du milieu de la plage de chaque gène — sinon même un joueur "neutre"
     * démarrerait avec des stats aléatoires éloignées de la normale vanilla.
     */
    private static double getBaseValueForSpecies(String entityType, Gene.GeneType geneType) {
        if (entityType.equalsIgnoreCase("player")) {
            return switch (geneType) {
                case BODY_SIZE -> 1.0;             // taille vanilla exacte
                case SPEED -> 1.0;                 // multiplicateur neutre (voir applyGenomeMutations)
                case STRENGTH -> 1.0;               // dégâts mains nues vanilla
                case HEALTH_MAX -> 20.0;            // 10 cœurs vanilla
                case DEFENSE -> 0.0;                // pas de résistance gratuite
                case JUMP_HEIGHT -> 1.0;
                case SWIM_SPEED -> 1.0;
                case REGENERATION -> 0.0;           // pas de regen gratuite hors faim pleine
                case FIRE_RESISTANCE, COLD_RESISTANCE, POISON_RESISTANCE -> 0.0;
                case METABOLISM_RATE -> 1.0;        // faim vanilla
                case NOCTURNAL -> 0.0;               // diurne par défaut comme tout joueur
                default -> 0.0;
            };
        }

        Random rand = new Random(entityType.hashCode() + geneType.ordinal());
        double[] bounds = Gene.getValueBounds(geneType);
        double mid = (bounds[0] + bounds[1]) / 2.0;
        double variance = (bounds[1] - bounds[0]) * 0.2;
        return mid + (rand.nextDouble() - 0.5) * variance;
    }

    /**
     * Calcule le score de fitness basé sur les gènes actuels et l'environnement.
     */
    public double calculateFitness(EnvironmentData env) {
        double fitness = 1.0;

        // Adaptation à la température
        if (env.temperature < 0.2) {
            double coldRes = getGeneValue(Gene.GeneType.COLD_RESISTANCE);
            fitness *= (0.3 + coldRes * 0.7);
        }
        if (env.temperature > 0.8) {
            double fireRes = getGeneValue(Gene.GeneType.FIRE_RESISTANCE);
            fitness *= (0.3 + fireRes * 0.7);
        }

        // Pression des prédateurs
        if (env.predatorPressure > 0.5) {
            double speed = getGeneValue(Gene.GeneType.SPEED);
            double defense = getGeneValue(Gene.GeneType.DEFENSE);
            fitness *= (speed / 5.0 * 0.5 + defense * 0.5);
        }

        this.fitnessScore = fitness;
        return fitness;
    }

    public double getGeneValue(Gene.GeneType type) {
        Gene gene = genes.get(type);
        return gene != null ? gene.getValue() : 0.0;
    }

    public Gene getGene(Gene.GeneType type) {
        return genes.get(type);
    }

    public Map<Gene.GeneType, Gene> getAllGenes() {
        return Collections.unmodifiableMap(genes);
    }

    public String getSpeciesLineage() { return speciesLineage; }
    public int getTotalMutations() { return totalMutations; }
    public double getFitnessScore() { return fitnessScore; }
    public long getDayBorn() { return dayBorn; }

    // =================== NBT SERIALIZATION ===================

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Lineage", speciesLineage);
        tag.putInt("TotalMutations", totalMutations);
        tag.putLong("DayBorn", dayBorn);
        tag.putDouble("FitnessScore", fitnessScore);

        ListTag geneList = new ListTag();
        for (Gene gene : genes.values()) {
            geneList.add(gene.toNBT());
        }
        tag.put("Genes", geneList);

        return tag;
    }

    public static Genome fromNBT(CompoundTag tag) {
        String lineage = tag.getString("Lineage");
        Genome genome = new Genome(lineage.split(">")[0]);
        genome.speciesLineage = lineage;
        genome.totalMutations = tag.getInt("TotalMutations");
        genome.dayBorn = tag.getLong("DayBorn");
        genome.fitnessScore = tag.getDouble("FitnessScore");

        ListTag geneList = tag.getList("Genes", 10);
        for (int i = 0; i < geneList.size(); i++) {
            Gene gene = Gene.fromNBT(geneList.getCompound(i));
            genome.genes.put(gene.getType(), gene);
        }

        return genome;
    }

    /**
     * Données environnementales influençant la pression de sélection.
     */
    public static class EnvironmentData {
        public double temperature;
        public double humidity;
        public double predatorPressure;
        public double foodAvailability;
        public boolean isNether;
        public boolean isEnd;

        public EnvironmentData(double temp, double humid, double pred, double food) {
            this.temperature = temp;
            this.humidity = humid;
            this.predatorPressure = pred;
            this.foodAvailability = food;
        }
    }
}
