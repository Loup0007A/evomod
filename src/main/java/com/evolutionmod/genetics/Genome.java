package com.evolutionmod.genetics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Génome d'un individu (mob, joueur) ou gabarit d'une espèce (population, plante).
 *
 * Il ne contient que les gènes pertinents pour son espèce : un gène absent vaut
 * toujours sa valeur neutre, ce qui garantit qu'une créature non mutée se
 * comporte exactement comme en vanilla.
 */
public class Genome {

    private final String species;
    private final EnumMap<GeneType, Double> values = new EnumMap<>(GeneType.class);
    private final EnumMap<GeneType, Integer> geneMutations = new EnumMap<>(GeneType.class);
    private String lineage;
    private int totalMutations;
    private int generation;

    public Genome(String species, String lineage) {
        this.species = species;
        this.lineage = lineage;
    }

    // ───────────────────────── Création ─────────────────────────

    public static Genome fromProfile(SpeciesProfile profile) {
        Genome g = new Genome(profile.id(), "");
        for (GeneType type : profile.genes()) {
            g.values.put(type, type.clamp(profile.baseValue(type)));
        }
        return g;
    }

    public Genome copy() {
        Genome g = new Genome(species, lineage);
        g.values.putAll(values);
        g.geneMutations.putAll(geneMutations);
        g.totalMutations = totalMutations;
        g.generation = generation;
        return g;
    }

    /**
     * Crée un individu à partir du gabarit de son espèce : chaque gène reçoit une
     * petite variation individuelle (personne n'est le clone exact de la
     * population) et, rarement, une mutation spontanée plus marquée.
     */
    public Genome spawnIndividual(RandomSource random, double variation) {
        Genome g = copy();
        g.totalMutations = 0;
        g.geneMutations.clear();
        for (GeneType type : g.values.keySet()) {
            double v = g.values.get(type) + random.nextGaussian() * type.range() * variation;
            g.values.put(type, type.clamp(v));
        }
        if (random.nextFloat() < 0.02f) {
            g.mutateRandomGene(random, 0.25);
        }
        return g;
    }

    // ───────────────────────── Mutation ─────────────────────────

    /**
     * Mutation journalière : chaque gène a une probabilité {@code chance} de varier
     * selon une loi normale ; 4 % des mutations sont « radicales » (grand saut).
     *
     * @return la liste des gènes modifiés (éventuellement vide)
     */
    public List<GeneType> mutate(RandomSource random, double chance, double sigma) {
        List<GeneType> changed = new ArrayList<>();
        for (GeneType type : List.copyOf(values.keySet())) {
            if (random.nextDouble() >= chance) continue;
            boolean radical = random.nextDouble() < 0.04;
            double amplitude = radical ? 0.25 : sigma;
            double delta = random.nextGaussian() * type.range() * amplitude;
            double old = values.get(type);
            values.put(type, type.clamp(old + delta));
            geneMutations.merge(type, 1, Integer::sum);
            totalMutations++;
            changed.add(type);
        }
        return changed;
    }

    /** Force la mutation d'un gène pris au hasard (mutagène, laboratoire...). */
    public GeneType mutateRandomGene(RandomSource random, double amplitude) {
        if (values.isEmpty()) return null;
        List<GeneType> keys = new ArrayList<>(values.keySet());
        GeneType type = keys.get(random.nextInt(keys.size()));
        double delta = random.nextGaussian() * type.range() * amplitude;
        values.put(type, type.clamp(values.get(type) + delta));
        geneMutations.merge(type, 1, Integer::sum);
        totalMutations++;
        return type;
    }

    /**
     * Croisement sexué : pour chaque gène, l'enfant hérite d'un mélange pondéré
     * aléatoirement des deux parents (dominance variable), puis peut muter.
     */
    public static Genome crossover(Genome a, Genome b, RandomSource random) {
        Genome child = new Genome(a.species, a.totalMutations >= b.totalMutations ? a.lineage : b.lineage);
        for (GeneType type : a.values.keySet()) {
            double va = a.get(type);
            double vb = b.values.containsKey(type) ? b.get(type) : va;
            double w = random.nextDouble();
            // 70 % du temps un parent domine nettement, sinon mélange équilibré
            if (random.nextDouble() < 0.7) w = w < 0.5 ? 0.15 : 0.85;
            double v;
            if (type == GeneType.HUE) {
                v = blendHue(va, vb, w);
            } else {
                v = va * w + vb * (1 - w);
            }
            child.values.put(type, type.clamp(v));
        }
        child.generation = Math.max(a.generation, b.generation) + 1;
        child.mutate(random, 0.08, 0.05);
        return child;
    }

