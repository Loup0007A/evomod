package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public abstract class FaceAttachedHorizontalDirectionalBlock extends HorizontalDirectionalBlock {
   public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;

   protected FaceAttachedHorizontalDirectionalBlock(BlockBehaviour.Properties pProperties) {
      super(pProperties);
   }

   @Override
   protected abstract MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec();

   @Override
   protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      return canAttach(pLevel, pPos, getConnectedDirection(pState).getOpposite());
   }

   public static boolean canAttach(LevelReader pReader, BlockPos pPos, Direction pDirection) {
      BlockPos $$3 = pPos.relative(pDirection);
      return pReader.getBlockState($$3).isFaceSturdy(pReader, $$3, pDirection.getOpposite());
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      for (Direction $$1 : pContext.getNearestLookingDirections()) {
         BlockState $$2;
         if ($$1.getAxis() == Direction.Axis.Y) {
            $$2 = this.defaultBlockState()
               .setValue(FACE, $$1 == Direction.UP ? AttachFace.CEILING : AttachFace.FLOOR)
               .setValue(FACING, pContext.getHorizontalDirection());
         } else {
            $$2 = this.defaultBlockState().setValue(FACE, AttachFace.WALL).setValue(FACING, $$1.getOpposite());
         }

         if ($$2.canSurvive(pContext.getLevel(), pContext.getClickedPos())) {
            return $$2;
         }
      }

      return null;
   }

   @Override
   protected BlockState updateShape(
      BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos
   ) {
      return getConnectedDirection(pState).getOpposite() == pFacing && !pState.canSurvive(pLevel, pCurrentPos)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
   }

   protected static Direction getConnectedDirection(BlockState pState) {
      switch ((AttachFace)pState.getValue(FACE)) {
         case CEILING:
            return Direction.DOWN;
         case FLOOR:
            return Direction.UP;
         default:
            return pState.getValue(FACING);
      }
   }
}
