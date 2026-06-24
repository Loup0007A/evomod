package com.evolutionmod.genetics;

import net.minecraft.nbt.CompoundTag;
import java.util.Random;
import java.util.UUID;

/**
 * Représente un gène unique dans le génome d'une entité ou plante.
 * Chaque gène encode un trait spécifique avec une valeur numérique,
 * une dominance, et une probabilité de mutation.
 */
public class Gene {

    public enum GeneType {
        // Gènes morphologiques (apparence)
        BODY_SIZE,          // Taille du corps (0.1 = minuscule, 5.0 = géant)
        COLOR_HUE,          // Teinte de couleur (0-360)
        COLOR_SATURATION,   // Saturation (0-1)
        COLOR_BRIGHTNESS,   // Luminosité (0-1)
        LIMB_LENGTH,        // Longueur des membres
        SKIN_TEXTURE,       // Type de texture (0=normal, 1=écailleux, 2=épineux, 3=lumineux)
        BIOLUMINESCENCE,    // Émission de lumière (0=non, >0=intensité)
        APPENDAGE_COUNT,    // Nombre d'appendices supplémentaires
        EYE_TYPE,           // Type d'yeux (0=normal, 1=multiple, 2=absent, 3=infrarouge)
        PATTERN_TYPE,       // Motif sur le corps (rayures, taches, etc.)

        // Gènes de capacités
        SPEED,              // Vitesse de déplacement
        STRENGTH,           // Force d'attaque
        DEFENSE,            // Résistance aux dégâts
        HEALTH_MAX,         // Points de vie maximum
        JUMP_HEIGHT,        // Hauteur de saut
        SWIM_SPEED,         // Vitesse de nage
        FLIGHT,             // Capacité de vol (0=non, 1=planement, 2=vol actif)
        CLIMBING,           // Capacité d'escalade (0-1)
        STEALTH,            // Niveau de furtivité
        REGENERATION,       // Taux de régénération PV

        // Gènes comportementaux
        AGGRESSION,         // Agressivité (0=passif, 1=neutre, 2=agressif, 3=ultra-agressif)
        PACK_BEHAVIOR,      // Comportement de meute (0=solitaire, 1=grégaire)
        NOCTURNAL,          // Activité nocturne (0=jour, 1=nuit, 2=les deux)
        INTELLIGENCE,       // IA et comportement complexe
        TERRITORY_SIZE,     // Taille du territoire défendu

        // Gènes métaboliques
        DIET_TYPE,          // Régime alimentaire (0=herbivore, 1=omnivore, 2=carnivore, 3=inorganique)
        FIRE_RESISTANCE,    // Résistance au feu
        POISON_RESISTANCE,  // Résistance au poison
        COLD_RESISTANCE,    // Résistance au froid
        METABOLISM_RATE,    // Vitesse métabolique (affecte la faim)
        VENOMOUS,           // Production de venin (0=non, >0=puissance)
        EXPLOSION_POWER,    // Puissance d'explosion (creepers et apparentés)

        // Gènes végétaux (pour plantes/arbres)
        FRUIT_COLOR,        // Couleur des fruits
        FRUIT_SIZE,         // Taille des fruits
        FRUIT_EFFECT,       // Effet des fruits quand mangés
        WOOD_HARDNESS,      // Dureté du bois
        LEAF_SHAPE,         // Forme des feuilles
        GROWTH_SPEED,       // Vitesse de croissance
        HEIGHT_MAX,         // Hauteur maximale
        ROOT_SPREAD,        // Propagation des racines
        PHOTOSYNTHESIS,     // Efficacité photosynthétique
        SEASONAL_ADAPT      // Adaptation saisonnière
    }

    private final UUID geneId;
    private final GeneType type;
    private double value;
    private double dominance;       // 0.0 à 1.0, chance d'expression
    private double mutationRate;    // Probabilité de mutation par jour
    private double mutationRange;   // Amplitude maximale de mutation
    private int generation;         // Génération depuis l'ancêtre

    private static final Random RANDOM = new Random();

    public Gene(GeneType type, double value, double dominance, double mutationRate, double mutationRange) {
        this.geneId = UUID.randomUUID();
        this.type = type;
        this.value = value;
        this.dominance = dominance;
        this.mutationRate = mutationRate;
        this.mutationRange = mutationRange;
        this.generation = 0;
    }

    /**
     * Tente de muter ce gène. Retourne true si une mutation s'est produite.
     * Les mutations peuvent être ponctuelles (petites) ou radicales (rares).
     */
    public boolean tryMutate(double environmentalPressure) {
        double effectiveMutationRate = mutationRate * (1.0 + environmentalPressure * 0.5);

        if (RANDOM.nextDouble() < effectiveMutationRate) {
            double mutationType = RANDOM.nextDouble();

            if (mutationType < 0.85) {
                // Mutation ponctuelle - petite variation gaussienne
                double delta = RANDOM.nextGaussian() * mutationRange * 0.3;
                value = clampValue(value + delta);
            } else if (mutationType < 0.97) {
                // Mutation modérée
                double delta = (RANDOM.nextDouble() * 2 - 1) * mutationRange;
                value = clampValue(value + delta);
            } else {
                // Mutation radicale - saut vers une valeur complètement différente
                value = getRadicalMutation();
            }

            // La dominance peut aussi muter légèrement
            if (RANDOM.nextDouble() < 0.1) {
                dominance = Math.max(0.0, Math.min(1.0, dominance + RANDOM.nextGaussian() * 0.05));
            }

            generation++;
            return true;
        }
        return false;
    }

