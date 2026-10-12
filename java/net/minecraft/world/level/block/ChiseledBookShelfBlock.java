package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class ChiseledBookShelfBlock extends BaseEntityBlock {
   public static final MapCodec<ChiseledBookShelfBlock> CODEC = simpleCodec(ChiseledBookShelfBlock::new);
   private static final int MAX_BOOKS_IN_STORAGE = 6;
   public static final int BOOKS_PER_ROW = 3;
   public static final List<BooleanProperty> SLOT_OCCUPIED_PROPERTIES = List.of(
      BlockStateProperties.CHISELED_BOOKSHELF_SLOT_0_OCCUPIED,
      BlockStateProperties.CHISELED_BOOKSHELF_SLOT_1_OCCUPIED,
      BlockStateProperties.CHISELED_BOOKSHELF_SLOT_2_OCCUPIED,
      BlockStateProperties.CHISELED_BOOKSHELF_SLOT_3_OCCUPIED,
      BlockStateProperties.CHISELED_BOOKSHELF_SLOT_4_OCCUPIED,
      BlockStateProperties.CHISELED_BOOKSHELF_SLOT_5_OCCUPIED
   );

   @Override
   public MapCodec<ChiseledBookShelfBlock> codec() {
      return CODEC;
   }

   public ChiseledBookShelfBlock(BlockBehaviour.Properties p_249989_) {
      super(p_249989_);
      BlockState $$1 = this.stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);

      for (BooleanProperty $$2 : SLOT_OCCUPIED_PROPERTIES) {
         $$1 = $$1.setValue($$2, Boolean.valueOf(false));
      }

      this.registerDefaultState($$1);
   }

   @Override
   protected RenderShape getRenderShape(BlockState pState) {
      return RenderShape.MODEL;
   }

   @Override
   protected ItemInteractionResult useItemOn(
      ItemStack pStack, BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHitResult
   ) {
      if (pLevel.getBlockEntity(pPos) instanceof ChiseledBookShelfBlockEntity $$7) {
         if (!pStack.is(ItemTags.BOOKSHELF_BOOKS)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         } else {
            OptionalInt $$9 = this.getHitSlot(pHitResult, pState);
            if ($$9.isEmpty()) {
               return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            } else if (pState.getValue(SLOT_OCCUPIED_PROPERTIES.get($$9.getAsInt()))) {
               return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            } else {
               addBook(pLevel, pPos, pPlayer, $$7, pStack, $$9.getAsInt());
               return ItemInteractionResult.sidedSuccess(pLevel.isClientSide);
            }
         }
      } else {
         return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
      if (pLevel.getBlockEntity(pPos) instanceof ChiseledBookShelfBlockEntity $$5) {
         OptionalInt $$7 = this.getHitSlot(pHitResult, pState);
         if ($$7.isEmpty()) {
            return InteractionResult.PASS;
         } else if (!pState.getValue(SLOT_OCCUPIED_PROPERTIES.get($$7.getAsInt()))) {
            return InteractionResult.CONSUME;
         } else {
            removeBook(pLevel, pPos, pPlayer, $$5, $$7.getAsInt());
            return InteractionResult.sidedSuccess(pLevel.isClientSide);
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   private OptionalInt getHitSlot(BlockHitResult pHitReselt, BlockState pState) {
      return getRelativeHitCoordinatesForBlockFace(pHitReselt, pState.getValue(HorizontalDirectionalBlock.FACING)).map(p_327255_ -> {
         int $$1 = p_327255_.y >= 0.5F ? 0 : 1;
         int $$2 = getSection(p_327255_.x);
         return OptionalInt.of($$2 + $$1 * 3);
      }).orElseGet(OptionalInt::empty);
   }

   private static Optional<Vec2> getRelativeHitCoordinatesForBlockFace(BlockHitResult pHitResult, Direction pFace) {
      Direction $$2 = pHitResult.getDirection();
      if (pFace != $$2) {
         return Optional.empty();
      } else {
         BlockPos $$3 = pHitResult.getBlockPos().relative($$2);
         Vec3 $$4 = pHitResult.getLocation().subtract((double)$$3.getX(), (double)$$3.getY(), (double)$$3.getZ());
         double $$5 = $$4.x();
         double $$6 = $$4.y();
         double $$7 = $$4.z();

         return switch ($$2) {
            case NORTH -> Optional.of(new Vec2((float)(1.0 - $$5), (float)$$6));
            case SOUTH -> Optional.of(new Vec2((float)$$5, (float)$$6));
            case WEST -> Optional.of(new Vec2((float)$$7, (float)$$6));
            case EAST -> Optional.of(new Vec2((float)(1.0 - $$7), (float)$$6));
            case DOWN, UP -> Optional.empty();
         };
      }
   }

   private static int getSection(float pX) {
      float $$1 = 0.0625F;
      float $$2 = 0.375F;
      if (pX < 0.375F) {
         return 0;
      } else {
         float $$3 = 0.6875F;
         return pX < 0.6875F ? 1 : 2;
      }
   }

   private static void addBook(Level pLevel, BlockPos pPos, Player pPlayer, ChiseledBookShelfBlockEntity pBlockEntity, ItemStack pBookStack, int pSlot) {
      if (!pLevel.isClientSide) {
         pPlayer.awardStat(Stats.ITEM_USED.get(pBookStack.getItem()));
         SoundEvent $$6 = pBookStack.is(Items.ENCHANTED_BOOK) ? SoundEvents.CHISELED_BOOKSHELF_INSERT_ENCHANTED : SoundEvents.CHISELED_BOOKSHELF_INSERT;
         pBlockEntity.setItem(pSlot, pBookStack.consumeAndReturn(1, pPlayer));
         pLevel.playSound(null, pPos, $$6, SoundSource.BLOCKS, 1.0F, 1.0F);
      }
   }

   private static void removeBook(Level pLevel, BlockPos pPos, Player pPlayer, ChiseledBookShelfBlockEntity pBlockEntity, int pSlot) {
      if (!pLevel.isClientSide) {
         ItemStack $$5 = pBlockEntity.removeItem(pSlot, 1);
         SoundEvent $$6 = $$5.is(Items.ENCHANTED_BOOK) ? SoundEvents.CHISELED_BOOKSHELF_PICKUP_ENCHANTED : SoundEvents.CHISELED_BOOKSHELF_PICKUP;
         pLevel.playSound(null, pPos, $$6, SoundSource.BLOCKS, 1.0F, 1.0F);
         if (!pPlayer.getInventory().add($$5)) {
            pPlayer.drop($$5, false);
         }

         pLevel.gameEvent(pPlayer, GameEvent.BLOCK_CHANGE, pPos);
      }
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
      return new ChiseledBookShelfBlockEntity(pPos, pState);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(HorizontalDirectionalBlock.FACING);
      SLOT_OCCUPIED_PROPERTIES.forEach(p_261456_ -> pBuilder.add(p_261456_));
   }

   @Override
   protected void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
      if (!pState.is(pNewState.getBlock())) {
         boolean $$9;
         label32: {
            if (pLevel.getBlockEntity(pPos) instanceof ChiseledBookShelfBlockEntity $$6 && !$$6.isEmpty()) {
               for (int $$7 = 0; $$7 < 6; $$7++) {
                  ItemStack $$8 = $$6.getItem($$7);
                  if (!$$8.isEmpty()) {
                     Containers.dropItemStack(pLevel, (double)pPos.getX(), (double)pPos.getY(), (double)pPos.getZ(), $$8);
                  }
               }

               $$6.clearContent();
               $$9 = true;
               break label32;
            }

            $$9 = false;
         }

         super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
         if ($$9) {
            pLevel.updateNeighbourForOutputSignal(pPos, this);
         }
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      return this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, pContext.getHorizontalDirection().getOpposite());
   }

   @Override
   public BlockState rotate(BlockState pState, Rotation pRotation) {
      return pState.setValue(HorizontalDirectionalBlock.FACING, pRotation.rotate(pState.getValue(HorizontalDirectionalBlock.FACING)));
   }

   @Override
   public BlockState mirror(BlockState pState, Mirror pMirror) {
      return pState.rotate(pMirror.getRotation(pState.getValue(HorizontalDirectionalBlock.FACING)));
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState pState) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
      if (pLevel.isClientSide()) {
         return 0;
      } else {
         return pLevel.getBlockEntity(pPos) instanceof ChiseledBookShelfBlockEntity $$3 ? $$3.getLastInteractedSlot() + 1 : 0;
      }
   }
}
