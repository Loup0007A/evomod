package net.minecraft.world.entity.npc;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ServerLevelData;

public class WanderingTraderSpawner implements CustomSpawner {
   private static final int DEFAULT_TICK_DELAY = 1200;
   public static final int DEFAULT_SPAWN_DELAY = 24000;
   private static final int MIN_SPAWN_CHANCE = 25;
   private static final int MAX_SPAWN_CHANCE = 75;
   private static final int SPAWN_CHANCE_INCREASE = 25;
   private static final int SPAWN_ONE_IN_X_CHANCE = 10;
   private static final int NUMBER_OF_SPAWN_ATTEMPTS = 10;
   private final RandomSource random = RandomSource.create();
   private final ServerLevelData serverLevelData;
   private int tickDelay;
   private int spawnDelay;
   private int spawnChance;

   public WanderingTraderSpawner(ServerLevelData pServerLevelData) {
      this.serverLevelData = pServerLevelData;
      this.tickDelay = 1200;
      this.spawnDelay = pServerLevelData.getWanderingTraderSpawnDelay();
      this.spawnChance = pServerLevelData.getWanderingTraderSpawnChance();
      if (this.spawnDelay == 0 && this.spawnChance == 0) {
         this.spawnDelay = 24000;
         pServerLevelData.setWanderingTraderSpawnDelay(this.spawnDelay);
         this.spawnChance = 25;
         pServerLevelData.setWanderingTraderSpawnChance(this.spawnChance);
      }
   }

   @Override
   public int tick(ServerLevel pLevel, boolean pSpawnHostiles, boolean pSpawnPassives) {
      if (!pLevel.getGameRules().getBoolean(GameRules.RULE_DO_TRADER_SPAWNING)) {
         return 0;
      } else if (--this.tickDelay > 0) {
         return 0;
      } else {
         this.tickDelay = 1200;
         this.spawnDelay -= 1200;
         this.serverLevelData.setWanderingTraderSpawnDelay(this.spawnDelay);
         if (this.spawnDelay > 0) {
            return 0;
         } else {
            this.spawnDelay = 24000;
            if (!pLevel.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
               return 0;
            } else {
               int $$3 = this.spawnChance;
               this.spawnChance = Mth.clamp(this.spawnChance + 25, 25, 75);
               this.serverLevelData.setWanderingTraderSpawnChance(this.spawnChance);
               if (this.random.nextInt(100) > $$3) {
                  return 0;
               } else if (this.spawn(pLevel)) {
                  this.spawnChance = 25;
                  return 1;
               } else {
                  return 0;
               }
            }
         }
      }
   }

   private boolean spawn(ServerLevel pServerLevel) {
      Player $$1 = pServerLevel.getRandomPlayer();
      if ($$1 == null) {
         return true;
      } else if (this.random.nextInt(10) != 0) {
         return false;
      } else {
         BlockPos $$2 = $$1.blockPosition();
         int $$3 = 48;
         PoiManager $$4 = pServerLevel.getPoiManager();
         Optional<BlockPos> $$5 = $$4.find(p_219713_ -> p_219713_.is(PoiTypes.MEETING), p_219711_ -> true, $$2, 48, PoiManager.Occupancy.ANY);
         BlockPos $$6 = $$5.orElse($$2);
         BlockPos $$7 = this.findSpawnPositionNear(pServerLevel, $$6, 48);
         if ($$7 != null && this.hasEnoughSpace(pServerLevel, $$7)) {
            if (pServerLevel.getBiome($$7).is(BiomeTags.WITHOUT_WANDERING_TRADER_SPAWNS)) {
               return false;
            }

            WanderingTrader $$8 = EntityType.WANDERING_TRADER.spawn(pServerLevel, $$7, MobSpawnType.EVENT);
            if ($$8 != null) {
               for (int $$9 = 0; $$9 < 2; $$9++) {
                  this.tryToSpawnLlamaFor(pServerLevel, $$8, 4);
               }

               this.serverLevelData.setWanderingTraderId($$8.getUUID());
               $$8.setDespawnDelay(48000);
               $$8.setWanderTarget($$6);
               $$8.restrictTo($$6, 16);
               return true;
            }
         }

         return false;
      }
   }

   private void tryToSpawnLlamaFor(ServerLevel pServerLevel, WanderingTrader pTrader, int pMaxDistance) {
      BlockPos $$3 = this.findSpawnPositionNear(pServerLevel, pTrader.blockPosition(), pMaxDistance);
      if ($$3 != null) {
         TraderLlama $$4 = EntityType.TRADER_LLAMA.spawn(pServerLevel, $$3, MobSpawnType.EVENT);
         if ($$4 != null) {
            $$4.setLeashedTo(pTrader, true);
         }
      }
   }

   @Nullable
   private BlockPos findSpawnPositionNear(LevelReader pLevel, BlockPos pPos, int pMaxDistance) {
      BlockPos $$3 = null;
      SpawnPlacementType $$4 = SpawnPlacements.getPlacementType(EntityType.WANDERING_TRADER);

      for (int $$5 = 0; $$5 < 10; $$5++) {
         int $$6 = pPos.getX() + this.random.nextInt(pMaxDistance * 2) - pMaxDistance;
         int $$7 = pPos.getZ() + this.random.nextInt(pMaxDistance * 2) - pMaxDistance;
         int $$8 = pLevel.getHeight(Heightmap.Types.WORLD_SURFACE, $$6, $$7);
         BlockPos $$9 = new BlockPos($$6, $$8, $$7);
         if ($$4.isSpawnPositionOk(pLevel, $$9, EntityType.WANDERING_TRADER)) {
            $$3 = $$9;
            break;
         }
      }

      return $$3;
   }

   private boolean hasEnoughSpace(BlockGetter pLevel, BlockPos pPos) {
      for (BlockPos $$2 : BlockPos.betweenClosed(pPos, pPos.offset(1, 2, 1))) {
         if (!pLevel.getBlockState($$2).getCollisionShape(pLevel, $$2).isEmpty()) {
            return false;
         }
      }

      return true;
   }
}
