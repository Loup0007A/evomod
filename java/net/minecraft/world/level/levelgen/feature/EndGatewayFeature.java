package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration;

public class EndGatewayFeature extends Feature<EndGatewayConfiguration> {
   public EndGatewayFeature(Codec<EndGatewayConfiguration> pCodec) {
      super(pCodec);
   }

   @Override
   public boolean place(FeaturePlaceContext<EndGatewayConfiguration> pContext) {
      BlockPos $$1 = pContext.origin();
      WorldGenLevel $$2 = pContext.level();
      EndGatewayConfiguration $$3 = pContext.config();

      for (BlockPos $$4 : BlockPos.betweenClosed($$1.offset(-1, -2, -1), $$1.offset(1, 2, 1))) {
         boolean $$5 = $$4.getX() == $$1.getX();
         boolean $$6 = $$4.getY() == $$1.getY();
         boolean $$7 = $$4.getZ() == $$1.getZ();
         boolean $$8 = Math.abs($$4.getY() - $$1.getY()) == 2;
         if ($$5 && $$6 && $$7) {
            BlockPos $$9 = $$4.immutable();
            this.setBlock($$2, $$9, Blocks.END_GATEWAY.defaultBlockState());
            $$3.getExit().ifPresent(p_341906_ -> {
               if ($$2.getBlockEntity($$9) instanceof TheEndGatewayBlockEntity $$5x) {
                  $$5x.setExitPosition(p_341906_, $$3.isExitExact());
               }
            });
         } else if ($$6) {
            this.setBlock($$2, $$4, Blocks.AIR.defaultBlockState());
         } else if ($$8 && $$5 && $$7) {
            this.setBlock($$2, $$4, Blocks.BEDROCK.defaultBlockState());
         } else if (($$5 || $$7) && !$$8) {
            this.setBlock($$2, $$4, Blocks.BEDROCK.defaultBlockState());
         } else {
            this.setBlock($$2, $$4, Blocks.AIR.defaultBlockState());
         }
      }

      return true;
   }
}