    /**
     * Croise ce gène avec un autre gène du même type (reproduction).
     */
    public Gene crossWith(Gene other) {
        if (this.type != other.type) {
            throw new IllegalArgumentException("Impossible de croiser des gènes de types différents");
        }

        // Sélection basée sur la dominance
        double totalDominance = this.dominance + other.dominance;
        Gene dominant, recessive;

        if (RANDOM.nextDouble() < (this.dominance / totalDominance)) {
            dominant = this;
            recessive = other;
        } else {
            dominant = other;
            recessive = this;
        }

        // Valeur héritée : mélange pondéré par dominance
        double inheritedValue = dominant.value * 0.7 + recessive.value * 0.3;

        // Taux de mutation des enfants : légèrement supérieur à la moyenne parentale
        double childMutationRate = (this.mutationRate + other.mutationRate) / 2.0 * 1.1;

        Gene child = new Gene(
            type,
            inheritedValue,
            (dominant.dominance + recessive.dominance) / 2.0,
            Math.min(childMutationRate, 1.0),
            (this.mutationRange + other.mutationRange) / 2.0
        );
        child.generation = Math.max(this.generation, other.generation) + 1;
        return child;
    }

    private double clampValue(double val) {
        double[] bounds = getValueBounds(type);
        return Math.max(bounds[0], Math.min(bounds[1], val));
    }

    private double getRadicalMutation() {
        double[] bounds = getValueBounds(type);
        return bounds[0] + RANDOM.nextDouble() * (bounds[1] - bounds[0]);
    }

    public static double[] getValueBounds(GeneType type) {
        return switch (type) {
            case BODY_SIZE -> new double[]{0.1, 6.0};
            case COLOR_HUE -> new double[]{0, 360};
            case COLOR_SATURATION, COLOR_BRIGHTNESS -> new double[]{0, 1};
            case LIMB_LENGTH -> new double[]{0.1, 4.0};
            case SKIN_TEXTURE -> new double[]{0, 5};
            case BIOLUMINESCENCE -> new double[]{0, 15};
            case APPENDAGE_COUNT -> new double[]{0, 8};
            case EYE_TYPE -> new double[]{0, 4};
            case PATTERN_TYPE -> new double[]{0, 6};
            case SPEED -> new double[]{0.1, 5.0};
            case STRENGTH -> new double[]{0.1, 10.0};
            case DEFENSE -> new double[]{0, 1};
            case HEALTH_MAX -> new double[]{1, 500};
            case JUMP_HEIGHT -> new double[]{0.1, 5.0};
            case SWIM_SPEED -> new double[]{0, 3.0};
            case FLIGHT -> new double[]{0, 2};
            case CLIMBING -> new double[]{0, 1};
            case STEALTH -> new double[]{0, 1};
            case REGENERATION -> new double[]{0, 2.0};
            case AGGRESSION -> new double[]{0, 3};
            case PACK_BEHAVIOR -> new double[]{0, 1};
            case NOCTURNAL -> new double[]{0, 2};
            case INTELLIGENCE -> new double[]{0, 10};
            case TERRITORY_SIZE -> new double[]{0, 100};
            case DIET_TYPE -> new double[]{0, 3};
            case FIRE_RESISTANCE, POISON_RESISTANCE, COLD_RESISTANCE -> new double[]{0, 1};
            case METABOLISM_RATE -> new double[]{0.1, 5.0};
            case VENOMOUS -> new double[]{0, 10};
            case EXPLOSION_POWER -> new double[]{1, 12}; // 1=creeper vanilla, 12=cratère colossal
            case FRUIT_COLOR -> new double[]{0, 360};
            case FRUIT_SIZE -> new double[]{0.1, 5.0};
            case FRUIT_EFFECT -> new double[]{0, 20};
            case WOOD_HARDNESS -> new double[]{0, 10};
            case LEAF_SHAPE -> new double[]{0, 8};
            case GROWTH_SPEED -> new double[]{0.1, 5.0};
            case HEIGHT_MAX -> new double[]{1, 30};
            case ROOT_SPREAD -> new double[]{1, 20};
            case PHOTOSYNTHESIS -> new double[]{0, 5};
            case SEASONAL_ADAPT -> new double[]{0, 4};
            default -> new double[]{0, 1};
        };
    }

    // Sérialisation NBT
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("GeneId", geneId.toString());
        tag.putString("Type", type.name());
        tag.putDouble("Value", value);
        tag.putDouble("Dominance", dominance);
        tag.putDouble("MutationRate", mutationRate);
        tag.putDouble("MutationRange", mutationRange);
        tag.putInt("Generation", generation);
        return tag;
    }

    public static Gene fromNBT(CompoundTag tag) {
        GeneType type = GeneType.valueOf(tag.getString("Type"));
        Gene gene = new Gene(
            type,
            tag.getDouble("Value"),
            tag.getDouble("Dominance"),
            tag.getDouble("MutationRate"),
            tag.getDouble("MutationRange")
        );
        gene.generation = tag.getInt("Generation");
        return gene;
    }

    // Getters
    public GeneType getType() { return type; }
    public double getValue() { return value; }
    public double getDominance() { return dominance; }
    public double getMutationRate() { return mutationRate; }
    public int getGeneration() { return generation; }
    public UUID getGeneId() { return geneId; }

    @Override
    public String toString() {
        return String.format("Gene[%s=%.3f, dom=%.2f, mut=%.4f, gen=%d]",
            type.name(), value, dominance, mutationRate, generation);
    }
}