    /**
     * Déplace ce génome (typiquement un gabarit d'espèce) vers {@code target}
     * d'une fraction {@code weight}. Un poids négatif l'en éloigne : c'est le
     * mécanisme de sélection (positive ou négative).
     */
    public void pullTowards(Genome target, double weight) {
        for (Map.Entry<GeneType, Double> e : values.entrySet()) {
            GeneType type = e.getKey();
            if (!target.values.containsKey(type)) continue;
            double cur = e.getValue();
            double tv = target.get(type);
            double next;
            if (type == GeneType.HUE) {
                next = blendHue(tv, cur, weight);
            } else {
                next = cur + (tv - cur) * weight;
            }
            e.setValue(type.clamp(next));
        }
    }

    private static double blendHue(double a, double b, double wA) {
        double diff = ((a - b + 540.0) % 360.0) - 180.0;
        return b + diff * wA;
    }

    // ───────────────────────── Analyse ─────────────────────────

    /** Divergence moyenne 0..1 par rapport à un génome de référence. */
    public double divergenceFrom(Genome reference) {
        if (values.isEmpty()) return 0;
        double total = 0;
        int count = 0;
        double pigment = Math.max(get(GeneType.PIGMENT), reference.get(GeneType.PIGMENT));
        for (Map.Entry<GeneType, Double> e : values.entrySet()) {
            GeneType type = e.getKey();
            double d = type.distance(e.getValue(), reference.get(type));
            if (type == GeneType.HUE) d *= pigment; // une teinte invisible ne compte pas
            total += d;
            count++;
        }
        return count == 0 ? 0 : total / count;
    }

    /** Gène le plus éloigné de son neutre (trait le plus « marquant »). */
    public GeneType dominantTrait() {
        GeneType best = null;
        double bestScore = 0.08;
        for (Map.Entry<GeneType, Double> e : values.entrySet()) {
            GeneType t = e.getKey();
            if (t == GeneType.HUE) continue;
            double s = t.distance(e.getValue(), t.neutral);
            if (s > bestScore) {
                bestScore = s;
                best = t;
            }
        }
        return best;
    }

    /** Couleur ARGB héritée des gènes HUE/PIGMENT (alpha = intensité). */
    public int tintColor() {
        double pigment = get(GeneType.PIGMENT);
        float hue = (float) (get(GeneType.HUE) / 360.0);
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.85f, 1.0f) & 0xFFFFFF;
        int alpha = (int) Math.round(Math.min(1.0, pigment) * 140.0);
        return (alpha << 24) | rgb;
    }

    // ───────────────────────── Accès ─────────────────────────

    public double get(GeneType type) {
        Double v = values.get(type);
        return v != null ? v : type.neutral;
    }

    public void set(GeneType type, double value) {
        values.put(type, type.clamp(value));
    }

    public boolean has(GeneType type) {
        return values.containsKey(type);
    }

    public Set<GeneType> genes() {
        return Collections.unmodifiableSet(values.keySet());
    }

    public int mutationsOf(GeneType type) {
        return geneMutations.getOrDefault(type, 0);
    }

    public String species() {
        return species;
    }

    public String lineage() {
        return lineage;
    }

    public void setLineage(String lineage) {
        this.lineage = lineage;
    }

    public int totalMutations() {
        return totalMutations;
    }

    public int generation() {
        return generation;
    }

    public void setGeneration(int generation) {
        this.generation = generation;
    }

    // ───────────────────────── NBT ─────────────────────────

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Species", species);
        tag.putString("Lineage", lineage == null ? "" : lineage);
        tag.putInt("Mutations", totalMutations);
        tag.putInt("Generation", generation);
        CompoundTag genes = new CompoundTag();
        CompoundTag counts = new CompoundTag();
        for (Map.Entry<GeneType, Double> e : values.entrySet()) {
            genes.putDouble(e.getKey().name(), e.getValue());
            int c = geneMutations.getOrDefault(e.getKey(), 0);
            if (c > 0) counts.putInt(e.getKey().name(), c);
        }
        tag.put("Genes", genes);
        tag.put("GeneMutations", counts);
        return tag;
    }

    public static Genome load(CompoundTag tag) {
        Genome g = new Genome(tag.getString("Species"), tag.getString("Lineage"));
        g.totalMutations = tag.getInt("Mutations");
        g.generation = tag.getInt("Generation");
        CompoundTag genes = tag.getCompound("Genes");
        for (String key : genes.getAllKeys()) {
            GeneType type = GeneType.byName(key);
            if (type != null) g.values.put(type, type.clamp(genes.getDouble(key)));
        }
        CompoundTag counts = tag.getCompound("GeneMutations");
        for (String key : counts.getAllKeys()) {
            GeneType type = GeneType.byName(key);
            if (type != null) g.geneMutations.put(type, counts.getInt(key));
        }
        return g;
    }

    /** Vrai si le tag ressemble à un génome sauvegardé par cette version du mod. */
    public static boolean isValid(CompoundTag tag) {
        return tag != null && tag.contains("Species") && tag.contains("Genes");
    }
}
