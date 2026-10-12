package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class SculkShriekerBlockEntity extends BlockEntity implements GameEventListener.Provider<VibrationSystem.Listener>, VibrationSystem {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int WARNING_SOUND_RADIUS = 10;
   private static final int WARDEN_SPAWN_ATTEMPTS = 20;
   private static final int WARDEN_SPAWN_RANGE_XZ = 5;
   private static final int WARDEN_SPAWN_RANGE_Y = 6;
   private static final int DARKNESS_RADIUS = 40;
   private static final int SHRIEKING_TICKS = 90;
   private static final Int2ObjectMap<SoundEvent> SOUND_BY_LEVEL = Util.make(new Int2ObjectOpenHashMap(), p_222866_ -> {
      p_222866_.put(1, SoundEvents.WARDEN_NEARBY_CLOSE);
      p_222866_.put(2, SoundEvents.WARDEN_NEARBY_CLOSER);
      p_222866_.put(3, SoundEvents.WARDEN_NEARBY_CLOSEST);
      p_222866_.put(4, SoundEvents.WARDEN_LISTENING_ANGRY);
   });
   private int warningLevel;
   private final VibrationSystem.User vibrationUser = new SculkShriekerBlockEntity.VibrationUser();
   private VibrationSystem.Data vibrationData = new VibrationSystem.Data();
   private final VibrationSystem.Listener vibrationListener = new VibrationSystem.Listener(this);

   public SculkShriekerBlockEntity(BlockPos pPos, BlockState pBlockState) {
      super(BlockEntityType.SCULK_SHRIEKER, pPos, pBlockState);
   }

   @Override
   public VibrationSystem.Data getVibrationData() {
      return this.vibrationData;
   }

   @Override
   public VibrationSystem.User getVibrationUser() {
      return this.vibrationUser;
   }

   @Override
   protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      super.loadAdditional(pTag, pRegistries);
      if (pTag.contains("warning_level", 99)) {
         this.warningLevel = pTag.getInt("warning_level");
      }

      RegistryOps<Tag> $$2 = pRegistries.createSerializationContext(NbtOps.INSTANCE);
      if (pTag.contains("listener", 10)) {
         VibrationSystem.Data.CODEC
            .parse($$2, pTag.getCompound("listener"))
            .resultOrPartial(p_341844_ -> LOGGER.error("Failed to parse vibration listener for Sculk Shrieker: '{}'", p_341844_))
            .ifPresent(p_281147_ -> this.vibrationData = p_281147_);
      }
   }

   @Override
   protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      super.saveAdditional(pTag, pRegistries);
      pTag.putInt("warning_level", this.warningLevel);
      RegistryOps<Tag> $$2 = pRegistries.createSerializationContext(NbtOps.INSTANCE);
      VibrationSystem.Data.CODEC
         .encodeStart($$2, this.vibrationData)
         .resultOrPartial(p_341843_ -> LOGGER.error("Failed to encode vibration listener for Sculk Shrieker: '{}'", p_341843_))
         .ifPresent(p_222871_ -> pTag.put("listener", p_222871_));
   }

   @Nullable
   public static ServerPlayer tryGetPlayer(@Nullable Entity pEntity) {
      if (pEntity instanceof ServerPlayer) {
         return (ServerPlayer)pEntity;
      } else {
         if (pEntity != null) {
            LivingEntity $$6 = pEntity.getControllingPassenger();
            if ($$6 instanceof ServerPlayer) {
               return (ServerPlayer)$$6;
            }
         }

         if (pEntity instanceof Projectile $$3) {
            Entity var3 = $$3.getOwner();
            if (var3 instanceof ServerPlayer) {
               return (ServerPlayer)var3;
            }
         }

         if (pEntity instanceof ItemEntity $$5) {
            Entity var9 = $$5.getOwner();
            if (var9 instanceof ServerPlayer) {
               return (ServerPlayer)var9;
            }
         }

         return null;
      }
   }

   public void tryShriek(ServerLevel pLevel, @Nullable ServerPlayer pPlayer) {
      if (pPlayer != null) {
         BlockState $$2 = this.getBlockState();
         if (!$$2.getValue(SculkShriekerBlock.SHRIEKING)) {
            this.warningLevel = 0;
            if (!this.canRespond(pLevel) || this.tryToWarn(pLevel, pPlayer)) {
               this.shriek(pLevel, pPlayer);
            }
         }
      }
   }

   private boolean tryToWarn(ServerLevel pLevel, ServerPlayer pPlayer) {
      OptionalInt $$2 = WardenSpawnTracker.tryWarn(pLevel, this.getBlockPos(), pPlayer);
      $$2.ifPresent(p_222838_ -> this.warningLevel = p_222838_);
      return $$2.isPresent();
   }

   private void shriek(ServerLevel pLevel, @Nullable Entity pSourceEntity) {
      BlockPos $$2 = this.getBlockPos();
      BlockState $$3 = this.getBlockState();
      pLevel.setBlock($$2, $$3.setValue(SculkShriekerBlock.SHRIEKING, Boolean.valueOf(true)), 2);
      pLevel.scheduleTick($$2, $$3.getBlock(), 90);
      pLevel.levelEvent(3007, $$2, 0);
      pLevel.gameEvent(GameEvent.SHRIEK, $$2, GameEvent.Context.of(pSourceEntity));
   }

   private boolean canRespond(ServerLevel pLevel) {
      return this.getBlockState().getValue(SculkShriekerBlock.CAN_SUMMON)
         && pLevel.getDifficulty() != Difficulty.PEACEFUL
         && pLevel.getGameRules().getBoolean(GameRules.RULE_DO_WARDEN_SPAWNING);
   }

   public void tryRespond(ServerLevel pLevel) {
      if (this.canRespond(pLevel) && this.warningLevel > 0) {
         if (!this.trySummonWarden(pLevel)) {
            this.playWardenReplySound(pLevel);
         }

         Warden.applyDarknessAround(pLevel, Vec3.atCenterOf(this.getBlockPos()), null, 40);
      }
   }

   private void playWardenReplySound(Level pLevel) {
      SoundEvent $$1 = (SoundEvent)SOUND_BY_LEVEL.get(this.warningLevel);
      if ($$1 != null) {
         BlockPos $$2 = this.getBlockPos();
         int $$3 = $$2.getX() + Mth.randomBetweenInclusive(pLevel.random, -10, 10);
         int $$4 = $$2.getY() + Mth.randomBetweenInclusive(pLevel.random, -10, 10);
         int $$5 = $$2.getZ() + Mth.randomBetweenInclusive(pLevel.random, -10, 10);
         pLevel.playSound(null, (double)$$3, (double)$$4, (double)$$5, $$1, SoundSource.HOSTILE, 5.0F, 1.0F);
      }
   }

   private boolean trySummonWarden(ServerLevel pLevel) {
      return this.warningLevel < 4
         ? false
         : SpawnUtil.trySpawnMob(EntityType.WARDEN, MobSpawnType.TRIGGERED, pLevel, this.getBlockPos(), 20, 5, 6, SpawnUtil.Strategy.ON_TOP_OF_COLLIDER)
            .isPresent();
   }

   public VibrationSystem.Listener getListener() {
      return this.vibrationListener;
   }

   class VibrationUser implements VibrationSystem.User {
      private static final int LISTENER_RADIUS = 8;
      private final PositionSource positionSource = new BlockPositionSource(SculkShriekerBlockEntity.this.worldPosition);

      public VibrationUser() {
      }

      @Override
      public int getListenerRadius() {
         return 8;
      }

      @Override
      public PositionSource getPositionSource() {
         return this.positionSource;
      }

      @Override
      public TagKey<GameEvent> getListenableEvents() {
         return GameEventTags.SHRIEKER_CAN_LISTEN;
      }

      @Override
      public boolean canReceiveVibration(ServerLevel p_281256_, BlockPos p_281528_, Holder<GameEvent> p_335342_, GameEvent.Context p_282914_) {
         return !SculkShriekerBlockEntity.this.getBlockState().getValue(SculkShriekerBlock.SHRIEKING)
            && SculkShriekerBlockEntity.tryGetPlayer(p_282914_.sourceEntity()) != null;
      }

      @Override
      public void onReceiveVibration(
         ServerLevel p_283372_, BlockPos p_281679_, Holder<GameEvent> p_330622_, @Nullable Entity p_282286_, @Nullable Entity p_281384_, float p_283119_
      ) {
         SculkShriekerBlockEntity.this.tryShriek(p_283372_, SculkShriekerBlockEntity.tryGetPlayer(p_281384_ != null ? p_281384_ : p_282286_));
      }

      @Override
      public void onDataChanged() {
         SculkShriekerBlockEntity.this.setChanged();
      }

      @Override
      public boolean requiresAdjacentChunksToBeTicking() {
         return true;
      }
   }
}
