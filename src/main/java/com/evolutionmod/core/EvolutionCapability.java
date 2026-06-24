package com.evolutionmod.core;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.genetics.Genome;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Capability Forge qui attache un génome évolutif à chaque entité vivante.
 * C'est le cœur du système - sans ça, pas d'évolution.
 *
 * NOTE 1.21.1 (vérifié directement dans le code source Forge, branche 1.21.1) :
 * INBTSerializable<T> n'a QUE la signature à deux paramètres :
 *   T serializeNBT(HolderLookup.Provider)
 *   void deserializeNBT(HolderLookup.Provider, T)
 * Il n'existe PAS de version historique à un seul paramètre à overrider
 * en plus — mon hypothèse précédente (contourner via le bug Forge #9998)
 * était incorrecte pour cette release précise. HolderLookup.Provider n'est
 * pas utilisé dans le corps des méthodes ici (pas de Codecs/registres
 * dynamiques dans notre NBT), mais doit apparaître dans la signature pour
 * matcher l'interface.
 */
@Mod.EventBusSubscriber(modid = EvolutionMod.MOD_ID)
public class EvolutionCapability implements ICapabilityProvider,
        INBTSerializable<CompoundTag> {

    public static final Capability<IEvolutionData> EVOLUTION_CAP = CapabilityManager.get(
        new CapabilityToken<>() {}
    );

    private static final ResourceLocation CAP_ID =
        ResourceLocation.fromNamespaceAndPath(EvolutionMod.MOD_ID, "evolution_data");

    private final IEvolutionData data;
    private final LazyOptional<IEvolutionData> optional;

    public EvolutionCapability(LivingEntity entity) {
        String entityType = entity.getType().getDescriptionId()
            .replace("entity.minecraft.", "")
            .replace("entity." + EvolutionMod.MOD_ID + ".", "");
        this.data = new EvolutionData(Genome.createForSpecies(entityType));
        this.optional = LazyOptional.of(() -> this.data);
    }

    public static void register() {
        RegisterCapabilitiesEvent.class.getName(); // Force-load
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IEvolutionData.class);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event) {
        if (event.getObject() instanceof LivingEntity livingEntity) {
            EvolutionCapability cap = new EvolutionCapability(livingEntity);
            event.addCapability(CAP_ID, cap);
        }
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return EVOLUTION_CAP.orEmpty(cap, optional);
    }

    // ───── Sérialisation : signature réelle 1.21.1 (deux paramètres) ─────

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (data.getGenome() != null) {
            tag.put("Genome", data.getGenome().toNBT());
        }
        tag.putLong("LastEvolutionDay", data.getLastEvolutionDay());
        tag.putInt("GenerationNumber", data.getGenerationNumber());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag nbt) {
        if (nbt.contains("Genome")) {
            data.setGenome(Genome.fromNBT(nbt.getCompound("Genome")));
        }
        data.setLastEvolutionDay(nbt.getLong("LastEvolutionDay"));
        data.setGenerationNumber(nbt.getInt("GenerationNumber"));
    }

    /**
     * Helper pour récupérer la capability d'une entité.
     */
    public static LazyOptional<IEvolutionData> getEvolutionData(LivingEntity entity) {
        return entity.getCapability(EVOLUTION_CAP);
    }

    // =================== Interface ===================

    public interface IEvolutionData {
        Genome getGenome();
        void setGenome(Genome genome);
        long getLastEvolutionDay();
        void setLastEvolutionDay(long day);
        int getGenerationNumber();
        void setGenerationNumber(int gen);
        boolean hasEvolvedToday(long currentDay);
    }

    // =================== Implémentation ===================

    public static class EvolutionData implements IEvolutionData {
        private Genome genome;
        private long lastEvolutionDay = -1;
        private int generationNumber = 0;

        public EvolutionData(Genome genome) {
            this.genome = genome;
        }

        @Override public Genome getGenome() { return genome; }
        @Override public void setGenome(Genome g) { this.genome = g; }
        @Override public long getLastEvolutionDay() { return lastEvolutionDay; }
        @Override public void setLastEvolutionDay(long day) { this.lastEvolutionDay = day; }
        @Override public int getGenerationNumber() { return generationNumber; }
        @Override public void setGenerationNumber(int gen) { this.generationNumber = gen; }

        @Override
        public boolean hasEvolvedToday(long currentDay) {
            return lastEvolutionDay >= currentDay;
        }
    }
}
