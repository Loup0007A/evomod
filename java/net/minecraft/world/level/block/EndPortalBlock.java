package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EndPortalBlock extends BaseEntityBlock implements Portal {
   public static final MapCodec<EndPortalBlock> CODEC = simpleCodec(EndPortalBlock::new);
   protected static final VoxelShape SHAPE = Block.box(0.0, 6.0, 0.0, 16.0, 12.0, 16.0);

   @Override
   public MapCodec<EndPortalBlock> codec() {
      return CODEC;
   }

   public EndPortalBlock(BlockBehaviour.Properties p_53017_) {
      super(p_53017_);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
      return new TheEndPortalBlockEntity(pPos, pState);
   }

   @Override
   protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SHAPE;
   }

   @Override
   protected void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
      if (pEntity.canUsePortal(false)
         && Shapes.joinIsNotEmpty(
            Shapes.create(pEntity.getBoundingBox().move((double)(-pPos.getX()), (double)(-pPos.getY()), (double)(-pPos.getZ()))),
            pState.getShape(pLevel, pPos),
            BooleanOp.AND
         )) {
         if (!pLevel.isClientSide && pLevel.dimension() == Level.END && pEntity instanceof ServerPlayer $$4 && !$$4.seenCredits) {
            $$4.showEndCredits();
            return;
         }

         pEntity.setAsInsidePortal(this, pPos);
      }
   }

   @Override
   public DimensionTransition getPortalDestination(ServerLevel pLevel, Entity pEntity, BlockPos pPos) {
      ResourceKey<Level> $$3 = pLevel.dimension() == Level.END ? Level.OVERWORLD : Level.END;
      ServerLevel $$4 = pLevel.getServer().getLevel($$3);
      if ($$4 == null) {
         return null;
      } else {
         boolean $$5 = $$3 == Level.END;
         BlockPos $$6 = $$5 ? ServerLevel.END_SPAWN_POINT : $$4.getSharedSpawnPos();
         Vec3 $$7 = $$6.getBottomCenter();
         float $$8 = pEntity.getYRot();
         if ($$5) {
            EndPlatformFeature.createEndPlatform($$4, BlockPos.containing($$7).below(), true);
            $$8 = Direction.WEST.toYRot();
            if (pEntity instanceof ServerPlayer) {
               $$7 = $$7.subtract(0.0, 1.0, 0.0);
            }
         } else {
            if (pEntity instanceof ServerPlayer $$9) {
               return $$9.findRespawnPositionAndUseSpawnBlock(false, DimensionTransition.DO_NOTHING);
            }

            $$7 = pEntity.adjustSpawnLocation($$4, $$6).getBottomCenter();
         }

         return new DimensionTransition(
            $$4, $$7, pEntity.getDeltaMovement(), $$8, pEntity.getXRot(), DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET)
         );
      }
   }

   @Override
   public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
      double $$4 = (double)pPos.getX() + pRandom.nextDouble();
      double $$5 = (double)pPos.getY() + 0.8;
      double $$6 = (double)pPos.getZ() + pRandom.nextDouble();
      pLevel.addParticle(ParticleTypes.SMOKE, $$4, $$5, $$6, 0.0, 0.0, 0.0);
   }

   @Override
   public ItemStack getCloneItemStack(LevelReader pLevel, BlockPos pPos, BlockState pState) {
      return ItemStack.EMPTY;
   }

   @Override
   protected boolean canBeReplaced(BlockState pState, Fluid pFluid) {
      return false;
   }
}
