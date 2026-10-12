package com.evolutionmod.core;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.genetics.Genome;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Données génétiques attachées à chaque entité vivante.
 *
 * Le génome est créé paresseusement côté serveur (à l'arrivée de l'entité dans
 * le monde) et uniquement pour les espèces qui évoluent. Côté client, il est
 * rempli par synchronisation réseau.
 */
@Mod.EventBusSubscriber(modid = EvolutionMod.MOD_ID)
public class EvolutionCapability implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<EvolutionData> CAP = CapabilityManager.get(new CapabilityToken<>() {});
    private static final ResourceLocation ID = EvolutionMod.id("genetics");

    private final EvolutionData data = new EvolutionData();
    private final LazyOptional<EvolutionData> optional = LazyOptional.of(() -> data);

    @SubscribeEvent
    public static void onAttach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof LivingEntity) {
            EvolutionCapability provider = new EvolutionCapability();
            event.addCapability(ID, provider);
            event.addListener(provider.optional::invalidate);
        }
    }

    /** Conserve le génome du joueur après la mort ou le retour de l'End. */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        get(original).ifPresent(old -> get(event.getEntity()).ifPresent(neu -> {
            if (old.genome() != null) neu.setGenome(old.genome().copy());
            neu.setLastCycle(old.lastCycle());
            neu.setBornDay(old.bornDay());
        }));
        original.invalidateCaps();
    }

    public static LazyOptional<EvolutionData> get(Entity entity) {
        return entity.getCapability(CAP);
    }

    /** Génome de l'entité ou {@code null} s'il n'en a pas (encore). */
    @Nullable
    public static Genome genome(Entity entity) {
        return get(entity).resolve().map(EvolutionData::genome).orElse(null);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return CAP.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        return data.save();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag nbt) {
        data.load(nbt);
    }

    // ───────────────────────── Données ─────────────────────────

    @AutoRegisterCapability
    public static class EvolutionData {
        private Genome genome;
        private long lastCycle = -1;
        private long bornDay = -1;
        private boolean legacy;

        @Nullable
        public Genome genome() { return genome; }
        public void setGenome(@Nullable Genome genome) { this.genome = genome; }
        public long lastCycle() { return lastCycle; }
        public void setLastCycle(long day) { this.lastCycle = day; }
        public long bornDay() { return bornDay; }
        public void setBornDay(long day) { this.bornDay = day; }
        /** Vrai si l'entité a été modifiée par l'ancienne version 1.0 du mod (attributs à réparer). */
        public boolean isLegacy() { return legacy; }
        public void clearLegacy() { this.legacy = false; }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            if (genome != null) tag.put("Genome", genome.save());
            tag.putLong("LastCycle", lastCycle);
            tag.putLong("Born", bornDay);
            return tag;
        }

        public void load(CompoundTag tag) {
            CompoundTag g = tag.getCompound("Genome");
            genome = Genome.isValid(g) ? Genome.load(g) : null; // les génomes v1.0 sont régénérés
            lastCycle = tag.contains("LastCycle") ? tag.getLong("LastCycle") : -1;
            bornDay = tag.contains("Born") ? tag.getLong("Born") : -1;
            legacy = tag.contains("LastEvolutionDay");
        }
    }
}
