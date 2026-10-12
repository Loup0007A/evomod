package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignApplicator;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class SignBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   protected static final float AABB_OFFSET = 4.0F;
   protected static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
   private final WoodType type;

   protected SignBlock(WoodType pType, BlockBehaviour.Properties pProperties) {
      super(pProperties);
      this.type = pType;
   }

   @Override
   protected abstract MapCodec<? extends SignBlock> codec();

   @Override
   protected BlockState updateShape(
      BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos
   ) {
      if (pState.getValue(WATERLOGGED)) {
         pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
      }

      return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
   }

   @Override
   protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SHAPE;
   }

   @Override
   public boolean isPossibleToRespawnInThis(BlockState pState) {
      return true;
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
      return new SignBlockEntity(pPos, pState);
   }

   @Override
   protected ItemInteractionResult useItemOn(
      ItemStack pStack, BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHitResult
   ) {
      if (pLevel.getBlockEntity(pPos) instanceof SignBlockEntity $$7) {
         SignApplicator $$10 = pStack.getItem() instanceof SignApplicator $$9 ? $$9 : null;
         boolean $$11 = $$10 != null && pPlayer.mayBuild();
         if (!pLevel.isClientSide) {
            if ($$11 && !$$7.isWaxed() && !this.otherPlayerIsEditingSign(pPlayer, $$7)) {
               boolean $$12 = $$7.isFacingFrontText(pPlayer);
               if ($$10.canApplyToSign($$7.getText($$12), pPlayer) && $$10.tryApplyToSign(pLevel, $$7, $$12, pPlayer)) {
                  $$7.executeClickCommandsIfPresent(pPlayer, pLevel, pPos, $$12);
                  pPlayer.awardStat(Stats.ITEM_USED.get(pStack.getItem()));
                  pLevel.gameEvent(GameEvent.BLOCK_CHANGE, $$7.getBlockPos(), GameEvent.Context.of(pPlayer, $$7.getBlockState()));
                  pStack.consume(1, pPlayer);
                  return ItemInteractionResult.SUCCESS;
               } else {
                  return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
               }
            } else {
               return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
         } else {
            return !$$11 && !$$7.isWaxed() ? ItemInteractionResult.CONSUME : ItemInteractionResult.SUCCESS;
         }
      } else {
         return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
      if (pLevel.getBlockEntity(pPos) instanceof SignBlockEntity $$5) {
         if (pLevel.isClientSide) {
            Util.pauseInIde(new IllegalStateException("Expected to only call this on server"));
         }

         boolean $$7 = $$5.isFacingFrontText(pPlayer);
         boolean $$8 = $$5.executeClickCommandsIfPresent(pPlayer, pLevel, pPos, $$7);
         if ($$5.isWaxed()) {
            pLevel.playSound(null, $$5.getBlockPos(), $$5.getSignInteractionFailedSoundEvent(), SoundSource.BLOCKS);
            return InteractionResult.SUCCESS;
         } else if ($$8) {
            return InteractionResult.SUCCESS;
         } else if (!this.otherPlayerIsEditingSign(pPlayer, $$5) && pPlayer.mayBuild() && this.hasEditableText(pPlayer, $$5, $$7)) {
            this.openTextEdit(pPlayer, $$5, $$7);
            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.PASS;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   private boolean hasEditableText(Player pPlayer, SignBlockEntity pSignEntity, boolean pIsFrontText) {
      SignText $$3 = pSignEntity.getText(pIsFrontText);
      return Arrays.stream($$3.getMessages(pPlayer.isTextFilteringEnabled()))
         .allMatch(p_327267_ -> p_327267_.equals(CommonComponents.EMPTY) || p_327267_.getContents() instanceof PlainTextContents);
   }

   public abstract float getYRotationDegrees(BlockState pState);

   public Vec3 getSignHitboxCenterPosition(BlockState pState) {
      return new Vec3(0.5, 0.5, 0.5);
   }

   @Override
   protected FluidState getFluidState(BlockState pState) {
      return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
   }

   public WoodType type() {
      return this.type;
   }

   public static WoodType getWoodType(Block pBlock) {
      WoodType $$1;
      if (pBlock instanceof SignBlock) {
         $$1 = ((SignBlock)pBlock).type();
      } else {
         $$1 = WoodType.OAK;
      }

      return $$1;
   }

   public void openTextEdit(Player pPlayer, SignBlockEntity pSignEntity, boolean pIsFrontText) {
      pSignEntity.setAllowedPlayerEditor(pPlayer.getUUID());
      pPlayer.openTextEdit(pSignEntity, pIsFrontText);
   }

   private boolean otherPlayerIsEditingSign(Player pPlayer, SignBlockEntity pSignEntity) {
      UUID $$2 = pSignEntity.getPlayerWhoMayEdit();
      return $$2 != null && !$$2.equals(pPlayer.getUUID());
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
      return createTickerHelper(pBlockEntityType, BlockEntityType.SIGN, SignBlockEntity::tick);
   }
}
