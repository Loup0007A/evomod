package net.minecraft.world.level.block.entity.trialspawner;

import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public class TrialSpawnerData {
   public static final String TAG_SPAWN_DATA = "spawn_data";
   private static final String TAG_NEXT_MOB_SPAWNS_AT = "next_mob_spawns_at";
   private static final int DELAY_BETWEEN_PLAYER_SCANS = 20;
   private static final int TRIAL_OMEN_PER_BAD_OMEN_LEVEL = 18000;
   public static MapCodec<TrialSpawnerData> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_313188_ -> p_313188_.group(
               UUIDUtil.CODEC_SET.lenientOptionalFieldOf("registered_players", Sets.newHashSet()).forGetter(p_309580_ -> p_309580_.detectedPlayers),
               UUIDUtil.CODEC_SET.lenientOptionalFieldOf("current_mobs", Sets.newHashSet()).forGetter(p_311034_ -> p_311034_.currentMobs),
               Codec.LONG.lenientOptionalFieldOf("cooldown_ends_at", 0L).forGetter(p_309685_ -> p_309685_.cooldownEndsAt),
               Codec.LONG.lenientOptionalFieldOf("next_mob_spawns_at", 0L).forGetter(p_310876_ -> p_310876_.nextMobSpawnsAt),
               Codec.intRange(0, Integer.MAX_VALUE).lenientOptionalFieldOf("total_mobs_spawned", 0).forGetter(p_309745_ -> p_309745_.totalMobsSpawned),
               SpawnData.CODEC.lenientOptionalFieldOf("spawn_data").forGetter(p_312904_ -> p_312904_.nextSpawnData),
               ResourceKey.codec(Registries.LOOT_TABLE).lenientOptionalFieldOf("ejecting_loot_table").forGetter(p_310765_ -> p_310765_.ejectingLootTable)
            )
            .apply(p_313188_, TrialSpawnerData::new)
   );
   protected final Set<UUID> detectedPlayers = new HashSet<>();
   protected final Set<UUID> currentMobs = new HashSet<>();
   protected long cooldownEndsAt;
   protected long nextMobSpawnsAt;
   protected int totalMobsSpawned;
   protected Optional<SpawnData> nextSpawnData;
   protected Optional<ResourceKey<LootTable>> ejectingLootTable;
   @Nullable
   protected Entity displayEntity;
   @Nullable
   private SimpleWeightedRandomList<ItemStack> dispensing;
   protected double spin;
   protected double oSpin;

   public TrialSpawnerData() {
      this(Collections.emptySet(), Collections.emptySet(), 0L, 0L, 0, Optional.empty(), Optional.empty());
   }

   public TrialSpawnerData(
      Set<UUID> p_312543_,
      Set<UUID> p_311274_,
      long p_312908_,
      long p_311373_,
      int p_311452_,
      Optional<SpawnData> p_311258_,
      Optional<ResourceKey<LootTable>> p_312612_
   ) {
      this.detectedPlayers.addAll(p_312543_);
      this.currentMobs.addAll(p_311274_);
      this.cooldownEndsAt = p_312908_;
      this.nextMobSpawnsAt = p_311373_;
      this.totalMobsSpawned = p_311452_;
      this.nextSpawnData = p_311258_;
      this.ejectingLootTable = p_312612_;
   }

   public void reset() {
      this.detectedPlayers.clear();
      this.totalMobsSpawned = 0;
      this.nextMobSpawnsAt = 0L;
      this.cooldownEndsAt = 0L;
      this.currentMobs.clear();
      this.nextSpawnData = Optional.empty();
   }

   public boolean hasMobToSpawn(TrialSpawner pTrialSpawner, RandomSource pRandom) {
      boolean $$2 = this.getOrCreateNextSpawnData(pTrialSpawner, pRandom).getEntityToSpawn().contains("id", 8);
      return $$2 || !pTrialSpawner.getConfig().spawnPotentialsDefinition().isEmpty();
   }

   public boolean hasFinishedSpawningAllMobs(TrialSpawnerConfig pConfig, int pPlayers) {
      return this.totalMobsSpawned >= pConfig.calculateTargetTotalMobs(pPlayers);
   }

   public boolean haveAllCurrentMobsDied() {
      return this.currentMobs.isEmpty();
   }

   public boolean isReadyToSpawnNextMob(ServerLevel pLevel, TrialSpawnerConfig pConfig, int pPlayers) {
      return pLevel.getGameTime() >= this.nextMobSpawnsAt && this.currentMobs.size() < pConfig.calculateTargetSimultaneousMobs(pPlayers);
   }

   public int countAdditionalPlayers(BlockPos pPos) {
      if (this.detectedPlayers.isEmpty()) {
         Util.logAndPauseIfInIde("Trial Spawner at " + pPos + " has no detected players");
      }

      return Math.max(0, this.detectedPlayers.size() - 1);
   }

   public void tryDetectPlayers(ServerLevel pLevel, BlockPos pPos, TrialSpawner pSpawner) {
      boolean $$3 = (pPos.asLong() + pLevel.getGameTime()) % 20L != 0L;
      if (!$$3) {
         if (!pSpawner.getState().equals(TrialSpawnerState.COOLDOWN) || !pSpawner.isOminous()) {
            List<UUID> $$4 = pSpawner.getPlayerDetector().detect(pLevel, pSpawner.getEntitySelector(), pPos, (double)pSpawner.getRequiredPlayerRange(), true);
            boolean $$7;
            if (!pSpawner.isOminous() && !$$4.isEmpty()) {
               Optional<Pair<Player, Holder<MobEffect>>> $$6 = findPlayerWithOminousEffect(pLevel, $$4);
               $$6.ifPresent(p_341867_ -> {
                  Player $$4x = (Player)p_341867_.getFirst();
                  if (p_341867_.getSecond() == MobEffects.BAD_OMEN) {
                     transformBadOmenIntoTrialOmen($$4x);
                  }

                  pLevel.levelEvent(3020, BlockPos.containing($$4x.getEyePosition()), 0);
                  pSpawner.applyOminous(pLevel, pPos);
               });
               $$7 = $$6.isPresent();
            } else {
               $$7 = false;
            }

            if (!pSpawner.getState().equals(TrialSpawnerState.COOLDOWN) || $$7) {
               boolean $$8 = pSpawner.getData().detectedPlayers.isEmpty();
               List<UUID> $$9 = $$8
                  ? $$4
                  : pSpawner.getPlayerDetector().detect(pLevel, pSpawner.getEntitySelector(), pPos, (double)pSpawner.getRequiredPlayerRange(), false);
               if (this.detectedPlayers.addAll($$9)) {
                  this.nextMobSpawnsAt = Math.max(pLevel.getGameTime() + 40L, this.nextMobSpawnsAt);
                  if (!$$7) {
                     int $$10 = pSpawner.isOminous() ? 3019 : 3013;
                     pLevel.levelEvent($$10, pPos, this.detectedPlayers.size());
                  }
               }
            }
         }
      }
   }

   private static Optional<Pair<Player, Holder<MobEffect>>> findPlayerWithOminousEffect(ServerLevel pLevel, List<UUID> pPlayers) {
      Player $$2 = null;

      for (UUID $$3 : pPlayers) {
         Player $$4 = pLevel.getPlayerByUUID($$3);
         if ($$4 != null) {
            Holder<MobEffect> $$5 = MobEffects.TRIAL_OMEN;
            if ($$4.hasEffect($$5)) {
               return Optional.of(Pair.of($$4, $$5));
            }

            if ($$4.hasEffect(MobEffects.BAD_OMEN)) {
               $$2 = $$4;
            }
         }
      }

      return Optional.ofNullable($$2).map(p_341863_ -> Pair.of(p_341863_, MobEffects.BAD_OMEN));
   }

   public void resetAfterBecomingOminous(TrialSpawner pSpawner, ServerLevel pLevel) {
      this.currentMobs.stream().map(pLevel::getEntity).forEach(p_341869_ -> {
         if (p_341869_ != null) {
            pLevel.levelEvent(3012, p_341869_.blockPosition(), TrialSpawner.FlameParticle.NORMAL.encode());
            if (p_341869_ instanceof Mob $$2) {
               $$2.dropPreservedEquipment();
            }

            p_341869_.remove(Entity.RemovalReason.DISCARDED);
         }
      });
      if (!pSpawner.getOminousConfig().spawnPotentialsDefinition().isEmpty()) {
         this.nextSpawnData = Optional.empty();
      }

      this.totalMobsSpawned = 0;
      this.currentMobs.clear();
      this.nextMobSpawnsAt = pLevel.getGameTime() + (long)pSpawner.getOminousConfig().ticksBetweenSpawn();
      pSpawner.markUpdated();
      this.cooldownEndsAt = pLevel.getGameTime() + pSpawner.getOminousConfig().ticksBetweenItemSpawners();
   }

   private static void transformBadOmenIntoTrialOmen(Player pPlayer) {
      MobEffectInstance $$1 = pPlayer.getEffect(MobEffects.BAD_OMEN);
      if ($$1 != null) {
         int $$2 = $$1.getAmplifier() + 1;
         int $$3 = 18000 * $$2;
         pPlayer.removeEffect(MobEffects.BAD_OMEN);
         pPlayer.addEffect(new MobEffectInstance(MobEffects.TRIAL_OMEN, $$3, 0));
      }
   }

   public boolean isReadyToOpenShutter(ServerLevel pLevel, float pDelay, int pTargetCooldownLength) {
      long $$3 = this.cooldownEndsAt - (long)pTargetCooldownLength;
      return (float)pLevel.getGameTime() >= (float)$$3 + pDelay;
   }

   public boolean isReadyToEjectItems(ServerLevel pLevel, float pDelay, int pTargetCooldownLength) {
      long $$3 = this.cooldownEndsAt - (long)pTargetCooldownLength;
      return (float)(pLevel.getGameTime() - $$3) % pDelay == 0.0F;
   }

   public boolean isCooldownFinished(ServerLevel pLevel) {
      return pLevel.getGameTime() >= this.cooldownEndsAt;
   }

   public void setEntityId(TrialSpawner pSpawner, RandomSource pRandom, EntityType<?> pEntityType) {
      this.getOrCreateNextSpawnData(pSpawner, pRandom).getEntityToSpawn().putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(pEntityType).toString());
   }

   protected SpawnData getOrCreateNextSpawnData(TrialSpawner pSpawner, RandomSource pRandom) {
      if (this.nextSpawnData.isPresent()) {
         return this.nextSpawnData.get();
      } else {
         SimpleWeightedRandomList<SpawnData> $$2 = pSpawner.getConfig().spawnPotentialsDefinition();
         Optional<SpawnData> $$3 = $$2.isEmpty() ? this.nextSpawnData : $$2.getRandom(pRandom).map(WeightedEntry.Wrapper::data);
         this.nextSpawnData = Optional.of($$3.orElseGet(SpawnData::new));
         pSpawner.markUpdated();
         return this.nextSpawnData.get();
      }
   }

   @Nullable
   public Entity getOrCreateDisplayEntity(TrialSpawner pSpawner, Level pLevel, TrialSpawnerState pSpawnerState) {
      if (!pSpawnerState.hasSpinningMob()) {
         return null;
      } else {
         if (this.displayEntity == null) {
            CompoundTag $$3 = this.getOrCreateNextSpawnData(pSpawner, pLevel.getRandom()).getEntityToSpawn();
            if ($$3.contains("id", 8)) {
               this.displayEntity = EntityType.loadEntityRecursive($$3, pLevel, Function.identity());
            }
         }

         return this.displayEntity;
      }
   }

   public CompoundTag getUpdateTag(TrialSpawnerState pSpawnerState) {
      CompoundTag $$1 = new CompoundTag();
      if (pSpawnerState == TrialSpawnerState.ACTIVE) {
         $$1.putLong("next_mob_spawns_at", this.nextMobSpawnsAt);
      }

      this.nextSpawnData
         .ifPresent(
            p_327366_ -> $$1.put(
                  "spawn_data",
                  (Tag)SpawnData.CODEC.encodeStart(NbtOps.INSTANCE, p_327366_).result().orElseThrow(() -> new IllegalStateException("Invalid SpawnData"))
               )
         );
      return $$1;
   }

   public double getSpin() {
      return this.spin;
   }

   public double getOSpin() {
      return this.oSpin;
   }

   SimpleWeightedRandomList<ItemStack> getDispensingItems(ServerLevel pLevel, TrialSpawnerConfig pConfig, BlockPos pPos) {
      if (this.dispensing != null) {
         return this.dispensing;
      } else {
         LootTable $$3 = pLevel.getServer().reloadableRegistries().getLootTable(pConfig.itemsToDropWhenOminous());
         LootParams $$4 = new LootParams.Builder(pLevel).create(LootContextParamSets.EMPTY);
         long $$5 = lowResolutionPosition(pLevel, pPos);
         ObjectArrayList<ItemStack> $$6 = $$3.getRandomItems($$4, $$5);
         if ($$6.isEmpty()) {
            return SimpleWeightedRandomList.empty();
         } else {
            SimpleWeightedRandomList.Builder<ItemStack> $$7 = new SimpleWeightedRandomList.Builder<>();
            ObjectListIterator var10 = $$6.iterator();

            while (var10.hasNext()) {
               ItemStack $$8 = (ItemStack)var10.next();
               $$7.add($$8.copyWithCount(1), $$8.getCount());
            }

            this.dispensing = $$7.build();
            return this.dispensing;
         }
      }
   }

   private static long lowResolutionPosition(ServerLevel pLevel, BlockPos pPos) {
      BlockPos $$2 = new BlockPos(Mth.floor((float)pPos.getX() / 30.0F), Mth.floor((float)pPos.getY() / 20.0F), Mth.floor((float)pPos.getZ() / 30.0F));
      return pLevel.getSeed() + $$2.asLong();
   }
}
