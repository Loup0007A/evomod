package net.minecraft.world.entity.vehicle;

import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DismountHelper {
   public static int[][] offsetsForDirection(Direction pDirection) {
      Direction $$1 = pDirection.getClockWise();
      Direction $$2 = $$1.getOpposite();
      Direction $$3 = pDirection.getOpposite();
      return new int[][]{
         {$$1.getStepX(), $$1.getStepZ()},
         {$$2.getStepX(), $$2.getStepZ()},
         {$$3.getStepX() + $$1.getStepX(), $$3.getStepZ() + $$1.getStepZ()},
         {$$3.getStepX() + $$2.getStepX(), $$3.getStepZ() + $$2.getStepZ()},
         {pDirection.getStepX() + $$1.getStepX(), pDirection.getStepZ() + $$1.getStepZ()},
         {pDirection.getStepX() + $$2.getStepX(), pDirection.getStepZ() + $$2.getStepZ()},
         {$$3.getStepX(), $$3.getStepZ()},
         {pDirection.getStepX(), pDirection.getStepZ()}
      };
   }

   public static boolean isBlockFloorValid(double pDistance) {
      return !Double.isInfinite(pDistance) && pDistance < 1.0;
   }

   public static boolean canDismountTo(CollisionGetter pLevel, LivingEntity pPassenger, AABB pBoundingBox) {
      for (VoxelShape $$4 : pLevel.getBlockCollisions(pPassenger, pBoundingBox)) {
         if (!$$4.isEmpty()) {
            return false;
         }
      }

      return pLevel.getWorldBorder().isWithinBounds(pBoundingBox);
   }

   public static boolean canDismountTo(CollisionGetter pLevel, Vec3 pOffset, LivingEntity pPassenger, Pose pPose) {
      return canDismountTo(pLevel, pPassenger, pPassenger.getLocalBoundsForPose(pPose).move(pOffset));
   }

   public static VoxelShape nonClimbableShape(BlockGetter pLevel, BlockPos pPos) {
      BlockState $$2 = pLevel.getBlockState(pPos);
      return !$$2.is(BlockTags.CLIMBABLE) && (!($$2.getBlock() instanceof TrapDoorBlock) || !$$2.getValue(TrapDoorBlock.OPEN))
         ? $$2.getCollisionShape(pLevel, pPos)
         : Shapes.empty();
   }

   public static double findCeilingFrom(BlockPos pPos, int pCeiling, Function<BlockPos, VoxelShape> pShapeForPos) {
      BlockPos.MutableBlockPos $$3 = pPos.mutable();
      int $$4 = 0;

      while ($$4 < pCeiling) {
         VoxelShape $$5 = pShapeForPos.apply($$3);
         if (!$$5.isEmpty()) {
            return (double)(pPos.getY() + $$4) + $$5.min(Direction.Axis.Y);
         }

         $$4++;
         $$3.move(Direction.UP);
      }

      return Double.POSITIVE_INFINITY;
   }

   @Nullable
   public static Vec3 findSafeDismountLocation(EntityType<?> pEntityType, CollisionGetter pLevel, BlockPos pPos, boolean pOnlySafePositions) {
      if (pOnlySafePositions && pEntityType.isBlockDangerous(pLevel.getBlockState(pPos))) {
         return null;
      } else {
         double $$4 = pLevel.getBlockFloorHeight(nonClimbableShape(pLevel, pPos), () -> nonClimbableShape(pLevel, pPos.below()));
         if (!isBlockFloorValid($$4)) {
            return null;
         } else if (pOnlySafePositions && $$4 <= 0.0 && pEntityType.isBlockDangerous(pLevel.getBlockState(pPos.below()))) {
            return null;
         } else {
            Vec3 $$5 = Vec3.upFromBottomCenterOf(pPos, $$4);
            AABB $$6 = pEntityType.getDimensions().makeBoundingBox($$5);

            for (VoxelShape $$8 : pLevel.getBlockCollisions(null, $$6)) {
               if (!$$8.isEmpty()) {
                  return null;
               }
            }

            if (pEntityType != EntityType.PLAYER
               || !pLevel.getBlockState(pPos).is(BlockTags.INVALID_SPAWN_INSIDE) && !pLevel.getBlockState(pPos.above()).is(BlockTags.INVALID_SPAWN_INSIDE)) {
               return !pLevel.getWorldBorder().isWithinBounds($$6) ? null : $$5;
            } else {
               return null;
            }
         }
      }
   }
}
