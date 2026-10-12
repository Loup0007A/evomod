package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class GroundPathNavigation extends PathNavigation {
   private boolean avoidSun;

   public GroundPathNavigation(Mob pMob, Level pLevel) {
      super(pMob, pLevel);
   }

   @Override
   protected PathFinder createPathFinder(int pMaxVisitedNodes) {
      this.nodeEvaluator = new WalkNodeEvaluator();
      this.nodeEvaluator.setCanPassDoors(true);
      return new PathFinder(this.nodeEvaluator, pMaxVisitedNodes);
   }

   @Override
   protected boolean canUpdatePath() {
      return this.mob.onGround() || this.mob.isInLiquid() || this.mob.isPassenger();
   }

   @Override
   protected Vec3 getTempMobPos() {
      return new Vec3(this.mob.getX(), (double)this.getSurfaceY(), this.mob.getZ());
   }

   @Override
   public Path createPath(BlockPos pPos, int pAccuracy) {
      LevelChunk $$2 = this.level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pPos.getX()), SectionPos.blockToSectionCoord(pPos.getZ()));
      if ($$2 == null) {
         return null;
      } else {
         if ($$2.getBlockState(pPos).isAir()) {
            BlockPos $$3 = pPos.below();

            while ($$3.getY() > this.level.getMinBuildHeight() && $$2.getBlockState($$3).isAir()) {
               $$3 = $$3.below();
            }

            if ($$3.getY() > this.level.getMinBuildHeight()) {
               return super.createPath($$3.above(), pAccuracy);
            }

            while ($$3.getY() < this.level.getMaxBuildHeight() && $$2.getBlockState($$3).isAir()) {
               $$3 = $$3.above();
            }

            pPos = $$3;
         }

         if (!$$2.getBlockState(pPos).isSolid()) {
            return super.createPath(pPos, pAccuracy);
         } else {
            BlockPos $$4 = pPos.above();

            while ($$4.getY() < this.level.getMaxBuildHeight() && $$2.getBlockState($$4).isSolid()) {
               $$4 = $$4.above();
            }

            return super.createPath($$4, pAccuracy);
         }
      }
   }

   @Override
   public Path createPath(Entity pEntity, int pAccuracy) {
      return this.createPath(pEntity.blockPosition(), pAccuracy);
   }

   private int getSurfaceY() {
      if (this.mob.isInWater() && this.canFloat()) {
         int $$0 = this.mob.getBlockY();
         BlockState $$1 = this.level.getBlockState(BlockPos.containing(this.mob.getX(), (double)$$0, this.mob.getZ()));
         int $$2 = 0;

         while ($$1.is(Blocks.WATER)) {
            $$1 = this.level.getBlockState(BlockPos.containing(this.mob.getX(), (double)(++$$0), this.mob.getZ()));
            if (++$$2 > 16) {
               return this.mob.getBlockY();
            }
         }

         return $$0;
      } else {
         return Mth.floor(this.mob.getY() + 0.5);
      }
   }

   @Override
   protected void trimPath() {
      super.trimPath();
      if (this.avoidSun) {
         if (this.level.canSeeSky(BlockPos.containing(this.mob.getX(), this.mob.getY() + 0.5, this.mob.getZ()))) {
            return;
         }

         for (int $$0 = 0; $$0 < this.path.getNodeCount(); $$0++) {
            Node $$1 = this.path.getNode($$0);
            if (this.level.canSeeSky(new BlockPos($$1.x, $$1.y, $$1.z))) {
               this.path.truncateNodes($$0);
               return;
            }
         }
      }
   }

   protected boolean hasValidPathType(PathType pPathType) {
      if (pPathType == PathType.WATER) {
         return false;
      } else {
         return pPathType == PathType.LAVA ? false : pPathType != PathType.OPEN;
      }
   }

   public void setCanOpenDoors(boolean pCanOpenDoors) {
      this.nodeEvaluator.setCanOpenDoors(pCanOpenDoors);
   }

   public boolean canPassDoors() {
      return this.nodeEvaluator.canPassDoors();
   }

   public void setCanPassDoors(boolean pCanPassDoors) {
      this.nodeEvaluator.setCanPassDoors(pCanPassDoors);
   }

   public boolean canOpenDoors() {
      return this.nodeEvaluator.canPassDoors();
   }

   public void setAvoidSun(boolean pAvoidSun) {
      this.avoidSun = pAvoidSun;
   }

   public void setCanWalkOverFences(boolean pCanWalkOverFences) {
      this.nodeEvaluator.setCanWalkOverFences(pCanWalkOverFences);
   }
}
