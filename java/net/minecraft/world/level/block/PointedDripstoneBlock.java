package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PointedDripstoneBlock extends Block implements Fallable, SimpleWaterloggedBlock {
   public static final MapCodec<PointedDripstoneBlock> CODEC = simpleCodec(PointedDripstoneBlock::new);
   public static final DirectionProperty TIP_DIRECTION = BlockStateProperties.VERTICAL_DIRECTION;
   public static final EnumProperty<DripstoneThickness> THICKNESS = BlockStateProperties.DRIPSTONE_THICKNESS;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   private static final int MAX_SEARCH_LENGTH_WHEN_CHECKING_DRIP_TYPE = 11;
   private static final int DELAY_BEFORE_FALLING = 2;
   private static final float DRIP_PROBABILITY_PER_ANIMATE_TICK = 0.02F;
   private static final float DRIP_PROBABILITY_PER_ANIMATE_TICK_IF_UNDER_LIQUID_SOURCE = 0.12F;
   private static final int MAX_SEARCH_LENGTH_BETWEEN_STALACTITE_TIP_AND_CAULDRON = 11;
   private static final float WATER_TRANSFER_PROBABILITY_PER_RANDOM_TICK = 0.17578125F;
   private static final float LAVA_TRANSFER_PROBABILITY_PER_RANDOM_TICK = 0.05859375F;
   private static final double MIN_TRIDENT_VELOCITY_TO_BREAK_DRIPSTONE = 0.6;
   private static final float STALACTITE_DAMAGE_PER_FALL_DISTANCE_AND_SIZE = 1.0F;
   private static final int STALACTITE_MAX_DAMAGE = 40;
   private static final int MAX_STALACTITE_HEIGHT_FOR_DAMAGE_CALCULATION = 6;
   private static final float STALAGMITE_FALL_DISTANCE_OFFSET = 2.0F;
   private static final int STALAGMITE_FALL_DAMAGE_MODIFIER = 2;
   private static final float AVERAGE_DAYS_PER_GROWTH = 5.0F;
   private static final float GROWTH_PROBABILITY_PER_RANDOM_TICK = 0.011377778F;
   private static final int MAX_GROWTH_LENGTH = 7;
   private static final int MAX_STALAGMITE_SEARCH_RANGE_WHEN_GROWING = 10;
   private static final float STALACTITE_DRIP_START_PIXEL = 0.6875F;
   private static final VoxelShape TIP_MERGE_SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 16.0, 11.0);
   private static final VoxelShape TIP_SHAPE_UP = Block.box(5.0, 0.0, 5.0, 11.0, 11.0, 11.0);
   private static final VoxelShape TIP_SHAPE_DOWN = Block.box(5.0, 5.0, 5.0, 11.0, 16.0, 11.0);
   private static final VoxelShape FRUSTUM_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
   private static final VoxelShape MIDDLE_SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
   private static final VoxelShape BASE_SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);
   private static final float MAX_HORIZONTAL_OFFSET = 0.125F;
   private static final VoxelShape REQUIRED_SPACE_TO_DRIP_THROUGH_NON_SOLID_BLOCK = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);

   @Override
   public MapCodec<PointedDripstoneBlock> codec() {
      return CODEC;
   }

   public PointedDripstoneBlock(BlockBehaviour.Properties p_154025_) {
      super(p_154025_);
      this.registerDefaultState(
         this.stateDefinition
            .any()
            .setValue(TIP_DIRECTION, Direction.UP)
            .setValue(THICKNESS, DripstoneThickness.TIP)
            .setValue(WATERLOGGED, Boolean.valueOf(false))
      );
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(TIP_DIRECTION, THICKNESS, WATERLOGGED);
   }

   @Override
   protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      return isValidPointedDripstonePlacement(pLevel, pPos, pState.getValue(TIP_DIRECTION));
   }

   @Override
   protected BlockState updateShape(
      BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos
   ) {
      if (pState.getValue(WATERLOGGED)) {
         pLevel.scheduleTick(pPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
      }

      if (pDirection != Direction.UP && pDirection != Direction.DOWN) {
         return pState;
      } else {
         Direction $$6 = pState.getValue(TIP_DIRECTION);
         if ($$6 == Direction.DOWN && pLevel.getBlockTicks().hasScheduledTick(pPos, this)) {
            return pState;
         } else if (pDirection == $$6.getOpposite() && !this.canSurvive(pState, pLevel, pPos)) {
            if ($$6 == Direction.DOWN) {
               pLevel.scheduleTick(pPos, this, 2);
            } else {
               pLevel.scheduleTick(pPos, this, 1);
            }

            return pState;
         } else {
            boolean $$7 = pState.getValue(THICKNESS) == DripstoneThickness.TIP_MERGE;
            DripstoneThickness $$8 = calculateDripstoneThickness(pLevel, pPos, $$6, $$7);
            return pState.setValue(THICKNESS, $$8);
         }
      }
   }

   @Override
   protected void onProjectileHit(Level pLevel, BlockState pState, BlockHitResult pHit, Projectile pProjectile) {
      if (!pLevel.isClientSide) {
         BlockPos $$4 = pHit.getBlockPos();
         if (pProjectile.mayInteract(pLevel, $$4)
            && pProjectile.mayBreak(pLevel)
            && pProjectile instanceof ThrownTrident
            && pProjectile.getDeltaMovement().length() > 0.6) {
            pLevel.destroyBlock($$4, true);
         }
      }
   }

   @Override
   public void fallOn(Level pLevel, BlockState pState, BlockPos pPos, Entity pEntity, float pFallDistance) {
      if (pState.getValue(TIP_DIRECTION) == Direction.UP && pState.getValue(THICKNESS) == DripstoneThickness.TIP) {
         pEntity.causeFallDamage(pFallDistance + 2.0F, 2.0F, pLevel.damageSources().stalagmite());
      } else {
         super.fallOn(pLevel, pState, pPos, pEntity, pFallDistance);
      }
   }

   @Override
   public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
      if (canDrip(pState)) {
         float $$4 = pRandom.nextFloat();
         if (!($$4 > 0.12F)) {
            getFluidAboveStalactite(pLevel, pPos, pState)
               .filter(p_221848_ -> $$4 < 0.02F || canFillCauldron(p_221848_.fluid))
               .ifPresent(p_221881_ -> spawnDripParticle(pLevel, pPos, pState, p_221881_.fluid));
         }
      }
   }

   @Override
   protected void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      if (isStalagmite(pState) && !this.canSurvive(pState, pLevel, pPos)) {
         pLevel.destroyBlock(pPos, true);
      } else {
         spawnFallingStalactite(pState, pLevel, pPos);
      }
   }

   @Override
   protected void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      maybeTransferFluid(pState, pLevel, pPos, pRandom.nextFloat());
      if (pRandom.nextFloat() < 0.011377778F && isStalactiteStartPos(pState, pLevel, pPos)) {
         growStalactiteOrStalagmiteIfPossible(pState, pLevel, pPos, pRandom);
      }
   }

   @VisibleForTesting
   public static void maybeTransferFluid(BlockState pState, ServerLevel pLevel, BlockPos pPos, float pRandChance) {
      if (!(pRandChance > 0.17578125F) || !(pRandChance > 0.05859375F)) {
         if (isStalactiteStartPos(pState, pLevel, pPos)) {
            Optional<PointedDripstoneBlock.FluidInfo> $$4 = getFluidAboveStalactite(pLevel, pPos, pState);
            if (!$$4.isEmpty()) {
               Fluid $$5 = $$4.get().fluid;
               float $$6;
               if ($$5 == Fluids.WATER) {
                  $$6 = 0.17578125F;
               } else {
                  if ($$5 != Fluids.LAVA) {
                     return;
                  }

                  $$6 = 0.05859375F;
               }

               if (!(pRandChance >= $$6)) {
                  BlockPos $$9 = findTip(pState, pLevel, pPos, 11, false);
                  if ($$9 != null) {
                     if ($$4.get().sourceState.is(Blocks.MUD) && $$5 == Fluids.WATER) {
                        BlockState $$10 = Blocks.CLAY.defaultBlockState();
                        pLevel.setBlockAndUpdate($$4.get().pos, $$10);
                        Block.pushEntitiesUp($$4.get().sourceState, $$10, pLevel, $$4.get().pos);
                        pLevel.gameEvent(GameEvent.BLOCK_CHANGE, $$4.get().pos, GameEvent.Context.of($$10));
                        pLevel.levelEvent(1504, $$9, 0);
                     } else {
                        BlockPos $$11 = findFillableCauldronBelowStalactiteTip(pLevel, $$9, $$5);
                        if ($$11 != null) {
                           pLevel.levelEvent(1504, $$9, 0);
                           int $$12 = $$9.getY() - $$11.getY();
                           int $$13 = 50 + $$12;
                           BlockState $$14 = pLevel.getBlockState($$11);
                           pLevel.scheduleTick($$11, $$14.getBlock(), $$13);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      LevelAccessor $$1 = pContext.getLevel();
      BlockPos $$2 = pContext.getClickedPos();
      Direction $$3 = pContext.getNearestLookingVerticalDirection().getOpposite();
      Direction $$4 = calculateTipDirection($$1, $$2, $$3);
      if ($$4 == null) {
         return null;
      } else {
         boolean $$5 = !pContext.isSecondaryUseActive();
         DripstoneThickness $$6 = calculateDripstoneThickness($$1, $$2, $$4, $$5);
         return $$6 == null
            ? null
            : this.defaultBlockState()
               .setValue(TIP_DIRECTION, $$4)
               .setValue(THICKNESS, $$6)
               .setValue(WATERLOGGED, Boolean.valueOf($$1.getFluidState($$2).getType() == Fluids.WATER));
      }
   }

   @Override
   protected FluidState getFluidState(BlockState pState) {
      return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
   }

   @Override
   protected VoxelShape getOcclusionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return Shapes.empty();
   }

   @Override
   protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      DripstoneThickness $$4 = pState.getValue(THICKNESS);
      VoxelShape $$5;
      if ($$4 == DripstoneThickness.TIP_MERGE) {
         $$5 = TIP_MERGE_SHAPE;
      } else if ($$4 == DripstoneThickness.TIP) {
         if (pState.getValue(TIP_DIRECTION) == Direction.DOWN) {
            $$5 = TIP_SHAPE_DOWN;
         } else {
            $$5 = TIP_SHAPE_UP;
         }
      } else if ($$4 == DripstoneThickness.FRUSTUM) {
         $$5 = FRUSTUM_SHAPE;
      } else if ($$4 == DripstoneThickness.MIDDLE) {
         $$5 = MIDDLE_SHAPE;
      } else {
         $$5 = BASE_SHAPE;
      }

      Vec3 $$11 = pState.getOffset(pLevel, pPos);
      return $$5.move($$11.x, 0.0, $$11.z);
   }

   @Override
   protected boolean isCollisionShapeFullBlock(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return false;
   }

   @Override
   protected float getMaxHorizontalOffset() {
      return 0.125F;
   }

   @Override
   public void onBrokenAfterFall(Level pLevel, BlockPos pPos, FallingBlockEntity pFallingBlock) {
      if (!pFallingBlock.isSilent()) {
         pLevel.levelEvent(1045, pPos, 0);
      }
   }

   @Override
   public DamageSource getFallDamageSource(Entity pEntity) {
      return pEntity.damageSources().fallingStalactite(pEntity);
   }

   private static void spawnFallingStalactite(BlockState pState, ServerLevel pLevel, BlockPos pPos) {
      BlockPos.MutableBlockPos $$3 = pPos.mutable();
      BlockState $$4 = pState;

      while (isStalactite($$4)) {
         FallingBlockEntity $$5 = FallingBlockEntity.fall(pLevel, $$3, $$4);
         if (isTip($$4, true)) {
            int $$6 = Math.max(1 + pPos.getY() - $$3.getY(), 6);
            float $$7 = 1.0F * (float)$$6;
            $$5.setHurtsEntities($$7, 40);
            break;
         }

         $$3.move(Direction.DOWN);
         $$4 = pLevel.getBlockState($$3);
      }
   }

   @VisibleForTesting
   public static void growStalactiteOrStalagmiteIfPossible(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      BlockState $$4 = pLevel.getBlockState(pPos.above(1));
      BlockState $$5 = pLevel.getBlockState(pPos.above(2));
      if (canGrow($$4, $$5)) {
         BlockPos $$6 = findTip(pState, pLevel, pPos, 7, false);
         if ($$6 != null) {
            BlockState $$7 = pLevel.getBlockState($$6);
            if (canDrip($$7) && canTipGrow($$7, pLevel, $$6)) {
               if (pRandom.nextBoolean()) {
                  grow(pLevel, $$6, Direction.DOWN);
               } else {
                  growStalagmiteBelow(pLevel, $$6);
               }
            }
         }
      }
   }

   private static void growStalagmiteBelow(ServerLevel pLevel, BlockPos pPos) {
      BlockPos.MutableBlockPos $$2 = pPos.mutable();

      for (int $$3 = 0; $$3 < 10; $$3++) {
         $$2.move(Direction.DOWN);
         BlockState $$4 = pLevel.getBlockState($$2);
         if (!$$4.getFluidState().isEmpty()) {
            return;
         }

         if (isUnmergedTipWithDirection($$4, Direction.UP) && canTipGrow($$4, pLevel, $$2)) {
            grow(pLevel, $$2, Direction.UP);
            return;
         }

         if (isValidPointedDripstonePlacement(pLevel, $$2, Direction.UP) && !pLevel.isWaterAt($$2.below())) {
            grow(pLevel, $$2.below(), Direction.UP);
            return;
         }

         if (!canDripThrough(pLevel, $$2, $$4)) {
            return;
         }
      }
   }

   private static void grow(ServerLevel pServer, BlockPos pPos, Direction pDirection) {
      BlockPos $$3 = pPos.relative(pDirection);
      BlockState $$4 = pServer.getBlockState($$3);
      if (isUnmergedTipWithDirection($$4, pDirection.getOpposite())) {
         createMergedTips($$4, pServer, $$3);
      } else if ($$4.isAir() || $$4.is(Blocks.WATER)) {
         createDripstone(pServer, $$3, pDirection, DripstoneThickness.TIP);
      }
   }

   private static void createDripstone(LevelAccessor pLevel, BlockPos pPos, Direction pDirection, DripstoneThickness pThickness) {
      BlockState $$4 = Blocks.POINTED_DRIPSTONE
         .defaultBlockState()
         .setValue(TIP_DIRECTION, pDirection)
         .setValue(THICKNESS, pThickness)
         .setValue(WATERLOGGED, Boolean.valueOf(pLevel.getFluidState(pPos).getType() == Fluids.WATER));
      pLevel.setBlock(pPos, $$4, 3);
   }

   private static void createMergedTips(BlockState pState, LevelAccessor pLevel, BlockPos pPos) {
      BlockPos $$4;
      BlockPos $$3;
      if (pState.getValue(TIP_DIRECTION) == Direction.UP) {
         $$3 = pPos;
         $$4 = pPos.above();
      } else {
         $$4 = pPos;
         $$3 = pPos.below();
      }

      createDripstone(pLevel, $$4, Direction.DOWN, DripstoneThickness.TIP_MERGE);
      createDripstone(pLevel, $$3, Direction.UP, DripstoneThickness.TIP_MERGE);
   }

   public static void spawnDripParticle(Level pLevel, BlockPos pPos, BlockState pState) {
      getFluidAboveStalactite(pLevel, pPos, pState).ifPresent(p_221856_ -> spawnDripParticle(pLevel, pPos, pState, p_221856_.fluid));
   }

   private static void spawnDripParticle(Level pLevel, BlockPos pPos, BlockState pState, Fluid pFluid) {
      Vec3 $$4 = pState.getOffset(pLevel, pPos);
      double $$5 = 0.0625;
      double $$6 = (double)pPos.getX() + 0.5 + $$4.x;
      double $$7 = (double)((float)(pPos.getY() + 1) - 0.6875F) - 0.0625;
      double $$8 = (double)pPos.getZ() + 0.5 + $$4.z;
      Fluid $$9 = getDripFluid(pLevel, pFluid);
      ParticleOptions $$10 = $$9.is(FluidTags.LAVA) ? ParticleTypes.DRIPPING_DRIPSTONE_LAVA : ParticleTypes.DRIPPING_DRIPSTONE_WATER;
      pLevel.addParticle($$10, $$6, $$7, $$8, 0.0, 0.0, 0.0);
   }

   @Nullable
   private static BlockPos findTip(BlockState pState, LevelAccessor pLevel, BlockPos pPos, int pMaxIterations, boolean pIsTipMerge) {
      if (isTip(pState, pIsTipMerge)) {
         return pPos;
      } else {
         Direction $$5 = pState.getValue(TIP_DIRECTION);
         BiPredicate<BlockPos, BlockState> $$6 = (p_202023_, p_202024_) -> p_202024_.is(Blocks.POINTED_DRIPSTONE) && p_202024_.getValue(TIP_DIRECTION) == $$5;
         return findBlockVertical(pLevel, pPos, $$5.getAxisDirection(), $$6, p_154168_ -> isTip(p_154168_, pIsTipMerge), pMaxIterations).orElse(null);
      }
   }

   @Nullable
   private static Direction calculateTipDirection(LevelReader pLevel, BlockPos pPos, Direction pDir) {
      Direction $$3;
      if (isValidPointedDripstonePlacement(pLevel, pPos, pDir)) {
         $$3 = pDir;
      } else {
         if (!isValidPointedDripstonePlacement(pLevel, pPos, pDir.getOpposite())) {
            return null;
         }

         $$3 = pDir.getOpposite();
      }

      return $$3;
   }

   private static DripstoneThickness calculateDripstoneThickness(LevelReader pLevel, BlockPos pPos, Direction pDir, boolean pIsTipMerge) {
      Direction $$4 = pDir.getOpposite();
      BlockState $$5 = pLevel.getBlockState(pPos.relative(pDir));
      if (isPointedDripstoneWithDirection($$5, $$4)) {
         return !pIsTipMerge && $$5.getValue(THICKNESS) != DripstoneThickness.TIP_MERGE ? DripstoneThickness.TIP : DripstoneThickness.TIP_MERGE;
      } else if (!isPointedDripstoneWithDirection($$5, pDir)) {
         return DripstoneThickness.TIP;
      } else {
         DripstoneThickness $$6 = $$5.getValue(THICKNESS);
         if ($$6 != DripstoneThickness.TIP && $$6 != DripstoneThickness.TIP_MERGE) {
            BlockState $$7 = pLevel.getBlockState(pPos.relative($$4));
            return !isPointedDripstoneWithDirection($$7, pDir) ? DripstoneThickness.BASE : DripstoneThickness.MIDDLE;
         } else {
            return DripstoneThickness.FRUSTUM;
         }
      }
   }

   public static boolean canDrip(BlockState p_154239_) {
      return isStalactite(p_154239_) && p_154239_.getValue(THICKNESS) == DripstoneThickness.TIP && !p_154239_.getValue(WATERLOGGED);
   }

   private static boolean canTipGrow(BlockState pState, ServerLevel pLevel, BlockPos pPos) {
      Direction $$3 = pState.getValue(TIP_DIRECTION);
      BlockPos $$4 = pPos.relative($$3);
      BlockState $$5 = pLevel.getBlockState($$4);
      if (!$$5.getFluidState().isEmpty()) {
         return false;
      } else {
         return $$5.isAir() ? true : isUnmergedTipWithDirection($$5, $$3.getOpposite());
      }
   }

   private static Optional<BlockPos> findRootBlock(Level pLevel, BlockPos pPos, BlockState pState, int pMaxIterations) {
      Direction $$4 = pState.getValue(TIP_DIRECTION);
      BiPredicate<BlockPos, BlockState> $$5 = (p_202015_, p_202016_) -> p_202016_.is(Blocks.POINTED_DRIPSTONE) && p_202016_.getValue(TIP_DIRECTION) == $$4;
      return findBlockVertical(pLevel, pPos, $$4.getOpposite().getAxisDirection(), $$5, p_154245_ -> !p_154245_.is(Blocks.POINTED_DRIPSTONE), pMaxIterations);
   }

   private static boolean isValidPointedDripstonePlacement(LevelReader pLevel, BlockPos pPos, Direction pDir) {
      BlockPos $$3 = pPos.relative(pDir.getOpposite());
      BlockState $$4 = pLevel.getBlockState($$3);
      return $$4.isFaceSturdy(pLevel, $$3, pDir) || isPointedDripstoneWithDirection($$4, pDir);
   }

   private static boolean isTip(BlockState pState, boolean pIsTipMerge) {
      if (!pState.is(Blocks.POINTED_DRIPSTONE)) {
         return false;
      } else {
         DripstoneThickness $$2 = pState.getValue(THICKNESS);
         return $$2 == DripstoneThickness.TIP || pIsTipMerge && $$2 == DripstoneThickness.TIP_MERGE;
      }
   }

   private static boolean isUnmergedTipWithDirection(BlockState pState, Direction pDir) {
      return isTip(pState, false) && pState.getValue(TIP_DIRECTION) == pDir;
   }

   private static boolean isStalactite(BlockState pState) {
      return isPointedDripstoneWithDirection(pState, Direction.DOWN);
   }

   private static boolean isStalagmite(BlockState pState) {
      return isPointedDripstoneWithDirection(pState, Direction.UP);
   }

   private static boolean isStalactiteStartPos(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      return isStalactite(pState) && !pLevel.getBlockState(pPos.above()).is(Blocks.POINTED_DRIPSTONE);
   }

   @Override
   protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
      return false;
   }

   private static boolean isPointedDripstoneWithDirection(BlockState pState, Direction pDir) {
      return pState.is(Blocks.POINTED_DRIPSTONE) && pState.getValue(TIP_DIRECTION) == pDir;
   }

   @Nullable
   private static BlockPos findFillableCauldronBelowStalactiteTip(Level pLevel, BlockPos pPos, Fluid pFluid) {
      Predicate<BlockState> $$3 = p_154162_ -> p_154162_.getBlock() instanceof AbstractCauldronBlock
            && ((AbstractCauldronBlock)p_154162_.getBlock()).canReceiveStalactiteDrip(pFluid);
      BiPredicate<BlockPos, BlockState> $$4 = (p_202034_, p_202035_) -> canDripThrough(pLevel, p_202034_, p_202035_);
      return findBlockVertical(pLevel, pPos, Direction.DOWN.getAxisDirection(), $$4, $$3, 11).orElse(null);
   }

   @Nullable
   public static BlockPos findStalactiteTipAboveCauldron(Level pLevel, BlockPos pPos) {
      BiPredicate<BlockPos, BlockState> $$2 = (p_202030_, p_202031_) -> canDripThrough(pLevel, p_202030_, p_202031_);
      return findBlockVertical(pLevel, pPos, Direction.UP.getAxisDirection(), $$2, PointedDripstoneBlock::canDrip, 11).orElse(null);
   }

   public static Fluid getCauldronFillFluidType(ServerLevel pLevel, BlockPos pPos) {
      return getFluidAboveStalactite(pLevel, pPos, pLevel.getBlockState(pPos))
         .map(p_221858_ -> p_221858_.fluid)
         .filter(PointedDripstoneBlock::canFillCauldron)
         .orElse(Fluids.EMPTY);
   }

   private static Optional<PointedDripstoneBlock.FluidInfo> getFluidAboveStalactite(Level pLevel, BlockPos pPos, BlockState pState) {
      return !isStalactite(pState) ? Optional.empty() : findRootBlock(pLevel, pPos, pState, 11).map(p_221876_ -> {
         BlockPos $$2 = p_221876_.above();
         BlockState $$3 = pLevel.getBlockState($$2);
         Fluid $$4;
         if ($$3.is(Blocks.MUD) && !pLevel.dimensionType().ultraWarm()) {
            $$4 = Fluids.WATER;
         } else {
            $$4 = pLevel.getFluidState($$2).getType();
         }

         return new PointedDripstoneBlock.FluidInfo($$2, $$4, $$3);
      });
   }

   private static boolean canFillCauldron(Fluid p_154159_) {
      return p_154159_ == Fluids.LAVA || p_154159_ == Fluids.WATER;
   }

   private static boolean canGrow(BlockState pDripstoneState, BlockState pState) {
      return pDripstoneState.is(Blocks.DRIPSTONE_BLOCK) && pState.is(Blocks.WATER) && pState.getFluidState().isSource();
   }

   private static Fluid getDripFluid(Level pLevel, Fluid pFluid) {
      if (pFluid.isSame(Fluids.EMPTY)) {
         return pLevel.dimensionType().ultraWarm() ? Fluids.LAVA : Fluids.WATER;
      } else {
         return pFluid;
      }
   }

   private static Optional<BlockPos> findBlockVertical(
      LevelAccessor pLevel,
      BlockPos pPos,
      Direction.AxisDirection pAxis,
      BiPredicate<BlockPos, BlockState> pPositionalStatePredicate,
      Predicate<BlockState> pStatePredicate,
      int pMaxIterations
   ) {
      Direction $$6 = Direction.get(pAxis, Direction.Axis.Y);
      BlockPos.MutableBlockPos $$7 = pPos.mutable();

      for (int $$8 = 1; $$8 < pMaxIterations; $$8++) {
         $$7.move($$6);
         BlockState $$9 = pLevel.getBlockState($$7);
         if (pStatePredicate.test($$9)) {
            return Optional.of($$7.immutable());
         }

         if (pLevel.isOutsideBuildHeight($$7.getY()) || !pPositionalStatePredicate.test($$7, $$9)) {
            return Optional.empty();
         }
      }

      return Optional.empty();
   }

   private static boolean canDripThrough(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
      if (pState.isAir()) {
         return true;
      } else if (pState.isSolidRender(pLevel, pPos)) {
         return false;
      } else if (!pState.getFluidState().isEmpty()) {
         return false;
      } else {
         VoxelShape $$3 = pState.getCollisionShape(pLevel, pPos);
         return !Shapes.joinIsNotEmpty(REQUIRED_SPACE_TO_DRIP_THROUGH_NON_SOLID_BLOCK, $$3, BooleanOp.AND);
      }
   }

   static record FluidInfo(BlockPos pos, Fluid fluid, BlockState sourceState) {
   }
}
