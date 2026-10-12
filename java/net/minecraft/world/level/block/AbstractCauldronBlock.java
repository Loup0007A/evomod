package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractCauldronBlock extends Block {
   private static final int SIDE_THICKNESS = 2;
   private static final int LEG_WIDTH = 4;
   private static final int LEG_HEIGHT = 3;
   private static final int LEG_DEPTH = 2;
   protected static final int FLOOR_LEVEL = 4;
   private static final VoxelShape INSIDE = box(2.0, 4.0, 2.0, 14.0, 16.0, 14.0);
   protected static final VoxelShape SHAPE = Shapes.join(
      Shapes.block(),
      Shapes.or(box(0.0, 0.0, 4.0, 16.0, 3.0, 12.0), box(4.0, 0.0, 0.0, 12.0, 3.0, 16.0), box(2.0, 0.0, 2.0, 14.0, 3.0, 14.0), INSIDE),
      BooleanOp.ONLY_FIRST
   );
   protected final CauldronInteraction.InteractionMap interactions;

   @Override
   protected abstract MapCodec<? extends AbstractCauldronBlock> codec();

   public AbstractCauldronBlock(BlockBehaviour.Properties pProperties, CauldronInteraction.InteractionMap pInteractions) {
      super(pProperties);
      this.interactions = pInteractions;
   }

   protected double getContentHeight(BlockState pState) {
      return 0.0;
   }

   protected boolean isEntityInsideContent(BlockState pState, BlockPos pPos, Entity pEntity) {
      return pEntity.getY() < (double)pPos.getY() + this.getContentHeight(pState) && pEntity.getBoundingBox().maxY > (double)pPos.getY() + 0.25;
   }

   @Override
   protected ItemInteractionResult useItemOn(
      ItemStack pStack, BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHitResult
   ) {
      CauldronInteraction $$7 = this.interactions.map().get(pStack.getItem());
      return $$7.interact(pState, pLevel, pPos, pPlayer, pHand, pStack);
   }

   @Override
   protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SHAPE;
   }

   @Override
   protected VoxelShape getInteractionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return INSIDE;
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState pState) {
      return true;
   }

   @Override
   protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
      return false;
   }

   public abstract boolean isFull(BlockState pState);

   @Override
   protected void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      BlockPos $$4 = PointedDripstoneBlock.findStalactiteTipAboveCauldron(pLevel, pPos);
      if ($$4 != null) {
         Fluid $$5 = PointedDripstoneBlock.getCauldronFillFluidType(pLevel, $$4);
         if ($$5 != Fluids.EMPTY && this.canReceiveStalactiteDrip($$5)) {
            this.receiveStalactiteDrip(pState, pLevel, pPos, $$5);
         }
      }
   }

   protected boolean canReceiveStalactiteDrip(Fluid pFluid) {
      return false;
   }

   protected void receiveStalactiteDrip(BlockState pState, Level pLevel, BlockPos pPos, Fluid pFluid) {
   }
}
