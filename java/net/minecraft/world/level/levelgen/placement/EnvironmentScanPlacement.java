package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

public class EnvironmentScanPlacement extends PlacementModifier {
   private final Direction directionOfSearch;
   private final BlockPredicate targetCondition;
   private final BlockPredicate allowedSearchCondition;
   private final int maxSteps;
   public static final MapCodec<EnvironmentScanPlacement> CODEC = RecordCodecBuilder.mapCodec(
      p_191650_ -> p_191650_.group(
               Direction.VERTICAL_CODEC.fieldOf("direction_of_search").forGetter(p_191672_ -> p_191672_.directionOfSearch),
               BlockPredicate.CODEC.fieldOf("target_condition").forGetter(p_191670_ -> p_191670_.targetCondition),
               BlockPredicate.CODEC
                  .optionalFieldOf("allowed_search_condition", BlockPredicate.alwaysTrue())
                  .forGetter(p_191668_ -> p_191668_.allowedSearchCondition),
               Codec.intRange(1, 32).fieldOf("max_steps").forGetter(p_191652_ -> p_191652_.maxSteps)
            )
            .apply(p_191650_, EnvironmentScanPlacement::new)
   );

   private EnvironmentScanPlacement(Direction p_191645_, BlockPredicate p_191646_, BlockPredicate p_191647_, int p_191648_) {
      this.directionOfSearch = p_191645_;
      this.targetCondition = p_191646_;
      this.allowedSearchCondition = p_191647_;
      this.maxSteps = p_191648_;
   }

   public static EnvironmentScanPlacement scanningFor(
      Direction pDirectionOfSearch, BlockPredicate pTargetCondition, BlockPredicate pAllowedSearchCondition, int pMaxSteps
   ) {
      return new EnvironmentScanPlacement(pDirectionOfSearch, pTargetCondition, pAllowedSearchCondition, pMaxSteps);
   }

   public static EnvironmentScanPlacement scanningFor(Direction pDirectionOfSearch, BlockPredicate pTargetCondition, int pMaxSteps) {
      return scanningFor(pDirectionOfSearch, pTargetCondition, BlockPredicate.alwaysTrue(), pMaxSteps);
   }

   @Override
   public Stream<BlockPos> getPositions(PlacementContext pContext, RandomSource pRandom, BlockPos pPos) {
      BlockPos.MutableBlockPos $$3 = pPos.mutable();
      WorldGenLevel $$4 = pContext.getLevel();
      if (!this.allowedSearchCondition.test($$4, $$3)) {
         return Stream.of();
      } else {
         for (int $$5 = 0; $$5 < this.maxSteps; $$5++) {
            if (this.targetCondition.test($$4, $$3)) {
               return Stream.of($$3);
            }

            $$3.move(this.directionOfSearch);
            if ($$4.isOutsideBuildHeight($$3.getY())) {
               return Stream.of();
            }

            if (!this.allowedSearchCondition.test($$4, $$3)) {
               break;
            }
         }

         return this.targetCondition.test($$4, $$3) ? Stream.of($$3) : Stream.of();
      }
   }

   @Override
   public PlacementModifierType<?> type() {
      return PlacementModifierType.ENVIRONMENT_SCAN;
   }
}
