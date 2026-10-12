package com.evolutionmod.world;

import com.evolutionmod.genetics.Genome;
import com.evolutionmod.genetics.SpeciesProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.ArrayList;
import java.util.List;

/**
 * État évolutif d'une espèce entière (population).
 *
 * Le {@code template} représente le génome « moyen » de la population : les
 * nouveaux individus en sont issus, et la sélection naturelle le déplace vers
 * les individus qui réussissent (ou l'éloigne de ceux qui échouent).
 * L'{@code origin} reste figé : c'est l'ancêtre vanilla qui sert à mesurer la
 * divergence et à déclencher les spéciations.
 */
public class SpeciesRecord {

    public static final int HISTORY_LENGTH = 40;
    public static final double SPECIATION_STEP = 0.07;

    private final String id;
    private final SpeciesProfile.Kind kind;
    private final String root;
    private Genome template;
    private Genome origin;
    private final List<String> lineages = new ArrayList<>();
    private final List<Float> divergenceHistory = new ArrayList<>();

    private long firstSeenDay;
    private int population;
    private long births;
    private long deaths;
    private long killedByPlayers;
    private long successes;

    public SpeciesRecord(SpeciesProfile profile, long day) {
        this.id = profile.id();
        this.kind = profile.kind();
        this.root = profile.baseName();
        this.template = Genome.fromProfile(profile);
        this.origin = template.copy();
        this.firstSeenDay = day;
    }

    private SpeciesRecord(String id, SpeciesProfile.Kind kind, String root) {
        this.id = id;
        this.kind = kind;
        this.root = root;
    }

    public double divergence() {
        return template.divergenceFrom(origin);
    }

    /** Niveau de spéciation attendu pour la divergence actuelle. */
    public int targetLevel() {
        return (int) Math.floor(divergence() / SPECIATION_STEP);
    }

    public String currentLineage() {
        return lineages.isEmpty() ? "" : lineages.get(lineages.size() - 1);
    }

    /** Ajoute une nouvelle lignée et la renvoie. */
    public String speciate(long seed) {
        String name = SpeciesProfile.lineageName(id, root, lineages.size() + 1, seed);
        lineages.add(name);
        template.setLineage(name);
        return name;
    }

    public void recordDivergenceSample() {
        divergenceHistory.add((float) divergence());
        while (divergenceHistory.size() > HISTORY_LENGTH) divergenceHistory.remove(0);
    }

    // ───────────────────────── Statistiques ─────────────────────────

    public void addBirth() { births++; }
    public void addDeath(boolean byPlayer) {
        deaths++;
        if (byPlayer) killedByPlayers++;
    }
    public void addSuccess() { successes++; }
    public void setPopulation(int population) { this.population = population; }

    public String id() { return id; }
    public SpeciesProfile.Kind kind() { return kind; }
    public Genome template() { return template; }
    public Genome origin() { return origin; }
    public List<String> lineages() { return lineages; }
    public List<Float> divergenceHistory() { return divergenceHistory; }
    public long firstSeenDay() { return firstSeenDay; }
    public int population() { return population; }
    public long births() { return births; }
    public long deaths() { return deaths; }
    public long killedByPlayers() { return killedByPlayers; }
    public long successes() { return successes; }

    // ───────────────────────── NBT ─────────────────────────

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        tag.putString("Kind", kind.name());
        tag.putString("Root", root);
        tag.put("Template", template.save());
        tag.put("Origin", origin.save());
        ListTag names = new ListTag();
        for (String n : lineages) names.add(StringTag.valueOf(n));
        tag.put("Lineages", names);
        int[] hist = new int[divergenceHistory.size()];
        for (int i = 0; i < hist.length; i++) hist[i] = Float.floatToIntBits(divergenceHistory.get(i));
        tag.putIntArray("History", hist);
        tag.putLong("FirstSeen", firstSeenDay);
        tag.putInt("Population", population);
        tag.putLong("Births", births);
        tag.putLong("Deaths", deaths);
        tag.putLong("Killed", killedByPlayers);
        tag.putLong("Successes", successes);
        return tag;
    }

    public static SpeciesRecord load(CompoundTag tag) {
        SpeciesProfile.Kind kind;
        try {
            kind = SpeciesProfile.Kind.valueOf(tag.getString("Kind"));
        } catch (IllegalArgumentException e) {
            kind = SpeciesProfile.kindOf(tag.getString("Id"));
        }
        SpeciesRecord r = new SpeciesRecord(tag.getString("Id"), kind, tag.getString("Root"));
        r.template = Genome.load(tag.getCompound("Template"));
        r.origin = Genome.load(tag.getCompound("Origin"));
        ListTag names = tag.getList("Lineages", 8);
        for (int i = 0; i < names.size(); i++) r.lineages.add(names.getString(i));
        for (int bits : tag.getIntArray("History")) r.divergenceHistory.add(Float.intBitsToFloat(bits));
        r.firstSeenDay = tag.getLong("FirstSeen");
        r.population = tag.getInt("Population");
        r.births = tag.getLong("Births");
        r.deaths = tag.getLong("Deaths");
        r.killedByPlayers = tag.getLong("Killed");
        r.successes = tag.getLong("Successes");
        return r;
    }

    /** Résumé envoyé aux clients pour les écrans (sans données inutiles). */
    public CompoundTag toClientTag() {
        CompoundTag tag = save();
        tag.putDouble("Divergence", divergence());
        return tag;
    }
}
