package net.minecraft.world.level.levelgen.flat;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.LayerConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.slf4j.Logger;

public class FlatLevelGeneratorSettings {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<FlatLevelGeneratorSettings> CODEC = RecordCodecBuilder.create(
         p_209800_ -> p_209800_.group(
                  RegistryCodecs.homogeneousList(Registries.STRUCTURE_SET)
                     .lenientOptionalFieldOf("structure_overrides")
                     .forGetter(p_209812_ -> p_209812_.structureOverrides),
                  FlatLayerInfo.CODEC.listOf().fieldOf("layers").forGetter(FlatLevelGeneratorSettings::getLayersInfo),
                  Codec.BOOL.fieldOf("lakes").orElse(false).forGetter(p_161912_ -> p_161912_.addLakes),
                  Codec.BOOL.fieldOf("features").orElse(false).forGetter(p_209809_ -> p_209809_.decoration),
                  Biome.CODEC.lenientOptionalFieldOf("biome").orElseGet(Optional::empty).forGetter(p_209807_ -> Optional.of(p_209807_.biome)),
                  RegistryOps.retrieveElement(Biomes.PLAINS),
                  RegistryOps.retrieveElement(MiscOverworldPlacements.LAKE_LAVA_UNDERGROUND),
                  RegistryOps.retrieveElement(MiscOverworldPlacements.LAKE_LAVA_SURFACE)
               )
               .apply(p_209800_, FlatLevelGeneratorSettings::new)
      )
      .comapFlatMap(FlatLevelGeneratorSettings::validateHeight, Function.identity())
      .stable();
   private final Optional<HolderSet<StructureSet>> structureOverrides;
   private final List<FlatLayerInfo> layersInfo = Lists.newArrayList();
   private final Holder<Biome> biome;
   private final List<BlockState> layers;
   private boolean voidGen;
   private boolean decoration;
   private boolean addLakes;
   private final List<Holder<PlacedFeature>> lakes;

   private static DataResult<FlatLevelGeneratorSettings> validateHeight(FlatLevelGeneratorSettings p_161906_) {
      int $$1 = p_161906_.layersInfo.stream().mapToInt(FlatLayerInfo::getHeight).sum();
      return $$1 > DimensionType.Y_SIZE
         ? DataResult.error(() -> "Sum of layer heights is > " + DimensionType.Y_SIZE, p_161906_)
         : DataResult.success(p_161906_);
   }

   private FlatLevelGeneratorSettings(
      Optional<HolderSet<StructureSet>> p_256456_,
      List<FlatLayerInfo> p_255826_,
      boolean p_255740_,
      boolean p_255726_,
      Optional<Holder<Biome>> p_256292_,
      Holder.Reference<Biome> p_255964_,
      Holder<PlacedFeature> p_256419_,
      Holder<PlacedFeature> p_255710_
   ) {
      this(p_256456_, getBiome(p_256292_, p_255964_), List.of(p_256419_, p_255710_));
      if (p_255740_) {
         this.setAddLakes();
      }

      if (p_255726_) {
         this.setDecoration();
      }

      this.layersInfo.addAll(p_255826_);
      this.updateLayers();
   }

   private static Holder<Biome> getBiome(Optional<? extends Holder<Biome>> pBiome, Holder<Biome> pDefaultBiome) {
      if (pBiome.isEmpty()) {
         LOGGER.error("Unknown biome, defaulting to plains");
         return pDefaultBiome;
      } else {
         return (Holder<Biome>)pBiome.get();
      }
   }

   public FlatLevelGeneratorSettings(Optional<HolderSet<StructureSet>> pStructureOverrides, Holder<Biome> pBiome, List<Holder<PlacedFeature>> pLakes) {
      this.structureOverrides = pStructureOverrides;
      this.biome = pBiome;
      this.layers = Lists.newArrayList();
      this.lakes = pLakes;
   }

   public FlatLevelGeneratorSettings withBiomeAndLayers(List<FlatLayerInfo> pLayerInfos, Optional<HolderSet<StructureSet>> pStructureSets, Holder<Biome> pBiome) {
      FlatLevelGeneratorSettings $$3 = new FlatLevelGeneratorSettings(pStructureSets, pBiome, this.lakes);

      for (FlatLayerInfo $$4 : pLayerInfos) {
         $$3.layersInfo.add(new FlatLayerInfo($$4.getHeight(), $$4.getBlockState().getBlock()));
         $$3.updateLayers();
      }

      if (this.decoration) {
         $$3.setDecoration();
      }

      if (this.addLakes) {
         $$3.setAddLakes();
      }

      return $$3;
   }

