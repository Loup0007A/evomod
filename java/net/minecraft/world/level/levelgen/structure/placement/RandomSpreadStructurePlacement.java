package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public class RandomSpreadStructurePlacement extends StructurePlacement {
   public static final MapCodec<RandomSpreadStructurePlacement> CODEC = RecordCodecBuilder.mapCodec(
         p_204996_ -> placementCodec(p_204996_)
               .and(
                  p_204996_.group(
                     Codec.intRange(0, 4096).fieldOf("spacing").forGetter(RandomSpreadStructurePlacement::spacing),
                     Codec.intRange(0, 4096).fieldOf("separation").forGetter(RandomSpreadStructurePlacement::separation),
                     RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR).forGetter(RandomSpreadStructurePlacement::spreadType)
                  )
               )
               .apply(p_204996_, RandomSpreadStructurePlacement::new)
      )
      .validate(RandomSpreadStructurePlacement::validate);
   private final int spacing;
   private final int separation;
   private final RandomSpreadType spreadType;

   private static DataResult<RandomSpreadStructurePlacement> validate(RandomSpreadStructurePlacement p_286361_) {
      return p_286361_.spacing <= p_286361_.separation ? DataResult.error(() -> "Spacing has to be larger than separation") : DataResult.success(p_286361_);
   }

   public RandomSpreadStructurePlacement(
      Vec3i p_227000_,
      StructurePlacement.FrequencyReductionMethod p_227001_,
      float p_227002_,
      int p_227003_,
      Optional<StructurePlacement.ExclusionZone> p_227004_,
      int p_227005_,
      int p_227006_,
      RandomSpreadType p_227007_
   ) {
      super(p_227000_, p_227001_, p_227002_, p_227003_, p_227004_);
      this.spacing = p_227005_;
      this.separation = p_227006_;
      this.spreadType = p_227007_;
   }

   public RandomSpreadStructurePlacement(int pSpacing, int pSeparation, RandomSpreadType pSpreadType, int pSalt) {
      this(Vec3i.ZERO, StructurePlacement.FrequencyReductionMethod.DEFAULT, 1.0F, pSalt, Optional.empty(), pSpacing, pSeparation, pSpreadType);
   }

   public int spacing() {
      return this.spacing;
   }

   public int separation() {
      return this.separation;
   }

   public RandomSpreadType spreadType() {
      return this.spreadType;
   }

   public ChunkPos getPotentialStructureChunk(long pSeed, int pRegionX, int pRegionZ) {
      int $$3 = Math.floorDiv(pRegionX, this.spacing);
      int $$4 = Math.floorDiv(pRegionZ, this.spacing);
      WorldgenRandom $$5 = new WorldgenRandom(new LegacyRandomSource(0L));
      $$5.setLargeFeatureWithSalt(pSeed, $$3, $$4, this.salt());
      int $$6 = this.spacing - this.separation;
      int $$7 = this.spreadType.evaluate($$5, $$6);
      int $$8 = this.spreadType.evaluate($$5, $$6);
      return new ChunkPos($$3 * this.spacing + $$7, $$4 * this.spacing + $$8);
   }

   @Override
   protected boolean isPlacementChunk(ChunkGeneratorStructureState pStructureState, int pX, int pZ) {
      ChunkPos $$3 = this.getPotentialStructureChunk(pStructureState.getLevelSeed(), pX, pZ);
      return $$3.x == pX && $$3.z == pZ;
   }

   @Override
   public StructurePlacementType<?> type() {
      return StructurePlacementType.RANDOM_SPREAD;
   }
}
