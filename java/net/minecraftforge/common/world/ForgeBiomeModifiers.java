package net.minecraftforge.common.world;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.registries.ForgeRegistries;

public final class ForgeBiomeModifiers {
   private ForgeBiomeModifiers() {
   }

   public static record AddFeaturesBiomeModifier(HolderSet<Biome> biomes, HolderSet<PlacedFeature> features, GenerationStep.Decoration step)
      implements BiomeModifier {
      public static final MapCodec<ForgeBiomeModifiers.AddFeaturesBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(
         builder -> builder.group(
                  Biome.LIST_CODEC.fieldOf("biomes").forGetter(ForgeBiomeModifiers.AddFeaturesBiomeModifier::biomes),
                  PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(ForgeBiomeModifiers.AddFeaturesBiomeModifier::features),
                  GenerationStep.Decoration.CODEC.fieldOf("step").forGetter(ForgeBiomeModifiers.AddFeaturesBiomeModifier::step)
               )
               .apply(builder, ForgeBiomeModifiers.AddFeaturesBiomeModifier::new)
      );

      @Override
      public void modify(Holder<Biome> biome, BiomeModifier.Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
         if (phase == BiomeModifier.Phase.ADD && this.biomes.contains(biome)) {
            BiomeGenerationSettingsBuilder generationSettings = builder.getGenerationSettings();
            this.features.forEach(holder -> generationSettings.addFeature(this.step, (Holder<PlacedFeature>)holder));
         }
      }

      @Override
      public MapCodec<? extends BiomeModifier> codec() {
         return CODEC;
      }
   }

   public static record AddSpawnsBiomeModifier(HolderSet<Biome> biomes, List<MobSpawnSettings.SpawnerData> spawners) implements BiomeModifier {
      public static final MapCodec<ForgeBiomeModifiers.AddSpawnsBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(
         builder -> builder.group(
                  Biome.LIST_CODEC.fieldOf("biomes").forGetter(ForgeBiomeModifiers.AddSpawnsBiomeModifier::biomes),
                  Codec.either(MobSpawnSettings.SpawnerData.CODEC.listOf(), MobSpawnSettings.SpawnerData.CODEC)
                     .xmap(
                        either -> (List)either.map(Function.identity(), List::of),
                        list -> list.size() == 1 ? Either.right((MobSpawnSettings.SpawnerData)list.get(0)) : Either.left(list)
                     )
                     .fieldOf("spawners")
                     .forGetter(ForgeBiomeModifiers.AddSpawnsBiomeModifier::spawners)
               )
               .apply(builder, ForgeBiomeModifiers.AddSpawnsBiomeModifier::new)
      );

      public static ForgeBiomeModifiers.AddSpawnsBiomeModifier singleSpawn(HolderSet<Biome> biomes, MobSpawnSettings.SpawnerData spawner) {
         return new ForgeBiomeModifiers.AddSpawnsBiomeModifier(biomes, List.of(spawner));
      }

      @Override
      public void modify(Holder<Biome> biome, BiomeModifier.Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
         if (phase == BiomeModifier.Phase.ADD && this.biomes.contains(biome)) {
            MobSpawnSettingsBuilder spawns = builder.getMobSpawnSettings();

            for (MobSpawnSettings.SpawnerData spawner : this.spawners) {
               spawns.addSpawn(spawner.type.getCategory(), spawner);
            }
         }
      }

      @Override
      public MapCodec<? extends BiomeModifier> codec() {
         return CODEC;
      }
   }

   public static record RemoveFeaturesBiomeModifier(HolderSet<Biome> biomes, HolderSet<PlacedFeature> features, Set<GenerationStep.Decoration> steps)
      implements BiomeModifier {
      public static final MapCodec<ForgeBiomeModifiers.RemoveFeaturesBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(
         builder -> builder.group(
                  Biome.LIST_CODEC.fieldOf("biomes").forGetter(ForgeBiomeModifiers.RemoveFeaturesBiomeModifier::biomes),
                  PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(ForgeBiomeModifiers.RemoveFeaturesBiomeModifier::features),
                  Codec.either(GenerationStep.Decoration.CODEC.listOf(), GenerationStep.Decoration.CODEC)
                     .xmap(
                        either -> (Set)either.map(Set::copyOf, Set::of),
                        set -> set.size() == 1 ? Either.right(set.toArray(GenerationStep.Decoration[]::new)[0]) : Either.left(List.copyOf(set))
                     )
                     .optionalFieldOf("steps", EnumSet.allOf(GenerationStep.Decoration.class))
                     .forGetter(ForgeBiomeModifiers.RemoveFeaturesBiomeModifier::steps)
               )
               .apply(builder, ForgeBiomeModifiers.RemoveFeaturesBiomeModifier::new)
      );

      public static ForgeBiomeModifiers.RemoveFeaturesBiomeModifier allSteps(HolderSet<Biome> biomes, HolderSet<PlacedFeature> features) {
         return new ForgeBiomeModifiers.RemoveFeaturesBiomeModifier(biomes, features, EnumSet.allOf(GenerationStep.Decoration.class));
      }

      @Override
      public void modify(Holder<Biome> biome, BiomeModifier.Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
         if (phase == BiomeModifier.Phase.REMOVE && this.biomes.contains(biome)) {
            BiomeGenerationSettingsBuilder generationSettings = builder.getGenerationSettings();

            for (GenerationStep.Decoration step : this.steps) {
               generationSettings.getFeatures(step).removeIf(this.features::contains);
            }
         }
      }

      @Override
      public MapCodec<? extends BiomeModifier> codec() {
         return CODEC;
      }
   }

   public static record RemoveSpawnsBiomeModifier(HolderSet<Biome> biomes, HolderSet<EntityType<?>> entityTypes) implements BiomeModifier {
      public static final MapCodec<ForgeBiomeModifiers.RemoveSpawnsBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(
         builder -> builder.group(
                  Biome.LIST_CODEC.fieldOf("biomes").forGetter(ForgeBiomeModifiers.RemoveSpawnsBiomeModifier::biomes),
                  RegistryCodecs.homogeneousList(ForgeRegistries.Keys.ENTITY_TYPES)
                     .fieldOf("entity_types")
                     .forGetter(ForgeBiomeModifiers.RemoveSpawnsBiomeModifier::entityTypes)
               )
               .apply(builder, ForgeBiomeModifiers.RemoveSpawnsBiomeModifier::new)
      );

      @Override
      public void modify(Holder<Biome> biome, BiomeModifier.Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
         if (phase == BiomeModifier.Phase.REMOVE && this.biomes.contains(biome)) {
            MobSpawnSettingsBuilder spawns = builder.getMobSpawnSettings();

            for (MobCategory category : MobCategory.values()) {
               spawns.getSpawner(category).removeIf(data -> this.entityTypes.contains(ForgeRegistries.ENTITY_TYPES.getHolder(data.type).get()));
            }
         }
      }

      @Override
      public MapCodec<? extends BiomeModifier> codec() {
         return CODEC;
      }
   }
}
