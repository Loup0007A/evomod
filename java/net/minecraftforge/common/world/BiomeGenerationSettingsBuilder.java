package net.minecraftforge.common.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class BiomeGenerationSettingsBuilder extends BiomeGenerationSettings.PlainBuilder {
   public BiomeGenerationSettingsBuilder(BiomeGenerationSettings orig) {
      for (GenerationStep.Carving k : orig.getCarvingStages()) {
         this.carvers.put(k, new ArrayList<>());

         for (Holder<ConfiguredWorldCarver<?>> v : orig.getCarvers(k)) {
            this.carvers.get(k).add(v);
         }
      }

      orig.features().forEach(l -> {
         ArrayList<Holder<PlacedFeature>> featureList = new ArrayList<>();
         l.forEach(featureList::add);
         this.features.add(featureList);
      });
   }

   public List<Holder<PlacedFeature>> getFeatures(GenerationStep.Decoration stage) {
      this.addFeatureStepsUpTo(stage.ordinal());
      return this.features.get(stage.ordinal());
   }

   public List<Holder<ConfiguredWorldCarver<?>>> getCarvers(GenerationStep.Carving stage) {
      return this.carvers.computeIfAbsent(stage, key -> new ArrayList<>());
   }
}
