package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class LongJumpToRandomPos<E extends Mob> extends Behavior<E> {
   protected static final int FIND_JUMP_TRIES = 20;
   private static final int PREPARE_JUMP_DURATION = 40;
   protected static final int MIN_PATHFIND_DISTANCE_TO_VALID_JUMP = 8;
   private static final int TIME_OUT_DURATION = 200;
   private static final List<Integer> ALLOWED_ANGLES = Lists.newArrayList(new Integer[]{65, 70, 75, 80});
   private final UniformInt timeBetweenLongJumps;
   protected final int maxLongJumpHeight;
   protected final int maxLongJumpWidth;
   protected final float maxJumpVelocityMultiplier;
   protected List<LongJumpToRandomPos.PossibleJump> jumpCandidates = Lists.newArrayList();
   protected Optional<Vec3> initialPosition = Optional.empty();
   @Nullable
   protected Vec3 chosenJump;
   protected int findJumpTries;
   protected long prepareJumpStart;
   private final Function<E, SoundEvent> getJumpSound;
   private final BiPredicate<E, BlockPos> acceptableLandingSpot;

   public LongJumpToRandomPos(
      UniformInt pTimeBetweenLongJumps, int pMaxLongJumpHeight, int pMaxLongJumpWidth, float pMaxJumpVelocity, Function<E, SoundEvent> pGetJumpSound
   ) {
      this(pTimeBetweenLongJumps, pMaxLongJumpHeight, pMaxLongJumpWidth, pMaxJumpVelocity, pGetJumpSound, LongJumpToRandomPos::defaultAcceptableLandingSpot);
   }

   public static <E extends Mob> boolean defaultAcceptableLandingSpot(E p_251540_, BlockPos p_248879_) {
      Level $$2 = p_251540_.level();
      BlockPos $$3 = p_248879_.below();
      return $$2.getBlockState($$3).isSolidRender($$2, $$3) && p_251540_.getPathfindingMalus(WalkNodeEvaluator.getPathTypeStatic(p_251540_, p_248879_)) == 0.0F;
   }

   public LongJumpToRandomPos(
      UniformInt pTimeBetweenLongJumps,
      int pMaxLongJumpHeight,
      int pMaxLongJumpWidth,
      float pMaxJumpVelocity,
      Function<E, SoundEvent> pGetJumpSound,
      BiPredicate<E, BlockPos> pAcceptableLandingSpot
   ) {
      super(
         ImmutableMap.of(
            MemoryModuleType.LOOK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.LONG_JUMP_MID_JUMP,
            MemoryStatus.VALUE_ABSENT
         ),
         200
      );
      this.timeBetweenLongJumps = pTimeBetweenLongJumps;
      this.maxLongJumpHeight = pMaxLongJumpHeight;
      this.maxLongJumpWidth = pMaxLongJumpWidth;
      this.maxJumpVelocityMultiplier = pMaxJumpVelocity;
      this.getJumpSound = pGetJumpSound;
      this.acceptableLandingSpot = pAcceptableLandingSpot;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, Mob pOwner) {
      boolean $$2 = pOwner.onGround() && !pOwner.isInWater() && !pOwner.isInLava() && !pLevel.getBlockState(pOwner.blockPosition()).is(Blocks.HONEY_BLOCK);
      if (!$$2) {
         pOwner.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, this.timeBetweenLongJumps.sample(pLevel.random) / 2);
      }

      return $$2;
   }

   protected boolean canStillUse(ServerLevel pLevel, Mob pEntity, long pGameTime) {
      boolean $$3 = this.initialPosition.isPresent()
         && this.initialPosition.get().equals(pEntity.position())
         && this.findJumpTries > 0
         && !pEntity.isInWaterOrBubble()
         && (this.chosenJump != null || !this.jumpCandidates.isEmpty());
      if (!$$3 && pEntity.getBrain().getMemory(MemoryModuleType.LONG_JUMP_MID_JUMP).isEmpty()) {
         pEntity.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, this.timeBetweenLongJumps.sample(pLevel.random) / 2);
         pEntity.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
      }

      return $$3;
   }

   protected void start(ServerLevel pLevel, E pEntity, long pGameTime) {
      this.chosenJump = null;
      this.findJumpTries = 20;
      this.initialPosition = Optional.of(pEntity.position());
      BlockPos $$3 = pEntity.blockPosition();
      int $$4 = $$3.getX();
      int $$5 = $$3.getY();
      int $$6 = $$3.getZ();
      this.jumpCandidates = BlockPos.betweenClosedStream(
            $$4 - this.maxLongJumpWidth,
            $$5 - this.maxLongJumpHeight,
            $$6 - this.maxLongJumpWidth,
            $$4 + this.maxLongJumpWidth,
            $$5 + this.maxLongJumpHeight,
            $$6 + this.maxLongJumpWidth
         )
         .filter(p_217317_ -> !p_217317_.equals($$3))
         .map(p_217314_ -> new LongJumpToRandomPos.PossibleJump(p_217314_.immutable(), Mth.ceil($$3.distSqr(p_217314_))))
         .collect(Collectors.toCollection(Lists::newArrayList));
   }

   protected void tick(ServerLevel pLevel, E pOwner, long pGameTime) {
      if (this.chosenJump != null) {
         if (pGameTime - this.prepareJumpStart >= 40L) {
            pOwner.setYRot(pOwner.yBodyRot);
            pOwner.setDiscardFriction(true);
            double $$3 = this.chosenJump.length();
            double $$4 = $$3 + (double)pOwner.getJumpBoostPower();
            pOwner.setDeltaMovement(this.chosenJump.scale($$4 / $$3));
            pOwner.getBrain().setMemory(MemoryModuleType.LONG_JUMP_MID_JUMP, true);
            pLevel.playSound(null, pOwner, this.getJumpSound.apply(pOwner), SoundSource.NEUTRAL, 1.0F, 1.0F);
         }
      } else {
         this.findJumpTries--;
         this.pickCandidate(pLevel, pOwner, pGameTime);
      }
   }

   protected void pickCandidate(ServerLevel pLevel, E pEntity, long pPrepareJumpStart) {
      while (!this.jumpCandidates.isEmpty()) {
         Optional<LongJumpToRandomPos.PossibleJump> $$3 = this.getJumpCandidate(pLevel);
         if (!$$3.isEmpty()) {
            LongJumpToRandomPos.PossibleJump $$4 = $$3.get();
            BlockPos $$5 = $$4.getJumpTarget();
            if (this.isAcceptableLandingPosition(pLevel, pEntity, $$5)) {
               Vec3 $$6 = Vec3.atCenterOf($$5);
               Vec3 $$7 = this.calculateOptimalJumpVector(pEntity, $$6);
               if ($$7 != null) {
                  pEntity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker($$5));
                  PathNavigation $$8 = pEntity.getNavigation();
                  Path $$9 = $$8.createPath($$5, 0, 8);
                  if ($$9 == null || !$$9.canReach()) {
                     this.chosenJump = $$7;
                     this.prepareJumpStart = pPrepareJumpStart;
                     return;
                  }
               }
            }
         }
      }
   }

   protected Optional<LongJumpToRandomPos.PossibleJump> getJumpCandidate(ServerLevel pLevel) {
      Optional<LongJumpToRandomPos.PossibleJump> $$1 = WeightedRandom.getRandomItem(pLevel.random, this.jumpCandidates);
      $$1.ifPresent(this.jumpCandidates::remove);
      return $$1;
   }

   private boolean isAcceptableLandingPosition(ServerLevel pLevel, E pEntity, BlockPos pPos) {
      BlockPos $$3 = pEntity.blockPosition();
      int $$4 = $$3.getX();
      int $$5 = $$3.getZ();
      return $$4 == pPos.getX() && $$5 == pPos.getZ() ? false : this.acceptableLandingSpot.test(pEntity, pPos);
   }

   @Nullable
   protected Vec3 calculateOptimalJumpVector(Mob pMob, Vec3 pTarget) {
      List<Integer> $$2 = Lists.newArrayList(ALLOWED_ANGLES);
      Collections.shuffle($$2);
      float $$3 = (float)(pMob.getAttributeValue(Attributes.JUMP_STRENGTH) * (double)this.maxJumpVelocityMultiplier);

      for (int $$4 : $$2) {
         Optional<Vec3> $$5 = LongJumpUtil.calculateJumpVectorForAngle(pMob, pTarget, $$3, $$4, true);
         if ($$5.isPresent()) {
            return $$5.get();
         }
      }

      return null;
   }

   public static class PossibleJump extends WeightedEntry.IntrusiveBase {
      private final BlockPos jumpTarget;

      public PossibleJump(BlockPos pJumpTarget, int pWeight) {
         super(pWeight);
         this.jumpTarget = pJumpTarget;
      }

      public BlockPos getJumpTarget() {
         return this.jumpTarget;
      }
   }
}
