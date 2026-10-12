package com.evolutionmod.world;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.genetics.SpeciesProfile;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Toutes les données évolutives globales d'un monde, sauvegardées dans
 * {@code <monde>/data/evolutionmod.dat}. Il n'y a plus aucun état statique :
 * chaque monde (et chaque serveur) a ses propres espèces et son histoire.
 */
public class EvolutionSavedData extends SavedData {

    public static final String NAME = EvolutionMod.MOD_ID;
    public static final int MAX_HISTORY = 120;

    private long lastProcessedDay = -1;
    private long cycles;
    private long totalMutations;
    private int speciations;
    private long seed = System.nanoTime();
    private final Map<String, SpeciesRecord> species = new LinkedHashMap<>();
    private final List<HistoryEntry> history = new ArrayList<>();
    private final List<Float> pressureHistory = new ArrayList<>();

    public EvolutionSavedData() {}

    public static EvolutionSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(EvolutionSavedData::new, EvolutionSavedData::load, null), NAME);
    }

    // ───────────────────────── Espèces ─────────────────────────

    public SpeciesRecord getOrCreate(SpeciesProfile profile, long day) {
        SpeciesRecord r = species.get(profile.id());
        if (r == null) {
            r = new SpeciesRecord(profile, day);
            species.put(profile.id(), r);
            setDirty();
        }
        return r;
    }

    public SpeciesRecord get(String id) {
        return species.get(id);
    }

    public Map<String, SpeciesRecord> species() {
        return Collections.unmodifiableMap(species);
    }

    // ───────────────────────── Histoire ─────────────────────────

    public void addHistory(HistoryEntry entry) {
        history.add(entry);
        while (history.size() > MAX_HISTORY) history.remove(0);
        setDirty();
    }

    public List<HistoryEntry> history() {
        return Collections.unmodifiableList(history);
    }

    public void recordPressure(double pressure) {
        pressureHistory.add((float) pressure);
        while (pressureHistory.size() > SpeciesRecord.HISTORY_LENGTH) pressureHistory.remove(0);
    }

    public List<Float> pressureHistory() {
        return pressureHistory;
    }

    // ───────────────────────── Compteurs ─────────────────────────

    public long lastProcessedDay() { return lastProcessedDay; }
    public void setLastProcessedDay(long day) { this.lastProcessedDay = day; setDirty(); }
    public long cycles() { return cycles; }
    public void addCycle() { cycles++; setDirty(); }
    public long totalMutations() { return totalMutations; }
    public void addMutations(long n) { totalMutations += n; setDirty(); }
    public int speciations() { return speciations; }
    public void addSpeciation() { speciations++; setDirty(); }
    public long seed() { return seed; }

    public void reset() {
        species.clear();
        history.clear();
        pressureHistory.clear();
        cycles = 0;
        totalMutations = 0;
        speciations = 0;
        seed = System.nanoTime();
        setDirty();
    }

    // ───────────────────────── NBT ─────────────────────────

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Version", 2);
        tag.putLong("LastProcessedDay", lastProcessedDay);
        tag.putLong("Cycles", cycles);
        tag.putLong("TotalMutations", totalMutations);
        tag.putInt("Speciations", speciations);
        tag.putLong("Seed", seed);

        ListTag list = new ListTag();
        for (SpeciesRecord r : species.values()) list.add(r.save());
        tag.put("Species", list);

        ListTag hist = new ListTag();
        for (HistoryEntry e : history) hist.add(e.save());
        tag.put("History", hist);

        int[] p = new int[pressureHistory.size()];
        for (int i = 0; i < p.length; i++) p[i] = Float.floatToIntBits(pressureHistory.get(i));
        tag.putIntArray("PressureHistory", p);
        return tag;
    }

    public static EvolutionSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        EvolutionSavedData data = new EvolutionSavedData();
        if (tag.getInt("Version") < 2) {
            // Ancien format (v1.0.0) incompatible : on repart proprement.
            EvolutionMod.LOGGER.info("Anciennes données d'évolution ignorées (format obsolète).");
            return data;
        }
        data.lastProcessedDay = tag.getLong("LastProcessedDay");
        data.cycles = tag.getLong("Cycles");
        data.totalMutations = tag.getLong("TotalMutations");
        data.speciations = tag.getInt("Speciations");
        data.seed = tag.getLong("Seed");

        ListTag list = tag.getList("Species", 10);
        for (int i = 0; i < list.size(); i++) {
            try {
                SpeciesRecord r = SpeciesRecord.load(list.getCompound(i));
                data.species.put(r.id(), r);
            } catch (Exception e) {
                EvolutionMod.LOGGER.warn("Espèce illisible ignorée : {}", e.toString());
            }
        }
        ListTag hist = tag.getList("History", 10);
        for (int i = 0; i < hist.size(); i++) data.history.add(HistoryEntry.load(hist.getCompound(i)));
        for (int bits : tag.getIntArray("PressureHistory")) data.pressureHistory.add(Float.intBitsToFloat(bits));
        return data;
    }
}
