package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.ArrayUtils;

public class BedBlock extends HorizontalDirectionalBlock implements EntityBlock {
   public static final MapCodec<BedBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_309266_ -> p_309266_.group(DyeColor.CODEC.fieldOf("color").forGetter(BedBlock::getColor), propertiesCodec()).apply(p_309266_, BedBlock::new)
   );
   public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
   public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;
   protected static final int HEIGHT = 9;
   protected static final VoxelShape BASE = Block.box(0.0, 3.0, 0.0, 16.0, 9.0, 16.0);
   private static final int LEG_WIDTH = 3;
   protected static final VoxelShape LEG_NORTH_WEST = Block.box(0.0, 0.0, 0.0, 3.0, 3.0, 3.0);
   protected static final VoxelShape LEG_SOUTH_WEST = Block.box(0.0, 0.0, 13.0, 3.0, 3.0, 16.0);
   protected static final VoxelShape LEG_NORTH_EAST = Block.box(13.0, 0.0, 0.0, 16.0, 3.0, 3.0);
   protected static final VoxelShape LEG_SOUTH_EAST = Block.box(13.0, 0.0, 13.0, 16.0, 3.0, 16.0);
   protected static final VoxelShape NORTH_SHAPE = Shapes.or(BASE, LEG_NORTH_WEST, LEG_NORTH_EAST);
   protected static final VoxelShape SOUTH_SHAPE = Shapes.or(BASE, LEG_SOUTH_WEST, LEG_SOUTH_EAST);
   protected static final VoxelShape WEST_SHAPE = Shapes.or(BASE, LEG_NORTH_WEST, LEG_SOUTH_WEST);
   protected static final VoxelShape EAST_SHAPE = Shapes.or(BASE, LEG_NORTH_EAST, LEG_SOUTH_EAST);
   private final DyeColor color;

   @Override
   public MapCodec<BedBlock> codec() {
      return CODEC;
   }

   public BedBlock(DyeColor p_49454_, BlockBehaviour.Properties p_49455_) {
      super(p_49455_);
      this.color = p_49454_;
      this.registerDefaultState(this.stateDefinition.any().setValue(PART, BedPart.FOOT).setValue(OCCUPIED, Boolean.valueOf(false)));
   }

   @Nullable
   public static Direction getBedOrientation(BlockGetter pLevel, BlockPos pPos) {
      BlockState $$2 = pLevel.getBlockState(pPos);
      return $$2.getBlock() instanceof BedBlock ? $$2.getValue(FACING) : null;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
      if (pLevel.isClientSide) {
         return InteractionResult.CONSUME;
      } else {
         if (pState.getValue(PART) != BedPart.HEAD) {
            pPos = pPos.relative(pState.getValue(FACING));
            pState = pLevel.getBlockState(pPos);
            if (!pState.is(this)) {
               return InteractionResult.CONSUME;
            }
         }

         if (!canSetSpawn(pLevel)) {
            pLevel.removeBlock(pPos, false);
            BlockPos $$5 = pPos.relative(pState.getValue(FACING).getOpposite());
            if (pLevel.getBlockState($$5).is(this)) {
               pLevel.removeBlock($$5, false);
            }

            Vec3 $$6 = pPos.getCenter();
            pLevel.explode(null, pLevel.damageSources().badRespawnPointExplosion($$6), null, $$6, 5.0F, true, Level.ExplosionInteraction.BLOCK);
            return InteractionResult.SUCCESS;
         } else if (pState.getValue(OCCUPIED)) {
            if (!this.kickVillagerOutOfBed(pLevel, pPos)) {
               pPlayer.displayClientMessage(Component.translatable("block.minecraft.bed.occupied"), true);
            }

            return InteractionResult.SUCCESS;
         } else {
            pPlayer.startSleepInBed(pPos).ifLeft(p_49477_ -> {
               if (p_49477_.getMessage() != null) {
                  pPlayer.displayClientMessage(p_49477_.getMessage(), true);
               }
            });
            return InteractionResult.SUCCESS;
         }
      }
   }

   public static boolean canSetSpawn(Level pLevel) {
      return pLevel.dimensionType().bedWorks();
   }

   private boolean kickVillagerOutOfBed(Level pLevel, BlockPos pPos) {
      List<Villager> $$2 = pLevel.getEntitiesOfClass(Villager.class, new AABB(pPos), LivingEntity::isSleeping);
      if ($$2.isEmpty()) {
         return false;
      } else {
         $$2.get(0).stopSleeping();
         return true;
      }
   }

   @Override
   public void fallOn(Level pLevel, BlockState pState, BlockPos pPos, Entity pEntity, float pFallDistance) {
      super.fallOn(pLevel, pState, pPos, pEntity, pFallDistance * 0.5F);
   }

   @Override
   public void updateEntityAfterFallOn(BlockGetter pLevel, Entity pEntity) {
      if (pEntity.isSuppressingBounce()) {
         super.updateEntityAfterFallOn(pLevel, pEntity);
      } else {
         this.bounceUp(pEntity);
      }
   }

   private void bounceUp(Entity pEntity) {
      Vec3 $$1 = pEntity.getDeltaMovement();
      if ($$1.y < 0.0) {
         double $$2 = pEntity instanceof LivingEntity ? 1.0 : 0.8;
         pEntity.setDeltaMovement($$1.x, -$$1.y * 0.66F * $$2, $$1.z);
      }
   }

   @Override
   protected BlockState updateShape(
      BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos
   ) {
      if (pFacing == getNeighbourDirection(pState.getValue(PART), pState.getValue(FACING))) {
         return pFacingState.is(this) && pFacingState.getValue(PART) != pState.getValue(PART)
            ? pState.setValue(OCCUPIED, pFacingState.getValue(OCCUPIED))
            : Blocks.AIR.defaultBlockState();
      } else {
         return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
      }
   }

   private static Direction getNeighbourDirection(BedPart pPart, Direction pDirection) {
      return pPart == BedPart.FOOT ? pDirection : pDirection.getOpposite();
   }

   @Override
   public BlockState playerWillDestroy(Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer) {
      if (!pLevel.isClientSide && pPlayer.isCreative()) {
         BedPart $$4 = pState.getValue(PART);
         if ($$4 == BedPart.FOOT) {
            BlockPos $$5 = pPos.relative(getNeighbourDirection($$4, pState.getValue(FACING)));
            BlockState $$6 = pLevel.getBlockState($$5);
            if ($$6.is(this) && $$6.getValue(PART) == BedPart.HEAD) {
               pLevel.setBlock($$5, Blocks.AIR.defaultBlockState(), 35);
               pLevel.levelEvent(pPlayer, 2001, $$5, Block.getId($$6));
            }
         }
      }

      return super.playerWillDestroy(pLevel, pPos, pState, pPlayer);
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      Direction $$1 = pContext.getHorizontalDirection();
      BlockPos $$2 = pContext.getClickedPos();
      BlockPos $$3 = $$2.relative($$1);
      Level $$4 = pContext.getLevel();
      return $$4.getBlockState($$3).canBeReplaced(pContext) && $$4.getWorldBorder().isWithinBounds($$3) ? this.defaultBlockState().setValue(FACING, $$1) : null;
   }

   @Override
   protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      Direction $$4 = getConnectedDirection(pState).getOpposite();
      switch ($$4) {
         case NORTH:
            return NORTH_SHAPE;
         case SOUTH:
            return SOUTH_SHAPE;
         case WEST:
            return WEST_SHAPE;
         default:
            return EAST_SHAPE;
      }
   }

   public static Direction getConnectedDirection(BlockState pState) {
      Direction $$1 = pState.getValue(FACING);
      return pState.getValue(PART) == BedPart.HEAD ? $$1.getOpposite() : $$1;
   }

   public static DoubleBlockCombiner.BlockType getBlockType(BlockState pState) {
      BedPart $$1 = pState.getValue(PART);
      return $$1 == BedPart.HEAD ? DoubleBlockCombiner.BlockType.FIRST : DoubleBlockCombiner.BlockType.SECOND;
   }

   private static boolean isBunkBed(BlockGetter pLevel, BlockPos pPos) {
      return pLevel.getBlockState(pPos.below()).getBlock() instanceof BedBlock;
   }

   public static Optional<Vec3> findStandUpPosition(
      EntityType<?> pEntityType, CollisionGetter pCollisionGetter, BlockPos pPos, Direction pDirection, float pYRot
   ) {
      Direction $$5 = pDirection.getClockWise();
      Direction $$6 = $$5.isFacingAngle(pYRot) ? $$5.getOpposite() : $$5;
      if (isBunkBed(pCollisionGetter, pPos)) {
         return findBunkBedStandUpPosition(pEntityType, pCollisionGetter, pPos, pDirection, $$6);
      } else {
         int[][] $$7 = bedStandUpOffsets(pDirection, $$6);
         Optional<Vec3> $$8 = findStandUpPositionAtOffset(pEntityType, pCollisionGetter, pPos, $$7, true);
         return $$8.isPresent() ? $$8 : findStandUpPositionAtOffset(pEntityType, pCollisionGetter, pPos, $$7, false);
      }
   }

   private static Optional<Vec3> findBunkBedStandUpPosition(
      EntityType<?> pEntityType, CollisionGetter pCollisionGetter, BlockPos pPos, Direction pStateFacing, Direction pEntityFacing
   ) {
      int[][] $$5 = bedSurroundStandUpOffsets(pStateFacing, pEntityFacing);
      Optional<Vec3> $$6 = findStandUpPositionAtOffset(pEntityType, pCollisionGetter, pPos, $$5, true);
      if ($$6.isPresent()) {
         return $$6;
      } else {
         BlockPos $$7 = pPos.below();
         Optional<Vec3> $$8 = findStandUpPositionAtOffset(pEntityType, pCollisionGetter, $$7, $$5, true);
         if ($$8.isPresent()) {
            return $$8;
         } else {
            int[][] $$9 = bedAboveStandUpOffsets(pStateFacing);
            Optional<Vec3> $$10 = findStandUpPositionAtOffset(pEntityType, pCollisionGetter, pPos, $$9, true);
            if ($$10.isPresent()) {
               return $$10;
            } else {
               Optional<Vec3> $$11 = findStandUpPositionAtOffset(pEntityType, pCollisionGetter, pPos, $$5, false);
               if ($$11.isPresent()) {
                  return $$11;
               } else {
                  Optional<Vec3> $$12 = findStandUpPositionAtOffset(pEntityType, pCollisionGetter, $$7, $$5, false);
                  return $$12.isPresent() ? $$12 : findStandUpPositionAtOffset(pEntityType, pCollisionGetter, pPos, $$9, false);
               }
            }
         }
      }
   }

   private static Optional<Vec3> findStandUpPositionAtOffset(
      EntityType<?> pEntityType, CollisionGetter pCollisionGetter, BlockPos pPos, int[][] pOffsets, boolean pSimulate
   ) {
      BlockPos.MutableBlockPos $$5 = new BlockPos.MutableBlockPos();

      for (int[] $$6 : pOffsets) {
         $$5.set(pPos.getX() + $$6[0], pPos.getY(), pPos.getZ() + $$6[1]);
         Vec3 $$7 = DismountHelper.findSafeDismountLocation(pEntityType, pCollisionGetter, $$5, pSimulate);
         if ($$7 != null) {
            return Optional.of($$7);
         }
      }

      return Optional.empty();
   }

   @Override
   protected RenderShape getRenderShape(BlockState pState) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(FACING, PART, OCCUPIED);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
      return new BedBlockEntity(pPos, pState, this.color);
   }

   @Override
   public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
      super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
      if (!pLevel.isClientSide) {
         BlockPos $$5 = pPos.relative(pState.getValue(FACING));
         pLevel.setBlock($$5, pState.setValue(PART, BedPart.HEAD), 3);
         pLevel.blockUpdated(pPos, Blocks.AIR);
         pState.updateNeighbourShapes(pLevel, pPos, 3);
      }
   }

   public DyeColor getColor() {
      return this.color;
   }

   @Override
   protected long getSeed(BlockState pState, BlockPos pPos) {
      BlockPos $$2 = pPos.relative(pState.getValue(FACING), pState.getValue(PART) == BedPart.HEAD ? 0 : 1);
      return Mth.getSeed($$2.getX(), pPos.getY(), $$2.getZ());
   }

   @Override
   protected boolean isPathfindable(BlockState pState, PathComputationType pPathComputationType) {
      return false;
   }

   private static int[][] bedStandUpOffsets(Direction pFirstDir, Direction pSecondDir) {
      return (int[][])ArrayUtils.addAll(bedSurroundStandUpOffsets(pFirstDir, pSecondDir), bedAboveStandUpOffsets(pFirstDir));
   }

   private static int[][] bedSurroundStandUpOffsets(Direction pFirstDir, Direction pSecondDir) {
      return new int[][]{
         {pSecondDir.getStepX(), pSecondDir.getStepZ()},
         {pSecondDir.getStepX() - pFirstDir.getStepX(), pSecondDir.getStepZ() - pFirstDir.getStepZ()},
         {pSecondDir.getStepX() - pFirstDir.getStepX() * 2, pSecondDir.getStepZ() - pFirstDir.getStepZ() * 2},
         {-pFirstDir.getStepX() * 2, -pFirstDir.getStepZ() * 2},
         {-pSecondDir.getStepX() - pFirstDir.getStepX() * 2, -pSecondDir.getStepZ() - pFirstDir.getStepZ() * 2},
         {-pSecondDir.getStepX() - pFirstDir.getStepX(), -pSecondDir.getStepZ() - pFirstDir.getStepZ()},
         {-pSecondDir.getStepX(), -pSecondDir.getStepZ()},
         {-pSecondDir.getStepX() + pFirstDir.getStepX(), -pSecondDir.getStepZ() + pFirstDir.getStepZ()},
         {pFirstDir.getStepX(), pFirstDir.getStepZ()},
         {pSecondDir.getStepX() + pFirstDir.getStepX(), pSecondDir.getStepZ() + pFirstDir.getStepZ()}
      };
   }

   private static int[][] bedAboveStandUpOffsets(Direction pDir) {
      return new int[][]{{0, 0}, {-pDir.getStepX(), -pDir.getStepZ()}};
   }
}
