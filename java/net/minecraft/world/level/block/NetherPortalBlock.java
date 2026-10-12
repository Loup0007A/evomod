package net.minecraft.world.level.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;

public class NetherPortalBlock extends Block implements Portal {
   public static final MapCodec<NetherPortalBlock> CODEC = simpleCodec(NetherPortalBlock::new);
   public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
   private static final Logger LOGGER = LogUtils.getLogger();
   protected static final int AABB_OFFSET = 2;
   protected static final VoxelShape X_AXIS_AABB = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
   protected static final VoxelShape Z_AXIS_AABB = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);

   @Override
   public MapCodec<NetherPortalBlock> codec() {
      return CODEC;
   }

   public NetherPortalBlock(BlockBehaviour.Properties p_54909_) {
      super(p_54909_);
      this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
   }

   @Override
   protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      switch ((Direction.Axis)pState.getValue(AXIS)) {
         case Z:
            return Z_AXIS_AABB;
         case X:
         default:
            return X_AXIS_AABB;
      }
   }

   @Override
   protected void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      if (pLevel.dimensionType().natural()
         && pLevel.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
         && pRandom.nextInt(2000) < pLevel.getDifficulty().getId()) {
         while (pLevel.getBlockState(pPos).is(this)) {
            pPos = pPos.below();
         }

         if (pLevel.getBlockState(pPos).isValidSpawn(pLevel, pPos, EntityType.ZOMBIFIED_PIGLIN)) {
            Entity $$4 = EntityType.ZOMBIFIED_PIGLIN.spawn(pLevel, pPos.above(), MobSpawnType.STRUCTURE);
            if ($$4 != null) {
               $$4.setPortalCooldown();
            }
         }
      }
   }

   @Override
   protected BlockState updateShape(
      BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos
   ) {
      Direction.Axis $$6 = pFacing.getAxis();
      Direction.Axis $$7 = pState.getValue(AXIS);
      boolean $$8 = $$7 != $$6 && $$6.isHorizontal();
      return !$$8 && !pFacingState.is(this) && !new PortalShape(pLevel, pCurrentPos, $$7).isComplete()
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
   }

   @Override
   protected void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
      if (pEntity.canUsePortal(false)) {
         pEntity.setAsInsidePortal(this, pPos);
      }
   }

   @Override
   public int getPortalTransitionTime(ServerLevel pLevel, Entity pEntity) {
      return pEntity instanceof Player $$2
         ? Math.max(
            1,
            pLevel.getGameRules()
               .getInt(
                  $$2.getAbilities().invulnerable ? GameRules.RULE_PLAYERS_NETHER_PORTAL_CREATIVE_DELAY : GameRules.RULE_PLAYERS_NETHER_PORTAL_DEFAULT_DELAY
               )
         )
         : 0;
   }

   @Nullable
   @Override
   public DimensionTransition getPortalDestination(ServerLevel pLevel, Entity pEntity, BlockPos pPos) {
      ResourceKey<Level> $$3 = pLevel.dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER;
      ServerLevel $$4 = pLevel.getServer().getLevel($$3);
      if ($$4 == null) {
         return null;
      } else {
         boolean $$5 = $$4.dimension() == Level.NETHER;
         WorldBorder $$6 = $$4.getWorldBorder();
         double $$7 = DimensionType.getTeleportationScale(pLevel.dimensionType(), $$4.dimensionType());
         BlockPos $$8 = $$6.clampToBounds(pEntity.getX() * $$7, pEntity.getY(), pEntity.getZ() * $$7);
         return this.getExitPortal($$4, pEntity, pPos, $$8, $$5, $$6);
      }
   }

   @Nullable
   private DimensionTransition getExitPortal(ServerLevel pLevel, Entity pEntity, BlockPos pPos, BlockPos pExitPos, boolean pIsNether, WorldBorder pWorldBorder) {
      Optional<BlockPos> $$6 = pLevel.getPortalForcer().findClosestPortalPosition(pExitPos, pIsNether, pWorldBorder);
      BlockUtil.FoundRectangle $$9;
      DimensionTransition.PostDimensionTransition $$10;
      if ($$6.isPresent()) {
         BlockPos $$7 = $$6.get();
         BlockState $$8 = pLevel.getBlockState($$7);
         $$9 = BlockUtil.getLargestRectangleAround(
            $$7, $$8.getValue(BlockStateProperties.HORIZONTAL_AXIS), 21, Direction.Axis.Y, 21, p_343533_ -> pLevel.getBlockState(p_343533_) == $$8
         );
         $$10 = DimensionTransition.PLAY_PORTAL_SOUND.then(p_343530_ -> p_343530_.placePortalTicket($$7));
      } else {
         Direction.Axis $$11 = pEntity.level().getBlockState(pPos).getOptionalValue(AXIS).orElse(Direction.Axis.X);
         Optional<BlockUtil.FoundRectangle> $$12 = pLevel.getPortalForcer().createPortal(pExitPos, $$11);
         if ($$12.isEmpty()) {
            LOGGER.error("Unable to create a portal, likely target out of worldborder");
            return null;
         }

         $$9 = $$12.get();
         $$10 = DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET);
      }

      return getDimensionTransitionFromExit(pEntity, pPos, $$9, pLevel, $$10);
   }

   private static DimensionTransition getDimensionTransitionFromExit(
      Entity pEntity,
      BlockPos pPos,
      BlockUtil.FoundRectangle pRectangle,
      ServerLevel pLevel,
      DimensionTransition.PostDimensionTransition pPostDimensionTransition
   ) {
      BlockState $$5 = pEntity.level().getBlockState(pPos);
      Direction.Axis $$6;
      Vec3 $$8;
      if ($$5.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
         $$6 = $$5.getValue(BlockStateProperties.HORIZONTAL_AXIS);
         BlockUtil.FoundRectangle $$7 = BlockUtil.getLargestRectangleAround(
            pPos, $$6, 21, Direction.Axis.Y, 21, p_342174_ -> pEntity.level().getBlockState(p_342174_) == $$5
         );
         $$8 = pEntity.getRelativePortalPosition($$6, $$7);
      } else {
         $$6 = Direction.Axis.X;
         $$8 = new Vec3(0.5, 0.0, 0.0);
      }

      return createDimensionTransition(
         pLevel, pRectangle, $$6, $$8, pEntity, pEntity.getDeltaMovement(), pEntity.getYRot(), pEntity.getXRot(), pPostDimensionTransition
      );
   }

   private static DimensionTransition createDimensionTransition(
      ServerLevel pLevel,
      BlockUtil.FoundRectangle pRectangle,
      Direction.Axis pAxis,
      Vec3 pOffset,
      Entity pEntity,
      Vec3 pSpeed,
      float pYRot,
      float pXRot,
      DimensionTransition.PostDimensionTransition pPostDimensionTransition
   ) {
      BlockPos $$9 = pRectangle.minCorner;
      BlockState $$10 = pLevel.getBlockState($$9);
      Direction.Axis $$11 = $$10.getOptionalValue(BlockStateProperties.HORIZONTAL_AXIS).orElse(Direction.Axis.X);
      double $$12 = (double)pRectangle.axis1Size;
      double $$13 = (double)pRectangle.axis2Size;
      EntityDimensions $$14 = pEntity.getDimensions(pEntity.getPose());
      int $$15 = pAxis == $$11 ? 0 : 90;
      Vec3 $$16 = pAxis == $$11 ? pSpeed : new Vec3(pSpeed.z, pSpeed.y, -pSpeed.x);
      double $$17 = (double)$$14.width() / 2.0 + ($$12 - (double)$$14.width()) * pOffset.x();
      double $$18 = ($$13 - (double)$$14.height()) * pOffset.y();
      double $$19 = 0.5 + pOffset.z();
      boolean $$20 = $$11 == Direction.Axis.X;
      Vec3 $$21 = new Vec3((double)$$9.getX() + ($$20 ? $$17 : $$19), (double)$$9.getY() + $$18, (double)$$9.getZ() + ($$20 ? $$19 : $$17));
      Vec3 $$22 = PortalShape.findCollisionFreePosition($$21, pLevel, pEntity, $$14);
      return new DimensionTransition(pLevel, $$22, $$16, pYRot + (float)$$15, pXRot, pPostDimensionTransition);
   }

   @Override
   public Portal.Transition getLocalTransition() {
      return Portal.Transition.CONFUSION;
   }

   @Override
   public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
      if (pRandom.nextInt(100) == 0) {
         pLevel.playLocalSound(
            (double)pPos.getX() + 0.5,
            (double)pPos.getY() + 0.5,
            (double)pPos.getZ() + 0.5,
            SoundEvents.PORTAL_AMBIENT,
            SoundSource.BLOCKS,
            0.5F,
            pRandom.nextFloat() * 0.4F + 0.8F,
            false
         );
      }

      for (int $$4 = 0; $$4 < 4; $$4++) {
         double $$5 = (double)pPos.getX() + pRandom.nextDouble();
         double $$6 = (double)pPos.getY() + pRandom.nextDouble();
         double $$7 = (double)pPos.getZ() + pRandom.nextDouble();
         double $$8 = ((double)pRandom.nextFloat() - 0.5) * 0.5;
         double $$9 = ((double)pRandom.nextFloat() - 0.5) * 0.5;
         double $$10 = ((double)pRandom.nextFloat() - 0.5) * 0.5;
         int $$11 = pRandom.nextInt(2) * 2 - 1;
         if (!pLevel.getBlockState(pPos.west()).is(this) && !pLevel.getBlockState(pPos.east()).is(this)) {
            $$5 = (double)pPos.getX() + 0.5 + 0.25 * (double)$$11;
            $$8 = (double)(pRandom.nextFloat() * 2.0F * (float)$$11);
         } else {
            $$7 = (double)pPos.getZ() + 0.5 + 0.25 * (double)$$11;
            $$10 = (double)(pRandom.nextFloat() * 2.0F * (float)$$11);
         }

         pLevel.addParticle(ParticleTypes.PORTAL, $$5, $$6, $$7, $$8, $$9, $$10);
      }
   }

   @Override
   public ItemStack getCloneItemStack(LevelReader pLevel, BlockPos pPos, BlockState pState) {
      return ItemStack.EMPTY;
   }

   @Override
   protected BlockState rotate(BlockState pState, Rotation pRot) {
      switch (pRot) {
         case COUNTERCLOCKWISE_90:
         case CLOCKWISE_90:
            switch ((Direction.Axis)pState.getValue(AXIS)) {
               case Z:
                  return pState.setValue(AXIS, Direction.Axis.X);
               case X:
                  return pState.setValue(AXIS, Direction.Axis.Z);
               default:
                  return pState;
            }
         default:
            return pState;
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(AXIS);
   }
}
