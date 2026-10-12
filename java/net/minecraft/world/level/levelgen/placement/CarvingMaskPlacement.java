package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.GenerationStep;

public class CarvingMaskPlacement extends PlacementModifier {
   public static final MapCodec<CarvingMaskPlacement> CODEC = GenerationStep.Carving.CODEC
      .fieldOf("step")
      .xmap(CarvingMaskPlacement::new, p_191593_ -> p_191593_.step);
   private final GenerationStep.Carving step;

   private CarvingMaskPlacement(GenerationStep.Carving p_191589_) {
      this.step = p_191589_;
   }

   public static CarvingMaskPlacement forStep(GenerationStep.Carving pStep) {
      return new CarvingMaskPlacement(pStep);
   }

   @Override
   public Stream<BlockPos> getPositions(PlacementContext pContext, RandomSource pRandom, BlockPos pPos) {
      ChunkPos $$3 = new ChunkPos(pPos);
      return pContext.getCarvingMask($$3, this.step).stream($$3);
   }

   @Override
   public PlacementModifierType<?> type() {
      return PlacementModifierType.CARVING_MASK_PLACEMENT;
   }
}
