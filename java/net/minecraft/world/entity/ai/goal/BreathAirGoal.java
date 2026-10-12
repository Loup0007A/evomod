package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;

public class BreathAirGoal extends Goal {
   private final PathfinderMob mob;

   public BreathAirGoal(PathfinderMob pMob) {
      this.mob = pMob;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
   }

   @Override
   public boolean canUse() {
      return this.mob.getAirSupply() < 140;
   }

   @Override
   public boolean canContinueToUse() {
      return this.canUse();
   }

   @Override
   public boolean isInterruptable() {
      return false;
   }

   @Override
   public void start() {
      this.findAirPosition();
   }

   private void findAirPosition() {
      Iterable<BlockPos> $$0 = BlockPos.betweenClosed(
         Mth.floor(this.mob.getX() - 1.0),
         this.mob.getBlockY(),
         Mth.floor(this.mob.getZ() - 1.0),
         Mth.floor(this.mob.getX() + 1.0),
         Mth.floor(this.mob.getY() + 8.0),
         Mth.floor(this.mob.getZ() + 1.0)
      );
      BlockPos $$1 = null;

      for (BlockPos $$2 : $$0) {
         if (this.givesAir(this.mob.level(), $$2)) {
            $$1 = $$2;
            break;
         }
      }

      if ($$1 == null) {
         $$1 = BlockPos.containing(this.mob.getX(), this.mob.getY() + 8.0, this.mob.getZ());
      }

      this.mob.getNavigation().moveTo((double)$$1.getX(), (double)($$1.getY() + 1), (double)$$1.getZ(), 1.0);
   }

   @Override
   public void tick() {
      this.findAirPosition();
      this.mob.moveRelative(0.02F, new Vec3((double)this.mob.xxa, (double)this.mob.yya, (double)this.mob.zza));
      this.mob.move(MoverType.SELF, this.mob.getDeltaMovement());
   }

   private boolean givesAir(LevelReader pLevel, BlockPos pPos) {
      BlockState $$2 = pLevel.getBlockState(pPos);
      return (pLevel.getFluidState(pPos).isEmpty() || $$2.is(Blocks.BUBBLE_COLUMN)) && $$2.isPathfindable(PathComputationType.LAND);
   }
}
