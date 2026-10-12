package com.evolutionmod.brew;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Mixtures en cours dans les chaudrons d'un monde (une entrée par chaudron). */
public class BrewSavedData extends SavedData {

    public static final String NAME = "evolutionmod_brews";

    /** Une mixture : ingrédients, minuterie et seuil d'explosion. */
    public static class Brew {
        public final List<String> items = new ArrayList<>();
        public long readyAt;
        public int capacity;
        public long seed;
        public boolean announced;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            ListTag l = new ListTag();
            for (String s : items) l.add(StringTag.valueOf(s));
            t.put("Items", l);
            t.putLong("ReadyAt", readyAt);
            t.putInt("Capacity", capacity);
            t.putLong("Seed", seed);
            t.putBoolean("Announced", announced);
            return t;
        }

        static Brew load(CompoundTag t) {
            Brew b = new Brew();
            ListTag l = t.getList("Items", 8);
            for (int i = 0; i < l.size(); i++) b.items.add(l.getString(i));
            b.readyAt = t.getLong("ReadyAt");
            b.capacity = t.getInt("Capacity");
            b.seed = t.getLong("Seed");
            b.announced = t.getBoolean("Announced");
            return b;
        }
    }

    private final Map<Long, Brew> brews = new HashMap<>();

    public static BrewSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(BrewSavedData::new, BrewSavedData::load, null), NAME);
    }

    public Brew get(BlockPos pos) {
        return brews.get(pos.asLong());
    }

    public void put(BlockPos pos, Brew brew) {
        brews.put(pos.asLong(), brew);
        setDirty();
    }

    public void remove(BlockPos pos) {
        if (brews.remove(pos.asLong()) != null) setDirty();
    }

    public Map<Long, Brew> all() {
        return brews;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, Brew> e : brews.entrySet()) {
            CompoundTag t = e.getValue().save();
            t.putLong("Pos", e.getKey());
            list.add(t);
        }
        tag.put("Brews", list);
        return tag;
    }

    public static BrewSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        BrewSavedData d = new BrewSavedData();
        ListTag list = tag.getList("Brews", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            d.brews.put(t.getLong("Pos"), Brew.load(t));
        }
        return d;
    }
}
