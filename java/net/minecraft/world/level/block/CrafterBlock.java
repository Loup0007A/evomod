package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeCache;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class CrafterBlock extends BaseEntityBlock {
   public static final MapCodec<CrafterBlock> CODEC = simpleCodec(CrafterBlock::new);
   public static final BooleanProperty CRAFTING = BlockStateProperties.CRAFTING;
   public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
   private static final EnumProperty<FrontAndTop> ORIENTATION = BlockStateProperties.ORIENTATION;
   private static final int MAX_CRAFTING_TICKS = 6;
   private static final int CRAFTING_TICK_DELAY = 4;
   private static final RecipeCache RECIPE_CACHE = new RecipeCache(10);
   private static final int CRAFTER_ADVANCEMENT_DIAMETER = 17;

   public CrafterBlock(BlockBehaviour.Properties p_310228_) {
      super(p_310228_);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(ORIENTATION, FrontAndTop.NORTH_UP)
            .setValue(TRIGGERED, Boolean.valueOf(false))
            .setValue(CRAFTING, Boolean.valueOf(false))
      );
   }

   @Override
   protected MapCodec<CrafterBlock> codec() {
      return CODEC;
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState pState) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
      return pLevel.getBlockEntity(pPos) instanceof CrafterBlockEntity $$4 ? $$4.getRedstoneSignal() : 0;
   }

   @Override
   protected void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pNeighborBlock, BlockPos pNeighborPos, boolean pMovedByPiston) {
      boolean $$6 = pLevel.hasNeighborSignal(pPos);
      boolean $$7 = pState.getValue(TRIGGERED);
      BlockEntity $$8 = pLevel.getBlockEntity(pPos);
      if ($$6 && !$$7) {
         pLevel.scheduleTick(pPos, this, 4);
         pLevel.setBlock(pPos, pState.setValue(TRIGGERED, Boolean.valueOf(true)), 2);
         this.setBlockEntityTriggered($$8, true);
      } else if (!$$6 && $$7) {
         pLevel.setBlock(pPos, pState.setValue(TRIGGERED, Boolean.valueOf(false)).setValue(CRAFTING, Boolean.valueOf(false)), 2);
         this.setBlockEntityTriggered($$8, false);
      }
   }

   @Override
   protected void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      this.dispenseFrom(pState, pLevel, pPos);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
      return pLevel.isClientSide ? null : createTickerHelper(pBlockEntityType, BlockEntityType.CRAFTER, CrafterBlockEntity::serverTick);
   }

   private void setBlockEntityTriggered(@Nullable BlockEntity pBlockEntity, boolean pTriggered) {
      if (pBlockEntity instanceof CrafterBlockEntity $$2) {
         $$2.setTriggered(pTriggered);
      }
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
      CrafterBlockEntity $$2 = new CrafterBlockEntity(pPos, pState);
      $$2.setTriggered(pState.hasProperty(TRIGGERED) && pState.getValue(TRIGGERED));
      return $$2;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      Direction $$1 = pContext.getNearestLookingDirection().getOpposite();

      Direction $$2 = switch ($$1) {
         case DOWN -> pContext.getHorizontalDirection().getOpposite();
         case UP -> pContext.getHorizontalDirection();
         case NORTH, SOUTH, WEST, EAST -> Direction.UP;
      };
      return this.defaultBlockState()
         .setValue(ORIENTATION, FrontAndTop.fromFrontAndTop($$1, $$2))
         .setValue(TRIGGERED, Boolean.valueOf(pContext.getLevel().hasNeighborSignal(pContext.getClickedPos())));
   }

   @Override
   public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
      if (pState.getValue(TRIGGERED)) {
         pLevel.scheduleTick(pPos, this, 4);
      }
   }

   @Override
   protected void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
      Containers.dropContentsOnDestroy(pState, pNewState, pLevel, pPos);
      super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
      if (pLevel.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         BlockEntity $$5 = pLevel.getBlockEntity(pPos);
         if ($$5 instanceof CrafterBlockEntity) {
            pPlayer.openMenu((CrafterBlockEntity)$$5);
         }

         return InteractionResult.CONSUME;
      }
   }

   protected void dispenseFrom(BlockState pState, ServerLevel pLevel, BlockPos pPos) {
      if (pLevel.getBlockEntity(pPos) instanceof CrafterBlockEntity $$3) {
         CraftingInput var11 = $$3.asCraftInput();
         Optional<RecipeHolder<CraftingRecipe>> $$6 = getPotentialResults(pLevel, var11);
         if ($$6.isEmpty()) {
            pLevel.levelEvent(1050, pPos, 0);
         } else {
            RecipeHolder<CraftingRecipe> $$7 = $$6.get();
            ItemStack $$8 = $$7.value().assemble(var11, pLevel.registryAccess());
            if ($$8.isEmpty()) {
               pLevel.levelEvent(1050, pPos, 0);
            } else {
               $$3.setCraftingTicksRemaining(6);
               pLevel.setBlock(pPos, pState.setValue(CRAFTING, Boolean.valueOf(true)), 2);
               $$8.onCraftedBySystem(pLevel);
               this.dispenseItem(pLevel, pPos, $$3, $$8, pState, $$7);

               for (ItemStack $$9 : $$7.value().getRemainingItems(var11)) {
                  if (!$$9.isEmpty()) {
                     this.dispenseItem(pLevel, pPos, $$3, $$9, pState, $$7);
                  }
               }

               $$3.getItems().forEach(p_312802_ -> {
                  if (!p_312802_.isEmpty()) {
                     p_312802_.shrink(1);
                  }
               });
               $$3.setChanged();
            }
         }
      }
   }

   public static Optional<RecipeHolder<CraftingRecipe>> getPotentialResults(Level pLevel, CraftingInput pInput) {
      return RECIPE_CACHE.get(pLevel, pInput);
   }

   private void dispenseItem(
      ServerLevel pLevel, BlockPos pPos, CrafterBlockEntity pCrafter, ItemStack pStack, BlockState pState, RecipeHolder<CraftingRecipe> pRecipe
   ) {
      Direction $$6 = pState.getValue(ORIENTATION).front();
      Container $$7 = HopperBlockEntity.getContainerAt(pLevel, pPos.relative($$6));
      ItemStack $$8 = pStack.copy();
      if ($$7 != null && ($$7 instanceof CrafterBlockEntity || pStack.getCount() > $$7.getMaxStackSize(pStack))) {
         while (!$$8.isEmpty()) {
            ItemStack $$9 = $$8.copyWithCount(1);
            ItemStack $$10 = HopperBlockEntity.addItem(pCrafter, $$7, $$9, $$6.getOpposite());
            if (!$$10.isEmpty()) {
               break;
            }

            $$8.shrink(1);
         }
      } else if ($$7 != null) {
         while (!$$8.isEmpty()) {
            int $$11 = $$8.getCount();
            $$8 = HopperBlockEntity.addItem(pCrafter, $$7, $$8, $$6.getOpposite());
            if ($$11 == $$8.getCount()) {
               break;
            }
         }
      }

      if (!$$8.isEmpty()) {
         Vec3 $$12 = Vec3.atCenterOf(pPos);
         Vec3 $$13 = $$12.relative($$6, 0.7);
         DefaultDispenseItemBehavior.spawnItem(pLevel, $$8, 6, $$6, $$13);

         for (ServerPlayer $$14 : pLevel.getEntitiesOfClass(ServerPlayer.class, AABB.ofSize($$12, 17.0, 17.0, 17.0))) {
            CriteriaTriggers.CRAFTER_RECIPE_CRAFTED.trigger($$14, pRecipe.id(), pCrafter.getItems());
         }

         pLevel.levelEvent(1049, pPos, 0);
         pLevel.levelEvent(2010, pPos, $$6.get3DDataValue());
      }
   }

   @Override
   protected RenderShape getRenderShape(BlockState pState) {
      return RenderShape.MODEL;
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
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(ORIENTATION, TRIGGERED, CRAFTING);
   }
}
