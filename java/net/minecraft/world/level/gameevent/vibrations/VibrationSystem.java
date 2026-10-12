package net.minecraft.world.level.gameevent.vibrations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public interface VibrationSystem {
   List<ResourceKey<GameEvent>> RESONANCE_EVENTS = List.of(
      GameEvent.RESONATE_1.key(),
      GameEvent.RESONATE_2.key(),
      GameEvent.RESONATE_3.key(),
      GameEvent.RESONATE_4.key(),
      GameEvent.RESONATE_5.key(),
      GameEvent.RESONATE_6.key(),
      GameEvent.RESONATE_7.key(),
      GameEvent.RESONATE_8.key(),
      GameEvent.RESONATE_9.key(),
      GameEvent.RESONATE_10.key(),
      GameEvent.RESONATE_11.key(),
      GameEvent.RESONATE_12.key(),
      GameEvent.RESONATE_13.key(),
      GameEvent.RESONATE_14.key(),
      GameEvent.RESONATE_15.key()
   );
   int DEFAULT_VIBRATION_FREQUENCY = 0;
   ToIntFunction<ResourceKey<GameEvent>> VIBRATION_FREQUENCY_FOR_EVENT = Util.make(new Reference2IntOpenHashMap(), p_330465_ -> {
      p_330465_.defaultReturnValue(0);
      p_330465_.put(GameEvent.STEP.key(), 1);
      p_330465_.put(GameEvent.SWIM.key(), 1);
      p_330465_.put(GameEvent.FLAP.key(), 1);
      p_330465_.put(GameEvent.PROJECTILE_LAND.key(), 2);
      p_330465_.put(GameEvent.HIT_GROUND.key(), 2);
      p_330465_.put(GameEvent.SPLASH.key(), 2);
      p_330465_.put(GameEvent.ITEM_INTERACT_FINISH.key(), 3);
      p_330465_.put(GameEvent.PROJECTILE_SHOOT.key(), 3);
      p_330465_.put(GameEvent.INSTRUMENT_PLAY.key(), 3);
      p_330465_.put(GameEvent.ENTITY_ACTION.key(), 4);
      p_330465_.put(GameEvent.ELYTRA_GLIDE.key(), 4);
      p_330465_.put(GameEvent.UNEQUIP.key(), 4);
      p_330465_.put(GameEvent.ENTITY_DISMOUNT.key(), 5);
      p_330465_.put(GameEvent.EQUIP.key(), 5);
      p_330465_.put(GameEvent.ENTITY_INTERACT.key(), 6);
      p_330465_.put(GameEvent.SHEAR.key(), 6);
      p_330465_.put(GameEvent.ENTITY_MOUNT.key(), 6);
      p_330465_.put(GameEvent.ENTITY_DAMAGE.key(), 7);
      p_330465_.put(GameEvent.DRINK.key(), 8);
      p_330465_.put(GameEvent.EAT.key(), 8);
      p_330465_.put(GameEvent.CONTAINER_CLOSE.key(), 9);
      p_330465_.put(GameEvent.BLOCK_CLOSE.key(), 9);
      p_330465_.put(GameEvent.BLOCK_DEACTIVATE.key(), 9);
      p_330465_.put(GameEvent.BLOCK_DETACH.key(), 9);
      p_330465_.put(GameEvent.CONTAINER_OPEN.key(), 10);
      p_330465_.put(GameEvent.BLOCK_OPEN.key(), 10);
      p_330465_.put(GameEvent.BLOCK_ACTIVATE.key(), 10);
      p_330465_.put(GameEvent.BLOCK_ATTACH.key(), 10);
      p_330465_.put(GameEvent.PRIME_FUSE.key(), 10);
      p_330465_.put(GameEvent.NOTE_BLOCK_PLAY.key(), 10);
      p_330465_.put(GameEvent.BLOCK_CHANGE.key(), 11);
      p_330465_.put(GameEvent.BLOCK_DESTROY.key(), 12);
      p_330465_.put(GameEvent.FLUID_PICKUP.key(), 12);
      p_330465_.put(GameEvent.BLOCK_PLACE.key(), 13);
      p_330465_.put(GameEvent.FLUID_PLACE.key(), 13);
      p_330465_.put(GameEvent.ENTITY_PLACE.key(), 14);
      p_330465_.put(GameEvent.LIGHTNING_STRIKE.key(), 14);
      p_330465_.put(GameEvent.TELEPORT.key(), 14);
      p_330465_.put(GameEvent.ENTITY_DIE.key(), 15);
      p_330465_.put(GameEvent.EXPLODE.key(), 15);

      for (int $$1 = 1; $$1 <= 15; $$1++) {
         p_330465_.put(getResonanceEventByFrequency($$1), $$1);
      }
   });

   VibrationSystem.Data getVibrationData();

   VibrationSystem.User getVibrationUser();

   static int getGameEventFrequency(Holder<GameEvent> pGameEvent) {
      return pGameEvent.unwrapKey().map(VibrationSystem::getGameEventFrequency).orElse(0);
   }

   static int getGameEventFrequency(ResourceKey<GameEvent> p_335067_) {
      return VIBRATION_FREQUENCY_FOR_EVENT.applyAsInt(p_335067_);
   }

   static ResourceKey<GameEvent> getResonanceEventByFrequency(int pFrequency) {
      return RESONANCE_EVENTS.get(pFrequency - 1);
   }

   static int getRedstoneStrengthForDistance(float pDistance, int pMaxDistance) {
      double $$2 = 15.0 / (double)pMaxDistance;
      return Math.max(1, 15 - Mth.floor($$2 * (double)pDistance));
   }

   public static final class Data {
      public static Codec<VibrationSystem.Data> CODEC = RecordCodecBuilder.create(
         p_327444_ -> p_327444_.group(
                  VibrationInfo.CODEC.lenientOptionalFieldOf("event").forGetter(p_281665_ -> Optional.ofNullable(p_281665_.currentVibration)),
                  VibrationSelector.CODEC.fieldOf("selector").forGetter(VibrationSystem.Data::getSelectionStrategy),
                  ExtraCodecs.NON_NEGATIVE_INT.fieldOf("event_delay").orElse(0).forGetter(VibrationSystem.Data::getTravelTimeInTicks)
               )
               .apply(
                  p_327444_, (p_281934_, p_282381_, p_282931_) -> new VibrationSystem.Data((VibrationInfo)p_281934_.orElse(null), p_282381_, p_282931_, true)
               )
      );
      public static final String NBT_TAG_KEY = "listener";
      @Nullable
      VibrationInfo currentVibration;
      private int travelTimeInTicks;
      final VibrationSelector selectionStrategy;
      private boolean reloadVibrationParticle;

      private Data(@Nullable VibrationInfo pCurrentVibration, VibrationSelector pSelectionStrategy, int pTravelTimeInTicks, boolean pReloadVibrationParticle) {
         this.currentVibration = pCurrentVibration;
         this.travelTimeInTicks = pTravelTimeInTicks;
         this.selectionStrategy = pSelectionStrategy;
         this.reloadVibrationParticle = pReloadVibrationParticle;
      }

      public Data() {
         this(null, new VibrationSelector(), 0, false);
      }

      public VibrationSelector getSelectionStrategy() {
         return this.selectionStrategy;
      }

      @Nullable
      public VibrationInfo getCurrentVibration() {
         return this.currentVibration;
      }

      public void setCurrentVibration(@Nullable VibrationInfo pCurrentVibration) {
         this.currentVibration = pCurrentVibration;
      }

      public int getTravelTimeInTicks() {
         return this.travelTimeInTicks;
      }

      public void setTravelTimeInTicks(int pTravelTimeInTicks) {
         this.travelTimeInTicks = pTravelTimeInTicks;
      }

      public void decrementTravelTime() {
         this.travelTimeInTicks = Math.max(0, this.travelTimeInTicks - 1);
      }

      public boolean shouldReloadVibrationParticle() {
         return this.reloadVibrationParticle;
      }

      public void setReloadVibrationParticle(boolean pReloadVibrationParticle) {
         this.reloadVibrationParticle = pReloadVibrationParticle;
      }
   }

   public static class Listener implements GameEventListener {
      private final VibrationSystem system;

      public Listener(VibrationSystem pSystem) {
         this.system = pSystem;
      }

      @Override
      public PositionSource getListenerSource() {
         return this.system.getVibrationUser().getPositionSource();
      }

      @Override
      public int getListenerRadius() {
         return this.system.getVibrationUser().getListenerRadius();
      }

      @Override
      public boolean handleGameEvent(ServerLevel pLevel, Holder<GameEvent> pGameEvent, GameEvent.Context pContext, Vec3 pPos) {
         VibrationSystem.Data $$4 = this.system.getVibrationData();
         VibrationSystem.User $$5 = this.system.getVibrationUser();
         if ($$4.getCurrentVibration() != null) {
            return false;
         } else if (!$$5.isValidVibration(pGameEvent, pContext)) {
            return false;
         } else {
            Optional<Vec3> $$6 = $$5.getPositionSource().getPosition(pLevel);
            if ($$6.isEmpty()) {
               return false;
            } else {
               Vec3 $$7 = $$6.get();
               if (!$$5.canReceiveVibration(pLevel, BlockPos.containing(pPos), pGameEvent, pContext)) {
                  return false;
               } else if (isOccluded(pLevel, pPos, $$7)) {
                  return false;
               } else {
                  this.scheduleVibration(pLevel, $$4, pGameEvent, pContext, pPos, $$7);
                  return true;
               }
            }
         }
      }

      public void forceScheduleVibration(ServerLevel pLevel, Holder<GameEvent> pGameEvent, GameEvent.Context pContext, Vec3 pPos) {
         this.system
            .getVibrationUser()
            .getPositionSource()
            .getPosition(pLevel)
            .ifPresent(p_327449_ -> this.scheduleVibration(pLevel, this.system.getVibrationData(), pGameEvent, pContext, pPos, p_327449_));
      }

      private void scheduleVibration(
         ServerLevel pLevel, VibrationSystem.Data pData, Holder<GameEvent> pGameEvent, GameEvent.Context pContext, Vec3 pPos, Vec3 pSensorPos
      ) {
         pData.selectionStrategy
            .addCandidate(new VibrationInfo(pGameEvent, (float)pPos.distanceTo(pSensorPos), pPos, pContext.sourceEntity()), pLevel.getGameTime());
      }

      public static float distanceBetweenInBlocks(BlockPos pPos1, BlockPos pPos2) {
         return (float)Math.sqrt(pPos1.distSqr(pPos2));
      }

      private static boolean isOccluded(Level pLevel, Vec3 pEventPos, Vec3 pVibrationUserPos) {
         Vec3 $$3 = new Vec3((double)Mth.floor(pEventPos.x) + 0.5, (double)Mth.floor(pEventPos.y) + 0.5, (double)Mth.floor(pEventPos.z) + 0.5);
         Vec3 $$4 = new Vec3(
            (double)Mth.floor(pVibrationUserPos.x) + 0.5, (double)Mth.floor(pVibrationUserPos.y) + 0.5, (double)Mth.floor(pVibrationUserPos.z) + 0.5
         );

         for (Direction $$5 : Direction.values()) {
            Vec3 $$6 = $$3.relative($$5, 1.0E-5F);
            if (pLevel.isBlockInLine(new ClipBlockStateContext($$6, $$4, p_283608_ -> p_283608_.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS))).getType()
               != HitResult.Type.BLOCK) {
               return false;
            }
         }

         return true;
      }
   }

   public interface Ticker {
      static void tick(Level pLevel, VibrationSystem.Data pData, VibrationSystem.User pUser) {
         if (pLevel instanceof ServerLevel $$3) {
            if (pData.currentVibration == null) {
               trySelectAndScheduleVibration($$3, pData, pUser);
            }

            if (pData.currentVibration != null) {
               boolean $$5 = pData.getTravelTimeInTicks() > 0;
               tryReloadVibrationParticle($$3, pData, pUser);
               pData.decrementTravelTime();
               if (pData.getTravelTimeInTicks() <= 0) {
                  $$5 = receiveVibration($$3, pData, pUser, pData.currentVibration);
               }

               if ($$5) {
                  pUser.onDataChanged();
               }
            }
         }
      }

      private static void trySelectAndScheduleVibration(ServerLevel pLevel, VibrationSystem.Data pData, VibrationSystem.User pUser) {
         pData.getSelectionStrategy()
            .chosenCandidate(pLevel.getGameTime())
            .ifPresent(
               p_282059_ -> {
                  pData.setCurrentVibration(p_282059_);
                  Vec3 $$4 = p_282059_.pos();
                  pData.setTravelTimeInTicks(pUser.calculateTravelTimeInTicks(p_282059_.distance()));
                  pLevel.sendParticles(
                     new VibrationParticleOption(pUser.getPositionSource(), pData.getTravelTimeInTicks()), $$4.x, $$4.y, $$4.z, 1, 0.0, 0.0, 0.0, 0.0
                  );
                  pUser.onDataChanged();
                  pData.getSelectionStrategy().startOver();
               }
            );
      }

      private static void tryReloadVibrationParticle(ServerLevel pLevel, VibrationSystem.Data pData, VibrationSystem.User pUser) {
         if (pData.shouldReloadVibrationParticle()) {
            if (pData.currentVibration == null) {
               pData.setReloadVibrationParticle(false);
            } else {
               Vec3 $$3 = pData.currentVibration.pos();
               PositionSource $$4 = pUser.getPositionSource();
               Vec3 $$5 = $$4.getPosition(pLevel).orElse($$3);
               int $$6 = pData.getTravelTimeInTicks();
               int $$7 = pUser.calculateTravelTimeInTicks(pData.currentVibration.distance());
               double $$8 = 1.0 - (double)$$6 / (double)$$7;
               double $$9 = Mth.lerp($$8, $$3.x, $$5.x);
               double $$10 = Mth.lerp($$8, $$3.y, $$5.y);
               double $$11 = Mth.lerp($$8, $$3.z, $$5.z);
               boolean $$12 = pLevel.sendParticles(new VibrationParticleOption($$4, $$6), $$9, $$10, $$11, 1, 0.0, 0.0, 0.0, 0.0) > 0;
               if ($$12) {
                  pData.setReloadVibrationParticle(false);
               }
            }
         }
      }

      private static boolean receiveVibration(ServerLevel pLevel, VibrationSystem.Data pData, VibrationSystem.User pUser, VibrationInfo pVibrationInfo) {
         BlockPos $$4 = BlockPos.containing(pVibrationInfo.pos());
         BlockPos $$5 = pUser.getPositionSource().getPosition(pLevel).map(BlockPos::containing).orElse($$4);
         if (pUser.requiresAdjacentChunksToBeTicking() && !areAdjacentChunksTicking(pLevel, $$5)) {
            return false;
         } else {
            pUser.onReceiveVibration(
               pLevel,
               $$4,
               pVibrationInfo.gameEvent(),
               pVibrationInfo.getEntity(pLevel).orElse(null),
               pVibrationInfo.getProjectileOwner(pLevel).orElse(null),
               VibrationSystem.Listener.distanceBetweenInBlocks($$4, $$5)
            );
            pData.setCurrentVibration(null);
            return true;
         }
      }

      private static boolean areAdjacentChunksTicking(Level pLevel, BlockPos pPos) {
         ChunkPos $$2 = new ChunkPos(pPos);

         for (int $$3 = $$2.x - 1; $$3 <= $$2.x + 1; $$3++) {
            for (int $$4 = $$2.z - 1; $$4 <= $$2.z + 1; $$4++) {
               if (!pLevel.shouldTickBlocksAt(ChunkPos.asLong($$3, $$4)) || pLevel.getChunkSource().getChunkNow($$3, $$4) == null) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   public interface User {
      int getListenerRadius();

      PositionSource getPositionSource();

      boolean canReceiveVibration(ServerLevel pLevel, BlockPos pPos, Holder<GameEvent> pGameEvent, GameEvent.Context pContext);

      void onReceiveVibration(
         ServerLevel pLevel, BlockPos pPos, Holder<GameEvent> pGameEvent, @Nullable Entity pEntity, @Nullable Entity pPlayerEntity, float pDistance
      );

      default TagKey<GameEvent> getListenableEvents() {
         return GameEventTags.VIBRATIONS;
      }

      default boolean canTriggerAvoidVibration() {
         return false;
      }

      default boolean requiresAdjacentChunksToBeTicking() {
         return false;
      }

      default int calculateTravelTimeInTicks(float pDistance) {
         return Mth.floor(pDistance);
      }

      default boolean isValidVibration(Holder<GameEvent> pGameEvent, GameEvent.Context pContext) {
         if (!pGameEvent.is(this.getListenableEvents())) {
            return false;
         } else {
            Entity $$2 = pContext.sourceEntity();
            if ($$2 != null) {
               if ($$2.isSpectator()) {
                  return false;
               }

               if ($$2.isSteppingCarefully() && pGameEvent.is(GameEventTags.IGNORE_VIBRATIONS_SNEAKING)) {
                  if (this.canTriggerAvoidVibration() && $$2 instanceof ServerPlayer $$3) {
                     CriteriaTriggers.AVOID_VIBRATION.trigger($$3);
                  }

                  return false;
               }

               if ($$2.dampensVibrations()) {
                  return false;
               }
            }

            return pContext.affectedState() != null ? !pContext.affectedState().is(BlockTags.DAMPENS_VIBRATIONS) : true;
         }
      }

      default void onDataChanged() {
      }
   }
}
