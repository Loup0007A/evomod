package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;

public class FollowTemptation extends Behavior<PathfinderMob> {
   public static final int TEMPTATION_COOLDOWN = 100;
   public static final double DEFAULT_CLOSE_ENOUGH_DIST = 2.5;
   public static final double BACKED_UP_CLOSE_ENOUGH_DIST = 3.5;
   private final Function<LivingEntity, Float> speedModifier;
   private final Function<LivingEntity, Double> closeEnoughDistance;

   public FollowTemptation(Function<LivingEntity, Float> pSpeedModifier) {
      this(pSpeedModifier, p_288784_ -> 2.5);
   }

   public FollowTemptation(Function<LivingEntity, Float> pSpeedModifier, Function<LivingEntity, Double> pCloseEnoughDistance) {
      super(Util.make(() -> {
         Builder<MemoryModuleType<?>, MemoryStatus> $$0 = ImmutableMap.builder();
         $$0.put(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED);
         $$0.put(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED);
         $$0.put(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT);
         $$0.put(MemoryModuleType.IS_TEMPTED, MemoryStatus.REGISTERED);
         $$0.put(MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_PRESENT);
         $$0.put(MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT);
         $$0.put(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT);
         return $$0.build();
      }));
      this.speedModifier = pSpeedModifier;
      this.closeEnoughDistance = pCloseEnoughDistance;
   }

   protected float getSpeedModifier(PathfinderMob pPathfinder) {
      return this.speedModifier.apply(pPathfinder);
   }

   private Optional<Player> getTemptingPlayer(PathfinderMob pPathfinder) {
      return pPathfinder.getBrain().getMemory(MemoryModuleType.TEMPTING_PLAYER);
   }

   @Override
   protected boolean timedOut(long pGameTime) {
      return false;
   }

   protected boolean canStillUse(ServerLevel pLevel, PathfinderMob pEntity, long pGameTime) {
      return this.getTemptingPlayer(pEntity).isPresent()
         && !pEntity.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET)
         && !pEntity.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING);
   }

   protected void start(ServerLevel pLevel, PathfinderMob pEntity, long pGameTime) {
      pEntity.getBrain().setMemory(MemoryModuleType.IS_TEMPTED, true);
   }

   protected void stop(ServerLevel pLevel, PathfinderMob pEntity, long pGameTime) {
      Brain<?> $$3 = pEntity.getBrain();
      $$3.setMemory(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS, 100);
      $$3.setMemory(MemoryModuleType.IS_TEMPTED, false);
      $$3.eraseMemory(MemoryModuleType.WALK_TARGET);
      $$3.eraseMemory(MemoryModuleType.LOOK_TARGET);
   }

   protected void tick(ServerLevel pLevel, PathfinderMob pOwner, long pGameTime) {
      Player $$3 = this.getTemptingPlayer(pOwner).get();
      Brain<?> $$4 = pOwner.getBrain();
      $$4.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker($$3, true));
      double $$5 = this.closeEnoughDistance.apply(pOwner);
      if (pOwner.distanceToSqr($$3) < Mth.square($$5)) {
         $$4.eraseMemory(MemoryModuleType.WALK_TARGET);
      } else {
         $$4.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker($$3, false), this.getSpeedModifier(pOwner), 2));
      }
   }
}
