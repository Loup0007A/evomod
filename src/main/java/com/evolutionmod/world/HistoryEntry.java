package com.evolutionmod.world;

import net.minecraft.nbt.CompoundTag;

/**
 * Un événement marquant de l'histoire évolutive du monde.
 *
 * @param day     jour du monde
 * @param type    nature de l'événement
 * @param species espèce concernée (identifiant)
 * @param detail  information complémentaire (nom de lignée, gène...)
 * @param value   valeur numérique associée (divergence, nombre...)
 */
public record HistoryEntry(long day, Type type, String species, String detail, double value) {

    public enum Type {
        SPECIATION(0xFFFFB347),
        RADICAL_MUTATION(0xFFFF5B7A),
        NEW_SPECIES(0xFF6FD3FF),
        RECORD(0xFFC58BFF),
        LAB(0xFF7BE07B),
        PLAYER(0xFFFFE066);

        public final int color;

        Type(int color) {
            this.color = color;
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Day", day);
        tag.putString("Type", type.name());
        tag.putString("Species", species);
        tag.putString("Detail", detail);
        tag.putDouble("Value", value);
        return tag;
    }

    public static HistoryEntry load(CompoundTag tag) {
        Type type;
        try {
            type = Type.valueOf(tag.getString("Type"));
        } catch (IllegalArgumentException e) {
            type = Type.RECORD;
        }
        return new HistoryEntry(tag.getLong("Day"), type, tag.getString("Species"),
            tag.getString("Detail"), tag.getDouble("Value"));
    }
}
