package com.evolutionmod.network;

import com.evolutionmod.client.EvolutionStatusScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Transporte la liste complète des espèces évolvables (plantes + mobs +
 * joueur) vers le client pour affichage dans l'écran /evolution status.
 *
 * Chaque entrée associe un nom d'espèce à son génome NBT sérialisé — même
 * format que {@link Genome#toNBT()} utilisé partout ailleurs dans le mod,
 * donc directement réutilisable côté client sans nouveau code de parsing.
 */
public class EvolutionStatusPacket {

    private final Map<String, CompoundTag> speciesGenomes;
    private final long currentDay;
    private final long totalMutations;
    private final int speciationEvents;
    private final double environmentalPressure;

    public EvolutionStatusPacket(Map<String, CompoundTag> speciesGenomes, long currentDay,
                                  long totalMutations, int speciationEvents, double pressure) {
        this.speciesGenomes = speciesGenomes;
        this.currentDay = currentDay;
        this.totalMutations = totalMutations;
        this.speciationEvents = speciationEvents;
        this.environmentalPressure = pressure;
    }

    public static void encode(EvolutionStatusPacket packet, FriendlyByteBuf buf) {
        buf.writeLong(packet.currentDay);
        buf.writeLong(packet.totalMutations);
        buf.writeInt(packet.speciationEvents);
        buf.writeDouble(packet.environmentalPressure);

        buf.writeInt(packet.speciesGenomes.size());
        for (Map.Entry<String, CompoundTag> entry : packet.speciesGenomes.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeNbt(entry.getValue());
        }
    }

    public static EvolutionStatusPacket decode(FriendlyByteBuf buf) {
        long day = buf.readLong();
        long totalMut = buf.readLong();
        int speciations = buf.readInt();
        double pressure = buf.readDouble();

        int count = buf.readInt();
        Map<String, CompoundTag> map = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String name = buf.readUtf();
            CompoundTag tag = buf.readNbt();
            map.put(name, tag);
        }

        return new EvolutionStatusPacket(map, day, totalMut, speciations, pressure);
    }

    public static void handle(EvolutionStatusPacket packet, CustomPayloadEvent.Context context) {
        // Déjà sur le thread principal (consumerMainThread) : on peut ouvrir
        // un Screen directement, ce qui n'est permis que sur ce thread.
        Minecraft.getInstance().setScreen(new EvolutionStatusScreen(packet));
    }

    // ── Accesseurs pour le Screen ──────────────────────────────────────
    public Map<String, CompoundTag> getSpeciesGenomes() { return speciesGenomes; }
    public long getCurrentDay() { return currentDay; }
    public long getTotalMutations() { return totalMutations; }
    public int getSpeciationEvents() { return speciationEvents; }
    public double getEnvironmentalPressure() { return environmentalPressure; }
}
