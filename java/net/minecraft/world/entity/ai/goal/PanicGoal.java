package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

public class PanicGoal extends Goal {
   public static final int WATER_CHECK_DISTANCE_VERTICAL = 1;
   protected final PathfinderMob mob;
   protected final double speedModifier;
   protected double posX;
   protected double posY;
   protected double posZ;
   protected boolean isRunning;
   private final Function<PathfinderMob, TagKey<DamageType>> panicCausingDamageTypes;

   public PanicGoal(PathfinderMob pMob, double pSpeedModifier) {
      this(pMob, pSpeedModifier, DamageTypeTags.PANIC_CAUSES);
   }

   public PanicGoal(PathfinderMob pMob, double pSpeedModifier, TagKey<DamageType> pPanicCausingDamageTypes) {
      this(pMob, pSpeedModifier, p_341380_ -> pPanicCausingDamageTypes);
   }

   public PanicGoal(PathfinderMob pMob, double pSpeedModifier, Function<PathfinderMob, TagKey<DamageType>> pPanicCausingDamageTypes) {
      this.mob = pMob;
      this.speedModifier = pSpeedModifier;
      this.panicCausingDamageTypes = pPanicCausingDamageTypes;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE));
   }

   @Override
   public boolean canUse() {
      if (!this.shouldPanic()) {
         return false;
      } else {
         if (this.mob.isOnFire()) {
            BlockPos $$0 = this.lookForWater(this.mob.level(), this.mob, 5);
            if ($$0 != null) {
               this.posX = (double)$$0.getX();
               this.posY = (double)$$0.getY();
               this.posZ = (double)$$0.getZ();
               return true;
            }
         }

         return this.findRandomPosition();
      }
   }

   protected boolean shouldPanic() {
      return this.mob.getLastDamageSource() != null && this.mob.getLastDamageSource().is(this.panicCausingDamageTypes.apply(this.mob));
   }

   protected boolean findRandomPosition() {
      Vec3 $$0 = DefaultRandomPos.getPos(this.mob, 5, 4);
      if ($$0 == null) {
         return false;
      } else {
         this.posX = $$0.x;
         this.posY = $$0.y;
         this.posZ = $$0.z;
         return true;
      }
   }

   public boolean isRunning() {
      return this.isRunning;
   }

   @Override
   public void start() {
      this.mob.getNavigation().moveTo(this.posX, this.posY, this.posZ, this.speedModifier);
      this.isRunning = true;
   }

   @Override
   public void stop() {
      this.isRunning = false;
   }

   @Override
   public boolean canContinueToUse() {
      return !this.mob.getNavigation().isDone();
   }

   @Nullable
   protected BlockPos lookForWater(BlockGetter pLevel, Entity pEntity, int pRange) {
      BlockPos $$3 = pEntity.blockPosition();
      return !pLevel.getBlockState($$3).getCollisionShape(pLevel, $$3).isEmpty()
         ? null
         : BlockPos.findClosestMatch(pEntity.blockPosition(), pRange, 1, p_196649_ -> pLevel.getFluidState(p_196649_).is(FluidTags.WATER)).orElse(null);
   }
}
