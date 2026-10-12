package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.pathfinder.Path;
import org.apache.commons.lang3.mutable.MutableLong;

public class AcquirePoi {
   public static final int SCAN_RANGE = 48;

   public static BehaviorControl<PathfinderMob> create(
      Predicate<Holder<PoiType>> pAcquirablePois, MemoryModuleType<GlobalPos> pAcquiringMemory, boolean pOnlyIfAdult, Optional<Byte> pEntityEventId
   ) {
      return create(pAcquirablePois, pAcquiringMemory, pAcquiringMemory, pOnlyIfAdult, pEntityEventId);
   }

   public static BehaviorControl<PathfinderMob> create(
      Predicate<Holder<PoiType>> pAcquirablePois,
      MemoryModuleType<GlobalPos> pExistingAbsentMemory,
      MemoryModuleType<GlobalPos> pAcquiringMemory,
      boolean pOnlyIfAdult,
      Optional<Byte> pEntityEventId
   ) {
      int $$5 = 5;
      int $$6 = 20;
      MutableLong $$7 = new MutableLong(0L);
      Long2ObjectMap<AcquirePoi.JitteredLinearRetry> $$8 = new Long2ObjectOpenHashMap();
      OneShot<PathfinderMob> $$9 = BehaviorBuilder.create(
         p_258276_ -> p_258276_.group(p_258276_.absent(pAcquiringMemory))
               .apply(
                  p_258276_,
                  p_258300_ -> (p_258292_, p_258293_, p_258294_) -> {
                        if (pOnlyIfAdult && p_258293_.isBaby()) {
                           return false;
                        } else if ($$7.getValue() == 0L) {
                           $$7.setValue(p_258292_.getGameTime() + (long)p_258292_.random.nextInt(20));
                           return false;
                        } else if (p_258292_.getGameTime() < $$7.getValue()) {
                           return false;
                        } else {
                           $$7.setValue(p_258294_ + 20L + (long)p_258292_.getRandom().nextInt(20));
                           PoiManager $$9x = p_258292_.getPoiManager();
                           $$8.long2ObjectEntrySet().removeIf(p_22338_ -> !((AcquirePoi.JitteredLinearRetry)p_22338_.getValue()).isStillValid(p_258294_));
                           Predicate<BlockPos> $$10 = p_258266_ -> {
                              AcquirePoi.JitteredLinearRetry $$3 = (AcquirePoi.JitteredLinearRetry)$$8.get(p_258266_.asLong());
                              if ($$3 == null) {
                                 return true;
                              } else if (!$$3.shouldRetry(p_258294_)) {
                                 return false;
                              } else {
                                 $$3.markAttempt(p_258294_);
                                 return true;
                              }
                           };
                           Set<Pair<Holder<PoiType>, BlockPos>> $$11 = $$9x.findAllClosestFirstWithType(
                                 pAcquirablePois, $$10, p_258293_.blockPosition(), 48, PoiManager.Occupancy.HAS_SPACE
                              )
                              .limit(5L)
                              .collect(Collectors.toSet());
                           Path $$12 = findPathToPois(p_258293_, $$11);
                           if ($$12 != null && $$12.canReach()) {
                              BlockPos $$13 = $$12.getTarget();
                              $$9x.getType($$13).ifPresent(p_326810_ -> {
                                 $$9x.take(pAcquirablePois, (p_217108_, p_217109_) -> p_217109_.equals($$13), $$13, 1);
                                 p_258300_.set(GlobalPos.of(p_258292_.dimension(), $$13));
                                 pEntityEventId.ifPresent(p_147369_ -> p_258292_.broadcastEntityEvent(p_258293_, p_147369_));
                                 $$8.clear();
                                 DebugPackets.sendPoiTicketCountPacket(p_258292_, $$13);
                              });
                           } else {
                              for (Pair<Holder<PoiType>, BlockPos> $$14 : $$11) {
                                 $$8.computeIfAbsent(
                                    ((BlockPos)$$14.getSecond()).asLong(), p_264881_ -> new AcquirePoi.JitteredLinearRetry(p_258292_.random, p_258294_)
                                 );
                              }
                           }

                           return true;
                        }
                     }
               )
      );
      return pAcquiringMemory == pExistingAbsentMemory
         ? $$9
         : BehaviorBuilder.create(p_258269_ -> p_258269_.group(p_258269_.absent(pExistingAbsentMemory)).apply(p_258269_, p_258302_ -> $$9));
   }

   @Nullable
   public static Path findPathToPois(Mob pMob, Set<Pair<Holder<PoiType>, BlockPos>> pPoiPositions) {
      if (pPoiPositions.isEmpty()) {
         return null;
      } else {
         Set<BlockPos> $$2 = new HashSet<>();
         int $$3 = 1;

         for (Pair<Holder<PoiType>, BlockPos> $$4 : pPoiPositions) {
            $$3 = Math.max($$3, ((PoiType)((Holder)$$4.getFirst()).value()).validRange());
            $$2.add((BlockPos)$$4.getSecond());
         }

         return pMob.getNavigation().createPath($$2, $$3);
      }
   }

   static class JitteredLinearRetry {
      private static final int MIN_INTERVAL_INCREASE = 40;
      private static final int MAX_INTERVAL_INCREASE = 80;
      private static final int MAX_RETRY_PATHFINDING_INTERVAL = 400;
      private final RandomSource random;
      private long previousAttemptTimestamp;
      private long nextScheduledAttemptTimestamp;
      private int currentDelay;

      JitteredLinearRetry(RandomSource pRandom, long pTimestamp) {
         this.random = pRandom;
         this.markAttempt(pTimestamp);
      }

      public void markAttempt(long pTimestamp) {
         this.previousAttemptTimestamp = pTimestamp;
         int $$1 = this.currentDelay + this.random.nextInt(40) + 40;
         this.currentDelay = Math.min($$1, 400);
         this.nextScheduledAttemptTimestamp = pTimestamp + (long)this.currentDelay;
      }

      public boolean isStillValid(long pTimestamp) {
         return pTimestamp - this.previousAttemptTimestamp < 400L;
      }

      public boolean shouldRetry(long pTimestamp) {
         return pTimestamp >= this.nextScheduledAttemptTimestamp;
      }

      @Override
      public String toString() {
         return "RetryMarker{, previousAttemptAt="
            + this.previousAttemptTimestamp
            + ", nextScheduledAttemptAt="
            + this.nextScheduledAttemptTimestamp
            + ", currentDelay="
            + this.currentDelay
            + "}";
      }
   }
}
