package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class MoveToTargetSink extends Behavior<Mob> {
   private static final int MAX_COOLDOWN_BEFORE_RETRYING = 40;
   private int remainingCooldown;
   @Nullable
   private Path path;
   @Nullable
   private BlockPos lastTargetPos;
   private float speedModifier;

   public MoveToTargetSink() {
      this(150, 250);
   }

   public MoveToTargetSink(int pMinDuration, int pMaxDuration) {
      super(
         ImmutableMap.of(
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
            MemoryStatus.REGISTERED,
            MemoryModuleType.PATH,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.VALUE_PRESENT
         ),
         pMinDuration,
         pMaxDuration
      );
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, Mob pOwner) {
      if (this.remainingCooldown > 0) {
         this.remainingCooldown--;
         return false;
      } else {
         Brain<?> $$2 = pOwner.getBrain();
         WalkTarget $$3 = $$2.getMemory(MemoryModuleType.WALK_TARGET).get();
         boolean $$4 = this.reachedTarget(pOwner, $$3);
         if (!$$4 && this.tryComputePath(pOwner, $$3, pLevel.getGameTime())) {
            this.lastTargetPos = $$3.getTarget().currentBlockPosition();
            return true;
         } else {
            $$2.eraseMemory(MemoryModuleType.WALK_TARGET);
            if ($$4) {
               $$2.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            }

            return false;
         }
      }
   }

   protected boolean canStillUse(ServerLevel pLevel, Mob pEntity, long pGameTime) {
      if (this.path != null && this.lastTargetPos != null) {
         Optional<WalkTarget> $$3 = pEntity.getBrain().getMemory(MemoryModuleType.WALK_TARGET);
         boolean $$4 = $$3.map(MoveToTargetSink::isWalkTargetSpectator).orElse(false);
         PathNavigation $$5 = pEntity.getNavigation();
         return !$$5.isDone() && $$3.isPresent() && !this.reachedTarget(pEntity, $$3.get()) && !$$4;
      } else {
         return false;
      }
   }

   protected void stop(ServerLevel pLevel, Mob pEntity, long pGameTime) {
      if (pEntity.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)
         && !this.reachedTarget(pEntity, pEntity.getBrain().getMemory(MemoryModuleType.WALK_TARGET).get())
         && pEntity.getNavigation().isStuck()) {
         this.remainingCooldown = pLevel.getRandom().nextInt(40);
      }

      pEntity.getNavigation().stop();
      pEntity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      pEntity.getBrain().eraseMemory(MemoryModuleType.PATH);
      this.path = null;
   }

   protected void start(ServerLevel pLevel, Mob pEntity, long pGameTime) {
      pEntity.getBrain().setMemory(MemoryModuleType.PATH, this.path);
      pEntity.getNavigation().moveTo(this.path, (double)this.speedModifier);
   }

   protected void tick(ServerLevel pLevel, Mob pOwner, long pGameTime) {
      Path $$3 = pOwner.getNavigation().getPath();
      Brain<?> $$4 = pOwner.getBrain();
      if (this.path != $$3) {
         this.path = $$3;
         $$4.setMemory(MemoryModuleType.PATH, $$3);
      }

      if ($$3 != null && this.lastTargetPos != null) {
         WalkTarget $$5 = $$4.getMemory(MemoryModuleType.WALK_TARGET).get();
         if ($$5.getTarget().currentBlockPosition().distSqr(this.lastTargetPos) > 4.0 && this.tryComputePath(pOwner, $$5, pLevel.getGameTime())) {
            this.lastTargetPos = $$5.getTarget().currentBlockPosition();
            this.start(pLevel, pOwner, pGameTime);
         }
      }
   }

   private boolean tryComputePath(Mob pMob, WalkTarget pTarget, long pTime) {
      BlockPos $$3 = pTarget.getTarget().currentBlockPosition();
      this.path = pMob.getNavigation().createPath($$3, 0);
      this.speedModifier = pTarget.getSpeedModifier();
      Brain<?> $$4 = pMob.getBrain();
      if (this.reachedTarget(pMob, pTarget)) {
         $$4.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
      } else {
         boolean $$5 = this.path != null && this.path.canReach();
         if ($$5) {
            $$4.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
         } else if (!$$4.hasMemoryValue(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)) {
            $$4.setMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, pTime);
         }

         if (this.path != null) {
            return true;
         }

         Vec3 $$6 = DefaultRandomPos.getPosTowards((PathfinderMob)pMob, 10, 7, Vec3.atBottomCenterOf($$3), (float) (Math.PI / 2));
         if ($$6 != null) {
            this.path = pMob.getNavigation().createPath($$6.x, $$6.y, $$6.z, 0);
            return this.path != null;
         }
      }

      return false;
   }

   private boolean reachedTarget(Mob pMob, WalkTarget pTarget) {
      return pTarget.getTarget().currentBlockPosition().distManhattan(pMob.blockPosition()) <= pTarget.getCloseEnoughDist();
   }

   private static boolean isWalkTargetSpectator(WalkTarget p_277420_) {
      return p_277420_.getTarget() instanceof EntityTracker $$2 ? $$2.getEntity().isSpectator() : false;
   }
}