   public void setDecoration() {
      this.decoration = true;
   }

   public void setAddLakes() {
      this.addLakes = true;
   }

   public BiomeGenerationSettings adjustGenerationSettings(Holder<Biome> pBiome) {
      if (!pBiome.equals(this.biome)) {
         return pBiome.value().getGenerationSettings();
      } else {
         BiomeGenerationSettings $$1 = this.getBiome().value().getGenerationSettings();
         BiomeGenerationSettings.PlainBuilder $$2 = new BiomeGenerationSettings.PlainBuilder();
         if (this.addLakes) {
            for (Holder<PlacedFeature> $$3 : this.lakes) {
               $$2.addFeature(GenerationStep.Decoration.LAKES, $$3);
            }
         }

         boolean $$4 = (!this.voidGen || pBiome.is(Biomes.THE_VOID)) && this.decoration;
         if ($$4) {
            List<HolderSet<PlacedFeature>> $$5 = $$1.features();

            for (int $$6 = 0; $$6 < $$5.size(); $$6++) {
               if ($$6 != GenerationStep.Decoration.UNDERGROUND_STRUCTURES.ordinal()
                  && $$6 != GenerationStep.Decoration.SURFACE_STRUCTURES.ordinal()
                  && (!this.addLakes || $$6 != GenerationStep.Decoration.LAKES.ordinal())) {
                  for (Holder<PlacedFeature> $$8 : $$5.get($$6)) {
                     $$2.addFeature($$6, $$8);
                  }
               }
            }
         }

         List<BlockState> $$9 = this.getLayers();

         for (int $$10 = 0; $$10 < $$9.size(); $$10++) {
            BlockState $$11 = $$9.get($$10);
            if (!Heightmap.Types.MOTION_BLOCKING.isOpaque().test($$11)) {
               $$9.set($$10, null);
               $$2.addFeature(
                  GenerationStep.Decoration.TOP_LAYER_MODIFICATION, PlacementUtils.inlinePlaced(Feature.FILL_LAYER, new LayerConfiguration($$10, $$11))
               );
            }
         }

         return $$2.build();
      }
   }

   public Optional<HolderSet<StructureSet>> structureOverrides() {
      return this.structureOverrides;
   }

   public Holder<Biome> getBiome() {
      return this.biome;
   }

   public List<FlatLayerInfo> getLayersInfo() {
      return this.layersInfo;
   }

   public List<BlockState> getLayers() {
      return this.layers;
   }

   public void updateLayers() {
      this.layers.clear();

      for (FlatLayerInfo $$0 : this.layersInfo) {
         for (int $$1 = 0; $$1 < $$0.getHeight(); $$1++) {
            this.layers.add($$0.getBlockState());
         }
      }

      this.voidGen = this.layers.stream().allMatch(p_209802_ -> p_209802_.is(Blocks.AIR));
   }

   public static FlatLevelGeneratorSettings getDefault(
      HolderGetter<Biome> pBiomes, HolderGetter<StructureSet> pStructureSetGetter, HolderGetter<PlacedFeature> pPlacedFeatureGetter
   ) {
      HolderSet<StructureSet> $$3 = HolderSet.direct(
         pStructureSetGetter.getOrThrow(BuiltinStructureSets.STRONGHOLDS), pStructureSetGetter.getOrThrow(BuiltinStructureSets.VILLAGES)
      );
      FlatLevelGeneratorSettings $$4 = new FlatLevelGeneratorSettings(Optional.of($$3), getDefaultBiome(pBiomes), createLakesList(pPlacedFeatureGetter));
      $$4.getLayersInfo().add(new FlatLayerInfo(1, Blocks.BEDROCK));
      $$4.getLayersInfo().add(new FlatLayerInfo(2, Blocks.DIRT));
      $$4.getLayersInfo().add(new FlatLayerInfo(1, Blocks.GRASS_BLOCK));
      $$4.updateLayers();
      return $$4;
   }

   public static Holder<Biome> getDefaultBiome(HolderGetter<Biome> pBiomes) {
      return pBiomes.getOrThrow(Biomes.PLAINS);
   }

   public static List<Holder<PlacedFeature>> createLakesList(HolderGetter<PlacedFeature> pPlacedFeatureGetter) {
      return List.of(
         pPlacedFeatureGetter.getOrThrow(MiscOverworldPlacements.LAKE_LAVA_UNDERGROUND),
         pPlacedFeatureGetter.getOrThrow(MiscOverworldPlacements.LAKE_LAVA_SURFACE)
      );
   }
}
