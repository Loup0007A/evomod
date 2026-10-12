package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class LeaveVineDecorator extends TreeDecorator {
   public static final MapCodec<LeaveVineDecorator> CODEC = Codec.floatRange(0.0F, 1.0F)
      .fieldOf("probability")
      .xmap(LeaveVineDecorator::new, p_226037_ -> p_226037_.probability);
   private final float probability;

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.LEAVE_VINE;
   }

   public LeaveVineDecorator(float p_226031_) {
      this.probability = p_226031_;
   }

   @Override
   public void place(TreeDecorator.Context pContext) {
      RandomSource $$1 = pContext.random();
      pContext.leaves().forEach(p_226035_ -> {
         if ($$1.nextFloat() < this.probability) {
            BlockPos $$3 = p_226035_.west();
            if (pContext.isAir($$3)) {
               addHangingVine($$3, VineBlock.EAST, pContext);
            }
         }

         if ($$1.nextFloat() < this.probability) {
            BlockPos $$4 = p_226035_.east();
            if (pContext.isAir($$4)) {
               addHangingVine($$4, VineBlock.WEST, pContext);
            }
         }

         if ($$1.nextFloat() < this.probability) {
            BlockPos $$5 = p_226035_.north();
            if (pContext.isAir($$5)) {
               addHangingVine($$5, VineBlock.SOUTH, pContext);
            }
         }

         if ($$1.nextFloat() < this.probability) {
            BlockPos $$6 = p_226035_.south();
            if (pContext.isAir($$6)) {
               addHangingVine($$6, VineBlock.NORTH, pContext);
            }
         }
      });
   }

   private static void addHangingVine(BlockPos pPos, BooleanProperty pSideProperty, TreeDecorator.Context pContext) {
      pContext.placeVine(pPos, pSideProperty);
      int $$3 = 4;

      for (BlockPos var4 = pPos.below(); pContext.isAir(var4) && $$3 > 0; $$3--) {
         pContext.placeVine(var4, pSideProperty);
         var4 = var4.below();
      }
   }
}
