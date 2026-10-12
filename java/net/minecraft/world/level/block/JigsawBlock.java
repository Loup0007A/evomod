package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;

public class JigsawBlock extends Block implements EntityBlock, GameMasterBlock {
   public static final MapCodec<JigsawBlock> CODEC = simpleCodec(JigsawBlock::new);
   public static final EnumProperty<FrontAndTop> ORIENTATION = BlockStateProperties.ORIENTATION;

   @Override
   public MapCodec<JigsawBlock> codec() {
      return CODEC;
   }

   public JigsawBlock(BlockBehaviour.Properties p_54225_) {
      super(p_54225_);
      this.registerDefaultState(this.stateDefinition.any().setValue(ORIENTATION, FrontAndTop.NORTH_UP));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(ORIENTATION);
   }

   @Override
   protected BlockState rotate(BlockState pState, Rotation pRotation) {
      return pState.setValue(ORIENTATION, pRotation.rotation().rotate(pState.getValue(ORIENTATION)));
   }

   @Override
   protected BlockState mirror(BlockState pState, Mirror pMirror) {
      return pState.setValue(ORIENTATION, pMirror.rotation().rotate(pState.getValue(ORIENTATION)));
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      Direction $$1 = pContext.getClickedFace();
      Direction $$2;
      if ($$1.getAxis() == Direction.Axis.Y) {
         $$2 = pContext.getHorizontalDirection().getOpposite();
      } else {
         $$2 = Direction.UP;
      }

      return this.defaultBlockState().setValue(ORIENTATION, FrontAndTop.fromFrontAndTop($$1, $$2));
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
      return new JigsawBlockEntity(pPos, pState);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
      BlockEntity $$5 = pLevel.getBlockEntity(pPos);
      if ($$5 instanceof JigsawBlockEntity && pPlayer.canUseGameMasterBlocks()) {
         pPlayer.openJigsawBlock((JigsawBlockEntity)$$5);
         return InteractionResult.sidedSuccess(pLevel.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   public static boolean canAttach(StructureTemplate.StructureBlockInfo pInfo, StructureTemplate.StructureBlockInfo pInfo2) {
      Direction $$2 = getFrontFacing(pInfo.state());
      Direction $$3 = getFrontFacing(pInfo2.state());
      Direction $$4 = getTopFacing(pInfo.state());
      Direction $$5 = getTopFacing(pInfo2.state());
      JigsawBlockEntity.JointType $$6 = JigsawBlockEntity.JointType.byName(pInfo.nbt().getString("joint"))
         .orElseGet(() -> $$2.getAxis().isHorizontal() ? JigsawBlockEntity.JointType.ALIGNED : JigsawBlockEntity.JointType.ROLLABLE);
      boolean $$7 = $$6 == JigsawBlockEntity.JointType.ROLLABLE;
      return $$2 == $$3.getOpposite() && ($$7 || $$4 == $$5) && pInfo.nbt().getString("target").equals(pInfo2.nbt().getString("name"));
   }

   public static Direction getFrontFacing(BlockState pState) {
      return pState.getValue(ORIENTATION).front();
   }

   public static Direction getTopFacing(BlockState pState) {
      return pState.getValue(ORIENTATION).top();
   }
}
