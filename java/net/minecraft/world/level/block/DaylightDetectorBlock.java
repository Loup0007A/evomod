package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DaylightDetectorBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DaylightDetectorBlock extends BaseEntityBlock {
   public static final MapCodec<DaylightDetectorBlock> CODEC = simpleCodec(DaylightDetectorBlock::new);
   public static final IntegerProperty POWER = BlockStateProperties.POWER;
   public static final BooleanProperty INVERTED = BlockStateProperties.INVERTED;
   protected static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0);

   @Override
   public MapCodec<DaylightDetectorBlock> codec() {
      return CODEC;
   }

   public DaylightDetectorBlock(BlockBehaviour.Properties p_52382_) {
      super(p_52382_);
      this.registerDefaultState(this.stateDefinition.any().setValue(POWER, Integer.valueOf(0)).setValue(INVERTED, Boolean.valueOf(false)));
   }

   @Override
   protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SHAPE;
   }

   @Override
   protected boolean useShapeForLightOcclusion(BlockState pState) {
      return true;
   }

   @Override
   protected int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
      return pBlockState.getValue(POWER);
   }

   private static void updateSignalStrength(BlockState pState, Level pLevel, BlockPos pPos) {
      int $$3 = pLevel.getBrightness(LightLayer.SKY, pPos) - pLevel.getSkyDarken();
      float $$4 = pLevel.getSunAngle(1.0F);
      boolean $$5 = pState.getValue(INVERTED);
      if ($$5) {
         $$3 = 15 - $$3;
      } else if ($$3 > 0) {
         float $$6 = $$4 < (float) Math.PI ? 0.0F : (float) (Math.PI * 2);
         $$4 += ($$6 - $$4) * 0.2F;
         $$3 = Math.round((float)$$3 * Mth.cos($$4));
      }

      $$3 = Mth.clamp($$3, 0, 15);
      if (pState.getValue(POWER) != $$3) {
         pLevel.setBlock(pPos, pState.setValue(POWER, Integer.valueOf($$3)), 3);
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
      if (pPlayer.mayBuild()) {
         if (pLevel.isClientSide) {
            return InteractionResult.SUCCESS;
         } else {
            BlockState $$5 = pState.cycle(INVERTED);
            pLevel.setBlock(pPos, $$5, 2);
            pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(pPlayer, $$5));
            updateSignalStrength($$5, pLevel, pPos);
            return InteractionResult.CONSUME;
         }
      } else {
         return super.useWithoutItem(pState, pLevel, pPos, pPlayer, pHitResult);
      }
   }

   @Override
   protected RenderShape getRenderShape(BlockState pState) {
      return RenderShape.MODEL;
   }

   @Override
   protected boolean isSignalSource(BlockState pState) {
      return true;
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
      return new DaylightDetectorBlockEntity(pPos, pState);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
      return !pLevel.isClientSide && pLevel.dimensionType().hasSkyLight()
         ? createTickerHelper(pBlockEntityType, BlockEntityType.DAYLIGHT_DETECTOR, DaylightDetectorBlock::tickEntity)
         : null;
   }

   private static void tickEntity(Level p_153113_, BlockPos p_153114_, BlockState p_153115_, DaylightDetectorBlockEntity p_153116_) {
      if (p_153113_.getGameTime() % 20L == 0L) {
         updateSignalStrength(p_153115_, p_153113_, p_153114_);
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(POWER, INVERTED);
   }
}
