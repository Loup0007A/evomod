package net.minecraft.world.level.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class DispenserBlock extends BaseEntityBlock {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<DispenserBlock> CODEC = simpleCodec(DispenserBlock::new);
   public static final DirectionProperty FACING = DirectionalBlock.FACING;
   public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
   private static final DefaultDispenseItemBehavior DEFAULT_BEHAVIOR = new DefaultDispenseItemBehavior();
   public static final Map<Item, DispenseItemBehavior> DISPENSER_REGISTRY = Util.make(
      new Object2ObjectOpenHashMap(), p_327262_ -> p_327262_.defaultReturnValue(DEFAULT_BEHAVIOR)
   );
   private static final int TRIGGER_DURATION = 4;

   @Override
   public MapCodec<? extends DispenserBlock> codec() {
      return CODEC;
   }

   public static void registerBehavior(ItemLike pItem, DispenseItemBehavior pBehavior) {
      DISPENSER_REGISTRY.put(pItem.asItem(), pBehavior);
   }

   public static void registerProjectileBehavior(ItemLike pItem) {
      DISPENSER_REGISTRY.put(pItem.asItem(), new ProjectileDispenseBehavior(pItem.asItem()));
   }

   public DispenserBlock(BlockBehaviour.Properties p_52664_) {
      super(p_52664_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TRIGGERED, Boolean.valueOf(false)));
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
      if (pLevel.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         BlockEntity $$5 = pLevel.getBlockEntity(pPos);
         if ($$5 instanceof DispenserBlockEntity) {
            pPlayer.openMenu((DispenserBlockEntity)$$5);
            if ($$5 instanceof DropperBlockEntity) {
               pPlayer.awardStat(Stats.INSPECT_DROPPER);
            } else {
               pPlayer.awardStat(Stats.INSPECT_DISPENSER);
            }
         }

         return InteractionResult.CONSUME;
      }
   }

   protected void dispenseFrom(ServerLevel pLevel, BlockState pState, BlockPos pPos) {
      DispenserBlockEntity $$3 = pLevel.getBlockEntity(pPos, BlockEntityType.DISPENSER).orElse(null);
      if ($$3 == null) {
         LOGGER.warn("Ignoring dispensing attempt for Dispenser without matching block entity at {}", pPos);
      } else {
         BlockSource $$4 = new BlockSource(pLevel, pPos, pState, $$3);
         int $$5 = $$3.getRandomSlot(pLevel.random);
         if ($$5 < 0) {
            pLevel.levelEvent(1001, pPos, 0);
            pLevel.gameEvent(GameEvent.BLOCK_ACTIVATE, pPos, GameEvent.Context.of($$3.getBlockState()));
         } else {
            ItemStack $$6 = $$3.getItem($$5);
            DispenseItemBehavior $$7 = this.getDispenseMethod(pLevel, $$6);
            if ($$7 != DispenseItemBehavior.NOOP) {
               $$3.setItem($$5, $$7.dispense($$4, $$6));
            }
         }
      }
   }

   protected DispenseItemBehavior getDispenseMethod(Level pLevel, ItemStack pItem) {
      return (DispenseItemBehavior)(!pItem.isItemEnabled(pLevel.enabledFeatures()) ? DEFAULT_BEHAVIOR : DISPENSER_REGISTRY.get(pItem.getItem()));
   }

   @Override
   protected void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
      boolean $$6 = pLevel.hasNeighborSignal(pPos) || pLevel.hasNeighborSignal(pPos.above());
      boolean $$7 = pState.getValue(TRIGGERED);
      if ($$6 && !$$7) {
         pLevel.scheduleTick(pPos, this, 4);
         pLevel.setBlock(pPos, pState.setValue(TRIGGERED, Boolean.valueOf(true)), 2);
      } else if (!$$6 && $$7) {
         pLevel.setBlock(pPos, pState.setValue(TRIGGERED, Boolean.valueOf(false)), 2);
      }
   }

   @Override
   protected void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      this.dispenseFrom(pLevel, pState, pPos);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
      return new DispenserBlockEntity(pPos, pState);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      return this.defaultBlockState().setValue(FACING, pContext.getNearestLookingDirection().getOpposite());
   }

   @Override
   protected void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
      Containers.dropContentsOnDestroy(pState, pNewState, pLevel, pPos);
      super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
   }

   public static Position getDispensePosition(BlockSource pBlockSource) {
      return getDispensePosition(pBlockSource, 0.7, Vec3.ZERO);
   }

   public static Position getDispensePosition(BlockSource pBlockSource, double pMultiplier, Vec3 pOffset) {
      Direction $$3 = pBlockSource.state().getValue(FACING);
      return pBlockSource.center()
         .add(
            pMultiplier * (double)$$3.getStepX() + pOffset.x(),
            pMultiplier * (double)$$3.getStepY() + pOffset.y(),
            pMultiplier * (double)$$3.getStepZ() + pOffset.z()
         );
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState pState) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState pBlockState, Level pLevel, BlockPos pPos) {
      return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(pLevel.getBlockEntity(pPos));
   }

   @Override
   protected RenderShape getRenderShape(BlockState pState) {
      return RenderShape.MODEL;
   }

   @Override
   protected BlockState rotate(BlockState pState, Rotation pRotation) {
      return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState pState, Mirror pMirror) {
      return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(FACING, TRIGGERED);
   }
}
