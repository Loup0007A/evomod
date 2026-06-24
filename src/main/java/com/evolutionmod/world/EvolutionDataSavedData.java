package com.evolutionmod.world;

import com.evolutionmod.EvolutionMod;
import com.evolutionmod.core.EvolutionManager;
import com.evolutionmod.genetics.Genome;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Sauvegarde persistante des données d'évolution mondiales.
 * Stocke dans world/data/evolutionmod_data.dat
 *
 * Signatures cibles pour Forge/NeoForge 1.21.1 :
 *   save(CompoundTag, HolderLookup.Provider)   ← abstract depuis 1.20.5
 *   load(CompoundTag, HolderLookup.Provider)   ← static factory pour computeIfAbsent
 *
 * HolderLookup.Provider n'est pas utilisé ici (pas de Codecs ni de registres
 * dynamiques dans notre NBT simple), mais il doit apparaître dans la signature
 * pour matcher l'abstraction de la classe parente.
 */
public class EvolutionDataSavedData extends SavedData {

    public static final String DATA_NAME = EvolutionMod.MOD_ID + "_data";

    public EvolutionDataSavedData() {}

    // ─────────────────────────────────────────────────
    //  Factory statique (signature 1.21.1)
    // ─────────────────────────────────────────────────

    public static EvolutionDataSavedData getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(
                EvolutionDataSavedData::new,
                EvolutionDataSavedData::load,
                null   // DataFixTypes — null = pas de DFU upgrade nécessaire
            ),
            DATA_NAME
        );
    }

    /** Charge depuis NBT — appelé automatiquement par Factory si la sauvegarde existe. */
    public static EvolutionDataSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        EvolutionDataSavedData data = new EvolutionDataSavedData();
        EvolutionManager mgr = EvolutionManager.getInstance();

        if (tag.contains("SpeciesTemplates")) {
            ListTag speciesList = tag.getList("SpeciesTemplates", 10); // 10 = CompoundTag
            for (int i = 0; i < speciesList.size(); i++) {
                CompoundTag speciesTag = speciesList.getCompound(i);
                String speciesName = speciesTag.getString("SpeciesName");
                Genome genome = Genome.fromNBT(speciesTag.getCompound("Genome"));
                // Injecter dans le manager via un setter dédié qu'on va ajouter
                mgr.injectSpeciesTemplate(speciesName, genome);
            }
        }

        EvolutionMod.LOGGER.info("Données d'évolution chargées. Jour: {}", tag.getLong("CurrentDay"));
        return data;
    }

    // ─────────────────────────────────────────────────
    //  Override save (signature 1.21.1 : deux paramètres)
    // ─────────────────────────────────────────────────

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        EvolutionManager mgr = EvolutionManager.getInstance();

        tag.putLong("CurrentDay",        mgr.getCurrentDay());
        tag.putLong("TotalMutations",    mgr.getTotalMutationsWorldwide());
        tag.putInt( "SpeciationEvents",  mgr.getSpeciationEvents());

        ListTag speciesList = new ListTag();
        for (Map.Entry<String, Genome> entry : mgr.getSpeciesTemplates().entrySet()) {
            CompoundTag st = new CompoundTag();
            st.putString("SpeciesName", entry.getKey());
            st.put("Genome", entry.getValue().toNBT());
            speciesList.add(st);
        }
        tag.put("SpeciesTemplates", speciesList);

        EvolutionMod.LOGGER.debug("Données sauvegardées : {} espèces.", speciesList.size());
        return tag;
    }

    // ─────────────────────────────────────────────────
    //  Déclencheur de sauvegarde (appelé depuis DayChangeEventHandler)
    // ─────────────────────────────────────────────────

    public static void onWorldSave(ServerLevel level) {
        if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            getOrCreate(level).setDirty();
        }
    }
}
